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
package com.sun.labs.minion;

import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.Serializable;
import java.util.Comparator;

public class WeightedFeature implements Feature, Serializable {

    /**
     * The name of the feature.
     */
    protected String name;

    /**
     * The ID for the feature, used when doing postings.
     */
    protected int id;

    /**
     * The frequency of the feature.
     */
    protected int freq;

    /**
     * The weight associated with the feature.
     */
    protected float weight;

    /**
     * The dictionary entry that gave us this feature. This is an optimization
     * to avoid having to re-fetch and re-decode features when doing things like
     * <code>DocumentVector.findSimilar</code>
     */
    protected transient QueryEntry entry;

    /**
     * A weight comparator where small weights are less than large weights
     */
    public static final Comparator<WeightedFeature> WEIGHT_COMPARATOR =
            new Comparator<WeightedFeature>() {

                @Override
                public int compare(WeightedFeature o1,
                        WeightedFeature o2) {
                    if(o1.getWeight() < o2.getWeight()) {
                        return -1;
                    }

                    if(o1.getWeight() > o2.getWeight()) {
                        return 1;
                    }
                    return 0;
                }

                @Override
                public boolean equals(Object o) {
                    return false;
                }
            };

    /**
     * An "inverse" weight comparator where large weights are less than small
     * weights
     */
    public static final Comparator<WeightedFeature> INVERSE_WEIGHT_COMPARATOR =
            new Comparator<WeightedFeature>() {

                @Override
                public int compare(WeightedFeature o1,
                        WeightedFeature o2) {
                    if(o1.getWeight() < o2.getWeight()) {
                        return 1;
                    }

                    if(o1.getWeight() > o2.getWeight()) {
                        return -1;
                    }
                    return 0;
                }

                @Override
                public boolean equals(Object o) {
                    return false;
                }
            };

    /**
     * An inverse count comparator.
     */
    public static final Comparator<WeightedFeature> INV_COUNT_COMPARATOR =
            new Comparator<WeightedFeature>() {

                @Override
                public int compare(WeightedFeature o1, WeightedFeature o2) {
                    return o2.freq - o1.freq;
                }

                @Override
                public boolean equals(Object o) {
                    return this == o;
                }
            };

    public static final Comparator<WeightedFeature> NAME_COMPARATOR =
            new Comparator<WeightedFeature>() {

                @Override
                public int compare(WeightedFeature t, WeightedFeature t1) {
                    return t.name.compareTo(t1.name);
                }
            };

    public WeightedFeature() {
    }

    public WeightedFeature(String name) {
        this(name, 0);
    }

    public WeightedFeature(String name, float weight) {
        this.name = name;
        this.weight = weight;
    } // WeightedFeature constructor

    public WeightedFeature(String name, int id, float weight) {
        this.name = name;
        this.id = id;
        this.weight = weight;
    } // WeightedFeature constructor

    public WeightedFeature(QueryEntry<String> entry, int freq, float weight) {
        this.name = entry.getName();
        this.id = entry.getID();
        this.entry = entry;
        this.freq = freq;
        this.weight = weight;
    }

    public WeightedFeature(WeightedFeature f) {
        name = f.name;
        weight = f.weight;
        id = f.id;
        entry = f.entry;
        id = f.id;
    }

    /**
     * Sets the name of this feature.
     *
     * @param name the name of the feature.
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the name of this feature.
     *
     * @return the name of the feature.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Gets the weight associated with this feature.
     */
    public float getWeight() {
        return weight;
    }

    /**
     * Sets the weight associated with this feature.
     *
     * @param weight the weight
     */
    public void setWeight(float weight) {
        this.weight = weight;
    }

    /**
     * Combines another weighted feature with this one. The other feature is
     * assumed to have the same name!
     */
    public void combine(WeightedFeature f) {
        freq += f.freq;
        weight += f.weight;
    }

    /**
     * Encodes the information in this feature onto the given buffer.
     *
     * @param b a buffer onto which the feature's information can be encoded.
     */
    @Override
    public void encode(WriteableBuffer b) {
        b.byteEncode(Float.floatToIntBits(weight), 4);
    }

    /**
     * Decodes the information in this feature from the given buffer.
     *
     * @param b a buffer from which the feature's information can be decoded.
     */
    @Override
    public void decode(ReadableBuffer b) {
        weight = Float.intBitsToFloat(b.byteDecode(4));
    }
    //
    // Implementation of Occurrence.

    /**
     * Sets the ID associated with this feature.
     */
    @Override
    public void setID(int id) {
        this.id = id;
    }

    /**
     * Gets the ID associated with an occurrence.
     */
    @Override
    public int getID() {
        return id;
    }

    public void setFreq(int freq) {
        this.freq = freq;
    }

    public int getFreq() {
        return freq;
    }

    @Override
    public int getPosition() {
        return 1;
    }

    /**
     * Gets the count of occurrences that this feature represents.
     *
     * @return the number of occurrences, which is always 1 in this case
     */
    @Override
    public int getCount() {
        return 1;
    }

    /**
     * Sets the count of occurrences that this occurrence represents.
     *
     * @param count the number of occurrences.
     */
    @Override
    public void setCount(int count) {
        freq = count;
    }

    public void setEntry(QueryEntry entry) {
        this.entry = entry;
    }

    public QueryEntry getEntry() {
        return entry;
    }

    /**
     * Compares two features on the basis of their names.
     */
    @Override
    public int compareTo(Feature o) {
        return name.compareTo(o.getName());
    }

    /**
     * Two WeightedFeature objects are equal if they represent the same feature
     * -- that is, their names are the same, but their weights need not be.
     *
     * @param o the object to compare to this one
     * @return true if the names of the features are the same
     */
    @Override
    public boolean equals(Object o) {
        try {
            return name.equals(((WeightedFeature) o).getName());
        } catch(ClassCastException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return String.format("%s %.4f", name, weight);
    }
} // WeightedFeature
