package com.sun.labs.minion.test;

import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.query.Element;
import com.sun.labs.minion.query.Passage;
import com.sun.labs.minion.query.Term;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Quick test for the query API.
 */
public class TestQueryAPI {
    
    public static Object get(Result r, String field) {
        List l = r.getField(field);
        if(l != null) {
            if(l.isEmpty()) {
                return null;
            }
            return l.get(0);
        }
        return null;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        
        SearchEngine e = SearchEngineFactory.getSearchEngine(args[0]);
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        String l = null;
        while((l = r.readLine()) != null) {
            
            l = l.trim();
            if(l.isEmpty()) {
                continue;
            }
            List<Element> els = new ArrayList<Element>();
            for(String token : l.split("\\s+")) {
                token = token.trim();
                if(token.isEmpty()) {
                    continue;
                }
                els.add(new Term(token));
            }
            
            Passage pass = new Passage(els);
            
            ResultSet rs = e.search(pass);
            System.out.format("Query: %d hits for %s\n", rs.size(), pass);
            for(Result result : rs.getResults(0, 10)) {
                System.out.format("%s %.3f", result.getKey(), result.getScore());
                for(int i = 1; i < args.length; i++) {
                    Object o = get(result, args[i]);
                    System.out.format(" %s", o);
                }
                System.out.println("");
            }
        }
        
        e.close();
    }
}
