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

package com.sun.labs.minion.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Properties;

import com.sun.labs.minion.MetaDataStore;

/**
 * The MetaDataStore provides a mechanism to store name/value pairs
 * in associate with an index.  The search engine does not store any
 * data of its own in the MetaDataStore.  This service exists to allow
 * applications that use the search engine to store a few values
 * in the index directory in a safe way for the engine.  Changes are
 * persisted at the same time that the engine dumps in-memory data
 * to disk.
 */

public class MetaDataStoreImpl implements MetaDataStore
{
    protected File metaDataFile;

    protected Properties prop;
    
    public MetaDataStoreImpl(String indexDir) throws IOException {
        metaDataFile = new File(indexDir + "/meta.data");
        prop = new Properties();
        if (metaDataFile.exists()) {
            prop.load(new FileInputStream(metaDataFile));
        }
    }

    public synchronized void setProperty(String name, String value) {
        prop.setProperty(name, value);
    }

    public String getProperty(String name) {
        return prop.getProperty(name);
    }

    public String getProperty(String name, String defaultValue) {
        return prop.getProperty(name, defaultValue);
    }
    
    public void store() throws IOException {
        prop.store(new FileOutputStream(metaDataFile),
                   "Application MetaData -- do not edit");
    }
}
