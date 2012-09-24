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

package com.sun.labs.minion.retrieval;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;

/**
 * An operator that takes a single saved field name as an operand and
 * then returns the documents that have no defined values for that field.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class Undefined extends UnaryOperator {
    
    public static final String logTag = "UNDEF";
    
    /**
     * Creates an undefined operator that takes a single operand, the
     * name of the field we want to check for undefined values.
     */
    public Undefined(NameTerm operand) {
        super(operand);
    }
    
    /**
     * Estimates the size of the results set.  The only estimate that we can
     * return quickly is the number of documents in the partition.  This will
     * pretty much ensure that this operator is always evaluated last.
     */
    @Override
    protected int calculateEstimatedSize() {
        return part.getNDocs();
    }
    
    /**
     * Evaluates the operator, returning a set of documents with no defined
     * field values.
     */
    @Override
    public ArrayGroup eval(ArrayGroup ag) {
        
        NameTerm field = (NameTerm) operands.get(0);
        FieldInfo fi = part.getPartitionManager().getMetaFile().getFieldInfo(field.getName());
        if(fi == null) {
            logger.warning(String.format("Unknown field %s in <undefined> operator", field.getName()));
            return new ArrayGroup(part, new int[0], 0);
        }
        
        if(!fi.hasAttribute(FieldInfo.Attribute.SAVED)) {
            logger.warning("Non saved field " + field.getName() + " for <undefined> operator");
            
            //
            // If this isn't a saved field, return the empty set.
            return new ArrayGroup(part, new int[0], 0);
        }
        
        DiskField sf = ((InvFileDiskPartition) part).getDF(fi);
        if(sf == null) {
            
            //
            // If the field is null, but saved, then there are no defined values
            // for this partition, so we need to return all of the documents.
            // If we had an input group, we'll just send that back, since the
            // intersection would return all the documents anyways.
            if(ag != null) {
                return ag;
            }

            //
            // Nope.  All the documents it is!
            return new NegativeGroup();
        }
        
        return sf.getUndefined(ag);
    }
    
}
