/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package com.sun.labs.minion.classification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A container for human selected terms that specifies terms that must or must
 * not occur in particular classifiers.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class HumanSelected  {
    
    private String name;
    
    private Set<String> include;
    
    private Set<String> exclude;
    
    private Map<String, WeightedTerm> weight;
    
    public HumanSelected(Element c) {
        name = c.getAttribute("name");
        NodeList il = c.getElementsByTagName("include");
        include = new HashSet<String>();
        for(int i = 0; i < il.getLength(); i++) {
            include.add(((Element) il.item(i)).getTextContent());
        }
        NodeList el = c.getElementsByTagName("exclude");
        exclude = new HashSet<String>();
        for(int i = 0; i < el.getLength(); i++) {
            exclude.add(((Element) el.item(i)).getTextContent());
        }
        NodeList wl = c.getElementsByTagName("weight");
        weight = new HashMap<String, HumanSelected.WeightedTerm>();
        for(int i = 0; i < wl.getLength(); i++) {
            Element wt = (Element) wl.item(i);
            String term = wt.getTextContent();
            weight.put(term, new WeightedTerm(term,
                    Float.parseFloat(wt.getAttribute("val"))));
        }
    }
    
    public boolean exclude(String term) {
        return exclude.contains(term);
    }
    
    public boolean include(String term) {
        return include.contains(term);
    }
    
    public float getWeight(String term) {
        WeightedTerm wt = weight.get(term);
        if(wt == null) {
            return 1;
        }
        return wt.weight;
    }
    
    public String getName() {
        return name;
    }
    
    class WeightedTerm {
        private String term;
        private float weight;
        public WeightedTerm(String term, float weight) {
            this.term = term;
            this.weight = weight;
        }
    }
    
}
