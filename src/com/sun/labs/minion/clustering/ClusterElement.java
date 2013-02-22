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

package com.sun.labs.minion.clustering;

import com.sun.labs.minion.Result;

/**
 * A single element in a results cluster.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class ClusterElement {
    
    /**
     * The point in N-space representing this element.
     */
    public double[] point;
    
    /**
     * The actual result that this element represents.
     */
    protected Result r;
    
    /**
     * The cluster that this element is a member of.
     */
    public int member;
    
    /**
     * Creates a ClusterElement
     */
    public ClusterElement(Result r, double[] point) {
        this.r = r;
        this.point = point;
        this.member = -1;
    }
    
    public double[] getPoint() {
        return point;
    }
    
    /**
     * Computes the distance between this cluster element an a given point in
     * the same space.
     */
    public double dist(double[] p) {
        return ClusterUtil.euclideanDistance(point, p);
    }
    
    @Override
    public String toString() {
        return r.getKey();
    }
}
