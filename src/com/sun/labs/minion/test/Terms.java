/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.labs.minion.test;

import com.sun.labs.minion.Log;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.query.Element;
import com.sun.labs.minion.query.Term;
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.EnumSet;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author stgreen
 */
public class Terms {

    private static void display(ResultSet rs) throws Exception {
        System.out.println(String.format("%d results", rs.size()));
        for(Result result : rs.getResults(0, 10)) {
            System.out.println(String.format("%.3f %s", result.getScore(),
                    result.getSingleFieldValue("subject")));
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        //
        // Use the labs format logging.
        Logger rl = Logger.getLogger("");
        for(Handler h : rl.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new SimpleLabsLogFormatter());
            try {
                h.setEncoding("utf-8");
            } catch(Exception ex) {
                rl.severe("Error setting output encoding");
            }
        }

        Log.setLogger(rl);
        Log.setLevel(3);
        SearchEngine e = SearchEngineFactory.getSearchEngine(args[0]);
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        String t;
        while((t = r.readLine()) != null) {
            Element el = new Term(t);
            ResultSet rs1 = e.search(el);
            display(rs1);
//            ResultSet rs2 = e.search(t);
//            display(rs2);
        }
        e.close();
    }

}
