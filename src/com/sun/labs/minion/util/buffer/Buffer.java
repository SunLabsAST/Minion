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

package com.sun.labs.minion.util.buffer;

/**
 * An interface for the buffers used by the search engine.  Buffers have a
 * postition that indicates where the next byte will be written or read, a
 * limit that indicates the last byte that may be written or read, and a
 * way to determine how much space is remaining in a buffer.
 *
 * @see WriteableBuffer
 * @see ReadableBuffer
 */
public interface Buffer {

    /**
     * Gets the position in the buffer.
     *
     * @return The position in the buffer.
     */
    public int position();

    /**
     * Sets the position in the buffer.
     *
     * @param position The point to which the buffer's position should be
     * set.
     */
    public void position(int position);

    /**
     * Gets the limit of this buffer, i.e., the last readable position.
     */
    public int limit();

    /**
     * Sets the limit of this buffer, i.e., the last readable position.
     */
    public void limit(int l);

    /**
     * Gets the amount of space remaining in the buffer.
     */
    public int remaining();
    
}// Buffer
