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

package com.sun.labs.minion.lexmorph;  // gotta have this for Pipe hookup

//import com.sun.labs.minion.pipeline.DocumentEvent;
//import com.sun.labs.minion.pipeline.Pipe;
import com.sun.labs.minion.pipeline.Stage;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;


// can't import stuff that is covered by package
// import Lexicon; // note path is ../../lexicon/  need this in classpath
// import State;
// import Phrase;
// import NominalPhrase;

// ///import nova.jan.NovaIndexServer; //keep just abstract version
// import Receiver;


/////public class ATNPhraseExtractor extends Lexicon {
// modified to use externmal Lexicon
public class ATNPhraseExtractor {
	class Pipe {
		
	}

/* all the class variables for the extractor go here */
  //====================================
 /* Next 46 items from /lab/indexing/java/phrase_extraction/lispATN/phraseStructs.lisp
 */
  static  State  cardinalStart ;
  static  State  cityAfterCountry ;
  static  State  cityAfterState ;
  static  State  cityStart ;
  static  State  dateAfterDay ;
  static  State  dateAfterMonth ;
  static  State  dateAfterMonthCardinal ;
  static  State  dateAfterMonthDay ;
  static  State  dateAfterMonthDayCardinal ;
  static  State  dateAfterMonthDayHour ;
  static  State  dateAfterMonthDayHourMinutes ;
  static  State  dateAfterMonthDayHourMinutesSeconds ;
  static  State  dateAfterTime ;
  static  State  dateAfterWeekday ;
  static  State  detStart ;
  static  State  firstnameStart ;
  static  State  generalPhraseStart ;
  static  State  lastnameStart ;
  static  State  monthStart ;
  static  State  nameAfterFirst ;
  static  State  nameAfterLast ;
  static  State  nameAfterTitle ;
  static  State  npAfterAdj ;
  static  State  npAfterAdv ;
  static  State  npAfterAn ;
  static  State  npAfterCity ;
  static  State  npAfterDet ;
  static  State  npAfterNoun ;
  static  State  npAfterPostmod ;
  static  State  npAfterVerb ;
  static  State  numberStart ;
  static  State  ordinalStart ;
  static  State  popDate ;
  static  State  popNpStack ;
  static  State  prefixtitleStart ;
  static  State  prepositionStart ;
  static  State  singleletterStart ;
  static  State  weekdayStart ;
  static  State  xAfterCardinal ;
  static  State  xAfterCardinalAnd ;
  static  State  xAfterNumber ;
  static  State  xAfterOrdinal ;
  static  State  xAfterOrdinalTimeUnit ;
  static  State  xAfterPreposition ;
 public static final int  CARDINALSTART = 0 ,
                        CITYAFTERCOUNTRY = 1 ,
                        CITYAFTERSTATE = 2 ,
                        CITYSTART = 3 ,
                        DATEAFTERDAY = 4 ,
                        DATEAFTERMONTH = 5 ,
                        DATEAFTERMONTHCARDINAL = 6 ,
                        DATEAFTERMONTHDAY = 7 ,
                        DATEAFTERMONTHDAYCARDINAL = 8 ,
                        DATEAFTERMONTHDAYHOUR = 9 ,
                        DATEAFTERMONTHDAYHOURMINUTES = 10 ,
                        DATEAFTERMONTHDAYHOURMINUTESSECONDS = 11 ,
                        DATEAFTERTIME = 12 ,
                        DATEAFTERWEEKDAY = 13 ,
                        DETSTART = 14 ,
                        FIRSTNAMESTART = 15 ,
                        GENERALPHRASESTART = 16 ,
                        LASTNAMESTART = 17 ,
                        MONTHSTART = 18 ,
                        NAMEAFTERFIRST = 19 ,
                        NAMEAFTERLAST = 20 ,
                        NAMEAFTERTITLE = 21 ,
                        NPAFTERADJ = 22 ,
                        NPAFTERADV = 23 ,
                        NPAFTERAN = 24 ,
                        NPAFTERCITY = 25 ,
                        NPAFTERDET = 26 ,
                        NPAFTERNOUN = 27 ,
                        NPAFTERPOSTMOD = 28 ,
                        NPAFTERVERB = 29 ,
                        NUMBERSTART = 30 ,
                        ORDINALSTART = 31 ,
                        POPDATE = 32 ,
                        POPNPSTACK = 33 ,
                        PREFIXTITLESTART = 34 ,
                        PREPOSITIONSTART = 35 ,
                        SINGLELETTERSTART = 36 ,
                        WEEKDAYSTART = 37 ,
                        XAFTERCARDINAL = 38 ,
                        XAFTERCARDINALAND = 39 ,
                        XAFTERNUMBER = 40 ,
                        XAFTERORDINAL = 41 ,
                        XAFTERORDINALTIMEUNIT = 42 ,
                        XAFTERPREPOSITION = 43 ;
 /* Next 318 items from /lab/indexing/java/phrase_extraction/lispATN/transNoDecls.lisp
 */
  int  sp_unsetNumberValue ;
  Word[]  sp_monthNameList ;
  static  Word  word_Someunknownmont_Etc ;
 /* parameter to turn on tracing of phrase extraction. */
  boolean  scantrace ;
 /* file to print trace information to. */
  PrintStream  scantraceFile ;
 /* parameter that causes scantrace to print processed words in lowercase. */
  boolean  sp_scantraceInLowercase ;
 /* turns off scanner printout */
  boolean  sp_suppressPrintout ;
 /* causes getWord to index all words found by scanner */
  boolean  sp_indexAllWordsFlag ;
 /* parameter that triggers phrase extraction in addition to name recognition. */
  boolean  sp_findPhrasesFlag ;
 /* parameter to cause name scanner to also attempt to tag other parts of speech. */
  boolean  sp_tagPosFlag ;
 /* parameter that causes followParallelNounPhrase and switchToPhrase to be
  disabled in the grammar and replaced by automatic phrase tracking
  in parseWord (which the grammar can start and stop but doesn't
  have to maintain. */
  boolean  sp_autoPhraseFlag ;
 /* parameter that causes recognition of a name to suppress
  an alternative interpretation as a noun -- e.g. Bill Woods. */
  boolean  sp_namesSuppressNounsFlag ;
 /* parameter that causes scanner to suppress
  noun phrases whose heads are npr's. */
  boolean  sp_suppressProperHeads ;
 /* parameter that causes scanner to suppress
  verb phrases whose objects are npr's. */
  boolean  sp_suppressProperObjectVerbPhrases ;
 /* parameter that causes scanner to check for
  agreement between determiners and head nouns. */
  boolean  sp_checkDeterminerAgreement ;
 /* parameter that causes scanner to check that noun
  phrases either have determiners or qualify for no
  determiner by being plural, mass, or other exception. */
  boolean  sp_requireDeterminerJustification ;
 /* parameter to cause scanner to parse conjunctions. */
  boolean  sp_includeConjunctions ;
  static  Word[]  wordSet_Aisle$Apartment_Etc ;
 /* nouns that frequently take postmods that are numbers or letters. */
  Word[]  sp_postmodNouns ;
 /* forces nouunPhraseStart to take an advQualifier as
    an advQualifier even when if could also be a verb */
  boolean  sp_forceAdvQualifier ;
 /* maximum number of years into the future that a
  two digit date will be taken to refer. */
  int  sp_futureHorizon ;
 /* variable to control the printout of found words (for timing) */
  boolean  suppressPrintout ;
 /* variable to control sensitivity for recording questionable names */
  int  confidenceThreshold ;
  static  Object  object_Indexscannedwor_Etc ;
 /* parameter to name a function to be called for every word found by indexFile.  
  The args will be (word capcode sentenceStartFlag file beg end).
  The variable *indexWordFn*, which causes indexFile to
  process every word, will be set to this value when indexFile
  is called.  Note: *processWordFn* could be used to effect a
  simple debugging break by setting *debugStopAtWords* to a list
  of words at any of which you'd like to enter a debugging break
  before proceeding, and then setting *processWordFn* to
  'debugBreakCheck, the name of a simple debugging break function. */
  Object  sp_processWordFn ;
 /* parameter to name a function to be called for every multiword phrase
  found by indexFile.
  The args will be (phrase file beg end). */
  Object  sp_processPhraseFn ;
 /* parameter to name a function to be called for every phrase
  found by indexFileNamesPhrases.
  The args will be (phrase file beg end). */
  Object  sp_processNamePhraseFn ;
 /* specifies list of words at which processing will pause
   in a debugging break.  This break happens in processWord
   after the current word has been identified and before
   the grammar rules are applied. */
  Word[]  sp_stopAtWords ;
 /* specifies list of words at which processing will pause
   in a debugging break when *processWordFn* is set to
   'debugBreakCheck. */
  Word[]  sp_debugStopAtWords ;
  static  Object  object_Indexscannedphr_Etc ;
  static  Word[]  wordSet_Almost$Down$Few_Etc ;
 /*  Words that are break words but that breakwordTest should reject */
  Word[]  sp_dontBreakOnTheseWords ;
  static  Word[]  wordSet_All$Both$Les$Lo_Etc ;
 /* words to be selected by canBePluralDeterminer */
  Word[]  sp_pluralDeterminerWords ;
  static  Word[]  wordSet_A$An$Another$Ea_Etc ;
 /* these are ruled out by a test in mustBeMassOrPlural.  They are mainly 
   singular determiners but have a few strange plural ones as well */
  Word[]  sp_bannedAsMassOrPluralDeterminers ;
  static  Word[]  wordSet_A$An$Another$Ea_Etc$ ;
 /* words to be selected by canBesingularDeterminer */
  Word[]  sp_singularDeterminerWords ;
  static  Word[]  wordSet_All$Any_More$An_Etc ;
 /* words to be rejected by countDeterminer */
  Word[]  sp_bannedAsCountDeterminerWords ;
  static  Atom  feature_Breakword ;
  static  Category  categorySet_Det$Number ;
  static  Atom  feature_Detfeatures ;
  static  Atom  feature_Pldet ;
  static  Atom  feature_Sgdet ;
  static  Atom  feature_Massdet ;
  static  Word  word_Roman_Numeral ;
  static  Atom[]  featureSet_Pldet ;
  static  Atom[]  featureSet_Sgdet$Massdet ;
  static  Atom[]  featureSet_Sgdet ;
  static  Atom[]  featureSet_Massdet ;
  static  Atom[]  featureSet_Countdet$Pldet ;
  static  Atom[]  featureSet_Countdet ;
  static  Category  categorySet_Number ;
  static  Category  categorySet_Det ;
  static  Category  categorySet_Nm ;
  static  Category  categorySet_Nc ;
  static  Word  word_Unit_Of_Countin_Etc ;
  static  Category  categorySet_N ;
  static  Category  categorySet_Npl ;
  static  Word[]  wordSet_How_Many$A$An ;
  static  Atom  feature_Indobj ;
  static  Category  categorySet_Npl$Nm ;
  static  Category  categorySet_Npl$Unit ;
  static  Category  categorySet_Adv$Qualifier$N_Etc ;
  static  Atom  feature_Adv ;
  static  Category  categorySet_Adv ;
  static  Category  categorySet_Adv$Clausal$Adv_Etc ;
  static  Word[]  wordSet_Afternoon$Eveni_Etc ;
  Word[]  sp_daytimeWords ;
  static  Category  categorySet_Adj$Ord ;
  static  Atom  feature_Derivation ;
  static  Category  categorySet_Unit ;
  static  Word  word_Unit ;
  static  Word  word_Unit_Of_Time ;
  static  Atom  feature_Monthdays ;
  static  Category  categorySet_Integer$Ord ;
  static  Atom  atom_Past ;
  static  Atom  atom_Future ;
  static  Atom  atom_Abstract ;
  static  Category  categorySet_Adj$N ;
  static  Category  categorySet_Adj ;
  static  Category  category_Adv$Adj$N ;
  static  Value  capcode_Uc ;
  static  Category  categorySet_Npr ;
  static  Value  capcode_Ic ;
  static  Atom[]  featureSet_Lc$Ic ;
  static  Value  capcode_Lc ;
  static  Category  categorySet_Det$Prep$Conj$P_Etc ;
  static  Word  word_$S ;
  static  Word  word_Backslashtick ;
  static  Category  category_Unknown ;
  static  String  staticString80Blanks ;
  static  Category  categorySet_Det$Prep$Conj ;
  static  Category  categorySet_Vt ;
  static  Category  categorySet_N$V$Adj$Adv$Con_Etc ;
 /* all the non name cats that mean a word can be something else */
  Category  sp_allNonnameCats ;
  static  Atom  feature_Nameconfidence ;
  static  Category  categorySet_Firstname$Malef_Etc ;
  static  Category  category_Name ;
  static  Category  categorySet_Name ;
  static  Category  category_Number ;
  static  Category  category_Cardinal ;
  static  Category  category_Integer ;
  static  Category  category_City ;
  static  Category  category_Prefixtitle ;
  static  Category  category_Title ;
  static  Category  category_Dualname ;
  static  Category  categorySet_Firstname$Malef_Etc$ ;
  static  Atom  feature_Lastname ;
  static  Category  category_Firstname ;
  static  Category  category_Lastname ;
  static  Category  categorySet_Lastname ;
  static  Category  category_Numberunit ;
  static  Atom  feature_Unit_Of_Countin_Etc ;
  static  Category  category_N ;
  static  Category  category_Ordinal ;
  static  Category  category_Ord ;
  static  Category  category_Determiner ;
  static  Category  category_Det ;
  static  Category  category_Monthname ;
  static  Category  category_Month ;
  static  Category  category_Namesuffix ;
  static  Category  category_Statename ;
  static  Category  categorySet_Statename$State_Etc ;
  static  Category  category_Noun ;
  static  Category  category_Verb ;
  static  Category  category_V ;
  static  Category  category_Adjective ;
  static  Category  category_Adj ;
  static  Category  category_Adverb ;
  static  Category  category_Adv ;
  static  Category  category_Preposition ;
  static  Category  category_Prep ;
  static  Category  category_Conjunction ;
  static  Category  category_Conj ;
  static  Category  category_Interjection ;
  static  Category  category_Interj ;
  static  Word[]  wordSet_Sbreak$2cr ;
  static  Atom  feature_Monthconfidence ;
  static  Atom[]  featureSet_Ic$Uc ;
  static  Word[]  wordSet_May$March$Mar$D_Etc ;
  static  Category  categorySet_Month ;
  static  Word[]  wordSet_An$Man$May$Will_Etc ;
 /* words which are likely to be verbs but could be names */
  Word[]  sp_extremelyUnlikelyNames ;
  static  Word[]  wordSet_May$Will$Might$_Etc ;
 /* words that should not be allowed as last names without awesome evidence */
  Word[]  sp_verbsUnlikelyAsLastnames ;
  static  Category  categorySet_Monthname$Numbe_Etc ;
 /* The word classes that have phrase starters */
  Category  sp_wordclasses ;
  static  Word  word_Backslashquote ;
  static  Atom  feature_Wordclass ;
  static  Value  value_Singleletter ;
  static  Atom  feature_Stoplist ;
  static  Category  categorySet_Det$Prep$Pro ;
  static  Category  category_N$V$Adj$Ord$Adv ;
  static  Word[]  wordSet_A$The ;
  static  Word[]  wordSet_Is$Am$Are$Was$W_Etc ;
  static  Category  categorySet_Det$N$Adj$Adv$V_Etc ;
  static  Word  word_Comma ;
  static  Word  word_Dollar ;
  static  Word  word_$ ;
  static  Word  word_Slash ;
  static  Word  word_Minus ;
  static  Atom  feature_Weekdayconfiden_Etc ;
  static  Atom  feature_Number ;
  static  Atom  feature_Plural ;
  static  Word  word_That ;
  static  Word  word_A ;
  static  Word[]  wordSet_A$I ;
  static  Word  word_Dot ;
  static  Category  categorySet_Ord$Integer ;
  static  Word  word_The ;
  static  Word[]  wordSet_A$M$P$M ;
  static  Word[]  wordSet_Am$Pm$A$M$P$M$N_Etc ;
  static  Word[]  wordSet_Edt$Est$Pdt$Pst_Etc ;
  static  Word  word_Timezone ;
  static  Word[]  wordSet_Thousand$Millio_Etc ;
  static  Word  word_Hundred ;
  static  Word  word_Currency ;
  static  Word[]  wordSet_Day$Week$Month ;
  static  Word  word_Of ;
  static  Word[]  wordSet_Of$In ;
  static  Word[]  wordSet_Hundredth$Thous_Etc ;
  static  Category  categorySet_Ord ;
  static  Category  categorySet_Integer ;
  static  Word  word_And ;
  static  Word[]  wordSet_March$May ;
  static  Word[]  wordSet_In$Of ;
  static  Word[]  wordSet_Charset_Comma ;
  static  Word[]  wordSet_Charset_Dot ;
  static  Word[]  wordSet_Charset_Colonco_Etc ;
  static  Word  word_Colon ;
  static  Word[]  wordSet_Charset_Colon ;
  static  Object  nIL_Priorword ;
  static  Object  nIL_And ;
  static  Value  value_Dualname ;
  static  Category  category_Singleletter ;
  static  Category  categorySet_Firstname ;
  static  Category  category_N$V$Adj$Adv ;
  static  Category  categorySet_V$Adv ;
  static  Category  categorySet_Det$Prep$Pro$Nu_Etc ;
  static  Atom[]  featureSet_Lc$Uc ;
  static  Word  word_My ;
  static  Category  categorySet_N$Adj$Adv ;
  static  Word[]  wordSet_See$So$Soon ;
  static  Category  categorySet_Nameprefix ;
  static  Category  categorySet_Det$N$Adj$Adv ;
  static  Atom  feature_Ic ;
  static  Category  categorySet_Namesuffix ;
  static  Word[]  wordSet_San$Some ;
  static  Word  word_Will ;
  static  Category  categorySet_Adv$V ;
  static  Word  word_More ;
  static  Word  word_2cr ;
  static  Category  categorySet_Prefixtitle ;
  static  Word  word_Sbreak ;
  static  Category  categorySet_Firstname$Dualn_Etc ;
  static  Word  word_To ;
  static  Category  categorySet_V ;
  static  Category  categorySet_Prespart ;
  static  Word[]  wordSet_After$Before$Fo_Etc ;
  static  Word  word_Day ;
  static  Category  categorySet_City ;
  static  Word[]  wordSet_A ;
  static  Category  categorySet_Cardinal ;
  static  Category  categorySet_Predet$Pro ;
  static  Category  categorySet_Postdet ;
  static  Word[]  wordSet_And$Or ;
  static  Word[]  wordSet_Percent ;
  static  Category  categorySet_Firstname$Lastn_Etc ;
  static  Word[]  wordSet_Less$More$Most ;
  static  Word[]  wordSet_Less$More$Most$_Etc ;
  static  Word  word_Way ;
  static  Category  categorySet_Npl$Nm$Pro$Det$_Etc ;
  static  Category  categorySet_V$Det$Number ;
  static  Category  categorySet_Adj$Ord$Number ;
  static  Category  categorySet_Adv$Adj$Ord ;
  static  Word[]  wordSet_A$An$Several$Th_Etc ;
  static  Word[]  wordSet_A$An$The$This$T_Etc ;
 /* not independently semantically important so don't record with phrase */
  Word[]  sp_trivialDeterminers ;
  static  Word  word_On ;
  static  Category  categorySet_Weekday ;
  static  Word[]  wordSet_$Endoffile$ ;
  static  Word  word_Number ;
  static  Word  word_An ;
  static  Word[]  wordSet_Next$Last$Each$_Etc ;
  static  Word[]  wordSet_Earlier$Sooner$_Etc ;
  static  Word[]  wordSet_After$Before$Fr_Etc ;
  static  Word[]  wordSet_The$A$An ;
  static  Category  categorySet_Adj$Ord$Adv ;
  static  Category  categorySet_Det$N$Number ;
  static  Word[]  wordSet_Can$Do$Does$Wil_Etc ;
  static  Category  categorySet_Pro ;
  static  Word[]  wordSet_Do$Does ;
  static  Category  categorySet_Npl$Nm$Pro$Det$_Etc$ ;
  static  Word  word_$Nc$Relative$Pe_Etc ;
  static  Word[]  wordSet_Be$But$For ;
  static  Category  categorySet_Adj$Post ;
  static  Word[]  wordSet_Both$Each$La$Le_Etc ;
  static  Category  categorySet_Det$Number$Prep ;
  static  Word[]  wordSet_Of ;
  static  Word[]  wordSet_An ;
  static  Category  categorySet_Adv$Adj$N ;
  static  Category  categorySet_N$V$Adv$Adj ;
  static  Category  categorySet_Statecode ;
  static  Category  categorySet_Country ;
  static  Word  word_Region ;
  static  Category  categorySet_Adv$Ord ;
  static  Atom[]  featureSet_Uc$Ic ;
  static  Category  categorySet_Title ;
  static  Atom  feature_Abbrev ;
  static  Word[]  wordSet_A$An$Several$Th_Etc$ ;
  static  Word[]  wordSet_Next$Last ;
  static  Category  categorySet_Nsg ;
  static  Word[]  wordSet_A$An ;
  static  Category  category_Npr ;
  static  Category  categorySet_V$Nm;
  static  Category  category_Nc ;
  static  Category  category_Nm ;
  static  Category  categorySet_N$V$Adj$Adv ;
  static  Word[]  wordSet_Both$Each$La$Le_Etc$ ;
  static  Category  categorySet_Adv$Qualifier ;
 /* Next 159 items from /lab/indexing/java/phrase_extraction/lispATN/transLexSetup.lisp
 */
  String  scanvowels ;
  String  scanconsonants ;
  String  whitespaceCharacters ;
  String  punctuationCharacters ;
  String  digits ;
  Atom[]  charType ;
  static  Atom  atom_Vowel ;
  static  Atom  atom_Consonant ;
  static  Atom[]  featureSet_Vowel$Consonant ;
  static  Atom  atom_Digit ;
  static  Atom  atom_White ;
  static  Atom  atom_Punct ;
  static  Atom[]  featureSet_White$Punct ;
  static  Category  category_Weekday ;
  static  Word  word_5 ;
  static  Word  word_9 ;
  static  Word  word_January ;
  static  Word  word_1 ;
  static  Word  word_31 ;
  static  Word  word_February ;
  static  Word  word_2 ;
  static  Word  word_28 ;
  static  Word  word_March ;
  static  Word  word_3 ;
  static  Word  word_6 ;
  static  Word  word_April ;
  static  Word  word_4 ;
  static  Word  word_30 ;
  static  Word  word_8 ;
  static  Word  word_May ;
  static  Word  word_June ;
  static  Word  word_July ;
  static  Word  word_7 ;
  static  Word  word_August ;
  static  Word  word_September ;
  static  Word  word_October ;
  static  Word  word_10 ;
  static  Word  word_November ;
  static  Word  word_11 ;
  static  Word  word_December ;
  static  Word  word_12 ;
  static  Word  word_Jan ;
  static  Word  word_Feb ;
  static  Word  word_Mar ;
  static  Word  word_Apr ;
  static  Word  word_Jun ;
  static  Word  word_Jul ;
  static  Word  word_Aug ;
  static  Word  word_Sept ;
  static  Word  word_Sep ;
  static  Word  word_Oct ;
  static  Word  word_Nov ;
  static  Word  word_Dec ;
  static  Word  word_Sunday ;
  static  Word  word_Monday ;
  static  Word  word_Tuesday ;
  static  Word  word_Wednesday ;
  static  Word  word_Thursday ;
  static  Word  word_Friday ;
  static  Word  word_Saturday ;
  static  Word  word_Sundays ;
  static  Word  word_Mondays ;
  static  Word  word_Tuesdays ;
  static  Word  word_Wednesdays ;
  static  Word  word_Thursdays ;
  static  Word  word_Fridays ;
  static  Word  word_Saturdays ;
  static  Word  word_Sun ;
  static  Word  word_Mon ;
  static  Word  word_Tues ;
  static  Word  word_Tue ;
  static  Word  word_Wed ;
  static  Word  word_Thurs ;
  static  Word  word_Fri ;
  static  Word  word_Sat ;
  static  Word  word_Su ;
  static  Word  word_Mo ;
  static  Word  word_Tu ;
  static  Word  word_Th ;
  static  Word  word_Fr ;
  static  Word  word_Sa ;
  static  Word  word_At ;
  static  Word  word_In ;
  static  Word  word_Since ;
  static  Word  word_Before ;
  static  Word  word_After ;
  static  Word  word_During ;
  static  Word  word_Till ;
  static  Word  word_Until ;
  static  Word  word_From ;
  static  Word  word_Through ;
  static  Word  word_Now ;
  static  Category  category_Date ;
  static  Word  word_Today ;
  static  Word  word_Tomorrow ;
  static  Word  word_Yesterday ;
  static  Word  word_Late ;
  static  Word  word_Early ;
  static  Word  word_Fall ;
  static  Word  word_Winter ;
  static  Word  word_Spring ;
  static  Word  word_Summer ;
  static  Category  category_Timeunit ;
  static  Word  word_Week ;
  static  Word  word_Month ;
  static  Word  word_Year ;
  static  Word  word_Hour ;
  static  Word  word_Minute ;
  static  Word  word_Thousand ;
  static  Word  word_Last ;
  static  Word  word_Next ;
  static  Word  word_This ;
  static  Word  word_President ;
  static  Word  word_Vicepresident ;
  static  Word  word_B ;
  static  Word  word_C ;
  static  Word  word_D ;
  static  Word  word_E ;
  static  Word  word_F ;
  static  Word  word_G ;
  static  Word  word_H ;
  static  Word  word_I ;
  static  Word  word_J ;
  static  Word  word_K ;
  static  Word  word_L ;
  static  Word  word_M ;
  static  Word  word_N ;
  static  Word  word_O ;
  static  Word  word_P ;
  static  Word  word_Q ;
  static  Word  word_R ;
  static  Word  word_S ;
  static  Word  word_T ;
  static  Word  word_U ;
  static  Word  word_V ;
  static  Word  word_W ;
  static  Word  word_X ;
  static  Word  word_Y ;
  static  Word  word_Z ;
  static  Word  word_Jr ;
  static  Word  word_Sr ;
  static  Word  word_Ii ;
  static  Word  word_Iii ;
  static  Word  word_Iv ;
  static  Word  word_Esq ;
  static  Word  word_Van ;
  static  Category  category_Lastnameprefix ;
  static  Word  word_Von ;
  static  Word  word_De ;
  static  Word  word_Der ;
  static  Word  word_Da ;
  static  Word  word_Mac ;
  static  Word  word_Mc ;
  static  Word  word_St ;
  static  Word  word_Bill ;
  static  Word  word_Woods ;
  static  Word  word_Gross ;
  static  Atom  atom_Numericalvalue ;
  static  Word  word_144 ;
 /* Next 36 items from /lab/indexing/java/phrase_extraction/lispATN/transHandWork.lisp
 */
  WordBuffer  punctbuffer ;
  static  Word  word_Indent ;
  static  Word[]  wordSet_Charset_Backsla_Etc ;
  static  Word  word_Space ;
  static  Word  word_Backslashn ;
  static  Word  word_Backslashr ;
  static  Word  word_Backslashu0010 ;
  static  Word  word_Backslashf ;
  static  Category  categorySet_Singleletter$Nu_Etc ;
  static  Word[]  wordSet_Charset_Queryba_Etc ;
  static  Word  word_$Endoffile$ ;
  static  Atom  feature_Numeralnumberp ;
  Word  sp_word5 ;
  Word  sp_word6 ;
  Word  sp_word7 ;
  Word  sp_word8 ;
  Word  sp_word9 ;
  static  Atom  atom_Mac ;
  static  Atom  feature_Compounds ;
  static  Atom  feature_Word$Class ;
  static  Atom  atom_String ;
  static  Atom  feature_Known ;
  static  Value  value_T ;
  static  Atom  feature_Guessed ;
  static  Atom  feature_Startfunction ;
  static  Value  value_Guessed ;
  static  Atom  feature_Penalties ;
  static  Object  nIL_Th ;
  static  Object  nIL_St ;
  static  Object  nIL_Nd ;
  static  Object  nIL_Rd ;
 /* the curent year */
  int  sp_thisYear ;
// added public marks on methods 1 aug 01  pmartin
////
// added to support calls to the concept Store... pmartin 4aug00

public Receiver csReceiver = null;


////////////added 31 July 01 for Stephen's pipe connections

    /**
     * The <code>Pipe</code> where output will be written.
     */
    protected Pipe outputPipe = null;

    /**
     * The <code>Stage</code> where output will be written.
     */
    protected Stage outputStage = null;

    /**
     * Set the <code>Pipe</code> where phrases will be written when
     * transmit is called.
     */
    public void setPipe(Pipe p) {
	outputPipe = p;
    }

    /**
     * Set the <code>Stage</code> where phrases will be written when
     * transmit is called.
     */
    public void setStage(Stage s) {
	outputStage = s;
    }

///////////////end of pipe connections additions


// end of the spliced instance vars from atnpe2...
  //====================================


/* after the class var declarations */


///// modified to use external Lexicon .. pmartin 31 july 01
/////////////////////////

Lexicon lex;  //the external Lexicon

public ATNPhraseExtractor(){
   this(new Lexicon());
}

public ATNPhraseExtractor(int wordTabSiz, int atomTabSiz){
       this(new Lexicon(wordTabSiz, atomTabSiz));
}

public ATNPhraseExtractor(Lexicon externalLex){
    lex = externalLex;


    /*  all the value setting for the 'global'  instance vars goes here...*/
    //==========================================
  cardinalStart  =  new State( "cardinalStart" ,  CARDINALSTART );
  cityAfterCountry  =  new State( "cityAfterCountry" ,  CITYAFTERCOUNTRY );
  cityAfterState  =  new State( "cityAfterState" ,  CITYAFTERSTATE );
  cityStart  =  new State( "cityStart" ,  CITYSTART );
  dateAfterDay  =  new State( "dateAfterDay" ,  DATEAFTERDAY );
  dateAfterMonth  =  new State( "dateAfterMonth" ,  DATEAFTERMONTH );
  dateAfterMonthCardinal  =  new State( "dateAfterMonthCardinal" ,  DATEAFTERMONTHCARDINAL );
  dateAfterMonthDay  =  new State( "dateAfterMonthDay" ,  DATEAFTERMONTHDAY );
  dateAfterMonthDayCardinal  =  new State( "dateAfterMonthDayCardinal" ,  DATEAFTERMONTHDAYCARDINAL );
  dateAfterMonthDayHour  =  new State( "dateAfterMonthDayHour" ,  DATEAFTERMONTHDAYHOUR );
  dateAfterMonthDayHourMinutes  =  new State( "dateAfterMonthDayHourMinutes" ,  DATEAFTERMONTHDAYHOURMINUTES );
  dateAfterMonthDayHourMinutesSeconds  =  new State( "dateAfterMonthDayHourMinutesSeconds" ,  DATEAFTERMONTHDAYHOURMINUTESSECONDS );
  dateAfterTime  =  new State( "dateAfterTime" ,  DATEAFTERTIME );
  dateAfterWeekday  =  new State( "dateAfterWeekday" ,  DATEAFTERWEEKDAY );
  detStart  =  new State( "detStart" ,  DETSTART );
  firstnameStart  =  new State( "firstnameStart" ,  FIRSTNAMESTART );
  generalPhraseStart  =  new State( "generalPhraseStart" ,  GENERALPHRASESTART );
  lastnameStart  =  new State( "lastnameStart" ,  LASTNAMESTART );
  monthStart  =  new State( "monthStart" ,  MONTHSTART );
  nameAfterFirst  =  new State( "nameAfterFirst" ,  NAMEAFTERFIRST );
  nameAfterLast  =  new State( "nameAfterLast" ,  NAMEAFTERLAST );
  nameAfterTitle  =  new State( "nameAfterTitle" ,  NAMEAFTERTITLE );
  npAfterAdj  =  new State( "npAfterAdj" ,  NPAFTERADJ );
  npAfterAdv  =  new State( "npAfterAdv" ,  NPAFTERADV );
  npAfterAn  =  new State( "npAfterAn" ,  NPAFTERAN );
  npAfterCity  =  new State( "npAfterCity" ,  NPAFTERCITY );
  npAfterDet  =  new State( "npAfterDet" ,  NPAFTERDET );
  npAfterNoun  =  new State( "npAfterNoun" ,  NPAFTERNOUN );
  npAfterPostmod  =  new State( "npAfterPostmod" ,  NPAFTERPOSTMOD );
  npAfterVerb  =  new State( "npAfterVerb" ,  NPAFTERVERB );
  numberStart  =  new State( "numberStart" ,  NUMBERSTART );
  ordinalStart  =  new State( "ordinalStart" ,  ORDINALSTART );
  popDate  =  new State( "popDate" ,  POPDATE );
  popNpStack  =  new State( "popNpStack" ,  POPNPSTACK );
  prefixtitleStart  =  new State( "prefixtitleStart" ,  PREFIXTITLESTART );
  prepositionStart  =  new State( "prepositionStart" ,  PREPOSITIONSTART );
  singleletterStart  =  new State( "singleletterStart" ,  SINGLELETTERSTART );
  weekdayStart  =  new State( "weekdayStart" ,  WEEKDAYSTART );
  xAfterCardinal  =  new State( "xAfterCardinal" ,  XAFTERCARDINAL );
  xAfterCardinalAnd  =  new State( "xAfterCardinalAnd" ,  XAFTERCARDINALAND );
  xAfterNumber  =  new State( "xAfterNumber" ,  XAFTERNUMBER );
  xAfterOrdinal  =  new State( "xAfterOrdinal" ,  XAFTERORDINAL );
  xAfterOrdinalTimeUnit  =  new State( "xAfterOrdinalTimeUnit" ,  XAFTERORDINALTIMEUNIT );
  xAfterPreposition  =  new State( "xAfterPreposition" ,  XAFTERPREPOSITION );
  sp_unsetNumberValue  =  -1 ;
  sp_monthNameList  =  new Word[] {lex.makeWord( "JANUARY" ), lex.makeWord( "FEBRUARY" ), lex.makeWord( "MARCH" ), lex.makeWord( "APRIL" ), lex.makeWord( "MAY" ), lex.makeWord( "JUNE" ), lex.makeWord( "JULY" ), lex.makeWord( "AUGUST" ), lex.makeWord( "SEPTEMBER" ), lex.makeWord( "OCTOBER" ), lex.makeWord( "NOVEMBER" ), lex.makeWord( "DECEMBER" )};
  word_Someunknownmont_Etc  = lex.makeWord( "SOMEUNKNOWNMONTH" );
  scantrace  =  false ;
  scantraceFile  =  null ;
  sp_scantraceInLowercase  =  false ;
  sp_suppressPrintout  =  false ;
  sp_indexAllWordsFlag  =  true ;
  sp_findPhrasesFlag  =  true ;
  sp_tagPosFlag  =  true ;
  sp_autoPhraseFlag  =  true ;
  sp_namesSuppressNounsFlag  =  true ;
  sp_suppressProperHeads  =  false ;
  sp_suppressProperObjectVerbPhrases  =  true ;
  sp_checkDeterminerAgreement  =  true ;
  sp_requireDeterminerJustification  =  true ;
  sp_includeConjunctions  =  false ;
  wordSet_Aisle$Apartment_Etc  = lex.makeWordSet( "AISLE/APARTMENT/BERTH/BUILDING/BUS/CAR/CELL/CHAPTER/CLAUSE/COLUMN/DISTRICT/EQUATION/FIGURE/GATE/GRADE/HIGHWAY/ITEM/JUNCTION/LANE/LEVEL/LINE/LOT/MOD/MODEL/NUMBER/PAGE/PARAGRAPH/PARCEL/PART/PLATFORM/POINT/PRECINCT/REFERENCE/RELEASE/ROOM/ROUND/ROUTE/ROW/SECTION/SENTENCE/SLIP/STAGE/STATE/SYSTEM/TERMINAL/TRACK/VERSE/VERSION/WARD/WORD" );
  sp_postmodNouns  =  wordSet_Aisle$Apartment_Etc ;
  sp_forceAdvQualifier  =  false ;
  sp_futureHorizon  =  25 ;
  suppressPrintout  =  false ;
  confidenceThreshold  =  5 ;
  object_Indexscannedwor_Etc  = lex.makeAtom( "indexscannedword" );
  sp_processWordFn  =  object_Indexscannedwor_Etc ;
  sp_processPhraseFn  =  null ;
  sp_processNamePhraseFn  =  null ;
  sp_stopAtWords  =  null ;
  sp_debugStopAtWords  =  null ;
  object_Indexscannedphr_Etc  = lex.makeAtom( "indexscannedphrase" );
  wordSet_Almost$Down$Few_Etc  = lex.makeWordSet( "ALMOST/DOWN/FEW/FEWER/FEWEST/GET/GIVE/LAST/LESS/MORE/MOST/NEARLY/NORMALLY/OCCASIONALLY/OFF/OFTEN/ONLY/OTHER/PROBABLY/RATHER/REALLY/SELDOM/SEVERAL/SOMETIMES/SOMEWHAT/TOO/UP/USUALLY/VERY/WELL" );
  sp_dontBreakOnTheseWords  =  wordSet_Almost$Down$Few_Etc ;
  wordSet_All$Both$Les$Lo_Etc  = lex.makeWordSet( "ALL/BOTH/LES/LOTS_OF/MANY/MOST/SEVERAL/SUCH/THESE/THOSE" );
  sp_pluralDeterminerWords  =  wordSet_All$Both$Les$Lo_Etc ;
  wordSet_A$An$Another$Ea_Etc  = lex.makeWordSet( "A/AN/ANOTHER/EACH/EVERY/LA/MORE_THAN_ONE/ONE/SUCH_A/BOTH/LES/MANY/SEVERAL/THESE/THOSE" );
  sp_bannedAsMassOrPluralDeterminers  =  wordSet_A$An$Another$Ea_Etc ;
  wordSet_A$An$Another$Ea_Etc$  = lex.makeWordSet( "A/AN/ANOTHER/EACH/EVERY/LA/MORE_THAN_ONE/ONE/SUCH_A/THAT/THIS" );
  sp_singularDeterminerWords  =  wordSet_A$An$Another$Ea_Etc$ ;
  wordSet_All$Any_More$An_Etc  = lex.makeWordSet( "ALL/ANY_MORE/ANYMORE/LESS/LOTS_OF/MORE/MOST/MUCH/OTHER/SUCH" );
  sp_bannedAsCountDeterminerWords  =  wordSet_All$Any_More$An_Etc ;
  feature_Breakword  = lex.makeAtom( "breakword" );
  categorySet_Det$Number  = lex.makeCategory( "DET/NUMBER" );
  feature_Detfeatures  = lex.makeAtom( "detfeatures" );
  feature_Pldet  = lex.makeAtom( "pldet" );
  feature_Sgdet  = lex.makeAtom( "sgdet" );
  feature_Massdet  = lex.makeAtom( "massdet" );
  word_Roman_Numeral  = lex.makeWord( "ROMAN_NUMERAL" );
  featureSet_Pldet  =  new Atom[] {lex.makeAtom( "pldet" )};
  featureSet_Sgdet$Massdet  =  new Atom[] {lex.makeAtom( "sgdet" ), lex.makeAtom( "massdet" )};
  featureSet_Sgdet  =  new Atom[] {lex.makeAtom( "sgdet" )};
  featureSet_Massdet  =  new Atom[] {lex.makeAtom( "massdet" )};
  featureSet_Countdet$Pldet  =  new Atom[] {lex.makeAtom( "countdet" ), lex.makeAtom( "pldet" )};
  featureSet_Countdet  =  new Atom[] {lex.makeAtom( "countdet" )};
  categorySet_Number  = lex.makeCategory( "NUMBER" );
  categorySet_Det  = lex.makeCategory( "DET" );
  categorySet_Nm  = lex.makeCategory( "NM" );
  categorySet_Nc  = lex.makeCategory( "NC" );
  word_Unit_Of_Countin_Etc  = lex.makeWord( "UNIT_OF_COUNTING" );
  categorySet_N  = lex.makeCategory( "N" );
  categorySet_Npl  = lex.makeCategory( "NPL" );
  wordSet_How_Many$A$An  = lex.makeWordSet( "HOW_MANY/A/AN" );
  feature_Indobj  = lex.makeAtom( "indobj" );
  categorySet_Npl$Nm  = lex.makeCategory( "NPL/NM" );
  categorySet_Npl$Unit  = lex.makeCategory( "NPL/UNIT" );
  categorySet_Adv$Qualifier$N_Etc  = lex.makeCategory( "ADV-QUALIFIER/NEG/NEG-ADV/QADV" );
  feature_Adv  = lex.makeAtom( "adv" );
  categorySet_Adv  = lex.makeCategory( "ADV" );
  categorySet_Adv$Clausal$Adv_Etc  = lex.makeCategory( "ADV-CLAUSAL/ADV-CLAUSE-FINAL/ADV-PART/ADV-PRE/ADV-PRED/ADV-SPECIAL" );
  wordSet_Afternoon$Eveni_Etc  = lex.makeWordSet( "AFTERNOON/EVENING/MORNING/NIGHTTIME/NOON" );
  sp_daytimeWords  =  wordSet_Afternoon$Eveni_Etc ;
  categorySet_Adj$Ord  = lex.makeCategory( "ADJ/ORD" );
  feature_Derivation  = lex.makeAtom( "derivation" );
  categorySet_Unit  = lex.makeCategory( "UNIT" );
  word_Unit  = lex.makeWord( "UNIT" );
  word_Unit_Of_Time  = lex.makeWord( "UNIT_OF_TIME" );
  feature_Monthdays  = lex.makeAtom( "monthdays" );
  categorySet_Integer$Ord  = lex.makeCategory( "INTEGER/ORD" );
  atom_Past  = lex.makeAtom( "past" );
  atom_Future  = lex.makeAtom( "future" );
  atom_Abstract  = lex.makeAtom( "abstract" );
  categorySet_Adj$N  = lex.makeCategory( "ADJ/N" );
  categorySet_Adj  = lex.makeCategory( "ADJ" );
  category_Adv$Adj$N  = lex.makeCategory( "ADV/ADJ/N" );
  capcode_Uc  = lex.makeAtom( "uc" );
  categorySet_Npr  = lex.makeCategory( "NPR" );
  capcode_Ic  = lex.makeAtom( "ic" );
  featureSet_Lc$Ic  =  new Atom[] {lex.makeAtom( "lc" ), lex.makeAtom( "ic" )};
  capcode_Lc  = lex.makeAtom( "lc" );
  categorySet_Det$Prep$Conj$P_Etc  = lex.makeCategory( "DET/PREP/CONJ/POSS" );
  word_$S  = lex.makeWord( "'S" );
  word_Backslashtick  = lex.makeWord( "\'" );
  category_Unknown  = lex.makeCategory( "UNKNOWN" );
  staticString80Blanks  =  "                                                                                " ;
  categorySet_Det$Prep$Conj  = lex.makeCategory( "DET/PREP/CONJ" );
  categorySet_Vt  = lex.makeCategory( "VT" );
  categorySet_N$V$Adj$Adv$Con_Etc  = lex.makeCategory( "N/V/ADJ/ADV/CONJ/DET/INTERJ/NUMBER/ORD/PREP/AUX" );
  sp_allNonnameCats  =  categorySet_N$V$Adj$Adv$Con_Etc ;
  feature_Nameconfidence  = lex.makeAtom( "nameconfidence" );
  categorySet_Firstname$Malef_Etc  = lex.makeCategory( "FIRSTNAME/MALEFIRSTNAME/FEMALEFIRSTNAME/NAME/LASTNAME/TITLE" );
  category_Name  = lex.makeCategory( "NAME" );
  categorySet_Name  = lex.makeCategory( "NAME" );
  category_Number  = lex.makeCategory( "NUMBER" );
  category_Cardinal  = lex.makeCategory( "CARDINAL" );
  category_Integer  = lex.makeCategory( "INTEGER" );
  category_City  = lex.makeCategory( "CITY" );
  category_Prefixtitle  = lex.makeCategory( "PREFIXTITLE" );
  category_Title  = lex.makeCategory( "TITLE" );
  category_Dualname  = lex.makeCategory( "DUALNAME" );
  categorySet_Firstname$Malef_Etc$  = lex.makeCategory( "FIRSTNAME/MALEFIRSTNAME/FEMALEFIRSTNAME" );
  feature_Lastname  = lex.makeAtom( "lastname" );
  category_Firstname  = lex.makeCategory( "FIRSTNAME" );
  category_Lastname  = lex.makeCategory( "LASTNAME" );
  categorySet_Lastname  = lex.makeCategory( "LASTNAME" );
  category_Numberunit  = lex.makeCategory( "NUMBERUNIT" );
  feature_Unit_Of_Countin_Etc  = lex.makeAtom( "unit_of_counting" );
  category_N  = lex.makeCategory( "N" );
  category_Ordinal  = lex.makeCategory( "ORDINAL" );
  category_Ord  = lex.makeCategory( "ORD" );
  category_Determiner  = lex.makeCategory( "DETERMINER" );
  category_Det  = lex.makeCategory( "DET" );
  category_Monthname  = lex.makeCategory( "MONTHNAME" );
  category_Month  = lex.makeCategory( "MONTH" );
  category_Namesuffix  = lex.makeCategory( "NAMESUFFIX" );
  category_Statename  = lex.makeCategory( "STATENAME" );
  categorySet_Statename$State_Etc  = lex.makeCategory( "STATENAME/STATECODE" );
  category_Noun  = lex.makeCategory( "NOUN" );
  category_Verb  = lex.makeCategory( "VERB" );
  category_V  = lex.makeCategory( "V" );
  category_Adjective  = lex.makeCategory( "ADJECTIVE" );
  category_Adj  = lex.makeCategory( "ADJ" );
  category_Adverb  = lex.makeCategory( "ADVERB" );
  category_Adv  = lex.makeCategory( "ADV" );
  category_Preposition  = lex.makeCategory( "PREPOSITION" );
  category_Prep  = lex.makeCategory( "PREP" );
  category_Conjunction  = lex.makeCategory( "CONJUNCTION" );
  category_Conj  = lex.makeCategory( "CONJ" );
  category_Interjection  = lex.makeCategory( "INTERJECTION" );
  category_Interj  = lex.makeCategory( "INTERJ" );
  wordSet_Sbreak$2cr  = lex.makeWordSet( "SBREAK/2CR" );
  feature_Monthconfidence  = lex.makeAtom( "monthconfidence" );
  featureSet_Ic$Uc  =  new Atom[] {lex.makeAtom( "ic" ), lex.makeAtom( "uc" )};
  wordSet_May$March$Mar$D_Etc  = lex.makeWordSet( "MAY/MARCH/MAR/DEC/SEP" );
  categorySet_Month  = lex.makeCategory( "MONTH" );
  wordSet_An$Man$May$Will_Etc  = lex.makeWordSet( "AN/MAN/MAY/WILL/SAY/SAYS/SAID/SEE/SEES/SOON/YOU/WERE" );
  sp_extremelyUnlikelyNames  =  wordSet_An$Man$May$Will_Etc ;
  wordSet_May$Will$Might$_Etc  = lex.makeWordSet( "MAY/WILL/MIGHT/BE/HAVE/DO/IS/ARE/AM/WAS/WERE/HAS/HAD" );
  sp_verbsUnlikelyAsLastnames  =  wordSet_May$Will$Might$_Etc ;
  categorySet_Monthname$Numbe_Etc  = lex.makeCategory( "MONTHNAME/NUMBER/ORDINAL/CARDINAL/WEEKDAY/PREPOSITION/DETERMINER/FIRSTNAME/DUALNAME/LASTNAME/SINGLELETTER/PREFIXTITLE/DATE/ADJECTIVE/NOUN/TIMEUNIT/NUMBERUNIT/CONJUNCTION/NAMESUFFIX/LASTNAMEPREFIX" );
  sp_wordclasses  =  categorySet_Monthname$Numbe_Etc ;
  word_Backslashquote  = lex.makeWord( "\"" );
  feature_Wordclass  = lex.makeAtom( "wordclass" );
  value_Singleletter  = lex.makeAtom( "singleletter" );
  feature_Stoplist  = lex.makeAtom( "stoplist" );
  categorySet_Det$Prep$Pro  = lex.makeCategory( "DET/PREP/PRO" );
  category_N$V$Adj$Ord$Adv  = lex.makeCategory( "N/V/ADJ/ORD/ADV" );
  wordSet_A$The  = lex.makeWordSet( "A/THE" );
  wordSet_Is$Am$Are$Was$W_Etc  = lex.makeWordSet( "IS/AM/ARE/WAS/WERE/BE/'S/(QUOTE S)" );
  categorySet_Det$N$Adj$Adv$V_Etc  = lex.makeCategory( "DET/N/ADJ/ADV/VERB" );
  word_Comma  = lex.makeWord( "," );
  word_Dollar  = lex.makeWord( "DOLLAR" );
  word_$  = lex.makeWord( "$" );
  word_Slash  = lex.makeWord( "/" );
  word_Minus  = lex.makeWord( "-" );
  feature_Weekdayconfiden_Etc  = lex.makeAtom( "weekdayconfidence" );
  feature_Number  = lex.makeAtom( "number" );
  feature_Plural  = lex.makeAtom( "plural" );
  word_That  = lex.makeWord( "THAT" );
  word_A  = lex.makeWord( "A" );
  wordSet_A$I  = lex.makeWordSet( "A/I" );
  word_Dot  = lex.makeWord( "." );
  categorySet_Ord$Integer  = lex.makeCategory( "ORD/INTEGER" );
  word_The  = lex.makeWord( "THE" );
  wordSet_A$M$P$M  = lex.makeWordSet( "A.M/P.M" );
  wordSet_Am$Pm$A$M$P$M$N_Etc  = lex.makeWordSet( "AM/PM/A.M/P.M/NOON/MIDNIGHT" );
  wordSet_Edt$Est$Pdt$Pst_Etc  = lex.makeWordSet( "EDT/EST/PDT/PST/CDT/CST/MDT/MST/GREENWICH" );
  word_Timezone  = lex.makeWord( "TIMEZONE" );
  wordSet_Thousand$Millio_Etc  = lex.makeWordSet( "THOUSAND/MILLION/BILLION/TRILLION" );
  word_Hundred  = lex.makeWord( "HUNDRED" );
  word_Currency  = lex.makeWord( "CURRENCY" );
  wordSet_Day$Week$Month  = lex.makeWordSet( "DAY/WEEK/MONTH" );
  word_Of  = lex.makeWord( "OF" );
  wordSet_Of$In  = lex.makeWordSet( "OF/IN" );
  wordSet_Hundredth$Thous_Etc  = lex.makeWordSet( "HUNDREDTH/THOUSANDTH/MILLIONTH/BILLIONTH/TRILLIONTH" );
  categorySet_Ord  = lex.makeCategory( "ORD" );
  categorySet_Integer  = lex.makeCategory( "INTEGER" );
  word_And  = lex.makeWord( "AND" );
  wordSet_March$May  = lex.makeWordSet( "MARCH/MAY" );
  wordSet_In$Of  = lex.makeWordSet( "IN/OF" );
  wordSet_Charset_Comma  =  new Word[] {lex.makeWord( "," )};
  wordSet_Charset_Dot  =  new Word[] {lex.makeWord( "." )};
  wordSet_Charset_Colonco_Etc  =  new Word[] {lex.makeWord( ":" ), lex.makeWord( "," )};
  word_Colon  = lex.makeWord( ":" );
  wordSet_Charset_Colon  =  new Word[] {lex.makeWord( ":" )};
  nIL_Priorword  = lex.makeAtom( "priorword" );
  nIL_And  = lex.makeAtom( "and" );
  value_Dualname  = lex.makeAtom( "dualname" );
  category_Singleletter  = lex.makeCategory( "SINGLELETTER" );
  categorySet_Firstname  = lex.makeCategory( "FIRSTNAME" );
  category_N$V$Adj$Adv  = lex.makeCategory( "N/V/ADJ/ADV" );
  categorySet_V$Adv  = lex.makeCategory( "V/ADV" );
  categorySet_Det$Prep$Pro$Nu_Etc  = lex.makeCategory( "DET/PREP/PRO/NUMBER" );
  featureSet_Lc$Uc  =  new Atom[] {lex.makeAtom( "lc" ), lex.makeAtom( "uc" )};
  word_My  = lex.makeWord( "MY" );
  categorySet_N$Adj$Adv  = lex.makeCategory( "N/ADJ/ADV" );
  wordSet_See$So$Soon  = lex.makeWordSet( "SEE/SO/SOON" );
  categorySet_Nameprefix  = lex.makeCategory( "NAMEPREFIX" );
  categorySet_Det$N$Adj$Adv  = lex.makeCategory( "DET/N/ADJ/ADV" );
  feature_Ic  = lex.makeAtom( "ic" );
  categorySet_Namesuffix  = lex.makeCategory( "NAMESUFFIX" );
  wordSet_San$Some  = lex.makeWordSet( "SAN/SOME" );
  word_Will  = lex.makeWord( "WILL" );
  categorySet_Adv$V  = lex.makeCategory( "ADV/V" );
  word_More  = lex.makeWord( "MORE" );
  word_2cr  = lex.makeWord( "2CR" );
  categorySet_Prefixtitle  = lex.makeCategory( "PREFIXTITLE" );
  word_Sbreak  = lex.makeWord( "SBREAK" );
  categorySet_Firstname$Dualn_Etc  = lex.makeCategory( "FIRSTNAME/DUALNAME" );
  word_To  = lex.makeWord( "TO" );
  categorySet_V  = lex.makeCategory( "V" );
  categorySet_Prespart  = lex.makeCategory( "PRESPART" );
  wordSet_After$Before$Fo_Etc  = lex.makeWordSet( "AFTER/BEFORE/FOR/FROM/PRIOR_TO/SINCE" );
  word_Day  = lex.makeWord( "DAY" );
  categorySet_City  = lex.makeCategory( "CITY" );
  wordSet_A  = lex.makeWordSet( "A" );
  categorySet_Cardinal  = lex.makeCategory( "CARDINAL" );
  categorySet_Predet$Pro  = lex.makeCategory( "PREDET/PRO" );
  categorySet_Postdet  = lex.makeCategory( "POSTDET" );
  wordSet_And$Or  = lex.makeWordSet( "AND/OR" );
  wordSet_Percent  = lex.makeWordSet( "PERCENT" );
  categorySet_Firstname$Lastn_Etc  = lex.makeCategory( "FIRSTNAME/LASTNAME/CITY" );
  wordSet_Less$More$Most  = lex.makeWordSet( "LESS/MORE/MOST" );
  wordSet_Less$More$Most$_Etc  = lex.makeWordSet( "LESS/MORE/MOST/MUCH" );
  word_Way  = lex.makeWord( "WAY" );
  categorySet_Npl$Nm$Pro$Det$_Etc  = lex.makeCategory( "NPL/NM/PRO/DET/NUMBER" );
  categorySet_V$Det$Number  = lex.makeCategory( "V/DET/NUMBER" );
  categorySet_Adj$Ord$Number  = lex.makeCategory( "ADJ/ORD/NUMBER" );
  categorySet_Adv$Adj$Ord  = lex.makeCategory( "ADV/ADJ/ORD" );
  wordSet_A$An$Several$Th_Etc  = lex.makeWordSet( "A/AN/SEVERAL/THAT/THE/THESE/THIS/THOSE" );
  wordSet_A$An$The$This$T_Etc  = lex.makeWordSet( "A/AN/THE/THIS/THAT/THESE/THOSE/ALL/BOTH/ENOUGH/ANOTHER/YOUR/MY/HIS/HER/THEIR/OUR/WHAT/WHICH/WHOSE" );
  sp_trivialDeterminers  =  wordSet_A$An$The$This$T_Etc ;
  word_On  = lex.makeWord( "ON" );
  categorySet_Weekday  = lex.makeCategory( "WEEKDAY" );
  wordSet_$Endoffile$  = lex.makeWordSet( "<ENDOFFILE>" );
  word_Number  = lex.makeWord( "NUMBER" );
  word_An  = lex.makeWord( "AN" );
  wordSet_Next$Last$Each$_Etc  = lex.makeWordSet( "NEXT/LAST/EACH/THE" );
  wordSet_Earlier$Sooner$_Etc  = lex.makeWordSet( "EARLIER/SOONER/LATER/AGO" );
  wordSet_After$Before$Fr_Etc  = lex.makeWordSet( "AFTER/BEFORE/FROM" );
  wordSet_The$A$An  = lex.makeWordSet( "THE/A/AN" );
  categorySet_Adj$Ord$Adv  = lex.makeCategory( "ADJ/ORD/ADV" );
  categorySet_Det$N$Number  = lex.makeCategory( "DET/N/NUMBER" );
  wordSet_Can$Do$Does$Wil_Etc  = lex.makeWordSet( "CAN/DO/DOES/WILL" );
  categorySet_Pro  = lex.makeCategory( "PRO" );
  wordSet_Do$Does  = lex.makeWordSet( "DO/DOES" );
  categorySet_Npl$Nm$Pro$Det$_Etc$  = lex.makeCategory( "NPL/NM/PRO/DET/ADJ/ORD/ADV/NUMBER" );
  word_$Nc$Relative$Pe_Etc  = lex.makeWord( "!NC/RELATIVE/PERSON" );
  wordSet_Be$But$For  = lex.makeWordSet( "BE/BUT/FOR" );
  categorySet_Adj$Post  = lex.makeCategory( "ADJ-POST" );
  wordSet_Both$Each$La$Le_Etc  = lex.makeWordSet( "BOTH/EACH/LA/LES/MOST/THAT" );
  categorySet_Det$Number$Prep  = lex.makeCategory( "DET/NUMBER/PREP" );
  wordSet_Of  = lex.makeWordSet( "OF" );
  wordSet_An  = lex.makeWordSet( "AN" );
  categorySet_Adv$Adj$N  = lex.makeCategory( "ADV/ADJ/N" );
  categorySet_N$V$Adv$Adj  = lex.makeCategory( "N/V/ADV/ADJ" );
  categorySet_Statecode  = lex.makeCategory( "STATECODE" );
  categorySet_Country  = lex.makeCategory( "COUNTRY" );
  word_Region  = lex.makeWord( "REGION" );
  categorySet_Adv$Ord  = lex.makeCategory( "ADV/ORD" );
  featureSet_Uc$Ic  =  new Atom[] {lex.makeAtom( "uc" ), lex.makeAtom( "ic" )};
  categorySet_Title  = lex.makeCategory( "TITLE" );
  feature_Abbrev  = lex.makeAtom( "abbrev" );
  wordSet_A$An$Several$Th_Etc$  = lex.makeWordSet( "A/AN/SEVERAL/THAT/THE/THIS" );
  wordSet_Next$Last  = lex.makeWordSet( "NEXT/LAST" );
  categorySet_Nsg  = lex.makeCategory( "NSG" );
  wordSet_A$An  = lex.makeWordSet( "A/AN" );
  category_Npr  = lex.makeCategory( "NPR" );
  categorySet_V$Nm  = lex.makeCategory( "V/NAME/NM" );
  category_Nc  = lex.makeCategory( "NC" );
  category_Nm  = lex.makeCategory( "NM" );
  categorySet_N$V$Adj$Adv  = lex.makeCategory( "N/V/ADJ/ADV" );
  wordSet_Both$Each$La$Le_Etc$  = lex.makeWordSet( "BOTH/EACH/LA/LES/THAT" );
  categorySet_Adv$Qualifier  = lex.makeCategory( "ADV-QUALIFIER" );
  scanvowels  =  "aeiouy" ;
  scanconsonants  =  "tcbswpmdfurlnhvgkqjxz" ;
  whitespaceCharacters  =  " \t\n\r\n\f" ;
  punctuationCharacters  =  ".,;:?!-$%#+=*\"\'()[]{}<>/`~\\|^@&_" ;
  digits  =  "0123456789" ;
  charType  =  new  Atom[256] ;
  atom_Vowel  = lex.makeAtom( "vowel" );
  atom_Consonant  = lex.makeAtom( "consonant" );
  featureSet_Vowel$Consonant  =  new Atom[] {lex.makeAtom( "vowel" ), lex.makeAtom( "consonant" )};
  atom_Digit  = lex.makeAtom( "digit" );
  atom_White  = lex.makeAtom( "white" );
  atom_Punct  = lex.makeAtom( "punct" );
  featureSet_White$Punct  =  new Atom[] {lex.makeAtom( "white" ), lex.makeAtom( "punct" )};
  category_Weekday  = lex.makeCategory( "WEEKDAY" );
  word_5  = lex.makeWord( 5 );
  word_9  = lex.makeWord( 9 );
  word_January  = lex.makeWord( "JANUARY" );
  word_1  = lex.makeWord( 1 );
  word_31  = lex.makeWord( 31 );
  word_February  = lex.makeWord( "FEBRUARY" );
  word_2  = lex.makeWord( 2 );
  word_28  = lex.makeWord( 28 );
  word_March  = lex.makeWord( "MARCH" );
  word_3  = lex.makeWord( 3 );
  word_6  = lex.makeWord( 6 );
  word_April  = lex.makeWord( "APRIL" );
  word_4  = lex.makeWord( 4 );
  word_30  = lex.makeWord( 30 );
  word_8  = lex.makeWord( 8 );
  word_May  = lex.makeWord( "MAY" );
  word_June  = lex.makeWord( "JUNE" );
  word_July  = lex.makeWord( "JULY" );
  word_7  = lex.makeWord( 7 );
  word_August  = lex.makeWord( "AUGUST" );
  word_September  = lex.makeWord( "SEPTEMBER" );
  word_October  = lex.makeWord( "OCTOBER" );
  word_10  = lex.makeWord( 10 );
  word_November  = lex.makeWord( "NOVEMBER" );
  word_11  = lex.makeWord( 11 );
  word_December  = lex.makeWord( "DECEMBER" );
  word_12  = lex.makeWord( 12 );
  word_Jan  = lex.makeWord( "JAN" );
  word_Feb  = lex.makeWord( "FEB" );
  word_Mar  = lex.makeWord( "MAR" );
  word_Apr  = lex.makeWord( "APR" );
  word_Jun  = lex.makeWord( "JUN" );
  word_Jul  = lex.makeWord( "JUL" );
  word_Aug  = lex.makeWord( "AUG" );
  word_Sept  = lex.makeWord( "SEPT" );
  word_Sep  = lex.makeWord( "SEP" );
  word_Oct  = lex.makeWord( "OCT" );
  word_Nov  = lex.makeWord( "NOV" );
  word_Dec  = lex.makeWord( "DEC" );
  word_Sunday  = lex.makeWord( "SUNDAY" );
  word_Monday  = lex.makeWord( "MONDAY" );
  word_Tuesday  = lex.makeWord( "TUESDAY" );
  word_Wednesday  = lex.makeWord( "WEDNESDAY" );
  word_Thursday  = lex.makeWord( "THURSDAY" );
  word_Friday  = lex.makeWord( "FRIDAY" );
  word_Saturday  = lex.makeWord( "SATURDAY" );
  word_Sundays  = lex.makeWord( "SUNDAYS" );
  word_Mondays  = lex.makeWord( "MONDAYS" );
  word_Tuesdays  = lex.makeWord( "TUESDAYS" );
  word_Wednesdays  = lex.makeWord( "WEDNESDAYS" );
  word_Thursdays  = lex.makeWord( "THURSDAYS" );
  word_Fridays  = lex.makeWord( "FRIDAYS" );
  word_Saturdays  = lex.makeWord( "SATURDAYS" );
  word_Sun  = lex.makeWord( "SUN" );
  word_Mon  = lex.makeWord( "MON" );
  word_Tues  = lex.makeWord( "TUES" );
  word_Tue  = lex.makeWord( "TUE" );
  word_Wed  = lex.makeWord( "WED" );
  word_Thurs  = lex.makeWord( "THURS" );
  word_Fri  = lex.makeWord( "FRI" );
  word_Sat  = lex.makeWord( "SAT" );
  word_Su  = lex.makeWord( "SU" );
  word_Mo  = lex.makeWord( "MO" );
  word_Tu  = lex.makeWord( "TU" );
  word_Th  = lex.makeWord( "TH" );
  word_Fr  = lex.makeWord( "FR" );
  word_Sa  = lex.makeWord( "SA" );
  word_At  = lex.makeWord( "AT" );
  word_In  = lex.makeWord( "IN" );
  word_Since  = lex.makeWord( "SINCE" );
  word_Before  = lex.makeWord( "BEFORE" );
  word_After  = lex.makeWord( "AFTER" );
  word_During  = lex.makeWord( "DURING" );
  word_Till  = lex.makeWord( "TILL" );
  word_Until  = lex.makeWord( "UNTIL" );
  word_From  = lex.makeWord( "FROM" );
  word_Through  = lex.makeWord( "THROUGH" );
  word_Now  = lex.makeWord( "NOW" );
  category_Date  = lex.makeCategory( "DATE" );
  word_Today  = lex.makeWord( "TODAY" );
  word_Tomorrow  = lex.makeWord( "TOMORROW" );
  word_Yesterday  = lex.makeWord( "YESTERDAY" );
  word_Late  = lex.makeWord( "LATE" );
  word_Early  = lex.makeWord( "EARLY" );
  word_Fall  = lex.makeWord( "FALL" );
  word_Winter  = lex.makeWord( "WINTER" );
  word_Spring  = lex.makeWord( "SPRING" );
  word_Summer  = lex.makeWord( "SUMMER" );
  category_Timeunit  = lex.makeCategory( "TIMEUNIT" );
  word_Week  = lex.makeWord( "WEEK" );
  word_Month  = lex.makeWord( "MONTH" );
  word_Year  = lex.makeWord( "YEAR" );
  word_Hour  = lex.makeWord( "HOUR" );
  word_Minute  = lex.makeWord( "MINUTE" );
  word_Thousand  = lex.makeWord( "THOUSAND" );
  word_Last  = lex.makeWord( "LAST" );
  word_Next  = lex.makeWord( "NEXT" );
  word_This  = lex.makeWord( "THIS" );
  word_President  = lex.makeWord( "PRESIDENT" );
  word_Vicepresident  = lex.makeWord( "VICEPRESIDENT" );
  word_B  = lex.makeWord( "B" );
  word_C  = lex.makeWord( "C" );
  word_D  = lex.makeWord( "D" );
  word_E  = lex.makeWord( "E" );
  word_F  = lex.makeWord( "F" );
  word_G  = lex.makeWord( "G" );
  word_H  = lex.makeWord( "H" );
  word_I  = lex.makeWord( "I" );
  word_J  = lex.makeWord( "J" );
  word_K  = lex.makeWord( "K" );
  word_L  = lex.makeWord( "L" );
  word_M  = lex.makeWord( "M" );
  word_N  = lex.makeWord( "N" );
  word_O  = lex.makeWord( "O" );
  word_P  = lex.makeWord( "P" );
  word_Q  = lex.makeWord( "Q" );
  word_R  = lex.makeWord( "R" );
  word_S  = lex.makeWord( "S" );
  word_T  = lex.makeWord( "T" );
  word_U  = lex.makeWord( "U" );
  word_V  = lex.makeWord( "V" );
  word_W  = lex.makeWord( "W" );
  word_X  = lex.makeWord( "X" );
  word_Y  = lex.makeWord( "Y" );
  word_Z  = lex.makeWord( "Z" );
  word_Jr  = lex.makeWord( "JR" );
  word_Sr  = lex.makeWord( "SR" );
  word_Ii  = lex.makeWord( "II" );
  word_Iii  = lex.makeWord( "III" );
  word_Iv  = lex.makeWord( "IV" );
  word_Esq  = lex.makeWord( "ESQ" );
  word_Van  = lex.makeWord( "VAN" );
  category_Lastnameprefix  = lex.makeCategory( "LASTNAMEPREFIX" );
  word_Von  = lex.makeWord( "VON" );
  word_De  = lex.makeWord( "DE" );
  word_Der  = lex.makeWord( "DER" );
  word_Da  = lex.makeWord( "DA" );
  word_Mac  = lex.makeWord( "MAC" );
  word_Mc  = lex.makeWord( "MC" );
  word_St  = lex.makeWord( "ST" );
  word_Bill  = lex.makeWord( "BILL" );
  word_Woods  = lex.makeWord( "WOODS" );
  word_Gross  = lex.makeWord( "GROSS" );
  atom_Numericalvalue  = lex.makeAtom( "numericalvalue" );
  word_144  = lex.makeWord( 144 );
  punctbuffer  =  new WordBuffer( 50 );
  word_Indent  = lex.makeWord( "INDENT" );
  wordSet_Charset_Backsla_Etc  =  new Word[] {lex.makeWord( "\n" ), lex.makeWord( "\r" ), lex.makeWord( "\n" )};
  word_Space  = lex.makeWord( " " );
  word_Backslashn  = lex.makeWord( "\n" );
  word_Backslashr  = lex.makeWord( "\r" );
  word_Backslashu0010  = lex.makeWord( "\u0010" );
  word_Backslashf  = lex.makeWord( "\f" );
  categorySet_Singleletter$Nu_Etc  = lex.makeCategory( "SINGLELETTER/NUMBER" );
  wordSet_Charset_Queryba_Etc  =  new Word[] {lex.makeWord( "?" ), lex.makeWord( "!" )};
  word_$Endoffile$  = lex.makeWord( "<ENDOFFILE>" );
  feature_Numeralnumberp  = lex.makeAtom( "numeralnumberp" );
  sp_word5  =  word_5 ;
  sp_word6  =  word_6 ;
  sp_word7  =  word_7 ;
  sp_word8  =  word_8 ;
  sp_word9  =  word_9 ;
  atom_Mac  = lex.makeAtom( "mac" );
  feature_Compounds  = lex.makeAtom( "compounds" );
  feature_Word$Class  = lex.makeAtom( "word-class" );
  atom_String  = lex.makeAtom( "string" );
  feature_Known  = lex.makeAtom( "known" );
  value_T  = lex.makeAtom( "t" );
  feature_Guessed  = lex.makeAtom( "guessed" );
  feature_Startfunction  = lex.makeAtom( "startfunction" );
  value_Guessed  = lex.makeAtom( "guessed" );
  feature_Penalties  = lex.makeAtom( "penalties" );
  nIL_Th  = lex.makeAtom( "th" );
  nIL_St  = lex.makeAtom( "st" );
  nIL_Nd  = lex.makeAtom( "nd" );
  nIL_Rd  = lex.makeAtom( "rd" );
  sp_thisYear  = getThisYear();


    /* after the setting of class vars.... */
  }

  /* add all the parser methods go here  */
  //==================================

 State applyTransition( State  selectedState ) {
  /* Dispatch to next state */
   switch (selectedState.indexVal) {
      case CARDINALSTART:
          return  cardinalStart();
      case CITYAFTERCOUNTRY:
          return  cityAfterCountry();
      case CITYAFTERSTATE:
          return  cityAfterState();
      case CITYSTART:
          return  cityStart();
      case DATEAFTERDAY:
          return  dateAfterDay();
      case DATEAFTERMONTH:
          return  dateAfterMonth();
      case DATEAFTERMONTHCARDINAL:
          return  dateAfterMonthCardinal();
      case DATEAFTERMONTHDAY:
          return  dateAfterMonthDay();
      case DATEAFTERMONTHDAYCARDINAL:
          return  dateAfterMonthDayCardinal();
      case DATEAFTERMONTHDAYHOUR:
          return  dateAfterMonthDayHour();
      case DATEAFTERMONTHDAYHOURMINUTES:
          return  dateAfterMonthDayHourMinutes();
      case DATEAFTERMONTHDAYHOURMINUTESSECONDS:
          return  dateAfterMonthDayHourMinutesSeconds();
      case DATEAFTERTIME:
          return  dateAfterTime();
      case DATEAFTERWEEKDAY:
          return  dateAfterWeekday();
      case DETSTART:
          return  detStart();
      case FIRSTNAMESTART:
          return  firstnameStart();
      case GENERALPHRASESTART:
          return  generalPhraseStart();
      case LASTNAMESTART:
          return  lastnameStart();
      case MONTHSTART:
          return  monthStart();
      case NAMEAFTERFIRST:
          return  nameAfterFirst();
      case NAMEAFTERLAST:
          return  nameAfterLast();
      case NAMEAFTERTITLE:
          return  nameAfterTitle();
      case NPAFTERADJ:
          return  npAfterAdj();
      case NPAFTERADV:
          return  npAfterAdv();
      case NPAFTERAN:
          return  npAfterAn();
      case NPAFTERCITY:
          return  npAfterCity();
      case NPAFTERDET:
          return  npAfterDet();
      case NPAFTERNOUN:
          return  npAfterNoun();
      case NPAFTERPOSTMOD:
          return  npAfterPostmod();
      case NPAFTERVERB:
          return  npAfterVerb();
      case NUMBERSTART:
          return  numberStart();
      case ORDINALSTART:
          return  ordinalStart();
      case POPDATE:
          return  popDate();
      case POPNPSTACK:
          return  popNpStack();
      case PREFIXTITLESTART:
          return  prefixtitleStart();
      case PREPOSITIONSTART:
          return  prepositionStart();
      case SINGLELETTERSTART:
          return  singleletterStart();
      case WEEKDAYSTART:
          return  weekdayStart();
      case XAFTERCARDINAL:
          return  xAfterCardinal();
      case XAFTERCARDINALAND:
          return  xAfterCardinalAnd();
      case XAFTERNUMBER:
          return  xAfterNumber();
      case XAFTERORDINAL:
          return  xAfterOrdinal();
      case XAFTERORDINALTIMEUNIT:
          return  xAfterOrdinalTimeUnit();
      case XAFTERPREPOSITION:
          return  xAfterPreposition();
      default:
          throw  new SwitchVarOutOfRangeException( selectedState );
   }
 }

/* Copyright 1999 Paul Martin for Sun Microsystems Labs */

/**
 * These ExtraLexiconMethods of ATNPhraseParser class are handbuilt to

 * support the various kinds of classes used in calls that were 
 * different in Lisp.
 *
 * This hand-built file should be logically included (by concatenation 
 * or by importing) in the testATN final java file.
 *
 *
 * This file logically should be pasted into the file that has a class 
 * that extends Value... so these methods are lexically scoped inside
 * Value or its extension.
 * 
 * These would be methods of Value if it weren't just an interface
 *
 * 02oct01 tweaked to live outside of lexicon...
 * 12sep00 added nodeSequenceReplaceLast 
 * 07jun00 added nodeSequence[Last|Secondlast|ButLast]{Lexicon.Word}
 * 26jan00 added head-word-picking code to nodeSequenceFirstWord 
 */

 boolean singletonIntersect(Atom[] key, Atom[] set){
   /* true only if key is of length one and that one is
    * an element of the set */
   if ((key == null) ||(1 != key.length)){
      return false;
    }else{
      int i = 0;
      while (i < set.length) {
	if  (set[i] == key[0]) return true;
	i++;
      }
      return false;
    }
 }


 boolean singletonMember(Atom[] key, List set){
   /* true only if key is of length one and that one is
    * a member of the set  and the set is a List*/
   if ((key == null) || (1 != key.length)){
      return false;
    }else{
      return  set.hasMember(key[0]);
    }
 }


 Value singletonList(List val){
	if ((val != null) && (1 == val.length()))
	  return val.elementAt(0);
	else
	  return null;
 }
	
 boolean singletonIntersect(Word[] key, Word[] set){
   /* true only if key is of length one and that one is
    * an element of the set */
   if ((key == null) || (1 != key.length)){
      return false;
    }else{
      int i = 0;
      while (i < set.length) {
	if  (set[i] == key[0]) return true;
	i++;
      }
      return false;
    }
 }


 boolean singletonMember(Word[] key, List set){
   /* true only if key is of length one and that one is
    * a member of the set  and the set is a Lexicon.List*/
   if ((key == null) || (1 != key.length)){
      return false;
    }else{
      return set.hasMember(key[0]);
    }
 }

 boolean singletonp(Atom[] val) {
  return ((val != null) && (1 == val.length));
 }

 
 Atom singleton(Atom [] val){
	if ((val != null) && (1 == val.length))
	  return val[0];
	else
	  return null;
 }
 boolean singletonp(Word[] val) {
  return ((val != null) && (1 == val.length));
 }

 boolean multiWordp(Word[] words) {
   return ((words != null) && (words.length > 1));
 }
  
 Word singleton(Word [] val){
	if ((val != null) && (1 == val.length))
	  return val[0];
	else
	  return null;
 }
 
 Word[] makeSingleton(Word singWord){
  return new Word[] {singWord};
 }

 boolean catArraySubsumptionp(
     Category[] cat1, Category[] cat2){
   int i = 0;
      if (cat1 == null) return false;
      while (i < cat1.length) {
	  if (cat1[i].subsumesOneOf(cat2)) return true;
	  i++;
	}
      return false;
 }

/* node list stuff is here for replacing the Lisp car-cdr-cons treatment
 * of word collections .. but there may be phrases as well, so we
 * must treat them as Values rather than words...
 *
 *  There must be separate versions for node sequences of just Words...
 * 
 */

 public static Value[] nodeSequenceAddNode (Value[] vals, Value newVal){
   Value[] newVals;
   if (vals == null) {
     newVals = new Value[1];
     newVals[0] = newVal;
   }else{
     int oldSiz = vals.length;
     newVals = new Value[oldSiz + 1];
     for (int i=0; i<oldSiz; i++){ newVals[i] = vals[i];}
     newVals[oldSiz] = newVal;
   }
   return newVals;
 }
 public static Word[] nodeSequenceAddNode (Word[] words, Word newWord){
   Word[] newWords;
   if (words == null) {
     newWords = new Word[1];
     newWords[0] = newWord;
   }else{
     int oldSiz = words.length;
     newWords = new Word[oldSiz + 1];
     for (int i=0; i<oldSiz; i++){ newWords[i] = words[i];}
     newWords[oldSiz] = newWord;
   }
   return newWords;
 }


 public static Value[] nodeSequencePushNode (Value[] vals, Value newVal){
    Value[] newVals;
    if (vals == null) {
      newVals = new Value[1];
      newVals[0] = newVal;
    }else{ int oldSiz = vals.length;
    newVals = new Value[oldSiz + 1];
    for (int i=0; i<oldSiz; i++){ newVals[i+1] = vals[i];}
    newVals[0] = newVal;
    }
    return newVals;
 }
 public static Word[] nodeSequencePushNode (Word[] words, Word newWord){
    Word[] newWords;
    if (words == null) {
      newWords = new Word[1];
      newWords[0] = newWord;
    }else{ int oldSiz = words.length;
    newWords = new Word[oldSiz + 1];
    for (int i=0; i<oldSiz; i++){ newWords[i+1] = words[i];}
    newWords[0] = newWord;
    }
    return newWords;
 }


 public static Word[] nodeSequenceReplaceLast (Word[] words, Word newWord){
     if ((words == null) || (words.length == 0)) {
	 Word [] newWords = new Word[1];
	 newWords[0] = newWord;
	 return newWords;
     }else{
	 words[words.length - 1] = newWord;
	 return words;
     }
}

 public static Value[] nodeSequenceReplaceLast (Value[] vals, Value newVal){
     if ((vals == null) || (vals.length == 0)) {
	 Value [] newVals = new Value[1];
	 newVals[0] = newVal;
	 return newVals;
     }else{
	 vals[vals.length - 1] = newVal;
	 return vals;
     }
 }


 public static int nodeSequenceLength (Value[] vals){
  return vals.length;
 }
 public static Value nodeSequenceFirst (Value[] vals){
  if (vals != null)
   return vals[0];
   else return null;
 }

 public static Word nodeSequenceFirst (Word[] vals){
  if (vals != null)
   return vals[0];
   else return null;
 }


 public static Value nodeSequenceLast( Value[]  vals ) {
  /* safe for Lisp version where wordSequence may be atomic */
    if (vals != null)
	return vals[vals.length-1];
    else return null;
 }

 public static Word  nodeSequenceLast( Word[]  wds ) {
  /* safe for Lisp version where wordSequence may be atomic */
    if (wds != null)
	return wds[wds.length-1];
    else return null;
 }

 public static Value nodeSequenceSecondLast( Value[]  vals ) {
  /* safe for Lisp version where wordSequence may be atomic */
     if ((vals != null) && (vals.length > 1))
	return vals[vals.length-2];
    else return null;
 }
 
 public static Word nodeSequenceSecondLast( Word[]  wds ) {
  /* safe for Lisp version where wordSequence may be atomic */
     if ((wds != null) && (wds.length > 1))
	return wds[wds.length-2];
    else return null;
 }

//  public static Lexicon.Word nodeSequenceFirstWord (Value[] vals){
//   if ((vals == null) || (vals.length == 0)) 
//     return null;
//   else if (vals[0] instanceof Lexicon.Word)
//    return (Lexicon.Word)vals[0];
//   else if (vals[0] instanceof AdjPhrase)
//     return ((AdjPhrase)vals[0]).adjective;
//   else if (vals[0] instanceof AdvPhrase)
//      return ((AdvPhrase)vals[0]).headadv;
//   else { 
//     System.out.println("nodeSequenceFirstWord can't get word from phrase "
// 		       + vals[0].toString());
//     return null;
//   }
//  }

 public static  Word nodeSequenceFirstWord( Value[]  ws ) {
  /* safe for Lisp version where wordSequence may be atomic */
    return  adjOrAdvWord(nodeSequenceFirst( ws ));
 }


 public static Word nodeSequenceLastWord( Value[]  ws ) {
  /* safe for Lisp version where wordSequence may be atomic */
    return  adjOrAdvWord(nodeSequenceLast( ws ));
 }


 public static Word adjOrAdvWord( Value  v1 ) {
  /* gets the word out of adj or adv phrase if needed */
   if     ( v1  ==  null )
       return   null ;

   else  if   (v1.wordp())
       return  (Word) v1 ;

   else  if    ( v1  instanceof  AdjPhrase )
       return   ((AdjPhrase) v1).adjective ;

   else  if    ( v1  instanceof  AdvPhrase )
       return   ((AdvPhrase) v1).headadv ;

   else  { System.out.println("nodeSequenceFirstWord can't get word from phrase "
 		       + v1.toString());
         return   null ;
       }
 }

 public Word[] untransmittedWords(Value v){ 
    /* collects the words buried in a phrase struct, keeping only ones
       that have not been previously transmitted .. pmartin 4aug00 */
    Vector utWords = new Vector(20);
    addUT(v, utWords);
    if (utWords.size() == 0) return null;
    else {
	Word[] utwdArray =  new Word[utWords.size()];
	utWords.copyInto(utwdArray);
	// the real transmitter does these now!
	//	for (int i=0; i<utwdArray.length; i++)
	//        utwdArray[i].markTransmitted();
	return utwdArray;
    }
 }

 public void addUTs(Value[] vs, Vector utv){
     if (vs != null) {
	 for (int i=0; i<vs.length; i++){
	     addUT(vs[i], utv);
	 }
     }
 }

 public void addUT(Value v, Vector utv){
     if (v == null) {
	 // empty statement here
     } else if (v instanceof Word) {
	 if ((!utv.contains(v)) && (! ((Word)v).transmittedp()))
	     utv.addElement(v);
     } else if (v instanceof AdjPhrase) {
	 addUT(((AdjPhrase)v).adjective, utv);
	 addUTs(((AdjPhrase)v).advs, utv);
     } else if (v instanceof AdvPhrase){
	 addUT(((AdvPhrase)v).headadv, utv);
	 addUTs(((AdvPhrase)v).qualifadvs, utv);
     } else if (v instanceof NounPhrase){
	 addUT(((NounPhrase)v).noun, utv);  
	 addUT(((NounPhrase)v).determiner, utv); 
         addUTs(((NounPhrase)v).modifiers, utv);
	 addUTs(((NounPhrase)v).postmods, utv);
     } else if (v instanceof PossPhrase){
	 addUT(((PossPhrase)v).object, utv);
     } else if (v instanceof PrepPhrase){
	 addUT(((PrepPhrase)v).preposition, utv);
	 addUT(((PrepPhrase)v).object, utv);
     } else if (v instanceof NamePhrase){
	 addUTs(((NamePhrase)v).lastname, utv);
	 addUT(((NamePhrase)v).firstname, utv);
	 addUT(((NamePhrase)v).prefixtitle, utv);
	 addUTs(((NamePhrase)v).initials, utv);
	 addUTs(((NamePhrase)v).namesuffix, utv);
     } else if (v instanceof CityPhrase){
	 addUT(((CityPhrase)v).city, utv);
	 addUT(((CityPhrase)v).statename, utv);
	 addUT(((CityPhrase)v).country, utv);
	 addUT(((CityPhrase)v).postalCode, utv);
     } else if (v instanceof PlaceNamePhrase){
	 addUT(((PlaceNamePhrase)v).pname, utv);
	 addUT(((PlaceNamePhrase)v).statename, utv);
	 addUT(((PlaceNamePhrase)v).country, utv);
     } else if (v instanceof VerbPhrase){
	 addUT(((VerbPhrase)v).verb, utv);
	 addUT(((VerbPhrase)v).object, utv);
     } else if (v instanceof DatePhrase){
	 ///	addUT(((DatePhrase)v).year, utv);  //int not Lexicon.Word
	addUT(((DatePhrase)v).month, utv);
	///	addUT(((DatePhrase)v).day, utv);   //int not Lexicon.Word
	addUT(((DatePhrase)v).weekday, utv);
     } else if (v instanceof TimePhrase){
	 addUT(((TimePhrase)v).preposition, utv);
	 addUT(((TimePhrase)v).hour, utv);
	 addUT(((TimePhrase)v).dayTime, utv);
	 addUT(((TimePhrase)v).timezone, utv);
     } else
	 System.out.println("cannot add untransmitted words from " + v.toString());
 }

 public String csModsStr(String tag, Value[] vmods){
    String cs = "";
    if (vmods != null) 
	for (int i=0; i<vmods.length; i++)
	    cs = cs + csModStr(tag, vmods[i]);
    return cs;
 }

 public String csModStr(String tag, Value mod){
    if (mod == null) return "";
    else return " " + tag + ":" + csStr(mod);
 }

 public String csModIntStr(String tag, int val){
     if (val == sp_unsetNumberValue) return "";
     else return " " + tag + ":" + Integer.toString(val);
 }
    
 public String csStr(Value v){
     /* makes a crushed string for the structure of a phrase or word */
     String cs = "";
      if (v == null) 
      	 System.out.println("*** Warning*** csStr called for null value");
     else if (v instanceof Word) 
	 cs = ((Word)v).printString();
     else if (v instanceof AdjPhrase) 
	 cs = "(adj:" + csStr(((AdjPhrase)v).adjective) + 
	     csModsStr("adv", ((AdjPhrase)v).advs) + ")";	
     else if (v instanceof AdvPhrase)
	 cs = "(headAdv:" + csStr(((AdvPhrase)v).headadv) + 
	     csModsStr("qAdv", ((AdvPhrase)v).qualifadvs) + ")";
     else if (v instanceof NounPhrase)
	 cs = "(noun:" + csStr(((NounPhrase)v).noun) + 
	     csModStr("det", ((NounPhrase)v).determiner) + 
	     csModsStr("mod", ((NounPhrase)v).modifiers) + 
	     csModsStr("postMod", ((NounPhrase)v).postmods) + ")";
     else if (v instanceof PossPhrase)
	 cs = "(poss:" + csStr(((PossPhrase)v).object) + ")";
     else if (v instanceof PrepPhrase)
	 cs = "(prep:" + ((PrepPhrase)v).preposition.printString() + 
	     csModStr("obj", ((PrepPhrase)v).object) + ")";
     else if (v instanceof NamePhrase){
	 boolean doAux = true;
	 if ((((NamePhrase)v).lastname == null)  &&
	     (((NamePhrase)v).firstname != null))
	     cs = "(fnameHead:" + ((NamePhrase)v).firstname.printString();
	 else if (((NamePhrase)v).lastname != null) {
	     cs = "(lnameHead:" + ((NamePhrase)v).lastname[0].printString();
	     if (((NamePhrase)v).lastname.length > 1) 
		 cs = cs + csModsStr("lnameMod",
				     nodeSequenceButFirst(((NamePhrase)v).lastname));
		  
	     if (((NamePhrase)v).firstname != null)
		cs = cs +  " fname:" +  ((NamePhrase)v).firstname.printString();
	 } else doAux = false;
	 if (doAux) {
	     cs = cs + csModStr("prefix", ((NamePhrase)v).prefixtitle) + 
	     csModsStr("initial", ((NamePhrase)v).initials) + 
	     csModsStr("suffix", ((NamePhrase)v).namesuffix) + ")";
	 }
     }
     else if (v instanceof CityPhrase)
	 cs = "(city:" + ((CityPhrase)v).city.printString() + 
	     csModStr("state", ((CityPhrase)v).statename) + 
	     csModStr("country", ((CityPhrase)v).country) +
	     csModStr("postCode", ((CityPhrase)v).postalCode) + ")";
     else if (v instanceof PlaceNamePhrase)
	 cs = "(place:" + ((PlaceNamePhrase)v).pname.printString() + 
	     csModStr("state", ((PlaceNamePhrase)v).statename) + 
	     csModStr("country", ((PlaceNamePhrase)v).country) + ")";
     else if (v instanceof VerbPhrase)
	 cs = "(verb:" + ((VerbPhrase)v).verb.printString() + 
	     csModStr("vobj", ((VerbPhrase)v).object) + ")";
     else if (v instanceof DatePhrase){
	 cs = "(" + csModIntStr("year", ((DatePhrase)v).year) + 
	     csModStr("month", ((DatePhrase)v).month) +
	     csModIntStr("day", ((DatePhrase)v).day) + 
	     csModStr("weekday", ((DatePhrase)v).weekday) + ")";
     }
     else if (v instanceof TimePhrase)
	cs = "(timePrep:" + ((TimePhrase)v).preposition.printString() + 
	    csModStr("hour", ((TimePhrase)v).hour) +
	    csModStr("daytime", ((TimePhrase)v).dayTime) + 
	    csModStr("timezone", ((TimePhrase)v).timezone) + ")";
     else System.out.println("csStr can't figure out this phrase:" +
			     v.toString());
     return cs;
 }

 public String crushCsTag (String detail){
     String[] heads = new String[] {"adj", "headAdv", "noun", "poss", "prep",
				    "fnameHead", "lnameHead", "city", 
				    "place", "verb", "year", "timePrep"};
     String[] mods = new String[] {"adv", "qAdv", "det", "mod", "postMod",
				   "obj", "lnameMod", "fname", "prefix",
				   "initial", "suffix", "state", "country",
				   "vobj", "month", "day", "weekday",
				   "hour", "daytime", "timezone"};

     for (int i=0; i<heads.length; i++)
	 if (detail.equalsIgnoreCase(heads[i])) return "head";
     for (int i=0; i<mods.length; i++)
	 if (detail.equalsIgnoreCase(mods[i])) return "mod";
     return "strangeCsTag";
 }
	     

 public static Value[] nodeSequenceButLast (Value[] vals){
  int oldSiz;
  if ((vals == null) || ((oldSiz = vals.length) < 2))
    return null;
  else {
      int newSiz = oldSiz -1;
      Value[] newVals = new Value[oldSiz - 1];
      for (int i=0; i<newSiz; i++){ newVals[i] = vals[i];}
      return newVals;
  }
 }
 public static Word[] nodeSequenceButLast (Word[] words){
  int oldSiz;
  if ((words == null) || ((oldSiz = words.length) < 2))
    return null;
  else {
      int newSiz = oldSiz -1;
      Word[] newWords = new Word[oldSiz - 1];
      for (int i=0; i<newSiz; i++){ newWords[i] = words[i];}
      return newWords;
  }
 }
 public static Value[] nodeSequenceButFirst (Value[] vals){
  int oldSiz;
  if ((vals == null) || ((oldSiz = vals.length) < 2))
    return null;
  else {
      int newSiz = oldSiz -1;
      Value[] newVals = new Value[oldSiz - 1];
      for (int i=0; i<newSiz; i++){ newVals[i] = vals[i+1];}
      return newVals;
  }
 }
 public static Word[] nodeSequenceButFirst (Word[] words){
  int oldSiz;
  if ((words == null) || ((oldSiz = words.length) < 2))
    return null;
  else {
      int newSiz = oldSiz -1;
      Word[] newWords = new Word[oldSiz - 1];
      for (int i=0; i<newSiz; i++){ newWords[i] = words[i+1];}
      return newWords;
  }
 }

 public static Value nodeSequenceSecond (Value[] vals){
  if ((vals != null) && (vals.length > 1))
    return vals[1];
  else return null;
 }

 public static Value[] nodeSequenceRest (Value[] vals){
  int oldSiz;
  if ((vals == null) || ((oldSiz = vals.length) < 2))
    return null;
  else {
    Value[] newVals = new Value[oldSiz - 1];
    for (int i=1; i<oldSiz; i++){ newVals[i-1] = vals[i];}
    return newVals;
  }
 }
 public static Word[] nodeSequenceRest (Word[] words){
  int oldSiz;
  if ((words == null) || ((oldSiz = words.length) < 2))
    return null;
  else {
    Word[] newWords = new Word[oldSiz - 1];
    for (int i=1; i<oldSiz; i++){ newWords[i-1] = words[i];}
    return newWords;
  }
 }
 public static Value[] reverseNodeSequence (Value [] vals){
   int siz;
   if ((vals == null) || ((siz = vals.length) < 2))
     return vals;
   else {
     Value[] newVals = new Value[siz];
     for (int i=0; i<siz; i++){ newVals[i] = vals[siz-1-i];}
     return newVals;
   }
 }
 public static Word[] reverseNodeSequence (Word [] words){
   int siz;
   if ((words == null) || ((siz = words.length) < 2))
     return words;
   else {
     Word[] newWords = new Word[siz];
     for (int i=0; i<siz; i++){ newWords[i] = words[siz-1-i];}
     return newWords;
   }
 }

/* methods here are place holders for when Morphology is included */
 void clearMorphCache(){
 }

  // Derived from Lisp from /home/wwoods/macnet/parser/morphologyfns. 
 
    public boolean mustBePluralNoun (Word noun){
      /*   tests that word is a noun that can only be a plural noun.*/
      return ( noun.canBePluralNoun() && (! noun.canBeSingularNoun()));
    }

    public boolean mustBeSingularNoun (Word noun){
      /* tests that word is a noun that can only be a singular noun.*/
      return (noun.canBeSingularNoun() && (! noun.canBePluralNoun()));
    }

    //  end of pmartin morphologyFns insertions

/* these are methods that ATNParser needs but are done differently in Lisp */

 public Word packLexTime(int hour, int min){
    String minStr = Integer.toString(min);
    if (min < 10) minStr = "0" + minStr;
    String time =  Integer.toString(hour) + ":" + minStr;
    return lex.makeWord(time);
 }
 public Word packLexTime(int hour, int min, int sec){
    String secStr = Integer.toString(sec);
    if (sec < 10) secStr = "0" + secStr;
    String minStr = Integer.toString(min);
    if (min < 10) minStr = "0" + minStr;
    String time =  Integer.toString(hour) + ":" + minStr + ":" + secStr;
    return lex.makeWord(time);
 }

/* more extras needed for the Lists that are used in the lexicon */

 public static Value listFirst(List ll){
   if ((ll != null) && (ll.length() > 0)){ 
     return ll.elementAt(0);
   }else{
     return null;
   }
 }
 public static Value listSecond(List ll){
   if ((ll != null) && (ll.length() > 1)){ 
     return ll.elementAt(1);
   }else{
     return null;
   }
 }
  
 public Value[] appendVector(Vector vect){
   /* return an array that has "flattened" the vector elements by one level.
    * designed to assemble the vector elements analogous to a Lisp Append.
    */
   int newlen = 0;
   int newIdx = 0;
   List ele;
   Enumeration ve = vect.elements();
   while (ve.hasMoreElements()){
     ele = ((List)ve.nextElement());
     if (ele != null){
       newlen = newlen +ele.length();
     }
   }
   Value[] appContents = new Value[newlen];
   ve = vect.elements();
   while (ve.hasMoreElements()){
     ele = (List)ve.nextElement();
     if (ele != null){
       for (int i=0; i<ele.length(); i++){
	 appContents[newIdx++] = ele.elementAt(i);
       }
     }
   }
   return appContents;
 }

 public List appendVectorList(Vector vect){
   Value[] contents = appendVector(vect);
   if ((contents != null) && (contents.length > 0)){
     return lex.makeList(contents);
   }else{
     return null;
   }
 }

 public static List listTail(List ll){
   return listTailN(ll, 1);
 }

 public static List listTailTail(List ll){
   return listTailN(ll, 2);
 }

 public static List listTailN(List ll, int nTail){
   /* nTail is 1 for Lisp Tail, 2 for cddr, etc */
     int nSiz;
   if ((ll == null) || ((nSiz = ll.length()) <= nTail)){ 
     return null;
   }else{
     Value[] newContents = new Value[nSiz-nTail];
     Lexicon llLex = ll.lexicon();
     for (int i = nTail; i<nSiz; i++){
       newContents[i-nTail] = ll.elementAt(i);
     }
     //System.out.print("ListTailN calling new List with [" + newContents[0].toString());
     //for(int i=1; i<newContents.length; i++){System.out.print(", " +newContents[i].toString());}
     //System.out.println("]");
     List newL = llLex.makeList(newContents);
     //System.out.println("ListTailN returning list " + newL.toString());
     return newL;
   }
 }

  
 public static List assocList
                  (Value key, List alist){
   List foundList;
   if (alist != null){
     for (int i=0; i<alist.length(); i++){
       foundList = (List)alist.elementAt(i);
       if (key == listFirst(foundList)) {
	 return foundList;
       }
     }
   }
   return null;
 }

 public static List listMatch(
	          List keyList, List alist){
   List foundList;
   boolean match = true;
   if ((alist != null) && (keyList != null)){
     for (int i=0; i<alist.length(); i++){
       foundList = (List)alist.elementAt(i);
       for (int j =0; j<keyList.length(); j++){
	 if (keyList.contents[j] != foundList.contents[j]){
	   match = false;
	   break;}
       } 
       if (match) return foundList;
     }
   }
   return null;
 }

public static Vector addListToVector(List ll, Vector vv){
  for (int i=0; i<ll.length(); i++)
      vv.addElement(ll.contents[i]);
  return vv;
 }

public static String SAtoString(String[] sa){
       if (sa == null) return "null";
       StringBuffer sb = new StringBuffer();
       if (sa.length > 0) sb.append(sa[0]);
       for (int i=1; i<sa.length; i++) sb.append(", ").append(sa[i]);
       return "{" + sb.toString() + "}";
}
  public static String arrayToString(Object[] oa){
       if (oa == null) return "null";
       StringBuffer sb = new StringBuffer();
       String s;
       if (oa.length > 0) {
	   if (oa[0] instanceof String) 
	       s = (String)oa[0];
	   else s = oa[0].toString();
	   sb.append(s);
       }
       for (int i=1; i<oa.length; i++){
	   if (oa[0] instanceof String) 
	       s = (String)oa[i];
	   else s = oa[i].toString();
	   sb.append(", ").append(s);
       }
       return "{" + sb.toString() + "}";
 }
 /*  really set from a different file */
  Word[]  sp_knownWordList  =  null ;
 /*  really set from a different file */
  String  sp_knownWordFile  =  null ;

 Word getMonthName( int  monthnum ) {
  /*  maps 1-based month number to month-name word */
   if    ( ( monthnum  >  12 ) ||
           ( monthnum  <  1 ))
       return   word_Someunknownmont_Etc ;

   else   return   sp_monthNameList[(monthnum - 1)] ;
 }

 /* ("  ******  state variables used by the atn:   ****** ") */
  Value  month  =  null ;
  int  day  =  sp_unsetNumberValue ;
  int  year  =  sp_unsetNumberValue ;
  Word  weekday  =  null ;
  Word  dayTime  =  null ;
  Word  timezone  =  null ;
  Word  timeunit  =  null ;
  boolean  expectYear  =  false ;
  int  hour  =  sp_unsetNumberValue ;
  int  minutes  =  sp_unsetNumberValue ;
  int  seconds  =  sp_unsetNumberValue ;
  boolean  expectSeconds  =  false ;
  Word  dateSeparator  =  null ;
  Word  numberWord  =  null ;
  double  firstnum  =  0.0 ;
  double  othernum  =  0.0 ;
 /* flag indicating that a potential name is lower case */
  boolean  lcName  =  false ;
 /* flag indicating that a potential name is upper case */
  boolean  ucName  =  false ;
  Word  preposition  =  null ;
  boolean  plural  =  false ;
  Value  determiner  =  null ;
  Word  firstname  =  null ;
  Word[]  lastname  =  null ;
  Word[]  initials  =  null ;
  Word[]  namesuffix  =  null ;
  Word  prefixtitle  =  null ;
  double  secondnum  =  0.0 ;
  Integer  secondnumStart  =  null ;
  Integer  firstnumEnd  =  null ;
  Word  currency  =  null ;
  Word  noun  =  null ;
  Value[]  modifiers  =  null ;
  Value[]  adverbs  =  null ;
  Word  verb  =  null ;
  Value[]  postmods  =  null ;
  Vector  nounstack  =  null ;
  Word  city  =  null ;
  Word  statename  =  null ;
  Word  country  =  null ;
  Word  postalCode  =  null ;
  Integer  refcount  =  new Integer( 0 );
  boolean  somealpha  =  false ;
  boolean  somedigit  =  false ;
  boolean  somelower  =  false ;
  boolean  strangechar  =  false ;
  Word  lastInput  =  null ;
  Word  thisinput  =  null ;
  Value  priorcase  =  null ;
  State  scanState  =  null ;
  State  nounState  =  null ;
  String  file  =  null ;
  Integer  beg  =  null ;
  Integer  end  =  null ;
  Integer  previousEnd  =  null ;
  int  confidence  =  10 ;
 /* true when end of file has been found */
  boolean  finalFlag  =  false ;
  Word[]  foundWords  =  null ;
  Category  wordClass  =  null ;
 /* variable to keep track of current word being parsed. */
  Word  thisWord  =  null ;
 /* variable to keep track of the preceding word. */
  Word  priorWord  =  null ;
 /* variable to keep track of the capcode of thsWord. */
  Value  capcode  =  null ;
 /* variable to keep track of the capcode of the preceding word. */
  Value  priorCapcode  =  null ;
 /* variable to keep track of the preceding punct char(s) or codes. */
  Word  priorInput  =  null ;
 /* variable to keep track of features of current word. */
  Value[]  wordFeatures  =  null ;
 /* variable to record found item to be transmitted. */
  Phrase  transmitbuffer  =  null ;
 /* flag to indicate that the current transmission includes the current word. */
  boolean  usedThisWord  =  false ;
 /*  accumulates a list of guessed last names. */
  Vector  guessedNames  =  null ;
 /*  accumulates a list of found first names. */
  Vector  goodFirstNames  =  null ;
 /*  accumulates a list of found last names. */
  Vector  goodLastNames  =  null ;
 /* indicates whether a sentence break occurs before the current word. */
  boolean  sbreakFlag  =  false ;
 /* parameter to indicate whether text being analyzed is all upper case.
 It is initially t,and is set to nil as soon as a nonUpper case word is seen */
  boolean  ucText  =  true ;
 /*  Indicates whether text being analyzed is all initial caps
   It is initially true, then set to nil when a nonCapitalized word is seen. */
  boolean  icText  =  true ;
 /* variable to keep track of where current phrase started */
  Integer  phraseStart  =  null ;
 /* variable to keep track of where parallel alternative phrase started */
  Integer  altPhraseStart  =  null ;
 /*  Keeps track of position where an alternative to postmod phrase 
or adverb sequence would start */
  Integer  tempPhraseStart  =  null ;
 /*  Keeps track of position where an alternative previous phrase 
 would end */
  Integer  tempPhraseEnd  =  null ;
 /* variable to keep track of where alternative verb phrase started */
  Integer  verbPhraseStart  =  null ;
 /*  Keeps track of where possible nounPhrase following verb would start. */
  Integer  postVerbPhraseStart  =  null ;
 /* variable that records start position of a possible compound. */
  Integer  compoundStart  =  null ;
 /* variable that records the remaining tree of possible compound completions. */
  List  restCompoundTree  =  null ;
 /* variable that records queue of pending word matches while matching compounds. */
  Vector  sp_wordQueue  =  null ;
 /* variable to track register contents for tracing purposes */
  Object[]  sp_oldRegisterContents  =  null ;
 /* variable to track register contents for tracing purposes */
  Object[]  sp_newRegisterContents  =  null ;

 void freshStartIndexer() {
  /* set a bunch of ATN state vars for a fresh start */
    sp_processPhraseFn  =  object_Indexscannedphr_Etc ;
    clearMorphCache();
   warmStartIndexVars();
 }


 void warmStartIndexVars() {
    sbreakFlag  =  true ;
    ucText  =  true ;
    icText  =  true ;
    priorInput  =  null ;
    priorWord  =  null ;
    priorCapcode  =  null ;
    compoundStart  =  null ;
    restCompoundTree  =  null ;
    sp_wordQueue  =  null ;
    scanState  =  null ;
   initializeStateVariables();
 }


 boolean numberContainsValue( Number  x ) {
  /* true when number has integer value but is NOT the unset-value for numbers */
    return  ( ( x  instanceof  Number ) &&
           intContainsValue(x.intValue()));
 }


 boolean intContainsValue( int  x ) {
  /* true when x is an int and is NOT the unset-value for numbers */
    return   ( x  !=  sp_unsetNumberValue );
 }


 NounPhrase moveNpDeterminerToMods( NounPhrase  maybenounphrase ) {
  /* if there's a determiner, move it to mods */
   if     ( !  ( maybenounphrase  instanceof  NounPhrase ))
       throw  new ArgMustBeNounPhraseException( maybenounphrase );
   if     ( maybenounphrase.determiner  !=  null )
      { addNounPhraseModifier( maybenounphrase ,  maybenounphrase.determiner );
       setNounPhraseDeterminer( maybenounphrase ,  null );
      }
    return   maybenounphrase ;
 }


 State handleRetroPossessive() {
  /* action for two of the clauses in npAfterDet  */
    Phrase  head  =  ((PossPhrase) determiner).object ;
   if    ( scantrace )
      scantraceFile.print( "                backing up from false possessive: " + "\n" + " with determiner " + determiner.toString() + "\n" );
   if     ( head  instanceof  NamePhrase )
      {  prefixtitle  =  ((NamePhrase) head).prefixtitle ;
        firstname  =  ((NamePhrase) head).firstname ;
        initials  =  ((NamePhrase) head).initials ;
        lastname  =  ((NamePhrase) head).lastname ;
        namesuffix  =  ((NamePhrase) head).namesuffix ;
        determiner  =  null ;
      }

   else  if    ( head  instanceof  CityPhrase )
      {  city  =  ((CityPhrase) head).city ;
        statename  =  ((CityPhrase) head).statename ;
        postalCode  =  ((CityPhrase) head).postalCode ;
        country  =  ((CityPhrase) head).country ;
        determiner  =  null ;
      }

   else  if    ( head  instanceof  DatePhrase )
      {  weekday  =  ((DatePhrase) head).weekday ;
        month  =  ((DatePhrase) head).month ;
        day  =  ((DatePhrase) head).day ;
        year  =  ((DatePhrase) head).year ;
        determiner  =  null ;
      }

   else  if    ( head  instanceof  NounPhrase )
      {  modifiers  = reverseNodeSequence( ((NounPhrase) head).modifiers );
        postmods  = reverseNodeSequence( ((NounPhrase) head).postmods );
        noun  =  ((NounPhrase) head).noun ;
        determiner  =  ((NounPhrase) head).determiner ;
      }

   else   throw  new BadHeadTypeInComplexDeterminerException( determiner );
    verb  =  null ;
   /* ("clear verb so don't get false obj -- e.g., saw the king's") */
    return  popNpStack();
 }


 boolean breakwordTest( Word  word ) {
  /* breakwordTest (word) tests whether word should be
  treated as a breakword for some purposes in phrase
  extraction. */
    return  ( (word.getdict( feature_Breakword ) !=  null ) &&
            ( ! ( LexiconUtil.isMembOfArray( word ,  sp_dontBreakOnTheseWords )) ));
 }


 boolean canBePluralDeterminer( Word  x ) {
  /* canBePluralDeterminer (x) tests if x is a determiner
  that can agree with plural nouns. */
    return  (x.isFormOfCat( categorySet_Det$Number ) &&
           (LexiconUtil.isMembOfArray( x ,  sp_pluralDeterminerWords ) ||
              (x.getdict( feature_Detfeatures ) ==  null ) ||
              LexiconUtil.isInList( feature_Pldet , (List)(x.getdict( feature_Detfeatures ))) ||
             (x.generalNumberp() &&
                (x.numericalValue().doubleValue() !=  1.0 ))));
 }


 boolean canBeSingularDeterminer( Word  x ) {
  /* canBeSingularDeterminer (x) tests if x is a determiner
  that can agree with singular nouns. */
    return  (x.isFormOfCat( categorySet_Det$Number ) &&
           (LexiconUtil.isMembOfArray( x ,  sp_singularDeterminerWords ) ||
              (x.getdict( feature_Detfeatures ) ==  null ) ||
              LexiconUtil.isInList( feature_Sgdet , (List)(x.getdict( feature_Detfeatures ))) ||
              LexiconUtil.isInList( feature_Massdet , (List)(x.getdict( feature_Detfeatures ))) ||
             (x.generalNumberp() &&
                (x.numericalValue().doubleValue() ==  1.0 ))));
 }


 boolean canBeRomanNumeral( Word  word ) {
  /* nonNil if this word can be a roman numeral */
    return  (word.wordp() &&
           (LexiconUtil.isMembOfArray( word_Roman_Numeral , word.getIioParents())));
 }


 boolean mustBePluralDeterminer( Word  x ) {
  /* mustBePluralDeterminer (x) tests if x is a determiner
  that can only agree with plural nouns. */
    return  (detOrNumber( x ) &&
            ( ! ( canBeRomanNumeral( x )) ) &&
           (LexiconUtil.isMembOfArray( x ,  sp_pluralDeterminerWords ) ||
             detFeaturesTest( x ,  featureSet_Pldet ,  featureSet_Sgdet$Massdet ) ||
             (x.generalNumberp() &&
                (x.numericalValue().doubleValue() !=  1.0 ))));
 }


 boolean mustBeSingularDeterminer( Word  x ) {
  /* mustBeSingularDeterminer (x) tests if x is a determiner
  that can only agree with singular nouns. */
    return  (detOrNumber( x ) &&
            ( ! ( canBeRomanNumeral( x )) ) &&
           (LexiconUtil.isMembOfArray( x ,  sp_singularDeterminerWords ) ||
             detFeaturesTest( x ,  featureSet_Sgdet ,  featureSet_Pldet ) ||
             detFeaturesTest( x ,  featureSet_Massdet ,  featureSet_Countdet$Pldet ) ||
             (x.generalNumberp() &&
                (x.numericalValue().doubleValue() ==  1.0 ))));
 }


 boolean mustBeCountDeterminer( Word  x ) {
  /* mustBeCountDeterminer (x) tests if x is a determiner
  that can only agree with singular nouns. */
    return  (LexiconUtil.isMembOfArray( x ,  sp_bannedAsMassOrPluralDeterminers ) ||
           ( ( ! ( LexiconUtil.isMembOfArray( x ,  sp_bannedAsCountDeterminerWords )) ) &&
             (detFeaturesTest( x ,  featureSet_Countdet ,  featureSet_Massdet ) ||
               (x.isFormOfCat( categorySet_Number ) &&
                  ( ! ( canBeRomanNumeral( x )) )))));
 }


 boolean mustBeMassOrPluralDeterminer( Word  x ) {
  /* mustBeMassDeterminer (x) tests if x is a determiner
  that can only agree with mass nouns unless the noun
  is in the plural. */
    return  (LexiconUtil.isMembOfArray( x ,  sp_bannedAsCountDeterminerWords ) ||
           ( ( ! ( LexiconUtil.isMembOfArray( x ,  sp_bannedAsMassOrPluralDeterminers )) ) &&
             detFeaturesTest( x ,  featureSet_Massdet ,  featureSet_Countdet ) &&
             x.isFormOfCat( categorySet_Det ) &&
              ( ! ( x.isFormOfCat( categorySet_Number )) )));
 }


 boolean detFeaturesTest( Word  det ,  Atom[]  good ,  Atom[]  bad ) {
  /* tests that det is a symbol, has good detfeatures and doesn't have bad ones */
   if    (det.wordp())
      {  Value  dfeatures  = det.getdict( feature_Detfeatures );
        return  ( ( dfeatures  !=  null ) &&
               allFeatsThere( good , (List) dfeatures ) &&
               noFeatsThere( bad , (List) dfeatures ));
      }

   else   return   false ;
 }


 boolean allFeatsThere( Atom[]  keys ,  List  set ) {
   if    (singletonp( keys ))
       return  LexiconUtil.isInList(singleton( keys ),  set );

   else  {  boolean  tempboolean1007 ;
         tempboolean1007  =  true ;
        if     ( keys  !=  null )
           for ( int  i  =  0 ;  i  <  keys.length ;  i++ ) {
               Atom  key  =  keys[i] ;
              { if     ( ! ( LexiconUtil.isInList( key ,  set )) )
                   {  tempboolean1007  =  false ;
                     break ;
                   }
              }
           }
         return   tempboolean1007 ;
       }
 }


 boolean noFeatsThere( Atom[]  keys ,  List  set ) {
   if    (singletonp( keys ))
       return   ( ! ( LexiconUtil.isInList(singleton( keys ),  set )) );

   else  {  boolean  tempboolean1009 ;
         tempboolean1009  =  true ;
        if     ( keys  !=  null )
           for ( int  i  =  0 ;  i  <  keys.length ;  i++ ) {
               Atom  key  =  keys[i] ;
              { if    (LexiconUtil.isInList( key ,  set ))
                   {  tempboolean1009  =  false ;
                     break ;
                   }
              }
           }
         return   tempboolean1009 ;
       }
 }


 boolean nounAgreesWithDet( Word  noun ,  Value  det ) {
  /* nounAgreesWithDet (noun det) true when det can agree with noun in number. */
    return  ( ( !  sp_checkDeterminerAgreement ) ||
            ( det  instanceof  PossPhrase ) ||
           (det.wordp() &&
              ( ! ((mustBeSingularDeterminer((Word) det ) &&
                   mustBePluralNoun( noun )) ||
                 (mustBePluralDeterminer((Word) det ) &&
                   mustBeSingularNoun( noun )) ||
                 (mustBeCountDeterminer((Word) det ) &&
                   noun.isFormOfCat( categorySet_Nm ) &&
                    ( ! ( noun.isFormOfCat( categorySet_Nc )) ))))) ||
           noun.isInstanceOf( word_Unit_Of_Countin_Etc ));
 }


 boolean canBeSingularNonnameNounOrUnit() {
  /* test used in npAfterPostmod and npAfterAn */
    return  ( ( determiner  !=  null ) &&
           thisWord.isNonnameFormOfCat( categorySet_N ) &&
            ( ! ( mustBePluralNoun( thisWord )) ) &&
           (canBeUnit( noun ) ||
             (thisWord.isNonnameFormOfCat( categorySet_Nc ) &&
               noun.isFormOfCat( categorySet_Npl ))) &&
           (LexiconUtil.isMembOfArray( determiner ,  wordSet_How_Many$A$An ) ||
             wordIsFormOfCat( determiner ,  categorySet_Number )) &&
           (canBeUnit( thisWord ) ||
              ( ! ( ( verb  !=  null ) &&
                 verb.checkRootFeature( feature_Indobj )))));
 }


 boolean canBePluralOrMass( Word  noun ) {
  /* canBePluralOrMass (noun) tests if noun can be plural or a mass noun. */
    return  noun.isFormOfCat( categorySet_Npl$Nm );
 }


 boolean canBePluralOrUnit( Word  noun ) {
  /* canBePluralOrUnit (noun) tests if noun can be plural or a unit.
     Used in npAfterNoun and elsewhere */
    return  (noun.isFormOfCat( categorySet_Npl$Unit ) ||
           canBeUnit( noun ));
 }


 boolean topModifierCanBeAdvQualifier() {
  /* test used to verify that most recent modifier is a word and can be
   an AdvQualifier  ..
   pmartin 7 jan 00 solves issue of word versus phrase */
   {  Value  topmod  = nodeSequenceFirst( modifiers );
     return  ( ( topmod  !=  null ) &&
            topmod.wordp() &&
            canBeAdvQualifier((Word) topmod ));
   }
 }


 boolean canBeAdvQualifier( Word  word ) {
  /* canBeAdvQualifier (word) tests if word can be an adverb used to qualify an 
   adjective or other adverb. */
    return  (word.isFormOfCat( categorySet_Adv$Qualifier$N_Etc ) ||
            (word.getdict( feature_Adv ) !=  null ) ||
           (word.isFormOfCat( categorySet_Adv ) &&
              ( ! ( word.isFormOfCat( categorySet_Adv$Clausal$Adv_Etc )) )));
 }


 boolean allModifiersAreAdj() {
  /* allModifiersAreAdj () tests if all of
  the modifiers of a noun phrase are adjectives
  (as opposed to nouns). */
    boolean  tempboolean1010 ;
   {  tempboolean1010  =  true ;
    if     ( modifiers  !=  null )
       for ( int  i  =  0 ;  i  <  modifiers.length ;  i++ ) {
           Value  mod  =  modifiers[i] ;
           boolean  tempboolean1011 ;
          if     ( !  ( mod  instanceof  NamePhrase ))
             if     ( ! (mod.wordp() &&
                      ((Word) mod).isPenaltyFormOfCat( categorySet_Adj$Ord )))
                {  Word  tempWord1012 ;
                 if     ( mod  instanceof  AdjPhrase )
                     tempWord1012  =  ((AdjPhrase) mod).adjective ;

                 else   tempWord1012  = (Word) mod ;
                 if    (tempWord1012.isFormOfCat( categorySet_Adj$Ord ))
                     tempboolean1011  =  ( ! (mod.wordp() &&
                    		 LexiconUtil.isMembOfArray(mod ,  sp_daytimeWords )));

                 else   tempboolean1011  =  false ;
                }

             else   tempboolean1011  =  false ;

          else   tempboolean1011  =  false ;
          { if     ( !  tempboolean1011 )
               {  tempboolean1010  =  false ;
                 break ;
               }
          }
       }
     return   tempboolean1010 ;
   }
 }


 boolean looksLikeOrdinaryWord( Word  word ) {
  /* looksLikeOrdinaryWord (word) checks to see whether word looks like an 
   ordinary word as opposed to a name. */
    boolean  tempboolean1015 ;
    boolean  tempboolean1016 ;
   if    ( ( sp_knownWordList  !=  null ) ||
           ( sp_knownWordFile  !=  null ))
      if    (  tempboolean1015  = word.isKnownWord() )
          return   tempboolean1015 ;

      else  {  Word[]  tempWordSet1017  = word.getDirectRoots();
            tempboolean1016  =  false ;
           if     ( tempWordSet1017  !=  null )
              for ( int  i  =  0 ;  i  <  tempWordSet1017.length ;  i++ ) {
                  Word  root  =  tempWordSet1017[i] ;
                 { if    (root.isKnownWord())
                      {  tempboolean1016  =  true ;
                        break ;
                      }
                 }
              }
            return   tempboolean1016 ;
          }

   else  {  boolean  tempboolean1018 ;
         boolean  tempboolean1019 ;
         boolean  tempboolean1020 ;
        if    (  tempboolean1019  = word.guessedWordp() )
            tempboolean1018  =  tempboolean1019 ;

        else  if   (  tempboolean1020  = ( (word.getdict( feature_Derivation ) !=  null ) &&
                (word.getDirectRoots() ==  null )) )
            tempboolean1018  =  tempboolean1020 ;

        else  {  Word[]  tempWordSet1021  = word.getAllRoots();
              tempboolean1018  =  false ;
             if     ( tempWordSet1021  !=  null )
                for ( int  i  =  0 ;  i  <  tempWordSet1021.length ;  i++ ) {
                    Word  root  =  tempWordSet1021[i] ;
                   { if    (root.guessedWordp())
                        {  tempboolean1018  =  true ;
                          break ;
                        }
                   }
                }
            }
         return   ( !  tempboolean1018 );
       }
 }


 boolean canBeUnit( Word  word ) {
  /* canBeUnit (word) tests whether word can be a unit. */
    boolean  tempboolean1022 ;
    boolean  tempboolean1023 ;
   if    (  tempboolean1022  = word.isFormOfCat( categorySet_Unit ) )
       return   tempboolean1022 ;

   else  if   (  tempboolean1023  = word.isKindOf( word_Unit ) )
       return   tempboolean1023 ;

   else  if   (word.isFormOfCat( categorySet_Npl ))
      {  boolean  tempboolean1024 ;
        Word[]  tempWordSet1025  = word.getInflectionRoots();
        tempboolean1024  =  false ;
       if     ( tempWordSet1025  !=  null )
          for ( int  i  =  0 ;  i  <  tempWordSet1025.length ;  i++ ) {
              Word  x  =  tempWordSet1025[i] ;
             { if    (x.isFormOfCat( categorySet_Unit ) ||
                      x.isKindOf( word_Unit ))
                  {  tempboolean1024  =  true ;
                    break ;
                  }
             }
          }
        return   tempboolean1024 ;
      }

   else   return   false ;
 }


 boolean looksLikeUnit( Word  word ) {
  /* looksLikeUnit (word) tests whether word is a unit of some kind. */
    boolean  tempboolean1026 ;
   if    (  tempboolean1026  = word.isFormOfCat( categorySet_Unit ) )
       return   tempboolean1026 ;

   else  if   (word.isFormOfCat( categorySet_Npl ))
      {  boolean  tempboolean1027 ;
        Word[]  tempWordSet1028  = word.getInflectionRoots();
        tempboolean1027  =  false ;
       if     ( tempWordSet1028  !=  null )
          for ( int  i  =  0 ;  i  <  tempWordSet1028.length ;  i++ ) {
              Word  x  =  tempWordSet1028[i] ;
             { if    (x.isFormOfCat( categorySet_Unit ))
                  {  tempboolean1027  =  true ;
                    break ;
                  }
             }
          }
        return   tempboolean1027 ;
      }

   else   return   false ;
 }


 boolean looksLikeUnitOfTime( Word  word ) {
  /* looksLikeUnitOfTime (word) tests whether word is a unit of time. */
    boolean  tempboolean1029 ;
   if    (  tempboolean1029  = LexiconUtil.isMembOfArray( word_Unit_Of_Time , word.getIioParents()) )
       return   tempboolean1029 ;

   else  if   (word.isFormOfCat( categorySet_Npl ))
      {  boolean  tempboolean1030 ;
        Word[]  tempWordSet1031  = word.getInflectionRoots();
        tempboolean1030  =  false ;
       if     ( tempWordSet1031  !=  null )
          for ( int  i  =  0 ;  i  <  tempWordSet1031.length ;  i++ ) {
              Word  x  =  tempWordSet1031[i] ;
             { if    (LexiconUtil.isMembOfArray( word_Unit_Of_Time , x.getIioParents()))
                  {  tempboolean1030  =  true ;
                    break ;
                  }
             }
          }
        return   tempboolean1030 ;
      }

   else   return   false ;
 }


 boolean numberOKForDateDay( double  num ,  Word  monthword ) {
  /* partial test used in xAfterCardinal, xAfterNumber,  and dateAfterMonth */
   {  int  testdays  =  31 ;
     Number  thismonthdays  =  null ;
    if     ( monthword  !=  null )
        thismonthdays  = monthword.getdict(feature_Monthdays).numericalValue();
    if     ( thismonthdays  !=  null )
        testdays  = thismonthdays.intValue();
     return  ( ( num  == (int) num ) &&
             ( num  >  0 ) &&
             ( !  ( num  >  testdays )));
   }
 }

/* prelude */

 boolean wordOKNumForDateDay( Word  word ) {
  /* short call form */
    return  wordOKNumForDateDay( word ,  null );
 }


 boolean wordOKNumForDateDay( Word  word ,  Word  month ) {
  /* partial test used in xAfterCardinal, xAfterNumber,  and dateAfterMonth */
    return  (word.numeralp() &&
           numberOKForDateDay(word.numericalValue().doubleValue(),  month ));
 }

/* prelude */

 boolean integerOrOrdOkNumForDateDay( Word  word ) {
  /* short call form */
    return  integerOrOrdOkNumForDateDay( word ,  null );
 }


 boolean integerOrOrdOkNumForDateDay( Word  word ,  Word  month ) {
  /* partial test used in npAfterNoun */
    return  (word.isFormOfCat( categorySet_Integer$Ord ) &&
           numberOKForDateDay(word.numericalValue().doubleValue(),  month ));
 }


 boolean numCanAddToMultipleOfTenOrHundred( Word  newnumword ,  double  oldintnum ) {
  /* boolean used in xAfterCardinal */
   {  Number  tempnum  = newnumword.numericalValue();
    if     ( tempnum  !=  null )
       {  double  tempn  = tempnum.doubleValue();
         return  ( ( tempn  >  0 ) &&
                 ( oldintnum  !=  0 ) &&
                ((  ( tempn  <  10 ) &&
                     ((  oldintnum  %  10  ) ==  0 )) ||
                  ( ( tempn  <  100 ) &&
                     ((  oldintnum  %  100  ) ==  0 ))));
       }

    else   return   false ;
   }
 }


 boolean plausibleYear( Word  word ) {
  /* plausibleYear (word) tests whether word could be
  a twoDigit or fourDigit year number */
    return  (word.numeralp() &&
           word.integerWordp() &&
           ( ( word.getWordString().length()  ==  2 ) ||
              ( word.getWordString().length()  ==  4 )));
 }


 boolean likelyYear( double  num ) {
  /* likelyYear (num) tests whether the number looks like
  a fourDigit year number */
   if     ( num  == (int) num )
      {  int  baseyearv  =  sp_thisYear ;
        int  inum  = (int) num ;
        return  ( ( inum  >= (  baseyearv  -  200  )) &&
                ( inum  <= (  baseyearv  +  200  )));
      }

   else   return   false ;
 }

/* prelude */

 int makePresumedYear( int  x ) {
  /* short call form */
    return  makePresumedYear( x ,  null );
 }


 int makePresumedYear( int  x ,  Atom  pastorfutureflag ) {
  /* makePresumedYear (x &optional pastOrFutureFlag) takes
  a twoDigit year spec and produces the year spec to
  use in a date description.  When pastOrFutureFlag is
  'future it computes a future year; when pastOrFutureFlag
  is 'past, it computes a past year; if pastOrFutureFlag
  is 'abstract, it returns the ambiguous twoDigit year
  number; and otherwise it computes a year that is within
  the time span from *futureHorizon* years into the future
  to 100 years prior to that -- e.g., when *futureHorizon*
  is 25, it computes a year from 75 years previous to the 
  current year to 25 years into the future. */
   {  int  tempyear ;
     tempyear  = baseYear() +  x ;
    if     ( pastorfutureflag  ==  atom_Past )
       if     ( tempyear  >  sp_thisYear )
           return   tempyear  -  100 ;

       else   return   tempyear ;

    else  if    ( pastorfutureflag  ==  atom_Future )
       if     ( tempyear  <  sp_thisYear )
           return   tempyear  +  100 ;

       else   return   tempyear ;

    else  if    ( pastorfutureflag  ==  atom_Abstract )
        return   x ;

    else  if    ( tempyear  > (  25  +  sp_thisYear  ))
        return   tempyear  -  100 ;

    else  if    ( tempyear  <= (  sp_thisYear  +  25  ))
        return   tempyear  +  100 ;

    else   return   tempyear ;
   }
 }


 int baseYear() {
  /* base year to add to two digit year codes */
    return  (  sp_thisYear  /  100  ) *  100 ;
 }


 void assimilateVerbToModifiersIfPossible() {
  /* assimilateVerbToModifiersIfPossible () adds a verb to the
   modifiers list in a noun phrase if it is compatible. */
   if    ( ( verb  !=  null ) &&
           ( determiner  ==  null ) &&
          verb.looksLike( category_Adv$Adj$N ))
      if     ( modifiers  ==  null )
         {  modifiers  =  new Value[] { verb };
           verb  =  null ;
         }

      else  if   (verb.isFormOfCat( categorySet_Adj$N ))
         {  modifiers  = nodeSequenceAddNode( modifiers ,  verb );
           verb  =  null ;
         }

      else  {  Value  firstmod  = nodeSequenceLast( modifiers );
           if    (firstmod.wordp() &&
                  ((Word) firstmod).isFormOfCat( categorySet_Adj ))
              {  modifiers  = nodeSequenceReplaceLast( modifiers , (adjPhraseWithOneAdv((Word) firstmod ,  verb )));
                verb  =  null ;
              }

           else  if    ( firstmod  instanceof  AdjPhrase )
              {  modifiers  = nodeSequenceReplaceLast( modifiers , (adjPhraseAddAdv((AdjPhrase) firstmod ,  verb )));
                verb  =  null ;
              }

           else  { /* empty statement here */ }
          }
 }


 void processRestOfWordQueue() {
  /* action from parsePunct */
   while    ( sp_wordQueue  !=  null )

       {  compoundStart  =  null ;
        restCompoundTree  =  null ;
       processWordQueue( sp_wordQueue );
      }
 }


 boolean justLettersWithPeriodsBetweenThem( Word  word ) {
    return  justLettersWithPeriods( word ,  true );
 }

/* prelude */

 boolean justLettersWithPeriods( Word  word ) {
  /* short call form */
    return  justLettersWithPeriods( word ,  false );
 }


 boolean justLettersWithPeriods( Word  word ,  boolean  nofinalperiodflag ) {
  /* justLettersWithPeriods (word) tests whether
  word looks like an abbreviation by virtue
  of consisting of individual letters
  separated by periods -- e.g., u.s.g.s.
  Turning on NoFinalPeriodFlag rules out words ending in period */
   {  String  wordstring  = word.getWordString();
     boolean  expectperiod  =  false ;
    if     ( ! ( wordstring.equals( "" )) )
       {  boolean  tempboolean1034 ;
         tempboolean1034  =  true ;
        for ( int  i  =  1 ;  i  < wordstring.length();  i++ ) {
            boolean  tempboolean1035 ;
           if    ( expectperiod  &&
                   (wordstring.charAt((  i  -  1  )) ==  '.' ))
              {  expectperiod  =  false ;
                tempboolean1035  =  true ;
              }

           else  if   ( ( !  expectperiod ) &&
                  Character.isLetter(wordstring.charAt((  i  -  1  ))))
              {  expectperiod  =  true ;
                tempboolean1035  =  expectperiod ;
              }

           else   tempboolean1035  =  false ;
           if     ( !  tempboolean1035 )
              {  tempboolean1034  =  false ;
                break ;
              }
        }
        if    ( tempboolean1034 )
            return  ( expectperiod  ||
                    ( !  nofinalperiodflag ));

        else   return   false ;
       }

    else   return   false ;
   }
 }

 /* variable to remember saved punctbuffer while looking for compounds. */
  Word[]  sp_savedpunctbuffer  =  null ;

 Word[] savePunctBuffer() {
  /* savePunctbuffer () saves the punctuation buffer so it can
  be restored later.  This is used in getATNWord to save the state
  of the punctbuffer at the beginning of a potential compound, so
  it can be restored later when processing the compound word or
  the first word of what turned out not to be a compound. */
   {  sp_savedpunctbuffer  = punctbufferContents();
     return   sp_savedpunctbuffer ;
   }
 }

/* prelude */

 Word parseWord( Word  word ,  Value  pwcapcode ,  boolean  sentenceStartFlag ,  String  file ,  int  wordbeg ,  int  wordend ) {
  /* short call form */
    return  parseWord( word ,  pwcapcode ,  sentenceStartFlag ,  file ,  wordbeg ,  wordend ,  null ,  finalFlag );
 }


 Word parseWord( Word  word ,  Value  pwcapcode ,  boolean  sentenceStartFlag ,  String  file ,  int  wordbeg ,  int  wordend ,  Word  nextchar ) {
  /* short call form */
    return  parseWord( word ,  pwcapcode ,  sentenceStartFlag ,  file ,  wordbeg ,  wordend ,  nextchar ,  finalFlag );
 }


 Word parseWord( Word  word ,  Value  pwcapcode ,  boolean  sentenceStartFlag ,  String  file ,  int  wordbeg ,  int  wordend ,  Word  nextchar ,  boolean  pwfinalflag ) {
  /* parseWord (word pwcapcode sentenceStartFlag file wordBeg wordEnd
                        &optional nextchar (pwFinalFlag final-flag))
   is the function used to process each word
   found by the simple phrase indexer.  parseWord
   operates in the context of knowing the character
   immediately following the current word and having
   access to a punctbuffer of punctuation that separates
   it from the preceding word. */
    capcode  =  pwcapcode ;
    finalFlag  =  pwfinalflag ;
   {  State  transfn  =  null ;
     State  newstate  =  null ;
     Phrase  savetransmitbuffer  =  transmitbuffer ;
     boolean  saveusedthisword  =  usedThisWord ;
     State  priornounstate  =  null ;
     String  stateString  =  null ;
    if    ( sentenceStartFlag )
        sbreakFlag  =  true ;
    if    (upperCaseClashesWithText() &&
            ( priorcase  ==  capcode_Uc ) &&
            ( word.getWordString().length()  !=  1 ) &&
            ( ! (priorWord.isFormOfCat( categorySet_Npr ) ||
               word.isFormOfCat( categorySet_Npr ))))
       { if   ( scantrace )
           scantraceFile.print( "**          noting ucText with " + word.getWordString() + " at " + wordbeg + "\n" );
         icText  =  false ;
         ucText  =  true ;
       }
    if    ( ( !  icText ) &&
            ( capcode  ==  capcode_Ic ) &&
            ( priorcase  ==  capcode_Ic ) &&
            ( word.getWordString().length()  !=  1 ) &&
            ( ! (priorWord.isFormOfCat( categorySet_Npr ) ||
               word.isFormOfCat( categorySet_Npr ))))
       { if   ( scantrace )
           scantraceFile.print( "**          noting icText with " + word.getWordString() + " at " + wordbeg + "\n" );
         ucText  =  false ;
         icText  =  true ;
       }
    if    ( ucText  &&
    		LexiconUtil.isMembOfArray( capcode , featureSet_Lc$Ic ) &&
           ( icText  ||
              ( priorcase  !=  capcode_Uc )))
       { if   ( scantrace )
           scantraceFile.print( "**          noting not ucText with " + word.getWordString() + " at " + wordbeg + "\n" );
         ucText  =  false ;
       }
    if    ( icText  &&
           ( ucText  ||
             ( ( capcode  ==  capcode_Lc ) &&
                ( ! ( word.punctuationp()) ) &&
                ( ! ( word.isFormOfCat( categorySet_Det$Prep$Conj$P_Etc )) ) &&
                ( ! ( Character.isDigit(word.toString().charAt( 0 ))) ))))
       { if   ( scantrace )
           scantraceFile.print( "**          noting not icText with " + word.getWordString() + " at " + wordbeg + "\n" );
         icText  =  false ;
       }
    if    ( (prevPunct( 1 ) ==  word_Backslashtick ) &&
           noPrevPunctAfter( 1 ) &&
           patternCheckFinalS( priorWord ) &&
           priorWord.isFormOfCat( categorySet_Npl ))
       {  Word[]  oldpunctbuffer  = punctbufferContents();
         Value  capcode  =  priorCapcode ;
         int  pos  =  wordbeg  - punctbuffer.fillPointer();
         nextchar  = prevPunct( 2 );
        punctbuffer.clearWordBuffer();
        parseWord( word_$S ,  capcode ,  false ,  file ,  pos ,  1  +  pos ,  nextchar );
        restorePunctBuffer((nodeSequenceRest( oldpunctbuffer )));
       }
     refcount  =  new Integer( wordbeg );
     previousEnd  =  end ;
     end  =  new Integer( wordend );
     thisinput  =  nextchar ;
    setWordClassAndFeatures( word );
     thisWord  =  word ;
     priornounstate  =  nounState ;
    if    ( scantrace )
       if     ( sp_newRegisterContents  !=  null )
           sp_oldRegisterContents  =  sp_newRegisterContents ;

       else   sp_oldRegisterContents  = captureStateVariables();
    if    ( ( sp_stopAtWords  !=  null ) &&
    		LexiconUtil.isMembOfArray( word ,  sp_stopAtWords ))
       System.out.println( "DEBUG-FORMAT==debug break at " + word.getWordString() );
    if    ( ( scanState  !=  null ) &&
            ((  transfn  = scanState.transitionFunction() ) !=  null ) &&
            ((  newstate  = applyTransition( transfn ) ) !=  null ))
       { /* empty statement here */ }
    if    ( scantrace  &&
            ( priornounstate  !=  null ) &&
            ( nounState  !=  priornounstate ))
       scantraceFile.print( "                state " + scanState + " changed nounState from " + priornounstate + " to " + nounState + "\n" );
    if    ( sp_autoPhraseFlag )
       { if   ( ( newstate  !=  null ) ||
                ( transmitbuffer  !=  null ))
           { if    ( ( priornounstate  !=  null ) &&
                     ( priornounstate  ==  nounState ))
                { if   ( scantrace  &&
                         ( transmitbuffer  !=  null ) &&
                         ( !  usedThisWord ))
                    printScantrace( transmitbuffer );
                  transmitbuffer  =  null ;
                 if     ( newstate  !=  null )
                    {  nounState  = applyTransition( priornounstate );
                     if    ( scantrace )
                        scantraceFile.print( "                parallel phrase from " + priornounstate + " to " + nounState + "\n" );
                    }
                }
           }
        if     ( newstate  ==  null )
           { if    ( ( priornounstate  !=  null ) &&
                     ( priornounstate  ==  nounState ))
                { scanTraceSwitchToNounPhrase();
                 if    ( scantrace  &&
                         ( transmitbuffer  !=  null ) &&
                         ( !  usedThisWord ))
                    printScantrace( transmitbuffer );
                  transmitbuffer  =  null ;
                  newstate  = commitToNounState( true );
                }
           }
       }
    if    ( scantrace  &&
            ( transmitbuffer  !=  null ) &&
            ( !  usedThisWord ))
       printScantrace( transmitbuffer );
    if    ( ( newstate  ==  null ) &&
           ( ( transmitbuffer  ==  null ) ||
              ( !  usedThisWord )))
       {  scanState  =  null ;
        initializeStateVariables();
        if    (( ( ! ( wordClass.equals( category_Unknown )) ) &&
                  ((  transfn  = startFunction( wordClass ) ) !=  null )) ||
               ( sp_findPhrasesFlag  &&
                  ((  transfn  =  generalPhraseStart  ) !=  null )))
           {  beg  =  refcount ;
             newstate  = applyTransition( transfn );
           }

        else   newstate  =  null ;
       }
    if    ( scantrace )
       { if   ( sp_scantraceInLowercase )
           scantraceFile.print( word.getWordString().toLowerCase() );

        else  scantraceFile.print( capitalizeByCapcode(word.getWordString(), capcode) );
         int  spnum  = Math.max( 0 ,  15  -  word.getWordString().length() );
        System.out.print( staticString80Blanks.substring(0, spnum) );
        if     ( newstate  !=  null )
            stateString  =  newstate.name ;

        else  if   ( ( transmitbuffer  !=  null ) &&
                usedThisWord )
            stateString  =  "success" ;

        else   stateString  =  null ;
        scantraceFile.print( " " + wordClass.toString() + "  from " + scanState + " to " + stateString + " , capcode = " + capcode.toString() + ", conf. = " + confidence + "\n" );
         sp_newRegisterContents  = captureStateVariables();
        showChangedStateVariables( scantraceFile );
        if    ( ( transmitbuffer  !=  null ) &&
                usedThisWord )
           printScantrace( transmitbuffer );
       }
     scanState  =  newstate ;
     sbreakFlag  =  false ;
    punctbuffer.clearWordBuffer();
    if     ( ! ( ( priorcase  ==  capcode_Ic ) &&
              ( capcode  ==  capcode_Lc ) &&
             word.isFormOfCat( categorySet_Det$Prep$Conj )))
        priorcase  =  capcode ;
     priorWord  =  thisWord ;
     priorCapcode  = capcode();
     priorInput  =  null ;
     transmitbuffer  =  savetransmitbuffer ;
     usedThisWord  =  saveusedthisword ;
     return   thisWord ;
   }
 }


 State startParallelNounPhrase() {
  /* startParallelNounPhrase () starts a parallel noun phrase
  while parsing part of a name. */
    altPhraseStart  =  refcount ;
    nounState  = generalPhraseStart( true );
   if     ( nounState  !=  null )
      scantraceParallelPhraseStart( null );
    return   nounState ;
 }


 void scantraceParallelPhraseStart( State  fromstate ) {
  /*  just a tracing routine */
   if    ( scantrace )
      scantraceFile.print( "                starting parallel phrase from " + fromstate + " to " + nounState + "\n" );
 }


 State followParallelNounPhrase() {
  /* followParallelNounPhrase () follows a parallel noun phrase
  while parsing part of a name. */
   if     ( !  sp_autoPhraseFlag )
      if     ( nounState  !=  null )
         { if   ( scantrace )
             scantraceFile.print( "                parallel transition from " + nounState );
           nounState  = applyTransition( nounState );
          if    ( scantrace )
             scantraceFile.print( " to " + nounState + "\n" );
           return   nounState ;
         }

      else   return   null ;

   else   return   null ;
 }


 State switchToPhrase() {
  /* switchToPhrase () shifts to a parallel noun phrase hypothesis, 
  abandoning the name hypothesis */
   if     ( !  sp_autoPhraseFlag )
       return  commitToNounState();

   else   return   null ;
 }


 State abandonNameForNounPhrase() {
  /* If there is a parallel noun hypothesis, dump the 
    name hypothesis and go with noun */
   if     ( nounState  !=  null )
      { cancelNameCodeHypothesis();
       clearNameParts();
        return  switchToPhrase();
      }

   else   return   null ;
 }

/* prelude */

 State commitToNounState() {
  /* short call form */
    return  commitToNounState( false );
 }


 State commitToNounState( boolean  quietflag ) {
  /*   sets scanState to nounState then
    returns the state reached by transition from scanState */
   if     ( nounState  !=  null )
      { if    ( !  quietflag )
          scanTraceSwitchToNounPhrase();
        scanState  =  nounState ;
        nounState  =  null ;
        phraseStart  =  altPhraseStart ;
        return  applyTransition( scanState );
      }

   else   return   null ;
 }


 void scanTraceSwitchToNounPhrase() {
   if    ( scantrace )
      scantraceFile.print( "                switching to state " + nounState + " from " + scanState + "\n" );
 }


 void initializeParallelNounPhrase( State  fromstate ) {
  /* initializeParallelNounPhrase (fromState) starts an alternative
  noun phrase when the primary transition is
  about to follow a city or name interpretation. */
   if     ( nounState  ==  null )
      { if   (thisWord.isFormOfCat( categorySet_Det ))
          {  determiner  =  thisWord ;
           if     ( ! ( verb.isFormOfCat( categorySet_Vt )) )
               verb  =  null ;
            nounState  =  npAfterDet ;
          }

       else  if   (thisWord.isFormOfCat( categorySet_N ))
          {  noun  =  thisWord ;
            nounState  =  npAfterNoun ;
          }

       else  if   (thisWord.isFormOfCat( categorySet_Adj$N ))
          {  modifiers  =  new Value[] { thisWord };
            nounState  =  npAfterAdj ;
          }

       else  { if   (canBeAdvQualifier( thisWord ))
            {  adverbs  =  new Value[] { thisWord };
              nounState  =  npAfterAdv ;
            }
       }
       if     ( nounState  !=  null )
          {  altPhraseStart  =  phraseStart ;
           scantraceParallelPhraseStart( fromstate );
          }
      }
 }


 Value capcode() {
  /* capcode () is a function for producing
   a concise summary of the capitalization of a word. */
    return   capcode ;
 }


 Category analyzeWordClass( Word  word ) {
   {  Category[]  thesecategories ;
     Word  namconfword ;
     thesecategories  = word.getAllCats();
    if     (word.getdict( feature_Nameconfidence ) !=  null )
       { /* empty statement here */ }

    else  if    ( ! ( categorySet_Firstname$Malef_Etc.subsumesOneOf( thesecategories )) )
       { /* empty statement here */ }

    else  { if   (word.hasNonnameCat())
         { if   (word.looksLike( category_Name ))
              namconfword  =  sp_word9 ;

          else  if   (word.isNonpenaltyFormOfCat( categorySet_Name ))
              namconfword  =  sp_word8 ;

          else  if   (word.isPenaltyFormOfCat( categorySet_Name ,  2 ))
              namconfword  =  sp_word5 ;

          else   namconfword  =  sp_word6 ;
          word.putdict( feature_Nameconfidence ,  namconfword );
         }
    }
    {  Number  val  = word.findNumericalValue();
     if     ( val  instanceof  Number )
        word.setNumericalValue( val );
    }
    if    (word.numeralp())
        return   category_Number ;

    else  if   (LexiconUtil.isMembOfArray( category_Integer ,  thesecategories ))
        return   category_Cardinal ;

    else  if   (LexiconUtil.isMembOfArray( category_Number ,  thesecategories ))
        return   category_Number ;

    else  if   (LexiconUtil.isMembOfArray( category_City ,  thesecategories ))
        return   category_City ;

    else  if   (LexiconUtil.isMembOfArray( category_Title ,  thesecategories ))
        return   category_Prefixtitle ;

    else  if   (categorySet_Firstname$Malef_Etc$.subsumesOneOf( thesecategories ) &&
            (word.getdict( feature_Lastname ) !=  null ))
        return   category_Dualname ;

    else  if   (categorySet_Firstname$Malef_Etc$.subsumesOneOf( thesecategories ))
        return   category_Firstname ;

    else  if   (categorySet_Lastname.subsumesOneOf( thesecategories ))
        return   category_Lastname ;

    else  if   (LexiconUtil.isMembOfArray( feature_Unit_Of_Countin_Etc , word.getIioParents()))
        return   category_Numberunit ;

    else  {  boolean  tempboolean1039 ;
         if    (word.isFormOfCat( categorySet_Npl ))
            {  Word[]  tempWordSet1040  = word.getInflectionRoots( category_N );
              tempboolean1039  =  false ;
             if     ( tempWordSet1040  !=  null )
                for ( int  i  =  0 ;  i  <  tempWordSet1040.length ;  i++ ) {
                    Word  x  =  tempWordSet1040[i] ;
                   { if    (LexiconUtil.isMembOfArray( feature_Unit_Of_Countin_Etc , x.getIioParents()))
                        {  tempboolean1039  =  true ;
                          break ;
                        }
                   }
                }
            }

         else   tempboolean1039  =  false ;
         if    ( tempboolean1039 )
             return   category_Numberunit ;

         else  if   (LexiconUtil.isMembOfArray( category_Ord ,  thesecategories ))
             return   category_Ordinal ;

         else  if   (LexiconUtil.isMembOfArray( category_Det ,  thesecategories ))
             return   category_Determiner ;

         else  if   (LexiconUtil.isMembOfArray( category_Month ,  thesecategories ))
             return   category_Monthname ;

         else  if   (LexiconUtil.isMembOfArray( category_Title ,  thesecategories ))
             return   category_Prefixtitle ;

         else  if   (LexiconUtil.isMembOfArray( category_Namesuffix ,  thesecategories ))
             return   category_Namesuffix ;

         else  if   (LexiconUtil.isMembOfArray( category_City ,  thesecategories ))
             return   category_City ;

         else  if   (categorySet_Statename$State_Etc.subsumesOneOf( thesecategories ))
             return   category_Statename ;

         else  if    ( !  sp_tagPosFlag )
             return   category_Unknown ;

         else  if   (word.looksLike( category_N ))
             return   category_Noun ;

         else  if   (word.looksLike( category_V ))
             return   category_Verb ;

         else  if   (word.looksLike( category_Adj ))
             return   category_Adjective ;

         else  if   (word.looksLike( category_Adv ))
             return   category_Adverb ;

         else  if   (word.looksLike( category_Prep ))
             return   category_Preposition ;

         else  if   (word.looksLike( category_Conj ))
             return   category_Conjunction ;

         else  {  boolean  tempboolean1041 ;
               tempboolean1041  =  false ;
              if     ( thesecategories  !=  null )
                 for ( int  i  =  0 ;  i  <  thesecategories.length ;  i++ ) {
                     Category  x  =  thesecategories[i] ;
                    { if    (category_Adj.subsumesCategory( x ))
                         {  tempboolean1041  =  true ;
                           break ;
                         }
                    }
                 }
              if    ( tempboolean1041 )
                  return   category_Adjective ;

              else  {  boolean  tempboolean1042 ;
                    tempboolean1042  =  false ;
                   if     ( thesecategories  !=  null )
                      for ( int  i  =  0 ;  i  <  thesecategories.length ;  i++ ) {
                          Category  x  =  thesecategories[i] ;
                         { if    (category_Adv.subsumesCategory( x ))
                              {  tempboolean1042  =  true ;
                                break ;
                              }
                         }
                      }
                   if    ( tempboolean1042 )
                       return   category_Adverb ;

                   else  {  boolean  tempboolean1043 ;
                         tempboolean1043  =  false ;
                        if     ( thesecategories  !=  null )
                           for ( int  i  =  0 ;  i  <  thesecategories.length ;  i++ ) {
                               Category  x  =  thesecategories[i] ;
                              { if    (category_N.subsumesCategory( x ))
                                   {  tempboolean1043  =  true ;
                                     break ;
                                   }
                              }
                           }
                        if    ( tempboolean1043 )
                            return   category_Noun ;

                        else  {  boolean  tempboolean1044 ;
                              tempboolean1044  =  false ;
                             if     ( thesecategories  !=  null )
                                for ( int  i  =  0 ;  i  <  thesecategories.length ;  i++ ) {
                                    Category  x  =  thesecategories[i] ;
                                   { if    (category_V.subsumesCategory( x ))
                                        {  tempboolean1044  =  true ;
                                          break ;
                                        }
                                   }
                                }
                             if    ( tempboolean1044 )
                                 return   category_Verb ;

                             else  if   (LexiconUtil.isMembOfArray( category_Prep ,  thesecategories ))
                                 return   category_Preposition ;

                             else  if   (LexiconUtil.isMembOfArray( category_Conj ,  thesecategories ))
                                 return   category_Conjunction ;

                             else  if   (LexiconUtil.isMembOfArray( category_Interj ,  thesecategories ))
                                 return   category_Interjection ;

                             else   return   category_Unknown ;
                            }
                       }
                  }
             }
        }
   }
 }


 Word foundPunct( Word  punctcode ) {
  /* foundPunct (punctCode) is a function for handling
   found punctuation. */
   if    ( scantrace )
      scantraceFile.print( "   " + punctcode.getWordString() + "\n" );
    return   punctcode ;
 }


 boolean prevBreak() {
  /* prev-break () tests if there is a forced break
  between this word and previous words. */
    return  (sbreak() ||
            ( ! ( noPrevPunct()) ));
 }


 boolean sbreak() {
  /* sbreak () tests whether the current word is preceded by a sentence break.  This
  includes being preceded by a double carriage return as well as by terminal
  punctuation. */
    return  ( sbreakFlag  ||
    		LexiconUtil.isMembOfArray( priorInput ,  wordSet_Sbreak$2cr ));
 }


 Word prevPunct( int  n ) {
  /* prevPunct (n) gives the nth punctuation character that precedes the 
  current word.   When n is positive, counting begins with the first 
    such preceding punctuation character, and when n is negative, counting 
    begins with the punctuation character immediatedly preceding the 
   current word. */
   if    ( ( n  >  0 ) &&
           ( !  ( n  > punctbuffer.fillPointer())))
       return  punctbuffer.elementAt((  n  -  1  ));

   else  if   ( ( n  <  0 ) &&
           ( !  ((  0  -  n  ) > punctbuffer.fillPointer())))
       return  punctbuffer.elementAt((  n  + punctbuffer.fillPointer() ));

   else   return   null ;
 }


 boolean noPrevPunct() {
  /* noPrevPunct () tests that there is no previous
   punctuation except for whitespace characters. */
    boolean  tempboolean1045 ;
   if     (punctbuffer.fillPointer() >  0 )
      {  tempboolean1045  =  false ;
       for ( int  i  =  0 ;  i  < punctbuffer.fillPointer() -  1 ;  i++ ) {
          if     ( ! ( whitecodep((punctbuffer.elementAt(i).toString().charAt( 0 )))) )
             {  tempboolean1045  =  true ;
               break ;
             }
       }
      }

   else   tempboolean1045  =  false ;
    return   ( !  tempboolean1045 );
 }


 boolean noPrevPunctAfter( int  n ) {
  /* noPrevPunct (n) tests that there is no previous
   punctuation after the nth previous punctuation mark
   except for whitespace characters. */
    boolean  tempboolean1046 ;
   if    ( (punctbuffer.fillPointer() >  n ) &&
           ( !  ( n  > punctbuffer.fillPointer())))
      {  tempboolean1046  =  false ;
       for ( int  i  =  n ;  i  < punctbuffer.fillPointer() -  1 ;  i++ ) {
          if     ( ! ( whitecodep((punctbuffer.elementAt(i).toString().charAt( 0 )))) )
             {  tempboolean1046  =  true ;
               break ;
             }
       }
      }

   else   tempboolean1046  =  false ;
    return   ( !  tempboolean1046 );
 }

 /* list of lastnames previously mentioned in this context. */
  Word[]  mentionedLastNames  =  null ;
 /* list of firstnames previously mentioned in this context. */
  Word[]  mentionedFirstNames  =  null ;

 void initializeStateVariables() {
    confidence  =  10 ;
    month  =  null ;
    day  =  sp_unsetNumberValue ;
    year  =  sp_unsetNumberValue ;
    weekday  =  null ;
    dayTime  =  null ;
    timezone  =  null ;
    timeunit  =  null ;
    expectYear  =  false ;
    hour  =  sp_unsetNumberValue ;
    minutes  =  sp_unsetNumberValue ;
    seconds  =  sp_unsetNumberValue ;
    expectSeconds  =  false ;
    dateSeparator  =  null ;
    phraseStart  =  null ;
    altPhraseStart  =  null ;
    tempPhraseStart  =  null ;
    tempPhraseEnd  =  null ;
    verbPhraseStart  =  null ;
    postVerbPhraseStart  =  null ;
    numberWord  =  null ;
    firstnum  =  0.0 ;
    othernum  =  0.0 ;
    firstname  =  null ;
    lastname  =  null ;
    lcName  =  false ;
    ucName  =  false ;
    preposition  =  null ;
    plural  =  false ;
    determiner  =  null ;
    initials  =  null ;
    namesuffix  =  null ;
    prefixtitle  =  null ;
    secondnum  =  0.0 ;
    secondnumStart  =  null ;
    firstnumEnd  =  null ;
    currency  =  null ;
    noun  =  null ;
    modifiers  =  null ;
    adverbs  =  null ;
    verb  =  null ;
    postmods  =  null ;
    nounstack  =  null ;
    city  =  null ;
    statename  =  null ;
    country  =  null ;
    postalCode  =  null ;
 }


 State startFunction( Category  wordClass ) {
  /* startFunction (wordClass) gets the start function that creates
   the atn transition state for initial words of type wordClass.
   The start function of the wordClass is stored on its plist. */
    return  wordClass.getStartState();
 }


 boolean checkForMonth() {
  /* checkForMonth () checks if thisWord can be a month and
   adjusts confidences for varying circumstances of words
   such as dec, jan, jun, mar, sep. */
   if    (wordIsFormOfCat( thisWord ,  categorySet_Month ))
      {  int  startconf  =  10 ;
        Value  mcw  = thisWord.getdict( feature_Monthconfidence );
        int  monthconf  =  10 ;
       if    (intContainsValue( confidence ))
           startconf  =  confidence ;
       if     ( mcw  !=  null )
           monthconf  = ((Word) mcw).numericalValue().intValue();
        confidence  = Math.min( startconf ,  monthconf );
       if    (LexiconUtil.isMembOfArray(capcode(), featureSet_Ic$Uc ))
          { /* empty statement here */ }

       else   confidence  =  confidence  /  2 ;
       if    (LexiconUtil.isMembOfArray( thisWord ,  wordSet_May$March$Mar$D_Etc ) &&
               (capcode() ==  capcode_Ic ) &&
               ( ! ( sbreak()) ))
           confidence  =  8 ;
        return   true ;
      }

   else   return   false ;
 }

/* prelude */

 int computeNameConfidence( Word  word ,  Value  capcode ) {
  /* short call form */
    return  computeNameConfidence( word ,  capcode ,  false );
 }


 int computeNameConfidence( Word  word ,  Value  capcode ,  boolean  aftersbreak ) {
  /* computeNameConfidence (word capcode &optional afterSbreak)
  computes the confidence that the indicated word is a name.
  When afterSbreak is set, it computes the confidence
  when the word is the first word of a sentence. */
   {  int  nameconfidence  = defaultOrNameConfidence( word );
     boolean  capconflict  =  false ;
     boolean  capambig  =  false ;
     boolean  capsignal  =  false ;
    setNameCapcode(capcode());
    if    ( ( capcode  ==  capcode_Ic ) &&
            ( !  icText ) &&
            (prevPunct( 1 ) !=  word_Backslashquote ) &&
            ( ! ( LexiconUtil.isMembOfArray( priorcase , featureSet_Ic$Uc )) ) &&
            ( !  aftersbreak ))
        capsignal  =  true ;
    if    ((upperCaseClashesWithText() &&
              ( ! ( word.getdict(feature_Wordclass).equals( value_Singleletter )) )) ||
            ( capcode  ==  capcode_Lc ))
        capconflict  =  true ;
    if    ((LexiconUtil.isMembOfArray( capcode , featureSet_Ic$Uc ) &&
    		LexiconUtil.isMembOfArray( priorcase , featureSet_Ic$Uc ) &&
              ( !  icText ) &&
              ( !  ucText ) &&
             noPrevPunct()) ||
           (LexiconUtil.isMembOfArray( capcode , featureSet_Ic$Uc ) &&
        		   LexiconUtil.isMembOfArray( priorcase , featureSet_Ic$Uc ) &&
             noPrevPunct()) ||
           ( ( capcode  ==  capcode_Ic ) &&
             ( (prevPunct( 1 ) ==  word_Backslashquote ) ||
                icText  ||
                aftersbreak )))
        capambig  =  true ;
    if    (LexiconUtil.isMembOfArray( word ,  sp_extremelyUnlikelyNames ))
        nameconfidence  =  1 ;

    else  if   (word.wordp() &&
            (word.getdict( feature_Stoplist ) !=  null ))
        nameconfidence  =  2 ;

    else  { if   (word.isFormOfCat( categorySet_Det$Prep$Pro ))
          nameconfidence  =  3 ;
    }
    if    ( capconflict  ||
            capambig )
       if    (word.looksLike( category_Name ))
           nameconfidence  =  6 ;

       else  if   (word.looksLike( category_N$V$Adj$Ord$Adv ))
          if    ( capconflict )
              nameconfidence  =  nameconfidence  -  6 ;

          else   nameconfidence  = Math.min( nameconfidence ,  6 );

       else  if    ( ! ( word.hasNonnameCat()) )
          { if    ( capconflict )
                nameconfidence  =  nameconfidence  -  1 ;
          }

       else  if   ( capambig )
           nameconfidence  = Math.max( nameconfidence  -  3 , Math.min( nameconfidence ,  5 ));

       else  if   ( capconflict )
           nameconfidence  =  nameconfidence  -  5 ;

       else   nameconfidence  =  confidenceThreshold ;
    if    (LexiconUtil.isMembOfArray( priorWord ,  wordSet_A$The ) &&
           noPrevPunct() &&
            ( ! ( word.looksLike( category_Name )) ))
        nameconfidence  =  nameconfidence  -  2 ;
    if    ( capsignal  &&
            ( nameconfidence  <  confidenceThreshold ))
        nameconfidence  =  confidenceThreshold ;
     return   nameconfidence ;
   }
 }

/* prelude */

 State generalPhraseStart() {
  /* short call form */
    return  generalPhraseStart( false );
 }


 State generalPhraseStart( boolean  altflag ) {
  /*  the most general start function for phrases */
   if     ( !  altflag )
       phraseStart  =  refcount ;
   if    (thisWord.isFormOfCat( categorySet_Det ))
      {  determiner  =  thisWord ;
        return   npAfterDet ;
      }

   else  if   (breakwordTest( thisWord ))
       return   null ;

   else  if   ( sp_forceAdvQualifier  &&
          canBeAdvQualifier( thisWord ))
      {  adverbs  =  new Value[] { thisWord };
        return   npAfterAdv ;
      }

   else  if   (realNonBeVerb( thisWord ))
      {  verb  =  thisWord ;
        verbPhraseStart  =  refcount ;
        return   npAfterVerb ;
      }

   else  if   ( ( !  sp_forceAdvQualifier ) &&
          canBeAdvQualifier( thisWord ))
      {  adverbs  =  new Value[] { thisWord };
        return   npAfterAdv ;
      }

   else  if   (thisWord.isNonnameFormOfCat( categorySet_N ))
      {  noun  =  thisWord ;
        return   npAfterNoun ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Adj$N ))
      {  modifiers  =  new Value[] { thisWord };
        return   npAfterAdj ;
      }

   else   return   null ;
 }


 boolean thisWordNotToBeNowOrPast() {
    return   ( ! ( LexiconUtil.isMembOfArray( thisWord ,  wordSet_Is$Am$Are$Was$W_Etc )) );
 }


 boolean realNonBeVerb( Word  thisWord ) {
    return  (thisWord.isFormOfCat( categorySet_Vt ) &&
           thisWordNotToBeNowOrPast());
 }


 State cityStart() {
   if    (thisWord.isFormOfCat( categorySet_Name ))
       confidence  = computeNameConfidence( thisWord , capcode());
   if    ( sp_findPhrasesFlag  &&
          thisWord.isNonnameFormOfCat( categorySet_Det$N$Adj$Adv$V_Etc ))
      startParallelNounPhrase();
    phraseStart  =  refcount ;
    return  startNpAfterCity();
 }


 State startNpAfterCity() {
    city  =  thisWord ;
    return   npAfterCity ;
 }


 State monthStart() {
   checkForMonth();
    month  =  thisWord ;
    phraseStart  =  refcount ;
    expectYear  =  ( thisinput  ==  word_Comma );
    return   dateAfterMonth ;
 }


 State numberStart() {
   if     (prevPunct( -1 ) ==  word_$ )
       currency  =  word_Dollar ;
   if     ( ! ( thisWord.generalNumberp()) )
      { System.out.print( "*** warning -- number word " + thisWord.getWordString() + " has no numeric Value" + "\n" );
        return   null ;
      }

   else  { generalnumberSetup();
         dateSeparator  =  null ;
        if    ( ( currency  ==  null ) &&
               ( ( thisinput  ==  word_Slash ) ||
                  ( thisinput  ==  word_Minus )) &&
               thisWord.integerWordp() &&
                ( firstnum  <  32 ))
            dateSeparator  =  thisinput ;
         return   xAfterNumber ;
       }
 }


 State generalnumberSetup() {
  /*  common setup for numbers, ordinals, and cardinals, 
    but not called for suspected Roman Numerals */
    phraseStart  =  refcount ;
   if    (thisWord.generalNumberp())
       firstnum  = thisWord.numericalValue().doubleValue();

   else   firstnum  =  0.0 ;
    numberWord  =  thisWord ;
    return   null ;
 }


 State ordinalStart() {
   generalnumberSetup();
    return   xAfterOrdinal ;
 }


 State cardinalStart() {
   if     ( ! ( canBeRomanNumeral( thisWord )) )
      generalnumberSetup();
    return   xAfterCardinal ;
 }


 State weekdayStart() {
    weekday  =  thisWord ;
   {  Value  wdconf  = thisWord.getdict( feature_Weekdayconfiden_Etc );
    if     ( wdconf  !=  null )
        confidence  = ((Word) wdconf).numericalValue().intValue();

    else   confidence  =  10 ;
     phraseStart  =  refcount ;
     plural  =  (thisWord.getdict( feature_Number ) ==  feature_Plural );
     return   dateAfterWeekday ;
   }
 }


 State prepositionStart() {
    preposition  =  thisWord ;
    phraseStart  =  refcount ;
    return   xAfterPreposition ;
 }


 State detStart() {
   if     ( thisWord  !=  word_That )
       return  startNpAfterDet();

   else   return   null ;
 }


 State startNpAfterDet() {
    determiner  =  thisWord ;
    phraseStart  =  refcount ;
    return   npAfterDet ;
 }


 State startNpAfterFirstname() {
    firstname  =  thisWord ;
    confidence  = computeNameConfidence( thisWord , capcode());
   recordGoodFirstname();
    return   nameAfterFirst ;
 }


 State firstnameStart() {
   startNameSetup();
   startNpAfterFirstname();
   startParallelNpIfNeeded();
    return   nameAfterFirst ;
 }


 State startNameSetup() {
    phraseStart  =  refcount ;
   setNameCapcode(capcode());
    return   null ;
 }


 State startParallelNpIfNeeded() {
   if    ( sp_findPhrasesFlag  &&
          thisWord.isNonnameFormOfCat( categorySet_Det$N$Adj$Adv$V_Etc ))
       return  startParallelNounPhrase();

   else   return   null ;
 }


 State startNpAfterLastname() {
    lastname  = makeSingleton( thisWord );
    confidence  = computeNameConfidence( thisWord , capcode());
   recordGoodLastname();
    return   nameAfterLast ;
 }


 State lastnameStart() {
  /* temporary to use same criteria for last and first names */
   startNameSetup();
   startNpAfterLastname();
   startParallelNpIfNeeded();
    return   nameAfterLast ;
 }


 State singleletterStart() {
   if    (( (capcode() ==  capcode_Lc ) ||
             ucText ) &&
             LexiconUtil.isMembOfArray( thisWord ,  wordSet_A$I ) &&
           ( thisinput  !=  word_Dot ) &&
           ( ! ( LexiconUtil.isMembOfArray( priorWord ,  wordSet_A$The )) ))
      if     ( thisWord  ==  word_A )
         {  determiner  =  thisWord ;
           phraseStart  =  refcount ;
           return   npAfterDet ;
         }

      else   return   null ;

   else  {  initials  =  new Word[] { thisWord };
        setNameCapcode(capcode());
         confidence  =  8 ;
        if     ( thisinput  !=  word_Dot )
           if    (LexiconUtil.isMembOfArray( thisWord ,  wordSet_A$I ))
               confidence  =  4 ;

           else   confidence  =  6 ;
        if    ( sp_findPhrasesFlag  &&
                ( thisWord  ==  word_A ))
           startParallelNounPhrase();
         phraseStart  =  refcount ;
         return   nameAfterFirst ;
       }
 }


 State prefixtitleStart() {
    prefixtitle  =  thisWord ;
    confidence  = defaultOrNameConfidence( thisWord );
   if    ( sp_findPhrasesFlag  &&
          thisWord.hasNonnameCat())
      startParallelNounPhrase();
   setNameCapcode(capcode());
   if     (capcode() ==  capcode_Lc )
       confidence  =  confidence  -  4 ;
   if    (upperCaseClashesWithText() &&
          thisWord.hasNonnameCat())
       confidence  =  confidence  -  5 ;
   if     ( ! (LexiconUtil.isMembOfArray( priorWord ,  wordSet_A$The ) &&
             ( ! ( thisWord.looksLike( category_Name )) )))
      {  phraseStart  =  refcount ;
        return   nameAfterTitle ;
      }

   else   return   null ;
 }


 State dateAfterWeekday() {
   if    (breakButNotComma())
       return  popDate();

   else  if   (checkForMonth())
      {  month  =  thisWord ;
        return   dateAfterMonth ;
      }

   else  if   (wordOKNumForDateDay( thisWord ) ||
          thisWord.isFormOfCat( categorySet_Ord$Integer ))
      {  day  = thisWord.numericalValue().intValue();
        return   dateAfterDay ;
      }

   else  if    ( thisWord  ==  word_The )
      {  determiner  =  thisWord ;
        return   npAfterDet ;
      }

   else  {  confidence  =  9 ;
         return  popDate();
       }
 }


 State dateAfterDay() {
   if    (thisWord.isKnownWord() &&
          wordIsFormOfCat( thisWord ,  categorySet_Month ) &&
          noPrevPunct())
      {  Value  mcw  = thisWord.getdict( feature_Monthconfidence );
        int  mc  =  10 ;
       if     ( mcw  !=  null )
           mc  = ((Word) mcw).numericalValue().intValue();
        month  =  thisWord ;
        confidence  = Math.max( confidence ,  mc );
       if     (capcode() ==  capcode_Lc )
           confidence  =  confidence  -  2 ;
       if    (LexiconUtil.isMembOfArray( month ,  wordSet_May$March$Mar$D_Etc ) &&
               (capcode() ==  capcode_Ic ) &&
               ( ! ( sbreak()) ))
           confidence  =  8 ;
        return   dateAfterMonthDay ;
      }

   else  if   (canBeLowDigitWord() &&
          wordFitsAsLowDayNum())
      {  day  =  day  + thisWord.numericalValue().intValue();
        return   dateAfterDay ;
      }

   else  {  confidence  =  9 ;
         return  popDate();
       }
 }


 boolean canBeLowDigitWord() {
  /* test used in dateAfterDay and dateAfterMonthCardinal */
    return  (thisWord.isFormOfCat( categorySet_Ord$Integer ) &&
            ( ! ( priorWord.numeralp()) ));
 }


 State dateAfterTime() {
   if    (breakButNotComma() &&
           (prevPunct( 1 ) ==  word_Dot ) &&
           LexiconUtil.isMembOfArray( priorWord ,  wordSet_A$M$P$M ) &&
          noPunctOrCommaAfterOnePunct())
      {  Word  tempWord1050  = lex.makeWord( hour );
        return  transmit(( new TimePhrase( preposition ,  tempWord1050 ,  dayTime ,  timezone ,  phraseStart )),  10 );
      }

   else  if   (LexiconUtil.isMembOfArray( thisWord ,  wordSet_Am$Pm$A$M$P$M$N_Etc ) &&
           ( dayTime  ==  null ) &&
           ( timezone  ==  null ))
      {  dayTime  =  thisWord ;
        return   dateAfterTime ;
      }

   else  if   ((LexiconUtil.isMembOfArray( thisWord ,  wordSet_Edt$Est$Pdt$Pst_Etc ) ||
		   LexiconUtil.isMembOfArray( word_Timezone , thisWord.getIioParents())) &&
           ( timezone  ==  null ))
      {  timezone  =  thisWord ;
        return   dateAfterTime ;
      }

   else  {  Word  tempWord1052  = lex.makeWord( hour );
         return  transmit(( new TimePhrase( preposition ,  tempWord1052 ,  dayTime ,  timezone ,  phraseStart )),  10 );
       }
 }


 boolean thousandsMultiplierP( Word  nword ) {
    return  LexiconUtil.isMembOfArray( nword ,  wordSet_Thousand$Millio_Etc );
 }


 boolean hundredsMultiplierP() {
    return  ( ( thisWord  ==  word_Hundred ) &&
            ( firstnum  <  100 ));
 }


 boolean accumulateMultiplierFromSecondnum() {
   if    (thisWord.generalNumberp())
      {  othernum  =  othernum  + thisWord.numericalValue().doubleValue() *  secondnum ;
        secondnum  =  0.0 ;
        return   true ;
      }

   else   return   false ;
 }


 boolean accumulateMultiplier() {
   if    (thisWord.generalNumberp())
      {  othernum  =  othernum  + thisWord.numericalValue().doubleValue() *  firstnum ;
        firstnum  =  0.0 ;
        return   true ;
      }

   else   return   false ;
 }


 boolean accumulateSecondnumP() {
  /*  two predicates  from xAFterCardinalAnd  */
   if    (thisWord.generalNumberp() &&
          numberWord.generalNumberp() &&
          ((  ( ! ( ( numberWord  ==  word_Hundred ) ||
                  thousandsMultiplierP( numberWord ))) &&
              numberWord.isInstanceOf( word_Unit_Of_Countin_Etc )) ||
            ( ((  othernum  %  100  ) ==  0 ) &&
               ((  firstnum  %  100  ) ==  0 ))))
      {  int  ithisword  = thisWord.numericalValue().intValue();
        int  inumberword  = numberWord.numericalValue().intValue();
        return  (( ( firstnum  >  0 ) ||
                  ( othernum  >  0 )) &&
                ( ithisword  >  0 ) &&
                ( ithisword  <  100 ) &&
                ( inumberword  >  0 ) &&
                ((  secondnum  %  10  ) ==  0 ) &&
               ( ( secondnum  ==  0 ) ||
                  ( ithisword  <  10 )));
      }

   else   return   false ;
 }


 boolean accumulateFirstnum() {
   if    (thisWord.generalNumberp())
      {  firstnum  =  othernum  + thisWord.numericalValue().doubleValue() *  firstnum ;
        othernum  =  0.0 ;
        return   true ;
      }

   else   return   false ;
 }

/* prelude */

 State transmitAccumulateNums() {
  /* short call form */
    return  transmitAccumulateNums( null );
 }


 State transmitAccumulateNums( Integer  numend ) {
    firstnum  =  firstnum  +  othernum ;
    othernum  =  0.0 ;
    return  transmitFirstnumNoun( 10 ,  numend );
 }


 State transmitAndClearNums() {
    firstnum  =  firstnum  +  secondnum ;
    secondnum  =  0.0 ;
    othernum  =  0.0 ;
    return  transmitFirstnumNoun( 10 );
 }

/* prelude */

 State transmitFirstnumNoun( int  conf ) {
  /* short call form */
    return  transmitFirstnumNoun( conf ,  null );
 }


 State transmitFirstnumNoun( int  conf ,  Integer  numend ) {
    return  transmitNumberAsNoun( firstnum ,  conf ,  numend );
 }

/* prelude */

 State transmitNumberAsNoun( double  numb ,  int  conf ) {
  /* short call form */
    return  transmitNumberAsNoun( numb ,  conf ,  null );
 }


 State transmitNumberAsNoun( double  numb ,  int  conf ,  Integer  numend ) {
   {  Word  tempWord1053  = lex.makeWord( new Double( numb ));
     Value[]  curphrasepostmods  =  null ;
    if     ( currency  !=  null )
        curphrasepostmods  =  new Value[] {( new PrepPhrase( word_Currency , (NominalPhrase)(createTrivialNounPhrase( currency ))))};
     return  transmit(( new NounPhrase( null ,  null ,  tempWord1053 ,  curphrasepostmods ,  phraseStart )),  conf ,  false ,  numend );
   }
 }


 State xAfterNumber() {
   if    (prevBreak())
      { transmitFirstnumNoun( 9 );
        return  unlessCurrencyTryFirstnumAsYear();
      }

   else  if   (thousandsMultiplierP( thisWord ))
      { accumulateMultiplier();
        return   xAfterCardinal ;
      }

   else  if   (hundredsMultiplierP())
      {  firstnum  =  firstnum  *  100 ;
        return   xAfterCardinal ;
      }

   else  if   (checkForMonth())
      {  month  =  thisWord ;
       if    (numberOKForDateDay( firstnum ,  thisWord ))
           day  = (int) firstnum ;

       else   day  =  sp_unsetNumberValue ;
        return   dateAfterMonthDay ;
      }

   else  if   ( ( dateSeparator  !=  null ) &&
           ( thisinput  ==  dateSeparator ) &&
          wordOKNumForDateDay( thisWord ))
      {  month  = (Value)(getMonthName((int) firstnum ));
        day  = thisWord.numericalValue().intValue();
        expectYear  =  true ;
        return   dateAfterMonthDay ;
      }

   else  if   ( ( currency  ==  null ) &&
          likelyYear( firstnum ))
      { transmitFirstnumNoun( 10 );
        return  useFirstnumAsYear();
      }

   else  if   ( sp_findPhrasesFlag  &&
           ( currency  ==  null ) &&
          thisWord.hasNonnameCat())
      { transmitFirstnumNoun( 10 );
        determiner  = lex.makeWord( new Double( firstnum ));
        firstnum  =  0.0 ;
        return  npAfterDet();
      }

   else  { transmitFirstnumNoun( 10 );
         return   null ;
       }
 }


 State xAfterOrdinal() {
   if    (prevBreak())
       return   null ;

   else  if   (LexiconUtil.isMembOfArray( thisWord ,  wordSet_Day$Week$Month ))
      {  timeunit  =  thisWord ;
        return   xAfterOrdinalTimeUnit ;
      }

   else  if    ( thisWord  ==  word_Of )
       return   xAfterOrdinal ;

   else  if   (checkForMonth())
      {  month  =  thisWord ;
       if    (numberOKForDateDay( firstnum , (Word) month ))
           day  = (int) firstnum ;

       else   day  =  sp_unsetNumberValue ;
        return   dateAfterMonthDay ;
      }

   else  {  modifiers  =  new Value[] { numberWord };
         return  npAfterAdj();
       }
 }


 State xAfterOrdinalTimeUnit() {
   if    (prevBreak())
       return   null ;

   else  if   (LexiconUtil.isMembOfArray( thisWord ,  wordSet_Of$In ))
       return   xAfterOrdinal ;

   else   return   null ;
 }


 State xAfterCardinal() {
   if    (breakButNotComma())
      { transmitAccumulateNums();
        return  unlessCurrencyTryFirstnumAsYear();
      }

   else  if   ( ( othernum  ==  0 ) &&
          checkForMonth() &&
          numberOKForDateDay( firstnum ,  thisWord ))
      {  month  =  thisWord ;
        day  = (int) firstnum ;
        return   dateAfterMonthDay ;
      }

   else  if   (checkForMonth())
      { transmitNumberAsNoun( firstnum  +  othernum ,  10 );
       initializeStateVariables();
        return  monthStart();
      }

   else  if   (LexiconUtil.isMembOfArray( thisWord ,  wordSet_Hundredth$Thous_Etc ) &&
          accumulateFirstnum())
      {  numberWord  = lex.makeOrdinalWord((int) firstnum );
        return   xAfterOrdinal ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Ord ) &&
          numCanAddToMultipleOfTenOrHundred( thisWord ,  firstnum ))
      {  firstnum  =  firstnum  + thisWord.numericalValue().doubleValue();
       if     ( othernum  >  0 )
          {  firstnum  =  firstnum  +  othernum ;
            othernum  =  0.0 ;
          }
        numberWord  = lex.makeOrdinalWord((int) firstnum );
        return   xAfterOrdinal ;
      }

   else  if   (thousandsMultiplierP( thisWord ))
      { accumulateMultiplier();
        return   xAfterCardinal ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Integer ) &&
          thisWordContinuesFirstnum())
       return   xAfterCardinal ;

   else  if   ( ( thisWord  ==  word_Hundred ) ||
          nonNumberUnitOfCounting( thisWord ))
      {  firstnum  =  firstnum  * thisWord.numericalValue().doubleValue();
        return   xAfterCardinal ;
      }

   else  if   ( ( currency  ==  null ) &&
          likelyYear((  firstnum  +  othernum  )))
      { transmitAccumulateNums();
        return  useFirstnumAsYear();
      }

   else  if   ( ( thisWord  ==  word_And ) &&
          (hundredAndNumberTest() ||
            nonNumberUnitOfCounting( priorWord )))
      {  numberWord  =  priorWord ;
        secondnum  =  0.0 ;
        secondnumStart  =  null ;
        firstnumEnd  =  previousEnd ;
        return   xAfterCardinalAnd ;
      }

   else  if   ( sp_findPhrasesFlag  &&
           ( currency  ==  null ))
      { transmitAccumulateNums();
       moveNumsIntoNounPhrase();
        return  npAfterDet();
      }

   else  { transmitNumberAsNoun( firstnum  +  othernum ,  10 );
         return   null ;
       }
 }


 boolean hundredAndNumberTest() {
  /* predicate used in xAfterCardinal */
    return  ( ((  firstnum  %  100  ) ==  0 ) &&
            ((  othernum  %  100  ) ==  0 ) &&
           ( ( firstnum  >=  100 ) ||
              ( othernum  >=  100 )));
 }


 void moveNumsIntoNounPhrase() {
  /* used to wrap up xAfterCardinal and xAfterCardinalAnd */
    Word  numword  = lex.makeWord( new Double( firstnum ));
   if     ( determiner  !=  null )
       modifiers  = nodeSequencePushNode( modifiers ,  numword );

   else   determiner  =  numword ;
    firstnum  =  0.0 ;
    othernum  =  0.0 ;
 }


 boolean nonNumberUnitOfCounting( Word  nword ) {
  /* true of words like gross, dozen, score but not hundred.
     test used in xAfterCardinal  */
    return  (nword.generalNumberp() &&
            ( ! ( thisWord.isFormOfCat( categorySet_Number )) ) &&
            ( ! (thousandsMultiplierP( nword ) ||
                ( nword  ==  word_Hundred ))) &&
           nword.isInstanceOf( word_Unit_Of_Countin_Etc ));
 }


 boolean thisWordContinuesFirstnum() {
   {  Number  temp  = thisWordContinuesNumber( priorWord ,  firstnum );
    if     ( temp  !=  null )
       {  firstnum  =  temp.doubleValue() ;
         return   true ;
       }

    else   return   false ;
   }
 }


 boolean thisWordContinuesSecondnum() {
   {  Number  temp  = thisWordContinuesNumber( numberWord ,  secondnum );
    if     ( temp  !=  null )
       {  secondnum  =  temp.doubleValue() ;
         return   true ;
       }

    else   return   false ;
   }
 }


 Number thisWordContinuesNumber( Word  previousnumberword ,  double  valsofar ) {
  /* returns null if not a continuation, else new value */
   {  Number  vthis  = thisWord.numericalValue();
     Number  vprev  = previousnumberword.numericalValue();
    if    ( ( vthis  !=  null ) &&
            ( vprev  !=  null ))
        return  theseNumsContinueNumber(vthis.doubleValue(), vprev.doubleValue(),  valsofar );

    else   return   null ;
   }
 }


 Number theseNumsContinueNumber( double  nthis ,  double  nprev ,  double  valsofar ) {
   if    ( ( nthis  >  0 ) &&
           ( nprev  !=  0 ) &&
          ((  ( nthis  <  10 ) &&
               ((  valsofar  %  10  ) ==  0 )) ||
            ( ( nthis  <  100 ) &&
               ((  valsofar  %  100  ) ==  0 ))))
       return   new Double((  valsofar  +  nthis  ));

   else  if    ( nthis  <  10 )
       return   new Double(( (  valsofar  *  10  ) +  nthis  ));

   else  if    ( nthis  <  100 )
       return   new Double(( (  valsofar  *  100  ) +  nthis  ));

   else   return   null ;
 }


 State xAfterCardinalAnd() {
  /*  new for 2000....  */
   if     ( secondnumStart  ==  null )
       secondnumStart  =  refcount ;
   if    (prevBreak())
      { transmitAndClearNums();
       if    ( ( currency  ==  null ) &&
              likelyYear( firstnum ))
          { useFirstnumAsYear( firstnumEnd );
            firstnumEnd  =  null ;
            secondnumStart  =  null ;
          }
        return   null ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Integer ) &&
          accumulateSecondnumP())
      {  secondnum  =  secondnum  + thisWord.numericalValue().doubleValue();
        numberWord  =  thisWord ;
        return   xAfterCardinalAnd ;
      }

   else  {  boolean  tempboolean1056 ;
        if    (thousandsMultiplierP( thisWord ) &&
               thisWord.generalNumberp())
           {  double  nthisword  = thisWord.numericalValue().doubleValue();
            if    ( ( othernum  ==  0 ) ||
                    ( !  ( nthisword  >  othernum )))
               {  othernum  =  othernum  +  nthisword  *  firstnum  +  secondnum ;
                 tempboolean1056  =  true ;
               }

            else   tempboolean1056  =  false ;
           }

        else   tempboolean1056  =  false ;
        if    ( tempboolean1056 )
           {  firstnum  =  0.0 ;
             secondnum  =  0.0 ;
             return   xAfterCardinal ;
           }

        else  if   ( ( thisWord  ==  word_Hundred ) ||
               thousandsMultiplierP( thisWord ))
           {  secondnum  = thisWord.numericalValue().doubleValue() *  secondnum ;
            transmitAccumulateNums( firstnumEnd );
            unlessCurrencyTryFirstnumAsYear();
             phraseStart  =  secondnumStart ;
            {  firstnumEnd  =  null ;
              secondnumStart  =  null ;
            }
             firstnum  =  0.0 ;
            if     ( thisWord  ==  word_Hundred )
                firstnum  =  secondnum ;

            else   othernum  =  secondnum ;
             secondnum  =  0.0 ;
             return   xAfterCardinal ;
           }

        else  if   ( sp_findPhrasesFlag  &&
               thisWord.hasNonnameCat() &&
                ( currency  ==  null ))
           { transmitAndClearNums();
            moveNumsIntoNounPhrase();
             return  npAfterDet();
           }

        else  { transmitAndClearNums();
             unlessCurrencyTryFirstnumAsYear();
              return   null ;
            }
       }
 }


 State unlessCurrencyTryFirstnumAsYear() {
   if    ( ( currency  ==  null ) &&
          likelyYear( firstnum ))
       return  useFirstnumAsYear();

   else   return   null ;
 }

/* prelude */

 State useFirstnumAsYear() {
  /* short call form */
    return  useFirstnumAsYear( null );
 }


 State useFirstnumAsYear( Integer  endnum ) {
  /* action used in xAfterCardinalxAfterCardinalAnd xAFterOrdinal 
    xAfterNumber.  endNum is a position or null. */
    confidence  =  9 ;
    year  = (int) firstnum ;
    firstnum  =  0.0 ;
    return  popDate( endnum );
 }


 State useThisWordAsYear() {
  /* action from dateAfterMonth */
    year  = thisWord.numericalValue().intValue();
    return   popDate ;
 }


 int computeYearConfidence() {
  /* action from dateAfterMonth */
   { if   (thisWord.generalNumberp() &&
           likelyYear(thisWord.numericalValue().doubleValue()) &&
            ( ! (LexiconUtil.isMembOfArray( month ,  wordSet_March$May ) &&
                ( confidence  <  7 ))))
        confidence  =  10 ;

    else   confidence  = Math.min( confidence ,  8 );
     return   confidence ;
   }
 }


 State dateAfterMonth() {
   if    (prevBreak())
       return  popDate();

   else  if   (thisWord.numeralp() &&
          thisWord.integerWordp())
      if    ( expectYear )
         {  confidence  =  10 ;
           return  useThisWordAsYear();
         }

      else  if   (wordOKNumForDateDay( thisWord , (Word) month ))
         {  day  = thisWord.numericalValue().intValue();
           confidence  =  9 ;
           return   dateAfterMonthDay ;
         }

      else  { computeYearConfidence();
            return  useThisWordAsYear();
          }

   else  if    ( thisWord  ==  word_The )
       return   dateAfterMonth ;

   else  if   (LexiconUtil.isMembOfArray( thisWord ,  wordSet_In$Of ))
      {  day  =  sp_unsetNumberValue ;
        expectYear  =  true ;
        return   dateAfterMonthDay ;
      }

   else  if   (thisWord.generalNumberp() &&
          ((canBeRomanNumeral( thisWord ) &&
              wordOKNumForDateDay( thisWord , (Word) month )) ||
            thisWord.isFormOfCat( categorySet_Ord )))
      {  day  = thisWord.numericalValue().intValue();
        return   dateAfterMonthDay ;
      }

   else  if   (thisWord.generalNumberp() &&
          canBeRomanNumeral( thisWord ))
      {  year  = thisWord.numericalValue().intValue();
       computeYearConfidence();
       cancelNounState();
        return   popDate ;
      }

   else  if   (thisWord.generalNumberp())
      {  day  = thisWord.numericalValue().intValue();
        numberWord  =  thisWord ;
        return   dateAfterMonthCardinal ;
      }

   else  if    ( confidence  >  confidenceThreshold )
       return  popDate();

   else   return   null ;
 }


 State dateAfterMonthCardinal() {
  /* this state assumes the cardinal is the initial part of day number */
   if    (breakButNotComma())
       return  popDate();

   else  if   (plausibleYear( thisWord ))
      {  confidence  =  10 ;
        return  useThisWordAsYear();
      }

   else  if   (noPrevPunct() &&
           ( thisWord  ==  word_Of ))
       return   dateAfterMonthDay ;

   else  if   (canBeLowDigitWord() &&
          noPrevPunct() &&
          wordFitsAsLowDayNum())
      {  day  =  day  + thisWord.numericalValue().intValue();
        return   dateAfterMonthDay ;
      }

   else  if   (naturalCardinal() &&
          noPunctOrComma())
      {  secondnum  = thisWord.numericalValue().doubleValue();
       if     ( secondnumStart  ==  null )
          {  secondnumStart  =  refcount ;
            firstnumEnd  =  previousEnd ;
          }
        numberWord  =  thisWord ;
        return   dateAfterMonthDayCardinal ;
      }

   else   return  popDate();
 }

/* prelude */

 boolean wordFitsAsLowDayNum() {
  /* short call form */
    return  wordFitsAsLowDayNum( 31 );
 }


 boolean wordFitsAsLowDayNum( int  maxdays ) {
  /*  test from dateAfterMonthCardinal and dateAfterDay */
   if    (thisWord.generalNumberp())
      {  int  tempint  = thisWord.numericalValue().intValue();
        return  ( ( day  !=  sp_unsetNumberValue ) &&
                ( tempint  >  0 ) &&
                ( tempint  <  10 ) &&
                ( day  >  10 ) &&
                ((  day  %  10  ) ==  0 ) &&
                ( !  ((  day  +  tempint  ) >  maxdays )));
      }

   else   return   false ;
 }


 boolean naturalCardinal() {
  /*  test from dateAfterMonthCardinal and dateAfterMonthDay  */
    return  (wordClass.equals( category_Cardinal ) &&
            (thisWord.numericalValue().intValue() >  0 ));
 }


 boolean breakButNotComma() {
    return  breakButNotMemb( wordSet_Charset_Comma );
 }


 boolean breakButNotInitialsPeriod() {
    return  (prevBreak() &&
            ( ! ( ( ! ( sbreak()) ) &&
               breakJustMemb( wordSet_Charset_Dot ) &&
                ( initials  !=  null ))));
 }


 boolean breakButNotMemb( Word[]  chars ) {
    return  (prevBreak() &&
            ( ! ( ( ! ( sbreak()) ) &&
               breakJustMemb( chars ))));
 }


 boolean noPunctOrMemb( Word[]  chars ) {
  /* either no punctuation or else only one of the
  specified chars */
    return  (noPrevPunct() ||
           breakJustMemb( chars ));
 }


 boolean breakJustMemb( Word[]  chars ) {
    return  (LexiconUtil.isMembOfArray(prevPunct( 1 ),  chars ) &&
           noPrevPunctAfter( 1 ));
 }


 boolean noPunctOrComma() {
  /* Either there is no punctuation, or only a comma */
    return  noPunctOrMemb( wordSet_Charset_Comma );
 }


 boolean noPunctOrCommaAfterOnePunct() {
  /* After a first punctuation mark, either there is
  no punctuation, or there is only a comma */
    return  (noPrevPunctAfter( 1 ) ||
           ( (prevPunct( 2 ) ==  word_Comma ) &&
             noPrevPunctAfter( 2 )));
 }


 State dateAfterMonthDay() {
   if    (breakButNotMemb( wordSet_Charset_Colonco_Etc ))
       return  popDate();

   else  if   (thisWord.numeralp() &&
           ( thisinput  ==  word_Colon ) &&
           ( thisWord.getWordString().length()  ==  2 ))
      {  hour  = thisWord.numericalValue().intValue();
        return   dateAfterMonthDayHour ;
      }

   else  if   (thisWord.numeralp())
      { if   (plausibleYear( thisWord ))
           confidence  =  10 ;

       else   confidence  =  5 ;
        return  useThisWordAsYear();
      }

   else  if   (noPrevPunct() &&
		   LexiconUtil.isMembOfArray( thisWord ,  wordSet_In$Of ))
       return   dateAfterMonthDay ;

   else  if   (naturalCardinal())
      { secondnumSetup();
        return   dateAfterMonthDayCardinal ;
      }

   else   return  popDate();
 }


 void secondnumSetup() {
  /* like generalNumberSetup but for secondnum */
    secondnum  = thisWord.numericalValue().doubleValue();
    secondnumStart  =  refcount ;
    firstnumEnd  =  previousEnd ;
    numberWord  =  thisWord ;
 }

/* prelude */

 State transmitTimeAndDate( int  hour ) {
  /* short call form */
    return  transmitTimeAndDate( hour ,  0 ,  sp_unsetNumberValue ,  false );
 }


 State transmitTimeAndDate( int  hour ,  int  minutes ) {
  /* short call form */
    return  transmitTimeAndDate( hour ,  minutes ,  sp_unsetNumberValue ,  false );
 }


 State transmitTimeAndDate( int  hour ,  int  minutes ,  int  seconds ) {
  /* short call form */
    return  transmitTimeAndDate( hour ,  minutes ,  seconds ,  false );
 }


 State transmitTimeAndDate( int  hour ,  int  minutes ,  int  seconds ,  boolean  usedword ) {
  /* used in dateAfterMonthDayHour, ...Minutes, and ...MinutesSeconds  */
   {  Word  htime ;
    if    (intContainsValue( seconds ))
        htime  = packLexTime( hour ,  minutes ,  seconds );

    else   htime  = packLexTime( hour ,  minutes );
    transmit(( new TimePhrase( preposition ,  htime ,  dayTime ,  timezone ,  phraseStart )),  10 );
    printScantraceAndClear();
    transmit(( new DatePhrase( weekday ,  month ,  day ,  year ,  phraseStart )),  confidence ,  usedword );
     return   null ;
   }
 }


 State dateAfterMonthDayHour() {
   if    (breakButNotMemb( wordSet_Charset_Colonco_Etc ))
       return  transmitTimeAndDate( hour );

   else  if   (thisWord.numeralp() &&
           ( thisWord.getWordString().length()  ==  2 ))
      {  minutes  = thisWord.numericalValue().intValue();
       if     ( thisinput  ==  word_Colon )
           expectSeconds  =  true ;
        return   dateAfterMonthDayHourMinutes ;
      }

   else   return  transmitTimeAndDate( hour );
 }


 State dateAfterMonthDayHourMinutes() {
   if    (breakButNotMemb( wordSet_Charset_Colonco_Etc ))
       return  transmitTimeAndDate( hour ,  minutes );

   else  if   (thisWord.numeralp() &&
           expectSeconds  &&
           ( thisWord.getWordString().length()  ==  2 ) &&
          noPunctOrMemb( wordSet_Charset_Colon ))
      {  seconds  = thisWord.numericalValue().intValue();
        return   dateAfterMonthDayHourMinutesSeconds ;
      }

   else  if   (plausibleYear( thisWord ) &&
          noPunctOrComma())
      { thisWordIsYear();
        return  transmitTimeAndDate( hour ,  minutes ,  sp_unsetNumberValue ,  true );
      }

   else  if   (noPrevPunct() &&
           ( thisWord  ==  word_Of ))
       return   dateAfterMonthDayHourMinutes ;

   else  if   (naturalCardinal())
      { secondnumSetup();
        return   dateAfterMonthDayCardinal ;
      }

   else   return  transmitTimeAndDate( hour ,  minutes );
 }


 void thisWordIsYear() {
  /* used to penalize 2 digit years ... but dropped it */
    year  = thisWord.numericalValue().intValue();
 }


 void printScantraceAndClear() {
   if    ( scantrace )
      printScantrace( transmitbuffer );
    transmitbuffer  =  null ;
 }


 State dateAfterMonthDayHourMinutesSeconds() {
   if    (breakButNotComma())
       return  transmitTimeAndDate( hour ,  minutes ,  seconds );

   else  if   (plausibleYear( thisWord ))
      { thisWordIsYear();
        return  transmitTimeAndDate( hour ,  minutes ,  seconds ,  true );
      }

   else  if   (noPrevPunct() &&
           ( thisWord  ==  word_Of ))
       return   dateAfterMonthDayHourMinutesSeconds ;

   else  if   (naturalCardinal())
      { secondnumSetup();
        return   dateAfterMonthDayCardinal ;
      }

   else   return  transmitTimeAndDate( hour ,  minutes ,  seconds );
 }


 State dateAfterMonthDayCardinal() {
   if    (prevBreak())
      {  year  = (int)( secondnum  +  othernum );
        secondnum  =  0.0 ;
        othernum  =  0.0 ;
        return  popDate();
      }

   else  if   (thousandsMultiplierP( thisWord ))
      { accumulateMultiplierFromSecondnum();
        return   dateAfterMonthDayCardinal ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Integer ) &&
          thisWordContinuesSecondnum())
       return   dateAfterMonthDayCardinal ;

   else  if   (thisWord.isFormOfCat( categorySet_Integer ) &&
          accumulateSecondnumP())
      {  secondnum  =  secondnum  + thisWord.numericalValue().doubleValue();
        numberWord  =  thisWord ;
        return   dateAfterMonthDayCardinal ;
      }

   else  if    ( thisWord  ==  word_Hundred )
      {  secondnum  =  secondnum  *  100 ;
        return   dateAfterMonthDayCardinal ;
      }

   else  if   ( ( thisWord  ==  word_And ) &&
           ((  secondnum  %  100  ) ==  0 ))
       return   dateAfterMonthDayCardinal ;

   else  if   (thisWord.isFormOfCat( categorySet_Ord ) &&
           ( ! ( nIL_Priorword.equals( nIL_And )) ) &&
          thisWordContinuesSecondnum())
      { popDate( firstnumEnd );
       clearDate();
        firstnum  =  secondnum  +  othernum ;
        secondnum  =  0.0 ;
        othernum  =  0.0 ;
        numberWord  = lex.makeOrdinalWord((int) firstnum );
        phraseStart  =  secondnumStart ;
       {  firstnumEnd  =  null ;
         secondnumStart  =  null ;
       }
        return   xAfterOrdinal ;
      }

   else  {  year  = (int)( secondnum  +  othernum );
         secondnum  =  0.0 ;
         othernum  =  0.0 ;
         return  popDate();
       }
 }


 void clearDate() {
    weekday  =  null ;
    month  =  null ;
    day  =  sp_unsetNumberValue ;
    year  =  sp_unsetNumberValue ;
 }

/* prelude */

 State popDate() {
  /* short call form */
    return  popDate( null );
 }


 State popDate( Integer  phraseend ) {
  /*  this state allows carrying a date phrase long enough to find out whether it
   is part of a possessive phrase. */
   if    ( ( thisWord  ==  word_$S ) &&
           ( nounState  ==  null ))
      { /* ("*** for future: if verb is set to last, or noun-state
                 has adj next, add that to date") */
       transmit(( new DatePhrase( weekday ,  month ,  day ,  year ,  phraseStart )),  confidence ,  false ,  phraseend );
        determiner  = ( new PossPhrase((Phrase)( new DatePhrase( weekday ,  month ,  day ,  year ,  phraseStart ))));
       clearDate();
        return   npAfterDet ;
      }

   else  { /* ("*** for future: if verb is set to last, or noun-state
                     has adj next, add that to date") */
        transmit(( new DatePhrase( weekday ,  month ,  day ,  year ,  phraseStart )),  confidence ,  false ,  phraseend );
         return   null ;
       }
 }


 boolean lowerCaseClashesWithName() {
    return  ( (capcode() ==  capcode_Lc ) &&
            ( !  lcName ));
 }


 boolean capcodeClashesWithLcName() {
    return  ( lcName  &&
            (capcode() !=  capcode_Lc ));
 }


 boolean upperCaseClashesWithName() {
    return  ( (capcode() ==  capcode_Uc ) &&
            ( !  ucName ));
 }


 boolean capcodeClashesWithUcName() {
    return  ( ucName  &&
            (capcode() !=  capcode_Uc ));
 }


 boolean caseClashesWithName() {
    return  (lowerCaseClashesWithName() ||
           upperCaseClashesWithName());
 }


 boolean caseConsistentWithName() {
    return  ( lcName  ||
            (capcode() !=  capcode_Lc ));
 }


 boolean lowerCaseCodeAndName() {
    return  ( lcName  &&
            (capcode() ==  capcode_Lc ));
 }


 boolean upperCaseCodeAndName() {
    return  ( ucName  &&
            (capcode() ==  capcode_Uc ));
 }


 boolean nameIsOddCase() {
    return  (upperCaseCodeAndName() ||
           lowerCaseCodeAndName());
 }


 boolean upperCaseClashesWithText() {
    return  ( (capcode() ==  capcode_Uc ) &&
            ( !  ucText ));
 }


 boolean upperCaseMatchesText() {
    return  ( (capcode() ==  capcode_Uc ) &&
            ucText );
 }


 boolean betterNotCancelNoun() {
  /* either we can't give up noun or else name is too unlikely right now */
    return  ( ( !  sp_namesSuppressNounsFlag ) ||
            ( confidence  <  confidenceThreshold ) ||
            ( determiner  !=  null ) ||
           canBePluralOrMass( thisWord ) ||
            ( ! ( phraseStart.equals( altPhraseStart )) ));
 }


 boolean namePartIsOddCaseAndOrdinaryWord( Word  namepart ) {
  /* test called in nameAfterFirst (firstname) and nameAfterLast (singleton lastname)
  returns true if the collected name looks like an ordinary phrase ;e.g., Small Price */
    return  (( lcName  ||
              ucName ) &&
            ( initials  ==  null ) &&
            ( prefixtitle  ==  null ) &&
            ( namesuffix  ==  null ) &&
            ( ! ( namepart.looksLike( category_Name )) ) &&
           looksLikeOrdinaryWord( namepart ) &&
           namepart.hasNonnameCat() &&
           (prevBreak() ||
             ((  ( namepart  ==  firstname ) ||
                 namepart.getdict(feature_Wordclass).equals( value_Dualname )) &&
                ( ! (thisWord.looksLike( category_Name ) ||
                   (wordClass.equals( category_Singleletter ) &&
                      ( thisinput  ==  word_Dot )))) &&
               looksLikeOrdinaryWord( thisWord ) &&
               thisWord.hasNonnameCat())));
 }


 boolean firstnameAndThisWordArePenaltyNames() {
  /* test from nameAfterFirst  */
    return  ( ( firstname  !=  null ) &&
           ( ucText  ||
             lowerCaseCodeAndName()) &&
            ( prefixtitle  ==  null ) &&
           noPrevPunct() &&
           firstname.isPenaltyFormOfCat( categorySet_Firstname ) &&
           thisWord.isPenaltyFormOfCat( categorySet_Lastname ) &&
           looksLikeOrdinaryWord( firstname ) &&
           looksLikeOrdinaryWord( thisWord ) &&
           firstname.hasNonnameCat() &&
           thisWord.hasNonnameCat());
 }


 boolean punctuationOrCaseClashes() {
  /* test for nameAfterFirst */
    Word  tempWord1059  = singleton( initials );
    return  (breakButNotInitialsPeriod() ||
           unambiguousPriorPunct( tempWord1059 ) ||
           capcodeClashesWithLcName() ||
           capcodeClashesWithUcName() ||
           (lowerCaseClashesWithName() &&
             thisWord.hasNonnameCat()));
 }


 boolean unambiguousPriorPunct( Word  previoustoken ) {
  /* we have punctuation and it is not part of an abbreviation */
    return  ( ( ! ( noPrevPunct()) ) &&
            ( ! ( punctCanBeAbbrevPunct( previoustoken )) ));
 }


 void moveNamePartsIntoDeterminer() {
  /* action from nameAfterFirst and nameAfterLast */
    determiner  = (Value)( new PossPhrase((Phrase)( new NamePhrase( prefixtitle ,  firstname ,  initials ,  lastname ,  namesuffix ))));
   cancelNameCodeHypothesis();
   clearNameParts();
 }


 boolean haveFirstnameOrGoodPrefixtitle() {
  /* don't want to guess a name if only have initials or ambig title  */
    return  ( ( firstname  !=  null ) ||
           ( ( prefixtitle  !=  null ) &&
              ( ! ( prefixtitle.hasNonnameCat()) )));
 }


 boolean weaknames() {
  /*  weakNames() tests if
  the indicated firstname and lastname with specified case
  information is too poor to consider a name. */
   if     ( lastname  ==  null )
       return  weakFirstName();

   else  if   (multiWordp( lastname ))
      {  boolean  tempboolean1060 ;
        tempboolean1060  =  false ;
       if     ( lastname  !=  null )
          for ( int  i  =  0 ;  i  <  lastname.length ;  i++ ) {
              Word  ln  =  lastname[i] ;
             { if    (weakLastName( ln ))
                  {  tempboolean1060  =  true ;
                    break ;
                  }
             }
          }
        return   tempboolean1060 ;
      }

   else  {  Word  tempWord1061  = singleton( lastname );
         return  weakLastName( tempWord1061 );
       }
 }


 boolean weakLastName( Word  lastnm ) {
  /* is-weak-name (lastnm) tests if
  the indicated firstname and lastname with specified case
  information is too poor to consider a name. */
    return  (( lcName  ||
              ucText ) &&
           lastnm.isNonnameFormOfCat( categorySet_N ) &&
           lastnm.looksLike( category_N$V$Adj$Adv ) &&
           weakFirstName());
 }


 boolean weakFirstName() {
    return  ( ( firstname  ==  null ) ||
           ( ( ! ( firstname.looksLike( category_Name )) ) &&
             firstname.hasNonnameCat()));
 }


 boolean haveNamePartsAndWordCouldBeLastname() {
  /* test used as we run out of other choices for a word in nameAfterFirst */
    return  (haveFirstnameOrGoodPrefixtitle() &&
           thisWord.syllabic() &&
            ( lastname  ==  null ) &&
            ( determiner  ==  null ) &&
            ( ! (thisWord.isNonnameFormOfCat( categorySet_V$Adv ) &&
               ( (capcode() ==  capcode_Lc ) ||
                 ( ucName  &&
                    (capcode() ==  capcode_Uc ))))) &&
            ( !  ( confidence  <  confidenceThreshold )) &&
            ( ! ( LexiconUtil.isMembOfArray( thisWord ,  sp_verbsUnlikelyAsLastnames )) ) &&
            ( ! ( thisWord.isFormOfCat( categorySet_Det$Prep$Pro$Nu_Etc )) ) &&
            ( ! ( caseClashesWithName()) ) &&
            ( ! (LexiconUtil.isMembOfArray(capcode(), (Value[]) featureSet_Lc$Uc ) &&
               looksLikeOrdinaryWord( thisWord ) &&
               thisWord.hasNonnameCat())) &&
            ( ! (LexiconUtil.isMembOfArray( firstname ,  sp_extremelyUnlikelyNames ))) &&
            ( ! ( ( nounState  !=  null ) &&
               ( ( determiner  !=  null ) ||
                  ( adverbs  !=  null ) ||
                  ( modifiers  !=  null )))) &&
            ( ! ( ( firstname  ==  word_My ) &&
               thisWord.isFormOfCat( categorySet_N$Adj$Adv ))));
 }


 State nameAfterFirst() {
   if    ( ( firstname  !=  null ) &&
           ( initials  ==  null ))
      {  ucName  = ( ucName  ||
          ( priorCapcode  ==  capcode_Uc ));
        lcName  = ( lcName  ||
          ( priorCapcode  ==  capcode_Lc ));
      }
   if     ( thisWord  ==  word_$S )
      { if   (goodInitialCapFirstname() ||
              ((  ( firstname  !=  null ) ||
                   ( lastname  !=  null )) &&
                nameHypothesisIsWorthKeeping() &&
                 ( ! ( weaknames()) )))
          { recordGoodFirstname();
           recordGoodLastname();
           if     ( prefixtitle  !=  null )
              transmitName();
           if     ( ! ( betterNotCancelNoun()) )
              cancelNounState();
          }
        return  makeNamePartsIntoDeterminerAndFollowNoun();
      }

   else  if   ( ( lastname  ==  null ) &&
          namePartIsOddCaseAndOrdinaryWord( firstname ))
       return  abandonNameForNounPhrase();

   else  if   (firstnameAndThisWordArePenaltyNames())
       return  abandonNameForNounPhrase();

   else  if   (punctuationOrCaseClashes())
      { if   ( ( firstname  !=  null ) &&
              ( ( namesuffix  !=  null ) ||
                firstname.looksLike( category_Name )) &&
               ( ! ( lcName  &&
                  firstname.hasNonnameCat())))
          transmitLikelyNameParts();
       if    (nounHypothesisIsBetterThanName())
           return  abandonNameForNounPhrase();

       else   return   null ;
      }

   else  if   (nameIsOddCase() &&
           ( ! ( ( firstname  !=  null ) &&
              noPrevPunct() &&
              (firstname.looksLike( category_Name ) ||
                 ( ! ( firstname.hasNonnameCat()) )))) &&
          looksLikeOrdinaryWord( thisWord ) &&
          thisWord.isPenaltyFormOfCat( categorySet_Lastname ) &&
          thisWord.hasNonnameCat() &&
           ( ! ( ( firstname  ==  word_My ) &&
              thisWord.isFormOfCat( categorySet_N$Adj$Adv ))))
      { if   (( ( firstname  !=  null ) ||
                 ( lastname  !=  null ) ||
                ( ( prefixtitle  !=  null ) &&
                   ( initials  !=  null ))) &&
              ( ( namesuffix  !=  null ) ||
                 ( lastname  !=  null ) ||
                ( ( firstname  !=  null ) &&
                  firstname.looksLike( category_Name ))) &&
               ( ! ( ( firstname  !=  null ) &&
                   lcName  &&
                  firstname.hasNonnameCat())))
          transmitLikelyNameParts();
        return  abandonNameForNounPhrase();
      }

   else  if   (thisWord.isFormOfCat( categorySet_Lastname ) &&
           ( ! ( caseClashesWithName()) ) &&
           ( ! (LexiconUtil.isMembOfArray( firstname ,  wordSet_See$So$Soon ) &&
              ( lcName  ||
                 ucName  ||
                 ucText  ||
                 (capcode() !=  capcode_Ic )) &&
              thisWord.hasNonnameCat())))
      {  int  defconf  = defaultOrNameConfidence( thisWord );
        int  clashfee  =  0 ;
       if    (caseClashesWithName())
           clashfee  =  4 ;

       else  { if   (upperCaseMatchesText())
             clashfee  =  3 ;
       }
        confidence  = Math.max( confidence ,  defconf  -  clashfee );
        lastname  = (Word[])(nodeSequenceAddNode( lastname ,  thisWord ));
       followParallelNounPhrase();
        return   nameAfterLast ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Nameprefix ) &&
           ( ! ( caseClashesWithName()) ))
      {  lastname  = (Word[])(nodeSequenceAddNode( lastname ,  thisWord ));
       if    (upperCaseClashesWithText() &&
              thisWord.hasNonnameCat())
          { if     ( !  ucName )
                confidence  =  confidence  -  5 ;
          }
       followParallelNounPhrase();
        return   nameAfterFirst ;
      }

   else  if   (wordClass.equals( category_Singleletter ) &&
           ( thisinput  ==  word_Dot ) &&
          ( lcName  ||
        		  LexiconUtil.isMembOfArray(capcode(), (Value[]) featureSet_Ic$Uc )))
      {  initials  = (Word[])(nodeSequenceAddNode( initials ,  thisWord ));
       followParallelNounPhrase();
        return   nameAfterFirst ;
      }

   else  if   (goodNamesuffix())
       return  acceptNamesuffix();

   else  if   (haveNamePartsAndWordCouldBeLastname())
      {  confidence  = Math.min( confidence ,  6 );
        lastname  = (Word[])(nodeSequenceAddNode( lastname ,  thisWord ));
       guessName( thisWord );
       followParallelNounPhrase();
        return   nameAfterLast ;
      }

   else  if   ( ( firstname  !=  null ) &&
          ( ( namesuffix  !=  null ) ||
             ( prefixtitle  !=  null ) ||
             ( ! ( thisWord.isFormOfCat( categorySet_Det$N$Adj$Adv )) ) ||
            firstname.looksLike( category_Name )) &&
           ( ! ( lcName  &&
              firstname.hasNonnameCat())))
      { transmitLikelyNameParts();
        return  abandonNameForNounPhrase();
      }

   else  if    ( nounState  !=  null )
       return  abandonNameForNounPhrase();

   else   return   null ;
 }


 boolean haveGoodNameParts() {
  /* a test portion from nameAfterLast */
   {  Word  singlelastname  = singleton( lastname );
     return  (( ( firstname  !=  null ) ||
               ( initials  !=  null ) ||
               ( prefixtitle  !=  null ) ||
               ( namesuffix  !=  null ) ||
               ( ! ( ( singlelastname  !=  null ) &&
                  singlelastname.hasNonnameCat()))) &&
             ( ! ( nounHypothesisIsBetterThanName()) ) &&
             ( ! ( weaknames()) ));
   }
 }


 State transmitNamePartsThenRunNounHypothesis() {
  /* action from nameAfterLast */
   if    (haveGoodNameParts())
      { transmitLikelyNameParts();
       if     ( ! ( betterNotCancelNoun()) )
          cancelNounStateWithoutClearing();
      }
   if     ( nounState  !=  null )
       return  abandonNameForNounPhrase();

   else  if   (( ( determiner  !=  null ) ||
             ( modifiers  !=  null )) &&
          thisWord.isNonnameFormOfCat( categorySet_N ) &&
           ( ! ( thisWord.isPenaltyFormOfCat( categorySet_N ,  2 )) ))
      { if   ( ( firstname  !=  null ) &&
               (nodeSequenceFirstWord( modifiers ) ==  firstname ))
           modifiers  = nodeSequenceRest( modifiers );
        modifiers  = nodeSequencePushNode( modifiers , (Value)( new NamePhrase( prefixtitle ,  firstname ,  initials ,  lastname ,  namesuffix )));
        phraseStart  =  altPhraseStart ;
        noun  =  thisWord ;
       clearNameParts();
        return   npAfterNoun ;
      }

   else   return   null ;
 }


 boolean goodInitialCapFirstname() {
  /* a test used by nameAfterFirst and nameAfterLast when an apostropheS arrives */
    return  ( ( priorCapcode  ==  capcode_Ic ) &&
            ( !  icText ) &&
            ( ! ( ( nounState  !=  null ) &&
                ( firstname  ==  priorWord ) &&
               priorWord.isNonnameFormOfCat( categorySet_N$Adj$Adv ) &&
               LexiconUtil.isInList( feature_Ic , (List)(priorWord.getCapcodes())))));
 }


 boolean lastnameCanCombineWithThis() {
  /* test used in nameAfterLast */
    return  ( ( lastname  !=  null ) &&
           thisWord.isFormOfCat( categorySet_Lastname ) &&
           noPrevPunct() &&
            ( !  ( confidence  <  confidenceThreshold )) &&
            ( ! (caseClashesWithName() &&
               thisWord.hasNonnameCat())));
 }


 State moveLastnameToFirstnameAndSetNewLastname() {
  /* an action from nameAfterLast */
   {  int  clashfee ;
     int  defconf  = defaultOrNameConfidence( thisWord );
    if    (caseClashesWithName())
        clashfee  =  4 ;

    else   clashfee  =  0 ;
     firstname  = singleton( lastname );
     lastname  = makeSingleton( thisWord );
     confidence  = Math.max( confidence ,  defconf  -  clashfee );
    if    (upperCaseMatchesText() &&
           looksLikeOrdinaryWord( thisWord ) &&
           thisWord.hasNonnameCat())
       { if     ( ! ( thisWord.looksLike( category_Name )) )
             confidence  =  confidence  -  5 ;
       }
    followParallelNounPhrase();
     return   nameAfterLast ;
   }
 }


 State makeNamePartsIntoDeterminerAndFollowNoun() {
  /* an action used after finding apostropheS in nameAfterFirst and nameAfterLast */
   if     ( nounState  !=  null )
      { cancelNameCodeHypothesis();
       clearNameParts();
        return   null ;
      }

   else  { clearNounStateVariables();
        moveNamePartsIntoDeterminer();
         return   npAfterDet ;
       }
 }


 State acceptAdditionalLastname() {
  /* action from nameAfterLast */
    confidence  = Math.max( confidence , defaultOrNameConfidence( thisWord ));
   if    (caseConsistentWithName())
      { /* empty statement here */ }

   else   confidence  =  confidence  -  4 ;
   if    (upperCaseClashesWithText() &&
          looksLikeOrdinaryWord( thisWord ) &&
          thisWord.hasNonnameCat())
      { if     ( !  ucName )
            confidence  =  confidence  -  5 ;
      }
    lastname  = (Word[])(nodeSequenceAddNode( lastname ,  thisWord ));
   followParallelNounPhrase();
    return   nameAfterLast ;
 }


 boolean goodNamesuffix() {
  /* a test from nameAfterFirst and nameAfterLast */
    return  ((thisWord.isFormOfCat( categorySet_Namesuffix ) ||
             (canBeRomanNumeral( thisWord ) &&
               thisWord.generalNumberp() &&
                (thisWord.numericalValue().intValue() <  30 ))) &&
           noPunctOrComma());
 }


 boolean veryGoodNamesuffix() {
  /* test from nameAfterTitle */
    return  (thisWord.isFormOfCat( categorySet_Namesuffix ) &&
            ( lastname  !=  null ) &&
           noPunctOrComma() &&
            ( ! ( lowerCaseClashesWithName()) ) &&
            ( ! (upperCaseClashesWithName() &&
                ( ! ( canBeRomanNumeral( thisWord )) ))));
 }


 int defaultOrNameConfidence( Word  word ) {
  /* returns an integer that is the default nameconfidence or the one
   found on the properties of the word */
   {  Value  ncw  = word.getdict( feature_Nameconfidence );
    if     ( ncw  !=  null )
        return  ((Word) ncw).numericalValue().intValue();

    else   return   10 ;
   }
 }


 State acceptNamesuffix() {
  /* action from nameAfterFirst and nameAfterLast */
    confidence  = Math.max( confidence , defaultOrNameConfidence( thisWord ));
   if    (caseConsistentWithName())
      { /* empty statement here */ }

   else   confidence  =  confidence  -  4 ;
   if    (upperCaseClashesWithText() &&
          looksLikeOrdinaryWord( thisWord ) &&
          thisWord.hasNonnameCat())
      { if     ( !  ucName )
            confidence  =  confidence  -  5 ;
      }
    namesuffix  = (Word[])(nodeSequenceAddNode( namesuffix ,  thisWord ));
   followParallelNounPhrase();
    return   nameAfterLast ;
 }


 State nameAfterLast() {
   if     ( thisWord  ==  word_$S )
      { if   (goodInitialCapFirstname() ||
              haveGoodNameParts())
          { transmitLikelyNameParts();
           if     ( ! ( betterNotCancelNoun()) )
              { if     ( nounState  !=  null )
                   { cancelNameCodeHypothesis();
                     determiner  =  null ;
                    cancelNounStateWithoutClearing();
                   }
              }
          }
        return  makeNamePartsIntoDeterminerAndFollowNoun();
      }

   else  {  Word  tempWord1064  = singleton( lastname );
        if    ( ( firstname  ==  null ) &&
               singletonp( lastname ) &&
               namePartIsOddCaseAndOrdinaryWord( tempWord1064 ))
            return  abandonNameForNounPhrase();

        else  if   ( ( firstname  ==  null ) &&
                ( initials  ==  null ) &&
                ( prefixtitle  ==  null ) &&
               singletonIntersect( lastname ,  wordSet_San$Some ))
            return  abandonNameForNounPhrase();

        else  if   ( (singleton( lastname ) ==  word_Will ) &&
                ( namesuffix  ==  null ) &&
               ( ucName  ||
                  lcName  ||
                  ucText  ||
                  icText ) &&
               thisWord.isRootOfCat( categorySet_Adv$V ))
            return   null ;

        else  if   ( (singleton( lastname ) ==  word_More ) &&
                ( namesuffix  ==  null ) &&
               ( ucName  ||
                  lcName  ||
                  ucText  ||
                  icText ) &&
                ( firstname  !=  null ) &&
               firstname.isRootOfCat( categorySet_Adv ))
            return   null ;

        else  if   ( ( namesuffix  !=  null ) ||
               (sbreak() &&
                  ( ! ( ( initials  !=  null ) &&
                      ( lastname  ==  null )))) ||
                ( priorInput  ==  word_2cr ) ||
               capcodeClashesWithLcName())
            return  transmitNamePartsThenRunNounHypothesis();

        else  {  Word  tempWord1065  = singleton( lastname );
             if    (singletonp( lastname ) &&
                    lastnameCanCombineWithThis() &&
                    tempWord1065.isFormOfCat( categorySet_Firstname ) &&
                     ( firstname  ==  null ) &&
                     ( initials  ==  null ) &&
                     ( namesuffix  ==  null ) &&
                     ( ! ( multiWordp( lastname )) ))
                 return  moveLastnameToFirstnameAndSetNewLastname();

             else  {  Word  tempWord1066  = singleton( lastname );
                  if    (lastnameCanCombineWithThis() &&
                         ( ( ! ( lcName  ||
                                ucText )) ||
                           (thisWord.looksLike( category_Name ) &&
                             (multiWordp( lastname ) ||
                               tempWord1066.looksLike( category_Name )))) &&
                          ( ! (multiWordp( lastname ) &&
                              ( lastname.length  >  2 ))))
                      return  acceptAdditionalLastname();

                  else  if   (goodNamesuffix())
                      return  acceptNamesuffix();

                  else   return  transmitNamePartsThenRunNounHypothesis();
                 }
            }
       }
 }


 boolean noPunctOrAbbrevPunct( Word  previousWord ) {
  /* true if there is no punctuation or it can be explained 
   as part of previousWord which can be an abbreviation */
    return  (noPrevPunct() ||
           punctCanBeAbbrevPunct( previousWord ));
 }


 boolean punctCanBeAbbrevPunct( Word  previousWord ) {
    return  ( ( previousWord  !=  null ) &&
           (justLettersWithPeriodsBetweenThem( previousWord ) ||
             getDictAbbrev( previousWord )) &&
            (prevPunct( 1 ) ==  word_Dot ) &&
           noPrevPunctAfter( 1 ));
 }


 boolean isTitleReallyPlainNoun() {
  /* test from nameAfterTitle */
    return  ( (capcode() !=  capcode_Lc ) &&
           noPrevPunct() &&
            ( ! ( thisWord.isFormOfCat( categorySet_Name )) ) &&
           looksLikeOrdinaryWord( thisWord ) &&
           thisWord.hasNonnameCat() &&
            ( firstname  ==  null ) &&
            ( initials  ==  null ) &&
            ( lastname  ==  null ) &&
            ( namesuffix  ==  null ) &&
           prefixtitle.hasNonnameCat());
 }


 boolean areWordAndTitleReallyPlainNouns() {
  /* test from nameAfterTitle */
   looksLikeOrdinaryWord( thisWord );
   prefixtitle.hasNonnameCat();
   thisWord.hasNonnameCat();
   prefixtitle.isPenaltyFormOfCat( categorySet_Prefixtitle );
    return  thisWord.isPenaltyFormOfCat( categorySet_Name );
 }


 boolean capitalizedInitialAndAName() {
  /* test from nameAfterTitle */
    return  (wordClass.equals( category_Singleletter ) &&
           noPunctOrAbbrevPunct( prefixtitle ) &&
           ( ( thisinput  ==  word_Dot ) ||
              ( prefixtitle  !=  null ) ||
              ( firstname  !=  null )) &&
              LexiconUtil.isMembOfArray(capcode(), (Value[]) featureSet_Ic$Uc ));
 }


 boolean lastnameMightBeFirstname() {
  /* test from nameAfterTitle given an initial */
    Word  tempWord1067  = singleton( lastname );
    return  ( ( lastname  !=  null ) &&
            ( firstname  ==  null ) &&
            ( initials  ==  null ) &&
           (tempWord1067.isFormOfCat( categorySet_Firstname ) ||
             singleton(lastname).getdict(feature_Wordclass).equals( value_Dualname )) &&
            ( ! ( multiWordp( lastname )) ) &&
           noPrevPunct());
 }


 boolean nameHypothesisIsWorthKeeping() {
    return  (( ( firstname  !=  null ) ||
              ( lastname  !=  null ) ||
              ( initials  !=  null )) &&
            ( ! ( nounHypothesisIsBetterThanName()) ));
 }


 boolean nounHypothesisIsBetterThanName() {
    return  ( ( nounState  !=  null ) &&
            ( confidence  <  confidenceThreshold ));
 }


 void transmitLikelyNameParts() {
  /* action for starting new name while holding one */
   recordGoodFirstname();
   recordGoodLastname();
   transmitName();
 }


 void transmitName() {
   transmit((Phrase)( new NamePhrase( prefixtitle ,  firstname ,  initials ,  lastname ,  namesuffix ,  phraseStart )));
 }


 void setLastnameMaybeMovingOldLastToFirst() {
  /* part of an action in nameAfterTitle */
   if    (lastnameMayBeFirstname() &&
          caseConsistentWithName() &&
           ( ! (upperCaseClashesWithText() &&
               ( !  ucName ) &&
              looksLikeOrdinaryWord( thisWord ) &&
              thisWord.hasNonnameCat())) &&
           ((  firstname  = singleton( lastname ) ) !=  null ))
       lastname  =  null ;
    lastname  = (Word[])(nodeSequenceAddNode( lastname ,  thisWord ));
 }


 boolean thisWordCouldBeLastname() {
  /* test from nameAfterTitle */
    return  (haveFirstnameOrGoodPrefixtitle() &&
           noPunctOrAbbrevPunct( prefixtitle ) &&
            ( !  ( confidence  <  confidenceThreshold )) &&
            ( ! ( LexiconUtil.isMembOfArray( thisWord ,  sp_verbsUnlikelyAsLastnames )) ) &&
            ( ! ( thisWord.isFormOfCat( categorySet_Det$Prep$Pro$Nu_Etc )) ) &&
            ( ! ( caseClashesWithName()) ) &&
            ( ! (thisWord.isNonnameFormOfCat( categorySet_V$Adv ) &&
               ( (capcode() ==  capcode_Lc ) ||
                 ( ucName  &&
                    (capcode() ==  capcode_Uc ))))) &&
            ( ! ( ( nounState  !=  null ) &&
               ( ( determiner  !=  null ) ||
                  ( adverbs  !=  null ) ||
                  ( modifiers  !=  null )))));
 }


 boolean lastnameMayBeFirstname() {
  /*  test in useWordAsLastName and setLastNameMaybeMovingOldLastToFirst */
    Word  tempWord1068  = singleton( lastname );
    return  ( ( lastname  !=  null ) &&
           singletonp( lastname ) &&
            ( firstname  ==  null ) &&
            ( initials  ==  null ) &&
           getWordClass(tempWord1068).equals( category_Dualname ));
 }


 State useWordAsLastname() {
  /* action from nameAfterTitle */
   followParallelNounPhrase();
   if    ( ( firstname  !=  null ) &&
           ( lastname  !=  null ))
      { transmitLikelyNameParts();
        return   null ;
      }

   else  if   ( ( confidence  >  confidenceThreshold ) &&
           ( ! ( thisWord.numeralp()) ) &&
          thisWord.syllabic())
      { if   (lastnameMayBeFirstname())
          {  firstname  = singleton( lastname );
            lastname  =  null ;
          }
        lastname  = (Word[])(nodeSequenceAddNode( lastname ,  thisWord ));
       guessName( thisWord );
        return   nameAfterLast ;
      }

   else  if   ( ( firstname  !=  null ) ||
           ( lastname  !=  null ) ||
           ( initials  !=  null ))
      { transmitLikelyNameParts();
       cancelNameCodeHypothesis();
       clearNameParts();
        return   null ;
      }

   else   return  abandonNameForNounPhrase();
 }


 State nameAfterTitle() {
   if    (isTitleReallyPlainNoun())
       return  abandonNameForNounPhrase();

   else  if   ( ( priorInput  ==  word_2cr ) ||
          ( ( priorInput  ==  word_Sbreak )) ||
           ( namesuffix  !=  null ))
      { if   (( ( firstname  !=  null ) ||
                 ( lastname  !=  null ) ||
                 ( initials  !=  null )) &&
              ( ( !  lcName ) ||
                ( ( firstname  !=  null ) &&
                   ( lastname  !=  null ))) &&
               ( ! ( caseClashesWithName()) ))
          transmitLikelyNameParts();
        return   null ;
      }

   else  if   ( ucText  &&
          noPrevPunct() &&
          areWordAndTitleReallyPlainNouns())
       return  abandonNameForNounPhrase();

   else  if   (wordClass.equals( category_Prefixtitle ))
      { if   (nameHypothesisIsWorthKeeping())
          transmitLikelyNameParts();
       initializeStateVariables();
       clearNameParts();
        return  prefixtitleStart();
      }

   else  if   (thisWord.isFormOfCat( categorySet_Nameprefix ) &&
          noPunctOrAbbrevPunct( prefixtitle ))
      { if   (upperCaseClashesWithText() &&
              thisWord.hasNonnameCat())
           confidence  =  confidence  -  5 ;
        lastname  = (Word[])(nodeSequenceAddNode( lastname ,  thisWord ));
       followParallelNounPhrase();
        return   nameAfterLast ;
      }

   else  if   (capitalizedInitialAndAName())
      { followParallelNounPhrase();
       if    (lastnameMightBeFirstname())
          {  firstname  = singleton( lastname );
            lastname  =  null ;
            initials  =  new Word[] { thisWord };
            return   nameAfterFirst ;
          }

       else  if    ( lastname  !=  null )
          { if   ( ( ! ( nounHypothesisIsBetterThanName()) ) &&
                   ( ! ( weaknames()) ))
              { transmitLikelyNameParts();
               if     ( ! ( betterNotCancelNoun()) )
                  cancelNounStateWithoutClearing();
              }
            return  abandonNameForNounPhrase();
          }

       else  {  initials  = (Word[])(nodeSequenceAddNode( initials ,  thisWord ));
             return   nameAfterFirst ;
           }
      }

   else  if   (veryGoodNamesuffix())
      { if   (caseConsistentWithName())
          { /* empty statement here */ }

       else   confidence  =  confidence  -  4 ;
       if    (upperCaseClashesWithText() &&
              thisWord.hasNonnameCat())
          { if     ( !  ucName )
                confidence  =  confidence  -  5 ;
          }
        namesuffix  = (Word[])(nodeSequenceAddNode( namesuffix ,  thisWord ));
        return   nameAfterLast ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Lastname ) &&
           ( ! ( caseClashesWithName()) ) &&
          noPunctOrAbbrevPunct( prefixtitle ))
      { setLastnameMaybeMovingOldLastToFirst();
       followParallelNounPhrase();
        return   nameAfterLast ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Firstname ) &&
          thisWord.isFormOfCat( categorySet_Lastname ) &&
           ( ! ( caseClashesWithName()) ) &&
          noPunctOrAbbrevPunct( prefixtitle ))
      { setLastnameMaybeMovingOldLastToFirst();
       if    (caseConsistentWithName())
          { /* empty statement here */ }

       else   confidence  =  confidence  -  4 ;
       if    (upperCaseClashesWithText() &&
              looksLikeOrdinaryWord( thisWord ) &&
              thisWord.hasNonnameCat())
           confidence  =  confidence  -  5 ;
       followParallelNounPhrase();
       if    (multiWordp( lastname ) &&
               ( lastname.length  >  2 ))
          { transmitLikelyNameParts();
            return   null ;
          }

       else   return   nameAfterLast ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Firstname ) &&
           ( lastname  ==  null ) &&
           ( initials  ==  null ) &&
           ( namesuffix  ==  null ) &&
           ( ! ( caseClashesWithName()) ) &&
          noPunctOrAbbrevPunct( prefixtitle ))
      {  firstname  =  thisWord ;
       if    (caseConsistentWithName())
          { /* empty statement here */ }

       else   confidence  =  confidence  -  4 ;
       if    (upperCaseClashesWithText() &&
              looksLikeOrdinaryWord( thisWord ) &&
              thisWord.hasNonnameCat())
          { if     ( !  ucName )
                confidence  =  confidence  -  5 ;
          }
       followParallelNounPhrase();
        return   nameAfterFirst ;
      }

   else  if   (thisWordCouldBeLastname())
       return  useWordAsLastname();

   else  if   ( ( firstname  !=  null ) ||
           ( lastname  !=  null ) ||
           ( initials  !=  null ))
      { transmitLikelyNameParts();
        return  abandonNameForNounPhrase();
      }

   else   return  abandonNameForNounPhrase();
 }


 boolean detOrNumber( Word  wordorphrase ) {
  /* test from xAfterPreposition and npAfterDet */
    return  (wordorphrase.wordp() &&
           wordorphrase.isFormOfCat( categorySet_Det$Number ));
 }


 boolean numberOrMemb( Value  detword ,  Word[]  set ) {
  /* called with <set> optionally holding some particular words to match */
    return  ( ( detword  !=  null ) &&
           detword.wordp() &&
           (LexiconUtil.isMembOfArray((Word) detword ,  set ) ||
             ((Word) detword).isFormOfCat( categorySet_Number )));
 }


 State xAfterPreposition() {
    phraseStart  =  refcount ;
   if    (prevBreak())
       return   null ;

   else  if   (looksLikeTime( thisWord ))
      {  hour  = thisWord.numericalValue().intValue();
        return   dateAfterTime ;
      }

   else  if   (integerOfMonthPhrase())
       return  popNpStackToDate();

   else  if   (thisWord.isFormOfCat( categorySet_Integer ) &&
           ( ! ( canBeRomanNumeral( thisWord )) ))
       return  cardinalStart();

   else  if   (thisWord.isFormOfCat( categorySet_Ord ))
       return  ordinalStart();

   else  if   (thisWord.isFormOfCat( categorySet_Number ))
      { generalnumberSetup();
        return   xAfterNumber ;
      }

   else  if   (detOrNumber( thisWord ))
      {  determiner  =  thisWord ;
        return   npAfterDet ;
      }

   else  if   (breakwordTest( thisWord ))
       return   null ;

   else  if   (wordClass.equals( category_City ))
       return  cityStart();

   else  if   (categorySet_Firstname$Dualn_Etc.subsumesCategory( wordClass ))
       return  firstnameStart();

   else  if   (wordClass.equals( category_Lastname ))
       return  lastnameStart();

   else  if   (wordClass.equals( category_Prefixtitle ))
       return  prefixtitleStart();

   else  {  boolean  tempboolean1069 ;
         boolean  tempboolean1070 ;
         boolean  tempboolean1071 ;
        if    (  tempboolean1071  = ( ( preposition  ==  word_To ) &&
               thisWord.isFormOfCat( categorySet_V )) )
            tempboolean1070  =  tempboolean1071 ;

        else  if   (LexiconUtil.isMembOfArray( preposition ,  wordSet_After$Before$Fo_Etc ))
            tempboolean1070  = thisWord.isFormOfCat( categorySet_Prespart );

        else   tempboolean1070  =  false ;
        if    ( tempboolean1070 )
            tempboolean1069  = thisWordNotToBeNowOrPast();

        else   tempboolean1069  =  false ;
        if    ( tempboolean1069 )
           {  preposition  =  null ;
             verb  =  thisWord ;
             verbPhraseStart  =  refcount ;
             return   npAfterVerb ;
           }

        else  if   (canBeAdvQualifier( thisWord ))
           {  adverbs  =  new Value[] { thisWord };
             return   npAfterAdv ;
           }

        else  if   (thisWord.isFormOfCat( categorySet_N ))
           {  noun  =  thisWord ;
             return   npAfterNoun ;
           }

        else  if   (thisWord.isFormOfCat( categorySet_Adj$N ))
           {  modifiers  =  new Value[] { thisWord };
             return   npAfterAdj ;
           }

        else   return   null ;
       }
 }


 boolean integerOfMonthPhrase() {
  /* test used in xAfterPreposition  */
   if    (thisWord.isFormOfCat( categorySet_Integer ) &&
           ( preposition  ==  word_Of ) &&
          singletonNounStack())
      {  NpStackElt  nstop  = topOfNounStack();
        Word  nstopnoun  =  nstop.noun ;
        return  ( ( nstop.prep  ==  word_Of ) &&
                ( nstop.det  ==  null ) &&
               nstopnoun.isFormOfCat( categorySet_Month ) &&
                ( nstop.postmodstack  ==  null ));
      }

   else   return   false ;
 }


 boolean phraseIsOfTheOrdinalDay() {
  /*  test used twice in npAfterNoun */
    return  ( ( thisWord  ==  word_Of ) &&
            ( noun  ==  word_Day ) &&
            ( determiner  !=  null ) &&
           determiner.equals( word_The ) &&
            ( modifiers  !=  null ) &&
            ( 1  == nodeSequenceLength( modifiers )) &&
           wordIsFormOfCat(nodeSequenceFirst( modifiers ),  categorySet_Ord ));
 }


 boolean OrdinalDayOfTheMonthPhrase() {
  /* test used in xAfterNoun   */
   if    (singletonNounStack() &&
          noun.isFormOfCat( categorySet_Month ))
      {  NpStackElt  nstop  = topOfNounStack();
        Value  nstopdet  =  nstop.det ;
        Word  nstopnoun  =  nstop.noun ;
        Value[]  nstopmods  =  nstop.modstack ;
        return  ( ( nstopdet  !=  null ) &&
                ( nstop.prep  ==  word_Of ) &&
               nstopdet.equals( word_The ) &&
                ( nstopnoun  ==  word_Day ) &&
               wordIsFormOfCat(nodeSequenceFirst( nstopmods ),  categorySet_Ord ) &&
                ( 1  == nodeSequenceLength( nstopmods )) &&
                ( nstop.postmodstack  ==  null ));
      }

   else   return   false ;
 }


 boolean OrdinalOfTheMonthPhrase() {
  /* test used in xAfterNoun   */
   if    (singletonNounStack() &&
          noun.isFormOfCat( categorySet_Month ))
      {  NpStackElt  nstop  = topOfNounStack();
        Value  nstopdet  =  nstop.det ;
        Word  nstopnoun  =  nstop.noun ;
        return  ( ( nstop.prep  ==  word_Of ) &&
                ( nstopdet  !=  null ) &&
               nstopdet.equals( word_The ) &&
               nstopnoun.isFormOfCat( categorySet_Ord ) &&
                ( nstop.postmodstack  ==  null ));
      }

   else   return   false ;
 }


 State popNpStackToDate() {
  /* used in xAfterPreposition */
   {  NpStackElt  nstop  = popNounStack();
     month  = (Value)( nstop.noun );
     phraseStart  =  nstop.phraseStart ;
     nounstack  =  null ;
    if    (thisWord.numeralp())
       {  year  = thisWord.numericalValue().intValue();
         return   popDate ;
       }

    else  if    ( ! ( canBeRomanNumeral( thisWord )) )
       {  expectYear  =  true ;
         secondnum  = thisWord.numericalValue().doubleValue();
         numberWord  =  thisWord ;
         return   dateAfterMonthDayCardinal ;
       }

    else   return   null ;
   }
 }


 State popNPStackToOrdinalMonthPhrase( Word  ordword ) {
  /* used twice in npAfterNoun */
    month  =  noun ;
    day  = ordword.numericalValue().intValue();
    phraseStart  =  popNounStack().phraseStart ;
    return   dateAfterMonthDay ;
 }


 State startNamePhraseAndAltNounHypothesis() {
  /*  action used in npAfterDet */
    phraseStart  =  refcount ;
   moveNounOrModToNounState();
    return  startNameAndNounState();
 }


 void moveNounOrModToNounState() {
    altPhraseStart  =  phraseStart ;
   if    (thisWord.isFormOfCat( categorySet_N ))
      {  noun  =  thisWord ;
        nounState  =  npAfterNoun ;
      }

   else  {  modifiers  = nodeSequencePushNode( modifiers ,  thisWord );
         nounState  =  npAfterAdj ;
       }
 }


 State startNameAndNounState() {
  /* action called from startNamePhraseAndAltNounHypothesis 
    and moveNpHypothesisToNounStateAndRunName */
   scantraceParallelPhraseStart( scanState );
   if    (thisWord.isFormOfCat( categorySet_City ))
       return  startNpAfterCity();

   else  if   (thisWord.isFormOfCat( categorySet_Firstname ))
       return  startNpAfterFirstname();

   else  if   (thisWord.isFormOfCat( categorySet_Lastname ))
       return  startNpAfterLastname();

   else   return   null ;
 }


 boolean verbCanBeNoun() {
  /* test used in npAfterDet to decide whether to treat
  presumed verb as noun head */
    return  ( ( verb  !=  null ) &&
           verb.isFormOfCat( categorySet_N ) &&
            ( postmods  ==  null ) &&
           numberOrMemb( determiner ,  wordSet_A ));
 }


 State verbWithDetBecomesNounWithPostmod() {
  /* action from npAfterDet to change theory of the phrase */
    postmods  =  new Value[] { determiner };
   makeVerbNoun();
    return  npAfterPostmod();
 }


 void makeVerbNoun() {
    noun  =  verb ;
    verb  =  null ;
    determiner  =  null ;
 }


 State verbBecomesNounWithThisWordPostmod() {
  /* action from npAfterVerb to change theory of the phrase; consumes thisWord */
    postmods  =  new Value[] { thisWord };
   makeVerbNoun();
    return   npAfterPostmod ;
 }


 boolean wordIsFormOfCat( Value  filler ,  Category  cat ) {
  /* ensures filler is an atom and then checks its category;
  used wherever determiner might be a complex struct or just a word */
    return  ( ( filler  !=  null ) &&
           filler.wordp() &&
           ((Word) filler).isFormOfCat( cat ));
 }


 boolean determinerIsCardinalWord() {
  /* test used two places in npAfterDet  */
    return  ( ( determiner  !=  null ) &&
           determiner.wordp() &&
            ( ! ( ((Word) determiner).numeralp()) ) &&
           ((Word) determiner).isFormOfCat( categorySet_Cardinal ));
 }


 void useDeterminerAsFirstnum() {
    firstnum  = ((Word) determiner).numericalValue().doubleValue();
    numberWord  = (Word) determiner ;
 }


 State npAfterDet() {
   if    (sentencePunctOrBreakword() &&
           ( determiner  instanceof  PossPhrase ))
       return  handleRetroPossessive();

   else  if   (thisWord.isFormOfCat( categorySet_Integer ) &&
           ( ! ( thisWord.numeralp()) ) &&
          determinerIsCardinalWord())
      { useDeterminerAsFirstnum();
        return  xAfterCardinal();
      }

   else  if   (thisWord.isFormOfCat( categorySet_Ord ) &&
          determinerIsCardinalWord())
      { useDeterminerAsFirstnum();
        return  xAfterOrdinal();
      }

   else  if   (thisWord.isFormOfCat( categorySet_Integer ) &&
           ( ! ( canBeRomanNumeral( thisWord )) ))
       return  cardinalStart();

   else  if   (thisWord.isFormOfCat( categorySet_Ord ))
       return  ordinalStart();

   else  if   (thisWord.isFormOfCat( categorySet_Number ))
      { generalnumberSetup();
        return   xAfterNumber ;
      }

   else  if   (prevBreak() &&
          verbCanBeNoun())
       return  verbWithDetBecomesNounWithPostmod();

   else  if   (prevBreak())
      { if    ( nounstack  !=  null )
          {  noun  = (Word) determiner ;
            determiner  =  null ;
           popNpStack();
          }
        determiner  =  null ;
        return   null ;
      }

   else  if   (detOrNumber( thisWord ) &&
          wordIsFormOfCat( determiner ,  categorySet_Predet$Pro ))
      {  determiner  =  thisWord ;
        return   npAfterDet ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Postdet ))
       return   npAfterDet ;

   else  if   (thisWord.isFormOfCat( categorySet_Number ))
      {  modifiers  =  new Value[] { thisWord };
        return   npAfterAdj ;
      }

   else  if   ( sp_includeConjunctions  &&
		   LexiconUtil.isMembOfArray( thisWord ,  wordSet_And$Or ))
      {  noun  = (Word) determiner ;
        determiner  =  null ;
        return  stackNounAndStartPrep();
      }

   else  if   (breakwordTest( thisWord ) &&
          verbCanBeNoun())
       return  verbWithDetBecomesNounWithPostmod();

   else  if   (breakwordTest( thisWord ))
      {  determiner  =  null ;
        return   null ;
      }

   else  if   (LexiconUtil.isMembOfArray( thisWord ,  wordSet_Percent ) &&
          wordIsFormOfCat( determiner ,  categorySet_Number ))
      {  modifiers  =  new Value[] { determiner };
        determiner  =  null ;
        noun  =  thisWord ;
        return   npAfterNoun ;
      }

   else  if   (canBeAdvQualifier( thisWord ))
      { if   (wordIsFormOfCat( determiner ,  categorySet_Adv ))
          {  adverbs  =  new Value[] { determiner ,  thisWord };
            determiner  =  null ;
          }

       else   adverbs  =  new Value[] { thisWord };
        return   npAfterAdv ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Firstname$Lastn_Etc ) &&
           ( nounState  ==  null ))
       return  startNamePhraseAndAltNounHypothesis();

   else  if   (thisWord.isFormOfCat( categorySet_Adv ) &&
		   LexiconUtil.isMembOfArray( determiner ,  wordSet_Less$More$Most ))
      {  adverbs  =  new Value[] { determiner ,  thisWord };
        determiner  =  null ;
        return   npAfterAdv ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_N ))
      {  noun  =  thisWord ;
        return   npAfterNoun ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Adj ) &&
		   LexiconUtil.isMembOfArray( determiner ,  wordSet_Less$More$Most$_Etc ))
      {  modifiers  =  new Value[] {(Value)(adjPhraseWithOneAdv( thisWord , (Word) determiner ))};
        determiner  =  null ;
        return   npAfterAdj ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Adj$N ))
      {  modifiers  = nodeSequenceAddNode( modifiers ,  thisWord );
        return   npAfterAdj ;
      }

   else  if   (verbCanBeNoun())
       return  verbWithDetBecomesNounWithPostmod();

   else  if    ( determiner  instanceof  PossPhrase )
       return  handleRetroPossessive();

   else  { if    ( nounstack  !=  null )
           {  noun  = (Word) determiner ;
             determiner  =  null ;
            popNpStack();
           }
         determiner  =  null ;
         return   null ;
       }
 }


 boolean advsOkForMoveToMods() {
  /* test used in npAfterAdv (and through reconsiderAdvFromNpAfterAdv) */
    return  ( ( adverbs  !=  null ) &&
           nodeSequenceLast(adverbs).wordp() &&
           ((Word) nodeSequenceLast(adverbs)).isFormOfCat( categorySet_Adj$Ord ));
 }


 State reconsiderAdvFromNpAfterAdv() {
  /*  action from npAfterAdv  */
   if    (headOfModSequenceCouldBeNoun( adverbs ))
       return  moveLastModifierToNoun( adverbs );

   else  if   (advsOkForMoveToMods())
      { moveAdvsToMods( adverbs );
       clearTempPhrase();
        return  npAfterAdj();
      }

   else  { tryToUseFirstModAsNounAndPopNpStack();
        clearVerbParts();
        clearTempPhrase();
         return   null ;
       }
 }


 State npAfterAdv() {
   if    ( ( 1  == nodeSequenceLength( adverbs )) &&
           (nodeSequenceFirstWord( adverbs ) ==  word_Way ) &&
           ( determiner  !=  null ) &&
          determiner.equals( word_The ) &&
          thisWord.isFormOfCat( categorySet_Npl$Nm$Pro$Det$_Etc ) &&
           ( postmods  ==  null ))
       return   null ;

   else  if   (sentencePunctOrBreakword() ||
          (thisWord.isFormOfCat( categorySet_V$Det$Number ) &&
             ( ! ( ( verb  !=  null ) ||
                 ( determiner  !=  null ) ||
                 ( modifiers  !=  null ) ||
                 ( adverbs  !=  null ) ||
                 ( noun  !=  null ) ||
                 ( nounstack  !=  null ) ||
                thisWord.isFormOfCat( categorySet_N$Adj$Adv )))))
       return  reconsiderAdvFromNpAfterAdv();

   else  if   (canBeAdvQualifier( thisWord ))
      {  adverbs  = nodeSequenceAddNode( adverbs ,  thisWord );
        return   npAfterAdv ;
      }

   else  if   (thisWord.numeralp() ||
          wordIsFormOfCat( thisWord ,  categorySet_Adj$Ord$Number ))
      {  modifiers  = nodeSequencePushNode( modifiers , (Value)( new AdjPhrase( thisWord ,  adverbs )));
        adverbs  =  null ;
       clearTempPhrase();
        return   npAfterAdj ;
      }

   else  if   ( ( 1  == nodeSequenceLength( adverbs )) &&
          thisWord.isFormOfCat( categorySet_N ) &&
          nodeSequenceFirstWord(adverbs).isFormOfCat( categorySet_Adj$N ))
       return  pushAdvModThisWordIsNoun((Value)(nodeSequenceFirstWord( adverbs )));

   else  if   ( (nodeSequenceLength( adverbs ) >  1 ) &&
          nodeSequenceFirst(adverbs).wordp() &&
          thisWord.isFormOfCat( categorySet_N ) &&
          nodeSequenceLastWord(adverbs).isFormOfCat( categorySet_Adj$N ) &&
          adjOrAdvWord(nodeSequenceSecondLast(adverbs)).isFormOfCat( categorySet_Adj$Ord ))
       return  pushAdvModThisWordIsNoun((Value)(adjPhraseFromAdvs( adverbs )));

   else  if   (headOfModSequenceCouldBeNoun( adverbs ))
       return  moveLastModifierToNoun( adverbs );

   else  if   (advsOkForMoveToMods())
      { moveAdvsToMods( adverbs );
        return  npAfterAdj();
      }

   else  { tryToUseFirstModAsNounAndPopNpStack();
        clearTempPhrase();
        clearVerbParts();
         return   null ;
       }
 }


 State pushAdvModThisWordIsNoun( Value  advmod ) {
  /* action used three places in npAfterAdv */
    noun  =  thisWord ;
    modifiers  = nodeSequencePushNode( modifiers ,  advmod );
    adverbs  =  null ;
    return   npAfterNoun ;
 }


 boolean adjPhraseCouldBeNoun( AdjPhrase  adjph ) {
  /* test used in  makeModifiersIntoNpOrDropNp  */
   {  Word  adj  =  adjph.adjective ;
     Value[]  advs  =  adjph.advs ;
     return  headOfModSequenceCouldBeNoun(nodeSequenceAddNode( advs ,  adj ));
   }
 }


 boolean headOfModSequenceCouldBeNoun( Value[]  modsseq ) {
  /* test used in npAfterAdj, reconsiderAdvFromNpAfterAdv (called by 
   npAfterAdv), adjPhraseCouldBeNoun (from makeModifiersIntoNpOrDroNp),
  and npAfterAdj  */
   {  Value  head  = nodeSequenceLast( modsseq );
     Value  rm1  = nodeSequenceSecondLast( modsseq );
     return  ( ( head  !=  null ) &&
            head.wordp() &&
            ((Word) head).isFormOfCat( categorySet_N ) &&
            ( ( rm1  ==  null ) ||
              (rm1.wordp() &&
                ((Word) rm1).isFormOfCat( categorySet_Adj$Ord )) ||
              ( (nodeSequenceLength( modsseq ) ==  2 ) &&
                rm1.wordp() &&
                ((Word) rm1).isFormOfCat( categorySet_N ))));
   }
 }


 boolean allElementsAreWords( Value[]  valseq ) {
    boolean  tempboolean1075 ;
   {  tempboolean1075  =  true ;
    if     ( valseq  !=  null )
       for ( int  i  =  0 ;  i  <  valseq.length ;  i++ ) {
           Value  el  =  valseq[i] ;
          { if     ( !  ( el  instanceof  Word ))
               {  tempboolean1075  =  false ;
                 break ;
               }
          }
       }
     return   tempboolean1075 ;
   }
 }


 void tryToUseFirstModAsNounAndPopNpStack() {
  /* action from npAfterAdv and reconsiderAdvFromNpAfterAdv */
   if    ( ( nounstack  !=  null ) &&
           ( modifiers  !=  null ) &&
          nodeSequenceFirst(modifiers).wordp() &&
          nodeSequenceFirstWord(modifiers).isFormOfCat( categorySet_N ))
      {  noun  = nodeSequenceFirstWord( modifiers );
        modifiers  = nodeSequenceRest( modifiers );
        adverbs  =  null ;
       popNpStack( tempPhraseEnd );
      }
 }


 State makeModifiersIntoNpOrDropNp() {
  /* action used at hard break and final case of npAfterAdj */
   {  Value  ws1  = nodeSequenceFirst( modifiers );
     State  tempState1076 ;
    if     ( ws1  instanceof  AdjPhrase )
       {  State  tempState1077 ;
         modifiers  = nodeSequenceRest( modifiers );
        if    (adjPhraseCouldBeNoun((AdjPhrase) ws1 ))
            tempState1077  = moveAdjPhraseToNoun((AdjPhrase) ws1 );

        else   tempState1077  =  null ;
        if     ( tempState1077  !=  null )
            tempState1076  =  tempState1077 ;

        else   tempState1076  =  null ;
       }

    else   tempState1076  =  null ;
    if     ( tempState1076  !=  null )
        return   tempState1076 ;

    else  { {  modifiers  =  null ;
           determiner  =  null ;
         }
          return   null ;
        }
   }
 }


 State moveAdjPhraseToNoun( AdjPhrase  adjph ) {
  /* action used in npAfterAdj through makeModifiersIntoNpOrDropNp */
    noun  =  adjph.adjective ;
   moveAdvsToMods( adjph.advs );
    return  npAfterNoun();
 }


 State moveLastModifierToNoun( Value[]  modseq ) {
  /* action used in npAfterAdj directly and through reconsiderAdvFromNpAfterAdv 
    in npAfterAdv */
    noun  = nodeSequenceLastWord( modseq );
   moveAdvsToMods(nodeSequenceButLast( modseq ));
   clearTempPhrase();
    return  npAfterNoun();
 }


 void clearTempPhrase() {
    tempPhraseStart  =  null ;
    tempPhraseEnd  =  null ;
 }


 void moveAdvsToMods( Value[]  advlist ) {
  /* action called from reconsiderAdvFromNpAfterAdv and moveLastModifierToNoun 
  (both in npAfterAdj) and npAfterAdv */
   if     ( advlist  !=  null )
      {  Value  newmod ;
       if     ( 1  == nodeSequenceLength( advlist ))
           newmod  = nodeSequenceFirst( advlist );

       else   newmod  = (Value)(adjPhraseFromAdvs( advlist ));
        modifiers  = nodeSequencePushNode( modifiers ,  newmod );
      }
    adverbs  =  null ;
 }


 State moveNpHypothesisToNounStateAndRunName() {
  /* action in npAfterAdj when a strong name start is encountered */
    altPhraseStart  =  phraseStart ;
   if    (canBeAdvQualifier( thisWord ))
      { if   (topModifierCanBeAdvQualifier() &&
              noPrevPunct())
          moveTopModifierToAdverbs();

       else   adverbs  =  new Value[] { thisWord };
        nounState  =  npAfterAdv ;
      }

   else  moveNounOrModToNounState();
    return  startNameAndNounState();
 }


 void moveTopModifierToAdverbs() {
  /* called directly from npAfterAdj and  also indirectly through 
     moveNpHypothesisToNounStateAndRunName */
    adverbs  =  new Value[] {(Value)(advPhraseWithOneQualifAdv( thisWord , (Word)(nodeSequenceFirst( modifiers ))))};
    modifiers  = nodeSequenceRest( modifiers );
 }


 State npAfterAdj() {
   if    (breakwordTest( thisWord ) ||
          (prevBreak() &&
             ( ! ( ( ! ( sbreak()) ) &&
                breakJustMemb( wordSet_Charset_Comma ) &&
                thisWord.isFormOfCat( categorySet_Adv$Adj$Ord )))))
       return  makeModifiersIntoNpOrDropNp();

   else  if   (thisWord.isFormOfCat( categorySet_Firstname$Lastn_Etc ) &&
           ( nounState  ==  null ) &&
          prevBreak())
       return  moveNpHypothesisToNounStateAndRunName();

   else  if   (canBeAdvQualifier( thisWord ))
      { if   (topModifierCanBeAdvQualifier())
          moveTopModifierToAdverbs();

       else   adverbs  =  new Value[] { thisWord };
        tempPhraseStart  =  refcount ;
        tempPhraseEnd  =  previousEnd ;
        return   npAfterAdv ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_N ) &&
          noPrevPunct())
      {  noun  =  thisWord ;
        return   npAfterNoun ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Adj$Ord ))
      { if   ( ( modifiers  !=  null ) &&
              thisWord.isFormOfCat( categorySet_Adj ) &&
              topModifierCanBeAdvQualifier())
           modifiers  = nodeSequencePushNode(nodeSequenceRest( modifiers ), (Value)(adjPhraseWithOneAdv( thisWord , (Word)(nodeSequenceFirst( modifiers )))));

       else   modifiers  = nodeSequencePushNode( modifiers ,  thisWord );
        return   npAfterAdj ;
      }

   else   return  makeModifiersIntoNpOrDropNp();
 }


 State makePostmodsFromNounAndNumber() {
  /*  action in addNumberValueToNumberPhrase and npAfterNoun.
   thisWord is a number that combines with noun to make a postmod */
    postmods  = nodeSequencePushNode( postmods , (Value)(makeModWord( new Value[] { noun ,  thisWord })));
   if     ( modifiers  !=  null )
      {  Value  mod1  = nodeSequenceFirst( modifiers );
       if    (mod1.wordp())
           noun  = (Word) mod1 ;

       else   noun  = makeModWord( mod1 );
        modifiers  = nodeSequenceRest( modifiers );
      }

   else  {  noun  =  verb ;
         verb  =  null ;
       }
    return   npAfterPostmod ;
 }


 State addNumberValueToNumberPhrase() {
  /*  test and action combo used in npAfterNoun */
   {  Value  mod1  =  null ;
    if     ( modifiers  !=  null )
        mod1  = nodeSequenceFirst( modifiers );
    if    ( ( modifiers  !=  null ) &&
           ((mod1.wordp() &&
               ((Word) mod1).isFormOfCat( categorySet_N )) ||
              ( mod1  instanceof  NominalPhrase )))
        return  makePostmodsFromNounAndNumber();

    else  if   ( ( determiner  ==  null ) &&
            ( modifiers  ==  null ) &&
            ( verb  !=  null ) &&
           verb.isFormOfCat( categorySet_N ))
        return  makePostmodsFromNounAndNumber();

    else  {  boolean  tempboolean1081 ;
         if    ( ( modifiers  !=  null ) &&
                 ( mod1  instanceof  AdjPhrase ) &&
                ((AdjPhrase) mod1).adjective.isFormOfCat( categorySet_N ))
            {  Value[]  advs  =  ((AdjPhrase) mod1).advs ;
              tempboolean1081  = (((Word) nodeSequenceFirst(advs)).isFormOfCat( categorySet_Adj$Ord ) ||
               ( ( advs.length  >  1 ) &&
                 ((Word) nodeSequenceFirst(advs)).isFormOfCat( categorySet_N ) &&
                 ((Word) nodeSequenceSecond(advs)).isFormOfCat( categorySet_Adj$Ord )));
            }

         else   tempboolean1081  =  false ;
         if    ( tempboolean1081 )
            {  postmods  = nodeSequencePushNode( postmods , (Value)(makeModWord( new Value[] { noun ,  thisWord })));
              noun  =  ((AdjPhrase) mod1).adjective ;
             {  Value[]  advs  =  ((AdjPhrase) mod1).advs ;
               Value  newmod ;
               modifiers  = nodeSequenceRest( modifiers );
              if    (((Word) nodeSequenceFirst(advs)).isFormOfCat( categorySet_Adj$Ord ))
                 if     (nodeSequenceLength( advs ) >  1 )
                     newmod  = (Value)(adjPhraseFromAdvs( advs ));

                 else   newmod  = nodeSequenceFirst( advs );

              else   newmod  = (Value)(adjPhraseFromAdvs( advs ));
               modifiers  = nodeSequencePushNode( modifiers ,  newmod );
             }
              return   npAfterPostmod ;
            }

         else  {  modifiers  = nodeSequencePushNode( modifiers ,  noun );
               noun  =  thisWord ;
               return   popNpStack ;
             }
        }
   }
 }


 boolean goodDateModsOrDet() {
  /* a test for whether to transmit a date; used in npAfterAdv and npAfterNoun */
    return  (( ( modifiers  !=  null ) ||
              ( postmods  !=  null )) &&
           ( ( determiner  ==  null ) ||
             numberOrMemb( determiner ,  wordSet_A$An$Several$Th_Etc )));
 }


 void wordIsNounForParallelHypothesis() {
  /* action setting up parallel hypothesis with new word treated as noun */
    modifiers  = nodeSequencePushNode( modifiers ,  noun );
    noun  =  thisWord ;
    nounState  =  npAfterNoun ;
 }


 void wordIsVerbForParallelHypothesis() {
  /* action setting up parallel hypothesis with new word treated as noun */
   clearNounVerbParts();
    verb  =  thisWord ;
    nounState  =  npAfterVerb ;
 }


 void wordIsAdjForParallelHypothesis() {
  /* action setting up parallel hypothesis with new word treated as adj */
   addNounAndThisWordToModifiers();
    nounState  =  npAfterAdj ;
 }


 void wordIsAdvForParallelHypothesis() {
  /* action setting up parallel hypothesis with new word treated as adv */
   clearNounVerbParts();
    adverbs  =  new Value[] { thisWord };
    nounState  =  npAfterAdv ;
 }


 State startParallelPhraseFromNpAfterNoun() {
  /*  npAfterNoun hit a word that means the alternative phrase should be handling
   some of this but there's no alternative phrase running */
   popNpStackIfNeeded();
    altPhraseStart  =  phraseStart ;
   if    (thisWord.isNonnameFormOfCat( categorySet_N ))
      wordIsNounForParallelHypothesis();

   else  if   (thisWord.looksLike( category_V ))
      wordIsVerbForParallelHypothesis();

   else  if   (thisWord.looksLike( category_Adv ))
      wordIsAdvForParallelHypothesis();

   else  if   (thisWord.isFormOfCat( categorySet_N ))
      wordIsNounForParallelHypothesis();

   else  if   (thisWord.isFormOfCat( categorySet_Adj ))
      wordIsAdjForParallelHypothesis();

   else  if   (thisWord.isFormOfCat( categorySet_V ))
      wordIsVerbForParallelHypothesis();

   else  if   (canBeAdvQualifier( thisWord ))
      wordIsAdvForParallelHypothesis();

   else  wordIsAdjForParallelHypothesis();
    phraseStart  =  refcount ;
    return  startNameAndNounState();
 }


 void popNpStackIfNeeded() {
  /* called in case there is enough phrase to transmit before continuing;
  causes an intermediate nounphrase to be generated during parsing
  of a larger one -- called from various places in npAfterNoun */
   if    ( ( modifiers  !=  null ) ||
           ( postmods  !=  null ) ||
           ( determiner  !=  null ) ||
          ( ( verb  !=  null ) &&
            verb.isFormOfCat( categorySet_Adj$N )) ||
          ( ( verb  !=  null ) &&
            canBePluralOrMass( noun )))
      popNpStackTop();
 }


 boolean nontrivialDeterminer( Value  det ) {
  /* there is a determiner and just it with a noun is worth transmitting */
    return  ( ( det  !=  null ) &&
            ( ! ( LexiconUtil.isMembOfArray((Word) det ,  sp_trivialDeterminers )) ));
 }


 State reinterpretNounAsMod() {
  /* called in npAfterNoun when an adj arrives and held noun is suspect */
    modifiers  = nodeSequencePushNode( modifiers ,  noun );
    noun  =  null ;
    return  npAfterAdj();
 }


 State npAfterNoun() {
   if    (breakButNotComma())
       return  popNpStack();

   else  if   (OrdinalOfTheMonthPhrase())
       return  popNPStackToOrdinalMonthPhrase( topOfNounStack().noun );

   else  if   (OrdinalDayOfTheMonthPhrase())
       return  popNPStackToOrdinalMonthPhrase((Word)(nodeSequenceFirst( topOfNounStack().modstack )));

   else  if   ( ( preposition  ==  word_On ) &&
          noun.isFormOfCat( categorySet_Month ) &&
          integerOrOrdOkNumForDateDay( thisWord ,  noun ))
      {  day  = thisWord.numericalValue().intValue();
        month  =  noun ;
        noun  =  null ;
        return   dateAfterMonthDay ;
      }

   else  if   (noun.isFormOfCat( categorySet_Month ) &&
          (LexiconUtil.isMembOfArray( thisWord ,  wordSet_In$Of ) ||
            thisWord.isFormOfCat( categorySet_Integer )))
      {  month  =  noun ;
        noun  =  null ;
        return  dateAfterMonthDay();
      }

   else  if   (noun.isFormOfCat( categorySet_Weekday ) &&
           ( modifiers  ==  null ) &&
           ( determiner  ==  null ) &&
           ( postmods  ==  null ) &&
          ( ( thisWord  ==  word_The ) ||
            integerOrOrdOkNumForDateDay( thisWord )))
      {  weekday  =  noun ;
        noun  =  null ;
       if     ( thisWord  ==  word_The )
          {  determiner  =  thisWord ;
            return   npAfterDet ;
          }

       else  {  day  = thisWord.numericalValue().intValue();
             return   dateAfterDay ;
           }
      }

   else  if   (prevBreak() ||
		   LexiconUtil.isMembOfArray( thisWord ,  wordSet_$Endoffile$ ))
       return  popNpStack();

   else  if   ( ( noun  ==  word_Number ) &&
          thisWord.isFormOfCat( categorySet_Number ))
       return  addNumberValueToNumberPhrase();

   else  if   ( ( thisWord  ==  word_An ) &&
          canBePluralOrUnit( noun ))
      { popNpStackTop();
        tempPhraseStart  =  refcount ;
        tempPhraseEnd  =  previousEnd ;
        return   npAfterAn ;
      }

   else  if   (LexiconUtil.isMembOfArray( thisWord ,  wordSet_Next$Last$Each$_Etc ))
       return  popNpStack();

   else  if   (LexiconUtil.isMembOfArray( thisWord ,  wordSet_Earlier$Sooner$_Etc ) &&
          looksLikeUnitOfTime( noun ))
      {  postmods  = nodeSequencePushNode( postmods ,  thisWord );
        confidence  =  8 ;
       if    (goodDateModsOrDet())
          transmitRelativeDatePhrase();
        return   null ;
      }

   else  if   (LexiconUtil.isMembOfArray( thisWord ,  wordSet_After$Before$Fr_Etc ) &&
          looksLikeUnitOfTime( noun ) &&
           ( determiner  !=  null ) &&
          numberOrMemb( determiner ,  wordSet_The$A$An ) &&
          determinerIsOKForDate())
      { popNpStackIfNeeded();
        return  stackNounAndStartPrep();
      }

   else  if   (nounMakesGoodTimePhrase() &&
           ( ! ( phraseIsOfTheOrdinalDay()) ))
       return  transmitRelativeDatePhrase();

   else  if   ( ( noun  ==  word_Number ) &&
          thisWord.isFormOfCat( categorySet_Number ) &&
          ((  ( modifiers  !=  null ) &&
              nodeSequenceFirstWord(modifiers).isFormOfCat( categorySet_N )) ||
            ( ( verb  !=  null ) &&
               ( determiner  ==  null ) &&
              verb.isFormOfCat( categorySet_N ))))
       return  makePostmodsFromNounAndNumber();

   else  if   (noun.isNonnameFormOfCat( categorySet_N ) &&
          noun.isPenaltyFormOfCat( categorySet_N ,  2 ) &&
          ((thisWord.isFormOfCat( categorySet_Adj$Ord ) &&
              noun.isNonpenaltyFormOfCat( categorySet_Adj$Ord$Adv )) ||
            thisWord.numeralp() ||
            thisWord.isFormOfCat( categorySet_Det$N$Number )) &&
           ( ! ( ( modifiers  !=  null ) &&
               ( ! ( allModifiersAreAdj()) ))) &&
          (thisWord.isNonpenaltyFormOfCat( categorySet_Det ) ||
            (noun.isFormOfCat( categorySet_Adj$Ord ) &&
               ( ! ( noun.isNonpenaltyFormOfCat( categorySet_Adv )) ))))
      if    (thisWord.isNonpenaltyFormOfCat( categorySet_Det ))
          return  popNpStack();

      else   return  reinterpretNounAsMod();

   else  if    ( thisWord  ==  word_$S )
      { popNpStackIfNeeded();
       assimilateVerbToModifiersIfPossible();
        determiner  = (Value)( new PossPhrase((Phrase)(assembleNounPhrase( determiner ,  modifiers ,  noun ,  postmods ))));
       clearNounPartsExceptDet();
        return   npAfterDet ;
      }

   else  if   (LexiconUtil.isMembOfArray( noun ,  wordSet_Can$Do$Does$Wil_Etc ) &&
          thisWord.isRootOfCat( categorySet_V ) &&
          ( ( ! ( ( determiner  !=  null ) &&
                allModifiersAreAdj())) ||
            ( ( determiner  !=  null ) &&
               ( modifiers  ==  null ) &&
              wordIsFormOfCat( determiner ,  categorySet_Pro ) &&
               ( ! (LexiconUtil.isMembOfArray( noun ,  wordSet_Do$Does ) &&
                  nounAgreesWithDet( noun ,  determiner ))))))
       return   null ;

   else  if   ( ( thisWord  ==  word_Of ) ||
          ( sp_includeConjunctions  &&
        		  LexiconUtil.isMembOfArray( thisWord ,  wordSet_And$Or )))
      { if   (( ( modifiers  !=  null ) ||
                 ( postmods  !=  null )) &&
               ( ! ( phraseIsOfTheOrdinalDay()) ))
          popNpStackTop();
        return  stackNounAndStartPrep();
      }

   else  if   ( ( noun  ==  word_Way ) &&
           ( determiner  !=  null ) &&
          determiner.equals( word_The ) &&
           ( modifiers  ==  null ) &&
          thisWord.isFormOfCat( categorySet_Npl$Nm$Pro$Det$_Etc$ ) &&
           ( postmods  ==  null ))
       return   null ;

   else  if   (noun.isKindOf( word_$Nc$Relative$Pe_Etc ) &&
           ( determiner  !=  null ))
       return  popNpStack();

   else  if    (thisWord.getdict( feature_Breakword ) !=  null )
       return  popNpStack();

   else  if   ((thisWord.isFormOfCat( categorySet_Number ) ||
            wordClass.equals( category_Singleletter )) &&
           ( ! ( noun.isPenaltyFormOfCat( categorySet_N ,  2 )) ))
      { popNpStackIfNeeded();
        tempPhraseStart  =  refcount ;
        tempPhraseEnd  =  previousEnd ;
        postmods  = nodeSequencePushNode( postmods ,  thisWord );
        return   npAfterPostmod ;
      }

   else  {  boolean  tempboolean1082 ;
         boolean  tempboolean1083 ;
         boolean  tempboolean1084 ;
         boolean  tempboolean1085 ;
        if    (  tempboolean1083  = (breakwordTest( thisWord ) &&
                ( thisWord  !=  word_Of )) )
            tempboolean1082  =  tempboolean1083 ;

        else  if   (  tempboolean1084  = (thisWord.looksLike( category_Prep ) &&
                ( thisWord  !=  word_Of )) )
            tempboolean1082  =  tempboolean1084 ;

        else  if   (  tempboolean1085  = LexiconUtil.isMembOfArray( thisWord ,  wordSet_Be$But$For ) )
            tempboolean1082  =  tempboolean1085 ;

        else  if   (looksLikeUnit( noun ))
           {  Value  mod ;
             Value  tempValue1086 ;
            if     ( modifiers  ==  null )
               if     ( determiner  !=  null )
                   tempValue1086  =  determiner ;

               else   tempValue1086  =  null ;

            else   tempValue1086  =  null ;
            if     ( tempValue1086  !=  null )
                mod  =  tempValue1086 ;

            else   mod  = nodeSequenceFirst( modifiers );
             tempboolean1082  = ( ( mod  !=  null ) &&
              mod.wordp() &&
              ((Word) mod).isFormOfCat( categorySet_Number ) &&
              nounAgreesWithDet( noun ,  mod ));
           }

        else   tempboolean1082  =  false ;
        if    ( tempboolean1082 )
            return  popNpStack();

        else  if   ( ( ! ( thisWord.isFormOfCat( categorySet_Number )) ) &&
               (LexiconUtil.isMembOfArray( thisWord ,  sp_extremelyUnlikelyNames ) ||
                 (thisWord.isFormOfCat( categorySet_Prespart ) &&
                    ( ! ( thisWord.isPenaltyFormOfCat( categorySet_V )) ))))
            return  popNpStack();

        else  if   (thisWord.isFormOfCat( categorySet_Firstname$Lastn_Etc ) &&
                ( nounState  ==  null ))
            return  startParallelPhraseFromNpAfterNoun();

        else  if   (thisWord.isFormOfCat( categorySet_Det$Number ) &&
               noun.isPenaltyFormOfCat( categorySet_N ,  2 ))
            return  popNpStack();

        else  if   (thisWord.isFormOfCat( categorySet_Adj$Post ))
           { popNpStackIfNeeded();
             postmods  = nodeSequencePushNode( postmods ,  thisWord );
             return   popNpStack ;
           }

        else  if   (canBeAdvQualifier( thisWord ))
           { popNpStackIfNeeded();
             modifiers  = nodeSequencePushNode( modifiers ,  noun );
             noun  =  null ;
             adverbs  =  new Value[] { thisWord };
             tempPhraseStart  =  refcount ;
             tempPhraseEnd  =  previousEnd ;
             return   npAfterAdv ;
           }

        else  if   (thisWord.isFormOfCat( categorySet_N ) &&
                ( ! ( thisWord.isFormOfCat( categorySet_Number )) ) &&
                ( ! ( wordClass.equals( category_Singleletter )) ) &&
                ( ! ( thisWord.looksLike( category_Adv )) ))
           { popNpStackIfNeeded();
             modifiers  = nodeSequencePushNode( modifiers ,  noun );
             noun  =  null ;
            if    ( ( determiner  !=  null ) &&
                    ( ! ( LexiconUtil.isMembOfArray( determiner ,  wordSet_Both$Each$La$Le_Etc )) ) &&
                    ( ! ( nounAgreesWithDet( thisWord ,  determiner )) ))
               {  modifiers  = nodeSequencePushNode( modifiers ,  thisWord );
                 return   npAfterAdj ;
               }

            else  {  noun  =  thisWord ;
                  return   npAfterNoun ;
                }
           }

        else  if   (thisWord.isFormOfCat( categorySet_Det$Number$Prep ))
            return  popNpStack();

        else  if   (thisWord.isFormOfCat( categorySet_N ))
           { popNpStackIfNeeded();
             modifiers  = nodeSequencePushNode( modifiers ,  noun );
             noun  =  thisWord ;
             return   npAfterNoun ;
           }

        else  if   (thisWord.isFormOfCat( categorySet_Adj$Ord ) &&
               allModifiersAreAdj() &&
               noun.isFormOfCat( categorySet_Adj$Ord ))
           { popNpStackIfNeeded();
            addNounAndThisWordToModifiers();
             return   npAfterAdj ;
           }

        else  if   (thisWord.isFormOfCat( categorySet_Adj ))
            return  popNpStack();

        else   return  popNpStack();
       }
 }


 void addNounAndThisWordToModifiers() {
  /* action from npAfterNoun and wordIsAdjForParallaleHypothesis */
    modifiers  = nodeSequencePushNode(nodeSequencePushNode( modifiers ,  noun ),  thisWord );
    noun  =  null ;
 }


 State npAfterAn() {
  /* handles cases like 'three dollars an hour [ago]'  */
   if    (sentencePunctOrBreakword( wordSet_Of ))
      { popNpStackTop( tempPhraseEnd );
        return   null ;
      }

   else  if   (nounAgreesWithDet( thisWord ,  word_An ) &&
          canBeSingularNonnameNounOrUnit())
      {  postmods  = nodeSequencePushNode( postmods , (Value)( new PrepPhrase( word_An , (NominalPhrase)(createTrivialNounPhrase( thisWord )))));
        return   npAfterPostmod ;
      }

   else  if   (falsePostmodIsDeterminer( wordSet_An ))
       return  salvageFalsePostmod();

   else  {  determiner  =  word_An ;
         nounstack  =  null ;
        clearNounVerbPartsExceptDet();
        promoteTempPhrase();
         return  npAfterDet();
       }
 }


 boolean falsePostmodIsDeterminer( Word[]  detlist ) {
  /*  test used in npAfterAn and similar spot in npAfterPostmods  */
    return  (thisWord.isFormOfCat( categorySet_N ) &&
           numberOrMemb(nodeSequenceFirst( postmods ),  detlist ) &&
           nounAgreesWithDet( thisWord , nodeSequenceFirst( postmods )));
 }


 boolean falsePostmodNumber() {
  /* test used in npAfterPostmod very much like general false postmod */
    return  (thisWord.isFormOfCat( categorySet_Number ) &&
           (nodeSequenceFirst(postmods).wordp() &&
             (numberOrMemb(nodeSequenceFirst( postmods ),  wordSet_A ) ||
               ((Word) nodeSequenceFirst(postmods)).isKindOf( word_Number ))));
 }


 State salvageFalsePostmod() {
  /*  action from npAfterAn and npAfterPostmod  */
    Value  falsemod  = nodeSequenceFirst( postmods );
    postmods  = nodeSequenceRest( postmods );
   if    (noun.isFormOfCat( categorySet_V ) &&
           ( postmods  ==  null ) &&
           ( modifiers  ==  null ) &&
           ( verb  ==  null ))
      {  Word  newverb  =  noun ;
       if     ( nounstack  !=  null )
          popNpStack( tempPhraseEnd );
        verb  =  newverb ;
      }

   else   verb  =  null ;
    determiner  =  falsemod ;
   promoteTempPhraseThisWordIsNoun();
    return   npAfterNoun ;
 }


 void promoteTempPhraseThisWordIsNoun() {
   promoteTempPhrase();
    noun  =  thisWord ;
    modifiers  =  null ;
    adverbs  =  null ;
    postmods  =  null ;
 }


 void promoteTempPhrase() {
    phraseStart  =  tempPhraseStart ;
   clearTempPhrase();
 }

/* prelude */

 boolean sentencePunctOrBreakword() {
  /* short call form */
    return  sentencePunctOrBreakword( null );
 }


 boolean sentencePunctOrBreakword( Word[]  exceptions ) {
  /* test used in npAfterPostmod, npAfterAn, npAfterCity, npAfterAdv, 
   npAfterDet, and npAfterVerb. 
   Basically asks if any of the hardest breaks are happening here? */
    return  (prevBreak() ||
           (breakwordTest( thisWord ) &&
              ( ! ( LexiconUtil.isMembOfArray( thisWord ,  exceptions )) )));
 }


 State npAfterPostmod() {
   if    (sentencePunctOrBreakword( wordSet_Of ))
       return  popNpStack();

   else  if    ( thisWord  ==  word_Of )
      { popNpStackTop();
       clearTempPhrase();
        return  stackNounAndStartPrep();
      }

   else  if   (falsePostmodNumber())
       return  popNpStack();

   else  if   (nodeSequenceFirst(postmods).equals( word_A ) &&
          canBeSingularNonnameNounOrUnit())
      {  postmods  = nodeSequenceRest( postmods );
        postmods  = nodeSequencePushNode( postmods , (Value)( new PrepPhrase( word_A , (NominalPhrase)(createTrivialNounPhrase( thisWord )))));
        return   popNpStack ;
      }

   else  if   (falsePostmodIsDeterminer( wordSet_A ))
       return  salvageFalsePostmod();

   else  if   ( ( postmods  !=  null ) &&
          numberOrMemb(nodeSequenceFirst( postmods ),  wordSet_A ) &&
          nounAgreesWithDet( thisWord , nodeSequenceFirst( postmods )) &&
          looksLikeUnit( thisWord ))
       return   null ;

   else  if   ( ( postmods  !=  null ) &&
           (nodeSequenceRest( postmods ) ==  null ) &&
          numberOrMemb(nodeSequenceFirst( postmods ),  wordSet_A ) &&
          thisWord.isFormOfCat( categorySet_Adv$Adj$N ) &&
           ( ! ( LexiconUtil.isMembOfArray( noun ,  sp_postmodNouns )) ))
      {  determiner  = nodeSequenceFirst( postmods );
       promoteTempPhrase();
       clearNounVerbPartsExceptDet();
        return  npAfterDet();
      }

   else  if   ( ( preposition  !=  null ) &&
           ( determiner  ==  null ) &&
           ( postmods  !=  null ) &&
          looksLikeTime((Word)(nodeSequenceFirst( postmods ))))
       return   null ;

   else  if   (thisWord.isFormOfCat( categorySet_N ))
      { while    ( postmods  !=  null )

           {  modifiers  = nodeSequencePushNode( modifiers , (Value)(makeModWord( new Value[] { noun , nodeSequenceFirst( postmods )})));
            postmods  = nodeSequenceRest( postmods );
          }
        noun  =  thisWord ;
       clearTempPhrase();
        return   npAfterNoun ;
      }

   else   return  popNpStack();
 }


 void setNameCapcode( Value  thecapcode ) {
   if     ( thecapcode  ==  capcode_Uc )
       ucName  =  true ;
   if     ( thecapcode  ==  capcode_Lc )
       lcName  =  true ;
 }


 void cancelNameCodeHypothesis() {
    ucName  =  false ;
    lcName  =  false ;
 }


 void clearLocationParts() {
    city  =  null ;
    statename  =  null ;
    country  =  null ;
    postalCode  =  null ;
 }


 void clearNameParts() {
    firstname  =  null ;
    lastname  =  null ;
    initials  =  null ;
    prefixtitle  =  null ;
    namesuffix  =  null ;
 }


 State stackNounAndStartPrep() {
   pushToNounStack();
    preposition  =  thisWord ;
    return   xAfterPreposition ;
 }


 void clearVerbParts() {
    adverbs  =  null ;
    determiner  =  null ;
    modifiers  =  null ;
    verb  =  null ;
 }


 void clearNounVerbParts() {
    determiner  =  null ;
   clearNounVerbPartsExceptDet();
 }


 void clearNounPartsExceptDet() {
    adverbs  =  null ;
    modifiers  =  null ;
    noun  =  null ;
    postmods  =  null ;
 }


 void clearNounVerbPartsExceptDet() {
   clearNounPartsExceptDet();
    verb  =  null ;
 }


 void clearNounStateVariables() {
    preposition  =  null ;
   clearNounVerbParts();
 }


 void cancelNounState() {
   if     ( nounState  !=  null )
      { if   ( scantrace )
          scantraceFile.print( "                cancelling state " + nounState + "\n" );
       clearNounVerbParts();
        nounState  =  null ;
      }
 }


 void cancelNounStateWithoutClearing() {
  /* written this way so we could produce different trace if desired */
   if     ( nounState  !=  null )
      { if   ( scantrace )
          scantraceFile.print( "                cancelling state " + nounState + "\n" );
        nounState  =  null ;
      }
 }


 State makeCityIntoDeterminer() {
  /* action for apostropheS in npAfterCity and cityAfterCountry */
   if    (( ( priorCapcode  ==  capcode_Ic ) &&
             ( !  icText ) &&
             ( ! ( ( nounState  !=  null ) &&
                city.isNonnameFormOfCat( categorySet_N$Adj$Adv ) &&
                LexiconUtil.isInList( feature_Ic , (List)(city.getCapcodes()))))) ||
          (((   ucText  &&
                 ( priorCapcode  ==  capcode_Uc )) ||
              ( icText  &&
                 ( priorCapcode  ==  capcode_Ic ))) &&
             ( nounState  ==  null ) &&
             ( ! ( city.isNonnameFormOfCat( categorySet_N$V$Adv$Adj )) )))
      { transmit((Phrase)( new CityPhrase( city ,  statename ,  postalCode ,  country ,  phraseStart )),  10 );
       if     ( ! ( betterNotCancelNoun()) )
          cancelNounState();
      }
   if     ( nounState  !=  null )
       return   null ;

   else   return   npAfterDet ;
 }


 State npAfterCity() {
   if     ( thisWord  ==  word_$S )
       return  makeCityIntoDeterminer();

   else  if   (thisWord.isFormOfCat( categorySet_Statename$State_Etc ) &&
          ( (capcode() ==  capcode_Uc ) ||
             ( ! ( thisWord.isFormOfCat( categorySet_Statecode )) )) &&
          noPunctOrComma())
      {  statename  =  thisWord ;
        return   cityAfterState ;
      }

   else  if   ((thisWord.isFormOfCat( categorySet_Country ) ||
            thisWord.isKindOf( word_Region )) &&
          noPunctOrComma())
      {  country  =  thisWord ;
        return   cityAfterCountry ;
      }

   else  if   (sentencePunctOrBreakword( wordSet_Of ))
       return   null ;

   else  if   ( ( confidence  <  confidenceThreshold ) &&
           ( ! (thisWord.isFormOfCat( categorySet_Name ) ||
              thisWord.isFormOfCat( categorySet_Adv$Ord ) ||
              (wordClass.equals( category_Singleletter ) &&
            		  LexiconUtil.isMembOfArray(capcode(), (Value[]) featureSet_Uc$Ic )))))
       return   null ;

   else  if   (city.isFormOfCat( categorySet_Firstname ))
      {  firstname  =  city ;
       switchFromCityToNameState( nameAfterFirst );
        return  nameAfterFirst();
      }

   else  if   (city.isFormOfCat( categorySet_Lastname ))
      {  lastname  = makeSingleton( city );
       switchFromCityToNameState( nameAfterLast );
        return  nameAfterLast();
      }

   else  if   (city.isFormOfCat( categorySet_Title ))
      {  prefixtitle  =  city ;
       switchFromCityToNameState( nameAfterTitle );
        return  nameAfterTitle();
      }

   else  {  city  =  null ;
         return   null ;
       }
 }


 State switchFromCityToNameState( State  namestate ) {
  /* doesn't use funcall or set to make Java move easier */
   setNameCapcode( priorCapcode );
    city  =  null ;
   if    ( scantrace )
      scantraceFile.print( "                switching to state " + namestate + " from " + scanState + "\n" );
   {  scanState  =  namestate ;
     return   scanState ;
   }
 }


 boolean getDictAbbrev( Word  wd ) {
  /* checks whether word is known to be an abbreviation */
    return   (wd.getdict( feature_Abbrev ) !=  null );
 }


 State cityAfterState() {
   if    ((thisWord.isFormOfCat( categorySet_Country ) ||
            thisWord.isKindOf( word_Region )) &&
          (noPunctOrComma() ||
            ( (prevPunct( 1 ) ==  word_Dot ) &&
              getDictAbbrev( statename ) &&
              noPunctOrCommaAfterOnePunct())))
      {  country  =  thisWord ;
        return   cityAfterCountry ;
      }

   else   return  cityAfterCountry();
 }


 State cityAfterCountry() {
   transmit((Phrase)( new CityPhrase( city ,  statename ,  postalCode ,  country ,  phraseStart )),  10 );
   if     ( thisWord  ==  word_$S )
       return  makeCityIntoDeterminer();

   else  { clearLocationParts();
         return   null ;
       }
 }


 boolean properObjectTest() {
  /* true if proper object is allowed or this one has a common noun sense */
    return  ( ( !  sp_suppressProperObjectVerbPhrases ) ||
           noun.isNonnameFormOfCat( categorySet_N ));
 }


 boolean properHeadTest() {
  /* true if proper heads are allowed or if noun has nonName meanings */
    return  ( ( !  sp_suppressProperHeads ) ||
           noun.isNonnameFormOfCat( categorySet_N ));
 }


 boolean nounIsAcceptableHead() {
  /* test from popNpStack */
    return  (noun.isFormOfCat( categorySet_N ) &&
           properHeadTest() &&
            ( ! ( noun.isPenaltyFormOfCat( categorySet_N ,  2 )) ));
 }


 void traceTransmitBuffer() {
  /* called inside popNpStack to report actions with transmitBuffer */
   if     ( transmitbuffer  !=  null )
      printScantraceAndClear();
 }

/* prelude */

 boolean nounMakesGoodTimePhrase() {
  /* short call form */
    return  nounMakesGoodTimePhrase( false );
 }


 boolean nounMakesGoodTimePhrase( boolean  nomodsok ) {
  /*  test used in npAfterNoun and popNpStack  */
   if    (looksLikeUnitOfTime( noun ) &&
          ( ( determiner  ==  null ) ||
            nounAgreesWithDet( noun ,  determiner )))
      {  boolean  tempboolean1087 ;
        boolean  tempboolean1088 ;
       if    (  tempboolean1088  = numberOrMemb( determiner ,  wordSet_A$An$Several$Th_Etc$ ) )
           tempboolean1087  =  tempboolean1088 ;

       else  if   ( ( determiner  ==  null ) &&
              noun.isFormOfCat( categorySet_Nsg ))
          {  boolean  tempboolean1089 ;
           if    (  tempboolean1089  = (LexiconUtil.isMembOfArray(nodeSequenceFirst( modifiers ),  wordSet_Next$Last ) &&
                   (nodeSequenceRest( modifiers ) ==  null )) )
               tempboolean1087  =  tempboolean1089 ;

           else  if   ( nomodsok )
               tempboolean1087  = ( ( modifiers  ==  null ) &&
            		   LexiconUtil.isMembOfArray( verb ,  wordSet_Next$Last ));

           else   tempboolean1087  =  false ;
          }

       else   tempboolean1087  =  false ;
       if    ( tempboolean1087 )
           return  determinerIsOKForDate();

       else   return   false ;
      }

   else   return   false ;
 }


 boolean determinerIsOKForDate() {
  /*  test used in npAfterNoun and nounMakesGoodTimePhrase 
    to rule out 'a friday' and 'the 9 years'   */
   {  boolean  tempboolean1090 ;
    if    (LexiconUtil.isMembOfArray( determiner ,  wordSet_A$An ))
       {  tempboolean1090  =  false ;
        if     ( modifiers  !=  null )
           for ( int  i  =  0 ;  i  <  modifiers.length ;  i++ ) {
               Value  x  =  modifiers[i] ;
              { if    (x.wordp() &&
                       ((Word) x).isFormOfCat( categorySet_Weekday ))
                   {  tempboolean1090  =  true ;
                     break ;
                   }
              }
           }
       }

    else   tempboolean1090  =  false ;
    if     ( !  tempboolean1090 )
        return   ( ! ( ( determiner  !=  null ) &&
                 determiner.equals( word_The ) &&
                 mustBePluralNoun( noun )));

    else   return   false ;
   }
 }

/* prelude */

 void popNpAfterVerb() {
  /* short call form */
   popNpAfterVerb( null );
 }


 void popNpAfterVerb( Integer  phraseend ) {
  /* all the actions that popNpStack does when both the verb and noun 
  registers are set */
   if    ((verb.isNonnameFormOfCat( categorySet_Adj$N ) ||
            verb.looksLike( category_Npr )) &&
          nounIsAcceptableHead() &&
           ( determiner  ==  null ) &&
           ( ! ( ( modifiers  !=  null ) &&
               ( ! ( verb.isFormOfCat( categorySet_Adj )) ) &&
               ( ! (nodeSequenceFirst(modifiers).wordp() &&
                  ((Word) nodeSequenceFirst(modifiers)).isFormOfCat( categorySet_N ))))))
      { transmit((Phrase)(assembleNounPhrase( determiner ,  modifiers ,  noun ,  postmods ,  verb ,  verbPhraseStart )),  confidence ,  false ,  phraseend );
       traceTransmitBuffer();
      }
   if    (( ( modifiers  !=  null ) ||
             ( postmods  !=  null )) &&
          nounIsAcceptableHead())
      { transmit((Phrase)(assembleNounPhrase( determiner ,  modifiers ,  noun ,  postmods ,  null ,  postVerbPhraseStart )),  confidence ,  false ,  phraseend );
       traceTransmitBuffer();
      }
   if    (verb.isNonpenaltyFormOfCat( categorySet_V ) &&
           ( ! ( ( determiner  ==  null ) &&
               ( ! ( canBePluralOrMass( noun )) ))) &&
           ( ! ( noun.isPenaltyFormOfCat( categorySet_N ,  2 )) ) &&
          properObjectTest())
      transmit((Phrase)( new VerbPhrase( verb , assembleNounPhrase( determiner ,  modifiers ,  noun ,  postmods ),  verbPhraseStart )),  confidence ,  false ,  phraseend );
 }


 void popNpGeneralCase() {
  /*  popNpStack action for transmitting an np with an okay determiner */
    Word  tempWord1091 ;
   if    (noun.isFormOfCat( categorySet_V$Nm))
      if    ( ( determiner  !=  null ) &&
              ( !  ( determiner  instanceof  PossPhrase )) &&
             mustBeCountDeterminer((Word) determiner ))
          tempWord1091  = makeNovelNounSense( noun ,  category_Nc );

      else  if   ( ( determiner  !=  null ) &&
              ( !  ( determiner  instanceof  PossPhrase )) &&
              ( ! ( noun.isFormOfCat( categorySet_Npl )) ) &&
             mustBeMassOrPluralDeterminer((Word) determiner ))
          tempWord1091  = makeNovelNounSense( noun ,  category_Nm );

      else  if   (noun.isFormOfCat( categorySet_Nm ) &&
              ( ! ( noun.isFormOfCat( categorySet_Nc )) ))
          tempWord1091  = noun.makeSenseName( category_Nm );

      else   tempWord1091  = noun.makeSenseName( category_N );

   else   tempWord1091  =  noun ;
   transmit((Phrase)(assembleNounPhrase( determiner ,  modifiers ,  tempWord1091 ,  postmods ,  null ,  phraseStart )),  confidence );
 }


 Word makeNovelNounSense( Word  noun ,  Category  ntype ) {
  /*  called from popNpGeneralCase when novel sense of a noun is required */
   if     ( ! ( noun.isFormOfCat( ntype )) )
      System.out.print( "warning -- " + noun.getWordString() + " is not known to be " + ntype.toString() + "\n" );
    return  noun.makeSenseName( ntype );
 }


 NominalPhrase makeNominalPhrase( Word  extramod ) {
  /* renaming of Bills's make-this-constituent() makes up a constituent package
  for the current constituent for inclusion in a prepositional phrase.
  To use as a replacement for makeThisConstituent, pass the VERB register
  as the extraMod argument */
   if    ( ( determiner  !=  null ) ||
           ( modifiers  !=  null ) ||
           ( postmods  !=  null ) ||
           ( extramod  !=  null ))
       return  (NominalPhrase)(assembleNounPhrase( determiner ,  modifiers ,  noun ,  postmods ,  extramod ));

   else  if    ( city  !=  null )
       return  (NominalPhrase)( new CityPhrase( city ,  statename ,  postalCode ,  country ));

   else  if   ( ( prefixtitle  !=  null ) ||
           ( firstname  !=  null ) ||
           ( initials  !=  null ) ||
           ( lastname  !=  null ) ||
           ( namesuffix  !=  null ))
       return  (NominalPhrase)( new NamePhrase( prefixtitle ,  firstname ,  initials ,  lastname ,  namesuffix ));

   else  if   ( ( weekday  !=  null ) ||
           ( month  !=  null ) ||
          intContainsValue( day ) ||
          intContainsValue( year ))
       return  (NominalPhrase)( new DatePhrase( weekday ,  month ,  day ,  year ));

   else  if    ( noun  !=  null )
       return  (NominalPhrase)(createTrivialNounPhrase( noun ));

   else   return   null ;
 }

/* prelude */

 State popNpStack() {
  /* short call form */
    return  popNpStack( null );
 }


 State popNpStack( Integer  phraseend ) {
  /* process the stacked noun phrases, and clear regs for new noun work  */
   transmitPoppingNpStack( phraseend );
   popNpStackTop( phraseend );
    weekday  =  null ;
   clearNounStateVariables();
    return   null ;
 }

/* prelude */

 void popNpStackTop() {
  /* short call form */
   popNpStackTop( null );
 }


 void popNpStackTop( Integer  phraseend ) {
  /* process just the top one of the stacked noun phrases.
  This same functionality was formerly done with (popNpStack 'nonfinal) */
   if    ( ( firstname  !=  null ) ||
           ( lastname  !=  null ) ||
           ( prefixtitle  !=  null ) ||
           ( namesuffix  !=  null ))
       confidence  = Math.max( confidence ,  6 );

   else   confidence  =  10 ;
   if    ( sp_suppressProperHeads  &&
           ( noun  !=  null ) &&
           ( noun.getWordString().length()  ==  1 ) &&
           ( ! ( noun.isFormOfCat( categorySet_Number )) ))
      { /* empty statement here */ }

   else  if   ( ( city  !=  null ) &&
           ( determiner  ==  null ) &&
           ( modifiers  ==  null ) &&
           ( postmods  ==  null ))
      transmit((Phrase)( new CityPhrase( city ,  statename ,  postalCode ,  country ,  phraseStart )),  confidence ,  false ,  phraseend );

   else  if   ( ( noun  !=  null ) &&
           ( determiner  ==  null ) &&
           sp_requireDeterminerJustification  &&
           ( postmods  ==  null ) &&
           ( ! ( canBePluralOrMass( noun )) ) &&
           ( ! (thisWord.isFormOfCat( categorySet_Name ) &&
               ( ! ( thisWord.isNonnameFormOfCat( categorySet_N$V$Adj$Adv )) ))) &&
           ( ! ((LexiconUtil.isMembOfArray(nodeSequenceFirst( modifiers ),  wordSet_Next$Last ) ||
        		   LexiconUtil.isMembOfArray( verb ,  wordSet_Next$Last )) &&
              looksLikeUnitOfTime( noun ))))
      { /* empty statement here */ }

   else  if   ( ( determiner  !=  null ) &&
           sp_checkDeterminerAgreement  &&
           ( ! ( ( noun  !=  null ) &&
              nounAgreesWithDet( noun ,  determiner ))))
      { /* empty statement here */ }

   else  if   ( ( noun  !=  null ) &&
          nounMakesGoodTimePhrase( true ))
      { if   ( ( modifiers  ==  null ) &&
    		  LexiconUtil.isMembOfArray( verb ,  wordSet_Next$Last ))
          {  modifiers  =  new Value[] { verb };
            verb  =  null ;
          }
       transmitRelativeDatePhrase();
      }

   else  if   ( ( verb  !=  null ) &&
           ( noun  !=  null ))
      popNpAfterVerb();

   else  if   ( ( noun  !=  null ) &&
          ( ( determiner  ==  null ) ||
        		  LexiconUtil.isMembOfArray( determiner ,  wordSet_Both$Each$La$Le_Etc$ ) ||
            ( ( noun  !=  null ) &&
              nounAgreesWithDet( noun ,  determiner ))) &&
          properHeadTest() &&
          ( ( modifiers  !=  null ) ||
             ( postmods  !=  null ) ||
             ( determiner  instanceof  PossPhrase ) ||
            ( ( noun  !=  null ) &&
              noun.isFormOfCat( categorySet_V ) &&
               ( ! ( noun.isPenaltyFormOfCat( categorySet_N ,  2 )) ))))
      popNpGeneralCase();

   else  { /* empty statement here */ }
 }

/* prelude */

 State transmitRelativeDatePhrase() {
  /* short call form */
    return  transmitRelativeDatePhrase( null );
 }


 State transmitRelativeDatePhrase( Integer  phraseend ) {
  /* action to record relative date phrase used in popNpStack and NpAfterNoun */
    confidence  =  8 ;
   if    ( ( modifiers  !=  null ) ||
           ( postmods  !=  null ))
      transmit((Phrase)(assembleRelativeDatePhrase( weekday ,  determiner ,  modifiers ,  noun ,  postmods ,  null ,  phraseStart )),  confidence ,  false ,  phraseend );
    return   null ;
 }


 boolean noNameHypothesis() {
  /* true when there's not currently a name hypothesis */
    return   ( ! ( ( firstname  !=  null ) ||
              ( lastname  !=  null ) ||
              ( prefixtitle  !=  null ) ||
              ( initials  !=  null ) ||
              ( namesuffix  !=  null )));
 }


 boolean wordIsNumberOrSingleletter() {
  /* test used in npAfterVerb */
    return  (thisWord.isFormOfCat( categorySet_Number ) ||
           wordClass.equals( category_Singleletter ));
 }


 State npAfterVerb() {
  /*  the state reached when starting a potential noun/verb phrase with a verb. */
    postVerbPhraseStart  =  refcount ;
   if    (sentencePunctOrBreakword( wordSet_Of ))
      {  verb  =  null ;
        return   null ;
      }

   else  if   (wordIsNumberOrSingleletter())
      if    ( ( ! ( LexiconUtil.isMembOfArray( verb ,  sp_postmodNouns )) ) &&
             ( ( thisWord  ==  word_A ) ||
               (thisWord.isFormOfCat( categorySet_Number ) &&
                 thisWord.numeralp())))
         {  determiner  =  thisWord ;
           return   npAfterDet ;
         }

      else  if   (verb.isFormOfCat( categorySet_N ) &&
              ( ! ( verb.isPenaltyFormOfCat( categorySet_N ,  2 )) ))
          return  verbBecomesNounWithThisWordPostmod();

      else  if   (thisWord.isFormOfCat( categorySet_Det$Number ) ||
             thisWord.generalNumberp())
         {  determiner  =  thisWord ;
           return   npAfterDet ;
         }

      else  {  modifiers  =  new Value[] { thisWord };
            return   npAfterAdj ;
          }

   else  if   ( ( thisWord  ==  word_Of ) &&
          verb.isFormOfCat( categorySet_N ))
      {  noun  =  verb ;
        postmods  =  null ;
        verb  =  null ;
        return  stackNounAndStartPrep();
      }

   else  if   (thisWord.isFormOfCat( categorySet_Adv$Qualifier ))
      {  adverbs  =  new Value[] { thisWord };
        return   npAfterAdv ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Month ) &&
          npAfterVerbIsOnlyHypothesis())
      { initializeParallelNounPhrase( npAfterVerb );
        return  monthStart();
      }

   else  if   (thisWord.isFormOfCat( categorySet_City ) &&
          npAfterVerbIsOnlyHypothesis())
      { initializeParallelNounPhrase( npAfterVerb );
        city  =  thisWord ;
        return   npAfterCity ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Firstname ) &&
          npAfterVerbIsOnlyHypothesis())
      { initializeParallelNounPhrase( npAfterVerb );
        firstname  =  thisWord ;
       setNameCapcode(capcode());
        return   nameAfterFirst ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Lastname ) &&
          npAfterVerbIsOnlyHypothesis())
      { initializeParallelNounPhrase( npAfterVerb );
        lastname  = makeSingleton( thisWord );
       setNameCapcode(capcode());
        return   nameAfterLast ;
      }

   else  if   ( ( thisWord  ==  word_Of ) &&
          verb.isFormOfCat( categorySet_N ))
      { stackNounAndStartPrep();
        return   xAfterPreposition ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Det ))
      {  determiner  =  thisWord ;
       if     ( ! ( verb.isFormOfCat( categorySet_Vt )) )
           verb  =  null ;
        return   npAfterDet ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_N ))
      {  noun  =  thisWord ;
        return   npAfterNoun ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Adj$N ))
      {  modifiers  =  new Value[] { thisWord };
        return   npAfterAdj ;
      }

   else  if   (thisWord.isFormOfCat( categorySet_Adv ))
       return   npAfterVerb ;

   else  {  verb  =  null ;
         return   null ;
       }
 }


 boolean npAfterVerbIsOnlyHypothesis() {
  /* test called from npAfterVerb */
    return  ( ( scanState  ==  npAfterVerb ) &&
           noNameHypothesis());
 }

/// this fragment is fixedHandWork.jav, derived from the 
/// rough translations of the transHandWork.lisp files
//// containing some hand-massaged code, and a latest
//// version of transmit adjusted for the PipeLine model
/////
/// 2oct01 moved to run outside the Lexicon
/// 1aug01 plus a start function for externmal use... pmartin 
// 31jul01 pmartin transmit version changed
////



String[] stateVariablesNames = {
 /*list of the names of the ATN state variables.
   Note that this is the order we print and save them in.*/
    // int
 "confidence", "year", "day", "hour", "minutes", "seconds", 
   // double
 "firstnum", "othernum", "secondnum",
    // boolean 
 "expectYear", "expectSeconds", "lcname", "ucname",  "plural",
    // Integer
  "firstnumEnd", "secondnumStart",  "phraseStart", "altPhraseStart",
  "tempPhraseStart", "tempPhraseEnd", "verbPhraseStart",  "postVerbPhraseStart", 
    // Word  
 "weekday", "daytime", "timezone", "timeunit", "dateSeparator", "numberword",
 "preposition", "firstname", "prefixtitle", 
 "currency", "noun", "verb", 
 "city", "statename", "country", "postalCode",
    // Value 
  "determiner", "month", 
    // Vector
 "nounStack",
    // Word[] 
 "lastname", "initials", "namesuffix",
    // Value[]
 "modifiers", "adverbs", "postmods"
 };

public void initializePhraseExtractor(){ // run after loading basic dicts!
    freshStartIndexer();
    resetNames();
    initializeCharacterArray();
    installGrammarWordProps();
    simplePhraseLexSetup();
    installStartFuns();
}


void resetNames(){ // made up ... gotta call it somewhere
  goodFirstNames = new Vector(); //each of these is filled with Words
  goodLastNames = new Vector();
  guessedNames = new Vector();
}


State transmit( Phrase  x ) {
   return  transmit( x ,  confidence ,  false , null );
 }

State transmit( Phrase  x ,  int  confidence ) {
    return  transmit( x ,  confidence ,  false , null);
 }

 State transmit( Phrase  x ,  int  confidence ,  boolean  usedcurrent ) {
  return  transmit( x ,  confidence ,  usedcurrent , null);
 }

//  State transmit( Phrase  x ,  int  confidence ,  boolean  usedcurrent,
// 		 Integer phraseEnd) {
//      Integer realStart, realEnd;

//      if (phraseEnd != null) realEnd = phraseEnd;
//      else if (usedcurrent) realEnd = end;
//      else realEnd = previousEnd;


//      /* beg is where the scanning process started following transitions, but
//        phrase-start is where the gramar says the current phrase actually 
//        started */
//      if (phraseStart != null) realStart = phraseStart;
//      else realStart = beg;

//   /* transmit (x &optional confidence usedCurrent) transmits a found phrase x.
//    *  The flag usedCurrent is true if the current word was used
//    *  as part of the phrase, false if curent word served only to delimit
//    *  the end of it. */
//    if ( usedcurrent ) { usedThisWord  =  true ;}
//    //  usedThisWord tells foundWord whether the word is available to start a new phrase
//    transmitbuffer  =  x ;
//    boolean printed = false;
//    Word[] newWords = untransmittedWords(x);
//    String csString = csStr(x);
//    int xindex = 0;
//    if (realStart != null) xindex = realStart.intValue();
//    if  (confidence > confidenceThreshold){
//      if ( !(scantrace || sp_suppressPrintout)){
//        scantraceFile.println( "\n" + "******  found " + x.printString() + " ******" + "\n" + "\n" );
//        printed = true;
//      }else {
//          System.out.println( "\n" + "******  found " + x.printString() +
// 			     " ******" + "\n" + "\n" ); 
// 	 // System.out.println("conceptStore = " + csString + " at " + xindex +
// 	 //			" with new words " + printStringArray(newWords));
//      }
//      if (csReceiver != null) csReceiver.receive(csString, xindex, newWords);

//    }else{
//     if (!printed)  System.out.println( "\n" + "***** no conf " + x.printString() + " ******" + "\n" + "\n" );
//      }
//    return   null ;
//  }

    /**  THIS VERSION FOR Pipeline
     *
     * transmit (x &optional confidence usedCurrent) transmits a found
     * phrase x.  The flag usedCurrent is true if the current word was used
     * as part of the phrase, false if curent word served only to delimit
     * the end of it.
     */
 State transmit(Phrase  x ,  int  confidence ,  boolean  usedcurrent,
		 Integer phraseEnd) {
     
     Integer realStart, realEnd;

     if (phraseEnd != null) {
	 realEnd = phraseEnd;
     } else if (usedcurrent) {
	 realEnd = end;
     } else {
	 realEnd = previousEnd;
     }

     /*
       beg is where the scanning process started following transitions, but
       phrase-start is where the gramar says the current phrase actually 
       started
     */
     if (phraseStart != null) {
	 realStart = phraseStart;
     } else {
	 realStart = beg;
     }

   if(usedcurrent) {
       usedThisWord  =  true ;
   }

   if(confidence > confidenceThreshold) {
	   //TODO
	   /*

       if(outputStage != null) {
	   outputStage.processData(DocumentEvent.DE_PHRASE,
				   0,
				   realStart.intValue(),
				   realEnd.intValue(),
				   x);
       } else if(outputPipe != null) {
	 outputPipe.add(new DocumentEvent(DocumentEvent.DE_PHRASE,
					  0,
					  realStart.intValue(),
					  realEnd.intValue(),
					  x));
     } else{
	 System.out.println( "No pipe.  Phrase: " +
			     x.printString() +
			     "\n");
     }
   */}
   return null ;
 }


Word parsePunct(Word  punct ,  String  file ,  int  position, boolean finalFlg ) {
  /* parsePunct (punct) is the function used to process
   punctuation marks. */
   finalFlag  =  finalFlg ; // replacement for binding by index-file .. pmartin 1dec99
   if (punct.equals( word_Space ))  //most common case tested first
      { punctbuffer.vectorPush(punct);
       if    ( (LexiconUtil.isMembOfArray(priorInput, wordSet_Charset_Backsla_Etc) ) ||
              (priorInput == word_2cr))
          {  priorInput  =  word_Indent ;
           foundPunct( word_Indent );
          }
       else  if   (priorInput == word_Indent )
	 { /* empty statement here */ } //leave it at indent
       else   priorInput  =  punct ;
      }

   else  if  ( (punct == word_Backslashn ) &&
	       (priorInput == word_Backslashr ) )
     { /* empty statement here */ } //filter out lf from cr+lf convention

   else  if  ( (punct == word_Backslashr ) &&
	       (priorInput == word_Backslashn ))
     { /* empty statement here */ } //filter out cr from lf+cr convention

   else  if   (punct ==  word_Backslashu0010 ) //indexFile convention for paragraph break punct
       sbreakFlag  =  true ;

   else  if   (punct == word_Backslashf)
      { punctbuffer.vectorPush(word_Backslashf);
        priorInput  =  word_Backslashf ;
      } // don't count page breaks as paragraph breaks or as counting for double cr

   else  if   (LexiconUtil.isMembOfArray(punct, wordSet_Charset_Backsla_Etc ))
      { punctbuffer.vectorPush(word_Backslashn);
       if    ((LexiconUtil.isMembOfArray(priorInput, wordSet_Charset_Backsla_Etc )) ||
              (priorInput ==  word_Indent ))
          { processRestOfWordQueue();
	  priorInput  =  word_2cr ; //keep track of multiple newlines
            foundPunct( word_2cr );
          }

       else  if   (priorInput == word_2cr )
	 { /* empty statement here */ } // do nothing

       else   priorInput  =  punct ;
      }

   else  if   (punct == word_Dot) // keep track of sentence breaks
      { punctbuffer.vectorPush(punct);
      if    ( (punctbuffer.fillPointer() ==  1 ) && // this is the first punct
              (categorySet_Singleletter$Nu_Etc.subsumesCategory( wordClass ) ||
	       getDictAbbrev( thisWord ) ||
	       justLettersWithPeriodsBetweenThem( thisWord )))
           priorInput  =  punct ;

       else  { processRestOfWordQueue();
             priorInput  =  word_Sbreak ;
             sbreakFlag  =  true ;
           }
      }

   else  if   (LexiconUtil.isMembOfArray(punct, wordSet_Charset_Queryba_Etc ))
      { processRestOfWordQueue();
       punctbuffer.vectorPush(punct);
        priorInput  =  word_Sbreak ;
        sbreakFlag  =  true ;
      }

   else  { if    ( ! ( whitecodep((int)(punct.getWordString().charAt( 0 )))) )
           processRestOfWordQueue();
      punctbuffer.vectorPush(punct);
         priorInput  =  punct ;
       }
   if    ( finalFlag )
      { processRestOfWordQueue();
        sbreakFlag  =  true ;
        priorInput  =  word_Sbreak ;
       parseWord( word_$Endoffile$ ,  capcode_Lc ,  false ,  file ,  position ,  position );
       warmStartIndexVars();
      }
   if    ( scantrace )
      { scantraceFile.println( " " );
      scantraceFile.println(punct.getWordString());
      }
    return   punct ;
 }


 void initializeCharacterArray() {
  /* initializeCharacterArray () sets up character array chartype
    -- used for fast testing of break, white, and punct characters. */
   int charTypeSiz = charType.length;
   for ( int  i  =  0 ;  i  <  charTypeSiz ;  i++ ) {charType[i]  =  null ;  }
   for ( int  j  =  0 ;  j  <  scanvowels.length() ;  j++ ) {
       char  x  = scanvowels.charAt( j );
       charType[(int)(Character.toUpperCase(x))]  =  atom_Vowel ;
       charType[(int)(Character.toLowerCase(x))]  =  atom_Vowel ;
   }
   for ( int  j  =  0 ;  j  <  scanconsonants.length() ;  j++ ) {
       char  x  = scanconsonants.charAt( j );
       charType[(int)(Character.toUpperCase(x))]  =  atom_Consonant ;
       charType[(int)(Character.toLowerCase(x))]  =  atom_Consonant  ;
   }
   for ( int  j  =  0 ;  j  <  digits.length() ;  j++ ) {
       char  x  = digits.charAt( j );
       charType[(int) x]  =  atom_Digit ;
   }
   for ( int  j  =  0 ;  j  <  punctuationCharacters.length() ;  j++ ) {
       char  x  = punctuationCharacters.charAt( j );
       charType[(int) x]  =  atom_Punct ;
   }
   for ( int  j  =  0 ;  j  <  whitespaceCharacters.length() ;  j++ ) {
       char  x  = whitespaceCharacters.charAt( j );
       charType[(int) x]  =  atom_White ;
   }
 }
 void restorePunctBuffer( Word[]  contents ) {
  /* restorePunctbuffer (contents) restores the state of a saved
  punctbuffer -- used in getWord to process compound words. */
    punctbuffer.restore(contents);
 }

 void processWordQueue( Vector  queue ) {
   /* ProcessWordQueue (queue) empties the word queue and processes
    * each of the words in it.  The oldest word in the queue is processed
    * without letting it consider starting possible compound phrases, but
    * all the others are treated as if they were being seen for the 
    * first time.  This processing is done when backing out of a potential
    * compound that has been partially recognized but cannot be completed.
    * ProcessWordQueue is called from ParsePunct and GetATNWord.
    * Java differs from the Lisp version in that the Word Queue has been
    * built as a tail-growing list, so no reversing is needed.
    */
     boolean processWordQueueDebug = false;
     if (processWordQueueDebug){
	 System.out.print("entered process word queue with queue = ");
	 if (queue == null){
	     System.out.println(" <null>");
	 }else{
	     System.out.println(queue.toString());
	 }
     }
   if (queue != null){
     int pendSiz = queue.size();
     WordQElt[]  pending  = new WordQElt[pendSiz];
     queue.copyInto(pending);
     WordQElt el = pending[0];
     sp_wordQueue  =  null ;
     restorePunctBuffer(el.punctbuf);
     if (processWordQueueDebug)
	 System.out.println("process word queue calling parseWord on " + el.word.printString());
     parseWord(el.word, el.capcode, el.sentencestart, el.file, el.begin, el.end, 
	       el.nextchar, el.finalp);
     for (int i = 1;  i < pendSiz; i++ ) {
       WordQElt wqe = pending[i];
       restorePunctBuffer(wqe.punctbuf);
       if (processWordQueueDebug)
	   System.out.println("process word queue calling getATNWord on " + wqe.word.printString());
       getATNWord(wqe.word, wqe.capcode, wqe.sentencestart, wqe.file, wqe.begin, wqe.end, 
		  wqe.nextchar, wqe.finalp);
     }
   }
 }


 boolean canBeNonname( Word  word ) {
  /* canBeNonname (word) tests whether word can be something other than a name. */
    return word.hasNonnameCat()  ;
 }


 boolean canBeNonname( Word[]  words ) {
  /* canBeNonname (words[]) tests whether word can be something other than a name. */
    return words[0].hasNonnameCat()  ;
 }


 Word lastWordOfSeq( Word[]  wseq ) {
  /* deals with the atom-or-list representation used for some registers.
   lastName is an example */
   return   wseq[wseq.length - 1] ;
 }


Word previousWord() {  // Lisp used a stack, but we use vector, so reversed
  /* previousWord () returns the word preceding the
  current punctuation mark.  This will be taken
  from the wordQueue. */
   if     ( sp_wordQueue  !=  null )
       return  ((WordQElt) sp_wordQueue.lastElement()).word;
   else   return   thisWord ;
 }


 Word[] punctbufferContents() {
  /* punctbufferContents () copies out the contents of punctbuffer 
  as a list of characters. */
  return punctbuffer.contents();
 }


 Category getWordClass( Word  word ) {
   /**** Lisp version could remember null wordClass .. this one can't ...
    *  so we now make AnalyzeWord produce a class named unknown to mean 
    *  "don't use a class here and don't call AnalyzeWord again" ...
    */
   Category wc = null;
   Value wcv = word.getdict(feature_Wordclass);

   if ( wcv == null ){
       wc = analyzeWordClass( word );
       word.putdict(feature_Wordclass, wc);
   } else if (wcv instanceof Category){
       wc = (Category) wcv;
   } else {
       System.out.println("getWordClass for " + word.toString() + " got a non-Category value " +
			  wcv.toString());
   }
   return  wc;
 }

/* prelude */

 void printScantrace( Phrase  foundphrase ) {
  /* short call form */
   printScantrace( foundphrase ,  confidence );
 }


 void printScantrace( Phrase  foundphrase ,  int  conf ) {
   if    ( scantrace )
      scantraceFile.print( "\n" + "******  found " + foundphrase.printString() +
			   " " + conf + " ******" + "\n" + "\n" );
 }


 void setWordClassAndFeatures(Word w) {
  /* called from parseWord ... handled differently in Java  .. we ignore wordFeatures */
     //   if ( w.numeralp()){  //pmartin 29aug00 ...special case is bad here
     //    wordClass  =  category_Number ;
     //}else 
     if  ( (wordClass  = getWordClass(w)) ==  null ){
          wordClass  =  category_Unknown ;
    }  
 }


 Word guessName( Word  name ) { //basic call form
     return guessName(name, file, refcount.intValue());
 }


 Word guessName( Word  name, String fstr, int idx ) {
  /* guessName (name) records that name is being guessed as a name. */
   if   (name.isFormOfCat( category_Lastname )){ 
     return   null ;
    }else{
     Category oldclass  = (Category)name.getdict(feature_Wordclass);
     Category newclass ;
     if (! suppressPrintout )
          scantraceFile.println( "\n" + "Guessing " + name.getWordString() + " is a lastname." + "\n" );
     name.morph();
     if (! looksLikeOrdinaryWord(name))
         name.clearWord();
     if (name.hasNonnameCat())
	 name.addWordCategory(category_Lastname ,  1 );
      else
         name.addWordCategory(category_Lastname ,  0 );
     name.markGuessedName(fstr,idx);  //pmartin 21dec00
     if    (oldclass.equals(category_Firstname ))
        newclass  =  category_Dualname ;
       else   newclass  =  category_Lastname ;
     name.putdict( feature_Wordclass ,  newclass );
     name.putdict( feature_Nameconfidence , word_8);
     name.putdict( feature_Known ,  value_T );
     guessedNames.addElement(name);
     return   name ;
  }
 }

 void recordGoodFirstname() {
   if    ( ( firstname  !=  null ) &&
           ( !  ( confidence  <  confidenceThreshold )))
      vectorPushNew(firstname,goodFirstNames);
 }


 void recordGoodLastname() {
   if    ( ( lastname  !=  null ) &&
           ( !  ( confidence  <  confidenceThreshold ))){
       Word lnWord = singleton(lastname);
       if (lnWord == null) lnWord = lex.makeWord(lastname);
       vectorPushNew(lnWord,goodLastNames);
   }
 }

 Vector vectorPush(Object item, Vector oldVect){
   oldVect.addElement(item);
   return oldVect;
 }
   
 Vector vectorPushNew(Object item, Vector oldVect){
   if (oldVect.lastIndexOf(item) == -1) {oldVect.addElement(item);}
   return oldVect;
 }
  
 public Vector reverseVector (Vector vals){
   int siz = vals.size();
   Vector newVals = new Vector(siz);
   for (int i = siz-1; i > -1; i-- ){ 
     newVals.addElement(vals.elementAt(i));
   }
   return newVals;
 } 
   
 boolean singletonNounStack() {
  /* test for npAfterNoun */
    return  ( ( nounstack  !=  null ) &&
	      ( 1  == nounstack.size()));
 }

 void pushToNounStack() {
   if (nounstack == null) nounstack = new Vector(); 
   nounstack.addElement(new NpStackElt(thisWord, determiner, modifiers, 
				       noun, postmods, verb, phraseStart));
   clearNounVerbParts();
 }

  NpStackElt popNounStack() {
      NpStackElt stackelt = null;
      if (nounstack != null) { 
	  int nsSiz = nounstack.size();
	  if (nsSiz > 0) {
	      stackelt  = ((NpStackElt) nounstack.elementAt(nsSiz-1)) ;
	      nounstack.removeElementAt(nsSiz-1);
	  }
      }
      return stackelt;
  }

 NpStackElt topOfNounStack() {
  /* returns element that would be popped first */
     if (nounstack != null) { 
	 int nsSiz = nounstack.size();
	 if (nsSiz > 0) 
	    return ((NpStackElt) nounstack.elementAt(nsSiz-1));
     }
     return null;
 }

 void transmitPoppingNpStack() {
  /* short call form */
   transmitPoppingNpStack( null );
 }


 void transmitPoppingNpStack( Integer phraseEnd ) {
   int nsSiz;
   while ( (nounstack  !=  null) && ((nsSiz = nounstack.size()) > 0) ) { 
     if   (( ( modifiers  !=  null ) ||
	       ( postmods  !=  null ) ||
	       ( determiner  !=  null )) &&
	     ( priorWord  !=  word_$S ))
	   popNpStackTop( phraseEnd ); 
     traceTransmitBuffer();
     NpStackElt  stackelt  = popNounStack();
     PrepPhrase pp = new PrepPhrase(stackelt.prep, (NominalPhrase) makeNominalPhrase(verb));
     postmods  = stackelt.postmodstack;
     postmods = nodeSequencePushNode(postmods, pp);
     determiner  =  stackelt.det ;
     modifiers  =  stackelt.modstack ;
     noun  =  stackelt.noun ;
     verb  =  stackelt.verb ;
     phraseStart  =  stackelt.phraseStart ;
   }
 }


 boolean looksLikeTime( Word  x ) {
  /* replaces timep... tests if the symbol x looks like a time.
   *  rewritten for Java pmartin 26Aug99 to match behavior of Lisp
   *  version in morphfns.lisp 
   */
   int  minutes, hour, pos, len ;
   hour = -1;
   minutes = -1;
   pos = -1;
   String  xstring = x.getWordString();
   int strlen = xstring.length();
   if  ((len  = strlen) >  3){ pos  = xstring.indexOf( ':' );  }
   if  ( (pos !=  -1) && (len == (pos + 2))){
     try {hour = Integer.parseInt(xstring.substring(0, (pos - 1)));
          minutes  = Integer.parseInt(xstring.substring(pos, strlen));
     }  catch (NumberFormatException e) {
     }
     return  (( hour  > -1 ) && ( minutes > -1 ) &&
	      ( minutes  <  60 ) && 
	      (( hour  <  24 ) ||
	       (( hour  ==  24 ) && ( minutes  ==  0 ))));
   }
   else return   false ;
 }

 NounPhrase relativeDatePhraseNounPhrase( DatePhrase  dateph ) {
    return  (NounPhrase) dateph.month ;
 }

 public boolean alphaCharP(char ch){
   return Character.isLetter(ch);
 }

 public boolean digitCharP(char ch){
   return Character.isDigit(ch);
 }
  

 public boolean patternCheckFinalS( Word  wd ) {
   {  String  str  = wd.getWordString();
     char  chr  = str.charAt(str.length() -  1 );
     return  ((chr == 'S') || (chr == 's' ));
   }
 }

 void showChangedStateVariables() {
  /* short call form */
   showChangedStateVariables( scantraceFile );
 }

 void showChangedStateVariables( PrintStream  output ) {
  /* show-changed-state-variables (&optional output) prints out the
  new contents of state variables that have changed. */
   if    ( ( sp_oldRegisterContents  !=  null ) &&
           ( sp_newRegisterContents  !=  null ))
       for (int i=0; i<stateVariablesNames.length; i++){
	   if ((sp_oldRegisterContents[i] != sp_newRegisterContents[i]) &&
	       ((sp_oldRegisterContents[i] == null) ||
		(!(sp_oldRegisterContents[i].equals(sp_newRegisterContents[i])))))
	       printStateVariable(output, i, sp_newRegisterContents[i]);
       }
 }

/* prelude */

 void showNonEmptyStateVariables() {
  /* short call form */
   showNonEmptyStateVariables( scantraceFile );
 }

 void showNonEmptyStateVariables( PrintStream  output ) {
  /* show-nonempty-state-variables (&optional output) prints out the
  contents of state variables that are nonempty. */
     Object[] sv = captureStateVariables();
     String svString;
     for (int i=0; i<stateVariablesNames.length; i++){
	 if (sv[i] != null)
	     printStateVariable(output, i, sv[i]);
	 }
 }
 
void printStateVariable(PrintStream output, int svi, Object stateV){
    String str = null;
    if (stateV == null)
	str = "null";
    else if (svi < afterIntegers) 
	str = stateV.toString();
    else if (svi < afterValues) 
	str = ((Value)stateV).printString();
    else if (svi < afterVectors)
	str = stateV.toString();
    else if (svi < afterValueArrays) 
	str = LexiconUtil.printStringArray((Value[]) stateV);

    output.println("                  " + stateVariablesNames[svi] +
		       " = " + str);
}

int afterInts, afterDoubs, afterBools, afterIntegers, afterWords, afterValues;
int afterVectors, afterWordArrays, afterValueArrays;

 Object[] captureStateVariables() {
  /* capture-state-variables () makes a list of the current
  contents of the state variables. */
     Object[] sv = new Object[stateVariablesNames.length];
     int i = 0;
     sv[i++] = new Integer(confidence);
     sv[i++] = new Integer(year);
     sv[i++] = new Integer(day);
     sv[i++] = new Integer(hour);
     sv[i++] = new Integer(minutes);
     sv[i++] = new Integer(seconds);
     afterInts = i;

     sv[i++] = new Double(firstnum);
     sv[i++] = new Double(othernum);
     sv[i++] = new Double(secondnum);
     afterDoubs = i;

     sv[i++] = new Boolean(expectYear);
     sv[i++] = new Boolean(expectSeconds);
     sv[i++] = new Boolean(lcName);
     sv[i++] = new Boolean(ucName);
     sv[i++] = new Boolean(plural);
     afterBools = i;
     
     sv[i++] = firstnumEnd; 
     sv[i++] = secondnumStart;
     sv[i++] = phraseStart;
     sv[i++] = altPhraseStart;
     sv[i++] = tempPhraseStart;
     sv[i++] = tempPhraseEnd;
     sv[i++] = verbPhraseStart;
     sv[i++] = postVerbPhraseStart;
     afterIntegers = i;
 
     sv[i++] = weekday;
     sv[i++] = dayTime;
     sv[i++] = timezone;
     sv[i++] = timeunit;
     sv[i++] = dateSeparator;
     sv[i++] = numberWord;
     sv[i++] = preposition;
     sv[i++] = firstname;
     sv[i++] = prefixtitle;
     sv[i++] = currency;
     sv[i++] = noun;
     sv[i++] = verb;
     sv[i++] = city;
     sv[i++] = statename;
     sv[i++] = country;
     sv[i++] = postalCode;
     afterWords = i;

     sv[i++] = determiner;
     sv[i++] = month;
     afterValues = i;

     sv[i++] = nounstack;
     afterVectors = i;

     sv[i++] = lastname;
     sv[i++] = initials;
     sv[i++] = namesuffix;
     afterWordArrays = i;

     sv[i++] = modifiers;
     sv[i++] = adverbs;
     sv[i++] = postmods;
     afterValueArrays = i;

     return sv;
 }

 Word makeModWord(Value node){
    /** pmartin 27nov00 for name phrases, etc.  */
    if (node instanceof Word) 
	return (Word)node;
    else if (node instanceof Phrase)
	return finishMakeModWord(makePhraseWord((Phrase)node), (Phrase)node);
    else {
	System.out.println("Error in makeModWord with Value =" + node.toString());
	return (Word)node;
    }
 }

 Word makeModWord( Value[]  nodeSeq ) {
  /* makeModWord (nodeSeq) makes a lexical entry for rest of nodeSeq
   as a modifier of the 1st word -- 
      action  used in nameAfterLast and npAfterNoun */
    Word[] wordSeq = new Word[nodeSeq.length];
    Word modword ;
    for (int i=0; i<wordSeq.length; i++){
      if (nodeSeq[i] instanceof Word){
	wordSeq[i] = (Word)nodeSeq[i];
      }else{
	wordSeq[i] = makePhraseWord((Phrase)nodeSeq[i]);
      }}
    modword  = lex.makeWord( wordSeq );
    NounPhrase nphrase  = 
      new NounPhrase( null , null ,(Word)(nodeSequenceFirst(nodeSeq)),
		      nodeSequenceRest(nodeSeq), phraseStart);
    return finishMakeModWord(modword, nphrase);
 }

Word finishMakeModWord(Word modword, Phrase ph){
    /* split from makemodword 27nov00 pmartin */
    modword.morph();
    if ( ! ( modword.isFormOfCat( categorySet_Adj$N )) )
       modword.addWordCategory( category_Adj ,  0 );
 
    if    ( scantrace  &&
            ( !  suppressPrintout ) &&
            ( sp_processPhraseFn  !=  null ))
       printScantrace( ph );
    if     ( sp_processPhraseFn  !=  null )
      {printScantrace(ph);
      /* call to  processPhraseFun of phrase */}

    return modword;
 }



String capitalizeByCapcode( String  wstring ,  Value  capcode ) {
  /**** capitalize-by-capcode (wstring capcode) makes
   * an appropriately capitalized string according
   * to capcode. Hand mapped 9Sep99 pmartin from macnet/parser/dictfns.
   * capcode is either an Atom or a List of Atoms with Integer
   * numericalValues.
   */

  if (( capcode == null) || (capcode.equals( capcode_Lc ))){
       return wstring.toLowerCase();
  }else if (capcode.equals( capcode_Ic )){
    StringBuffer sb = new StringBuffer(wstring.toLowerCase());
    char first = sb.charAt(0);
    sb.setCharAt(0, Character.toUpperCase(first));
    return sb.toString();
  }else if (capcode.equals( capcode_Uc )){
    return wstring.toUpperCase();
  }else if (!(capcode instanceof List)){
    throw new IllegalCapcodeException(capcode);
  }else{
    List ccList = (List)capcode;
    int prev = 0;
    Value valInt;
    Number atIntVal;
    int i,j;
    char ch;
    int cclen = ccList.length();
    int[] ccInts = new int[cclen];
    for (i=0; i< cclen; i++){
      valInt = ccList.elementAt(i);
      if ((valInt instanceof Atom) &&
	  ((atIntVal = ((Atom)valInt).numericalValue()) != null)){
	if (atIntVal instanceof Integer){
	  j = ((Integer)atIntVal).intValue();
	}else if (atIntVal instanceof Double){
	  j = ((Double)atIntVal).intValue();
	}else{throw new IllegalCapcodeException(capcode);}
	if (j > prev){
	  ccInts[i] = j;
	  prev = j;
	}else{throw new IllegalCapcodeException(capcode);}
      }else{
	throw new IllegalCapcodeException(capcode);
      }
    }
    int wlen = wstring.length();
    if (wlen < prev){
      throw new StringIsTooShortForCapcodeException(wstring, capcode);
    }else{
      StringBuffer sbw = new StringBuffer(wstring.toLowerCase());
      for (i=0;  i<cclen; i++){
	j = ccInts[i];
	ch = sbw.charAt(j - 1);
	sbw.setCharAt((j - 1), Character.toUpperCase(ch));
      }
      return sbw.toString();
    }
  }
 }  


/*  Hand stubbed-out version for initial testing....does not support multi-word "words" */

 Word getATNWord( Word  word ,  Value  capcode ,  boolean  sentenceStartFlag ,  String  file ,
		  int  wordbeg ,  int  wordend ) {
  /* short call form */
    return  getATNWord( word ,  capcode ,  sentenceStartFlag ,  file ,  wordbeg ,  wordend ,  
			null ,  finalFlag );
 }

 Word getATNWord( Word  word ,  Value  capcode ,  boolean  sentenceStartFlag ,  String  file ,  
		  int  wordbeg ,  int  wordend ,  Word  nextchar ) {
  /* short call form */
    return  getATNWord( word ,  capcode ,  sentenceStartFlag ,  file ,  wordbeg ,  wordend ,  
			nextchar ,  finalFlag );
 }

//  Lexicon.Word getATNWord( Lexicon.Word  word ,  Value  capcode ,  boolean  sentenceStartFlag ,  String  file ,  
// 		  int  wordbeg ,  int  wordend ,  Lexicon.Word  nextchar ,  boolean  finalFlag ) {
//    /* getATNWord (word capcode sentenceStartFlag file wordBeg wordEnd
//                         &optional nextchar (finalFlag final-flag))
//        is the function called for each word found by
//        the scanner.  It checks for compound phrases and
//        then passes the result to parseWord to process. */

//    /****** real version also fluffs out abbreviations .. this version is a stub
//     * which only supports words that have one token.
//     *  written 9 Sept 1999  pmartin 
//     */
//     thisWord  =  word ;
//     word.morph();
//     sp_wordQueue  =  null ;
//     restcompoundtree  =  null ;
//     compoundstart  =  null ;
//     sp_savedpunctbuffer  =  null ;
//     parseWord( word ,  capcode ,  sentenceStartFlag ,  file ,  wordbeg ,  wordend ,  nextchar );
//     punctbuffer.clearWordBuffer() ;
//     return   thisWord ;
//  }

 Word makePhraseName( Phrase  phrase ) {
   /* makePhraseName (phrase) makes a name to use for the concept 
      corresponding to the indicated structured phrase 
      (as returned by transmits from indexFilePhrase). */
   String pname =  phrase.phraseNameString();
   return  lex.makeWord( pname );
 }
 
 Word makePhraseWord( Phrase  phrase ) {
   /* makePhraseName (phrase) makes a name from the words in the concept 
      corresponding to the indicated structured phrase 
      (as returned by transmits from indexFilePhrase). */
   String pname =  phrase.getWordString();
   return  lex.makeWord( pname );
 }
 



 void assimilateAndIndex(Word xWord, Integer wordStart ,Integer wordEnd,
			 String file, String xString, Value capcode, 
			 boolean sentenceStartFlag){
     // stub to debug with .. pmartin 28Sept99 
     //if (debugFlag) {
     //	 System.out.println("assimilateAndIndex called for word string " +
     //			    xString);
     // }
 }

Value[] parseInput(String inString, boolean debugFlag){
  /* hacked up pmartin 12sep99 .. patched 8Oct99 for control chars
   *  patched up some more 20apr00 for less gibberish in output.
   *  modified 12aug00 pmartin to add calls to the indexer
   */
  StringTokenizer inEnumerator = new StringTokenizer (inString, " ", true);
  Vector sToks = new Vector ();
  String wStr;
  String lastWordStr = null;
  boolean sentenceStart = true;
  boolean finalFlag = false;
  Word newWord = null;
  Word lastWord = null;
  Word nextChar = null;
  int wordPos = 0;
  int tailPos = 0;
  Value[] vToks;
  Value capcode;
  boolean punctp;
  while ((lastWordStr != null) || (inEnumerator.hasMoreTokens())){
    if (lastWordStr == null) {
      wStr = inEnumerator.nextToken();
      newWord = lex.getOrGuessWord(wStr);
     }else{
      wStr = lastWordStr;
      newWord = lastWord;
      lastWordStr = null;
    }
    wordPos = inString.indexOf(wStr, tailPos);
    tailPos = wordPos + wStr.length();
    capcode = lex.makeCapcode(wStr);

    if (debugFlag)
	System.out.println("debug ParseInput: token string=>" + wStr +
			   "<= yielding word or punct =>" +
			   newWord.printString() + "<= with capcode=" + 
			   capcode.printString());
    sToks.addElement(newWord);
    if  (inEnumerator.hasMoreTokens()){
      lastWordStr =  inEnumerator.nextToken();
      lastWord = lex.getOrGuessWord(lastWordStr);
      if (lastWord.punctuationp()) {
	nextChar = lastWord;
	punctp = true;
      } else if ((lastWordStr.length() > 1) && 
		 ((lastWordStr.indexOf("\n\n") != -1))){
	  nextChar = word_2cr; // subst special flag for double cr
	  lastWord = nextChar;
	  punctp = true;
      }else{
	  nextChar = word_Space;
	  punctp = false;
      }
      if (debugFlag)
	  System.out.println("debug ParseInput: peek ahead =>" + 
			     lastWord.printString() +
			     "<= with punctP of " + punctp);
    }else{
      finalFlag = true;
      if (debugFlag) 
	  System.out.println("debug ParseInput: setting final flag; newWord=>"
			     + newWord + "<=");
    }
    if (newWord.punctuationp()){
	if (debugFlag)
	    System.out.println("debug ParseInput: calling parsePunct with=>" + 
			       newWord.printString() + "<=");
	parsePunct(newWord, "tty-input", wordPos, finalFlag);
    }else{
	if (debugFlag)
	    System.out.println("debug ParseInput: calling getATNWord with word=>"
			       + newWord.printString() + "<= and nextChar =>" +
			       nextChar.printString() + "<=");
	getATNWord(newWord, capcode, sentenceStart, "tty-input", wordPos, 
		   tailPos, nextChar, finalFlag);
    }
    sentenceStart = false;
  }

  vToks = new Word[sToks.size()];
  sToks.copyInto(vToks);
  return (Value[])vToks;
}
  
int getThisYear () {
  /* hacked up by PMartin 1feb00 to replace new code by Bill for Unix date */
  return (Calendar.getInstance()).get(Calendar.YEAR);
}
/// file wordQueue.jav
// tweaked once more 2oct01 for external Lexicon
///

 void indexWord(Word tw, int start, int end){
     /** pmartin 12aug00 sends off word if there is a receiver waiting for it
	 else does nothing */
     if (csReceiver != null){ 
	 if (scantrace) System.out.println("indexing word " + tw.printString() +
					   " from " + start + " to " + end);
	 csReceiver.receiveWordPos(tw, start, end);
     }
 }

// method getATNWord is from file wordQueue.jav
// tweaked clearBuffer to clearWordBuffer to remove Lisp conflict
// pmartin 7jun00
// modified 7aug00 pmartin to fix abbreviations that are a list of lists
// modified 12aug00 pmartin to add calls to the indexer
// tweaked 19oct00 pmartin to allow nil list elements to be empty
//                 lists or  Java null
 Word getATNWord( Word  word ,  Value  capcode ,  boolean  sentenceStartFlag ,  
		  String  file ,  int  wordBeg ,  int  wordEnd ,  Word  nextchar , 
		  boolean  finalFlag ) {
  /* getATNWord (word capcode sentenceStartFlag file wordBeg wordEnd
                        &optional nextchar (finalFlag final-flag))
   is the function called for each word found by
   the scanner.  It checks for compound phrases and
   then passes the result to parseWord to process. */

   /* 'global' restCompoundTree may have the "rest" of a compound tree that is
    *   currently being traversed in it .. if so, try to continue it. 
    */

    //  boolean getAtnDebug = true;
    boolean getAtnDebug = false;
   if (getAtnDebug) System.out.println("Entered getATNWord with word " + word.toString());
   if (sp_indexAllWordsFlag)  indexWord(word, wordBeg, wordEnd);
   thisWord  =  word ;
   word.morph();
   
   Vector compoundTrees = new Vector();
   List tempTree = null;
   List xTree;
   Value xValue;
   Vector currentCompoundSenses = new Vector();
   Vector newCompoundTrees = null;
   Vector tempCompoundTrees;
   List abbrevs = (List)word.getdict(feature_Abbrev);
   Word xWord = null;
   WordQElt wqEl;

   /* restCompoundTree non-null means we are currently chasing a phrase */
   if (restCompoundTree != null){ 
     if (getAtnDebug) System.out.println("debugging getAtnWord .. restCompoundTree = " +
					 restCompoundTree.toString());
     if  ( (tempTree  = assocList(word, restCompoundTree)) != null){
       compoundTrees.addElement(tempTree);
       if (getAtnDebug) System.out.println("debugging getAtnWord ... found word in restCompoundTree = " +
					   tempTree.toString());
     }
     if (abbrevs != null){  //extra stuff for lists added 8feb00 pm
       if (getAtnDebug) System.out.println("debugging getAtnWord .. abbrevs = " + abbrevs.toString());
       for ( int  i  =  0;  i  <  abbrevs.length();  i++ ) {
	 Value abrv = abbrevs.elementAt(i);
	 if (abrv.wordp()){  
	   if ((tempTree = assocList((Word)abrv, restCompoundTree)) != null){
	     compoundTrees.addElement(tempTree);
	     break;}
	 }else if (abrv.listp()){ 
	   if ((tempTree = listMatch((List)abrv, restCompoundTree)) != null){
	     addListToVector(tempTree,compoundTrees);
	     break;}
	 }
       }
       if ((tempTree != null) && getAtnDebug)
	 System.out.println(
	    "debugging getAtnWord ... found abbrev in restCompoundTree = " +
	    tempTree.toString());

       }
     }
   if (compoundTrees.size() == 0) compoundTrees = null;

   restCompoundTree = null; /* We will reconstruct this if we are inside a phrase */

   /* compoundTrees non-empty means this word continues an open phrase */
   if (compoundTrees != null){
     if (getAtnDebug) System.out.println("getAtnWord continuing a phrase");
     tempCompoundTrees = new Vector();
     for (int i = 0; i<compoundTrees.size(); i++){
       tempTree = (List)compoundTrees.elementAt(i);
       xValue = listSecond(tempTree);
       if ((xValue != null) && (xValue.wordp())){
       xWord = (Word)listSecond(tempTree);
	 currentCompoundSenses.addElement(xValue);
       }
       xTree = listTailTail(tempTree);
       if (xTree != null) {
	 tempCompoundTrees.addElement(xTree);
       }
     }
     if (getAtnDebug) System.out.println("getAtnWord collected tempCompoundTrees = " +
					 tempCompoundTrees.toString());
     restCompoundTree = appendVectorList(tempCompoundTrees);
     if (getAtnDebug) {
       System.out.print("getAtnWord made new restCompoundTree = ");
       if (restCompoundTree == null) {
	 System.out.println("null");
       }else{
	  System.out.println(restCompoundTree.toString());
       }
     }

       /* word queue empty means we're not currently working on a compound */
     }else if (sp_wordQueue == null){
       newCompoundTrees =  new Vector();
       if ((tempTree = (List)word.getdict(feature_Compounds)) 
                       !=  null )
	 newCompoundTrees.addElement(tempTree);

       if (abbrevs != null) {
	   if (getAtnDebug)  
	       System.out.println("making new compound trees from abbrevs");
	   includeAbbrevs(abbrevs, tempTree, newCompoundTrees);
       }
 
      currentCompoundSenses  =  null ;
      if (newCompoundTrees.size() > 0){
	 tempCompoundTrees = new Vector();
	 for (int i=0; i<newCompoundTrees.size(); i++){
	   tempTree = (List)newCompoundTrees.elementAt(i);
	   for (int j=0; j<tempTree.length(); j++) {
	     tempCompoundTrees.addElement(tempTree.elementAt(j));
	   }
	 }
	 restCompoundTree =  lex.makeList(tempCompoundTrees);
	 if (getAtnDebug) System.out.println("getAtnWord word queue was empty but" +
					     " new restCompoundTree = " +
					     restCompoundTree.toString());
       }else{
	 restCompoundTree = null;
	 newCompoundTrees = null;  // simplify later tests
       }
     } else {
       currentCompoundSenses = null;
       restCompoundTree = null;
     }
    
    if ( scantrace  && 
        ( ((compoundTrees !=  null) && (compoundTrees.size() > 0)) ||
          ((newCompoundTrees != null) && newCompoundTrees.size() > 0)) ){
      	String wordStr;
	String wordsComingStr = "<null queue>";
	if (sp_wordQueue != null) {
	  StringBuffer wordsComingStrBuf = new StringBuffer();
	  for (int i=0; i<sp_wordQueue.size(); i++){
	    wordsComingStrBuf.append(" ").append(((WordQElt)sp_wordQueue.elementAt(i)).word.getWordString());
	  }
	  wordsComingStr = wordsComingStrBuf.toString();
	}
	if ( sp_scantraceInLowercase ){
	  wordStr =  word.getWordString().toLowerCase();
	}else{
	  wordStr = capitalizeByCapcode(word.getWordString(), capcode);
	}
	scantraceFile.println( "    " + wordStr );
	int blanks = Math.max(0, 15 - wordStr.length());
	System.out.print( staticString80Blanks.substring(0, blanks));
	scantraceFile.println( " potential compound  transition after " + 
			       wordsComingStr + "\n" );
    }

    if ((compoundTrees != null) && (compoundTrees.size() > 0)){ 
    /* this word continues a potential compound */
      if (currentCompoundSenses.size() > 0){
	if (scantrace) {
	  scantraceFile.println( "\n" + "******  noticed phrases: " + 
				 currentCompoundSenses.toString() + "\n" );
	}
	for ( int  i  =  0;  i < currentCompoundSenses.size() ;  i++ ) {
	  xWord = (Word)currentCompoundSenses.elementAt(i);
	  assimilateAndIndex( xWord, compoundStart , new Integer( wordEnd ),  file ,
			      xWord.getWordString(), capcode,  sentenceStartFlag );
	}
	int cpStartInt = compoundStart.intValue();
	if (restCompoundTree  ==  null){  /* AND there are no longer phrases */
	 /* we just 'choose' the first one */
	 xWord  = (Word)currentCompoundSenses.elementAt(0);  
	 restCompoundTree  =  null ;
	 compoundStart  =  null ;
	 sp_wordQueue  =  null ;
	 restorePunctBuffer(sp_savedpunctbuffer);
	 sp_savedpunctbuffer  =  null;
	 
	 if (getAtnDebug) System.out.println("getAtnWord finished phrase and chose " +
					     xWord.toString());
	 parseWord(xWord, capcode, sentenceStartFlag, file, cpStartInt,
		   wordEnd ,  nextchar ,  finalFlag );
	}else{ /* there are longer phrases -- wait to see if we find one  */
	  /* but set up the Word Queue to reflect current phrase so far  */
	  wqEl = new WordQElt(punctbufferContents(), xWord, capcode, sentenceStartFlag,
			      file, cpStartInt, wordEnd, nextchar, finalFlag);
	  sp_wordQueue  = new Vector();
	  sp_wordQueue.addElement(wqEl);
	}
      
      }else if (restCompoundTree !=  null){ 
	/* no complete phrase yet but there could still be one */
	wqEl =  new WordQElt(punctbufferContents(), word,capcode, sentenceStartFlag,
			     file, wordBeg, wordEnd, nextchar, finalFlag);
	sp_wordQueue.addElement(wqEl);
	if (getAtnDebug) System.out.println("getAtnWord adding word "+ word.toString() +
					    " to word queue");

      }else{ /* nothing more to look for, so process wordQueue */
	compoundStart  =  null ;
	sp_savedpunctbuffer  =  null ;  /* we don't need this now */
	wqEl = new WordQElt(punctbufferContents(), word, capcode, sentenceStartFlag,
			      file, wordBeg, wordEnd, nextchar, finalFlag); 
	if (getAtnDebug) System.out.println("getAtnWord adding word "+ word.toString() +
					    " to word queue and processing queue");
	sp_wordQueue.addElement(wqEl);
	processWordQueue(sp_wordQueue);
      }

    }else if (sp_wordQueue  !=  null ){ /* failed to match current word in phrase tree */
      restCompoundTree  =  null ;
      compoundStart  =  null ;
      wqEl = new WordQElt(punctbufferContents(), word, capcode, sentenceStartFlag,
			  file, wordBeg, wordEnd, nextchar, finalFlag); 
      sp_wordQueue.addElement(wqEl);
      if (getAtnDebug) System.out.println("getAtnWord failed to match word " +
					  word.toString() + " so processing queue");
      processWordQueue(sp_wordQueue);
 
    }else if ((newCompoundTrees !=  null) && (newCompoundTrees.size() > 0)){ 
      /* this word may start a compound */
      compoundStart  =  new Integer( wordBeg );
      savePunctBuffer();
      wqEl = new WordQElt(punctbufferContents(), word, capcode, sentenceStartFlag,
			  file, wordBeg, wordEnd, nextchar, finalFlag); 
      if (getAtnDebug) System.out.println("getAtnWord starting new word queue with "+
					  word.toString());
      sp_wordQueue  = new Vector();
      sp_wordQueue.addElement(wqEl);

    }else{ 
      sp_wordQueue  =  null ;
      restCompoundTree  =  null ;
      compoundStart  =  null ;
      sp_savedpunctbuffer  =  null ;
      if (getAtnDebug) System.out.println("getAtnWord falling through to parseWord " +
					  word.toString());
      parseWord(word, capcode, sentenceStartFlag, file, wordBeg, wordEnd, nextchar);
    }

   punctbuffer.clearWordBuffer();
   /* this needs to stay til parseWord kills it - no  */
   /* prev comment is not true, but why did I think so? -- because when process word
    queue starts working, it needs this punctbuffer back. -- residual problem:
    what if punct occurs in the middle of a potential compound that doesn't work out?
    then should get that punct back when we come to the coorresponding word in the word
    queue. 
    */
    return  thisWord;
 }

  void includeAbbrevs(List abbList, List tempTree, Vector newCompoundTrees){
      if (abbList != null) {
	  if (abbList.elementAt(0) instanceof List){
	      for (int j=0; j<abbList.length(); j++)
		  includeAbbrevs((List)(abbList.elementAt(j)), 
				 tempTree, newCompoundTrees);
	  } else {
	      Word xWord;
	      for (int i = 0; i<abbList.length(); i++){
		  xWord = (Word)abbList.elementAt(i);
		  if ((tempTree = 
		        (List)xWord.getdict(feature_Compounds)) 
		                 != null) {
		      newCompoundTrees.addElement(tempTree);
		  }
	      }
	  }
      }
  }


 boolean vowelcodep( int  i ) {
    return   ( charType[i]  ==  atom_Vowel );
 }


 boolean consonantcodep( int  i ) {
    return   ( charType[i]  ==  atom_Consonant );
 }


 boolean alphacodep( int  i ) {
    Atom  tempFeature1000  =  charType[i] ;
    return  LexiconUtil.isMembOfArray( tempFeature1000 ,  featureSet_Vowel$Consonant );
 }


 boolean digitcodep( int  i ) {
    return   ( charType[i]  ==  atom_Digit );
 }


 boolean whitecodep( int  i ) {
    return   ( charType[i]  ==  atom_White );
 }


 boolean punctcodep( int  i ) {
    return   ( charType[i]  ==  atom_Punct );
 }


 boolean breakcodep( int  i ) {
    Atom  tempFeature1001  =  charType[i] ;
    return  LexiconUtil.isMembOfArray( tempFeature1001 ,  featureSet_White$Punct );
 }


 void installStartFuns() {
   category_City.setStartState( cityStart );
   category_Monthname.setStartState( monthStart );
   category_Number.setStartState( numberStart );
   category_Ordinal.setStartState( ordinalStart );
   category_Cardinal.setStartState( cardinalStart );
   category_Weekday.setStartState( weekdayStart );
   category_Preposition.setStartState( prepositionStart );
   category_Determiner.setStartState( detStart );
   category_Firstname.setStartState( firstnameStart );
   category_Dualname.setStartState( firstnameStart );
   category_Lastname.setStartState( lastnameStart );
   category_Singleletter.setStartState( singleletterStart );
   category_Prefixtitle.setStartState( prefixtitleStart );
 }


 void putWordClass( Word  x ,  Category  z ) {
  /* putWordClass (x z) is used in nameFile.lisp
  to record information about known names. */
    Value  oldval  = x.getdict( feature_Wordclass );
    Word  namconfword ;
    namconfword  =  word_5 ;
   if    (oldval.equals( z ))
      { /* empty statement here */ }

   else  if    ( oldval  !=  null )
      scantraceFile.print( "\n" + "attempt to reset wordClass of " + x.getWordString() + " from " + oldval.toString() + " to " + z.toString() + " -- skipped." + "\n" );

   else  { if   (x.hasNonnameCat())
        { if   (x.looksLike( category_Name ))
             namconfword  =  word_9 ;
         x.putdict( feature_Nameconfidence ,  namconfword );
         x.putdict( feature_Wordclass ,  z );
        }
   }
 }


 void installGrammarWordProps() {
   word_January.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_January.markDict(lex.makeAtom( "monthnum" ),  word_1 ,  false );
   word_January.markDict(lex.makeAtom( "monthdays" ),  word_31 ,  false );
   word_February.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_February.markDict(lex.makeAtom( "monthnum" ),  word_2 ,  false );
   word_February.markDict(lex.makeAtom( "monthdays" ),  word_28 ,  false );
   word_March.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_March.markDict(lex.makeAtom( "monthnum" ),  word_3 ,  false );
   word_March.markDict(lex.makeAtom( "monthdays" ),  word_31 ,  false );
   word_March.markDict(lex.makeAtom( "monthconfidence" ),  word_6 ,  false );
   word_April.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_April.markDict(lex.makeAtom( "monthnum" ),  word_4 ,  false );
   word_April.markDict(lex.makeAtom( "monthdays" ),  word_30 ,  false );
   word_April.markDict(lex.makeAtom( "monthconfidence" ),  word_8 ,  false );
   word_May.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_May.markDict(lex.makeAtom( "monthnum" ),  word_5 ,  false );
   word_May.markDict(lex.makeAtom( "monthdays" ),  word_31 ,  false );
   word_May.markDict(lex.makeAtom( "monthconfidence" ),  word_2 ,  false );
   word_June.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_June.markDict(lex.makeAtom( "monthnum" ),  word_6 ,  false );
   word_June.markDict(lex.makeAtom( "monthdays" ),  word_30 ,  false );
   word_June.markDict(lex.makeAtom( "monthconfidence" ),  word_8 ,  false );
   word_July.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_July.markDict(lex.makeAtom( "monthnum" ),  word_7 ,  false );
   word_July.markDict(lex.makeAtom( "monthdays" ),  word_31 ,  false );
   word_July.markDict(lex.makeAtom( "monthconfidence" ),  word_9 ,  false );
   word_July.markDict(lex.makeAtom( "nameconfidence" ),  word_3 ,  false );
   word_August.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_August.markDict(lex.makeAtom( "monthnum" ),  word_8 ,  false );
   word_August.markDict(lex.makeAtom( "monthdays" ),  word_31 ,  false );
   word_August.markDict(lex.makeAtom( "monthconfidence" ),  word_9 ,  false );
   word_September.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_September.markDict(lex.makeAtom( "monthnum" ),  word_9 ,  false );
   word_September.markDict(lex.makeAtom( "monthdays" ),  word_30 ,  false );
   word_October.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_October.markDict(lex.makeAtom( "monthnum" ),  word_10 ,  false );
   word_October.markDict(lex.makeAtom( "monthdays" ),  word_31 ,  false );
   word_November.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_November.markDict(lex.makeAtom( "monthnum" ),  word_11 ,  false );
   word_November.markDict(lex.makeAtom( "monthdays" ),  word_30 ,  false );
   word_December.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_December.markDict(lex.makeAtom( "monthnum" ),  word_12 ,  false );
   word_December.markDict(lex.makeAtom( "monthdays" ),  word_31 ,  false );
   word_Jan.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_Jan.markDict(lex.makeAtom( "monthnum" ),  word_1 ,  false );
   word_Jan.markDict(lex.makeAtom( "monthdays" ),  word_31 ,  false );
   word_Jan.markDict(lex.makeAtom( "monthconfidence" ),  word_6 ,  false );
   word_Feb.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_Feb.markDict(lex.makeAtom( "monthnum" ),  word_2 ,  false );
   word_Feb.markDict(lex.makeAtom( "monthdays" ),  word_28 ,  false );
   word_Mar.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_Mar.markDict(lex.makeAtom( "monthnum" ),  word_3 ,  false );
   word_Mar.markDict(lex.makeAtom( "monthdays" ),  word_31 ,  false );
   word_Mar.markDict(lex.makeAtom( "monthconfidence" ),  word_6 ,  false );
   word_Apr.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_Apr.markDict(lex.makeAtom( "monthnum" ),  word_4 ,  false );
   word_Apr.markDict(lex.makeAtom( "monthdays" ),  word_30 ,  false );
   word_Jun.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_Jun.markDict(lex.makeAtom( "monthnum" ),  word_6 ,  false );
   word_Jun.markDict(lex.makeAtom( "monthdays" ),  word_30 ,  false );
   word_Jun.markDict(lex.makeAtom( "monthconfidence" ),  word_6 ,  false );
   word_Jul.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_Jul.markDict(lex.makeAtom( "monthnum" ),  word_7 ,  false );
   word_Jul.markDict(lex.makeAtom( "monthdays" ),  word_31 ,  false );
   word_Aug.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_Aug.markDict(lex.makeAtom( "monthnum" ),  word_8 ,  false );
   word_Aug.markDict(lex.makeAtom( "monthdays" ),  word_31 ,  false );
   word_Sept.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_Sept.markDict(lex.makeAtom( "monthnum" ),  word_9 ,  false );
   word_Sept.markDict(lex.makeAtom( "monthdays" ),  word_30 ,  false );
   word_Sep.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_Sep.markDict(lex.makeAtom( "monthnum" ),  word_9 ,  false );
   word_Sep.markDict(lex.makeAtom( "monthdays" ),  word_30 ,  false );
   word_Oct.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_Oct.markDict(lex.makeAtom( "monthnum" ),  word_10 ,  false );
   word_Oct.markDict(lex.makeAtom( "monthdays" ),  word_31 ,  false );
   word_Nov.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_Nov.markDict(lex.makeAtom( "monthnum" ),  word_11 ,  false );
   word_Nov.markDict(lex.makeAtom( "monthdays" ),  word_30 ,  false );
   word_Dec.markDict(lex.makeAtom( "wordclass" ),  category_Monthname ,  false );
   word_Dec.markDict(lex.makeAtom( "monthnum" ),  word_12 ,  false );
   word_Dec.markDict(lex.makeAtom( "monthdays" ),  word_31 ,  false );
   word_Dec.markDict(lex.makeAtom( "monthconfidence" ),  word_6 ,  false );
   word_Sunday.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Sunday.markDict(lex.makeAtom( "day" ),  word_1 ,  false );
   word_Monday.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Monday.markDict(lex.makeAtom( "day" ),  word_2 ,  false );
   word_Tuesday.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Tuesday.markDict(lex.makeAtom( "day" ),  word_3 ,  false );
   word_Wednesday.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Wednesday.markDict(lex.makeAtom( "day" ),  word_4 ,  false );
   word_Thursday.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Thursday.markDict(lex.makeAtom( "day" ),  word_5 ,  false );
   word_Friday.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Friday.markDict(lex.makeAtom( "day" ),  word_6 ,  false );
   word_Saturday.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Saturday.markDict(lex.makeAtom( "day" ),  word_7 ,  false );
   word_Sundays.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Sundays.markDict(lex.makeAtom( "day" ),  word_1 ,  false );
   word_Mondays.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Mondays.markDict(lex.makeAtom( "day" ),  word_2 ,  false );
   word_Tuesdays.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Tuesdays.markDict(lex.makeAtom( "day" ),  word_3 ,  false );
   word_Wednesdays.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Wednesdays.markDict(lex.makeAtom( "day" ),  word_4 ,  false );
   word_Thursdays.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Thursdays.markDict(lex.makeAtom( "day" ),  word_5 ,  false );
   word_Fridays.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Fridays.markDict(lex.makeAtom( "day" ),  word_6 ,  false );
   word_Saturdays.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Saturdays.markDict(lex.makeAtom( "day" ),  word_7 ,  false );
   word_Sun.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Sun.markDict(lex.makeAtom( "day" ),  word_1 ,  false );
   word_Sun.markDict(lex.makeAtom( "weekdayconfidence" ),  word_5 ,  false );
   word_Mon.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Mon.markDict(lex.makeAtom( "day" ),  word_2 ,  false );
   word_Tues.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Tues.markDict(lex.makeAtom( "day" ),  word_3 ,  false );
   word_Tue.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Tue.markDict(lex.makeAtom( "day" ),  word_3 ,  false );
   word_Wed.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Wed.markDict(lex.makeAtom( "day" ),  word_4 ,  false );
   word_Wed.markDict(lex.makeAtom( "weekdayconfidence" ),  word_5 ,  false );
   word_Thurs.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Thurs.markDict(lex.makeAtom( "day" ),  word_5 ,  false );
   word_Fri.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Fri.markDict(lex.makeAtom( "day" ),  word_6 ,  false );
   word_Sat.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Sat.markDict(lex.makeAtom( "day" ),  word_7 ,  false );
   word_Sat.markDict(lex.makeAtom( "weekdayconfidence" ),  word_5 ,  false );
   word_Su.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Su.markDict(lex.makeAtom( "day" ),  word_1 ,  false );
   word_Su.markDict(lex.makeAtom( "weekdayconfidence" ),  word_4 ,  false );
   word_Mo.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Mo.markDict(lex.makeAtom( "day" ),  word_2 ,  false );
   word_Mo.markDict(lex.makeAtom( "weekdayconfidence" ),  word_4 ,  false );
   word_Tu.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Tu.markDict(lex.makeAtom( "day" ),  word_3 ,  false );
   word_Tu.markDict(lex.makeAtom( "weekdayconfidence" ),  word_4 ,  false );
   word_Th.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Th.markDict(lex.makeAtom( "day" ),  word_5 ,  false );
   word_Th.markDict(lex.makeAtom( "weekdayconfidence" ),  word_4 ,  false );
   word_Fr.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Fr.markDict(lex.makeAtom( "day" ),  word_6 ,  false );
   word_Fr.markDict(lex.makeAtom( "weekdayconfidence" ),  word_4 ,  false );
   word_Sa.markDict(lex.makeAtom( "wordclass" ),  category_Weekday ,  false );
   word_Sa.markDict(lex.makeAtom( "day" ),  word_7 ,  false );
   word_Sa.markDict(lex.makeAtom( "weekdayconfidence" ),  word_4 ,  false );
   word_At.markDict(lex.makeAtom( "wordclass" ),  category_Preposition ,  false );
   word_At.markDict(lex.makeAtom( "preposition" ), lex.makeAtom( "locortime" ),  false );
   word_In.markDict(lex.makeAtom( "wordclass" ),  category_Preposition ,  false );
   word_In.markDict(lex.makeAtom( "preposition" ), lex.makeAtom( "locortime" ),  false );
   word_On.markDict(lex.makeAtom( "wordclass" ),  category_Preposition ,  false );
   word_On.markDict(lex.makeAtom( "preposition" ), lex.makeAtom( "locortime" ),  false );
   word_Since.markDict(lex.makeAtom( "wordclass" ),  category_Preposition ,  false );
   word_Since.markDict(lex.makeAtom( "preposition" ), lex.makeAtom( "time" ),  false );
   word_Before.markDict(lex.makeAtom( "wordclass" ),  category_Preposition ,  false );
   word_Before.markDict(lex.makeAtom( "preposition" ), lex.makeAtom( "time" ),  false );
   word_After.markDict(lex.makeAtom( "wordclass" ),  category_Preposition ,  false );
   word_After.markDict(lex.makeAtom( "preposition" ), lex.makeAtom( "time" ),  false );
   word_During.markDict(lex.makeAtom( "wordclass" ),  category_Preposition ,  false );
   word_During.markDict(lex.makeAtom( "preposition" ), lex.makeAtom( "time" ),  false );
   word_Till.markDict(lex.makeAtom( "wordclass" ),  category_Preposition ,  false );
   word_Till.markDict(lex.makeAtom( "preposition" ), lex.makeAtom( "time" ),  false );
   word_Until.markDict(lex.makeAtom( "wordclass" ),  category_Preposition ,  false );
   word_Until.markDict(lex.makeAtom( "preposition" ), lex.makeAtom( "time" ),  false );
   word_Of.markDict(lex.makeAtom( "wordclass" ),  category_Preposition ,  false );
   word_Of.markDict(lex.makeAtom( "preposition" ), lex.makeAtom( "poss" ),  false );
   word_From.markDict(lex.makeAtom( "wordclass" ),  category_Preposition ,  false );
   word_From.markDict(lex.makeAtom( "preposition" ), lex.makeAtom( "locortimeorperson" ),  false );
   word_To.markDict(lex.makeAtom( "wordclass" ),  category_Preposition ,  false );
   word_To.markDict(lex.makeAtom( "preposition" ), lex.makeAtom( "locortimeorperson" ),  false );
   word_Through.markDict(lex.makeAtom( "wordclass" ),  category_Preposition ,  false );
   word_Through.markDict(lex.makeAtom( "preposition" ), lex.makeAtom( "locortimeorperson" ),  false );
   word_Now.markDict(lex.makeAtom( "wordclass" ),  category_Date ,  false );
   word_Now.markDict(lex.makeAtom( "reference" ), lex.makeAtom( "deictic" ),  false );
   word_Today.markDict(lex.makeAtom( "wordclass" ),  category_Date ,  false );
   word_Today.markDict(lex.makeAtom( "reference" ), lex.makeAtom( "deictic" ),  false );
   word_Tomorrow.markDict(lex.makeAtom( "wordclass" ),  category_Date ,  false );
   word_Tomorrow.markDict(lex.makeAtom( "reference" ), lex.makeAtom( "deictic" ),  false );
   word_Yesterday.markDict(lex.makeAtom( "wordclass" ),  category_Date ,  false );
   word_Yesterday.markDict(lex.makeAtom( "reference" ), lex.makeAtom( "deictic" ),  false );
   word_Late.markDict(lex.makeAtom( "wordclass" ),  category_Adjective ,  false );
   word_Early.markDict(lex.makeAtom( "wordclass" ),  category_Adjective ,  false );
   word_Fall.markDict(lex.makeAtom( "wordclass" ),  category_Noun ,  false );
   word_Fall.markDict(lex.makeAtom( "noun" ), lex.makeAtom( "seasonorother" ),  false );
   word_Winter.markDict(lex.makeAtom( "wordclass" ),  category_Noun ,  false );
   word_Winter.markDict(lex.makeAtom( "noun" ), lex.makeAtom( "season" ),  false );
   word_Spring.markDict(lex.makeAtom( "wordclass" ),  category_Noun ,  false );
   word_Spring.markDict(lex.makeAtom( "noun" ), lex.makeAtom( "seasonorother" ),  false );
   word_Summer.markDict(lex.makeAtom( "wordclass" ),  category_Noun ,  false );
   word_Summer.markDict(lex.makeAtom( "noun" ), lex.makeAtom( "season" ),  false );
   word_Day.markDict(lex.makeAtom( "wordclass" ),  category_Timeunit ,  false );
   word_Week.markDict(lex.makeAtom( "wordclass" ),  category_Timeunit ,  false );
   word_Month.markDict(lex.makeAtom( "wordclass" ),  category_Timeunit ,  false );
   word_Year.markDict(lex.makeAtom( "wordclass" ),  category_Timeunit ,  false );
   word_Hour.markDict(lex.makeAtom( "wordclass" ),  category_Timeunit ,  false );
   word_Minute.markDict(lex.makeAtom( "wordclass" ),  category_Timeunit ,  false );
   word_Hundred.markDict(lex.makeAtom( "wordclass" ),  category_Numberunit ,  false );
   word_Hundred.setNumericalValue( new Double( 100 ));
   word_Thousand.markDict(lex.makeAtom( "wordclass" ),  category_Numberunit ,  false );
   word_Thousand.setNumericalValue( new Double( 1000 ));
   word_Last.markDict(lex.makeAtom( "wordclass" ),  category_Adjective ,  false );
   word_Last.markDict(lex.makeAtom( "reference" ), lex.makeAtom( "deictic" ),  false );
   word_Next.markDict(lex.makeAtom( "wordclass" ),  category_Adjective ,  false );
   word_Next.markDict(lex.makeAtom( "reference" ), lex.makeAtom( "deictic" ),  false );
   word_The.markDict(lex.makeAtom( "wordclass" ),  category_Determiner ,  false );
   word_The.markDict(lex.makeAtom( "reference" ), lex.makeAtom( "def" ),  false );
   word_This.markDict(lex.makeAtom( "wordclass" ),  category_Determiner ,  false );
   word_This.markDict(lex.makeAtom( "reference" ), lex.makeAtom( "deictic" ),  false );
   word_President.markDict(lex.makeAtom( "wordclass" ),  category_Prefixtitle ,  false );
   word_Vicepresident.markDict(lex.makeAtom( "wordclass" ),  category_Prefixtitle ,  false );
   word_A.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_A.markDict((Atom) determiner , lex.makeAtom( "indef" ),  false );
   word_B.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_C.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_D.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_E.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_F.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_G.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_H.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_I.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_J.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_K.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_L.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_M.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_N.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_O.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_P.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_Q.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_R.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_S.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_T.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_U.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_V.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_W.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_X.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_Y.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_Z.markDict(lex.makeAtom( "wordclass" ),  category_Singleletter ,  false );
   word_Jr.markDict(lex.makeAtom( "wordclass" ),  category_Namesuffix ,  false );
   word_Sr.markDict(lex.makeAtom( "wordclass" ),  category_Namesuffix ,  false );
   word_Ii.markDict(lex.makeAtom( "wordclass" ),  category_Namesuffix ,  false );
   word_Iii.markDict(lex.makeAtom( "wordclass" ),  category_Namesuffix ,  false );
   word_Iv.markDict(lex.makeAtom( "wordclass" ),  category_Namesuffix ,  false );
   word_Esq.markDict(lex.makeAtom( "wordclass" ),  category_Namesuffix ,  false );
   word_Van.markDict(lex.makeAtom( "wordclass" ),  category_Lastnameprefix ,  false );
   word_Von.markDict(lex.makeAtom( "wordclass" ),  category_Lastnameprefix ,  false );
   word_De.markDict(lex.makeAtom( "wordclass" ),  category_Lastnameprefix ,  false );
   word_Der.markDict(lex.makeAtom( "wordclass" ),  category_Lastnameprefix ,  false );
   word_Da.markDict(lex.makeAtom( "wordclass" ),  category_Lastnameprefix ,  false );
   word_Mac.markDict(lex.makeAtom( "wordclass" ),  category_Lastnameprefix ,  false );
   word_Mc.markDict(lex.makeAtom( "wordclass" ),  category_Lastnameprefix ,  false );
   word_St.markDict(lex.makeAtom( "wordclass" ),  category_Lastnameprefix ,  false );
   word_Bill.markDict(lex.makeAtom( "wordclass" ),  category_Firstname ,  false );
   word_Bill.markDict(lex.makeAtom( "nameconfidence" ),  word_7 ,  false );
   word_Woods.markDict(lex.makeAtom( "wordclass" ),  category_Lastname ,  false );
   word_Woods.markDict(lex.makeAtom( "nameconfidence" ),  word_8 ,  false );
   word_Gross.markDict( atom_Numericalvalue ,  word_144 ,  false );
 }

/**  Name          : simplePhraseLex.java .... based on  ThisLexicon.java
 **  Author        :  Paul Martin ... lifting from original by Alyssa Glass
 **  Created       :  27 October 1999  (based on Alyssa's 12 Aug 98 version)
 *
 *
 **  Last Modified :  4 oct 01 pmartin simplified using new Lex operations
 **       Modified :  1 oct 01 pmartin move to "outside" lexicon
 **       Modified :  4 dec 99 -- fixed types of entries in lex
 **  Comments      :  We just augment the already-loaded dictionary to support
 **                :     simplePhrase collection.  All other Lexicon stuff is in
 **                :     a real lexicon already loaded...
 **/


public void simplePhraseLexSetup() {
  /* adds the needed extra stuff to the dictionary for the 
   * simple Phrase extractor to work. 
   */
    //boolean simplePhraseDebug = true;
    boolean simplePhraseDebug = false;
    //pmartin added "2cr" 20apr00 for flagging double carriage ret
    String[] somePuncts ={ " ", ";", ":", "\t", "2cr"};  //?? need comma here?

    String[] functionWords = 
     {"a-la", "a-priori", "about", "above", "across", 
      "after", "again", "ahead", "all", "almost", "already",
      "also", "although", "always", "am", "amid", "among", "an", "and",
      "another", "any", "anybody", "anyhow", "anyone", "anything", 
      "anyway", "anyways", "anywhere", "approx", "approximately",
      "are", "around", "as", "as-in", "as-many-as", "as-related-to",
      "as-well-as", "at", "at-least", "at-most", "be", "because",
      "been", "before", "being", "below", "between", "both", "but",
      "by", "can", "cannot", "could", "did", "do", "does", "doing",
      "down", "due-to", "during", "each", "either", "elsewhere",
      "et-cetera", "etc", "ever", "every", "everyone", "everything",
      "exactly", "except", "few", "fewer", "fewer-than", "fewest",
      "finally", "for", "forever", "frequently", "from", "greater-than",
      "hast", "hath", "had", "has", "have", "having", "he", "her", "here",
      "hers", "him", "his", "how", "how-many", "how-much", "however", "i",
      "if", "in", "in-conjunction-with", "instead", "into", "is", "it", "its",
      "last", "least", "less", "less-than", "like", "many", "may", "me",
      "meanwhile", "might", "minus", "more", "more-than", "moreover", "most",
      "much", "must", "my", "namely", "nearly", "neither", "never", "nevermore",
      "no", "normally", "not", "nothing", "now", "occasionally", "of", "off",
      "often", "on", "once", "one", "ones", "only", "onto", "or", "other",
      "others", "otherwise", "our", "ours", "out", "out-of", "over", "per",
      "please", "plenty", "plus", "prior-to", "probably", "rather", "re",
      "really", "regarding", "relative-to", "same", "seldom", "several",
      "shall", "shalt", "she", "should", "since", "so", "some", "somebody",
      "someday", "somehow", "someone", "something", "sometime", "sometimes",
      "someway", "someways", "somewhat", "somewhere", "soon", "such", "such-as",
      "than", "that", "the", "their", "them", "then", "there", "thereafter",
      "therefore", "these", "they", "this", "those", "though", "through", "throughout",
      "thus", "to", "today", "tomorrow", "too", "toward", "towards", "under", "unless",
      "unlike", "until", "unto", "up", "upon", "usually", "versus", "very", "via",
      "vs", "was", "we", "were", "what", "what-time", "when", "whence", "whenever",
      "where", "whereas", "whereby", "wherefor", "wherefore", "whether", "which", "while",
      "who", "whom", "whose", "why", "will", "with", "with-respect-to", "within",
      "without", "would", "yesterday", "you", "your", "ye", "thee", "thou", "thy",
      "thine", "aren't", "can't", "couldn't", "didn't", "doesn't", "don't", "hadn't",
      "hasn't", "haven't", "isn't", "mightn't", "shouldn't", "wasn't", "won't",
      "wouldn't", "i'm", "you're", "he's", "she's", "it's", "they're", "we're",
      "i'd", "you'd", "he'd", "she'd", "it'd", "they'd", "we'd", "i've", "you've",
      "they've", "we've" };

  String[] breakStrings = 
  { "ain't", "anybody'd", "anybody'll", "anybody's", "aren't", "can't",
    "couldn't", "didn't", "dj'd", "dj'ed", "dj'ing", "doesn't", "don't",
    "hadn't", "hasn't", "haven't", "he'd", "he'll", "he's", "how'd", "how'll",
    "how're", "how's", "how've", "i'd", "i'll", "i'm", "i've", "isn't", "it'd",
    "it'll", "it's", "ko'd", "ko'ing", "ko's", "mightn't", "mustn't", "n't",
    "needn't", "nobody'd", "nobody'll", "o.k.'ed", "o.k.'ing", "oughtn't", "shan't",
    "she'd", "she'll", "she's", "shouldn't", "somebody'd", "somebody'll", "somebody's",
    "someone'd", "someone'll", "that'd", "that'll", "that's", "there'd", "there'll",
    "there're", "there's", "there've", "these'd", "these'll", "these're", "these've",
    "they'd", "they'll", "they're", "they've", "this'd", "this'll", "this's", "today'd",
    "today'll", "today's", "wasn't", "we'd", "we'll", "we're", "we've", "weren't",
    "what'd", "what'll", "what're", "what's", "what've", "when'd", "when'll", "when're",
    "when's", "when've", "where'd", "where'll", "where're", "where's", "where've",
    "who'd", "who'll", "who're", "who's", "who've", "why'd", "why'll", "why're", "why's",
    "why've", "won't", "wouldn't", "you'd", "you'll", "you're", "you've" };

  Category cat_punct = lex.makeCategory("punct");

  //Lexicon.Atom atom_breakword = lex.makeAtom("breakword");
  Atom atom_true = lex.makeAtom("true");

  Word _set;
  Word word_punctuation_mark = lex.makeWord("punctuation_mark");

  if (simplePhraseDebug) System.out.println("SimplePhrase adding punctuation");

  for (int i=0; i < somePuncts.length; i++){ 
    _set = lex.makeWord(somePuncts[i]);
    _set.addWordCategory(cat_punct, 0);
    _set.addIkoParent(word_punctuation_mark);
  }

  if (simplePhraseDebug) System.out.println("SimplePhrase adding functionWords");

  for (int i=0; i < functionWords.length; i++) 
      (lex.makeWord(functionWords[i])).markDict (feature_Breakword, atom_true, false);

  if (simplePhraseDebug) System.out.println("SimplePhrase adding break words");

  for (int i=0; i < breakStrings.length; i++)
      (lex.makeWord(breakStrings[i])).markDict (feature_Breakword, atom_true, false);

  if (simplePhraseDebug) System.out.println("SimplePhrase adding date numbers");

  /* These are the month day numbers that didn't make it from side-effects already */

  for (int i=13; i < 32;  i++)
      lex.makeWord(i);

  if (simplePhraseDebug) System.out.println("SimplePhrase adding wordclass for titles");

  //  Lexicon.Atom atom_wordClass = lex.makeAtom("wordclass");
  //  Lexicon.Atom atom_abbrev = lex.makeAtom("abbrev");
  Atom _singular = lex.makeAtom("singular");

  //Lexicon.Category cat_n = lex.makeCategory("n");
  //Lexicon.Category cat_prefixtitle = lex.makeCategory("prefixtitle");
  //Lexicon.Category cat_conjunction = lex.makeCategory("conjunction");
  //Lexicon.Category cat_firstname = lex.makeCategory("firstname");

  if (simplePhraseDebug) System.out.print("SimplePhrase adding etc");

  _set = lex.makeWord("etc");
    if (simplePhraseDebug) System.out.print("SimplePhrase adding wordClass to etc");
  _set.markDict(feature_Wordclass, category_Conjunction, false);
  if (simplePhraseDebug) System.out.println("SimplePhrase adding abbrev to etc");
  _set.markDict(feature_Abbrev, lex.makeWord("et-cetera"), true);

  if (simplePhraseDebug) System.out.println("SimplePhrase adding mister");

  _set = lex.makeWord("mr");
  _set.markDict (feature_Wordclass, category_Prefixtitle, false);
  Word _mister = lex.makeWord("mister");
  _mister.markDict (feature_Wordclass, category_Prefixtitle, false);
  _set.markDict (feature_Abbrev, _mister, true);

  if (simplePhraseDebug) System.out.println("SimplePhrase adding mistress");
  _set = lex.makeWord("mrs");
  _set.markDict (feature_Wordclass, category_Prefixtitle, false);
  Word _mistress = lex.makeWord("mistress");
  _mistress.markDict (feature_Wordclass, category_Prefixtitle, false);
  _set.markDict (feature_Abbrev, _mistress, true);

  if (simplePhraseDebug) System.out.println("SimplePhrase adding ms");
  Word _ms = lex.makeWord("ms");
  _ms.markDict (feature_Wordclass, category_Prefixtitle, false);
  if (simplePhraseDebug) System.out.println("SimplePhrase adding ms as abbrev for ms");
  /// kills addToProp
  //  _ms.markDict (feature_Abbrev, _ms, true);  
  // this self abbreviation may be a surprise somewhere !

  if (simplePhraseDebug) System.out.println("SimplePhrase adding professor");  
  Word _professor = lex.makeWord("professor");
  _professor.markDict (feature_Wordclass, category_Prefixtitle, false);
  Word _doctor = lex.makeWord("doctor");
  _doctor.markDict (feature_Wordclass, category_Prefixtitle, false);
  _set = lex.makeWord("prof");
  _set.markDict (feature_Wordclass, category_Prefixtitle, false);
  _set.markDict (feature_Abbrev, _professor, true);
  _set = lex.makeWord("dr");
  _set.markDict (feature_Wordclass, category_Prefixtitle, false);
  _set.markDict (feature_Abbrev, _doctor, true);

  if (simplePhraseDebug) System.out.println("SimplePhrase adding miss");
  _set = lex.makeWord("miss");
  _set.markDict (feature_Wordclass, category_Prefixtitle, false);

  if (simplePhraseDebug) System.out.println("SimplePhrase adding madam");
  Word _madam  = lex.makeWord("madam");
  _madam.markDict (feature_Wordclass, category_Prefixtitle, false);
  Word _madame = lex.makeWord("madame");
  _madame.markDict (feature_Wordclass, category_Prefixtitle, false);
  Word _monsieur = lex.makeWord("monsieur");
  _monsieur.markDict (feature_Wordclass, category_Prefixtitle, false);
  _set = lex.makeWord("madams");
  _set.markDict (feature_Wordclass, category_Prefixtitle, false);
  _set.markDict (_singular, _madam, true);
  Word _mesdames = lex.makeWord("mesdames");
  _mesdames.markDict (feature_Wordclass, category_Prefixtitle, false);
  _mesdames.markDict (_singular, _madame, true);
  Word _monsieurs = lex.makeWord("monsieurs");
  _monsieurs.markDict (feature_Wordclass, category_Prefixtitle, false);
  _monsieurs.markDict (_singular, _monsieur, true);
  _set = lex.makeWord("mme");
  _set.markDict (feature_Wordclass, category_Prefixtitle, false);
  _set.markDict (feature_Abbrev, _madame, true);
  _set = lex.makeWord("mmes");
  _set.markDict (feature_Wordclass, category_Prefixtitle, false);
  _set.markDict (feature_Abbrev, _mesdames, true);
  _set = lex.makeWord("mssr");
  _set.markDict (feature_Wordclass, category_Prefixtitle, false);
  _set.markDict (feature_Abbrev, _monsieur, true);
  _set = lex.makeWord("mssrs");
  _set.markDict (feature_Wordclass, category_Prefixtitle, false);
  _set.markDict (feature_Abbrev, _monsieurs, true);

  if (simplePhraseDebug) System.out.println("SimplePhrase adding president");
  _set = lex.makeWord("president");
  _set.markDict (feature_Wordclass, category_Prefixtitle, false);
  _set = lex.makeWord("vicepresident");
  _set.markDict (feature_Wordclass, category_Prefixtitle, false);

  if (simplePhraseDebug) System.out.println("SimplePhrase finished wordclass for titles");

  // tweaked this entry for modern time .. pmartin 8oct01
  _set = lex.makeWord("alyssa"); 
  _set.markDict (feature_Wordclass, category_Firstname,false);
  _set.addWordCategory(lex.makeCategory("femalefirstname"), 0);
  _set.addIkoParent(lex.makeWord("person"));
}

 void printIndex( Integer  index ,  PrintStream  s ) {
   if     ( index  !=  null )
      s.println( " " + index.toString() );
   s.println( ">" );
 }

  public class AdjPhrase extends Phrase {
    public Word adjective;
    public Value[] advs;


   AdjPhrase ( Word  adjective ,  Value[]  advs ) {
       this.adjective  =  adjective ;
       this.advs  =  advs ;
   }


        @Override
   public synchronized String toString() {
     /* automatic jmap-generated struct Printer for toString method */
       String  tempString1000  =  "AdjPhrase"  +  ":<" ;
      if     ( this.adjective  !=  null )
          tempString1000  =  tempString1000  +  " "  +  "adjective"  +  ":"  + this.adjective.toString();
      if     ( this.advs  !=  null )
          tempString1000  =  tempString1000  +  " "  +  "advs"  +  ":"  + LexiconUtil.toStringArray( this.advs );
       return  (  tempString1000  +  "> "  );
   }


        @Override
   public synchronized String printString() {
     /* automatic jmap-generated struct Printer for printString method */
       String  tempString1001  =  "AdjPhrase"  +  ":<" ;
      if     ( this.adjective  !=  null )
          tempString1001  =  tempString1001  +  " "  +  "adjective"  +  ":"  + this.adjective.printString();
      if     ( this.advs  !=  null )
          tempString1001  =  tempString1001  +  " "  +  "advs"  +  ":"  + LexiconUtil.printStringArray( this.advs );
       return  (  tempString1001  +  "> "  );
   }

 }

 AdjPhrase adjPhraseWithOneAdv( Word  adj ,  Word  adv ) {
    return   new AdjPhrase( adj , (makeSingleton( adv )));
 }


 AdjPhrase adjPhraseAddAdv( AdjPhrase  adjphrase ,  Word  adv ) {
    adjphrase.advs  = nodeSequenceAddNode( adjphrase.advs ,  adv );
    return   adjphrase ;
 }


 AdjPhrase adjPhraseFromAdvs( Value[]  advlist ) {
  /* treats tail of adv list as an adj, and uses remaining advs
   as modifiers of the adj */
    return   new AdjPhrase((Word)(nodeSequenceLast( advlist )), nodeSequenceButLast( advlist ));
 }

  public class AdvPhrase extends Phrase {
    public Word headadv;
    public Value[] qualifadvs;


   AdvPhrase ( Word  headadv ,  Value[]  qualifadvs ) {
       this.headadv  =  headadv ;
       this.qualifadvs  =  qualifadvs ;
   }


        @Override
   public synchronized String toString() {
     /* automatic jmap-generated struct Printer for toString method */
       String  tempString1002  =  "AdvPhrase"  +  ":<" ;
      if     ( this.headadv  !=  null )
          tempString1002  =  tempString1002  +  " "  +  "headadv"  +  ":"  + this.headadv.toString();
      if     ( this.qualifadvs  !=  null )
          tempString1002  =  tempString1002  +  " "  +  "qualifadvs"  +  ":"  + LexiconUtil.toStringArray( this.qualifadvs );
       return  (  tempString1002  +  "> "  );
   }


        @Override
   public synchronized String printString() {
     /* automatic jmap-generated struct Printer for printString method */
       String  tempString1003  =  "AdvPhrase"  +  ":<" ;
      if     ( this.headadv  !=  null )
          tempString1003  =  tempString1003  +  " "  +  "headadv"  +  ":"  + this.headadv.printString();
      if     ( this.qualifadvs  !=  null )
          tempString1003  =  tempString1003  +  " "  +  "qualifadvs"  +  ":"  + LexiconUtil.printStringArray( this.qualifadvs );
       return  (  tempString1003  +  "> "  );
   }

 }

 AdvPhrase advPhraseFromAdvs( Value[]  advlist ) {
  /* treats top of adv stack as main adv, and swaps remaining advs into
   fifo order as qualifiers of the main adv */
    return   new AdvPhrase((Word)(nodeSequenceFirst( advlist )), reverseNodeSequence(nodeSequenceRest( advlist )));
 }


 AdvPhrase advPhraseWithOneQualifAdv( Word  hadv ,  Word  qadv ) {
    return   new AdvPhrase( hadv , (makeSingleton( qadv )));
 }

  public class DatePhrase extends NominalPhrase {
    public Word weekday;
    public Value month;
    public int day;
    public int year;
    public Integer index;


   DatePhrase ( Word  weekday ,  Value  month ,  int  day ,  int  year ,  Integer  index ) {
       this.weekday  =  weekday ;
       this.month  =  month ;
       this.day  =  day ;
       this.year  =  year ;
       this.index  =  index ;
   }


   DatePhrase ( Word  weekday ,  Value  month ,  int  day ,  int  year ) {
     /* short call form */
      this( weekday ,  month ,  day ,  year ,  null );
   }


        @Override
   public synchronized String toString() {
     /* automatic jmap-generated struct Printer for toString method */
       String  tempString1004  =  "DatePhrase"  +  ":<" ;
      if     ( this.weekday  !=  null )
          tempString1004  =  tempString1004  +  " "  +  "weekday"  +  ":"  + this.weekday.toString();
      if     ( this.month  !=  null )
          tempString1004  =  tempString1004  +  " "  +  "month"  +  ":"  + this.month.toString();
      if     (intContainsValue( this.day ))
          tempString1004  =  tempString1004  +  " "  +  "day"  +  ":"  + Integer.toString( this.day );
      if     (intContainsValue( this.year ))
          tempString1004  =  tempString1004  +  " "  +  "year"  +  ":"  + Integer.toString( this.year );
      if     ( this.index  !=  null )
          tempString1004  =  tempString1004  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1004  +  "> "  );
   }


        @Override
   public synchronized String printString() {
     /* automatic jmap-generated struct Printer for printString method */
       String  tempString1005  =  "DatePhrase"  +  ":<" ;
      if     ( this.weekday  !=  null )
          tempString1005  =  tempString1005  +  " "  +  "weekday"  +  ":"  + this.weekday.printString();
      if     ( this.month  !=  null )
          tempString1005  =  tempString1005  +  " "  +  "month"  +  ":"  + this.month.printString();
      if     (intContainsValue( this.day ))
          tempString1005  =  tempString1005  +  " "  +  "day"  +  ":"  + Integer.toString( this.day );
      if     (intContainsValue( this.year ))
          tempString1005  =  tempString1005  +  " "  +  "year"  +  ":"  + Integer.toString( this.year );
      if     ( this.index  !=  null )
          tempString1005  =  tempString1005  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1005  +  "> "  );
   }

 }
  public class NounPhrase extends NominalPhrase {
    public Value determiner;
    public Value[] modifiers;
    public Word noun;
    public Value[] postmods;
    public Integer index;


   NounPhrase ( Value  determiner ,  Value[]  modifiers ,  Word  noun ,  Value[]  postmods ,  Integer  index ) {
       this.determiner  =  determiner ;
       this.modifiers  =  modifiers ;
       this.noun  =  noun ;
       this.postmods  =  postmods ;
       this.index  =  index ;
   }


   NounPhrase ( Value  determiner ,  Value[]  modifiers ,  Word  noun ,  Value[]  postmods ) {
     /* short call form */
      this( determiner ,  modifiers ,  noun ,  postmods ,  null );
   }


        @Override
   public synchronized String toString() {
     /* automatic jmap-generated struct Printer for toString method */
       String  tempString1006  =  "NounPhrase"  +  ":<" ;
      if     ( this.determiner  !=  null )
          tempString1006  =  tempString1006  +  " "  +  "determiner"  +  ":"  + this.determiner.toString();
      if     ( this.modifiers  !=  null )
          tempString1006  =  tempString1006  +  " "  +  "modifiers"  +  ":"  + LexiconUtil.toStringArray( this.modifiers );
      if     ( this.noun  !=  null )
          tempString1006  =  tempString1006  +  " "  +  "noun"  +  ":"  + this.noun.toString();
      if     ( this.postmods  !=  null )
          tempString1006  =  tempString1006  +  " "  +  "postmods"  +  ":"  + LexiconUtil.toStringArray( this.postmods );
      if     ( this.index  !=  null )
          tempString1006  =  tempString1006  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1006  +  "> "  );
   }


        @Override
   public synchronized String printString() {
     /* automatic jmap-generated struct Printer for printString method */
       String  tempString1007  =  "NounPhrase"  +  ":<" ;
      if     ( this.determiner  !=  null )
          tempString1007  =  tempString1007  +  " "  +  "determiner"  +  ":"  + this.determiner.printString();
      if     ( this.modifiers  !=  null )
          tempString1007  =  tempString1007  +  " "  +  "modifiers"  +  ":"  + LexiconUtil.printStringArray( this.modifiers );
      if     ( this.noun  !=  null )
          tempString1007  =  tempString1007  +  " "  +  "noun"  +  ":"  + this.noun.printString();
      if     ( this.postmods  !=  null )
          tempString1007  =  tempString1007  +  " "  +  "postmods"  +  ":"  + LexiconUtil.printStringArray( this.postmods );
      if     ( this.index  !=  null )
          tempString1007  =  tempString1007  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1007  +  "> "  );
   }

 }

 NounPhrase setNounPhraseModifiers( NounPhrase  np ,  Value[]  mods ) {
    np.modifiers  =  mods ;
    return   np ;
 }


 NounPhrase addNounPhraseModifier( NounPhrase  np ,  Value  mod ) {
    np.modifiers  = nodeSequenceAddNode( np.modifiers ,  mod );
    return   np ;
 }


 NounPhrase setNounPhraseDeterminer( NounPhrase  np ,  Value  det ) {
    np.determiner  =  det ;
    return   np ;
 }

  public class PossPhrase extends Phrase {
    public Phrase object;
    public Integer index;


   PossPhrase ( Phrase  object ,  Integer  index ) {
       this.object  =  object ;
       this.index  =  index ;
   }


   PossPhrase ( Phrase  object ) {
     /* short call form */
      this( object ,  null );
   }


        @Override
   public synchronized String toString() {
     /* automatic jmap-generated struct Printer for toString method */
       String  tempString1008  =  "PossPhrase"  +  ":<" ;
      if     ( this.object  !=  null )
          tempString1008  =  tempString1008  +  " "  +  "object"  +  ":"  + this.object.toString();
      if     ( this.index  !=  null )
          tempString1008  =  tempString1008  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1008  +  "> "  );
   }


        @Override
   public synchronized String printString() {
     /* automatic jmap-generated struct Printer for printString method */
       String  tempString1009  =  "PossPhrase"  +  ":<" ;
      if     ( this.object  !=  null )
          tempString1009  =  tempString1009  +  " "  +  "object"  +  ":"  + this.object.printString();
      if     ( this.index  !=  null )
          tempString1009  =  tempString1009  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1009  +  "> "  );
   }

 }
  public class PrepPhrase extends Phrase {
    public Word preposition;
    public NominalPhrase object;
    public Integer index;


   PrepPhrase ( Word  preposition ,  NominalPhrase  object ,  Integer  index ) {
       this.preposition  =  preposition ;
       this.object  =  object ;
       this.index  =  index ;
   }


   PrepPhrase ( Word  preposition ,  NominalPhrase  object ) {
     /* short call form */
      this( preposition ,  object ,  null );
   }


        @Override
   public synchronized String toString() {
     /* automatic jmap-generated struct Printer for toString method */
       String  tempString1010  =  "PrepPhrase"  +  ":<" ;
      if     ( this.preposition  !=  null )
          tempString1010  =  tempString1010  +  " "  +  "preposition"  +  ":"  + this.preposition.toString();
      if     ( this.object  !=  null )
          tempString1010  =  tempString1010  +  " "  +  "object"  +  ":"  + this.object.toString();
      if     ( this.index  !=  null )
          tempString1010  =  tempString1010  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1010  +  "> "  );
   }


        @Override
   public synchronized String printString() {
     /* automatic jmap-generated struct Printer for printString method */
       String  tempString1011  =  "PrepPhrase"  +  ":<" ;
      if     ( this.preposition  !=  null )
          tempString1011  =  tempString1011  +  " "  +  "preposition"  +  ":"  + this.preposition.printString();
      if     ( this.object  !=  null )
          tempString1011  =  tempString1011  +  " "  +  "object"  +  ":"  + this.object.printString();
      if     ( this.index  !=  null )
          tempString1011  =  tempString1011  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1011  +  "> "  );
   }

 }
  public class NpStackElt {
    public Word prep;
    public Value det;
    public Value[] modstack;
    public Word noun;
    public Value[] postmodstack;
    public Word verb;
    public Integer phraseStart;


   NpStackElt ( Word  prep ,  Value  det ,  Value[]  modstack ,  Word  noun ,  Value[]  postmodstack ,  Word  verb ,  Integer  phraseStart ) {
       this.prep  =  prep ;
       this.det  =  det ;
       this.modstack  =  modstack ;
       this.noun  =  noun ;
       this.postmodstack  =  postmodstack ;
       this.verb  =  verb ;
       this.phraseStart  =  phraseStart ;
   }


        @Override
   public synchronized String toString() {
     /* automatic jmap-generated struct Printer for toString method */
       String  tempString1012  =  "NpStackElt"  +  ":<" ;
      if     ( this.prep  !=  null )
          tempString1012  =  tempString1012  +  " "  +  "prep"  +  ":"  + this.prep.toString();
      if     ( this.det  !=  null )
          tempString1012  =  tempString1012  +  " "  +  "det"  +  ":"  + this.det.toString();
      if     ( this.modstack  !=  null )
          tempString1012  =  tempString1012  +  " "  +  "modstack"  +  ":"  + LexiconUtil.toStringArray( this.modstack );
      if     ( this.noun  !=  null )
          tempString1012  =  tempString1012  +  " "  +  "noun"  +  ":"  + this.noun.toString();
      if     ( this.postmodstack  !=  null )
          tempString1012  =  tempString1012  +  " "  +  "postmodstack"  +  ":"  + LexiconUtil.toStringArray( this.postmodstack );
      if     ( this.verb  !=  null )
          tempString1012  =  tempString1012  +  " "  +  "verb"  +  ":"  + this.verb.toString();
      if     ( this.phraseStart  !=  null )
          tempString1012  =  tempString1012  +  " "  +  "phraseStart"  +  ":"  + this.phraseStart.toString();
       return  (  tempString1012  +  "> "  );
   }


   public synchronized String printString() {
     /* automatic jmap-generated struct Printer for printString method */
       String  tempString1013  =  "NpStackElt"  +  ":<" ;
      if     ( this.prep  !=  null )
          tempString1013  =  tempString1013  +  " "  +  "prep"  +  ":"  + this.prep.printString();
      if     ( this.det  !=  null )
          tempString1013  =  tempString1013  +  " "  +  "det"  +  ":"  + this.det.printString();
      if     ( this.modstack  !=  null )
          tempString1013  =  tempString1013  +  " "  +  "modstack"  +  ":"  + LexiconUtil.printStringArray( this.modstack );
      if     ( this.noun  !=  null )
          tempString1013  =  tempString1013  +  " "  +  "noun"  +  ":"  + this.noun.printString();
      if     ( this.postmodstack  !=  null )
          tempString1013  =  tempString1013  +  " "  +  "postmodstack"  +  ":"  + LexiconUtil.printStringArray( this.postmodstack );
      if     ( this.verb  !=  null )
          tempString1013  =  tempString1013  +  " "  +  "verb"  +  ":"  + this.verb.printString();
      if     ( this.phraseStart  !=  null )
          tempString1013  =  tempString1013  +  " "  +  "phraseStart"  +  ":"  + this.phraseStart.toString();
       return  (  tempString1013  +  "> "  );
   }

 }
  public class WordQElt {
    public Word[] punctbuf;
    public Word word;
    public Value capcode;
    public boolean sentencestart;
    public String file;
    public int begin;
    public int end;
    public Word nextchar;
    public boolean finalp;


   WordQElt ( Word[]  punctbuf ,  Word  word ,  Value  capcode ,  boolean  sentencestart ,  String  file ,  int  begin ,  int  end ,  Word  nextchar ,  boolean  finalp ) {
       this.punctbuf  =  punctbuf ;
       this.word  =  word ;
       this.capcode  =  capcode ;
       this.sentencestart  =  sentencestart ;
       this.file  =  file ;
       this.begin  =  begin ;
       this.end  =  end ;
       this.nextchar  =  nextchar ;
       this.finalp  =  finalp ;
   }


        @Override
   public synchronized String toString() {
     /* automatic jmap-generated struct Printer for toString method */
       String  tempString1014  =  "WordQElt"  +  ":<" ;
      if     ( this.punctbuf  !=  null )
          tempString1014  =  tempString1014  +  " "  +  "punctbuf"  +  ":"  + LexiconUtil.toStringArray( this.punctbuf );
      if     ( this.word  !=  null )
          tempString1014  =  tempString1014  +  " "  +  "word"  +  ":"  + this.word.toString();
      if     ( this.capcode  !=  null )
          tempString1014  =  tempString1014  +  " "  +  "capcode"  +  ":"  + this.capcode.toString();
       tempString1014  =  tempString1014  +  " "  +  "sentencestart"  +  ":"  +  this.sentencestart ;
      if     ( this.file  !=  null )
          tempString1014  =  tempString1014  +  " "  +  "file"  +  ":"  + this.file.toString();
      if     (intContainsValue( this.begin ))
          tempString1014  =  tempString1014  +  " "  +  "begin"  +  ":"  + Integer.toString( this.begin );
      if     (intContainsValue( this.end ))
          tempString1014  =  tempString1014  +  " "  +  "end"  +  ":"  + Integer.toString( this.end );
      if     ( this.nextchar  !=  null )
          tempString1014  =  tempString1014  +  " "  +  "nextchar"  +  ":"  + this.nextchar.toString();
       tempString1014  =  tempString1014  +  " "  +  "finalp"  +  ":"  +  this.finalp ;
       return  (  tempString1014  +  "> "  );
   }


   public synchronized String printString() {
     /* automatic jmap-generated struct Printer for printString method */
       String  tempString1015  =  "WordQElt"  +  ":<" ;
      if     ( this.punctbuf  !=  null )
          tempString1015  =  tempString1015  +  " "  +  "punctbuf"  +  ":"  + LexiconUtil.printStringArray( this.punctbuf );
      if     ( this.word  !=  null )
          tempString1015  =  tempString1015  +  " "  +  "word"  +  ":"  + this.word.printString();
      if     ( this.capcode  !=  null )
          tempString1015  =  tempString1015  +  " "  +  "capcode"  +  ":"  + this.capcode.printString();
       tempString1015  =  tempString1015  +  " "  +  "sentencestart"  +  ":"  +  this.sentencestart ;
      if     ( this.file  !=  null )
          tempString1015  =  tempString1015  +  " "  +  "file"  +  ":"  + this.file.toString();
      if     (intContainsValue( this.begin ))
          tempString1015  =  tempString1015  +  " "  +  "begin"  +  ":"  + Integer.toString( this.begin );
      if     (intContainsValue( this.end ))
          tempString1015  =  tempString1015  +  " "  +  "end"  +  ":"  + Integer.toString( this.end );
      if     ( this.nextchar  !=  null )
          tempString1015  =  tempString1015  +  " "  +  "nextchar"  +  ":"  + this.nextchar.printString();
       tempString1015  =  tempString1015  +  " "  +  "finalp"  +  ":"  +  this.finalp ;
       return  (  tempString1015  +  "> "  );
   }

 }
  public class TimePhrase extends NominalPhrase {
    public Word preposition;
    public Word hour;
    public Word dayTime;
    public Word timezone;
    public Integer index;


   TimePhrase ( Word  preposition ,  Word  hour ,  Word  dayTime ,  Word  timezone ,  Integer  index ) {
       this.preposition  =  preposition ;
       this.hour  =  hour ;
       this.dayTime  =  dayTime ;
       this.timezone  =  timezone ;
       this.index  =  index ;
   }


   TimePhrase ( Word  preposition ,  Word  hour ,  Word  dayTime ,  Word  timezone ) {
     /* short call form */
      this( preposition ,  hour ,  dayTime ,  timezone ,  null );
   }


        @Override
   public synchronized String toString() {
     /* automatic jmap-generated struct Printer for toString method */
       String  tempString1016  =  "TimePhrase"  +  ":<" ;
      if     ( this.preposition  !=  null )
          tempString1016  =  tempString1016  +  " "  +  "preposition"  +  ":"  + this.preposition.toString();
      if     ( this.hour  !=  null )
          tempString1016  =  tempString1016  +  " "  +  "hour"  +  ":"  + this.hour.toString();
      if     ( this.dayTime  !=  null )
          tempString1016  =  tempString1016  +  " "  +  "dayTime"  +  ":"  + this.dayTime.toString();
      if     ( this.timezone  !=  null )
          tempString1016  =  tempString1016  +  " "  +  "timezone"  +  ":"  + this.timezone.toString();
      if     ( this.index  !=  null )
          tempString1016  =  tempString1016  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1016  +  "> "  );
   }


        @Override
   public synchronized String printString() {
     /* automatic jmap-generated struct Printer for printString method */
       String  tempString1017  =  "TimePhrase"  +  ":<" ;
      if     ( this.preposition  !=  null )
          tempString1017  =  tempString1017  +  " "  +  "preposition"  +  ":"  + this.preposition.printString();
      if     ( this.hour  !=  null )
          tempString1017  =  tempString1017  +  " "  +  "hour"  +  ":"  + this.hour.printString();
      if     ( this.dayTime  !=  null )
          tempString1017  =  tempString1017  +  " "  +  "dayTime"  +  ":"  + this.dayTime.printString();
      if     ( this.timezone  !=  null )
          tempString1017  =  tempString1017  +  " "  +  "timezone"  +  ":"  + this.timezone.printString();
      if     ( this.index  !=  null )
          tempString1017  =  tempString1017  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1017  +  "> "  );
   }

 }
  public class TimeSpecPhrase extends NominalPhrase {
    public Word prep1;
    public Phrase np1;
    public Word prep2;
    public Phrase np2;
    public Integer index;


   TimeSpecPhrase ( Word  prep1 ,  Phrase  np1 ,  Word  prep2 ,  Phrase  np2 ,  Integer  index ) {
       this.prep1  =  prep1 ;
       this.np1  =  np1 ;
       this.prep2  =  prep2 ;
       this.np2  =  np2 ;
       this.index  =  index ;
   }


   TimeSpecPhrase ( Word  prep1 ,  Phrase  np1 ,  Word  prep2 ,  Phrase  np2 ) {
     /* short call form */
      this( prep1 ,  np1 ,  prep2 ,  np2 ,  null );
   }


        @Override
   public synchronized String toString() {
     /* automatic jmap-generated struct Printer for toString method */
       String  tempString1018  =  "TimeSpecPhrase"  +  ":<" ;
      if     ( this.prep1  !=  null )
          tempString1018  =  tempString1018  +  " "  +  "prep1"  +  ":"  + this.prep1.toString();
      if     ( this.np1  !=  null )
          tempString1018  =  tempString1018  +  " "  +  "np1"  +  ":"  + this.np1.toString();
      if     ( this.prep2  !=  null )
          tempString1018  =  tempString1018  +  " "  +  "prep2"  +  ":"  + this.prep2.toString();
      if     ( this.np2  !=  null )
          tempString1018  =  tempString1018  +  " "  +  "np2"  +  ":"  + this.np2.toString();
      if     ( this.index  !=  null )
          tempString1018  =  tempString1018  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1018  +  "> "  );
   }


        @Override
   public synchronized String printString() {
     /* automatic jmap-generated struct Printer for printString method */
       String  tempString1019  =  "TimeSpecPhrase"  +  ":<" ;
      if     ( this.prep1  !=  null )
          tempString1019  =  tempString1019  +  " "  +  "prep1"  +  ":"  + this.prep1.printString();
      if     ( this.np1  !=  null )
          tempString1019  =  tempString1019  +  " "  +  "np1"  +  ":"  + this.np1.printString();
      if     ( this.prep2  !=  null )
          tempString1019  =  tempString1019  +  " "  +  "prep2"  +  ":"  + this.prep2.printString();
      if     ( this.np2  !=  null )
          tempString1019  =  tempString1019  +  " "  +  "np2"  +  ":"  + this.np2.printString();
      if     ( this.index  !=  null )
          tempString1019  =  tempString1019  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1019  +  "> "  );
   }

 }
  public class NamePhrase extends NominalPhrase {
    public Word prefixtitle;
    public Word firstname;
    public Word[] initials;
    public Word[] lastname;
    public Word[] namesuffix;
    public Integer index;


   NamePhrase ( Word  prefixtitle ,  Word  firstname ,  Word[]  initials ,  Word[]  lastname ,  Word[]  namesuffix ,  Integer  index ) {
       this.prefixtitle  =  prefixtitle ;
       this.firstname  =  firstname ;
       this.initials  =  initials ;
       this.lastname  =  lastname ;
       this.namesuffix  =  namesuffix ;
       this.index  =  index ;
   }


   NamePhrase ( Word  prefixtitle ,  Word  firstname ,  Word[]  initials ,  Word[]  lastname ,  Word[]  namesuffix ) {
     /* short call form */
      this( prefixtitle ,  firstname ,  initials ,  lastname ,  namesuffix ,  null );
   }


        @Override
   public synchronized String toString() {
     /* automatic jmap-generated struct Printer for toString method */
       String  tempString1020  =  "NamePhrase"  +  ":<" ;
      if     ( this.prefixtitle  !=  null )
          tempString1020  =  tempString1020  +  " "  +  "prefixtitle"  +  ":"  + this.prefixtitle.toString();
      if     ( this.firstname  !=  null )
          tempString1020  =  tempString1020  +  " "  +  "firstname"  +  ":"  + this.firstname.toString();
      if     ( this.initials  !=  null )
          tempString1020  =  tempString1020  +  " "  +  "initials"  +  ":"  + LexiconUtil.toStringArray( this.initials );
      if     ( this.lastname  !=  null )
          tempString1020  =  tempString1020  +  " "  +  "lastname"  +  ":"  + LexiconUtil.toStringArray( this.lastname );
      if     ( this.namesuffix  !=  null )
          tempString1020  =  tempString1020  +  " "  +  "namesuffix"  +  ":"  + LexiconUtil.toStringArray( this.namesuffix );
      if     ( this.index  !=  null )
          tempString1020  =  tempString1020  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1020  +  "> "  );
   }


        @Override
   public synchronized String printString() {
     /* automatic jmap-generated struct Printer for printString method */
       String  tempString1021  =  "NamePhrase"  +  ":<" ;
      if     ( this.prefixtitle  !=  null )
          tempString1021  =  tempString1021  +  " "  +  "prefixtitle"  +  ":"  + this.prefixtitle.printString();
      if     ( this.firstname  !=  null )
          tempString1021  =  tempString1021  +  " "  +  "firstname"  +  ":"  + this.firstname.printString();
      if     ( this.initials  !=  null )
          tempString1021  =  tempString1021  +  " "  +  "initials"  +  ":"  + LexiconUtil.printStringArray( this.initials );
      if     ( this.lastname  !=  null )
          tempString1021  =  tempString1021  +  " "  +  "lastname"  +  ":"  + LexiconUtil.printStringArray( this.lastname );
      if     ( this.namesuffix  !=  null )
          tempString1021  =  tempString1021  +  " "  +  "namesuffix"  +  ":"  + LexiconUtil.printStringArray( this.namesuffix );
      if     ( this.index  !=  null )
          tempString1021  =  tempString1021  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1021  +  "> "  );
   }

 }
  public class CityPhrase extends NominalPhrase {
    public Word city;
    public Word statename;
    public Word postalCode;
    public Word country;
    public Integer index;


   CityPhrase ( Word  city ,  Word  statename ,  Word  postalCode ,  Word  country ,  Integer  index ) {
       this.city  =  city ;
       this.statename  =  statename ;
       this.postalCode  =  postalCode ;
       this.country  =  country ;
       this.index  =  index ;
   }


   CityPhrase ( Word  city ,  Word  statename ,  Word  postalCode ,  Word  country ) {
     /* short call form */
      this( city ,  statename ,  postalCode ,  country ,  null );
   }


        @Override
   public synchronized String toString() {
     /* automatic jmap-generated struct Printer for toString method */
       String  tempString1022  =  "CityPhrase"  +  ":<" ;
      if     ( this.city  !=  null )
          tempString1022  =  tempString1022  +  " "  +  "city"  +  ":"  + this.city.toString();
      if     ( this.statename  !=  null )
          tempString1022  =  tempString1022  +  " "  +  "statename"  +  ":"  + this.statename.toString();
      if     ( this.postalCode  !=  null )
          tempString1022  =  tempString1022  +  " "  +  "postalCode"  +  ":"  + this.postalCode.toString();
      if     ( this.country  !=  null )
          tempString1022  =  tempString1022  +  " "  +  "country"  +  ":"  + this.country.toString();
      if     ( this.index  !=  null )
          tempString1022  =  tempString1022  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1022  +  "> "  );
   }


        @Override
   public synchronized String printString() {
     /* automatic jmap-generated struct Printer for printString method */
       String  tempString1023  =  "CityPhrase"  +  ":<" ;
      if     ( this.city  !=  null )
          tempString1023  =  tempString1023  +  " "  +  "city"  +  ":"  + this.city.printString();
      if     ( this.statename  !=  null )
          tempString1023  =  tempString1023  +  " "  +  "statename"  +  ":"  + this.statename.printString();
      if     ( this.postalCode  !=  null )
          tempString1023  =  tempString1023  +  " "  +  "postalCode"  +  ":"  + this.postalCode.printString();
      if     ( this.country  !=  null )
          tempString1023  =  tempString1023  +  " "  +  "country"  +  ":"  + this.country.printString();
      if     ( this.index  !=  null )
          tempString1023  =  tempString1023  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1023  +  "> "  );
   }

 }
  public class PlaceNamePhrase extends NominalPhrase {
    public Word pname;
    public Word statename;
    public Word country;
    public Integer index;


   PlaceNamePhrase ( Word  pname ,  Word  statename ,  Word  country ,  Integer  index ) {
       this.pname  =  pname ;
       this.statename  =  statename ;
       this.country  =  country ;
       this.index  =  index ;
   }


   PlaceNamePhrase ( Word  pname ,  Word  statename ,  Word  country ) {
     /* short call form */
      this( pname ,  statename ,  country ,  null );
   }


        @Override
   public synchronized String toString() {
     /* automatic jmap-generated struct Printer for toString method */
       String  tempString1024  =  "PlaceNamePhrase"  +  ":<" ;
      if     ( this.pname  !=  null )
          tempString1024  =  tempString1024  +  " "  +  "pname"  +  ":"  + this.pname.toString();
      if     ( this.statename  !=  null )
          tempString1024  =  tempString1024  +  " "  +  "statename"  +  ":"  + this.statename.toString();
      if     ( this.country  !=  null )
          tempString1024  =  tempString1024  +  " "  +  "country"  +  ":"  + this.country.toString();
      if     ( this.index  !=  null )
          tempString1024  =  tempString1024  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1024  +  "> "  );
   }


        @Override
   public synchronized String printString() {
     /* automatic jmap-generated struct Printer for printString method */
       String  tempString1025  =  "PlaceNamePhrase"  +  ":<" ;
      if     ( this.pname  !=  null )
          tempString1025  =  tempString1025  +  " "  +  "pname"  +  ":"  + this.pname.printString();
      if     ( this.statename  !=  null )
          tempString1025  =  tempString1025  +  " "  +  "statename"  +  ":"  + this.statename.printString();
      if     ( this.country  !=  null )
          tempString1025  =  tempString1025  +  " "  +  "country"  +  ":"  + this.country.printString();
      if     ( this.index  !=  null )
          tempString1025  =  tempString1025  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1025  +  "> "  );
   }

 }
  public class VerbPhrase extends Phrase {
    public Word verb;
    public NounPhrase object;
    public Integer index;


   VerbPhrase ( Word  verb ,  NounPhrase  object ,  Integer  index ) {
       this.verb  =  verb ;
       this.object  =  object ;
       this.index  =  index ;
   }


   VerbPhrase ( Word  verb ,  NounPhrase  object ) {
     /* short call form */
      this( verb ,  object ,  null );
   }


        @Override
   public synchronized String toString() {
     /* automatic jmap-generated struct Printer for toString method */
       String  tempString1026  =  "VerbPhrase"  +  ":<" ;
      if     ( this.verb  !=  null )
          tempString1026  =  tempString1026  +  " "  +  "verb"  +  ":"  + this.verb.toString();
      if     ( this.object  !=  null )
          tempString1026  =  tempString1026  +  " "  +  "object"  +  ":"  + this.object.toString();
      if     ( this.index  !=  null )
          tempString1026  =  tempString1026  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1026  +  "> "  );
   }


        @Override
   public synchronized String printString() {
     /* automatic jmap-generated struct Printer for printString method */
       String  tempString1027  =  "VerbPhrase"  +  ":<" ;
      if     ( this.verb  !=  null )
          tempString1027  =  tempString1027  +  " "  +  "verb"  +  ":"  + this.verb.printString();
      if     ( this.object  !=  null )
          tempString1027  =  tempString1027  +  " "  +  "object"  +  ":"  + this.object.printString();
      if     ( this.index  !=  null )
          tempString1027  =  tempString1027  +  " "  +  "index"  +  ":"  + this.index.toString();
       return  (  tempString1027  +  "> "  );
   }

 }

 boolean trivialNounPhraseP( Phrase  gnp ) {
  /*  tests a generalized noun phrase to see if it is a nounPhrase 
   with only the noun filled in */
    return  ( ( gnp  instanceof  NounPhrase ) &&
            ( ((NounPhrase) gnp).determiner  ==  null ) &&
            ( ((NounPhrase) gnp).modifiers  ==  null ) &&
            ( ((NounPhrase) gnp).postmods  ==  null ));
 }


 boolean relativeDatePhraseP( DatePhrase  dateph ) {
    return   (relativeDatePhraseNounPhrase( dateph ) instanceof  NounPhrase );
 }

/* prelude */

 DatePhrase assembleRelativeDatePhrase( Word  weekday ,  Value  determiner ,  Value[]  modstack ,  Word  noun ,  Value[]  postmodstack ) {
  /* short call form */
    return  assembleRelativeDatePhrase( weekday ,  determiner ,  modstack ,  noun ,  postmodstack ,  null ,  null );
 }


 DatePhrase assembleRelativeDatePhrase( Word  weekday ,  Value  determiner ,  Value[]  modstack ,  Word  noun ,  Value[]  postmodstack ,  Word  extramod ) {
  /* short call form */
    return  assembleRelativeDatePhrase( weekday ,  determiner ,  modstack ,  noun ,  postmodstack ,  extramod ,  null );
 }


 DatePhrase assembleRelativeDatePhrase( Word  weekday ,  Value  determiner ,  Value[]  modstack ,  Word  noun ,  Value[]  postmodstack ,  Word  extramod ,  Integer  index ) {
    return   new DatePhrase( weekday , (assembleNounPhrase( determiner ,  modstack ,  noun ,  postmodstack ,  extramod )),  sp_unsetNumberValue ,  sp_unsetNumberValue ,  index );
 }

/* prelude */

 NounPhrase createTrivialNounPhrase( Word  noun ) {
  /* short call form */
    return  createTrivialNounPhrase( noun ,  null );
 }


 NounPhrase createTrivialNounPhrase( Word  noun ,  Integer  index ) {
    return   new NounPhrase( null ,  null ,  noun ,  null ,  index );
 }

/* prelude */

 NounPhrase assembleNounPhrase( Value  det ,  Value[]  modstack ,  Word  noun ,  Value[]  postmodstack ) {
  /* short call form */
    return  assembleNounPhrase( det ,  modstack ,  noun ,  postmodstack ,  null ,  null );
 }


 NounPhrase assembleNounPhrase( Value  det ,  Value[]  modstack ,  Word  noun ,  Value[]  postmodstack ,  Word  newmod ) {
  /* short call form */
    return  assembleNounPhrase( det ,  modstack ,  noun ,  postmodstack ,  newmod ,  null );
 }


 NounPhrase assembleNounPhrase( Value  det ,  Value[]  modstack ,  Word  noun ,  Value[]  postmodstack ,  Word  newmod ,  Integer  index ) {
   {  Value[]  mods  = reverseNodeSequence( modstack );
    if     ( newmod  !=  null )
        mods  = nodeSequencePushNode( mods ,  newmod );
     return   new NounPhrase( det ,  mods ,  noun , reverseNodeSequence( postmodstack ),  index );
   }
 }

/*--- formatted by Jindent 2.0b2, (www.c-lab.de/~jindent) ---*/

/* Copyright 1999,2000,2001 Paul Martin for Sun Microsystems Labs */
/* tweaked to avoid name conflict with lisp ClearBuffer  */

/**
 * This class is handbuilt to support the buffers of various kinds used in the
 * Lisp version of the ATN Phrase Extractor.  The file can be swapped for one
 * using Vectors, but the first cut is to use arrays.  Making a class lets us
 * know the type of the elements ... vectors have to be cast to the right thing.
 *
 * This hand-built file should be included (by concatenation or by importing)
 * in the testATN final java file.
 */

// 2oct01 this version expects Lexicon to be external, lex to be my instance

class WordBuffer {
  public int fillPointer;  //points to next spot; 0 when empty
  public Vector bufVector;

  
  WordBuffer() {
    fillPointer = 0;
    bufVector = new Vector();
  }

  WordBuffer(int startSiz) {
    fillPointer = 0;
    bufVector = new Vector(startSiz);
  }

  int fillPointer() {
    return fillPointer;
  }
  
  public Word elementAt(int i) {
    return (Word) bufVector.elementAt(i);
  }

  public void clearWordBuffer(){
    fillPointer = 0;
    bufVector.removeAllElements(); 
  }

  WordBuffer vectorPush(Word newWord){
    bufVector.insertElementAt(newWord, fillPointer++);
    // System.out.println("vectorPush Word Buffer newWord pushed = "+ newWord.toString() +
    //                    " new fill pointer = " + fillPointer + " and size = " +
    //                    bufVector.size());
    return this;
  }

  WordBuffer vectorRest(){
    //System.out.println("Word Buffer Rest called with filPtr = " + fillPointer +
    //		       " and bufVector = " + bufVector.toString());
    int siz = this.fillPointer;
    WordBuffer newWB = new WordBuffer(siz);
    for (int i=1; i<siz; i++){
      newWB.vectorPush(this.elementAt(i));}
    // System.out.println("Word Buffer Rest returning new WordBuffer with fillPointer =" + newWB.fillPointer +
    //		       " and vector = " + newWB.bufVector.toString());
    return newWB;
  }

  Word[] contents(){
    /* copies vector contents into an array of words */
    //System.out.println("Word Buffer contents called with filPtr = " + fillPointer +
    //		       " and vector holding " + bufVector.toString());
    if (fillPointer > 0) {
      while (fillPointer > bufVector.size()){
	System.out.println("chopping down bufVector from " + bufVector.size());
	bufVector.removeElementAt(bufVector.size() - 1);
      }
      Word [] contents = new Word[fillPointer];
      bufVector.copyInto(contents);
      return contents;
    }else{
      return null;
    }
  }

  void restore(Word[] wFiller){
    /* copies a array of words into the current WordBuffer */
    //System.out.println("restore Word Buffer before filPtr = " + fillPointer +
    //		       " bufVector = " + bufVector.toString());
    clearWordBuffer();	
    //System.out.println("restore Word Buffer copying from " + wFiller.toString() +
    //		       " of length " + wFiller.length);
    // may be faster to loop through removing only the ones not written over...
    if (wFiller != null){
      int siz = wFiller.length;
      for (int i=0; i<siz; i++){
	bufVector.insertElementAt(wFiller[i],fillPointer++);
      }
    }
    //System.out.println("restore Word Buffer after filPtr = " + fillPointer +
    //		       " bufVector = " + bufVector.toString());
  }
 
  Value vectorPop(){
    Value le = null;
    //System.out.println("vectorPop Word Buffer before Pop filPtr = " + fillPointer +
    //		       " bufVector = " + bufVector.toString());
    if (fillPointer > 0){
      le = (Value)bufVector.elementAt(--fillPointer);
      bufVector.removeElementAt(fillPointer);
    }
    //System.out.println("vectorPop Word Buffer after Pop filPtr = " + fillPointer +
    //                   " bufVector = " + bufVector.toString());
    return le;
 }
  
}

/*  extra exceptions arising from hand translated code
 *  9 Sept 1999 pmartin
 */ 

  public class IllegalCapcodeException extends RuntimeException {
    public Value exCapcode;


   IllegalCapcodeException(Value capcode) {
      super( "Illegal capcode " + capcode.toString() );
       exCapcode = capcode;
   }

 }
 
  public class StringIsTooShortForCapcodeException extends RuntimeException {
    public String exWstring;
    public Value exCapcode;


  StringIsTooShortForCapcodeException(String wstring, Value capcode){
      super( "String " + wstring + "is too long for capcode" +
                 capcode.toString() );
      exCapcode  = capcode;
      exWstring = wstring;
   }

 }
 
/* end of handExcepts.java .... */
  public class SwitchVarOutOfRangeException extends RuntimeException {
    public State exSelectedState;


   SwitchVarOutOfRangeException ( State  selectedState ) {
      super( "Switch var out of range " + selectedState );
       exSelectedState  =  selectedState ;
   }

 }
  public class ArgMustBeNounPhraseException extends RuntimeException {
    public NounPhrase exMaybenounphrase;


   ArgMustBeNounPhraseException ( NounPhrase  maybenounphrase ) {
      super( "Arg must be noun phrase " + maybenounphrase.toString() + " in moveNpDeterminerToMods " + "\n" );
       exMaybenounphrase  =  maybenounphrase ;
   }

 }
  public class BadHeadTypeInComplexDeterminerException extends RuntimeException {
    public Value exDeterminer;


   BadHeadTypeInComplexDeterminerException ( Value  determiner ) {
      super( "Bad head type in complex determiner " + determiner.toString() + "" + "\n" );
       exDeterminer  =  determiner ;
   }

 }
  public class InterpBreakException extends RuntimeException {
    public Object exArgs;


   InterpBreakException ( Object  args ) {
      super( "interp break " + args.toString() );
       exArgs  =  args ;
   }

 }
  public class AdjoradvwordCannotGetAWordOutOfException extends RuntimeException {
    public Value exV1;


   AdjoradvwordCannotGetAWordOutOfException ( Value  v1 ) {
      super( "adjOrAdvWord cannot get a word out of " + v1.toString() + "" + "\n" );
       exV1  =  v1 ;
   }

 }
  public class DirectoryPathMustEndInColonException extends RuntimeException {
    public String exDirectory;


   DirectoryPathMustEndInColonException ( String  directory ) {
      super( "directory path must end in colon " + directory );
       exDirectory  =  directory ;
   }

 }
 } // end the class ATN
// end of file
