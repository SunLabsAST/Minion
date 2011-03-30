package com.sun.labs.minion.query;

import com.sun.labs.minion.retrieval.QueryElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A base class for proximity operators.
 */
public abstract class Proximity extends Operator implements Serializable {

    public Proximity(Collection<Element> elements) {
        super(elements);
    }

    public Proximity(Element[] elements) {
        super(elements);
    }

    public Proximity() {
        super();
    }
    
    protected List<QueryElement> getQueryElements() {
        int order = 0;
        List<QueryElement> operands = new ArrayList();
        for (Element e : elements) {
            QueryElement qel = e.getQueryElement();
            qel.setOrder(order++);
            operands.add(qel);
        }
        return operands;
    }
}
