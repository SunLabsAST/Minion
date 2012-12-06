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

package com.sun.labs.minion;

import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;

/**
 * A filter for results that can be applied against a result set when retrieving
 * results for display.
 * 
 * An implementation of this interface can be passed to the {@link ResultSet#getResults(int,int)}
 * method where it will be used to include (or exclude) particular results from 
 * the results that are returned to the user.
 * 
 */
public interface ResultsFilter {
    
    /**
     * Sets the partition that the filter will be running against. This can 
     * give you the opportunity to pre-compute field value IDs that can be used
     * to filter values more quickly.
     */
    public void setPartition(InvFileDiskPartition part);

    /**
     * Runs the filter against the result currently under consideration.
     * 
     * @param ra an accessor that can be used to get information about the current
     * result.  Note that the accessor is only valid for this call of the filter.
     * @return <code>true</code> if the current result should be included in the
     * results returned to the user, <code>false</code> if it should not.
     */
    public boolean filter(ResultAccessor ra);
    
    /**
     * Gets the number of times that the filter was used during the collection
     * of search results.
     */
    public int getTested();
    
    /**
     * Gets the number of times that a result passed the filter.
     */
    public int getPassed();
}
