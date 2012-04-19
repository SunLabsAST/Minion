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

/**
 * A class containing a single field value and an associated score.  Such values
 * are returned when looking for saved string field values similar to a given value.
 * 
 * @see SearchEngine#getMatching
 */
public class FieldValue implements Comparable<FieldValue> {
    
    private String value;
    private float score;

    /**
     * Creates a field value
     * @param value the field value
     * @param score the score associated with this value
     */
    public FieldValue(String value, float score) {
        this.value = value;
        this.score = score;
    }
    
    /**
     * Gets the value.
     * @return the field value
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Gets the score 
     * @return the score
     */
    public float getScore() {
        return score;
    }

    /**
     * Compares one field value to another.
     * @param o the other field value to compare
     * @return a value less than zero, zero, or greater than zero depending
     * on whether this field value is, respectively, smaller than, equal to, or greater
     * than the other value.
     */
    @Override
    public int compareTo(FieldValue o) {
        if(score < o.score) {
            return -1;
        }
        
        if(score > o.score) {
            return 1;
        }
        
        return value.compareTo(o.value);
    }

}
