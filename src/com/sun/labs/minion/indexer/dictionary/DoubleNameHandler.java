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

public class DoubleNameHandler implements NameEncoder<Double>, NameDecoder<Double> {
    
    /**
     * Encodes the double name of an entry, given the name of the previous
     * entry in the dictionary.
     *
     * @param prev The name of the previous entry in the dictionary.
     * @param curr The name of the entry to encode.
     * @param b The buffer onto which the name of the term should be
     * encoded.
     */
    public void encodeName(Double prev, Double curr, WriteableBuffer b) {

        int shared = 0;

        //
        // Put the bits into a long.
        long cn = Double.doubleToRawLongBits(curr.doubleValue());
        
        if(prev != null) {
            
            
            long pn = Double.doubleToRawLongBits(prev.doubleValue());

            //
            // Get the shared initial context.
            shared = LongNameHandler.getShared(pn, cn);
        }
        
        //
        // Encode the number of shared bytes.
        b.byteEncode(shared);

        //
        // Encode the remaining bytes.
        b.byteEncode(cn, 8 - shared);
    }

    /**
     * Decodes the double name of an entry, given a buffer of encoded
     * names and the name of the previous entry in the dictionary.
     *
     * @param prev The name of the previous entry in the dictionary.
     * @param b The buffer onto which the name of the term should be
     * encoded.
     * @return The decoded name.
     */
    public Double decodeName(Double prev, ReadableBuffer b) {

        //
        // Get the shared context and the remaining bytes.
        int shared = b.byteDecode();
        long val = b.byteDecodeLong(8 - shared);

        if(prev != null && shared > 0) {

            //
            // Mask in the leading bytes.
            long mask = 0xff00000000000000L;
            long pn =
                Double.doubleToRawLongBits(prev.doubleValue());
            
            for(int i = 0; i < shared; i++) {
                val |= (pn & mask);
                mask >>= 8;
            }
        }

        return Double.longBitsToDouble(val);
    }

    /**
     * Determines whether one double starts with another, which is true
     * only if they are equal!
     */
    public boolean startsWith(Double n, Double m) {
        return n.equals(m);
    }

} // DoubleNameHandler
