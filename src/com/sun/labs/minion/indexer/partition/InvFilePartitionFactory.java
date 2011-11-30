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

package com.sun.labs.minion.indexer.partition;

import com.sun.labs.util.props.ConfigDouble;
import com.sun.labs.util.props.ConfigInteger;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;

/**
 * A factory class for inverted file partitions.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class InvFilePartitionFactory extends DiskPartitionFactory {
    
    @ConfigInteger(defaultValue=3)
    public static final String PROP_MIN_STEM_LENGTH = "min_stem_length";
    
    private int minStemLength;
    
    @ConfigDouble(defaultValue=0.65)
    public static final String PROP_STEM_MATCH_CUTOFF = "stem_match_cutoff";
    
    private float stemMatchCutoff;
    
    /**
     * Creates a InvFilePartitionFactory
     */
    public InvFilePartitionFactory() {
    }
    
    @Override
    public DiskPartition getDiskPartition(int number, PartitionManager m) 
    throws java.io.IOException {
         return new InvFileDiskPartition(number, m, entryFactory);
   }
    
    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        minStemLength = ps.getInt(PROP_MIN_STEM_LENGTH);
        stemMatchCutoff = ps.getFloat(PROP_STEM_MATCH_CUTOFF);
    }
    
}
