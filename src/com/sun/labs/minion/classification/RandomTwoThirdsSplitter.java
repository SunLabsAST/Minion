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

import java.util.Iterator;

import com.sun.labs.minion.retrieval.ArrayGroup;
import com.sun.labs.minion.retrieval.ResultSetImpl;

import com.sun.labs.minion.IndexConfig;

/**
 * Provides two thirds/one third splits of a result set
 * by selecting documents at random to place in either
 * set.
 *
 * @author Jeff Alexander
 * @version $Revision: 1.1.2.3 $
 */

public class RandomTwoThirdsSplitter implements ResultSplitter
{
    /** 
     * The number of times to split 
     */
    protected int numSplits;

    /** 
     * The set that should be trained on
     */
    protected ResultSetImpl train;

    /** 
     * The set that should be used for validation 
     */
    protected ResultSetImpl validate;

    /** 
     * The full results, as passed in 
     */
    protected ResultSetImpl parent;

    /** 
     * The number of array groups in the result set 
     */
    protected int numArrayGroups;
    
    protected Doc[] allDocs;

    /** 
     * Default constructor.  Setup is performed in the init method.
     */
    public RandomTwoThirdsSplitter() {
    }

    public void init(ResultSetImpl parent, IndexConfig iC) {
        this.numSplits = iC.getRandomSplitterNumSplits();
        this.parent = parent;
        
        allDocs = new Doc[parent.size()];
        int currDoc = 0;
        for (Iterator it = parent.resultsIterator(); it.hasNext();) {
            ArrayGroup ag = (ArrayGroup)it.next();
            numArrayGroups++;
            int[] agDocs = ag.getDocs();

            //
            // Add Docs to allDocs for each doc in the array group
            for (int i = 0; i < agDocs.length; i++) {
                allDocs[currDoc++] = new Doc(agDocs[i], ag);
            }
        }

    }

    /** 
     * Gets the minimum number of docs needed for this splitter to be
     * useful.
     * 
     * @return the number 3
     */
    public int getMinDocs() {
        return 3;
    }
     
    /** 
     * Gets the first of the two subset
     * 
     * @return a result set that is a subset of the one passed in
     */
    public ResultSetImpl getTrainSet() {
        return train;
    }

    /** 
     * Gets the second of the two subsets
     * 
     * @return a result set that is a subset of the one passed in
     */
    public ResultSetImpl getValidateSet() {
        return validate;
    }

    /** 
     * Advances to the next split, if there is one.  getFirstSet()
     * and getSecondSet() will return the new splits after this
     * method is called
     * 
     * @return true if there is another split available
     */
    public boolean nextSplit() {
        if (numSplits-- <= 0) {
            return false;
        }
        //
        // Randomize the array
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < allDocs.length; i++) {
            int swap = random.nextInt(allDocs.length);
            Doc tmp = allDocs[i];
            allDocs[i] = allDocs[swap];
            allDocs[swap] = tmp;
        }

        //
        // Now take out the splits
        int twoThirds = allDocs.length * 2 / 3;
        int oneThird = allDocs.length - twoThirds;

        //
        // First the two thirds:
        ResultSetImpl.AGDocs[] trainDocs = new ResultSetImpl.AGDocs[numArrayGroups];
        int currAG = 0;
        for (Iterator it = parent.resultsIterator(); it.hasNext();) {
            ArrayGroup ag = (ArrayGroup)it.next();
            trainDocs[currAG] = parent.newAGDocs(ag);
            for (int i = 0; i < twoThirds; i++) {
                //
                // Note: I'm intentionally using == instead of equals() here
                // since equals() is kind of expensive on ArrayGroup, and I
                // know that I'm dealing with all the same instances here.
                if (allDocs[i].ag == ag) {
                    trainDocs[currAG].add(allDocs[i].docID);
                }
            }
            currAG++;
        }
        train = new ResultSetImpl(parent, trainDocs);

        //
        // Then the one third:
        ResultSetImpl.AGDocs[] validateDocs = new ResultSetImpl.AGDocs[numArrayGroups];
        currAG = 0;
        for (Iterator it = parent.resultsIterator(); it.hasNext();) {
            ArrayGroup ag = (ArrayGroup)it.next();
            validateDocs[currAG] = parent.newAGDocs(ag);
            for (int i = twoThirds; i < allDocs.length; i++) {
                if (allDocs[i].ag == ag) {
                    validateDocs[currAG].add(allDocs[i].docID);
                }
            }
            currAG++;
        }
        validate = new ResultSetImpl(parent, validateDocs);
        return true;
    }

    public class Doc {
        public int docID;
        public ArrayGroup ag;

        public Doc(int docID, ArrayGroup ag) {
            this.docID = docID;
            this.ag = ag;
        }
    }
}
