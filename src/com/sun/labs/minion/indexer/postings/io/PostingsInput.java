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

import com.sun.labs.minion.util.buffer.ReadableBuffer;

/**
 * An interface for things that can be used to read postings.
 */
public interface PostingsInput {

    /**
     * Reads a set of postings, returning them as a set of postings of the
     * appropriate type.
     * @param type the type of the postings to return.
     * @param offset The offset in the input at which the postings can be
     * found.
     * @param size The number of bytes to read to get the postings.
     * @throws java.io.IOException if there is any error reading the
     * postings.
     * @return A readable buffer containing the postings.
     */
    public ReadableBuffer read(long offset, int size) throws java.io.IOException;
    
}// PostingsInput
