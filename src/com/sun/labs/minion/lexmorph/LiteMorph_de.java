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

import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * This is a German version of LiteMorph
 *
 * @author W. A. Woods
 * @author Ann Houston
 * @version     1.2     05/09/2001
 *
 * The rules for this system were deveoped by Ann Houston, and the
 * Java code for interpreting them was developed by W. A. Woods
 *
 * @see LiteMorph
 */
public class LiteMorph_de extends LiteMorph{

    private static LiteMorph morph;

    /**
     * Make this a singleton class
     */
    private LiteMorph_de() {
    }

    /**
     * Return the LiteMorph for this class
     */
    public static LiteMorph getMorph() {
        if (morph == null) {
            morph = new LiteMorph_de();
        }
        return morph;
    }

    private static Hashtable paradigms = new Hashtable(1500, (float)0.7);

    // added voll- (vollziehen) 26mar01
    // 19apr01 removed re- prefix, causes incorrect analysis of many non prefix verbs that
    // begin with 're'. Also, re- prefix verbs are regular. Are there any irregulars?

    private static String inseparablePrefixes =

    "be de emp ent fehl er ge miss ver voll zer";

    private static String separablePrefixes =

// note:  shorter matches come after longer -- e.g. hin comes after hinter and hinweg

    "ab an auf auseinander aus bei durch ein empor entgegen fern fest herueber" +
//cc     " her&uuml;ber" +
    " her\u00fcber" +
//cc     " gegenueber gegen&uuml;ber gegen heim herab heran herauf heraus herbei herein" +
    " gegenueber gegen\u00fcber gegen heim herab heran herauf heraus herbei herein" +
    " herum herunter" +
//cc     " hervor her hinab hinauf hinaus hinein hinter hinueber hin&uuml;ber hinunter" +
    " hervor her hinab hinauf hinaus hinein hinter hinueber hin\u00fcber hinunter" +
    " hinweg hin los" +
    " mitan miteinander mitein mit nach rad sicher spazieren statt staub stehen" +
//cc     " ueberein ueber &uuml;berein &uuml;ber umher um unter voran voraus vorbei vorein" +
    " ueberein ueber \u00fcberein \u00fcber umher um unter voran voraus vorbei vorein" +
    " vorher vorweg vor" +
    " weg wiederan wiederauf wiederein wiederher wieder weiter zusammen" +
//cc     " zurecht zurueck zur&uuml;ck zu";
    " zurecht zurueck zur\u00fcck zu";

    private static String prefixes = separablePrefixes + " " +
                                     inseparablePrefixes;

    /**
     * function for declaring arguments to computeMorph for author mode checking
     * redefined from dummy method in parent class LiteMorph.
     */
    @Override
  protected String[] computeMorphArgs() { //legal values for arg in computeMorph
      String[] args = {"regVerb", "irrVerb"};
      return args;
    }

    /**
     * function for computing morphological variants of strong verbs
     * redefined from dummy method in parent class LiteMorph.
     */

    @Override
    protected String[] computeMorph(String input, String arg, int depth,
                                    String prefix, String suffix) {
        String paradigm = (String)paradigms.get(input);
        boolean insep = false; boolean sep = false; boolean zuPrefix = false;
        //        if (authorFlag) trace("Paradigm is currently: " + paradigm);
        if (paradigm == null) {
          if (authorFlag)
            trace("Did NOT find " + input + " in Verb Lookup Table");
          String tempPrefix, wordform;
          String[] tempVal = null;
          // collect all the prefixes (separable and inseparable) and check to see if we have one

          StringTokenizer tokens = new StringTokenizer(prefixes, " ");
            while (tokens.hasMoreTokens() && paradigm == null) {
            tempPrefix = tokens.nextToken();
            if (input.startsWith(tempPrefix) &&
                !(tempPrefix.equals("rad") && (input.length() > 3) &&
//cc                   charIsOneOf(input.charAt(3), "aeiou&auml;&ouml;&uuml;")) &&
                  charIsOneOf(input.charAt(3), "aeiou\u00e4\u00f6\u00fc")) &&
//cc                 // ("aeiou&auml;&ouml;&uuml;".indexOf(input.charAt(3)) >= 0)) &&
                // ("aeiou\u00e4\u00f6\u00fc".indexOf(input.charAt(3)) >= 0)) &&
                (prefix.equals("")  ||    //don't remove another prefix if you've already got one
                 (prefix.equals("zu") &&  //unless it's zu followed by an inseparable prefix
                  input.endsWith("en") && //and there is a potential infinitive ending
                  inseparablePrefixes.indexOf(tempPrefix) >= 0))
                ) {
              wordform = input.substring(tempPrefix.length()); //remove the prefix
              if (authorFlag)
                trace("In computeMorph("+arg+"): "+
                      input+" is "+tempPrefix+" + "+wordform);

              if (inseparablePrefixes.indexOf(tempPrefix) >= 0) {
                // This means that tempPrefix is an inseparable prefix, because
                // no separable prefix is a substring of an inseparable prefix.
                // Note that if insep is set to true, it can not later become false
                // because the separable prefixes are all tested first.
                insep = true; }
              else sep = true;
              if (authorFlag)
                trace("TESTING PREFIXES: INSEP: "+ insep +" SEP: "+ sep +
                      " PREFIX: "+ tempPrefix);

              // if wordform begins with 'ge' and we have a prefix, 'ge' is a pastpart marker
              if (wordform.startsWith("zu") && insep==false //a separable prefix precedes the zu
                  // && arg.equals("irrVerb") // not sure this is needed 8mar01
                   )
                wordform = wordform.substring(2);  //strip off optional 'zu'
              else if (insep && prefix.equals("zu") && arg.equals("regVerb") &&
                       wordform.endsWith("en"))
                prefix = ""; //drop zu from infinitive if followed by inseparable prefix
              if ((wordform.length() > 1) &&
                  (paradigms.get(wordform)!=null || sep)) {
                // require at least 2 characters for wordform
                // and only do this for strong verbs or separable prefixes
                if (authorFlag)
                  trace("Trying computeMorph("+arg+") on " + wordform +
                        " with prefix: " + tempPrefix +
                        " and suffix: " + suffix +
                        ", at depth " + (1 + depth));
                tempVal = computeMorph(wordform, arg, depth+1, tempPrefix, suffix);
                //   prefix+tempPrefix, suffix); //waw04/09/01 don't append two prefixes
                // note: this drops the prefix "zu" if there is one
              }
              else if (wordform.endsWith("en") &&
                       paradigms.get("ge"+wordform)!=null) { //try for ge past participle
                if (authorFlag)
                  trace("Trying computeMorph("+arg+") on ge" + wordform +
                        " with prefix: " + tempPrefix +
                        " and suffix: " + suffix +
                        ", at depth " + (1 + depth));
                tempVal = computeMorph("ge"+wordform, arg, depth+1,
                                       tempPrefix, suffix);
              }
              else {
                if (authorFlag)
                  trace("Did NOT find " + wordform +
                        " (prefix stripped) in Verb Lookup Table");
              }
              if (tempVal != null) {
                if (authorFlag)
                  trace("   returning " + tempVal.length + " computed values" +
                        " at depth " + (1 + depth));
                return tempVal;
              }
            }
          }
        }
       // if (input.endsWith("lich")) return null;  // prevents 'schlich' from retrieval in verb lookup table
        if ((paradigm == null) && arg.equals("irrVerb")) return null;

        if (arg.equals("regVerb")) {  //generate forms for regular verb
          Set<String> variants = new HashSet<String>();
          LiteMorphRule[] useRules;
          String infinitive, present, pastpart, baseInfinitive;
          String infinitiveEnding = "en";

          if (authorFlag)
            trace("Analyzing " + input + " as form of regular verb");
          int length = input.length();
          String consonant = "bcdfghjklmnpqrstvwxyz";

          // determine what the present stemform of the regular verb is
          // string is too short to be a German verb, used to be < 5, but 'ge' gets stripped first now
          if (length < 3) return null;
          // check whether input is an -ern or -eln type infinitive
          else if (input.endsWith("ern") || input.endsWith("eln")) {
            infinitiveEnding = input.substring(length-1);  //  infinitive ending is only 'n', not 'en' in these cases
            present = input.substring(0,length-1);  // omit only 'n' to form present tense stem
          }
          // remove 'ge' from past participle, it gets reattached via reg-pastpart
          // distinguish the morphophonemics of 'gearbeitet' (-et) and 'gesagt' (-t)
          else if (input.startsWith("ge") && input.endsWith("et")) {
            present = input.substring(2,length-2);
          }
          else if (input.startsWith("ge") && input.endsWith("t")) {
            present = input.substring(2,length-1);
          }
          // check for 2sg past with stem-final -t (arbeitetest)
          else if (input.endsWith("etest")) {
            present = input.substring(0,length-5);
          }
          // check for 3sg/2pl past and 1pl/3pl past for stem-final t forms (arbeitetet)
          else if (input.endsWith("etet") || input.endsWith("eten")) {
            present = input.substring(0,length-4);
          }
          else if (input.endsWith("tten")) {
            present = input.substring(0,length-2);
            if (authorFlag)
              trace("PRESENT with tten " + present);
          }
          //(input.endsWith("ten") ||   // omit because root 't' gets deleted, e.g., aufbereiten, beschriften
          else if (input.endsWith("tet")) {
            present = input.substring(0,length-3);
          }
          // check for 2sg subj, 2sg pres (stem-final t), 1/3sg past (stem-final t)
          else if (input.endsWith("est") || input.endsWith("ete")) {
            present = input.substring(0,length-3);
          }
          // check whether ending has a preceding e, based on phonological considerations
          else if (input.endsWith("tet") || input.endsWith("det") ||
                   (input.endsWith("et") && length > 5 &&
                    consonant.indexOf(input.charAt(length-3))>=0 &&
                    consonant.indexOf(input.charAt(length-4))>=0 &&
                    consonant.indexOf(input.charAt(length-5))>=0
                    )) {  // strip -et off these verb types, gearbeitet, geleidet, geoeffnet
            present = input.substring(0,length-2);
            if (input.startsWith("ge"))
                present = present.substring(2);  // need to remove pastpart 'ge', if present
          }

          // check for 2pl subj, 2sg pres
          // removed ambiguous -te from this rule 20apr01
          else if (input.endsWith("et") || input.endsWith("st"))  {
            present = input.substring(0,length-2);
          }
          // check whether input is regular -en infinitive
          else if (input.endsWith("en"))  {
            present = input.substring(0,length-2);
          }
          else if (input.endsWith("e") || input.endsWith("t")) {
            present = input.substring(0,length-1);
            // if (authorFlag) trace("Inside ends with t:" + present);
          }
          else {
            present = input;
          }

          baseInfinitive = present+infinitiveEnding;
          infinitive = prefix+present+infinitiveEnding;
          pastpart = present; // this implies that initial 'ge' has been stripped,
                              // and will need to be added back, if required.

          // cases where 'ge' should not be attached at beginning of pastpart  8mar01
          if (present.endsWith("ier") || insep == true) {
            pastpart =  pastpart;
            if (authorFlag)
              trace("ONE PASTPART IS: " + pastpart + " INSEP: " + insep);
          }
          /* else if (insep==false && !((present.length() > 4) &&
                      present.startsWith("rad") &&
//cc                       charIsOneOf(present.charAt(3), "aeiouy&auml;&euml;&iuml;&ouml;&uuml;&yuml;"))) {
                      charIsOneOf(present.charAt(3), "aeiouy\u00e4\u00eb\u00ef\u00f6\u00fc\u00ff"))) {
//cc           //           ("aeiouy&auml;&euml;&iuml;&ouml;&uuml;&yuml;".indexOf(present.charAt(3)) >= 0))) {
          //           ("aeiouy\u00e4\u00eb\u00ef\u00f6\u00fc\u00ff".indexOf(present.charAt(3)) >= 0))) {
            pastpart = "ge" + pastpart;
            if (authorFlag)
              trace("TWO PASTPART IS: " + pastpart);
          } */  // maybe don't need the else if clause
          // otherwise, attach 'ge' as pastpart marker   // 8mar01
          else {
            pastpart = "ge" + pastpart;
            if (authorFlag)
              trace("THREE PASTPART IS: " + pastpart + " INSEP: " + insep);
          }

          // if (authorFlag) trace("TWO PREFIX IS: " + prefix);
          pastpart = prefix + pastpart;   // uncommented 28mar01
          if (authorFlag)
            trace("Using regular paradigm  PREFIX: " + prefix + " INF: " +
                  infinitive + " PRES: " + present + "- PASTP: " +
                  pastpart + "-");
          morphWord(infinitive, depth+1, ":literal", variants);
          if (authorFlag)
            trace("TESTING SEP: " + sep + " PREFIX: " + prefix + " PRES: " +
                  present);
          // used to be a conditional (sep), but sep value gets lost each time we
          // cycle through ComputeMorph. This way is harmless, if prefix is nil  ach 8mar01
          // separable prefixes are attached to verbs in dependent clauses  ach 1mar01
          morphWord(prefix + present, depth+1, ":reg-pres", variants);
          morphWord(prefix + present, depth+1, ":reg-past", variants);
          morphWord(prefix + present, depth+1, ":reg-subj1", variants);
          morphWord(baseInfinitive, depth+1, ":literal", variants);  // waw 27Feb01
          morphWord(present, depth+1, ":reg-pres", variants);
          morphWord(present, depth+1, ":reg-past", variants);   // Note: also form for subj2
          morphWord(present, depth+1, ":reg-subj1", variants);  // subjunctive1 form ach 6dec00
          morphWord(pastpart, depth+1, ":reg-pastpart", variants);
         // morphWord(pastpart, depth+1, ":base-adj", variants); // don't generate these from verb 27apr01
          String[] result = new String[variants.size()];
          int i = 0;
          for (Iterator<String> iter = variants.iterator(); iter.hasNext();) {
            result[i++] = iter.next();
            
        }
          return result;
        }
        else if (arg.equals("irrVerb")) { //generate forms for irregular verb
          Set<String> variants = new HashSet<String>();
          if (authorFlag) trace("Inside IRRVERB");
          LiteMorphRule[] useRules;
          String infinitive, present, past, pastpart, baseInfinitive;
          StringTokenizer tokens = new StringTokenizer(paradigm, " ");
          infinitive = tokens.nextToken();
          present = tokens.nextToken();
          past = tokens.nextToken();
          pastpart = tokens.nextToken();
          if (authorFlag)
            trace("FOUND paradigm in Verb Lookup Table: "+infinitive+" "+present+
                  " "+ past + " " + pastpart);

          if (prefix.length()>0) {
            if (inseparablePrefixes.indexOf(prefix)>=0)
              {insep = true;}
            else {sep = true;}
          }

          baseInfinitive=infinitive;
          infinitive = prefix + infinitive;
          if (insep) {
            present = prefix + present;
            past = prefix + past;
            if (pastpart.startsWith("ge")) //remove the ge if prefix is insep
              pastpart = pastpart.substring(2);
          }
          pastpart = prefix + pastpart;
          if (authorFlag)
            trace("Using paradigm from Verb Lookup Table: " + infinitive + " " +
                  present + " " +
                  past + " " + pastpart);
          morphWord(infinitive, depth+1, ":literal", variants);
          morphWord(infinitive, depth+1, ":from-infinitive", variants);
          morphWord(present, depth+1, ":irr-pres", variants);
          morphWord(past, depth+1, ":irr-past", variants);
          morphWord(infinitive, depth+1, ":irr-subj1", variants); //ach 5dec00 from inf
          morphWord(past, depth+1, ":irr-subj2", variants);  // from past stem
          morphWord(pastpart, depth+1, ":literal", variants);
          // morphWord(pastpart, depth+1, ":base-adj", variants); // don't do these off verb 27apr01
          if (sep) { //for separable prefix case:
            // generate forms with prefix that were not generated above
            morphWord(prefix+present, depth+1, ":irr-pres", variants);
            morphWord(prefix+past, depth+1, ":irr-past", variants);
            morphWord(prefix+past, depth+1, ":irr-subj2", variants);  // from past stem
            morphWord(baseInfinitive, depth+1, ":irr-subj1", variants);
          }
          String[] result = new String[variants.size()];
          int i = 0;
          for (Iterator<String> iter = variants.iterator(); iter.hasNext();) {
            result[i++] = iter.next();
            
        }
          return result;
        }
        else return null;
    }

    /**
     * This is a locale-specific intialization.
     * Create the rules and exceptions HashTables for the sizes needed,
     * define a local variable for verb paradigms, and then call:
     *  initialize(exceptions, exceptionTable);
     *  initialize(paradigms, paradigmTable);
     */
    @Override
    protected void intialize() {
        if (rulesTable != null) {
            return;
        }

        // Set necessary LiteMorph class parameters:

        rulesTable = new Hashtable(100, (float)0.7);
        exceptions = new Hashtable(100, (float)0.7);

        // Define variables for use in rules:

//cc         defVar("$Consonant", "bcdfghjklmnpqrs&szlig;tvwxyz"); //include x for loan words
        defVar("$Consonant", "bcdfghjklmnpqrs\u00dftvwxyz"); //include x for loan words
        defVar("$Consonant-s", "bcdfghjklmnpqrtvwxyz"); //include x for loan words
//cc         defVar("$Umlaut", "&auml;&ouml;&uuml;&Auml;&Ouml;&Uuml;");
        defVar("$Umlaut", "\u00e4\u00f6\u00fc\u00c4\u00d6\u00dc");
        defVar("$EpandUmlaut", "aouAOU");
//cc         defVar("$Vowel", "aeiou&auml;&ouml;&uuml;");
        defVar("$Vowel", "aeiou\u00e4\u00f6\u00fc");
//cc         defVar("$Vowel+y", "aeiou&auml;&ouml;&uuml;y");
        defVar("$Vowel+y", "aeiou\u00e4\u00f6\u00fcy");
//cc         defVar("$Vowel+s", "aeiou&auml;&ouml;&uuml;s");
        defVar("$Vowel+s", "aeiou\u00e4\u00f6\u00fcs");
//cc         defVar("$Letter", "abcdefghijklmnopqrstuvwxyz&auml;&ouml;&uuml;");
        defVar("$Letter", "abcdefghijklmnopqrstuvwxyz\u00e4\u00f6\u00fc");
//cc         defVar("$AllLetter", "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSs&szlig;TtUuVvWwXxYyZz&Auml;&auml;&Ouml;&ouml;&Uuml;&uuml;");
        defVar("$AllLetter", "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSs\u00dfTtUuVvWwXxYyZz\u00c4\u00e4\u00d6\u00f6\u00dc\u00fc");

        // variables required to process capitalized input, e.g, 'Mann, Aepfel'
//cc         defVar("$Capital", "A&Auml;BCDEFGHIJKLMNO&Ouml;PQRSTU&Uuml;VWXYZ");
        defVar("$Capital", "A\u00c4BCDEFGHIJKLMNO\u00d6PQRSTU\u00dcVWXYZ");
//cc         defVar("$AllConsonant", "BbCcDdFGfgHhJjKkLlMmNnPpQqRrSs&szlig;TtVvWwXxYyZz");
        defVar("$AllConsonant", "BbCcDdFGfgHhJjKkLlMmNnPpQqRrSs\u00dfTtVvWwXxYyZz");
//cc         defVar("$AllVowel", "AaEeIiOoUu&Auml;&auml;&Ouml;&ouml;&Uuml;&uuml;");
        defVar("$AllVowel", "AaEeIiOoUu\u00c4\u00e4\u00d6\u00f6\u00dc\u00fc");

        // Define the exceptions:
        // Note that these exception table variables exist only during
        // initialization, after which the information is incorporated into
        // the appropriate hash tables, and similarly for the rule variables.

        // Include full verb paradigms for 'meinen' and 'sein'
        // here, because of overlap with paradigms for pronouns
        // 'mein' and 'sein'. ach 29nov00, waw 23feb05

        String[] exceptionTable = {

     "ab",
     "aber",
     "achtung",
     "allein",
     "aller alles allem allen alle",
     "als",
     "an am",
     "angewandten angewandter angewandtes angewandtem angewandte angewandtere" +
     " angewandterem angewandteren angewandteres angewandterer angewandtste" +
     " angewandtstem angewandtster angewandtsten angewandtstes",
     "antworten antworte antwortest antwortet geantwortet antwortete antwortetest" +
     " antwortetet antworteten", //otherwise will generate angetwortet, not prefix an- here
     "auch",
     "auf aufs",
//cc      "aus, au&szlig;er",
     "aus, au\u00dfer",
     "bald balde baldem balden balder baldes eher ehere eherem aheren eherer" +
     " eheres eheste ehestem ehesten ehester ehestes",
     "be",
     "bei beim",
     "belegen belegt belege belegst belegte belegtet belegtest belegten beleget" +
     " belegest beleg",
     "bewegen bewegt bewege bewegst bewegte bewegtet bewegtest bewegten beweget" +
     " bewegest beweg bewegtem bewegtes bewegtere bewegterem bewegteren" +
     " bewegterer bewegteres bewegtest bewegteste bewegtestem bewegtesten" +
     " bewegtester bewegtestes",
     "bis",
//cc      "buch buches buecher b&uuml;cher buechern b&uuml;chern",
     "buch buches buecher b\u00fccher buechern b\u00fcchern",
     "buche buchen",
     "bus busse busses bussen Bus Busse Busses Bussen",
     "da",
     "dahin",
     "damit",
     "dann",
//cc      "da&szlig;",
     "da\u00df",
     "dass",
     "de",
     "dein deiner deine deinem deines",
     "denn",
     "dennoch",
     "der dem den des die das dessen deren denen",
     "dieser dieses diesem diesen diese",
     "doch",
     "dort",
     "dorthin",
     "du",
     "durch durchs",
//cc      "d&uuml;rfen darfst darft d&uuml;rft durfte durftest durften durftet d&uuml;rfe d&uuml;rfest"+
     "d\u00fcrfen darfst darft d\u00fcrft durfte durftest durften durftet d\u00fcrfe d\u00fcrfest"+
//cc      " d&uuml;rfet d&uuml;rfte d&uuml;rftest d&uuml;rften d&uuml;rftet d&uuml;rfend gedurft",
     " d\u00fcrfet d\u00fcrfte d\u00fcrftest d\u00fcrften d\u00fcrftet d\u00fcrfend gedurft",
     "ebenso",
     "ein einer eine einem eines",
     "emp",
     "ent",
     "entgegen",
     "er",
     "es",
     "euer eueres euerem eueren euere",
     "fehl",
     "fern",
     "fest",
//cc      "f&uuml;r f&uuml;rs fuer fuers",
     "f\u00fcr f\u00fcrs fuer fuers",
     "ge",
     "gebiss gebisse gebisses gebissen",
     "gegen",
     "gegenueber",
//cc      "gegen&uuml;ber",
     "gegen\u00fcber",
     "gern gerne gernem gernen gerner gernes lieber liebere lieberem lieberen" +
     " lieberer lieberes liebste liebstem liebsten liebster liebstes",
     "gescheit gescheitem gescheiten gescheiter gescheites gescheitere" +
     " gescheiterem gescheiteren gescheiterer gescheiteres gescheiteste" +
     " gescheitestem gescheitesten gescheitester gescheitestes",
     "gesellschaft gesellschaften",
     "gesundheit",
     "gewinde gewinden gewindes",
//cc      "gross gro&szlig; grosse gro&szlig;e grossem gro&szlig;em grossen gro&szlig;en grosser gro&szlig;er grosses" +
     "gross gro\u00df grosse gro\u00dfe grossem gro\u00dfem grossen gro\u00dfen grosser gro\u00dfer grosses" +
//cc      " gro&szlig;es groesser gr&ouml;&szlig;er groessere gr&ouml;&szlig;ere groesserem gr&ouml;&szlig;erem groesseren" +
     " gro\u00dfes groesser gr\u00f6\u00dfer groessere gr\u00f6\u00dfere groesserem gr\u00f6\u00dferem groesseren" +
//cc      " gr&ouml;&szlig;eren groesserer gr&ouml;&szlig;erer groesseres gr&ouml;&szlig;eres groesste gr&ouml;&szlig;te groesstem" +
     " gr\u00f6\u00dferen groesserer gr\u00f6\u00dferer groesseres gr\u00f6\u00dferes groesste gr\u00f6\u00dfte groesstem" +
//cc      " gr&ouml;&szlig;tem groessten gr&ouml;&szlig;ten groesster gr&ouml;&szlig;ter groesstes gr&ouml;&szlig;tes",
     " gr\u00f6\u00dftem groessten gr\u00f6\u00dften groesster gr\u00f6\u00dfter groesstes gr\u00f6\u00dftes",
     "gut gute gutem guten guter gutes besser bessere besserem besseren besserer" +
     " besseres beste bestem besten bester bestes",
     "habe habst hat haben habt hatte hattest hatten hattet habest habet" +
//cc      " h&auml;tte h&auml;ttest h&auml;tten h&auml;ttet habend gehabt",
     " h\u00e4tte h\u00e4ttest h\u00e4tten h\u00e4ttet habend gehabt",
     "her",
     "herueber",
//cc      "her&uuml;ber",
     "her\u00fcber",
     "hervor",
     "hin",
     "hinab",
     "hinauf",
     "hinter",
     "hinweg",
//cc      "hoch hohe hohem hohen hoher hohes hoeher h&ouml;her hoehere h&ouml;here hoeherem" +
     "hoch hohe hohem hohen hoher hohes hoeher h\u00f6her hoehere h\u00f6here hoeherem" +
//cc      " h&ouml;herem hoeheren h&ouml;heren hoeherer h&ouml;herer hoeheres h&ouml;heres hoehste h&ouml;hste" +
     " h\u00f6herem hoeheren h\u00f6heren hoeherer h\u00f6herer hoeheres h\u00f6heres hoehste h\u00f6hste" +
//cc      " hoehstem h&ouml;hstem hoehsten h&ouml;hsten hoehster h&ouml;hster hoehstes h&ouml;hstes",
     " hoehstem h\u00f6hstem hoehsten h\u00f6hsten hoehster h\u00f6hster hoehstes h\u00f6hstes",
     "ich",
     "ihr ihrem ihren ihres ihre",
     "in ins im",
     "intern interne internem internen interner internes internere internerem" +
     " interneren internerer interneres internste internstem internsten" +
     " internster internstes",  // avoid 'internen' (false verb) generation 'internet'
     "ja",
     "je",
     "jeder jedes jedem jeden jede",
     "jedoch",
     "jener jenes jenem jenen jene",
     "jetzt",
     "kein keiner keines keinem keine",
     "mancher manches manchem manchen manche",
     "mein meiner meine meinem meines meinige meinigen", // added last two 23feb05
     "meinen mein meine meinst meint meinte meintest meintet meinten meinest" +
     " meinet gemeint", // waw added 24feb05 per meinen note above
     "mit",
     "miss",
//cc      "m&ouml;gen mag magst m&ouml;gt mochte mochtest mochten mochtet" +
     "m\u00f6gen mag magst m\u00f6gt mochte mochtest mochten mochtet" +
//cc      " m&ouml;ge m&ouml;gest m&ouml;get m&ouml;chte m&ouml;chtest m&ouml;chten m&ouml;chtet m&ouml;gend gemocht",
     " m\u00f6ge m\u00f6gest m\u00f6get m\u00f6chte m\u00f6chtest m\u00f6chten m\u00f6chtet m\u00f6gend gemocht",
     "nach",
     "nachdem",
     "nah nahe nahem nahen naher nahes naeher naehere naeherem naeheren naeherer" +
     " naeheres naechste naechstem naechsten naechster naechstes",
     "nein",
     "niemand",
     "noch",
     "nur",
     "ob",
     "obgleich",
     "obwohl",
     "oder",
     "rad",
     "russe",
     "re",
     "sieden siedet siedest sott sottst sottt sotten soette soettest" +
     " soettet soetten sotte sottest sottet gesotten gesiedet",
     "sein seiner seine seinem seines",
     "sein bin bist ist sind seid war warst wart waren sei seiest seist seien" +
//cc      " seiet w&auml;re w&auml;rest w&auml;rst w&auml;ren w&auml;ret w&auml;rt seiend gewesen",
     " seiet w\u00e4re w\u00e4rest w\u00e4rst w\u00e4ren w\u00e4ret w\u00e4rt seiend gewesen",
     "sie",
     "so",
     "solcher solches solchem solchen solche",
     "sollen soll sollst sollt sollte solltest sollten solltet solle sollest" +
     " sollet sollend gesollt",
     "sondern",
     "soviel",
     "spazieren",
     "statt",
     "staub",
//cc      "&uuml;ber",
     "\u00fcber",
     "ueber",
     "um ums",
     "umhin",
     "und",
     "uns",
     "unser unseres unserem unseren unsere",
     "unter",
     "ver",
     "verfuehren verfuehrt verfuehre verfuehrst verfuehrte verfuehrtest" +
     " verfuehrtet verfuehrten verfuehrest verfuehret" +
     " verfuehr", // regular verb takes precedence over subjunctive of 'verfahren'
     "viel viele vielem vielen vieler vieles mehr mehre mehrem mehren mehrer" +
     " mehres meist meiste meistem meisten meister meistes", // added meist 23feb05
     "voll",
     "von vom",
     "vor",
     "voraus",
     "vorbei",
     "vorhin",
//cc      "w&auml;hrend",
     "w\u00e4hrend",
     "waehrend",
     "wann",
     "warum",
     "was",
     "weg",
     "weiche weiches weicher weichem weichere weicherem weicheren weicherer" +
     " weicheres weichste weichstem weichsten weichster weichstes",
     "weil",
     "weiter",
     "welcher welches welchem welchen welche",
     "wenn",
     "werde wirst wirt wird werden werdet wurde wurdest wurden wurdet" +
//cc      " werdest w&uuml;rde w&uuml;rdest w&uuml;rden w&uuml;rdet werdend geworden",
     " werdest w\u00fcrde w\u00fcrdest w\u00fcrden w\u00fcrdet werdend geworden",
     "wie",
     "wieder",
     "wieviel",
     "wir",
     "wo",
     "wohin",
     "wollen will willst wollt wollte wolltest wollten wollte wolle" +
     " wollest wollet wollend gewollt",
     "zer",
     "zerrung zerrungen",
     "zu zur zum",
     "zurueck",
//cc      "zur&uuml;ck",
     "zur\u00fcck",
     "zusammen",
     "zuvor",
     "zwar",

        };

        initialize(exceptions, exceptionTable);

        // Define the German irregular verb paradigms:
        //      172 paradigms, 683 verb forms

        String[] paradigmTable = {

    "backen baeckt backte gebacken",
    "befehlen befiehlt befahl befohlen",
    "beginnen beginnt begann begonnen",
    "beissen beisst biss gebissen",
    "bergen birgt barg geborgen",
    "bersten birst barst geborsten",
    "bewegen bewegt bewog bewogen",
    "biegen biegt bog gebogen",
    "bieten bietet bot geboten",
    "binden bindet band gebunden",
    "bitten bittet bat gebeten",
    "blasen blaest blies geblasen",
    "bleiben bleibt blieb geblieben",
    "braten braet briet gebraten",
    "brechen bricht brach gebrochen",
    "brennen brennt brannte gebrannt",
    "bringen bringt brachte gebracht",
    "denken denkt dachte gedacht",
    "dreschen drischt drosch gedroschen",
    "dringen dringt drang gedrungen",
    "duerfen darf durfte gedurft",
    "empfehlen empfiehlt empfahl empfohlen",
    "essen isst ass gegessen",
    "fahren faehrt fuhr gefahren",
    "fallen faellt fiel gefallen",
    "fangen faengt fing gefangen",
    "fechten ficht focht gefochten",
    "finden findet fand gefunden",
    "flechten flicht flocht geflochten",
    "fliegen fliegt flog geflogen",
    "fliehen flieht floh geflohen",
    "fliessen fliesst floss geflossen",
    "fressen frisst frass gefressen",
    "frieren friert fror gefroren",
    "gebaeren gebiert gebar geboren",
    "geben gibt gab gegeben",
    "gedeihen gedeiht gedieh gediehen",
    "gehen geht ging gegangen",
    "gelingen gelingt gelang gelungen",
    "gelten gilt galt gegolten",
    "genesen genest genas genesen",
    "geniessen geniesst genoss genossen",
    "geschehen geschieht geschah geschehen",
    "gewinnen gewinnt gewann gewonnen",
    "giessen giesst goss gegossen",
    "gleichen gleicht glich geglichen",
    "gleiten gleitet glitt geglitten",
    "graben graebt grub gegraben",
    "greifen greift griff gegriffen",
    "haengen haengt hing gehangen",
    "halten haelt hielt gehalten",
    "heben hebt hob gehoben",
    "heissen heisst hiess geheissen",
    "helfen hilft half geholfen",
    "kennen kennt kannte gekannt",
    "klimmen klimmt klomm geklommen",
    "klingen klingt klang geklungen",
    "kneifen kneift kniff gekniffen",
    "koennen kann konnte gekonnt",
    "kommen kommt kam gekommen",
    "kriechen kriecht kroch gekrochen",
    "laden ladet lud geladen", // exceptions table won't work with seps/inseps on this root ach 27feb01
    "lassen laesst liess gelassen",
    "laufen laeuft lief gelaufen",
    "leiden leidet litt gelitten",
    "leihen leiht lieh geliehen",
    "lesen liest las gelesen",
    "liegen liegt lag gelegen",
    "luegen luegt log gelogen",
    "mahlen mahlt mahlte gemahlen",
    "meiden meidet mied gemieden",
    "melken milkt molk gemolken",
    "messen misst mass gemessen",
    "muessen muss musste gemusst",
    "nehmen nimmt nahm genommen",
    "nennen ninnt nannte genannt",
    "pfeifen pfeift pfiff gepfiffen",
    "preisen preist pries gepriesen",
    "quellen quillt quoll gequollen",
    "raten raet riet geraten",
    "reiben reibt rieb gerieben",
    "reissen reisst riss gerissen",
    "reiten reitet ritt geritten",
    "rennen rennt rannte gerannt",
    "riechen riecht roch gerochen",
    "ringen ringt rang gerungen",
    "rinnen rinnt rann geronnen",
    "rufen ruft rief gerufen",
    "saufen saeuft soff gesoffen",
    "saugen saugt sog gesogen",
    "schaffen schafft schuf geschaffen",
    "scheiden scheidet schied geschieden",
    "scheinen scheint schien geschienen",
    "schelten schilt schalt gescholten",
    "schieben schiebt schob geschoben",
    "schinden schindet schund geschunden",
    "schlafen schlaeft schlief geschlafen",
    "schlagen schlaegt schlug geschlagen",
    "schleichen schleicht schlich geschlichen",
    "schleifen schleifft schliff geschliffen",
    "schliessen schliesst schloss geschlossen",
    "schlingen schlingt schlang geschlungen",
    "schmeissen schmeisst schmiss geschmissen",
    "schmelzen schmilzt schmolz geschmolzen",
    "schneiden schneidet schnitt geschnitten",
    "schrecken schrickt schrack geschrocken",
    "schreiben schreibt schrieb geschrieben",
    "schreien schreit schrie geschrieen",
    "schreiten schreitet schritt geschritten",
    "schweigen schweigt schwieg geschwiegen",
    "schwellen schwillt schwoll geschwollen",
    "schwimmen schwimmt schwamm geschwommen",
    "schwinden schwindet schwand geschwunden",
    "schwingen schwingt schwang geschwungen",
    "schwoeren schwoert schwur geschworen",
    "sehen sieht sah gesehen",
    "sein ist war gewesen",
    "senden sendet sandte gesandt",
    "singen singt sang gesungen",
    "sinken sinkt sank gesunken",
    "sinnen sinnt sann gesonnen",
    "sitzen sitzt sass gesessen",
    "sollen soll sollte gesollt",
    "speien speit spie gespeit",
    "spinnen spinnt spann gesponnen",
    "sprechen spricht sprach gesprochen",
    "spriessen spriesst spross gesprossen",
    "springen springt sprang gesprungen",
    "stechen sticht stach gestochen",
    "stehen steht stand gestanden",
    "stehlen stiehlt stahl gestohlen",
    "steigen steigt stieg gestiegen",
    "sterben stirbt starb gestorben",
    "stinken stinkt stank gestunken",
    "stossen stoesst stiess gestossen",
    "streichen streicht strich gestrichen",
    "streiten streitet stritt gestritten",
    "tragen traegt trug getragen",
    "treffen trifft traf getroffen",
    "treiben treibt trieb getrieben",
    "treten tritt trat getreten",
    "trinken trinkt trank getrunken",
    "truegen truegt trog getrogen",
    "tun tut tat getan",
    "verderben verdirbt verdarb verdorben",
    "verdriessen verdriesst verdross verdrossen",
    "vergessen vergisst vergass vergessen",
    "verlieren verliert verlor verloren",
    "wachsen waechst wuchs gewachsen",
    "waegen waegt wog gewogen",
    "waschen waescht wusch gewaschen",
    "weben webt wob gewoben",
    "weichen weicht wich gewichen",
    "weisen weist wies gewiesen",
    "wenden wendet wandte gewandt",
    "werben wirbt warb geworben",
    "werden wirt wurde geworden",
    "werfen wirft warf geworfen",
    "wiegen wiegt wog gewogen",
    "winden windet wand gewunden",
    "wissen weisst wusste gewusst",
    "zeihen zeiht zieh gesziehen",
    "ziehen zieht zog gezogen",
    "zwingen zwingt zwang gezwungen",

        };

        initialize(paradigms, paradigmTable);


  // Define the rules:


  String[] rootRules = {//":unnamed",  // For unknown words (root-rules)

    // If the word begins with a capital, assume it is a noun and process as such.  16mar01
    "# $Capital -> (:cap-nouns)_",

    // Note: The inclusion of .$Vowel in every rule is to avoid spurious cases where the
    // word 'stem' would be a consonant cluster, i.e., the 'word' must be syllabic.

    //first check for pastpart-based adjectives, so don't pick up the verb form
    ".$Vowel $Consonant e n + e n|m|r|s  -> _,(:all-adj)_",    // (angeschlossenen)

    // Check for irregular verbs (strong or mixed) in Verb Lookup Table
    ".$Vowel -> TRY(!irrVerb)_,", // lookup form as is - (geben, gibt, gab, gegeben)
//cc     ".$Vowel $Umlaut -> TRY(:change-ablaut)_,", // in case form has &auml;,&ouml;,&uuml;, change to ae,oe,ue for Lookup Table
    ".$Vowel $Umlaut -> TRY(:change-ablaut)_,", // in case form has \u00e4,\u00f6,\u00fc, change to ae,oe,ue for Lookup Table

    // Test whether this is an imperative (stem) form of an irregular
     ".$Vowel  -> _,TRY(!irrVerb)en",            // (schweig)

    // Failing this, strip off inflectional endings and see whether resulting forms are in Verb Lookup Table
    // Test for Present/Past/Subjunctive forms

    // Test inflections on Pres Tense -sst, -(e)st, -(e)t  covers: Present Tense 1/2/3 sg, 2 pl.
    ".$Vowel s s + t -> TRY(!irrVerb)t",  // Present: 2/3sg with double-s ending (heisst)
    ".$Vowel s s + t -> TRY(!irrVerb)_",  // Past: 2pl with double-s ending (sasst)

    ".$Vowel + e ?s t -> TRY(!irrVerb)et", // Present: 2/3sg (schreitest,windet)
    ".$Vowel + e ?s t -> TRY(!irrVerb)en", // Subj1: 2/3sg,2pl (gehest,gehet,brennest,brennet)
    ".$Vowel + e ?s t -> TRY(!irrVerb)_",  // Subj2: 2/3sg,2pl No Umlaut (rietet, schnittet)

    ".$Vowel + ?s t -> TRY(!irrVerb)t",    // Present: 2/3sg, (gibst,gibt,ruft,rufst)
    ".$Vowel + t -> TRY(!irrVerb)_",       // Past: 2pl (sangt,quollt)
    ".$Vowel + e n -> TRY(!irrVerb)_",     // Past/Subj2 (no umlaut): 1/3pl (hiess,sass)

    ".$Vowel + e ?n -> TRY(!irrVerb)en",   // Subj1: 1sg,1/3pl (gehe,gehen,brenne,brennen)
    ".$Vowel + e ?n|t -> TRY(!irrVerb)_",  // Subj2: (no umlaut) (riebe,rieben,riebet)

    // Check Mixed verbs Past Tense in Verb Lookup Table
    // for Mixed verbs (as all regular verbs), Subj1 is same as Past
    "+ t e s t -> TRY(!irrVerb)te",     // past/subj2 2sg (branntest)
    "+ t e ?n|t -> TRY(!irrVerb)te",    // past/subj2 1,3sg/1/2/3pl (brannte,brannten,branntet)

    //Test Subj2 Strong Verbs   Remove umlaut, if applicable, then strip Subj2 endings

  // **** omit undoing umlauts on input forms, assume input -not- subjunctive 10jul01 ***
  //  "a|o|u < e > +$Consonant e # -> TRY(:strip-irr-subj2)<>", // 1sg (gaebe -> gabe)
//cc   //  "< &auml; > +$Consonant e # -> TRY(:strip-irr-subj2)<a>",      // 1sg (g&auml;be -> g&auml;be)
  //  "< \u00e4 > +$Consonant e # -> TRY(:strip-irr-subj2)<a>",      // 1sg (g\u00e4be -> g\u00e4be)
//cc   //  "< &ouml; > +$Consonant e # -> TRY(:strip-irr-subj2)<o>",      // 1sg (l&ouml;ge -> loge)
  //  "< \u00f6 > +$Consonant e # -> TRY(:strip-irr-subj2)<o>",      // 1sg (l\u00f6ge -> loge)
//cc   //  "< &uuml; > +$Consonant e # -> TRY(:strip-irr-subj2)<u>",      // 1sg (l&uuml;de -> lude)
  //  "< \u00fc > +$Consonant e # -> TRY(:strip-irr-subj2)<u>",      // 1sg (l\u00fcde -> lude)

 //   "a|o|u < e > +$Consonant e n|t # -> TRY(:strip-irr-subj2)<>", // 2/3pl (gaeben/et -> gaben/et)
//cc  //   "< &auml; > +$Consonant e n|t # -> TRY(:strip-irr-subj2)<a>", // 2pl/3pl (g&auml;ben/et -> gaben/et)
 //   "< \u00e4 > +$Consonant e n|t # -> TRY(:strip-irr-subj2)<a>", // 2pl/3pl (g\u00e4ben/et -> gaben/et)
//cc  //   "< &ouml; > +$Consonant e n|t # -> TRY(:strip-irr-subj2)<o>", // 2pl/3pl (l&ouml;gen/et -> logen/et)
 //   "< \u00f6 > +$Consonant e n|t # -> TRY(:strip-irr-subj2)<o>", // 2pl/3pl (l\u00f6gen/et -> logen/et)
//cc  //   "< &uuml; > +$Consonant e n|t # -> TRY(:strip-irr-subj2)<u>", // 2pl/3pl (l&uuml;den/et -> luden/et)
 //   "< \u00fc > +$Consonant e n|t # -> TRY(:strip-irr-subj2)<u>", // 2pl/3pl (l\u00fcden/et -> luden/et)

 //   "a|o|u < e > +$Consonant e s t # -> TRY(:strip-irr-subj2)<>",  // (gaebest -> gabest)
//cc  //   "< &auml; > +$Consonant e s t # -> TRY(:strip-irr-subj2)<a>",  // (g&auml;best -> gabest)
 //   "< \u00e4 > +$Consonant e s t # -> TRY(:strip-irr-subj2)<a>",  // (g\u00e4best -> gabest)
//cc  //   "< &ouml; > +$Consonant e s t # -> TRY(:strip-irr-subj2)<o>",  // (l&ouml;gest -> logest)
 //   "< \u00f6 > +$Consonant e s t # -> TRY(:strip-irr-subj2)<o>",  // (l\u00f6gest -> logest)
//cc  //   "< &uuml; > +$Consonant e s t # -> TRY(:strip-irr-subj2)<u>",  // (l&uuml;dest -> ludest)
 //   "< \u00fc > +$Consonant e s t # -> TRY(:strip-irr-subj2)<u>",  // (l\u00fcdest -> ludest)


    // Test for Present Participle - generate only adjectival forms
    // Nouns are generated by enumerated endings, e.g., en,s,e
    // Note: mixed verbs 'senden' 'wenden' will match on !irrVerbs, before the current rule

    "$Consonant e n d # -> _,en,s,e,(:all-adj)_",  // (dutzend,dutzende,elends,ankommend)
    "$Consonant e n d + e ?n|s -> _,e,s,es,en,(:all-adj)_", // (jahresendes)
    "$Consonant e n d + e r -> ers,(:all-adj)_,",           // (kalender,kalenders)
    "$Consonant e n d + e m -> (:all-adj)_",          // (fallendem,betonendem)
    "$Consonant e n d + s t -> (:all-adj)_",          // (dringendst)
    "$Consonant e n d + s t e ?n|m|r|s -> (:all-adj)_", // (tausendste,brennensten)
    "$Consonant e n d + e r e ?n|m|r|s -> (:all-adj)_", // (gleitendere,troztenderes)

    // Test for Past Participle - Mixed verbs - ONLY generate adjectival forms
     "# g e .$Letter e n + e r e ?m|n|r|s -> (:all-adj)_",          // comparative
     "# g e .$Letter + e r e ?m|n|r|s -> (:all-adj)_",              // comparative

    "# g e .$Letter e n + s t ?e ?m|n|r|s -> (:all-adj)_",          // superlative
    "# g e .$Letter t + ?e s t ?e ?m|n|r|s -> (:all-adj)_",         // superlative

    "# g e .$Letter t # -> TRY(!regVerb)_", // (gesagt,gekauft)
    "# g e .$Letter + e ?m|r|s -> (:all-adj)_", // (gebranntes,gebrannter,gebrannte)
    "# g e .$Letter + e n -> (:all-reg-verbs)_,(:all-adj)_", // (gebaren,gelangen)

    // Test for Past Participle - Strong verbs - generate only adjectival forms
    "# g e .$Letter e n + e ?m|n|r|s -> (:all-adj)_", // (gebundener, gebundene)

  // Note: above past participle rules, don't handle pastparts w/inseparable prefixes, need to add this!  ach 10mar01

    // At this point, the input did not match any form in Verb Lookup Table, nor was a participial adjectival form
    // Try other patterns, testing grammatically unambigous patterns before ambiguous ones
    // First, look for unamiguous Noun patterns

    // -Ung feminine nouns
    "u n g + ?e|s -> _,e,en,s",         // (bildung,bildunge)
    "u n g + e n -> _,e,en,s",          // (bildungen,bildungs)

    // -Ling forms
    "l i n g + ?e|s -> _,e,s,en",       // (keimling,keimlinge)
    "l i n g + e n -> _,e,s,en",        // (lehrlings,lehrlingen)

     // -Keit/Heit nouns
    "h|k e i t + e n -> _,en",     // (gesundheiten)
    "h|k e i t # -> _,en",         // (freiheit)

     // -Schaft nouns
    "s c h a f t + e n -> _,en,er,(:all-reg-verbs)en",// (wissenschaften,erwirtschaften)
    "s c h a f t + e r -> _,en,er",// (wissenschafter)
    "s c h a f t # -> _,en,er",    // (wissenschaft)

    // -Bar nouns - unambiguous noun, ambiguous -bar cases (N/A) ordered later
    "b a r + n|s -> _,en,n,s",  // (nachbar,nachbarn,nachbars)

    // -Taet nouns
//cc     "+ t a e t ?e -> taete,taeten,t&auml;t,t&auml;te,t&auml;ten",         // (universitaet)
    "+ t a e t ?e -> taete,taeten,t\u00e4t,t\u00e4te,t\u00e4ten",         // (universitaet)
//cc     "+ t &auml; t ?e -> t&auml;te,t&auml;ten,taet,taete,taeten",          // (universit&auml;t)
    "+ t \u00e4 t ?e -> t\u00e4te,t\u00e4ten,taet,taete,taeten",          // (universit\u00e4t)
//cc     "+ t a e t e n -> taete,taet,t&auml;ten,t&auml;te,t&auml;t",          // (universitaeten)
    "+ t a e t e n -> taete,taet,t\u00e4ten,t\u00e4te,t\u00e4t",          // (universitaeten)
//cc     "+ t &auml; t e n -> t&auml;te,t&auml;t,taeten,taete,taet",           // (universit&auml;te)
    "+ t \u00e4 t e n -> t\u00e4te,t\u00e4t,taeten,taete,taet",           // (universit\u00e4te)

    // -Nis nouns
    "n i s + s e ?s|n -> _,se,ses,sen", // (erlebnisse)
    "n i s # -> _,se,ses,sen,e,es,en",  // (gleichnis,gleichnisse,anise,anises)

    // -Ka ending nouns
//cc     "k + a -> as,um,en",                // (s&uuml;damerikas,attiken,narkotikum)
    "k + a -> as,um,en",                // (s\u00fcdamerikas,attiken,narkotikum)

    // -Ma forms
    "m + a -> a,as,en,(:base-adj)_",         // (drama,dramen,prima)

    // final -A nouns - many are borrowings (names), but not all.
    ".$Vowel + a # ->  a,as,en",             // (delta,deltas,kala,kalen)

    // -Ion ending nouns
    "i o n + e n -> _,en,s",                  // (functionen)
    "i o n + ?s -> _,en,s",                   // (deduktion,aktions)

    // Vowel + p ending nouns
    "$Letter y p + ?i ?e ?s|n -> _,en,e,s,ien", // (typen)

    // Vowel + z ending nouns
    "$Vowel z # -> _,e,en,es,ien", // (geizes,notizen,indizien)

    // Vowel + y ending nouns - probably all borrowings
    ".$Vowel y # -> _,en,e,s",           // (airway,airways)

    // erin/in ending nouns

    // -In/Innen forms
    "- g e r i n n e n # -> (:all-reg-verbs)_", // (gerinnen)
    "$Consonant i n # -> _,nen",         // (asiatin)
    "$Consonant i n + n e n -> _,nen",   // (asiatinnen)
    "e r i n # -> _,nen",                // (afikanerin)
    "e r i n + n e n -> _,nen",          // (afrikanerinnen)

    // final -w nouns - probably all borrowings
    ".$Vowel w # -> _,s",                // (show,shows)

    // -Eur nouns
    "e u r # -> _,en,e,s,in,innen",      // (amateur,amatuers,amateuren)

    // -K ending nouns
    "$Vowel k + ?s -> _,e,en,s,es",      // (plastik,plastiks)

    // -Um forms - seem to be all nouns
    "a u m + ?s -> _,s,(:aum-forms)_",   // (traum,traeumer)
    "i + u m ?s -> um,ums,en",           // (studium,studiums,imperien)
    "$Consonant + u m ?s -> en,um,ums,umes,umen", // (spektrum,spektrums)

    // -Uf forms - seem to be all nouns
    "$Consonant u f # -> _,e,en,s,es",   // (huf,hufen,notrul,notrufs)

    // Unambigous Adjective patterns

    // -Voll ending adjectives
    // con't apply umlaut to comp/super of these forms
    "v o l l # -> (:all-adj)_", // (freudevoll) handvoll - a noun!
    "v o l l + e ?n|m|r|s -> (:all-adj)_",     // (liebevollem,kunstvoller,liebevollen)
    "v o l l + s t e ?n|m|r|s -> (:all-adj)_", // (kunstvollstem,bedeutungsvollster)
    "v o l l + e r e ?n|m|r|s -> (:all-adj)_", // (sinnvoller,liebevollere,geistvolleren)

    // -Sam ending adjectives
    // don't umlaut the comp/superlative for these forms
    "s a m # -> (:all-adj)_",  // (langsam,wachsam)
    "s a m + e ?n|m|r|s -> (:all-adj)_",   // (langsame)
    "s a m + e r e ?n|m|r|s -> (:all-adj)_", // (kleidsamere)
    "s a m + s t e ?n|m|r|s -> (:all-adj)_", // (wachsamster,kleidsamstem)

    // non-s -Am forms  Note: -kam verbs will be matched earlier w/irrVerbs
    "a m # -> _,s,en,(:base-adj)_",                  // (monogam,gram)

    // -Bar endings adjectives (also nouns, verbs, where indicated)
    "b a r # -> _,n,s,en,(:all-adj)_",  // nouns end in -n -s (dankbar)
    "b a r + e ?n -> (:all-adj)_,(:all-reg-verbs)_", // (vereinbaren,nutzbare)
    "b a r + e m|r|s -> (:all-adj)_",  // (brennbarem,essbarer)
    "b a r + s t e ?n|m|r|s -> (:all-adj)_",         // (kostbarstem,nutzbarstes)
    "b a r + e r e ?n|m|r|s -> (:all-adj)_",         // (unsagbarere,brauchbareren)

     // Exception - 2sg verb form
    "b a r + s t -> (:all-reg-verbs)st",             // (barst,vereinbarst)

    // -Ar endings
    "a r # -> _,e,en,s,(:all-adj)_",              // (rar,rarerem,zar,zaren,notars)

    // -Ad endings   appear to be only nouns, no umlaut plurals 16mar01
    "a d # -> _,e,en,s,es,(:add-umlaut)er,(:add-umlaut)ern", // (pfad,pfade,sitzbad,sitzbades)

    // -Ld endings
    "l d # -> _,e,en,s,es,er,(:add-umlaut)e,(:add-umlaut)er,(:add-umlaut)ern,(:all-adj)_",
    // (mild,mildestem,bild,bildes,wald,waelder,schuld,schulden)

    // -Lich endings adjectives
    // assume no umlauts apply in comp/super on these forms
    "l i c h # -> (:all-adj)_", // (neulich,ploetzlich)
    "l i c h + e ?n|m|r|s -> (:all-adj)_",         // (fraglicher,deutliches)
    "l i c h + e r e ?n|m|r|s -> (:all-adj)_",     // (aehnlicherer,fraglicheres)
    "l i c h + s t ?e ?n|m|r|s -> (:all-adj)_",    // (bequemster,fraglichsten)


    // Ambiguous Noun/Adj cases:

    // -Fach forms
    // noun forms are enumerated below on rhs of rules
    "f a c h + ?s -> _,s,(:add-umlaut)er,(:add-umlaut)ern,(:all-adj)_",
    "f a c h + e ?s -> _,(:all-adj)_",
    "f a c h + e n -> _,(:all-adj)_,(:all-reg-verbs)_",
    "f a c h + e m|r -> _,(:all-adj)_",
    "f a c h + e r e ?m|n|r|s -> _,(:all-adj)_",
    "f a c h + s t e ?m|n|r|s -> _,(:all-adj)_",

    // -Haft forms
    // noun forms are enumerated below on rhs of rules
    // ordered later than -schaft rules
    "h a f t + ?e ?s -> _,e,en,es,(:all-adj)_",  // (beugehaft,traumhaft)
    "h a f t + e n -> _,e,en,es,(:base-adj)_",
    "h a f t + e m|r -> _,(:base-adj)_",
    "h a f t + e r e ?m|n|r|s -> _,(:all-adj)_",
    "h a f t + s t e ?m|n|r|s -> _,(:all-adj)_",

    // -Mal forms  Nouns are only -mal, -mals, -malen,
    "m a l + ?s ->  _,e,s,en", // (zweimal,dezimale)
    "m a l + ?e ?m|n|r -> _,(:all-adj)_", // (normaler)
    "m a l + e r e ?m|n|r|s -> _,(:all-adj)_", // (normalerem)
    "m a l + s t e ?m|n|r -> _,(:all-adj)_", // (normalsten)

    // -Vowel + l forms,
    // Noun endings are enumerated in rhs of rule
    "e l l # -> _,s,n,en,(:all-adj)_",
    "e l + ?s -> _,s,n,en,(:do-umlaut)_,(:rem-umlaut)s,(:do-umlaut)n,(:all-adj)_",  // (segel,segeln,sensibel,aepfel,apfel,aktuell)
    "a l # -> _,e,s,en,es,(:all-adj)_", // (kanal,saisonal)
    "o l # -> _,e,s,en", // (idol,idols,idolen,sybmol,symbolen)
     "i l # -> _,e,s,en,es,(:all-adj)_", // (senilstem,senil,stil,stile,reptilen)

    // -Ie forms, only nouns?
    "$Consonant i e # -> _,m,n,r,s",  // (bibliographie,bibliographien)

    // -Us forms
    "$Consonant + u s -> en,us,use,uses,(:base-adj)us", // (abikus,konfusem,ritus,riten)

    // -Vowel+t forms
    "$Consonant u t # -> _,e,en,s,es",               // (tribut,tribute)
    "$Consonant a t # -> _,e,en,s,es,(:base-adj)_",  // (delikat,akrobats)
    "$Consonant i t # -> _,e,en,s,es,(:base-adj)_",  // (explizit,appetits,favoriten)

     // -Los forms
     // -these forms don't umlaut in comp/superlative
     "l o s # -> (:all-adj)_", // (machtlos)
     "l o s + e s t e ?m|n|r|s -> (:all-adj)_", // (machtloseste)
     "l o s + e r e ?m|n|r|s -> (:all-adj)_", // (machtloseste)
     "l o s + e n -> _,en,(:base-adj)_,(:all-reg-verbs)_", // (verlose, sinnlose, Zellulosen)

    // -V forms - nouns are enumerated on rhs of rule
    // don't apply umlaut to comp/super with these forms
    "$Vowel v + ?s -> _,en,es,s,(:all-adj)_", // (motiv,motiven,suggestiv,archivs)
    "$Vowel v + e ?n|m|r|s  -> _,en,es,s,(:all-adj)_", // (leitmotive,primitivem)
    "$Vowel v + s t e ?n|m|r|s  -> (:all-adj)_",       // (relativster,negativste)
    "$Vowel v + e r e ?n|m|r|s  -> (:all-adj)_",       // (relativerer,objectiveres)

    // Note: Any rule containing a + element is automatically anchored on the right
    // (e.g., e i n + s t -> e); a # at the end is optional in that case. WAW

    // -Ein forms  -einen treated with verbs
    "e i n # -> e,s,en,es,(:all-adj)_",                 // (wein,ungemain)
    "e i n + e ?m|r|s -> e,s,en,es,(:all-adj)_",        // (beine,oesteinen)
    "e i n + s t e ?n|m|r|s -> (:all-adj)_",            // (kleinsten,ungemeinste)
    "e i n + e r e -> (:all-adj)_,(:all-reg-verbs)ere", // (versteinere)
    "e i n + e r e ?n|m|r|s # -> (:all-adj)_",          // (feinere,unreineres)

    // -Eur forms
    "e u r + e ?n -> _,e,en,(:all-adj)_",             // (amateure,amateuren)
    "e u r + e r -> _,er,ern,(:all-adj)_",            // (abendteurer,abendteurern)
    "e u r + e m|s -> _,(:all-adj)_",                 // (teure,teurem)
    // "e u r + s t e ?n -> _,(:all-adj)_",           // (no forms found)
    // "e u r + s t e m|r|s -> _,(:all-adj)_",        // (no forms found)
    "e u r + e r e m|n|r|s -> _,(:all-adj)_",         // (teurere,teurerem)

    // -Ur forms
    "$Consonant u r # -> _,en,(:all-adj)_",      // ordered after -eur patterns (tortur,torturen)
    "$Consonant u r + e n -> _,en,(:all-adj)_",  // (staturen,statur,puren)

    // -Ine forms   -ines forms may all be borrowings
    "$Consonant i n + e -> e,es,en,(:base-adj)_", // (termine,terminen,alpiner)

    // -X forms
    // Nouns are enumerated in rhs of rule, don't add umlaut to comp/superlative forms
    ".$Vowel x # -> _,e,es,en,(:all-adj)_", // (komplex,komplexen,paradoxester)

    // -C forms
    ".$Vowel c # -> _,(:base-adj)_",                 // (metallic,metallicem)

    // -Eis forms
    "e i  + s  -> _,s,e,en,se,sen,ser,ses,(:all-adj)s",  // (preis,preise,weisesten,weiserem)
    // -Ei forms  no-umlauted adjectival forms 26mar01
    "e i # -> _,e,en,(:all-adj)_", // (brei,breie,faseleien,freiere)

    // -One forms
    "$Consonant o n + e -> _,e,en,s,(:all-adj)e,(:all-reg-verbs)e", // (betonen,synchrones,amazone,flakons)
    // -Eme/Ese  nouns are generated by enumerated endings, e.g., n,s
    ".$Vowel $Consonant e m|s e # -> _,n,s,(:base-adj)_", // (randproblem,randproblemen)

    // Next test for ambiguous Noun/Verb patterns:

    // -Ern/Eln  nouns are generated by enumerated endings
    "e r|l + n -> _,n,s,(:noun-sg)_,(:convert-umlaut)_,(:convert-umlaut)n,(:all-reg-verbs)n",  // (stimmern,stimmer,wickeln,artikel,artikeln)

    // -Og     nouns are generated by enumerated endings
    "$Consonant e|o g # -> _,e,en,s,es",  // (luftweg,luftwegen,dialoge,dialoges)
    "$Consonant a g # -> _,(:do-umlaut)e,(:do-umlaut)en,s,es,(:all-reg-verbs)_", // (antrag,vermag)
    // Test for ambiguous N/Adj/V patterns:

    // -Ig nouns are generated by enumerated endings
    "e i g # -> e,en,s,(:all-adj)_,(:all-reg-verbs)_", // (schneeig,zweig,schweig)

    // don't add umlaut to comparative/superlative forms
    "$Consonant i g # -> e,s,(:all-adj)_",  // (koenig,koenige)
    "$Consonant i g + e ?r|s -> (:noun-pl)_,(:all-adj)_", // (Peiniger)
    "$Consonant i g + e n -> (:all-adj)_,(:all-reg-verbs)_",  // (baldigen,vereinigen)
    "$Consonant i g + e m -> (:all-adj)_",  // (lustigem,massigem)
    "$Consonant i g + s t -> (:all-reg-verbs)_",         // (vereinigst)
    "$Consonant i g + s t e ?n|m|r|s -> (:all-adj)_", // (billigste,billigstem)
    "$Consonant i g + e r e ?n|m|r|s -> (:all-adj)_", // (findigere,giftigeres,hitzigerer)

    // -Ee  perhaps only nouns
    "$Consonant e e # -> _,n,s",                 // (idee,ideen,tournee,tourneen)

    // -Oo
    "o o # -> _,e,en,es,s",         // (Waterloo)

    // -Er  ordered after, e.g., i g + e r e r
    "$Consonant + e r ?e -> (:all-adj-with-umlaut)_,(:noun-sg)er,(:noun-pl)er",  // (blaetter,wetter)
    "$Consonant + e r e m|n|r|s -> (:all-adj-with-umlaut)_",  //

    "$Consonant + ?e s t ?e -> (:all-adj-with-umlaut)_",  // (waermst)
    "$Consonant + ?e s t e ?m|n|r|s -> (:all-adj-with-umlaut)_",  // (waermster)

    // -E
    "e # -> (:all-forms)_", // (Abfolge,zwoelfte)

    // -T
    ".$Vowel t # -> (:all-adj)_,(:all-reg-verbs)_,(:noun-pl)_", // (adressat,anblickt,bekannt)

    // -Z nouns are generated by enumerated endings
    ".$Vowel $Consonant z # -> _e,en,es,(:noun-pl)e,(:all-reg-verbs)_,(:all-adj)_",  // (allianz,allianzen,kurz,verschmolz,tanz,taenze)


    // Test for more ambiguous N/Adj patterns:

    // Os

    ".$Vowel $Consonant o # -> _,s",         // (kino,auto)
    ".$Vowel o + s -> _,s,(:all-adj)s",  // (kinos,furios,rigoros)

    ".$Vowel + e m -> (:all-adj-with-umlaut)_",                  // (sortiertem)
    ".$Vowel + e s -> _,e,(:noun-pl)_,(:all-adj-with-umlaut)_",  // (etwaiges,warmes)

    // Otherwise, use the default rule, and generate all forms
    ".$Vowel -> (:all-forms)_", // (defaultRule)

        };


  // Named Rules

  String[] genLiteral = {//":literal",
            //generate exactly this form --  used for the infinitive and pastpart

    ".$Vowel -> _,(:convert-umlaut)_",  // (kommen, gekommen)

        };

  String[] IrrPres = {//":irr-pres",
            // generates irr-pres forms for 2/3rd sing pres

    ".$Vowel s s t # -> _",             // 2/3sg are same with final -ss (schliesst)
    ".$Vowel t|d + e t # -> et,est",    // (findet,gleitet)
    ".$Vowel + t -> t,st",              // (kommt,blaest,schmilzt)
//cc     ".$Vowel -> _,t",                   // (wei&szlig;,wei&szlig;t)
    ".$Vowel -> _,t",                   // (wei\u00df,wei\u00dft)

        };

  String[] FromInfinitive = {//":from-infinitive",
            // generates irr-pres forms for 2pl from infinitive

     ".$Vowel d|t + e n # -> en,et",    // (finden,findet)
     ".$Vowel + e n # -> en,t",         // (sehen,seht)

        };


  String[] IrrPast = {//":irr-past",
            //generate forms from the past 3sg

    "+ t e # -> te,test,tet,ten",       // Mixed verbs (backte,machte)
    ".$Vowel s s # -> _,t,en",          // -ss, 2s/3s same (ass,asst,assen)
    ".$Vowel t|d # -> _,est,et,en",     // (tat,tatest,tatet)
    ".$Vowel # -> _,st,t,en",           // (kam)

        };


  String[] IrrSubj1 = {//":irr-subj1",
            //generate subjunctive 1 forms from infinitive

    ".$Vowel *$Consonant + e n # -> e,est,et,en",       // (geben -> gebe gebest gebet geben)
    ".$Vowel *$Consonant e l|r + n # -> e,est,et,en",   // (segeln -> segele segelest segelet segelen)
                                   // (rudern -> rudere ruderest ruderet ruderen)
    "$Consonant u + n # -> e,est,et,en",                 // (tun -> tue tuest tuet tuen)

        };

  String[] IrrSubj2 = {//":irr-subj2",
            // generate subjunctive 2 forms from past tense stem, add umlaut to stem
            // needs the '+' anchor to match correctly

    "a|o|u .$Consonant e # -> (:do-ablaut)_,(:do-ablaut)st,(:do-ablaut)t,(:do-ablaut)n", // (backte -> baeckte)
    "a|o|u .$Consonant -> (:do-ablaut)e,(:do-ablaut)est,(:do-ablaut)et,(:do-ablaut)en",  // (gab -> gaebe gaebest gaeben gaebet)
// tbd check the following, waw 23feb05
// The following was strangely commented out by being at the end of the previous line
// (after a lot of tabs) // doesn't look like the previous two, so still omitting it
//     "i ?e +$Consonant # -> e,est,et,en",   // (schliffe schliffest schliffen schliffet) (riefe riefest riefen riefet)

        };

  String[] testForIrrSubj2 = {//":strip-irr-subj2",   // ach 12dec00
            // This method assumes any umlaut on the stem has been removed, e.g. gaebe -> gabe
            // strips the subj2 endings off, then checks if stripped stem = past stem in Verb Lookup Table

    ".$Vowel $Consonant + e -> TRY(!irrVerb)_",         // (gabe -> gab)
    ".$Vowel $Consonant + e t|n -> TRY(!irrVerb)_",     // (gabet -> gab)
    ".$Vowel $Consonant + e s t -> TRY(!irrVerb)_",     // (gabest -> gab)

        };

  String[] allRegVerbs = {//":all-reg-verbs",
            //generate regular verb forms by calling computeMorph

    ".$Vowel $Consonant -> TRY(!regVerb)_",  // (kauft,sagte,arbeiten,verkleinere)

        };

  /* String[] RegInf = {//":reg-inf",
     // generates infinitive based on inflected information
     // RegVerbs logic in ComputeMorph is not sufficient to handle all cases 26mar01
     // assumes it is being passed a stem form (inflection stripped off)

    " e r # -> n",      // (wander -> wandern)
    " e l # -> n",      // (handel -> handeln)
    ".$Vowel # -> en",  // (sag -> sagen)
        };
  */

  String[] RegPres = {//":reg-pres",
            //generate present forms from the verb stem

    "e l|r # -> e,st,t,en,n",                           // -n form generates correct infinitive -ern,-eln
    // t is sometime a past tense indicator, so handle the ambiguity denk-ten, abdicht-en

    ".$Vowel $Consonant + t -> _,e,st,t,en,te,test,tet,ten",  // (beschriften,kaufen)
    ".$Vowel t|d + e t|n -> _,e,est,et,en",             // (bedeutest,arbeite)
    ".$Vowel t|d + ?e -> _,e,est,et,en",                // (bedeutest,arbeite)
    ".$Vowel g n + e t|n -> _,e,est,et,en",             // (eigne)
    ".$Vowel g n + ?e -> _,e,est,et,en",                // (eignest,eignen)
    ".$Vowel + e -> (:reg-pres)_",                 // avoid double e (anspeie+e)
    ".$Vowel $Consonant $Consonant n # -> _,e,est,et,en",   // (oeffne,oeffnest,oeffnet)
    ".$Vowel # -> _,e,st,t,en",                    // (sage,sagst,sagt,sagen)

        };

  String[] RegPast = {//":reg-past",
            //generate past forms from the verb stem

    "e l|r # -> te,test,tet,ten,n",             // -n form generates correct infinitive -ern,-eln
    ".$Vowel $Consonant t # -> e,est,et,en,ete,etest,etet,eten",  // (sagte,beschriftetet)
    ".$Vowel t|d + e t|n -> ete,etest,etet,eten", // (arbeitete,arbeiteten)
    ".$Vowel t|d + ?e -> ete,etest,etet,eten", // (arbeitete,arbeiteten)
    ".$Vowel g n + ?e ?t|n -> ete,etest,etet,eten", // (regnet,regnete)
    ".$Vowel n g + ?e -> ete,etest,etet,eten", // (regneteten)
    ".$Vowel $Consonant $Consonant n # -> ete,etest,etet,eten", // (oeffnete,oeffnetest)
    // ".$Vowel + e -> (:reg-past)_",           // avoid double e (anspeie+e)
    ".$Vowel # -> te,test,tet,ten",             // (sagte,sagtest,sagtet,sagten)

        };

   String[] RegSubj1 = {//":reg-subj1",
           // generate subj1 from the verb stem

    "e l|r # -> e,est,et,en,n",  // -n form generates correct infinitive -ern,-eln
    ".$Vowel $Consonant + t # -> e,est,et,en,te,test,tet,ten",   // (saget,beschriftet)
    ".$Vowel # -> e,est,et,en",  // (sage,sagest,saget,sagen)

        };

  String[] RegPastpart = {//":reg-pastpart",
            //generate past participle forms from the (prefix) + ge + PastP-stem

   ".$Vowel $Consonant $Consonant n # -> et",   // (geoeffnet)
    "i e r *$Consonant # -> t",                 // (studiert)
    ".$Vowel t|d # -> et",                      // (gearbeitet)
    ".$Vowel g n # -> et",                      // (geregnet)
    ".$Vowel # -> t",                           // (gesag + t)
        };


  String[] changeAblaut = {//":change-ablaut", // change umlaut form to expanded form

//cc     "< &auml; > .$Consonant -> TRY(!irrVerb)<ae>",   // (g&auml;b -> gaeb)
    "< \u00e4 > .$Consonant -> TRY(!irrVerb)<ae>",   // (g\u00e4b -> gaeb)
//cc     "< &ouml; > .$Consonant -> TRY(!irrVerb)<oe>",   // (l&ouml;g -> loeg)
    "< \u00f6 > .$Consonant -> TRY(!irrVerb)<oe>",   // (l\u00f6g -> loeg)
//cc     "< &uuml; > .$Consonant -> TRY(!irrVerb)<ue>",   // (l&uuml;g -> lueg)
    "< \u00fc > .$Consonant -> TRY(!irrVerb)<ue>",   // (l\u00fcg -> lueg)

        };

   String[] doAblaut = {//":do-ablaut", // vowel shift in 2/3sg. irregular verbs

//cc     "< a > u $Consonant -> <&auml;>,<ae>",   // (laufe,laeuft)
    "< a > u $Consonant -> <\u00e4>,<ae>",   // (laufe,laeuft)

//cc     "< a > $Consonant -> <&auml;>,<ae>",     // avoid prefix 'aus' -> 'aeus' (ausgab -> ausgaeb)
    "< a > $Consonant -> <\u00e4>,<ae>",     // avoid prefix 'aus' -> 'aeus' (ausgab -> ausgaeb)
//cc     "< o > $Consonant -> <&ouml;>,<oe>",     // (stossen,stoesst)
    "< o > $Consonant -> <\u00f6>,<oe>",     // (stossen,stoesst)
//cc     "< u > $Consonant -> <&uuml;>,<ue>",     // (examples?)
    "< u > $Consonant -> <\u00fc>,<ue>",     // (examples?)

    "< > e h $Consonant -> <i>",        // (sehe,sieht) but, 'lese','liest' not handled here!
    "< e > $Consonant $Consonant -> <i>",   // (helfe -> hilft)
    "q u < e > $Consonant -> <i>",      // (quelle -> quillt)
        };


  String[] addUmlaut = {//":add-umlaut", // add umlaut for plural nouns

//cc     "< a > u $Consonant -> <&auml;>,<ae>",           // (haus,h&auml;user,haeuser)
    "< a > u $Consonant -> <\u00e4>,<ae>",           // (haus,h\u00e4user,haeuser)
//cc     "$AllConsonant < a > *$Consonant ?e *$Consonant ?e *$Consonant # -> <&auml;>,<ae>",  // (mann,maenner)
    "$AllConsonant < a > *$Consonant ?e *$Consonant ?e *$Consonant # -> <\u00e4>,<ae>",  // (mann,maenner)
//cc     "$AllConsonant < o > *$Consonant ?e *$Consonant ?e *$Consonant # -> <&ouml;>,<oe>",  // (wort,woerter)
    "$AllConsonant < o > *$Consonant ?e *$Consonant ?e *$Consonant # -> <\u00f6>,<oe>",  // (wort,woerter)
//cc     "$AllConsonant < u > *$Consonant ?e *$Consonant ?e *$Consonant # -> <&uuml;>,<ue>",  // (kuss,kuesst)
    "$AllConsonant < u > *$Consonant ?e *$Consonant ?e *$Consonant # -> <\u00fc>,<ue>",  // (kuss,kuesst)
//cc     "# < a > $Consonant -> <&auml;>,<ae>",             // (apfel,&auml;pfel)
    "# < a > $Consonant -> <\u00e4>,<ae>",             // (apfel,\u00e4pfel)
//cc     "# < o > $Consonant -> <&ouml;>,<oe>",             // (ofen,&ouml;fen)
    "# < o > $Consonant -> <\u00f6>,<oe>",             // (ofen,\u00f6fen)
    "# u n -> un",                                // don't umlaut initial un-
//cc     "# < u > $Consonant -> <&uuml;>,<ue>",             // (ubel,&uuml;bel)
    "# < u > $Consonant -> <\u00fc>,<ue>",             // (ubel,\u00fcbel)
//cc     "# < A > $Consonant -> <Ae>,<&Auml;>",             // (Apfel,Aepfel)
    "# < A > $Consonant -> <Ae>,<\u00c4>",             // (Apfel,Aepfel)
//cc     "# < O > $Consonant -> <Oe>,<&Ouml;>",             // (Ofen,&Ouml;fen)
    "# < O > $Consonant -> <Oe>,<\u00d6>",             // (Ofen,\u00d6fen)
    "# U n -> Un",                                // (Undank)
//cc     "# < U > $Consonant -> <Ue>,<&Uuml;>",             // (Ubel,&Uuml;bel,Uebel)
    "# < U > $Consonant -> <Ue>,<\u00dc>",             // (Ubel,\u00dcbel,Uebel)
        };

  String[] remUmlaut = {//":rem-umlaut", // remove umlaut for singular nouns

    "< a e > u $Consonant -> <a>",                // (haeuser,haus)
//cc     "< &auml; > u $Consonant -> <a>",                  // (h&auml;user,haus)
    "< \u00e4 > u $Consonant -> <a>",                  // (h\u00e4user,haus)
    "$AllConsonant a|o|u < e > *$Consonant ?e *$Consonant ?e *$Consonant # -> <>",  // (maenner,mann)
//cc     "$AllConsonant < &auml; > *$Consonant ?e *$Consonant ?e *$Consonant # -> <a>",  // (m&auml;nner,mann)
    "$AllConsonant < \u00e4 > *$Consonant ?e *$Consonant ?e *$Consonant # -> <a>",  // (m\u00e4nner,mann)
//cc     "$AllConsonant < &ouml; > *$Consonant ?e *$Consonant ?e *$Consonant # -> <o>",  // (w&ouml;rter,wort)
    "$AllConsonant < \u00f6 > *$Consonant ?e *$Consonant ?e *$Consonant # -> <o>",  // (w\u00f6rter,wort)
//cc     "$AllConsonant < &uuml; > *$Consonant ?e *$Consonant ?e *$Consonant # -> <u>",  // (k&uuml;sst,kuss)
    "$AllConsonant < \u00fc > *$Consonant ?e *$Consonant ?e *$Consonant # -> <u>",  // (k\u00fcsst,kuss)
    "#a|o|u < e > $Consonant -> <>",              // (aepfel,apfel)
//cc     "# < &auml; > $Consonant -> <a>",                  // (&auml;pfel,apfel)
    "# < \u00e4 > $Consonant -> <a>",                  // (\u00e4pfel,apfel)
//cc     "# < &ouml; > $Consonant -> <o>",                  // (&ouml;fen,ofen)
    "# < \u00f6 > $Consonant -> <o>",                  // (\u00f6fen,ofen)
//cc     "# < &uuml; > $Consonant -> <u>",                  // (&uuml;bel,ubel)
    "# < \u00fc > $Consonant -> <u>",                  // (\u00fcbel,ubel)
    "#A|O|U < e > $Consonant -> <>",              // (Aepfel,Apfel)
//cc     "# < &Auml; > $Consonant -> <A>",                  // (&Auml;pfel,Apfel)
    "# < \u00c4 > $Consonant -> <A>",                  // (\u00c4pfel,Apfel)
//cc     "# < &Ouml; > $Consonant -> <O>",                  // (&Ouml;fen,Ofen)
    "# < \u00d6 > $Consonant -> <O>",                  // (\u00d6fen,Ofen)
//cc     "# < &Uuml; > $Consonant -> <U>",                  // (&Uuml;bel,Ubel)
    "# < \u00dc > $Consonant -> <U>",                  // (\u00dcbel,Ubel)
        };

  String[] convertUmlaut = {//":convert-umlaut", // change to umlaut form

//cc     "< a e > .$Consonant -> <&auml;>",          // (g&auml;b <- gaeb)
    "< a e > .$Consonant -> <\u00e4>",          // (g\u00e4b <- gaeb)
//cc     "< o e > .$Consonant -> <&ouml;>",          // (l&ouml;g <- loeg)
    "< o e > .$Consonant -> <\u00f6>",          // (l\u00f6g <- loeg)
//cc     "< u e > .$Consonant -> <&uuml;>",          // (l&uuml;g <- lueg)
    "< u e > .$Consonant -> <\u00fc>",          // (l\u00fcg <- lueg)
//cc     "# < A e > .$Consonant -> <&Auml;>",        // ()
    "# < A e > .$Consonant -> <\u00c4>",        // ()
//cc     "# < O e > .$Consonant -> <&Ouml;>",        // ()
    "# < O e > .$Consonant -> <\u00d6>",        // ()
//cc     "# < U e > .$Consonant -> <&Uuml;>",        // ()
    "# < U e > .$Consonant -> <\u00dc>",        // ()

//cc     "< &auml; > .$Consonant -> <ae>",          // (g&auml;b -> gaeb)
    "< \u00e4 > .$Consonant -> <ae>",          // (g\u00e4b -> gaeb)
//cc     "< &ouml; > .$Consonant -> <oe>",          // (l&ouml;g -> loeg)
    "< \u00f6 > .$Consonant -> <oe>",          // (l\u00f6g -> loeg)
//cc     "< &uuml; > .$Consonant -> <ue>",          // (l&uuml;g -> lueg)
    "< \u00fc > .$Consonant -> <ue>",          // (l\u00fcg -> lueg)
//cc     "# < &Auml; > .$Consonant -> <Ae>",        // ()
    "# < \u00c4 > .$Consonant -> <Ae>",        // ()
//cc     "# < &Ouml; > .$Consonant -> <Oe>",        // ()
    "# < \u00d6 > .$Consonant -> <Oe>",        // ()
//cc     "# < &Uuml; > .$Consonant -> <Ue>",        // ()
    "# < \u00dc > .$Consonant -> <Ue>",        // ()

        };


   String[] doUmlaut = {//":do-umlaut", // add/remove umlauts over an input form

    ".$Vowel -> (:add-umlaut)_,(:rem-umlaut)_,(:convert-umlaut)_",

        };


  String[] CapNouns = {//":cap-nouns",   // used when input begins with capital

    "u n g + ?e|s -> _,e,en,s,es",       // (Bildung,Bildunge,Bildungs)
    "u n g + e ?n|s -> _,e,en,s,es",     // (Bildung,Bildungen,Jungen)
    "l i n g + ?e|s -> _,e,s,en,es",     // (Keimling,Keimlinge,Lehrlings)
    "l i n g + e ?n|s -> _,e,s,en,es",   // (Wildlinge,Straeflingen)
    "h|k e i t + ?s -> _,en,s",          // (Freiheit)
    "h|k e i t + e n -> _,en,s",         // (Gesundheiten)
    "s c h a f t + e n -> _,en,s",       // (Wissenschaften)
    "s c h a f t + ?s -> _,en,s",        // (Wissenschaft)
    "h a f t + ?e -> _,en,e,es",         // (Beugehaft)
    "h a f t + e ?n|s -> _,en,e,es",     // (Fehlerhafte)
    "b a r + ?n|s -> _,n,s",             // (Nachbar,Nachbarn,Nachbars)
    "t a e t + ?e|s -> _,e,en,s",        // (Universitaet)
    "t a e t + e n -> _,e,en,s",         // (Universitaeten,Universitaete)
    "n i s + s e ?s|n -> _,se,ses,sen",  // (Erlebnisse)
    "n i s # -> _,se,ses,sen,e,es,en",   // (Gleichnis,Gleichnisse,Anise)
     "$Consonant a + s -> _,s",          // (Kamera,Kameras,Suedamerikas)
    ".$Vowel $Consonant + a # -> as,en,um", // (Delta,Deltas,Drama,Dramen,Kuriosum)


    "i o n + e n -> _,en,s",             // (Functionen)
    "+ i o n ?s -> ion,ien,ionen,ions",  // (Deduktion,Stadien)
    "e i n + ?s -> _,e,s,en,es",         // (Wein,Steine)
    "e i n + e ?n|s # -> _,e,s,en,es",   // (Tafelweinen,Tafelwein)
    ".$Consonant i n + ?s -> _,s,nen",   // (Freundin,Asiatinnen)
    ".$Consonant i n + n e n -> _,s,nen",  // (Freundin,Asiatinnen,Afrikanerin)
    "e u r + ?e|s -> _,en,e,s,(:gender-link)_",  // (Amateur,Amatuers,Amateuren)
    "e u r + e n -> _,en,e,s,(:gender-link)_",   // (Amateur,Amatuers,Amateuren)

    "$AllLetter y p + ?i ?e|s ?n -> _,en,e,s,ien",  // (Typen,Urlaubstip,Grundprinzipien)
    "$Vowel z + ?e ?n|s -> _,e,en,es,(:noun-pl)e",  // (Geiz,Geizes,Notiz,Notizen,Taenze)

    "$Consonant + i z e ?s|n -> ex,exe,exes,izes,izen,exen,ix", // (Index,Indexen,indizes)
    "$Consonant + e x ?e ?n|s -> ex,exe,exes,izes,exen", // (Index,Indexen,indizes)
    ".$AllVowel + x ?e ?n|s -> _,xe,xes,xen,zes,zen", // (Plateaux,PlateauMatrizen)

    ".$Vowel y + ?s -> _,s",    // (Airway,Airways)
    ".$Vowel w + ?s -> _,s",    // (Show,Shows)

    ".$Vowel s s + ?e ?n|s -> (:do-umlaut)_,(:do-umlaut)es,(:do-umlaut)e,(:do-umlaut)en", // (Fluss,Flusses)
//cc     ".$Vowel + &szlig; ?e ?n|s -> (:rem-umlaut)&szlig;,(:rem-umlaut)sses,(:do-umlaut)sse,(:do-umlaut)ssen", // (Schu&szlig;,Sch&uuml;sse)
    ".$Vowel + \u00df ?e ?n|s -> (:rem-umlaut)\u00df,(:rem-umlaut)sses,(:do-umlaut)sse,(:do-umlaut)ssen", // (Schu\u00df,Sch\u00fcsse)

    "$Vowel k + ?e|s -> _,e,en,s,es",       // (Plastik,Plastiks)
    "$Vowel k + e ?n|s -> _,e,en,s,es",     // ()

    "a u m # -> _,es,(:aum-forms)_",        // (Baum,Baumes,Baeume)
    "a u m + ?e s -> _,s,es,(:aum-forms)_", // (Baum,Baumes,Baeume)
    "a e u m + e ?n -> _,(:aum-forms)_",    // (Baum,Baumes,Baeume)
//cc     "&auml; u m + e ?n -> _,(:aum-forms)_",      // (Baum,Baumes,Baeume)
    "\u00e4 u m + e ?n -> _,(:aum-forms)_",      // (Baum,Baumes,Baeume)

    "i + u m ?s -> um,ums,en",          // (Studium,Studiums,Ministerien)
    ".$Vowel + u m ?s -> ums,en,a",     // (Kuriosa,Spektrums,Serum,Seren)
    "$Consonant u r s + ?e ?n|s -> _,e,en,es",   // (Konkurs,Konkurses)
    "$Consonant u r + ?e ?n -> _,en,s",          // (Zensur,Zensuren,Arbiturs)
    "$Consonant u f + ?e|s ?n|s -> _,e,en,s,es", // (Huf,Hufen,Notrul,Notrufs)
    "d + u s -> us,i",                  // (Modus,Modi)
    "d + i -> us,i",                    // (Modus,Modi)
    "$Consonant + u s -> en,us,usse,ussen,usses",  // (Medienzirkusse,Medienzirkussen,Ritus,Riten)
    "$Consonant u s + s e ?n|s -> _,se,sen,ses",  // (Medienzirkusse,Medienzirkussen)

    "$Consonant e e + ?s|n -> _,s,n",  // (Idee,Ideen,Komitees)
    "$Consonant a|o g # -> _,e,en,es,s,(:do-umlaut)e,(:do-umlaut)en,s,es",  // (Epilog,Kataloge)
     "$Consonant e g # -> _,e,en,s,es",  // (Beleg,Belege)

    "i e # -> _,n",                     // (Kalorien,Kalorie)
    "e i # -> _,e,en",                  // (Partei,Parteien)
    "e i g # -> e,en,s",                // (Zweig,Zweige)
    "i l # -> _,e,s,en,es",             // (Korperteil,Civil)
    "e i s # -> _,e,en,er,es",          // (Beweis,Beweise)
    "f a c h + ?e ?s|n -> _,e,es,en,(:add-umlaut)er,(:add-umlaut)ern", // (Hauptfach)
    "m a l # -> _,en,e,s",              // (Merkmal)
//cc     "a l # -> _,(:do-umlaut)e,(:do-umlaut)en,s,es", // (Hochtal,Kan&auml;len)
    "a l # -> _,(:do-umlaut)e,(:do-umlaut)en,s,es", // (Hochtal,Kan\u00e4len)
    "o o # -> _,s,",                    // (Waterloo)
    "$Vowel v + ?s -> _,e,en,es,s",     // (Motiv,Motiven,Archive)
    "$Vowel c # -> _,s,en",             // (Cadillac,Armagnac)
    ".$Vowel $Consonant i o # -> _,s",  // (Radio,Radios)
    ".$Vowel + o ?s -> o,os,en",        // (Kino,Kinos,Passivsaldos)

    "p u l s # -> _,e,s,en",         // (Impuls,Impulse)
    "a|o|u e l l + ?e ?n -> _,e,en,(:convert-umlaut)e,(:convert-umlaut)en",         // (Huelle,Huellen)
//cc     "&auml;|&ouml;|&uuml; l l + ?e ?n -> _,e,en,(:convert-umlaut)e,(:convert-umlaut)en",
    "\u00e4|\u00f6|\u00fc l l + ?e ?n -> _,e,en,(:convert-umlaut)e,(:convert-umlaut)en",
    "e l l + ?e|s -> _,e,s,en",        // (Rebell,Rebells,Rebellen)
    "e l l + e n -> _,e,s,en",         // (Flanellen)
    "$Consonant e l + ?n|s -> _,n,s,(:do-umlaut)_,(:do-umlaut)n,(:rem-umlaut)s",  // (Aepfel,Apfel,Angel,Angeln)

    ".$AllVowel + e r ?s|n -> a,s,es,er,ers,ern,(:gender-link)_,(:do-umlaut)er,(:do-umlaut)ern,(:rem-umlaut)_,(:rem-umlaut)s,(:rem-umlaut)es", // (Mannes,Mann,Ablagern)
    ".$AllVowel + e -> _,e,s,es,en,ens,(:gender-link)_,(:convert-umlaut)e,(:convert-umlaut)en,(:rem-umlaut)_,(:rem-umlaut)es", // (Soehne,Sohn,Aufgabe,Aufgaben)
    ".$AllVowel $Consonant + ?e s -> _,e,en,ern,(:gender-link)_,(:convert-umlaut)_,(:do-umlaut)e,(:convert-umlaut)s,(:do-umlaut)en,(:do-umlaut)er,(:do-umlaut)ern",  // (Sohnes,Soehne)
    "a u s # -> _,e,es,en,(:add-umlaut)er,(:add-umlaut)ern", // (Haus,Haueser)

//cc     ".&auml;|&Auml;|&Ouml;|&ouml;|U&uuml; + e n -> _,e,en,ens,(:gender-link)_,(:convert-umlaut)e,(:convert-umlaut)en,(:rem-umlaut)_,(:rem-umlaut)es",
    ".\u00e4|\u00c4|\u00d6|\u00f6|U\u00fc + e n -> _,e,en,ens,(:gender-link)_,(:convert-umlaut)e,(:convert-umlaut)en,(:rem-umlaut)_,(:rem-umlaut)es",
    " a|A|O|o|U|u e .$Consonant + e n -> _,e,en,ens,(:gender-link)_,(:convert-umlaut)e,(:convert-umlaut)en,(:rem-umlaut)_,(:rem-umlaut)es",
    ".$AllVowel + e n -> _,a,e,s,es,us,um,ums,ens,(:gender-link)_", // (Beinen,Bein,Beine,Thema,Ritus,Riten)

    // default pattern
    ".$AllVowel -> _,e,s,es,en,er,ern,(:gender-link)_,(:do-umlaut)_,(:do-umlaut)e,(:do-umlaut)en,(:do-umlaut)er,(:do-umlaut)ern",
    // (Aepfel,Maenner,Frauen,Autos,Versuche)

        };

  String[] GenderLink = {//":gender-link",
  // generates feminine -in, -innen forms for masculine nouns

    ".$Vowel $Consonant # -> in,innen",          // ()

        };

  String[] NounPl = {//":noun-pl",
  // also generate the dative -en forms

    ".$Vowel + e ?s -> _,en,(:add-umlaut)_,(:add-umlaut)e,(:add-umlaut)en,(:add-umlaut)er,(:add-umlaut)ern",  // ()
    "a u s # -> _,en,(:do-umlaut)er,(:do-umlaut)ern",  // (Haeuser)
    ".$Vowel + s -> _,n,en,(:do-umlaut)er,(:do-umlaut)ern",  // ()
    ".$Vowel e + n -> _,(:convert-umlaut)_,(:convert-umlaut)n",  // (aufgabe,aufgaben)
    ".$Vowel e r + ?s  -> s,n,en,(:gender-link)_,(:convert-umlaut)_,(:convert-umlaut)n,(:convert-umlaut)en",  // (aufpassers,bankier,ablagern)
    ".$Vowel # -> _,e,s,en,er,(:gender-link)_,(:add-umlaut)_,(:add-umlaut)e,(:add-umlaut)en,(:add-umlaut)er,(:add-umlaut)ern",  // (aepfel,maenner,frauen,autos,versuche)

        };

  String[] NounSg = {//":noun-sg",
  // also generate the genitive -s,-es forms

    "i n + n e n -> _,s",  // (freundinnen,freundin)

    "a|o|u e .$Consonant + e ?n -> _,s,es,ens,(:convert-umlaut)en,(:convert-umlaut)ens,(:rem-umlaut)_,(:rem-umlaut)s,(:rem-umlaut)es", // (sohn,maedchens)
//cc     "&auml;|&ouml;|&uuml; .$Consonant + e ?n -> _,s,es,ens,(:convert-umlaut)en,(:convert-umlaut)ens,(:rem-umlaut)_,(:rem-umlaut)s,(:rem-umlaut)es", // (sohn,maedchens)
    "\u00e4|\u00f6|\u00fc .$Consonant + e ?n -> _,s,es,ens,(:convert-umlaut)en,(:convert-umlaut)ens,(:rem-umlaut)_,(:rem-umlaut)s,(:rem-umlaut)es", // (sohn,maedchens)
    ".$Vowel + e ?n -> _,e,s,es,us,um,ens,(:gender-link)_", // (beinen,bein,datum,daten,student,studentin)

    "a|o|u e .$Consonant + e r ?n -> (:gender-link)_,(:rem-umlaut)_,(:rem-umlaut)s,(:rem-umlaut)es", // (maenner,mannes)
//cc     ".&auml;|&ouml;|&uuml; + e r ?n -> (:gender-link)_,(:rem-umlaut)_,(:rem-umlaut)s,(:rem-umlaut)es", // (m&auml;nner,mannes)
    ".\u00e4|\u00f6|\u00fc + e r ?n -> (:gender-link)_,(:rem-umlaut)_,(:rem-umlaut)s,(:rem-umlaut)es", // (m\u00e4nner,mannes)
    ".$Vowel + e r ?n -> _,s,es,(:gender-link)_", // bilder,bild)

    ".$Vowel + e s -> _,e,s,es,(:gender-link)_",  // (angebotes,angebot)

//cc     ".&auml;|&ouml;|&uuml; + s -> _,e,s,es,(:gender-link)_,(:convert-umlaut)_,(:convert-umlaut)s", // (maedchens,m&auml;dchens)
    ".\u00e4|\u00f6|\u00fc + s -> _,e,s,es,(:gender-link)_,(:convert-umlaut)_,(:convert-umlaut)s", // (maedchens,m\u00e4dchens)
//cc     "a|o|u e .$Consonant + s -> _,e,s,es,(:gender-link)_,(:convert-umlaut)_,(:convert-umlaut)s", // (maedchens,m&auml;dchens)
    "a|o|u e .$Consonant + s -> _,e,s,es,(:gender-link)_,(:convert-umlaut)_,(:convert-umlaut)s", // (maedchens,m\u00e4dchens)
    ".$Vowel + s -> _,e,s,es",  // (abdrucks,abdruck)

    ".$Vowel # -> _,s,es,(:gender-link)_,(:rem-umlaut)_,(:rem-umlaut)s,(:rem-umlaut)es",  // (apfel,buches,onkels)

        };

   String[] AumForms = {//":aum-forms",
   // generates limited plural endings with umlaut on stem vowel; assumes the form passed ends in -um stem

   "$AllConsonant a u m # -> _,es,(:add-umlaut)e,(:add-umlaut)en", // (baumes,baeume)
   "$AllConsonant < a e > u m # -> e,en,(:convert-umlaut)e,(:convert-umlaut)en,(:rem-umlaut)_,(:rem-umlaut)es",
//cc    "$AllConsonant < &auml; > u m # -> e,en,(:convert-umlaut)e,(:convert-umlaut)en,(:rem-umlaut)_,(:rem-umlaut)es",
   "$AllConsonant < \u00e4 > u m # -> e,en,(:convert-umlaut)e,(:convert-umlaut)en,(:rem-umlaut)_,(:rem-umlaut)es",

        };

  String[] AllAdj = {//":all-adj",

    // Note: don't umlaut forms with a two-vowel stem
    ".$Vowel -> (:base-adj)_,(:adj-comp)_,(:adj-super)_",
        };

   String[] AllAdjWithUmlaut = {//":all-adj-with-umlaut",

   // some adjectives take umlauts in comparative and superlative,
   // not wholly predictable, so do both umlauted, unumlauted

   // "$Consonant a|o|u < > $Consonant -> (:base-adj)_,(:adj-comp)<e>,(:adj-comp)_,(:adj-super)<e>,(:adj-super)_",

//cc    "$Consonant < a > $Consonant -> (:base-adj)_,(:adj-comp)_,(:adj-super)_,(:adj-comp)<ae>,(:adj-super)<ae>,(:adj-comp)<&auml;>,(:adj-super)<&auml;>",
   "$Consonant < a > $Consonant -> (:base-adj)_,(:adj-comp)_,(:adj-super)_,(:adj-comp)<ae>,(:adj-super)<ae>,(:adj-comp)<\u00e4>,(:adj-super)<\u00e4>",
//cc    "$Consonant < o > $Consonant -> (:base-adj)_,(:adj-comp)_,(:adj-super)_,(:adj-comp)<oe>,(:adj-super)<oe>,(:adj-comp)<&ouml;>,(:adj-super)<&ouml;>",
   "$Consonant < o > $Consonant -> (:base-adj)_,(:adj-comp)_,(:adj-super)_,(:adj-comp)<oe>,(:adj-super)<oe>,(:adj-comp)<\u00f6>,(:adj-super)<\u00f6>",
//cc    "$Consonant < u > $Consonant -> (:base-adj)_,(:adj-comp)_,(:adj-super)_,(:adj-comp)<ue>,(:adj-super)<ue>,(:adj-comp)<&uuml;>,(:adj-super)<&uuml;>",
   "$Consonant < u > $Consonant -> (:base-adj)_,(:adj-comp)_,(:adj-super)_,(:adj-comp)<ue>,(:adj-super)<ue>,(:adj-comp)<\u00fc>,(:adj-super)<\u00fc>",

//cc    "# < a > $Consonant -> (:base-adj)_,(:adj-comp)_,(:adj-super)_,(:adj-comp)<ae>,(:adj-super)<ae>,(:adj-comp)<&auml;>,(:adj-super)<&auml;>",
   "# < a > $Consonant -> (:base-adj)_,(:adj-comp)_,(:adj-super)_,(:adj-comp)<ae>,(:adj-super)<ae>,(:adj-comp)<\u00e4>,(:adj-super)<\u00e4>",
//cc    "# < o > $Consonant -> (:base-adj)_,(:adj-comp)_,(:adj-super)_,(:adj-comp)<oe>,(:adj-super)<oe>,(:adj-comp)<&ouml;>,(:adj-super)<&ouml;>",
   "# < o > $Consonant -> (:base-adj)_,(:adj-comp)_,(:adj-super)_,(:adj-comp)<oe>,(:adj-super)<oe>,(:adj-comp)<\u00f6>,(:adj-super)<\u00f6>",
//cc    "# < u > $Consonant -> (:base-adj)_,(:adj-comp)_,(:adj-super)_,(:adj-comp)<ue>,(:adj-super)<ue>,(:adj-comp)<&uuml;>,(:adj-super)<&uuml;>",
   "# < u > $Consonant -> (:base-adj)_,(:adj-comp)_,(:adj-super)_,(:adj-comp)<ue>,(:adj-super)<ue>,(:adj-comp)<\u00fc>,(:adj-super)<\u00fc>",

   ".$Vowel -> (:base-adj)_,(:adj-comp)_,(:adj-super)_",

        };


  String[] BaseAdj = {//":base-adj",

    // generate the base (non-comparative/superlative) form of adjectives

    ".$Vowel -> _,e,em,en,er,es",     // (warm,kalt)

        };

 String[] AdjComp = {//":adj-comp",

    //generate comparative adjective forms

    ".$Vowel + e l -> ler,lere,lerem,leren,lerer,leres", // (dunkel -> dunkler,eitel -> eitler)
    "$Vowel + e r -> rer,rere,rerem,reren,rerer,reres",  // (teuer -> teurer, but sauberer,heiterer)
    ".$Vowel -> ere,erem,eren,erer,eres",
        };

  String[] AdjSuper = {//":adj-super",

    // generate superlative adjective forms, some morphophonemic considerations

//cc     ".$Vowel t|d|s|z # -> est,este,estem,esten,ester,estes",  // (k&uuml;rzesten,&auml;ltester)
    ".$Vowel t|d|s|z # -> est,este,estem,esten,ester,estes",  // (k\u00fcrzesten,\u00e4ltester)
    ".$Vowel h # -> st,est,ste,este,stem,estem,sten,esten,ster,ester,stes,estes",  // (froh+sten,falsch+este)
    ".$Vowel  -> st,ste,stem,sten,ster,stes", // (tollstem, klarster)

        };


  String[] AllForms = {//":all-forms",
            //generate all the noun, verb, and adj forms

    ".$Vowel + e -> (:noun-sg)e,(:noun-pl)e,(:all-adj-with-umlaut)_,(:all-reg-verbs)e,(:convert-umlaut)e",
    ".$Vowel # -> (:noun-sg)_,(:noun-pl)_,(:all-adj-with-umlaut)_,(:all-reg-verbs)_,(:convert-umlaut)_",
            //  (maedchen)

        };

        defRules(":unnamed", rootRules);
        defRules(":literal", genLiteral);
        defRules(":irr-pres", IrrPres);
        defRules(":from-infinitive", FromInfinitive);  // 15dec00
        defRules(":irr-past", IrrPast);
        defRules(":irr-subj2", IrrSubj2);
        defRules(":strip-irr-subj2", testForIrrSubj2);
        defRules(":irr-subj1", IrrSubj1);
        // defRules(":reg-inf", RegInf);
        defRules(":reg-pres", RegPres);
        defRules(":reg-past", RegPast);
        defRules(":reg-subj1", RegSubj1);
        defRules(":reg-pastpart", RegPastpart);
        defRules(":all-reg-verbs", allRegVerbs);
        defRules(":change-ablaut", changeAblaut);  // 3may01 ach
        defRules(":do-ablaut", doAblaut);
        defRules(":add-umlaut", addUmlaut);
        defRules(":rem-umlaut", remUmlaut);
        defRules(":convert-umlaut", convertUmlaut); // 3may01
        defRules(":do-umlaut", doUmlaut);
        defRules(":cap-nouns", CapNouns);
        defRules(":gender-link", GenderLink);
        defRules(":noun-pl", NounPl);
        defRules(":noun-sg", NounSg);
        defRules(":aum-forms", AumForms);
        defRules(":all-adj", AllAdj);
        defRules(":all-adj-with-umlaut", AllAdjWithUmlaut);
        defRules(":base-adj", BaseAdj);
        defRules(":adj-comp", AdjComp);
        defRules(":adj-super", AdjSuper);
        defRules(":all-forms", AllForms);

    }


    /**
     * The following static final boolean variable authorFlag is a flag for
     * use by localization authors when developing morphological rules.
     * This flag will be set to false for a delivered run-time version, but
     * can be set true when a morphological rule set is being developed.
     * This flag is used to enable format checking and tracing that is important
     * during rule development, but which is unnecessary in the run-time
     * rule system, after the rule developer has used this facility to insure
     * that the rules are well-formed.  It is a static final variable, so
     * that the compiler will optimize the extra code away when the variable
     * is false, so that the run-time class files will be smaller.  When
     * authorFlag is false, all of the code associated with the tracing
     * mechanism will automatically be eliminated by the compiler.
     * This class needs its own authorFlag variable, since it is static.
     *
     * See the LiteMorph.java and LiteMorphRule.java for more information.
     */
    public static final boolean authorFlag = false;

    /**
     * For tracing the testing of LiteMorph rules.
     */
    public static boolean traceFlag = false; //so tester can set it

    /**
     * For tracing behavior of rule matching.
     */
    private static void trace(String str) {
        if( authorFlag && traceFlag ) {
            System.out.println("LiteMorph_de: " + str);
        }
    }

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
