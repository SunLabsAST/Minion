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

package com.sun.labs.minion.test.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/** This class links input and output streams so that data taken from input
 *  stream is transfered to the output stream. This class can be used to 
 *  connect standard input/ouput stream of Java application with
 *  output/input streams of spawned child process, so that all user's input is
 *  redirected to the child and all it's output is visible for the user.<P>
 *
 *  This class starts a thread, which transfers data from input stream to 
 *  output stream until End Of File is reached or IOException caused by IO 
 *  error is catched. 
 */
public class Pipe { 
    /** Default size of buffer used to transfer data from the input 
     *  stream to the output stream.
     */
    static public int defaultBufferSize = 4096;

    /** Establish connection between input and output streams with specified
     *  size of buffer used for data transfer. 
     * 
     * @param in  input stream
     * @param out output stream
     * @param bufferSize size of buffer used to transfer data from the input 
     *  stream to the output stream
     */
    static public void between(InputStream in,OutputStream out,int bufferSize) 
    { 
        (new PipeThread(in, out, bufferSize)).start();
    }

    /** Establish connection between input and output streams with default
     *  buffer size. 
     * 
     * @param in  input stream
     * @param out output stream
     */
    static public void between(InputStream in, OutputStream out) { 
        (new PipeThread(in, out, defaultBufferSize)).start();
    }
}

class PipeThread extends Thread {  
    InputStream  in;
    OutputStream out;
    byte[]       buffer;

    PipeThread(InputStream in, OutputStream out, int bufferSize) { 
        this.in = in;
        this.out = out;
        buffer = new byte[bufferSize];
    }

    public void run() { 
        try { 
            int length;
            while ((length = in.read(buffer)) > 0) { 
                out.write(buffer, 0, length);
                //out.flush();
            }
        } catch(IOException ex) {}
    }
}
	    
