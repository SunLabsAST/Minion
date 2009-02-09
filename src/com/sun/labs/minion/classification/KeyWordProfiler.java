/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */
package com.sun.labs.minion.classification;

import com.sun.labs.minion.QueryConfig;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.util.props.ConfigString;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileMemoryPartition;
import com.sun.labs.minion.indexer.postings.FieldedPostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.retrieval.DictTerm;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A profiler class that puts documents into classes based
 * on the presence of particular keywords.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class KeyWordProfiler implements Profiler, Configurable {

    /**
     * A map from keywords to class names.
     */
    private Map<String, List<String>> m;

    Logger logger = Logger.getLogger(getClass().getName());

    private static String logTag = "KWP";

    public KeyWordProfiler() {
        m = new HashMap<String, List<String>>();
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        classField = ps.getString(PROP_CLASS_FIELD);
        keywordData = ps.getString(PROP_KEYWORD_DATA);
        loadKeywords(ps.getInstanceName(), keywordData);
    }

    public String getName() {
        return null;
    }
    @ConfigString(defaultValue = "class")
    public static final String PROP_CLASS_FIELD = "class_field";

    private String classField;

    public String getClassField() {
        return classField;
    }

    public void setClassField(String classField) {
        this.classField = classField;
    }
    @ConfigString(defaultValue = "why")
    public static final String PROP_FROM_FIELD = "from_field";

    private String fromField;

    public String getFromField() {
        return fromField;
    }

    public void setFromField(String fromField) {
        this.fromField = fromField;
    }
    @ConfigString(defaultValue = "keywords.xml")
    public static final String PROP_KEYWORD_DATA = "keyword_data";

    private String keywordData;

    public String getKeywordData() {
        return keywordData;
    }

    public void setKeywordData(String keywordData) {
        this.keywordData = keywordData;
    }

    private void loadKeywords(String instanceName, String keywordData) throws PropertyException {
        try {
            DocumentBuilder b = DocumentBuilderFactory.newInstance().
                    newDocumentBuilder();
            URL kwu = getClass().getResource(keywordData);
            if(kwu == null) {
                kwu = (new File(keywordData)).toURI().toURL();
            }
            Document d = b.parse(kwu.openStream());
            NodeList n = d.getElementsByTagName("class");
            for(int i = 0; i < n.getLength(); i++) {
                Element ce = (Element) n.item(i);
                NodeList wl = ce.getElementsByTagName("term");
                List<String> words = new ArrayList<String>();
                for(int j = 0; j < wl.getLength(); j++) {
                    words.add(((Element) wl.item(j)).getTextContent().trim());
                }
                m.put(ce.getAttribute("name"), words);
            }
        } catch(MalformedURLException ex) {
            throw new PropertyException(instanceName, PROP_KEYWORD_DATA,
                    "Bad file name: " + keywordData);
        } catch(SAXException ex) {
            throw new PropertyException(instanceName, PROP_KEYWORD_DATA,
                    "Parse error: " + ex.getMessage());
        } catch(IOException ex) {
            throw new PropertyException(instanceName, PROP_KEYWORD_DATA,
                    "Error reading file: " + ex.getMessage());
        } catch(ParserConfigurationException ex) {
            throw new PropertyException(instanceName, PROP_KEYWORD_DATA,
                    "Error in parser configuration: " + ex.getMessage());
        }

    }

    /**
     * Runs the keyword profile.
     */
    public boolean profile(SearchEngineImpl engine, InvFileMemoryPartition mp,
            InvFileDiskPartition dp) {
        QueryConfig qc = engine.getQueryConfig();
        boolean modified = false;

        //
        // Figure out whether we need to look at a particular field and set up
        // the features for the postings iterator.
        int[] fields = null;
        int field = -1;
        WeightingFunction wf = qc.getWeightingFunction();
        WeightingComponents wc = qc.getWeightingComponents();
        boolean noStats = false;
        if(wc.N == 0) {
            wc.N = dp.getNDocs();
            noStats = true;
        }
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures(wf, wc);
        if(fromField != null) {
            fields = mp.getManager().getMetaFile().getFieldArray(fromField);
            for(int i = 0; i < fields.length; i++) {
                if(fields[i] > 0) {
                    field = i;
                    break;
                }
            }
            feat.setFields(fields);
        }

        //
        // Get info for the field where we will put the classes.
        FieldInfo fi = dp.getManager().getMetaFile().getFieldInfo(classField);
        Set[] vm = new Set[dp.getMaxDocumentID() + 1];
        for(Map.Entry<String, List<String>> e : m.entrySet()) {
            float[] scores = new float[dp.getMaxDocumentID() + 1];
            Set<String> terms = new HashSet<String>();
            List<QueryEntry> qes = new ArrayList<QueryEntry>();
            for(String w : e.getValue()) {
                DictTerm dt = new DictTerm(w);
                dt.setQueryConfig(qc);
                dt.setDoMorph(true);
                dt.setPartition(dp);
                QueryEntry[] wqe = dt.getTerms();
                for(QueryEntry qe : wqe) {
                    if(!terms.contains(qe.getName())) {
                        terms.add(qe.getName().toString());
                        qes.add(qe);
                    }
                }
            }


            for(QueryEntry qe : qes) {
                if(noStats) {
                    wc.ft = qe.getN();
                } else {
                    wc.setTerm((String) qe.getName());
                }
                wf.initTerm(wc);
                PostingsIterator pi = qe.iterator(feat);
                if(pi != null) {
                    while(pi.next()) {
                        if(field > 0) {
                            float[] fw = ((FieldedPostingsIterator) pi).
                                    getFieldWeights();
                            scores[pi.getID()] += fw[field];
                        } else {
                            scores[pi.getID()] += pi.getWeight();
                        }
                    }
                }
            }

            for(int i = 1; i < scores.length; i++) {
                scores[i] /= dp.getDocumentLength(i);
            }

            float thresh = getThreshold(scores);
            for(int i = 1; i < scores.length; i++) {
                if(scores[i] > thresh) {
                    if(vm[i] == null) {
                        vm[i] = new HashSet<String>();
                    }
                    vm[i].add(e.getKey());
                }
            }
        }

        for(int i = 1; i < vm.length; i++) {
            if(vm[i] != null) {
                for(String v : (Set<String>) vm[i]) {
                    mp.savedData(fi, i, v);
                }
                modified = true;
            }
        }


        return modified;
    }

    /**
     * Gets a threshold for determining class membership.
     */
    private float getThreshold(float[] scores) {
        //        float sum = 0;
        //        float ss = 0;
        //        float n = 0;
        //        for(int i = 1; i < scores.length; i++) {
        //            if(scores[i] > 0) {
        //                n++;
        //                sum += scores[i];
        //            }
        //        }
        //        float mean = sum / n;
        //        for(int i = 1; i < scores.length; i++) {
        //            if(scores[i] > 0) {
        //                float diff = scores[i] - mean;
        //                ss = diff * diff;
        //            }
        //        }
        //        float sd = (float) java.lang.Math.sqrt(ss / (n -1));
        //        log.debug(logTag, 0, String.format("n: %f mean: %.3f sd: %.3f sum: ", n, mean, sd, mean+sd));
        //        return mean + sd;
        return 0.04f;
    }
}
