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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides a combination of a document key and a map of field value pairs.
 */
public class IndexableMap implements Indexable {
    
    /**
     * The key for the document represented by this map.
     */
    protected String key;
    
    /**
     * The map from field names to field values.
     */
    protected Map<String,Object> map;
    
    /**
     * Creates an indexable map, suitable for passing to an indexing queue in
     * a search engine.  This object will create a map to which field name and
     * and value pairs can be added.
     *
     * @param key a unique key for the document represented by this map.
     * @see #put(String, Object)
     */
    public IndexableMap(String key) {
        this.key = key;
        map = new LinkedHashMap<String,Object>();
    }
    
    /**
     * Creates an indexable map, suitable for passing to an indexing queue in
     * a search engine.
     *
     * @param key a unique key for the document represented by this map.
     * @param map a map from field names to field values.  An implementation of
     * map that retains the order in which elements are added is the best thing
     * to use.
     */
    public IndexableMap(String key, Map<String,Object> map) {
        this.key = key;
        this.map = map;
    }
    
    /**
     * Puts a field name and value pair into our map.
     *
     * @param name the name of the field
     * @param value a value for the field.
     */
    public void put(String name, Object value) {
        map.put(name, value);
    }
    
    /**
     * Gets the key for this document.
     * @return The key for this document
     */
    @Override
    public String getKey() {
        return key;
    }
    
    /**
     * Gets the map from field names to field values
     * @return the map from field names to field values.
     */
    @Override
    public Map<String, Object> getMap() {
        return map;
    }

    @Override
    public String toString() {
        return "IndexableMap{" + "key=" + key + '}';
    }
}
