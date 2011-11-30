package com.sun.labs.minion.indexer.partition;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * A header for a partition, indicating where field information can be found
 * in the files.
 */
public class PartitionHeader {

    /**
     * The number of fields in the partition.
     */
    private int nFields;

    /**
     * The offsets to where the fields can be found in the files.
     */
    private List<Long> fieldOffsets;

    /**
     * The offset of the document dictionary for the partition.
     */
    private long docDictOffset;

    private int n;

    public PartitionHeader() {
        fieldOffsets = new ArrayList<Long>();
    }

    public PartitionHeader(RandomAccessFile raf) throws java.io.IOException {
        docDictOffset = raf.readLong();
        nFields = raf.readInt();
        fieldOffsets = new ArrayList<Long>();
        for(int i = 0; i < nFields; i++) {
            fieldOffsets.add(raf.readLong());
        }
    }

    public void addOffset(long offset) {
        fieldOffsets.add(offset);
    }

    public List<Long> getFieldOffsets() {
        return new ArrayList<Long>(fieldOffsets);
    }

    public long getDocDictOffset() {
        return docDictOffset;
    }

    public void setDocDictOffset(long docDictOffset) {
        this.docDictOffset = docDictOffset;
    }


    public void write(RandomAccessFile raf) throws java.io.IOException {
        raf.writeLong(docDictOffset);
        raf.writeInt(nFields);
        for(long l : fieldOffsets) {
            raf.writeLong(l);
        }
   }

}
