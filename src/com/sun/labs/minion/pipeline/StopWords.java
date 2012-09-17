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
package com.sun.labs.minion.pipeline;

import com.sun.labs.util.props.ConfigStringList;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A configurable set of stop words.
 */
public class StopWords implements Configurable {

    @ConfigStringList(mandatory = false)
    public static final String PROP_STOPWORDS_FILES = "stopwords_files";

    private String name;

    private Set<String> stopwords;

    static final Logger logger = Logger.getLogger(StopWords.class.getName());

    public StopWords() {
        stopwords = new HashSet<String>();
    }

    public void addFile(String stopwordsFile) {
        BufferedReader reader = null;
        try {
            InputStream is = getClass().getResourceAsStream(stopwordsFile);
            if(is != null) {
                reader = new BufferedReader(new InputStreamReader(is));
            } else {
                reader = new BufferedReader(new FileReader(stopwordsFile));
            }
            for(String curr = reader.readLine();
                    curr != null; curr = reader.readLine()) {
                curr = curr.trim();
                if(!curr.isEmpty() && curr.charAt(0) != '#') {
                    stopwords.add(curr.toLowerCase());
                }
            }
        } catch(Exception stopE) {
            logger.warning("Error reading stop words: " + stopE.getMessage());
        } finally {
            try {
                if(reader != null) {
                    reader.close();
                }
            } catch(IOException ex) {
            }
        }

    }

    public boolean isStop(String s) {
        return stopwords.contains(s);
    }

    public int size() {
        return stopwords.size();
    }

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        List<String> swf = ps.getStringList(PROP_STOPWORDS_FILES);
        for(String stopwordsFile : swf) {
            addFile(stopwordsFile);
        }

    }

    public String getName() {
        return name;
    }
}
