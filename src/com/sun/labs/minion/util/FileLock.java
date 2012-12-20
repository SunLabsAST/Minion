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
package com.sun.labs.minion.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a class that locks a file so that only a single thread or
 * process may access it.  Note that all the classes have to try to get the
 * lock, this is simply a synchronization tool and will not work if some
 * threads or processes do not first try to acquire the lock.
 * 
 * <p>
 * 
 * The locking is done using Java NIO file locks.  Such locks are VM-wide, so 
 * we need to be careful to ensure that the locks are also thread safe.  To that
 * end, the class stores a map of threads to the locks that they hold.  Threads
 * within a VM will synchronize on this map when acquiring, releasing or testing
 * locks.
 * 
 * <p>
 * 
 * This, unfortunately, requires that multiple instances of this class that want
 * to lock the same file <em>must</em> share the same map, otherwise the desired
 * locking behavior will not work across the threads using the different instances.
 * To that end, a copy constructor is provided that ensures that two locks for
 * the same file share the same map, even if the number of retries and the sleep
 * periods are different.
 */
public class FileLock {

    private static final Logger logger = Logger.getLogger(FileLock.class.getName());

    /**
     * The lock file for the file that we want to lock.
     */
    private File lockFile;

    /**
     * The timeout for the lock.
     */
    private long defaultTimeout;

    /**
     * A lock for in-process exclusion.
     */
    private ReentrantLock lock = new ReentrantLock(true);

    /**
     * The file we want to lock, as a file.
     */
    private RandomAccessFile openFile;
    
    /**
     * The channel underlying the file.
     */
    private FileChannel openChannel;
    
    /**
     * The inter-process filesystem lock.
     */
    private java.nio.channels.FileLock fileLock;


    /**
     * Makes a lock that will use the default timeout of 5 seconds.
     * This <em><b>does not</b></em> lock the file.  Use
     * the <code>acquireLock</code> method for that.
     *
     * @param dir The directory where the lock should reside.
     * @param f The file that we want to lock.
     * @see #acquireLock
     */
    public FileLock(File dir, File f) {
        this(dir, f, 5, TimeUnit.SECONDS);
    }

    /**
     * Makes a lock that will use the default timeout of 5 seconds.
     * This <em><b>does not</b></em> lock the file.  Use
     * the <code>acquireLock</code> method for that.
     *
     * @param f The <code>File</code> that we want to lock.
     * @see #acquireLock
     */
    public FileLock(File f) {
        this(null, f, 5, TimeUnit.SECONDS);
    }

    /**
     * Creates a lock for a file in the current directory.
     * @param f the name of the file to lock
     * @param timeout the timeout to use when trying to acquire the lock
     * @param units the units for the timeout.  This will be converted to milliseconds, 
     * so beware of truncation!
     */
    public FileLock(File f, long timeout, TimeUnit units) {
        this(null, f, timeout, units);
    }

    /**
     * Creates a lock for a file in a given directory.  
     * @param dir the directory where the lock file should be created.  If this
     * is <code>null</code>, the current working directory will be used
     * @param f the name of the file to lock
     * @param timeout the timeout to use when trying to acquire the lock
     * @param units the units for the timeout.  This will be converted to milliseconds, 
     * so beware of truncation!
     */
    public FileLock(File dir, File f, long timeout, TimeUnit units) {
        lockFile = new File(dir, f.getName() + ".lock");
        this.defaultTimeout = units.toMillis(timeout);
    }

    /**
     * Acquires the lock on the file.  This code will try repeatedly to
     * acquire the lock.
     *
     * @throws FileLockException when the lock cannot be acquired within
     * the specified parameters.
     * @throws java.io.IOException if there is an I/O error while obtaining
     * the lock
     */
    public void acquireLock()
            throws java.io.IOException, FileLockException {
        acquireLock(defaultTimeout);
    }
    
    public void acquireLock(long timeout, TimeUnit units) 
            throws java.io.IOException, FileLockException {
        acquireLock(units.toMillis(timeout));
    }
    

    public void acquireLock(long timeout)
            throws java.io.IOException, FileLockException {
        
        //
        // See whether we can acquire this lock within the process.
        try {
            if(!lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
                throw new FileLockException(String.format("Timed out waiting for lock on %s", lockFile));
            }
        } catch (InterruptedException ex) {
            throw new FileLockException(String.format("Interrupted waiting for %s", lockFile), ex);
        }

        //
        // Try to get the lock on the actual file, which will insulate us from
        // other processes trying to access it.
        boolean locked = false;
        try {
            openFile = new RandomAccessFile(lockFile, "rw");
            openChannel = openFile.getChannel();
            fileLock = openChannel.tryLock();
            locked = fileLock != null;
            if(!locked) {
                throw new FileLockException(String.format("Unable to acquire interprocess lock on %s", lockFile));
            }
        } catch (OverlappingFileLockException ex) {
            throw new FileLockException(String.format("Tried to get lock with other thread waiting: %s", lockFile), ex);
        } catch (IOException ex) {
            throw new FileLockException(String.format("Error locking: %s", lockFile), ex);
        } finally {
            //
            // If we didn't get the interprocess lock, then release the 
            // intraprocess lock.
            if(!locked) {
                lock.unlock();
            }
        }
    }

    /**
     * Release the lock on the file, if the calling thread currently holds
     * it.  This is accomplished by deleting our lock file.
     *
     * @throws FileLockException when the lock cannot be released.
     */
    public void releaseLock() throws FileLockException {

        if(!lock.isHeldByCurrentThread()) {
            throw new FileLockException(String.format("Can't release lock not held on %s", lockFile));
        }
        
        if(fileLock != null) {
            try {
                fileLock.release();
            } catch (IOException ex) {
                throw new FileLockException(String.format("Error unlocking %s", lockFile));
            }
            try {
                openChannel.close();
                openFile.close();
            } catch (IOException ex) {
                throw new FileLockException(String.format("Error cleaning up for %s", lockFile), ex);
            }
        }
    }


    /**
     * Tells us whether we currently hold a lock on the file.
     * @return <CODE>true</CODE> if the current thread holds the lock, <CODE>false</CODE>
     * otherwise.
     */
    public boolean hasLock() {
        return lock.isHeldByCurrentThread();
    }

    /**
     * Gets a string describing the lock.
     * @return A string describing the lock.
     */
    @Override
    public String toString() {
        return lock.toString() + " " + fileLock.toString();
    }

} // FileLock
