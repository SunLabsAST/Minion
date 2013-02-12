package com.sun.labs.minion.indexer.partition;

import com.sun.labs.minion.util.FileLock;
import com.sun.labs.minion.util.FileLockException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A container for the active file, which is a file containing the numbers of the
 * currently active partitions in a given engine.
 */
public class ActiveFile {

    private static final Logger logger = Logger.getLogger(ActiveFile.class.getName());

    private File activeFile;

    private FileLock activeLock;

    public ActiveFile(File indexDir, File lockDir, String type) {
        activeFile = new File(indexDir, "AL." + type);
        activeLock = new FileLock(lockDir, activeFile, 300, TimeUnit.SECONDS);
        activeLock.setName("active");
    }

    public void lock() throws IOException, FileLockException {
        activeLock.acquireLock();
    }
    
    public boolean isLocked() {
        return activeLock.hasLock();
    }

    public void unlock() throws FileLockException {
        activeLock.releaseLock();
    }

    public long lastModified() {
        return activeFile.lastModified();
    }

    public boolean setLastModified(long time) {
        return activeFile.setLastModified(time);
    }

    /**
     * Reads the numbers of the active partitions from the active file.
     *
     * @throws java.io.IOException if there are any errors reading the
     * active file.
     * @throws FileLockException if there is any error
     * locking or unlocking the active file.
     * @return a list of the numbers of the active
     * partitions.
     */
    public List<Integer> read() throws java.io.IOException,
            FileLockException {

        boolean releaseNeeded = false;
        List<Integer> ret =
                new ArrayList<Integer>();

        if(!activeLock.hasLock()) {
            activeLock.acquireLock();
            releaseNeeded = true;
        }

        RandomAccessFile active;
        int nParts = 0;

        //
        // Handle the case of a non-existent active file.
        if(!activeFile.exists()) {
            try {
                active = new RandomAccessFile(activeFile, "rw");
                active.writeInt(nParts);
                active.close();
                if(logger.isLoggable(Level.FINEST)) {
                    logger.finest(String.format("Created active file: %s", activeFile));
                }
                return ret;
            } finally {
                if(releaseNeeded) {
                    activeLock.releaseLock();
                }
            }
        }

        //
        // We know we need to read data.
        //
        // Open the file containing the data.
        active = new RandomAccessFile(activeFile, "r");

        //
        // Read the number of partitions.
        try {
            nParts = active.readInt();
        } catch(java.io.IOException ioe) {
            logger.severe("Error reading number of " +
                    "active partitions.");
            active.close();
            if(releaseNeeded) {
                activeLock.releaseLock();
            }
            throw ioe;
        }

        //
        // Read the partition numbers, then close the file.
        try {
            for(int i = 0; i < nParts; i++) {
                ret.add(active.readInt());
            }
        } catch(java.io.IOException ioe) {
            logger.severe("Error reading partition numbers.");
            active.close();
            if(releaseNeeded) {
                activeLock.releaseLock();
            }
            throw ioe;
        }
        active.close();

        if(logger.isLoggable(Level.FINEST)) {
            logger.finest(String.format("Read AL: %s", ret));
        }

        //
        // If we locked it just for reading, then we can release the lock
        // now.
        if(releaseNeeded) {
            activeLock.releaseLock();
        }

        return ret;
    }
    
    /**
     * Writes a list of partition numbers to the active file.  Locks the
     * active file if necessary.
     *
     * @param parts The list of partitions or partition numbers to write to
     * the active file.
     * @throws java.io.IOException if there is an error writing the file.
     * @throws FileLockException if there is any error
     * locking the active file.
     */
    public void write(Collection<DiskPartition> parts) throws
            java.io.IOException,
            FileLockException {

        boolean releaseNeeded = false;
        if(!activeLock.hasLock()) {
            activeLock.acquireLock();
            releaseNeeded = true;
        }

        if(logger.isLoggable(Level.FINEST)) {
            logger.finer(String.format("Writing AL: %s", parts));
        }

        RandomAccessFile active;

        //
        // We want to write partition numbers in order.
        List<Integer> sort = getPartitionNumbers(parts);

        try {
            //
            // Open the file containing the data.
            active = new RandomAccessFile(activeFile, "rw");

            //
            // Write the list.
            active.writeInt(parts.size());
            for(Integer pn : sort) {
                active.writeInt(pn);
            }
            active.setLength(active.getFilePointer());
            active.close();
        } catch(java.io.IOException ioe) {
            logger.severe("Error writing active file");
            throw ioe;
        } finally {
            if(releaseNeeded) {
                activeLock.releaseLock();
            }
        }
    }

    /**
     * Gets a list of the partition numbers for the given partitions.  This can
     * be used when we want to act on collections of partition numbers, rather
     * than the partitions themselves.
     *
     * @param parts the partitions for which we want the numbers
     * @return an list of the active partition numbers, in increasing order.
     */
    public static List<Integer> getPartitionNumbers(Collection<DiskPartition> parts) {
        List<Integer> ret = new ArrayList<Integer>();
        for(DiskPartition dp : parts) {
            ret.add(dp.getPartitionNumber());
        }
        Collections.sort(ret);
        return ret;
    }

    @Override
    public String toString() {
        return activeFile.toString();
    }
}
