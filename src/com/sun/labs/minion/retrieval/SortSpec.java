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
package com.sun.labs.minion.retrieval;

import java.util.StringTokenizer;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.dictionary.DiskDictionaryBundle;
import com.sun.labs.minion.indexer.dictionary.DiskDictionaryBundle.Fetcher;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import java.util.logging.Logger;

/**
 * A class to contain a sorting specification for a results set.  Results
 * can be sorted by any combination of saved fields and the score assigned
 * to the document.
 */
public class SortSpec {

    /**
     * The original specification.
     */
    protected String spec;

    /**
     * The size of the specification, in terms of the number of fields.
     */
    protected int size;

    /**
     * The fields upon which to sort.
     */
    protected FieldInfo[] fields;

    /**
     * Fetchers for any saved fields in the sorting spec.
     */
    protected DiskDictionaryBundle.Fetcher[] fetchers;

    /**
     * The directions in which to sort each of the fields.
     * <code>true</code> indicates that the sort is in an increasing
     * direction, while <code>false</code> indicates that the sort is in a
     * decreasing direction.
     */
    protected boolean[] directions;

    protected static Logger logger = Logger.getLogger(SortSpec.class.getName());

    protected static String logTag = "SSPEC";

    /**
     * Creates a sorting specification from the given string description.
     *
     * @param spec The sorting specification.  This is of the form:
     * <tt>[+|-]fieldName{,[+|-]fieldName}{,[+|-]fieldName}...</tt>
     * <p>
     * A <tt>+</tt> indicates that the values from the named field should
     * be sorted in increasing order, and a <tt>-</tt> indicates that the
     * values from the named field should be sorted in decreasing order.
     * If the direction specifier is not given, <tt>+</tt> will be
     * assumed.  If a field given as part of the sorting specification does not
     * exist or is not a saved field, that field will be ignored for the purposes
     * of sorting.
     * @param manager a partition manager.  This is used to retrieve the field
     * information for each of the named fields.
     */
    public SortSpec(PartitionManager manager, String spec) {
        if(spec != null) {
            this.spec = spec;
        } else {
            this.spec = "";
        }
        StringTokenizer tok = new StringTokenizer(spec, ",");
        size = tok.countTokens();
        fields = new FieldInfo[size];
        directions = new boolean[size];
        for(int i = 0; i < size; i++) {
            String fieldSpec = tok.nextToken();
            String fn;
            char dc = fieldSpec.charAt(0);
            boolean dir;
            if(dc == '+' || dc == '-') {
                fn = fieldSpec.substring(1);
                directions[i] = dc == '+';
            } else {
                fn = fieldSpec;
                directions[i] = true;
            }
            if(fn.equals("score")) {
                fields[i] = new FieldInfo();
            } else {
                fields[i] = manager.getFieldInfo(fn);
                if(fields[i] == null) {
                    logger.warning(String.format("Unknown field in sort spec: %s", fn));
                } else if(!fields[i].hasAttribute(FieldInfo.Attribute.SAVED)) {
                    logger.warning(String.format("Can't sort on field %s that doesn't have SAVED attribute", fn));
                }
            }
        }
    } // SortSpec constructor

    /**
     * Constructs a partition specific sorting specification that includes fetchers
     * for the saved fields appearing in the sorting specification.
     */
    public SortSpec(SortSpec ss, InvFileDiskPartition part) {
        spec = ss.spec;
        size = ss.size;
        fields = (FieldInfo[]) ss.fields.clone();
        directions = (boolean[]) ss.directions.clone();
        fetchers = new Fetcher[size];
        for(int i = 0; i < size; i++) {
            if(fields[i] != null && fields[i].getID() != 0) {
                fetchers[i] = part.getDF(fields[i]).getFetcher();
            }
        }
    }

    public boolean getDirection(int i) {
        return directions[i];
    }

    public String toString() {
        return spec;
    }
} // SortSpec
