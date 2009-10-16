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

package com.sun.labs.minion;

import com.sun.labs.util.props.ConfigBoolean;
import com.sun.labs.util.props.ConfigComponent;
import com.sun.labs.util.props.ConfigComponentList;
import com.sun.labs.util.props.ConfigInteger;
import com.sun.labs.util.props.ConfigString;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.knowledge.KnowledgeSource;
import com.sun.labs.minion.lexmorph.LiteMorph_en;
import com.sun.labs.minion.pipeline.StopWords;
import com.sun.labs.minion.retrieval.CollectionStats;
import com.sun.labs.minion.retrieval.TFIDF;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.CharUtils;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that holds configuration data for querying.
 */
public class QueryConfig implements Cloneable,
        Configurable {

    //Configuration properties

    protected String configName;

    /**
     * The property for the maximum number of terms to return in response
     * to wildcard dictionary lookups when processing queries.
     */
    @ConfigInteger(defaultValue = 100)
    public static final String PROP_MAX_TERMS =
            "max_terms";

    /**
     * The property for the maximum amount of time (in milliseconds) to
     * spend doing wildcard dictionary lookups during querying.  A value
     * less than zero indicates that there should be no limit on the time
     * spent doing wildcard matches in the dictionary.
     */
    @ConfigInteger(defaultValue = 1000)
    public static final String PROP_MAX_WC_TIME =
            "max_wc_time";

    /**
     * The property for the limit of the window size to consider when
     * computing proximity queries.
     */
    @ConfigInteger(defaultValue = 4000)
    public static final String PROP_PROXIMITY_LIMIT =
            "proximity_limit";

    /**
     * The property indicating whether we should allow proximity queries to
     * cross field boundaries.
     */
    @ConfigBoolean(defaultValue = false)
    public static final String PROP_FIELD_CROSS =
            "field_cross";

    /**
     * The property indicating whether a query term provided in all upper
     * case characters should be considered in a case insensitive way
     * (i.e., all case variants will be considered).
     */
    @ConfigBoolean(defaultValue = true)
    public static final String PROP_ALL_UPPER_IS_CI =
            "all_upper_is_CI";

    /**
     * The property for the maximum amount of time (in milliseconds) to
     * spend on any query.  A value less than zero indicates that there
     * should be no time limit.
     */
    @ConfigInteger(defaultValue = -1)
    public static final String PROP_MAX_QUERY_TIME =
            "max_query_time";

    /**
     * The property naming the class containing the weighting function to
     * use for querying and document similarity.  Any class named here
     * <em>must</em> be a subclass of {@link
     * com.sun.labs.minion.retrieval.WeightingFunction}
     */
    @ConfigString(defaultValue = "com.sun.labs.minion.retrieval.TFIDF")
    public static final String PROP_WEIGHTING_FUNCTION =
            "weighting_function";

    /**
     * The property indicating whether perfect proximity scores (i.e., when
     * a passage exactly matches the query) should be boosted by the sum of
     * the term weights associated with the document, in order to provide
     * differentiation between multiple documents with perfect passages.
     */
    @ConfigBoolean(defaultValue = true)
    public static final String PROP_BOOST_PERFECT_PROXIMITY =
            "boost_perfect_proximity";

    /**
     * The property for the list of field multipliers to be used during
     * querying.  Such multipliers can be used (for example) to boost the
     * scores of hits occurring in titles or section headers.
     */
    @ConfigComponentList(type = FieldMultiplier.class)
    public static final String PROP_FIELD_MULTIPLIERS =
            "field_multipliers";

    /**
     * The property for a knowledge source to consult during querying.
     */
    @ConfigComponent(type = com.sun.labs.minion.knowledge.KnowledgeSource.class)
    public static final String PROP_KNOWLEDGE_SOURCE =
            "knowledge_source";

    /**
     * The search engine associated with the collection that we're
     * querying.
     */
    protected SearchEngine e;

    /**
     * The amount of time to spend on proximity queries, in milliseconds.
     */
    protected long proxLimit = 4000;

    /**
     * The maximum number of terms to retrieve from the dictionary for any
     * one term operator.
     */
    protected int maxDictTerms = 100;

    /**
     * The map from field names to multipliers for those fields.
     */
    protected Map<String, Float> fieldMultipliers =
            new HashMap<String, Float>();

    /**
     * The names of fields to which multipliers have been applied.
     */
    protected String[] multFields;

    /**
     * The multiplier values to use for the multiplied fields.  Elements
     * correspond to the elements in {@link #multFields}.
     *
     * @see #multFields
     */
    protected float[] multValues;

    /**
     * The maximum amount of time to spend on a dictionary lookup, in
     * milliseconds.
     */
    protected long maxDictLookupTime = 1000;

    /**
     * Whether we should always find all case variants.
     */
    protected boolean alwaysFindCaseVariants = false;

    /**
     * Whether all upper case terms should be treated case insensitively.
     * The default is to do so.
     */
    protected boolean allUpperIsCI = true;

    /**
     * Whether all lower case terms should be treated case insensitively.
     * The default is to do so.
     */
    protected boolean allLowerIsCI = true;

    /**
     * Whether proximity queries are allowed to cross field boundaries.
     * Default is false.
     */
    protected boolean fieldCross = false;

    /**
     * Whether we should boost perfect proximity scores with term weights.
     */
    protected boolean boostPerfectProx = true;

    /**
     * The maximum amount of time to spend on a query, in ms.  Less than zero
     * indicates that there is no maximum.
     */
    protected long maxQueryTime = -1;

    /**
     * A string representation of the sorting specification to use for
     * sorting results.
     */
    protected String sortSpec = "-score";

    /**
     * The weighting function to use to weight documents for a term.
     */
    public WeightingFunction wf = new TFIDF();

    /**
     * A set of weighting components to use when computing term weights.
     */
    public WeightingComponents wc = new WeightingComponents();

    /**
     * Whether we should do document vector length normalization for
     * queries.
     */
    protected boolean dvln;

    /**
     * Words to ignore during document vector creation.
     */
    protected StopWords vectorZeroWords;


    /**
     * Statistics for the collection we're evaluating the query in.
     */
    public CollectionStats cs;

    /**
     * The knowledge source to use. Default to english morphological analyzer.
     */
    protected KnowledgeSource knowledgeSource =
            LiteMorph_en.getMorph();

    /**
     * Our log.
     */
    static Logger logger = Logger.getLogger(QueryConfig.class.getName());

    /**
     * The log tag.
     */
    protected static String logTag = "QC";

    /**
     * Creates a query configuration with the default values.
     */
    public QueryConfig() {
    }

    public void setEngine(SearchEngine e) {
        this.e = e;
    }

    /**
     * Gets the value of proxLimit.
     *
     * @return the value of proxLimit
     */
    public long getProxLimit() {
        return proxLimit;
    }

    /**
     * Sets the value of proxLimit.
     *
     * @param proxLimit The new value of proxLimit
     */
    public void setProxLimit(int proxLimit) {
        this.proxLimit = proxLimit;
    }

    /**
     * Gets the value of maxTerms.
     *
     * @return the value of maxTerms
     */
    public int getMaxDictTerms() {
        return maxDictTerms;
    }

    /**
     * Sets the maximum number of terms to retrive from the dictionary.
     *
     * @param maxDictTerms The maximum number of terms.
     */
    public void setMaxDictTerms(int maxDictTerms) {
        this.maxDictTerms = maxDictTerms;
    }

    /**
     * Gets the value of maxWCTime.
     *
     * @return the value of maxWCTime
     */
    public long getMaxDictLookupTime() {
        return maxDictLookupTime;
    }

    /**
     * Sets the maximum time that will be spent on dictionary lookups.  A
     * value of -1 indicates that there is no limit.
     *
     * @param maxDictLookupTime The maximum time to spend on a dictionary
     * lookup.
     */
    public void setMaxDictLookupTime(long maxDictLookupTime) {
        this.maxDictLookupTime = maxDictLookupTime;
    }

    /**
     * Sets the field multipliers in use.
     *
     * @param fieldMultipliers A map from field names (as
     * <code>String</code>s) to field multipliers (as
     * <code>float</code>s).  If the map is empty, no multipliers will be
     * applied.
     */
    public void setFieldMultipliers(Map<String, Float> fieldMultipliers) {
        this.fieldMultipliers = fieldMultipliers;
        if(fieldMultipliers.size() != 0) {
            multFields = new String[fieldMultipliers.size()];
            multValues = new float[multFields.length];
            int n = 0;
            for(Iterator<Map.Entry<String, Float>> i =
                    fieldMultipliers.entrySet().
                    iterator(); i.hasNext();) {
                Map.Entry<String, Float> ent = i.next();
                multFields[n] = ent.getKey();
                multValues[n++] = ent.getValue();
            }
        } else {
            multFields = null;
            multValues = null;
        }
    }

    /**
     * Gets the names of the fields that have multipliers associated with
     * them.
     * @return the names of the fields that have mulitpliers associated with them
     */
    public String[] getMultFields() {
        return multFields;
    }

    /**
     * Gets the multipliers assciated with the fields.
     * @see #getMultFields
     * @return the multipliers associated with the multiplier fields
     */
    public float[] getMultValues() {
        return multValues;
    }

    /**
     * Gets a map from field names to field multipliers.
     * @return a map from multiplier fields to mulitpliers for those fields
     */
    public Map<String, Float> getFieldMultipliers() {
        return new HashMap<String, Float>(fieldMultipliers);
    }

    /**
     * Gets a weighting function to use for weighting documents.
     *
     * @return an instance of the weighting function, or <code>null</code>
     * if no weighting function has been defined.
     */
    public WeightingFunction getWeightingFunction() {
        try {
            if(wf == null) {
                return null;
            }

            return (WeightingFunction) wf.getClass().newInstance();
        } catch(Exception ex) {
            logger.log(Level.SEVERE, "Exception creating weighting function", ex);
            return null;
        }
    }

    /**
     * Gets a set of weighting components that can be used when calculating
     * term weights.
     *
     * @return an instance of the weighting components class, or
     * <code>null</code> if there is some error instantiating the class.
     * The set of weighting components that are returned will have been
     * initialized with the collection-level statistics for this collection.
     */
    public WeightingComponents getWeightingComponents() {
        return getWeightingComponents(getCollectionStats());
    }

    /**
     * Gets a set of weighting components that can be used when calculating
     * term weights.
     *
     * @param cs the collection statistics to use when calculating
     * components.
     * @return an instance of the weighting components class, or
     * <code>null</code> if there is some error instantiating the class.
     * The set of weighting components that are returned will have been
     * initialized with the collection-level statistics for this collection.
     */
    public WeightingComponents getWeightingComponents(CollectionStats cs) {
        try {
            //
            // Get a set of weighting components and initialize it with the
            // current collection statistics.
            WeightingComponents ret =
                    (WeightingComponents) wc.getClass().
                    newInstance();
            ret.setCollection(cs);
            return ret;
        } catch(Exception ex) {
            logger.log(Level.SEVERE, "Exception creating weighting function", ex);
            return null;
        }
    }

    /**
     * Sets the function used for weighting terms in documents.
     * @param wf the weighting function to use for weighting terms
     */
    public void setWeightingFunction(WeightingFunction wf) {
        this.wf = wf;
    }

    public StopWords getVectorZeroWords() {
        return vectorZeroWords;
    }

    /**
     * Gets the statistics for the collection against which we're
     * evaluating.
     * @return the collection level statistics for this engine
     */
    public CollectionStats getCollectionStats() {
        return e.getPM().getCollectionStats();
    }

    /**
     * Determines whether case should be taken into account when processing
     * the given string.  If the index upon which we're operating does not have
     * case sensitive entries, then case will never be taken into account.
     *
     * @param s The string to process.
     * @return <code>true</code> if the current configuration indicates
     * that the string should be processed in a case sensitive fashion.
     * The decision depends on:
     *
     * <ol>
     * <li>If we're always supposed to find case variants, then we
     * aren't case sensitive.
     * <li>If the query is all upper case and we want such things to
     * match case insensitively.
     * <li>If the query is all lower case and we want such things to
     * match case insensitively.
     * <li>If the string is incapable of being cased, we are case sensitive.
     * </ol>
     */
    public boolean caseSensitive(String s) {
        if(s == null) {
            return false;
        }

        //
        // If this is an uncased index, then we always want to match case
        // insensitively.
        if(!((SearchEngineImpl) e).getPM().isCasedIndex()) {
            return false;
        }

        return !(alwaysFindCaseVariants ||
                (allUpperIsCI && CharUtils.toUpperCase(s).
                equals(s)) ||
                (allLowerIsCI &&
                CharUtils.toLowerCase(s).equals(s))) ||
                CharUtils.isUncased(s);
    }

    /**
     * Gets whether we want to modify perfect proximity scores by adding in the
     * term weight scores for the documents in order to distinguish perfect
     * hits.
     * @return <code>true</code> if perfect proximity scores should be
     * boosted by the term weights for the passages.
     */
    public boolean getBoostPerfectProx() {
        return boostPerfectProx;
    }

    /**
     * Sets the sorting specification.
     * @param sortSpec the sorting specification to use for this query
     */
    public void setSortSpec(String sortSpec) {
        this.sortSpec = sortSpec;
    }

    /**
     * Gets the sorting specification.
     * @return the sorting specification to use for this query
     */
    public String getSortSpec() {
        return sortSpec;
    }

    /**
     * Sets the statistics for the collection against which we're
     * evaluating.
     * @param cs the statistics for the collection
     */
    public void setCollectionStats(CollectionStats cs) {
        this.cs = cs;
    }

    /**
     * Gets the knowledge source.
     * @return the knowledge source to use to look up term variants.
     */
    public KnowledgeSource getKnowledgeSource() {
        return knowledgeSource;
    }

    /**
     * Clones the query configuration.
     */
    public Object clone() {
        QueryConfig result = null;
        try {
            result = (QueryConfig) super.clone();
            result.defaultFields = new ArrayList<FieldInfo>(defaultFields);
        } catch(CloneNotSupportedException ex) {
            throw new InternalError();
        }
        return result;
    }

    /**
     * Creates a query configuration from a property sheet described in an external XML file.
     * @param ps The properties.
     * @throws com.sun.labs.util.props.PropertyException if there is any error
     * retrieving the properties.
     */
    public void newProperties(PropertySheet ps)
            throws PropertyException {
        setWeightingFunction(ps);
        setProxLimit(ps.getInt(PROP_PROXIMITY_LIMIT));
        setMaxDictLookupTime(ps.getInt(PROP_MAX_WC_TIME));
        setMaxDictTerms(ps.getInt(PROP_MAX_TERMS));
        setMaxQueryTime(ps.getInt(PROP_MAX_QUERY_TIME));
        setFieldCross(ps.getBoolean(PROP_FIELD_CROSS));
        setBoostPerfectProximity(ps.getBoolean(PROP_BOOST_PERFECT_PROXIMITY));
        setAllUpperIsCI(ps.getBoolean(PROP_ALL_UPPER_IS_CI));
        vectorZeroWords =
                (StopWords) ps.getComponent(PROP_VECTOR_ZERO_WORDS);
        setFieldMultipliers((List<FieldMultiplier>) ps.getComponentList(PROP_FIELD_MULTIPLIERS));
        try {
            setKnowledgeSource((KnowledgeSource) ps.getComponent(PROP_KNOWLEDGE_SOURCE));
        } catch(PropertyException pe) {
        }
        defaultFields = (List<FieldInfo>) ps.getComponentList(PROP_DEFAULT_FIELDS);
    }

    private void setAllUpperIsCI(boolean allUpperIsCI) {
        this.allUpperIsCI = allUpperIsCI;
    }

    private void setBoostPerfectProximity(boolean boostPerfectProx) {
        this.boostPerfectProx = boostPerfectProx;
    }

    private void setFieldCross(boolean fieldCross) {
        this.fieldCross = fieldCross;
    }

    private void setKnowledgeSource(KnowledgeSource knowledgeSource) {
        this.knowledgeSource = knowledgeSource;
    }

    private void setFieldMultipliers(List<FieldMultiplier> mults)
            throws PropertyException {
        Map<String, Float> map =
                new HashMap<String, Float>();
        for(FieldMultiplier fm : mults) {
            map.put(fm.getName(), fm.getValue());
        }
        setFieldMultipliers(map);
    }

    private void setMaxQueryTime(long maxQueryTime) {
        this.maxQueryTime = maxQueryTime;
    }

    private void setWeightingFunction(PropertySheet ps)
            throws PropertyException {
        String wfClass = ps.getString(PROP_WEIGHTING_FUNCTION);
        if(wfClass != null) {
            try {
                wf = (WeightingFunction) Class.forName(wfClass).newInstance();
            } catch(ClassNotFoundException cnf) {
                logger.warning("Unable to load weighting function class: " +
                        wfClass +
                        " using default: " + wf.toString());
            } catch(InstantiationException ine) {
                logger.log(Level.WARNING, "Error instantiating weighting function class: " +
                        wfClass + " using default: " + wf.toString(), ine);
            } catch(ClassCastException cce) {
                logger.warning("Weighting function class: " + wfClass +
                        " does not appear to implement WeightingFunction " +
                         " using default: " +
                        wf.toString());
            } catch(Exception oe) {
                logger.log(Level.WARNING, "Error instantiating weighting function class: " +
                        wfClass + " using default: " + wf.toString(), oe);
            }
        }
    }
    
    @ConfigComponent(type=com.sun.labs.minion.pipeline.StopWords.class)
    public static final String PROP_VECTOR_ZERO_WORDS = "vector_zero_words";

    /**
     * A property for a list of fields to search by default.
     */
    @ConfigComponentList(type=com.sun.labs.minion.FieldInfo.class,defaultList={})
    public static final String PROP_DEFAULT_FIELDS = "default_fields";
    List<FieldInfo> defaultFields;

    public void addDefaultField(String field) {
        FieldInfo fi = e.getFieldInfo(field);
        if(fi == null) {
            logger.warning("Field: " + field + " does not exist");
        } else {
            defaultFields.add(fi);
        }
    }
    
    public void removeDefaultField(String field) {
        FieldInfo fi = e.getFieldInfo(field);
        if (fi == null) {
            logger.warning("Field: " + field + " does not exist");
        } else {
            defaultFields.remove(fi);
        }
    }

    public List<FieldInfo> getDefaultFields() {
        return defaultFields;
    }


} // QueryConfig
