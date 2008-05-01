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

package com.sun.labs.minion.util;

/**
 * A stack of Java objects.
 */

public class Stack  {

    /**
     * Build a default-sized stack.
     */
    public Stack() {
	stack = new Object[16];
	N = 0;
    }

    /**
     * Build a stack with a given number of elements available.
     */
    public Stack(int n) {
	stack = new Object[n];
    }

    /**
     * Build a stack from another stack.
     */
    public Stack(Stack s) {
	stack = new Object[s.stack.length];
	N = s.N;
	System.arraycopy(s.stack, 0, stack, 0, s.stack.length);
    }
    
    /**
     * Ensure that there is enough room in the stack for the given number
     * of elements.
     *
     * @param n The number of elements we want to store.
     */
    protected void ensureCapacity(int n) {

	//
	// Our current size suffices.
	if(n < stack.length) {
	    return;
	}

	//
	// Get the max of 2 times the current capacity or n.
	int newCap = Math.max(stack.length * 2, n);

	//
	// Make new heap, copy, then replace.
	Object[] temp = new Object[newCap];
	System.arraycopy(stack, 0, temp, 0, stack.length);
	stack = temp;
    }

    /**
     * Test whether this Stack is empty.
     *
     * @return true if it is empty, false otherwise.
     */
    public boolean empty() {
	return N == 0;
    }

    /**
     * Push an element onto the stack.
     *
     * @param o the <code>Object</code> to push.
     */
    public void push(Object o) {
	ensureCapacity(N+1);
	stack[N++] = o;
    }

    /**
     * Peek at the top of the stack, but don't return it.
     *
     * @return the object on top of the stack, or null if the stack is
     * empty.
     */
    public Object peek() {
	if(N > 0) {
	    return stack[N-1];
	} else {
	    return null;
	}
    }

    /**
     * Pop the top element off the stack.
     *
     * @return the object on top of the stack, or null if the stack is
     * empty.
     */
    public Object pop() {
	if(N > 0) {
	    N--;
	    Object 	r = stack[N];
	    stack[N]   	  = null;
	    return r;
	} else {
	    return null;
	}
    }

    /**
     * An array to store the objects in.
     */
    protected Object[] stack;

    /**
     * The number of elements in the stack.
     */
    protected int N;
    
} // Stack
