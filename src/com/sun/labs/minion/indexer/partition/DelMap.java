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

import java.io.File;
import java.io.RandomAccessFile;

import com.sun.labs.minion.util.FileLock;

import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.Buffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that holds the two components of a deletion map:  the number
 * of documents deleted and the bitmap of the deleted documents.
 */
public class DelMap implements Cloneable {

    /**
     * The file that contains the deletion map we're managing.
     */
    protected File delFile;

    /**
     * A lock for the file.
     */
    protected FileLock lock;

    /**
     * The number of deleted documents.
     */
    protected int nDeleted;

    /**
     * The bitmap of deleted documents.
     */
    protected Buffer delMap;

    /**
     * Whether the bitmap has been modified since it was read.
     */
    protected boolean dirty;

    /**
     * The log.
     */
    static Logger logger = Logger.getLogger(DelMap.class.getName());

    /**
     * The log tag.
     */
    protected static String logTag = "DM";

    private Partition part;

    /**
     * Creates an empty deletion bitmap.
     */
    public DelMap() {
        delMap = new ArrayBuffer(32);
        nDeleted = 0;
    }

    public void setPartition(Partition part) {
        this.part = part;
    }

    /**
     * Instantiates a <code>DelMap</code> from the given file, which is
     * locked with the given lock.
     *
     * @param delFile The file that the deletion map is read from.
     * @param lock The lock for that file.
     */
    public DelMap(File delFile, FileLock lock) {
        this.delFile = delFile;
        this.lock = lock;
        read();
    }

    /**
     * Reads the deletion bitmap from the file, locking if necessary.  This
     * merely returns the buffer, it does not set the deletion bitmap!
     * @return the buffer containing the deletion bitmap.
     */
    protected synchronized Buffer readBuffer() {

        //
        // If the file doesn't exist, we're done.
        if(!delFile.exists()) {
            return null;
        }

        //
        // Read our deletion bitmap, locking if necessary.
        boolean releaseNeeded = false;
        try {
            if(!lock.hasLock()) {
                lock.acquireLock();
                releaseNeeded = true;
            }
            RandomAccessFile raf = new RandomAccessFile(delFile, "r");
            int size = raf.readInt();
            byte[] b = new byte[size];
            raf.readFully(b);
            Buffer ret = new ArrayBuffer(b);
            ret.position(ret.limit());
            raf.close();
            return ret;
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Error reading delmap: " + delFile, e);
            return null;
        } finally {
            try {
                if(releaseNeeded) {
                    lock.releaseLock();
                }
            } catch(Exception e2) {
            }
        }
    }

    /**
     * Reads the deletion bitmap from the file, and computes the number of
     * deleted documents.
     */
    protected synchronized void read() {

        delMap = readBuffer();
        if(delMap == null) {
            nDeleted = 0;
        } else {
            nDeleted = (int) ((ReadableBuffer) delMap).countBits();
        }
        dirty = false;
    }

    public static void write(File f, WriteableBuffer m)
            throws java.io.IOException {
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        raf.writeInt((int) m.position());
        m.write(raf);
        raf.close();
    }

    /**
     * Writes this map to a file.
     */
    public synchronized void write() {
        write(delFile);
    }

    /**
     * Writes this map to a file.
     * @param delFile the file to which we'll write the deletion map
     */
    public synchronized void write(File delFile) {

        if(!dirty || nDeleted == 0 || delMap == null) {
            return;
        }

        boolean releaseNeeded = false;
        try {
            if(lock != null && !lock.hasLock()) {
                lock.acquireLock();
                releaseNeeded = true;
            }
            write(delFile, (WriteableBuffer) delMap);
            dirty = false;
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Error during write", e);
        } finally {
            if(releaseNeeded) {
                try {
                    lock.releaseLock();
                } catch(Exception e2) {
                }
            }
        }
    }

    /**
     * Writes this map to a buffer
     * @param buff the buffer to which we'll write this map
     */
    public synchronized boolean write(WriteableBuffer buff) {

        if(!dirty || nDeleted == 0 || delMap == null) {
            return false;
        }

        boolean releaseNeeded = false;
        try {
            if(lock != null && !lock.hasLock()) {
                lock.acquireLock();
                releaseNeeded = true;
            }
            
            buff.byteEncode(delMap.position(), 4);
            ((WriteableBuffer) delMap).write(buff);
            dirty = false;
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Error during write", e);
        } finally {
            if(releaseNeeded) {
                try {
                    lock.releaseLock();
                } catch(Exception e2) {
                }
            }
        }
        return true;
    }

    /**
     * Synchronizes the map in memory with the map on disk.
     *
     */
    protected synchronized void sync() {

        boolean releaseNeeded = false;
        try {
            //
            // Lock if we need to.
            if(!lock.hasLock()) {
                lock.acquireLock();
                releaseNeeded = true;
            }

            //
            // If we're the first deletions, then just write the file.
            if(!delFile.exists()) {
                if(dirty) {
                    write();
                    dirty = false;
                    return;
                }

                //
                // If we're not dirty, then we don't have anything
                // anyways.
                return;
            }

            //
            // If the file exists, but we're not dirty, then just read the
            // file.
            if(!dirty) {
                read();
                return;
            }

            //
            // OK, the file exists, and we're dirty, so read the file,
            // combine the maps and write the file.
            Buffer fmap = readBuffer();

            //
            // We need to combine our map and theirs, and write the
            // results back to the file.
            if(fmap != null) {
                ((WriteableBuffer) delMap).or((ReadableBuffer) fmap);
            }
            nDeleted = (int) ((ReadableBuffer) delMap).countBits();
            write();
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Error syncing delmap: " + delFile, e);
        } finally {
            if(releaseNeeded) {
                try {
                    lock.releaseLock();
                } catch(Exception e) {
                }
            }
        }
    }

    /**
     * A helper that atomically syncs the deletion map and returns the buffer.
     * @return the synchronized deletion map
     */
    protected synchronized ReadableBuffer syncGetMap() {
        sync();
        return getDelMap();
    }

    /**
     * Sets the bit indicating tha the given ID has been deleted.
     * @param id the ID of the document to delete
     * @return <code>true</code> if the document was deleted successfully, <code>false</code>
     * otherwise.  Note that we will return <code>false</code> if this ID has 
     * already been deleted in this map.  That is, this method will only return 
     * true the first time that a document is deleted.
     */
    public synchronized boolean delete(int id) {

        if(delMap == null) {
            delMap = new ArrayBuffer(id);
        }

        //
        // Only count each deletion once!
        if(!((ReadableBuffer) delMap).test(id)) {
            ((WriteableBuffer) delMap).set(id);
//            if(part != null && part instanceof DiskPartition) {
//            logger.info(part + " del " + id);
//            }
            nDeleted++;
            dirty = true;
            //
            // If we actually deleted something we'll return true, since 
            // if we don't delete something, we want to make sure that another
            // partition will get a crack at it!
            return true;
        } else {
//            if (part != null && part instanceof DiskPartition) {
//            logger.info(part + " already del " + id);
//            }

        }

        //
        // If the bit for the ID was already set, then return false because we've
        // already deleted it.
        return false;
    }

    /**
     * Tells us whether a document has been deleted.
     * @param id the ID of the document that we want to check
     * @return <code>true</code> if the document has been deleted, <code>false</code>
     * otherwise.
     *
     * This is not synchronized because we know the underlying buffer can get the
     * byte without having to do a read from the filesystem.  We may be opening
     * ourselves to a race condition, but them's the breaks.
     *
     */
    public boolean isDeleted(int id) {
        return delMap != null && ((ReadableBuffer) delMap).test(id);
    }

    /**
     * Clears out the map.
     * 
     */
    public synchronized void clear() {
        ((WriteableBuffer) delMap).clear();
        nDeleted = 0;
    }

    /**
     * Clones this map.
     */
    public synchronized Object clone() {
        DelMap result = null;
        try {
            result = (DelMap) super.clone();
        } catch(CloneNotSupportedException e) {
            throw new InternalError();
        }

        result.nDeleted = nDeleted;
        if(delMap != null) {
            result.delMap = (ArrayBuffer) ((ArrayBuffer) delMap).clone();
        } else {
            result.delMap = null;
        }
        return result;
    }

    /**
     * Gets the number of deleted documents.
     * @return the number of deleted documents
     */
    public int getNDeleted() {
        return nDeleted;
    }

    /**
     * Gets the current deletion map.  A clone of the map is returned, so that
     * the caller can't affect our representation of deleted documents.
     * @return a copy of the current deletion map.
     * @see #syncGetMap
     */
    public ReadableBuffer getDelMap() {
        if(delMap != null) {
            return (ArrayBuffer) ((ArrayBuffer) delMap).clone();
        }
        return null;
    }

    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append(nDeleted + " deleted docs: ");
        for(int i = 1; i < delMap.limit() * 8; i++) {
            if(((ReadableBuffer) delMap).test(i)) {
                b.append(i + " ");
            }
        }
        return b.toString();
    }

    /**
     * A main program that prints out a deletion map.
     */
    public static void main(String[] args) throws Exception {
        File m = new File(args[0]);
        FileLock l = new FileLock(new File(args[0] + ".lock"));
        DelMap dl = new DelMap(m, l);
        System.out.println(dl);
    }
} // DelMap

