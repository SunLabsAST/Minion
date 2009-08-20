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

import com.sun.labs.minion.util.BitBuffer; // make sure steve's beans are in classpath

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.HashSet;

//  pre-jdk1.4 version
import java.util.Hashtable; // need this for morphCache waw 24feb05
// JDK1.4 and forward uses fast concurrent version
// import com.sun.labs.minion.util.ConcurrentHashtable;
import java.util.concurrent.ConcurrentHashMap;


import java.util.Iterator;
import java.util.Map;
// import java.util.Properties; //pmartin 10oct01
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

/**
To load corelifdict8.jlf (17,000 entries from 10,099 words), you need to
increase memory allocation; running "java -ms10m -mx30m" will do it.

To load biglifworddict.jlf (97,819 entries from 84,524 words), I guessed
running "java -ms60m -mx270m" should do it.  This was sufficient for
ExactVM to load both lexicons: biglifworddict.jlf and biglifformdict.jlf
(76,604 entries from 76,346 words), a total of 174,423 entries.
 */
/**

This Lexicon class is designed to support the needs of morphological
analysis, text scanning, parsing, and conceptual indexing.

This first section describes the Lexicon class and its structure and
operation.  The subsequent section describes an external string format
for reading entries from files and printing them out.

A lexicon is made out of words, categories, atoms and lists.  The class
Lexicon owns the constructors for these objects; they can only be made
with the methods makeWord, makeCategory, makeAtom, and makeList.  This allows
the lexicon to insure that there is only one word object for any given
string (and similarly for categories and atoms) and to use fast eq tests
for object membership in lists.  All four of these Classes are internal
classes of the class Lexicon and they all implement the class Value, which
serves as their common abstraction and the type for elements of lists.
A word's features, properties, and property values are realized as objects
of the types atom and list; they don't have their own object types.

To use a lexicon, one would first make a lexicon object (e.g., testlex) and
then ask it to make words, as in:

Lexicon.Word testword = testlex.makeWord(teststring);

to make a word for the string teststring.  For now, many of Word's instance
variables are public so that you can add syntactic categories and other
properties to the resulting word by setting its public variables.  There
are preferred methods such as setCategories for setting some of them.
Eventually I plan to have a complete set of methods for adding information
as well as for testing and would then make these variables private.

Both words and categories participate in hierarchies.  Methods such as
isFormOfCat will automatically test a word for subcategories of the
specified category.  The method isKindOf will automatically test against
the ancestors of the word's parents.  It is intended that this same Lexicon
interface can later be implemented with categories represented by bitvectors
for more efficient category testing of words.  The method setTestCategories
provides a way to specify which categories are to be treated in this way.

The Lexicon class also includes code for automatically creating disjunctive
categories when makeCategory is called with a slash-separated string of
category names rather than a single category, e.g., makeCategory("n/adj/v").
This will create a new category with the indicated name that automatically
has the categories for "n" "adj" and "v" as subcategories, so that it will
match words that are in any of these categories.  The current code prefers
to have these categories listed in alphabetical order, but will work with
any order (at a small cost in category overhead) and will also work with
category strings with comma, space and tab separators as well (but it will
standardize the disjunctive category name to alphabetical order with
comma separators).  (Acknowledgement: Paul Martin wrote the code to parse
and interpret these disjunctive category names.)

The method names of Word are generally the same as existing lisp function
names in the Pilot indexer, with uppercase "X" replacing lower case "-x"
for all letters x, except where renamed for clarity or consistency as noted
in comments.

Currently, Word's methods for syllabification are only defined as
temporary approximations.  These are marked with the comment: //temporary

The category hierarchy is still tentative in places.

 */

/*  Lexical entry format:

The Lexicon class supports a method makeEntry designed to work with
lexical entries read from an external file.  A lexicon file will consist
of a sequence of entries, one entry per line.  The format for an entry
string (in bnf) is:

<entrystring> ::= <wordstring>:<category>*;<attribute-value-pair>*
<attribute-value-pair> ::= <attribute>:<value>*;

Actually, attribute-value-pairs have several more specific forms:

<attribute-value-pair> ::= p1:<category>*;
<attribute-value-pair> ::= p2:<category>*;
<attribute-value-pair> ::= p3:<category>*;
<attribute-value-pair> ::= wordclass:<category>;  //pmartin 9Nov99
<attribute-value-pair> ::= features:<atom>*;
<attribute-value-pair> ::= capcodes:<capcode>*;
<attribute-value-pair> ::= icodes:<atom>*;
<attribute-value-pair> ::= root:<word>*;
<attribute-value-pair> ::= prefix:<word>*;
<attribute-value-pair> ::= suffix:<word>*;
<attribute-value-pair> ::= iko:<word>*;
<attribute-value-pair> ::= iio:<word>*;
<attribute-value-pair> ::= numval:<number>;
<attribute-value-pair> ::= <atom>:<atom>*;
<attribute-value-pair> ::= senseof:<word>*;  //pmartin 31jul00
<attribute-value-pair> ::= variant-of:<word|wordList>*;  //pmartin 25oct00

<capcode> ::= ic                           //initial capital
<capcode> ::= lc                           //lower case
<capcode> ::= uc                           //all upper case
<capcode> ::= (<integer> <integer>*)       //e.g., (1 3) for "McCarthy"

Spaces and tabs are optional after the colons and semicolons.
Some white space is necessary to separate multiple values of properties.
No white space is permitted before the first colon of an entry (unless
it is intended to be part of the word being defined).

An entry should include at most one <attribute>:<value>* field for any
given attribute.  If there are more than one such pair in an entry,
only the values of the last one will be remembered (each will replace
the values of the previous one).  Similarly, if there is more than one
value for the numval field, only the last value will be remembered.

The method makeEntry in the class Lexicon:

public Word makeEntry (String entryString) {...}

will set up the lexical entry for the indicated word (i.e., will create
a Lexicon.Word object and give it the appropriate properties).

The principal attributes in an entry string are listed above, prior to the
open-ended, general purpose atomic property capability in the last clause.
These set up the principal components of a word, stored in its instance
variables:

p1, p2, p3, [p4 for too far out](for the corresponding penalized categories)
features, (an open-ended array of feature atoms)
capcodes, (a list of capitalization codes: ic,lc,uc, or a list of int atoms)
icodes, (icodes are atoms that name inflection paradigms)
root, prefix, suffix, subsenses, (values of these are words)
iko, (lists words or phrases that this word is a kind of)
iio, (lists words or phrases that this word is an instance of)
numval (numerical value of this word -- there should be at most one of these).

These attributes (except for capcodes) will be treated specially by
makeEntry to set up the appropriate instance variables for the resulting
word.  Any other attributes (plus the capcodes attribute, which can take
lists as values) will be set up as attribute-value pairs in the word's
property list.  The method makeEntry will return the resulting word, whose
instance variables will retain its lexical information and will respond
to methods to test lexical properties.  Here are some examples:

Lexicon testlex = new Lexicon();

Lexicon.Word a = testlex.makeEntry("a: det; p2: nc; subsenses: "+
"                                     !nc/a/grade !nc/a/letter;");
Lexicon.Word !nc/a/grade = testlex.makeEntry("!nc/a/grade: nc; iko: grade;");
Lexicon.Word !nc/a/letter = testlex.makeEntry("!nc/a/letter:nc;iko:letter;");

When a word is inflected, it will be marked with a syntactic category that
indicates it's inflectional features as well as its primary category.  E.g.,

Lexicon.Word broken = testlex.makeEntry("broken: pastpart; root: break;");
Lexicon.Word words = testlex.makeEntry("words: ncp; root: word;");
Lexicon.Word fish = testlex.makeEntry("fish: nmsp;");

A word's lexical information can be examined by sending it the method
printEntryString(), as in:

System.out.println(,"testword4.printEntryString() = " +
testword4.printEntryString());


There is a method loadFile (<filename>) that can be given to a lexicon
to load successive lexical entries from a file.  By convention, such
lexical files should have the extension jlf (for Java lexical format).


Issues:

The lif translator needs to separate irr icodes for noun, verb, adj, adv
into irrn, irrv, irradj, and irradv, depending on the category in which
the irr code occurs.

 */
/*******  The Lexicon class definition follows:  *******/

/* Definition for a natural language lexicon class */
public class Lexicon {

    static int defaultWordTableSize = 10000;

    static int defaultCategoryTableSize = 300;

    static int defaultAtomTableSize = 500;

    static int defaultCatListSize = 20;

    static final int PROPERTIES_HASH_SIZE = 4; // pm 26sep02

    static String defaultTestCategoriesString =
            "adj adj-post adv adv-qualifier " +
            " anyn anync city det firstname lastname month " +
            " n name nameprefix namesuffix " +
            " nc nco nm nn npl npr nsg nsp number postdet prep prespart " +
            " pro statecode title v vi vt weekday " +
            " 1sg/2sg/3sg/comparative/npl/past/pastpart/prespart/superlative/not13sg " +
            " adj/adv/aux/conj/det/interj/anyn/number/prep/v   adj/adv/det/anyn " +
            " adj/adv/det/nm/npl/number/ord/pro   adj/adv/anyn  adj/adv/anyn/v " +
            " adj/adv/nn/v" + // compoundable cat pm 01apr04
            " adj/adv/ord  adj/anyn  adj/anyn/v  adj/number/ord  adj/ord  adv/v " +
            " city/firstname/lastname  country  det/anyn/number " +
            " det/nm/npl/number/pro  det/number  det/number/prep " +
            " det/number/prep/pro  det/number/v  name/nm/v  nc/unit  npl/unit " +
            " predet/pro  statecode/statename";

    static final long LONGHIBIT = 0x8000000000000000L; //pm 14mar02
    //pmartin 10sep01

    static final int DefaultIndexWordLimit = 500000;

    static final int SpareWordsSize = 100;

    static final int IndexCategoryLimit = 500;

    static final int IndexAtomLimit = 20000; // pm 13apr04

    static final int IndexAtomNormal = 1000; // spot an "atom leak"

    static final int BlockBytes = 8192; // bytes

    static final int BlockBits = BlockBytes * 8;  //bits

    static final int NODISKINDEX = 0x40000000;

    static final int DISKOUTDATED = 0x20000000;

    static final int NOWORDENTRY = 0x10000000;

    static final int MARKEDINDEX = NODISKINDEX | DISKOUTDATED | NOWORDENTRY;

    static final int WORDINDEXMASK = ~MARKEDINDEX; // use to recover int

    static final int ATOMVALS = 1,  WORDVALS = 2,  STRUCTVALS = 3,  MULTIWORDVALS =
            4,  LISTVALS = 5,  CATVALS = 6;

    static final int UNPACKALL = 0,  UNPACKCORE = 1,  KEEPPACKED = 2;

    protected int IndexWordLimit = DefaultIndexWordLimit;

    protected int ShrinkLexLimit = IndexWordLimit / 5;  // minimum worth shrinking

    //public boolean productionFlag = false; // true for stripped version
    public boolean productionFlag = true; // false for dictionary debugging

    private ArrayList reloadEntries; //pmartin 20aug01

    private ConcurrentHashMap categoryTable,  atomTable;

    ConcurrentHashMap wordTable;

    private int wordTableSize,  categoryTableSize,  atomTableSize;

    private int categoryFreePos,  canonicalCategories,  atomFreePos;

    int wordFreePos;

    private Category[] testCategories,  nonrootCategories;

    HashSet featCategoriesHash;

    Category nonrootCategory;

    Category integerCategory;

    Category numberCategory;

    protected Category ordinalCategory;  //pmartin 12Apr00

    protected Category compoundableCats;  //pmartin 1Apr04

    protected Category properCats,  nonnameCats; //pmartin 15may02

    protected Category prepCategory,  proCategory;

    protected Category verbCategory,  viCategory,  vtCategory;

    protected Category pastCategory,  pastpartCategory,  pppCategory;

    ;
    protected Category not3sgCategory,  not13sgCategory,  v3sgCategory,  vpast3sgCategory,  v2sgCategory,  vpast2sgCategory;

    protected Category v1sgCategory,  v13sgCategory,  prespartCategory;

    protected Category nameCategory,  anyncCategory,  ncCategory;

    protected Category anynCategory,  nounCategory,  nmCategory,  nnCategory;

    protected Category nspCategory,  nsgCategory,  nplCategory,  nprCategory;

    protected Category alphanumCategory,  syllabicCategory;

    protected Category prefixCategory,  suffixCategory; //pm 01apr04

    protected Category polysyllabicCategory,  monosyllabicCategory;

    Category punctCategory;  //pmartin 25Aug99

    protected Category adjCategory;  //pmartin 6Jan00

    protected Category adjAdvAnyNVCategory; // adj/adv/anyn/v

    protected Category advCategory;  //pmartin 26jul00

    Category unknownCategory;  //pmartin 25Aug99 ...leave out of subsumption

    protected Category wordCategory; //pmartin 04oct03 ... top of the cat subsumption

    protected Atom icAtom,  lcAtom,  mcAtom,  ucAtom; //for capcodes

    Atom abbrevAtom; //pmartin 20sep02

    Atom abbrevsAtom; //pmartin 23sep02

    protected Atom agrAtom; //pmartin 10aug01

    Atom allVariantsAtom;

    Atom authorAtom; //pmartin 1aug00

    protected Atom comparativeAtom;

    protected Atom compoundOfAtom; //pmartin 15may01

    private Atom cstructAtom; //pmartin 14may01

    Atom cutVarOfAtom; //pmartin 15oct02

    protected Atom derivationAtom;

    protected Atom entailsAtom,  entailedbyAtom; //pm 26sep03

    protected Atom falseRootAtom;  //pm 4aug03

    protected Atom formAtom,  formsAtom;  //pmartin 21may01

    protected Atom guessedAtom;  //pmartin 25Aug99

    Atom guessedNameAtom;  //pmartin 21dec00

    protected Atom hasDashPrefixAtom,  hasDashSuffixAtom; // pm 19mar04

    protected Atom icodesAtom,  iioAtom,  ikoAtom; //pm 07apr04

    protected Atom massAtom;

    Atom misspellingsAtom;

    Atom misspellingOfAtom;

    Atom morphServerAtom;  //pmartin 10aug00

    Atom morphStatusAtom;  //pmartin 14dec01 split from author

    Atom nicknameOfAtom;

    Atom nicknamesAtom;

    Atom not13SgAtom;

    Atom not13SingAtom;

    Atom not13SAtom;

    Atom notYetMorphedAtom;  //pmartin 28jun01

    protected Atom numberAtom;  //pmartin 10aug01

    Atom numValErrorAtom; //pmartin 12dec00

    protected Atom oneSAtom,  oneSgAtom,  oneSingAtom; //pmartin 10aug01

    protected Atom one3SAtom,  one3SgAtom,  one3SingAtom; //pmartin 10aug01

    protected Atom pastpartAtom,  pastDashPartAtom; //pmartin 10aug01

    protected Atom penaltyAtom,  penaltiesAtom,  plAtom,  pncodeAtom;

    protected Atom prefixAtom,  prespartAtom,  presDashPartAtom; //pmartin 6Jan00

    protected Atom presentAtom,  pastAtom; //pmartin 6Jan00

    protected Atom relationAtom,  rootAtom;  //pm 7apr04

    protected Atom scalarAtom,  senseofAtom;

    protected Atom sgSlashPlAtom,  suffixAtom;

    protected Atom starIcodeAtom;

    protected Atom superlativeAtom,  takesObjAtom;

    protected Atom tenseAtom,  tnsAtom;

    protected Atom threeSAtom,  threeSgAtom,  threeSingAtom; //pmartin 10aug01

    Atom transmitFlagAtom;  //pmartin 4aug00

    protected Atom trueAtom;  //pmartin 5Jan00

    protected Atom twoSAtom,  twoSgAtom,  twoSingAtom; //pmartin 10aug01

    protected Atom untensedAtom;

    Atom variantsAtom;  //pmartin 20sept02

    Atom variantsLinkedAtom;  //pmartin 23oct02

    private Atom wordClassAtom;  //pmartin 9Nov99

    protected Word abbreviationWord;

    protected Word integerWord; // pmartin 7dec00

    protected Word misspellingWord;

    protected Word modifiedByWord;

    protected Word nicknameWord;

    protected Word personWord;

    protected Word thingWord;

    protected Word word_0,  word_1,  word_2,  word_3; //pm 19mar04

    protected Word[] smallIntegers;

    private List emptyList;   //pmartin 19oct00
    /// 12nov02 private Vector personStopperVect; // pmartin 17oct00

    HashSet personStopperHash; // pmartin 7mar02

    HashSet productionUnneeded; // pmartin 27nov01

    HashSet salvageProperties; // pmartin 26sep02

    private HashSet wordProperties; // pmartin 26sep02

    private HashSet categoryFlagStrings; // category tagging strings

    private HashSet wordFlagStrings;     // word flag tagging strings

    private HashSet multiWordPropsStrings; //pmartin 30sep02

    String closeQuoteStr;

    String openQuoteStr;

    int[] diskIndexTable = null;

    Value[] valueIndexTable = null;

    volatile int highCatIndex = 1;

    volatile int highAtomIndex = IndexCategoryLimit;

    volatile int highWordIndex = IndexAtomLimit;

    private volatile boolean currentlyShrinkingLex = false;

    public RandomAccessFile binLexRAF = null; // pipeline control needs public

    public int madeWordEntries = 0;

    public int reloadedWordEntries = 0;

    public int reloadedBitBuffers = 0;

    public Lexicon() {
        this(defaultWordTableSize, defaultAtomTableSize);
    }

    public Lexicon(int startWordSize) {
        this(startWordSize, defaultAtomTableSize);
    }

    public Lexicon(int startWordSize, int startAtomSize) {
        wordTableSize = startWordSize;
        wordTable = new ConcurrentHashMap(wordTableSize, 0.5f, 16);
        wordFreePos = 0;
        atomTableSize = startAtomSize;
        atomTable = new ConcurrentHashMap(atomTableSize, 0.5f, 16);
        atomFreePos = 0;

        reloadEntries = new ArrayList(); //pmartin 20aug01
        abbrevAtom = makeAtom("abbrev");
        abbrevsAtom = makeAtom("abbrevs");
        agrAtom = makeAtom("agr"); //pmartin 10aug01
        allVariantsAtom = makeAtom("allVariants");
        authorAtom = makeAtom("author"); //pmartin 1aug00
        comparativeAtom = makeAtom("comparative");
        compoundOfAtom = makeAtom("compoundOf"); //pmartin 14may01
        cstructAtom = makeAtom("c-structure"); //pmartin 14may01
        cutVarOfAtom = makeAtom("cutVarOf"); //pmartin 15oct02
        derivationAtom = makeAtom("derivation"); //pm 01apr04
        entailsAtom = makeAtom("entails"); //pm26sep02
        entailedbyAtom = makeAtom("entailedBy"); //pm26sep02
        falseRootAtom = makeAtom("false-root"); //pmartin 4aug03
        formAtom = makeAtom("form"); //pmartin 6Jan00
        formsAtom = makeAtom("forms"); //pmartin 22may01
        guessedAtom = makeAtom("guessed"); //pmartin 25Aug99
        guessedNameAtom = makeAtom("guessedname"); //pmartin 21dec00
        hasDashPrefixAtom = makeAtom("has-prefix");
        hasDashSuffixAtom = makeAtom("has-suffix");
        icAtom = makeAtom("ic");
        iioAtom = makeAtom("iio");
        icodesAtom = makeAtom("icodes");
        ikoAtom = makeAtom("iko");
        lcAtom = makeAtom("lc");
        massAtom = makeAtom("mass");
        mcAtom = makeAtom("mc");
        misspellingOfAtom = makeAtom("misspellingOf");
        misspellingsAtom = makeAtom("misspellings");
        morphServerAtom = makeAtom("morphserver"); //pmartin 10aug00
        morphStatusAtom = makeAtom("morphstatus"); //pmartin 14dec01
        nicknameOfAtom = makeAtom("nicknameOf"); //pmartin 15oct02
        nicknamesAtom = makeAtom("nicknames"); //pmartin 15oct02

        not13SAtom = makeAtom("not13s");  //pmartin 10aug01
        not13SgAtom = makeAtom("not13sg");  //pmartin 10aug01
        not13SingAtom = makeAtom("not13sing");  //pmartin 10aug01
        notYetMorphedAtom = makeAtom("notYetMorphed");
        numValErrorAtom = makeAtom("numvalerror"); //pmartin 12dec00
        numberAtom = makeAtom("number"); //pmartin 10aug01
        oneSAtom = makeAtom("1s");  //pmartin 10aug01
        oneSgAtom = makeAtom("1sg");  //pmartin 10aug01
        oneSingAtom = makeAtom("1sing");  //pmartin 10aug01
        one3SAtom = makeAtom("13s");  //pmartin 10aug01
        one3SgAtom = makeAtom("13sg");  //pmartin 10aug01
        one3SingAtom = makeAtom("13sing");  //pmartin 10aug01
        pastAtom = makeAtom("past");  //pmartin 6Jan00
        pastpartAtom = makeAtom("pastpart");  //pmartin 10aug01
        pastDashPartAtom = makeAtom("past-part");  //pmartin 6Jan00
        penaltyAtom = makeAtom("penalty");
        penaltiesAtom = makeAtom("penalties");
        plAtom = makeAtom("pl"); // 10aug01
        pncodeAtom = makeAtom("pncode"); // pm 19mar04
        prefixAtom = makeAtom("prefix");
        presentAtom = makeAtom("present");  //pmartin 6Jan00
        prespartAtom = makeAtom("prespart");   //pmartin 10aug01
        presDashPartAtom = makeAtom("pres-part");   //pmartin 6Jan00
        relationAtom = makeAtom("relation");
        rootAtom = makeAtom("root");
        scalarAtom = makeAtom("scalar");
        senseofAtom = makeAtom("senseof");  //pmartin 31jul00
        sgSlashPlAtom = makeAtom("sg/pl");  //pmartin 10aug01
        starIcodeAtom = makeAtom("*");
        suffixAtom = makeAtom("suffix");
        superlativeAtom = makeAtom("superlative");
        takesObjAtom = makeAtom("takesobj"); //pmartin 30sep02
        tenseAtom = makeAtom("tense");  //pmartin 6Jan00
        threeSAtom = makeAtom("3s");  //pmartin 10aug01
        threeSgAtom = makeAtom("3sg");  //pmartin 10aug01
        threeSingAtom = makeAtom("3sing");  //pmartin 6Jan00
        tnsAtom = makeAtom("tns");  //pmartin 10aug01
        twoSAtom = makeAtom("2s");  //pmartin 10aug01
        twoSgAtom = makeAtom("2sg");  //pmartin 10aug01
        twoSingAtom = makeAtom("2sing");  //pmartin 10aug01
        transmitFlagAtom = makeAtom("transmitflag"); // pmartin 4aug00
        trueAtom = makeAtom("true"); //pmartin 5Jan00 marking "plus" properties
        ucAtom = makeAtom("uc");
        untensedAtom = makeAtom("untensed");
        variantsAtom = makeAtom("variants");
        variantsLinkedAtom = makeAtom("variantslinked");
        wordClassAtom = makeAtom("wordclass"); //pmartin 9Nov99

        emptyList = makeList(new Value[0]);  //pmartin 19oct00 SHARED


        openQuoteStr = "|";
        closeQuoteStr = "|";

        // pmartin 20jan00 added size fiddling to category table
        float atomFactor = atomTableSize / defaultAtomTableSize;
        categoryTableSize = defaultCategoryTableSize;
        if(atomFactor > 1.0) {
            categoryTableSize =
                    Math.round(atomFactor * defaultCategoryTableSize);
        }
        categoryTable = new ConcurrentHashMap(categoryTableSize, 0.5f, 16);
        categoryFreePos = 0;
        canonicalCategories = 0;
        if(authorFlag) {
            if(debug) {
                logger.finest("New lexicon of size " + wordTableSize +
                        " = " + this.toString());
            }
        }

        setCategories("n nc nco nm nmc nmp nms nmsp nn npl nsg nsp npr " +
                "       v vi vit vt vti vtinp vtnp modal aux past partpart prespart ing ppp " +
                "       1sg 2sg past2sg 3sg past3sg 13sg not3sg not13sg " +
                "       adj adj-post adj-pred adv adv-clausal adv-clause-final " +
                "       adv-part adv-pre adv-pred adv-qualifier adv-special " +
                "       neg neg-adv neg-aux neg-v prep punct qdet qword qpro qadv " +
                "       pro ord comp compl op comma colon semi space tab newline" +
                "       poss det predet postdet conj subconj interj prefix suffix " +
                "       unit number integer digits teens tens alphanum " +
                "       city statename statecode country weekday month date phonenumber " +
                "       name title firstname malefirstname femalefirstname initial lastname " +
                "       doubledualname maledualname femaledualname dualfirstname dualname " +
                "       nameprefix namesuffix syllabic monosyllabic polysyllabic" +
                "       unknown word");  // unknown added pmartin 25Aug99

        nonrootCategories = getCategories("1sg 2sg 3sg 13sg cadj cadv " +
                "comparative ing " +
                "npl past pastpart prespart ppp " +
                "sadj sadv superlative not13sg", " ");
// expanded to cover multi-cats that arise in inflectionRoot testing.
// pmartin 7mar02
        nonrootCategory = makeCategory("1sg/2sg/past2sg/3sg/past3sg/13sg/" +
                "comparative/cadj/cadv/sadj/sadv/" +
                "npl/past/pastpart/prespart/ppp/ing/" +
                "superlative/not13sg");

        featCategoriesHash = new HashSet();
        featCategoriesHash.add(makeCategory("1sg"));
        featCategoriesHash.add(makeCategory("2sg"));
        featCategoriesHash.add(makeCategory("past2sg"));
        featCategoriesHash.add(makeCategory("3sg"));
        featCategoriesHash.add(makeCategory("past3sg"));
        featCategoriesHash.add(makeCategory("13sg"));
        featCategoriesHash.add(makeCategory("not13sg"));
        featCategoriesHash.add(makeCategory("not3sg"));

        numberCategory = makeCategory("number");
        ordinalCategory = makeCategory("ord"); //pmartin 12Apr00
        integerCategory = makeCategory("integer");
        alphanumCategory = makeCategory("alphanum");
        anynCategory = makeCategory("anyn"); // pm 27apr04
        anyncCategory = makeCategory("anync"); // pm 27apr04
        nameCategory = makeCategory("name"); //pmartin 7mar02
        nounCategory = makeCategory("n");
        ncCategory = makeCategory("nc");
        nmCategory = makeCategory("nm");
        verbCategory = makeCategory("v");
        viCategory = makeCategory("vi");
        vtCategory = makeCategory("vt");
        pastCategory = makeCategory("past");
        pastpartCategory = makeCategory("pastpart");
        pppCategory = makeCategory("past/pastpart");
        prepCategory = makeCategory("prep");
        proCategory = makeCategory("pro");
        not3sgCategory = makeCategory("not3sg");
        not13sgCategory = makeCategory("not13sg");
        v3sgCategory = makeCategory("3sg");
        vpast3sgCategory = makeCategory("past3sg");
        v2sgCategory = makeCategory("2sg");
        vpast2sgCategory = makeCategory("past2sg");
        v1sgCategory = makeCategory("1sg");
        v13sgCategory = makeCategory("13sg");
        prespartCategory = makeCategory("prespart");
        nspCategory = makeCategory("nsp");
        nsgCategory = makeCategory("nsg");
        nplCategory = makeCategory("npl");
        nprCategory = makeCategory("npr");
        nnCategory = makeCategory("nn");
        prefixCategory = makeCategory("prefix"); // pm 01apr04
        suffixCategory = makeCategory("suffix"); //pm 01apr04
        syllabicCategory = makeCategory("syllabic");
        monosyllabicCategory = makeCategory("monosyllabic");
        polysyllabicCategory = makeCategory("polysyllabic");
        punctCategory = makeCategory("punct"); //pmartin 25Aug99
        unknownCategory = makeCategory("unknown"); //pmartin 25Aug99
        wordCategory = makeCategory("word"); //pmartin 04oct02
        adjCategory = makeCategory("adj"); //pmartin 6Jan00
        adjAdvAnyNVCategory = makeCategory(" adj/adv/anyn/v ");
        advCategory = makeCategory("adv"); //pmartin 26jul00
        compoundableCats = makeCategory("adj adv nn v"); // pm 1apr04
        properCats = makeCategory("name month weekday title city statename" +
                " country statecode initial firstname" +
                " lastname malefirstname femalefirstname" +
                " doubledualname femaledualname" +
                " dualfirstname maledualname dualname");
        nonnameCats = makeCategory("nn "); // altered => all non-npr cats
        // testor of cat canonicalization
        makeCategory("date/month/weekday");
        makeCategory("month date weekday");
        makeCategory("weekday,month,date");


        setSubcategories("word", "anyn v aux adj adv conj det prep pro qword " +
                " comparative superlative punct syllabic name");
        // top of heap


        setSubcategories("anyn", "anync nn");  // they're all normal or broadly-nc
        // we suspect that "nco" is no longer used
        setSubcategories("anync", "nc npr");
        setSubcategories("nco", "alphanum nn"); // common noun, nn is normal noun
        setSubcategories("nn", "n");
        setSubcategories("n", "nc nm");
        setSubcategories("nc", " number unit alphanum title" +
                " ncm nmc npl np ns nsp" +
                " nspm npsm nps");
        setSubcategories("nm", "ncm nmc nmp nms nmsp npm nsm nspm npsm nmps");
        setSubcategories("npl", "nsp nmp nmsp");
        setSubcategories("nsg", "nsp nc ncm nm nmc nms nmsp npr");
        setSubcategories("number", "integer");
        setSubcategories("integer", "digits tens teens");
        setSubcategories("npr",
                " title month weekday city statename country statecode " +
                " name phonenumber date alphanum number");
        setSubcategories("v", "vi vt");
        setSubcategories("vt", "vit vti vtinp vtnp"); // root of transitive verb
        setSubcategories("vi", "vit vti vtinp");      // root of intransitive verb
        //setSubcategories("past", "ppp");     // ppp is past and pastpart
        // added all this stuff under vti --  pmartin 14nov00 so "joined" isrealverb
        setSubcategories("vit", "past pastpart prespart neg-v " +
                "1sg 2sg 3sg 13sg not13sg not3sg");
        setSubcategories("vti", "past pastpart prespart neg-v " +
                "1sg 2sg 3sg 13sg not13sg not3sg");
        setSubcategories("past", "ppp past2sg past3sg"); //pmartin 19mar04
        setSubcategories("pastpart", "ppp");  // for marking typical -ed verbs
        setSubcategories("prespart", "ing"); // ing is a shorthand for prespart
        setSubcategories("neg-v", "neg-aux");
        setSubcategories("aux", "modal neg-aux");
        // added to pmartin 14nov00
        //setSubcategories("adj", "adj-post adj-pred ord");
        setSubcategories("adj", "adj-post adj-pred ord cadj sadj");
        // setSubcategories("adv", "adv-clausal adv-clause-final adv-part "+
        //   " adv-pre adv-pred adv-qualifier adv-special neg neg-adv qadv");
        setSubcategories("adv", "adv-clausal adv-clause-final adv-part " +
                " adv-pre adv-pred adv-qualifier adv-special neg neg-adv qadv" +
                "cadv sadv");
        setSubcategories("conj", "subconj");
        setSubcategories("det", "predet postdet qdet");
        setSubcategories("prep", "detprep op");
        setSubcategories("pro", "qpro");
        setSubcategories("qword", "qdet qpro qadv");
        setSubcategories("comparative", "cadj cadv");
        setSubcategories("superlative", "sadj sadv");
        setSubcategories("punct", "colon comma semi period whitespace");
        setSubcategories("whitespace", "space tab newline");
        setSubcategories("syllabic", "monosyllabic polysyllabic");
        setSubcategories("name", "initial firstname lastname");
        setSubcategories("firstname", "malefirstname femalefirstname");
        setSubcategories("femalefirstname",
                "doubledualname femaledualname dualfirstname");
        setSubcategories("malefirstname",
                "doubledualname maledualname dualfirstname");
        setSubcategories("lastname", "doubledualname " +
                "maledualname femaledualname dualname");

        abbreviationWord = makeWord("abbreviation"); // for wired-in iio
        integerWord = makeWord("integer"); // for wired-in iio of integers
        modifiedByWord = makeWord("modified_by"); // for compounding
        misspellingWord = makeWord("misspelling"); // for wired-in iio
        nicknameWord = makeWord("nickname"); // for wired-in iio
        personWord = makeWord("person");
        thingWord = makeWord("thing");
        word_0 = makeWord(0);
        word_1 = makeWord(1);
        word_2 = makeWord(2);
        word_3 = makeWord(3);
        smallIntegers = new Word[]{
                    makeWord(0), makeWord(1), makeWord(2), makeWord(3),
                    makeWord(4),
                    makeWord(5), makeWord(6), makeWord(7), makeWord(8),
                    makeWord(9)};

//     /* set up the person stopper vector for faster searches */
//     personStopperVect = new Vector(defaultCatListSize);
//     personStopperVect.addElement(personWord);
//     personStopperVect.addElement(makeWord("male_person"));
//     personStopperVect.addElement(makeWord("female_person"));
//     personStopperVect.addElement(makeWord("!nc/male"));
//     personStopperVect.addElement(makeWord("!nc/female"));
//     personStopperVect.addElement(thingWord);

        personStopperHash = new HashSet(10);
        personStopperHash.add(personWord);
        personStopperHash.add(makeWord("male_person"));
        personStopperHash.add(makeWord("female_person"));
        personStopperHash.add(makeWord("!nc/male"));
        personStopperHash.add(makeWord("!nc/female"));


        // all the properties that should be cleaned off for production speed/size
        productionUnneeded = new HashSet(10);
        productionUnneeded.add(makeAtom("author"));
        productionUnneeded.add(makeAtom("domain"));
        productionUnneeded.add(makeAtom("in-state"));
        // productionUnneeded.add(makeAtom("variants"));
        productionUnneeded.add(makeAtom("derivation"));


        // all the attributes whose values should be words (or List of words)
        wordProperties = new HashSet(30);
        wordProperties.add("abbrev");
        wordProperties.add("can-be-misspelling-of");
        wordProperties.add("compounds");
        wordProperties.add("compound-of");
        wordProperties.add("derived-from");
        wordProperties.add("derivedfrom");
        wordProperties.add("domain");
        wordProperties.add("entails");
        wordProperties.add("false-root");
        wordProperties.add("from");
        wordProperties.add("in-state");
        wordProperties.add("misspellingof");
        wordProperties.add("misspelling-of");
        wordProperties.add("nickname-of");
        wordProperties.add("nicknameof");
        wordProperties.add("nicknames");
        wordProperties.add("sense");
        wordProperties.add("senseof");
        wordProperties.add("subsenses");
        wordProperties.add("takes-obj");
        wordProperties.add("variantof");
        wordProperties.add("variants");


        // category tagging strings
        categoryFlagStrings = new HashSet(10);
        categoryFlagStrings.add("p1");
        categoryFlagStrings.add("p2");
        categoryFlagStrings.add("p3");
        categoryFlagStrings.add("p4");
        categoryFlagStrings.add("wordclass");

        // word flag tagging strings
        wordFlagStrings = new HashSet(20);
        wordFlagStrings.add("relation");
        wordFlagStrings.add("root");
        wordFlagStrings.add("has-root");
        wordFlagStrings.add("prefix");
        wordFlagStrings.add("has-prefix");
        wordFlagStrings.add("suffix");
        wordFlagStrings.add("has-suffix");
        wordFlagStrings.add("subsenses");

        // all the properties that need to force a multi-word value
        multiWordPropsStrings = new HashSet(20);
        multiWordPropsStrings.add("abbrev");
        multiWordPropsStrings.add("can-be-misspelling-of");
        multiWordPropsStrings.add("entails");
        multiWordPropsStrings.add("iko");
        multiWordPropsStrings.add("kindof");
        multiWordPropsStrings.add("iio");
        multiWordPropsStrings.add("instanceof");
        multiWordPropsStrings.add("misspellingof");
        multiWordPropsStrings.add("misspelling-of");
        multiWordPropsStrings.add("nickname-of");
        multiWordPropsStrings.add("nicknameof");
        multiWordPropsStrings.add("nicknames");
        multiWordPropsStrings.add("variantof");
        multiWordPropsStrings.add("variant-of");
        multiWordPropsStrings.add("takesobj");
        multiWordPropsStrings.add("takes-obj");

        // all the properties that should survive a soft clearWord()
        salvageProperties = new HashSet(10);
        salvageProperties.add(abbrevAtom);
        salvageProperties.add(abbrevsAtom);
        salvageProperties.add(cutVarOfAtom);
        salvageProperties.add(entailedbyAtom);
        salvageProperties.add(entailsAtom);
        salvageProperties.add(misspellingOfAtom);
        salvageProperties.add(misspellingsAtom);
        salvageProperties.add(nicknameOfAtom);
        salvageProperties.add(nicknamesAtom);
        salvageProperties.add(senseofAtom);
        salvageProperties.add(takesObjAtom);
        // currently a basic slot of WordEntry
        //      salvageProperties.add(variantofAtom);
        salvageProperties.add(variantsAtom);
    }

    public Word knownOrScratchWord(String str) { // pmartin 16July01
        Word wd = getWord(str);
        if(wd != null) {
            return wd;
        } else {
            return makeScratchWord(str);
        }
    }

    public Word knownOrScratchWord(String str, Hashtable morphCache) { // waw 11Mar05
        Word wd = getWord(str);
        if(wd != null) {
            return wd;
        } else {
            return findOrMakeScratchWord(str, morphCache);
        }
    }

    public Word findOrMakeScratchWord(String str, Hashtable morphCache) { // waw 12Mar05
        if(morphCache == null) {
            return makeScratchWord(str);
        }
        Word wd = (Word) morphCache.get(str);
        if(wd != null) {
            return wd;
        } else {
            return makeScratchWord(str, morphCache);
        }
    }

    public Word makeScratchWord(String str) { // pmartin 4jan01
        return new Word(this, str); // allow non-lower case
    }

    public Word makeScratchWord(String str, Hashtable morphCache) { // waw 11Mar05
        Word wd = new Word(this, str, morphCache); // allow non-lower case
        return wd;
    }

    public Word makeWord(String wordstring) {
        return innerMakeWord(wordstring.toLowerCase()); //pmartin 13sep99
    }

    public Word makeWord(String wordstring, Hashtable morphCache) {
        return innerMakeWord(wordstring.toLowerCase(), morphCache); // waw 19Feb05
    }

    public Word makeWord(WordToken t) {
        if(t.word == null) {
            t.word = innerMakeWord(t.lcToken);
        }
        return t.word;
    }

    public Word makeSenseWordIfNeeded(Category c, String qstr) {
        Word alreadyWd = getWord(qstr);
        if(c == wordCategory && qstr.indexOf("/") < 0) {
            return makeWord(qstr); // even if it doesn't already exist
        }
        if(alreadyWd != null && alreadyWd.isFormOfCat(c) &&
                !(alreadyWd.isAmbiguous())) {
            return alreadyWd;
        } else {
            return innerMakeWord("!" + c.printString() + "/" + qstr);
        }
    }

    public Word makePhraseWord(Word[] pwords) {// pmartin  14may01
        String fs = LexiconUtil.getWordStringArray(pwords);
        Word pw = makeWord(fs);
        Word[] phraseCopy = (Word[]) pwords.clone();
        pw.addCompoundOf(phraseCopy);
        return pw;
    }

    public Word installWord(Word val, String str) { //pmartin 2jan01
        if((wordFreePos > 1) && ((wordFreePos % wordTableSize) == 1)) {
            logger.finest("\n****growing word table: original " + wordTableSize +
                    ": to " + wordFreePos +
                    ":  making " + str);
        }
        wordTable.put(str, val);
        wordFreePos++;
        if((wordFreePos % 10000) == 0) {
            logger.finer((wordFreePos / 1000) + "K = " + str + "  ");
        }
        return val;
    }

    public Word innerMakeWord(String str) {
        return innerMakeWord(str, null);
    }

    public Word innerMakeWord(String str, Hashtable morphCache) {

        Word val = (Word) wordTable.get(str);

        if(val == null && morphCache != null) { // waw 02-18-05
            // word is not in dict, but may be in morphCache
            val = (Word) morphCache.get(str);
        }

        if(val == null && morphCache != null) { // waw 02-18-05
            // word is not in morphCache either, so make one
            val = makeScratchWord(str, morphCache);
        }
// if val is still null, then morphCache is null, so make a real word
        if(val == null) {  //pmartin 20jan00

            val = new Word(this, str);
            installWord(val, str);
            Double numericalValue = null;
            if(LexiconUtil.startsLikeNumber(str)) {
                try {
                    numericalValue = new Double(str);
                } //see if this word should have a numericalNumber value
                catch(NumberFormatException e) {
                }
            }
            // gotta cover at least these "automatic" cases
            // (tbd? waw 19Mar05: but these cases are already covered in new Word(str), above)
            if((numericalValue != null) ||
                    (LexiconUtil.senseNameStringp(str)) ||
                    (LexiconUtil.compoundStringp(str))) {
                val.makeWordEntry();
            }
        }
        return val;
    }

    public Word makeWord(int num) {
        return innerMakeWord(String.valueOf(num));
    }

    public Word makeWord(Number num) {
        String numString = String.valueOf(num);
        if(numString.endsWith(".0")) {
            numString = numString.substring(0, numString.length() - 2);
        }
        return innerMakeWord(numString);
    }

    public Word makeWord(Word[] pwords) {
        // pmartin 8sep99 to handle phrase making calls in ATN
        // pmartin 17may01 changed to include compoundOf
        // ?? StringBuffer sb = new StringBuffer(pwords[0].wordstring);
        return makePhraseWord(pwords);
    }

    public Word makeWord(List wdList) {
        // pmartin 24oct00 to handle phrase making in variant-of
        StringBuffer sb = new StringBuffer(
                ((Word) wdList.elementAt(0)).wordstring);
        for(int i = 1; i < wdList.length(); i++) {
            sb = sb.append("_").append(((Word) wdList.elementAt(i)).wordstring);
        }
        return makeWord(sb.toString());
    }

    public Word makeWord(Vector wdVect) {
        // pmartin 25oct00 to handle phrase making in variant-of
        //  String vstr = "< ";
        //  for (int iv=0; iv < wdVect.size(); iv++)
        //          vstr = vstr + ((Word)wdVect.elementAt(iv)).wordstring + " ";
        StringBuffer sb = new StringBuffer(
                ((Word) wdVect.elementAt(0)).wordstring);
        for(int i = 1; i < wdVect.size(); i++) {
            sb = sb.append("_").append(((Word) wdVect.elementAt(i)).wordstring);
        }
        return makeWord(sb.toString());
    }

    public Word makeWord(ArrayList wdAL) {
        //  to handle phrase making in variant-of
        if(wdAL.size() < 1) {
            return null;
        }
        StringBuffer sb = new StringBuffer(((Word) wdAL.get(0)).wordstring);
        for(int i = 1; i < wdAL.size(); i++) {
            sb = sb.append("_").append(((Word) wdAL.get(i)).wordstring);
        }
        return makeWord(sb.toString());
    }

    public Word makeOrdinalWord(int intOrdinal) {
        return makeOrdinalWord((long) intOrdinal);
    }

    public Word makeOrdinalWord(double doubOrdinal) {
        if((doubOrdinal < Long.MAX_VALUE) &&
                ((long) doubOrdinal == doubOrdinal)) {
            return makeOrdinalWord((long) doubOrdinal);
        } else {
            String numOrd = Double.toString(doubOrdinal) + "th";  //big ugly ones
            return getOrMakeOrdinal(numOrd, new Double(doubOrdinal));
        }
    }

    public Word makeOrdinalWord(long longOrdinal) {
        /* make-ordinal (long) makes an ordinal name for the specfied
         *  long integer. Code lifted from
         *  macnet/transfer/indexing/morphologyfns.lisp 12Apr00
         *  And fixed for teens pmartin 12Apr00
         */

        long units = longOrdinal % 10;
        long tens = (longOrdinal % 100) - units;
        String tail, numOrdStr;

        if(tens == 10) {
            tail = "th";  // case for all teens
        } else if(units == 1) {
            tail = "st";
        } else if(units == 2) {
            tail = "nd";
        } else if(units == 3) {
            tail = "rd";
        } else {
            tail = "th";
        }

        numOrdStr = (String.valueOf(longOrdinal)).concat(tail);

        if(longOrdinal < Integer.MAX_VALUE) {
            Integer valInt = new Integer((int) longOrdinal);
            return getOrMakeOrdinal(numOrdStr, valInt);
        } else {
            Double valDoub = new Double(Long.toString(longOrdinal));
            return getOrMakeOrdinal(numOrdStr, valDoub);
        }
    }

    public Word getOrMakeOrdinal(String numOrdStr, Integer intVal) {
        Word ordWord = getWord(numOrdStr);
        if(ordWord == null) {
            ordWord = makeWord(numOrdStr);
            WordEntry ordwe = ordWord.makeWordEntry();
            ordwe.numericalValue = intVal;
            ordwe.numeralp = false;
            ordwe.addWordCategory(ordinalCategory, 0);
        }
        return ordWord;
    }

    public Word getOrMakeOrdinal(String numOrdStr, Double doubVal) {
        Word ordWord = getWord(numOrdStr);
        if(ordWord == null) {
            ordWord = makeWord(numOrdStr);
            WordEntry ordwe = ordWord.makeWordEntry();
            ordwe.numericalValue = doubVal;
            ordwe.numeralp = false;
            ordwe.addWordCategory(ordinalCategory, 0);
        }
        return ordWord;
    }

    /* addition to handle word sets from ATN Phrase Extractor ...
    30June99 pmartin
    added innerMakeWordSet, makeTabWordSet, and makeSpaceWordSet 17 july 03 for
    morph to use with sense words (which contain slashes inside them)
     */
    public Word[] makeWordSet(String slashSepWordString) {
        return makeWordSet(slashSepWordString, "/,");
    }

    public Word[] makeSpaceWordSet(String spaceSepWordString) {
        return innerMakeWordSet(spaceSepWordString, " ");
    }

    public Word[] makeTabWordSet(String tabSepWordString) {
        return innerMakeWordSet(tabSepWordString, "\t");
    }

    public Word[] makeTabWordSequence(String tabSepWordString) {
        return innerMakeWordSet(tabSepWordString, "\t");
    }

    public Word[] makeWordSet(String sepWordString, String sep) {
        return makeWordSet(sepWordString, sep, null);
    }

    public Word[] makeWordSet(String sepWordString, String sep,
            Hashtable morphCache) {
        String sepChars = sep + " ,/\t\n\r";
        return innerMakeWordSet(sepWordString, sepChars, morphCache);
    }

    public Word[] innerMakeWordSet(String sepWordString, String sepChars) {
        return innerMakeWordSet(sepWordString, sepChars, null);
    }

    public Word[] innerMakeWordSet(String sepWordString, String sepChars,
            Hashtable morphCache) {
        String newWordString;
        ArrayList wordSet = new ArrayList(defaultCatListSize);
        StringTokenizer wordStringTokens =
                new StringTokenizer(sepWordString, sepChars);
        while(wordStringTokens.hasMoreTokens()) {
            newWordString = wordStringTokens.nextToken();
            wordSet.add(makeWord(newWordString, morphCache));
        }
        int siz = wordSet.size();
        if(siz > 0) { //set up root categories
            Word[] wordArray = new Word[siz];
            ;
            wordArray = (Word[]) (wordSet.toArray(wordArray));
            return wordArray;
        } else {
            return null;
        }
    }

    public Word getWord(String wordstring) {
        return (Word) wordTable.get((wordstring.toLowerCase())); //pmartin 13sep99
    }

    public Word getWord(WordToken t) {
        if(t.word == null) {
            t.word = (Word) wordTable.get(t.lcToken);
        }
        return t.word;
    }

    public Word getWord(int num) {
        return getWord(String.valueOf(num));
    }

    public Word getWord(Number num) {
        return getWord(String.valueOf(num));
    }

    public Word getOrGuessWord(String wordstr) { //pmartin 5Jan00
        Word w = getWord(wordstr);
        if(w == null) {
            w = makeGuessedWord(wordstr);
        }
        return w;
    }

    public Word makeGuessedWord(String str) { //pmartin 5Jan00
        Word lexWord = makeWord(str);
        return makeGuessedWord(lexWord);
    }

    public Word makeGuessedWord(Word lexWord) { //pmartin 5Jan00
    /* modified to add verbs 6Jan00 pmartin
        and split from string input version 10Jan00
        with remotemorph added 2aug00 pmartin */
        guessWordAttributes(lexWord);
        return lexWord;
    }

    public Word guessWordAttributes(Word lexWord) {
        Word wroot = null;
        if(morphAsVerbForm(lexWord)) {
            /* empty statement here--all done */
        } else if(morphPluralTest(lexWord)) {
            lexWord.markGuessedWord(nplCategory);
            String lexws = lexWord.wordstring;
            int wsLen = lexws.length();
            if(wsLen > 1) {
                wroot = getWord(lexws.substring(0, wsLen - 1));
            }
            if((wroot == null) && (wsLen > 4) && (lexws.endsWith("es"))) {
                wroot = getWord(lexws.substring(0, wsLen - 2));
            }
            if(wroot != null) {
                lexWord.addRoot(wroot);
            }
        } else {
            lexWord.markGuessedWord(nounCategory);
        }
        if(authorFlag) {
            if(debug || wordTrace) {
                logger.finest("Lexicon: added guessed word " +
                        lexWord.printEntryString());
            }
        }
        return lexWord;
    }

    public List makeModifierList(Word mod, Word prepWord, Word root) {//pm 21apr04
        return makeList(new Value[]{root, makeList(new Value[]{prepWord, mod})});
    }

    public List makeModifierList(Word mod, Word root) {//pm 9apr04
        return makeModifierList(mod, modifiedByWord, root);
    }

    public Word makeStructuredConcept(Word swd, Category cat, List cstruct) {
        /** make a word with category cat whose cstruct property is cstruct
         */
        // System.out.println("makeStructuredConcept of " + swd.printString() +
        //                 " " + cat.printString() + " " + cstruct.printString());
        swd.addWordCategory(cat, 0);
        swd.putdict(cstructAtom, cstruct);
        return swd;
    }

    public Word makeStructuredConcept(String str, Category cat, List cstruct) {
        Word wd = makeWord(str);
        return makeStructuredConcept(wd, cat, cstruct);
    }

    public Word makeStructuredConcept(Category cat, List cstruct) {
        return makeStructuredConcept(LexiconUtil.fringeString(cstruct), cat,
                cstruct);
    }

    public HashMap findEquivs() {  //pmartin 9apr02
        // find iko or iios that point to words with word senses
        Enumeration wdEnum = wordTable.elements();
        HashSet hasSenses = new HashSet(wordFreePos);
        Word wd;
        WordEntry we;
        while(wdEnum.hasMoreElements()) {
            wd = (Word) wdEnum.nextElement();
            we = wd.getWordEntry();
            if((we != null) && (we.subsenses != null)) {
                hasSenses.add(wd);
            }
        }
        System.out.println("findequivs sees " + hasSenses.size() +
                " words with subsenses from " + wordTable.size() +
                "known words");
        HashMap equivs = new HashMap(wordFreePos);
        Enumeration wdEn2 = wordTable.elements();
        Word[] parents;
        fe:
        while(wdEn2.hasMoreElements()) {
            wd = (Word) wdEn2.nextElement();
            we = wd.getWordEntry();
            if((we != null) && (we.sensenamep == null)) {
                parents = we.ikoParents;
                if(parents != null) {
                    for(int i = 0; i < parents.length; i++) {
                        if(hasSenses.contains(parents[i])) {
                            equivs.put(wd.wordstring, wd);
                            continue fe;
                        }
                    }
                }
                parents = we.iioParents;
                if(parents != null) {
                    for(int i = 0; i < parents.length; i++) {
                        if(hasSenses.contains(parents[i])) {
                            equivs.put(wd.wordstring, wd);
                            continue fe;
                        }
                    }
                }
            }
        }
        int nequivs = equivs.size();
        System.out.println("find Equivs got " + nequivs + " equivocations.\n");
        return equivs;
    }

    public int preserveWords() {
        /* marks all currently known words as never to be purged */
        Enumeration wordEnum = wordTable.elements();
        int wdCount = 0;
        Word wd;
        while(wordEnum.hasMoreElements()) {
            wd = (Word) wordEnum.nextElement();
            wd.dontPurge = true;
            wdCount++;
        }
        return wdCount;
    }

    public int preserveParents() {
        /* marks the parents of all words that are currently preserved */
        Enumeration wordEnum = wordTable.elements();
        int wdCount = 0;
        Word wd;
        Word[] wdParents;
        while(wordEnum.hasMoreElements()) {
            wd = (Word) wordEnum.nextElement();
            if(wd.dontPurge) {
                wdParents = wd.getAllParents();
                if(wdParents != null) {
                    for(int i = 0; i < wdParents.length; i++) {
                        if(!wdParents[i].dontPurge) {
                            wdParents[i].dontPurge = true;
                            wdCount++;
                        }
                    }
                }
            }
        }
        return wdCount;
    }

    public int fastPreserveParents() {
        /* marks the parents of all words that are currently preserved */
        Word wd;
        Enumeration wordEnum = wordTable.elements();
        HashSet workWords = new HashSet(wordFreePos + 1);
        while(wordEnum.hasMoreElements()) {
            wd = (Word) wordEnum.nextElement();
            if(wd.dontPurge) {
                workWords.add(wd);
            }
        }
        logger.finest("fast preserve found " + workWords.size() +
                " already preserved words");
        HashSet doneWords = new HashSet(wordFreePos);

        int wwCount;
        int markCount = 0;
        Word dpw, spw;
        Word[] todo = new Word[wordFreePos];
        Word[] someParents;
        WordEntry we;
        Iterator it;

        Value senseParentsVal;
        List senseParents;
        // loop until residual is null
        while(workWords.size() > 0) {
            // can't iterate on HashSet while adding elements, so collect 1st
            it = workWords.iterator();
            wwCount = 0;
            while(it.hasNext()) {
                todo[wwCount++] = (Word) it.next();
            }
            for(int i = 0; i < wwCount; i++) {
                dpw = todo[i];
                we = dpw.getWordEntry();
                if((we != null) && (!doneWords.contains(dpw))) {
                    // variants are parents
                    if((someParents = we.variantOf) != null) {
                        for(int ii = 0; ii < someParents.length; ii++) {
                            if(!doneWords.contains(someParents[ii])) {
                                workWords.add(someParents[ii]);
                            }
                        }
                    }
                    // roots are morphological parents
                    if((someParents = we.roots) != null) {
                        for(int ii = 0; ii < someParents.length; ii++) {
                            if(!doneWords.contains(someParents[ii])) {
                                workWords.add(someParents[ii]);
                            }
                        }
                    }
                    // ikos are kind-of parents
                    if((someParents = we.ikoParents) != null) {
                        for(int ii = 0; ii < someParents.length; ii++) {
                            if(!doneWords.contains(someParents[ii])) {
                                workWords.add(someParents[ii]);
                            }
                        }
                    }
                    // iios are instance-of parents
                    if((someParents = we.iioParents) != null) {
                        for(int ii = 0; ii < someParents.length; ii++) {
                            if(!doneWords.contains(someParents[ii])) {
                                workWords.add(someParents[ii]);
                            }
                        }
                    }
                    // senseOf are sense parents
                    if((senseParentsVal = we.getdict(senseofAtom)) != null) {
                        if(senseParentsVal.listp()) {
                            senseParents = (List) senseParentsVal;
                            for(int ii = 0; ii < senseParents.length(); ii++) {
                                spw = (Word) senseParents.elementAt(ii);
                                if(!doneWords.contains(spw)) {
                                    workWords.add(spw);
                                }
                            }
                        } else {
                            spw = (Word) senseParentsVal;
                            if(!doneWords.contains(spw)) {
                                workWords.add(spw);
                            }
                        }
                    }
                }
                doneWords.add(dpw);
                workWords.remove(dpw);
            }
        }
        // all words needing the mark are in hash doneWords
        it = doneWords.iterator();
        while(it.hasNext()) {
            dpw = (Word) it.next();
            if(!dpw.dontPurge) {
                dpw.dontPurge = true;
                markCount++;
            }
        }
        return markCount;
    }

    public int trimWords() { // mark word disk pointers for logical stubs
        int trimees = 0;
        Word wd;
        // wrong?? need to leave a gap to gropw atoms??
        //    int loww = highAtomIndex;
        int loww = IndexAtomLimit;
        int highw = highWordIndex;
        for(int i = loww; i < highw; i++) {
            wd = (Word) valueIndexTable[i];
            if(wd.hasWordEntry()) {
                if(!wd.hasParents()) {
                    wd.index = NODISKINDEX | (wd.index & WORDINDEXMASK);
                    wd.wordEntry = null;
                    trimees++;
                }
            }
        }
        return trimees;
    }

    public int purgeWords() {
        /* purges all words except those marked as never to be purged */
        Enumeration wordEnum = wordTable.elements();
        int wdCount = 0;
        Word wd;
        while(wordEnum.hasMoreElements()) {
            wd = (Word) wordEnum.nextElement();
            if(!wd.dontPurge) {
                wd.purge();
                wdCount++;
            }
        }
        return wdCount;
    }

    public void clearIndex() {
        valueIndexTable = null;
        diskIndexTable = null;
    }

    public void clearIndex(int nSize) {
        diskIndexTable = new int[nSize];
        valueIndexTable = new Value[nSize];
        // fill the table with stubs to make sure we have empty flags
        // we don't really use the zeroth entry, so make sure it's covered
        for(int i = 0; i < nSize; i++) {
            diskIndexTable[i] = -1;
            valueIndexTable[i] = null;
        }
    }

    public void incFixWordIndex() { // grow the word table by 10%
        fixWordIndex((int) (IndexWordLimit * 1.1));
    }

    public void fixWordIndex(int bigger) {  // grow the word table
        int[] ndi = new int[bigger];
        Value[] nvi = new Value[bigger];
        for(int i = 0; i < IndexWordLimit; i++) {
            ndi[i] = diskIndexTable[i];
            nvi[i] = valueIndexTable[i];
        }
        for(int i = IndexWordLimit; i < bigger; i++) {
            ndi[i] = -1;
            nvi[i] = null;
        }
        diskIndexTable = ndi;
        valueIndexTable = nvi;
        IndexWordLimit = bigger;
    }

    public synchronized int assignAdditionalIndexNumbers() {
        // to be run after numbers are assigned and some old compressed
        // structs exist.  Can't re-number from scratch, but may have new
        // categories and atoms to encode....
        int newidx = 0;
        int ncats = categoryTable.size();
        if(ncats > highCatIndex) {
            // we've got at least one new one to assign an index number to
            Enumeration catEnum = categoryTable.elements();
            Category ncat;
            while(catEnum.hasMoreElements()) {
                ncat = (Category) catEnum.nextElement();
                if(ncat.index == 0) {
                    ncat.index = highCatIndex;
                    valueIndexTable[highCatIndex] = ncat;
                    highCatIndex++;
                    newidx++;
                    if(highCatIndex >= IndexCategoryLimit) {
                        System.err.println("assignAdditionalIndexNumbers " +
                                "has too many (" +
                                highCatIndex + ") categories");
                        return -1;
                    }
                }
            }

        }
        int natoms = atomTable.size();
        boolean needToWarn = true;
        if(natoms > (highAtomIndex - IndexCategoryLimit)) {
            // we've got at least one new one to assign an index number to
            Enumeration atomEnum = atomTable.elements();
            Atom natom;
            while(atomEnum.hasMoreElements()) {
                natom = (Atom) atomEnum.nextElement();
                if((natom.index & WORDINDEXMASK) == 0) {
                    natom.index = natom.index | highAtomIndex; // keep any high
                    // marked bits
                    valueIndexTable[highAtomIndex] = natom;
                    highAtomIndex++;
                    newidx++;
                    if(needToWarn && highAtomIndex >= IndexAtomNormal) {
                        logger.finest("too many atoms being made -- at " +
                                highAtomIndex + " last new ones were ");
                        for(int ia = highAtomIndex - 5; ia < highAtomIndex; ia++) {
                            logger.finest(valueIndexTable[ia] + ", ");
                        }
                        needToWarn = false;

                    }


                    if(highAtomIndex >= IndexAtomLimit) {
                        System.err.println("assignAdditionalIndexNumbers " +
                                "has too many (" +
                                highAtomIndex + ") atoms");
                        return -1;
                    }
                }
            }
        }
        return newidx;
    }

    public void assignIndexNumbers() {
        int idx = 1; // we skip zero to mean not loaded
        int cats = categoryTable.size();
        int atoms = atomTable.size();
        int words = wordTable.size();
        if(cats >= (IndexCategoryLimit - 1)) {
            System.err.println("too many Categories to assignIndexNumbers");
            return;
        } else if(atoms >= (IndexAtomLimit - IndexCategoryLimit - 1)) {
            System.err.println("too many atoms to assignIndexNumbers");
            return;
        } else if(words >= (IndexWordLimit - IndexAtomLimit - 1)) {
            fixWordIndex(words + IndexAtomLimit + SpareWordsSize);
        }


        clearIndex(IndexWordLimit);

        logger.finest("assign index nums found " + cats + " cats; " +
                atoms + " atoms; " + words + " words.");

        Iterator it;
        Map.Entry mpent;
        TreeMap catMap = new TreeMap(categoryTable);
        it = catMap.entrySet().iterator();
        while(it.hasNext()) {
            mpent = (Map.Entry) it.next();
            Category cat = (Category) mpent.getValue();
            valueIndexTable[idx] = cat;
            cat.index = idx++;
        }

        highCatIndex = idx;
        idx = IndexCategoryLimit;  // need because we may be growing cats
        TreeMap atomMap = new TreeMap(atomTable);
        it = atomMap.entrySet().iterator();
        while(it.hasNext()) {
            mpent = (Map.Entry) it.next();
            Atom atm = (Atom) mpent.getValue();
            valueIndexTable[idx] = atm;
            atm.index = idx++;
        }
        highAtomIndex = idx;
        idx = IndexAtomLimit; // need because we may be growing atoms
        TreeMap wordMap = new TreeMap(wordTable);
        it = wordMap.entrySet().iterator();
        while(it.hasNext()) {
            mpent = (Map.Entry) it.next();
            Word wrd = (Word) mpent.getValue();

            valueIndexTable[idx] = wrd;
            int idxbits = wrd.index & MARKEDINDEX;
            wrd.index = idxbits | idx++;
        }
        highWordIndex = idx;
    }

    public int getHighWordIndex() {
        return highWordIndex;
    }

    public void restoreIndexNumbers() {
        // resets all the index numbers for the words previously indexed
        // only needed because some moves set coded vals in index for a word
        // wrong??  may be gap unassigned for new atoms??
        // for (int i=highAtomIndex; i < highWordIndex; i++){
        for(int i = IndexAtomLimit; i < highWordIndex; i++) {
            Word w = (Word) valueIndexTable[i];
            w.index = i;
        }
    }

    public int shrinkLexSize() { // how many words could be discarded??
        // note that if the wordTable is currently shrinking, this number can
        // be artificially negative....
        return wordTable.size() - highWordIndex + highAtomIndex;
    }

    public synchronized int forgetMorphWords(int tooHiWord) {
        // drops words from wordTable that are not indexed (meaning morphed)
        // or above the index number provided
        int maxWord = highWordIndex;
        if(tooHiWord > 0) {
            maxWord = tooHiWord;
        }
        Enumeration wordEnum = wordTable.elements();
        int wdCount = 0;
        Word wd;
        int wdidx;
        while(wordEnum.hasMoreElements()) {
            wd = (Word) wordEnum.nextElement();
            wdidx = wd.index & WORDINDEXMASK;
            if((wdidx > maxWord) || (wdidx == 0)) {
                wordTable.remove(wd.wordstring);
                wdCount++;
            }
        }
        return wdCount;
    }

    public void saveMorphWords(int tooHiWord, String fname) {
        // writes jlf for words from wordTable that are not indexed
        //(meaning morphed) or above the index number provided
        PrintWriter fout;
        int maxWord = highWordIndex;
      if (tooHiWord > 0) maxWord = tooHiWord;
      if (authorFlag)
        if (debug)
            logger.finest("saving words after " + maxWord);
      Enumeration wordEnum = wordTable.elements();
        Word wd;
        int wdidx;
        try {
            fout = new PrintWriter(new BufferedWriter(new FileWriter(fname)),
                    true);
            while(wordEnum.hasMoreElements()) {
                wd = (Word) wordEnum.nextElement();
                wdidx = wd.index & WORDINDEXMASK;
                if((wdidx > maxWord) || (wdidx == 0)) {
                    fout.println(wd.printEntryString());
                }
            }
            return;
        } catch(IOException e) {
            return;
        }
    }

    public int shrinkLex() {
        if(currentlyShrinkingLex) {
            logger.info("shrinklex already in progress so skipping");
            return 0;
        }
        int wts = shrinkLexSize();
        if(wts <= ShrinkLexLimit) {
            logger.info("shrinkLex was skipped; only " + wts +
                    " shrinkable words");
            return 0;
        }
        currentlyShrinkingLex = true;
        int wds = innerShrinkLex();
        currentlyShrinkingLex = false;
        return wds;
    }

    public synchronized int innerShrinkLex() {
        int wts = shrinkLexSize();
        logger.info("shrinking lex with " + wts + " spare words");
        int news = assignAdditionalIndexNumbers();
        if(news > 0) {
            logger.info("shrinklex found " + news + " new cats/atoms");
        }
        int icrush = crushLex();
        logger.info("crushLex repacked " + icrush + " words");
        int imorph = forgetMorphWords(highWordIndex);
        logger.info("shrinklex forgot " + imorph + " morphed words");
        return icrush + imorph;
    }

    public int dumpBinaryLexicon(String blName) {
        int icount = dumpLex(blName);
        String dblHistory = ":unknown previous history\n" +
                ":dumped " + icount + " objects";
        icount = dumpLexIndex(blName);
        if(icount > 0) {
            dblHistory += " with index\n";
        } else {
            dblHistory += " but index failed\n";
        }
        dumpHistory(dblHistory, blName);
        return icount;
    }

    public int dumpLex(String binLexOutStr) {
        FileOutputStream binLexFileStream;
        DataOutputStream binLexStream;
        String binLexFileName = binLexOutStr + ".blex";
        try {
            binLexFileStream = new FileOutputStream(binLexFileName);
        } catch(java.io.FileNotFoundException fnfe) {
            System.err.println("Unable to open bin Lex out file " +
                    binLexFileName);
            return -1;
        }
        binLexStream = new DataOutputStream(binLexFileStream);

        assignIndexNumbers();

        int objBytes;
        int diskOffset = 0;
        Value val;
        int objCount = 0;
        BitBuffer obb = new BitBuffer();
        for(int objIndex = 1; objIndex < highWordIndex; objIndex++) {
            obb.clear();
            val = valueIndexTable[objIndex];
            diskIndexTable[objIndex] = -1;
            if(val != null) {
                objCount++;
                diskIndexTable[objIndex] = diskOffset;
                if(val instanceof Category) {
                    ((Category) val).encode(obb);
                // System.out.println("encoded " +
                //    ((Category)val).printString()
                //           + " in " + obb.getWBytes());
                } else if(val instanceof Atom) {
                    ((Atom) val).encode(obb);
                } else if(val instanceof Word) {
                    ((Word) val).encode(obb);
                } else {
                    System.err.println("dumpLex found bad obj number " +
                            objIndex);
                    return objCount;
                }
                objBytes = obb.getWBytes();
                diskOffset += objBytes;
                try {
                    obb.write(binLexStream);
                } catch(java.io.IOException ioe) {
                    System.err.println("Fatal error writing Lex out file " +
                            binLexOutStr);
                    return -1;
                }
            }
        }
        try {
            binLexStream.close();
            binLexFileStream.close();
        } catch(java.io.IOException ioe) {
        }

        return objCount;
    }

    public int loadLex(String binLexInStr) {
        return loadLex(binLexInStr, highWordIndex, UNPACKALL);
    }

    public int loadPackedLex(String binLexInStr) {
        return loadLex(binLexInStr, highWordIndex, KEEPPACKED);
    }

    public int loadCoreLex(String binLexInStr) {
        return loadLex(binLexInStr, highWordIndex, UNPACKCORE);
    }

    public int loadLex(String binLexInStr, int highLoadIndex, int unpackAct) {
        FileInputStream binLexFileStream;
        DataInputStream binLexStream;
        String binLexFileName = binLexInStr + ".blex";
        try {
            binLexFileStream = new FileInputStream(binLexFileName);
        } catch(java.io.FileNotFoundException fnfe) {
            System.err.println("Unable to open bin Lex input file " +
                    binLexFileName);
            return -1;
        }
        binLexStream = new DataInputStream(binLexFileStream);

        int highLoad = Math.min(highLoadIndex, highWordIndex);
        Value val;
        int objIndex = 0;
        int objCount = 0;
        BitBuffer ibb; // sometimes we keep these so use a new one each time
        for(objIndex = 1; objIndex < highLoad; objIndex++) {
            if(diskIndexTable[objIndex] > -1) { // skip the empties
                try {
                    ibb = new BitBuffer(binLexStream);
                } catch(java.io.IOException ioe) {
                    System.err.println("Fatal error reading Lex file " +
                            binLexFileName);
                    return -1;
                }
                val = restoreBBObj(ibb, objIndex, unpackAct);
                if(val != null) {
                    objCount++;
                }
            }
        }
        try {
            binLexStream.close();
            binLexFileStream.close();
        } catch(java.io.IOException ioe) {
        }

        return objCount;
    }

    public synchronized int crushLex() {
        // not any more
        // restoreIndexNumbers();
        Word cw;
        int crushCount = 0;
        int oldHiWord = highWordIndex;
        logger.finest("crushing words from " + IndexAtomLimit +
                " to " + highWordIndex);
        for(int i = IndexAtomLimit; i < highWordIndex; i++) {
            cw = (Word) valueIndexTable[i];
            if(!cw.dontPurge) {
                if(cw.crush()) {
                    crushCount++;
                }
            }
        }
        if(oldHiWord != highWordIndex) {
            logger.finest("crushlex moved hiWord from " + oldHiWord +
                    " to " + highWordIndex);
        }
        return crushCount;
    }

    public int dumpLexIndex(String binLexOutStr) {
        /* only makes sense to run after running dumpLex to fill in diskOffsets
         */
        FileOutputStream binIdxFileStream;
        DataOutputStream binIdxStream;
        String binIdxFileName = binLexOutStr + ".bidx";
        try {
            binIdxFileStream = new FileOutputStream(binIdxFileName);
        } catch(java.io.FileNotFoundException fnfe) {
            System.err.println("Unable to open bin LexIndex out file " +
                    binIdxFileName);
            return -1;
        }
        binIdxStream = new DataOutputStream(binIdxFileStream);

        int diskOffset;
        Value val;
        String valString;
        int objIndex = 0;
        int objCount = 0;
        int bitsInBuf = 0;
        BitBuffer ibb = new BitBuffer(BlockBytes);
        int bitLimit = BlockBits - 32 - 320; // 32 for size, last is a fudge
        bitsInBuf = ibb.gammaEncode(highCatIndex + 1);
        bitsInBuf += ibb.gammaEncode(highAtomIndex + 1);
        bitsInBuf += ibb.gammaEncode(highWordIndex + 1);
        for(objIndex = 1; objIndex < highWordIndex; objIndex++) {
            val = valueIndexTable[objIndex];
            diskOffset = diskIndexTable[objIndex];
            bitsInBuf++;
            if(val == null) {
                ibb.push(true); // empty slot flag
            } else {
                objCount++;
                ibb.push(false);
                if(val instanceof Category) {
                    valString = ((Category) val).wordstring;
                } else if(val instanceof Atom) {
                    valString = ((Atom) val).wordstring;
                } else if(val instanceof Word) {
                    valString = ((Word) val).wordstring;
                } else {
                    System.err.println("dumpLexIndex bad obj number " + objIndex);
                    return objCount;
                }
                bitsInBuf += ibb.gammaEncode(diskOffset + 1);
                bitsInBuf += ibb.encodeUTF(valString);
                if(bitsInBuf > bitLimit) {
                    try {
                        ibb.write(binIdxStream);
                    } catch(java.io.IOException ioe) {
                        System.err.println("Fatal error writing binIdx file " +
                                binIdxFileName);
                        return -1;
                    }
                    ibb.clear();
                    bitsInBuf = 0;
                }
            }
        }
        if(bitsInBuf > 0) {
            try {
                ibb.write(binIdxStream);
            } catch(java.io.IOException ioe) {
                System.err.println("Fatal error writing binIdx file " +
                        binIdxFileName);
                return -1;
            }
        }
        try {
            binIdxStream.close();
            binIdxFileStream.close();
        } catch(java.io.IOException ioe) {
        }

        return objCount;
    }

    public int loadLexIndex(String binLexOutStr) {
        FileInputStream binIdxFileStream;
        DataInputStream binIdxStream;
        String binIdxFileName = binLexOutStr + ".bidx";
        try {
            binIdxFileStream = new FileInputStream(binIdxFileName);
        } catch(java.io.FileNotFoundException fnfe) {
            System.err.println("Unable to open bin LexIndex input file " +
                    binIdxFileName);
            return -1;
        }
        logger.finest("opened bin index file " + binIdxFileName +
                " and cleared memory index");
        binIdxStream = new DataInputStream(binIdxFileStream);

        int diskOffset;
        Value val;
        boolean emptySlot;
        String valString;
        int objIndex = 0;
        int objCount = 0;
        BitBuffer ibb = new BitBuffer(BlockBits);
        try {
            ibb.read(binIdxStream);
        } catch(java.io.IOException ioe) {
            System.err.println("Fatal error reading LexIndex in file " +
                    binIdxFileName);
            return -1;
        }

        int curBlockLen = ibb.length();
        highCatIndex = ibb.gammaDecode() - 1;
        highAtomIndex = ibb.gammaDecode() - 1;
        highWordIndex = ibb.gammaDecode() - 1;
        logger.finest("loadLexIndex set hiCat=" + highCatIndex +
                " hiAtm=" + highAtomIndex + " hiWd=" + highWordIndex);

        if(IndexWordLimit < highWordIndex) {
            IndexWordLimit = highWordIndex + SpareWordsSize;
            logger.finest("raising IndexWordLimit to " +
                    IndexWordLimit);
        }
        clearIndex(IndexWordLimit);

        for(objIndex = 1; objIndex < highWordIndex; objIndex++) {
            if(ibb.tell() == curBlockLen) {
                try {
                    ibb.read(binIdxStream);
                } catch(java.io.IOException ioe) {
                    System.err.println("Fatal error reading LexIndex in file " +
                            binIdxFileName);
                    return -1;
                }
                curBlockLen = ibb.length();
            }
            emptySlot = ibb.pop();
            if(emptySlot) {
                valueIndexTable[objIndex] = null;
                diskIndexTable[objIndex] = -1;
            } else {
                objCount++;
                diskOffset = ibb.gammaDecode() - 1;
                valString = ibb.decodeUTF();
                if(objIndex < highCatIndex) {
                    val = makeCategory(valString);
                    ((Category) val).index = objIndex;
                } else if(objIndex < highAtomIndex) {
                    val = makeAtom(valString);
                    ((Atom) val).index = objIndex;
                } else if(objIndex < highWordIndex) {
                    val = makeWord(valString);
                    ((Word) val).index = objIndex;
                } else {
                    System.err.println("reload index " + objIndex +
                            " more than high word");
                    return -1;
                }
                valueIndexTable[objIndex] = val;
                diskIndexTable[objIndex] = diskOffset;
            }
        }
        try {
            binIdxStream.close();
            binIdxFileStream.close();
        } catch(java.io.IOException ioe) {
        }

        return objCount;
    }

    public RandomAccessFile openBinLexRAF(String rafName) {
        RandomAccessFile blrf = null;
        String rafString = rafName + ".blex";
        try {
            blrf = new RandomAccessFile(rafString, "r");
            binLexRAF = blrf;
        } catch(java.io.IOException ioe) {
            System.err.println("can't open binlex raf " + rafString);
        }
        return binLexRAF;
    }

    public void closeBinLexRAF() {
        if(binLexRAF != null) {
            try {
                binLexRAF.close();
                binLexRAF = null;
            } catch(java.io.IOException ioe) {
            }
        }
    }

    public Value reloadBinObj(int idx) {
        return reloadBinObj(idx, binLexRAF);
    }

    public Value reloadBinObj(int idx, RandomAccessFile binLexRaf) {
        int diskIdx = diskIndexTable[idx];
        if((diskIdx & NODISKINDEX) != 0) {
            System.err.println("No disk record for index " + idx);
            return null;
        } else if((diskIdx & DISKOUTDATED) != 0) {
            System.err.println("Can't reload outdated disk object");
            return null;
        } else if((diskIdx & NOWORDENTRY) != 0) {
            System.err.println("No WordEntry on disk for object");
            return null;
        }
        BitBuffer tempBB = new BitBuffer();

        try {
            if(diskIdx > binLexRaf.length()) {
                System.err.println("Can't reload with bin lex index " +
                        diskIdx + " exceeding file length");
                return null;
            } else {
                binLexRaf.seek(diskIdx);
                tempBB.read(binLexRaf);
            }
        } catch(java.io.IOException ioe) {
            System.err.println("I/O error seeking/reading bin lex raf");
            return null;
        }
        return restoreBBObj(tempBB, idx, UNPACKALL);
    }

    public Value restoreBBObj(BitBuffer tempBB, int idx, int unpackAct) {
        Value val = null;
        if(idx < highCatIndex) {
            val = decodeCategory(tempBB, idx);
        } else if(idx < highAtomIndex) {
            val = decodeAtom(tempBB, idx);
        } else if(idx < highWordIndex) {
            val = decodeWord(tempBB, idx, unpackAct);
        } else {
            System.err.println("reload requested too high index " + idx);
            return null;
        }
        if(val != null) {
            valueIndexTable[idx] = val;
        }
        return val;
    }

    void addMorphEntry(String morphAnswer) {
        /* if string is non-null and matches jlf-entries pattern, add words to lex */
        boolean traceMorph = true;
        if((morphAnswer != null) && (morphAnswer.startsWith("(JLF-ENTRIES "))) {
            String newEnt = null;
            String trimmedEnt;
            String ctlPlusQuotes = "\t\n\f\r\"";
            StringTokenizer lexStkn =
                    new StringTokenizer(morphAnswer.substring(13), ctlPlusQuotes);
            String junk = null;
            if(lexStkn.hasMoreTokens()) {
                junk = lexStkn.nextToken();
            //log.debug(logTag, 10,"discarding " + junk); //word string
            }
            if(lexStkn.hasMoreTokens()) {
                junk = lexStkn.nextToken();
            //log.debug(logTag, 10,"discarding " + junk); //end string
            }
            while(lexStkn.hasMoreTokens()) {
                newEnt = lexStkn.nextToken();
                //if (traceMorph)
                trimmedEnt = newEnt.toLowerCase().trim();
                //if (traceMorph)
                Word newWord = makeEntry(trimmedEnt);
                newWord.addMorphServerWordMark();
                if(lexStkn.hasMoreTokens()) {
                    junk = lexStkn.nextToken(); //start next entry string
                }
            }
        }
    }

    public boolean morphPluralTest(Word wd) { //pmartin 5Jan00
        /* primitive test for use on unknown words */
        return wd.getWordString().endsWith("s");
    }

    public boolean morphAsVerbForm(Word wd) { //pmartin 6Jan00
        /* primitive test and action for use on unknown words */
        String str = wd.getWordString();
        int len = str.length();
        Word vroot = null;
        if((len > 3) && (str.endsWith("s")) &&
                ((vroot = getWord(str.substring(0, len - 1))) != null) &&
                (vroot.isFormOfCat(verbCategory))) {
            wd.markDerivedVerb(vroot, threeSingAtom, null);
            if(vroot.isFormOfCat(nounCategory)) {
                wd.addWordCategory(nounCategory, 0);
            }
            return true;
        } else if((len > 5) && (str.endsWith("ing")) &&
                ((vroot = getWord(str.substring(0, len - 3))) != null) &&
                (vroot.isFormOfCat(verbCategory))) {
            wd.markDerivedVerb(vroot, prespartAtom, presentAtom);
            return true;
        } else if((len > 4) && (str.endsWith("ed")) &&
                (((vroot = getWord(str.substring(0, len - 2))) != null) ||
                ((vroot = getWord(str.substring(0, len - 1))) != null)) &&
                (vroot.isFormOfCat(verbCategory))) {
            wd.markDerivedVerb(vroot, pastpartAtom, pastAtom);
            return true;
        } else {
            return false;
        }
    }

    public boolean isFormOfCat(String wordstr, String cat) {
        Word thisWord = this.getWord(wordstr);
        boolean ifc = false;
        Category thisCat = this.makeCategory(cat);
        if(thisWord != null) {
            ifc = thisWord.isFormOfCat(thisCat);
        }
        if(authorFlag && wordTrace) {
            String tws = "<not in lex>";
            if(thisWord != null) {
                tws = thisWord.wordstring;
            }
            logger.finest("Lexicon:  isFormOfCat \"" + wordstr + "\" = " +
                    tws + ", cat = " + thisCat + " is " + ifc);
        }
        return ifc;
    }

    public int size() {
        return wordFreePos;
    }

    public int atomSize() { // pmartin 27jan00 for debugging lexicons
        return atomFreePos;
    }

    public boolean isKnownWord(String str) { //pmartin 13sep99
        String ws = str.toLowerCase();
        boolean inTab = (wordTable.get(ws) != null);
        if(authorFlag && wordTrace) {
            logger.finest("Lexicon: isKnownWord of string " + ws +
                    " is " + inTab);
        }
        return inTab;

    }

    public boolean isKnownWord(WordToken t) {
        return t.word == null;
    }

    public boolean isKnownWord(Word w) { // is it in THIS word table?
        String ws = w.wordstring;
        boolean inTab = (wordTable.get(ws) != null);
        if(authorFlag && wordTrace) {
            logger.finest("Lexicon: isKnownWord of word " + ws +
                    " is " + inTab);
        }
        return inTab;

    }

    public Word[] mapWordAttrs(String attrStr, String valStr) {  //pmartin 2aug00
        return mapWordAttrs(makeAtom(attrStr), makeAtom(valStr));
    }

    public Word[] mapWordAttrs(Atom attr, Atom val) {   //pmartin 2aug00
        HashSet wins = new HashSet(1000);
        Enumeration ewt = wordTable.elements();
        Word wd;
        while(ewt.hasMoreElements()) {
            wd = (Word) ewt.nextElement();
            if(wd.getdict(attr) == val) {
                wins.add(wd);
            }
        }
        if(wins.size() > 0) {
            Word[] wordArray = new Word[wins.size()];
            wordArray = (Word[]) (wins.toArray(wordArray));
            return wordArray;
        } else {
            return null;
        }
    }

    public Word[] mapWordAttrp(String strattr) {   //pmartin 21dec00
        return mapWordAttrp(makeAtom(strattr));
    }

    public Word[] mapWordAttrp(Atom attr) {   //pmartin 21dec00
        HashSet wins = new HashSet(1000);
        Enumeration ewt = wordTable.elements();
        Word wd;
        while(ewt.hasMoreElements()) {
            wd = (Word) ewt.nextElement();
            if(wd.getdict(attr) != null) {
                wins.add(wd);
            }
        }
        if(wins.size() > 0) {
            Word[] wordArray = new Word[wins.size()];
            wordArray = (Word[]) (wins.toArray(wordArray));
            return wordArray;
        } else {
            return null;
        }
    }

    public Word[] mapWordAuthor(String sval) {   //pmartin 2aug00
        return mapWordAuthor(makeAtom(sval));
    }

    public Word[] mapWordAuthor(Atom val) {   //pmartin 2aug00
        HashSet wins = new HashSet(1000);
        Enumeration ewt = wordTable.elements();
        Word wd;
        while(ewt.hasMoreElements()) {
            wd = (Word) ewt.nextElement();
            if(wd.authorp(val)) {
                wins.add(wd);
            }
        }
        if(wins.size() > 0) {
            Word[] wordArray = new Word[wins.size()];
            wordArray = (Word[]) (wins.toArray(wordArray));
            return wordArray;
        } else {
            return null;
        }
    }

    public void printAtoms() {
        int natoms = atomTable.size();
        if(natoms > 0) {
            System.out.println("currently " + natoms + " atoms");
            Enumeration atomEnum = atomTable.elements();
            Atom natom;
            while(atomEnum.hasMoreElements()) {
                natom = (Atom) atomEnum.nextElement();
                System.out.println(natom.printEntryString());
            }
        }
    }

    public Word packLex(String s) {
        return this.makeWord(s);
    }

    public Word packLex(String s1, String s2) {
        return packLex(s1.concat(s2));
    }

    public Word packLex(String s1, String s2, String s3) {
        return packLex(s1.concat(s2).concat(s3));
    }

    public Word packLex(String s1, String s2, String s3, String s4) {
        return packLex(s1.concat(s2).concat(s3).concat(s4));
    }

    public Word makeEntry(String entryString) {

        boolean entryDebug = false;
        if(authorFlag) {
            if(entryDebug) {
                logger.finest("Starting makeEntry (" + entryString + ")");
            }
        }

    StringTokenizer fieldEnumerator, nameEnumerator;
        QuoteStringTokenizer attrEnumerator;
        fieldEnumerator = new StringTokenizer(entryString, ";");
        String fieldString, attrString, valueString, token;
        Vector values = null;
        boolean categoryFlag = false;
        boolean numFlag = false;
        boolean wordFlag = false;
        boolean multiWordFlag = false;
        boolean skipAttr = false; // true to read but ignore an attribute
        boolean valueFlag = false;
        boolean singleFlag = false; // pmartin 13dec00  for author and nameconfidence
        boolean structFlag = false; // 20oct00 pmartin for c-structure
        // causes a struct to have atoms for tags and words for vals
        boolean firstFlag = true;
        Word word = null;
        WordEntry worde = null;
        Atom attrAtom = null;
        Vector rootcats = new Vector(defaultCatListSize);
        while(fieldEnumerator.hasMoreTokens()) {
            fieldString = fieldEnumerator.nextToken();
            if(authorFlag) {
                if(entryDebug) {
                    logger.finest("field token=>" +
                            fieldString + "<=");
                }
            }
            attrEnumerator = new QuoteStringTokenizer(fieldString, "|", ":");
            if(!attrEnumerator.hasMoreTokens() && // no wordstring in entry
                    !fieldString.equals(":")) { // and not entry for the empty word
                logger.finest("**** warning: there's nothing in field: " +
                        fieldString + " in lexical entry: " +
                        entryString + " -- ignored");
            }
            if(firstFlag) {
                if(entryString.startsWith(";:")) {
                    attrString = ";";
                } else if(entryString.startsWith(";")) {
                    attrString = ";" + attrEnumerator.nextToken();
                } else if(entryString.startsWith("::")) {
                    attrString = ":";
                } else if(entryString.startsWith(":;")) {
                    attrString = "";
                } else if(entryString.startsWith(":")) {
                    attrString = ":" + attrEnumerator.nextToken();
                } else {
                    attrString = attrEnumerator.nextToken();
                }
            } else if(fieldString.startsWith(":")) {
                logger.finest("**** warning: there's no attribute in field: " +
                        fieldString + " in lexical entry for " +
                        word.wordstring + " -- rest ignored");
                return word;
            } else {
                attrString = attrEnumerator.nextToken();
            }

            if(authorFlag) {
                if(entryDebug) {
                    values = new Vector(defaultCatListSize);  //maybe reuse for speed?
                }
            }
            categoryFlag = false;
            numFlag = false;
            wordFlag = false;
            skipAttr = false;
            structFlag = false; // pmartin 20 oct00
            valueFlag = false;
            singleFlag = false; //pmartin 13dec00
            if(!firstFlag) { //clean up the attribute name, if necessary
                nameEnumerator = new StringTokenizer(attrString, " \t\n\r");
                if(!nameEnumerator.hasMoreTokens()) {
                    if(fieldEnumerator.hasMoreTokens()) {
                        logger.finest("**** warning: no attribute name in field: " +
                                fieldString + " in lexical entry for " +
                                word.wordstring + " -- rest of entry ignored");
                    } else {
                        break; //this was empty space beyond the last ; or empty entry
                    }
                } else {
                    attrString = nameEnumerator.nextToken();
                }
                if(nameEnumerator.hasMoreTokens()) {
                    logger.finest("**** warning: multiple attribute name in: " +
                            fieldString + " in lexical entry for " +
                            word.wordstring + " -- used first one only");
                }
            }
            if(firstFlag) {
                word = innerMakeWord(attrString);
                worde = word.makeWordEntry(); // maybe innerMakeWord made one
                if((attrString.length() > 1) && (attrString.endsWith(" "))) {
                    logger.finest("**** warning: word ends with space in: " +
                            fieldString + " in lexical entry for " +
                            word.wordstring + " -- probably an error");
                } else if((attrString.length() > 1) &&
                        (attrString.endsWith("\t"))) {
                    logger.finest("**** warning: word ends with tab in: " +
                            fieldString + " in lexical entry for " +
                            word.wordstring + " -- probably an error");
                } else if((attrString.length() > 1) &&
                        (attrString.endsWith("\r"))) {
                    logger.finest("**** warning: word ends with return in: " +
                            fieldString + " in lexical entry for " +
                            word.wordstring + " -- probably an error");
                } else if((attrString.length() > 1) &&
                        (attrString.endsWith("\n"))) {
                    logger.finest("**** warning: word ends with newline in: " +
                            fieldString + " in lexical entry for " +
                            word.wordstring + " -- probably an error");
                }
                if(word != null) //clear any previous information
                {
                    word.clearWord(false);
                } else {
                    return null;
                }
            } else if(categoryFlagStrings.contains(attrString)) {
                categoryFlag = true;
            } else if(wordFlagStrings.contains(attrString)) {
                wordFlag = true;
            } else if(attrString.equals("nameconfidence")) {  // pmartin 13dec00
                wordFlag = true;
                valueFlag = true;
                singleFlag = true;
            } else if(attrString.equals("author")) {  // pmartin 13dec00
                valueFlag = true;
                singleFlag = true;
                skipAttr = productionFlag;  // skip this one if doing production
            } else if(multiWordPropsStrings.contains(attrString)) {
                wordFlag = true;
                valueFlag = true;
                multiWordFlag = true;
            } else if(attrString.equals("numval")) {
                numFlag = true;
                worde.numeralp = true;    // moved here 19apr00 pmartin
            } else if(attrString.equals("value")) { //added pmartin 18Apr00
                numFlag = true;
            } else if(attrString.equals("icodes")) {
                valueFlag = false; // don't let general attribute code do it
            } else if(attrString.equals("capcodes")) {
                valueFlag = true; // let general attribute code do it
            } else if(isWordProp(attrString)) {
                valueFlag = true; // let general attribute code do it
                wordFlag = true; // but make lists of words instead of atoms
                if(productionFlag &&
                        (attrString.equals("domain") ||
                        attrString.equals("in-state"))) {
                    skipAttr = true;
                }
            } else if(attrString.equals("c-structure")) { // pmartin 20oct00
                valueFlag = true; // let general attribute code do it
                structFlag = true; // read a-list like structures with atom tags
                wordFlag = true; // but make a-list vals into words instead of
            // atoms
            } else {
                valueFlag = true; // the values will be atoms and lists
            }
            if(attrEnumerator.hasMoreTokens()) {
                valueString = attrEnumerator.nextToken();
            } else {
                valueString = "";
            }
            if(attrEnumerator.hasMoreTokens()) {
                logger.finest("**** warning: extra colon in field: " +
                        fieldString + " in lexical entry for " +
                        word.wordstring + " -- rest ignored");
            }
            if(valueFlag) {
                int vtype = ATOMVALS;
                if(wordFlag) {
                    vtype = WORDVALS;
                }
                if(structFlag) {
                    vtype = STRUCTVALS;
                }
                if(multiWordFlag) {
                    vtype = MULTIWORDVALS;
                }
                Value[] varray =
                        Lexicon.this.valuesFromString(valueString, vtype);
                valueString = "";
                if(multiWordFlag) {
                    Word[] words = new Word[varray.length];
                    for(int iw = 0; iw < varray.length; iw++) {
                        words[iw] = (Word) varray[iw];
                    }
                    if(attrString.equals("iko") || attrString.equals("kindof")) {
                        if(word.sensenamep()) {
                            word.setIkoParents(words);
                        } else {
                            word.addIkoParents(words);
                        }
                    } else if(attrString.equals("iio") ||
                            attrString.equals("instanceof")) {
                        word.addIioParents(words);
                    } else if(attrString.equals("variantof") ||
                            attrString.equals("variant-of")) {
                        for(int ii = 0; ii < words.length; ii++) {
                            if(word.loopingProperty(words[ii])) {
                                logger.finest("**** warning: " +
                                        "dropping variant-of " + words[ii] +
                                        " to avoid looping in " +
                                        word.wordstring);
                                word.addCutVarOf(words[ii]);
                            } else {
                                word.addVariantOf(words[ii]);
                                words[ii].addVariants(word);
                            }
                        }
                    } else if(attrString.equals("nickname-of") ||
                            attrString.equals("nicknameof")) {
                        for(int ii = 0; ii < words.length; ii++) {
                            if(word.loopingProperty(words[ii])) {
                                logger.finest("**** warning: " +
                                        "dropping nickname-of " + words[ii] +
                                        " to avoid looping in " +
                                        word.wordstring);
                                word.addCutVarOf(words[ii]);
                            } else {
                                word.addNicknameOf(words[ii]);
                                words[ii].addNicknames(word);
                            }
                        }
                    } else if(attrString.equals("abbrev")) {
                        for(int ii = 0; ii < words.length; ii++) {
                            if(word.loopingProperty(words[ii])) {
                                logger.finest("**** warning: " +
                                        "dropping abbrev(of) " + words[ii] +
                                        " to avoid looping in " +
                                        word.wordstring);
                                word.addCutVarOf(words[ii]);
                            } else {
                                word.addAbbrevOf(words[ii]);
                                words[ii].addAbbrevs(word);
                            }
                        }
                    } else if(attrString.equals("entails")) {
                        for(int a = 0; a < words.length; a++) {
                            word.addEntails(words[a]);
                        }
                    } else if(attrString.equals("takes-obj") ||
                            attrString.equals("takesobj")) {
                        worde.putdict(takesObjAtom, makeList(varray));
                    //        for (int a=0; a < words.length; a++)
                    //    word.addEntails(words[a]);
                    } else if(attrString.equals("can-be-misspelling-of") ||
                            attrString.equals("misspellingof") ||
                            attrString.equals("misspelling-of")) {
                        for(int ii = 0; ii < words.length; ii++) {
                            if(word.loopingProperty(words[ii])) {
                                logger.finest("**** warning: " +
                                        "dropping misppelling-of " +
                                        words[ii] +
                                        " to avoid looping in " +
                                        word.wordstring);
                                word.addCutVarOf(words[ii]);
                            } else {
                                word.addMisspellingOf(words[ii]);
                                words[ii].addMisspellings(word);
                            }
                        }
                    } else {
                        logger.finest("word entry for " +
                                word.wordstring +
                                " has weird attr " + attrString);
                    }
                    multiWordFlag = false;
                } else if(!skipAttr) {
                    attrAtom = makeAtom(attrString);
                    if(singleFlag) {
                        worde.putdict(attrAtom, varray[0]);
                        if(varray.length > 1) {
                            logger.finest("**** too many vals for " +
                                    attrString +
                                    " of word " + word.printString());
                        }
                    } else {
                        worde.putdict(attrAtom, this.makeList(varray));
                    }
                }
            }
            StringTokenizer temp = new StringTokenizer(valueString, " \t\n\r");
            while(temp.hasMoreElements()) {
                token = temp.nextToken();
                if(firstFlag || categoryFlag) {
                    if((this.getCategory(token)) == null) {
                        logger.finest("**** warning: unknown category : " +
                                token + " in lexical entry for " +
                                word.wordstring + " -- made one");
                    }
                    Category cat = this.makeCategory(token);
                    values.addElement(cat);
                    if(cat.isRootCategory()) {
                        rootcats.addElement(cat); //keep total list of roots
                    }          // note: this is not enough to support an isPenaltyRootOfCat
                } else if(wordFlag) {
                    values.addElement(this.innerMakeWord(token));
                } else if(numFlag) { //pmartin 25aug00 for fraction values
                    try {
                        int slashpos, numerator, denominator;
                        double newNumVal;
                        if((slashpos = token.indexOf("/")) > 0) {
                            numerator = Integer.parseInt(token.substring(0,
                                    slashpos));
                            denominator = Integer.parseInt(token.substring(
                                    slashpos + 1));
                            newNumVal = ((double) numerator /
                                    (double) denominator);
                            worde.numericalValue = new Double(newNumVal);
                        } else {
                            worde.numericalValue = new Double(token);
                        }
                    } //see if this atom has a numericalNumber value
                    catch(NumberFormatException e) {
                    }
                    if(worde.numericalValue != null) {
                        worde.numeralp = true;
                        double tempD = 0;
                        int tempI = 0;
                        boolean tempB = false;
                        try {
                            tempI = worde.numericalValue.intValue();
                            tempD = worde.numericalValue.doubleValue();
                            tempB = true;
                        } catch(Exception e) {
                        }
                        if(tempB && tempD == tempI) {
                            worde.numericalValue = new Integer(tempI);
                        }
                    } else {
                        logger.finest("couldn't make Double value for " +
                                word.wordstring + " from " + token);
                    }
                } else {
                    values.addElement(this.makeAtom(token));
                }
            }
            if((values.size() == 0) && !firstFlag && //allow no noPenaltyCategories
                    !numFlag && !valueFlag) //valueFlag set them already
            {
                logger.finest("**** warning: no values for " +
                        attrString +
                        " in lexical entry for " + word.wordstring);
            }
            if(values.size() > 0) {
                if(categoryFlag || firstFlag) {
                    Category[] array = new Category[values.size()];
                    values.copyInto(array);

                    if(firstFlag) {
                        worde.noPenaltyCategories =
                                LexiconUtil.mergeArrays(
                                worde.noPenaltyCategories, array);
                    } else if(attrString.equals("p1")) {
                        worde.penalty1Categories = array;
                    } else if(attrString.equals("p2")) {
                        worde.penalty2Categories = array;
                    } else if(attrString.equals("p3")) {
                        worde.penalty3Categories = array;
                    } else if(attrString.equals("p4")) // pmartin 17apr02
                    {
                    } /* throw it away for now -- too far out */ else if(attrString.
                            equals("wordclass")) //pmartin 9Nov99
                    {
                        worde.putdict(wordClassAtom, array[0]);
                    }
                } else if(wordFlag) {
                    Word[] wArray = null;
                    if(values.size() > 0) {
                        wArray = new Word[values.size()];
                        values.copyInto(wArray);
                    }
                    if(attrString.equals("root") ||
                            attrString.equals("has-root")) {
                        worde.roots = wArray;
                    } else if(attrString.equals("prefix") ||
                            attrString.equals("has-prefix")) {
                        worde.prefixes = wArray;
                    } else if(attrString.equals("suffix") ||
                            attrString.equals("has-suffix")) {
                        worde.suffixes = wArray;
                    } else if(attrString.equals("compound-of")) {
                        word.addCompoundOf(wArray);
                    } else if(attrString.equals("subsenses")) {
                        worde.subsenses = wArray;
                    }
                } else if(!numFlag) {
                    Atom[] array = new Atom[values.size()];
                    values.copyInto(array);

                    if(attrString.equals("features")) {
                        worde.features = array;
                    } else if(attrString.equals("icodes")) {
                        worde.inflectionCodes = array;
                    }
                //else worde.putdict(makeAtom(attrString), this.makeList(array));
                }
            }
            firstFlag = false; //turn firstflag off now for subsequent fields
        }
        if(rootcats.size() > 0) { //set up root categories
            Category[] rootcatArray = new Category[rootcats.size()];
            rootcats.copyInto(rootcatArray);
            worde.rootCategories = LexiconUtil.mergeArrays(worde.rootCategories,
                    rootcatArray);
        }
        // now fixup form, agr, number, tense properties that should
        // have become icats.. pmartin 9aug01
        word.fixIcatProperties(false);

        /*  if (debug) logger.finest("Finished Entry is: (" +
        word.printEntryString() + ")\n" +
        "        with root cats: " +
        (makeList(word.rootCategories)).printString());
         */
        return word;
    }

    public boolean isWordProp(String attrString) { // made into hashSet
        return wordProperties.contains(attrString);
    }

    public boolean loadFile(String filename) {
        int deferredThisFile;
        int previouslyDeferred = reloadEntries.size();
        BufferedReader inBfRdr = null;
        FileReader fRdr = null;
        String input;
        Word result;
        try {
            fRdr = new FileReader(filename);
            inBfRdr = new BufferedReader(fRdr);
        } catch(FileNotFoundException e) {
            return false;
        }
        try {
            while(true) {
                input = inBfRdr.readLine(); //try using readUTF
                if(input != null) {
                    if(input.length() > 0) {
                        try {
                            result = makeEntry(input);
                            if(result == null) // couldn't make any more words
                            {
                                return false;
                            }
                        } catch(OutOfMemoryError e) {
                            return false;
                        }
                    }
                } else {
                    break;
                }
            }
        } catch(IOException e) {
            return false;
        }

        deferredThisFile = reloadEntries.size() - previouslyDeferred;
        if(deferredThisFile > 0) {
            if(inBfRdr != null) {
                try {
                    inBfRdr.close();
                } catch(IOException e) {
                    inBfRdr = null;
                }
            }
        }
        if(fRdr != null) {
            try {
                fRdr.close();
            } catch(IOException e) {
                fRdr = null;
            }
        }
        return true;
    }

    public void reloadDeferredEntries() {
        /* pmartin 20aug01 retry any entries that had loose ends on first effort*/
        if(reloadEntries.size() > 0) {
            logger.finest("Reloading " + reloadEntries.size() +
                    " deferred words");
            for(int i = 0; i < reloadEntries.size(); i++) {
                makeEntry((String) reloadEntries.get(i));
            }
            reloadEntries.clear();
        }
    }

    public void dumpHistory(String hist, String fname) {
        PrintWriter pwout;
        FileWriter fwout;
        BufferedWriter bwout;
        try {
            fwout = new FileWriter(fname + ".bhis");
            bwout = new BufferedWriter(fwout);
            pwout = new PrintWriter(bwout, true);
            pwout.print("Binary format of 13apr04\nProduction Flag set to " +
                    productionFlag + "\n" + hist);
            pwout.close();
            bwout.close();
            fwout.close();
        } catch(IOException e) {
            return;
        }
        return;
    }

    public String restoreHistory(String fname) {
        String bhis = "";
        BufferedReader inBfRdr = null;
        FileReader fRdr = null;
        String input;
        try {
            fRdr = new FileReader(fname + ".bhis");
            inBfRdr = new BufferedReader(fRdr);
        } catch(FileNotFoundException e) {
            return "";
        }
        try {
            while(true) {
                input = inBfRdr.readLine();
                if(input == null || input.length() == 0) {
                    break;
                }
                if(bhis.equals("")) {
                    bhis += input;
                }
            }
        } catch(IOException e) {
            ;
        }

        if(inBfRdr != null) {
            try {
                inBfRdr.close();
            } catch(IOException e) {
                inBfRdr = null;
            }
        }
        if(fRdr != null) {
            try {
                fRdr.close();
            } catch(IOException e) {
                fRdr = null;
            }
        }
        return bhis;
    }

    public void saveFile(String filename) {
        saveFile(filename, "morphserver");
        return;
    }

    public void saveFile(String filename, String authorStr) {
        saveFile(filename, makeAtom(authorStr));
        return;
    }

    public void saveFile(String filename, Atom authorAtom) {
        /* save words created by authorAtom to jlf file */
        PrintWriter out;
        Enumeration words = wordTable.elements();
        Word key;
        if(authorFlag) {
            if(debug) {
                try {
                    out = new PrintWriter(new BufferedWriter(new FileWriter(
                            filename)), true);
                    while(words.hasMoreElements()) {
                        key = (Word) words.nextElement();
                        if(key.authorp(authorAtom)) {
                            if(authorFlag) {
                                if(debug) {
                                    logger.finest(key.toString() + " > " +
                                            key.printEntryString());
                                }
                            }
                            out.println(key.printEntryString());
                        }
                    }
                } catch(IOException e) {
                    return;
                }
            }
        }
        return;
    }

    public void saveFileWithProp(String filename, String prop) {
        /* saves words with a value for prop to dictionary file */
        Atom propAtom = makeAtom(prop);
        saveFileWithProp(filename, propAtom);
    }

    public void saveFileWithProp(String filename, Atom propAtom) {
        /* saves words with a value for prop to dictionary file */
        PrintWriter out;
        Enumeration words = wordTable.elements();
        Word key;
        if(authorFlag) {
            if(debug) {
                try {
                    out = new PrintWriter(new BufferedWriter(new FileWriter(
                            filename)), true);
                    while(words.hasMoreElements()) {
                        key = (Word) words.nextElement();
                        if(key.getdict(propAtom) != null) {
                            if(debug) {
                                logger.finest(key.toString() + " > " +
                                        key.printEntryString());
                            }
                            out.println(key.printEntryString());
                        }
                    }
                } catch(IOException e) {
                    return;
                }
            }
        }
        return;
    }

    public void clear() {
        wordTable = new ConcurrentHashMap(wordTableSize, 0.5f, 16);
        wordFreePos = 0;
        return;
    }

    public void setSize(int size) {
        wordTableSize = size;
        this.clear();
        return;
    }

    public Atom makeAtom(String namestring) {
        String str = namestring.toLowerCase();
        Atom val = (Atom) atomTable.get(str);

        if(val == null) {

            //pmartin 20jan00 let tables grow
            if((atomFreePos > 1) && ((atomFreePos % atomTableSize) == 1)) {
                logger.finest("\n****growing atom table: original  " +
                        atomTableSize + ":to " +
                        atomFreePos + " making " + str);
            }

            val = new Atom(this, str);
            atomTable.put(str, val);
            atomFreePos++;
        }
        return val;
    }

    public Atom makeAtom(Number num) { // pmartin 15Sep99
        return makeAtom(num.toString());
    }

    public Atom makeAtom(int intVal) {  // pmartin 15Sep99
        return makeAtom(Integer.toString(intVal));
    }

    public Atom getAtom(String namestring) {
        return (Atom) atomTable.get(namestring.toLowerCase()); //pmartin 13sep99
    }

    public Category makeCategory(String namestring) {
        String _str = namestring.toLowerCase();
        Category _val = (Category) categoryTable.get(_str);

        if(_val == null) {
            // if (authorFlag)
            //          if (debug)

            if(categoryFreePos < categoryTableSize) {
                /* will add code later to enlarge table when full */

                Vector _subCats = new Vector(defaultCatListSize);
                boolean _canonical = true;
                boolean _addSubCats = true;
                String _catTok;
                String _unCanonicalizedString = "";
                int _strCompare;

                StringTokenizer _catTokens = new StringTokenizer(_str,
                        " ,/\t\n\r");
                while(_catTokens.hasMoreTokens()) {
                    _catTok = _catTokens.nextToken();
                    if(_subCats.size() > 0) {
                        _unCanonicalizedString += "/";
                    }
                    _unCanonicalizedString += _catTok;
                    boolean _needsToBeAdded = true;
                    for(int i = 0; i < _subCats.size(); i++) {
                        _strCompare = _catTok.compareTo((String) _subCats.
                                elementAt(i));
                        if(_strCompare == 0) { /* found a duplicate */
                            _canonical = false;
                            _needsToBeAdded = false;
                            break;
                        } else if(_strCompare < 0) { /* out of alpha order */
                            _canonical = false;
                            _needsToBeAdded = false;
                            _subCats.insertElementAt(_catTok, i);
                            break;
                        }
                    }
                    if(_needsToBeAdded) {
                        /* it goes at the end */
                        _subCats.addElement(_catTok);
                    }
                }
                /* insert the un-canonical category */
                if(_canonical == false) { /* add the non-canonical one too */
                    //if (debug) logger.finest("New category isn't in canonical form:" +
                    //        _unCanonicalizedString);
                    String _canonicalCatString = (String) _subCats.elementAt(0);
                    for(int k = 1; k < _subCats.size(); k++) {
                        _canonicalCatString += "/" + _subCats.elementAt(k);
                    }
                    /*  _canonicalCatString = _canonicalCatString; removed intern */
                    //if(authorFlag)
                    //  if (debug)
          /* check for canonical string already in table */
                    _val = (Category) categoryTable.get(_canonicalCatString);
                    if(_val == null) {
                        //if (authorFlag)
                        //if (debug)
                        //logger.finest("canonical form of " + _unCanonicalizedString +
                        //          " wasn't already a category, " +
                        //          "so adding new category: " + _canonicalCatString);
                        _val = new Category(this, _canonicalCatString);
                        categoryTable.put(_canonicalCatString, _val);
                        categoryFreePos++;
                    } else { /* the canonical category already has the subCats */
                        _addSubCats = false;
                    }
                } else {  //despite its name, it is in canonical form
                    _val = new Category(this, _unCanonicalizedString);
                    canonicalCategories++;
                }

                /* connect the string with the value of the canonical category */
                categoryTable.put(_str, _val);
                //if(authorFlag)
                //if (debug)

                /* Add the sub cats if there are any and there wasn't already a cat */
                if(_addSubCats & (_subCats.size() > 1)) {
                    /* fill in the subCats of this category */
                    //if(authorFlag)
                    // if (debug)
                    Category[] _subCatArray = new Category[_subCats.size()];
                    // not interned... so can't do this
                    //        _subCats.copyInto(_subCatArray);
                    //
                    for(int i = 0; i < _subCats.size(); i++) {
                        _subCatArray[i] = makeCategory((String) _subCats.
                                elementAt(i));
                    }
                    _val.subcats = _subCatArray;

                    if(authorFlag) {
                        if(debug) {
                            String dbgSubs = "";
                            for(int ii = 0; ii < _subCats.size(); ii++) {
                                dbgSubs += _subCatArray[ii] + " ";
                            }
                        //if(authorFlag)
                        }
                    }
                }
                categoryFreePos++;
            }
        }
        if(LexiconUtil.isMembOfArray(_val, nonrootCategories)) {
            _val.isRootCategory = false;
        }
        //if(authorFlag)
        //if (debug)
        return _val;
    }

    public Category getCategory(String nameString) {
        return (Category) categoryTable.get(nameString.toLowerCase()); // 13sep99
    }

    public Category[] getCategories(String catString, String breakChars) {

        //if (authorFlag)
        //if (debug)
        // logger.finest("Starting getCategories ("+ catString +", "+ breakChars +")");

        Category[] catArray = null;
        Vector categories = new Vector(defaultCatListSize);
        String token;

        StringTokenizer temp = new StringTokenizer(catString, breakChars);
        while(temp.hasMoreTokens()) {
            token = temp.nextToken();
            categories.addElement(this.makeCategory(token));
        }
        if(categories.size() > 0) {
            catArray = new Category[categories.size()];

            categories.copyInto(catArray);

        //if(authorFlag)
        // if (debug)
        }
        return catArray;
    }

    public Category[] setCategories(String catString) {
        Category[] catArray = this.getCategories(catString, " ,\t");
        if(catArray != null) {
            return catArray;
        } else {
            return null;
        }
    }

    // 11oct pmartin added default test cats string
    public Category[] setTestCategories(String catString) {
        if((catString == null) || (catString.equals(""))) {
            catString = defaultTestCategoriesString;
        }
        Category[] catArray = this.getCategories(catString, " ,\t");
        if(catArray != null) {
            this.testCategories = catArray;
            return catArray;
        } else {
            return null;
        }
    }

    public Category[] setSubcategories(String catname, String subcats) {
        Category cat = this.getCategory(catname);
        if(cat == null) {
            logger.finest("**** warning: " +
                    "unknown category in setSubcategories(" +
                    catname + ", ...)");
            cat = this.makeCategory(catname);
        }
        Category[] catArray = this.getCategories(subcats, " ,\t");
        if(catArray != null) {
            //if(authorFlag)
            //if (debug)
            cat.subcats = catArray;
            return catArray;
        } else {
            return null;
        }
    }

    public int setCategoryBits() { // pmartin 13mar02
        Enumeration cats = categoryTable.elements();
        HashSet uniCats = new HashSet(defaultCategoryTableSize);
        HashSet orCats = new HashSet(defaultCategoryTableSize);
        Category cat;
        while(cats.hasMoreElements()) {
            cat = (Category) cats.nextElement();
            cat.myCatBits = null;
            cat.subCatsBits = null;
            String cws = cat.wordstring;
            if((cws.indexOf("/") > 0) || (cws.indexOf(",") > 0) ||
                    (cws.indexOf("\t") > 0) || (cws.indexOf("\n") > 0) ||
                    (cws.indexOf("\r") > 0)) {
                orCats.add(cat);
            } else {
                uniCats.add(cat);
            }
        }
        int ucnum = uniCats.size();
        Category[] uc = new Category[ucnum];
        int basize = (ucnum + 63) / 64; // how many longs do we need?
        int jcatnum = 0;
        int testunis = 0;
        // fill in the self bits for uni-cats
        // but do the ones in TestCategories first and in order
        if(testCategories != null) {
            for(int i = 0; i < testCategories.length; i++) {
                cat = testCategories[i];
                if(uniCats.contains(cat)) {
                    long[] ba = new long[basize];
                    ba[jcatnum / 64] = LONGHIBIT >>> (jcatnum % 64);
                    uc[jcatnum++] = cat;
                    cat.myCatBits = ba;
                    uniCats.remove(cat);
                }
            }
            testunis = jcatnum;
        }
        if(uniCats.size() > 0) {
            Iterator it = uniCats.iterator();
            while(it.hasNext()) {
                cat = (Category) it.next();
                long[] ba = new long[basize];
                ba[jcatnum / 64] = LONGHIBIT >>> (jcatnum % 64);
                uc[jcatnum++] = cat;
                cat.myCatBits = ba;
            }
        }
        // now mark all the unit category subsumptions
        HashSet triedCats = new HashSet(defaultCategoryTableSize);
        for(int i = 0; i < ucnum; i++) {
            cat = uc[i];
            long[] subsba = new long[basize];
            for(int j = 0; j < ucnum; j++) {
                triedCats.clear();
                Category maybeSubCat = uc[j];
                if(cat.subsumesCategory(maybeSubCat, triedCats)) {
                    for(int ii = 0; ii < basize; ii++) {
                        subsba[ii] |= maybeSubCat.myCatBits[ii];
                    }
                }
            }
            cat.subCatsBits = subsba;
        }
        // now fill in the or-ed categories
        int orcs = orCats.size();
        Iterator ot = orCats.iterator();
        while(ot.hasNext()) {
            cat = (Category) ot.next();
            long[] myba = new long[basize];
            long[] subsba = new long[basize];
            for(int k = 0; k < cat.subcats.length; k++) {
                for(int j = 0; j < basize; j++) {
                    myba[j] |= cat.subcats[k].myCatBits[j];
                    subsba[j] |= cat.subcats[k].subCatsBits[j];
                }
                cat.myCatBits = myba;
                cat.subCatsBits = subsba;
            }
        }

        // now fix up nonameCats as the complement of all properCats
        for(int k = 0; k < basize; k++) {
            long invert = ~properCats.myCatBits[k];
            nonnameCats.myCatBits[k] = invert;
            nonnameCats.subCatsBits[k] = invert;
        }
//      System.out.println("setCategoryBits used " + basize +
//                         " longs for " + ucnum + " basic (incl " +
//                         testunis + " ordered test ones) and " +
//                         orcs + " or-ed categories");
//      System.out.print("properCats = ");
//      properCats.showCategory();
//      System.out.print("nonnameCats = ");
//      nonnameCats.showCategory();

        logger.finest("setCategoryBits used " + basize +
                " longs for " + ucnum + " basic (incl " +
                testunis + " ordered test ones) and " +
                orcs + " or-ed categories");
        return basize;
    }

    public void showCategories() { // 25july01 pmartin
        logger.finest("Showing categoryTable of " + canonicalCategories +
                " categories with " + categoryTable.size() + " names");
        Enumeration cats = categoryTable.elements();
        while(cats.hasMoreElements()) {
            ((Category) cats.nextElement()).showCategory();
        }
    }

    public List makeList(Value input) {
        if(input == null) {
            return makeList(new Atom[0]);
        }
        return new List(this, input);
    }

    public List makeList(Value[] input) {
        if(input == null) {
            return makeList(new Atom[0]);
        }
        return new List(this, input);
    }

    public List makeList(Vector vinput) {// added 4oct01 pmartin
        if(vinput == null) {
            return makeList(new Atom[0]);
        }
        return new List(this, vinput);
    }

    public List makeList(ArrayList input) {
        if(input == null) {
            return makeList(new Atom[0]);
        }
        int siz = input.size();
        Value[] va = new Value[siz];
        va = (Value[]) (input.toArray(va));
        return new List(this, va);
    }

    public List makeListOfWords(Word w1) {
        return makeList(new Word[]{w1});
    }

    public List makeListOfWords(Word w1, Word w2) {
        return makeList(new Word[]{w1, w2});
    }

    public List makeListOfWords(Word w1, Word w2, Word w3) {
        return makeList(new Word[]{w1, w2, w3});
    }

    public List makeListOfAtoms(String input) {  // pmartin addition 17 July 99
        return makeListOfAtoms(input, "/");
    }

    public List makeListOfAtoms(String input, String breakChars) {
        Atom[] aarray = null;
        ArrayList contents = new ArrayList(defaultCatListSize);
        String token;

        StringTokenizer temp = new StringTokenizer(input, breakChars);
        while(temp.hasMoreTokens()) {
            token = temp.nextToken();
            contents.add(this.makeAtom(token));
        }
        if(contents.size() > 0) {
            aarray = new Atom[contents.size()];
            aarray = (Atom[]) (contents.toArray(aarray));
        }
        return this.makeList(aarray);
    }

    public Value[] valuesFromString(String input, boolean makeWordFlag) {
        /**reads values from string,
        wordFlag true means make words instead of atoms */
        int vtype = ATOMVALS;
        if(makeWordFlag) {
            vtype = WORDVALS;
        }
        return valuesFromString(input, vtype);
    }

    public Value[] valuesFromString(String input, int vtype) {
        /** reads values from string, building lists if needed.
        vtype is either
        ATOMVALS, yielding atoms
        WORDVALS yielding words,
        STRUCTVALS yielding an alist with atom tags and word vals,
        or MULTIWORDVALS where lists of words are made into multiWords
         */
        QuoteStringTokenizer listEnumerator =
                new QuoteStringTokenizer(input, "|", "()", true);
        ArrayList listValues = new ArrayList(defaultCatListSize);
        Stack nestedLists = new Stack();
        String listToken, atomToken;
        Word newword;
        List newlist;
        int parenDepth = 0;
        while(listEnumerator.hasMoreTokens()) {
            listToken = listEnumerator.nextToken();
            if(listToken.equals(")")) {
                if(parenDepth > 0) { //we've finished a sublist
                    if(listValues.size() > 0) {
                        if(vtype == MULTIWORDVALS) {
                            newword = makeWord(listValues);
                            //logger.finest("valuesFromString just made word " +
                            //                    newword.wordstring);
                            newlist = null; // keep compiler happy
                        } else {
                            newlist = makeList(listValues);
                            newword = null; // appease compiler
                        }
                    } else {
                        newlist = emptyList;
                        newword = null;
                    //  newlist = null; removed 19oct00 for safer mapping
                    }
                    listValues = (ArrayList) nestedLists.pop();
                    if(vtype == MULTIWORDVALS) {
                        listValues.add(newword);
                    } else {
                        listValues.add(newlist);
                    }
                    parenDepth--;
                } else {
                    logger.finest("**** warning: unmatched right paren in: " +
                            input);
                    return null;
                }
            } else if(listToken.equals("(")) {
                nestedLists.push(listValues); //save previous working list
                listValues = new ArrayList(defaultCatListSize); //start new list
                parenDepth++;
            } // removed pmartin 10 oct 2000 for word nil...
            //   else if (listToken.equals("nil")) { // need to store a real null
            // listValues.add(null);  // pmartin 4Oct99
            // }
            else {
                StringTokenizer temp = new StringTokenizer(listToken, " \t\n\r");
                Value tempv;
                while(temp.hasMoreTokens()) {
                    atomToken = temp.nextToken();
                    if(vtype == ATOMVALS) {
                        listValues.add(this.makeAtom(atomToken));
                    } else if((vtype == WORDVALS) || (vtype == MULTIWORDVALS)) {
                        // special treatment of "nil" removed 19oct00 pmartin
                        //if (atomToken.equals("nil")){
                        //listValues.add(null);
                        // }else{
                        listValues.add(this.innerMakeWord(atomToken));
                    // }
                    } else if(vtype == STRUCTVALS) { //pmartin 20oct00 for c-structs
                        if((temp.hasMoreTokens()) || (listValues.size() == 0)) {
                            tempv = this.makeAtom(atomToken);
                        } else {
                            tempv = this.innerMakeWord(atomToken);
                        }
                        listValues.add(tempv);
                    } else {
                        logger.finest("**** warning: strange Value type " +
                                +vtype + " requested for " +
                                input);
                        listValues.add(this.makeAtom(atomToken));
                    }
                }
            }
        }
        if(parenDepth > 0) {
            logger.finest("**** warning: unmatched right paren in: " +
                    input);
            return null;
        }
        Value[] retVals = new Value[listValues.size()];
        return (Value[]) (listValues.toArray(retVals));
    }

    /* methods to interface Lexicon objects to the BitBuffer */
    public Atom[] decodeAtoms(BitBuffer bb) {
        if(bb.pop()) {
            return null;
        }
        Atom[] atms;
        if(bb.pop()) {
            atms = new Atom[1];
            atms[0] = decodeAtomP(bb);
            return atms;
        }
        int[] ints = bb.differenceDecodeArray();
        if(ints == null) {
            return null;
        }
        int n = ints.length;
        atms = new Atom[n];
        for(int i = 0; i < n; i++) {
            atms[i] = (Atom) valueIndexTable[ints[i]];
        }
        return atms;
    }

    public Category[] decodeCategories(BitBuffer bb) {
        if(bb.pop()) {
            return null;
        }
        Category[] cats;
        if(bb.pop()) {
            cats = new Category[1];
            cats[0] = (Category) decodeAtomP(bb);
            return cats;
        }
        int[] ints = bb.differenceDecodeArray();
        if(ints == null) {
            return null;
        }
        int n = ints.length;
        cats = new Category[n];
        for(int i = 0; i < n; i++) {
            cats[i] = (Category) valueIndexTable[ints[i]];
        }
        return cats;
    }

    public Word[] decodeWords(BitBuffer bb) {
        if(bb.pop()) {
            return null;
        }
        Word[] wds;
        if(bb.pop()) {
            wds = new Word[1];
            wds[0] = decodeWordP(bb);
            return wds;
        }
        int[] ints = bb.differenceDecodeArray();
        if(ints == null) {
            return null;
        }
        int n = ints.length;
        wds = new Word[n];
        for(int i = 0; i < n; i++) {
            Value vit = valueIndexTable[ints[i]];
            if(vit instanceof Word) {
                wds[i] = (Word) vit;
            } else {
                System.err.println("decoding word array found " + vit);
                wds[i] = makeWord(vit.toString());
            }
        }
        return wds;
    }

    public List decodeList(BitBuffer bb) {
        int tag = bb.gammaDecode();
        if(tag != LISTVALS) {
            System.err.println("bad list tag " + tag +
                    " in decodeList");
        }
        Value[] vals = decodeValues(bb);
        if((vals == null) || (vals.length == 0)) {
            return null;
        }
        List newl = new List(this, vals);
        return newl;
    }

    public Value[] decodeValues(BitBuffer bb) {
        Value[] vals = null;
        if(bb.pop()) {
            return null;
        }
        int nplusone = bb.gammaDecode();
        if(nplusone > 1) {
            int n = nplusone - 1;
            vals = new Value[n];
            for(int i = 0; i < n; i++) {
                vals[i] = decodeValue(bb);
            }
        }
        return vals;
    }

    public Atom decodeAtom(BitBuffer bb, int idx) {
        Atom atm = (Atom) valueIndexTable[idx];
        atm.index = idx;
        bb.reset();
        atm.numericalValue = decodeNumber(bb);
        atm.props = decodeValueHash(bb);
        return atm;
    }

    public Category decodeCategory(BitBuffer bb, int idx) {
        Category cat = (Category) valueIndexTable[idx];
        //System.out.print("debug decodeCategory idx=" + idx + " ");
        //cat.showCategory();
        cat.index = idx;
        bb.reset();
        cat.numericalValue = decodeNumber(bb);
        cat.props = decodeValueHash(bb);
        cat.subcats = decodeCategories(bb);
        cat.isRootCategory = bb.pop();
        //System.out.print("end of decode ");;
        //cat.showCategory();
        return cat;
    }

    public Word decodeWord(BitBuffer bb, int idx, int unpackAct) { // pmartin 17sep01
        boolean coreWord;
        Word wd = (Word) valueIndexTable[idx];
        wd.index = idx;
        bb.reset();
        coreWord = bb.pop();
        wd.dontPurge = coreWord;
        boolean makeWE = bb.pop();
        WordEntry we = null;

        if(!makeWE) {
            wd.index = NOWORDENTRY | wd.index;  // tell everybody to ignore disk
        } else if(coreWord || (unpackAct == UNPACKALL)) {
            if(wd.hasWordEntry()) {
                we = wd.wordEntry;
            } else {
                we = new WordEntry(this, wd);
                wd.index = (wd.index & WORDINDEXMASK); // clear marks
                reloadedWordEntries++;
            }
            we.noPenaltyCategories = decodeCategories(bb);
            we.penalty1Categories = decodeCategories(bb);
            we.penalty2Categories = decodeCategories(bb);
            we.penalty3Categories = decodeCategories(bb);
            we.rootCategories = decodeCategories(bb);
            we.features = decodeAtoms(bb);
            we.capcodes = decodeList(bb);
            we.roots = decodeWords(bb);
            we.prefixes = decodeWords(bb);
            we.suffixes = decodeWords(bb);
            we.compoundOf = decodeValues(bb);
            we.variantOf = decodeWords(bb);

            /*Word[] variants = we.variantOf;
            if (variants != null) {
            System.out.println(we.printWordEntryString() + " HAS VARIANTS");
            }*/
            we.ikoParents = decodeWords(bb);
            we.iioParents = decodeWords(bb);
            we.inflectionCodes = decodeAtoms(bb);
            we.subsenses = decodeWords(bb);
            we.numeralp = bb.pop();
            we.numericalValue = decodeNumber(bb);
            we.properties = decodeValueHash(bb);
            we.sensenamep = null;
            if(bb.pop()) {
                we.sensenamep = new Object[3];
                we.sensenamep[0] = decodeCategoryP(bb);
                we.sensenamep[1] = decodeWordP(bb);
                we.sensenamep[2] = bb.decodeUTF();
            }
        } else if(unpackAct == KEEPPACKED) {
            bb.reset(); // put the BB index back to front
            reloadedBitBuffers++;
            wd.bbuf = bb;
        }
        wd.wordEntry = we;
        return wd;
    }

    public Value decodeValue(BitBuffer bb) {
        int tag = bb.gammaDecode();
        if(tag == LISTVALS) {
            Value[] dvals = decodeValues(bb);
            if((dvals == null) || (dvals.length == 0)) {
                return null;
            } else {
                return new List(this, dvals);
            }
        } else if(tag == WORDVALS) {
            return decodeWordP(bb);
        } else if(tag == CATVALS) {
            return decodeCategoryP(bb);
        } else if(tag == ATOMVALS) {
            return decodeAtomP(bb);
        } else {
            return null;
        }
    }

    public Word decodeWordP(BitBuffer bb) {
        return (Word) valueIndexTable[bb.gammaDecode()];
    }

    public Atom decodeAtomP(BitBuffer bb) {
        return (Atom) valueIndexTable[bb.gammaDecode()];
    }

    public Category decodeCategoryP(BitBuffer bb) {
        return (Category) valueIndexTable[bb.gammaDecode()];
    }

    public ConcurrentHashMap decodeValueHash(BitBuffer bb) {
        if(bb.pop()) {
            return null;
        }
        int vhSize = bb.gammaDecode();
        int htsize = Math.max(2, vhSize);
        ConcurrentHashMap vh = new ConcurrentHashMap(htsize);
        Atom key;
        Value val;
        for(int i = 0; i < vhSize; i++) {
            key = decodeAtomP(bb);
            val = decodeValue(bb);
            if(val == null) {
                vh.remove(key);
            } else {
                vh.put(key, val);
            }
        }
        return vh;
    }

    public Number decodeNumber(BitBuffer bb) {
        if(bb.pop()) {
            return null;
        }
        if(bb.pop()) {
            return new Integer(bb.decodeInt());
        }
        // it's 64 bits long
        boolean doublep = bb.pop();
        long lv = bb.decodeLong();
        if(!doublep) {
            return new Long(lv);
        } else {
            return new Double(Double.longBitsToDouble(lv));
        }
    }

    /*  end of BitBuffer encoding and decoding  */
    TreeSet findStubs() { //pmartin 24mar04
        TreeSet sstubs = new TreeSet();
        Enumeration wordEnum = wordTable.elements();
        while(wordEnum.hasMoreElements()) {
            Word testWord = (Word) wordEnum.nextElement();
            if(testWord.getWordEntry() == null) {
                sstubs.add(testWord.wordstring);
            }
        }
        return sstubs;
    }

    public List listAppend(List l1, List l2) { //pmartin 11mar04
        int l1len = l1.length();
        int l2len = l2.length();
        if(l1len == 0) {
            return new List(this, l2);
        }
        if(l2len == 0) {
            return new List(this, l1);
        }
        Value[] newva = new Value[l1len + l2len];
        for(int ii = 0; ii < l1len; ii++) {
            newva[ii] = l1.contents[ii];
        }
        for(int ii = 0; ii < l2len; ii++) {
            newva[l1len + ii] = l2.contents[ii];
        }
        return new List(this, newva);
    }

    // static method to iterate toString over value arrays
    public Value mergeValues(Value v1, Value v2) {
        if((v1 == null) ||
                (v1.listp() && (((List) v1).length() == 0)) ||
                v1.equal(v2)) {
            return v2;
        }
        if((v2 == null) ||
                (v2.listp() && (((List) v2).length() == 0))) {
            return v1;
        }
        Value[] va1, va2;
        if(v1.listp()) {
            va1 = ((List) v1).contents;
        } else {
            va1 = new Value[]{v1};
        }
        if(v2.listp()) {
            va2 = ((List) v2).contents;
        } else {
            va2 = new Value[]{v2};
        }
        return new List(this, LexiconUtil.mergeArrays(va1, va2));
    }

    public List mergeLists(List l1, List l2) {
        // merge two lists
        if((l1 == null) || (l1.length() == 0) || l1.equal(l2)) {
            return l2;
        }
        if((l2 == null) || (l2.length() == 0)) {
            return l1;
        }
        return new List(this, LexiconUtil.mergeArrays(l1.contents, l2.contents));
    }

    public Value makeCapcode(String key) { // semi-automatic transl of Lisp
    /* make-capcode (key) makes the value for a
        capcodes property from a string.
        Note: the function capitalize-by-capcode
        makes an appropriately capitalized
        string from a string and a capcode. */

        String lcString = key.toLowerCase();
        int keyLen = key.length();
        if(key.equals(lcString)) {
            return lcAtom;
        } else if(key.equals(key.toUpperCase())) {
            return ucAtom;
        } else {
            char first = key.charAt(0);
            if((first == Character.toUpperCase(first)) &&
                    (key.regionMatches(1, lcString, 1, (keyLen - 1))) &&
                    (key.indexOf('_') == -1) &&
                    (key.indexOf('-') == -1) &&
                    (key.indexOf('\'') == -1)) {
                return icAtom;
            } else {
                ArrayList uChars = new ArrayList();
                for(int i = 0; i < keyLen; i++) {
                    if(Character.isUpperCase(key.charAt(i))) {
                        uChars.add(makeAtom(i + 1));
                    }
                }
                return makeList(uChars);
            }
        }
    }


    // Comparator interface:
    public boolean equals(Object obj) {
        return (this == obj);
    }

    public int compare(Object o1, Object o2) {
        if(o1 == null) {
            if(o2 == null) {
                return 0;
            } else {
                return -1; //null comes before anything
            }
        } else if(o2 == null) {
            return 1;
        }
        if(o1 instanceof Word && o2 instanceof Word) {
            return compare((Word) o1, (Word) o2);
        }
        if(o1 instanceof WordToken && o2 instanceof WordToken) {
            return compare((WordToken) o1, (WordToken) o2);
        } else {
            return compareStrings(o1.toString(), o2.toString());
        }
    }

    // Sorting functions:
    /**
     * Method for testing if two words are in alphabetical order.
     * Lexicon.compare(x y) is a general ordering function for sorting
     * any mixture of numbers and words.  It orders things in the
     * way a dictionary might, rather than in their ascii order.
     * Specifically, all numbers come first, ordered by numerical
     * value.  Character sequences are ordered so that strings
     * with the same letter sequence but different case (or
     * different accents) will be sorted together.
     * Characters are ordered by a function (compareChars)
     * in which special chars precede digits, which precede
     * alphabetics, and capitals only precede lowercase
     * characters when they are the same letter. A sample
     * of the ordering assigned is: (.2 0.2 2 3.14 20 ! - [ ~
     * 2A 'A' 'a' A a 'A' 'a' '?' '?' '?' '?' '?' '?' '?'
     * ALPHA Alpha alPha alpha B).
     * @param word1 The putative earlier word in the order.
     * @param word2 The putative later word in the order.
     */
    public int compare(Word word1, Word word2) {
        if(word1 == null) {
            if(word2 == null) {
                return 0;
            } else {
                return -1; //null comes before anything
            }
        } else if(word2 == null) {
            return 1;
        } else if(word1 == word2) {
            return 0;
        } else if(word1.numericalValue() != null) {
            if(word2.numericalValue() != null) {
                double val1 = word1.numericalValue().doubleValue();
                double val2 = word2.numericalValue().doubleValue();
                if(val1 > val2) {
                    return 1;
                } else if(val1 < val2) {
                    return -1;
                }
                // integers come before equivalent doubles:
                if(word1.numericalValue() instanceof Integer) {
                    if(!(word2.numericalValue() instanceof Integer)) {
                        return -1;
                    }
                //otherwise they're both integers, so fall through to string test
                } else if(word2.numericalValue() instanceof Integer) {
                    return 1;
                }
            // otherwise they're both doubles, so fall through to string test
            // Now we have two integers or two doubles with same value but different
            // strings E.g., 1 and 001 or 1.2 and 1.200, so don't return here,
            // but fall through to the string comparison below
            } else {
                return -1;
            }
        } else if(word2.numericalValue() != null) {
            return 1; //numbers come before alphabetic
        }
        String wordString1 = word1.getWordString(); //case doesn't matter for ordering words
        String wordString2 = word2.getWordString();
        if(wordString1 == null) {
            if(wordString2 == null) {
                return 0;
            } else {
                return -1; //empty comes before anything
            }
        } else if(wordString2 == null) {
            return 1;
        }
        return compareStrings(wordString1, wordString2);
    }

    /**
     * Method for testing if two word tokens are in alphabetical order,
     * considering two associated capitalization strings to resolve
     * ties.  Otherwise the same as Lexicon.compare(word1, word2).
     * Lexicon.compare(x y) is a general ordering function for sorting
     * any mixture of numbers and words.  It orders things in the
     * way a dictionary might, rather than in their ascii order.
     * Specifically, all numbers come first, ordered by numerical
     * value.  Character sequences are ordered so that strings
     * with the same letter sequence but different case (or
     * different accents) will be sorted together.
     * Characters are ordered by a function (compareChars)
     * in which special chars precede digits, which precede
     * alphabetics, and capitals only precede lowercase
     * characters when they are the same letter. A sample
     * of the ordering assigned is: (0.2 2 3.14 20 ! - [ ~
     * 2A #\\A #\\a A \\a \"A\" \"a\" #\\? \\? \"?\" \"?\" \"?\" \"?\" \"?\"
     * ALPHA \"Alpha\" \"alPha\" \"alpha\" #\\B).
     * @param wt1 The putative earlier WordToken in the order.
     * @param wt2 The putative later WordToken in the order.
     */

//   public int compare(WordToken wt1, WordToken wt2) {
//     Word word1 = wt1.wordObject;
//     Word word2 = wt2.wordObject;
//     if (word1 == null) {
//       if (word2 == null) return 0;
//       else return -1; //null comes before anything
//     }
//     else if (word2 == null) return 1;
//     else if (word1 == word2) return compareStrings(wt1.wordString, wt2.wordString);
//     else if (word1.numericalValue() != null) {
//       if (word2.numericalValue() != null) {
//         double val1 = word1.numericalValue().doubleValue();
//         double val2 = word2.numericalValue().doubleValue();
//         if (val1 > val2) return 1;
//         else if (val1 < val2) return -1;
//         // integers come before equivalent doubles:
//         if (word1.numericalValue() instanceof Integer) {
//           if (! (word2.numericalValue() instanceof Integer)) return -1;
//           //otherwise they're both integers, so fall through to string test
//         }
//         else if (word2.numericalValue() instanceof Integer) return 1;
//         // otherwise they're both doubles, so fall through to string test
//         // Now we have two integers or two doubles with same value but different
//         // strings E.g., 1 and 001 or 1.2 and 1.200, so don't return here,
//         // but fall through to the string comparison below
//       }
//       else return -1;
//     }
//     else if (word2.numericalValue() != null) return 1; //numbers come before alphabetic
//     String wordString1 = wt1.wordString;
//     String wordString2 = wt2.wordString;
//     if (wordString1 == null) {
//       if (wordString2 == null) return 0;
//       else return -1; //empty comes before anything
//     }
//     else if (wordString2 == null) return 1;
//     else return compareStrings(wordString1, wordString2);
//   }
    /**
     * Method for comparing two strings for human-friendly alphabetical order.
     */
    public int compareStrings(String string1, String string2) {
        if(authorFlag) {
            debug("  comparing " + string1 + " with " + string2);
        }
        char char1, char2;
        int caseResult = 0;
        // Set length to the shortest of the two strings.
        int length = string1.length();
        if(length > string2.length()) {
            length = string2.length();
        }
        for(int i = 0; i < length; i++) {
            char1 = string1.charAt(i);
            char2 = string2.charAt(i);
            if(char1 == char2) { //keep searching
            } else if((Character.toLowerCase(char1) ==
                    Character.toLowerCase(char2)) ||
                    ("Aa??????????????".indexOf(char1) >= 0 &&
                    "Aa??????????????".indexOf(char2) >= 0) ||
                    ("Ee????????".indexOf(char1) >= 0 &&
                    "Ee????????".indexOf(char2) >= 0) ||
                    ("Ii????????".indexOf(char1) >= 0 &&
                    "Ii????????".indexOf(char2) >= 0) ||
                    ("Oo??????????????".indexOf(char1) >= 0 &&
                    "Oo??????????????".indexOf(char2) >= 0) ||
                    ("Uu????????".indexOf(char1) >= 0 &&
                    "Uu????????".indexOf(char2) >= 0) ||
                    ("Cc??".indexOf(char1) >= 0 &&
                    "Cc??".indexOf(char2) >= 0) ||
                    ("Nn??".indexOf(char1) >= 0 &&
                    "Nn??".indexOf(char2) >= 0) ||
                    ("Yy??".indexOf(char1) >= 0 &&
                    "Yy??".indexOf(char2) >= 0)) {
                if(caseResult == 0) { //this is first character with just a case or accent difference
                    caseResult = compareChars(char1, char2);
                    if(authorFlag) {
                        debug("  comparing " + char1 + " with " + char2 + " = " +
                                caseResult);
                    }
                }
            } else {
                int charOrder = compareChars(string1.charAt(i),
                        string2.charAt(i));
                if(charOrder != 0) {
                    return charOrder;
                }
            }
        }
        if(caseResult != 0) {
            return caseResult;
        } else if(string1.length() < string2.length()) {
            return -1;
        } else if(string1.length() == string2.length()) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * Method for comparing two chars for alphabetical order.
     * char-order (x y) is an ordering function for
     * characters used in alphorder.  It orders things
     * in the way a dictionary might, rather than in
     * ascii order --  specifically, all special chars
     * come first, followed by digits, followed by
     * alphabetics, and capitals only precede lowercase
     * characters when they are the same letter.  Also,
     * accented characters come after the character they
     * accent, with the following collating sequence:
     * A a ? ? ? ? ? ? ? ? ? ? ? ? ? ? B b C c ? ? D d
     * E e ? ? ? ? ? ? ? ? F f I i ? ? ? ? ? ? ? ? J j
     * N n ? ? O o ? ? ? ? ? ? ? ? ? ? ? ? ? ? P p
     * U u ? ? ? ? ? ? ? ? V v Y y ? ? Z z
     * (some of the upper case versions are nonprinting).
     */
    public int compareChars(char char1, char char2) {
        if(authorFlag) {
            debug("  comparing " + char1 + " with " + char2);
        }
        String alphaChars = letterOrder;
        int pos1 = alphaChars.indexOf(char1);
        int pos2 = alphaChars.indexOf(char2);
        if(char1 == char2) {
            return 0;
        } else if(pos1 >= 0) { //char1 is alphabetic
            if(pos1 < pos2) {
                return -1; //they're in order
            } else {
                return 1;
            }
        //this includes the case of pos2 < 0 (and the equal case is already done)
        } else if(pos2 >= 0) {
            return -1; //char1 is not alpha and char2 is; so they're in order
        } else if(Character.isLetter(char1) && !Character.isLetter(char2)) {
            return 1;
        } else if(Character.isLetter(char2) && !Character.isLetter(char1)) {
            return -1;
        } else if(Character.isDigit(char1) && !Character.isDigit(char2)) {
            return -1;
        } else if(Character.isDigit(char2) && !Character.isDigit(char1)) {
            return 1;
        } else if((char1 == '_') && !(" \t\n\r".indexOf(char2) >= 0)) {
            return -1; // _ precedes all
        } else if((char2 == '_') && !(" \t\n\r".indexOf(char1) >= 0)) {
            return 1;  // but white space
        } else if((int) char1 < (int) char2) {
            return -1;
        } else if((int) char2 < (int) char1) {
            return 1;
        } else {
            return 0;
        }
    }
    public String letterOrder =
            "Aa??????????????BbCc??DdEe????????FfGgHhIi????????JjKkLlMmNn??Oo??????????????PpQqRrSsTtUu????????VvWwXxYy??Zz";

    /**
     * For debugging.
     */
    public static final boolean authorFlag = false;
    //     public static final boolean authorFlag = true;
    //change authorFlag false for production version

    // for major debugging of lexical calls
    public static boolean debug = false;
    // public static boolean debug = true;

    // for tracing word type checking and creation
    public static boolean wordTrace = false;
    // public static boolean wordTrace = true;

    private static void debug(String str) {
        if(debug) {
        }
    }

    public static void startDebug() {
        debug = true;
    }

    public static void stopDebug() {
        debug = false;
    }
    static Logger logger = Logger.getLogger(Lexicon.class.getName());

    protected static String logTag = "LEX";

}
