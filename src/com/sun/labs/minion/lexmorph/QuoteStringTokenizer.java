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

package com.sun.labs.minion.lexmorph;

import java.util.Enumeration;
import java.util.StringTokenizer;

public class QuoteStringTokenizer extends Object implements Enumeration{

    private static String whitespace = " \t\n\r";
    boolean returnDelims, peeked;
    String openQ ;
    String closeQ ;
    String delimStr;
    String breaks;
    String peekToken;
    StringTokenizer sTok;

 public QuoteStringTokenizer(String from){
     this(from, "|", whitespace);
 }
 public QuoteStringTokenizer(String from, String quotes){
     this(from, quotes, whitespace);
 }
 public QuoteStringTokenizer(String from, String quotes, String delims){
     this(from, quotes, delims, false);
 }
 public QuoteStringTokenizer(String from, String quotes, String delims,
			     boolean retDelims){
  
  returnDelims = retDelims;
  openQ = "";
  if ((quotes != null) && (quotes.length() > 0))
      openQ = quotes.substring(0,1);
  closeQ = openQ;
  if ((quotes != null) && (quotes.length() > 1))
      closeQ = quotes.substring(1,2);
  delimStr = delims;
  breaks = delims + openQ;
  if (openQ != closeQ) breaks = breaks + closeQ;
  peekToken = "";
  peeked = false;
  sTok = new StringTokenizer(from, breaks, true);
  //System.out.println("new QST(\"" + from + "\",  \"" + breaks + "\")");
 }

    public boolean hasMoreTokens(){
	return (peeked || sTok.hasMoreTokens());
    }
    @Override
    public boolean hasMoreElements(){
	return this.hasMoreTokens();
    }

    @Override
    public Object nextElement(){
	return this.nextToken();
    }

    public String nextToken(){
	String tokStr = "";
	String readStr;
	boolean gotSome = false;
	if (peeked){
	    tokStr = peekToken;
	    peekToken = "";
	    peeked =false;
	}else{
	    while (sTok.hasMoreTokens()){
		 readStr = sTok.nextToken();
		 //System.out.println("QST loop got token =>" + readStr + "<=");
		 if (readStr.equals(openQ)){
		     String quotedStr = "";
		     gotSome = true;
		     while ((sTok.hasMoreTokens()) && 
			    !(readStr = sTok.nextToken()).equals(closeQ)){
			 quotedStr = quotedStr + readStr;
		     }
		     tokStr = tokStr +  quotedStr;
		 }else if (delimStr.indexOf(readStr) != -1){ // found a delim
		     if (returnDelims){
			 if (gotSome){
			     peeked = true;
			     peekToken = readStr;
			     break;
			 }else{
			     //System.out.println("QST returning delim =>" + 
			     //			readStr + "<=");
			     return readStr;
			 }
		     }else{
			 if (gotSome)
			     break;
		     }
		 }else{
		     gotSome = true;
		     tokStr = tokStr + readStr;
		 }
	    }
	}
	//System.out.println("QST returning token =>" + tokStr + "<=");
	return tokStr;
    }

}
