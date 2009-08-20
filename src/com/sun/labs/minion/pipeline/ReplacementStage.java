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

import com.sun.labs.util.props.ConfigString;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * A stage that replaces some words with others.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class ReplacementStage extends StageAdapter implements Configurable {

    private Map<String, String> replace;

    static Logger logger = Logger.getLogger(ReplacementStage.class.getName());

    protected static String logTag = "RS";

    /**
     * Creates a ReplacementStage
     */
    public ReplacementStage() {
        replace = new HashMap<String, String>();
    }

    /**
     * Processes a token from further up the pipeline.
     *
     * @param t The token to process.
     */
    public void token(Token t) {
        String r = replace.get(t.getToken());
        if(r != null) {
            t.setToken(r);
        }
        downstream.token(t);
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        String replacementFile = ps.getString(PROP_REPLACEMENT_FILE);
        if(replacementFile != null) {
            try {
                FileReader reader = new FileReader(replacementFile);
                BufferedReader buffReader = new BufferedReader(reader);
                for(String curr = buffReader.readLine(); curr != null; curr =
                                buffReader.readLine()) {
                    StringTokenizer tok = new StringTokenizer(curr);
                    replace.put(tok.nextToken(), tok.nextToken());
                }
                reader.close();
            } catch(Exception stopE) {
                logger.warning("Error reading stop words: " +
                        stopE.getMessage());
            }
        }

    }
    @ConfigString
    public static final String PROP_REPLACEMENT_FILE = "replacement_file";

}
