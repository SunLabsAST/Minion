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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;

/**
 * This class encapsulates a policy whereby the first non-null variants of the 
 * knowledge sources are returned.
 * @author bh37721
 * $Id: SequencedKnowledgeSource.java,v 1.1.2.6 2008/01/17 18:34:35 stgreen Exp $
 */
public class SequencedKnowledgeSource implements CompositeKnowledgeSource {

    List<KnowledgeSource> sources = new ArrayList<KnowledgeSource>();

    /**
     * Property name for list of knowledge sources
     */
    @ConfigComponentList(type = KnowledgeSource.class)
    private final String PROP_KNOWLEDGE_SOURCES =
            "knowledge_sources";

    /* (non-Javadoc)
	 * @see com.sun.labs.minion.knowledge.CompositeKnowledgeSource#addSource(com.sun.labs.minion.knowledge.KnowledgeSource)
	 */
    public void addSource(KnowledgeSource aSource) {
        sources.add(aSource);
    }

    /**
	 * Return the variants of the first knowledge source that has any
	 */
    public Set<String> variantsOf(String term) {
        Set<String> allVariants = new HashSet<String>();
        for (Iterator<KnowledgeSource> sourceIt = sources.iterator(); sourceIt.hasNext();) {
            if (!allVariants.isEmpty()) {
                break;
            }
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
