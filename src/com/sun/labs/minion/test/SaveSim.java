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
import com.sun.labs.minion.lexmorph.LiteMorph;
import com.sun.labs.minion.lexmorph.LiteMorph_en;
import com.sun.labs.minion.retrieval.ResultImpl;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.util.Util;
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * A main class that computes all of the similarities between documents that
 * are the results of an initial query.
 */
public class SaveSim {

    static Logger logger = Logger.getLogger(SaveSim.class.getName());

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

            //
            // Open the search engine and run the query to get the document
            // vectors that we're interested in.
            se = SearchEngineFactory.getSearchEngine(indexDir);
            ResultSet rs = se.search(query, sort);
            logger.info(String.format("Got %d documents for %s in %dms",
                                      rs.size(), query, rs.getQueryTime()));
            TermVector[] tvs = new TermVector[rs.size()];
            PostingsIteratorFeatures feat = new PostingsIteratorFeatures(se.
                    getQueryConfig().getWeightingFunction(),
                                                                         se.
                    getQueryConfig().getWeightingComponents());
            feat.setFields(((SearchEngineImpl) se).getManager().getMetaFile().
                    getFieldArray(
                    field));
            int p = 0;

            //
            // A map from terms to term vectors, since we'll want to do fast
            // lookups in a bit.
            Map<String, TermVector> tvByName = new HashMap<String, TermVector>();
            Map<String, Integer> posByName = new HashMap<String, Integer>();

            //
            // For each result, pull the terms and frequencies from the document
            // vectors.
            for(Result r : rs.getAllResults(sort != null)) {
                DocKeyEntry dke = ((ResultImpl) r).getKeyEntry();

                //
                // Get the document key.  Since we're looking at tags here, this
                // key is the name of the tag, possibly with some extra stuff on
                // it.
                String docKey = removeStuff(dke.getName().toString());
                tvs[p] = new TermVector(docKey,
                                        (DiskPartition) dke.getPartition(), dke.
                        iterator(feat));
                tvByName.put(docKey, tvs[p]);
                posByName.put(docKey, p);
                p++;
            }
            logger.info("Got term vectors");

            //
            // Now we want to build equivalence sets for the keys (i.e., tags)
            // that we pulled the vectors for.  We'll do this by iterating through
            // the available terms, running lightmorph and combining vectors.
            LiteMorph morph = LiteMorph_en.getMorph();
            List<KeyEquivalenceSet> sets = new ArrayList<KeyEquivalenceSet>();
            int nCombined = 0;
            for(int i = 0; i < p; i++) {
                if(tvs[i] == null) {
                    continue;
                }

                String key = tvs[i].key;
                KeyEquivalenceSet kes = new KeyEquivalenceSet(key, feat.
                        getWeightingFunction(), feat.getWeightingComponents());

                //
                // Always add the one we're starting with.
                kes.add(key, tvs[i]);
                Set<String> vars = morph.variantsOf(key);
                for(String var : vars) {
                    if(var.equals(key)) {
                        continue;
                    }
                    
                    TermVector tv = tvByName.get(var);
                    if(tv != null) {
                        kes.add(var, tv);
                        tvs[posByName.get(var)] = null;
                    }
                }
                if(kes.names.size() > 1) {
                    nCombined++;
                    logger.info(String.format("Computed KES %d/%d for %s: %s", nCombined,
                                              p, key, kes.names));
                }
                kes.finish();
                sets.add(kes);
            }

            //
            // Turn our list of equivalence sets into an array.
            KeyEquivalenceSet[] aset = sets.toArray(new KeyEquivalenceSet[0]);
            p = aset.length;

            DataOutput out = new DataOutputStream(
                    new FileOutputStream(outFile));
            out.writeInt(p);
            ScoredTag[] sa = new ScoredTag[p];

            //
            // Now compute the total pair-wise similarties for the equivalence sets
            // that we just built.
            for(int i = 0; i < p; i++) {
                out.writeUTF(aset[i].tv.key);
                out.writeInt(aset[i].names.size());
                for(String n : aset[i].names) {
                    out.writeUTF(n);
                }
                logger.info(String.format("%d/%d %s computing %d similarities",
                                          i + 1, p, aset[i].tv.getKey(), p));
                for(int j = 0; j < p; j++) {
                    sa[j] = new ScoredTag(aset[j].tv.getKey(), aset[i].tv.dot(
                            aset[j].tv));
                }
                Util.sort(sa);
                logger.info(String.format(" Most similar: %s", Util.
                        arrayToString(sa, 0, 10)));
                for(ScoredTag a : sa) {
                    out.writeUTF(a.name);
                    out.writeFloat(a.score);
                }
            }
        } finally {
            if(se != null) {
                se.close();
            }
        }
    }

    public static String removeStuff(String tag) {
        int p = tag.indexOf(':');
        if(p > 0) {
            return tag.substring(p + 1);
        }
        return tag;
    }

    public static class Term {

        String name;

        int freq;

        float weight;

        public Term(String name, int freq) {
            this.name = name;
            this.freq = freq;
        }

        public void add(Term t) {
            freq += t.freq;
        }
    }

    public static class KeyEquivalenceSet {

        Set<String> names = new HashSet<String>();

        TermVector tv;

        WeightingComponents wc;

        WeightingFunction wf;

        public KeyEquivalenceSet(String key, WeightingFunction wf,
                                 WeightingComponents wc) {
            names.add(key);
            tv = new TermVector(key);
            this.wc = wc;
            this.wf = wf;
        }

        public void add(String key, TermVector v) {
            names.add(key);
            tv.add(v);
        }

        public void finish() {

            //
            // Use the shortest name in our set as the name of this equivalence
            // set.
            int minlen = Integer.MAX_VALUE;
            String min = null;
            for(String name : names) {
                if(name.length() < minlen) {
                    minlen = name.length();
                    min = name;
                }
            }
            tv.setKey(min);

            //
            // For each of the terms in the combined vectors, compute the
            // weight.
            for(Term t : tv) {
                wc.setTerm(t.name);
                wf.initTerm(wc);
                wc.fdt = t.freq;
                t.weight = wf.termWeight(wc);
            }
            tv.computeLength();
            tv.normalize();
        }
    }

    public static class TermVector implements Iterable<Term> {

        Map<String, Term> m = new LinkedHashMap();

        public float length;

        public String key;

        public TermVector(String key, DiskPartition p, PostingsIterator pi) {
            this.key = key;
            while(pi.next()) {
                Term t = new Term(p.getTerm(pi.getID()).getName().toString(),
                                  pi.getFreq());
                m.put(t.name, t);
            }
        }

        public TermVector(String key) {
            this.key = key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public void computeLength() {
            length = 0;
            for(Term t : this) {
                length += t.weight * t.weight;
            }
            length = (float) Math.sqrt(length);
        }

        private void normalize() {
            double foo = 0;
            for(Term t : m.values()) {
                t.weight /= length;
                foo += t.weight * t.weight;
            }
        }
        
        public float dot(TermVector tv) {
            float sum = 0;
            for(Map.Entry<String, Term> e : m.entrySet()) {
                Term ot = tv.m.get(e.getKey());
                if(ot != null) {
                    sum += e.getValue().weight * ot.weight;
                }
            }
            return sum;
        }

        /**
         * Adds the vectors together, modifying the current vector.
         *
         * @param tv the vector to add to this one.
         */
        public void add(TermVector tv) {

            //
            // Get a copy of the map that we can frob.
            Map<String, Term> copy = new LinkedHashMap<String, Term>(tv.m);

            //
            // Add the terms in common.
            for(Map.Entry<String, Term> e : m.entrySet()) {
                Term t = copy.remove(e.getKey());
                if(t != null) {
                    e.getValue().add(t);
                }
            }

            //
            // Put in whatever's left.
            m.putAll(copy);

        }

        @Override
        public Iterator<Term> iterator() {
            return m.values().iterator();
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
