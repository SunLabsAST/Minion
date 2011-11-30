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

package com.sun.labs.minion;

/**
 * The interface to a stage in the indexing pipeline.  When using a
 * CustomAnalyzer for indexing, these methods are available to
 * control the construction of a document.  Generally speaking,
 * your analyzer is called once for each field and the pipeline is
 * ready to receive the text contained in that field when the
 * {@link #text} method is called.  However, if your analyzer determines
 * that another field value is encountered during processing this
 * field, it may use {@link #startField} to effectively push a
 * new current field onto the stack.  Any text passed to the stage
 * will then be considered to be part of the new field.  Calling
 * {@link #endField} will pop the new field off the stack and
 * any further text will be considered to be part of the original
 * field for which the analyzer was invoked.
 *
 */

public interface PipelineStage {
    
    /**
     * Sets the pipeline of which this stage is a member.
     * @param pipeline the pipeline of which this stage is a member.
     */
    public void setPipeline(Pipeline pipeline);
    
    /**
     * Gets the pipeline of which this stage is a member.
     * 
     * @return 
     */
    public Pipeline getPipeline();
    
    /**
     * Send some text to be processed by the pipeline.
     * @param text
     */
    public void process(String text);
}
