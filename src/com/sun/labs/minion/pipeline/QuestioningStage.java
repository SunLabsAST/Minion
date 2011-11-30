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
package com.sun.labs.minion.pipeline;

import com.sun.labs.util.props.ConfigString;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import java.util.logging.Logger;

/**
 * A Stage that tries to identify the questions within a field, sending any
 * questions that are found to another field where they will be stored.
 * 
 */
public class QuestioningStage extends StageAdapter {

    private static final Logger logger = Logger.getLogger(QuestioningStage.class.getName());

    @ConfigString(defaultValue="question")
    public static final String PROP_QUESTION_FIELD = "question_field";

    private String questionFieldName;

    /**
     * Maximum buffer size to hold tokens that form a question
     */
    private static final int MAX_BUFFER_SIZE = 75;

    /**
     * Flag to keep internal state of whether we are keeping tokens from a question
     */
    private boolean inQuestion;

    /**
     * Buffer holding the tokens that form a question
     */
    private List<Token> tokenBuffer = new ArrayList<Token>();

    /**
     * Set of (English) words that start a question
     */
    private static final Set<String> questionStarters = new HashSet<String>();

    private boolean upperCaseStart = false;

    static {
        questionStarters.add("what");
        questionStarters.add("who");
        questionStarters.add("how");
        questionStarters.add("when");
        questionStarters.add("why");
        questionStarters.add("which");
        questionStarters.add("does");
        questionStarters.add("will");
        questionStarters.add("can");
        questionStarters.add("is");
    }

    @Override
    public void token(Token t) {
        String s = t.getToken();
        if(isQuestionStart(s)) {
            char startChar = s.charAt(0);
            startQuestion(Character.isUpperCase(startChar));
        }
        if(inQuestion) {
            tokenBuffer.add(t);
            if(tokenBuffer.size() > MAX_BUFFER_SIZE) {
                brokenQuestion();
            }
        } else {
            super.token(t);
        }
    }

    /**
     * Determine if the given string could be considered to be the start of a question.
     * This also depends on whether or not we are in a question, and if so whether the 
     * question began with an uppercase char.
     * @param s the string that may be a question starting word
     * @return true if the string could be considered to be the start of a question
     */
    private boolean isQuestionStart(String s) {
        //
        //If it's not a starter token return false
        if(!questionStarters.contains(s.toLowerCase())) {
            return false;
        }
        //
        //It's a starter token
        //
        //If I'm not in a question, return true
        if(!inQuestion) {
            return true;
        }
        //
        //I'm in an existing question
        //
        //If the starter token begins with an upper case char return true
        if(Character.isUpperCase(s.charAt(0))) {
            return true;
        }
        //
        //The starter token doesn't begin with an upper case char
        //
        //If the existing question began with an upper case, return false, otherwise true

        return !upperCaseStart;
    }

    @Override
    public void punctuation(Token p) {
        if(inQuestion) {
            tokenBuffer.add(p);
            String s = p.getToken();
            if(s.length() == 1) {
                switch(s.charAt(0)) {
                    case '?':
                        endQuestion();
                        break;

                    case '.':
                        brokenQuestion();
                        break;

                    default:
                        break;
                }
            }
            if(tokenBuffer.size() > MAX_BUFFER_SIZE) {
                brokenQuestion();
            }
        } else {
            super.punctuation(p);
        }
    }

    private void brokenQuestion() {
        inQuestion = false;
        flushTokens();
    }

    private void endQuestion() {
        if(inQuestion) {
            sendTokens();
        }
        inQuestion = false;
    }

    private void sendTokens() {
        if(!tokenBuffer.isEmpty()) {
            flushTokens();
        }
    }

    private void startQuestion(boolean characterCase) {
        //
        //We might decide that this is a bit heavy handed and ignore the start of 
        //a question "within" a question
        if(inQuestion) {
            //
            //Already in a question so flush current tokens
            flushTokens();
        }
        inQuestion = true;
        upperCaseStart = characterCase;
    }

    private void flushTokens() {
        if(!tokenBuffer.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for(Token t : tokenBuffer) {
                sb.append(t.getToken());
            }
            addField(questionFieldName, sb.toString());
            tokenBuffer.clear();
        }
    }

    @Override
    public void newProperties(PropertySheet propertySheet) throws PropertyException {
        super.newProperties(propertySheet);
        questionFieldName = propertySheet.getString(PROP_QUESTION_FIELD);
    }
}
