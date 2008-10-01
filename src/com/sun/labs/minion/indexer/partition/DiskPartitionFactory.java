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

import com.sun.labs.util.props.ConfigComponent;
import com.sun.labs.util.props.ConfigInteger;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import com.sun.labs.minion.indexer.dictionary.DictionaryFactory;
import com.sun.labs.util.props.ConfigBoolean;

/**
 *
 * A factory class for generating memory partitions and disk partitions during
 * indexing and querying.
 *
 */
public class DiskPartitionFactory implements Configurable {
    
    /**
     * A property for the factory that we'll use to create the main dictionary.
     */
    @ConfigComponent(type=com.sun.labs.minion.indexer.dictionary.DictionaryFactory.class)
    public static final String PROP_MAIN_DICT_FACTORY = "main_dict_factory";
    
    /**
     * The main dictionary factory.
     */
    protected DictionaryFactory mainDictFactory;
    
    /**
     * A property for the factory that we'll use to create the document
     * dictionaries for partitions.
     */
    @ConfigComponent(type=com.sun.labs.minion.indexer.dictionary.DictionaryFactory.class)
    public static final String PROP_DOCUMENT_DICT_FACTORY = "document_dict_factory";
    
    /**
     * The document dictionary factory.
     */
    protected DictionaryFactory documentDictFactory;
    
    /**
     * A property for the size of the in-memory buffer to use when merging
     * partitions.
     */
    @ConfigInteger(defaultValue=1048576)
    public static final String PROP_MERGE_BUFF_SIZE = "merge_buff_size";
    
    /**
     * The size of the merge buffer.
     */
    protected int mergeBuffSize;

    @ConfigBoolean(defaultValue=false)
    public static final String PROP_CACHE_VECTOR_LENGTHS = "cache_vector_lengths";

    protected boolean cacheVectorLengths;
    
    /**
     * Creates a new instance of DiskPartitionFactory
     */
    public DiskPartitionFactory() {
    }
    
    public void newProperties(PropertySheet ps) throws PropertyException {
        mainDictFactory = (DictionaryFactory) ps.getComponent(PROP_MAIN_DICT_FACTORY);
        documentDictFactory = (DictionaryFactory) ps.getComponent(PROP_DOCUMENT_DICT_FACTORY);
        mergeBuffSize = ps.getInt(PROP_MERGE_BUFF_SIZE);
        cacheVectorLengths = ps.getBoolean(PROP_CACHE_VECTOR_LENGTHS);
    }
    
    /**
     * Gets a disk partition with the given number, managed by the given manager.
     * 
     * @param number the partition number that we want to get
     * @param m the manager that will be managing the partition
     * @return the partition with the given number
     * @throws java.io.IOException if there is any error opening the partition
     */
    public DiskPartition getDiskPartition(int number, PartitionManager m)
    throws java.io.IOException {
        return new DiskPartition(number, m, mainDictFactory, documentDictFactory, cacheVectorLengths);
    }
    
}
