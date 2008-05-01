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

package com.sun.labs.minion.knowledge;

import com.sun.labs.util.props.ConfigString;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import com.sun.labs.minion.lexmorph.LiteMorph_en;

import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import com.sun.labs.util.props.PropertySheet.PropertyType;
import java.rmi.registry.Registry;

/**
 * A KnowledgeMorph is a wrapper around a LiteMorph, given that there's no way
 * to create a LiteMorph (due to maintaining singleton-ness).
 * This is a way of using reflection to create the appropriate LiteMorph instance.
 * @author Bernard Horan
 *
 */
public class KnowledgeMorph implements Configurable, KnowledgeSource {
    private Class liteMorphClass = LiteMorph_en.class;
    private String liteMorphSelector = "getMorph";
    private String configName;
    private KnowledgeSource knowledgeSource;
    
    /**
     * The property name for the lightMorph class.
     */
    @ConfigString
    private final String PROP_LITEMORPH_CLASS = "light_morph_class";
    
    /**
     * The property name for the static method selector.
     */
    @ConfigString
    private final String PROP_METHOD_SELECTOR = "method_selector";

    /**
     * Default constructor.
     */
    public KnowledgeMorph() {
        super();
        
    }

    /* (non-Javadoc)
     * @see com.sun.labs.util.props.Configurable#newProperties(com.sun.labs.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        String liteMorphClassName = ps.getString(PROP_LITEMORPH_CLASS);
        if (liteMorphClassName == null) {
            //
            //No class provided
            return;
        }
        String selector = ps.getString(PROP_METHOD_SELECTOR);
        if (selector == null) {
            //
            //No selector provided
        } else {
            liteMorphSelector = selector;
        }
        
        try {
            liteMorphClass = Class.forName(liteMorphClassName);
        } catch (ClassNotFoundException e) {
            throw new PropertyException(ps.getInstanceName(), PROP_LITEMORPH_CLASS, "No such class: " + liteMorphClassName);
        }
        
        
    }

    /* (non-Javadoc)
     * @see com.sun.labs.util.props.Configurable#getName()
     */
    public String getName() {
        return configName;
    }
    
    /**
     * Use reflection to create a knowledge source
     * @return a KnowledgeSource 
     */
    protected KnowledgeSource createKnowledgeSource() {
        Method liteMorphSelectorMethod = null;
        Class[] parameterTypes = new Class[0];
        try {
            liteMorphSelectorMethod = liteMorphClass.getDeclaredMethod(liteMorphSelector, parameterTypes);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        if (liteMorphSelectorMethod == null) {
            return null;
        }
        
        Object ks = null;
        try {
            ks = liteMorphSelectorMethod.invoke(liteMorphClass, new Object[0]);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return (KnowledgeSource) ks;
    }
    
    /**
     * Accessor method, lazily creates a knowledgesoruce if one doesn't exist
     * @return a KnowledgeSource
     */
    public KnowledgeSource getKnowledgeSource() {
        if (knowledgeSource == null) {
            knowledgeSource = createKnowledgeSource();
        }
        return knowledgeSource;
    }

    /* (non-Javadoc)
     * @see com.sun.labs.minion.knowledge.KnowledgeSource#variantsOf(java.lang.String)
     */
    public Set<String> variantsOf(String term) {
        KnowledgeSource ks = getKnowledgeSource();
        if (ks == null) {
            return null;
        } else {
            return ks.variantsOf(term);
        }
    }

}
