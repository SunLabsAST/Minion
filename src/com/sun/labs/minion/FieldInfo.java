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
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.EnumSet;
import com.sun.labs.util.props.ConfigEnum;
import java.util.Date;
import java.util.logging.Logger;

/**
 * A class that can be used to tell the indexer what to do with the data
 * contained in a field.
 *
 * <p>
 *
 * Fields can be defined in the configuration file used to create a search
 * engine or via the {@link com.sun.labs.minion.SearchEngine#defineField} method.
 *
 * <p>
 *
 * Each field has a name, which is a string.  Note that field names are <em>case
 * insensitive</em>.  This means, for example, that <code>title</code>,
 * <code>Title</code>, and <code>TITLE</code> will be considered the same.
 *
 * <p>
 *
 * The disposition of the field data by the indexer is controlled
 * by the attributes that are assigned to the field.  The following attributes
 * are defined by the {@link Attribute} enumeration:
 *
 *
 * <dl>
 *
 * <dt><code>TOKENIZED</code></dt>
 * <dd>The field value should be tokenized.    If this attribute is set, then
 * the data in the field will be
 * tokenized according to the rules of whatever tokenizer is currently
 * being used.  Typically, this means that the data in the field will be broken
 * into tokens at spaces and punctuation.
 * </dd>
 *
 * <dt><code>INDEXED</code></dt>
 *
 * <dd>Any terms in the field (whether the field is tokenized or
 * un-tokenized) will have entries added to the main dictionary and postings
 * data added to the postings file for that dictionary.  Fields that specify
 * this attribute can be specified in queries that use the
 * <code>&lt;contains&gt;</code> operator.  For example, if the <code>title</code>
 * field has the <code>INDEXED</code> attribute, then the query:
 * <blockquote>
 * <code>title &lt;contains&gt; java</code>
 * </blockquote>
 * will return those documents that have the term <code>java</code> in the
 * <code>title</code> field.
 * </dd>
 *
 * <dt><code>VECTORED</code></dt>
 *
 * <dd>
 * This attribute indicates that terms extracted from the field value should be
 * added to a document vector specific to the field, as well as to the overall
 * document vector for this document.   Specifying this attribute allows
 * applications to perform classification or document similarity computations
 * against just this field.  So, for example, you could find a set of documents
 * that have titles similar to a given document's title.
 * </dd>
 *
 * <dt><code>TRIMMED</code></dt>
 *
 * <dd>
 * This attribute indicates that field values passed into the indexer should
 * have any leading or trailing spaces trimmed from the values before they
 * are processed any further.
 * </dd>
 *
 * <dt><code>CASE_SENSITIVE</code></dt>
 *
 * <dd>
 * This attribute indicates that a given saved string field should be treated
 * in a case sensitive manner.  If a saved field has the case sensitive attribute
 * set, then relational queries against that field <em>must</em> match the case
 * of the values stored in the field.
 * </dd>
 *
 * <dt><code>SAVED</code></dt>
 *
 * <dd>This attribute indicates that the value for the field should be stored
 * in the index exactly as it provided.  Values that are in saved fields are
 * available for parametric searches (e.g., <code>price &lt; 10</code>) and
 * for results sorting. If <code>SAVED</code> is specified as an attribute,
 * then <em>one</em> of the following types <em>must</em> be specified:
 *
 * <dl>
 *
 * <dt><code>INTEGER</code></dt>
 *
 * <dd> Some quantity storeable in a 64 bit integer.</dd>
 *
 * <dt><code>FLOAT</code></dt>
 *
 * <dd>Some quantity storeable in a 64 bit double.</dd>
 *
 * <dt><code>DATE</code></dt>
 *
 * <dd>The field value is a date, given in some text representation.  This
 * date will be parsed and then stored in a Java long as the number of
 * milliseconds since the epoch (00:00:00 GMT, January 1, 1970).</dd>
 *
 * <dt><code>STRING</code></dt>
 *
 * <dd>A text field that consists of a variable number of characters. The
 * default variable width field is the empty string.</dd>
 *
 * </dl>
 * </dl>
 *
 * <p>
 *
 * The attributes of the <code>FieldInfo</code> object can be set by providing
 * an {@link java.util.EnumSet} to the constructor, or by using the
 * <code>setAttribute</code> method.
 *
 */
public class FieldInfo implements Cloneable,
        Configurable {

    /**
     * The various attributes that a field can have.  Any field may specify
     * any combination of these attributes.  If a field has the <code>SAVED</code>
     * attribute, then it should also have a type specified.
     */
    public enum Attribute {

        TOKENIZED, INDEXED, VECTORED, SAVED, TRIMMED, CASE_SENSITIVE

    }

    /**
     * The types that a saved field can have.  Note the special type
     * <code>NONE</code>, which means that the field is not saved and therefore
     * has no type.
     */
    public enum Type {

        NONE, STRING, INTEGER, FLOAT, DATE, FEATURE_VECTOR

    }
    /**
     * The property name for the type.
     */
    @ConfigEnum(type = FieldInfo.Type.class, defaultValue = "NONE")
    public static final String PROP_TYPE = "type";

    /**
     * The property name for the vectored attribute.
     */
    @ConfigBoolean(defaultValue = false)
    public static final String PROP_VECTORED = "vectored";

    /**
     * The property name for the tokenized attribute.
     */
    @ConfigBoolean(defaultValue = false)
    public static final String PROP_TOKENIZED = "tokenized";

    /**
     * The property name for the indexed attribute.
     */
    @ConfigBoolean(defaultValue = false)
    public static final String PROP_INDEXED = "indexed";

    /**
     * The property name for the saved attribute.
     */
    @ConfigBoolean(defaultValue = false)
    public static final String PROP_SAVED = "saved";

    /**
     * The property name for the trimmed attribute.
     */
    @ConfigBoolean(defaultValue = false)
    public static final String PROP_TRIMMED = "trimmed";

    protected static Logger logger = Logger.getLogger(FieldInfo.class.getName());

    public static final String logTag = "FI";

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
        this(0, null, null, Type.NONE);
    }

    /**
     * Constructs a <code>FieldInfo</code> instance for the given field
     * name.
     *
     * @param name The name of the field.
     */
    public FieldInfo(String name) {
        this(0, name, null, Type.NONE);
    }

    /**
     * Constructs a <code>FieldInfo</code> instance for the given field
     * name.
     *
     * @param id the ID of the field
     * @param name The name of the field.
     */
    public FieldInfo(int id, String name) {
        this(id, name, null, Type.NONE);
    }

    /**
     * Constructs a <code>FieldInfo</code> object with the given attributes
     * and type.
     *
     * @param name The name of the field.
     * @param attributes A set of field attributes.  Note that we take a copy
     * of this set, so it is safe to modify or reuse the attributes.
     *
     */
    public FieldInfo(String name,
            EnumSet<Attribute> attributes) {
        this(0, name, attributes, Type.NONE);
    }

    /**
     * Constructs a <code>FieldInfo</code> object with the given attributes
     * and type.
     *
     * @param name The name of the field.
     * @param attributes A set of field attributes.  Note that we take a copy
     * of this set, so it is safe to modify or reuse the attributes.
     * @param type A type that will be used if the attributes indicate that the
     * field is saved.
     */
    public FieldInfo(String name,
            EnumSet<Attribute> attributes,
            Type type) {
        this(0, name, attributes, type);
    }

    /**
     * Constructs a <code>FieldInfo</code> object with the given attributes
     * and sub-attribute.
     *
     * @param id the ID to assign to this field.
     * @param name The name of the field.
     * @param attributes A set of field attributes.  Note that we take a copy
     * of this set, so it is safe to modify or reuse the attributes.
     * @param type A type that will be used if the attributes for the field
     * includes the <code>SAVED</code> attribute.
     * @throws IllegalArgumentException if the attributes of the field specify
     * that a field is saved and the type of the field is <code>null</code> or
     * Type.NONE, or if a type other than Type.NONE is specified and the
     * attributes do not contain the SAVED attribute.
     */
    public FieldInfo(int id, String name,
            EnumSet<Attribute> attributes,
            Type type) {
        this.id = id;
        this.name = name == null ? null : name.toLowerCase();
        if(attributes != null) {
            this.attributes = attributes.clone();
        } else {
            this.attributes =
                    EnumSet.noneOf(Attribute.class);
        }
        if(type != null) {
            this.type = type;
        } else {
            this.type = Type.NONE;
        }

        //
        // Quick checks to make sure that the saved attribute and the type
        // make sense.
        if(isSaved() && this.type == Type.NONE) {
            throw new IllegalArgumentException("Saved field " + name +
                    " must have a type specified");
        }

        if(!isSaved() && this.type != Type.NONE) {
            throw new IllegalArgumentException("Unsaved field " + name +
                    " should not specify " + this.type + " type");
        }
    }

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
     * Gets the field's name.
     * @return the name of the field. Note that field names are case insensitive.
     */
    public String getName() {
        return this.name;
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
     * Sets the attributes associated with the field.  The provided attributes
     * will replace whatever attributes are currently associated with the
     * field.
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

    /**
     * Gets the field type.
     * @return the type of this field.  If this field does not have the
     * <code>SAVED</code> attribute, then the type <code>NONE</code> is
     * returned.
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets the default saved value for a field of this type.
     * @return a default value for the saved type of this field.  For numeric fields
     * (including DATE fields), 0 is returned.  For string fields, the empty string
     * is returned.  If this field
     * is not a saved field, <code>null</code> will be returned.
     */
    public Object getDefaultSavedValue() {
        if(!isSaved()) {
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
            case FEATURE_VECTOR:
                return new double[0];
            default:
                logger.warning("Field: " + name + " " +
                        "has unknown SAVED type: " + type +
                        ", using STRING.");
                return "";
        }

    }

    /**
     * Gets the numeric id of this field.
     *
     * @return the id
     */
    public int getID() {
        return id;
    }

    /**
     * Indicates whether the field is indexed or not.
     * @return <code>true</code> if this field has the indexed attribute,
     * <code>false</code> otherwise.
     */
    public boolean isIndexed() {
        return attributes.contains(Attribute.INDEXED);
    }

    /**
     * Tells whether the field is tokenized or not.
     * @return <code>true</code> if this field has the tokenized attribute,
     * <code>false</code> otherwise.
     */
    public boolean isTokenized() {
        return attributes.contains(Attribute.TOKENIZED);
    }

    /**
     * Tells whether the field is saved or not.
     * @return <code>true</code> if this field has the saved attribute,
     * <code>false</code> otherwise.
     */
    public boolean isSaved() {
        return attributes.contains(Attribute.SAVED);
    }

    /**
     * Tells whether this field should have tokens added to the document
     * vector or not.
     * @return <code>true</code> if this field has the vectored attribute,
     * <code>false</code> otherwise.
     */
    public boolean isVectored() {
        return attributes.contains(Attribute.VECTORED);
    }

    /**
     * Tells whether a string saved field should have it's values trimmed of spaces
     * before the values are stored in the index.
     * @return <code>true</code> if the field values should be trimmed,
     * <code>false</code>
     * otherwise.
     */
    public boolean isTrimmed() {
        return attributes.contains(Attribute.TRIMMED);
    }

    /**
     * Tells whether this field is meant to be stored in a case sensitive
     * fashion. This attribute only makes sense for fields of type
     * <code>STRING</code>.  If a saved field has the case sensitive attribute
     * set, then relational queries against that field <em>must</em> match the case
     * of the values stored in the field.
     * @return <code>true</code> if this field is case sensitive, <code>false</code>
     * otherwise
     */
    public boolean isCaseSensitive() {
        return attributes.contains(Attribute.CASE_SENSITIVE);
    }

    @Override
    public String toString() {
        return this.name + ": " + id + " type: " + type + " attributes: " +
                attributes;
    }

    /**
     * Sets the attributes and type of this field from a provided property
     * sheet.
     *
     * @param ps a property sheet for this field
     * @throws com.sun.labs.util.props.PropertyException if there is any error
     * processing the properties
     * @see com.sun.labs.util.props.Configurable#newProperties(com.sun.labs.util.props.PropertySheet)
     *
     */
    @Override
    public void newProperties(PropertySheet ps)
            throws PropertyException {

        //
        // The name of the field will be the name of the configured component.
        name = ps.getInstanceName();
        if(ps.getBoolean(PROP_TRIMMED)) {
            attributes.add(Attribute.TRIMMED);
        }
        if(ps.getBoolean(PROP_SAVED)) {
            attributes.add(Attribute.SAVED);
        }
        if(ps.getBoolean(PROP_VECTORED)) {
            attributes.add(Attribute.VECTORED);
        }
        if(ps.getBoolean(PROP_TOKENIZED)) {
            attributes.add(Attribute.TOKENIZED);
        }
        if(ps.getBoolean(PROP_INDEXED)) {
            attributes.add(Attribute.INDEXED);
        }
        type = (Type) ps.getEnum(PROP_TYPE);
    }

    /**
     * Sets the type of this field info from a string.  If the type provided
     * is not one of the defined types, then an error will be logged and
     * the type <code>STRING</code> will be used as the default.
     * @param typeString a string representation of the type.
     */
    private void setType(String typeString) {
        try {
            type = Enum.valueOf(Type.class, typeString.toUpperCase());
        } catch(IllegalArgumentException iae) {
            logger.severe("Unknown type for field " + this.name +
                    ": " + typeString +
                    ", defaulting to String");
            type = Type.STRING;
        }
    }

    /**
     * Writes this field information object to a data output.
     * @param out the output where we will write the object
     * @throws java.io.IOException if there is any error writing the information
     */
    public void write(DataOutput out)
            throws IOException {
        out.writeUTF(name);
        out.writeInt(id);
        for(Attribute a : Attribute.values()) {
            out.writeBoolean(attributes.contains(a));
        }
        out.writeUTF(type.toString());
    }

    /**
     * Reads a filed information object from the provided input.
     * @param in the input from which we will read the field information
     * @throws java.io.IOException if there is any error reading the field
     * information
     */
    public void read(DataInput in)
            throws java.io.IOException {
        name = in.readUTF();
        id = in.readInt();
        attributes =
                EnumSet.noneOf(Attribute.class);
        for(Attribute a : Attribute.values()) {
            if(in.readBoolean()) {
                attributes.add(a);
            }
        }
        setType(in.readUTF());
    }

    /**
     * Gets a set of the typical attributes for an indexed field.
     * @return a set of attributes containing the INDEXED, TOKENIZED, and VECTORED
     * attributes
     */
    public static EnumSet<Attribute> getIndexedAttributes() {
        return EnumSet.of(Attribute.INDEXED,
                Attribute.TOKENIZED,
                Attribute.VECTORED);
    }
} // FieldInfo
