package com.sun.labs.minion.test;

import com.sun.labs.minion.indexer.partition.ActiveFile;
import com.sun.labs.minion.indexer.partition.DelMap;
import com.sun.labs.minion.indexer.partition.PartitionHeader;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.util.FileLock;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.util.LabsLogFormatter;
import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class Provenance {

    private static final Logger logger = Logger.getLogger(Provenance.class.
            getName());

    private static void checkProvenance(File indexDir, int partNumber,
                                        String prefix) throws IOException {
        File partFile = PartitionManager.makeDictionaryFile(indexDir.
                getAbsolutePath(), partNumber);
        PartitionHeader header = new PartitionHeader(partFile);
        File delFile = PartitionManager.makeDeletedDocsFile(indexDir.toString(), partNumber);
        int nDeleted = 0;
        if(delFile.exists()) {
            DelMap delMap = new DelMap(delFile, new FileLock(new File(indexDir, delFile.toString() + ".lock")));
            nDeleted = delMap.getNDeleted();
        }
        int nDocs = header.getnDocs() - nDeleted;
        System.out.format("%s%d %d/%d/%d %s\n", 
                          prefix, partNumber, 
                          header.getnDocs(), nDeleted, header.getnDocs() - nDeleted,
                          header.getProvenance());
        if(header.getProvenance().equals("marshaled")) {
        } else if(header.getProvenance().startsWith("merged ")) {
            String[] partNums = header.getProvenance().substring(7).split(" ");
            for(String partNum : partNums) {
                checkProvenance(indexDir, Integer.parseInt(partNum), prefix
                        + "  ");
            }
        } else {
            System.out.format("Unknown provenance string: %s", header.
                    getProvenance());
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        for(Handler h : Logger.getLogger("").getHandlers()) {
            h.setFormatter(new LabsLogFormatter());
            h.setLevel(Level.ALL);
        }

        String flags = "d:";
        Getopt gopt = new Getopt(args, flags);
        int c;
        File indexDir = null;

        if(args.length == 0) {
            System.err.format(
                    "Usage Provenance -d <index dir> [partNum] [partNum]...\n");
            return;
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
            System.err.format(
                    "Usage PartitionHeader -d <index dir> [partNum] [partNum]...\n");
            return;
        }

        if(gopt.optInd >= args.length) {
            try {
                ActiveFile af = new ActiveFile(indexDir, indexDir, "PM", false);
                for(Integer partNumber : af.read()) {
                    checkProvenance(indexDir, partNumber, "");
                }
            } catch(Exception ex) {
                logger.log(Level.SEVERE, "Error reading partition file", ex);
            }
        } else {

            for(int i = gopt.optInd; i < args.length; i++) {
                try {
                    int partNumber = Integer.parseInt(args[i]);
                    checkProvenance(indexDir, partNumber, "");
                } catch(NumberFormatException ex) {
                    logger.log(Level.SEVERE, String.format(
                            "Bad partition number: %s", args[i]));
                } catch(IOException ex) {
                    logger.log(Level.SEVERE, String.format(
                            "Error reading partition file %s", args[i]), ex);
                }
            }
        }
    }
}
