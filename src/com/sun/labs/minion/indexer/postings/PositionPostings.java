package com.sun.labs.minion.indexer.postings;

import com.sun.labs.minion.indexer.postings.io.PostingsInput;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.Buffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A postings class that holds documents, frequencies and term positions.
 */
public class PositionPostings implements Postings {
    
    private static final Logger logger = Logger.getLogger(PositionPostings.class.getName());
    
    /**
     * The position where we're collecting data.
     */
    protected int pos = -1;

    /**
     * The position where we're collecting positions.
     */
    protected int posPos = 0;

    /**
     * The total number of occurrences in the postings list.  Note that this is
     * a long, even though the return value from getTotalOccurrences is an int.
     * This is because, while it doesn't make any sense to return a long's worth 
     * of counts, we may collect more than an int's worth.
     */
    protected long totalOccurrences;

    /**
     * The maximum frequency.
     */
    protected int maxfdt;

    protected int[] ids;
    
    /**
     * The frequencies for these postings.
     */
    protected int[] freqs;

    /**
     * The positions of occurrence for these postings.
     */
    protected int[] posns;
    
    protected Buffer idBuff;
    
    protected Buffer posnBuff;
    
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
     * The last positions offset in this postings list.
     */
    protected int lastPosnOffset;

    /**
     * The IDs in the skip table.
     */
    protected int[] skipID;

    /**
     * The positions in the id/frequency postings associated with the entry
     * ids in the skip table.
     */
    protected int[] skipIDOffsets;

    /**
     * The offsets in the position postings associated with the entry
     * ids in the skip table.
     */
    protected int[] skipPosnOffsets;

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
    protected int skipSize = 256;

    PostingsInput posnInput;
    
    long posnOffset;
    
    int posnSize;
    
    public PositionPostings() {
        currentID = -1;
        ids = new int[8];
        freqs = new int[ids.length];
        posns = new int[ids.length];
    }

    /**
     * Makes a postings entry that is useful during querying.  Note that we're
     * only ever constructed with one of our buffers, we'll read the other
     * later if we need it.
     *
     * @param idBuffer the data read from a postings file.
     */
    public PositionPostings(PostingsInput[] in, long[] offset, int[] size) throws IOException {
        this(in[0].read(offset[0], size[0]), 0, 0);
        posnInput = in[1];
        posnOffset = offset[1];
        posnSize = size[1];
    }

    /**
     * Makes a postings entry that is useful during querying.
     *
     * @param b the data read from a postings file.
     * @param offset The offset in the buffer from which we should start
     * reading.  If this value is greater than 0, then we need to share the
     * bit buffer, since we may be part of a larger postings entry that
     * will need multiple readers.
     * @param size the size of the postings in bytes
     */
    public PositionPostings(ReadableBuffer idBuff, int offset, int size) {
        if(offset > 0) {
            this.idBuff = idBuff.slice(offset, size);
        } else {
            this.idBuff = idBuff;
        }

        //
        // Get the initial data.
        nIDs = ((ReadableBuffer) this.idBuff).byteDecode();
        lastID = ((ReadableBuffer) this.idBuff).byteDecode();
        lastPosnOffset = ((ReadableBuffer) this.idBuff).byteDecode();

        //
        // Decode the skip table.
        nSkips = ((ReadableBuffer) this.idBuff).byteDecode();

        if(nSkips > 0) {

            skipID = new int[nSkips + 1];
            skipIDOffsets = new int[nSkips + 1];
            skipPosnOffsets = new int[nSkips + 1];
            int currSkipDoc = 0;
            int currIDOffset = 0;
            int currPosnOffset = 0;
            for(int i = 1; i <= nSkips; i++) {
                currSkipDoc += ((ReadableBuffer) this.idBuff).byteDecode();
                currIDOffset += ((ReadableBuffer) this.idBuff).byteDecode();
                currPosnOffset += ((ReadableBuffer) this.idBuff).byteDecode();
                skipID[i] = currSkipDoc;
                skipIDOffsets[i] = currIDOffset;
                skipPosnOffsets[i] = currPosnOffset;
            }

            //
            // Now, the values for the positions of the entries are
            // from the *end* of the skip table, and we want them to be
            // from the beginning of the buffer so that we can use
            // jump directly to them.  So we need to figure out
            // how much to add.
            dataStart = (int) this.idBuff.position();
            for(int i = 0; i <= nSkips; i++) {
                skipIDOffsets[i] += dataStart;
            }
        } else {
            dataStart = (int) this.idBuff.position();
        }
    }

    @Override
    public String[] getChannelNames() {
        return new String[]{"post", "prox"};
    }
    
    @Override
    public void add(Occurrence o) {
        if(!(o instanceof FieldOccurrence)) {
            throw new IllegalArgumentException("Position Postings require positions!");
        }
        int oid = o.getID();
        if(oid != currentID) {
            nIDs++;
            pos++;
            if(nIDs >= ids.length) {
                int nl = nIDs + 128;
                ids = Arrays.copyOf(ids, nl);
                freqs = Arrays.copyOf(freqs, nl);
            }
            ids[pos] = oid;
            freqs[pos] = o.getCount();
            currentID = oid;
            lastID = currentID;
        } else {
            freqs[pos] += o.getCount();
        }
        if(posPos >= posns.length) {
            posns = Arrays.copyOf(posns, posns.length + 128);
        }
        posns[posPos++] = ((FieldOccurrence) o).getPos();
    }
    
    /**
     * Adds a skip to the skip table.
     *
     * @param id The ID that the skip is pointing to.
     * @param idOffset The position in the postings to skip to.
     */
    protected void addSkip(int id, int idOffset, int posnOffset) {
        if(skipID == null) {
            skipID = new int[4];
            skipIDOffsets = new int[4];
            skipPosnOffsets = new int[4];
        } else if(nSkips + 1 >= skipID.length) {
            skipID = Arrays.copyOf(skipID, skipID.length * 2);
            skipIDOffsets = Arrays.copyOf(skipIDOffsets, skipID.length);
            skipPosnOffsets = Arrays.copyOf(skipPosnOffsets, skipID.length);
        }
        skipID[nSkips] = id;
        skipIDOffsets[nSkips] = idOffset;
        skipPosnOffsets[nSkips] = posnOffset;
        nSkips++;
    }
    
    @Override
    public void write(PostingsOutput[] out, long[] offset, int[] size) throws java.io.IOException {
        
        //
        // Encode the postings data that's in our arrays, if there is any.
        encodeIDs();
        
        //
        // Encode the header data for the postings.
        WriteableBuffer headerBuff = out[0].getTempBuffer();
        encodeHeaderData(headerBuff);

        //
        // Write the buffers.
        offset[0] = out[0].position();
        size[0] = out[0].write(new WriteableBuffer[]{headerBuff, (WriteableBuffer) idBuff});
        offset[1] = out[1].position();
        size[1] = out[1].write((WriteableBuffer) posnBuff);
    }

    protected void encodeIDs() {

        if(idBuff == null) {
            idBuff = new ArrayBuffer(nIDs * 2);
            posnBuff = new ArrayBuffer(nIDs * 2);
        }
        if(idBuff.position() > 0) {
            
            //
            // If there's data in the buffer already then we've already been
            // called or we were appending, so we're done.
            return;
        }
        WriteableBuffer wIDBuff = (WriteableBuffer) idBuff;
        WriteableBuffer wPosnBuff = (WriteableBuffer) posnBuff;

        int prevID = 0;
        lastPosnOffset = 0;
        int pp = 0;

        for(int i = 0; i < nIDs; i++) {
            if(i > 0 && i % skipSize == 0) {
                addSkip(ids[i], (int) wIDBuff.position(), (int) wPosnBuff.position());
            }
            wIDBuff.byteEncode(ids[i] - prevID);
            wIDBuff.byteEncode(freqs[i]);
            wIDBuff.byteEncode(wPosnBuff.position() - lastPosnOffset);
            lastPosnOffset = (int) wPosnBuff.position();
            prevID = ids[i];
            int prevPosn = 0;
            for(int j = 0; j < freqs[i]; j++, pp++) {
                int currPosn = posns[pp];
                try {
                wPosnBuff.byteEncode(currPosn - prevPosn);
                } catch (ArithmeticException ex) {
                    logger.log(Level.SEVERE, String.format("Error encoding position %d (prev %d) at %d (numposns: %d) for doc %d", 
                            currPosn, prevPosn, 
                            pp, posPos, prevID));
                    throw ex;
                }
                prevPosn = currPosn;
            }
        }
    }
    
    protected void encodeHeaderData(WriteableBuffer headerBuff) {
        //
        // Encode the number of IDs and the last ID
        headerBuff.byteEncode(nIDs);
        headerBuff.byteEncode(lastID);
        headerBuff.byteEncode(lastPosnOffset);

        //
        // Encode the skip table.
        headerBuff.byteEncode(nSkips);
        int prevSkipID = 0;
        int prevIDOffset = 0;
        int prevPosnOffset = 0;
        for(int i = 0; i < nSkips; i++) {
            headerBuff.byteEncode(skipID[i] - prevSkipID);
            headerBuff.byteEncode(skipIDOffsets[i] - prevIDOffset);
            headerBuff.byteEncode(skipPosnOffsets[i] - prevPosnOffset);
            prevSkipID = skipID[i];
            prevIDOffset = skipIDOffsets[i];
            prevPosnOffset = skipPosnOffsets[i];
        }

    }

    public Type getType() {
        return Type.ID_FREQ_POS;
    }

    public int getN() {
        return nIDs;
    }

    public int getLastID() {
        return lastID;
    }

    public long getTotalOccurrences() {
        return totalOccurrences;
    }

    public int getMaxFDT() {
        return maxfdt;
    }

    public void remap(int[] idMap) {
    }

    public void append(Postings p, int start) {
        PositionPostings other = (PositionPostings) p;
        other.readPositions();

        //
        // Check for empty postings on the other side.
        if(other.nIDs == 0) {
            return;
        }

        if(idBuff == null) {
            idBuff = new ArrayBuffer((int) other.idBuff.limit());
            posnBuff = new ArrayBuffer((int) other.posnBuff.limit());
        }

        //
        // We'll need to know where we started this entry.
        int idOffset = (int) idBuff.position();
        int mPosnOffset = (int) posnBuff.position();

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
        int firstFreq = ((ReadableBuffer) other.idBuff).byteDecode();
        ((ReadableBuffer) other.idBuff).byteDecode();
        adj = (int) (other.idBuff.position() - adj);

        //
        // Get the first document sequence number off the entry we're
        // appending and replace it with a document gap that
        // incorporates the new starting sequence number.
        int newID = firstID + start - 1;
        int nb = ((WriteableBuffer) idBuff).byteEncode(newID - lastID);
        nb += ((WriteableBuffer) idBuff).byteEncode(firstFreq);
        nb += ((WriteableBuffer) idBuff).byteEncode(mPosnOffset - lastPosnOffset);
        adj = nb - adj;

        //
        // Get a buffer for the remaining postings, and make sure that it
        // looks like a buffer that was written and not read by slicing and
        // manipulating the position.
        
        //
        // We can just append the positions.
        ((WriteableBuffer) idBuff).append((ReadableBuffer) other.idBuff);
        ((WriteableBuffer) posnBuff).append((ReadableBuffer) other.posnBuff);

        //
        // The last ID on this entry is now the last ID from the entry we
        // appended, suitably remapped.
        lastID = other.lastID + start - 1;
        lastPosnOffset = mPosnOffset + other.lastPosnOffset;
        
        //
        // Increment the number of documents in this new entry.
        nIDs += other.nIDs;
        
        if(other.nSkips > 0) {

            //
            // Now we need to fix up the skip table.  The skip postions in
            // the other entry had the length of the initial part of the
            // compressed representation added to them, so we need to fix
            // that and replace the document IDs with something reasonable.
            // First we need to make the skip table big enough to hold all
            // this data.
            if(skipID != null) {
                skipID = Arrays.copyOf(skipID, skipID.length + other.nSkips + 1);
                skipIDOffsets = Arrays.copyOf(skipIDOffsets, skipIDOffsets.length + other.nSkips + 1);
                skipPosnOffsets = Arrays.copyOf(skipPosnOffsets, skipPosnOffsets.length + other.nSkips + 1);
            } else {
                skipID = new int[other.nSkips + 1];
                skipIDOffsets = new int[other.nSkips + 1];
                skipPosnOffsets = new int[other.nSkips + 1];
            }

            //
            // Now fix up the other skips.
            for(int i = 1; i <= other.nSkips; i++) {
               addSkip(other.skipID[i] + start - 1, 
                       other.skipIDOffsets[i] - other.dataStart + idOffset + adj, 
                       other.skipPosnOffsets[i] + mPosnOffset);
            }
        }
    }

    public void append(Postings p, int start, int[] idMap) {
        
        //
        // Set up the buffers on the first call.
        if(idBuff == null) {
            idBuff = new ArrayBuffer(p.getN());
            posnBuff = new ArrayBuffer(p.getN());
        }

        if(idMap == null) {
            append(p, start);
            return;
        }
        
        ((PositionPostings) p).readPositions();
        
        //
        // We'll iterate through the postings.
        PostingsIteratorWithPositions pi = (PostingsIteratorWithPositions) p.iterator(null);
        WriteableBuffer wIDBuff = (WriteableBuffer) idBuff;
        WriteableBuffer wPosnBuff = (WriteableBuffer) posnBuff;
        while(pi.next()) {
            int origID = pi.getID();
            int mapID = idMap[origID];

            //
            // Skip deleted documents.
            if(mapID < 0) {
                continue;
            }

            //
            // Increment our ID count, and see if we need to add a skip.
            nIDs++;
            if(nIDs % skipSize == 0) {
                addSkip(mapID, (int) idBuff.position(), (int) posnBuff.position());
            }

            wIDBuff.byteEncode(mapID - lastID);
            wIDBuff.byteEncode(pi.getFreq());
            wIDBuff.byteEncode(wPosnBuff.position() - lastPosnOffset);
            lastPosnOffset = (int) wPosnBuff.position();
            int freq = pi.getFreq();
            int[] tp = pi.getPositions();
            for(int i = 0; i < freq; i++) {
                wPosnBuff.byteEncode(tp[i]);
            }

            //
            // Set the new last document for our entry.
            lastID = mapID;
        }
    }
    
    private void readPositions() {
        if(posnBuff == null) {
            try {
                posnBuff = posnInput.read(posnOffset, posnSize);
            } catch(IOException ex) {
                logger.log(Level.SEVERE, String.format("Unable to read positions"), ex);
            }            
        }
    }

    public PostingsIterator iterator(PostingsIteratorFeatures features) {
        if(pos >= 0) {
            return new UncompressedIterator(features);
        } else {
            if(features != null && features.positions && (posnBuff == null || posnBuff.position() == 0)) {
                readPositions();
            }
            return new CompressedIterator(features);
        }
    }

    public void clear() {
        if(ids == null) {
            return;
        }
        nIDs = 0;
        currentID = -1;
        lastID = 0;
        nSkips = 0;
        pos = -1;
        posPos = 0;
        if(idBuff != null) {
            ((WriteableBuffer) idBuff).clear();
            ((WriteableBuffer) posnBuff).clear();
        }
    }
    
    public String toString() {
        if(idBuff != null) {
            return idBuff.toString(dataStart, dataStart + 10, Buffer.DecodeMode.BYTE_ENCODED);
        } else {
            return null;
        }
    }
    
    /**
     * A postings iterator than can be used for in-memory data.
     */
    public class UncompressedIterator implements PostingsIteratorWithPositions {

        int currPos = -1;

        PostingsIteratorFeatures features;

        private int ourN;
        
        private int currPosnPos = 0;
        
        private int freqSum = 0;
        
        private boolean gettingPositions;
        
        private int[] currPosns;

        /**
         * The weighting function.
         */
        protected WeightingFunction wf;

        /**
         * A set of weighting components.
         */
        protected WeightingComponents wc;

        public UncompressedIterator(PostingsIteratorFeatures features) {
            this.features = features;
            if(features != null) {
                wf = features.getWeightingFunction();
                wc = features.getWeightingComponents();
                gettingPositions = features.getPositions();
                if(gettingPositions) {
                    currPosns = new int[4];
                }
            }
            ourN = nIDs;
        }

        public int getN() {
            return ourN;
        }

        public boolean next() {
            boolean ret = ourN != 0 && ++currPos < ourN;
            if(ret && gettingPositions) {
                if(currPos > 0) {
                    currPosnPos += freqs[currPos-1];
                }
            }
            return ret;
        }

        public boolean findID(int id) {
            currPos = Arrays.binarySearch(ids, 0, ourN, id);
            boolean ret = false;
            
            if(currPos > 0) {
                ret = true;
            } else {
                currPos = -currPos;
            }
            //
            // Bummer:  if we're supposed to be getting postions, 
            // we need to figure out where the offsets are.
            if(gettingPositions) {
                currPosnPos = 0;
                for(int i = 0; i < currPos; i++) {
                    currPosnPos += freqs[i];
                }
            }
            return ret;
        }

        public void reset() {
            currPos = -1;
            currPosnPos = 0;
        }

        public int getID() {
            return ids[currPos];
        }

        public int getFreq() {
            return freqs[currPos];
        }

        public float getWeight() {
            if(wf == null) {
                return getFreq();
            }
            wc.fdt = getFreq();
            return wf.termWeight(wc);
        }

        public int[] getPositions() {
            int f = freqs[currPos];
            if(f >= currPosns.length) {
                currPosns = new int[f];
            }
            for(int i = 0, j = currPosnPos; i < f; i++, j++) {
                currPosns[i] = posns[j];
            }
            return currPosns;
        }

        public int compareTo(PostingsIterator other) {
            return getID() - other.getID();
        }

        public PostingsIteratorFeatures getFeatures() {
            return features;
        }
    }
    
    /**
     * A postings iterator that can be used for compressed data that's been
     * read from disk.
     */
    public class CompressedIterator implements PostingsIteratorWithPositions {

        /**
         * A readable buffer for the id/freq postings.
         */
        protected ReadableBuffer ridf;
        
        /**
         * A readable buffer for the positions, if needed.
         */
        protected ReadableBuffer rPosn;

        /**
         * Whether we've finished the entry.
         */
        protected boolean done;

        /**
         * The current ID.
         */
        protected int currID;
        
        /**
         * The current freq.
         */
        protected int currFreq;
        
        /**
         * The current offset into the position postings.
         */
        protected int currOffset;
        
        protected int[] currPosns;

        /**
         * The weighting function.
         */
        protected WeightingFunction wf;

        /**
         * A set of weighting components.
         */
        protected WeightingComponents wc;

        /**
         * The current block of postings that we've jumped into using findID.
         */
        protected int cb;

        private PostingsIteratorFeatures features;

        /**
         * Creates a postings iterator for this postings type.
         */
        public CompressedIterator(PostingsIteratorFeatures features) {
            this.features = features;
            ridf = ((ReadableBuffer) idBuff).duplicate();
            ridf.position(dataStart);
            done = nIDs == 0;
            if(features != null) {
                wf = features.getWeightingFunction();
                wc = features.getWeightingComponents();
                if(features.positions) {
                    if(posnBuff == null) {
                        logger.log(Level.SEVERE, String.format("Unable to get positions"));
                    } else {
                        rPosn = ((ReadableBuffer) posnBuff).duplicate();
                    }
                }
            }
        }

        /**
         * Gets the number of IDs in this postings list.
         */
        public int getN() {
            return nIDs;
        }

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
        public boolean next() {
            return next(-1, -1, -1);
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
        protected boolean next(int pos, int id, int offset) {
            if(done) {
                return false;
            }

            //
            // If we were given a position, then we position there.
            if(pos > 0) {
                ridf.position(pos);

                if(id > -1) {
                    //
                    // We'll use the offset and id that were passed in, likely
                    // from a skip table, and ignore the decode values for those
                    // elements.
                    currID = id;
                    currOffset = offset;
                    ridf.byteDecode();
                    currFreq = ridf.byteDecode();
                    ridf.byteDecode();
                } else {
                    currID += ridf.byteDecode();
                    currFreq = ridf.byteDecode();
                    currOffset += ridf.byteDecode();
                }
            } else {
                currID += ridf.byteDecode();
                currFreq = ridf.byteDecode();
                currOffset += ridf.byteDecode();
            }

            done = currID == lastID;

            return true;
        }

        public int[] getPositions() {
            if(rPosn != null) {
                rPosn.position(currOffset);
                if(currPosns == null || currFreq >= currPosns.length) {
                    currPosns = new int[currFreq];
                }
                int prevPosn = 0;
                for(int i = 0; i < currFreq; i++) {
                    prevPosn += rPosn.byteDecode();
                    currPosns[i] = prevPosn;
                }
            }
            return currPosns;
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

            //
            // Set up.  Start at the beginning or skip to the right place.
            if(nSkips == 0) {
                if(id < currID) {
                    currID = 0;
                    next(dataStart, -1, -1);
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
                if(cb >= skipID.length || skipID[cb] < id || currID > id) {
                    int p = Arrays.binarySearch(skipID, id);
                    if(p < 0) {
                        p = -p - 2;
                    }
                    if(p == 0) {
                        //
                        // The first element is a catch all for the "first"
                        // skip, which means we start at the beginning of the
                        // entry, just as we do when there are no skips.
                        currID = 0;
                        next(dataStart, -1, -1);
                    } else {
                        next(skipIDOffsets[p], skipID[p], skipPosnOffsets[p]);
                    }
                }
            }

            while(currID < id) {
                if(!next(-1, -1, -1)) {
                    return false;
                }
            }

            return currID == id;
        }

        /**
         * Resets the iterator to the beginning of the entry.  Data will not be
         * decoded until the <code>next</code> method is called.
         */
        public void reset() {
            ridf.position(dataStart);
            currID = 0;
            currOffset = 0;
            done = nIDs == 0;
        }

        /**
         * Gets the ID that the iterator is currently pointing at.
         *
         * @return The ID that the iterator is pointing at, or 0 if the
         * iterator has not been advanced yet, or has been exhausted.
         */
        public int getID() {
            return currID;
        }

        /**
         * Gets the weight associated with the current ID, as generated by
         * some weighting function.
         */
        public float getWeight() {
            if(wf == null) {
                return currFreq;
            }
            wc.fdt = currFreq;
            return wf.termWeight(wc);
        }

        /**
         * Gets the frequency associated with the current ID.
         */
        public int getFreq() {
            return currFreq;
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
        public int compareTo(PostingsIterator other) {
            return getID() - ((PostingsIterator) other).getID();
        }
    }
}
