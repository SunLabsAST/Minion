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
 * The MetaDataStore provides a mechanism to store name/value pairs
 * in associate with an index.  The search engine does not store any
 * data of its own in the MetaDataStore.  This service exists to allow
 * applications that use the search engine to store a few values
 * in the index directory in a safe way for the engine.  Changes are
 * persisted at the same time that the engine dumps in-memory data
 * to disk.
 */
public interface MetaDataStore {

    /**
     * Sets a propery in the meta data store.  Changes are made
     * consistent when the {@link SearchEngine#flush} method is
     * called.
     *
     * @param name the name of the property to set
     * @param value the value of the named property
     */
    public void setProperty(String name, String value);

    /**
     * Gets the value of the named property.
     *
     * @param name the property to retrieve
     * @return the property value
     */
    public String getProperty(String name);

    /**
     * Gets the value of the named property, or the default value
     * if no value exists.
     *
     * @param name the property to retrieve
     * @param defaultValue the default value to return if the
     *                     property is not found
     * @return the stored value or the default value
     */
    public String getProperty(String name, String defaultValue);
}
