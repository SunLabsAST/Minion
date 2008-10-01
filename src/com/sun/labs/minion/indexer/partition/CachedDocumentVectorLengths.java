/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
    }

    @Override
    public synchronized float getVectorLength(int docID) {
        if (cachedLens == null) {
            cachedLens = uncompressLens(vecLens);
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
                    cachedFieldLens[fieldID] = uncompressLens(fieldLens[fieldID]);
                }
                return cachedFieldLens[fieldID][docID];
        }
    }

    public void normalize(int[] docs, float[] scores, int p, float qw) {
        if (cachedLens == null) {
            cachedLens = uncompressLens(vecLens);
        }
        for (int i = 0; i < p; i++) {
            scores[i] /= (cachedLens[docs[i]] * qw);
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
