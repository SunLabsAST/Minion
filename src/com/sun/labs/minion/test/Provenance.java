package com.sun.labs.minion.test;

import com.sun.labs.minion.indexer.partition.PartitionHeader;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.util.Getopt;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class Provenance {
    
    private static final Logger logger = Logger.getLogger(Provenance.class.getName());
    
    private static void checkProvenance(File indexDir, int partNumber, String prefix) throws IOException {
        File partFile = PartitionManager.makeDictionaryFile(indexDir.getAbsolutePath(), partNumber);
        PartitionHeader header = new PartitionHeader(partFile);
        System.out.format("%s%d %s\n", prefix, partNumber, header.getProvenance());
        if(header.getProvenance().equals("marshalled")) {
            
        }  else if(header.getProvenance().startsWith("merged ")) {
            String[] partNums = header.getProvenance().substring(7).split(" ");
            for(String partNum : partNums) {
                checkProvenance(indexDir, Integer.parseInt(partNum), prefix + "  ");
            }
        } else {
            System.out.format("Unknown provenance string: %s", header.getProvenance());
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String flags = "d:";
        Getopt gopt = new Getopt(args, flags);
        int c;
        File indexDir = null;

        if(args.length == 0) {
            System.err.format("Usage Provenance -d <index dir> [partNum] [partNum]...\n");
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
            System.err.format("Usage PartitionHeader -d <index dir> [partNum] [partNum]...\n");
            return;
        }

        for(int i = gopt.optInd; i < args.length; i++) {
            RandomAccessFile raf = null;
            File partFile = null;
            try {
                int partNumber = Integer.parseInt(args[i]);
                checkProvenance(indexDir, partNumber, "");
            } catch(NumberFormatException ex) {
                logger.log(Level.SEVERE, String.format("Bad partition number: %s", args[i]));
            } catch(IOException ex) {
                logger.log(Level.SEVERE, String.format("Error reading partition file %s", partFile));
            } finally {
                if(raf != null) {
                    try {
                        raf.close();
                    } catch(java.io.IOException ex) {
                    }
                }
            }
        }
    }

}
