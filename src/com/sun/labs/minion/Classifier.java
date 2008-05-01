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

/**
 * This interface represents the classification features of
 * the search engine.  Presumably, SearchEngine will implement
 * this interface (in addition to Searcher which it already
 * implements).
 *
 */

public interface Classifier
{

    /**
     * 
     * Generates a classifier based on the documents in the provided
     * result set.  If the name provided is an existing class, then
     * the existing classifier will be replaced.  This method does not
     * affect any documents that have already been indexed.
     * @param results the set of documents to use for training the classifier
     * @param className the name of the class to create or replace
     * @param fieldName the name of the field where the results of the classifier
     * should be stored.
     * @throws com.sun.labs.minion.SearchEngineException If there is any error training the classifier
     */
    public void trainClass(ResultSet results, String className, String fieldName)
        throws SearchEngineException;

    /**
     * 
     * Generates a classifier based on the documents in the provided
     * result set.  If the name provided is an existing class, then
     * the existing classifier will be replaced.  This method does not
     * affect any documents that have already been indexed.
     * @param results the set of documents to use for training the classifier
     * @param className the name of the class to create or replace
     * @param fieldName the name of the field where the results of the classifier
     * should be stored.
     * @param fromField the vectored field from which we should build the classifiers.
     * If this parameter is <code>null</code> then data from all indexed fields will be used.  If this
     * parameter is the empty string, then data from the "body" field will be used.
     * @throws com.sun.labs.minion.SearchEngineException If there is any error training the classifier
     */
    public void trainClass(ResultSet results, String className, String fieldName,
            String fromField)
        throws SearchEngineException;

     /**
     * 
     * Generates a classifier based on the documents in the provided
     * result set.  If the name provided is an existing class, then
     * the existing classifier will be replaced.  This method does not
     * affect any documents that have already been indexed.
     * @param results the set of documents to use for training the classifier
     * @param className the name of the class to create or replace
     * @param fieldName the name of the field where the results of the classifier
     * should be stored.
     * @param p a progress monitor that will be notified as training proceeds
      * @throws com.sun.labs.minion.SearchEngineException If there is any error training the classifier
     */
    public void trainClass(ResultSet results, String className, String fieldName,
            Progress p)
        throws SearchEngineException;
   
    /**
     * 
     * Generates a classifier based on the documents in the provided
     * result set.  If the name provided is an existing class, then
     * the existing classifier will be replaced.  This method does not
     * affect any documents that have already been indexed.
     * @param results the set of documents to use for training the classifier
     * @param className the name of the class to create or replace
     * @param progress where to send progress events
     * @param fieldName the name of the field where the results of the classifier
     * should be stored.
     * @param fromField the vectored field from which we should build the classifiers.
     * If this parameter is <code>null</code> then data from all indexed fields will be used.  If this
     * parameter is the empty string, then data from the "body" field will be used.
     * @throws com.sun.labs.minion.SearchEngineException If there is any error training the classifier
     */
    public void trainClass(ResultSet results, String className, String fieldName, 
            String fromField, Progress progress)
        throws SearchEngineException;



    /**
     * 
     * Causes the engine to reclassify all documents against the classifier
     * for the given class name.  Upon completion of the classification, a
     * short pause will occur while switching from the old set of classes
     * to the new set (the implementation of this will determine exactly
     * what the characteristics of the switch are).  This method is only needed
     * when there are existing indexed documents and there has been a change
     * to the set of classifiers.  Since reclassifying will likely be a
     * lengthy process, it is never implicit in any of the other methods.
     * (Side note: Should this be a blocking call?  If not, should there be
     * a simple event/callback mechanism to notify a user of progress?)
     * @param className the class to reclassify all documents against
     * @throws com.sun.labs.minion.SearchEngineException If there is any error training the classifiers
     */
    public void reclassifyIndex(String className)
        throws SearchEngineException;


    /**
     * 
     * Creates a manual assignment of a set of documents to a set of classes.
     * All of the documents will be assigned to all of the classes.  Manual
     * assignments are stored independently of the automatic assignment the
     * engine performs while indexing.  The documents will also automatically
     * be indexed and classified.
     * @param docKeys the keys of the documents to classify
     * @param classNames the classes to assign the documents to
     * @throws com.sun.labs.minion.SearchEngineException if there is any error running the classifiers
     */
    public void classify(String[] docKeys, String[] classNames)
        throws SearchEngineException;
    
    /**
     * 
     * Returns the set of documents that was used to train the classifier
     * for the class with the provided class name.  (Note: Depending on how
     * the Document interface shapes up, maybe this method should return
     * Document[] instead of ResultSet?)
     * @return the set of documents that defines the named class
     * @param className the name of a class
     * @throws com.sun.labs.minion.SearchEngineException If there is any error retrieving the training documents
     */
    public ResultSet getTrainingDocuments(String className)
        throws SearchEngineException;


    /** 
     * Returns the names of the classes for which classifiers are defined.
     * If no classes are defined, an empty array is returned.
     * 
     * @return an array of class names
     */
    public String[] getClasses();
}
