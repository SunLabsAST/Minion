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
import java.util.logging.Logger;

/**
 * Provides a K-fold splitter.  The results are divided up into
 * K equal sized subsets.  At each of K iterations, one subset is
 * withheld from the training set to be the validation set.
 *
 * @author Jeff Alexander
 * @version $Revision: 1.1.2.2 $
 */

public class KFoldSplitter implements ResultSplitter
{

    /*
     * K - the number of folds
     */
    public int k;

    /** 
     * The number of documents in each fold. 
     */
    public int foldSize;
    
    /** 
     * The current fold when iterating 
     */
    public int currFold = 0;
    
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
    
    /** 
     * All the docs, paired with the array group each comes from
     */
    protected Doc[][] allDocs;

    /**
     * The log.
     */
    static Logger logger = Logger.getLogger(KFoldSplitter.class.getName());

    /**
     * The tag for this module.
     */
    protected static String logTag = "KFold";
    
    /** 
     * Default constructor.  Setup is performed in the init method.
     */
    public KFoldSplitter() {

    }

    public void init(ResultSetImpl parent, IndexConfig iC) {
        this.k = iC.getKFoldSplitterNumFolds();
        if (k < 1) {
            train = parent;
            return;
        }
        this.parent = parent;
        this.foldSize = (int)Math.ceil(parent.size() / (double)k);

        //
        // Should the docs be shuffled into a random order before
        // they're divied up?

        allDocs = new Doc[k][foldSize];

        int m = 0;
        int n = 0;
        for (Iterator it = parent.resultsIterator(); it.hasNext();) {
            ArrayGroup ag = (ArrayGroup)it.next();
            numArrayGroups++;
            int[] agDocs = ag.getDocs();

            //
            // Add Docs to allDocs for each doc in the array group,
            // Distributing them as evenly as possible over all folds.
            for (int i = 0; i < agDocs.length; i++) {
                allDocs[m][n] = new Doc(agDocs[i], ag);
                m = ++m % k;
                if (m == 0) {
                    // We wrapped around
                    n++;
                }
            }
        }

        /*
          // this is for randomized folds:
        allDocs = new Doc[k][foldSize];
        Doc[] docs = new Doc[parent.size()];
        int currDoc = 0;
        for (Iterator it = parent.resultsIterator(); it.hasNext();) {
            ArrayGroup ag = (ArrayGroup)it.next();
            numArrayGroups++;
            int[] agDocs = ag.getDocs();

            for (int i = 0; i < agDocs.length; i++) {
                docs[currDoc++] = new Doc(agDocs[i], ag);
            }
        }

                //
        // Randomize the array
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < docs.length; i++) {
            int swap = random.nextInt(docs.length);
            Doc tmp = docs[i];
            docs[i] = docs[swap];
            docs[swap] = tmp;
        }

        int n = 0;
        int m = 0;
        for (int i = 0; i < docs.length; i++) {
            allDocs[m][n] = docs[i];
            m = ++m % k;
            if (m == 0) {
                n++;
            }
            }

        */
    }

    
    /** 
     * Gets the minimum number of docs needed for this splitter to be
     * useful.
     * 
     * @return the number of folds
     */
    public int getMinDocs() {
        return k;
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
        //
        // If we've gone through all the folds, there are no more splits
        if (currFold >= k) {
            return false;
        }

        //
        // Go through all the docs to build our data sets
        ResultSetImpl.AGDocs[] trainDocs =
            new ResultSetImpl.AGDocs[numArrayGroups];
        ResultSetImpl.AGDocs[] validateDocs =
            new ResultSetImpl.AGDocs[numArrayGroups];
        
        ResultSetImpl.AGDocs[] target;
        for (int currK = 0; currK < k; currK++) {
            //
            // Are we building the training or validation set?
            if (currK == currFold) {
                target = validateDocs;
            } else {
                target = trainDocs;
            }
            for (int n = 0; (n < foldSize) && (allDocs[currK][n] != null); n++) {
                Doc d = allDocs[currK][n];
                //
                // Find the agdocs for this ag
                ResultSetImpl.AGDocs currAGDocs = null;
                int a;
                for (a = 0; (a < target.length) && (target[a] != null); a++) {
                    if (target[a].getGroup() == d.ag) {
                        currAGDocs = target[a];
                    }
                }

                //
                // If we didn't find one, make one
                if (currAGDocs == null) {
                    currAGDocs = parent.newAGDocs(d.ag);
                    target[a] = currAGDocs;
                }

                //
                // Add the doc
                currAGDocs.add(d.docID);
            }
        }

        train = new ResultSetImpl(parent, trainDocs);
        validate = new ResultSetImpl(parent, validateDocs);
        
        currFold++;
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
