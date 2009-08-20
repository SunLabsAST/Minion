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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import com.sun.labs.minion.indexer.dictionary.MemoryBiGramDictionary;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.dictionary.StringNameHandler;

import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.StreamPostingsOutput;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class InvFilePartitionUtils {

    /**
     * The log.
     */
    static Logger logger = Logger.getLogger(InvFilePartitionUtils.class.getName());

    /**
     * The tag for this module.
     */
    protected static String logTag = "IFUtil";

    /** Creates a new instance of InvFilePartitionUtils */
    public InvFilePartitionUtils() {
    }

    /**
     * Gets the files associated with the field store for a partition.
     *
     * @param partNumber the partition number for which we want the files.
     * @return an array of files.  The first is for the dictionary, and the
     * remaining are for the postings files.
     */
    protected static File[] getFieldFiles(PartitionManager manager,
            int partNumber) {
        return new File[]{
                    manager.makeDictionaryFile(partNumber, "fs"),
                    manager.makePostingsFile(partNumber, "fs")
                };
    }

    /**
     * Gets the files associated with the bigram postings for a partition.
     *
     * @param partNumber the partition number for which we want the files.
     * @return an array of files.  The first is for the dictionary, and the
     * remaining are for the postings files.
     */
    protected static File[] getBigramFiles(PartitionManager manager,
            int partNumber) {
        return new File[]{
                    manager.makeDictionaryFile(partNumber, "bi"),
                    manager.makePostingsFile(partNumber, "bi"),};
    }

    /**
     * Gets the files associated with the taxonomy postings for a partition.
     *
     * @param partNumber the partition number for which we want the files.
     * @return an array of files.  The first is for the dictionary, and the
     * remaining are for the postings files.
     */
    protected static File[] getTaxonomyFiles(PartitionManager manager,
            int partNumber) {
        return new File[]{
                    manager.makeTaxonomyFile(partNumber),};
    }

    protected static File[] getNGramFiles(PartitionManager manager,
            int partNumber) {
        return new File[]{
                    manager.makeDictionaryFile(partNumber, "ng"),
                    manager.makePostingsFile(partNumber, "ng"),
                    manager.makeDictionaryFile(partNumber, "sip"),
                    manager.makePostingsFile(partNumber, "sip")
                };
    }

    /** 
     * Gets all the files associated with a partition, including those specific to
     * the inverted file.
     * 
     * @return an array of files
     */
    protected static File[] getAllFiles(PartitionManager manager,
            int partNumber) {
        File[] commonFiles = Partition.getAllFiles(manager, partNumber);
        File[] fieldFiles = getFieldFiles(manager, partNumber);
        File[] biFiles = getBigramFiles(manager, partNumber);
        File[] taxFiles = getTaxonomyFiles(manager, partNumber);
        File[] ngFiles = getNGramFiles(manager, partNumber);

        File[] result = new File[commonFiles.length + fieldFiles.length +
                biFiles.length + taxFiles.length + ngFiles.length];
        int insert = 0;
        System.arraycopy(commonFiles, 0, result, insert, commonFiles.length);
        insert += commonFiles.length;

        System.arraycopy(fieldFiles, 0, result, insert, fieldFiles.length);
        insert += fieldFiles.length;

        System.arraycopy(biFiles, 0, result, insert, biFiles.length);
        insert += biFiles.length;

        System.arraycopy(taxFiles, 0, result, insert, taxFiles.length);
        insert += taxFiles.length;

        System.arraycopy(ngFiles, 0, result, insert, ngFiles.length);
        return result;
    }

    /**
     * Writes a bigram dictionary out to disk using the provided partition manager
     * and for the specified partition number
     * @param dict the bigram dictionary to write
     * @param manager the partition manager to use
     * @param partNumber the partition number that this dict is a part of
     */
    protected static void writeBigramDictionary(MemoryBiGramDictionary dict,
            PartitionManager manager,
            int partNumber) {
        try {
            //
            // Get the files that are used for the bigram dict.
            File[] files = getBigramFiles(manager, partNumber);

            //
            // Get a channel for the bigram dictionaries.
            RandomAccessFile dictFile = new RandomAccessFile(files[0], "rw");

            //
            // Get channels for the postings.
            OutputStream postStream =
                    new BufferedOutputStream(new FileOutputStream(files[1]),
                    8192);
            PostingsOutput postOut =
                    new StreamPostingsOutput(postStream);

            //
            // Write the bigram dict out to the bigram files
            dict.dump(manager.getIndexDir(),
                    new StringNameHandler(),
                    dictFile,
                    new PostingsOutput[]{
                        postOut
                    },
                    MemoryDictionary.Renumber.RENUMBER,
                    MemoryDictionary.IDMap.NONE,
                    null);

            dict.clear();

            //
            // Close off those files.
            dictFile.close();
            postStream.close();
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "Error dumping partition", ioe);
        }
    }
}
