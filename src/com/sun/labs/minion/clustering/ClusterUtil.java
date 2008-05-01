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

import java.util.Random;

/**
 * A set of utility methods for clustering operations.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class ClusterUtil {
    
    /**
     * Adds p2 to p1, returning p1.
     */ 
    public static double[] add(double[] p1, double[] p2) {
        for(int i = 0; i < p1.length; i++) {
            p1[i] += p2[i];
        }
        return p1;
    }
    
    public static double[] div(double[] p, double d) {
        for(int i = 0; i < p.length; i++) {
            p[i] /= d;
        }
        return p;
    }
    
    public static double[] randomPoint(int d) {
        double[] ret = new double[d];
        Random rand = new Random();
        for(int i = 0; i < ret.length; i++) {
            ret[i] = rand.nextDouble();
        }
        return ret;
    }
    
    public static double[] normalizedRandomPoint(int d) {
        return normalize(randomPoint(d));
    }
    
    public static double[] normalize(double[] p) {
        double sum = 0;
        for(int i = 0; i < p.length; i++) {
            sum += p[i] * p[i];
        }
        sum = Math.sqrt(sum);
        for(int i = 0; i < p.length; i++) {
            p[i] /= sum;
        }
        return p;
    }
    
    /**
     * Computes the Euclidean distance between two points.
     */
    public static double euclideanDistance(double[] p1, double[] p2) {
        double sum = 0;
        for(int i = 0; i < p1.length; i++) {
            double diff = p1[i] - p2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
    
    public static String toString(double[] p) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(int i = 0; i < p.length; i++) {
            if(i > 0) {
                sb.append(", ");
            }
            sb.append(p[i]);
        }
        sb.append("]");
        return sb.toString();
    }
    
    public static boolean equal(double[] p1, double[] p2) {
        for(int i = 0; i < p1.length; i++) {
            if(p1[i] != p2[i]) {
                return false;
            }
        }
        return true;
    }

    protected void finalize() throws Throwable {
    }
}
