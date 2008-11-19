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

import java.io.Serializable;

/**
 * A nanosecond stop watch.
 */
public class NanoWatch extends StopWatch implements Serializable {

    /**
     * Starts the timer.
     */
    public void start() {
        lastStart = System.nanoTime();
    }

    /**
     * Stops the timer.
     */
    public void stop() {
        clicks++;
        lastTime = System.nanoTime() - lastStart;
        time += lastTime;
    }

    /**
     * Gets the time for this nano stop watch in milliseconds.
     * @return the accumulated time for this stop watch in milliseconds.
     */
    public double getTimeMillis() {
        return getTime() / 1000000.0;
    }

    public double getLastTimeMillis() {
        return lastTime / 1000000.0;
    }

    public double getAvgTimeMillis() {
        return getTimeMillis() / (double) clicks;
    }
}
