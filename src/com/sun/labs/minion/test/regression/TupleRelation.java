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

package com.sun.labs.minion.test.regression;

/**
 * <p>
 * A tuple relation describes the relation of the tuple in entrys of a
 * graph. For example, if the tuple consists of all the documents
 * that satisfy the relation: contains the entrys "cat" and "dog" or
 * the entry "apple", i.e., ((cat AND dog) OR apple),
 * then this relation is represented in a graph like:
 * <pre>
 *
 *       OR
 *      /  \
 *    AND   apple
 *    / \
 * cat   dog
 *
 * </pre>
 * </p>
 *
 * <p>
 * A TupleRelation object is one node in this graph. Traversing from
 * the top node (the "OR" node in this example) gives the entire graph.
 * </p>
 */
public class TupleRelation {

    private String name;
    private TupleRelation left;
    private TupleRelation right;

    /**
     * Creates a TupleRelation with the given name.
     */
    public TupleRelation(String name) {
        this.name = name;
    }

    /**
     * Sets the left child.
     *
     * @param leftChild the left child
     */
    public void setLeft(TupleRelation leftChild) {
        this.left = leftChild;
    }

    /**
     * Sets the right child.
     *
     * @param rightChild the right child
     */
    public void setRight(TupleRelation rightChild) {
        this.right = rightChild;
    }

    /**
     * Returns the left child.
     *
     * @return the left child
     */
    public TupleRelation getLeft() {
        return left;
    }

    /**
     * Returns the right child.
     *
     * @return the right child
     */
    public TupleRelation getRight() {
        return right;
    }

    /**
     * Returns a string representation of this tuple relation.
     *
     * @return a string representation of this tuple relation
     */
    public String toString() {
        String result = "(";
        if (left != null) {
            result += left.toString();
        }
        result += (" " + name + " ");
        if (right != null) {
            result += right.toString();
        }
        result += ")";
        return result;
    }
}
