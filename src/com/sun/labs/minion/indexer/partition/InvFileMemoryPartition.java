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
package com.sun.labs.minion.indexer.partition;

import java.io.IOException;
import java.io.RandomAccessFile;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.MemoryField;
import com.sun.labs.minion.indexer.postings.Postings;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A "main" partition that holds indexed and saved data for the fields that
 * occur in documents.
 */
public class InvFileMemoryPartition extends MemoryPartition {

    private static Logger logger = Logger.getLogger(
            InvFileMemoryPartition.class.getName());

    /**
     * The fields making up this partition.
     */
    private MemoryField[] fields;

    private IndexEntry dockey;

    public InvFileMemoryPartition(PartitionManager manager) {
        super(manager, Postings.Type.NONE);
        fields = new MemoryField[16];
    }

    /**
     * Starts a new document in this partition.
     *
     * @param key The key for this document, which is added to the
     * dictionary.
     */
    public void startDocument(String key) {

        IndexEntry old = docDict.remove(key);
        if(old != null) {
            deletions.delete(old.getID());
        }
        dockey = docDict.put(key);

        for(MemoryField field : fields) {
            field.startDocument(key);
        }
    }

    /**
     * Adds a field to this document.
     */
    public void addField(FieldInfo fi, Object val) {

        //
        // Non-field stuff goes into the 0th position.
        int fid = 0;
        if(fi != null) {
            fid = fi.getID();
        }

        if(fid >= fields.length) {
            MemoryField[] temp = new MemoryField[fid * 2];
            System.arraycopy(fields, 0, temp, 0, fields.length);
            fields = temp;
        }
        if(fields[fid] == null) {
            fields[fid] = new MemoryField(fi, null);
        }

        fields[fid].addData(dockey.getID(), val);
    }

    /**
     * Gets the number of documents in this partition.
     *
     * @return the number of documents indexed into this partition.
     */
    public int getNDocs() {
        return docDict.size();
    }

    @Override
    protected void dumpCustom(String indexDir,
                              int partNumber,
                              PartitionHeader ph,
                              RandomAccessFile dictFile,
                              PostingsOutput[] postOut) throws IOException {

        RandomAccessFile tsRAF = null;
        int tsn = 0;
        try {
            tsn = manager.getMetaFile().getNextTermStatsNumber();
            File ntsf = manager.makeTermStatsFile(tsn);
            tsRAF = new RandomAccessFile(ntsf, "rw");
        } catch(Exception ex) {
            logger.severe(String.format(
                    "Error making term stats dictionary file for %s", this));
        }

        File vlf = manager.makeVectorLengthFile(partNumber);
        RandomAccessFile vlRAF = new RandomAccessFile(vlf, "rw");

        //
        // Dump the fields.
        for(MemoryField mf : fields) {
            if(mf != null) {
                ph.addOffset(dictFile.getFilePointer());
            }
            mf.dump(indexDir, dictFile, postOut, tsRAF, vlRAF, maxDocumentID);
        }

        try {
            if(tsRAF != null) {
                tsRAF.close();
                manager.getMetaFile().setTermStatsNumber(tsn);
                manager.updateTermStats();
            }
        } catch(Exception ex) {
            logger.log(Level.SEVERE,
                       String.format("Error setting term stats %d", tsn), ex);
        }

        vlRAF.close();


    }
}
