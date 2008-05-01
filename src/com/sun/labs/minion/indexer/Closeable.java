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

package com.sun.labs.minion.indexer;

/**
 * An interface for things that can be closed, but that must respect a delay to
 * account for things that may be in use.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public interface Closeable {
    
    public void setCloseTime(long closeTime);
    
    public long getCloseTime();
    
    /**
     * Close this thing.
     *
     * @param currTime the current time.  If the time that the thing is supposed
     * to be closed is before the current time, then it will be closed.
     * @return <code>true</code> if the thing was closed, <code>false</code> otherwise.
     */
    public boolean close(long currTime);
    
    public void createRemoveFile();
    
}
