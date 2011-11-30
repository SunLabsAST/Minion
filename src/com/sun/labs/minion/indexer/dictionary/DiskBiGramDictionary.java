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

package com.sun.labs.minion.indexer.dictionary;

import java.io.RandomAccessFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;

import com.sun.labs.minion.retrieval.ArrayGroup;
import com.sun.labs.minion.retrieval.ScoredGroup;
import com.sun.labs.minion.retrieval.ScoredQuickOr;

import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.entry.EntrySizeComparator;
import com.sun.labs.minion.indexer.entry.QueryEntry;

import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.Partition;

import com.sun.labs.minion.indexer.postings.Postings.Type;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.postings.PostingsIterator;

import com.sun.labs.minion.util.CharUtils;

public class DiskBiGramDictionary extends DiskDictionary {

    private DiskDictionary mainDict;

    public DiskBiGramDictionary(RandomAccessFile dictFile,
                                 RandomAccessFile postFile,
                                 PostingsInputType postInType,
                                 BufferType fileBufferType,
                                 int cacheSize,
                                 int nameBufferSize,
                                 int offsetsBufferSize,
                                 int infoBufferSize,
                                 int infoOffsetsBufferSize,
                                 Partition part,
                                 DiskDictionary mainDict)
            throws java.io.IOException {
        super(new EntryFactory(Type.ID_FREQ),
              new StringNameHandler(),
              dictFile,
              new RandomAccessFile[]{postFile},
              postInType,
              fileBufferType,
              cacheSize,
              nameBufferSize,
              offsetsBufferSize,
              infoBufferSize,
              infoOffsetsBufferSize,
              part);
        this.mainDict = mainDict;
    } // DiskBiGramDictionary constructor

    /**
     * Gets the IDs for terms that <em>potentially</em> match the given
     * wildcard expression.  The terms will need to be checked to see
     * whether or not they actually match.
     *
     * @param wc A wildcard expression.
     * @return An array of the IDs that <em>might</em> match the given
     * wildcard pattern.  The names of the entries that map to these IDs
     * will need to be tested to make sure that they actually match.  IF
     * this value is <code>null</code> then there are no possible matching
     * IDs.  If this value is an array of length 0, then all IDs must be
     * tested.
     */
    public int[] getMatching(String wc) {
        return getMatching(wc, true, true);
    }

    /**
     * Gets the IDs for terms that <em>potentially</em> match the given
     * wildcard expression.  The terms will need to be checked to see
     * whether or not they actually match.
     *
     * @param wc A wildcard expression.
     * @param starts If <code>true</code>, then the given expression must
     * start the term.
     * @param ends If <code>true</code>, then the given expression must
     * end the term.
     * @return An array of the IDs that <em>might</em> match the given
     * wildcard pattern.  The names of the entries that map to these IDs
     * will need to be tested to make sure that they actually match.  IF
     * this value is <code>null</code> then there are no possible matching
     * IDs.  If this value is an array of length 0, then all IDs must be
     * tested.
     */
    public int[] getMatching(String wc,
                              boolean starts,
                              boolean ends) {
        //
        // Quick sanity check.
        if(wc.length() == 0 || size() == 0) {
            return null;
        }

        //
        // Lower case the string, since we store lowercase.
        wc = CharUtils.toLowerCase(wc);

        //
        // Our candidate bigram terms.
        List bigrams = new ArrayList(wc.length());

        //
        // Candidate bigrams.
        char[] bg = new char[2];

        //
        // Iterate over the pattern, turning it into a set of overlapping
        // bigrams and unigrams.  Our starting and ending positions for
        // calculating bigrams will depend on whether the string has to be
        // anchored at one end of the pattern or another.
        int b, e;
        if(starts) {
            bg[0] = (char) 0;
            b = 0;
        } else {
            bg[0] = wc.charAt(0);
            b = 1;
        }

        int l = wc.length();
        if(ends) {
            e = l;
        } else {
            e = l - 1;
        }

        for(int i = b; i <= e; i++) {

            bg[1] = (ends && i == e) ? (char) 0 : wc.charAt(i);

            //
            // If there's any wildcard character, we can't have a bigram!
            if(bg[0] == '*' || bg[0] == '?' ||
                    bg[1] == '*' || bg[1] == '?') {
                bg[0] = bg[1];
                continue;
            }

            //
            // Get this bigram from the dictionary.  If we get null, then
            // we can instantly return null, since we know that the
            // dictionary we're associated with won't contain any entry
            // whose name contains this bigram!
            Entry bigram = get(new String(bg));
            if(bigram == null) {
                return null;
            }
            bigrams.add(bigram);
            bg[0] = bg[1];
        }

        //
        // We'll use an array group.
        ArrayGroup ag = null;

        //
        // If we have bigrams, then make our ID list.
        if(bigrams.size() > 0) {
            ag = intersect(bigrams);
        } else {

            //
            // Pull out any unigrams and use those.
            char[] ug = new char[wc.length()];
            int nu = 0;
            for(int i = 0; i < wc.length(); i++) {
                char c = wc.charAt(i);
                if(c != '*' && c != '?') {
                    ug[nu++] = c;
                }
            }

            if(nu == 0) {
                //
                // No unigrams, no bigrams.  It's all wildcards, so we need to check
                // everything.
                return new int[0];
            }

            //
            // There were no bigrams, but there were unigrams, so do
            // those.  We'll do this by iterating through all of the terms
            // that start with the given character.  This should give us
            // all the occurrences of the unigram.  We'll union together
            // the postings for these and then intersect the resulting
            // unions.
            for(int i = 0; i < nu; i++) {

                //
                // Get this character and the next greater one.
                String lower = Character.toString(ug[i]);
                String upper = Character.toString((char) ((int) ug[i] + 1));
                ArrayGroup curr = getUnigrams(lower, upper);

                if(ag == null) {
                    ag = curr;
                } else {
                    ag = ag.intersect(curr);
                }

                //
                // If we ever drop to zero size, we're done.
                if(ag.getSize() == 0) {
                    return null;
                }
            }
        }

        int[] ret = ag.getDocs();

        //
        // At this point, no hits means no matches.
        if(ret.length == 0) {
            return null;
        }
        return ret;
    }

    /**
     * Get all the terms that could be made from any of the bigrams in
     * the given term.  (This is used generally for spelling suggestions)
     *
     * @param wc the term to vary
     * @param allowPartial if true, allow for partial matches (meaning that
     *                     strings that have bigrams that aren't present in the
     *                     dictionary will still be returned), otherwise return
     *                     an empty array as soon as a bigram is found to be
     *                     missing
     * @return a list of term ids
     */
    public int[] getAllVariants(String wc, boolean allowPartial) {
        //
        // Quick sanity check.
        if(wc.length() == 0 || size() == 0) {
            return null;
        }

        //
        // Lower case the string, since we store lowercase.
        wc = CharUtils.toLowerCase(wc);

        //
        // Our candidate bigram terms.
        List bigrams = new ArrayList(wc.length());

        //
        // Candidate bigram and unigrams.
        char[] bg = new char[2];

        //
        // Iterate over the pattern, turning it into a set of overlapping
        // bigrams.
        bg[0] = wc.charAt(0);

        for(int i = 1; i < wc.length(); i++) {

            bg[1] = wc.charAt(i);


            //
            // Get this bigram from the dictionary.  If we get null, then
            // we can instantly return null, since we know that the
            // dictionary we're associated with won't contain any entry
            // whose name contains this bigram!
            Entry bigram = get(new String(bg));
            if(bigram == null) {
                if(allowPartial) {
                    bg[0] = bg[1];
                    continue;
                } else {
                    return null;
                }
            }
            bigrams.add(bigram);
            bg[0] = bg[1];
        }

        //
        // We'll use an array group.
        ArrayGroup ag = null;

        //
        // If we have bigrams, then make our ID list.
        if(bigrams.size() > 0) {
            ag = union(bigrams);
        } else {

            //
            // No bigrams and no unigrams.  We need to check everything.
            return new int[0];
        }

        int[] ret = ag.getDocs();
        if(ret.length == 0) {
            return null;
        }
        return ret;
    }

    /**
     * Given a list of entries, find the intersection of their ID sets.
     *
     * @param entries The entries to intersect.
     * @return A group containing the intersection.
     */
    protected ArrayGroup intersect(List entries) {

        //
        // Sort the terms by the number of IDs.
        Collections.sort(entries, new EntrySizeComparator());
        ArrayGroup ag = null;
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures();
        for(Iterator i = entries.iterator(); i.hasNext();) {
            QueryEntry e = (QueryEntry) i.next();
            if(ag == null) {
                ag = new ArrayGroup(e.iterator(feat));
            } else {
                ag = ag.destructiveIntersect(e.iterator(feat));
            }
        }
        return ag;
    }

    /**
     * Union together (quick or) all the ids pointed to by the entries
     * that are passed in
     *
     * @param entries the entries to union
     * @return a scored group containing the union
     */
    protected ArrayGroup union(List entries) {
        //
        // Throw all the entries into a ScoredQuickOr, possibly tossing
        // out entries below a particular score?
        ScoredQuickOr qor = new ScoredQuickOr((DiskPartition) part,
                                              mainDict.getMaxID());
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures();
        for(Iterator it = entries.iterator(); it.hasNext();) {
            QueryEntry e = (QueryEntry) it.next();
            qor.add(e.iterator(feat));
        }
        ScoredGroup sg = (ScoredGroup) qor.getGroup();
        if(sg.getSize() >= 1000) {
            sg.sort(true);
            float nthScore = sg.getScore(1000);
            sg.discardBelow(nthScore);
        }
        return sg;
    }

    /**
     * Given a lower bound character and upper bound character, find all
     * the bigrams that have that character as a first character.
     */
    protected ArrayGroup getUnigrams(String lower,
                                      String upper) {
        DictionaryIterator di = iterator(lower, true,
                                         upper, false);

        int[] ids = new int[di.estimateSize()];
        int p = 0;
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures();
        while(di.hasNext()) {
            PostingsIterator pi = ((QueryEntry) di.next()).iterator(feat);
            if(pi == null) {
                continue;
            }
            while(pi.next()) {
                ids[p++] = pi.getID();
            }
        }
        java.util.Arrays.sort(ids, 0, p);
        int size = 0;
        int prev = -1;
        for(int i = 0; i < p; i++) {
            if(ids[i] != prev) {
                ids[size++] = ids[i];
            }
            prev = ids[i];
        }
        return new ArrayGroup(ids, size);
    }

    public void merge(DiskBiGramDictionary[] dicts,
                       int[] starts,
                       int[][] postIDMaps,
                       RandomAccessFile mDictFile,
                       PostingsOutput postOut) throws java.io.IOException {
        ((DiskDictionary) dicts[0]).merge(new StringNameHandler(),
                                          null,
                                          (DiskDictionary[]) dicts,
                                          null,
                                          starts,
                                          postIDMaps,
                                          mDictFile,
                                          new PostingsOutput[]{postOut},
                                          false);
    }
} // DiskBiGramDictionary

