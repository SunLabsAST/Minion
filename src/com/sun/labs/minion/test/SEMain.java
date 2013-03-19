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

package com.sun.labs.minion.test;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.Indexable;
import com.sun.labs.minion.IndexableFile;
import com.sun.labs.minion.IndexableMap;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import java.util.Date;
import java.util.EnumSet;

/**
 * A base class for all of our sample programs.
 */
public class SEMain  {
    
    /**
     * Define the fields that will be used by makeDocument
     * 
     * @param engine the search engine in which to define the fields
     */
    public static void defineFields(SearchEngine engine)
        throws SearchEngineException
    {
        EnumSet saved = EnumSet.of(FieldInfo.Attribute.SAVED);
        engine.defineField(new FieldInfo("enc", saved, FieldInfo.Type.STRING));
        engine.defineField(new FieldInfo("last-mod", saved, FieldInfo.Type.DATE));
        EnumSet regular = FieldInfo.getIndexedAttributes();
        engine.defineField(new FieldInfo("file", regular));
        engine.defineField(new FieldInfo("filename", saved, FieldInfo.Type.STRING));
    }

    /**
     * Makes a document suitable for indexing or highlighting using a search
     * engine. 
     */
    public static Indexable makeDocument(IndexableFile f) {

        //
        // We'll use a sequenced map because we want to process things
        // the same way, every time!
        IndexableMap document = new IndexableMap(f.getAbsolutePath());

        //
        // Store the encoding that we're using so that we know how to open
        // it again later when we want to highlight.
        document.put("enc", f.getEncoding());

        //
        // Put the date in the map.
        document.put("last-mod", new Date(f.lastModified()));

        //
        // Finally, put in the file to index.  We won't be saving the
        // data into an explicit field, but that is possible.
        document.put("file", f);
        document.put("filename", f.toString());

        return document;
    }
    
} // SEMain
