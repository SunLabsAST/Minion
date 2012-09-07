package com.sun.labs.minion.test;

import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test the lightweight dictionary iterator's advanceTo method.
 */
public class AdvanceTest {

    private static final Logger logger = Logger.getLogger(AdvanceTest.class.getName());

    public static void main(String[] args) throws SearchEngineException {

        Logger l = Logger.getLogger("");
        for(Handler h : l.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new LabsLogFormatter());
        }

        SearchEngine engine = SearchEngineFactory.getSearchEngine(args[0]);
        PartitionManager pm = engine.getPM();
        TermStatsDiskDictionary tsds = pm.getTermStatsDict();
        DiskDictionary tsd = tsds.getDictionary(args[1]);
        logger.info(String.format("Term Stats dict for %s has %d entries", args[1], tsd.size()));
        if(tsd == null) {
            logger.info(String.format("No field %s in term stats", args[1]));
        } else {
            for(DiskPartition dp : pm.getActivePartitions()) {
                InvFileDiskPartition tp = (InvFileDiskPartition) dp;
                NanoWatch nw = new NanoWatch();
                nw.start();
                DiskDictionary<String> tokens = (DiskDictionary<String>) tp.getDF(args[1]).getTermDictionary();
                logger.info(String.format("Term dict for %s in %s has %d entries", args[1], tp, tokens.size()));
                LightIterator<String> tsi = tsd.literator();
                TermStatsQueryEntry ptse = null;
                TermStatsQueryEntry atse;
                int nChecked = 0;
                int lastID = 0;
                long totalGap = 0;
                for(Entry<String> qe : tokens) {
                    TermStatsQueryEntry tse = (TermStatsQueryEntry) tsd.get(qe.getName());
                    atse = (TermStatsQueryEntry) tsi.advanceTo(qe.getName(), ptse);
                    if(tse == null) {
                        if(atse != null) {
                            logger.severe(String.format("Get returned null advanceTo returned %s (%d) for %s (%d)",
                                    atse.getName(), atse.getID(),
                                    qe.getName(), qe.getID()));
                            break;
                        }
                    } else {
                        if(atse == null) {
                            logger.severe(String.format("Get returned %s (%s) advanceTo returned null for %s (%d)",
                                    tse.getName(), tse.getID(),
                                    qe.getName(), qe.getID()));
                            break;
                        } else {
                            if(!tse.getName().equals(atse.getName())) {
                                logger.severe(String.format("Get returned %s (%d) advanceTo returned %s (%d) for %s (%d)",
                                        tse.getName(), tse.getID(),
                                        atse.getName(), atse.getID(),
                                        qe.getName(), qe.getID()));
                                break;
                            }
                        }
                    }
                    if(atse != null) {
                        ptse = atse;
                    }
                    nChecked++;
                    if(tse != null) {
                        totalGap += (tse.getID() - lastID);
                        lastID = tse.getID();
                    }
                    if(nChecked % 5000 == 0) {
                        logger.info(String.format("Checked %d/%d", nChecked, tokens.size()));
                    }
                }
                if(nChecked % 5000 != 0) {
                    logger.info(String.format("Checked %d/%d", nChecked, tokens.size()));
                }
                nw.stop();
                logger.info(String.format("Average gap: %.2f", (double) totalGap / nChecked));
                QueryStats qs = ((DiskDictionary.LightDiskDictionaryIterator) tsi).getQueryStats();
                NIOFileReadableBuffer b = 
                        (NIOFileReadableBuffer) ((DiskDictionary.LightDiskDictionaryIterator) tsi).getLookupState().localNames;
                logger.info(String.format("Binary search time: %.2fms", qs.dictLookupW.getTimeMillis()));
                logger.info(String.format("Binary searches: %d", qs.dictLookupW.getClicks()));
                logger.info(String.format("Avg binary search time: %fms", qs.dictLookupW.getAvgTimeMillis()));
                logger.info(String.format("Total time: %.2f", nw.getTimeMillis()));
                logger.info(String.format("Total name reads: %d", b.reads));
            }
        }
        engine.close();
    }
}
