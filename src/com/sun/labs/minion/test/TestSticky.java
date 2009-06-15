/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.labs.minion.test;

import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.classification.WeightedFeature;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.retrieval.DocumentVectorImpl;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.util.LabsLogFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 *
 * @author stgreen
 */
public class TestSticky {

    private static void displayResults(String head, ResultSet rs, List<String> fields) throws Exception {
        System.out.println(String.format("%s", head));
        for(Result r : rs.getResults(0, 10)) {
            System.out.println(String.format("%.3f %s %s", r.getScore(), r.
                    getKey(), r.getSingleFieldValue("aura-name")));
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        String flags = "b:d:f:w:s:";
        Getopt gopt = new Getopt(args, flags);
        String indexDir = null;
        String key = null;
        List<String> words = new ArrayList<String>();
        List<String> fields = new ArrayList<String>();
        List<String> sticky = new ArrayList<String>();
        List<String> banned = new ArrayList<String>();
        int c;
        while((c = gopt.getopt()) != -1) {
            switch(c) {
                case 'b':
                    banned.add(gopt.optArg);
                    break;
                case 'd':
                    indexDir = gopt.optArg;
                    break;
                case 'f':
                    fields.add(gopt.optArg);
                    break;
                case 'w':
                    words.add(gopt.optArg);
                    break;
                case 's':
                    sticky.add(gopt.optArg);
                    break;
            }
        }

        if(indexDir == null || words.size() == 0) {
            System.err.println(
                    "Usage:  TestSticky -d <index dir> -f <field> -w <word> -b <ban word> -s <sticky term>");
            return;
        }

        if(fields.size() == 0) {
            fields = null;
        }

        //
        // Use the labs format logging.
        Logger logger = Logger.getLogger("");
        for(Handler h : logger.getHandlers()) {
            h.setFormatter(new LabsLogFormatter());
        }

        SearchEngine se = null;
        try {
            se = SearchEngineFactory.getSearchEngine(indexDir);
            //
            // Get weighted features from the cloud.  We'll only handle things with
            // positive weights.
            List<WeightedFeature> feat = new ArrayList<WeightedFeature>();
            for(String s : words) {
                feat.add(new WeightedFeature(s, 0.5f));
            }
            DocumentVectorImpl dv = new DocumentVectorImpl(se,
                    feat.toArray(new WeightedFeature[0]));
            if(fields != null) {
                dv.setField(fields.get(0));
            }

            System.out.println(String.format("dv: " + dv));

            ResultSet rs = dv.findSimilar();
            displayResults("Find similar", rs, fields);
            ResultSet inc = ((SearchEngineImpl) se).allTerms(sticky, fields, false);
            rs = rs.intersect(inc);
            displayResults("After intersect", rs, fields);
            ResultSet ban = ((SearchEngineImpl) se).anyTerms(banned, fields, false);
            rs = rs.difference(ban);
            displayResults("After ban", rs, fields);
        } finally {
            if(se != null) {
                se.close();
            }
        }
    }

}
