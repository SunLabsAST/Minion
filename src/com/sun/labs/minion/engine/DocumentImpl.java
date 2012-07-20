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
package com.sun.labs.minion.engine;

import com.sun.labs.minion.*;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Logger;

/**
 * An implementation of the Document interface for a search engine.
 *
 * @see com.sun.labs.minion.Document
 */
public class DocumentImpl implements Document {

    /**
     * The search engine that generated this document.
     */
    private SearchEngineImpl e;

    /**
     * The partition that this document was drawn from.
     */
    private InvFileDiskPartition p;

    /**
     * The ID for this document.
     */
    private int id;

    /**
     * The entry from the document dictionary for this document.
     */
    private QueryEntry dke;

    /**
     * The key associated with this document.
     */
    private String key;

    /**
     * A map from saved fields to values.
     */
    private Map<String, List> savedFields;

    /**
     * A map from vectored fields to lists of postings.
     */
    private Map<String, List<Posting>> vectoredFields;

    static final Logger logger = Logger.getLogger(DocumentImpl.class.getName());

    /**
     * Creates an empty document.
     */
    public DocumentImpl(SearchEngineImpl e, String key) {
        this.e = e;
        this.key = key;
    }

    /**
     * Creates a document backed by a document in the index.
     */
    public DocumentImpl(SearchEngineImpl e, InvFileDiskPartition p, int id) {
        this.e = e;
        this.p = p;
        this.id = id;
        dke = p.getDocumentDictionary().getByID(id);
        key = dke.getName().toString();
    }

    /**
     * Creates a document backed by a document in the index.
     */
    public DocumentImpl(QueryEntry dke) {
        this.dke = dke;
        id = dke.getID();
        key = dke.getName().toString();
        p = (InvFileDiskPartition) dke.getPartition();
        e = (SearchEngineImpl) p.getPartitionManager().getEngine();
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    public QueryEntry getEntry() {
        return dke;
    }

    private void initSF() {
        if(savedFields != null) {
            return;
        }
        if(p == null) {
            savedFields = new LinkedHashMap<String, List>();
        } else if(savedFields == null) {
            savedFields = p.getSavedValues(id);
        }
    }

    private void checkSaved(String field) {
        FieldInfo fi = e.invFilePartitionManager.getFieldInfo(field);
        if(fi == null || !fi.hasAttribute(FieldInfo.Attribute.SAVED)) {
            throw new IllegalArgumentException(field +
                    " must name a saved field.");
        }
    }

    @Override
    public List<Object> getSavedField(String field) {
        checkSaved(field);
        initSF();
        List l = savedFields.get(field);
        if(l == null) {
            return new ArrayList();
        }
        return new ArrayList(l);
    }

    @Override
    public Iterator<Map.Entry<String, List>> getSavedFields() {
        initSF();
        Map<String, List> x = new LinkedHashMap<String, List>();
        for(Map.Entry<String, List> e : savedFields.entrySet()) {
            x.put(e.getKey(), new ArrayList(e.getValue()));
        }
        return x.entrySet().iterator();
    }

    @Override
    public void setSavedField(String field, List values) {
        checkSaved(field);
        initSF();
        savedFields.put(field, values);
    }

    private void checkVectored(String field) {
        FieldInfo fi = e.invFilePartitionManager.getFieldInfo(field);
        if(fi == null || !fi.hasAttribute(FieldInfo.Attribute.VECTORED)) {
            throw new IllegalArgumentException(field +
                    " must name a vectored field.");
        }
    }

    private void initVF() {
        if(vectoredFields != null) {
            return;
        }
        vectoredFields = new LinkedHashMap<String, List<Posting>>();
        if(p == null) {
            return;
        }
        Collection<FieldInfo> vfs =
                p.getPartitionManager().getMetaFile().getFieldInfo(FieldInfo.Attribute.VECTORED);
        List<Posting> lp = getPostings(dke, 0);
        if(lp != null) {
            vectoredFields.put(null, lp);
        }
        for(FieldInfo fi : vfs) {
            lp = getPostings(dke, fi.getID());
            if(lp != null) {
                vectoredFields.put(fi.getName(), lp);
            }
        }
    }

    private List<Posting> getPostings(QueryEntry e, int fieldID) {
        List<Posting> ret = new ArrayList<Posting>();
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures(null, null);
        int[] f = new int[fieldID + 1];
        f[fieldID] = 1;
        PostingsIterator pi = e.iterator(feat);
        if(pi == null) {
            //
            // No postings for this field.
            return null;
        }

        while(pi.next()) {
            ret.add(new Posting(p.getDF(fieldID).getTerm(pi.getID(), false).getName().toString(),
                    pi.getFreq()));
        }
        return ret;
    }

    @Override
    public List<Posting> getPostings(String field) {
        checkVectored(field);
        initVF();
        List ret = new ArrayList<Posting>();
        List<Posting> l = vectoredFields.get(field);
        if(l != null) {
            ret.addAll(l);
        }
        return ret;
    }

    @Override
    public Iterator<Map.Entry<String, List<Posting>>> getPostings() {
        initVF();
        Map<String, List<Posting>> ret =
                new LinkedHashMap<String, List<Posting>>();
        for(Map.Entry<String, List<Posting>> e : vectoredFields.entrySet()) {
            ret.put(e.getKey(), new ArrayList<Posting>(e.getValue()));
        }
        return ret.entrySet().iterator();
    }

    @Override
    public void setPostings(String field, List<Posting> postings) {
        checkVectored(field);
        initVF();
        vectoredFields.put(field, postings);
    }

    public void index(SimpleIndexer si) throws SearchEngineException {

        initSF();
        initVF();
        si.startDocument(key);

        //
        // Process the saved fields.
        for(Map.Entry<String, List> ent : savedFields.entrySet()) {
            for(Object o : ent.getValue()) {
                si.addField(ent.getKey(), new Object[] {o});
            }
        }

        //
        // Process the indexed and vectored fields.
        for(Map.Entry<String, List<Posting>> ent : vectoredFields.entrySet()) {

            //
            // We're going to skip fields that are saved if they have
            // been handled above.
            if(ent.getKey() != null) {
                FieldInfo fi = e.invFilePartitionManager.getFieldInfo(
                        ent.getKey());
                if(fi.hasAttribute(FieldInfo.Attribute.SAVED) && savedFields.get(ent.getKey()) != null) {
                    continue;
                }
            }

            for(Posting po : ent.getValue()) {
                si.addTerm(ent.getKey(), po.getTerm(), po.getFreq());
            }
        }

        si.endDocument();
    }

    /**
     * Exports this document in XML to the given writer.
     */
    public void export(PrintWriter o) {

        o.format("<document>\n");
        o.format(" <key><![CDATA[%s]]></key>\n", key);
        initSF();
        initVF();
        for(Map.Entry<String, List> e : savedFields.entrySet()) {
            for(Object v : e.getValue()) {
                if(v instanceof String) {
                    o.format(" <string name=\"%s\"><![CDATA[%s]]></string>\n",
                            e.getKey(), v);
                } else if(v instanceof Long) {
                    o.format(" <long name=\"%s\">%d</long>\n", e.getKey(),
                            (Long) v);
                } else if(v instanceof Date) {
                    o.format(" <date name=\"%s\">%d</date>\n", e.getKey(),
                            ((Date) v).getTime());
                } else if(v instanceof Double) {
                    o.format(" <double name=\"%s\">%d</date>\n", e.getKey(),
                            Double.doubleToRawLongBits((Double) v));
                }
            }
        }

        //
        // Process the indexed and vectored fields.
        for(Map.Entry<String, List<Posting>> e : vectoredFields.entrySet()) {

            //
            // We're going to skip fields that are saved, as they will have
            // been handled above.
            if(e.getKey() != null) {
                FieldInfo fi = p.getPartitionManager().getFieldInfo(e.getKey());
                if(fi.hasAttribute(FieldInfo.Attribute.SAVED)) {
                    continue;
                }
            }

            if(e.getKey() == null) {
                o.format(" <vector>\n");
            } else {
                o.format(" <vector name=\"%s\">\n", e.getKey());
            }
            for(Posting p : e.getValue()) {
                o.format("  <p t=\"%s\" f=\"%d\"/>\n", p.getTerm(), p.getFreq());
            }
            o.format("</vector>\n");
        }
        o.format("</document>\n");
    }

    @Override
    public String toString() {
        return dke.getName() + " " + dke.getPartition() + " " + dke.getID();
    }
}
