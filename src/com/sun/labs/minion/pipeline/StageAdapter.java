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

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.Pipeline;
import com.sun.labs.minion.indexer.partition.InvFileMemoryPartition;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;


/**
 * An adapter class for the stage interface, for those who don't want to
 * bother with implementing methods that they don't care about.  The
 * implementation of each method simply passes down the data to the
 * downstream stage.
 *
 * @see com.sun.labs.minion.pipeline.Token
 * 
 */
public class StageAdapter implements Stage {
    
    protected Pipeline pipeline;

    /**
     * Our configuration name.
     */
    protected String name;
    
    /**
     * Our downstream stage.
     */
    protected Stage downstream;

    /**
     * Void constructor just in case.
     */
    public StageAdapter() {
    }

    /**
     * Construct a Stage with a downstream stage to which it can pass
     * output.
     *
     * @param d The stage to which we will pass data.
     */
    public StageAdapter(Stage d) {
        downstream = d;
    }

    @Override
    public void setDownstream(Stage s) {
        downstream = s;
    }

    @Override
    public Stage getDownstream() {
        return downstream;
    }

    @Override
    public void token(Token t) {
        if(downstream == null) return;
        downstream.token(t);
    }

    @Override
    public void punctuation(Token p) {
        if(downstream == null) return;
        downstream.punctuation(p);
    }

    @Override
    public void process(String text) {
        if (downstream == null) {
            return;
        }
        downstream.process(text);
    }
    
    @Override
    public void reset() {
        if(downstream != null) {
            downstream.reset();
        }
    }

    @Override
    public int getLastWordPosition() {
        if(downstream != null) {
            return downstream.getLastWordPosition();
        }
        return 0;
    }

    @Override
    public void setNextWordPosition(int wordPosition) {
        if(downstream != null) {
            downstream.setNextWordPosition(wordPosition);
        }
    }

    @Override
    public Pipeline getPipeline() {
        return pipeline;
    }

    @Override
    public void setPipeline(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public void addField(String field, Object value) {
        ((InvFileMemoryPartition) pipeline.getField().getPartition()).addField(field, value);
    }

    @Override
    public void addField(FieldInfo field, Object value) {
        ((InvFileMemoryPartition) pipeline.getField().getPartition()).addField(field, value);
    }

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        name = ps.getInstanceName();
    }

    @Override
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

} // StageAdapter
