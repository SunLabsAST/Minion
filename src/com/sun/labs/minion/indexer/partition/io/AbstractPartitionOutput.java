package com.sun.labs.minion.indexer.partition.io;

import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary.IDMap;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary.Renumber;
import com.sun.labs.minion.indexer.dictionary.NameEncoder;
import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.partition.MemoryPartition;
import com.sun.labs.minion.indexer.partition.PartitionHeader;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.util.FileLockException;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * An abstract base class for partition outputs that handles some of the boring
 * stuff.
 */
public abstract class AbstractPartitionOutput implements PartitionOutput {

    private static final Logger logger = Logger.getLogger(AbstractPartitionOutput.class.getName());

    private boolean longIndexingRun;

    /**
     * The partition being output.
     */
    protected MemoryPartition partition;

    /**
     * The partition manager for the partition that is being dumped.
     */
    protected PartitionManager partitionManager;

    /**
     * A header for the partition being dumped.
     */
    protected PartitionHeader partitionHeader;

    /**
     * The document keys for the partition that we're dumping.
     */
    protected Set<String> keys;

    /**
     * The number of the partition that is being dumped.
     */
    protected int partitionNumber;

    /**
     * An encoder for names during a dictionary dump.
     */
    protected NameEncoder encoder;

    /**
     * A dictionary output for this dump.
     */
    protected DictionaryOutput partDictOut;

    /**
     * How IDs in the dictionary should be remapped.
     */
    protected MemoryDictionary.IDMap idMap;

    /**
     * A postings ID map to use when dumping.
     */
    protected int[] postingsIDMap;

    /**
     * The files where we're putting the postings.
     */
    protected File[] postOutFiles;

    /**
     * A place to put postings.
     */
    protected PostingsOutput[] postOut;

    /**
     * How entries should be renumbered.
     */
    protected MemoryDictionary.Renumber renumber;

    /**
     * A buffer for writing vector lengths.
     */
    protected WriteableBuffer vectorLengthsBuffer;

    /**
     * A buffer where deletions can be written.
     */
    protected WriteableBuffer deletionsBuffer;

    private String name;

    private boolean started = false;

    public AbstractPartitionOutput(PartitionManager manager) throws IOException {
        this.partitionManager = manager;
    }

    public AbstractPartitionOutput(File outputDir) throws IOException {
        partitionManager = new PartitionManager(outputDir);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public PartitionManager getPartitionManager() {
        return partitionManager;
    }

    @Override
    public int startPartition(MemoryPartition partition) throws IOException {
        if(started) {
            throw new IllegalStateException("Already outputting a partition, can't start another");
        }
        try {
            this.partition = partition;
            partitionHeader = new PartitionHeader();
            partitionNumber = partitionManager.getMetaFile().getNextPartitionNumber();
            postOutFiles = partitionManager.makePostingsFiles(partitionNumber,
                    partition.getPostingsChannelNames());
        } catch(FileLockException ex) {
            throw new IOException("Error getting partition number", ex);
        }
        keys = null;
        return partitionNumber;
    }

    @Override
    public int startPartition(String[] postingsChannelNames) throws IOException {
        if(started) {
            throw new IllegalStateException("Already outputting a partition, can't start another");
        }
        try {
            partitionHeader = new PartitionHeader();
            partitionHeader.setPostingsChannelNames(postingsChannelNames);
            partitionNumber = partitionManager.getMetaFile().getNextPartitionNumber();
            postOutFiles = partitionManager.makePostingsFiles(partitionNumber, postingsChannelNames);
        } catch(FileLockException ex) {
            throw new IOException("Error getting partition number", ex);
        }
        keys = null;
        return partitionNumber;

    }

    @Override
    public void setPartitionNumber(int partitionNumber) {
        this.partitionNumber = partitionNumber;
    }

    @Override
    public void setKeys(Set<String> keys) {
        this.keys = new HashSet<String>(keys);
    }

    @Override
    public WriteableBuffer getDeletionsBuffer() {
        return deletionsBuffer;
    }

    @Override
    public int getMaxDocID() {
        return partitionHeader.getMaxDocID();
    }

    @Override
    public int getNDocs() {
        return partitionHeader.getnDocs();
    }

    @Override
    public DictionaryOutput getPartitionDictionaryOutput() {
        return partDictOut;
    }

    @Override
    public PartitionHeader getPartitionHeader() {
        return partitionHeader;
    }

    @Override
    public int getPartitionNumber() {
        return partitionNumber;
    }

    @Override
    public File[] getPostingsFiles() {
        return postOutFiles;
    }

    @Override
    public PostingsOutput[] getPostingsOutput() {
        return postOut;
    }

    @Override
    public void setPostingsOutput(PostingsOutput[] postOut) {
        this.postOut = postOut;
    }

    @Override
    public MemoryPartition getPartition() {
        return partition;
    }

    @Override
    public WriteableBuffer getVectorLengthsBuffer() {
        return vectorLengthsBuffer;
    }

    @Override
    public void setDictionaryEncoder(NameEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public NameEncoder getDictionaryEncoder() {
        return encoder;
    }

    @Override
    public void setDictionaryIDMap(IDMap idMap) {
        this.idMap = idMap;
    }

    @Override
    public IDMap getDictionaryIDMap() {
        return idMap;
    }

    @Override
    public void setDictionaryRenumber(Renumber renumber) {
        this.renumber = renumber;
    }

    @Override
    public Renumber getDictionaryRenumber() {
        return renumber;
    }

    @Override
    public void setMaxDocID(int maxDocID) {
        partitionHeader.setMaxDocID(maxDocID);
    }

    @Override
    public void setNDocs(int nDocs) {
        partitionHeader.setnDocs(nDocs);
    }

    @Override
    public void setPostingsIDMap(int[] postingsIDMap) {
        this.postingsIDMap = postingsIDMap;
    }

    @Override
    public int[] getPostingsIDMap() {
        return postingsIDMap;
    }

    @Override
    public boolean isLongIndexingRun() {
        return longIndexingRun;
    }

    public void setLongIndexingRun(boolean longIndexingRun) {
        this.longIndexingRun = longIndexingRun;
    }

    @Override
    public void flushVectorLengths() throws IOException {
        if(vectorLengthsBuffer.position() > 0) {
            //
            // The vector lengths.
            RandomAccessFile vectorLengthsFile = new RandomAccessFile(partitionManager.makeVectorLengthFile(partitionNumber), "rw");
            vectorLengthsBuffer.write(vectorLengthsFile.getChannel());
            vectorLengthsBuffer.clear();
            vectorLengthsFile.close();
        }
    }

    @Override
    public void flush() throws IOException {

        //
        // Flush the main dictionary output to our dictionary file.
        RandomAccessFile dictFile = new RandomAccessFile(partitionManager.makeDictionaryFile(partitionNumber), "rw");
        partDictOut.flush(dictFile);
        dictFile.close();

        if(postOut != null) {
            for(PostingsOutput po : postOut) {
                po.flush();
            }
        }

        if(vectorLengthsBuffer.position() > 0) {
            //
            // The vector lengths.
            RandomAccessFile vectorLengthsFile = new RandomAccessFile(partitionManager.makeVectorLengthFile(partitionNumber), "rw");
            vectorLengthsBuffer.write(vectorLengthsFile.getChannel());
            vectorLengthsBuffer.clear();
            vectorLengthsFile.close();
        }

        //
        // Deleted docs.
        if(deletionsBuffer.countBits() > 0) {
            RandomAccessFile delFile = new RandomAccessFile(partitionManager.makeDeletedDocsFile(partitionNumber), "rw");
            deletionsBuffer.write(delFile);
            deletionsBuffer.clear();
            delFile.close();
        }

        if(keys != null && !keys.isEmpty()) {
            partitionManager.addNewPartition(partitionNumber, keys);
        }
        started = false;
    }

    @Override
    public void cleanUp() {
        partDictOut.cleanUp();
        if(postOut != null) {
            for(PostingsOutput po : postOut) {
                po.cleanUp();
            }
        }
        vectorLengthsBuffer.clear();
        deletionsBuffer.clear();
    }

    public void close() throws IOException {
        if(partDictOut != null) {
            partDictOut.close();
        }
        if(postOut != null) {
            for(PostingsOutput po : postOut) {
                po.flush();
            }
        }
    }

    @Override
    public String toString() {
        return "APO: " + name + ' ' + partition + " partition number: " + partitionNumber;
    }
}
