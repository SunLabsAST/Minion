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

package com.sun.labs.minion.retrieval.cache;

import java.util.Map;
import com.sun.labs.minion.classification.WeightedFeature;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.retrieval.DocumentVectorImpl;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.MinionLog;

/**
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class DocCacheElement {
    
    protected DocumentVectorImpl dvi;
    
    protected static MinionLog log = MinionLog.getLog();
    
    protected static String logTag = "DCE";
    
    /**
     * Creates an element in a document term cache.
     */
    public DocCacheElement(DocKeyEntry key, DiskPartition part, String field,
            WeightingFunction wf, WeightingComponents wc) {
        dvi = new DocumentVectorImpl(part.getManager().getEngine(),
                key,
                field, wf, wc);
    }
    
    public WeightedFeature[] getFeatures() {
        return dvi.getFeatures();
    }
    
    public DocumentVectorImpl getDVI() {
        return dvi;
    }
}
