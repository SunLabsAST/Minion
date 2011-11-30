package com.sun.labs.minion.indexer.partition;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * A header for a partition, indicating where field information can be found
 * in the files.
 */
public class PartitionHeader {

    /**
     * The number of documents in the partition.
     */
    private int nDocs;

    /**
     * The maximum document ID assigned in the partition.
     */
    private int maxDocID;

    /**
     * The number of fields in the partition.
     */
    private int nFields;

    /**
     * The offsets to where the fields can be found in the files.
     */
    private List<FieldOffset> fieldOffsets;

    /**
     * The offset of the document dictionary for the partition.
     */
    private long docDictOffset;

    public PartitionHeader() {
        fieldOffsets = new ArrayList<FieldOffset>();
    }

    public PartitionHeader(RandomAccessFile raf) throws java.io.IOException {
        docDictOffset = raf.readLong();
        nFields = raf.readInt();
        fieldOffsets = new ArrayList<FieldOffset>(nFields);
        for(int i = 0; i < nFields; i++) {
            fieldOffsets.add(new FieldOffset(raf));
        }
        nDocs = raf.readInt();
        maxDocID = raf.readInt();
    }

    public void addOffset(int id, long offset) {
        fieldOffsets.add(new FieldOffset(id, offset));
    }

    public List<FieldOffset> getFieldOffsets() {
        return new ArrayList<FieldOffset>(fieldOffsets);
    }

    public long getDocDictOffset() {
        return docDictOffset;
    }

    public void setDocDictOffset(long docDictOffset) {
        this.docDictOffset = docDictOffset;
    }

    public int getnDocs() {
        return nDocs;
    }

    public void setnDocs(int nDocs) {
        this.nDocs = nDocs;
    }

    public int getMaxDocID() {
        return maxDocID;
    }

    public void setMaxDocID(int maxDocID) {
        this.maxDocID = maxDocID;
    }

    public void write(RandomAccessFile raf) throws java.io.IOException {
        raf.writeLong(docDictOffset);
        raf.writeInt(fieldOffsets.size());
        for(FieldOffset fo : fieldOffsets) {
            fo.write(raf);
        }
        raf.writeInt(nDocs);
        raf.writeInt(maxDocID);
   }

    @Override
    public String toString() {
        return "PartitionHeader{" + "nDocs=" + nDocs + " maxDocID=" + maxDocID +
                " nFields=" + nFields + " fieldOffsets=" + fieldOffsets +
                " docDictOffset=" + docDictOffset + '}';
    }

    protected static class FieldOffset {

        private long offset;

        private int id;

        public FieldOffset(int id, long offset) {
            this.id = id;
            this.offset = offset;
        }

        public FieldOffset(RandomAccessFile raf) throws IOException {
            id = raf.readInt();
            offset = raf.readLong();
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public long getOffset() {
            return offset;
        }

        protected void setOffset(long offset) {
            this.offset = offset;
        }

        public void write(RandomAccessFile raf) throws java.io.IOException {
            raf.writeInt(id);
            raf.writeLong(offset);
        }

        @Override
        public String toString() {
            return "FieldOffset{" + "id=" + id + " offset=" + offset + '}';
        }
    }

}
