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

import com.sun.labs.util.props.ConfigurationManager;
import com.sun.labs.util.props.PropertyException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * This is a factory class that can be used to get a search engine instance.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class SearchEngineFactory {

    /**
     * The name of the global property in the configuration containing the index directory.
     */
    public static final String GLOBAL_INDEX_DIRECTORY = "index_directory";

    /**
     * The configuration name of the default search engine.
     */
    public static final String DEFAULT_ENGINE = "search_engine";

    static Logger logger = Logger.getLogger(SearchEngineFactory.class.getName());

    public static final String logTag = "SEF";

    public static final String[] configFiles = {"standardConfig.xml",
        "dictionaryConfig.xml", "partitionConfig.xml",
        "pipelineConfig.xml",
        "classifierConfig.xml"};

    /**
     * Creates a SearchEngineFactory
     */
    public SearchEngineFactory() {
    }

    /**
     * Gets a search engine for the index in the provided directory.  This engine
     * will be configured with a set of defaults.
     * @param indexDir the directory containing the index that the search engine will use
     * @return a search engine for this directory
     * @throws SearchEngineException if there is any error opening the index
     */
    public static SearchEngine getSearchEngine(String indexDir)
            throws SearchEngineException {
        return getSearchEngine(indexDir, DEFAULT_ENGINE);
    }

    /**
     * Gets a search engine that combines the given configuration management
     * file with the default.
     *
     * @param indexDir the directory where the index is.  If this value is <code>null</code>,
     * the value should be specified in the configuration file!
     * @param engineName the configuration name of the search engine that should
     * be instantiated
     * @return a search engine for the given index directory and configuration file
     * @throws com.sun.labs.minion.SearchEngineException if there is an error
     * opening the search engine
     */
    public static SearchEngine getSearchEngine(String indexDir,
            String engineName)
            throws SearchEngineException {
        return getSearchEngine(indexDir, engineName, null);
    }

    /**
     * Gets a search engine that combines the given configuration management
     * file with the default.
     *
     * @param indexDir the directory where the index is.  If this value is <code>null</code>,
     * the value should be specified in the configuration file!
     * @param configFile a URL that points to a configuration file for the engine.  This value may be <code>null</code>.
     * @return a search engine for the given index directory and configuration file
     * @throws com.sun.labs.minion.SearchEngineException if there is an error
     * opening the search engine
     */
    public static SearchEngine getSearchEngine(String indexDir,
            URL configFile)
            throws SearchEngineException {
        return getSearchEngine(indexDir, DEFAULT_ENGINE, configFile);
    }

    /**
     * Gets a search engine that combines the given configuration management
     * file with the default.
     *
     * @param indexDir the directory where the index is.  If this value is <code>null</code>,
     * the value should be specified in the configuration file!
     * @param engineName the configuration name of the search engine that should
     * be instantiated
     * @param configFile a URL that points to a configuration file for the engine.  This value may be <code>null</code>.
     * @return a search engine for the given index directory and configuration file
     * @throws com.sun.labs.minion.SearchEngineException if there is an error
     * opening the search engine
     */
    public static SearchEngine getSearchEngine(String indexDir,
            String engineName,
            URL configFile)
            throws SearchEngineException {
        ConfigurationManager cm;
        try {
            File f = getDefaultConfigFile(indexDir);
            if(f != null && f.exists()) {

                //
                // We'll override the default configuration with the index directory
                // configuration.
                cm = getDefaultConfiguration(indexDir);
                try {
                    cm.addProperties(f.toURI().toURL());
                } catch(IOException ioe) {
                    logger.warning("Error loading index config file: " + f +
                            " Continuing without it");
                }

                //
                // Check for an engine name in the configuration and make sure
                // it matches up with what we were given.
                String en = cm.getGlobalProperty("engine_name");
                if(en != null) {
                    if(!en.equals(engineName)) {
                        logger.warning("getSearchEngine using inconsistent engine names.  Given \"" +
                                engineName + "\" config contains: \"" + en +
                                "\" using config name!");
                    }
                    engineName = en;
                }

                //
                // We'll silently allow override of the index directory, so that
                // indices are portable!
                if(indexDir != null) {
                    cm.setGlobalProperty(GLOBAL_INDEX_DIRECTORY, indexDir);
                }
            } else {
                cm = getDefaultConfiguration(indexDir);
                cm.setGlobalProperty("engine_name", engineName);
            }
            if(configFile != null) {
                cm.addProperties(configFile);
                if(indexDir != null) {
                    cm.setGlobalProperty(GLOBAL_INDEX_DIRECTORY, indexDir);
                }
            }
            checkConfigurationManager(cm);

            SearchEngine se =
                    (SearchEngine) cm.lookup(engineName);
            if(se == null) {
                throw new SearchEngineException("Unable to find configuration: " +
                        engineName);
            }
            return se;
        } catch(IOException ex) {
            throw new SearchEngineException("Error creating search engine", ex);
        } catch(PropertyException ex) {
            throw new SearchEngineException("Error creating search engine", ex);
        }
    }

    public static SearchEngine getSearchEngine(ConfigurationManager cm)
            throws SearchEngineException {
        try {
            checkConfigurationManager(cm);
            SearchEngine se =
                    (SearchEngine) cm.lookup("search_engine");
            return se;
        } catch(PropertyException ex) {
            throw new SearchEngineException("Error creating search engine", ex);
        }
    }

    /**
     * Gets the default configuration for an index in the given directory.
     * @param indexDir the index directory containing the config
     * @return the configuration manager for that index
     * @throws com.sun.labs.minion.SearchEngineException if there is an error
     * getting the configuration
     */
    public static ConfigurationManager getDefaultConfiguration(String indexDir)
            throws SearchEngineException {
        try {
            ConfigurationManager cm = new ConfigurationManager();
            for(int i = 0; i < configFiles.length;
                    i++) {
                URL config =
                        (new com.sun.labs.minion.SearchEngineFactory()).getClass().
                        getResource(configFiles[i]);
                cm.addProperties(config);
            }
            //
            // Set the global index directory property.
            if(indexDir != null) {
                cm.setGlobalProperty(GLOBAL_INDEX_DIRECTORY, indexDir);
            }
            return cm;
        } catch(IOException ex) {
            throw new SearchEngineException("Error reading configuration file in " +
                    indexDir, ex);
        } catch(PropertyException ex) {
            throw new SearchEngineException("Error processing configuration file in " +
                    indexDir, ex);
        }
    }

    /**
     * Checks to make sure that the configuration defines the index directory.
     * @param cm the configuration manager that we want to check
     * @throws com.sun.labs.minion.SearchEngineException if there is a problem with
     * the configuration
     */
    private static void checkConfigurationManager(ConfigurationManager cm)
            throws SearchEngineException {
        if(cm.getGlobalProperty(GLOBAL_INDEX_DIRECTORY) == null) {
            throw new SearchEngineException("Index directory must be defined");
        }
    }

    public static File getDefaultConfigFile(String indexDir) {
        return indexDir == null ? null
                : new File(indexDir + File.separatorChar +
                "config.xml");
    }
}
