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

package com.sun.labs.minion.retrieval;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.dictionary.BasicField;
import com.sun.labs.minion.indexer.dictionary.SavedField;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;

import com.sun.labs.minion.util.CDateParser;
import com.sun.labs.minion.query.Relation.Operator;

/**
 * A class to hold a term generated by a single parametric field
 * operator.  It should be used in cases where there is a single operation
 * on a single field.  When there are overlapping operations on a single
 * field (e.g., <code>num &gt;= 1 &lt;and&gt; num &lt; 25</code>), the
 * <code>RangeFieldTerm</code> should be used.
 */
public class FieldTerm extends QueryTerm {

    /**
     * The name of the field that is begin operated on.
     */
    protected String name;

    /**
     * The parametric operator, as defined in the relation class.
     */
    protected Operator op;

    /**
     * The value given in the query.
     */
    protected String val;

    /**
     * A date parser for date fields.
     */
    protected CDateParser dp;

    /**
     * For range queries, this is the lower bound on the range.
     */
    protected Object lowerBound = null;

    /**
     * Lower bound is inclusive?
     */
    protected boolean includeLower = false;

    /**
     * For range queries, this is the upper bound on the range.
     */
    protected Object upperBound = null;

    /**
     * Upper bound is inclusive?
     */
    protected boolean includeUpper = false;

    /**
     * The saved field that we're working on.
     */
    private SavedField sf;

    /**
     * An iterator that will produce terms from the dictionary associated
     * with this saved field.  This will be used when a range of terms is
     * requested (e.g., for a &lt;= operator);
     */
    protected DictionaryIterator iter;

    /**
     * The postings for a a single term pulled from the dictionary
     * associated with this saved field.  This will be used when a single
     * term is requested (e.g., for an = operator).
     */
    protected PostingsIterator pi;

    /**
     * This should be true if this FieldTerm was constructed with
     * range data.
     */
    protected boolean selectRange = false;

    protected static String logTag = "FT";

    protected static long dayMilliSeconds = 24 * 3600 * 1000;

    /**
     * Creates a field term.
     *
     * @param name The name of the field that we're operating on.
     * @param op The operator that we're using.
     * @param val The value to compare against, given in the query.
     */
    public FieldTerm(String name, Operator op, String val) {
        this.name = name;
        this.op = op;
        this.val = val;
    } // FieldTerm constructor

    /**
     * Construct a field term with a specific range of valid values.
     * This constructor handles queries in the form of (a &lt; x &lt; b).
     * All parameters must be valid and the operation is assumed
     * to be RANGE.
     *
     * @param name the name of the field to operate on
     * @param lowerBound the lower bound on the range
     * @param includeLower true if the lower bound is inclusive
     * @param upperBound the upper bound on the range
     * @param includeUpper true if the upper bound is inclusive
     */
    public FieldTerm(String name,
                      Object lowerBound, boolean includeLower,
                      Object upperBound, boolean includeUpper) {
        this.name = name;
        this.op = Operator.RANGE;
        this.lowerBound = lowerBound;
        this.includeLower = includeLower;
        this.upperBound = upperBound;
        this.includeUpper = includeUpper;

    }

    public String getName() {
        return name;
    }

    public Operator getOp() {
        return op;
    }

    /**
     * Gets the value of this field term, if it is not a range
     * operation.  Otherwise, returns null.
     * @return the value
     */
    public Object getValue() {
        return val;
    }

    public void setPartition(DiskPartition part) {

        InvFileDiskPartition ifpart = (InvFileDiskPartition) part;

        //
        // Set up for our range or for a single term.
        iter = null;
        pi = null;

        //
        // We'll need to operate based on the type.
        sf = ifpart.getFieldStore().getSavedField(name);
        if(sf == null) {
            return;
        }

        //
        // Similar doesn't use the iterator or a dictionary value, so it's a 
        // special case.
        if(op == Operator.SIMILAR) {
            super.setPartition(part);
            return;
        }


        //
        // Special case for character field values: whether we need to do
        // things case sensitively.  This depends on a couple of factors.
        // In order these are:
        //
        // 1. If we're supposed to match case, then we do it.
        // 2. If the query configuration says that we need to match case,
        //    then we do it.
        matchCase = qc.caseSensitive(val);

        //
        // Setup for getting a range of terms.
        //Object lowerBound = null;
        //boolean includeLower = false;
        //Object upperBound = null;
        //boolean includeUpper = false;

        //
        // We may need a date.
        Date d = null;
        long time = 0;
        boolean dayResolution = false;
        boolean getIterator = true;

        //
        // An object to pass to the iterator call.
        Object o = val;

        if(dp == null && sf.getField().getType() == FieldInfo.Type.DATE) {
            dp = new CDateParser();
        }

        //
        // Special case:  we need to deal with day resolution dates.  If
        // we're supposed to have a date, parse it and then figure out
        // whether it is for a whole day.  The range case is handled specially
        // below since it requires two dates.
        if(sf.getField().getType() == FieldInfo.Type.DATE && op != Operator.RANGE) {
            try {
                d = dp.parse(val);
                time = d.getTime();
                dayResolution = isDayResolution(d);
                o = new Date(time);
            } catch(java.text.ParseException pe) {
                log.warn(logTag, 3, "Invalid date format: \"" +
                         val + "\" for date field: " + name);
                return;
            }
        }

        //
        // Now, switch on the operator we were given to build our bounds.
        // We'll have to special case the date stuff
        switch(op) {
            case EQUALS:
                if(d != null && dayResolution) {

                    //
                    // All days equal to the given day. The lower bound is the
                    // time we were given, inclusive.  The upper bound is
                    // midnight for the next day, exclusive.
                    lowerBound = new Date(time);
                    includeLower = true;
                    upperBound = new Date(time + dayMilliSeconds);
                    includeUpper = false;
                } else {
                    pi = ifpart.getFieldPostings(name, o, matchCase);
                    getIterator = false;
                }
                break;

            case LESS_THAN:
                upperBound = o;
                includeUpper = false;
                break;

            case LEQ:

                if(d != null && dayResolution) {

                    //
                    // All days less than or equal to the given day.  The upper
                    // bound is midnight on the following day, exclusive
                    upperBound = new Date(time + dayMilliSeconds);
                    includeUpper = false;
                } else {
                    upperBound = o;
                    includeUpper = true;
                }
                break;

            case GREATER_THAN:
                if(d != null && dayResolution) {

                    //
                    // All days greater than the given day.  The lower bound is
                    // midnight on the following day, inclusive.
                    lowerBound = new Date(time + dayMilliSeconds);
                    includeLower = true;
                } else {
                    lowerBound = o;
                    includeLower = false;
                }
                break;

            case GEQ:

                //
                // All days greater than or equal to the given day.
                // The lower bound is the time we were given.
                lowerBound = o;
                includeLower = true;
                break;

            case RANGE:
                //
                // A little repeated code here, but it is a little unavoidable.
                // First, figure out if we're doing a range op on dates.
                if(sf.getField().getType() == FieldInfo.Type.DATE) {
                    try {
                        //
                        // For the lower bound:
                        if(lowerBound instanceof String) {
                            d = dp.parse((String) lowerBound);
                            time = d.getTime();
                            lowerBound = new Date(time);
                        }

                        //
                        // For the upper bound:
                        if(upperBound instanceof String) {
                            d = dp.parse((String) upperBound);
                            time = d.getTime();
                            if(isDayResolution(d) && includeUpper) {
                                upperBound = new Date(time + dayMilliSeconds);
                            }
                        }
                    } catch(java.text.ParseException pe) {
                        log.warn(logTag, 3, "Invalid date format: \"" +
                                 val + "\" for date field: " + name);
                        return;
                    }
                }
                //
                // If it isn't a date, then the bounds should already have been
                // set in the constructor.  Let the code below fetch an iterator.
                break;

            case MATCHES:
                iter = ifpart.getMatchingIterator(name, val,
                                                  matchCase);
                getIterator = false;
                break;
            case SUBSTRING:
                iter = ifpart.getSubstringIterator(name, val,
                                                   matchCase,
                                                   false, false);
                getIterator = false;
                break;

            case STARTS:
                iter = ifpart.getSubstringIterator(name, val,
                                                   matchCase,
                                                   true, false);
                getIterator = false;
                break;

            case ENDS:
                iter = ifpart.getSubstringIterator(name, val,
                                                   matchCase,
                                                   false, true);
                getIterator = false;
                break;
        }

        //
        // If we need to get an iterator, do it now.
        if(getIterator) {
            iter = ifpart.getFieldIterator(name, matchCase,
                                           lowerBound, includeLower,
                                           upperBound, includeUpper);
        }

        //
        // If we have an iterator, if there's nothing in the iterator, we
        // can just stop.
        if(iter != null && !iter.hasNext()) {
            iter = null;
        }

        //
        // Setup the partition for the superclass.
        super.setPartition(part);
    }

    /**
     * Checks whether a given date has a day-level resolution.  Checks
     * if hour, minute, second are all 0.
     */
    protected static boolean isDayResolution(Date d) {
        Calendar valCal = Calendar.getInstance();
        valCal.setTime(d);
        return valCal.get(Calendar.HOUR_OF_DAY) == 0 &&
                valCal.get(Calendar.MINUTE) == 0 &&
                valCal.get(Calendar.SECOND) == 0;
    }

    /**
     * Estimates the size of the results set for the given term.
     */
    protected int calculateEstimatedSize() {
        if(iter == null) {
            if(pi == null) {
                return 0;
            }
            return pi.getN();
        }

        return iter.estimateSize();
    }

    /**
     * Evaluates the term in the current partition.
     *
     * @param ag An array group that we can use to limit the evaluation of
     * the term.  If this group is <code>null</code> a new group will be
     * returned.  If this group is non-<code>null</code>, then the elements
     * in the group will be used to restrict the documents that we return
     * from the term.
     * @return A new <code>ArrayGroup</code> containing the results of
     * evaluating the term against the given group.  We will return an
     * instance of <code>StrictGroup</code>, since there are no weights
     * associated with the values.
     */
    public ArrayGroup eval(ArrayGroup ag) {

        if(op == Operator.SIMILAR) {
            if(sf == null) {
                return new ArrayGroup(0);
            }

            if(sf instanceof BasicField) {
                return ((BasicField) sf).getSimilar(ag, val, matchCase);
            } else {
                log.warn(logTag, 0, "SIMILAR attempted for non-basic field");
                return new ArrayGroup(0);
            }
        }

        //
        // If we don't have a dictionary iterator, then we must have a
        // single postings iterator.
        if(iter == null) {


            if(pi == null) {

                //
                // Oops, no posting iterator!  We're done.
                return new ArrayGroup(0);
            }

            //
            // If there's no group on the input, we can just grab the docs
            // from that postings iterator.
            if(ag == null) {
                return new ArrayGroup(pi);
            } else {

                //
                // Intersect the group we were given with the postings
                // iterator that we have for the field value.
                ArrayGroup ret = (ArrayGroup) ag.clone();
                return ret.destructiveIntersect(pi);
            }
        }

        //
        // We want to provide scores for some of the string operators, so handle
        // those separately.  The score assigned to the documents for a given 
        // field value will be the maximum proportion of a field value matched.
        if(sf.getField().getType() == FieldInfo.Type.STRING &&
                (op == Operator.MATCHES || op == Operator.SUBSTRING ||
                op == Operator.STARTS || op == Operator.ENDS)) {
            PostingsIteratorFeatures feat = new PostingsIteratorFeatures();
            feat.setCaseSensitive(true);
            feat.setQueryStats(qs);
            float[] scores = new float[part.getMaxDocumentID() + 1];
            float valLen = nonWildCardLength(val);
            while(iter.hasNext()) {
                QueryEntry qe = iter.next();
                PostingsIterator vpi = qe.iterator(feat);
                if(vpi == null) {
                    continue;
                }

                String match = (String) qe.getName();
                while(vpi.next()) {
                    scores[vpi.getID()] =
                            Math.max(scores[vpi.getID()], valLen / match.length());
               }
            }

            //
            // Because of wildcard characters, the pattern may have been longer
            // than the values in the field, so we need to clamp the scores to 1.0
            // in order to have normalized values.
            for(int i = 0; i < scores.length; i++) {
                scores[i] = Math.min(scores[i], 1);
            }
            
            ScoredGroup sg = new ScoredGroup(part, scores);
            if(ag != null) {
                sg = (ScoredGroup) ag.intersect(sg);
            }
            return sg;
        }

        //
        // We'll always build a group of the terms from the iterator and
        // then mung them into a group, it's much faster.  For example, for
        // the MailFinder, to = vpn-interest <and> msg-date >= 01/01/2004
        // runs about 50 times faster using this approach!
        // 
        // Note that we're setting case sensitivity to true here.  This is 
        // because the case sensitivity for the various string operations that
        // produce a number of entries has been taken care of at the point where
        // the query entries were generated from the underlying dictionaries.
        QuickOr or = new QuickOr(null, 2048);
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures();
        feat.setCaseSensitive(true);
        feat.setQueryStats(qs);
        while(iter.hasNext()) {
            QueryEntry qe = iter.next();
            or.add(qe.iterator(feat));
        }
        ArrayGroup og = or.getGroup();

        //
        // Intersect with whatever constraint we were given.  We'll
        // intersect destructively with the group we just got, since we can
        // return that as a new group.
        if(ag != null) {
            og.destructiveIntersect(ag);
        }
        return og;
    }

    public List getQueryTerms(Comparator c) {
        return new ArrayList();
    }

    private int nonWildCardLength(String s) {
        int l = 0;
        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
                case '*':
                case '?':
                    continue;
                default:
                    l++;
            }
        }
        return l;
    }

    public String toString(String prefix) {
        String myStr = name + " " + op;
        if(op != Operator.RANGE) {
            myStr = myStr + " " + val + " (" + estSize + ")";
        } else {
            myStr = myStr + " " + (includeLower ? "[" : "{") + lowerBound + "-" + upperBound + (includeUpper
                    ? "]" : "}");
        }
        return super.toString(prefix) + " " + myStr + " (" + estSize + ")";
    }

} // FieldTerm

