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

import com.sun.labs.minion.indexer.postings.Occurrence;

import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;

/**
 * An interface for the features defined by classifiers.  Features have
 * names and can be encoded and decoded from buffers.
 */
public interface Feature extends Comparable<Feature>, Occurrence {

    /**
     * Sets the name of this feature.
     *
     * @param name The name of the feature.
     */
    public void setName(String name);
    

    /**
     * Gets the name of this feature.
     *
     * @return the name of the feature.
     */
    public String getName();

    /**
     * Encodes the information in this feature onto the given buffer.
     *
     * @param b a buffer onto which the feature's information can be
     * encoded.
     */
    public void encode(WriteableBuffer b);

    /**
     * Decodes the information in this feature from the given buffer.
     *
     * @param b a buffer from which the feature's information can be
     * decoded.
     */
    public void decode(ReadableBuffer b);
}// Feature
