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

package com.sun.labs.minion.classification;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * The result of a classification operation for a particular classifier.
 */
public class ClassificationResult {

    /**
     * The name of the field into which these results should be placed.
     */
    private String field;
    
    private int maxID;
    
    /**
     * A list of individual classification results.
     */
    private List<DocResult> results;
    
    public ClassificationResult(String field) {
        this.field = field;
        results = new ArrayList<DocResult>();
    }
    
    public void add(int id, float score, String val) {
        maxID = Math.max(id, maxID);
        results.add(new DocResult(id, score, val));
    }
    
    public String getField() {
        return field;
    }
    
    public int getMaxID() {
        return maxID;
    }
    
    public List<DocResult> getResults() {
        return results;
    }
    
    public class DocResult {
        private int id;
        private float score;
        private String val;
        
        protected DocResult(int id, float score, String val) {
            this.id = id;
            this.score = score;
            this.val = val;
        }
        
        public int getID() {
            return id;
        }
        
        public float getScore() {
            return score;
        }
        
        public String getValue() {
            return val;
        }
    }
}
