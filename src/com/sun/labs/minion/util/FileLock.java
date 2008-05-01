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
public class FileLock implements Cloneable {

    MinionLog log = MinionLog.getLog();

    public static final String logTag = "FL";

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
        this(dir, f, 30, 150);
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
        this(null, f, 30, 150);
    }

    /**
     * Makes a lock that will sleep for the given amount of time between
     * the given number of attempts at locking.  This <em><b>does not</b></em> lock
     * the file.  Use the <code>acquireLock</code> method for that.
     *
     * @param f The <code>File</code> that we want to lock.
     * @param nR The number of times to try acquiring the lock when the
     * file is already locked.
     * @param millis The number of milliseconds to sleep between retries of
     * the lock.
     */
    public FileLock(File f, int nR, int millis) {
        this(null, f, nR, millis);
    }

    /**
     * Makes an instance of the class.  This <em><b>does not</b></em> lock
     * the file.  Use the <code>acquireLock</code> method for that.
     *
     * @param dir The directory where the lock file should be put. If this
     * is non-null, we will use the given directory for the lock file.  If
     * this is null, the abstract pathname given in f will be used.
     * @param f The <code>File</code> that we want to lock.
     * @param nR The number of times to try acquiring the lock when the
     * file is already locked.
     * @param millis The number of milliseconds to sleep between retries of
     * the lock.
     */
    public FileLock(File dir, File f, int nR, int millis) {
        lockFile = new File(dir, f.getName() + ".lock");
        nRetries = nR;
        sleepMillis = millis;
        lockState = new HashMap<Thread, LockState>();
    }

    /**
     * Creates a lock that's a copy of the given lock, except the number of
     * retries and the sleep time may differ.  The two locks will share the 
     * lock state so that multiple threads will not be able to hold concurrent
     * locks, but single threads will.
     * @param l the lock that we want to copy
     * @param nRetries the number of times to try to acquire the lock
     * @param sleepMillis the amount of time (in milliseconds) to sleep between
     * attempts to get the lock.
     */
    public FileLock(FileLock l, int nRetries, int sleepMillis) {
        this.lockFile = l.lockFile;
        this.nRetries = nRetries;
        this.sleepMillis = sleepMillis;
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

            //
            // Initialize the lock state.
            state = new LockState(new RandomAccessFile(lockFile, "rw"));
            int n = 0;
            while(n < nRetries) {

                //
                // Try for the lock.  We check for the size of the lock state to 
                // be zero because we need to account for locks held by another thread.
                if(lockState.size() == 0 && state.tryLock()) {
                    break;
                }

                n++;

                //
                // No luck locking the file, wait for our specified time (if
                // that time is greater than 0!) and try again.
                if(nRetries > 1 && sleepMillis > 0) {
                    try {
                        lockState.wait(sleepMillis);
                    } catch(InterruptedException ie) {
                    //
                        // If we get interrupted, we'll just try again.
                    }
                }
            }

            //
            // If we have the lock, then write the thread name
            // into the lock file so that we can debug locking problems.
            if(state.hasLock() && n < nRetries) {
                state.mark();
                lockState.put(ct, state);
            } else {

                //  
                // We didn't succeed.  Try to clean up before we leave.
                state.close();
                throw new FileLockException("Unable to acquire lock: " +
                        lockFile);
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
                    throw new FileLockException("Unable to release lock: " + state.lock, ex);
                }
                try {
                    state.openChannel.close();
                } catch(IOException ex) {
                    throw new FileLockException("Unable to close lock file channel: " + lockFile, ex);
                }
                try {
                    state.openFile.close();
                } catch(IOException ex) {
                    throw new FileLockException("Unable to close lock file: " + lockFile, ex);
                }

                //
                // Get rid of our state.
                lockState.remove(ct);

                //
                // We'll try to delete the lock file, but if it doesn't go, it's OK.
                lockFile.delete();


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

    /**
     * The lock file for the file that we want to lock.
     */
    private File lockFile;

    /**
     * The number of retries before giving up on locking.
     */
    protected int nRetries;

    /**
     * The number of milliseconds to sleep between locking retries.
     */
    protected int sleepMillis;

    /**
     * The thread local data for the state of the locks.
     */
    protected Map<Thread, LockState> lockState;

} // FileLock
