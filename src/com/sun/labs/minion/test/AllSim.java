package com.sun.labs.minion.test;

import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.util.Util;
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * A main class that computes all of the similarities between documents that
 * are the results of an initial query.
 */
public class AllSim {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        String flags = "d:f:o:s:q:";
        Getopt gopt = new Getopt(args, flags);
        String indexDir = null;
        String query = null;
        String field = null;
        String sort = null;
        String outFile = "sims.out";
        int c;
        while((c = gopt.getopt()) != -1) {
            switch(c) {
                case 'd':
                    indexDir = gopt.optArg;
                    break;
                case 'f':
                    field = gopt.optArg;
                    break;
                case 'o':
                    outFile = gopt.optArg;
                    break;
                case 'q':
                    query = gopt.optArg;
                    break;
                case 's':
                    sort = gopt.optArg;
                    break;
            }
        }

        if(indexDir == null || query == null || field == null) {
            System.err.println(
                    "Usage:  AllSim -d <index dir> -f <field> -q <query> [-o <outfile>]");
            return;
        }
        
        //
        // Use the labs format logging.
        Logger logger = Logger.getLogger("");
        for(Handler h : logger.getHandlers()) {
            h.setFormatter(new SimpleLabsLogFormatter());
        }

        SearchEngine se = null;
        try {
            se = SearchEngineFactory.getSearchEngine(indexDir);
            ResultSet rs = se.search(query, sort);
            logger.info(String.format("Got %d documents for %s in %dms", rs.size(), query, rs.getQueryTime()));
            DocumentVector[] dvs = new DocumentVector[rs.size()];
            FieldInfo fi = se.getFieldInfo(field);
            int p = 0;
            for(Result r : rs.getAllResults(sort != null)) {
                dvs[p++] = r.getDocumentVector(fi);
            }
            logger.info("Got document vectors");
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outFile));
            oos.writeInt(p);
            ScoredArtist[] sa = new ScoredArtist[p];
            for(int i = 0; i < p; i++) {
                oos.writeUTF(dvs[i].getKey());
                logger.info(String.format("%d/%d %s computing %d similarities", i+1, p, dvs[i].getKey(), p));
                for(int j = 0; j < p; j++) {
                    sa[j] = new ScoredArtist(dvs[j].getKey(), dvs[i].getSimilarity(dvs[j]));
                }
                Util.sort(sa);
                logger.info(String.format(" Most similar: %s", Util.toString(sa, 0, 10)));
                for(ScoredArtist a : sa) {
                    oos.writeUTF(a.name);
                    oos.writeFloat(a.score);
                }
            }
        } finally {
            if(se != null) {
                se.close();
            }
        }
    }

    public static class ScoredArtist implements Comparable<ScoredArtist> {
        String name;
        float score;

        public ScoredArtist(String name, float score) {
            this.name = name;
            this.score = score;
        }

        public int compareTo(ScoredArtist o) {
            if(score < o.score) {
                return 1;
            }

            if(score > o.score) {
                return -1;
            }
            return 0;
        }

        public String toString() {
            return String.format("<%s,%.3f>", name, score);
        }
    }

}
