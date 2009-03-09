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
 * An interface for clases that would like the opportunity to modify document
 * scores at the time that they are being considered for inclusion into a set
 * of results returned from the search engine.
 * 
 */
public interface ScoreModifier {
    /**
     * Modifies the provided score, possibly taking into account the data about
     * the current document using the provided result accessor.
     * 
     * <p>
     * 
     * Note that an implementation of this method may be called thousands or 
     * hundreds of thousands of times when deciding whether to add a document
     * to a set of results!
     * 
     * @param score the current score for the document under consideration
     * @param ra a result accessor that can provide information about the 
     * document currently under consideration
     * @return a modified score that will be used instead of the score computed 
     * by the search engine.
     */
    public float modifyScore(float score, ResultAccessor ra);
}
