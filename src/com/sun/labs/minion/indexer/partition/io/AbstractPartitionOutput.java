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
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;

/**
 * An abstract base class for partition outputs that handles some of the 
 * boring stuff.
 */
public abstract class AbstractPartitionOutput implements PartitionOutput {

    private static final Logger logger = Logger.getLogger(AbstractPartitionOutput.class.getName());

    /**
     * The partition manager for the partition that is being dumped.
     */
    protected PartitionManager manager;

    /**
     * A header for the partition being dumped.
     */
    protected PartitionHeader partHeader;

    /**
     * The number of the partition that is being dumped.
     */
    protected int partNumber;

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
     * A dictionary output for the term stats for the entire collection.
     */
    protected DictionaryOutput termStatsDictOut;

    /**
     * A buffer for writing vector lengths.
     */
    protected WriteableBuffer vectorLengthsBuffer;

    /**
     * A buffer where deletions can be written.
     */
    protected WriteableBuffer deletionsBuffer;

    private boolean started = false;

    public AbstractPartitionOutput(PartitionManager manager) throws IOException {
        this.manager = manager;
    }

    public int startPartition() throws IOException {
        if(started) {
            throw new IllegalStateException("Already outputting a partition, can't start another");
        }
        try {
            partHeader = new PartitionHeader();
            partNumber = manager.getMetaFile().getNextPartitionNumber();
            File[] files = MemoryPartition.getMainFiles(manager, partNumber);
            postOutFiles = Arrays.copyOfRange(files, 1, files.length);
        } catch(FileLockException ex) {
            throw new IOException("Error getting partition number", ex);
        }
        return partNumber;
    }

    public WriteableBuffer getDeletionsBuffer() {
        return deletionsBuffer;
    }

    public int getMaxDocID() {
        return partHeader.getMaxDocID();
    }

    public int getNDocs() {
        return partHeader.getnDocs();
    }

    public DictionaryOutput getPartitionDictionaryOutput() {
        return partDictOut;
    }

    public PartitionHeader getPartitionHeader() {
        return partHeader;
    }

    public int getPartitionNumber() {
        return partNumber;
    }

    public File[] getPostingsFiles() {
        return postOutFiles;
    }

    public PostingsOutput[] getPostingsOutput() {
        return postOut;
    }

    public void setPostingsOutput(PostingsOutput[] postOut) {
        this.postOut = postOut;
    }

    public DictionaryOutput getTermStatsDictionaryOutput() {
        return termStatsDictOut;
    }

    public WriteableBuffer getVectorLengthsBuffer() {
        return vectorLengthsBuffer;
    }

    public void setDictionaryEncoder(NameEncoder encoder) {
        this.encoder = encoder;
    }

    public NameEncoder getDictionaryEncoder() {
        return encoder;
    }

    public void setDictionaryIDMap(IDMap idMap) {
        this.idMap = idMap;
    }

    public IDMap getDictionaryIDMap() {
        return idMap;
    }

    public void setDictionaryRenumber(Renumber renumber) {
        this.renumber = renumber;
    }

    public Renumber getDictionaryRenumber() {
        return renumber;
    }

    public void setMaxDocID(int maxDocID) {
        partHeader.setMaxDocID(maxDocID);
    }

    public void setNDocs(int nDocs) {
        partHeader.setnDocs(nDocs);
    }

    public void setPostingsIDMap(int[] postingsIDMap) {
        this.postingsIDMap = postingsIDMap;
    }

    public int[] getPostingsIDMap() {
        return postingsIDMap;
    }

    public void flush(Set<String> keys) throws IOException {

        //
        // Flush the main dictionary output to our dictionary file.
        RandomAccessFile dictFile = new RandomAccessFile(manager.makeDictionaryFile(partNumber), "rw");
        partDictOut.flush(dictFile);
        dictFile.close();

        if(postOut != null) {
            for(PostingsOutput po : postOut) {
                po.flush();
            }
        }

        //
        // The vector lengths.
        RandomAccessFile vectorLengthsFile = new RandomAccessFile(manager.makeVectorLengthFile(partNumber), "rw");
        vectorLengthsBuffer.write(vectorLengthsFile.getChannel());
        vectorLengthsFile.close();

        //
        // Deleted docs.
        if(deletionsBuffer.countBits() > 0) {
            RandomAccessFile delFile = new RandomAccessFile(manager.makeDeletedDocsFile(partNumber), "rw");
            deletionsBuffer.write(delFile);
        }

        //
        // Dump the new term stats dictionary, if there is one.  There won't be if
        // this partition output was used for merging.
        if(termStatsDictOut != null) {
            try {
                int tsn = manager.getMetaFile().getNextTermStatsNumber();

                RandomAccessFile termStatsFile = new RandomAccessFile(manager.makeTermStatsFile(tsn), "rw");
                termStatsDictOut.flush(termStatsFile);
                termStatsFile.close();

                manager.getMetaFile().setTermStatsNumber(tsn);
                manager.updateTermStats();
            } catch(FileLockException ex) {
                throw new IOException(String.format(
                        "Error dumping flushing term stats %d", partNumber), ex);
            }

        }
        if(keys != null) {
            manager.addNewPartition(partNumber, keys);
        }
        started = false;
    }

    public void close() throws IOException {
        if(partDictOut != null) {
            partDictOut.close();
        }
        if(termStatsDictOut != null) {
            termStatsDictOut.close();
        }
        if(postOut != null) {
            for(PostingsOutput po : postOut) {
                po.flush();
            }
        }
    }
}
