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


/**
 * An ElementHandler to handle <i>Configuration</i> XML Elements
 * @author Bernard Horan
 *
 */
class ConfigurationElementHandler extends ElementHandler {

    /**
     * Constructor to set the name of the element handler to "Configuration"
     * @see com.sun.labs.minion.test.regression.merge.ElementHandler#ElementHandler(MergeTestReplayer)
     */
    public ConfigurationElementHandler(MergeTestReplayer replayer) {
        super(replayer);
        name = "Configuration";
    }
    /**
     * Inform the replayer that the confguration data is complete
     * @see com.sun.labs.minion.test.regression.merge.ElementHandler#endElement(java.lang.String)
     */
    public void endElement (String localName) {
        super.endElement(localName);
        replayer.completeConfiguration();
    }

}
