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

import com.sun.labs.util.props.ConfigComponentList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Iterator;

import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;

/**
 * This class encapsulates a policy whereby the union of variants of all
 * the knowledge sources are combined.
 */
public class CombinedKnowledgeSource implements CompositeKnowledgeSource {

    protected Set<KnowledgeSource> sources = new HashSet<KnowledgeSource>();

    /**
     * Property name for list of knowledge sources
     */
    @ConfigComponentList(type = KnowledgeSource.class)
    private final String PROP_KNOWLEDGE_SOURCES =
            "knowledge_sources";

    /* (non-Javadoc)
	 * @see com.sun.labs.minion.knowledge.CompositeKnowledgeSource#addSource()
	 */
    public void addSource(KnowledgeSource aSource) {
        sources.add(aSource);

    }

    /**
	 * Combine the variants of all the knowledge sources (eliminating duplicates)
	 */
    public Set<String> variantsOf(String term) {
        Set<String> allVariants = new HashSet<String>();
        for (Iterator<KnowledgeSource> sourceIt = sources.iterator(); sourceIt.hasNext();) {
            KnowledgeSource ks = sourceIt.next();
            allVariants.addAll(ks.variantsOf(term));

        }

        return allVariants;
    }

    /* (non-Javadoc)
     * @see com.sun.labs.util.props.Configurable#newProperties(com.sun.labs.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        List sources = ps.getComponentList(PROP_KNOWLEDGE_SOURCES);
        for (Iterator iter = sources.iterator(); iter.hasNext();) {
            addSource((KnowledgeSource) iter.next());
        }

    }

}
