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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import com.sun.labs.minion.util.StopWatch;

/**
 * A hierarchical agglomerative clusterer.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class HAC extends AbstractClusterer {
    
    public static final String logTag = "HAC";
    
    /**
     * Creates a HAC
     */
    public HAC() {
        super();
    }
    
    public void cluster() {
        
        StopWatch sw = new StopWatch();
        sw.start();
        
        //
        // Make the initial set of nodes to cluster.
        Node[] nodes = new Node[els.length];
        Set<Node> currNodes = new LinkedHashSet<Node>(nodes.length);
        for(int i = 0; i < nodes.length; i++) {
            nodes[i] = new Node(els[i]);
            currNodes.add(nodes[i]);
        }
        
        //
        // Queue up the distances between the nodes.
        PriorityQueue<Dist> q = new PriorityQueue<Dist>(nodes.length);
        for(int i = 0; i < nodes.length; i++) {
            for(int j = i+1; j < nodes.length; j++) {
                q.offer(new Dist(nodes[i], nodes[j]));
            }
        }
        
        //
        // Loop until we run out of distances or until we have reached the
        // right number of clusters.
        while(q.size() > 0 && currNodes.size() > k) {
            
            //
            // Get the smallest distance.
            Dist d = q.poll();
            
            //
            // Check if this distance has been outdated by an earlier
            // combination of one of its nodes.
            if(!currNodes.contains(d.n1) ||
                    !currNodes.contains(d.n2)) {
                continue;
            }
            
            //
            // Combine the nodes for this distance, remove them from consideration
            // compute new distances to the new node and then add the new node to
            // our set for consideration.
            Node n = new Node(d.n1, d.n2);
            currNodes.remove(d.n1);
            currNodes.remove(d.n2);
            for(Node x : currNodes) {
                q.offer(new Dist(n, x));
            }
            currNodes.add(n);
        }
        
        //
        // Now number our clusters and set the centroids.
        k = currNodes.size();
        clusters = new double[k][N];
        int member = 0;
        for(Node n : currNodes) {
            for(ClusterElement el : n.l) {
                el.member = member;
                ClusterUtil.add(clusters[member], el.point);
            }
            ClusterUtil.div(clusters[member], n.l.size());
            member++;
        }
        
        sw.stop();
        logger.info("clustering took: " + sw.getTime());
    }
    
    class Node {
        
        List<ClusterElement> l;
        
        public Node(ClusterElement el) {
            l = new ArrayList<ClusterElement>();
            l.add(el);
        }
        
        public Node(Node n1, Node n2) {
            l = new ArrayList<ClusterElement>();
            l.addAll(n1.l);
            l.addAll(n2.l);
        }
        
        public String toString() {
            return l.toString();
        }
    }
    
    class Dist implements Comparable<Dist> {
        Node n1;
        Node n2;
        double dist;
        public Dist(Node n1, Node n2) {
            this.n1 = n1;
            this.n2 = n2;
            dist = computeDistance();
        }
        
        /**
         * Computes the distance between two nodes.  The distance is the maximum
         * of all pairwise distances, which gives us a complete link algorithm.
         */
        public double computeDistance() {
            double max = 0;
            for(ClusterElement e1 : n1.l) {
                for(ClusterElement e2 : n2.l) {
                    double d = ClusterUtil.euclideanDistance(e1.point, e2.point);
                    if(d > max) {
                        max = d;
                    }
                }
            }
            return max;
        }
        
        public int compareTo(Dist o) {
            if(dist < o.dist) {
                return -1;
            }
            
            if(dist > o.dist) {
                return 1;
            }
            
            return 0;
        }
        
        public String toString() {
            return dist + " n1: " + n1 + " n2: " + n2;
            // return String.format("%f n1: %s n2: %s", dist, n1, n2);
        }
    }
}
