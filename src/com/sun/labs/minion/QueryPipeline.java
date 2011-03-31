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
package com.sun.labs.minion;

/**
 * A pipeline that will be used to transform queries before they are
 * evaluated.  Any terms or phrases in the query will be passed into this
 * pipeline and the results collected at the end, replacing the original
 * terms or phrases.  This could be used, for example, to apply stemming
 * to all terms that are part of a query if the index is stemmed.
 * 
 * The last stage of this pipeline must always be a TokenCollectorStage.
 */
public interface QueryPipeline extends Pipeline {
    /**
     * Gets the tokens collected after having passed a term or phrase
     * through the pipeline.
     * 
     * @return an array of processed tokens
     */
    public String[] getTokens();
}
