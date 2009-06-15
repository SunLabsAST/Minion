package com.sun.labs.minion.test;

import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
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
import java.io.IOException;
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
public class EquivSim {

    static Logger logger = Logger.getLogger(EquivSim.class.getName());

    private SearchEngine se;

    private String field;

    private KeyEquivalenceSet[] kes;

    private PostingsIteratorFeatures feat;

    Map<String, KeyEquivalenceSet> kesByName = new HashMap<String, KeyEquivalenceSet>();

    Map<String, Integer> posByName = new HashMap<String, Integer>();
    
    LiteMorph morph = LiteMorph_en.getMorph();

    float sims[][];

    float sosims[][];

    public EquivSim(ResultSet rs, SearchEngine se, String field, String sort)
            throws SearchEngineException {
        this.se = se;
        this.field = field;
        kes = new KeyEquivalenceSet[rs.size()];
        
        feat = new PostingsIteratorFeatures(se.getQueryConfig().
                getWeightingFunction(), se.getQueryConfig().
                getWeightingComponents());
        feat.setFields(((SearchEngineImpl) se).getManager().getMetaFile().
                getFieldArray(field));
        int p = 0;

        //
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
            TermVector tv = new TermVector(docKey,
                                    (DiskPartition) dke.getPartition(),
                                    dke.iterator(feat));
            kes[p] = new KeyEquivalenceSet(tv,
                    feat.getWeightingFunction(),
                    feat.getWeightingComponents());
            
            //
            // When we removed stuff, we might have generated a term that we've 
            // already seen, in which case, we want to combine this one with the
            // older one.
            Integer pos = posByName.get(docKey);
            if(pos != null) {
                kes[pos].add(kes[p]);
                kes[p] = null;
            } else {
                posByName.put(docKey, p);
            }
            p++;
        }
        logger.info(String.format("Got %d key equivalence sets", p));
    }

    private void combineKES(int p1, int p2) {

        kes[p1].add(kes[p2]);

        //
        // Remove the variants from the map.
        for(String name : kes[p2]) {
            posByName.remove(name);
        }

        //
        // Put the variants for kes1 in the map.
        for(String name : kes[p1]) {
            posByName.put(name, p1);
        }

        //
        // Remove the variant's vector.
        kes[p2] = null;
    }

    /**
     * Collects tags that are morphological variants.
     */
    protected void collectMorphVariants() {
        for(int i = 0; i < kes.length; i++) {
            if(kes[i] == null) {
                continue;
            }

            for(String key : kes[i]) {

                //
                // Generate the morphological variants.
                Set<String> vars = morph.variantsOf(key);
                for(String var : vars) {

                    //
                    // Where's this variant?
                    Integer pos = posByName.get(var);

                    if(pos == null || pos == i) {
                        continue;
                    }

                    //
                    // Combine the variants.
                    if(kes[pos] != null) {
                        combineKES(i, pos);
                    }
                }
            }
        }
    }

    /**
     * Determines whether two different tags are the same.
     *
     * @param ed the edit distance between the tags
     * @param sim the similarity of the tags in artist space
     * @return <code>true</code> if the tags should be considered to be the same.
     */
    private boolean sameTag(int ed, float sim) {
        switch(ed) {
            case 0:
                return sim > 0.5;
            case 1:
                return sim > 0.6;
            case 2:
                return sim > 0.74;
            case 3:
                return sim > 0.82;
            case 4:
                return sim > 0.99;
            default:
                return false;

        }
    }

    private void collectEditDistance(int maxEditDistance) {
        finish();
        for(int i = 0; i < kes.length; i++) {
            if(kes[i] == null) {
                continue;
            }

            for(int j = 0; j < kes.length; j++) {
                if(j == i || kes[j] == null) {
                    continue;
                }
                String k1 = null, k2 = null;
                int dist = 0;
                boolean trySim = false;
            check: for(String key : kes[i]) {
                k1 = key;
                    for(String ok : kes[j]) {
                        k2 =ok;
                        dist = Util.levenshteinDistance(key, ok);
                        if(dist <= maxEditDistance) {
                            trySim = true;

                            //
                            // We'll stop as soon as we get one distance
                            // under the min.
                            break check;
                        }
                    }
                }
                if(trySim) {
                    float sim = kes[i].tv.dot(kes[j].tv);
                    if(sameTag(dist, sim)) {
                        combineKES(i, j);
                        logger.info(String.format(
                                "combined %s %s ed: %d sim: %.3f set: %s", k1, k2, dist,
                                                  sim, kes[i].names));
                    }
                }
            }
        }
    }

    private void finish() {
        for(KeyEquivalenceSet k : kes) {
            if(k != null) {
                k.finish();
            }
        }
    }
    
    private void clearNulls() {
        List<KeyEquivalenceSet> l = new ArrayList<KeyEquivalenceSet>();
        for(KeyEquivalenceSet k : kes) {
            if(k != null) {
                l.add(k);
            }
        }
        kes = l.toArray(new KeyEquivalenceSet[0]);
    }

    private void generateSimilarities() {
        clearNulls();
        sims = new float[kes.length][kes.length];
        for(int i = 0; i < kes.length; i++) {
            if((i+1)%100 == 0) {
                logger.info(String.format("%d/%d %s computing similarities",
                                          i + 1, kes.length, kes[i].tv.getKey()));
            }
            for(int j = 0; j < kes.length; j++) {
                sims[i][j] = kes[i].tv.dot(kes[j].tv);
            }
        }
   }

    private void generateSecondOrderSimilarities() {
        if(sims == null) {
            generateSimilarities();
        }

        sosims = new float[kes.length][kes.length];
        for(int i = 0; i < sims.length; i++) {
            for(int j = 0; j < sims.length; j++) {
                sosims[i][j] = sim(i, j);
            }
        }
    }

    private float sim(int a, int b) {
        float dot = 0;
        float l1 = 0;
        float l2 = 0;
        float[] v1 = sims[a];
        float[] v2 = sims[b];
        for(int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            l1 += v1[i] * v1[i];
            l2 += v2[i] * v2[i];
        }
        return dot / (float) (Math.sqrt(l1) * Math.sqrt(l2));
    }

    private void outputSims(DataOutput out, float[][] os) throws java.io.IOException {
        ScoredTag[] sa = new ScoredTag[kes.length];
        for(int i = 0; i < kes.length; i++) {
            for(int j = 0; j < kes.length; j++) {
                sa[j] = new ScoredTag(kes[j].tv.getKey(), j, os[i][j]);
            }
            Util.sort(sa);

            logger.info(String.format("%s most similar: %s",
                                      kes[i].tv.getKey(),
                                      Util.arrayToString(sa, 1, 3)));
            for(ScoredTag a : sa) {
                out.writeInt(a.id);
                out.writeFloat(a.score);
            }
        }
    }

    private void output(String outFile) throws java.io.IOException {

        if(sims == null) {
            generateSimilarities();
            generateSecondOrderSimilarities();
        }

        DataOutputStream out = new DataOutputStream(new FileOutputStream(outFile));
        out.writeInt(kes.length);

        //
        // Now compute the total pair-wise similarties for the equivalence sets
        // that we just built.
        for(int i = 0; i < kes.length; i++) {
            out.writeUTF(kes[i].tv.key);
            out.writeInt(kes[i].names.size());
            for(String name : kes[i]) {
                out.writeUTF(name);
            }
        }

        outputSims(out, sims);
        outputSims(out, sosims);
        out.close();
    }


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        String flags = "d:f:m:o:s:q:";
        Getopt gopt = new Getopt(args, flags);
        String indexDir = null;
        String query = null;
        String field = null;
        String sort = null;
        String outFile = "sims.out";
        int maxEditDistance = 4;
        int c;
        while((c = gopt.getopt()) != -1) {
            switch(c) {
                case 'd':
                    indexDir = gopt.optArg;
                    break;
                case 'f':
                    field = gopt.optArg;
                    break;
                case 'm':
                    maxEditDistance = Integer.parseInt(gopt.optArg);
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
        Logger rl = Logger.getLogger("");
        for(Handler h : rl.getHandlers()) {
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
            EquivSim es = new EquivSim(rs, se, field, sort);
            es.collectMorphVariants();
            es.collectEditDistance(maxEditDistance);
            es.finish();
            es.output(outFile);

        } finally {
            if(se != null) {
                se.close();
            }
        }
    }

    /**
     * Do basic string cleaning.  Remove the <code>artist-tag:</code> initial type,
     * change underscore and dash to space, and remove multiple spaces.
     * @param tag the tag to clean
     * @return the cleaned up tag.
     */
    public static String removeStuff(String tag) {
        String orig = tag;
        int p = tag.indexOf(':');
        if(p > 0) {
            tag = tag.substring(p + 1);
        }
        //
        // Replace underscore and dash with space.
        tag.replaceAll("[_\\-]", " ");

        //
        // Squash multiple spaces to one.
        tag.replaceAll("\\s+", " ");
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

    public static class KeyEquivalenceSet implements Iterable<String> {

        Set<String> names = new HashSet<String>();

        TermVector tv;

        WeightingComponents wc;

        WeightingFunction wf;

        public KeyEquivalenceSet(TermVector tv,
                                 WeightingFunction wf,
                                 WeightingComponents wc) {
            names.add(tv.key);
            this.tv = tv;
            this.wc = wc;
            this.wf = wf;
        }

        public void add(KeyEquivalenceSet kes) {
            names.addAll(kes.names);
            tv.add(kes.tv);
        }

        public boolean contains(String name) {
            return names.contains(name);
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

        @Override
        public Iterator<String> iterator() {
            return names.iterator();
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
        
        int id;

        float score;

        public ScoredTag(String name, int id, float score) {
            this.name = name;
            this.id = id;
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
