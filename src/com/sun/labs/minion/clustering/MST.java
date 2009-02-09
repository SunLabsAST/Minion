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

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * A minimum spanning tree cluterer.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class MST extends AbstractClusterer {
    
    public static String logTag = "MST";
    
    public MST() {
        super();
    }
    
    /**
     * Builds the minimum spanning tree.
     */
    public void cluster() {
        
        //
        // Make a heap of edges organized by their size.
        PriorityQueue<Edge> eq = new PriorityQueue<Edge>(els.length);
        Vertex[] vs = new Vertex[els.length];
        
        //
        // Create vertices for each cluster element and put each vertex in
        // its own tree.
        for(int i = 0; i < els.length; i++) {
            els[i].member = -1;
            vs[i] = new Vertex(els[i]);
            vs[i].t = new Tree(vs[i]);
        }
        
        //
        // Now generate the set of edges heaped by distance.
        for(int i = 0; i < vs.length; i++) {
            for(int j = i+1; j < vs.length; j++) {
                Edge e = new Edge(vs[i], vs[j]);
                eq.offer(e);
            }
        }
        
        //
        // Proceed until we're out of edges.
        int x = 0;
        while(eq.size() > 0) {
            Edge e = eq.poll();
            
            //
            // Check if the edge connects two trees. If it does, then combine them so
            // That they're in the same tree.
            if(e.v1.t != e.v2.t) {
                e.v1.t.add(e);
                e.v1.t.combine(e.v2.t);
            }
        }
        
        //
        // Now the MST is whatever tree is left.  Heap the edges in increasing
        // distance.
        Tree mst = vs[0].t;
        eq = new PriorityQueue<Edge>(mst.e.size(),
                new Comparator<Edge>() {
            public int compare(Edge o1, Edge o2) {
                return -1 * o1.compareTo(o2);
            }
            
        });
        eq.addAll(mst.e);
        
        //
        // Remove the longest edges until we run our or have our k clusters.
        for(int i = 0; i < k && eq.size() > 0; i++) {
            Edge e = eq.poll();
            mst.remove(e);
        }
        
        //
        // Now, for each vertex, go and get the set of reachable vertices and tag things with
        // their cluster.
        int cl = 0;
        for(int i = 0; i < vs.length; i++) {
            if(vs[i].el.member == -1) {
                Set<Vertex> reach = mst.reachable(vs[i]);
                for(Vertex vx : reach) {
                    vx.el.member = cl;
                }
                cl++;
            }
        }
        
        k = cl;
        this.k = cl;
        
        //
        // Now we'll number the clusters and the elements and compute centroids.
        clusters = new double[k][N];
        int[] counts = new int[k];
        for(int i = 0; i < vs.length; i++) {
            ClusterUtil.add(clusters[vs[i].el.member], vs[i].el.point);
            counts[vs[i].el.member]++;
        }
        for(int i = 0; i < clusters.length; i++) {
            ClusterUtil.div(clusters[i], counts[i]);
        }
    }
    
    public class Vertex {
        
        public ClusterElement el;
        
        public Tree t;
        
        /**
         * Creates a Vertex
         */
        public Vertex(ClusterElement el) {
            this.el = el;
        }
        
        public String toString() {
            return el.r.getKey();
        }
    }
    public class Edge implements Comparable<Edge> {
        
        protected Vertex v1;
        
        protected Vertex v2;
        
        protected double dist;
        
        /**
         * Creates a Edge
         */
        public Edge(Vertex v1, Vertex v2) {
            this.v1 = v1;
            this.v2 = v2;
            dist = ClusterUtil.euclideanDistance(v1.el.point, v2.el.point);
        }
        
        public int compareTo(Edge o) {
            if(dist < o.dist) {
                return -1;
            }
            
            if(dist > o.dist) {
                return 1;
            }
            
            return 0;
        }
        
        public String toString() {
            return String.format("%s -> %s %.3f", v1, v2, dist);
        }
        
    }
    
    public class Tree {
        
        protected Set<Vertex> v;
        
        protected Set<Edge> e;
        
        /**
         * Creates a containing the single vertex provided.
         */
        public Tree(Vertex vx) {
            v = new HashSet<Vertex>();
            v.add(vx);
            e = new HashSet<Edge>();
        }
        
        public Tree(Tree t) {
            v = new HashSet<Vertex>();
            for(Vertex vx : t.v) {
                vx.t = this;
                v.add(vx);
            }
            e = new HashSet<Edge>();
            e.addAll(t.e);
        }
        
        public void reset() {
            for(Vertex vx : v) {
                vx.t = this;
            }
        }
        
        /**
         * Tests whether a given vertex is in the graph.
         */
        public boolean contains(Vertex v) {
            return this.v.contains(v);
        }
        
        public void add(Edge e) {
            this.e.add(e);
        }
        
        /**
         * Combines another tree with this one, making sure that the vertices in
         * the other tree indicate that they are now a member of this tree.
         *
         * @param t the tree to combine.
         */
        public void combine(Tree t) {
            for(Vertex x : t.v) {
                x.t = this;
                v.add(x);
            }
            e.addAll(t.e);
        }
        
        //
        // Remove a vertex from the graph and any edges with that vertex on
        // either end.
        public void remove(Vertex vx) {
            v.remove(vx);
            for (Iterator<Edge> i = e.iterator(); i.hasNext();) {
                Edge e = i.next();
                if(e.v1 == vx || e.v2 == vx) {
                    i.remove();
                }
            }
        }
        
        public void remove(Edge e) {
        }
        
        //
        // Restrict the graph to those vertices reachable from a given vertex.
        public Set<Vertex> reachable(Vertex vx) {
            Set<Vertex> seen = new HashSet<Vertex>();
            LinkedList<Vertex> toProcess = new LinkedList<Vertex>();
            toProcess.add(vx);
            while(toProcess.size() > 0) {
                Vertex curr = toProcess.removeFirst();
                if(seen.contains(curr)) {
                    continue;
                }
                
                seen.add(curr);
                for(Edge ed : e) {
                    if(ed.v1 == curr) {
                        toProcess.add(ed.v2);
                    }
                    if(ed.v2 == curr) {
                        toProcess.add(ed.v1);
                    }
                }
            }
            return seen;
        }
    }
}
