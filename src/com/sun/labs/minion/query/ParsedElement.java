package com.sun.labs.minion.query;

import com.sun.labs.minion.QueryPipeline;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.Searcher;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.retrieval.QueryElement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An element in a query that contains text that should be passed through a 
 * query parser, which will generate a tree of query elements for evaluation.
 */
public class ParsedElement extends Element {
    
    private static final Logger logger = Logger.getLogger(ParsedElement.class.getName());
    
    private String query;
    
    private Searcher.Operator defaultOperator;
    
    private Searcher.Grammar queryGrammar;
    
    public ParsedElement(String query) {
        this(query, Searcher.Operator.PAND, Searcher.Grammar.STRICT);
    }
    
    public ParsedElement(String query, Searcher.Operator defaultOperator, Searcher.Grammar queryGrammar) {
        this.query = query;
        this.defaultOperator = defaultOperator;
        this.queryGrammar = queryGrammar;
    }

    @Override
    public String toQueryString() {
        return query;
    }

    @Override
    public QueryElement getQueryElement(QueryPipeline pipeline) {
        try {
            return SearchEngineImpl.parseQuery(query, defaultOperator, queryGrammar, pipeline);
        } catch(SearchEngineException ex) {
            Throwable cause = ex.getCause();
            if(cause == null) {
                cause = ex;
            }
            logger.log(Level.SEVERE, String.format("Error parsing query %s: %s", query, cause));
            return null;
        }
    }

}
