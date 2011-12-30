package com.sun.labs.minion.test;

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
import com.sun.labs.util.LabsLogFormatter;
import java.util.List;
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
        DiskDictionary tsd = tsds.getDictionary(args[1]);
        if(tsd == null) {
            logger.info(String.format("No field %s in term stats", args[1]));
        } else {
            DiskDictionary<String> tokens = tp.getDF(args[1]).getTermDictionary(false);
            logger.info(String.format("Term Stats dict for %s has %d entries", args[1], tsd.size()));
            logger.info(String.format("Term dict for %s in %s has %d entries", args[1], tp, tokens.size()));
            LightIterator<String> tsi = tsd.literator();
            TermStatsQueryEntry ptse = null;
            TermStatsQueryEntry atse = null;
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
                        } else {
//                            logger.info(qe.getName());
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
                if(nChecked % 1000 == 0) {
                    logger.info(String.format("Checked %d/%d", nChecked, tokens.size()));
                }
            }
            if(nChecked % 1000 != 0) {
                logger.info(String.format("Checked %d/%d", nChecked, tokens.size()));
            }
            logger.info(String.format("Average gap: %.2f", (double) totalGap / nChecked));
        }
        engine.close();
    }
}
