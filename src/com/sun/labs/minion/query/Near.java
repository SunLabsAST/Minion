package com.sun.labs.minion.query;

import com.sun.labs.minion.QueryPipeline;
import com.sun.labs.minion.retrieval.QueryElement;
import java.io.Serializable;
import java.util.Collection;

/**
 * A passage retrieval operator.
 */
public class Near extends Proximity implements Serializable {
    
    /**
     * The maximum window size for the near operator.
     */
    private int n = 1000;

    public Near(Collection<Element> elements) {
        super(elements);
    }
    
    public Near(Collection<Element> elements, int n) {
        super(elements);
        this.n = n;
    }

    public Near(Element[] elements) {
        super(elements);
    }

    public Near(Element[] elements, int n) {
        super(elements);
        this.n = n;
    }

    public Near() {
        super();
    }

    @Override
    public QueryElement getQueryElement(QueryPipeline pipeline) {
        return new com.sun.labs.minion.retrieval.Near(getQueryElements(pipeline), n);
    }
    
    @Override
    public String toString() {
        return String.format("(Near/%d %s)", n, elements);
    }
}
