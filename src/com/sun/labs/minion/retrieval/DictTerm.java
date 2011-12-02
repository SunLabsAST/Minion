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

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.Stemmer;
import com.sun.labs.minion.indexer.DiskField;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.postings.PostingsIteratorWithPositions;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.util.Util;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * A concrete subclass of <code>QueryTerm</code> that represents a term
 * taken from one of the main dictionaries for a partition.  Note that a
 * term can be represented by several actual dictionary entries.
 *
 * <p>
 *
 * This class implements <code>Comparator</code> so that we can sort the
 * terms based on their frequency.
 */
public class DictTerm extends QueryTerm implements Comparator {

    /**
     * The set of morphological variations of the term as computed by the
     * lightweight morphology. We only need to compute this once per query
     * term, when the term is instantiated.
     */
    protected String[] knowledgeVariants;

    /**
     * The array of semantic variantions of the term as returned from the
     * taxonomies of all the partitions. We only need to compute this once per query term.
     */
    protected String[] semanticVariants;

    /**
     * The list of entries that were pulled from the dictionary for the
     * current partition.  A single query term may be expanded in various
     * ways to cover several entries in a dictionary.
     */
    protected QueryEntry[] dictEntries;

    protected PostingsIteratorFeatures feat;

    /**
     * A set of postings iterators for the terms that can be used to get
     * position info for proximity queries or for highlighting.
     */
    protected PostingsIteratorWithPositions[] pis;

    /**
     * An array that will hold per-field word positions.  This will be
     * given out when anyone asks for positions.
     */
    protected int[][] posns;
    
    /**
     * A weight associated with the query term, which can be used during the
     * similarity computation.  If this is not specified by the user, then we'll
     * compute a weight by assuming that the query term has a freqeuncy of 1.
     */
    private float termWeight;

    /**
     * Creates a dictionary term for a given query term.  This query term
     * may be expanded in various ways depending on the operators that have
     * been applied to the term.
     *
     * @param val The value from the query.
     */
    public DictTerm(String val) {
        this.val = val;
    } // DictTerm constructor

    public float getTermWeight() {
        return termWeight;
    }

    public void setTermWeight(float termWeight) {
        this.termWeight = termWeight;
    }

    /**
     * Sets the partition that this term will be operating on.  Does any
     * dictionary lookups required.
     *
     * @param part The partition that we will be evaluating against.
     */
    @Override
    public void setPartition(DiskPartition part) {

        super.setPartition(part);

        //
        // If this isn't a term in an inverted file then we stop
        // here
        if(!(part instanceof InvFileDiskPartition)) {
            return;
        }

        InvFileDiskPartition ifdp = (InvFileDiskPartition) part;

        //
        // Set up the features for our postings iterators once.
        if(feat == null) {
            feat = new PostingsIteratorFeatures();
            feat.setPositions(loadPositions);
            feat.setQueryStats(qs);
            feat.setWeightingFunction(wf);
            feat.setWeightingComponents(wc);
            feat.setQueryStats(qs);
        }

        //
        // A set to hold term variants from morphology or other sources.
        Set<QueryEntry> variants = new HashSet<QueryEntry>();
        
        if(searchFields == null) {
            searchFields = part.getPartitionManager().getEngine().getQueryConfig().getDefaultFields().toArray(new FieldInfo[0]);
        }
        
        for(FieldInfo sfi : searchFields) {
            DiskField df = ifdp.getDF(sfi);
            if(df == null) {
                continue;
            }

            //
            // We'll get morphological variants once.
            if(doMorph && knowledgeVariants == null && qc.getKnowledgeSource()
                    != null) {

                //
                // If we're supposed to match case, we'll use the case given in
                // the query term.
                Set<String> ksv = qc.getKnowledgeSource().variantsOf(val);
                knowledgeVariants = new String[ksv.size()];
                int i = 0;
                for(Iterator<String> iter = ksv.iterator(); iter.hasNext();) {
                    knowledgeVariants[i++] = iter.next();
                }
            }

            //
            // We'll start with the term itself, unless we're doing a wildcard.
            if(!doWild) {
                if(val != null) {
                    QueryEntry e = df.getTerm(val, matchCase);
                    if(e != null) {
                        variants.add(e);
                    }
                }
            }

            //
            // Knowledge Variants are up next.
            if(doMorph) {
                for(int i = 0; i < knowledgeVariants.length; i++) {
                    QueryEntry me = df.getTerm(knowledgeVariants[i], matchCase);
                    if(me != null) {
                        variants.add(me);
                    }
                }
            }

            //
            // Stemological variants.
            if(doStem) {
                Stemmer s = df.getStemmer();
                if(s != null) {
                    String stem = s.stem(val);
                    QueryEntry e = df.getStem(stem);
                    if(e != null) {
                        variants.add(e);
                    }
                }
            }

            //
            // Find wildcards.
            if(doWild) {
                variants.addAll(df.getMatching(val,
                                               matchCase,
                                               qc.getMaxDictTerms(),
                                               qc.getMaxDictLookupTime()));
            }
        }

        //
        // Make the final array.
        dictEntries = new QueryEntry[variants.size()];
        variants.toArray(dictEntries);
        Util.sort(dictEntries, (Comparator) this);

        //
        // Calculate the estimated size of the return set, which is simply
        // the sum of the doc frequencies.
        estSize = 0;
        for(int i = 0; i < dictEntries.length; i++) {
            estSize += dictEntries[i].getN();
        }

        pis = null;

        //
        // Do the set partition for this level.
        super.setPartition(part);
    }

    /**
     * Returns the already calculated estimated size.
     */
    @Override
    protected int calculateEstimatedSize() {
        return estSize;
    }

    /**
     * Evaluates the term in the current partition.
     *
     * @param ag An array group that we can use to limit the evaluation of
     * the term.  If this group is <code>null</code> a new group will be
     * returned.  If this group is non-<code>null</code>, then the elements
     * in the group will be used to restrict the documents that we return
     * from the term.
     * @return A new <code>ArrayGroup</code> containing the results of
     * evaluating the term against the given group.  The static type of the
     * returned group depends on the query status parameter.
     */
    @Override
    public ArrayGroup eval(ArrayGroup ag) {

        if(ag == null) {

            QuickOr or = strictEval
                    ? new QuickOr(part, estSize)
                    : new ScoredQuickOr(part, estSize);
            or.setQueryStats(qs);
            or.addFields(searchFields);
            for(QueryEntry qe : dictEntries) {
                wc.setField(qe.getField());
                wc.setTerm((String) qe.getName());
                float qw;
                if(termWeight == 0) {
                    qw = wf.initTerm(wc);
                } else {
                    qw = termWeight;
                }
                or.add(qe.iterator(feat), qw);
            }
            return or.getGroup();
        }

        //
        // We're going to intersect the documents in the term with the
        // documents in the set we were given, returning a new set.  We
        // need to do things differently depending on the type of the
        // group.
        if(ag instanceof ScoredGroup) {
            return intersect((ScoredGroup) ag);
        } else if(ag instanceof NegativeGroup) {
            return intersect((NegativeGroup) ag);
        } else {
            return intersect(ag);
        }
    }

    /**
     * Intersects a strict group with the current term.  This will try to
     * do the optimal thing.
     *
     * @param ag The group to intersect with.
     * @return a group representing the intersection of this term with the given
     * group
     */
    protected ArrayGroup intersect(ArrayGroup ag) {

        //
        // Quick check for emptyness.
        if(ag.size == 0) {
            return ag;
        }

        //
        // Who got touched?
        boolean[] used = new boolean[ag.size];

        //
        // If we're not strictly evaluating, we need to keep track of our
        // scores.
        float[] scores = null;
        if(!strictEval) {
            scores = new float[ag.size];
        }

        //
        // Loop through our terms.
        float sqw = 0;
        qs.intersectW.start();
        for(int i = 0; i < dictEntries.length; i++) {

            //
            // Keep track of the query weights.
            wc.setTerm((String) dictEntries[i].getName());
            float qw = wf.initTerm(wc);
            sqw += qw * qw;
            PostingsIterator pi = dictEntries[i].iterator(feat);
            if(pi == null) {
                continue;
            }
            pi.next();

            //
            // We need to decide whether we're going to use findID or just
            // iterate through the postings for the current term.
            if(dictEntries[i].getN() < 10 * ag.size) {

                //
                // We're going to iterate, since that will be less work.
                //
                // Loop through the documents in the set, trying to find
                // them in the ierator.
                qs.piW.start();
                for(int j = 0; j < ag.size; j++) {

                    //
                    // We can skip documents we've already encountered.
                    if(used[j]) {
                        continue;
                    }
                    while(pi.getID() < ag.docs[j] && pi.next());
                    used[j] = pi.getID() == ag.docs[j];
                    if(!strictEval && used[j]) {
                        scores[j] += pi.getWeight() * qw;
                    }
                }
                qs.piW.stop();
            } else {

                //
                // We're going to use findID.
                for(int j = 0; j < ag.size; j++) {
                    if(used[j]) {
                        continue;
                    }
                    used[j] = pi.findID(ag.docs[j]);
                    if(!strictEval && used[j]) {
                        scores[j] += pi.getWeight() * qw;
                    }
                }
            }
        }
        qs.intersectW.stop();

        //
        // Make a new set with only the docs that got used.  This may be a
        // scored array or a strict array.
        if(strictEval) {
            ArrayGroup ret = new ArrayGroup(ag.size);
            for(int i = 0; i < used.length; i++) {
                if(used[i]) {
                    ret.docs[ret.size++] = ag.docs[i];
                }
            }
            return ret;
        } else {
            ScoredGroup ret = new ScoredGroup(ag.size);
            for(int i = 0; i < used.length; i++) {
                if(used[i]) {
                    ret.docs[ret.size] = ag.docs[i];
                    ret.scores[ret.size++] = scores[i];
                }
            }

            //
            // Set up the query weights in the set we're returning.
            ret.sqw = sqw;
            return ret;
        }
    }

    /**
     * Intersects a scored group with the current term.  This will try to
     * do the optimal thing.
     *
     * @param ag The group to intersect with.
     */
    protected ArrayGroup intersect(ScoredGroup ag) {

        //
        // Quick check for emptyness.
        if(ag.size == 0) {
            return ag;
        }

        //
        // Who got touched?
        boolean[] used = new boolean[ag.size];
        float[] scores = new float[ag.size];

        //
        // Features for the iterators we'll create.
        PostingsIteratorFeatures feat =
                new PostingsIteratorFeatures(wf, wc, searchFields,
                fieldMultipliers,
                loadPositions,
                matchCase);
        feat.setQueryStats(qs);

        //
        // Loop through our terms.
        float sqw = 0;
        qs.intersectW.start();
        for(int i = 0; i < dictEntries.length; i++) {
            wc.setTerm((String) dictEntries[i].getName());
            float qw = wf.initTerm(wc);
            sqw += qw * qw;

            PostingsIterator pi = dictEntries[i].iterator(feat);
            if(pi == null) {
                continue;
            }
            pi.next();

            //
            // We need to decide whether we're going to use findID or just
            // iterate through the postings for the current term.  We'll
            // iterate through the postings if it's less work than running
            // findID too many times.
            if(dictEntries[i].getN() < 10 * ag.size) {

                qs.piW.start();
                //
                // Loop through the documents in the set, trying to find
                // them in the ierator.
                for(int j = 0; j < ag.size; j++) {
                    while(pi.getID() < ag.docs[j] && pi.next());
                    if(pi.getID() == ag.docs[j]) {
                        used[j] = true;
                        scores[j] += pi.getWeight() * qw;
                    }
                }
                qs.piW.stop();
            } else {

                //
                // We're going to use findID.
                for(int j = 0; j < ag.size; j++) {
                    if(pi.findID(ag.docs[j])) {
                        used[j] = true;
                        scores[j] += pi.getWeight() * qw;
                    }
                }
            }
        }
        qs.intersectW.stop();

        //
        // Make a new set with only the docs that got used.
        ScoredGroup ret = new ScoredGroup(ag.size);
        for(int i = 0; i < scores.length; i++) {
            if(used[i]) {
                ret.docs[ret.size] = ag.docs[i];

                //
                // If this was a strict evaluation, we'll simply use the
                // scores from the scored group.
                if(strictEval) {
                    ret.scores[ret.size++] = ag.scores[i];
                } else {
                    ret.scores[ret.size++] = scores[i] + ag.scores[i];
                }
            }
            ret.sqw = sqw + ag.sqw;
        }
        return ret;
    }

    /**
     * Intersects a scored group with the current term.  This will try to
     * do the optimal thing.
     *
     * @param ag The group to intersect with.
     */
    protected ArrayGroup intersect(NegativeGroup ag) {

        //
        // Quick hack.  Union the terms in strict mode and then intersect.
        boolean saveStrict = strictEval;
        strictEval = true;
        ArrayGroup sg = eval(null);
        strictEval = saveStrict;
        return ag.intersect(sg);
    }

    /**
     * Gets the positions for this term in the given document.
     *
     * @return a two dimensional array of integers.  The first dimension
     * is indexed by field ID.  The second dimension is the positions for
     * the given field.  The first element of each sub-array will contain
     * the number of positions following.  We will only return positions
     * for those fields in which we have a stated interest.  If there are
     * no such fields defined, then data from all fields will be returned.
     */
    public int[][] getPositions(int d) {

        //
        // If this isn't a term in an inverted file, return now
        if(!(part instanceof InvFileDiskPartition)) {
            return new int[0][0];
        }

        //
        // If we don't have iterators, then generate them now.
        if(pis == null) {
            //
            // Features for the iterators we'll create.
            PostingsIteratorFeatures feat =
                    new PostingsIteratorFeatures(wf, wc, searchFields,
                    fieldMultipliers,
                    loadPositions,
                    matchCase);
            feat.setQueryStats(qs);

            pis = new PostingsIteratorWithPositions[dictEntries.length];
            for(int i = 0; i < dictEntries.length; i++) {
                pis[i] = (PostingsIteratorWithPositions) dictEntries[i].iterator(feat);
            }

            posns = new int[searchFields.length][];
            for(int i = 0; i < posns.length; i++) {
                posns[i] = new int[4];
            }
        }

        //
        // Initialize the per-field sizes to 0.
        for(int i = 0; i < posns.length; i++) {
            posns[i][0] = 0;
        }

        //
        // Get the positions for each term, building them up as we go.
        for(int i = 0; i < dictEntries.length; i++) {
            if(pis[i] == null) {
                continue;
            }

            //
            // Get the positions for this document.
            if(pis[i].findID(d)) {
                int[] fpos = pis[i].getPositions();
                int n = fpos[0];
                if(n == 0) {
                    continue;
                }

                if(posns[0][0] + 1 + n >= posns[0].length) {
                    posns[0] = Arrays.copyOf(posns[0],
                            (posns[0].length + n + 1) * 2);
                }
                System.arraycopy(fpos, 1,
                        posns[0], posns[0][0] + 1,
                        n);
                posns[0][0] += n;
            }
        }

        return posns;
    }

    // Implementation of java.util.Comparator
    /**
     * Compares this comparator with another.
     *
     * @param object an <code>Object</code> to compare against.
     * @return <code>true</code> if this comparator is the same class as
     * the other, <code>false</code> otherwise.
     */
    public boolean equals(Object object) {
        return object.getClass() == this.getClass();
    }

    /**
     * Compares two terms by their respective term frequencies.
     *
     * @param o1 a term.
     * @param o2 another term.
     * @return Less than 0, greater than 0, or 0 if o1's term frequency is,
     * respectively, lower than, higher than, or equal to o2's term
     * frequency.
     */
    public int compare(Object o1, Object o2) {
        return ((QueryEntry) o1).getN() - ((QueryEntry) o2).getN();
    }

    public List getQueryTerms(java.util.Comparator c) {
        List l = new ArrayList();
        l.add(this);
        return l;
    }

    public QueryEntry[] getTerms() {
        return dictEntries;
    }

    public String getName() {
        return val;
    }

    public String toString() {
        return toString("");
    }

    public String toString(String prefix) {
        return super.toString(prefix) + " " + val + " estSize: " + estSize
                + " order: " + order;
    }
} // DictTerm

