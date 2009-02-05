package com.sun.labs.minion;

/**
 * An exception that can be thrown when a query built using the
 * programmatic query API contains errors.
 *
 * @see com.sun.labs.minion.query
 */
public class QueryException extends SearchEngineException {

    public QueryException(String s) {
        super(s);
    }

    public QueryException(String s, Throwable cause) {
        super(s, cause);
    }
}
