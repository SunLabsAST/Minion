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

import java.io.Serializable;

/**
 * A class that contains the name of a field and a weight that should be applied
 * to the similarity values for that field when finding documents similar to 
 * a given document.
 */
public class WeightedField implements Serializable {
    
    private FieldInfo field;
    
    private float weight;
    
    /**
     * Creates a weighted field for a given field and weight.
     * @param fieldName the name of the field to which we want to apply the weight
     * @param weight the weight that we want to apply
     */
    public WeightedField(FieldInfo field, float weight) {
        this.field = field;
        this.weight = weight;
    }
    
    public FieldInfo getField() {
        return field;
    }
    
    public float getWeight() {
        return weight;
    }

}
