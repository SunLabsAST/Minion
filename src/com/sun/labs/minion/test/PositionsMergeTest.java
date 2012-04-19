package com.sun.labs.minion.test;

import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionaryBundle;
import com.sun.labs.minion.indexer.dictionary.StringNameHandler;
import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.dictionary.io.DiskDictionaryOutput;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.StreamPostingsOutput;
import com.sun.labs.util.LabsLogFormatter;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A quick main class that we can use to profile dictionary writing.
 */
public class PositionsMergeTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        
        Logger logger = Logger.getLogger("");
        for(Handler h : logger.getHandlers()) {
            h.setFormatter(new LabsLogFormatter());
            h.setLevel(Level.ALL);
        }
        logger.setLevel(Level.FINER);

        SearchEngine engine = SearchEngineFactory.getSearchEngine(args[0]);
        DiskPartition[] parts = engine.getPM().getActivePartitions().toArray(
                new DiskPartition[0]);
        DiskDictionary[] dicts = new DiskDictionary[parts.length];
        int[] starts = new int[parts.length];
        int[][] docIDMaps = new int[parts.length][];
        starts[0] = 1;
        for(int i = 0; i < parts.length; i++) {
            DiskField df = ((InvFileDiskPartition) parts[i]).getDF(args[1]);
            docIDMaps[i] = parts[i].getDocIDMap(
                    parts[i].getDeletedDocumentsMap());
            if(i > 0) {
                starts[i] = starts[i - 1] + parts[i - 1].getNDocs();
            }
            if(df != null) {
                dicts[i] = df.getDictionary(
                        MemoryDictionaryBundle.Type.CASED_TOKENS);
            }
        }

        File outDir = new File(args[2]);
        if(!outDir.exists()) {
            outDir.mkdirs();
        }

        DictionaryOutput dictOut;
            dictOut = new DiskDictionaryOutput(outDir);
        PostingsOutput[] postOut = new PostingsOutput[dicts[0].
                getPostingsChannelNames().length];
        OutputStream[] streams = new OutputStream[postOut.length];
        for(int i = 0; i < postOut.length; i++) {
            streams[i] = new BufferedOutputStream(new FileOutputStream(new File(
                    outDir, String.format("%d.post", i))));
            postOut[i] = new StreamPostingsOutput(streams[i]);
        }

        DiskDictionary.merge(outDir, new StringNameHandler(),
                             dicts,
                             null, starts,
                             docIDMaps, dictOut,
                             postOut, true);
        RandomAccessFile raf = new RandomAccessFile(new File(outDir, "tmp.dict"), "rw");
        dictOut.flush(raf);
        raf.close();
        for(PostingsOutput po : postOut) {
            po.flush();
        }
        for(OutputStream os : streams) {
            os.close();
        }
        engine.close();
    }
}
