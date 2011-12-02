/*
 * Copyright 2011 Oracle and/or its affiliates. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 */
package com.sun.labs.minion.query;

import com.sun.labs.minion.QueryPipeline;
import com.sun.labs.minion.retrieval.QueryElement;
import java.io.Serializable;

/**
 * Adjusts the weight of part of a query element by the given adjustment
 * factor.  The adjustment is multiplicative, so an adjustment of 1.0
 * is equivalent to not using the Weight operator.
 */
public class Weight extends Unary implements Serializable {
        
    protected float factor;
    
    /**
     * Adjust the weight of a query element
     * 
     * @param element the subelement to modify
     * @param factor the adjustment factor, with 1.0 being neutral
     */
    public Weight(Element element, float factor) {
        super(element);
        this.factor = factor;
    }

    @Override
    public QueryElement getQueryElement(QueryPipeline pipeline) {
        return new com.sun.labs.minion.retrieval.Weight(
                element.getQueryElement(pipeline), factor);
    }

    @Override
    public String toQueryString() {
        return String.format("<weight> %f %s", factor, element.toQueryString());
    }
    
    
    
    @Override
    public String toString() {
        return "(Weight " + (elements.size() > 0 ? elements.get(0).toString() : "") + ")";
    }
}
