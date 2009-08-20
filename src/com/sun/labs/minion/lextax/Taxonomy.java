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
package com.sun.labs.minion.lextax;

import java.util.logging.Logger;

/**
 * An abstract class that holds the information common to both Memory and
 * Disk taxonomies.
 */
public abstract class Taxonomy {

    //
    // The relations between concepts. 
    public static final int MORPH_OF = 0;

    public static final int INV_MORPH_OF = 1;

    public static final int KIND_OF = 2;

    public static final int INV_KIND_OF = 3;

    public static final int INSTANCE_OF = 4;

    public static final int INV_INSTANCE_OF = 5;

    public static final int SENSE_OF = 6;

    public static final int INV_SENSE_OF = 7;

    public static final int ENTAILS = 8;

    public static final int INV_ENTAILS = 9;

    public static final int VARIANT_OF = 10;

    public static final int INV_VARIANT_OF = 11;

    public static final int ABBREVIATION_OF = 12;

    public static final int INV_ABBREVIATION_OF = 13;

    public static final int MISSPELLING_OF = 14;

    public static final int INV_MISSPELLING_OF = 15;

    public static final int NICKNAME_OF = 16;

    public static final int INV_NICKNAME_OF = 17;

    /**
     * The number of links and link types.
     */
    public static final int NLINKS = 18;

    public static final int NLINKTYPES = NLINKS / 2;

    public static final int PARENTS = 0;

    public static final int CHILDREN = 1;

    /**
     * the characters used in the hourglass display to show
     * link type.  These are a String so that longer sequences could
     * be used if desired.
     */
    public static final String[] LINKCHARS = {"m", "k", "i", "s",
        "e", "v", "a", "x", "n"};

    /**
     * The ID for the root of the taxonomy.
     */
    protected static final int TOP = 0;

    /**
     * The name of the top concept.
     */
    public static final String TOP_NAME = "_TOP_";

    /**
     * The log to which we write messages.
     */
    static Logger logger = Logger.getLogger(Taxonomy.class.getName());

} // Taxonomy
