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

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A common base class for unary operators.
 */
public abstract class Unary extends Operator implements Serializable {
    /**
     * The element this operator works on
     */
    protected Element element;
    
    public Unary() {
        super();
    }
    
    public Unary(Element element) {
        this.element = element;
        elements = new ArrayList<Element>();
        elements.add(element);
    }
}
