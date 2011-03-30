package com.sun.labs.minion.test;

import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.query.Element;
import com.sun.labs.minion.query.PAnd;
import com.sun.labs.minion.query.Passage;
import com.sun.labs.minion.query.Phrase;
import com.sun.labs.minion.query.Term;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Quick test for the query API.
 */
public class TestQueryAPI {

    public static Object get(Result r, String field) {
        List l = r.getField(field);
        if (l != null) {
            if (l.isEmpty()) {
                return null;
            }
            return l.get(0);
        }
        return null;
    }

    public static void run(SearchEngine e, Element el, String[] fields) throws
            Exception {
        ResultSet rs = e.search(el);
        System.out.format("Query: %d hits for %s\n", rs.size(), el);
        for (Result result : rs.getResults(0, 10)) {
            System.out.format("%s %.3f", result.getKey(), result.getScore());
            for (String field : fields) {
                Object o = get(result, field);
                System.out.format(" %s", o);
            }
            System.out.println("");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        SearchEngine e = SearchEngineFactory.getSearchEngine(args[0]);
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        String l = null;
        String[] fields = new String[0];
        if (args.length > 1) {
            fields = Arrays.copyOfRange(args, 1, args.length);
        }
        while ((l = r.readLine()) != null) {

            l = l.trim();
            if (l.isEmpty()) {
                continue;
            }
            List<Element> els = new ArrayList<Element>();
            for (String token : l.split("\\s+")) {
                token = token.trim();
                if (token.isEmpty()) {
                    continue;
                }
                els.add(new Term(token));
            }

            run(e, new Passage(els), fields);
            run(e, new PAnd(els), fields);
            run(e, new Phrase(els), fields);
        }

        e.close();
    }
}
