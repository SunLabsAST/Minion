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

import java.util.HashSet;
import java.util.Set;

/**
 * A weighted feature class that contains a 2x2 contingency table that can
 * be used to calculate the Mutual Information or Chi-squared measures.
 */
public class ContingencyFeature extends WeightedFeature {

    /**
     * The type of weight that we'll return.
     *
     * @see #MUTUAL_INFORMATION
     * @see #CHI_SQUARED
     */
    protected int type;

    /**
     * The number of documents in the class that contain this feature.
     */
    protected int a;

    /**
     * The number of documents <em>not</em> in the class that contain this
     * feature.
     */
    protected int b;

    /**
     * The number of documents in the class that <em>don't</em> contain
     * this feature.
     */
    protected int c;

    /**
     * The number of documents <em>not</em> in the class that
     * <em>don't</em> contain this feature.
     */
    protected int d;

    /**
     * The total number of documents.
     */
    protected int N;

    /**
     * Whether we've already calculated the weight.
     */
    protected boolean weightCalculated;

    /**
     * A constant indicating that we should return the MI value as the
     * weight for this feature.
     */
    public static final int MUTUAL_INFORMATION = 1;

    /**
     * A constant indicating that we should return the Chi-squared value as
     * the weight for this feature.
     */
    public static final int CHI_SQUARED = 2;

    protected Set<Integer> docIDs;
    
    /** 
     * Creates an empty contingency feature with the default MI type
     */
    public ContingencyFeature() {
        super();
        this.type = MUTUAL_INFORMATION;
    }
    
    /**
     * Creates an empty feature.
     */
    public ContingencyFeature(int type) {
        super();
        this.type = type;
    }

    /**
     * Creates a feature with the given name.
     */
    public ContingencyFeature(String name) {
        super(name);
    }

    /** 
     * Creates a copy of a contingency feature
     * 
     * @param cf feature to copy
     */
    public ContingencyFeature(ContingencyFeature cf) {
        super(cf);
        this.name = cf.name;
        this.type = cf.type;
        this.a = cf.a;
        this.b = cf.b;
        this.c = cf.c;
        this.d = cf.d;
        this.N = cf.N;
        this.weightCalculated = cf.weightCalculated;
        this.docIDs = cf.docIDs;
    }
    
    /** 
     * Adds the other feature's weights to this one's
     * 
     * @param other
     */
    public void sum(ContingencyFeature other) {
        a += other.a;
        b += other.b;
        c += other.c;
        d += other.d;
    }

    public int getA() {
        return a;
    }
    
    public int getB() {
        return b;
    }
    
    public void addDoc(int docID) {
        if (docIDs == null) {
            docIDs = new HashSet<Integer>();
        }
        docIDs.add(docID);
    }

    protected void wipeDocs() {
        docIDs = null;
    }

    protected Set<Integer> getDocs() {
        return docIDs;
    }
    
    /**
     * Gets the weight associated with this term.
     */
    public float getWeight() {
        if(weightCalculated) {
            return weight;
        }

        switch(type) {
        case MUTUAL_INFORMATION:
            weight = (float) Math.log(((float) a * N) /
                                      (float)((a + c) * ((long)a + b)));
            break;
        case CHI_SQUARED:
            weight =
                (N*(float) Math.pow((float) a*d - c*b, 2)) /
                ((float) ((long)a+c)*(b+d)*(a+b)*(c+d));
            break;
        default:
            weight =  0;
        }
        weightCalculated = true;
        return weight;
    }

    public void setWeight(float weight) {
        super.setWeight(weight);
        weightCalculated = true;
    }
    
    public String toString() {
        return name;
//        return String.format("%-20s a: %4d b: %4d c: %4d N: %4d weight: %.3f", 
//                name, a, b, c, N,
//                getWeight());
    }
    
} // ContingencyFeature
