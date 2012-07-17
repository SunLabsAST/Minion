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

package com.sun.labs.minion.indexer;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.util.CharUtils;
import com.sun.labs.minion.util.FileLock;
import com.sun.labs.minion.util.FileLockException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * A class to read and write the metafile for the engine. The metafile stores 
 * information about the index, such as the defined fields and the next partition
 * number to be generated.
 */
public class MetaFile implements Iterable<FieldInfo> {

    protected static final Logger logger = Logger.getLogger(MetaFile.class.
            getName());

    protected File metaFile;

    protected FileLock metaLock;

    protected int partNumber;

    protected int nextID;

    /**
     * The number of the current term stats dictionary.
     */
    protected int currentTermStatsNumber;

    /**
     * The number of the next term stats dictionary to be dumped.
     */
    protected int nextTermStatsNumber;

    protected Map<String, FieldInfo> nameToInfo;

    protected Map<Integer, FieldInfo> idToInfo;

    /**
     * Makes a meta file.  Needs to be read before it is useful.
     * @param f the file containing the meta information for the index
     */
    public MetaFile(SearchEngineImpl engine, File f) {
        this(engine, f, null);
    }

    /**
     * Makes a meta file.  Needs to be read before it is useful.
     * @param lockDir a directory where to put a lock for the meta file
     * @param metaFile the file containing the meta information for the index
     */
    public MetaFile(SearchEngineImpl engine, File metaFile, File lockDir) {
        this.metaFile = metaFile;
        metaLock = new FileLock(lockDir, metaFile, 60, TimeUnit.SECONDS);
        nameToInfo =
                new HashMap<String, FieldInfo>();
        idToInfo =
                new TreeMap<Integer, FieldInfo>();
    }

    /**
     * Locks the meta file for exclusive use.
     * @throws com.sun.labs.minion.util.FileLockException if there is an error locking the
     * meta file
     * @throws java.io.IOException if there is an error reading the meta file
     */
    public void lock()
            throws com.sun.labs.minion.util.FileLockException, java.io.IOException {
        metaLock.acquireLock();
    }

    /**
     * Unlocks the meta file.
     * @throws com.sun.labs.minion.util.FileLockException if there is an error unlocking the
     * meta file
     * @throws java.io.IOException if there is an error unlocking the meta file
     */
    public void unlock()
            throws com.sun.labs.minion.util.FileLockException, java.io.IOException {
        metaLock.releaseLock();
    }

    /**
     * Resets the meta file.
     */
    public synchronized void reset() {
        nameToInfo.clear();
        idToInfo.clear();
    }

    /**
     * Reads the meta file.
     * @throws com.sun.labs.minion.util.FileLockException if there is an error locking the
     * meta file
     * @throws java.io.IOException if there is an error reading the meta file
     */
    public synchronized void read()
            throws com.sun.labs.minion.util.FileLockException, java.io.IOException {

        boolean releaseNeeded = false;
        if(!metaLock.hasLock()) {
            metaLock.acquireLock();
            releaseNeeded = true;
        }

        reset();

        try {
            if(metaFile.exists()) {
                RandomAccessFile ra = new RandomAccessFile(metaFile, "r");
                partNumber = ra.readInt();
                currentTermStatsNumber = ra.readInt();
                nextTermStatsNumber = ra.readInt();
                nextID = ra.readInt();
                for(int i = 0; i < nextID; i++) {
                    FieldInfo fi = new FieldInfo(ra);
                    nameToInfo.put(fi.getName(), fi);
                    idToInfo.put(fi.getID(), fi);
                }
                ra.close();
            }
        } catch(java.io.IOException ioe) {
            throw ioe;
        } finally {
            if(releaseNeeded) {
                metaLock.releaseLock();
            }
        }
    }

    /**
     * Writes the meta file.
     */
    public synchronized void write()
            throws com.sun.labs.minion.util.FileLockException, java.io.IOException {

        boolean releaseNeeded = false;
        if(!metaLock.hasLock()) {
            metaLock.acquireLock();
            releaseNeeded = true;
        }

        try {
            RandomAccessFile ra = new RandomAccessFile(metaFile, "rw");
            ra.writeInt(partNumber);
            ra.writeInt(currentTermStatsNumber);
            ra.writeInt(nextTermStatsNumber);
            ra.writeInt(idToInfo.size());
            for(FieldInfo fi : idToInfo.values()) {
                fi.write(ra);
            }
            ra.close();
        } catch(java.io.IOException ioe) {
            throw ioe;
        } finally {
            if(releaseNeeded) {
                metaLock.releaseLock();
            }
        }
    }

    /**
     * Get the number of defined fields.
     */
    public synchronized int size() {
        return idToInfo.size();
    }
    
    /**
     * Gets an array for the IDs of the fields that have the given attribute.
     * @param attr the attribute that we wish the fields to have
     * @return an array that is as wide as the number of fields. If this array
     * has a 1 in the <em>i</em><sup>th</sup> position, then the field with ID
     * <em>i</em> has this attribute.
     */
    public synchronized int[] getFieldArray(FieldInfo.Attribute attr) {
        int[] ret = new int[size() + 1];
        ret[0] = 1;
        for(FieldInfo fi : idToInfo.values()) {
            if(fi.hasAttribute(attr)) {
                ret[fi.getID()] = 1;
            }
        }
        return ret;
    }
    
    /**
     * Gets a list of the information for the fields that have a given attribute.
     *
     * @param attr the attribute that we wish the fields to have
     * @return a list of the information for the fields that have this attribute.
     */
    public synchronized List<FieldInfo> getFieldInfo(FieldInfo.Attribute attr) {
        List<FieldInfo> ret = new ArrayList<FieldInfo>();
        for(FieldInfo fi : idToInfo.values()) {
            if(fi.hasAttribute(attr)) {
                ret.add(fi);
            }
        }
        return ret;
    }

    /**
     * Gets an array for the IDs of the fields that have all of the given attributes.
     *
     * @param attrs the attributes that we wish the fields to have
     * @return an array that is as wide as the number of fields. If this array
     * has a 1 in the <em>i</em><sup>th</sup> position, then the field with ID
     * <em>i</em> has all of these attributes.
     */
    public synchronized int[] getFieldArray(EnumSet<FieldInfo.Attribute> attrs) {
        int[] ret = new int[size() + 1];
        ret[0] = 1;
        for(FieldInfo fi : idToInfo.values()) {
            if(fi.getAttributes().containsAll(attrs)) {
                ret[fi.getID()] = 1;
            }
        }
        return ret;
    }

    /**
     * Gets a list of the information for the fields that have all of the given
     * attributes.
     *
     * @param attrs the attributes that we wish the fields to have
     * @return a list of the information for the fields that have all of these
     * attributes.
     */
    public synchronized List<FieldInfo> getFieldInfo(EnumSet<FieldInfo.Attribute> attrs) {
        List<FieldInfo> ret = new ArrayList<FieldInfo>();
        for(FieldInfo fi : idToInfo.values()) {
            if(fi.getAttributes().containsAll(attrs)) {
                ret.add(fi);
            }
        }
        return ret;
    }

    /**
     * Gets an array of field IDs suitable for use in postings iterators.
     * 
     * @param fieldName the name of the field 
     * @return an array of integers, as wide as the number of fields, with
     * 1s in the elements for the provided field.
     */
    public int[] getFieldArray(String fieldName) {
        int[] ret = new int[size() + 1];
        if(fieldName == null) {
            ret[0] = 1;
        } else {
            FieldInfo fi = getFieldInfo(fieldName);
            if(fi != null) {
                ret[fi.getID()] = 1;
            }
        }
        return ret;
    }
    
    /**
     * Gets an array of field IDs suitable for use in postings entry
     * iterators.
     *
     * @param fieldNames The names of the fields that we're interested in.
     * If <code>null</code> is given as a field value, then words that
     * don't occur in any field are what we're interested in.
     * @return an array of integers, as wide as the number of fields, with
     * 1s in the elements for the provided fields, or <code>null</code> if
     * the given array of field names is null.
     */
    public int[] getFieldArray(String[] fieldNames) {
        if(fieldNames == null) {
            return null;
        }

        int[] ret = new int[size() + 1];
        for(int i = 0; i < fieldNames.length; i++) {
            if(fieldNames[i] == null) {
                ret[0] = 1;
                continue;
            }
            FieldInfo fi = getFieldInfo(fieldNames[i]);
            if(fi != null) {
                ret[fi.getID()] = 1;
            }
        }
        return ret;
    }

    /**
     * Gets an array of field multipliers suitable for use in postings
     * entry iterators.
     *
     * @param fieldNames The names of the fields that we're interested in.
     * If a <code>null</code> field name is passed, that multiplier will be
     * used for occurrences that are not in any field.
     * @param mult The multipliers for the named fields, in the order that
     * the fields are given in.
     * @return an array as wide as the number of defined fields with the
     * multipliers specified by field ID.
     */
    public float[] getMultArray(String[] fieldNames, float[] mult) {

        if(fieldNames == null || mult == null) {
            return null;
        }

        float[] ret = new float[size() + 1];
        for(int i = 0; i < ret.length; i++) {
            ret[i] = 1;
        }
        for(int i = 0; i < fieldNames.length; i++) {
            if(fieldNames[i] == null) {
                ret[i] = mult[0];
            }
            FieldInfo fi = getFieldInfo(fieldNames[i]);
            if(fi != null) {
                ret[fi.getID()] = mult[i];
            }
        }
        return ret;
    }

    /**
     * Gets an array for field multipliers suitable for use in postings
     * iterators.
     */
    public float[] getMultArray(Map<String,Float> mult) {
        if(mult == null || mult.isEmpty()) {
            return null;
        }

        float[] ret = new float[size() + 1];

        for(Map.Entry<String,Float> e : mult.entrySet()) {
            String name = e.getKey();
            Float val = e.getValue();
            if(name == null) {
                ret[0] = val.floatValue();
            } else {
                FieldInfo fi = getFieldInfo(name);
                if(fi != null) {
                    ret[fi.getID()] = val.floatValue();
                }
            }
        }
        return ret;
    }

    /**
     * Tests whether the meta file exists yet.
     */
    public synchronized boolean exists() {
        return metaFile.exists();
    }


    /**
     * Gets the next valid partition number.
     */
    public synchronized int getNextPartitionNumber()
            throws java.io.IOException, com.sun.labs.minion.util.FileLockException {
        lock();
        read();
        partNumber++;
        write();
        unlock();
        return partNumber;
    }

    /**
     * Gets the latest valid partition number.
     */
    public synchronized int getPartitionNumber()
            throws java.io.IOException, com.sun.labs.minion.util.FileLockException {
        lock();
        read();
        unlock();
        return partNumber;
    }

    /**
     * Gets the next term stats dictionary file
     * 
     * @return the next valid number for a term stats dictionary.
     */
    public synchronized int getNextTermStatsNumber()
            throws java.io.IOException, com.sun.labs.minion.util.FileLockException {
        lock();
        read();
        nextTermStatsNumber++;
        write();
        unlock();
        return nextTermStatsNumber;
    }

    /**
     * Gets the latest valid term stats file number.
     * 
     * @return the current valid term stats file number
     */
    public synchronized int getTermStatsNumber()
            throws java.io.IOException, com.sun.labs.minion.util.FileLockException {
        lock();
        read();
        unlock();
        return currentTermStatsNumber;
    }

    /**
     * Sets the latest valid term stats file number.
     */
    public synchronized void setTermStatsNumber(int n)
            throws java.io.IOException, com.sun.labs.minion.util.FileLockException {
        lock();
        currentTermStatsNumber = n;
        write();
        unlock();
    }

    /**
     * Defines a field.  If the field already exists, the previously defined field
     * must have the same attributes and type as the provided field.
     *
     * @param fi A partially specified field information object.
     * @return the fully defined field information object
     * @throws com.sun.labs.minion.SearchEngineException if there is any error
     * defining the field, in particular, if the provided field attempts to
     * redefine the attributes or type associated with an already existing field.
     */
    public synchronized FieldInfo defineField(FieldInfo fi)
            throws SearchEngineException {
        FieldInfo ret = nameToInfo.get(fi.getName());

        if(ret == null) {
            ret = addField(fi);
        }

        return ret;
    }

    /**
     * Gets the information associated with a given field name.
     *
     * @param name The name of the field, or <code>null</code> for the no-field
     * field.
     * @return The associated information, or <code>null</code> if the
     * named field doesn't exist.
     */
    public synchronized FieldInfo getFieldInfo(String name) {
        if(name == null) {
            return null;
        }
        return nameToInfo.get(CharUtils.toLowerCase(name));
    }
    
    /**
     * Gets the information associated with a number of field names.
     */
    public Collection<FieldInfo> getFieldInfo(String[] names) {
        Set<FieldInfo> ret = new HashSet<FieldInfo>();
        for(String name : names) {
            ret.add(getFieldInfo(name));
        }
        return ret;
    }

    /**
     * Gets the information associated with a number of field names.
     */
    public Collection<FieldInfo> getFieldInfo(Collection<String> names) {
        Set<FieldInfo> ret = new HashSet<FieldInfo>();
        for(String name : names) {
            ret.add(getFieldInfo(name));
        }
        return ret;
    }

    /**
     * Gets the information associated with a given field id.
     *
     * @param id The ID of the field.
     * @return The associated information, or <code>null</code> if the
     * given field doesn't exist.
     */
    public synchronized FieldInfo getFieldInfo(int id) {
        return idToInfo.get(new Integer(id));
    }
    
    /**
     * Gets the ID corresponding to a vectored field with the given name.  This
     * method is a helper that can be used by a number of document similarity 
     * computations that are computing similarity based on a certain component
     * of a document vector.
     * 
     * @param name the name of the vectored field whose ID we want.
     * @return If <code>name</code> is <code>null</code>, then -1 is returned, which callers may
     * interpret as a request for the data from all vectored fields.  If <code>name</code> is the empty string,
     * then 0 is returned, which callers may interpret as a request for data from the unnamed body field.  
     * If <code>name</code> names a vectored field, then the ID for that field is returned.
     * If <code>name</code> does not name a known field, or if the field that it names
     * is not a vectored field, then a warning will be logged
     * and a default value of 0 will be returned.
     */
    public int getVectoredFieldID(String name) {
       if(name == null) {
            return -1;
        } else if(name.equals("")) {
            return 0;
        } else {
            FieldInfo fi = getFieldInfo(name);
            if(fi == null || !fi.hasAttribute(FieldInfo.Attribute.VECTORED)) {
                logger.warning(name + " is unknown or not a vectored field");
                return 0;
            } else {
                return fi.getID();
            }
        }        
    }

    /**
     * Gets an iterator for the field information in field ID order.
     * @return an iterator for the field values in the meta file
     */
    public synchronized Iterator<FieldInfo> fieldIterator() {
        return (new ArrayList(idToInfo.values())).iterator();
    }

    @Override
    public Iterator<FieldInfo> iterator() {
        return fieldIterator();
    }
    
    /**
     * Adds a field to the field map.
     *
     * @param fi A field information object.
     * @return A field information object with the field ID filled in.
     * @throws SearchEngineException if there is any error
     * defining the field, in particular, if the provided field attempts to
     * redefine the attributes or type associated with an already existing field.
     */
    public FieldInfo addField(FieldInfo fi)
            throws SearchEngineException {
        return addField(fi.getName(), fi.getAttributes(), fi.getType(), fi.getPipelineFactoryName());
    }

    /**
     * Adds a field to the field map.  This will log a warning and do
     * nothing if you attempt to add a field that already exists with
     * different attributes or type!
     *
     * @param name The name of the field.
     * @param attributes The set of attributes for the field
     * @param type The type of the field.
     * @return the information associated with the field.
     * @throws com.sun.labs.minion.SearchEngineException if there is any error
     * adding the new field.
     */
    public synchronized FieldInfo addField(String name,
                                           EnumSet<FieldInfo.Attribute> attributes,
                                           FieldInfo.Type type,
                                           String pipelineFactoryName)
            throws SearchEngineException {
        try {

            FieldInfo fi = getFieldInfo(name);

            //
            // If we already have a definition for this field, make sure that they're
            // not trying to change the attributes or type.
            if(fi != null) {
                if(!attributes.equals(fi.getAttributes()) || type != fi.getType()) {

                    throw new SearchEngineException(String.format(
                            "Attempt to redefine field: %s old type: %s "
                            + "old attr: %s new type: %s new attr: %s ignoring redefinition",
                                                                  name, fi.
                            getType(), fi.getAttributes(), type, attributes));
                }
                return fi;
            }

            //
            // We didn't find the field, we need to synchronize with the
            // version on disk, look for it, and then write the map back.
            lock();
            read();
            fi = getFieldInfo(name);
            if(fi == null) {

                //
                // We need to make a new field and put in in the maps and then
                // write the file to disk.
                fi = new com.sun.labs.minion.FieldInfo(++nextID, name,
                                                       attributes, type, pipelineFactoryName);
                nameToInfo.put(name, fi);
                idToInfo.put(fi.getID(), fi);
                write();
            }
            unlock();
            return fi;
        } catch(FileLockException ex) {
            throw new SearchEngineException("Error locking meta file " +
                                            "when adding " + name, ex);
        } catch(IOException ex) {
            throw new SearchEngineException("Error reading meta file " +
                                            "when adding " + name, ex);
        }
    }

    @Override
    public String toString() {
        StringBuilder temp = new StringBuilder();
        temp.append(metaFile.toString()).append(": ").append(partNumber).
                append(" ").append(currentTermStatsNumber).append(" ").append(nextTermStatsNumber);
        for(Iterator i = fieldIterator(); i.hasNext();) {
            temp.append("\n").append(i.next());
        }
        return temp.toString();
    }

    public static void main(String[] args)
            throws com.sun.labs.minion.util.FileLockException, java.io.IOException {

        MetaFile mf = new MetaFile(null, new File(args[0]));
        mf.read();
        if(args.length >= 3) {
            FieldInfo fi = mf.getFieldInfo(args[1]);
            fi.setName(args[2]);
            mf.write();
        } else {
            if(args.length > 1) {
                mf.setTermStatsNumber(Integer.parseInt(args[1]));
                mf.write();
            }
        }
        System.out.println(mf);
    }
} // MetaFile
