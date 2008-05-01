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

import java.util.Iterator;
import java.util.Set;

public class JlfEmitter {
    private String author;

    /**
     * Simple utility class that emits a line 
     * of lexical definition for the Minion lexicon.
     */
    public JlfEmitter(String anAuthor) {
        super();
        author = anAuthor;
    }

    void emitLexiconEntry(String rdfString, Set variantForms) {
        String rdfScored = format(rdfString);
        emitDefinition(rdfScored);
        emitVariants(rdfScored, variantForms);
    }

    void emitDefinition(String rdfString) {
        System.out.print(rdfString);
        System.out.print(":n;");
        emitAuthor();
        System.out.println();
    }

    /**
     * 
     */
    private void emitAuthor() {
        System.out.print("author:");
        System.out.print(author);
        System.out.print(";");
    }

    void emitVariants(String rdfString, Set variantForms) {
        for (Iterator iter = variantForms.iterator(); iter.hasNext();) {
            String element = format((String) iter.next());
            if (!element.equals(rdfString)) {
                System.out.print(element);
                System.out.print(":n;");
                emitAuthor();
                System.out.print("variant-of:");
                System.out.print(rdfString);
                System.out.println(";");
            }
        }
        
    }

    String format(String rdfString) {
        String formattedString = rdfString.replace(' ', '_');
        if (formattedString.indexOf(':')== -1) {
            return formattedString;
        } else {
            return "|" + formattedString + "|";
        }
    }

}
