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

import java.io.File;

/**
 * An extension of <code>File<code> that stores the encoding used for the
 * file.  If no encoding is provided by the application, then the system
 * default encoding is used, if it can be determined.  If that encoding
 * cannot be determined, then an encoding of UTF-8 will be used.
 */
public class IndexableFile extends File {

    /**
     * The encoding for the file
     */
    protected String encoding;

    /**
     * The exact path that we were passed, so we can handle things that aren't
     * files, like zip archives.
     */
    protected String exactPath;

    /**
     * Creates a new IndexableFile that is the child of a parent IndexableFile.
     * @param parent The IndexableFile that is the parent of the newly created IndexableFile
     * @param child The name of the IndexableFile that is the child of the existing parent
     */
    public IndexableFile(IndexableFile parent, String child) {
        super(parent, child);
        encoding = parent.encoding;
        exactPath = parent.exactPath + pathSeparatorChar + child;
    }

    /**
     * Creates an IndexableFile from an absolute pathname
     * @param pathname An absolute pathname for an IndexableFile
     */
    public IndexableFile(String pathname) {
        super(pathname);

        exactPath = pathname;

        //
        // Try to get the default encoding.
        try {
            encoding =
                    System.getProperties().getProperty("file.encoding");
        } catch(Exception e) {
            encoding = "utf-8";
        }
    }

    /**
     * Creates an indexable file for an absolute pathname with a specified encoding
     * @param pathname An absolute pathname for an IndexableFile
     * @param encoding The encoding for the file
     */
    public IndexableFile(String pathname, String encoding) {
        super(pathname);
        exactPath = pathname;
        this.encoding = encoding;
    }

    /**
     * Creates a new IndexableFile that is the child of a parent IndexableFile
     * with a specified encoding.
     * @param parent The IndexableFile that is the parent of the newly created IndexableFile
     * @param child The name of the IndexableFile that is the child of the existing parent
     * @param encoding The encoding for the indexable file
     */
    public IndexableFile(String parent, String child, String encoding) {
        super(parent, child);
        exactPath = parent + pathSeparatorChar + child;
        this.encoding = encoding;
    }

    /**
     * Gets the encoding value.
     * @return the encoding value.
     */
    public String getEncoding() {
        if(encoding == null) {
            return "utf-8";
        }
        return encoding;
    }

    /**
     * Sets the encoding value.
     * @param encoding The new encoding value.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Returns the pathname for the file, exactly as it was passed.
     * @return a string that provides the exact pathname of the indexablefile
     */
    public String getExactPathname() {
        return exactPath;
    }
} // IndexableFile
