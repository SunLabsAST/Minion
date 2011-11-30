package com.sun.labs.minion.indexer.partition;

import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * A header for a partition, indicating where field information can be found
 * in the files.
 */
public class PartitionHeader {
    
    private static final Logger logger = Logger.getLogger(PartitionHeader.class.getName());
    
    /**
     * The types of postings files in this partition.
     */
    private String[] postingsChannelNames;

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
        int ncn = raf.readInt();
        postingsChannelNames = new String[ncn];
        Charset utf8 = Charset.forName("utf-8");
        for(int i = 0; i < postingsChannelNames.length; i++) {
            int nb = raf.readInt();
            byte[] bs = new byte[nb];
            raf.read(bs);
            postingsChannelNames[i] = new String(bs, utf8);
        }
    }

    public void addOffset(int id, long offset) {
        fieldOffsets.add(new FieldOffset(id, offset));
        nFields++;
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

    public String[] getPostingsChannelNames() {
        return postingsChannelNames;
    }

    public void setPostingsChannelNames(String[] postingsChannelNames) {
        this.postingsChannelNames = postingsChannelNames;
    }

    public void write(RandomAccessFile raf) throws java.io.IOException {
        raf.writeLong(docDictOffset);
        raf.writeInt(fieldOffsets.size());
        for(FieldOffset fo : fieldOffsets) {
            fo.write(raf);
        }
        raf.writeInt(nDocs);
        raf.writeInt(maxDocID);
        raf.writeInt(postingsChannelNames.length);
        Charset utf8 = Charset.forName("utf-8");
        for(String pcn : postingsChannelNames) {
            byte[] bs = pcn.getBytes(utf8);
            raf.writeInt(bs.length);
            raf.write(bs);
        }
   }
    
    public void write(WriteableBuffer buff) {
        buff.byteEncode(docDictOffset, 8);
        buff.byteEncode(fieldOffsets.size(), 4);
        for(FieldOffset fo : fieldOffsets) {
            fo.write(buff);
        }
        buff.byteEncode(nDocs, 4);
        buff.byteEncode(maxDocID, 4);
        buff.byteEncode(postingsChannelNames.length, 4);
        Charset utf8 = Charset.forName("utf-8");
        for(String pcn : postingsChannelNames) {
            buff.encodeAsBytes(pcn, utf8);
        }
    }

    @Override
    public String toString() {
        return "PartitionHeader{" + "nDocs=" + nDocs + " maxDocID=" + maxDocID +
                " nFields=" + nFields + " fieldOffsets=" + fieldOffsets +
                " docDictOffset=" + docDictOffset + 
                " postingsChannelNames=" + Arrays.toString(postingsChannelNames) +
                '}';
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

        public void write(WriteableBuffer b) {
            b.byteEncode(id, 4);
            b.byteEncode(offset, 8);
        }

        @Override
        public String toString() {
            return "FieldOffset{" + "id=" + id + " offset=" + offset + '}';
        }
    }

}
