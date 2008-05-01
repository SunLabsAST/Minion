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

package com.sun.labs.minion.classification;

import com.sun.labs.minion.indexer.postings.DocumentVectorPostings;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.PostingsIterator;

import com.sun.labs.minion.util.Util;

import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;


/**
 * Postings for the cluster documents in the cluster partition.  These
 * are similar in their behavior to DocumentVectorPostings in that
 * occurences do not need to be added to them in order.  Unlike DVP, the
 * cluster postings extends that functionality into the merge behavior.
 * Postings need not be appended in order -- that is, the append method
 * is implemented as a merge.
 *
 * @author Jeff Alexander
 * @version $Revision: 1.1.2.6 $
 */

public class ClusterPostings extends DocumentVectorPostings
{

    protected static String logTag = "CP";
    /** 
     * Creates a set of postings suitable for use during indexing.
     */
    public ClusterPostings() {
        super();
    }

    /**
     * Creates a set of postings suitable for use during querying.
     *
     * @param b a buffer containing the encoded postings.
     */
    public ClusterPostings(ReadableBuffer b) {
        super(b);
    }

    /**
     * Appends another set of postings to this one, removing any data
     * associated with deleted documents.
     * 
     * @param p The postings to append.  Implementers can safely assume
     * that the postings being passed in are of the same class as the
     * implementing class.
     * @param start The new starting document ID for the partition
     * that the entry was drawn from.
     * @param idMap A map from old IDs in the given postings to new IDs
     * with gaps removed for deleted data.  If this is null, then there are
     * no deleted documents.
     */
    public void append(Postings p, int start, int[] idMap) {
        
        //
        // Iterate through the postings, adding the mapped occurrences
        // to our array of ids.
        PostingsIterator pi = p.iterator(null);
        while (pi.next()) {
            int origID = pi.getID();
            int mapID = idMap != null ? idMap[origID] : origID;

            //
            // If an id was deleted, skip it.
            if (mapID < 0) {
                continue;
            }

            //
            // Get the ID for this term in the new partition.
            int cID = mapID + start - 1;
            if(cID >= ids.length) {
                ids = Util.expandInt(ids, cID*2);
            }
            if (ids[cID] == 0) {
                nIDs++;
            }

            ids[cID]++;
            lastID = Math.max(lastID, cID);
        }
    }

    /** 
     * Writes the in-memory frequency array out to the postings buffers.
     * This is done in the remap method for consistency with the super
     * class.  Otherwise, this would probably be done in finish().  Since
     * the clusters' contents will already have been remapped at this point
     * (from the append method) we just flush the contents directly out to
     * disk.
     * 
     * @param idMap a map from old IDs to new IDs -- ignored
     */
    public void remap(int[] idMap) {
        
        //
        // If this wasn't called from a merge, just do the normal stuff
        if (idMap != null) {
            super.remap(idMap);
            return;
        }
        
        post = new ArrayBuffer(nIDs * 2);


        //
        // Since remap will get called once the merge is done, it is
        // now safe to dump all the in-memory data out to disk.
        prevID = 0;
        for (int i = 0, added = 0; i < ids.length; i++) {
            if (ids[i] > 0) {
                added++;
                if (added % skipSize == 0) {
                    addSkip(i, post.position());
                }

                int freq = ids[i];

                //
                // Update the postings stats
                to += freq;
                maxfdt = Math.max(maxfdt, freq);

                //
                // Encode the data
                ((WriteableBuffer) post).byteEncode(i - prevID);
                ((WriteableBuffer) post).byteEncode(freq);
                prevID = i;
            }
        }
        
        lastID = prevID;
    }
}
