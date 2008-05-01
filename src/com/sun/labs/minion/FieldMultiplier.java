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

import com.sun.labs.util.props.ConfigDouble;
import com.sun.labs.util.props.ConfigString;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;

/**
     * Inner class to encapsulate a field multiplier so it can be used by the
     * configuration management mechanism.
     */
public class FieldMultiplier implements Configurable {

    private String name;

    private float value;

    @ConfigString(mandatory = true)
    public static final String PROP_MULTIPLIER_NAME =
            "multiplier_name";

    @ConfigDouble(mandatory = true, defaultValue = 1)
    public static final String PROP_MULTIPLIER_VALUE =
            "multiplier_value";

    public String getName() {
        return name;
    }

    public float getValue() {
        return value;
    }

    public void newProperties(PropertySheet ps)
            throws PropertyException {
        name = ps.getString(PROP_MULTIPLIER_NAME);
        value = ps.getFloat(PROP_MULTIPLIER_VALUE);
    }
}
