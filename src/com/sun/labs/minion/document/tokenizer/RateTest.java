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

package com.sun.labs.minion.document.tokenizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.text.DecimalFormat;
import com.sun.labs.minion.pipeline.StageAdapter;

/**
 * Test indexing rate.
 * @author stgreen
 */
public class RateTest {
    
    /** Creates a new instance of RateTest */
    public RateTest() {
    }
    
    /**
     * A format object for formatting the output.
     */
    protected static DecimalFormat form = new DecimalFormat("########0.00");
    
    protected static String toMB(long x) {
        return form.format((float) x / (float) (1024 * 1024));
    }
    
    public static void reportProgress(long start,
            long docsize,
            int n) {
        float secs = (float) (System.currentTimeMillis() - start) / 1000;
        float MB = docsize / (1024 * 1024);
        
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        
        Class c = Class.forName(args[0]);
        Tokenizer tok = (Tokenizer) c.newInstance();
        tok.setDownstream(new StageAdapter());

        BufferedReader fr = new BufferedReader(new FileReader(args[1]));
        String f;
        long tot = 0;
        int n = 0;
        long start = System.currentTimeMillis();
        char[] data = new char[8192 * 2];
        while((f = fr.readLine()) != null) {
            File lf = new File(f);
            tot += lf.length();
            n++;
            Reader lr = new BufferedReader(new FileReader(lf));
            //
            // Read and tokenize the file.
            while(true) {
                int nr = lr.read(data);
                if(nr == -1) {
                    break;
                }
                tok.text(data, 0, nr);
            }
            
            lr.close();
            if(n % 1000 == 0) {
                reportProgress(start, tot, n);
            }
        }        
    }
}
