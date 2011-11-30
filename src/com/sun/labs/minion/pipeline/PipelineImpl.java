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
import com.sun.labs.minion.indexer.Field;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * An implementation of pipeline.
 */
public class PipelineImpl implements Pipeline {

    static final Logger logger = Logger.getLogger(PipelineImpl.class.getName());

    /**
     * The list of stages making up the pipeline.
     */
    private List<Stage> stages;

    private Stage head;

    private Field field;

    /**
     * Creates a AbstractPipelineImpl
     */
    public PipelineImpl(List<Stage> stages) {
        this.stages = stages;
        if(stages != null && !stages.isEmpty()) {
            head = stages.get(0);
        }
        for(Stage s : stages) {
            s.setPipeline(this);
        }
    }

    public void addStage(Stage s) {
        if(stages == null) {
            stages = new ArrayList<Stage>();
        }

        if(!stages.isEmpty()) {
            Stage last = stages.get(stages.size() - 1);
            last.setDownstream(s);
        }
        
        s.setPipeline(this);

        stages.add(s);
        head = stages.get(0);
    }

    public Stage getHead() {
        return head;
    }

    public void process(String text) {
        if(head != null) {
            head.process(text);
        }
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }
}
