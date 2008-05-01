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

import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import java.io.File;
import java.io.RandomAccessFile;

import com.sun.labs.minion.IndexConfig;
import com.sun.labs.minion.QueryConfig;
import com.sun.labs.util.props.ConfigComponent;
import com.sun.labs.util.props.PropertySheet;
import com.sun.labs.minion.indexer.dictionary.DictionaryFactory;

import com.sun.labs.minion.util.MinionLog;

import com.sun.labs.minion.indexer.entry.Entry;

/**
 * A superclass for <code>Partition</code>s.  This is mostly used to
 * provide a bit of information that we want subclasses to have in common.
 */
public abstract class Partition implements Comparable<Partition>, Configurable {
    
    /**
     * The manager for this partition.
     */
    protected PartitionManager manager;
    
    /**
     * The statistics for this partition.
     */
    protected PartitionStats stats;
    
    /**
     * The name of the entries.
     */
    protected String entryName;
    
    /**
     * The class for the postings entries that we'll be building.
     */
    protected Class entryClass;
    
    /**
     * The number of this partition.
     */
    protected int partNumber;
    
    /**
     * The maximum ID that was allocated in the partition.
     */
    protected int maxID;
    
    /**
     * The number of actual entries in the partition.
     */
    protected int nEntries;
    
    /**
     * The file for the main dictionaries.
     */
    protected RandomAccessFile mainDictFile;
    
    /**
     * The streams for the postings.
     */
    protected RandomAccessFile[] mainPostFiles;
    
    /**
     * The size, in bytes of the offsets that will be written for the
     * dictionaries in the partition.
     */
    protected static final int DICT_OFFSETS_SIZE = 16;
    
    /**
     * The log.
     */
    protected static MinionLog log = MinionLog.getLog();
    
    /**
     * The log tag.
     */
    protected static String logTag = "P";
    
    private String name;
    
    /**
     * Get the number for this file.
     */
    public int getPartitionNumber() {
        return partNumber;
    }
    
    /**
     * Gets the statistics for this partition.
     */
    public PartitionStats getStats() {
        return stats;
    }
    
    /**
     * Gets a set of statistics for the collection holding this partition.
     */
    public IndexConfig getIndexConfig() {
        return manager.engine.getIndexConfig();
    }
    
    /**
     * Gets a set of statistics for the collection holding this partition.
     */
    public QueryConfig getQueryConfig() {
        return manager.engine.getQueryConfig();
    }
    
    /**
     * Gets the number of documents in this partition.
     */
    public abstract int getNDocs();
    
    /**
     * Get the manager for this partition.
     */
    public PartitionManager getManager() {
        return manager;
    }
    
    /**
     * Gets the number of channels that we'll need to store postings for
     * our main dictionary.
     */
    public int getNumPostingsChannels() {
        try {
            return ((Entry) mainDictFactory.getEntryClass().newInstance()).getNumChannels();
        } catch (Exception e) {
            log.error(logTag, 1, "Error instantiating main entry", e);
            return 1;
        }
    }
    
    /**
     * Gets the files associated with the main postings for a partition.
     *
     * @return an array of files.  The first is for the dictionary, and the
     * remaining are for the postings files.
     */
    protected File[] getMainFiles() {
        return getMainFiles(manager, partNumber);
    }
    
    /**
     * Gets the files associated with the main postings for a partition.
     *
     * @param partNumber the partition number for which we want the files.
     * @return an array of files.  The first is for the dictionary, and the
     * remaining are for the postings files.
     */
    protected static File[] getMainFiles(PartitionManager manager,
            int partNumber) {
        
        int nFiles = manager.getNumPostingsChannels();
        File[] ret = new File[nFiles + 1];
        ret[0] = manager.makeDictionaryFile(partNumber, "main");
        if(nFiles == 1) {
            ret[1] = manager.makePostingsFile(partNumber, "main");
        } else {
            for(int i = 1; i <= nFiles; i++) {
                ret[i] = manager.makePostingsFile(partNumber, "main", i);
            }
        }
        
        return ret;
    }
    
    /**
     * Gets the files associated with the document keys for a partition.
     *
     * @return an array of files.  The first is for the dictionary, and the
     * remaining are for the postings files.
     */
    protected File[] getDocFiles() {
        return getDocFiles(manager, partNumber);
    }
    
    /**
     * Gets the files associated with the document keys for a partition.
     *
     * @param partNumber the partition number for which we want the files.
     * @return an array of files.  The first is for the dictionary, and the
     * remaining are for the postings files.
     */
    protected static File[] getDocFiles(PartitionManager manager,
            int partNumber) {
        return new File[] {
            manager.makeDictionaryFile(partNumber, "docs"),
            manager.makePostingsFile(partNumber, "docs")
        };
    }
    
    /**
     * Gets all the files associated with a partition.
     */
    protected File[] getAllFiles() {
        return getAllFiles(manager, partNumber);
    }
    
    /**
     * Gets all the files associated with a partition.
     */
    protected static File[] getAllFiles(PartitionManager manager,
            int partNumber) {
        
        File[] mf = getMainFiles(manager, partNumber);
        File[] df = getDocFiles(manager, partNumber);
        
        File[] ret = new File[mf.length + df.length + 1];
        
        int p = 0;
        for(int i = 0; i < mf.length; i++) {
            ret[p++] = mf[i];
        }
        
        for(int i = 0; i < df.length; i++) {
            ret[p++] = df[i];
        }
        
        ret[p++] = manager.makeVectorLengthFile(partNumber);
        
        return ret;
    }
    
    /**
     * Compares two partitions by their partition numbers.
     */
    public int compareTo(Partition p) {
        return partNumber - p.partNumber;
    }

    @ConfigComponent(type=com.sun.labs.minion.indexer.dictionary.DictionaryFactory.class)
    public static final String PROP_DOC_DICT_FACTORY = "doc_dict_factory";

    @ConfigComponent(type=com.sun.labs.minion.IndexConfig.class)
    public static final String PROP_INDEX_CONFIG = "index_config";
    
    @ConfigComponent(type=com.sun.labs.minion.indexer.dictionary.DictionaryFactory.class)
    public static final String PROP_MAIN_DICT_FACTORY = "main_dict_factory";

    @ConfigComponent(type=PartitionManager.class)
    public static final String PROP_PARTITION_MANAGER = "partition_manager";

    
    public String getName() {
        return name;
    }

    
    public void newProperties(PropertySheet ps) throws PropertyException {
        name = ps.getInstanceName();
        mainDictFactory = (DictionaryFactory) ps.getComponent(PROP_MAIN_DICT_FACTORY);
        docDictFactory = (DictionaryFactory) ps.getComponent(PROP_DOC_DICT_FACTORY);
        manager = (PartitionManager) ps.getComponent(PROP_PARTITION_MANAGER);
        indexConfig = (IndexConfig) ps.getComponent(PROP_INDEX_CONFIG);
    }

    
    protected DictionaryFactory docDictFactory;

    
    protected DictionaryFactory mainDictFactory;

    
    /**
     * The configuration for this index.
     */
    protected IndexConfig indexConfig;
    
    
} // Partition
