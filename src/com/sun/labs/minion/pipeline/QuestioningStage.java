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

import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.labs.minion.FieldInfo;

import com.sun.labs.util.props.ConfigBoolean;
import com.sun.labs.util.props.ConfigComponent;
import com.sun.labs.util.props.ConfigComponentList;
import java.util.logging.Logger;

/**
 * A Stage that tries to identify the questions within a document, and then
 * sends the tokens that form the questions down to the next stage delimited as a field.
 * @author Bernard Horan
 *
 */
public class QuestioningStage extends StageAdapter {

    /**
     * A list of the fields containing questions that we would like to process.
     */
    @ConfigComponentList(type = com.sun.labs.minion.FieldInfo.class)
    public static final String PROP_QUESTION_CONTAINING_FIELDS =
            "question_containing_fields";

    /**
     * Whether we should look for questions in non-field text.
     */
    @ConfigBoolean(defaultValue = true)
    public static final String PROP_CHECK_NON_FIELD_QUESTIONS =
            "check_non_field_questions";

    private boolean checkNonFieldQuestions;

    @ConfigComponent(type = com.sun.labs.minion.FieldInfo.class)
    public static final String PROP_QUESTION_FIELD = "question_field";

    private FieldInfo questionField;

    /**
     * Maximum buffer size to hold tokens that form a question
     */
    private static final int MAX_BUFFER_SIZE = 75;

    /**
     * Log
     */
    static final Logger logger = Logger.getLogger(QuestioningStage.class.getName());

    /**
     * A flag indicating whether we are in a field for which we're collecting
     * questions.
     */
    private boolean inQuestionField;

    /**
     * Flag to keep internal state of whether we are keeping tokens from a question
     */
    private boolean inQuestion;

    /**
     * Flag indicating whether we're in a field or not.
     */
    private boolean inField;

    /**
     * Buffer holding the tokens that form a question
     */
    private List<Token> tokenBuffer = new ArrayList<Token>();

    /**
     * Set of (English) words that start a question
     */
    private static Set<String> QUESTION_STARTERS;

    /**
     * The set of fields that we will check for questions.  If this set
     * is empty, then questions will be looked for in all fields.
     */
    private Set<String> questionContainingFields;

    private boolean upperCaseStart = false;

    private String saveKey;



    static {
        initializeQuestionStarters();
    }

    @Override
    public void token(Token t) {
        if(isCheckingTokens()) {
            String s = t.getToken();
            if(isQuestionStart(s)) {
                char startChar = s.charAt(0);
                startQuestion(Character.isUpperCase(startChar));
            }
            if(inQuestion) {
                buffer(t);
                if(tokenBuffer.size() > MAX_BUFFER_SIZE) {
                    logger.warning("buffer size exceeded");
                    brokenQuestion();
                }
            } else {
                super.token(t);
            }
        } else {
            super.token(t);
        }
    }

    /**
     * Am I checking tokens?<br>Depends if I'm in a field that's been
     * identified as one that I'm checking for questions, or (if there isn't one) if
     * I'm checking for questions when I'm not in a field
     * @return true if I'm checking tokens in the current field (if specified)
     */
    private boolean isCheckingTokens() {
        return inQuestionField || (checkNonFieldQuestions && !inField);
    }

    /**
     * Initialize the set of question starting words
     */
    private static void initializeQuestionStarters() {
        QUESTION_STARTERS = new HashSet<String>();
        QUESTION_STARTERS.add("what");
        QUESTION_STARTERS.add("who");
        QUESTION_STARTERS.add("how");
        QUESTION_STARTERS.add("when");
        QUESTION_STARTERS.add("why");
        QUESTION_STARTERS.add("which");
        QUESTION_STARTERS.add("does");
        QUESTION_STARTERS.add("will");
        QUESTION_STARTERS.add("can");
        QUESTION_STARTERS.add("is");
    }

    /**
     * Determine if the parameter string could be considered to be the start of a question.
     * This also depends on whether or not we are in a question, and if so whether the 
     * question began with an uppercase char.
     * @param s the string that may be a question starting word
     * @return true if the string could be considered to be the start of a question
     */
    private boolean isQuestionStart(String s) {
        //
        //If it's not a starter token return false
        if(!QUESTION_STARTERS.contains(s.toLowerCase())) {
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
            buffer(p);
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
                logger.warning("buffer size exceeded");
                brokenQuestion();
            }
        } else {
            super.punctuation(p);
        }
    }

    private void brokenQuestion() {
        inQuestion = false;
        flushTokens(false);
    }

    private void endQuestion() {
        if(inQuestion) {
            //log.log(logTag, MinionLog.LOG, "Ending question");
            //outputBuffer();
            sendTokens();
        }
        inQuestion = false;
    }

    private void sendTokens() {
        if(tokenBuffer.size() < 1) {
            logger.warning("no tokens to send");
        }
        flushTokens(true);
    }

    private void buffer(Token t) {
        tokenBuffer.add(t);
    }

    private void startQuestion(boolean characterCase) {
        //
        //We might decide that this is a bit heavy handed and ignore the start of 
        //a question "within" a question
        if(inQuestion) {
            //
            //Already in a question so flush current tokens
            flushTokens(false);
        }
        inQuestion = true;
        upperCaseStart = characterCase;
    }

    private void flushTokens(boolean saveData) {
        if(tokenBuffer.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for(Token t : tokenBuffer) {
                if(t.getType() == Token.PUNCT) {
                    super.punctuation(t);
                } else {
                    super.token(t);
                }
                if(saveData) {
                    sb.append(t.getToken());
                }
            }
            tokenBuffer.clear();
        }
    }

    @Override
    public void newProperties(PropertySheet propertySheet) throws PropertyException {
        super.newProperties(propertySheet);
        List temp = propertySheet.getComponentList(
                PROP_QUESTION_CONTAINING_FIELDS);
        questionContainingFields = new HashSet<String>();
        for(Object o : temp) {
            questionContainingFields.add(((FieldInfo) o).getName());
        }
        checkNonFieldQuestions = propertySheet.getBoolean(
                PROP_CHECK_NON_FIELD_QUESTIONS);
        questionField = (FieldInfo) propertySheet.getComponent(
                PROP_QUESTION_FIELD);
    }

    public Set<String> getQuestionContainingFields() {
        return questionContainingFields;
    }

    public void setQuestionContainingFields(Set<String> questionFields) {
        this.questionContainingFields = new HashSet<String>(
                questionContainingFields);
    }
}
