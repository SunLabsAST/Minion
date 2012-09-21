package com.sun.labs.minion.test;

import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.indexer.Field;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary.LookupState;
import com.sun.labs.minion.indexer.dictionary.LightIterator;
import com.sun.labs.minion.indexer.dictionary.TermStatsDiskDictionary;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.TermStatsQueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.util.buffer.NIOFileReadableBuffer;
import com.sun.labs.util.LabsLogFormatter;
import com.sun.labs.util.NanoWatch;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test the lightweight dictionary iterator's advanceTo method.
 */
public class AdvanceSpeed {
    
    private static final Logger logger = Logger.getLogger(AdvanceSpeed.class.getName());
    
    public static void main(String[] args) throws SearchEngineException {
        
        Logger l = Logger.getLogger("");
        for(Handler h : l.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new LabsLogFormatter());
        }

        SearchEngine engine = SearchEngineFactory.getSearchEngine(args[0]);
        PartitionManager pm = engine.getPM();
        List<DiskPartition> parts = pm.getActivePartitions();
        int minSize = Integer.MAX_VALUE;
        InvFileDiskPartition tp = null;
        for(DiskPartition dp : pm.getActivePartitions()) {
            if(dp.getNDocs() < minSize) {
                minSize = dp.getNDocs();
                tp = (InvFileDiskPartition) dp;
            }
        }
        TermStatsDiskDictionary tsds = pm.getTermStatsDict();
        DiskDictionary tsd = tsds.getDictionary(args[1], Field.TermStatsType.RAW);
        if(tsd == null) {
            logger.info(String.format("No field %s in term stats", args[1]));
        } else {
            NanoWatch nw = new NanoWatch();
            nw.start();
            DiskDictionary<String> tokens = (DiskDictionary<String>) tp.getDF(args[1]).getTermDictionary(Field.TermStatsType.RAW);
            logger.info(String.format("Term Stats dict for %s has %d entries", args[1], tsd.size()));
            logger.info(String.format("Term dict for %s in %s has %d entries", args[1], tp, tokens.size()));
            LightIterator<String> tsi = tsd.literator();
            TermStatsQueryEntry ptse = null;
            TermStatsQueryEntry atse = null;
            int nChecked = 0;
            int lastID = 0;
            long totalGap = 0;
            for(Entry<String> qe : tokens) {
                atse = (TermStatsQueryEntry) tsi.advanceTo(qe.getName(), ptse);
                nChecked++;
                if(atse != null) {
                    totalGap += (atse.getID() - lastID);
                    lastID = atse.getID();
                    ptse = atse;
                }
                if(nChecked % 1000 == 0) {
                    logger.info(String.format("Checked %d/%d", nChecked, tokens.size()));
                }
            }
            if(nChecked % 1000 != 0) {
                logger.info(String.format("Checked %d/%d", nChecked, tokens.size()));
            }
            nw.stop();
            logger.info(String.format("Average gap: %.2f", (double) totalGap / nChecked));
            QueryStats qs = ((DiskDictionary.LightDiskDictionaryIterator) tsi).getQueryStats();
            LookupState lus = ((DiskDictionary.LightDiskDictionaryIterator) tsi).getLookupState();
            logger.info(String.format("Binary search time: %.2fms", qs.dictLookupW.getTimeMillis()));
            logger.info(String.format("Binary searches: %d", qs.dictLookupW.getClicks()));
            logger.info(String.format("Avg binary search time: %fms", qs.dictLookupW.getAvgTimeMillis()));
            logger.info(String.format("Total time: %.2f", nw.getTimeMillis()));
            logger.info(String.format("Steps taken: %d", 
                    ((DiskDictionary.LightDiskDictionaryIterator) tsi).getStepsTaken()));
            logger.info(String.format("Reads"));
            logger.info(String.format(" names:        %7d", 
                    ((NIOFileReadableBuffer) lus.localNames).reads));
            logger.info(String.format(" name offsets: %7d", 
                    ((NIOFileReadableBuffer) lus.localNameOffsets).reads));
            logger.info(String.format(" info:         %7d", 
                    ((NIOFileReadableBuffer) lus.localInfo).reads));
            logger.info(String.format(" info offsets: %7d", 
                    ((NIOFileReadableBuffer) lus.localInfoOffsets).reads));
        }
        engine.close();
    }
}
