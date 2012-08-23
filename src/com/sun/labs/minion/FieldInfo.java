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

import com.sun.labs.util.props.ConfigEnum;
import com.sun.labs.util.props.ConfigEnumSet;
import com.sun.labs.util.props.ConfigString;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that can be used to tell the indexer what to do with the data
 * contained in a field.
 *
 * <p>
 *
 * Fields can be defined in the configuration file used to create a search
 * engine or via the {@link com.sun.labs.minion.SearchEngine#defineField}
 * method.
 *
 * <p>
 *
 * Each field has a name, which is a string. Note that field names are <em>case
 * insensitive</em>. This means, for example, that
 * <code>title</code>,
 * <code>Title</code>, and
 * <code>TITLE</code> will be considered the same.
 *
 * <p>
 *
 * The disposition of the field data by the indexer is controlled by the
 * attributes that are assigned to the field. The following attributes are
 * defined by the {@link Attribute} enumeration:
 *
 * <dl>
 *
 * <dt><code>TOKENIZED</code></dt> <dd>The field value should be tokenized. If
 * this attribute is set, then the data in the field will be tokenized according
 * to the rules of whatever tokenizer is currently being used. Typically, this
 * means that the data in the field will be broken into tokens at spaces and
 * punctuation. </dd>
 *
 * <dt><code>INDEXED</code></dt>
 *
 * <dd>Any terms in the field (whether the field is tokenized or un-tokenized)
 * will have entries added to the main dictionary and postings data added to the
 * postings file for that dictionary. Fields that specify this attribute can be
 * specified in queries that use the
 * <code>&lt;contains&gt;</code> operator. For example, if the
 * <code>title</code> field has the
 * <code>INDEXED</code> attribute, then the query: <blockquote>
 * <code>title &lt;contains&gt; java</code> </blockquote> will return those
 * documents that have the term
 * <code>java</code> in the
 * <code>title</code> field. </dd>
 *
 * <dt><code>VECTORED</code></dt>
 *
 * <dd> This attribute indicates that terms extracted from the field value
 * should be added to a document vector specific to the field, as well as to the
 * overall document vector for this document. Specifying this attribute allows
 * applications to perform classification or document similarity computations
 * against just this field. So, for example, you could find a set of documents
 * that have titles similar to a given document's title. </dd>
 *
 * <dt><code>TRIMMED</code></dt>
 *
 * <dd> This attribute indicates that field values passed into the indexer
 * should have any leading or trailing spaces trimmed from the values before
 * they are processed any further. </dd>
 *
 * <dt><code>CASED</code></dt>
 *
 * <dd> This attribute indicates that a given string field should be stored so
 * that case sensitive and case insensitive queries can be run against it. If a
 * string field does not have this attribute set, then queries against that
 * field will be run in a case insensitive manner. This attribute applies to
 * both the saved field values as well as the tokens extracted from the field
 * when the
 * <code>TOKENIZED</code> attribute is set. </dd>
 *
 * <dt><code>SAVED</code></dt>
 *
 * <dd>This attribute indicates that the value for the field should be stored in
 * the index exactly as it provided. Values that are in saved fields are
 * available for parametric searches (e.g.,
 * <code>price &lt; 10</code>) and for results sorting. If
 * <code>SAVED</code> is specified as an attribute, then <em>one</em> of the
 * following types <em>must</em> be specified:
 *
 * <dl>
 *
 * <dt><code>INTEGER</code></dt>
 *
 * <dd> Some quantity storable in a 64 bit integer.</dd>
 *
 * <dt><code>FLOAT</code></dt>
 *
 * <dd>Some quantity storable in a 64 bit double.</dd>
 *
 * <dt><code>DATE</code></dt>
 *
 * <dd>The field value is a date, given in some text representation. This date
 * will be parsed and then stored in a Java long as the number of milliseconds
 * since the epoch (00:00:00 GMT, January 1, 1970).</dd>
 *
 * <dt><code>STRING</code></dt>
 *
 * <dd>A text field that consists of a variable number of characters. The
 * default string field is the empty string.</dd>
 *
 * </dl> </dl>
 *
 * <p>
 *
 * The attributes of the
 * <code>FieldInfo</code> object can be set by providing an
 * {@link java.util.EnumSet} to the constructor, or by using the
 * <code>setAttribute</code> method.
 *
 */
public class FieldInfo implements Cloneable, Configurable {

    protected static final Logger logger = Logger.getLogger(FieldInfo.class.
            getName());

    /**
     * The various attributes that a field can have. Any field may specify any
     * combination of these attributes.
     */
    public enum Attribute {

        /**
         * Values for the field will be broken into tokens. The universal
         * tokenizer is used by default.
         */
        TOKENIZED,
        /**
         * Values in the field will be placed into the main dictionary, for use
         * as query terms. This is useful in conjunction with the
         * <code>TOKENIZED</code> attribute.
         */
        INDEXED,
        /**
         * Positions for the tokens in this field will be stored in the index
         */
        POSITIONS,
        /**
         * The values tokenized from this field will be stored in a document
         * vector for this field, which will allow document similarity
         * computations to be done using the data from this field.
         */
        VECTORED,
        /**
         * When things are added to the field, they will be stored with their
         * original case intact.
         */
        CASED,
        /**
         * When things are added to the field, they will be stored in an uncased
         * way so that they can be looked up using any case.
         */
        UNCASED,
        /**
         * When tokens are indexed into the field, the tokens will be stored as
         * stems. At query time, search terms will be stemmed as well.
         */
        STEMMED,
        /**
         * When tokens are indexed into the field, the tokens will be stored
         * unstemmed. At query time, search terms will be expanded.
         */
        UNSTEMMED,
        /**
         * The values for this field will be stored as-is in the index for use
         * in relational queries or for sorting search results.
         */
        SAVED,
        /**
         * This field, if it is INDEXED will be a default field for searches if
         * no other field information is provided.
         */
        DEFAULT,
        /**
         * This field should be considered for caching when it is under heavy
         * use.
         */
        CACHE;

    }

    /**
     * The types of fields. The type of the field influences the names that can
     * be stored in the dictionary.
     */
    public enum Type {

        NONE, STRING, INTEGER, FLOAT, DATE;

    }
    /**
     * The property name for the type.
     */
    @ConfigEnum(type = FieldInfo.Type.class, defaultValue = "NONE")
    public static final String PROP_TYPE = "type";

    /**
     * The set of attributes for the class.
     */
    @ConfigEnumSet(type = FieldInfo.Attribute.class, defaultList = {"INDEXED",
                                                                    "TOKENIZED",
                                                                    "VECTORED",
                                                                    "UNCASED",
                                                                    "CASED"})
    public static final String PROP_ATTRIBUTES = "attributes";

    /**
     * The name of a factory for pipelines.
     */
    @ConfigString(defaultValue = FieldInfo.DEFAULT_PIPELINE_FACTORY_NAME)
    public static final String PROP_PIPELINE_FACTORY_NAME = "pipelineFactoryName";

    private String pipelineFactoryName;
    
    /**
     * The name of the configuration component containing the default pipeline.
     */
    public static final String DEFAULT_PIPELINE_FACTORY_NAME = "pipeline_factory";

    /**
     * The name of the factory for stemmers.
     */
    @ConfigString(defaultValue = FieldInfo.DEFAULT_STEMMER_FACTORY_NAME)
    public static final String PROP_STEMMER_FACTORY_NAME = "stemmerFactoryName";
    
    private String stemmerFactoryName;
    
    /**
     * The name of the default stemmer factory.
     */
    public static final String DEFAULT_STEMMER_FACTORY_NAME = "stemmerFactory";

    /**
     * The name of this field.
     */
    private String name;

    /**
     * The id of this field.
     */
    private int id;

    /**
     * The set of attributes for this field.
     */
    private EnumSet<Attribute> attributes;

    /**
     * The type of this field.
     */
    private Type type;

    public FieldInfo() {
        attributes = getDefaultAttributes();
        type = Type.NONE;
    }

    /**
     * Constructs a
     * <code>FieldInfo</code> instance for the given field name.
     *
     * @param name The name of the field.
     */
    public FieldInfo(String name) {
        this(0, name, getDefaultAttributes(), Type.NONE, DEFAULT_PIPELINE_FACTORY_NAME, DEFAULT_STEMMER_FACTORY_NAME);
    }

    /**
     * Constructs a
     * <code>FieldInfo</code> instance for the given field name.
     *
     * @param id the ID of the field
     * @param name The name of the field.
     */
    public FieldInfo(int id, String name) {
        this(id, name, getDefaultAttributes(), Type.NONE, DEFAULT_PIPELINE_FACTORY_NAME, DEFAULT_STEMMER_FACTORY_NAME);
    }

    /**
     * Constructs a
     * <code>FieldInfo</code> object with the given attributes and type.
     *
     * @param name The name of the field.
     * @param attributes A set of field attributes. Note that we take a copy of
     * this set, so it is safe to modify or reuse the attributes.
     *
     */
    public FieldInfo(String name, EnumSet<Attribute> attributes) {
        this(0, name, attributes, Type.NONE, DEFAULT_PIPELINE_FACTORY_NAME, DEFAULT_STEMMER_FACTORY_NAME);
    }

    /**
     * Constructs a
     * <code>FieldInfo</code> object with the given attributes and type.
     *
     * @param name The name of the field.
     * @param attributes A set of field attributes. Note that we take a copy of
     * this set, so it is safe to modify or reuse the attributes.
     * @param type A type that will be used if the attributes indicate that the
     * field is saved.
     */
    public FieldInfo(String name, EnumSet<Attribute> attributes, Type type) {
        this(0, name, attributes, type, DEFAULT_PIPELINE_FACTORY_NAME, DEFAULT_STEMMER_FACTORY_NAME);
    }

    /**
     * Constructs a
     * <code>FieldInfo</code> object with the given attributes and type.
     *
     * @param name The name of the field.
     * @param attributes A set of field attributes. Note that we take a copy of
     * this set, so it is safe to modify or reuse the attributes.
     * @param type A type that will be used if the attributes indicate that the
     * field is saved.
     */
    public FieldInfo(String name, EnumSet<Attribute> attributes, Type type,
                     String pipelineFactoryName) {
        this(0, name, attributes, type, pipelineFactoryName, DEFAULT_STEMMER_FACTORY_NAME);
    }

    /**
     * Constructs a
     * <code>FieldInfo</code> object with the given attributes and
     * sub-attribute.
     *
     * @param id the ID to assign to this field.
     * @param name The name of the field.
     * @param attributes A set of field attributes. Note that we take a copy of
     * this set, so it is safe to modify or reuse the attributes.
     * @param type The field type
     * @throws IllegalArgumentException if the attributes of the field specify
     * that a field is saved and the type of the field is <code>null</code> or
     * Type.NONE, or if a type other than Type.NONE is specified and the
     * attributes do not contain the SAVED attribute.
     */
    public FieldInfo(int id, String name,
                     EnumSet<Attribute> attributes,
                     Type type, 
                     String pipelineFactoryName, 
                     String stemmerFactoryName) {

        this.id = id;
        this.name = name == null ? null : name.toLowerCase();
        if(attributes != null) {
            this.attributes = attributes.clone();
        } else {
            this.attributes = getDefaultAttributes();
        }
        this.type = type;
        if(pipelineFactoryName == null && this.attributes.contains(
                Attribute.INDEXED)) {
            logger.log(Level.WARNING, String.format(
                    "Field %s is indexed but has no pipeline defined, using the default",
                                                    name));
            this.pipelineFactoryName = DEFAULT_PIPELINE_FACTORY_NAME;
        } else {
            this.pipelineFactoryName = pipelineFactoryName;
        }
        if(stemmerFactoryName == null && this.attributes.contains(
                Attribute.STEMMED)) {
            logger.log(Level.WARNING, String.
                    format(
                    "Field %s is stemmed but has no stemmer defined, using the default",
                    name));
            this.stemmerFactoryName = DEFAULT_STEMMER_FACTORY_NAME;
        } else {
            this.stemmerFactoryName = stemmerFactoryName;
        }
    }

    public FieldInfo(RandomAccessFile raf) throws IOException {
        read(raf);
    }

    @Override
    public FieldInfo clone() {
        FieldInfo result = null;
        try {
            result = (FieldInfo) super.clone();
            result.attributes = attributes.clone();
        } catch(CloneNotSupportedException e) {
            throw new InternalError();
        }
        return result;
    }

    /**
     * Gets the name of the field.
     *
     * @return the name of the field. Note that field names are case
     * insensitive.
     */
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Adds an attribute to the field.
     *
     * @param attr the attribute value to set.
     * @return the current field information object, allowing chained
     * invocations.
     */
    public FieldInfo addAttribute(Attribute attr) {
        attributes.add(attr);
        return this;
    }

    /**
     * Removes an attribute from the field.
     *
     * @param attr the attribute value to set.
     * @return the current field information object, allowing chained
     * invocations.
     */
    public FieldInfo removeAttribute(Attribute attr) {
        attributes.remove(attr);
        return this;
    }

    /**
     * Sets the attributes associated with the field. The provided attributes
     * will replace whatever attributes are currently associated with the field.
     *
     * @param attributes the attribute value to set.
     */
    public void setAttributes(EnumSet<Attribute> attributes) {
        this.attributes = attributes.clone();
    }

    /**
     * Gets the attributes associated with this field.
     *
     * @return the set of attributes that this field has.
     */
    public EnumSet<Attribute> getAttributes() {
        return attributes.clone();
    }

    public boolean hasAttribute(Attribute attribute) {
        return attributes.contains(attribute);
    }

    /**
     * Gets the field type.
     *
     * @return the type of this field. If this field does not have the
     * <code>SAVED</code> attribute, then the type <code>NONE</code> is
     * returned.
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets the default saved value for a field of this type.
     *
     * @return a default value for the saved type of this field. For numeric
     * fields (including DATE fields), 0 is returned. For string fields, the
     * empty string is returned. If this field is not a saved field,
     * <code>null</code> will be returned.
     */
    public Object getDefaultSavedValue() {
        if(!attributes.contains(Attribute.SAVED)) {
            return null;
        }
        switch(type) {
            case INTEGER:
                return new Long(0);
            case FLOAT:
                return new Double(0);
            case DATE:
                return new Date(0);
            case STRING:
                return "";
            default:
                logger.warning(String.format(
                        "Field: %s has unknown type %s, using STRING", name,
                                             type));
                return "";
        }

    }

    public void setPipelineFactoryName(String piplineFactoryName) {
        this.pipelineFactoryName = piplineFactoryName;
    }

    public String getPipelineFactoryName() {
        return pipelineFactoryName;
    }

    public String getStemmerFactoryName() {
        return stemmerFactoryName;
    }

    public void setStemmerFactoryName(String stemmerFactoryName) {
        this.stemmerFactoryName = stemmerFactoryName;
    }

    /**
     * Gets the numeric id of this field.
     *
     * @return the id
     */
    public int getID() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("%s: %d type: %s attributes: %s stemmer: %s pipeline: %s", 
                             this.name, id,
                             type, attributes, stemmerFactoryName, pipelineFactoryName);
    }

    /**
     * Sets the attributes and type of this field from a provided property
     * sheet.
     *
     * @param ps a property sheet for this field
     * @throws com.sun.labs.util.props.PropertyException if there is any error
     * processing the properties
     * @see
     * com.sun.labs.util.props.Configurable#newProperties(com.sun.labs.util.props.PropertySheet)
     *
     */
    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {

        //
        // The name of the field will be the name of the configured component.
        name = ps.getInstanceName();
        attributes = (EnumSet<FieldInfo.Attribute>) ps.getEnumSet(
                PROP_ATTRIBUTES);
        type = (Type) ps.getEnum(PROP_TYPE);
        pipelineFactoryName = ps.getString(PROP_PIPELINE_FACTORY_NAME);
    }

    /**
     * Sets the type of this field info from a string. If the type provided is
     * not one of the defined types, then an error will be logged and the type
     * <code>STRING</code> will be used as the default.
     *
     * @param typeString a string representation of the type.
     */
    private void setType(String typeString) {
        try {
            type = Enum.valueOf(Type.class, typeString.toUpperCase());
        } catch(IllegalArgumentException iae) {
            logger.severe(String.format(
                    "Unknown type for field %s: %s, defaulting to NONE",
                    this.name, typeString));
            type = Type.NONE;
        }
    }

    /**
     * Writes this field information object to a data output.
     *
     * @param out the output where we will write the object
     * @throws java.io.IOException if there is any error writing the information
     */
    public void write(DataOutput out)
            throws IOException {
        out.writeUTF(name);
        out.writeUTF(type.name());
        out.writeInt(id);
        out.writeInt(attributes.size());
        for(FieldInfo.Attribute a : attributes) {
            out.writeUTF(a.name());
        }
        if(pipelineFactoryName == null) {
            out.writeUTF("");
        } else {
            out.writeUTF(pipelineFactoryName);
        }
        if(stemmerFactoryName == null) {
            out.writeUTF("");
        } else {
            out.writeUTF(stemmerFactoryName);
        }
    }

    /**
     * Reads a filed information object from the provided input.
     *
     * @param in the input from which we will read the field information
     * @throws java.io.IOException if there is any error reading the field
     * information
     */
    private void read(DataInput in)
            throws java.io.IOException {
        name = in.readUTF();
        setType(in.readUTF());
        id = in.readInt();
        int na = in.readInt();
        attributes = EnumSet.noneOf(Attribute.class);
        for(int i = 0; i < na; i++) {
            attributes.add(Attribute.valueOf(in.readUTF()));
        }
        pipelineFactoryName = in.readUTF();
        if(pipelineFactoryName.isEmpty()) {
            pipelineFactoryName = null;
        }
        stemmerFactoryName = in.readUTF();
        if(stemmerFactoryName.isEmpty()) {
            stemmerFactoryName = null;
        }
    }

    /**
     * Gets a set of the typical attributes for an indexed field.
     *
     * @return a set of attributes containing the INDEXED, TOKENIZED, and
     * VECTORED attributes
     */
    public static EnumSet<Attribute> getIndexedAttributes() {
        return EnumSet.of(Attribute.INDEXED,
                          Attribute.TOKENIZED,
                          Attribute.POSITIONS,
                          Attribute.CASED,
                          Attribute.UNCASED,
                          Attribute.VECTORED);
    }

    /**
     * Gets the default set of attributes, which indexes a field, but doesn't
     * vector or save it.
     *
     * @return
     */
    public static EnumSet<Attribute> getDefaultAttributes() {
        return EnumSet.of(Attribute.INDEXED,
                          Attribute.CASED,
                          Attribute.UNCASED,
                          Attribute.TOKENIZED,
                          Attribute.POSITIONS);
    }

    /**
     * Gets a set of the typical attributes for an indexed field.
     *
     * @return a set of attributes containing the INDEXED, TOKENIZED, and
     * VECTORED attributes
     */
    public static EnumSet<Attribute> getIndexedAndSavedAttributes() {
        return EnumSet.of(Attribute.INDEXED,
                          Attribute.TOKENIZED,
                          Attribute.POSITIONS,
                          Attribute.CASED,
                          Attribute.UNCASED,
                          Attribute.VECTORED,
                          Attribute.SAVED);
    }

    /**
     * Gets a set of attributes for a field that is only SAVED.
     */
    public static EnumSet<Attribute> getSavedAttributes() {
        return EnumSet.of(Attribute.SAVED);
    }

    /**
     * Gets a set of attributes for a string field that stores case insensitive
     * versions of the saved values as well.
     */
    public static EnumSet<Attribute> getUncasedStringAttributes() {
        return EnumSet.of(Attribute.SAVED, Attribute.UNCASED);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final FieldInfo other = (FieldInfo) obj;
        if((this.name == null) ? (other.name != null)
           : !this.name.equals(other.name)) {
            return false;
        }
        if(this.id != other.id) {
            return false;
        }
        if(this.attributes != other.attributes
                && (this.attributes == null
                || !this.attributes.equals(other.attributes))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 37 * hash + this.id;
        hash =
                37 * hash
                + (this.attributes != null ? this.attributes.hashCode() : 0);
        hash = 37 * hash + (this.type != null ? this.type.hashCode() : 0);
        return hash;
    }
} // FieldInfo

