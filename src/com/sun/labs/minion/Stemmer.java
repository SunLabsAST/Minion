package com.sun.labs.minion;

/**
 * A stemmer.
 */
public interface Stemmer {
    /**
     * Stems the given token.
     * @param token the token to stem.
     * @return the stem.
     */
    public String stem(String token);
}
