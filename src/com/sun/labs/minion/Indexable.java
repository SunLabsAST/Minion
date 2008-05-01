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

import java.util.Map;

/**
 * An interface that should be implemented by things that are indexable.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public interface Indexable {
    
    /**
     * Gets the unique key associated with this indexable object.
     *
     * @return the unique key associated with this object.
     */
    public String getKey();
    
    /**
     * Gets the map from field names to field values associated with this indexable object.
     *
     * @return a map from field names to field values.  If there was some problem
     * generating the map, <code>null</code> can be returned and the indexer will
     * ignore this document.
     */
    public Map<String,Object> getMap();
    
}
