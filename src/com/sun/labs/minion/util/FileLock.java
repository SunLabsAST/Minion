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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
    private long timeout;

    /**
     * The thread local data for the state of the locks.
     */
    protected Map<Thread, LockState> lockState;

    /**
     * Makes a lock that will retry the default number of
     * times (30) and sleep the default amount of time between retries (150
     * milliseconds).  This <em><b>does not</b></em> lock the file.  Use
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
     * Makes a lock that will retry the default number of
     * times (30) and sleep the default amount of time between retries (150
     * milliseconds).  This <em><b>does not</b></em> lock the file.  Use
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
        this.timeout = units.toMillis(timeout);
        lockState = new HashMap<Thread, LockState>();
    }

    /**
     * Creates a lock that's a copy of the given lock, except the timeout
     * may differ.  The two locks will share the 
     * lock state so that multiple threads will not be able to hold concurrent
     * locks, but single threads will.
     * @param l the lock that we want to copy
     * attempts to get the lock.
     * @param timeout the timeout to use when trying to acquire the lock
     * @param units the units for the timeout.  This will be converted to milliseconds, 
     * so beware of truncation!
     */
    public FileLock(FileLock l, long timeout, TimeUnit units) {
        this.lockFile = l.lockFile;
        this.timeout = units.toMillis(timeout);
        this.lockState = l.lockState;
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

        Thread ct = Thread.currentThread();

        synchronized(lockState) {

            //
            // Determine whether we have the lock already.
            LockState state = lockState.get(ct);

            if(state != null) {

                //
                // We already have the lock.
                return;
            }

            state = new LockState(new RandomAccessFile(lockFile, "rw"));

            //
            // A timeout of zero means we try once and punt if we don't get it.
            if(timeout == 0) {
                if(lockState.isEmpty() && state.tryLock()) {
                    state.mark();
                    lockState.put(ct, state);
                    return;
                } else {
                    state.close();
                    throw new FileLockException("Unable to acquire lock: " +
                            lockFile);
                }
            }

            //
            // When's the latest time that we'll try?
            long startTime = System.currentTimeMillis();
            long lastTime = startTime + timeout;

            //
            // Initialize the lock state.
            while(System.currentTimeMillis() <= lastTime) {

                //
                // Try for the lock.  We check for the size of the lock state to 
                // be zero because we need to account for locks held by another thread.
                if(lockState.isEmpty() && state.tryLock()) {
                    break;
                }

                try {
                    lockState.wait(250);
                } catch(InterruptedException ie) {
                }
            }

            //
            // If we have the lock, then write the thread name
            // into the lock file so that we can debug locking problems.
            if(state.hasLock()) {
                state.mark();
                lockState.put(ct, state);
            } else {

                //  
                // We didn't succeed.  Try to clean up before we leave.
                state.close();
                throw new FileLockException(String.format("Unable to acquire lock %s", lockFile));
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

        Thread ct = Thread.currentThread();

        synchronized(lockState) {
            LockState state = lockState.get(ct);
            if(state != null) {
                try {
                    state.lock.release();
                } catch(IOException ex) {
                    throw new FileLockException("Unable to release lock: " +
                            state.lock, ex);
                }
                try {
                    state.close();
                } catch(IOException ex) {
                    throw new FileLockException(String.format("Unable to close lock: %s",lockFile), ex);
                }

                //
                // Get rid of our state.
                lockState.remove(ct);

                //
                // We'll try to delete the lock file, but if it doesn't go, it's OK.
                if(!lockFile.delete()) {
                    if(logger.isLoggable(Level.FINE)) {
                        logger.fine(String.format("Couldn't delete lock file %s (this is OK)", lockFile.getName()));
                    }
                }


                //
                // Wake up the waiters.
                lockState.notifyAll();
                return;
            }

        //
        // If we try to release a lock that we don't hold, then just return
        // quietly.
        }
    }

    /**
     * Trades the lock from one thread to another.
     * @param owner The owner of the lock.
     * @param taker The thread taking the locking.
     * @throws com.sun.labs.minion.util.FileLockException If there is an error trading the lock.
     */
    public void tradeLock(Thread owner, Thread taker)
            throws FileLockException {
        synchronized(lockState) {
            LockState state = lockState.get(owner);
            if(state == null) {
                throw new FileLockException("Unable to trade lock: " +
                        lockFile + " not held by " + owner);
            }
            lockState.remove(owner);
            lockState.put(taker, state);
        }
    }

    /**
     * Tells us whether the given thread has a lock on the file.
     * @param t The thread.
     * @return <CODE>true</CODE> if the given thread has the lock, <CODE>false</CODE> otherwise.
     */
    public boolean hasLock(Thread t) {
        synchronized(lockState) {
            return lockState.get(t) != null;
        }
    }

    /**
     * Tells us whether we currently hold a lock on the file.
     * @return <CODE>true</CODE> if the current thread holds the lock, <CODE>false</CODE>
     * otherwise.
     */
    public boolean hasLock() {
        return hasLock(Thread.currentThread());
    }

    /**
     * Gets a string describing the lock.
     * @return A string describing the lock.
     */
    public String toString() {
        return lockState.toString();
    }

    private class LockState {

        public LockState(RandomAccessFile openFile) {
            this.openFile = openFile;
            this.openChannel = openFile.getChannel();
        }

        /**
         * Try to get the lock embodied by this state.
         * 
         * @return <code>true</code> if we got the lock, <code>false</code> 
         * otherwise.
         */
        public boolean tryLock() throws IOException {
            try {
                lock = openChannel.tryLock();
            } catch(java.nio.channels.OverlappingFileLockException ole) {
                return false;
            }
            return lock != null;
        }

        public boolean hasLock() {
            return lock != null;
        }

        /**
         * Marks the lock file with the thread that owns it.
         */
        private void mark() throws IOException {
            openFile.writeUTF(Thread.currentThread().getName() + "-" +
                    System.currentTimeMillis());
        }

        /**
         * Closes the file and channel for this lock.
         */
        private void close() throws IOException {
            openChannel.close();
            openFile.close();
        }
        
        RandomAccessFile openFile;

        FileChannel openChannel;

        java.nio.channels.FileLock lock;

    }
} // FileLock
