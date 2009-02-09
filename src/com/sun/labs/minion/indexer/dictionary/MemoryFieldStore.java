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

import com.sun.labs.minion.SearchEngineException;
import java.io.RandomAccessFile;


import com.sun.labs.minion.util.Stack;
import com.sun.labs.minion.util.Util;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.classification.ClassificationResult;
import com.sun.labs.minion.indexer.MetaFile;


import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import java.util.logging.Level;

/**
 * A field store to be used during indexing.
 */
public class MemoryFieldStore extends FieldStore {

    /**
     * The count of currently active fields.
     */
    protected int nActive;

    /**
     * A set of active fields.
     */
    protected int[] activeFields;

    /**
     * A stack of the fields that we're processing.
     */
    protected Stack fieldStack;

    /**
     * The ID of the current document that we're processing.
     */
    protected int currDoc;

    /**
     * Whether we're in a document.  Used to catch the case when we start a
     * new document before ending the old one.
     */
    protected boolean inDocument;

    /**
     * A boolean indicating whether words should be indexed or not.
     */
    protected boolean shouldIndex;

    /**
     * A boolean indicating whether words should be added to the document
     * vector or not.
     */
    protected boolean shouldVector;

    /**
     * The tag for this module.
     */
    protected static String logTag = "MFS";

    /**
     * Constructs the field store for use.
     */
    public MemoryFieldStore(MetaFile f) {
        metaFile = f;
        fieldStack = new Stack();
        savedFields = new SavedField[metaFile.size()];
        activeFields = new int[Math.max(metaFile.size(), 1)];
    }

    /**
     * Defines a field, given a field information object.
     * @param fi the information for the field that we want to define
     * @return the field information for the field that we want to define
     */
    public FieldInfo defineField(FieldInfo fi) {
        try {
            return metaFile.defineField(fi);
        } catch(SearchEngineException ex) {
            logger.log(Level.SEVERE, "Error defining field: " + fi, ex);
            return null;
        }
    }

    /**
     * Gets the active fields list.
     */
    public int[] getActiveFields() {
        return activeFields;
    }

    /**
     * Tells the field store that a new document has been started.  This
     * will flush any unsaved data.
     */
    public void startDocument(int docID) {
        currDoc = docID;
        if(inDocument) {
            endDocument();
        }
        inDocument = true;
        shouldIndex = true;
        shouldVector = true;
        activeFields[0] = 1;
    }

    /**
     * Tells the field store that a particular field has started.
     *
     * @param f The <code>FieldInfo</code> object for the field that is
     * starting.
     * @return the fieldID for this field.
     */
    public int startField(FieldInfo f) {

        //
        // Define the field, which will do nothing if everything is OK.
        FieldInfo fi = metaFile.getFieldInfo(f.getName());
        if(fi == null) {
            try {
                fi = metaFile.defineField(f);
            } catch(SearchEngineException ex) {
                logger.log(Level.SEVERE,
                        "Error defining field at startField: " + f, ex);
            }
        }

        //
        // See whether we need to extend our array of currently active fields.
        if(fi.getID() >= activeFields.length) {
            activeFields = Util.expandInt(activeFields, fi.getID() + 1);
        }


        //
        // See whether we should add tokens to the document vector.
        if(!fi.isVectored()) {
            shouldVector = false;
        }

        //
        // See whether we should index.
        if(!fi.isIndexed()) {
            shouldIndex = false;
        } else {

            //
            // Keep track of the number of active fields.
            if(activeFields[fi.getID()] == 0) {
                nActive++;
                activeFields[0] = 0;
            }
            activeFields[fi.getID()]++;
            shouldIndex = true;
        }

        //
        // Put this field on our stack.
        fieldStack.push(fi);

        return fi.getID();
    }

    /**
     * Saves the given data in the current field.
     */
    public void saveData(int docID, Object data) {
        FieldInfo fi = (FieldInfo) fieldStack.peek();
        saveData(fi, docID, data);
    }

    public void saveData(FieldInfo cfi, FieldInfo sfi, ClassificationResult r) {
        if(!cfi.isSaved() || (sfi != null && !sfi.isSaved())) {
            return;
        }
        SavedField csf = getSavedField(cfi);
        SavedField ssf = sfi == null ? null : getSavedField(sfi);
        for(ClassificationResult.DocResult dr : r.getResults()) {
            csf.add(dr.getID(), dr.getValue());
            if(ssf != null) {
                ssf.add(dr.getID(), dr.getScore());
            }
        }
    }

    public void saveData(FieldInfo fi, int docID, Object data) {
        if(fi.isSaved()) {
            SavedField sf = getSavedField(fi);
            sf.add(docID, data);
        }

    }

    public SavedField getSavedField(FieldInfo fi) {
        if(fi.isSaved()) {
            if(fi.getID() >= savedFields.length) {
                SavedField[] temp = new SavedField[fi.getID() * 2];
                System.arraycopy(savedFields, 0, temp,
                        0, savedFields.length);
                savedFields = temp;
            }

            if(savedFields[fi.getID()] == null) {
                savedFields[fi.getID()] = makeSavedField(fi);
            }
            return savedFields[fi.getID()];

        }
        return null;
    }

    /**
     * Creates a saved field entry based on the type of the field.
     */
    protected SavedField makeSavedField(FieldInfo fi) {
        if(fi.getType() != FieldInfo.Type.FEATURE_VECTOR) {
            return new BasicField(fi);
        } else {
            return new FeatureVector(fi);
        }
    }

    /**
     * Tells the field store that a field has ended.
     */
    public void endField() {

        FieldInfo fi = (FieldInfo) fieldStack.pop();

        if(fi == null) {
            logger.warning("Empty field stack at endField");
            shouldIndex = true;
            shouldVector = true;
            return;
        }

        //
        // Remove this field from the indexed fields markers.
        if(fi.isIndexed()) {
            activeFields[fi.getID()]--;

            //
            // Keep track of the number of active fields.
            if(activeFields[fi.getID()] == 0) {
                nActive--;
                if(nActive == 0) {
                    activeFields[0] = 1;
                }
            }
        }

        //
        // See whether we should be indexing the next field down.
        fi = (FieldInfo) fieldStack.peek();
        shouldIndex = fi == null || fi.isIndexed();
        shouldVector = fi == null || fi.isVectored();
    }

    /**
     * Ends the document.  Will flush the field stack completely, that is,
     * it will appear that any open fields ended at the document end.  If
     * any fields are open, we will log a warning for each open field.
     */
    public void endDocument() {

        while(!fieldStack.empty()) {
            endField();
        }
        shouldIndex = true;
        shouldVector = true;
        inDocument = false;
    }

    /**
     * Dump the field store to disk.  This mostly requires dumping the
     * saved fields.
     *
     * @param path The path to the directory where the field store will be
     * written.
     * @param dictFile The file to which the field dictionaries will be
     * written.
     * @param postOut The outputs to which the field postings will be
     * written.
     * @throws java.io.IOException if there is an error during writing.
     */
    public void dump(String path,
            RandomAccessFile dictFile,
            PostingsOutput[] postOut)
            throws java.io.IOException {

        //
        // Finish any document that we're currently in.
        if(inDocument) {
            endDocument();
        }

        //
        // Make a header for the field store, and write it out, remembering
        // where we wrote it!
        FieldStoreHeader fsh = new FieldStoreHeader(metaFile.size());
        long offsetPos = dictFile.getFilePointer();
        fsh.write(dictFile.getChannel());

        for(int i = 0; i < savedFields.length; i++) {

            //
            // If we have a saved field, and that field has some data
            // stored in it, then go ahead and dump it.
            if(savedFields[i] != null && savedFields[i].size() > 0) {

                //
                // Store the offset of the dictionary and then dump the
                // field data.
                fsh.addOffset(i, dictFile.getFilePointer());
                savedFields[i].dump(path, dictFile, postOut, currDoc);
            }
        }

        //
        // Write the filled in header.
        dictFile.seek(offsetPos);
        fsh.write(dictFile.getChannel());
    }

    /**
     * Clears the saved fields for the next indexing run.
     */
    public void clear() {
        for(int i = 0; i < savedFields.length; i++) {
            if(savedFields[i] != null) {
                savedFields[i].clear();
            }
        }
    }

    /**
     * A boolean indicating whether words should be indexed or not.
     */
    public boolean shouldIndex() {
        return shouldIndex;
    }

    /**
     * Indicates whether a field should contribute tokens to the document
     * vector.
     */
    public boolean shouldVector() {
        return shouldVector;
    }
} // MemoryFieldStore
