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
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.ResultsCluster;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.util.props.ConfigInteger;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.sun.labs.minion.classification.Feature;
import com.sun.labs.minion.classification.FeatureCluster;
import com.sun.labs.minion.classification.FeatureClusterSet;
import com.sun.labs.minion.classification.FeatureClusterer;
import com.sun.labs.minion.classification.FeatureSelector;
import com.sun.labs.minion.classification.WeightedFeature;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.retrieval.DocumentVectorImpl;
import com.sun.labs.minion.retrieval.ResultImpl;
import com.sun.labs.minion.retrieval.ResultSetImpl;
import com.sun.labs.minion.retrieval.TermStats;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.retrieval.cache.DocCache;
import com.sun.labs.minion.util.MinionLog;
import com.sun.labs.minion.util.StopWatch;

/**
 * An abstract base class for clustering algorithms.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public abstract class AbstractClusterer implements Configurable {
    
    protected Map<String,Integer> features;
    
    protected Map<Integer,List<String>> ptof;
    
    protected String field;
    
    protected int k;
    
    protected int N;
    
    protected ResultSetImpl rs;
    
    protected ClusterElement[] els;
    
    protected double[][] clusters;
    
    protected String[] names;
    
    protected FeatureClusterer fc;
    
    protected FeatureSelector fs;
    
    protected int nFeat;
    
    protected List<ClusterElement> ignored;
    
    protected List<ClusterElement> empty;
    
    protected List<ClusterElement> te;
    
    protected static double[] one = {1};
    
    public static final MinionLog log = MinionLog.getLog();
    
    public static final String logTag = "ACL";
    
    protected DocCache dc;
    
    public AbstractClusterer() {
    }
    
    public void init(String field) throws SearchEngineException {
        this.field = field;
        features = new HashMap<String,Integer>();
        ptof = new HashMap<Integer,List<String>>();
        te = new ArrayList<ClusterElement>();
        ignored = new ArrayList<ClusterElement>();
        empty = new ArrayList<ClusterElement>();
        
        //
        // Get the elements for our clusters, which has a side-effect of providing
        // the dimensionality of the space that we're clustering in.
        if(fc != null) {
            getElements(fc, fs, nFeat);
        } else {
            getElements();
        }
    }
    
    public void setResults(ResultSet r) {
        this.rs = (ResultSetImpl) r;
    }
    
    public void setFeatureInfo(FeatureClusterer fc, FeatureSelector fs, int nFeat) {
        this.fc = fc;
        this.fs = fs;
        this.nFeat = nFeat;
    }
    
    public void setK(int k) {
        this.k = k;
    }
    
    public void setData(String d) {
    }
    
    /**
     * Gets the elements that are to be clustered from a set of results.  The first
     * step is to select the given number of feature clusters from the result set.
     *
     * @param fc a feature clusterer
     * @param fs a feature selector
     * @param nFeat the number of features to select
     */
    protected void getElements(
            FeatureClusterer fc,
            FeatureSelector fs,
            int nFeat) throws SearchEngineException {
        
        dc = new DocCache(-1, rs.getEngine());
        
        StopWatch sw = new StopWatch();
        sw.start();
        fc.setField(field);
        fc.setDocCache(dc);
        FeatureClusterSet clusters = fc.cluster(rs);
        sw.stop();
        log.debug(logTag, 0, "feature clustering took: " + sw.getTime());
        
        //
        // Now select the clusters that will be used for training
        sw.reset();
        sw.start();
        FeatureClusterSet selectedClusters =
                fs.select(clusters,
                dc.getWeightingComponents(),
                rs.size(), nFeat, rs.getEngine());
        sw.stop();
        log.debug(logTag, 0, "feature selection took: " + sw.getTime());
        sw.reset();
        
        TermStats[] featStats = new TermStats[selectedClusters.size()];
        
        WeightingComponents wc = dc.getWeightingComponents();
        for(Iterator i = selectedClusters.getContents().iterator(); i.hasNext(); ) {
            List<String> fnames = new ArrayList();
            FeatureCluster clust = (FeatureCluster) i.next();
            featStats[N] = new TermStats(clust.getName());
            for(Iterator j = clust.getContents().iterator(); j.hasNext(); ) {
                Feature f = (Feature) j.next();
                TermStats ts = wc.getTermStats(f.getName());
                if(ts != null) {
                    featStats[N].add(ts);
                    features.put(f.getName(), N);
                    fnames.add(f.getName());
                }
            }
            ptof.put(N, fnames);
            N++;
        }
        List l = rs.getAllResults(false);
        int n = 0;
        sw.start();
        WeightingFunction wf = dc.getWeightingFunction();
        for(Iterator i = l.iterator(); i.hasNext(); ) {
            ResultImpl r = (ResultImpl) i.next();
            int[] freqs = new int[N];
            DocKeyEntry dke = r.getKeyEntry();
            WeightedFeature[] feat = dc.get(dke, field, r.getPart()).getFeatures();
            
            //
            // Notice documents that are empty.
            if(feat.length == 0) {
                empty.add(new ClusterElement(r, one));
                continue;
            }
            
            int added = 0;
            for(int j = 0; j < feat.length; j++) {
                Integer x = features.get(feat[j].getName());
                if(x != null) {
                    freqs[x] += feat[j].getFreq();
                    added++;
                }
            }
            
            //
            // If we added some features from this document, then put it on our
            // list of elements.
            if(added > 0) {
                wc.setDocument(dke, field);
                double[] point = new double[N];
                double sum = 0;
                for(int j = 0; j < freqs.length; j++) {
                    wc.setTerm(featStats[j]);
                    wc.fdt = freqs[j];
                    wf.initTerm(wc);
                    point[j] = wf.termWeight(wc) / wc.dvl;
                    sum += point[j] * point[j];
                }
                
                //
                // Normalize the point length.
                sum = Math.sqrt(sum);
                for(int j = 0; j < point.length; j++) {
                    point[j] /= sum;
                }
                
                //
                // Note that we'll normalize the point to unit length!
                te.add(new ClusterElement(r, point));
            } else {
                ignored.add(new ClusterElement(r, one));
            }
        }
        sw.stop();
        log.debug(logTag, 0, "getting features took: " + sw.getTime());
        
        els = te.toArray(new ClusterElement[0]);
    }
    
    protected void getElements() throws SearchEngineException {
        
        //
        // We just have the results.  We need to build a map from feature
        // names to positions in n-space.  We'll just go ahead and do this
        // willy-nilly, since we'll eventually transform all of our results
        // into this space.
        List l = rs.getAllResults(false);
        List<WeightedFeature[]> vecs = new ArrayList<WeightedFeature[]>();
        int n = 0;
        int p = 0;
        
        //
        // We'll go through once to build the name to position map and then
        // through again to make the actual points.
        for(Iterator i = l.iterator(); i.hasNext(); ) {
            Result r = (Result) i.next();
            WeightedFeature[] vec = ((DocumentVectorImpl) r.getDocumentVector()).getFeatures();
            
            //
            // A document with no words isn't that useful for clustering.
            if(vec.length == 0) {
                empty.add(new ClusterElement(r, one));
                continue;
            }
            
            vecs.add(vec);
            for(int j = 0; j < vec.length; j++) {
                Integer x = features.get(vec[j].getName());
                if(x == null) {
                    features.put(vec[j].getName(), p++);
                }
            }
        }
        
        List<ClusterElement> te = new ArrayList<ClusterElement>();
        for(int i = 0; i < vecs.size(); i++) {
            WeightedFeature[] vec = vecs.get(i);
            double[] point = new double[features.size()];
            for(int j = 0; j < vec.length; j++) {
                point[features.get(vec[j].getName())] = vec[j].getWeight();
            }
            
            //
            // Note that we don't have to normalize here because it was already
            // done for us.
            te.add(new ClusterElement((Result) l.get(i), point));
        }
        
        els = te.toArray(new ClusterElement[0]);
        N = features.size();
    }
    
    public Set<ResultsCluster> getClusters() {
        ResultsClusterImpl[] res = new ResultsClusterImpl[k];
        for(int i = 0; i < res.length; i++) {
            res[i] = new ResultsClusterImpl(features, clusters[i], rs.getEngine());
            if(names != null) {
                res[i].setName(names[i]);
            }
        }
        for(int i = 0; i < els.length; i++) {
            res[els[i].member].add(els[i].r, els[i].point);
        }
        
        //
        // Sort the results by the size of the set.
        com.sun.labs.minion.util.Util.sort(res);
        Set<ResultsCluster> ret = new LinkedHashSet<ResultsCluster>();
        for(int i = 0; i < res.length; i++) {
            
            //
            // Skip clusters of size 0.
            if(res[i].res.size() > 0) {
                ret.add(res[i]);
            }
        }
        
        //
        // Add in any things that were missed out on to a final cluster.
        if(ignored.size() > 0) {
            ResultsClusterImpl ic = new ResultsClusterImpl(features, one, rs.getEngine());
            for(ClusterElement e : ignored) {
                ic.add(e.r, one);
            }
            ic.setName("Remainder");
            ic.nameOnlyDescription = true;
            ret.add(ic);
        }
        
        if(empty.size() > 0) {
            ResultsClusterImpl ec = new ResultsClusterImpl(features, one, rs.getEngine());
            for(ClusterElement e : empty) {
                ec.add(e.r, one);
            }
            ec.setName("Empty");
            ec.nameOnlyDescription = true;
            ret.add(ec);
        }
        return ret;
    }
    
    public ClusterElement[] getEls() {
        return els;
    }
    
    public String toString(double[] p) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        int n = 0;
        for(int i = 0; i < p.length; i++) {
            if(p[i] > 0) {
                if(n > 0) {
                    sb.append(", ");
                }
                sb.append(String.format("(%.3f %s)", p[i], ptof.get(i)));
                n++;
            }
        }
        sb.append("]");
        return sb.toString();
    }
    
    public abstract void cluster();
    
    public void newProperties(PropertySheet ps) throws PropertyException {
        k = ps.getInt(PROP_K);
    }
    
    @ConfigInteger(defaultValue=10)
    public static final String PROP_K = "k";
    
}
