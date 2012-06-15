package com.sun.labs.minion.util;


import java.util.logging.Logger;

/**
 * A pair of things.
 */
public class Pair<T1, T2> {

    private static final Logger logger = Logger.getLogger(Pair.class.getName());
    
    private T1 a;
    
    private T2 b;
    
    public Pair(T1 a, T2 b) {
        this.a = a;
        this.b = b;
    }

    public T1 getA() {
        return a;
    }

    public T2 getB() {
        return b;
    }

    @Override
    public int hashCode() {
        return a.hashCode() + b.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final Pair<T1, T2> other = (Pair<T1, T2>) obj;
        if(this.a != other.a && (this.a == null || !this.a.equals(other.a))) {
            return false;
        }
        if(this.b != other.b && (this.b == null || !this.b.equals(other.b))) {
            return false;
        }
        return true;
    }
    
    
    
}
