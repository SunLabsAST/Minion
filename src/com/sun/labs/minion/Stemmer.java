package com.sun.labs.minion;

import com.sun.labs.util.props.Component;

/**
 * A stemmer.
 */
public interface Stemmer extends Component, Cloneable {
    
    /**
     * Stems the given token.
     * @param token the token to stem.
     * @return the stem.
     */
    public String stem(String token);
    
    /**
     * Returns a copy of this stemmer.  Used by the factory to generate new
     * stemmers for each field.
     * @return a copy of this stemmer.
     */
    public Stemmer clone();
}
