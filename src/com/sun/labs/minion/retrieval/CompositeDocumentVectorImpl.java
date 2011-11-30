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
package com.sun.labs.minion.retrieval;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.WeightedFeature;
import com.sun.labs.minion.WeightedField;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.MetaFile;
import com.sun.labs.minion.pipeline.StopWords;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * An implementation of document vector that provides for a composite document
 * vector, that is, a document vector made by taking a linear combination of 
 * more than one vectored field.
 * 
 * @see DocumentVectorImpl for an implementation that uses features from the
 * whole document or from just one vectored field.
 */
public class CompositeDocumentVectorImpl implements DocumentVector {

    /**
     * The search engine that generated this vector.
     */
    protected transient SearchEngine e;

    /**
     * The individual field vectors that are composed into this vector.
     */
    private DocumentVectorImpl[] vecs;

    /**
     * The name of the key, which will survive transport.
     */
    private String keyName;

    /**
     * A linear combination of the fields composing this vector.
     */
    protected WeightedField[] fields;

    private static Logger logger = Logger.getLogger(CompositeDocumentVectorImpl.class.
            getName());

    protected transient StopWords ignoreWords;

    protected CompositeDocumentVectorImpl() {
    }

    /**
     * Creates a document vector for a particular field from a search result.
     *
     * @param r The search result for which we want a document vector.
     * @param fields a linear combination of fields and weights that should be
     * used to build this document vector.  The field names provided in the array should
     * be the names of vectored fields.  If a provided field name does not name
     * a vectored field, a warning will be logged, but the operation will proceed.
     * 
     * <P>If this paramater contains a weighted field whose name is <code>null</code>,
     * that indicates that the data from the unnamed body field should be used with
     * the associated weight.
     * 
     * <P>It is probably a good idea if the weights associated with the fields
     * sum to 1, although it is not required.  If the weights do not sum to one, 
     * then you may get document similarities greater than 1 as the result of a 
     * <code>findSimilar<code> operation.
     */
    public CompositeDocumentVectorImpl(ResultImpl r, WeightedField[] fields) {
        this(r.set.getEngine(), r.ag.part.getDocumentDictionary().getByID(r.doc),
             fields);
    }

    /**
     * Creates a document vector for a given document.
     *
     * @param e The search engine with which the docuemnt is associated.
     * @param key The entry from the document dictionary for the given
     * document.
     * @param fields a linear combination of the vectored fields in this document
     * that we will use to build the document vector.
     * If this value is <code>null</code> a vector for the whole document will
     * be returned.  If one of the values in a non-<code>null</code> array has 
     * the name of the field set to <code>null</code>, then the vector will include
     * data from the unnamed body field. If one of the fields provided is not
     * a vectored field, then a warning will be issued, but processing will
     * proceed.
     */
    public CompositeDocumentVectorImpl(SearchEngine e,
                                       QueryEntry key,
                                       WeightedField[] fields) {

        this.e = e;
        keyName = key.getName().toString();
        vecs = new DocumentVectorImpl[fields.length];
        for(int i = 0; i < fields.length; i++) {
            vecs[i] = new DocumentVectorImpl(e, key, fields[i].getFieldName());
        }
    }

    public CompositeDocumentVectorImpl(SearchEngine e,
                                       WeightedFeature[] wf,
                                       WeightedField[] fields) {
        this.e = e;
        keyName = null;
        vecs = new DocumentVectorImpl[fields.length];
        for(int i = 0; i < fields.length; i++) {
            vecs[i] = new DocumentVectorImpl(e, wf);
        }
    }

    public DocumentVector copy() {
        CompositeDocumentVectorImpl ret = new CompositeDocumentVectorImpl();
        ret.e = e;
        ret.keyName = keyName;
        ret.ignoreWords = ignoreWords;
        ret.vecs = vecs.clone();
        return ret;
    }

    private int getFieldID(WeightedField field, MetaFile mf) {
        int fid;
        if(field.getFieldName() == null) {
            fid = 0;
        } else {
            FieldInfo fi = mf.getFieldInfo(field.getFieldName());
            if(!fi.hasAttribute(FieldInfo.Attribute.TOKENIZED)) {
                logger.warning("Non vectored field: " + fi.getName()
                        + " for composite document vector");
            }
            fid = fi.getID();
        }
        return fid;
    }

    public SearchEngine getEngine() {
        return e;
    }

    public void setEngine(SearchEngine e) {
        this.e = e;
    }

    /**
     * Calculates the dot product of this document vector with another.  Because
     * a composite document vector is composed of multiple fields with associated 
     * weights, the dot product of two vectors will take these fields and weights 
     * into account.
     * 
     * <p>When the other document vector contains a field that this one does not, 
     * (or vice versa (note: it's probably not a good idea to compute the dot 
     * product of such vectors!)), then there will be no contribution from that field.  When
     * the two document vectors have a field in common, the of the vectors will
     * be multiplied by any associated field weights before they are multiplied
     * together.
     *
     * @param dvi another document vector
     * @return the dot product of the two vectors (i.e. the sum of the
     * products of the components in each dimension)
     */
    public float dot(CompositeDocumentVectorImpl dvi) {
        float ret = 0;

        for(int i = 0; i < fields.length; i++) {
            WeightedField wf = fields[i];
            for(int j = 0; j < dvi.fields.length; j++) {
                if(fields[j].getFieldName().equals(wf.getFieldName())) {
                    ret += vecs[i].dot(dvi.vecs[j]) * fields[i].getWeight() * dvi.fields[i].
                            getWeight();
                }
            }
        }
        return ret;
    }

    public Set<String> getTerms() {
        Set<String> ret = new HashSet<String>();
        for(int i = 0; i < vecs.length; i++) {
            ret.addAll(vecs[i].getTerms());
        }
        return ret;
    }

    /**
     * Gets a map of term names to weights, where the weights
     * represent the amount the term contributed to the similarity
     * of the two documents.  Only terms that occur in both documents
     * are returned, as all other terms have weight zero.  The keys
     * in the Map are sorted according to the natural ordering
     * of their values.  That is, the first string returned from
     * an iterator over the key set will be the term with the
     * highest weight.
     *
     * @param dv the document vector to compare this one to
     * @return a sorted hash map of String names to Float weights
     */
    public Map<String, Float> getSimilarityTermMap(DocumentVector dv) {
        CompositeDocumentVectorImpl dvi = (CompositeDocumentVectorImpl) dv;
        Map<String, Float> m = new HashMap<String, Float>();
        for(int i = 0; i < vecs.length; i++) {
            WeightedField wf = fields[i];
            for(int j = 0; j < dvi.fields.length; j++) {
                if(fields[j].getFieldName().equals(wf.getFieldName())) {
                    SortedSet<WeightedFeature> cst = vecs[i].getSimilarityTerms(
                            dvi.vecs[j]);
                    for(WeightedFeature cf : cst) {
                        Float curr = m.get(cf.getName());
                        if(curr == null) {
                            curr = 1f;
                        }
                        m.put(cf.getName(), curr * cf.getWeight() * fields[j].
                                getWeight() * fields[i].getWeight());
                    }
                }
            }
        }
        return m;
    }

    /**
     * Gets a sorted (by weight) set of the terms contributing to
     * document similarity with the provided document.  The set consists
     * of WeightedFeatures that represent the terms that the documents
     * have in common and their combined weights.
     *
     * @param dvi the document to compare this one to
     * @return a sorted set of WeightedFeature that occurred in both documents
     */
    public SortedSet<WeightedFeature> getSimilarityTerms(
            CompositeDocumentVectorImpl dvi) {

        SortedSet ret = new TreeSet();
        Map<String, Float> m = getSimilarityTermMap(dvi);
        for(Map.Entry<String, Float> e : m.entrySet()) {
            ret.add(new WeightedFeature(e.getKey(), e.getValue()));
        }
        return ret;
    }

    /**
     * Normalizes the length of this vector to 1.  Since this is a composite
     * vector, we normalize each of the composites.  Not sure this actually 
     * makes much sense.
     */
    public void normalize() {

        for(int i = 0; i < vecs.length; i++) {
            vecs[i].normalize();
        }
    }

    private Map<String, Float> combineAll() {
        Map<String, Float> ret = new HashMap<String, Float>();
        for(int i = 0; i < vecs.length; i++) {
            for(WeightedFeature wf : vecs[i].getFeatures()) {
                Float f = ret.get(wf.getName());
                if(f == null) {
                    f = 0f;
                }
                f += (wf.getWeight() * fields[i].getWeight());
                ret.put(wf.getName(), f);
            }
        }
        return ret;
    }

    /**
     * Gets a sorted set of features.
     *
     * @return a set of the features in this vector, sorted by name.  If this
     * document vector is composed of multiple fields with associated weights, 
     * these features will take these weights into account.
     */
    public SortedSet<WeightedFeature> getSet() {
        SortedSet ret = new TreeSet();
        for(Map.Entry<String, Float> e : combineAll().entrySet()) {
            ret.add(new WeightedFeature(e.getKey(), e.getValue()));
        }
        return ret;
    }

    public SortedSet<WeightedFeature> getWeightOrderedSet() {
        SortedSet ret =
                new TreeSet(WeightedFeature.getInverseWeightComparator());
        for(Map.Entry<String, Float> e : combineAll().entrySet()) {
            ret.add(new WeightedFeature(e.getKey(), e.getValue()));
        }
        return ret;
    }

    /**
     * Computes the similarity between this document vector and the
     * supplied vector.  The larger the value, the greater the similarity.
     * The measurement returned is the cosine of the angle between the
     * vectors.
     *
     * @param otherVector the vector representing the document to compare
     * this vector to
     * @return the cosine of the angle between the two vectors
     */
    public float getSimilarity(DocumentVector otherVector) {
        return getSimilarity((CompositeDocumentVectorImpl) otherVector);
    }

    public float getSimilarity(CompositeDocumentVectorImpl otherVector) {
        return dot(otherVector);
    }

    /**
     * Finds similar documents to this one.  An OR is run with all the terms
     * in the documents.  The resulting docs are returned ordered from
     * most similar to least similar.
     *
     * @return documents similar to the one this vector represents
     */
    public ResultSet findSimilar() {
        return findSimilar("-score");
    }

    public ResultSet findSimilar(String sortOrder) {
        return findSimilar(sortOrder, 1.0);
    }

    /**
     * Finds documents that are similar to this one.  An OR is run with all the terms
     * in the documents.  The resulting docs are returned ordered from
     * most similar to least similar.
     *
     * @param sortOrder a string describing the order in which to sort the results
     * @param skimPercent a number between 0 and 1 representing what percent of the features should be used to perform findSimilar
     * @return documents similar to the one this vector represents
     */
    public ResultSet findSimilar(String sortOrder, double skimPercent) {
        return null;
    }

    // Doc inherited from interface.  Gets a HashMap of the top N terms, or
    // docLength terms if docLength < N.  The map is from String term names
    // to Float weights.
    public Map<String, Float> getTopWeightedTerms(int nTerms) {

        Map<String, Float> m = combineAll();

        //
        // If we're getting everything, then just return everything.
        if(nTerms >= m.size()) {
            return m;
        } else {

            //
            // Figure out the top n.
            Set<WeightedFeature> s =
                    new TreeSet(WeightedFeature.getInverseWeightComparator());
            for(Map.Entry<String, Float> e : m.entrySet()) {
                s.add(new WeightedFeature(e.getKey(), e.getValue()));
            }
            Iterator<WeightedFeature> i = s.iterator();
            for(int j = 0; j < nTerms && i.hasNext(); j++) {
                WeightedFeature wf = i.next();
                m.put(wf.getName(), wf.getWeight());
            }
        }

        return m;
    }

    public String getKey() {
        return keyName;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < fields.length; i++) {
            sb.append(String.format("%s: %s \n", fields[i].getFieldName(), vecs[i].
                    toString()));
        }
        return sb.toString();
    }
}
