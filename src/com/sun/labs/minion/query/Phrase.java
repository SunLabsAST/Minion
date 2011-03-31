package com.sun.labs.minion.query;

import com.sun.labs.minion.QueryPipeline;
import com.sun.labs.minion.retrieval.QueryElement;
import java.io.Serializable;
import java.util.Collection;

/**
 * A passage retrieval operator.
 */
public class Phrase extends Proximity implements Serializable {

    public Phrase(Collection<Element> elements) {
        super(elements);
    }

    public Phrase(Element[] elements) {
        super(elements);
    }

    public Phrase() {
        super();
    }

    @Override
    public QueryElement getQueryElement(QueryPipeline pipeline) {
        return new com.sun.labs.minion.retrieval.Phrase(getQueryElements(pipeline));
    }
    
    @Override
    public String toString() {
        return String.format("(Phrase %s)", elements);
    }

}
