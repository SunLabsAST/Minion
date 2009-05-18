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

import com.sun.labs.minion.indexer.dictionary.DiskDictionary.BufferType;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary.PostingsInputType;
import com.sun.labs.util.props.ConfigInteger;
import com.sun.labs.util.props.ConfigString;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.io.IOException;
import java.io.RandomAccessFile;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.Partition;
import com.sun.labs.util.props.ConfigEnum;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class DictionaryFactory implements Configurable {

    /**
     * Creates a DictionaryFactory
     */
    public DictionaryFactory() {
    }
    Logger logger = Logger.getLogger(getClass().getName());

    protected static String logTag = "DF";

    private boolean isCased;

    public void newProperties(PropertySheet ps) throws PropertyException {
        entryClassName =
                ps.getString(PROP_ENTRY_CLASS_NAME);
        try {
            entryClass = Class.forName(entryClassName);

            //
            // At this point we'll see whether this entry class is a cased entry
            // type so that we can tell people later on without having to do
            // an instantiation every time someone runs a query.
            isCased =
                    entryClass.newInstance() instanceof com.sun.labs.minion.indexer.entry.CasedEntry;
        } catch(ClassNotFoundException ex) {
            throw new PropertyException(ps.getInstanceName(),
                    PROP_ENTRY_CLASS_NAME,
                    "Cannot find entry class: " +
                    entryClassName);
        } catch(InstantiationException ie) {
            throw new PropertyException(ps.getInstanceName(),
                    PROP_ENTRY_CLASS_NAME,
                    "Cannot instantiate entry class: " +
                    entryClassName);
        } catch(IllegalAccessException iae) {
            throw new PropertyException(ps.getInstanceName(),
                    PROP_ENTRY_CLASS_NAME,
                    "Illegal access.  Cannot instantiate entry class: " +
                    entryClassName);
        }
        cacheSize = ps.getInt(PROP_CACHE_SIZE);
        postingsInputType = (PostingsInputType) ps.getEnum(PROP_POSTINGS_INPUT);
        nameBufferSize = ps.getInt(PROP_NAME_BUFFER_SIZE);
        offsetsBufferSize = ps.getInt(PROP_OFFSETS_BUFFER_SIZE);
        infoBufferSize = ps.getInt(PROP_INFO_BUFFER_SIZE);
        infoOffsetsBufferSize = ps.getInt(PROP_INFO_OFFSETS_BUFFER_SIZE);
        fileBufferType = (BufferType) ps.getEnum(PROP_FILE_BUFFER_TYPE);
    }
    
    @ConfigString(defaultValue = "com.sun.labs.minion.indexer.entry.IDEntry")
    public static final String PROP_ENTRY_CLASS_NAME = "entry_class_name";

    private String entryClassName;

    protected Class entryClass;

    public String getEntryClassName() {
        return entryClassName;
    }

    public void setEntryClassName(String entryClassName) {
        this.entryClassName = entryClassName;
    }

    public Class getEntryClass() {
        return entryClass;
    }

    public void setEntryClass(Class entryClass) {
        this.entryClass = entryClass;
    }

    public int getNumPostingsChannels() {
        try {
            return ((Entry) entryClass.newInstance()).getNumChannels();
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Error instantiating main entry", e);
            return 1;
        }
    }

    /**
     * Indicates whether the entry type used for this dictionary is cased.
     *
     * @return <code>true</code> if the entry type for this dictionary is a
     * subclass of {@link com.sun.labs.minion.indexer.entry.CasedEntry}, <code>false</code>
     * otherwise.
     */
    public boolean hasCasedEntry() {
        return isCased;
    }
    /**
     * The size of the entry cache
     */
    @ConfigInteger(defaultValue = 256)
    public static String PROP_CACHE_SIZE = "cache_size";

    protected int cacheSize;

    /**
     * The size of the buffer to use for entry information.
     */
    @ConfigInteger(defaultValue = 1024)
    public static String PROP_INFO_BUFFER_SIZE = "info_buffer_size";

    protected int infoBufferSize;

    /**
     * The size of the buffer to use for the offsets to the entry information.
     */
    @ConfigInteger(defaultValue = 1024)
    public static String PROP_INFO_OFFSETS_BUFFER_SIZE =
            "info_offsets_buffer_size";

    protected int infoOffsetsBufferSize;

    /**
     * The size of the buffer to use for the entry names in the dictionary.
     */
    @ConfigInteger(defaultValue = 2048)
    public static String PROP_NAME_BUFFER_SIZE = "name_buffer_size";

    protected int nameBufferSize;

    /**
     * The size of the buffer to use for the offsets into the entry names.
     */
    @ConfigInteger(defaultValue = 1024)
    public static String PROP_OFFSETS_BUFFER_SIZE = "offsets_buffer_size";

    protected int offsetsBufferSize;

    /**
     * The type of postings input stream to use when reading postings.
     */
    @ConfigEnum(type=com.sun.labs.minion.indexer.dictionary.DiskDictionary.PostingsInputType.class, defaultValue = "FILE_FULL_POST")
    public static String PROP_POSTINGS_INPUT = "postings_input";

    protected DiskDictionary.PostingsInputType postingsInputType;

    /**
     * The type of file buffers to use when reading dictionary data.
     */
    @ConfigEnum(type=com.sun.labs.minion.indexer.dictionary.DiskDictionary.BufferType.class, defaultValue="FILEBUFFER")
    public static final String PROP_FILE_BUFFER_TYPE = "file_buffer_type";

    private DiskDictionary.BufferType fileBufferType;

    /**
     * Gets a bigram dictionary.
     */
    public DiskBiGramDictionary getBiGramDictionary(
            DiskDictionary mainDict,
            RandomAccessFile dictFile,
            RandomAccessFile postFile,
            DiskPartition part) throws IOException {
        return new DiskBiGramDictionary(dictFile, postFile, 
                postingsInputType,
                fileBufferType,
                cacheSize, nameBufferSize, offsetsBufferSize, infoBufferSize,
                infoOffsetsBufferSize, part, mainDict);
    }

    /**
     * Gets a disk dictionary that's configured according to the configuration, except it
     * uses a different entry type.
     * @param entryClass the class of entry that the dictionary contains, which
     * overrides the configured value.
     * @param decoder A decoder for the names in this dictionary.
     * @param dictFile The file containing the dictionary.
     * @param postFiles The files containing the postings associated with
     * the entries in this dictionary.
     * @param part The partition with which this dictionary is associated.
     * @throws java.io.IOException if there is any error loading the dictionary
     */
    public DiskDictionary getDiskDictionary(Class entryClass,
            NameDecoder decoder, RandomAccessFile dictFile,
            RandomAccessFile[] postFiles,
            DiskPartition part) throws IOException {
        return new DiskDictionary(entryClass, decoder, dictFile, postFiles,
                postingsInputType, fileBufferType,
                cacheSize, nameBufferSize, offsetsBufferSize,
                infoBufferSize, infoOffsetsBufferSize, part);
    }

    /**
     * Gets a disk dictionary that's configured according to the configuration.
     * @param decoder A decoder for the names in this dictionary.
     * @param dictFile The file containing the dictionary.
     * @param postFiles The files containing the postings associated with
     * the entries in this dictionary.
     * @param part The partition with which this dictionary is associated.
     * @throws java.io.IOException if there is any error loading the dictionary
     */
    public DiskDictionary getDiskDictionary(NameDecoder decoder,
            RandomAccessFile dictFile, RandomAccessFile[] postFiles,
            DiskPartition part) throws IOException {
        return new DiskDictionary(entryClass, decoder, dictFile, postFiles,
                postingsInputType, fileBufferType,
                cacheSize, nameBufferSize, offsetsBufferSize,
                infoBufferSize, infoOffsetsBufferSize, part);
    }

    /**
     * Gets a cached dictionary that's configured according to the configuration.
     */
    public DiskDictionary getCachedDiskDictionary(NameDecoder decoder,
            RandomAccessFile dictFile, RandomAccessFile[] postFiles,
            DiskPartition part) throws IOException {
        return new CachedDiskDictionary(entryClass, decoder, dictFile, postFiles);
    }

    public MemoryDictionary getMemoryDictionary(Partition p) {
        MemoryDictionary ret =
                new MemoryDictionary(entryClass);
        ret.setPartition(p);
        return ret;
    }
}
