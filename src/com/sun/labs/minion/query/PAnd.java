package com.sun.labs.minion.query;

import com.sun.labs.minion.QueryPipeline;
import com.sun.labs.minion.retrieval.QueryElement;
import java.io.Serializable;
import java.util.Collection;

/**
 * A passage retrieval operator.
 */
public class PAnd extends Proximity implements Serializable {

    public PAnd(Collection<Element> elements) {
        super(elements);
    }

    public PAnd(Element[] elements) {
        super(elements);
    }

    public PAnd() {
        super();
    }

    @Override
    public QueryElement getQueryElement(QueryPipeline pipeline) {
        return new com.sun.labs.minion.retrieval.PAnd(getQueryElements(pipeline));
    }

    @Override
    public String toQueryString() {
        return Proximity.getPrefixOperatorQueryString("<pand>", elements, fields);
    }
    
    @Override
    public String toString() {
        return String.format("(PAnd %s)", elements);
    }

}
