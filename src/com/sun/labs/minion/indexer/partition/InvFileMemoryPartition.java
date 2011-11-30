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
import com.sun.labs.minion.Indexable;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.MemoryField;
import com.sun.labs.minion.indexer.dictionary.TermStatsHeader;
import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.partition.io.PartitionOutput;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.pipeline.Token;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A "main" partition that holds indexed and saved data for the fields that
 * occur in documents.
 */
public class InvFileMemoryPartition extends MemoryPartition {

    private static final Logger logger = Logger.getLogger(
            InvFileMemoryPartition.class.getName());

    /**
     * The fields making up this partition.
     */
    private MemoryField[] fields;

    private IndexEntry dockey;
    
    public InvFileMemoryPartition() {
        super();
    }

    public InvFileMemoryPartition(PartitionManager manager) {
        super(manager, Postings.Type.NONE);
        fields = new MemoryField[16];
    }

    public void index(Indexable doc) {
        startDocument(doc.getKey());
        for(Map.Entry<String,Object> field : doc.getMap().entrySet()) {
            FieldInfo fi = manager.getFieldInfo(field.getKey());
            if(fi == null) {
                logger.warning(String.format("Unknown field: %s in %s", field.getKey(), doc.getKey()));
                continue;
            }
            addField(fi, field.getValue());
        }
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
        
        maxDocumentID = dockey.getID();

        for(MemoryField field : fields) {
            if(field != null) {
                field.startDocument(dockey);
            }
        }
    }

    public boolean isIndexed(String key) {
        return docDict.get(key) != null;
    }

    private MemoryField getMF(FieldInfo fi) {
        
        //
        // Non-field stuff goes into the 0th position.
        int fid = 0;
        if(fi != null) {
            fid = fi.getID();
        }

        if(fid >= fields.length) {
            fields = Arrays.copyOf(fields, fid * 2);
        }
        if(fields[fid] == null) {
            fields[fid] = new MemoryField(this, fi, new EntryFactory(
                    Postings.Type.ID_FREQ));
            
            //
            // We might have just started this document!
            fields[fid].startDocument(dockey);
        }
        return fields[fid];
    }
    
    public void addField(String field, Object val) {
        FieldInfo fi = manager.getFieldInfo(field);

        if(field != null && fi == null) {
            logger.warning(String.format("Can't add term to undefined field %s",
                                         field));
        }
        addField(fi, val);
    }

    /**
     * Adds a field to this document.
     */
    public void addField(FieldInfo fi, Object val) {
        MemoryField mf = getMF(fi);
        mf.addData(dockey.getID(), val);
    }

    /**
     * Adds a term to a single field in this document.
     */
    public void addTerm(String field, String term, int count) {
        FieldInfo fi = manager.getFieldInfo(field);
        if(field != null && fi == null) {
            logger.warning(String.format("Can't add term to undefined field %s", field));
        }
        MemoryField mf = getMF(fi);
        Token t = new Token(term, count);
        t.setID(dockey.getID());
        mf.token(t);
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
    protected void dumpCustom(PartitionOutput partOut) throws IOException {

        int tsn = 0;
        
        DictionaryOutput partDictOut = partOut.getPartitionDictionaryOutput();
        DictionaryOutput termStatsDictOut = null;
        try {
            termStatsDictOut = partOut.getTermStatsDictionaryOutput();
            termStatsDictOut.byteEncode(0, 8);
        } catch(Exception ex) {
            logger.log(Level.SEVERE, String.format(
                    "Error making term stats dictionary file for %s", this), ex);
        }
        
        //
        // Dump the fields.  Keep track of the offsets of the field and of the
        // offsets for the term statistics dictionaries for the fields.
        TermStatsHeader tsh = new TermStatsHeader();
        for(MemoryField mf : fields) {
            if(mf == null) {
                continue;
            }
            
            //
            // Remember where we are 
            long fieldOffset = partDictOut.position();
            long termStatsOff = termStatsDictOut.position();
            
            logger.fine(String.format("Dumping %s", mf.getInfo().getName()));
            
            //
            // Dump the dictionary and deal with the result.
            switch(mf.dump(partOut)) {
                case NOTHING_DUMPED:
                    logger.finer(String.format("No dicts dumped"));
                    fieldOffset = -1;
                    termStatsOff = -1;
                    break;
                case DICTS_DUMPED:
                    termStatsOff = -1;
                    break;
                case EVERYTHING_DUMPED:
                    break;
            }
            partOut.getPartitionHeader().addOffset(mf.getInfo().getID(), fieldOffset);
            tsh.addOffset(mf.getInfo().getID(), termStatsOff);
        }

        try {
            if(termStatsDictOut != null) {
                int tshpos = termStatsDictOut.position();
                tsh.write(termStatsDictOut);
                int end = termStatsDictOut.position();
                termStatsDictOut.position(0);
                termStatsDictOut.byteEncode(tshpos, 8);
                termStatsDictOut.position(end);
                
            }
        } catch(Exception ex) {
            logger.log(Level.SEVERE,
                       String.format("Error setting term stats %d", tsn), ex);
        }
    }
    
    public void clear() {
        super.clear();
        for(MemoryField mf : fields) {
            if(mf != null) {
                mf.clear();
            }
        }
    }
}
