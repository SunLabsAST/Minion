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

package com.sun.labs.minion.test;

import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.Logger;
import com.sun.labs.minion.util.MinionLog;

/**
 *
 * @author stgreen
 */
public class MultiDV implements Callable<List<Result>> {

    SearchEngine engine;

    DocumentVector dv;

    public MultiDV(SearchEngine engine) {
        this.engine = engine;
    }

    public DocumentVector getDV(String key) {
        try {
            return engine.getDocumentVector(key);
        } catch(SearchEngineException see) {
            return null;
        }   
    }

    public void setDV(DocumentVector dv) {
        this.dv = dv;
    }

    public List<Result> call() throws Exception {
        dv.setEngine(engine);
        return dv.findSimilar("-score").getResults(0, 10);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        //
        // Set up the log.
        Logger logger = Logger.getLogger("");
        for(Handler h : logger.getHandlers()) {
            h.setFormatter(new SimpleLabsLogFormatter());
        }

        MinionLog.setLogger(logger);
        MinionLog.setLevel(3);


        MultiDV[] mdvs = new MultiDV[args.length];
        for(int i = 0; i < args.length; i++) {
            mdvs[i] = new MultiDV(SearchEngineFactory.getSearchEngine(args[i]));
        }
        ExecutorService executor = Executors.newCachedThreadPool();

        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        String k;
        while((k = r.readLine()) != null) {
            DocumentVector dv = null;
            for(int i = 0; i < mdvs.length; i++) {
                dv = mdvs[i].getDV(k);
                if(dv != null) {
                    break;
                }
            }

            if(dv == null) {
                logger.info("No document for: " + k);
                continue;
            }
        }

    }
}
