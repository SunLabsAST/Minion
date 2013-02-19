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
package com.sun.labs.minion.indexer.entry;

import com.sun.labs.minion.indexer.dictionary.Dictionary;
import com.sun.labs.minion.indexer.partition.Partition;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * An interface describing things that can be stored in dictionaries,
 * either for indexing purposes or for querying purposes.  All entries have
 * a name that can be gotten.
 * @param <N> the type of the name for the entry, which must implement comparable,
 * so that entries can be sorted.
 */
public abstract class Entry<N extends Comparable> implements Comparable<Entry>, Cloneable {

    private static final Logger logger = Logger.getLogger(Entry.class.getName());

    /**
     * The ID of this entry.
     */
    protected int id;

    /**
     * The name of this entry.
     */
    protected N name;

    /**
     * The dictionary that this entry was drawn from.
     */
    protected Dictionary dictionary;

    /**
     * The postings associated with this entry.  A single entry may have
     * several postings associated with it.
     */
    protected Postings post;

    /**
     * The type of postings associated with this entry.
     */
    protected Postings.Type type;

    /**
     * The number of postings associated with this entry.
     */
    protected int n;
    
    /**
     * The maximum frequency for this term in the documents.
     */
    protected int maxFDT = 1;
    
    /**
     * The sizes of the postings, in bytes.
     */
    protected int[] size;

    /**
     * The offsets of the postings in a postings file.
     */
    protected long[] offset;

    /**
     * Gets the name of this entry
     *
     * @return The name of this entry.
     */
    public N getName() {
        return name;
    }

    public void setName(N name) {
        this.name = name;
    }

    /**
     * Gets the ID associated with this entry.
     * @return the ID of this entry.
     */
    public int getID() {
        return id;
    }

    @Override
    public int compareTo(Entry e) {
        return name.compareTo(e.name);
    }

    /**
     * Gets an iterator for the postings in this entry
     * @param features the features that the iterator should have
     * @return an iterator for the postings, or null if there are no
     * postings associated with this entry
     */
    public abstract PostingsIterator iterator(PostingsIteratorFeatures features);

    /**
     * Gets the number of postings associated with this entry.  This is
     * used to sort entries by their frequency during query operations.
     *
     * @return The number of postings associated with this entry.
     */
    public int getN() {
        return n;
    }

    public Postings getPostings() {
        return post;
    }

    /**
     * Gets the total number of occurrences associated with this entry.
     * For this base class, this is the same as n.
     *
     * @return The total number of occurrences associated with this entry.
     */
    public long getTotalOccurrences() {
        return n;
    }

    /**
     * Gets the maximum frequency in the postings associated with this
     * entry.  For this base class, this is always 1.
     * @return the maximum frequency.
     */
    public int getMaxFDT() {
        return maxFDT;
    }

    /**
     * Sets the dictionary that this entry was drawn from.
     * @param dict the dictionary containing this entry.
     */
    public void setDictionary(Dictionary dict) {
        this.dictionary = dict;
    }

    /**
     * Gets the partition that this entry was drawn from.
     * @return the partition
     */
    public Partition getPartition() {
        return dictionary.getPartition();
    }

    @Override
    public String toString() {
        return String.format("%s %d (%s)", name, id, type);
    }
    
    public String toVerboseString() {
        return String.format("%s: off: %s size: %s", name, 
                Arrays.toString(offset), Arrays.toString(size));
    }

    /**
     * Sets the ID associated with this entry.
     *
     * @param id The id to use for this entry.
     */
    public void setID(int id) {
        this.id = id;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        try {
            Entry e = (Entry) super.clone();
            e.post = null;
            return e;
        } catch(CloneNotSupportedException ex) {
            throw(ex);
        }
    }

}
