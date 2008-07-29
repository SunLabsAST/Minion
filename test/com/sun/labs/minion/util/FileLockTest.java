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

import com.sun.labs.minion.util.FileLockException;
import com.sun.labs.minion.util.FileLock;
import com.sun.labs.util.LabsLogFormatter;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * A test for file locks.
 */
public class FileLockTest {

    File tmpDir;
    
    File lockFile;

    Logger log;

    public FileLockTest() {
        String td = System.getProperty("java.io.tmpdir");
        if(td == null) {
            td = "/tmp";
        }
        tmpDir = new File(td);
        lockFile = new File(tmpDir, "foo");

        for(Handler h : Logger.getLogger("").getHandlers()) {
            h.setFormatter(new LabsLogFormatter());
        }

        log = Logger.getLogger("com.sun.labs.minion.util.FileLockTest");

    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
        lockFile.delete();
        (new File(lockFile.toString() + ".lock")).delete();
    }

    /**
     * Tests a simple acquire and release.
     */
    @Test
    public void simple() throws IOException, FileLockException {
        FileLock instance = new FileLock(lockFile);
        instance.acquireLock();
        instance.releaseLock();
    }

    /**
     * Tests whether hasLock works with a lock that is acquired.
     */
    @Test
    public void hasLockAfterAcquire() throws IOException, FileLockException {
        FileLock instance = new FileLock(lockFile);
        instance.acquireLock();
        assertTrue(instance.hasLock());
        instance.releaseLock();
    }

    /**
     * Tests whether hasLock works when a lock is not acquired.
     */
    @Test
    public void notHasLockBeforeAcquire() throws IOException, FileLockException {
        FileLock instance = new FileLock(lockFile);
        assertFalse(instance.hasLock());
        instance.acquireLock();
        instance.releaseLock();
    }

    /**
     * Tests whether hasLock is false after a lock has been released.
     */
    /**
     * Tests whether hasLock works when a lock is not acquired.
     */
    @Test
    public void notHasLockAfterRelease() throws IOException, FileLockException {
        FileLock instance = new FileLock(lockFile);
        instance.acquireLock();
        instance.releaseLock();
        assertFalse(instance.hasLock());
    }

    /**
     * Tests multiple acquires, which should be idempotent.
     */
    @Test
    public void multipleAcquire() throws IOException, FileLockException {
        FileLock instance = new FileLock(lockFile);
        instance.acquireLock();
        instance.acquireLock();
        instance.releaseLock();
    }

    /**
     * Tests multiple releases, which should be idempotent.
     */
    @Test
    public void multipleRelease() throws IOException, FileLockException {
        FileLock instance = new FileLock(lockFile);
        instance.acquireLock();
        instance.releaseLock();
        instance.releaseLock();
    }

    /**
     * Tests whether an acquire for a locked file will fail.  This requires us
     * to use a separate thread and try the locking there.
     */
    @Test(expected = FileLockException.class)
    public void failMultipleAcquire() throws IOException, FileLockException {
        FileLock instance = new FileLock(lockFile, 0, TimeUnit.SECONDS);
        instance.acquireLock();

        TestThread tt = new TestThread(instance, 1);

        Thread t = new Thread(tt);
        t.start();
        try {
            t.join();
        } catch(InterruptedException ie) {

        }
        instance.releaseLock();
        if(tt.e != null && tt.e instanceof FileLockException) {
            throw ((FileLockException) tt.e);
        }
    }

    /**
     * Tests two-threaded acquire and release of the lock.
     */
    @Test
     public void twoThreadAcquireAndRelease() throws IOException, FileLockException {
        FileLock instance = new FileLock(lockFile);
        multiThreading(instance, 2, 1);
    }

    /**
     * Tests two-threaded acquire and release of the lock multiple times.
     */
    @Test
     public void twoThreadMultiAcquireAndRelease() throws IOException, FileLockException {
        FileLock instance = new FileLock(lockFile, 30, TimeUnit.SECONDS);
        multiThreading(instance, 2, 5);
    }

    /**
     * Tests five-threaded acquire and release of the lock.
     */
    @Test
     public void fiveThreadAcquireAndRelease() throws IOException, FileLockException {
        FileLock instance = new FileLock(lockFile);
        multiThreading(instance, 5, 1);
    }

    /**
     * Tests five-threaded acquire and release of the lock multiple times
     */
    @Test
     public void fiveThreadMultiAcquireAndRelease() throws IOException, FileLockException {
        FileLock instance = new FileLock(lockFile);
        multiThreading(instance, 5, 10);
    }

    /**
     * Tests ten-threaded acquire and release of the lock multiple times
     */
    @Test
     public void tenThreadMultiAcquireAndRelease() throws IOException, FileLockException {
        FileLock instance = new FileLock(lockFile, 2500, TimeUnit.MILLISECONDS);
        multiThreading(instance, 10, 20);
    }

    /**
     * Tests multithreading under a number of conditions
     * @param lock the lock we can contend on
     * @param nThreads the number of threads that will contend for the lock
     * @param nIter the number of acquire/release iterations to run
     * @throws java.io.IOException if there is an error locking the file
     * @throws com.sun.labs.minion.util.FileLockException if there is an error locking the file
     */
    private void multiThreading(final FileLock lock, int nThreads, final int nIter) throws IOException, FileLockException {
        TestThread[] tt = new TestThread[nThreads];
        Thread[] lt = new Thread[nThreads];
        log.info("multiThreading " + nThreads + " " + nIter);

        for(int i = 0; i < lt.length; i++) {
            tt[i] = new TestThread(lock, nIter);
            lt[i] = new Thread(tt[i]);
            lt[i].setName("TestThread-" + (i+1));
            lt[i].start();
        }

        //
        // Join the threads.
        for(int i = 0; i < lt.length; i++) {
            try {
                lt[i].join();
                if(tt[i].e != null) {
                    fail("Failure: " + lt[i].getName() + " " + tt[i].e);
                }
            } catch(InterruptedException ie) {

            }
        }
    }

    public class TestThread implements Runnable {

        FileLock lock;

        int nIter;

        Exception e;

        public TestThread(FileLock lock, int nIter) {
            this.lock = lock;
            this.nIter = nIter;
        }

        public void run() {
            Logger log = Logger.getLogger(FileLock.class.getName());
            String tn = Thread.currentThread().getName();
            for(int i = 0; i < nIter; i++) {
                try {
                    log.info("acquire " + tn);
                    lock.acquireLock();
                    log.info("acquired " + tn);
                } catch(IOException ex) {
                    log.log(Level.SEVERE, "IOE on acquire", ex);
                    this.e = ex;
                } catch(FileLockException ex) {
                    log.log(Level.SEVERE, "FLE on acquire", ex);
                    this.e = ex;
                } finally {
                    if(lock.hasLock()) {
                        try {
                            log.info("release " + tn);
                            lock.releaseLock();
                            log.info("released " + tn);
                        } catch(FileLockException ex) {
                            log.log(Level.SEVERE, tn + " FLE on release", ex);
                            this.e = ex;
                        }
                    }
                }
            }
        }

    }
}
