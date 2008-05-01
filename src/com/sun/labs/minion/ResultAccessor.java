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

import java.util.List;

/**
 * An interface for accessing the values associated with a search result that is
 * being considered for inclusion in a list of results returned to a user.
 * 
 * @see ResultsFilter
 */
public interface ResultAccessor {
    
    /**
     * Gets the score for the result under consideration.
     */
    public float getScore();
    
    /**
     * Gets the key of the result under consideration
     * @return the key of the current result
     */
    public String getKey();
    
    /**
     * Gets the values associated with the given saved field for the current
     * result
     * @param field the field whose values we want
     * @return the values for this field. If the current result does not have
     * any saved values for this field, an empty list will be returned.
     */
    public List<Object> getField(String field);
    
    /**
     * Gets a single value for the given saved field for the current result
     * @param field the field whose value we want
     * @return the field value for the current result, or <code>null</code> if 
     * this result does not have a value saved for this field
     */
    public Object getSingleFieldValue(String field);
    
}
