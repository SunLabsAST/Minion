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

import java.util.HashMap;
import java.util.Map;

import com.sun.labs.minion.IndexConfig;
import com.sun.labs.minion.SearchEngine;

import com.sun.labs.minion.lexmorph.Lexicon;
import com.sun.labs.minion.lexmorph.MorphEngine;
import com.sun.labs.minion.lexmorph.Morph_en;
import com.sun.labs.minion.lexmorph.Word;
import com.sun.labs.minion.retrieval.ResultSetImpl;

/**
 * Provides an implementation of a feature clusterer built around the full
 * morphology engine. Words with the same root will be grouped together into
 * clusters.
 * 
 * @author Bernard Horan
 * @version $Id: MorphClusterer.java,v 1.1.2.4 2008/01/17 18:34:30 stgreen Exp $
 */
public class MorphClusterer extends ContingencyFeatureClusterer {

    /**
     * The morphological analyzer to use. Default to english.
     */
    private MorphEngine morph;

    /**
     * A map to hold the clusters keyed on roots
     */
    protected Map<String, FeatureCluster> clusterMap;

    /**
     * Create a new instance of me
     * 
     * @param type
     */
    public MorphClusterer(int type) {
        super(type);
        initializeClusterMap();
    }

    /**
     * Initiliaze the cluster map
     */
    private void initializeClusterMap() {
        clusterMap = new HashMap<String, FeatureCluster>();
    }

    /**
     * Default constructor
     */
    public MorphClusterer() {
        super();
        initializeClusterMap();
    }

    /**
     * Initialize the morphology. <br>
     * The index config for the search engine MUST have specified that 
     * the taxonomy is enabled and also provided a lexicon for the morphology engine.
     * @param anEngine a search engine
     */
    private void initializeMorphology(SearchEngine anEngine) {
        IndexConfig indexConfig = anEngine.getIndexConfig();
        if (!indexConfig.taxonomyEnabled()) {
            throw new RuntimeException("No taxonomy enabled for this engine");
        }
        Lexicon aLexicon = indexConfig.getLexicon();
        aLexicon
                .setTestCategories("adj adj-post adv adv-qualifier city "
                        + " det firstname lastname month n name nameprefix namesuffix "
                        + " nc nco nm nn npl npr nsg nsp number postdet prep prespart "
                        + " pro statecode title v vi vt weekday "
                        + " 1sg/2sg/3sg/comparative/npl/past/pastpart/prespart/superlative/not13sg "
                        + " adj/adv/aux/conj/det/interj/n/number/prep/v  adj/adv/det/n "
                        + " adj/adv/det/nm/npl/number/ord/pro  adj/adv/n  adj/adv/n/v "
                        + " adj/adv/ord  adj/n  adj/n/v  adj/number/ord  adj/ord  adv/v "
                        + " city/firstname/lastname  country  det/n/number "
                        + " det/nm/npl/number/pro  det/number  det/number/prep "
                        + " det/number/prep/pro  det/number/v  name/nm/v  nc/unit "
                        + " npl/unit  predet/pro  statecode/statename");
        morph = new Morph_en();
        morph.initialize(aLexicon);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.labs.minion.classification.ContingencyFeatureClusterer#cluster(com.sun.labs.minion.retrieval.ResultSetImpl)
     */
    public FeatureClusterSet cluster(ResultSetImpl s) {
        initializeMorphology(s.getEngine());
        return super.cluster(s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.labs.minion.classification.ContingencyFeatureClusterer#addFeature(com.sun.labs.minion.classification.ContingencyFeature)
     */
    protected void addFeature(ContingencyFeature cf) {
        String name = cf.getName();
        Word word = morph.analyze(name);
        Word[] wordRoots = word.getRoots();

        String[] roots;
        if (wordRoots == null || wordRoots.length == 0) {
            roots = new String[1];
            roots[0] = word.getWordString();
        } else {
            roots = new String[wordRoots.length];
            for (int i = 0; i < wordRoots.length; i++) {
                roots[i] = wordRoots[i].getWordString();
            }
        }
        
        for (int i = 0; i < roots.length; i++) {
            //
            //See if we have already encountered this root
            String root = roots[i];
            ContingencyFeatureCluster cluster = (ContingencyFeatureCluster) clusterMap.get(root);
            if (cluster != null) {
                cluster.add(cf);
            } else {
                cluster = new ContingencyFeatureCluster(root);
                cluster.add(cf);
                clusterMap.put(root, cluster);
            }
        }
        
        
        
        
        //
        // See if the feature is already in a cluster. It will
        // be in the cluster map based on its shortest morph, earliest
        // in the alphabet.
        // If it is in, then all the morphs will be. Otherwise,
        // add a new cluster with all the morphs
        String shortest = name;
        for (int i = 0; i < roots.length; i++) {
            if (roots[i].length() < shortest.length()) {
                if (roots[i].compareTo(shortest) < 0) {
                    shortest = roots[i];
                }
            }
        }

        ContingencyFeatureCluster clust = (ContingencyFeatureCluster) clusterMap.get(shortest);
        if (clust == null) {
            clust = new ContingencyFeatureCluster(shortest);
            for (int i = 0; i < roots.length; i++) {
                ContingencyFeature curr = new ContingencyFeature(cf);
                curr.setName(roots[i]);
                clust.add(curr);
            }
            clust.add(cf);
            clust.docIDs = null;
            clusterMap.put(shortest, clust);
        } else {
            clust.add(cf);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.labs.minion.classification.ContingencyFeatureClusterer#newInstance()
     */
    public FeatureClusterer newInstance() {
        return new MorphClusterer();
    }

    /* (non-Javadoc)
     * @see com.sun.labs.minion.classification.ContingencyFeatureClusterer#getClusters()
     */
    protected FeatureClusterSet getClusters() {
        //
        // Sort the feature clusters and return it
        FeatureClusterSet fcs = new FeatureClusterSet(clusterMap.values());
        initializeClusterMap();
        return fcs;
    }

}
