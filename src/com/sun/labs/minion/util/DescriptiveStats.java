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

package com.sun.labs.minion.util;

/**
 * A basic class to provide simple descriptive stats.
 */
public class DescriptiveStats {

    protected int min = Integer.MAX_VALUE;
    protected int max = Integer.MIN_VALUE;
    protected long sum;
    protected int n;
    protected int[] logBins = new int[9];
    protected int nZeros;
    public DescriptiveStats() {
    }
    
    public void add(int v) {
        n++;
        sum += v;
        max = Math.max(max, v);
        min = Math.min(min, v);
        if(v > 0) {
            logBins[Math.min((int) Math.floor(Math.log10(v)), 
                    logBins.length-1)]++;
        } else {
            logBins[0]++;
            nZeros++;
        }
    }
    
    public double mean() {
        return sum / (double) n;
    }
    
    public int min() {
        return min;
    }
    
    public int max() {
        return max;
    }
    
    public int n() {
        return n;
    }
    
    public int[] bins() {
        return logBins;
    }
    
    public int nZeros() {
        return nZeros;
    }
            

}
