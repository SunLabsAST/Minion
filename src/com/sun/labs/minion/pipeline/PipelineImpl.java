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

import com.sun.labs.minion.Pipeline;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * An abstract implementation of pipeline.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public abstract class PipelineImpl implements Pipeline {

    /**
     * The log.
     */
    static final Logger logger = Logger.getLogger(PipelineImpl.class.getName());

    /**
     * The list of stages making up the pipeline.
     */
    protected List<Stage> pipeline;

    /**
     * Creates a AbstractPipelineImpl
     */
    public PipelineImpl() {
        pipeline = new ArrayList<Stage>();
    }

    public void addStage(Stage s) {
        pipeline.add(s);
    }

    public Stage getHead() {
        if(!pipeline.isEmpty()) {
        return pipeline.get(0);
        }
        return null;
    }

    public void process(String text) {
        if(!pipeline.isEmpty()) {
            pipeline.get(0).process(text);
        }
    }
}
