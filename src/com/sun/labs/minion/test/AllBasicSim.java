package com.sun.labs.minion.test;

import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.retrieval.ResultImpl;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.util.Util;
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * A main class that computes all of the similarities between documents that
 * are the results of an initial query.
 */
public class AllBasicSim {

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
            logger.info(String.format("Got %d documents for %s in %dms",
                    rs.size(), query, rs.getQueryTime()));
            TermVector[] tvs = new TermVector[rs.size()];
            PostingsIteratorFeatures feat = new PostingsIteratorFeatures(se.
                    getQueryConfig().getWeightingFunction(),
                    se.getQueryConfig().getWeightingComponents());
            feat.setFields(((SearchEngineImpl) se).getManager().getMetaFile().
                    getFieldArray(
                    field));
            int p = 0;
            for(Result r : rs.getAllResults(sort != null)) {
                DocKeyEntry dke = ((ResultImpl) r).getKeyEntry();
                tvs[p++] = new TermVector(dke.getName().toString(),
                        (DiskPartition) dke.getPartition(), dke.iterator(feat));
            }
            logger.info("Got term vectors");
            ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(outFile));
            oos.writeInt(p);
            ScoredTag[] sa = new ScoredTag[p];
            for(int i = 0; i < p; i++) {
                oos.writeUTF(tvs[i].getKey());
                logger.info(String.format("%d/%d %s computing %d similarities",
                        i + 1, p, tvs[i].getKey(), p));
                for(int j = 0; j < p; j++) {
                    sa[j] = new ScoredTag(tvs[j].getKey(), tvs[i].dot(tvs[j]));
                }
                Util.sort(sa);
                logger.info(String.format(" Most similar: %s", Util.
                        arrayToString(sa, 0, 10)));
                for(ScoredTag a : sa) {
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

    public static class Term {

        String name;

        int freq;

        public Term(String name, int freq) {
            this.name = name;
            this.freq = freq;
        }
    }

    public static class TermVector {

        Map<String, Term> m = new LinkedHashMap();

        public float length;

        public String key;

        public TermVector(String key, DiskPartition p, PostingsIterator pi) {
            this.key = key;
            while(pi.next()) {
                Term t = new Term(p.getTerm(pi.getID()).getName().toString(),
                        pi.getFreq());
                length += pi.getFreq() * pi.getFreq();
                m.put(t.name, t);
            }
            length = (float) Math.sqrt(length);
        }

        public String getKey() {
            return key;
        }

        public float dot(TermVector tv) {
            float sum = 0;
            for(Map.Entry<String, Term> e : m.entrySet()) {
                Term ot = tv.m.get(e.getKey());
                if(ot != null) {
                    sum += e.getValue().freq * ot.freq;
                }
            }
            return sum / (length * tv.length);
        }
    }

    public static class ScoredTag implements Comparable<ScoredTag> {

        String name;

        float score;

        public ScoredTag(String name, float score) {
            this.name = name;
            this.score = score;
        }

        public int compareTo(ScoredTag o) {
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
