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

package com.sun.labs.minion.indexer.dictionary;

import java.util.Date;

import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;

public class DateNameHandler extends LongNameHandler {
    
    /**
     * Encodes the name of an entry, given the name of the previous entry
     * in the dictionary.
     *
     * @param prev The name of the previous entry in the dictionary.
     * @param curr The name of the entry to encode.
     * @param b The buffer onto which the name of the term should be
     * encoded.
     */
    public void encodeName(Object prev, Object curr, WriteableBuffer b) {
        Long d1 = null;
        if(prev != null) {
            d1 = new Long(((Date) prev).getTime());
        }
        super.encodeName(d1, new Long(((Date) curr).getTime()),
                         b);
    }

    /**
     * Decodes the name of an entry, given a buffer of encoded names and
     * the name of the previous entry in the dictionary.
     *
     * @param prev The name of the previous entry in the dictionary.
     * @param b The buffer onto which the name of the term should be
     * encoded.
     * @return The decoded name.
     */
    public Object decodeName(Object prev, ReadableBuffer b) {

        Long d1 = null;
        if(prev != null) {
            d1 = new Long(((Date) prev).getTime());
        }
        return new Date(((Long) super.decodeName(d1, b)).longValue());
    }
    
} // DateNameHandler
