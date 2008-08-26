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
import com.sun.labs.minion.FieldValue;
import java.io.RandomAccessFile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.*;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.postings.PostingsIterator;

import com.sun.labs.minion.indexer.postings.io.PostingsOutput;

/**
 * A field store that can be used for querying operations.
 */
public class DiskFieldStore extends FieldStore {
    
    /**
     * The partition this field store is associated with.
     */
    protected DiskPartition part;
    
    /**
     * The number of documents.
     */
    protected int nDocs;
    
    /**
     * The tag for this module.
     */
    protected static String logTag = "DFS";

    /**
     * Reads the field store from the provided file.
     *
     * @param part The partition that this field store is associated with.
     * @param dictFile The file containing the dictionaries for the saved fields.
     * @param postFiles The files containing the postings for the saved fields.
     * @param metaFile The meta file to use to get field information.
     * @throws java.io.IOException if there is an error during reading
     */
    public DiskFieldStore(DiskPartition part,
            RandomAccessFile dictFile,
            RandomAccessFile[] postFiles,
            DictionaryFactory fieldStoreDictFactory,
            DictionaryFactory bigramDictFactory,
            MetaFile metaFile)
            throws java.io.IOException {
        
        this.part = part;
        this.metaFile = metaFile;
        savedFields = new SavedField[metaFile.size()+1];
        
        FieldStoreHeader fsh = new FieldStoreHeader(dictFile.getChannel());
        for(int i = 1; i <= fsh.nFields; i++) {
            long pos = fsh.getOffset(i);
            if(pos > 0) {
                dictFile.seek(pos);
                savedFields[i] =
                        makeSavedField(metaFile.getFieldInfo(i),
                        dictFile,
                        postFiles,
                        fieldStoreDictFactory,
                        bigramDictFactory,
                        part);
            }
        }
    }
    
    /**
     * Makes a saved field instance of the appropriate type.
     */
    protected SavedField makeSavedField(FieldInfo fi,
            RandomAccessFile dictFile,
            RandomAccessFile[] postFiles,
            DictionaryFactory fieldStoreDictFactory,
            DictionaryFactory bigramDictFactory,            
            DiskPartition part) throws java.io.IOException {
        if(fi.getType() != FieldInfo.Type.FEATURE_VECTOR) {
            return new BasicField(fi, dictFile, postFiles, fieldStoreDictFactory, 
                    bigramDictFactory, part);
        } else {
            return new FeatureVector(fi, dictFile, postFiles, part);
        }
    }
    
    /**
     * Closes the field store.
     */
    public void close() throws java.io.IOException {
    }
    
    /**
     * Gets a saved field from a field name.
     *
     */
    public SavedField getSavedField(String name) {
        return getSavedField(metaFile.getFieldInfo(name));
    }
    
    /**
     * Gets a saved field from a field name.
     *
     */
    public SavedField getSavedField(FieldInfo fi) {
        if(fi == null || fi.getID() >= savedFields.length) {
            return null;
        }
        return savedFields[fi.getID()];
    }
    
    /**
     * Gets the type of hte named field, if it is a saved field.
     *
     * @param name The name of the field.
     * @return The type of the field, as defined in (@link
     * com.sun.labs.minion.FieldInfo.Type}. If the name is not the name of a
     * saved field, then FieldInfo.Type.NONE is returned.
     */
    public FieldInfo.Type getFieldType(String name) {
        SavedField f = getSavedField(name);
        if(f != null) {
            return f.getField().getType();
        }
        return FieldInfo.Type.NONE;
    }
    
    /**
     * Gets saved data for a particular field.
     *
     * @param name The name of the field.
     * @param docID The document whose field value we want.
     * @param all If <code>true</code>, return all known values for the
     * field in the given document.  If <code>false</code> return only one
     * value.
     * @return If <code>all</code> is <code>true</code>, then return a
     * <code>List</code> of the values stored in the given field in the
     * given document.  The elements of the list will have a type that is
     * appropriate to the type of the saved field.  If <code>all</code> is
     * <code>false</code>, a single value of the appropriate type will be
     * returned.
     *
     * <P>
     *
     * If the given name is not the name of a saved field, or the document
     * ID is invalid, <code>null</code> will be returned.
     */
    public Object getSavedFieldData(String name, int docID, boolean all) {
        return getSavedFieldData(metaFile.getFieldInfo(name), docID, all);
    }
    
    public Object getSavedFieldData(FieldInfo fi, int docID, boolean all) {
        
        //
        // If the field information is null, then return null or an empty list
        // depending on whether we're supposed to return all values.
        if(fi == null) {
            if(all) {
                return new ArrayList();
            } else {
                return null;
            }
        }
        SavedField f = getSavedField(fi);
        
        //
        // No saved field by that name.
        if(f == null) {
            if(all) {
                return new ArrayList();
            } else {
                return null;
            }
        }
        
        Object ret = f.getSavedData(docID, all);
        if(ret == null) {
            if(all) {
                return new ArrayList();
            } else {
                return null;
            }
        }
        
        return ret;
    }
    
    /**
     * Get the default value for a saved field.
     */
    public Object getDefaultSavedFieldData(String name) {
        return getDefaultSavedFieldData(metaFile.getFieldInfo(name));
    }
    
    /**
     * Get the default value for a saved field.
     */
    public Object getDefaultSavedFieldData(FieldInfo fi) {
        if(fi == null) {
            return null;
        }
        return fi.getDefaultSavedValue();
    }
    
    /**
     * Computes the euclidean distance between the given document and all
     * documents.  The distance is based on the features stored in the saved field
     * with the given name.
     */
    public double[] euclideanDistance(double[] vec, String field) {
        SavedField f = getSavedField(field);
        if(!(f instanceof com.sun.labs.minion.indexer.dictionary.FeatureVector)) {
            return null;
        }
        
        return ((com.sun.labs.minion.indexer.dictionary.FeatureVector) f).euclideanDistance(vec);
    }
    
    /**
     * Gets an interator for the field values for a given document.
     */
    public Iterator getFields(int docID) {
        return null;
    }
    
    public BasicField.Fetcher getFetcher(FieldInfo fi) {
        BasicField f = (BasicField) getSavedField(fi);
        if(f == null) {
            return null;
        }
        return f.getFetcher();
    }

    public BasicField.Fetcher getFetcher(String field) {
        return getFetcher(getFieldInfo(field));
    }

    /**
     * Gets the values for the given field that match the given pattern.
     * @param field the saved, string field against whose values we will match.
     * If the named field is not saved or is not a string field, then <code>null</code>
     * will be returned.
     * @param pattern the pattern for which we'll find matching field values.
     * @return a sorted set of field values.  This set will be ordered by the
     * proportion of the field value that is covered by the given pattern.  If the 
     * named field is not saved or is not a string, then <code>null</code> will
     * be returned.
     */
    public SortedSet<FieldValue> getMatching(String field, String pattern) {
        BasicField f = (BasicField) getSavedField(field);
        if(f != null && f.field.getType() == FieldInfo.Type.STRING) {
            return f.getMatching(pattern);
        }
        return null;
    }
    
    /**
     * Gets an iterator for the values in a given range in a field.
     *
     * @param name The name of the field we need an iterator for.
     * @param caseSensitive If true, case should be taken into account when
     * iterating through the values.  This value will only be observed for
     * character fields!
     * @param lowerBound The lower bound on the iterator.  If
     * <code>null</code>, only the upper bound is considered and the
     * iteration will commence with the first term in the dictionary.
     * @param includeLower If <code>true</code>, then the lower bound will
     * be included in the terms returned by the iterator, if it occurs in
     * the dictionary.
     * @param upperBound The upper bound on the iterator.  If
     * <code>null</code>, only the lower bound is considered and the
     * iteration will end at the last term in the dictionary.
     * @param includeUpper If <code>true</code>, then the upper bound will
     * be included in the terms returned by the iterator, if it occurs in
     * the dictionary.
     * @return An iterator for the dictionary entries contained in the
     * range, or <code>null</code> if there is no such range or the named
     * field is not a saved field.
     */
    public DictionaryIterator getFieldIterator(String name,
            boolean caseSensitive,
            Object lowerBound,
            boolean includeLower,
            Object upperBound,
            boolean includeUpper) {
        SavedField f = getSavedField(name);
        if(f == null) {
            return null;
        }
        
        return f.iterator(lowerBound, includeLower,
                upperBound, includeUpper);
    }
    
    /**
     * Gets an iterator for the character saved field values that match a
     * given wildcard pattern.
     *
     * @param name The name of the field whose values we wish to match
     * against.
     * @param val The wildcard value against which we will match.
     * @param caseSensitive If <code>true</code>, then case will be taken
     * into account during the match.
     * @param maxEntries The maximum number of entries to return.  If zero or
     * negative, return all possible entries.
     * @param timeLimit The maximum amount of time (in milliseconds) to
     * spend trying to find matches.  If zero or negative, no time limit is
     * imposed.
     */
    public DictionaryIterator getMatchingIterator(String name,
            String val,
            boolean caseSensitive,
            int maxEntries,
            long timeLimit) {
        BasicField f = (BasicField) getSavedField(name);
        if(f == null) {
            return null;
        }
        
        if(f.getField().getType() != FieldInfo.Type.STRING) {
            log.warn(logTag, 3,
                    "MATCHES is an invalid operator for a " +
                    "non-character field: " + name);
            return null;
        }
        
        QueryEntry[] ret = ((DiskDictionary) f.values)
        .getMatching(f.bigrams, val, caseSensitive, maxEntries, timeLimit);
        return new ArrayDictionaryIterator((DiskDictionary) f.values, ret);
    }
    
    /**
     * Gets an iterator for the character saved field values that contain a
     * given substring.
     *
     * @param name The name of the field whose values we wish to match
     * against.
     * @param val The substring that we are looking for.
     * @param caseSensitive If <code>true</code>, then case will be taken
     * into account during the match.
     * @param starts If <code>true</code>, the value must start with the
     * given substring.
     * @param ends If <code>true</code>, the value must end with the given
     * substring.
     * @param maxEntries The maximum number of entries to return.  If zero or
     * negative, return all possible entries.
     * @param timeLimit The maximum amount of time (in milliseconds) to
     * spend trying to find matches.  If zero or negative, no time limit is
     * imposed.
     */
    public DictionaryIterator getSubstringIterator(String name,
            String val,
            boolean caseSensitive,
            boolean starts,
            boolean ends,
            int maxEntries,
            long timeLimit) {
        BasicField f = (BasicField) getSavedField(name);
        if(f == null) {
            return null;
        }
        
        if(f.getField().getType() != FieldInfo.Type.STRING) {
            log.warn(logTag, 3,
                    "MATCHES is an invalid operator for a " +
                    "non-character field: " + name);
            return null;
        }
        
        QueryEntry[] ret = ((DiskDictionary) f.values)
        .getSubstring(f.bigrams, val, caseSensitive,
                starts, ends, maxEntries, timeLimit);
        return new ArrayDictionaryIterator((DiskDictionary) f.values, ret);
    }
    
    /**
     * Gets the postings associated with a particular field value.
     *
     * @param name The name of the field for which we want postings.
     * @param value The value from the field for which we want postings.
     * @param caseSensitive If true, case should be taken into account when
     * iterating through the values.  This value will only be observed for
     * character fields!
     * @return The postings associated with that value, or
     * <code>null</code> if there is no such value in the field.
     */
    public PostingsIterator getFieldPostings(String name,
            Object value,
            boolean caseSensitive) {
        SavedField f = getSavedField(name);
        if(f == null) {
            return null;
        }
        
        QueryEntry e = f.get(value, caseSensitive);
        
        if(e == null) {
            
            //
            // If there's no such entry, then we're done.
            return null;
        }
        
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures();
        feat.setCaseSensitive(caseSensitive);
        PostingsIterator pi = e.iterator(feat);
        return pi;
    }
    
    /**
     * Merges a number of field stores into a single store.
     *
     * @param stores the field stores to merge.
     * @param maxID The maximum document ID in the merged partition.
     * @param starts The new starting document IDs for the partitions.
     * @param nUndel The number of documents that are not deleted in each of the partitionsS
     * @param dictFile The file where the merged dictionaries will be written.
     * @param postOut The output where the merged postings will be written.
     * @param docIDMaps The maps from old to new document IDs.
     * @throws java.io.IOException when there is an error writing the
     * file.
     */
    public void merge(DiskFieldStore[] stores,
            int maxID,
            int[] starts,
            int[] nUndel,
            int[][] docIDMaps,
            RandomAccessFile dictFile,
            PostingsOutput postOut)
            throws java.io.IOException {
        
        //
        // Make a header for the merged dictionary.
        FieldStoreHeader fsh = new FieldStoreHeader(metaFile.size());
        long offsetPos = dictFile.getFilePointer();
        fsh.write(dictFile.getChannel());
        
        //
        // Figure out which of the fields have data that needs to be
        // merged.
        for(int i = 1; i <= fsh.nFields; i++) {
            boolean atLeastOne = false;
            SavedField[] fields = new SavedField[stores.length];
            SavedField merger = null;
            for(int j = 0; j < stores.length; j++) {
                if(i < stores[j].savedFields.length &&
                        stores[j].savedFields[i] != null) {
                    fields[j] = stores[j].savedFields[i];
                    merger = fields[j];
                    atLeastOne = true;
                }
            }
            
            //
            // If we had at least one saved field at this position, go
            // ahead and do the merge.
            if(atLeastOne) {
                fsh.addOffset(i, dictFile.getFilePointer());
                merger.merge(part.getManager().getIndexDir(),
                             fields, maxID,
                             starts, nUndel, docIDMaps,
                             dictFile, postOut);
            }
        }
        
        //
        // We used relative puts, so we need to make sure the position is
        // right for writing out the data.  So, position to the end of the
        // buffer of saved field dictionary offsets and then write them.
        dictFile.seek(offsetPos);
        fsh.write(dictFile.getChannel());
    }
    
    /**
     * Gets a map from saved field names to the saved field values for those
     * fields.
     */
    public Map<String,List> getSavedFields(int docID) {
        Map<String,List> ret = new LinkedHashMap<String,List>();
        for(int i = 0; i < savedFields.length; i++) {
            if(savedFields[i] == null) {
                continue;
            }
            
            FieldInfo fi = metaFile.getFieldInfo(i);
            ret.put(fi.getName(), (List) getSavedFieldData(fi, docID, true));
        }
        return ret;
    }
    
} // DiskFieldStore
