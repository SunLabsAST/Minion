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

package com.sun.labs.minion.test.regression.merge;

import java.io.IOException;

import com.sun.labs.minion.SearchEngineException;

/**
 * This interface is used by MergeTest (and its subclasses) to report the results of the test.
 * @author Bernard Horan
 *
 */
interface TestReporter {

    /**
     * Begin the test report. 
     * @throws IOException
     */
    public abstract void startReport() throws IOException;

    /**
     * Outputs the name of each file.
     * @param testFiles an array of Strings containing the names of the files
     * @throws IOException
     */
    public abstract void reportTestFiles(String[] testFiles) throws IOException;

    /**
     * End the test report. 
     * @throws IOException
     */
    public abstract void endReport() throws IOException;

    /**
     * Start the <em>i</em>th iteration. 
     * @param i the count of the iteration
     * @throws IOException
     */
    public abstract void startIteration(int i) throws IOException;

    /**
     * End the iteration.
     * @throws IOException
     */
    public abstract void endIteration() throws IOException;

    /**
     * Start indexing.
     * @throws IOException
     */
    public abstract void startIndexing() throws IOException;

    /**
     * Index a file and indicate its success at being indexed.
     * @param file the name of the file indexed
     * @param success a boolean indicating the success of the index of the file
     * @throws IOException
     */
    public abstract void index(String file, boolean success) throws IOException;

    /**
     * End indexing.
     * @throws IOException
     */
    public abstract void endIndexing() throws IOException;

    /**
     * Start invertng
     * @throws IOException
     */
    public abstract void startInverting() throws IOException;

    /**
     * End inverting
     * @throws IOException
     */
    public abstract void endInverting() throws IOException;

    /**
     * Report an exception while inverting
     * @param e the exception to be reported
     * @throws IOException
     */
    public abstract void invertException(Exception e) throws IOException;

    /**
     * Start diff.
     * @throws IOException
     */
    public abstract void startDiff() throws IOException;

    /**
     * Report the output from diff
     * @param outputPathname the name of the file compared
     * @param b the result of the comparison. False if different.
     * @throws IOException 
     */
    public abstract void reportDiff(String outputPathname, boolean b) throws IOException;

    /**
     * Reports the end of diff
     * @throws IOException
     */
    public abstract void endDiff() throws IOException;

    /**
     * Start optimising
     * @throws IOException
     */
    public abstract void startOptimizing() throws IOException;

    /**
     * End optimising
     * @throws IOException
     */
    public abstract void endOptimizing() throws IOException;

    /**
     * Reports an exception while optimising
     * @param e a SearchEngineException
     * @throws IOException
     */
    public abstract void optimizeException(SearchEngineException e)
            throws IOException;

    /**
     * Sets the name of the file onto which the reporter should write its report
     * @param reportFileName the name of a file
     */
    public abstract void setReportFilename(String reportFileName);

    /**
     * Report the number of missing files following inversion
     * @param i the number of missing files
     * @throws IOException 
     */
    public abstract void startReportMissingFiles(int i) throws IOException;

    /**
     * End the report of missing files
     * @throws IOException 
     */
    public abstract void endReportMissingFiles() throws IOException;

    /**
     * Report a missing file following inversion
     * @param filename the name of the missing file
     * @throws IOException 
     */
    public abstract void missingFile(String filename) throws IOException;

    

}
