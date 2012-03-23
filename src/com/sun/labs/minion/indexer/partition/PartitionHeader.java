package com.sun.labs.minion.indexer.partition;

import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import com.sun.labs.util.LabsLogFormatter;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A header for a partition, indicating where field information can be found in
 * the files.
 */
public class PartitionHeader {

    private static final Logger logger = Logger.getLogger(PartitionHeader.class.getName());

    /**
     * The provenance of this partition, describing where it came from.
     */
    private String provenance;

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
        read(raf);
    }

    /**
     * Creates a partition header from a file.  This is sensitive to the file
     * format and may break at any time. Do not use it unless you know what you're
     * doing.
     * 
     * Seriously.  Just don't.
     * 
     * @param f The file containing partition data.
     * @throws java.io.IOException 
     */
    public PartitionHeader(File f) throws java.io.IOException {
        RandomAccessFile raf = new RandomAccessFile(f, "r");
        long headerOffset = raf.readLong();
        raf.seek(headerOffset);
        read(raf);
        raf.close();
    }
    
    /**
     * Reads a partition header from a file that's been positioned at the point
     * where the header is.
     * @param raf the file to read from.
     * @throws java.io.IOException 
     */
    private void read(RandomAccessFile raf) throws java.io.IOException {
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
            postingsChannelNames[i] = readString(raf, utf8);
        }
        provenance = readString(raf, utf8);
        
    }

    private String readString(RandomAccessFile raf, Charset cs) throws IOException {
        int nb = raf.readInt();
        byte[] bs = new byte[nb];
        raf.read(bs);
        return new String(bs, cs);
    }

    public int getnFields() {
        return nFields;
    }

    public void setnFields(int nFields) {
        this.nFields = nFields;
    }

    public String getProvenance() {
        return provenance;
    }

    public void setProvenance(String provenance) {
        this.provenance = provenance;
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
            write(raf, pcn, utf8);
        }
        if(provenance == null) {
            provenance = "none";
        }
        write(raf, provenance, utf8);
    }

    private void write(RandomAccessFile file, String s, Charset cs) throws java.io.IOException {
        byte[] bs = s.getBytes(cs);
        file.writeInt(bs.length);
        file.write(bs);
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
        if(provenance != null) {
            buff.encodeAsBytes(provenance, utf8);
        } else {
            buff.encodeAsBytes("none", utf8);
        }
    }

    @Override
    public String toString() {
        return "PartitionHeader{" + "nDocs=" + nDocs + " maxDocID=" + maxDocID
                + " nFields=" + nFields + " fieldOffsets=" + fieldOffsets
                + " docDictOffset=" + docDictOffset
                + " postingsChannelNames=" + Arrays.toString(postingsChannelNames)
                + " provenance: %s" + provenance 
                + '}';
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

    public static void main(String[] args) {
        String flags = "d:";
        Getopt gopt = new Getopt(args, flags);
        int c;
        File indexDir = null;

        if(args.length == 0) {
            System.err.format("Usage PartitionHeader -d <index dir> [partNum] [partNum]...\n");
            return;
        }

        Logger l = Logger.getLogger("");
        for(Handler h : l.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new LabsLogFormatter());
        }
        while((c = gopt.getopt()) != -1) {
            switch(c) {

                case 'd':
                    indexDir = new File(gopt.optArg);
                    indexDir = new File(indexDir, "index");
                    break;
            }
        }

        if(indexDir == null) {
            System.err.format("Usage PartitionHeader -d <index dir> [partNum] [partNum]...\n");
            return;
        }

        for(int i = gopt.optInd; i < args.length; i++) {
            RandomAccessFile raf = null;
            File partFile = null;
            try {
                int partNumber = Integer.parseInt(args[i]);
                partFile = PartitionManager.makeDictionaryFile(indexDir.getAbsolutePath(), partNumber);
                PartitionHeader header = new PartitionHeader(partFile);
                System.out.format("%s\n", header.toString());
            } catch(NumberFormatException ex) {
                logger.log(Level.SEVERE, String.format("Bad partition number: %s", args[i]));
            } catch(IOException ex) {
                logger.log(Level.SEVERE, String.format("Error reading partition file %s", partFile));
            } finally {
                if(raf != null) {
                    try {
                        raf.close();
                    } catch(java.io.IOException ex) {
                    }
                }
            }
        }
    }
}
