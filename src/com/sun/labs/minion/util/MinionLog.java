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

import com.sun.labs.util.props.ConfigString;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A general purpose logging facility for the indexing and query engine.
 * There are three kinds of messages that can be logged:
 *
 * <dl>
 * <dt>Log messages</dt><dd>These are logged using the <code>log</code>
 * method.
 * <dt>Warning messages</dt><dd>These are logged using the
 * <code>warn</code> method.</dd>
 * <dt>Error messages</dt><dd>These are logged using the <code>error</code>
 * method</dd>
 * <dt>Debug messages</dt><dd>These are logged using the <code>debug</code>
 * method</dd>
 * </dl>
 *
 * Each type of message can be specified on it's own stream, if required.
 * If you specify the same stream for more than one type, it will be
 * handled correctly.
 *
 * <p>
 *
 * You can also specify a logging level, and only messages whose level is
 * <em>lower</em> than that level will be printed.
 */
@Deprecated
public class MinionLog implements Configurable {

    /**
     * The property name for the log file.
     */
    @ConfigString(mandatory = false)
    public final static String PROP_LOG_FILE = "log_file";

    /**
     * Protected constructor for the singleton log.
     */
    public MinionLog() {
        instance = this;
    }

    /**
     * Gets the singleton log object.
     */
    public static MinionLog getLog() {
        if(instance == null) {
            instance = new MinionLog();
        }
        return instance;
    }

    /**
     * Gets the log level for a given message type.
     *
     * @param type The type of message we want the level for.
     * @return the log level
     */
    public static int getLogLevel(int type) {
        return logLevels[type];
    }

    /**
     * Gets the log level for a given message type.
     *
     * @param type The type of message we want the level for.
     * @return the log level
     */
    public static int getLevel(int type) {
        return logLevels[type];
    }

    /**
     * Sets the log level for all message types.
     *
     * @param ll The logging level.
     */
    public static void setLevel(int ll) {
        for(int i = 0; i < logLevels.length; i++) {
            logLevels[i] = ll;
        }
    }

    /**
     * Sets the log level for a particular message type.
     *
     * @param type The type of message that we want to set the logging
     * level for.
     * @param ll The logging level.
     */
    public static void setLevel(int type, int ll) {
        logLevels[type] = ll;
    }

    public static void setLogger(Logger logger) {
        jul = logger;
    }

    /**
     * Sets the all the logs to the same stream.
     *
     * @param os The stream that logging data will be sent to.  A null
     * value will disable all logging.
     */
    public static void setStream(OutputStream os) {
        for(int i = 0; i < streams.length; i++) {
            streams[i] = os;
        }
        PrintWriter pw = null;
        if(os != null) {
            try {
                pw = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true);
            } catch(java.io.UnsupportedEncodingException e) {
            }
        } else {
            pw = null;
        }
        for(int i = 0; i < writers.length; i++) {
            writers[i] = pw;
        }
    }

    /**
     * Sets the all the logs to the same writer.
     *
     * @param pw The printwriter that logging data will be sent to.  A null
     * value will disable all logging.
     */
    public static void setStream(PrintWriter pw) {
        for(int i = 0; i < streams.length; i++) {
            streams[i] = null;
        }
        for(int i = 0; i < writers.length; i++) {
            writers[i] = pw;
        }
    }

    /**
     * Sets one of the streams.
     *
     * @param s The stream to set.
     * @param os The stream that logging data will be sent to.  A null
     * value will disable logging for that message type.
     */
    public static void setStream(int s, OutputStream os) {

        //
        // A null stream means "turn off logging for this type."
        if(os == null) {
            writers[s] = null;
            return;
        }

        //
        // Check if we already know about this stream.
        for(int i = 0; i < 4; i++) {
            if(i != s && os == streams[i]) {
                writers[s] = writers[i];
                return;
            }
        }

        //
        // We don't, so make a new writer.
        streams[s] = os;
        try {
            writers[s] = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true);
        } catch(java.io.UnsupportedEncodingException e) {
        }
    }

    /**
     * Checks the logging level and then writes a message if appropriate.
     *
     * @param module The module that is reporting
     * @param type The type of message
     * @param level The level of the current message
     * @param label The label to be pre-pended to the message.
     * @param msg The message to send to the log.
     */
    private static void log_label(String module, int type, int level,
                                  String label, String msg, Throwable t) {
        if(level <= logLevels[type]) {
            if(writers[type] != null) {
                writers[type].println("[" + df.format(new Date()) + "] " + label +
                                      module + " " + msg);
                writers[type].flush();
                if(t != null) {
                    t.printStackTrace(writers[type]);
                }
            } else {
                if(jul != null) {
                    switch(type) {
                    case DEBUG:
                    case LOG:
                        jul.log(Level.INFO, module + " " + msg, t);
                        break;
                    case WARN:
                        jul.log(Level.WARNING, module + " " + msg, t);
                        break;
                    case ERROR:
                        jul.log(Level.SEVERE, module + " " + msg, t);
                        break;
                    }
                }
            }
        }

    }

    /**
     * Logs a log message to the log stream.
     *
     * @param module The module that is reporting
     * @param level The level of the current message
     * @param msg The message to send to the log.
     */
    public static void log(String module, int level, String msg) {
        log_label(module, LOG, level, "", msg, null);
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
        log_label(module, LOG, level, "", msg, t);
    }

    /**
     * Logs a warning message.
     *
     * @param module The module that is reporting
     * @param level The level of the current message
     * @param msg The message to send to the log.
     */
    public static void warn(String module, int level, String msg) {
        log_label(module, WARN, level, "WARNING: ", msg, null);
    }

    /**
     * Logs a warning message.
     *
     * @param module The module that is reporting
     * @param level The level of the current message
     * @param msg The message to send to the log.
     * @param t A throwable that we can print a stack trace for.
     */
    public static void warn(String module, int level, String msg,
                            Throwable t) {
        log_label(module, WARN, level, "WARNING: ", msg, t);
    }

    /**
     * Logs an error message.
     *
     * @param module The module that is reporting
     * @param level The level of the current message
     * @param msg The message to send to the log.
     */
    public static void error(String module, int level, String msg) {
        log_label(module, ERROR, level, "ERROR: ", msg, null);
    }

    /**
     * Logs an error message.
     *
     * @param module The module that is reporting
     * @param level The level of the current message
     * @param msg The message to send to the log.
     * @param t A throwable that we can print a stack trace for.
     */
    public static void error(String module, int level, String msg,
                             Throwable t) {
        log_label(module, ERROR, level, "ERROR: ", msg, t);
    }

    /**
     * Logs a debug message.
     *
     * @param module The module that is reporting
     * @param level The level of the current message
     * @param msg The message to send to the log.
     */
    public static void debug(String module, int level, String msg) {
        log_label(module, DEBUG, level, "DEBUG: ", msg, null);
    }

    /**
     * Logs a debug message.
     *
     * @param module The module that is reporting
     * @param level The level of the current message
     * @param msg The message to send to the log.
     * @param t A throwable that we can print a stack trace for.
     */
    public static void debug(String module, int level, String msg,
                             Throwable t) {
        log_label(module, DEBUG, level, "DEBUG: ", msg, t);
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        String lf = ps.getString(PROP_LOG_FILE);
        if(lf != null) {
            try {
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(lf));
                setStream(out);
            } catch(FileNotFoundException ex) {
            //
                // Can't log this...
            }
        }
    }

    /**
     * The static instance for our Singleton.
     */
    protected static MinionLog instance = null;

    private static Logger jul;
    /**
     * Format for log messages.
     */

    static DateFormat df = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss");

    /**
     * The logging levels.
     */
    protected static int[] logLevels = new int[4];

    /**
     * The output streams.
     */
    protected static OutputStream[] streams = new OutputStream[4];

    /**
     * The <code>PrintWriter</code>s associated with the streams.
     */
    protected static PrintWriter[] writers = new PrintWriter[4];

    /**
     * Static constant for log messages.
     */
    public static final int LOG = 0;

    /**
     * Static constant for warning messages.
     */
    public static final int WARN = 1;

    /**
     * Static constant for error messages.
     */
    public static final int ERROR = 2;

    /**
     * Static constant for debug messages.
     */
    public static final int DEBUG = 3;

} // MinionLog
