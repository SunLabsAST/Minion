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
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import com.sun.labs.minion.pipeline.TokenCollectorStage;
import com.sun.labs.minion.util.MinionLog;

/**
 *
 * @author stgreen
 */
public class Test {
    
    Tokenizer tok;
    
    char[] buff;
    
    /**
     * Runs a tokenizer against a given list of files.
     */
    public Test(Tokenizer tok) {
        this.tok = tok;
        buff = new char[32 * 1024];
    }
    
    public void tokenize(Reader r, String key) throws java.io.IOException {
        int p = 0;
        tok.startDocument(key);
        while(true) {
            int n = r.read(buff);
            if(n == -1) {
                tok.endDocument(0);
                break;
            }
            tok.text(buff, 0, n);
            p += n;
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        
        MinionLog.setStream(System.out);
        MinionLog.setLevel(4);
        
        TokenCollectorStage tcs1 = new TokenCollectorStage();
//        PrintStage ps1 = new PrintStage(tcs1, true);
        Tokenizer tok1 = new UniversalTokenizer(tcs1);
        Test t1 = new Test(tok1);
        
        TokenCollectorStage tcs2 = new TokenCollectorStage();
//        PrintStage ps2 = new PrintStage(tcs2, true);
        Tokenizer tok2 = new JCCTokenizer(tcs2);
        Test t2 = new Test(tok2);

        BufferedReader fr = new BufferedReader(new FileReader(args[0]));
        String f;
        while((f = fr.readLine()) != null) {
            File lf = new File(f);
            Reader lr = new InputStreamReader(new FileInputStream(lf), args[1]);
            t1.tokenize(lr, f);
            lr.close();
            lr = new InputStreamReader(new FileInputStream(lf), args[1]);
            t2.tokenize(lr, f);
            lr.close();
//            MinionLog.log("TEST", 0, "Universal Tokenizer");
//            MinionLog.log("TEST", 0, "\n" + t1.tok.getDownstream().toString());
//            MinionLog.log("TEST", 0, "JCC Tokenizer");
//            MinionLog.log("TEST", 0, "\n" + t2.tok.getDownstream().toString());
            if(!t1.tok.getDownstream().equals(t2.tok.getDownstream())) {
                System.err.println("Error tokenizing: " + f);
            }
        }
    }
}
