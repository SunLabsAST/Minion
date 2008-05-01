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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.retrieval.ArrayGroup;
import com.sun.labs.minion.retrieval.ArrayGroup.DocIterator;
import com.sun.labs.minion.util.MinionLog;
import com.sun.labs.minion.util.Util;
import com.sun.labs.minion.util.buffer.FileReadableBuffer;
import com.sun.labs.minion.util.buffer.FileWriteableBuffer;

/**
 * A class that can be used to save feature vectors in an index.  A feature vector
 * is simply an array of <code>double</code>s that represent the features.  The
 * width of the feature vector is determined by the first vector that is indexed.
 * If subsequent values have a different width a warning will be issued.
 *
 * <p>
 *
 * Currently, this class will only store one feature vector per document.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class FeatureVector implements SavedField {
    
    /**
     * The information for this field.
     */
    protected FieldInfo fi;
    
    /**
     * A map from document IDs to the indices where feature vectors can be found
     * in the stored features.
     */
    protected int idToFeat[];
    
    /**
     * The features stored during indexing.
     */
    protected double features[];
    
    /**
     * The current position in the features array.
     */
    protected int pos = 0;
    
    /**
     * The width of the feature vectors that we're storing.
     */
    protected int width = 0;
    
    protected static MinionLog log = MinionLog.getLog();
    
    protected static String logTag = "FV";
    
    /**
     * Creates a <code>FeatureVector</code> that can be used to store data
     * at indexing time.
     */
    public FeatureVector(FieldInfo fi) {
        this.fi = fi;
        idToFeat = new int[1024];
        features = new double[2048];
        pos = 1;
    }
    
    /**
     * Constructs a feature vector field that will be used to retrieve
     * data during querying.
     *
     * @param field The <code>FieldInfo</code> for this saved field.
     * @param dictFile The file containing the dictionary for this field.
     * @param postFiles The files containing the postings for this field.
     * @param part The disk partition that this field is associated with.
     *
     * @throws java.io.IOException if there is any error loading the field
     * data.
     */
    public FeatureVector(FieldInfo field,
            RandomAccessFile dictFile,
            RandomAccessFile[] postFiles,
            DiskPartition part)
            throws java.io.IOException {
        this.fi = (FieldInfo) field.clone();
        
        
        width = dictFile.readInt();
        pos = dictFile.readInt();
        int maxID = dictFile.readInt();
        FileReadableBuffer buff = new FileReadableBuffer(dictFile,
                dictFile.getFilePointer(),
                maxID*4 + pos * 8,
                32768);
        
        idToFeat = new int[maxID+1];
        for(int i = 1; i <= maxID; i++) {
            idToFeat[i] = buff.byteDecode(4);
        }
        
        features = new double[pos];
        for(int i = 0; i < pos; i++) {
            features[i] = Double.longBitsToDouble(buff.byteDecodeLong(8));
        }
    }
    
    
    /**
     * Adds data to this saved field.  Assumes that data is an array
     * of <code>double</code.  The width of the vectors that are
     * stored by this field will be set by the fist vector that is
     * stored.  If subsequent vectors have a different length, a
     * warning will be issued.
     *
     * @param docID the document ID for the data we're adding.
     * @param data the data to add. We assume that this is an array of <code>double</code>
     * @throws ClassCastException if data is not an array of double.
     */
    public void add(int docID, Object data) {
        
        double[] vector = (double[]) data;
        
        if(width == 0) {
            width = vector.length;
        }
        
        if(vector.length != width) {
            log.warn(logTag, 3, "Incorrect feature vector length for " +
                    fi.getName() + " got " + vector.length + " expected " +
                    width + ".  Ignoring");
            return;
        }
        
        //
        // Make sure we have enough room in our id to features map.
        if(docID >= idToFeat.length) {
            idToFeat = Util.expandInt(idToFeat, idToFeat.length*2);
        }
        
        //
        // Make sure we have enough room in our features vector.
        if(pos+width >= features.length) {
            features = Util.expandDouble(features, features.length*2);
        }
        
        //
        // Save the data.
        idToFeat[docID] = pos;
        System.arraycopy(vector, 0, features, pos, width);
        pos += width;
    }
    
    /**
     * Dumps our saved data to the file.  We won't actually store anything in the
     * postings file, we'll just dump everything to the dictionary file.
     */
    public void dump(String path, RandomAccessFile dictFile,
            PostingsOutput[] postOut, int maxID) throws IOException {
        
        //
        // Write the header stuff.
        dictFile.writeInt(width);
        dictFile.writeInt(pos);
        dictFile.writeInt(maxID);
        
        //
        // Write the positions for each document ID.
        FileWriteableBuffer buff = new FileWriteableBuffer(dictFile, 32768);
        for(int i = 1; i <= maxID; i++) {
            buff.byteEncode(idToFeat[i], 4);
        }
        
        //
        // Encode the features.
        for(int i = 0; i < pos; i++) {
            buff.byteEncode(Double.doubleToRawLongBits(features[i]), 8);
        }
        
        buff.flush();
    }
    
    /**
     * Unsupported operation.
     */
    public QueryEntry get(Object v, boolean caseSensitive) {
        throw new UnsupportedOperationException("Feature vectors do not " +
                "support get by value.");
    }
    
    public FieldInfo getField() {
        return fi;
    }
    
    /**
     * Gets the data saved for a particular document ID.  If no data
     * was stored for that ID, <code>null</code> is returned.
     *
     * @param docID the document whose data we want
     * @param all if <code>true</code> a list containing the single stored value
     * for the document will be returned.
     * @return the data
     */
    public Object getSavedData(int docID, boolean all) {
        int pos = idToFeat[docID];
        if(pos == 0) {
            return null;
        }
        double[] ret = new double[width];
        System.arraycopy(features, pos, ret, 0, width);
        
        if(all) {
            List l = new ArrayList();
            l.add(ret);
            return l;
        }
        return ret;
    }
    
    public ArrayGroup getUndefined(ArrayGroup ag) {
        ArrayGroup ret = new ArrayGroup(ag == null ? 2048 : ag.getSize());
        
        if(ag == null) {
            for(int i = 1; i < idToFeat.length; i++) {
                if(idToFeat[i] == 0) {
                    ret.addDoc(i);
                }
            }
        } else {
            //
            // Just check for the documents in the set.
            for(DocIterator i = ag.iterator(); i.next(); ) {
                if(idToFeat[i.getDoc()] == 0) {
                    ret.addDoc(i.getDoc());
                }
            }
        }
        return ret;
    }
    
    public DictionaryIterator iterator(Object lowerBound, boolean includeLower,
            Object upperBound, boolean includeUpper) {
        return null;
    }
    
    public int size() {
        return features.length / width;
    }
    
    public int compareTo(Object o) {
        return fi.getID() - ((FeatureVector) o).fi.getID();
    }
    
    
    public void clear() {
        pos = 1;
        for(int i = 0; i < idToFeat.length; i++) {
            idToFeat[i] = 0;
        }
    }
    
    public long bytesInUse() {
        return idToFeat.length * 4 + features.length * 8;
    }
    
    /**
     * Gets the default value for a feature vector, which is <code>null</code>
     */
    public Object getDefault() {
        return null;
    }
    
    /**
     * Computes the Euclidean distance of the given feature vector to the vector
     * for the given ID.
     *
     * @param vec a feature vector
     * @param docID the id of the document to which we want to compute the
     * distance.  If there is no data stored for this document,
     * <code>Double.POSITIVE_INFINITY</code>
     * is returned.
     *
     */
    public double euclideanDistance(double[] vec, int docID) {
        int p = idToFeat[docID];
        if(p == 0) {
            return Double.POSITIVE_INFINITY;
        }
        
        double d = 0;
        for(int i = 0; i < vec.length; i++) {
            double x = vec[i] - features[p++];
            d += x * x;
        }
        return Math.sqrt(d);
    }
    
    /**
     * Computes the Euclidean distance from the given document to all other
     * documents.
     *
     * @param docID the document.
     * @return an array of double, indexed by document ID.  If there is no data
     * associated with the document that we were given, <code>null</code> is
     * returned.  If a document does
     * not have data associated with it, the value for that document will be
     * <code>Double.POSITIVE_INFINITY</code>
     */
    public double[] euclideanDistance(int docID) {
        double[] ret = new double[idToFeat.length];
        
        //
        // Get the feature vector for the document ID that we were passed.
        double[] x = (double[]) getSavedData(docID, false);
        
        if(x == null) {
            return null;
        }
        
        //
        // Loop through the id to features array, looking for non-zero positions.
        // When we find one, calculate a distance.
        for(int i = 0, p = 0; i < idToFeat.length; i++) {
            int curr = idToFeat[i];
            if(curr == 0) {
                ret[i] = Double.POSITIVE_INFINITY;
                continue;
            }
            
            double d = 0;
            for(int j = 0; j < x.length; j++) {
                double diff = x[j] - features[p++];
                d += diff * diff;
            }
            ret[i] = Math.sqrt(d);
        }
        
        return ret;
    }
    
    /**
     * Computes the Euclidean distance from the given document to all other
     * documents.
     *
     * @param vec the feature vector to which we're going to compute similarity.
     * @return an array of double, indexed by document ID.  If a document does
     * not have data associated with it, the value for that document will be
     * <code>Double.POSITIVE_INFINITY</code>
     */
    public double[] euclideanDistance(double[] vec) {
        double[] ret = new double[idToFeat.length];
        
        //
        // Loop through the id to features array, looking for non-zero positions.
        // When we find one, calculate a distance.
        for(int i = 1, p = 1; i < idToFeat.length; i++) {
            if(idToFeat[i] == 0) {
                ret[i] = Double.POSITIVE_INFINITY;
                continue;
            }
            
            double d = 0;
            for(int j = 0; j < vec.length; j++) {
                double diff = vec[j] - features[p++];
                d += diff * diff;
            }
            ret[i] = Math.sqrt(d);
        }
        
        return ret;
    }
    
    public void merge(String path, SavedField[] fields, int maxID, int[] starts,
            int[] nUndel,
            int[][] docIDMaps, RandomAccessFile dictFile,
            PostingsOutput postOut) throws IOException {
        
        //
        // Create the files to which we'll write our data.  We'll base the
        // name on the path and the name of our thread.
        String tname = Thread.currentThread().getName();
        
        File idFile = new File(path + File.separator + tname + ".idf");
        RandomAccessFile idRAF = new RandomAccessFile(idFile, "rw");
        FileWriteableBuffer ids = new FileWriteableBuffer(idRAF, 32768);
        
        File featFile = new File(path + File.separator + tname + ".feat");
        RandomAccessFile featRAF = new RandomAccessFile(featFile, "rw");
        FileWriteableBuffer feats = new FileWriteableBuffer(featRAF, 32768);
        
        //
        // The offset that needs to be applied to the positions in the merge.
        int mpos = 0;
        int mmaxID = 0;
        int mwidth = 0;
        
        //
        // We need to encode an initial 0 in each of our "arrays".  After that,
        // we'll encode from 1 in each of the merging vectors.
        feats.byteEncode(Double.doubleToRawLongBits(0), 8);
        
        //
        // Merge the fields, skipping null ones.
        for(int i = 0; i < fields.length; i++) {
            if(fields[i] == null) {
                continue;
            }
            
            FeatureVector v = (FeatureVector) fields[i];
            int[] idMap = docIDMaps[i];
            mwidth = v.width;
            
            //
            // If there are no deleted documents, then just write things out.
            if(idMap == null) {
                for(int j = 1; j < v.idToFeat.length; j++) {
                    int p = v.idToFeat[j];
                    if(p != 0) {
                        p += mpos;
                    }
                    ids.byteEncode(p, 4);
                }
                
                for(int j = 1; j < v.features.length; j++) {
                    feats.byteEncode(Double.doubleToRawLongBits(v.features[j]), 8);
                }
                mmaxID += v.idToFeat.length - 1;
                mpos += v.pos-1;
            } else {
                //
                // We need to deal with deleted documents.
                for(int j = 1; j < v.idToFeat.length; j++) {
                    if(idMap[j] >= 0) {
                        ids.byteEncode(mpos, 4);
                        for(int k = 0, p = v.idToFeat[j]; k < v.width; k++, p++) {
                            feats.byteEncode(Double.doubleToRawLongBits(v.features[p]), 8);
                        }
                        mpos += v.width;
                        mmaxID++;
                    }
                }
            }
        }
        
        mpos++;
        
        //
        // Write the header.
        dictFile.writeInt(mwidth);
        dictFile.writeInt(mpos);
        dictFile.writeInt(mmaxID);
        
        //
        // Transfer to the dictionary channel.
        FileChannel dictChan = dictFile.getChannel();
        ids.write(dictChan);
        feats.write(dictChan);
    }
    
    /**
     * Gets the distance between two feature vectors stored in different
     * partitions.
     *
     * @param id1 the id of the document containing the vector in this partition
     * @param v the saved field holding the vector for the other partition
     * @param id2 the id of the document containing the vector in the other partition
     * @return the distance between the feature vectors, or <code>Double.POSITIVE_INFINITY</code>
     * if either of the vector is undefined for the given IDs.
     */
    public double distance(int id1, FeatureVector v, int id2) {
        int p1 = idToFeat[id1];
        int p2 = v.idToFeat[id2];
        if(p1 == 0 || p2 == 0) {
            return Double.POSITIVE_INFINITY;
        }
        
        double dist = 0;
        for(int i = 0; i < width; i++) {
            double diff = features[p1++] - v.features[p2++];
            dist += diff * diff;
        }
        return Math.sqrt(dist);
    }
    
    public double distance(int d1, int d2) {
        int p1 = idToFeat[d1];
        int p2 = idToFeat[d2];
        double dist = 0;
        for(int i = 0; i < width; i++) {
            double diff = features[p1++] - features[p2++];
            dist += diff * diff;
        }
        return Math.sqrt(dist);
    }
    
}
