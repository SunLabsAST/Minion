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

import java.io.RandomAccessFile;
import java.io.File;

import java.nio.channels.FileChannel;

import com.sun.labs.minion.indexer.entry.IndexEntry;

import com.sun.labs.minion.indexer.partition.PartitionStats;

import com.sun.labs.minion.util.Util;

import com.sun.labs.minion.util.buffer.FileWriteableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.util.logging.Logger;

/**
 * A class that will write a dictionary to a file.  This can be used when
 * dumping or merging dictionaries.
 */
public class DictionaryWriter {

    /**
     * A header for the dictionary we're writing.
     */
    protected DictionaryHeader dh;

    /**
     * An encoder for the names in our dictionary.
     */
    protected NameEncoder encoder;

    /**
     * A set of partition statistics for the partition who's dictionaries
     * we're dumping/merging.  This may be <code>null</code>.
     */
    protected PartitionStats partStats;

    /**
     * The name of the previous entry added to the merged dictionary.
     */
    protected Object prevName;

    /**
     * A buffer to hold names.
     */
    protected WriteableBuffer names;

    /**
     * A buffer to hold the offsets of the uncompressed names in the
     * merged dictionary.
     */
    protected WriteableBuffer nameOffsets;

    /**
     * A buffer to hold term information.
     */
    protected WriteableBuffer info;

    /**
     * A buffer to hold term information offsets.
     */
    protected WriteableBuffer infoOffsets;

    /**
     * The number of offsets that we've encoded.
     */
    protected int nOffsets;

    /**
     * A file to hold the temporary names buffer.
     */
    protected File namesFile;

    /**
     * Random access file for the temporary names buffer.
     */
    protected RandomAccessFile namesRAF;

    /**
     * A file to hold the temporary name offsets buffer.
     */
    protected File nameOffsetsFile;

    /**
     * Random access file for the temporary names offsets buffer.
     */
    protected RandomAccessFile nameOffsetsRAF;

    /**
     * A file to hold the temporary info buffer.
     */
    protected File infoFile;

    /**
     * Random access file for the temporary names buffer.
     */
    protected RandomAccessFile infoRAF;

    /**
     * A file to hold the temporary name offsets buffer.
     */
    protected File infoOffsetsFile;

    /**
     * Random access file for the temporary names offsets buffer.
     */
    protected RandomAccessFile infoOffsetsRAF;

    /**
     * A map from ID to position in the dictionary.
     */
    protected int[] idToPosn;

    static Logger logger = Logger.getLogger(DictionaryWriter.class.getName());

    protected static String logTag = "DW";

    protected static int OUT_BUFFER_SIZE = 16 * 1024;

    /**
     * Creates a dictionary writer that will write data to disk.
     *
     * @param path The path where the temporary files should be written.
     * @param encoder An encoder for the names of the entries.
     * @param partStats the set of stats for this partition
     * @param nChans The number of postings channels used by the
     * dictionary.
     * @param renumber A flag indicating how entries in the dictionary were
     * renumbered during sorting.  We only care about {@link
     * MemoryDictionary.Renumber#NONE}, value which inidicates to us that we
     * need to keep a map from entry ID to position in the dictionary.
     * @throws java.io.IOException if there was an error writing to disk
     */
    public DictionaryWriter(String path,
            NameEncoder encoder,
            PartitionStats partStats,
            int nChans,
            MemoryDictionary.Renumber renumber)
            throws java.io.IOException {

        this.encoder = encoder;
        this.partStats = partStats;
        dh = new DictionaryHeader(nChans);

        //
        // Create the files to which we'll write our data.  We'll base the
        // name on the path and the name of our thread.
        String tname = Thread.currentThread().getName();

        File pf = new File(path);

        namesFile = Util.getTempFile(pf, "names", ".n");
        namesRAF = new RandomAccessFile(namesFile, "rw");
        names = new FileWriteableBuffer(namesRAF, OUT_BUFFER_SIZE);

        nameOffsetsFile = Util.getTempFile(pf, "offsets", ".no");
        nameOffsetsRAF = new RandomAccessFile(nameOffsetsFile, "rw");
        nameOffsets = new FileWriteableBuffer(nameOffsetsRAF, OUT_BUFFER_SIZE);

        infoFile = Util.getTempFile(pf, "info", ".i");
        infoRAF = new RandomAccessFile(infoFile, "rw");
        info = new FileWriteableBuffer(infoRAF, OUT_BUFFER_SIZE);

        infoOffsetsFile = Util.getTempFile(pf, "infooff", ".io");
        infoOffsetsRAF = new RandomAccessFile(infoOffsetsFile, "rw");
        infoOffsets = new FileWriteableBuffer(infoOffsetsRAF, OUT_BUFFER_SIZE);

        //
        // If we're not going to renumber the entries in the dictionary, then
        // we need to keep a map from ID to position in the dictionary so that
        // we can easily look up entries by ID.
        if(renumber == MemoryDictionary.Renumber.NONE) {
            idToPosn = new int[1024];
        }

    } // DictionaryWriter constructor

    /**
     * Writes an entry to the dictionary.
     */
    public void write(IndexEntry e) {

        //
        // See if this a entry that should be uncompressed.  If so, we need
        // to record the position.
        if(dh.size % 4 == 0) {
            nameOffsets.byteEncode(names.position(),
                    dh.nameOffsetsBytes);
            nOffsets++;
            prevName = null;
        }

        //
        // Encode the name.
        encoder.encodeName(prevName, e.getName(), names);

        //
        // Encode the entry information, first taking note of where
        // this information is being encoded.
        infoOffsets.byteEncode(info.position(),
                dh.entryInfoOffsetsBytes);
        e.encodePostingsInfo(info);

        //
        // Keep the ID to position map up to date, if necessary.
        if(idToPosn != null) {
            if(e.getID() >= idToPosn.length) {
                idToPosn = Util.expandInt(idToPosn,
                        Math.max(e.getID() * 2,
                        idToPosn.length * 2));
            }
            idToPosn[e.getID()] = dh.size;
        }

        //
        // If we're keeping track of partition stats for someone, then give
        // them this entry to mull over.
        if(partStats != null) {
            partStats.processEntry(e);
        }

        prevName = e.getName();

        //
        // The dictionary is now one bigger!
        dh.size++;
        dh.maxEntryID = Math.max(dh.maxEntryID, e.getID());
    }

    /**
     * Finishes by writing the dictionary to the given file.
     */
    public void finish(RandomAccessFile dictFile)
            throws java.io.IOException {

        //
        // Write our header.
        long hPos = dictFile.getFilePointer();
        dh.write(dictFile);

        //
        // Make up our header.
        if(dh.maxEntryID == 0) {
            dh.maxEntryID = dh.size;
        }

        dh.computeValues();

        //
        // If we need to write our ID to position map, do it now.
        if(idToPosn != null) {

            //
            // First we need to recode the data, given that we know the
            // size of the merged dictionary.
            dh.idToPosnPos = dictFile.getFilePointer();
            FileWriteableBuffer temp =
                    new FileWriteableBuffer(dictFile, OUT_BUFFER_SIZE);
            for(int i = 0; i <= dh.maxEntryID; i++) {
                temp.byteEncode(idToPosn[i], dh.idToPosnBytes);
            }
            dh.idToPosnSize = temp.position();
            temp.flush();
        } else {
            dh.idToPosnSize = -1;
        }

        FileChannel dictChan = dictFile.getChannel();

        //
        // Write the names to the output.
        dh.namesPos = dictFile.getFilePointer();
        dh.namesSize = names.position();
        names.write(dictChan);

        //
        // Write the name offsets to the output.
        dh.nameOffsetsPos = dictFile.getFilePointer();
        dh.nameOffsetsSize = nameOffsets.position();
        nameOffsets.write(dictChan);

        //
        // Write the entry information to the output.
        dh.entryInfoPos = dictFile.getFilePointer();
        dh.entryInfoSize = info.position();
        info.write(dictChan);

        //
        // Write the entry information offsets to the output.
        dh.entryInfoOffsetsPos = dictFile.getFilePointer();
        dh.entryInfoOffsetsSize = infoOffsets.position();
        infoOffsets.write(dictChan);

        //
        // OK, zip back, write the header and then come back here.
        long end = dictFile.getFilePointer();
        dictFile.seek(hPos);
        dh.goodMagic();
        dh.write(dictFile);
        dictFile.seek(end);

        //
        // Close and delete the temporary files.
        namesRAF.close();
        if(!namesFile.delete()) {
            logger.severe("Failed to delete temporary namesFile");
        }
        nameOffsetsRAF.close();
        if(!nameOffsetsFile.delete()) {
            logger.severe("Failed to delete temporary nameOffsetsFile");
        }
        infoRAF.close();
        if(!infoFile.delete()) {
            logger.severe("Failed to delete temporary infoFile");
        }
        infoOffsetsRAF.close();
        if(!infoOffsetsFile.delete()) {
            logger.severe("Failed to delete temporary infoOffsetsFile");
        }
    }
} // DictionaryWriter
