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

import com.sun.labs.minion.util.buffer.ReadableBuffer;
import java.io.IOException;

/**
 *
 */
public class CachedDocumentVectorLengths extends DocumentVectorLengths {

    private float[] cachedLens;

    private float[][] cachedFieldLens;
    
    public CachedDocumentVectorLengths(DiskPartition part, boolean adjustStats) throws IOException {
        super(part, adjustStats);
        cachedFieldLens = new float[part.getManager().getMetaFile().size()+1][];
    }

    @Override
    public synchronized float getVectorLength(int docID) {
        if (cachedLens == null) {
            cachedLens = uncompressLens(vecLens.duplicate());
        }
        return cachedLens[docID];
    }

    @Override
    public synchronized float getVectorLength(int docID, int fieldID) {
        switch (fieldID) {
            case -1:
                return getVectorLength(docID);
            default:
                if (fieldLens[fieldID] == null) {
                    return 1;
                }
                if(cachedFieldLens[fieldID] == null) {
                    cachedFieldLens[fieldID] = uncompressLens(fieldLens[fieldID].duplicate());
                }
                return cachedFieldLens[fieldID][docID];
        }
    }

    /**
     * Normalizes a set of document scores all in one go.
     *
     * @param docs the document IDs to normalize
     * @param scores the document scores
     * @param p the number of document IDs and scores in the array
     * @param qw the query weight to use for normalization
     * @param fieldID the ID of the field that the scores were computed from and that
     * should be used for normalization.
     */
    public void normalize(int[] docs, float[] scores, int p, float qw,
            int fieldID) {

        float[] lvl;

        synchronized(this) {
            switch(fieldID) {
                case -1:
                    if(cachedLens == null) {
                        cachedLens = uncompressLens(vecLens.duplicate());
                    }
                    lvl = cachedLens;
                    break;
                default:
                    if(fieldLens[fieldID] == null) {
                        return;
                    }
                    if(cachedFieldLens[fieldID] == null) {
                        cachedFieldLens[fieldID] = uncompressLens(
                                fieldLens[fieldID].duplicate());
                    }
                    lvl = cachedFieldLens[fieldID];
                    break;
            }
        }
        for(int i = 0; i < p; i++) {
            scores[i] /= (lvl[docs[i]] * qw);
        }
    }
    
    private float[] uncompressLens(ReadableBuffer b) {
        float[] ret = new float[part.getMaxDocumentID()+1];
        for (int i = 1; i <= part.getMaxDocumentID(); i++) {
            ret[i] = b.decodeFloat();
        }
        return ret;
    }


}
