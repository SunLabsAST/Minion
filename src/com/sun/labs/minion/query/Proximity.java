package com.sun.labs.minion.query;

import com.sun.labs.minion.QueryPipeline;
import com.sun.labs.minion.retrieval.QueryElement;
import com.sun.labs.minion.util.StringUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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
    
    protected List<QueryElement> getQueryElements(QueryPipeline pipeline) {
        int order = 0;
        List<QueryElement> operands = new ArrayList();
        for (Element e : elements) {
            QueryElement qel = e.getQueryElement(pipeline);
            qel.setOrder(order++);
            operands.add(qel);
        }
        return operands;
    }
}
