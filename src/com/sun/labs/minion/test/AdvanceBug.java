package com.sun.labs.minion.test;

import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.dictionary.LightIterator;
import com.sun.labs.minion.indexer.dictionary.TermStatsDiskDictionary;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.entry.TermStatsQueryEntry;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.util.CharUtils;
import com.sun.labs.minion.util.Util;
import com.sun.labs.util.LabsLogFormatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test the lightweight dictionary iterator's advanceTo method.
 */
public class AdvanceBug {

    private static final Logger logger = Logger.getLogger(AdvanceBug.class.getName());

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
        
        QueryEntry<String> lastEntry = tsd.getByID(tsd.size());
        
        Logger.getLogger(DiskDictionary.class.getName()).setLevel(Level.FINE);
        LightIterator<String> tsi = tsd.literator();
        tsi.advanceTo(lastEntry.getID() - 6);
        while(tsi.next()) {
            TermStatsQueryEntry tsqe = (TermStatsQueryEntry) tsi.getEntry(null);
            logger.info(String.format("Iterate %d name: %s (%s)",
                    tsqe.getID(), tsqe.getName(), Util.toHexDigits(tsqe.getName())));
        }
        
        tsi = tsd.literator();
        for(int i = 2; i < args.length; i++) {
            try {
                int id = Integer.parseInt(args[i]);
                TermStatsQueryEntry tsqe = (TermStatsQueryEntry) tsd.getByID(id);
                logger.info(String.format("Found: %d: %s (%s)", id, tsqe.getName(), Util.toHexDigits(tsqe.getName())));
            } catch (NumberFormatException ex) {
                String s = CharUtils.decodeUnicode(args[i]);
                logger.info(String.format("Looking for %s: %s", args[i], s));
                TermStatsQueryEntry tsqe = (TermStatsQueryEntry) tsi.advanceTo(s, null);
                if(tsqe != null) {
                    logger.info(String.format("Found: %s (%s) id: %d",
                            tsqe.getName(), Util.toHexDigits(tsqe.getName()), tsqe.getID()));
                    TermStatsQueryEntry gqe = (TermStatsQueryEntry) tsd.get(tsqe.getName());
                    logger.info(String.format("Get by name %d name: %s (%s)",
                            gqe.getID(), gqe.getName(), Util.toHexDigits(gqe.getName())));
                }
                
            }
        }
        engine.close();
    }
}
