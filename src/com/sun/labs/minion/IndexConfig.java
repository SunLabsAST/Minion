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
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.ConfigurationManager;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import com.sun.labs.minion.lexmorph.Lexicon;
import java.util.logging.Logger;

/**
 * A class that holds configuration data for indexing documents.
 */
public class IndexConfig implements Cloneable,
        Configurable {

    //Configuration Manager properties
    /**
     * Name used by configuration manager
     */
    protected String configName;

    /**
     * The property for the name of the index directory.  This will typically
     * be set to the value of the global index directory property.
     */
    @ConfigString
    public static final String PROP_INDEX_DIRECTORY = "index_directory";

    /**
     * The property for the symbolic name of the index that we're using.  This
     * property can be set and used by an application that wishes to display a
     * user-friendly name.
     */
    @ConfigString(mandatory = false)
    public static final String PROP_INDEX_NAME = "index_name";

    /**
     * The property for the number of random splits to use when doing validation
     * using a random splitter when building classifiers.
     */
    @ConfigInteger(defaultValue = 10)
    public static final String PROP_RANDOM_SPLITTER_NUMSPLITS =
            "random_splitter_numsplits";

    /**
     * The property for the number of folds to use when doing k-fold cross
     * validation when building classifiers.
     */
    @ConfigInteger(defaultValue = 10)
    public static final String PROP_KFOLD_SPLITTER_NUMFOLDS =
            "kfold_splitter_numfolds";

    /**
     * The property indicating whether we should attempt to do feature backoff
     * during classification.  If this property is <code>true</code>, then the
     * system will attempt to reduce the number of features that will be used
     * to build the classifiers to see if doing that improves the classification
     * performance on the test data.
     */
    @ConfigBoolean(defaultValue = true)
    public static final String PROP_ENABLE_FEATURE_BACKOFF =
            "enable_feature_backoff";

    /**
     * The property indicating whether we should store the scores associated with
     * classifiers when a new document is successfully classified.  An application
     * can use the stored classifier scores to show the user how likely it is
     * that a given document belongs to a given class of documents.
     */
    @ConfigBoolean(defaultValue = false)
    public static final String PROP_STORE_CLASSIFIER_SCORES =
            "store_classifier_scores";

    @ConfigBoolean(defaultValue = false)
    public static final String PROP_STORE_NON_CLASSIFIED =
            "store_non_classified";

    /**
     * The property that contains a list of the names of the field information
     * objects that this index should contain.
     */
    @ConfigComponentList(type = FieldInfo.class)
    public static final String PROP_FIELD_INFO = "field_info";

    /**
     * The property that names the location of the lexicon.
     */
    @ConfigString(mandatory = false)
    public static final String PROP_LEXICON_LOCATION = "lexicon_location";

    /**
     * The property indicating for whether the taxonomy should be enabled?
     */
    @ConfigBoolean(defaultValue = false)
    public static final String PROP_TAXONOMY_ENABLED = "taxonomy_enabled";

    /**
     * A property that names the default field information to use when
     * encountering an unknown field during indexing.
     */
    @ConfigComponent(type = FieldInfo.class)
    public static final String PROP_DEFAULT_FIELD_INFO =
            "default_field_info";

    /**
     * The directory that holds the index.
     */
    protected String indexDir;

    /**
     * The symbolic name of the collection.
     */
    protected String indexName;

    /**
     * A map from field names to the field information for those names.
     */
    protected Map<String, FieldInfo> fieldInfo =
            new HashMap<String, FieldInfo>();

    /**
     * The exemplar field info to use when encountering an unknown field name
     * during indexing.
     */
    protected FieldInfo defaultField;

    /**
     * The lexicon.
     */
    protected Lexicon lexicon;

    /**
     * The file that contains the lexicon
     */
    protected String lexiconFile;

    // The following settings relate to classification
    /**
     * The number of random splits that should be made when the random splitter
     * is being used.
     */
    protected int randomSplitterNumSplits;

    /**
     * The number of folds that should be made when the k-fold splitter
     * is being used.
     */
    protected int kFoldSplitterNumFolds;

    /**
     * Turns on the ability to back off the number of features to try to
     * make a better classifier.
     */
    protected boolean enableFeatureBackoff;

    /**
     * Whether to store classifier scores in per-class saved fields.
     * Only use this if you define the fields as saved (and not indexed)
     * in an instance of the index config.  This is really only for the demo.
     */
    protected boolean storeClassifierScores;

    /**
     * Whether to store the results of failed classifications.  This
     * can be used to determine why things were not classified.
     */
    protected boolean storeNonClassified;

    /**
     * Flag to indicate if to enable the taxonomy
     * Independent of whether a lexicon is specified
     */
    protected boolean taxonomyEnabled;

    /**
     * The log for the search engine
     */
    static Logger logger = Logger.getLogger(IndexConfig.class.getName());

    /**
     * A tag that will be used for log entries
     */
    protected static String logTag = "IC";

    private ConfigurationManager cm;

    /**
     * Default constructor. Used by configuration manager
     */
    public IndexConfig() {
    }

    /**
     * Creates an index configuration for a given directory, using all of
     * the default values.
     * @param indexDir the directory where the index will be
     */
    public IndexConfig(String indexDir) {
        this.indexDir = indexDir;
    }

    /**
     * Set the configuration manager if you'd like the other mutator methods
     * in this class to make changes to the actual configuration (so that the
     * changes will be made available for saving)
     *
     * @param cm the configuration manager that created this index config
     */
    public void setConfigurationManager(ConfigurationManager cm) {
        this.cm = cm;
    }

    /**
     * Gets the index directory.
     * @return the directory containing the index for this search engine
     */
    public String getIndexDirectory() {
        return indexDir;
    }

    /**
     * Gets the name of the index.
     * @return the symbolic name for the index, or the name of the directory containing the
     * index if no symbolic name has been assigned
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Gets the map from field names to field information objects.
     * @return the map from field names to field information objects
     */
    public Map<String, FieldInfo> getFieldInfo() {
        return fieldInfo;
    }

    public String getLexiconFile() {
        return lexiconFile;
    }

    public int getRandomSplitterNumSplits() {
        return randomSplitterNumSplits;
    }

    public int getKFoldSplitterNumFolds() {
        return kFoldSplitterNumFolds;
    }

    public boolean getDoFeatureBackoff() {
        return enableFeatureBackoff;
    }

    public boolean storeClassifierScores() {
        return storeClassifierScores;
    }

    public boolean storeNonClassified() {
        return storeNonClassified;
    }

    /**
     * Creates an indexing configuration from a property sheet described in an external XML file.  A
     * description of each of the properties follows.
     *
     * @param ps the property sheet containing the properties.
     * @throws com.sun.labs.util.props.PropertyException if there is any error processing the provided properties
     */
    public void newProperties(PropertySheet ps)
            throws PropertyException {
        indexDir = ps.getString(PROP_INDEX_DIRECTORY);
        if(indexName == null) {
            indexName = indexDir;
        }
        indexName = ps.getString(PROP_INDEX_NAME);
        for(FieldInfo fi : (List<FieldInfo>) ps.getComponentList(PROP_FIELD_INFO)) {
            fieldInfo.put(fi.getName(), fi);
        }

        //
        // Get the default field information.
        defaultField = (FieldInfo) ps.getComponent(PROP_DEFAULT_FIELD_INFO);

        enableFeatureBackoff =
                ps.getBoolean(PROP_ENABLE_FEATURE_BACKOFF);
        kFoldSplitterNumFolds = ps.getInt(PROP_KFOLD_SPLITTER_NUMFOLDS);
        randomSplitterNumSplits = ps.getInt(PROP_RANDOM_SPLITTER_NUMSPLITS);
        storeClassifierScores =
                ps.getBoolean(PROP_STORE_CLASSIFIER_SCORES);
        storeNonClassified =
                ps.getBoolean(PROP_STORE_NON_CLASSIFIED);
        String l = ps.getString(PROP_LEXICON_LOCATION);
        if(l != null) {
            lexiconFile = l;
            lexicon = readLexiconFile(l);
        }
        taxonomyEnabled =
                ps.getBoolean(PROP_TAXONOMY_ENABLED);
    }

    private Lexicon readLexiconFile(String filename) {
        //
        // Initialize the Lexicon.
        Lexicon lex = new Lexicon(350000, 1000);
        lex.loadLexIndex(filename);
        /////	lexicon.loadLex(lex); // keep it packed
        lex.loadPackedLex(filename);
        lex.setTestCategories(null);
        return lex;
    }

    public Lexicon getLexicon() {
        return lexicon;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#getName()
     */
    public String getName() {
        return configName;
    }

    /**
     * Should we we using a taxonomy?
     * @return a boolean indicating if a taxonomy should be created.
     */
    public boolean taxonomyEnabled() {
        return taxonomyEnabled;
    }

    /**
     * Sets the field information to use when encountering unknown fields during
     * indexing.
     * @param fieldInfo an exemplar field information object containing the 
     * attributes and type to use when encountering unknown fields during indexing.
     * 
     */
    public void setDefaultFieldInfo(FieldInfo fieldInfo) {
        defaultField = fieldInfo.clone();
    }

    /**
     * Gets the field information to use for an unknown field.
     * 
     * @return the field information for the unknown field.
     */
    public FieldInfo getDefaultFieldInfo(String name) {
        return new FieldInfo(name, defaultField.getAttributes(),
                defaultField.getType());
    }
} // IndexConfig
