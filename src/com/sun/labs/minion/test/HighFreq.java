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

import com.sun.labs.minion.IndexConfig;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import com.sun.labs.util.props.ConfigurationManager;
import com.sun.labs.util.props.ConfigurationManagerUtils;
import com.sun.labs.util.props.PropertyException;

import com.sun.labs.minion.indexer.entry.Entry;

import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.partition.DiskPartition;

import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.util.Getopt;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A main program that selects high frequency terms from an index.
 */
public class HighFreq {

    Map counts;

    private static IndexConfig indexConfig;

    public HighFreq(SearchEngine engine, boolean total) {
        counts = new HashMap();
        PartitionManager manager = ((SearchEngineImpl) engine).getManager();

        for(Iterator i = manager.getActivePartitions().iterator(); i.hasNext();) {
            DiskPartition p = (DiskPartition) i.next();
            for(Iterator d = p.getMainDictionaryIterator(); d.hasNext();) {
                Entry e = (Entry) d.next();
                String t = (String) e.getName();

                //
                // We only want all lower case terms.
                if(!t.equals(t.toLowerCase())) {
                    continue;
                }

                Count c = (Count) counts.get(t);
                if(c == null) {
                    c = new Count(t);
                    counts.put(e.getName(), c);
                }
                if(total) {
                    c.add(e.getTotalOccurrences());
                } else {
                    c.add(e.getN());
                }
            }
        }
    }

    public List select(int n) {
        PriorityQueue<Count> h = new PriorityQueue<Count>();
        for(Iterator i = counts.values().iterator(); i.hasNext();) {
            Count c = (Count) i.next();
            if(h.size() < n) {
                h.offer(c);
            } else {
                if((h.peek()).compareTo(c) < 0) {
                    h.poll();
                    h.offer(c);
                }
            }
        }

        List<Count> ret = new ArrayList<Count>();

        while(h.size() > 0) {
            ret.add(h.poll());
        }
        Collections.reverse(ret);
        return ret;
    }

    /**
     * Load the configuration from the file.
     * @param cmFile the filename of the XML file containing my configuration
     */
    public static void loadConfig(String cmFile) {
        ConfigurationManager cm;
        try {
            URL url = new File(cmFile).toURI().toURL();
            cm = new ConfigurationManager(url);
            indexConfig = (IndexConfig) cm.lookup("indexConfig");
        } catch(IOException ioe) {
            System.err.println("I/O error during initialization: \n   " + ioe);
            return;
        } catch(PropertyException e) {
            System.err.println("Error during initialization: \n  " + e);
            return;
        }
        if(indexConfig == null) {
            System.err.println("Can't find indexConfig in " + cmFile);
            return;
        }
        ConfigurationManagerUtils.toXML(cm);
    }

    class Count implements Comparable {

        public String name;

        public long count;

        public Count(String name) {
            this.name = name;
        }

        public void add(long c) {
            count += c;
        }

        public int compareTo(Object o) {
            return (int) (count - ((Count) o).count);
        }

        public String toString() {
            return name + " " + count;
        }
    }

    public static void usage() {
        System.out.println(
                "Usage: java HighFreq [options]\n" +
                " -d <index dir>   " +
                "Directory containing index (Required)\n" +
                " -n <num>         " +
                "The number of terms to show\n" +
                " -t               " +
                "Use total frequency, rather than doc frequency.");
        return;
    }

    public static void main(String[] args) throws Exception {

        if(args.length == 0) {
            usage();
            return;
        }

        String logTag = "Indexer";
        String flags = "d:n:tx:";
        Getopt gopt = new Getopt(args, flags);
        String indexDir = null;
        String cmFile = null;
        boolean total = false;
        int n = 1000;
        int c;


        //
        // Set up the logging for the search engine.  We'll send everything
        // to the standard output, except for errors, which will go to
        // standard error.  We'll set the level at 3, which is pretty
        // verbose.
        Logger logger = Logger.getLogger(HighFreq.class.getName());

        //
        // Handle the options.
        while((c = gopt.getopt()) != -1) {
            switch(c) {

                case 'd':
                    indexDir = gopt.optArg;
                    break;

                case 'n':
                    n = Integer.parseInt(gopt.optArg);
                    break;

                case 't':
                    total = true;
                    break;
                case 'x':
                    cmFile = gopt.optArg;
                    break;
            }
        }

        if(indexDir == null && cmFile == null) {
            logger.warning("You must specify an index directory or configuration file.");
            usage();
            return;
        }

        //
        // Open our engine for use.  We give it the properties that we read
        // and no query properties.
        SearchEngine engine;
        try {
            engine = SearchEngineFactory.getSearchEngine(cmFile, indexDir);
        } catch(SearchEngineException se) {
            logger.log(Level.SEVERE, "Error opening collection", se);
            return;
        }

        HighFreq hf = new HighFreq(engine, total);

        List l = hf.select(n);

        for(Iterator i = l.iterator(); i.hasNext();) {
            System.out.println(i.next());
        }

        engine.close();
    }
} // HighFreq
