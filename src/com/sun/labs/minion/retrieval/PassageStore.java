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

import com.sun.labs.minion.util.Util;

/**
 * A class to store the passages for a single field while doing proximity
 * queries or passage highlighting.
 */
public class PassageStore {

    /**
     * The array group that we're associated with.
     */
    protected ArrayGroup ag;

    /*
     * Any hit passages that were stored during retrieval.
     */
    protected int[] pass;

    /**
     * The starting positions in the passages array for the passages for a
     * given document.  This array will correspond to the docs array in an
     * <code>ArrayGroup</code>.
     */
    protected int[] passStartPosns;

    /**
     * The ending positions in the passages array for the passages for a
     * given document.  This array will correspond to the docs array in an
     * <code>ArrayGroup</code>.
     */
    protected int[] passEndPosns;

    /**
     * The penalties associated with each of the passsages.
     */
    protected float[] penalties;

    /**
     * The current position in the passages array.
     */
    protected int passPosn;

    /**
     * The number of passages stored in the <code>pass</code> array.
     */
    protected int nPass;

    /**
     * The width of the passages that we're storing.
     */
    protected int width;

    /**
     * Instantiates a passage store.
     *
     * @param ag The array group that we're associated with.
     */
    public PassageStore(ArrayGroup ag) {
        this.ag = ag;
        this.width = ag.width;
        pass = new int[16];
        passStartPosns = new int[ag.docs.length];
        passEndPosns = new int[ag.docs.length];
        penalties = new float[ag.docs.length];

        for(int i = 0; i < passStartPosns.length; i++) {
            passStartPosns[i] = -1;
        }
    } // PassageStore constructor

    /**
     * Adds a passage to this passage store.
     *
     * @param pos The position to which the passage should be added.
     * @param p The passage to add.
     */
    public void add(int pos, int[] p, float penalty) {
        if(passPosn + width >= pass.length) {
            pass = Util.expandInt(pass, (passPosn+width)*2);
        }
        //
        // Store the start position, if necessary.
        if(passStartPosns[pos] < 0) {
            passStartPosns[pos] = passPosn;
        }
        
        System.arraycopy(p, 0, pass, passPosn, p.length);
        passPosn += p.length;

        //
        // Store the end position (exclusive).
        passEndPosns[pos] = passPosn;

        //
        // Store the penalty.
        if(nPass + 1 >= penalties.length) {
            penalties = Util.expandFloat(penalties,
                                         penalties.length*2);
        }
        penalties[nPass++] = penalty;
    }

    /**
     * Gets all of the passages associated with a document as a single
     * array of int.
     */
    protected int[] getAllPassages(int doc) {

        int pos = Util.binarySearch(ag.docs, 0, ag.size, doc);

        if(pos < 0 || passStartPosns[pos] < 0) {
            return new int[0];
        }

        int[] ret = new int[passEndPosns[pos] - passStartPosns[pos]];
        System.arraycopy(pass, passStartPosns[pos], ret, 0,
                         passEndPosns[pos] - passStartPosns[pos]);
        return ret;
    }

    protected float getPenalty(int doc) {
        int pos = Util.binarySearch(ag.docs, 0, ag.size, doc);
        if(pos < 0 || passStartPosns[pos] < 0) {
            return 0;
        }
        return penalties[passStartPosns[pos] / width];
    }

    protected int[][] getUniquePassages(int doc) {
        
        int pos = Util.binarySearch(ag.docs, 0, ag.size, doc);

        if(pos < 0 || passStartPosns[pos] < 0) {
            return new int[0][];
        }

        int n = (passEndPosns[pos] - passStartPosns[pos]) / width;
        int[][] ret = new int[n][width];
        for(int i = passStartPosns[pos], j = 0; i < passEndPosns[pos];
            i += width, j++) {
            System.arraycopy(pass, i, ret[j], 0, width);
        }
        return ret;
    }

    protected float[] getPenalties(int doc) {
        int pos = Util.binarySearch(ag.docs, 0, ag.size, doc);

        if(pos < 0 || passStartPosns[pos] < 0) {
            return new float[0];
        }

        int n = (passEndPosns[pos] - passStartPosns[pos]) / width;
        float[] ret = new float[n];
        for(int i = passStartPosns[pos] / width, j = 0; j < n;  i++, j++) {
            ret[j] = penalties[i];
        }
        return ret;
    }

    public String toString(int doc) {
        int pos = Util.binarySearch(ag.docs, 0, ag.size, doc);
        if(pos < 0 || passStartPosns[pos] < 0) {
            return "{}";
        }
            
        StringBuffer b = new StringBuffer();
        b.append("{");
        if(passStartPosns[pos] >= 0) {
            for(int i = passStartPosns[pos];
                i < passEndPosns[pos]; i+= width) {
                if(i > passStartPosns[pos]) {
                    b.append(", ");
                }
                b.append("[");
                for(int j = i ; j < i+width; j++) {
                    if(j > i) {
                        b.append(' ');
                    }
                    b.append(pass[j]);
                }
                b.append("]");
            }
        }
        b.append('}');
        return b.toString();
    }
    
} // PassageStore
