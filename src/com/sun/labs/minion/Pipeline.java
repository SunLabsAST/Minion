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

import com.sun.labs.minion.indexer.Field;
import com.sun.labs.minion.pipeline.Stage;

/**
 * A class that encapsulates the machinery of a single indexing pipeline.
 * A pipeline can be used for indexing data or for performing highlighting
 * operations.
 *
 */
public interface Pipeline {
    
    /**
     * Gets the field that this pipeline is used for.
     */
    public Field getField();
    
    /**
     * Sets the field that this pipeline is used for.
     */
    public void setField(Field field);
    
    /**
     * Gets the head of the pipeline.
     *
     * @return the stage at the head of the pipeline.
     */
    public Stage getHead();

    /**
     * Adds a stage to the end of the pipeline.
     *
     * @param s the stage to add
     */
    public void addStage(Stage s);

    /**
     * Processes the given textual data through the pipeline.
     * @param text the text to process.
     */
    public void process(String text);
    
    /**
     * Resets the pipeline for the next batch of text.
     */
    public void reset();

}
