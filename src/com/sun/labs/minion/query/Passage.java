package com.sun.labs.minion.query;

import com.sun.labs.minion.retrieval.QueryElement;
import java.io.Serializable;
import java.util.Collection;

/**
 * A passage retrieval operator.
 */
public class Passage extends Proximity implements Serializable {

    public Passage(Collection<Element> elements) {
        super(elements);
    }

    public Passage(Element[] elements) {
        super(elements);
    }

    public Passage() {
        super();
    }

    @Override
    public QueryElement getQueryElement() {
        return new com.sun.labs.minion.retrieval.Passage(getQueryElements());
    }

    @Override
    public String toString() {
        return String.format("(Passage %s)", elements);
    }
}
