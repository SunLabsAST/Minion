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

package com.sun.labs.minion.knowledge;

import com.sun.labs.util.props.Configurable;

/**
 * @author bh37721
 * $Id: CompositeKnowledgeSource.java,v 1.1.2.3 2008/01/17 18:34:35 stgreen Exp $
 * A CompositeKnowledgeSource is a container for multiple knowledge sources. Follows the Composite
 * pattern.
 */
public interface CompositeKnowledgeSource extends KnowledgeSource, Configurable {
	/**
	 * Add a KnowledgeSource.
	 */
	public void addSource(KnowledgeSource aSource);

}
