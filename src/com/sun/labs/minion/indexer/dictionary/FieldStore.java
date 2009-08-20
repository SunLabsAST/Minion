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
package com.sun.labs.minion.indexer.dictionary;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.SearchEngineException;
import java.util.EnumSet;
import java.util.Map;
import com.sun.labs.minion.indexer.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A common base class for <code>MemoryFieldStore</code> and
 * <code>DiskFieldStore</code>.
 */
public class FieldStore {

    /**
     * The meta file.
     */
    protected MetaFile metaFile;

    /**
     * The saved fields.
     */
    protected SavedField[] savedFields;

    /**
     * The log.
     */
    static Logger logger = Logger.getLogger(FieldStore.class.getName());

    /**
     * The log tag.
     */
    protected static String logTag = "FS";

    /**
     * The header for the old-style field store.
     */
    protected static String header = "SLSE v2.0 Field Store\n";

    /**
     * Gets the info for a given field name.
     *
     * @param name the field name.
     * @return The new field information object, or <code>null</code> if there
     * is no such field.
     */
    public FieldInfo getFieldInfo(String name) {
        return metaFile.getFieldInfo(name);
    }

    /**
     * Gets the info for a given field name.  This will add the named field
     * to the mapping and assign it an ID if it doesn't have one already.
     *
     * @param name the field name.
     * @param attributes The field attributes.
     * @param type the field type.
     * @return The new field information object, or <code>null</code> if
     * there was some problem with the meta file.
     */
    protected FieldInfo getFieldInfo(String name,
            EnumSet<FieldInfo.Attribute> attributes,
            FieldInfo.Type type) {

        //
        // Get the field ID from our current meta file.
        try {
            return metaFile.addField(name, attributes, type);
        } catch(SearchEngineException see) {
            logger.log(Level.SEVERE, "Error getting field info", see);
            return null;
        }
    }

    /**
     * Gets the number of currently defined fields.
     */
    public int getNFields() {
        return metaFile.size();
    }

    /**
     * Gets the ID for a given field info object.
     *
     * @param f The <code>FieldInfo</code> object.
     */
    public int getFieldID(FieldInfo f) {
        FieldInfo fi = metaFile.getFieldInfo(f.getName());
        if(fi == null) {
            try {
                fi = metaFile.addField(f);
            } catch(SearchEngineException see) {
                logger.log(Level.SEVERE, "Error getting field info", see);
                return 0;
            }
        }
        return fi.getID();
    }

    /**
     * Gets the name associated with a field ID.
     *
     * @param id The field ID of interest.
     * @return The name, or null if the id is out of range for this field
     * store.
     */
    public String getFieldName(int id) {
        FieldInfo fi = metaFile.getFieldInfo(id);
        return fi == null ? null : fi.getName();
    }

    /**
     * Gets the type of a saved field.
     *
     * @param name the name of the field for which we want the type
     * @return The saved field type, or 0 if the named field does not exist
     * or is not a saved field.
     */
    public FieldInfo.Type getFieldType(String name) {
        FieldInfo fi = metaFile.getFieldInfo(name);
        return fi == null ? FieldInfo.Type.NONE : fi.getType();
    }

    /**
     * Gets an array of field IDs suitable for use in postings entry
     * iterators.
     *
     * @param fieldNames The names of the fields that we're interested in.
     * If <code>null</code> is given as a field value, then words that
     * don't occur in any field are what we're interested in.
     * @return an array of integers, as wide as the number of fields, with
     * 1s in the elements for the provided fields, or <code>null</code> if
     * the given array of field names is null.
     */
    public int[] getFieldArray(String[] fieldNames) {
        return metaFile.getFieldArray(fieldNames);
    }

    /**
     * Gets an array of field multipliers suitable for use in postings
     * entry iterators.
     *
     * @param fieldNames The names of the fields that we're interested in.
     * If a <code>null</code> field name is passed, that multiplier will be
     * used for occurrences that are not in any field.
     * @param mult The multipliers for the named fields, in the order that
     * the fields are given in.
     * @return an array as wide as the number of defined fields with the
     * multipliers specified by field ID.
     */
    public float[] getMultArray(String[] fieldNames, float[] mult) {
        return metaFile.getMultArray(fieldNames, mult);
    }

    /**
     * Gets an array for field multipliers suitable for use in postings
     * iterators.
     */
    public float[] getMultArray(Map mult) {
        return metaFile.getMultArray(mult);
    }

    /**
     * Determines whether a given field name is a saved field or not.
     */
    public boolean isSavedField(String name) {
        FieldInfo fi = metaFile.getFieldInfo(name);
        return fi != null && fi.isSaved();
    }

    /**
     * Gets an array of the vectored fields.  The no-field field is always
     * assumed to be vectored.
     */
    public int[] getVectoredFields() {
        return metaFile.getVectoredFields();
    }
} // FieldStore
