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
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.Partition;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.util.props.ConfigComponent;
import com.sun.labs.util.props.ConfigEnum;
import com.sun.labs.util.props.ConfigInteger;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

/**
 *
 */
public class DictionaryFactory implements Configurable {

    static final Logger logger = Logger.getLogger(DictionaryFactory.class.getName());

    public static final String PROP_ENTRY_FACTORY = "entryFactory";

    @ConfigComponent(type=com.sun.labs.minion.indexer.entry.EntryFactory.class, mandatory=false)
    private EntryFactory factory;

    /**
     * The size of the entry cache
     */
    @ConfigInteger(defaultValue = 1024)
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
    @ConfigEnum(type=com.sun.labs.minion.indexer.dictionary.DiskDictionary.PostingsInputType.class, defaultValue = "CHANNEL_FULL_POST")
    public static String PROP_POSTINGS_INPUT = "postings_input";

    protected DiskDictionary.PostingsInputType postingsInputType;

    /**
     * The type of file buffers to use when reading dictionary data.
     */
    @ConfigEnum(type=com.sun.labs.minion.indexer.dictionary.DiskDictionary.BufferType.class, defaultValue="NIOFILEBUFFER")
    public static final String PROP_FILE_BUFFER_TYPE = "file_buffer_type";

    private DiskDictionary.BufferType fileBufferType;

    /**
     * Creates a DictionaryFactory
     */
    public DictionaryFactory() {
    }

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {

        factory = (EntryFactory) ps.getComponent(PROP_ENTRY_FACTORY);
        if(factory == null) {
            factory = new EntryFactory(Postings.Type.ID_FREQ);
        }
        cacheSize = ps.getInt(PROP_CACHE_SIZE);
        postingsInputType = (PostingsInputType) ps.getEnum(PROP_POSTINGS_INPUT);
        nameBufferSize = ps.getInt(PROP_NAME_BUFFER_SIZE);
        offsetsBufferSize = ps.getInt(PROP_OFFSETS_BUFFER_SIZE);
        infoBufferSize = ps.getInt(PROP_INFO_BUFFER_SIZE);
        infoOffsetsBufferSize = ps.getInt(PROP_INFO_OFFSETS_BUFFER_SIZE);
        fileBufferType = (BufferType) ps.getEnum(PROP_FILE_BUFFER_TYPE);
    }

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
    public DiskDictionary getDiskDictionary(EntryFactory factory,
            NameDecoder decoder, RandomAccessFile dictFile,
            RandomAccessFile[] postFiles,
            DiskPartition part) throws IOException {
        DictionaryHeader dh = getDictHeader(dictFile);
        if(dh.size <= cacheSize) {
            return new CachedDiskDictionary(factory, decoder, dictFile,
                    postFiles, postingsInputType, fileBufferType, part);
        }
        return new DiskDictionary(factory, decoder, dictFile, postFiles,
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

        //
        // If we have a big cache and a small dictionary, then just read the
        // darned thing.
        DictionaryHeader dh = getDictHeader(dictFile);
        if(dh.size <= cacheSize) {
            return new CachedDiskDictionary(factory, decoder, dictFile,
                    postFiles, postingsInputType, fileBufferType, part);
        }

        //
        // Normal disk-based dictionary.
        return new DiskDictionary(factory, decoder, dictFile, postFiles,
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
        return new CachedDiskDictionary(factory, decoder, dictFile, postFiles);
    }

    public MemoryDictionary getMemoryDictionary(Partition p) {
        MemoryDictionary ret = new MemoryDictionary(factory);
        ret.setPartition(p);
        return ret;
    }

    private DictionaryHeader getDictHeader(RandomAccessFile dictFile) throws IOException {
        long pos = dictFile.getFilePointer();
        DictionaryHeader dh = new DictionaryHeader(dictFile);
        dictFile.seek(pos);
        return dh;
    }
}
