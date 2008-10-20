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

package com.sun.labs.minion.retrieval.parser;

import java.util.*;

public class SimpleNode implements Node, Cloneable {
    protected Node parent;
    protected ArrayList children;
    protected int id;
    protected Parser parser;

    public static int INVALID =  -1;
    
    protected int operator = INVALID;
    protected String value = "";
    protected int unary = INVALID;

    /* To avoid having to use a hard-coded/checked in version
       of term, I'm putting support for term modifiers here in
       SimpleNode.  They'll only be set for terms by the grammar */
    protected boolean matchCase = false;
    protected boolean doExact = false;
    protected boolean doMorph = false;
    protected boolean doStem = false;
    protected boolean doWild = false;
    protected boolean doExpand = false;

    /* To support Lucene's ranges (and maybe our own if we add
       something to the grammar) we need a few more fields */

    // Include lower bound in range:
    protected boolean incLower = false;
    // Include upper bound in range:
    protected boolean incUpper = false;
    // Lower bound for range:
    protected String lower = null;
    // Upper bound for range:
    protected String upper = null;
    
    public SimpleNode(int i) {
        id = i;
    }


    public SimpleNode(Parser p, int i) {
        this(i);
        parser = p;
    }


/** 
 * A copy constructor that copies the values, but not anything
 * related to this node's place in the tree.
 * 
 */
    public Object clone() {
        SimpleNode other = new SimpleNode(parser, id);
        other.operator = operator;
        other.value = value;
        other.unary = unary;
        other.matchCase = matchCase;
        other.doExact = doExact;
        other.doMorph = doMorph;
        other.doStem = doStem;
        other.doWild = doWild;
        other.doExpand = doExpand;
        other.incLower = incLower;
        other.incUpper = incUpper;
        other.lower = lower;
        other.upper = upper;
        return other;
    }

    /*
    public static Node jjtCreate(int id) {
        return new SimpleNode(id);
    }

    public static Node jjtCreate(Parser p, int id) {
        return new SimpleNode(p, id);
        }
    */

    public void jjtOpen() {
    }

    public void jjtClose() {
    }
  
    public void jjtSetParent(Node n) { parent = n; }
    public Node jjtGetParent() { return parent; }

    public void jjtAddChild(Node n, int i) {
        if (children == null) {
            children = new ArrayList(i + 1);
        }
        if (i >= children.size()) {
            children.ensureCapacity(i + 1);
            while (children.size() < i + 1) {
                children.add(null);
            }
        }
        children.set(i, n);
    }

    public Node jjtGetChild(int i) {
        return (Node)(children.get(i));
    }

    public ArrayList getChildren() {
        if (children == null) {
            return new ArrayList();
        } else {
            return children;
        }
    }

    public int jjtGetNumChildren() {
        return (children == null) ? 0 : children.size();
    }

    /* You can override these two methods in subclasses of SimpleNode to
       customize the way the node appears when the tree is dumped.  If
       your output uses more than one line you should override
       toString(String), otherwise overriding toString() is probably all
       you need to do. */

    public String toString() {
        if (parser instanceof WebParser) {
            return WebParserTreeConstants.jjtNodeName[id] + (value!=""?" val: " + value:"");
        } else if (parser instanceof StrictParser) {
            return StrictParserTreeConstants.jjtNodeName[id] + (value!=""?" val: " + value:"") + (operator>=0?" op: " + StrictParserConstants.tokenImage[operator]:"") + (unary>=0?" una: " + StrictParserConstants.tokenImage[unary]:"");
        } else if (parser instanceof LuceneParser) {
            String ret = "";
            if (unary != INVALID) {
                ret = LuceneParserConstants.tokenImage[unary] + " ";
            }
            if (!(this instanceof LuceneASTrange)) {
                ret += LuceneParserTreeConstants.jjtNodeName[id] + (value!=""? " val: " + value : "");
            } else {
                ret += "range " + (incLower?"[":"{") + lower + "-" + upper + (incUpper?"]":"}");
            }
            return ret;
        }
        return "?";
    }
    public String toString(String prefix) {
        return prefix + toString();
    }

    /* Override this method if you want to customize how the node dumps
       out its children. */

    public void dump(String prefix) {
        System.out.println(toString(prefix));
        if (children != null) {
            ListIterator it = children.listIterator();
            while (it.hasNext()) {
                SimpleNode n = (SimpleNode)it.next();
                if (n != null) {
                    n.dump(prefix + " ");
                }
            }
        }
    }
}

