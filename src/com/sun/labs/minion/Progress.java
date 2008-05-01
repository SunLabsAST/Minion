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
 * An interface for tracking progress
 *
 */
public interface Progress {
    
    /** 
     * Starts a progress session
     * 
     * @param numSteps the total number of steps in the session
     */
    public void start(int numSteps);
    
    /** 
     * Proceed to the next step and specify the name of the step
     * 
     * @param name the name of the step
     */
    public void next(String name);
    
    /** 
     * Proceed to the next step without specifying a name.
     * If a previous name was assigned, it will be unchanged.
     */
    public void next();
    
    /** 
     * Stops a progress session
     */
    public void done();
    
}
