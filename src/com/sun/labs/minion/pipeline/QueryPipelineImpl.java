/*
 * Copyright 2011 Oracle and/or its affiliates. All Rights Reserved.
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
 */
package com.sun.labs.minion.pipeline;

import com.sun.labs.minion.QueryPipeline;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.util.props.PropertyException;
import java.util.List;

/**
 * The implementation of a pipeline that will be used to transform queries
 * before they are evaluated.  Any terms or phrases in the query will be
 * passed into this pipeline and the results collected at the end, replacing
 * the original terms or phrases.  This could be used, for example, to applying
 * stemming to all terms that are part of a query if the index is stemmed.
 * The last stage of this pipeline must always be a TokenCollectorStage.
 */
public class QueryPipelineImpl extends PipelineImpl implements QueryPipeline {

    private TokenCollectorStage tcs;
    
    public QueryPipelineImpl(PipelineFactory factory, SearchEngine engine,
            List<Stage> pipeline) {
        super(pipeline);
        Stage s = pipeline.get(pipeline.size() - 1);
        if (!(s instanceof TokenCollectorStage)) {
            throw new PropertyException("", "",
                    "Last stage of QueryPipeline must be TokenCollectorStage");
        }
        tcs = (TokenCollectorStage) s;
    }
    
    /**
     * Takes the tokens that were collected by the TCS and return their strings.
     * This method will empty out the collector after fetching the current
     * tokens.
     * 
     * @return String versions of collected Tokens
     */
    @Override
    public String[] getTokens() {
        Token[] tokens = tcs.getTokens();
        String[] terms = new String[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            terms[i] = tokens[i].getToken();
        }
        tcs.reset();
        return terms;
    }
}
