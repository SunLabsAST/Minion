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
            QueryElement qel = SearchEngineImpl.parseQuery(query, defaultOperator, queryGrammar, pipeline);
            if(fields != null && !fields.isEmpty()) {
                for(String field : fields) {
                    qel.addSearchFieldName(field);
                }
            }
            qel.strictEval = strict;
            return qel;
        } catch(SearchEngineException ex) {
            Throwable cause = ex.getCause();
            if(cause == null) {
                cause = ex;
            }
            logger.log(Level.SEVERE, String.format("Error parsing query %s: %s", query, cause));
            return null;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final ParsedElement other = (ParsedElement) obj;
        if((this.query == null) ? (other.query != null) : !this.query.equals(other.query)) {
            return false;
        }
        if(this.defaultOperator != other.defaultOperator) {
            return false;
        }
        if(this.queryGrammar != other.queryGrammar) {
            return false;
        }
        return true;
    }
}
