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

import com.sun.labs.minion.ClusterStatistics;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.ResultsCluster;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.labs.minion.classification.FeatureCluster;
import com.sun.labs.minion.classification.FeatureClusterSet;
import com.sun.labs.minion.classification.FeatureClusterer;
import com.sun.labs.minion.classification.FeatureSelector;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.retrieval.ResultImpl;
import com.sun.labs.minion.retrieval.ResultSetImpl;
import com.sun.labs.minion.retrieval.ScoredGroup;

/**
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class ResultsClusterImpl implements ResultsCluster,
                                           Comparable<ResultsClusterImpl> {

    SearchEngine e;
    List<ResultImpl> res;
    Set<String> keys;
    ResultSet rs;
    String name;
    ClusterStatisticsImpl stats;

    /**
     * The most central result in this set.
     */
    Result mc;

    /**
     * The distance of the most central result from the centroid.
     */
    double mcDist;

    /**
     * The map from feature names to index in the point, which we
     * can use later to choose a representative set of words for the
     * cluster.
     */
    Map<String, Integer> features;

    boolean nameOnlyDescription;

    /**
     * The centroid of the cluster.
     */
    double[] centroid;

    static Logger logger = Logger.getLogger(ResultsClusterImpl.class.getName());

    protected static String logTag = "RCI";

    /**
     * Creates a ClusterResultImpl
     */
    public ResultsClusterImpl(Map<String, Integer> features, double[] centroid, SearchEngine e) {
        this.features = features;
        this.keys = new HashSet<String>();
        this.e = e;
        this.centroid = centroid;
        mcDist = Double.MAX_VALUE;
        res = new ArrayList<ResultImpl>();
        stats = new ClusterStatisticsImpl();
    }

    /**
     * Creates a results cluster by reading it from the given input.
     * @param e the engine associated with this cluster
     * @param in the input from which we can read the cluster
     * @throws java.io.IOException if there is any error reading the data
     * @throws com.sun.labs.minion.SearchEngineException if there is an error
     * creating the results
     */
    public ResultsClusterImpl(SearchEngine e, DataInput in)
            throws java.io.IOException, SearchEngineException {
        this.e = e;
        nameOnlyDescription = in.readBoolean();
        int k = in.readInt();
        centroid = new double[k];
        for(int i = 0; i < centroid.length;
                i++) {
            centroid[i] = in.readDouble();
        }

        int nf = in.readInt();
        features = new HashMap<String, Integer>();
        for(int i = 0; i < nf;
                i++) {
            features.put(in.readUTF(), in.readInt());
        }

        //
        // Read the keys and scores and then transduce that into a list of
        // results that we can add to this cluster to build the stats.  It's a bit
        // of a roundabout way to do this, but it will make sure that our data
        // structures are set up correctly.
        int nk = in.readInt();
        Map<String, Float> km = new LinkedHashMap<String, Float>();
        for(int i = 0; i < nk;
                i++) {
            km.put(in.readUTF(), in.readFloat());
        }
        mcDist = Double.MAX_VALUE;
        keys = new HashSet<String>();
        res = new ArrayList<ResultImpl>();
        stats = new ClusterStatisticsImpl();
        rs = ((SearchEngineImpl) e).getResults(km);
        for(Result r : rs.getAllResults(false)) {
            addInternal((ResultImpl) r, r.getScore());
        }
    }

    /**
     * Adds a result to this cluster when the distance value to use for the
     * result is stored in a field in the result.
     */
    protected void add(Result r, String field) {
        List l = null;
        if(field != null) {
            l = r.getField(field);
        }
        if(l == null || l.size() == 0) {
            add(r, 2);
        } else {
            add(r, (Double) l.get(0));
        }
    }

    /**
     * Adds a result to this cluster where the result is represented using the
     * given point.  This is used to calcluate the distance to the centroid.
     */
    protected void add(Result r, double[] point) {
        add(r, ClusterUtil.euclideanDistance(centroid, point));
    }

    protected void add(Result r, double d) {
        addInternal((ResultImpl) ((ResultImpl) r).clone(), (float) d);
    }

    private void addInternal(ResultImpl ri, float d) {
        ri.setScore(d);
        res.add(ri);
        keys.add(ri.getKey());
        stats.add(d);
        if(d < mcDist) {
            mc = ri;
            mcDist = d;
        }
    }

    /**
     * Gets a description of this cluster as a list of at most the top n terms.
     */
    public List<String> getDescription(int n) {

        if(nameOnlyDescription) {
            List<String> ret = new ArrayList<String>();
            ret.add(name);
            return ret;
        }
        //
        // Otherwise, choose the most highly weighted terms in the centroid.
        Map<Integer, HE> top = new HashMap<Integer, HE>();
        for(Map.Entry<String, Integer> e : features.entrySet()) {
            HE he = top.get(e.getValue());
            if(he == null) {
                he = new HE(e.getKey(), centroid[e.getValue()]);
                top.put(e.getValue(), he);
            } else {
                he.name.add(e.getKey());
                he.weight += centroid[e.getValue()];
            }
        }
        HE[] x = top.values().toArray(new HE[0]);
        com.sun.labs.minion.util.Util.sort(x);
        List<String> ret = new ArrayList<String>();
        for(int i = 0; i < n && i < x.length;
                i++) {
            ret.add(String.format("<%s, %.3f>", x[i].name.get(0), x[i].weight));
        }
        return ret;
    }

    public synchronized ResultSet getResults() {
        if(rs == null) {

            Map<DiskPartition, ScoredGroup> map =
                    new HashMap<DiskPartition, ScoredGroup>();
            for(ResultImpl r : res) {

                DiskPartition p = r.getPart();
                ScoredGroup ag = map.get(p);
                if(ag == null) {
                    ag = new ScoredGroup(128);
                    ag.setPartition(p);
                    map.put(p, ag);
                }
                ag.addDoc(r.getDocID(), r.getScore());
            }
            List groups = new ArrayList();
            groups.addAll(map.values());
            rs = new ResultSetImpl(e, "+score", groups);
        }
        return rs;
    }

    public List<Result> getResultList() {
        return new ArrayList<Result>(res);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private List<String> getDescription(FeatureClusterer fc, FeatureSelector fs, int nFeat) {
        ResultSetImpl r = (ResultSetImpl) getResults();

        //
        // Cluster the features and select the number that we want.
        FeatureClusterSet clusters = fc.cluster(r);

        //
        // Now select the clusters that will be used for training
        FeatureClusterSet selectedClusters =
                fs.select(clusters, r.getEngine().getQueryConfig().getWeightingComponents(), r.size(), nFeat, r.getEngine());

        List<String> ret = new ArrayList<String>();
        for(Iterator i = selectedClusters.getContents().iterator(); i.hasNext();) {
            ret.add(((FeatureCluster) i.next()).getContents().toString());
        }

        return ret;
    }

    public int compareTo(ResultsClusterImpl o) {
        return o.getResults().size() - getResults().size();
    }

    public boolean contains(String key) {
        return keys.contains(key);
    }

    public double distance(String key) {
        try {
            com.sun.labs.minion.retrieval.DocumentVectorImpl dv =
                    (com.sun.labs.minion.retrieval.DocumentVectorImpl) e.getDocumentVector(key);
            com.sun.labs.minion.classification.WeightedFeature[] feat = dv.getFeatures();
            double[] point = new double[centroid.length];
            for(int i = 0; i < feat.length;
                    i++) {
                java.lang.Integer p = features.get(feat[i].getName());
                if(p != null) {
                    point[p] += feat[i].getWeight();
                }
            }

            return com.sun.labs.minion.clustering.ClusterUtil.euclideanDistance(centroid,
                                                                   com.sun.labs.minion.clustering.ClusterUtil.normalize(point));
        } catch(SearchEngineException ex) {
            logger.log(Level.SEVERE, null, ex);
            return 0;
        }
    }

    public ClusterStatistics getStatistics() {
        return stats;
    }

    public Result getMostCentralResult() {
        return mc;
    }

    public int size() {
        return res.size();
    }

    class HE implements Comparable<HE> {

        public HE(String name, double weight) {
            this.name = new ArrayList<String>();
            this.name.add(name);
            this.weight = weight;
        }

        public int compareTo(ResultsClusterImpl.HE o) {
            if(weight < o.weight) {
                return 1;
            }

            if(weight > o.weight) {
                return -1;
            }

            return 0;
        }
        List<String> name;
        int p;
        double weight;
    }

    /**
     * Writes the cluster data to the given output.
     *
     * @param out the output
     * @throws java.io.IOException if there is any error writing
     */
    public void save(DataOutput out)
            throws java.io.IOException {

        //
        // Is this a weird one?
        out.writeBoolean(nameOnlyDescription);
        
        //
        // Write the centroid.
        out.writeInt(centroid.length);
        for(int i = 0; i < centroid.length;
                i++) {
            out.writeDouble(centroid[i]);
        }

        //
        // Write the features.
        out.writeInt(features.size());
        for(Map.Entry<String, Integer> e : features.entrySet()) {
            out.writeUTF(e.getKey());
            out.writeInt(e.getValue());
        }

        //
        // Write out the keys and scores.
        out.writeInt(res.size());
        for(ResultImpl r : res) {
            out.writeUTF(r.getKey());
            out.writeFloat(r.getScore());
        }
    }
}
