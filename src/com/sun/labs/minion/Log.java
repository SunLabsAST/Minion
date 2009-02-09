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

package com.sun.labs.minion;

import com.sun.labs.minion.util.MinionLog;

/**
 * A class that logs information for the search engine.
 */
@Deprecated
public class Log extends MinionLog {

    /**
     * Builds a log.
     */
    protected Log() {
        MinionLog.getLog();
    }

    /**
     * Sets the log level for all message types.
     *
     * @param ll The logging level.
     */
    public static void setLevel(int ll) {
        instance.setLevel(ll);
    }

    /**
     * Sets the log level for all message types.
     * @param type What kind of message is this? Such as warning, debug, log.
     * @param ll The logging level.
     */
    public static void setLevel(int type, int ll) {
        instance.setLevel(type, ll);
    }

    /**
     * Sets the all the logs to the same stream.
     *
     * @param os The stream that logging data will be sent to.  A null
     * value will disable all logging.
     */
    public static void setStream(java.io.OutputStream os) {
        instance.setStream(os);
    }
    
    /**
     * Sets the all the logs to the same writer.
     *
     * @param pw The writer that logging data will be sent to.  A null
     * value will disable all logging.
     */
    public static void setStream(java.io.PrintWriter pw) {
        instance.setStream(pw);
    }
    
    /**
     * Logs a log message to the log stream.
     *
     * @param module The module that is reporting
     * @param level The level of the current message
     * @param msg The message to send to the log.
     */
    public static void log(String module, int level, String msg) {
        instance.log(module, level, msg);
    }
    
    /**
     * Logs a log message to the log stream.
     *
     * @param module The module that is reporting
     * @param level The level of the current message
     * @param msg The message to send to the log.
     * @param t A throwable that we can print a stack trace for.
     */
    public static void log(String module, int level, String msg,
                           Throwable t) {
        instance.log(module, level, msg, t);
    }
    

    /**
     * Gets a log that can be used to log a variety of messages to a
     * stream.
     * @return A log
     */
    public static Log getLog() {
        return new Log();
    }

    /**
     * Override.
     */
    public static final int DEBUG = MinionLog.DEBUG;
} // Log
