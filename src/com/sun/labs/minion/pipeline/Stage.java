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

/**
 * An interface for any objects that wish to participate in an indexing
 * pipeline.  The methods in the class mirror our interpretation of a
 * document as a set of fields and values.  Any class implementing this
 * interface should provide a way for an instantiator of that class to pass
 * a downstream stage to the instance.
 *
 * <p>
 *
 * Any implementation of this interface should pass along any information
 * that it does not understand or want to deal with to the downstream
 * stage.
 *
 * @see com.sun.labs.minion.pipeline.StageAdapter;
 * 
 */

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.PipelineStage;
import com.sun.labs.util.props.Configurable;


public interface Stage extends Configurable, PipelineStage {

    /**
     * Gets the configuration name of this stage.  This information can be 
     * gotten from the @link{com.sun.labs.util.prop.PropertySheet#getInstanceName} 
     * method from the property sheet that is used to configure the stage in the 
     * <code>newProperties</code> method of the <code>Configurable</code> interface.
     */
    public String getName();
     
    /**
     * Sets the downstream stage of this stage.
     */
    public void setDownstream(Stage s);

    /**
     * Gets the downstream stage of this stage.
     */
    public Stage getDownstream();
    
    /**
     * Processes text, passing the results downstream.
     */
    public void process(String text);
    
    /**
     * Processes a token from further up the pipeline.
     *
     * @param t The token to process.
     */
    public void token(Token t);

    /**
     * Processes some punctuation from further up the pipeline.
     *
     * @param p The punctuation to process.
     */
    public void punctuation(Token p);
    
    /**
     * 
     * Processes a value for another field, so that a given pipeline stage 
     * (e.g., {@link QuestioningStage}) can define the value for a different
     * field.
     * @param field the field to which we want to add the data
     * @param value the value that we want to add
     */
    public void addField(String field, Object value);

    /**
     * 
     * Processes a value for another field, so that a given pipeline stage 
     * (e.g., {@link QuestioningStage}) can define the value for a different
     * field.
     * @param field the field to which we want to add the data
     * @param value the value that we want to add
     */
    public void addField(FieldInfo field, Object value);
} // Stage
