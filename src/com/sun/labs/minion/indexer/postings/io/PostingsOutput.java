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

package com.sun.labs.minion.indexer.postings.io;

import com.sun.labs.minion.util.buffer.WriteableBuffer;

/**
 * An interface to be implemented by things that write postings.
 */
public interface PostingsOutput {
    
    /**
     * Gets a temporary buffer that we can use to encode postings header data,
     * avoiding creating thousands of buffers during marshalling.
     * @return a temporary buffer that can be used when writing postings data.
     * This buffer should be cleared before use and not be retained past 
     * a single call to write.
     */
    public WriteableBuffer getTempBuffer();
    
    
    public int write(WriteableBuffer buff) throws java.io.IOException;

    /**
     * Writes out the buffers containing a set of postings information.
     * @param p the postings to write.
     * @return the number of bytes written.
     * @throws java.io.IOException if there are any errors writing the
     * postings.
     */
    public int write(WriteableBuffer[] buffs) throws java.io.IOException;

    /**
     * Gets the position of the current output.
     * @throws java.io.IOException if there is any error.
     * @return The current position of the output
     */
    public long position() throws java.io.IOException;

    /**
     * Flushes any buffered output to the underlying storage.
     *
     * @throws java.io.IOException if there is any error flushing the
     * postings.
     */
    public void flush() throws java.io.IOException;
    
    /**
     * Cleans up postings output from a failed marshall or merge operation.
     */
    public void cleanUp();
    
}// PostingsOutput
