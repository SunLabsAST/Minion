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
package com.sun.labs.minion.indexer.postings;

import com.sun.labs.minion.indexer.postings.io.PostingsInput;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.util.Util;
import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.Buffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A postings class for ID only postings.  These will be used for things
 * like the bigram tables used for wildcard expansion and for saved field
 * values.
 *
 * <p>
 *
 * The structure of the encoded data is as follows:
 *
 * <ol>
 *
 * <li>The number of IDs in the postings is byte encoded.</li>
 *
 * <li>The last ID in the postings list is byte encoded.</li>
 *
 * <li>The number of skips in the skip table is byte encoded.</li>
 *
 * <li>The skip table.  The number of entries per skip is dependent on the
 * application.  The skip table has the following structure.
 *
 * <ol>
 *
 * <li>The number of skips is byte encoded.</li>
 *
 * <li>For each skip we encode:
 *
 * <ol>
 *
 * <li>The ID of skipped entry.  This is byte encoded as a delta from the
 * previous ID in the skip table</li>
 *
 * <li>The position in the encoded data to skip to.  This is byte encoded
 * as a delta from the previous position in the skip table.  Note that this
 * position is relative to the <em>end</em> of the skip table!</li>
 *
 * </ol>
 *
 * </ol>
 *
 * <li>Each ID is encoded as a delta from the previous ID in the postings list.</li>
 *
 * </ol>
 */
public class IDPostings implements Postings, MergeablePostings {

    private static final Logger logger = Logger.getLogger(IDPostings.class.getName());

    /**
     * The compressed postings.
     */
    protected Buffer idBuff;

    /**
     * The uncompressed postings.
     */
    protected int[] ids;

    /**
     * The ID we're collecting the frequency for.
     */
    protected int currentID;

    /**
     * The number of IDs in the postings.
     */
    protected int nIDs;

    /**
     * The last ID in this postings list.
     */
    protected int lastID;

    /**
     * The IDs in the skip table.
     */
    protected int[] skipID;

    /**
     * The positions in the skip table.
     */
    protected int[] skipPos;

    /**
     * The number of skips in the skip table.
     */
    protected int nSkips;
    
    /**
     * The position in the compressed representation where the data
     * starts.
     */
    protected int dataStart;

    /**
     * The number of documents in a skip.
     */
    protected int skipSize = 16;
    
    /**
     * The position of the skip list in the postings buffer.
     */
    protected int skipTableOffset;

    /**
     * Makes a postings entry that is useful during indexing.
     *
     */
    public IDPostings() {
        ids = new int[4];
        currentID = -1;
    }

    /**
     * Makes a postings entry that is useful during querying.
     *
     * @param b the data read from a postings file.
     */
    public IDPostings(PostingsInput in[], long[] offset, int[] size) throws java.io.IOException {
        this(in[0].read(offset[0], size[0]), 0, 0);
    }

    /**
     * Makes a postings entry that is useful during querying.
     *
     * @param b The buffer containing the postings.
     * @param offset The offset in the buffer where our postings data
     * starts.
     * @param size The size of the data in our postings.
     *
     */
    public IDPostings(ReadableBuffer b, int offset, int size) {

        if(offset > 0) {
            idBuff = b.slice(offset, size);
        } else {
            idBuff = b;
        }

        //
        // Get the initial data.
        nIDs = ((ReadableBuffer) idBuff).byteDecode();
        lastID = ((ReadableBuffer) idBuff).byteDecode();
        skipTableOffset = ((ReadableBuffer) idBuff).byteDecode();
        dataStart = (int) idBuff.position();

        //
        // When we encoded the skip table offset at write time, we hadn't 
        // encoded the above stuff onto the buffer for the postings, so we need
        // to account for that now.
        if(skipTableOffset > 0) {
            skipTableOffset += dataStart;
        }
    }
    
    private void decodeSkipTable() {
        if(skipTableOffset > 0 && skipID == null) {
            ReadableBuffer ridb = ((ReadableBuffer) idBuff).duplicate();
            ridb.position(skipTableOffset);
            nSkips = ridb.byteDecode();
            skipID = new int[nSkips + 1];
            skipPos = new int[nSkips + 1];
            skipPos[0] = dataStart;
            int currSkipDoc = 0;
            int currSkipPos = dataStart;
            for(int i = 1; i <= nSkips; i++) {
                currSkipDoc += ridb.byteDecode();
                currSkipPos += ridb.byteDecode();
                skipID[i] = currSkipDoc;
                skipPos[i] = currSkipPos;
            }
        }
    }

    @Override
    public String[] getChannelNames() {
        return new String[]{"post"};
    }

    /**
     * We don't have any secondary information.
     */
    public void readSecondaryInformation(ReadableBuffer[] buffs) {
    }

    @Override
    public Type getType() {
        return Type.ID;
    }

    /**
     * Adds a skip to the skip table.
     *
     * @param id The ID that the skip is pointing to.
     * @param pos The position in the postings to skip to.
     */
    protected void addSkip(int id, int pos) {
        if(skipID == null) {
            skipID = new int[4];
            skipPos = new int[4];
        } else if(nSkips + 1 >= skipID.length) {
            skipID = Arrays.copyOf(skipID, skipID.length * 2);
            skipPos = Arrays.copyOf(skipPos, skipID.length);
        }
        skipID[nSkips] = id;
        skipPos[nSkips++] = pos;
    }

    /**
     * Sets the skip size.
     */
    public void setSkipSize(int size) {
        skipSize = size;
    }

    /**
     * Adds an occurrence to the postings list.
     *
     * @param oThe occurrence.
     */
    @Override
    public void add(Occurrence o) {
        int oid = o.getID();
        if(oid != currentID) {
            if(ids == null || nIDs + 1 >= ids.length) {
                ids = Arrays.copyOf(ids, (nIDs + 1) * 2);
            }
            ids[nIDs++] = oid;
            currentID = oid;
            lastID = currentID;
        }
    }

    @Override
    public int getN() {
        return nIDs;
    }

    @Override
    public int getLastID() {
        return lastID;
    }

    /**
     * Gets the maximum frequency in the postings associated with this
     * entry.  For ID only postings, this value is always 1.
     *
     * @return 1.
     */
    @Override
    public int getMaxFDT() {
        return 1;
    }

    /**
     * Gets the total number of occurrences in this postings list, which is
     * always the number of postings, since we don't encode any frequencies.
     */
    @Override
    public long getTotalOccurrences() {
        return nIDs;
    }

    /**
     * Gets the size of the postings, in bytes.
     */
    public int size() {
        return (int) idBuff.position();
    }

    @Override
    public void write(PostingsOutput[] out, long[] offset, int[] size) throws java.io.IOException {

        //
        // Write the buffers.
        offset[0] = out[0].position();
        encodeIDs();
        WriteableBuffer headerBuff = out[0].getTempBuffer();
        if(nSkips > 0) {
            skipTableOffset = (int) idBuff.position();
        }
        encodeHeaderData(headerBuff);
        encodeSkipTable();
        size[0] = out[0].write(
                new WriteableBuffer[]{
                    headerBuff,
                    (WriteableBuffer) idBuff});
    }

    protected void encodeHeaderData(WriteableBuffer headerBuff) {

        //
        // Encode the number of IDs and the last ID
        headerBuff.byteEncode(nIDs);
        headerBuff.byteEncode(lastID);
        headerBuff.byteEncode(skipTableOffset);
    }
    
    protected void encodeSkipTable() {

        //
        // Encode the skip table.
        if(nSkips > 0) {
            WriteableBuffer wIDBuff = (WriteableBuffer) idBuff;
            wIDBuff.byteEncode(nSkips);
            int prevSkipID = 0;
            int prevPos = 0;
            for(int i = 0; i < nSkips; i++) {
                wIDBuff.byteEncode(skipID[i] - prevSkipID);
                wIDBuff.byteEncode(skipPos[i] - prevPos);
                prevSkipID = skipID[i];
                prevPos = skipPos[i];
            }
        }
    }

    protected WriteableBuffer encodeIDs() {

        if(idBuff == null) {
            idBuff = new ArrayBuffer(nIDs);
        }
        if(idBuff.position() > 0) {
            return (WriteableBuffer) idBuff;
        }
        WriteableBuffer wIDBuff = (WriteableBuffer) idBuff;
        
        int prev = 0;
        for(int i = 0; i < nIDs; i++) {
            if(i > 0 && i % skipSize == 0) {
                addSkip(ids[i], (int) wIDBuff.position());
            }
            try {
                wIDBuff.byteEncode(ids[i] - prev);
            } catch(ArithmeticException ex) {
                logger.log(Level.SEVERE, String.format("Caught AX, nIDs = %d i = %d ids[i] = %d prev = %d", nIDs, i, ids[i], prev));
                throw (ex);
            }
            prev = ids[i];
            encodeOtherPostingsData(wIDBuff, i);
        }
        return wIDBuff;
    }

    protected void encodeOtherPostingsData(WriteableBuffer b, int i) {
    }

    /**
     * Remaps the IDs in this postings list according to the given
     * old-to-new ID map.
     *
     * <p>
     *
     * This is tricky, because we can't assume that the remapped IDs will
     * maintain the order of the IDs, even if the IDs have changed.  Thus,
     * we need to uncompres all of the IDs and then put them back together.
     *
     * @param idMap A map from the IDs currently in use in the postings to
     * new IDs.
     */
    @Override
    public void remap(int[] idMap) {
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
    @Override
    public void append(Postings p, int start) {

        IDPostings other = (IDPostings) p;

        //
        // Check for empty postings on the other side.
        if(other.nIDs == 0) {
            return;
        }

        if(idBuff == null) {
            idBuff = new ArrayBuffer((int) other.idBuff.limit());
        }

        //
        // We'll need to know where we started this entry.
        int index = (int) idBuff.position();

        //
        // This is tricky, so pay attention: The skip positions for the
        // entry we're appending are based on the byte index from the
        // beginning of the encoded documents.  We're about to take off the
        // first document ID and reencode it as a delta, which may change
        // the number of bits that it requires (it will proabably take more
        // bytes, since it's likely to be a small number originally and now
        // it will be a delta between two larger numbers).  So, we need to
        // figure out the difference between the old number of bytes and
        // the new number of bytes.  This is the adj variable.
        int adj = (int) other.idBuff.position();
        int firstID = ((ReadableBuffer) other.idBuff).byteDecode();
        adj = (int) (other.idBuff.position() - adj);

        //
        // Get the first document sequence number off the entry we're
        // appending and replace it with a document gap that
        // incorporates the new starting sequence number.  Note that
        // encoding the integer tells us the new number of bits, which we
        // divide by 8 to get the number of bytes.
        int newID = firstID + start - 1;
        int nb = ((WriteableBuffer) idBuff).byteEncode(newID - lastID);
        adj = nb - adj;

        //
        // Copy over the remaining postings, but don't copy the skip table
        // at the end, if there is one.
        if(other.skipTableOffset > 0) {
        ((WriteableBuffer) idBuff).append((ReadableBuffer) other.idBuff, 
                other.skipTableOffset - other.idBuff.position());
        } else {
            ((WriteableBuffer) idBuff).append((ReadableBuffer) other.idBuff);
        }

        //
        // The last ID on this entry is now the last ID from the entry we
        // appended, suitably remapped.
        lastID = other.lastID + start - 1;

        //
        // Increment the number of documents in this new entry.
        nIDs += other.nIDs;
        other.decodeSkipTable();
        if(other.nSkips > 0) {

            //
            // Now we need to fix up the skip table.  The skip postions in
            // the other entry had the length of the initial part of the
            // compressed representation added to them, so we need to fix
            // that and replace the document IDs with something reasonable.
            // First we need to make the skip table big enough to hold all
            // this data.
            if(skipID != null) {
                skipID = Arrays.copyOf(skipID,
                        skipID.length + other.nSkips + 1);
                skipPos = Arrays.copyOf(skipPos,
                        skipPos.length + other.nSkips + 1);
            } else {
                skipID = new int[other.nSkips + 1];
                skipPos = new int[other.nSkips + 1];
            }

            //
            // Now fix up the other skips.
            for(int i = 1; i <= other.nSkips; i++) {
                skipID[nSkips] = other.skipID[i] + start - 1;
                skipPos[nSkips++] =
                        other.skipPos[i] - other.dataStart + index + adj;
            }
        }
    }

    /**
     * Skips a set of postings from another postings entry.
     */
    protected void skip(ReadableBuffer b) {
    }

    @Override
    public void append(Postings p, int start, int[] idMap) {

        if(idBuff == null) {
            idBuff = new ArrayBuffer(p.getN() * 2);
        }

        //
        // If there's no id mapping to be done, then do a simple append.
        if(idMap == null) {
            append(p, start);
            return;
        }

        //
        // We'll iterate through the postings.
        PostingsIterator pi = p.iterator(null);
        WriteableBuffer wpost = (WriteableBuffer) idBuff;
        while(pi.next()) {
            int origID = pi.getID();
            int mapID = idMap[origID];

            //
            // Skip deleted documents.
            if(mapID < 0) {
                continue;
            }

            int cID = mapID + start - 1;

            //
            // Increment our ID count, and see if we need to add a skip.
            nIDs++;
            if(nIDs % skipSize == 0) {
                addSkip(cID, (int) idBuff.position());
            }

            wpost.byteEncode(cID - lastID);

            //
            // Set the new last document for our entry.
            lastID = cID;
        }
    }

    @Override
    public void merge(MergeablePostings mp, int[] map) {

        int n = ((Postings) mp).getN();
        int[] temp = new int[Math.max(n, nIDs)];

        PostingsIterator pi = ((Postings) mp).iterator(null);
        pi.next();
        int p1 = 0;
        int p2 = 0;
        int np = 0;
        while(p1 < n && p2 < nIDs) {
            int pid = map[pi.getID()];
            int diff = pid - ids[p2];
            if(diff < 0) {
                temp = Util.addExpand(temp, np++, pid);
                p1++;
                pi.next();
            } else if(diff > 0) {
                temp = Util.addExpand(temp, np++, ids[p2++]);
            } else {
                temp = Util.addExpand(temp, np++, ids[p2++]);
                p1++;
                pi.next();
            }
        }

        while(p1 < n) {
            temp = Util.addExpand(temp, np++, map[pi.getID()]);
            pi.next();
            p1++;
        }

        if(p2 < nIDs) {
            int toadd = (nIDs - p2);
            if(np + toadd >= temp.length) {
                temp = Arrays.copyOf(temp,
                        Math.max(np + toadd, temp.length * 2));
            }
            System.arraycopy(ids, p2, temp, np, toadd);
            np += toadd;
        }

        nIDs = np;
        ids = temp;
    }

    /**
     * Gets an iterator for the postings.
     *
     * @param features A set of features that the iterator must support.
     * @return A postings iterator.  The iterators for these postings do
     * not support any of the extra features available.  If any extra
     * features are requested, a warning will be logged and
     * <code>null</code> will be returned.
     */
    @Override
    public PostingsIterator iterator(PostingsIteratorFeatures features) {
        if(ids != null) {
            return new UncompressedIDIterator(features);
        }
        return new CompressedIDIterator(features);
    }

    @Override
    public void clear() {
        if(ids == null) {
            return;
        }
        nIDs = 0;
        currentID = -1;
        lastID = 0;
        nSkips = 0;
        skipTableOffset = 0;
        if(idBuff != null) {
            ((WriteableBuffer) idBuff).clear();
        }
    }

    @Override
    public String describe(boolean verbose) {
        StringBuilder b = new StringBuilder();
        b.append(getType()).append(' ').append("N: ").append(nIDs);
        if(verbose) {
            PostingsIterator pi = iterator(null);
            boolean first = true;
            while(pi.next()){
                if(!first) {
                    b.append(' ');
                }
                first = false;
                b.append(pi.getID()).append(',');
            }
        }
        return b.toString();
    }
    
    

    /**
     * A postings iterator than can be used for in-memory data.
     */
    public class UncompressedIDIterator implements PostingsIterator {

        int currPos = -1;

        PostingsIteratorFeatures features;

        private int ourN;

        public UncompressedIDIterator(PostingsIteratorFeatures features) {
            this.features = features;
            ourN = nIDs;
        }

        @Override
        public int getN() {
            return ourN;
        }

        @Override
        public boolean next() {
            return ourN != 0 && ++currPos < ourN;
        }

        @Override
        public boolean findID(int id) {
            currPos = Util.binarySearch(ids, 0, ourN, id);
            if(currPos > 0) {
                return true;
            }
            currPos = -currPos;
            return false;
        }

        @Override
        public void reset() {
            currPos = -1;
        }

        @Override
        public int getID() {
            return ids[currPos];
        }

        @Override
        public float getWeight() {
            return 1 * features.getMultiplier();
        }

        @Override
        public int getFreq() {
            return 1;
        }

        @Override
        public int compareTo(PostingsIterator other) {
            return getID() - other.getID();
        }

        @Override
        public PostingsIteratorFeatures getFeatures() {
            return features;
        }
    }

    /**
     * A postings iterator that can be used for compressed data that's been
     * read from disk.
     */
    public class CompressedIDIterator implements PostingsIterator {

        /**
         * A readable buffer for the postings.
         */
        protected ReadableBuffer rp;

        /**
         * Whether we've finished the entry.
         */
        protected boolean done;

        /**
         * The current ID.
         */
        protected int curr;

        /**
         * The current block of postings that we've jumped into using findID.
         */
        protected int cb;

        private PostingsIteratorFeatures features;

        /**
         * Creates a postings iterator for this postings type.
         */
        public CompressedIDIterator(PostingsIteratorFeatures features) {
            this.features = features;
            rp = ((ReadableBuffer) idBuff).duplicate();
            rp.position(dataStart);
            done = nIDs == 0;
        }

        /**
         * Gets the number of IDs in this postings list.
         */
        @Override
        public int getN() {
            return nIDs;
        }

        @Override
        public PostingsIteratorFeatures getFeatures() {
            return features;
        }

        /**
         * Moves to the next ID in this entry.  This method is different
         * than the <code>java.util.Iterator.next()</code> method in that
         * it does not return an object.  This would require too much
         * object creation overhead during retrieval, and saves the whole
         * <code>hasNext()</code>/<code>next()</code> function call
         * overhead.  You should use the accessor functions for the
         * iterator to find out the actual ID that the iterator is at.
         *
         * @return true if there is a next ID, false otherwise.
         */
        @Override
        public boolean next() {
            return next(-1, -1);
        }

        /**
         * Finds the next document in the postings entry.
         *
         * @param id The ID to use for that document, if we've skipped to
         * this point. If id is less than 0, we will use the ID as it was
         * decoded.
         * @return <code>true</code> if there is a next ID,
         * <code>false</code> otherwise.
         */
        protected boolean next(int pos, int id) {
            if(done) {
                return false;
            }

            //
            // If we were given a position, then we position there.
            if(pos > 0) {
                rp.position(pos);

                if(id > -1) {
                    rp.byteDecode();
                    curr = id;
                } else {
                    curr += rp.byteDecode();
                }
            } else {
                curr += rp.byteDecode();
            }

            done = curr == lastID;

            return true;
        }

        /**
         * Finds the given ID in the entry we're iterating through, if it
         * exists.  If the ID occurs in this entry, the iterator is left in
         * a state where the data for that ID has been decoded.  If the ID
         * does not occur in this entry, the iterator is left in a state
         * where the data for the next-highest ID in this entry has been
         * decoded.
         *
         * @param id The ID that we want to find.
         * @return <code>true</code> if the ID occurs in this entry,
         * <code>false</code> otherwise.
         */
        @Override
        public boolean findID(int id) {

            if(nIDs == 0) {
                return false;
            }

            //
            // We're only done if we're looking for something past the
            // end.
            if(id > lastID) {
                done = true;
                return false;
            }
            done = false;

            decodeSkipTable();
            
            //
            // Set up.  Start at the beginning or skip to the right place.
            if(nSkips == 0) {
                if(id < curr) {
                    curr = 0;
                    next(dataStart, -1);
                }
            } else {
                //
                // We can binary search in the skip table to find the right
                // block of postings to look for this ID, but we really
                // only want to do that when we're sure that we won't find
                // the ID that we want in the current block of postings.
                //
                // On average, we'll have to decode about skipSize/2
                // postings to find the ID in a block of postings if we
                // start at the beginning of the block we find by binary
                // searching.
                //
                // The problem is that the query engine will typically be
                // calling findID with increasing IDs, so we might be
                // better off to continue from where we are, instead of
                // possibly jumping back to the beginning of the block and
                // re-decoding a bunch of stuff.  So, we keep track of the
                // block that we're checking and only binary search if we
                // need to.
                if(cb >= skipID.length || skipID[cb] < id || curr > id) {
                    int p = Arrays.binarySearch(skipID, id);
                    if(p < 0) {
                        p = -p - 2;
                    }
                    if(p == 0) {
                        //
                        // The first element is a catch all for the "first"
                        // skip, which means we start at the beginning of the
                        // entry, just as we do when there are no skips.
                        curr = 0;
                        next(dataStart, -1);
                    } else {
                        next(skipPos[p], skipID[p]);
                    }
                }
            }

            while(curr < id) {
                if(!next(-1, -1)) {
                    return false;
                }
            }

            return curr == id;
        }

        /**
         * Resets the iterator to the beginning of the entry.  Data will not be
         * decoded until the <code>next</code> method is called.
         */
        @Override
        public void reset() {
            rp.position(dataStart);
            curr = 0;
            done = nIDs == 0;
        }

        /**
         * Gets the ID that the iterator is currently pointing at.
         *
         * @return The ID that the iterator is pointing at, or 0 if the
         * iterator has not been advanced yet, or has been exhausted.
         */
        @Override
        public int getID() {
            return curr;
        }

        /**
         * Gets the weight associated with the current ID, as generated by
         * some weighting function.
         */
        @Override
        public float getWeight() {
            return 1;
        }

        /**
         * Gets the frequency associated with the current ID.
         */
        @Override
        public int getFreq() {
            return 1;
        }

        /**
         * Compares this postings iterator to another one.  The comparison
         * is based on the current ID that the iterator is pointing at.
         *
         * @return less than zero, 0, or greater than 0 if the ID at the
         * head of the given postings iterator is less than, equal to, or
         * greater than the ID at the head of this postings iterator,
         * respectively.
         */
        @Override
        public int compareTo(PostingsIterator other) {
            return getID() - other.getID();
        }
    }
}
