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

package com.sun.labs.minion.classification;

import com.sun.labs.minion.indexer.postings.Occurrence;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;

import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.Buffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;

import com.sun.labs.minion.util.MinionLog;

/**
 * An implementation of Postings that we can use to store classifier
 * features.  The postings will store the IDs for the features as well as
 * encoding the other information about the features.
 */
public class FeaturePostings implements Postings {

    /**
     * An array of the features added.  We'll need to remap this
     * at dump time.
     */
    protected Feature[] feats;

    /**
     * The number of features that we hold.
     */
    protected int nIDs;

    /**
     * The last ID that we hold.
     */
    protected int lastID;

    /**
     * The last info offset that we hold.
     */
    protected int lastOff;

    /**
     * The offset of the start of the actual data in our ino buffer.
     */
    protected int dataStart;

    /**
     * A buffer holding feature IDs and offsets into the info buffer.
     */
    protected Buffer ino;

    /**
     * A buffer holding feature info.
     */
    protected Buffer info;

    protected static MinionLog log = MinionLog.getLog();

    protected static String logTag = "FP";

    /**
     * Creates a set of postings suitable for indexing time.
     */
    public FeaturePostings() {
        feats = new Feature[200];
    } // FeaturePostings constructor

    /**
     * Creates a set of postings suitable for querying time.
     */
    public FeaturePostings(ReadableBuffer ino,
                           ReadableBuffer info) {
        this.ino = ino;
        this.info = info;

        nIDs      = ((ReadableBuffer) this.ino).byteDecode();
        lastID    = ((ReadableBuffer) this.ino).byteDecode();
        lastOff   = ((ReadableBuffer) this.ino).byteDecode();
        dataStart = this.ino.position();
    }
    
    /**
     * Sets the skip size used for building the skip table.  This
     * implementation uses no skips.
     */
    public void setSkipSize(int size) {
    }

    /**
     * Gets the total number of occurrences for these postings, which is
     * just the number of features encoded.
     */
    public long getTotalOccurrences() {
        return nIDs;
    }

    /**
     * Gets the maximum fdt value for these postings, which is just 1,
     * since we're using real-valued features.
     */
    public int getMaxFDT() {
        return 1;
    }

    /**
     * Adds an occurrence to the postings list.  We assume that the
     * occurrence is actually an implementation of {@link Feature}.
     *
     * @param o The occurrence.
     */
    public void add(Occurrence o) {

        Feature f = (Feature) o;

        if(f.getID() >= feats.length) {
            Feature[] temp = new Feature[f.getID()+128];
            System.arraycopy(feats, 0, temp, 0, feats.length);
            feats = temp;
        }

        //
        // Warn on a duplicate feature.
        if(feats[f.getID()] != null) {
            log.warn(logTag, 3, "Duplicate feature in FeaturePostings: " +
                     f.getName());
        } else {
            nIDs++;
        }

        feats[f.getID()] = f;
    }

    /**
     * Gets the number of IDs in the postings list.
     */
    public int getN() {
        return nIDs;
    }

    public int getLastID() {
        return lastID;
    }
    
    /**
     * Finishes any ongoing encoding and prepares for the data to be
     * dumped.  This implementation doesn't require any finishing.
     */
    public void finish() {
    }

    /**
     * Gets the size of the postings, in bytes.
     */
    public int size() {
        return nIDs*32;
    }

    /**
     * Gets a number of <code>Buffers</code> whose contents represent the
     * postings.  These buffers can be written to disk.
     *
     * <p>
     *
     * This method must ensure that all of the data used by the entry is
     * properly handled by the time that the method returns.  This method
     * will be called by a dictionary when it is ready to dump the postings
     * data to a stream.
     *
     * @return An array of <code>Buffer</code>s containing the postings
     * data.  All of the data in these buffers must be written to the
     * postings file!
     */
    public WriteableBuffer[] getBuffers() {
        WriteableBuffer temp = new ArrayBuffer(16);
        temp.byteEncode(nIDs);
        temp.byteEncode(lastID);
        temp.byteEncode(lastOff);
        return new WriteableBuffer[] {
            temp, (WriteableBuffer) ino, (WriteableBuffer) info
        };
    }

    /**
     * Remaps the IDs in the features in these postings, resulting in the
     * encoding of the IDs and feature information to the buffers.
     *
     * @param idMap a map from the IDs currently in use in the postings to
     * new IDs.
     */
    public void remap(int[] idMap) {
        if(idMap == null) {
            return;
        }

        ino = new ArrayBuffer(nIDs*2);
        info = new ArrayBuffer(nIDs*5);

        for(int i = 0; i < idMap.length; i++) {
            int map = idMap[i];
            if(map < feats.length) {
                Feature f = feats[map];
                if(f == null) {
                    continue;
                }
                ((WriteableBuffer) ino).byteEncode(i - lastID);
                ((WriteableBuffer) ino).byteEncode(info.position() - lastOff);
                lastID = i;
                lastOff = info.position();
                f.encode((WriteableBuffer) info);
            }
        }
    }

    /**
     * Appends another set of postings to this one.
     * 
     * @param p The postings to append.  Implementers can safely assume
     * that the postings being passed in are of the same class as the
     * implementing class.
     * @param start The new starting document ID for the partition
     * that the entry was drawn from.
     */
    public void append(Postings p, int start) {

        FeaturePostings other = (FeaturePostings) p;

        if(other.nIDs == 0) {
            return;
        }

        //
        // Remember where the info buffer was when we started.
        int infopos = info.position();
        
        //
        // We'll decode the first ID and throw away the first offset.
        int firstID = ((ReadableBuffer) other.ino).byteDecode();
        
        ((ReadableBuffer) other.ino).byteDecode();

        //
        // Encode the new ID and the new offset.
        ((WriteableBuffer) ino).byteEncode(firstID + start - 1 - lastID);
        ((WriteableBuffer) ino).byteEncode(infopos - lastOff);

        //
        // Copy the rest of the data.
        ((WriteableBuffer) ino).append((ReadableBuffer) other.ino);
        ((WriteableBuffer) info).append((ReadableBuffer) other.info);

        //
        // Set up for next time.
        lastID  = other.lastID + start - 1;
        lastOff = other.lastOff + infopos;
   }

    /**
     * Appends another set of postings to this one, removing any data
     * associated with deleted documents.
     * 
     * @param p The postings to append.  Implementers can safely assume
     * that the postings being passed in are of the same class as the
     * implementing class.
     * @param start The new starting document ID for the partition
     * that the entry was drawn from.
     * @param idMap A map from old IDs in the given postings to new IDs
     * with gaps removed for deleted data.  If this is null, then there are
     * no deleted documents.
     */
    public void append(Postings p, int start, int[] idMap) {
        
        if(idMap == null) {
            append(p, start);
        }

        FeaturePostings other        = (FeaturePostings) p;
        int             prevOff      = 0;
        boolean         copyNextTime = false;

        if(ino == null) {
            ino = new ArrayBuffer(other.nIDs *2);
            info = new ArrayBuffer(other.info.limit());
        }
        
        //
        // Loop through the documents.
        for(int i = 0, origID = 0, off = 0; i < other.nIDs; i++) {

            origID += ((ReadableBuffer) other.ino).byteDecode();
            off    += ((ReadableBuffer) other.ino).byteDecode();
            int mapID = idMap[origID];

            //
            // Do we need to copy some bytes?
            if(copyNextTime) {

                lastOff = info.position();
                //
                // Encode the offset in the buffer.
                ((WriteableBuffer) info).append((ReadableBuffer) other.info,
                                                off - prevOff);
                copyNextTime = false;
            }

            other.info.position(off);
            prevOff = off;

            //
            // Skip deleted documents.
            if(mapID < 0) {
                continue;
            }
	    
            //
            // Get the document ID for this document in the new partition.
            int newID = mapID + start - 1;

            //
            // Increment our ID count, and see if we need to add a skip.
            nIDs++;

            //
            // Encode the id, frequency, and offset information.
            ((WriteableBuffer) ino).byteEncode(newID - lastID);
            ((WriteableBuffer) ino).byteEncode(info.position() - lastOff);

            //
            // Copy the data from the other field and position buffer,
            // which we'll do next time.
            copyNextTime = true;
            lastID = newID;
        }

        //
        // Handle the last bit of info data.
        if(copyNextTime) {
            lastOff = info.position();
            ((WriteableBuffer) info).append((ReadableBuffer) other.info);
        }
    }

    /**
     * Gets an iterator for the postings.
     * 
     * @param features A set of features that the iterator must support.
     * @return A postings iterator that supports the given features.  If
     * the underlying postings do not support a specified feature, then a
     * warning should be logged and <code>null</code> will be returned.
     */
    public PostingsIterator iterator(PostingsIteratorFeatures features) {
        return new Featurator(features);
    }

    public boolean hasPositionInformation() {
        return false;
    }

    public boolean hasFieldInformation() {
        return false;
    }

    public class Featurator implements PostingsIterator {

        /**
         * The current ID.
         */
        protected int curr;

        /**
         * The current offset.
         */
        protected int off;
        
        private PostingsIteratorFeatures features;

        public Featurator(PostingsIteratorFeatures features) {
            this.features = features;
        }
        
        public PostingsIteratorFeatures getFeatures() {
            return features;
        }

        /**
         * Gets the number of IDs that this iterator will produce.
         */
        public int getN() {
            return nIDs;
        }
    
        /**
         * Moves to the next feature in the postings list.
         *
         * @return true if there is a next value, false otherwise.
         */
        public boolean next() {
            if(curr >= lastID) {
                return false;
            }

            curr += ((ReadableBuffer) ino).byteDecode();
            off += ((ReadableBuffer) ino).byteDecode();

            return true;
        }

        /**
         * Finds the given document in the entry we're iterating through, if it
         * exists.  If the document occurs in this entry, the iterator is left
         * in a state where the data for that document has been decoded.  If
         * the document does not occur in this entry, the iterator is left in a
         * state where the data for the next-highest document sequence number
         * in this entry has been decoded.
         *
         * @param id The ID that we want to find.
         * @return true if the ID occurs in this entry, false otherwise.
         * @see #reset
         */
        public boolean findID(int id) {

            if(id < curr) {
                reset();
            }

            while(next()) {
                if(id == curr) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Resets the iterator to the beginning of the entry.  Data will not be
         * decoded until the <code>next</code> method is called.
         */
        public void reset() {
            curr = 0;
            off = 0;
            ino.position(dataStart);
        }

        /**
         * Gets the document sequence number that the iterator is currently
         * pointing at.
         *
         * @return The document sequence number that the iterator is pointing
         * at, or 0 if the iterator has not been advanced yet, or has been
         * exhausted.
         */
        public int getID() {
            return curr;
        }

        /**
         * Gets the weight of the term in the current document, as generated by
         * some weighting function.
         */
        public float getWeight() {
            return 1;
        }

        /**
         * Gets the frequency of the term in the current document.
         */
        public int getFreq() {
            return 1;
        }
    
        public int get(int[] ids) {
            int p;
            for(p = 0; p < ids.length && curr != lastID; p++) {
                curr += ((ReadableBuffer) ino).byteDecode();
                ids[p] = curr;
                ((ReadableBuffer) ino).byteDecode();
            }
            return p;
        }

        public int get(int[] ids, int[] freq) {
            int p;
            for(p = 0; p < ids.length && curr != lastID; p++) {
                curr += ((ReadableBuffer) ino).byteDecode();
                ids[p] = curr;
                ((ReadableBuffer) ino).byteDecode();
            }
            return p;
        }

        public int get(int[] ids, float[] weights) {
             int p;
            for(p = 0; p < ids.length && curr != lastID; p++) {
                curr += ((ReadableBuffer) ino).byteDecode();
                ids[p] = curr;
                ((ReadableBuffer) ino).byteDecode();
            }
            return p;
       }
        /**
         * Compares this postings iterator to another one.  Typically this
         * comparison should be based on the ID at the head of the iterator.
         */
        public int compareTo(Object o) {
            return curr - ((Featurator) o).curr;
        }

        /**
         * Decodes the current feature from the info buffer and returns it.
         *
         * @param f the feature into which we'll decode
         * @return the decoded feature.
         */
        public Feature decode(Feature f) {
            info.position(off);
            f.decode((ReadableBuffer) info);
            return f;
        }
    }

} // FeaturePostings
