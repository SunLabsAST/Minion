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
 * {@link #text} method is called.  However, if your analzyer determines
 * that another field value is encountered during processing this
 * field, it may use {@link #startField} to effectively push a
 * new current field onto the stack.  Any text passed to the stage
 * will then be considered to be part of the new field.  Calling
 * {@link #endField} will pop the new field off the stack and
 * any further text will be considered to be part of the original
 * field for which the analyzer was invoked.
 *
 * @author Jeff Alexander
 */

public interface PipelineStage
{
    /**
     * Instructs the pipeline to begin collecting data for a different
     * field
     *
     * @param field the object describing the field to start.  The field must
     *              already be defined in the index configuration.
     */
    public void startField(FieldInfo field);
    
    /**
     * Sends some text to be indexed as part of the field.  The text will
     * also be tokenzied if the field's tokenized property is true in the
     * index configuration.  Keep in mind that text queries are tokenized
     * before execution, so even if the text has been partially tokenized
     * by a CustomAnalyzer, it may still be worthwhile to allow it to be
     * tokenized by this method.
     *
     * This method will accumulate all the text that is entered with each
     * call until an endField is called (either by a CustomAnalyzer that
     * called startField or by the Minion Pipeline that called startField
     * before invoking the CustomAnalyzer).  If the field is described
     * as a saved field in the Minion configuration, the accumulated text
     * will also be saved in the field store.
     *
     * @param t The text to store and/or tokenize.
     * @param b The beginning position in the text buffer.
     * @param e The ending position in the text buffer.
     */
    public void text(char[] t, int b, int e);
    
    /**
     * Saves some data verbatim in the field store.  This should only be
     * used if the {@link #text} method is not used.
     *
     * @param sd the data to store
     */
    public void savedData(Object sd);
    
    /**
     * Instructs the pipeline to stop collecting data for a field
     *
     * @param field the object describing the field that is ending
     */
    public void endField(FieldInfo field);

}
