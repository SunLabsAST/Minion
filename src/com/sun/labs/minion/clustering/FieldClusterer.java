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
package com.sun.labs.minion.clustering;

import com.sun.labs.minion.ResultsCluster;
import com.sun.labs.minion.SearchEngineException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.sun.labs.minion.classification.FeatureClusterer;
import com.sun.labs.minion.classification.FeatureSelector;
import com.sun.labs.minion.retrieval.ResultImpl;
import com.sun.labs.minion.util.CharUtils;
import java.util.logging.Logger;

/**
 * A clusterer that produces clusters based on the values of a given field.
 * This clusterer will place a single document into more than one cluster if that
 * document has more than one value for the provided field.
 *
 * <p>
 *
 * The user can also specify a numeric field whose value specifies a ranking for
 * the documents in the cluster.
 * @author Stephen Green <stephen.green@sun.com>
 */
public class FieldClusterer extends AbstractClusterer {

    String field;

    String scoreField;

    boolean ignoreCase;

    Iterator hits;

    static Logger logger = Logger.getLogger(FieldClusterer.class.getName());

    protected static String logTag = "FC";

    /**
     * Creates a FieldClusterer
     */
    public FieldClusterer() {
        super();
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setScoreField(String scoreField) {
        this.scoreField = scoreField;
    }

    @Override
    public void setData(String data) {
        setField(data);
        setScoreField(data + "-score");
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    /**
     * Ignores any feature selector information that we're given, since it
     * won't affect our field-based clustering.
     */
    @Override
    public void setFeatureInfo(FeatureClusterer fc, FeatureSelector fs,
            int nFeat) {
        this.fc = null;
        this.fs = null;
        this.nFeat = 0;
    }

    /**
     * Gets the cluster elements for our clusterer.  This is a do-nothing for
     * us, since we'll compute this during clustering to account for multiple field values.
     */
    @Override
    protected void getElements() throws SearchEngineException {
        hits = rs.getAllResults(false).iterator();
    }

    /**
     * Clusters the elements with the same field value.
     */
    @Override
    public void cluster() {
        Map<String, Integer> cl = new HashMap<String, Integer>();
        k = 0;
        List<String> origNames = new ArrayList<String>();
        while(hits.hasNext()) {
            ResultImpl r = (ResultImpl) hits.next();

            List l = r.getField(field);

            //
            // No value for this field, so into the empty cluster it goes!
            if(l.size() == 0) {
                empty.add(new ClusterElement(r, one));
            }

            //
            // See if we need to get an iterator for the score field.
            Iterator s = null;
            if(scoreField != null) {
                List sl = r.getField(scoreField);
                if(sl.size() > 0) {
                    s = sl.iterator();
                }
            }

            //
            // Create cluster elements for each value of the field.
            for(Iterator j = l.iterator(); j.hasNext();) {
                String f = j.next().toString();
                String origF = f;
                if(ignoreCase) {
                    f = CharUtils.toLowerCase(f);
                }
                Integer p = cl.get(f);
                if(p == null) {
                    origNames.add(origF);
                    p = k++;
                }
                cl.put(f, p);

                //
                // We need to clone the result because we need to set per-value
                // scores.
                ResultImpl cr = (ResultImpl) r.clone();
                if(s != null) {
                    //
                    // Change the similarity to a distance here.
                    cr.setScore(1 - ((Double) s.next()).floatValue());
                }
                ClusterElement ce = new ClusterElement(cr, one);
                ce.member = p;
                te.add(ce);
            }
        }

        els = te.toArray(new ClusterElement[0]);
        clusters = new double[k][];
        for(int i = 0; i < clusters.length; i++) {
            clusters[i] = one;
        }
        names = origNames.toArray(new String[0]);
    }

    @Override
    public Set<ResultsCluster> getClusters() {
        ResultsClusterImpl[] res = new ResultsClusterImpl[k];
        for(int i = 0; i < res.length; i++) {
            res[i] = new ResultsClusterImpl(features, clusters[i],
                    rs.getEngine());
            if(names != null) {
                res[i].setName(names[i]);
            }
        }
        for(int i = 0; i < els.length; i++) {
            res[els[i].member].add(els[i].r, scoreField);
        }

        //
        // Sort the results by the size of the set.
        com.sun.labs.minion.util.Util.sort(res);
        Set<ResultsCluster> ret = new LinkedHashSet<ResultsCluster>();
        for(int i = 0; i < res.length; i++) {

            //
            // Skip clusters of size 0.
            if(res[i].res.size() > 0) {
                ret.add(res[i]);
            }
        }

        //
        // Add in any things that were missed out on to a final cluster.
        if(ignored.size() > 0) {
            ResultsClusterImpl ic = new ResultsClusterImpl(features, one, rs.
                    getEngine());
            for(ClusterElement e : ignored) {
                ic.add(e.r, scoreField);
            }
            ic.setName("Remainder");
            ret.add(ic);
        }

        if(empty.size() > 0) {
            ResultsClusterImpl ec = new ResultsClusterImpl(features, one, rs.
                    getEngine());
            for(ClusterElement e : empty) {
                ec.add(e.r, scoreField);
            }
            ec.setName("Empty");
            ret.add(ec);
        }
        return ret;
    }
}
