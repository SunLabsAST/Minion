package com.sun.labs.minion.util;


import java.util.Iterator;
import java.util.logging.Logger;

/**
 * A wrapper for an iterator that 
 */
public class QueuableIterator<N extends Comparable> implements Iterator<N>, Comparable<QueuableIterator<N>> {

    private static final Logger logger = Logger.getLogger(QueuableIterator.class.getName());
    
    private Iterator<N> i;
    
    private N current;
    
    public QueuableIterator(Iterator<N> i) {
        this.i = i;
    }

    @Override
    public boolean hasNext() {
        return i.hasNext();
    }

    @Override
    public N next() {
        current = i.next();
        return current;
    }
    
    public N getCurrent() {
        return current;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int compareTo(QueuableIterator<N> o) {
        return current.compareTo(o.current);
    }
}
