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

package com.sun.labs.minion.classification;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.util.props.ConfigComponentList;
import com.sun.labs.util.props.ConfigString;
import com.sun.labs.util.props.ConfigStringList;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.util.ArrayList;
import java.util.List;

/**
 * A configurable container that can be used to describe a set of classification
 * operations to perform that would not be done by the standard classification approach.
 * In particular, this class can be used to apply classifiers that were built from
 * the contents of one vectored field against the contents of a different vectored field.
 * 
 * <p>
 * 
 * Normally, classifiers are only applied against the vectored fields from which they
 * were built, but there may be cases where this behavior needs to be overridden.
 * By providing a list of instances of this class to the 
 * {@link ClassifierManager#PROP_EXTRA_CLASSIFICATIONS} property.
 * A set of non-standard classification operations can be performed.
 * 
 * @see ClassifierManager#PROP_EXTRA_CLASSIFICATIONS
 */
public class ExtraClassification implements Configurable {

    @ConfigString
    public static final String PROP_CLASSIFIER_FROM_FIELD =
            "classifier_from_field";

    private String classifierFromField;

    @ConfigComponentList(type = com.sun.labs.minion.FieldInfo.class)
    public static final String PROP_DOCUMENT_FROM_FIELDS =
            "document_from_fields";

    private List<String> documentFromFields;

    @ConfigComponentList(type = com.sun.labs.minion.FieldInfo.class, defaultList =
    {})
    public static final String PROP_CLASSIFIER_RESULT_FIELDS =
            "classifier_result_fields";
    
    private List<String> classifierResultFields;

    /**
     * A configurable list of classifier names to exclude when doing this extra
     * classification.
     */
    @ConfigStringList(defaultList={})
    public static final String PROP_EXCLUDED_CLASSIFIERS = "excluded_classifiers";

    private List<String> excludedClassifiers;

    public String getClassifierFromField() {
        return classifierFromField;
    }

    public List<String> getDocumentFromFields() {
        return documentFromFields;
    }

    public List<String> getClassifierResultFields() {
        return classifierResultFields;
    }
    
    public boolean isExcluded(String name) {
        return excludedClassifiers.contains(name);
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        classifierFromField = ps.getString(PROP_CLASSIFIER_FROM_FIELD);
        documentFromFields = fitos(ps, PROP_DOCUMENT_FROM_FIELDS);
        classifierResultFields = fitos(ps, PROP_CLASSIFIER_RESULT_FIELDS);
        if(classifierResultFields.size() == 0) {
            for(String f : documentFromFields) {
                classifierResultFields.add(f + "-assigned-label");
            }
        }
        excludedClassifiers = ps.getStringList(PROP_EXCLUDED_CLASSIFIERS);
    }

    private List<String> fitos(PropertySheet ps, String prop) throws PropertyException {
        List<FieldInfo> ff = (List<FieldInfo>) ps.getComponentList(prop);
        List<String> ret = new ArrayList<String>();
        for(FieldInfo fi : ff) {
            ret.add(fi.getName());
        }
        return ret;
    }
}
