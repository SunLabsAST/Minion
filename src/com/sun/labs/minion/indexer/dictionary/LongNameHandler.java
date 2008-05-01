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

import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;

public class LongNameHandler implements NameEncoder, NameDecoder {
    
    /**
     * Encodes the long name of an entry, given the name of the previous
     * entry in the dictionary.
     *
     * @param prev The name of the previous entry in the dictionary.
     * @param curr The name of the entry to encode.
     * @param b The buffer onto which the name of the term should be
     * encoded.
     */
    public void encodeName(Object prev, Object curr,
                           WriteableBuffer b) {

        int shared = 0;

        if(prev != null) {

            //
            // Get the shared initial context.
            shared = getShared(((Long) prev).longValue(),
                               ((Long) curr).longValue());
        }
        
        //
        // Encode the number of shared bytes.
        b.byteEncode(shared);

        //
        // Encode the remaining bytes.
        b.byteEncode(((Long) curr).longValue(), 8 - shared);
    }

    /**
     * Decodes the long name of an entry, given a buffer of encoded
     * names and the name of the previous entry in the dictionary.
     *
     * @param prev The name of the previous entry in the dictionary.
     * @param b The buffer onto which the name of the term should be
     * encoded.
     * @return The decoded name.
     */
    public Object decodeName(Object prev, ReadableBuffer b) {
        //
        // Get the shared context and the remaining bytes.
        int shared = b.byteDecode();
        long val = b.byteDecodeLong(8 - shared);

        if(prev != null && shared > 0) {

            //
            // Mask in the leading bytes.
            long mask = 0xff00000000000000L;
            long pn = ((Long) prev).longValue();
            
            for(int i = 0; i < shared; i++) {
                val |= (pn & mask);
                mask >>= 8;
            }
        }

        //
        // Set the name.
        return new Long(val);
    }

    /**
     * Determines whether one long starts with another, which is true only
     * if they are equal!
     */
    public boolean startsWith(Object n, Object m) {
        return n.equals(m);
    }

    /**
     * Finds the number of initial bytes shared by two longs.
     *
     * @param i One integer.
     * @param j Another integer.
     * @return the number of leading bytes shared by the two integers.
     */
    public static int getShared(long i, long j) {
        //
        // Figure out the bytes that they have in common.
        long mask = 0xff00000000000000L;

        //
        // XOR the longs, which will leave leading 0 bytes where they are
        // equal.
        long xor = i ^ j;

        //
        // Figure out where the first non-zero byte is in the long.
        int shared;
        for(shared = 0; shared < 8; shared++, mask >>= 8) {
            if((xor & mask) != 0) {
                break;
            }
        }

        return shared;
    }

} // LongNameHandler
