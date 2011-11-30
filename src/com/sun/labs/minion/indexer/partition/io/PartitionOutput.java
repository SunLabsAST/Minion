package com.sun.labs.minion.indexer.partition.io;

import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.dictionary.NameEncoder;
import com.sun.labs.minion.indexer.dictionary.TermStatsHeader;
import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.partition.MemoryPartition;
import com.sun.labs.minion.indexer.partition.PartitionHeader;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * An interface for things that want to write partition data.
 */
public interface PartitionOutput {
    
    public PartitionManager getPartitionManager();
    
    public int startPartition(MemoryPartition partition) throws IOException;

    public int startPartition(String[] postingsChannelNames) throws IOException;
    
    public void setKeys(Set<String> keys);
    
    public MemoryPartition getPartition();

    public int getPartitionNumber();
    
    public void setPartitionNumber(int partitionNumber);
    
    public PartitionHeader getPartitionHeader();
    
    public void setNDocs(int nDocs);
    
    public int getNDocs();
    
    public void setMaxDocID(int maxDocID);
    
    public int getMaxDocID();
    
    public void setName(String name);
    
    public String getName();

    public DictionaryOutput getPartitionDictionaryOutput();
    
    public void setDictionaryEncoder(NameEncoder encoder);
    
    public NameEncoder getDictionaryEncoder();
    
    public void setDictionaryRenumber(MemoryDictionary.Renumber renumber);
    
    public MemoryDictionary.Renumber getDictionaryRenumber();
    
    public void setDictionaryIDMap(MemoryDictionary.IDMap idmap);
    
    public MemoryDictionary.IDMap getDictionaryIDMap();
    
    public void setPostingsIDMap(int[] postMap);
    
    public int[] getPostingsIDMap();
    
    public File[] getPostingsFiles();
    
    public PostingsOutput[] getPostingsOutput();
    
    public void setPostingsOutput(PostingsOutput[] postOut);
    
    public DictionaryOutput getTermStatsDictionaryOutput();
    
    public TermStatsHeader getTermStatsHeader();
    
    public WriteableBuffer getDeletionsBuffer();

    public WriteableBuffer getVectorLengthsBuffer();
    
    public boolean isLongIndexingRun();
    
    public void flushVectorLengths() throws IOException;
    
    public void flush() throws IOException;
    
    public void cleanUp();

    public void close() throws IOException;
}
