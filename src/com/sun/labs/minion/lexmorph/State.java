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

package com.sun.labs.minion.lexmorph;

/**
 * this class is the wrapper for the integer (and string name) passed for
 * controlling the next state function to be called.  The states are passed
 * into the StateSwitcher to call the next one.
 * This hand-built file should be included (by concatenation or by importing)
 * in the testATN final java file.
 */
class State {
    public String name;
    public int indexVal;

    /**
     * Undocumented Class Constructor.
     * 
     * 
     * @param stname
     * @param indxV
     * 
     * @see
     */
    State(String stname, int indxV) {
        name = stname;
        indexVal = indxV;
    }

    /**
     * Undocumented Method Declaration.
     * 
     * 
     * @return
     * 
     * @see
     */
    @Override
    public String toString() {
        String pstr = "State:" + name;

        return pstr;
    }

  State transitionFunction(){
    /* a remnant of the Lisp version ...
        and we may do some kind of mapping here some day ...*/
    return this;
  }
}

