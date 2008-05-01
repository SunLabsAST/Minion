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
 * This is a Spanish version of LiteMorph
 *
 * @author W. A. Woods
 * @author Ann Houston
 * @version     1.2     10/08/2001
 *
 * The rules for this system and for the Spanish-specific computeMorph,
 * were developed by Ann Houston, and the Java code for interpreting
 * them was developed by W. A. Woods
 *
 * @see LiteMorph
 */
public class LiteMorph_es extends LiteMorph{

    private static LiteMorph morph;

    /**
     * Make this a singleton class
     */
     private LiteMorph_es() {
    }

    /**
     * Return the LiteMorph for this class
     */
    public static LiteMorph getMorph() {
        if (morph == null) {
            morph = new LiteMorph_es();
        }
        return morph;
    }

    private static Hashtable paradigms = new Hashtable(1500, (float)0.7);

    // Above hash table the right size for Spanish hash table?

   private static String prefixes = "ad ab ante a circun com contra con des de dis entre en extra ex im inter intra in" +
                " menos minus ob o pre pro post pos re super sub su sobre trans tras " ;

     // function for declaring arguments to computeMorph for author mode checking
     // redefined from dummy method in parent class LiteMorph.
   
  protected String[] computeMorphArgs() { //legal values for arg in computeMorph
      String[] args = {"irrVerb"};
      return args;
    }

    // function for computing morphological variants of irregular verbs
    // redefined from dummy method in parent class LiteMorph.

    protected String[] computeMorph(String input, String arg, int depth,
                            String prefix, String suffix) {

        String paradigm = (String)paradigms.get(input);
        // infinitive,progressive,participle,present,preterite,stem = (inf - ending)

        String infin, prog, part, pres, pret, stem;   
        infin = prog = part = pres = pret = stem = "";  // make sure all are initialized
        int length = input.length();
        // if (authorFlag) trace("Paradigm is currently: " + paradigm);

        if (paradigm == null) {
          if (authorFlag)
            trace("Did NOT find " + input + " in Verb Lookup Table");
          String tempPrefix, wordform;
          String[] tempVal = null;
          // collect prefixes - see if one matches input

          StringTokenizer tokens = new StringTokenizer(prefixes, " ");
          while (tokens.hasMoreTokens() && paradigm == null) {
            tempPrefix = tokens.nextToken();
            if (input.startsWith(tempPrefix) && (input.length() > 3)
                  && (prefix.equals(""))) {     // don't remove existing prefix

              wordform = input.substring(tempPrefix.length()); // strip prefix from input
              if (authorFlag)
                trace("In computeMorph("+arg+"): "+ input +" is "+ tempPrefix +
                      " + " + wordform);
              if (authorFlag)
                trace("Testing Prefixes: "+ tempPrefix);

              if ((wordform.length() > 1) && (paradigms.get(wordform)!=null)) {
                if (authorFlag)
                  trace("Trying computeMorph("+arg+") on " + wordform +
                        " with prefix: " + tempPrefix + ", at depth " + (1 + depth));
                tempVal = computeMorph(wordform, arg, depth+1, tempPrefix, suffix);
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

        if ((paradigm == null) && arg.equals("irrVerb")) return null;  // why this line?

        // test for irregular verb forms
        else if (arg.equals("irrVerb")) {
          Set<String> variants = new HashSet<String>();
          if (authorFlag)
            trace("Inside Spanish IrrVerb");
          LiteMorphRule[] useRules;
          StringTokenizer tokens = new StringTokenizer(paradigm, " ");
          infin = tokens.nextToken();
          prog = tokens.nextToken();
          part = tokens.nextToken();
          pres = tokens.nextToken();
          pret = tokens.nextToken();

          // derive the stem from infinitive
          if (infin.length() < 4) {  // (ver,ser,ir)
              stem = infin.substring(0,infin.length()-1);
          }
          else {
              stem = infin.substring(0,infin.length()-2);
          }

          if (authorFlag)
            trace("Found paradigm in Verb Lookup Table: "+infin+" "+prog+" "+
                  part + " " + pres + " "  + pret);

          // generates forms whether or not prefix = null, if prefix not null, included it.
          if (authorFlag)
            trace("Using paradigm from Verb Lookup Table: " + infin + " " +
                  prog + " " + part + " " + pres + " " + pret);

          if (infin.endsWith("ar"))  {
            morphWord(prefix+infin, depth+1, ":literal", variants);
            morphWord(prefix+prog, depth+1, ":irreg-prog-part", variants);
            morphWord(prefix+part, depth+1, ":irreg-prog-part", variants);

            morphWord(prefix+pres, depth+1, ":ar-irreg-pres", variants);  // generate irreg present
            morphWord(prefix+stem, depth+1, ":ar-imperf", variants);      // generate imperfect for -ar
            morphWord(prefix+pret, depth+1, ":ar-irreg-pret", variants);  // generate irreg preterite      
            morphWord(prefix+pres, depth+1, ":ar-irreg-pres-subj", variants);   // generate present subjunctive
            morphWord(prefix+pret, depth+1, ":ar-irreg-imp-subj", variants);    // generate imperfect subjunctive
            morphWord(prefix+stem, depth+1, ":ar-fut-subj", variants);    // generate future subjunctive
            morphWord(prefix+stem, depth+1, ":ar-irreg-imperative", variants);  // generate imperative -ar

            morphWord(prefix+infin, depth+1, ":all-cond", variants);
            morphWord(prefix+infin, depth+1, ":all-future", variants);
          }
          if (infin.endsWith("er"))  {
            morphWord(prefix+infin, depth+1, ":literal", variants);
            morphWord(prefix+prog, depth+1, ":irreg-prog-part", variants);
            morphWord(prefix+part, depth+1, ":irreg-prog-part", variants);

            morphWord(prefix+pres, depth+1, ":er-irreg-pres", variants);    // generate irreg -er present
            morphWord(prefix+stem, depth+1, ":er-ir-imperf", variants);     // geneate imperfect for -er
            morphWord(prefix+pret, depth+1, ":er-irreg-pret", variants);    // generate irreg  -er preterite
            morphWord(prefix+pres, depth+1, ":er-irreg-pres-subj", variants);  // generate present subjunctive
            morphWord(prefix+pret, depth+1, ":er-irreg-imp-subj", variants);   // generate imperfect subjunctive
            morphWord(prefix+stem, depth+1, ":er-fut-subj", variants);   // generate future subjunctive
            morphWord(prefix+pres, depth+1, ":er-irreg-imperative", variants);  // generate imperative -er

            morphWord(prefix+infin, depth+1, ":all-cond", variants);
            morphWord(prefix+infin, depth+1, ":all-future", variants);
          }
//cc           if (infin.endsWith("ir") || infin.endsWith("&iacute;r"))  {              // need &iacute;r for o&iacute;r
          if (infin.endsWith("ir") || infin.endsWith("\u00edr"))  {              // need \u00edr for o\u00edr
            morphWord(prefix+infin, depth+1, ":literal", variants);
            morphWord(prefix+prog, depth+1, ":irreg-prog-part", variants);
            morphWord(prefix+part, depth+1, ":irreg-prog-part", variants);
  
            morphWord(prefix+pres, depth+1, ":ir-irreg-pres", variants);    // generate irreg -ir present
            morphWord(prefix+stem, depth+1, ":er-ir-imperf", variants);     // generate imperfect for -ir
            morphWord(prefix+pret, depth+1, ":ir-irreg-pret", variants);    // generate irreg -er preterite
            morphWord(prefix+pres, depth+1, ":ir-irreg-pres-subj", variants);  // generate present subjunctive
            morphWord(prefix+pret, depth+1, ":ir-irreg-imp-subj", variants);   // generate imperfect subjunctive
            morphWord(prefix+stem, depth+1, ":ir-fut-subj", variants);   // generate future subjunctive
            morphWord(prefix+pres, depth+1, ":ir-irreg-imperative", variants);   // generate imperative -ir

            morphWord(prefix+infin, depth+1, ":all-cond", variants);
            morphWord(prefix+infin, depth+1, ":all-future", variants);
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


    /*
     * This is a locale-specific initialization.
     * Create the rules and exceptions HashTables for the sizes needed,
     * define a local variable for verb paradigms, and then call:
     *  initialize(exceptions, exceptionTable);
     *  initialize(paradigms, paradigmTable);
     */

    protected void intialize() {
        if (rulesTable != null) {
            return;
        }

        // Set necessary LiteMorph class parameters:

        rulesTable = new Hashtable(100, (float)0.7);
        exceptions = new Hashtable(100, (float)0.7);

        // Define variables for use in rules:

//cc         defVar("$Consonant", "bc&ccedil;dfghjklmn&ntilde;pqrstvwxyz");
        defVar("$Consonant", "bc\u00e7dfghjklmn\u00f1pqrstvwxyz");
//cc         defVar("$Vowel", "aeio&oacute;u&aacute;&eacute;&iacute;&uacute;&uuml;");
        defVar("$Vowel", "aeio\u00f3u\u00e1\u00e9\u00ed\u00fa\u00fc");
//cc         defVar("$Vowel+y", "aeiou&aacute;&eacute;&iacute;&uacute;&uuml;y");
        defVar("$Vowel+y", "aeiou\u00e1\u00e9\u00ed\u00fa\u00fcy");
//cc         defVar("$Accent", "&aacute;&eacute;&iacute;&oacute;&uacute;");
        defVar("$Accent", "\u00e1\u00e9\u00ed\u00f3\u00fa");

//cc         defVar("$Letter", "a&aacute;bc&ccedil;de&eacute;fghi&iacute;jklmn&ntilde;o&oacute;pqrstu&uacute;&uuml;vwxyz");
        defVar("$Letter", "a\u00e1bc\u00e7de\u00e9fghi\u00edjklmn\u00f1o\u00f3pqrstu\u00fa\u00fcvwxyz");
//cc         defVar("$AllLetter","AaBbC&Ccedil;c&ccedil;DdEeFfGgHhIiJjKkLlMmNn&ntilde;OoPpQqRrSs&szlig;TtUuVvWwXxYyZz");
        defVar("$AllLetter","AaBbC\u00c7c\u00e7DdEeFfGgHhIiJjKkLlMmNn\u00f1OoPpQqRrSs\u00dfTtUuVvWwXxYyZz");


        // Define the exceptions:
        // Note that these exception table variables exist only during initialization
        // after which the information is incorporated into the appropriate hash tables
        // and similarly for the rule variables.

     String[] exceptionTable = {
     
//cc      "adi&oacute;s",
     "adi\u00f3s",
     "afuera",
     "ahora",
     "al",
     "alga algas",
     "algo",
     "alguien",
//cc      "alg&uacute;n alg&uacute;no alg&uacute;nos alg&uacute;na alg&uacute;nas",
     "alg\u00fan alg\u00fano alg\u00fanos alg\u00fana alg\u00fanas",
     "anilla anillas",
     "anillo anillos",
     "antes",
     "aprendiza aprendiz aprendices",
//cc      "aqu&iacute;",
     "aqu\u00ed",
//cc      "ac&aacute;",
     "ac\u00e1",
//cc      "all&iacute;",
     "all\u00ed",
//cc      "all&aacute;",
     "all\u00e1",
     "ahi",
//cc      "a&ntilde;o a&ntilde;os",
     "a\u00f1o a\u00f1os",
     "asiento asientos",
     "atamiento atadura ataduras",
     "ayer",
     "banda bandas",
     "bando bandos",
     "bandada bandadas",
//cc      "beb&eacute; beb&eacute;s",
     "beb\u00e9 beb\u00e9s",
     "bolsilla bolsillas",
     "bolsillo bolsillos",
//cc      "bomb&oacute;n bombones",
     "bomb\u00f3n bombones",
     "bombona bombonas",
     "brochaza brochazas",
     "brochazo brochazos",
     "brota",
     "brotas",
     "cada",
     "camota camotas",
     "camote camotes",
     "carne",
     "carnada carnadas",
//cc      "carn&eacute; carn&eacute;s",
     "carn\u00e9 carn\u00e9s",
     "chispaza chispazas",
     "chispazo chispazos",
     "combatimiento combate combates",
//cc      "c&oacute;mo",
     "c\u00f3mo",
//cc      "cu&aacute;l cu&aacute;les",
     "cu\u00e1l cu\u00e1les",
//cc      "cu&aacute;ndo cu&aacute;nda",
     "cu\u00e1ndo cu\u00e1nda",
//cc      "cu&aacute;nto cu&aacute;nta",
     "cu\u00e1nto cu\u00e1nta",
     "cuclillas",
     "cuclillo cuclillos",
//cc      "dar doy das da damos dais dan di diste dio dimos disteis dieron daba dabas d&aacute;bamos dabais daban" +
     "dar doy das da damos dais dan di diste dio dimos disteis dieron daba dabas d\u00e1bamos dabais daban" +
//cc      " dar&iacute;a dar&iacute;as dar&iacute;amos dar&iacute;ais dar&iacute;an dar&eacute; dar&aacute;s dar&aacute; daremos dar&eacute;is dar&aacute;n d&eacute; des demos deis den" +
     " dar\u00eda dar\u00edas dar\u00edamos dar\u00edais dar\u00edan dar\u00e9 dar\u00e1s dar\u00e1 daremos dar\u00e9is dar\u00e1n d\u00e9 des demos deis den" +
//cc      " diera dieras di&eacute;ramos dierais dieran diese dieses di&eacute;semos dieseis diesen diere dieres di&eacute;remos" +
     " diera dieras di\u00e9ramos dierais dieran diese dieses di\u00e9semos dieseis diesen diere dieres di\u00e9remos" +
     " diereis dieren dad dando dado dados dada dadas",
     "del",
     "desmota desmotas",
     "desmote desmotes",
     "despince despinces",
     "despues",
//cc      "detr&aacute;s",
     "detr\u00e1s",
     "dicharacha dicharachas dicharacho dicharachos dicharachero dicharacheros dicharachera dicharacheras",
//cc      "d&iacute;filo d&iacute;filos d&iacute;fila d&iacute;filas difilia",
     "d\u00edfilo d\u00edfilos d\u00edfila d\u00edfilas difilia",
     "dios dioces diosa diosas",
//cc      "d&oacute;nde ad&oacute;nde",
     "d\u00f3nde ad\u00f3nde",
     "el",
//cc      "&eacute;l",
     "\u00e9l",
     "ella",
     "ellos ellas",
     "entonces",
     "entre",
     "escudo escudos",
     "fajares",
     "gorrilla gorrillas",
     "gorrillo gorrillos",
     "grama gramas",
     "gramo gramos",
     "gripa gripas",
     "gripe gripes",
//cc      "hemofilia hemof&iacute;lico hemof&iacute;licos hemof&iacute;lica hemof&iacute;licas",
     "hemofilia hemof\u00edlico hemof\u00edlicos hemof\u00edlica hemof\u00edlicas",
     "ir voy vas va vamos vais van iba ibas ibamos ibais iban fui fuiste fue fuimos fuisteis fueron" +
//cc      " ir&eacute; ir&aacute;s ir&aacute; iremos ir&eacute;is ir&aacute;n ir&iacute;a ir&iacute;as ir&iacute;amos ir&iacute;ais ir&iacute;an vaya vayas vayamos vay&aacute;is vayan" +
     " ir\u00e9 ir\u00e1s ir\u00e1 iremos ir\u00e9is ir\u00e1n ir\u00eda ir\u00edas ir\u00edamos ir\u00edais ir\u00edan vaya vayas vayamos vay\u00e1is vayan" +
//cc      " fuera fueras fu&eacute;ramos fuerais fueran fuere fueres fu&eacute;remos fuereis fueren fuese fueses fu&eacute;semos" +
     " fuera fueras fu\u00e9ramos fuerais fueran fuere fueres fu\u00e9remos fuereis fueren fuese fueses fu\u00e9semos" +
     " fueseis fuesen yendo ido ve id", 
//cc      "jam&aacute;s",
     "jam\u00e1s",
     "linternaza linternazas linternazo linternazos",
     "luis luises",
//cc      "mam&aacute; mam&aacute;s",
     "mam\u00e1 mam\u00e1s",
//cc      "ma&ntilde;ana ma&ntilde;anas",
     "ma\u00f1ana ma\u00f1anas",
     "medranza medranzas medra medro",
//cc      "m&iacute; m&iacute;s",
     "m\u00ed m\u00eds",
//cc      "mancebez manceb&iacute;a",
     "mancebez manceb\u00eda",
     "me",
     "morada moradas",
     "morado",
     "mujer mujeres",
     "nada",
     "nadie",
     "nevera neveras",
     "nevero neveros",
     "ni",
//cc      "ning&uacute;n ning&uacute;no ning&uacute;nos ning&uacute;na ning&uacute;nas",
     "ning\u00fan ning\u00fano ning\u00fanos ning\u00fana ning\u00fanas",
     "nos",
     "nosotros nosotras",
     "nuestro nuestra nuestros nuestras",
     "nunca",
     "o",
     "os",
//cc      "pap&aacute; pap&aacute;s",
     "pap\u00e1 pap\u00e1s",
     "paragona",
     "paragonas",
     "pelotilla pelotillas pelotillera pelotilleras pelotillero pelotilleros",
     "pero",
     "pontezuela pontezuelas puentezuelo puentezuelos",
     "portilla portillas",
     "portillo portillos",
     "pues",
//cc      "qu&eacute;",
     "qu\u00e9",
//cc      "qui&eacute;n qui&eacute;nes",
     "qui\u00e9n qui\u00e9nes",
     "recordanza recordanzas recordatorio recordatorios",
     "se",
//cc      "ser soy eres es somos sois son era eras &eacute;ramos erais eran ser&iacute;a ser&iacute;as ser&iacute;amos ser&iacute;ais ser&iacute;an" +
     "ser soy eres es somos sois son era eras \u00e9ramos erais eran ser\u00eda ser\u00edas ser\u00edamos ser\u00edais ser\u00edan" +
//cc      " ser&eacute; ser&aacute;s ser&aacute; seremos ser&eacute;is ser&aacute;n sea seas seamos se&aacute;is sean sed siendo sido",
     " ser\u00e9 ser\u00e1s ser\u00e1 seremos ser\u00e9is ser\u00e1n sea seas seamos se\u00e1is sean sed siendo sido",
     "siempre",
     "su sus",
     "vosotros vosotras",
     "ellos ellas",
     "tampoco",
//cc      "tambi&eacute;n",
     "tambi\u00e9n",
     "te",
//cc      "t&uacute;",
     "t\u00fa",
     "tu tus",
     "ustedes usted",         
     "velacho velachos velucha veluchas",  
     "vos",                                      
     "vuestro vuestra vuestros vuestras",
     "yo",
        };

        initialize(exceptions, exceptionTable);

        // Define the Spanish irregular verb paradigms:
        // The five forms are: infinitive, progressive, past participle, 1st present, 1st preterite

        String[] paradigmTable = {

//cc     "abrir abriendo abierto abro abr&iacute;",
    "abrir abriendo abierto abro abr\u00ed",
    "andar andando andado ando anduve",
//cc     "almorzar almorzando almorzado almuerzo almorc&eacute;",
    "almorzar almorzando almorzado almuerzo almorc\u00e9",
//cc     "arg&uuml;ir arguyendo arg&uuml;ido arguyo arg&uuml;i",
    "arg\u00fcir arguyendo arg\u00fcido arguyo arg\u00fci",
//cc     "asir asiendo asido asgo as&iacute;",
    "asir asiendo asido asgo as\u00ed",
//cc     "avergonzar avergonzando avergonzado averg&uuml;enzo avergonc&eacute;",
    "avergonzar avergonzando avergonzado averg\u00fcenzo avergonc\u00e9",
    "bendecir bendeciendo bendecido bendigo bendije",
//cc     "buscar buscando buscado busco busqu&eacute;",
    "buscar buscando buscado busco busqu\u00e9",
    "caber cabiendo cabido quepo cupe",
//cc     "caer cayendo ca&iacute;do caigo ca&iacute;",
    "caer cayendo ca\u00eddo caigo ca\u00ed",
//cc     "cocer cociendo cocido cuezo coc&iacute;",
    "cocer cociendo cocido cuezo coc\u00ed",
//cc     "coger cogiendo cogido cojo cog&iacute;",
    "coger cogiendo cogido cojo cog\u00ed",
    "conducir conduciendo conducido conduzco conduje",
//cc     "conocer conociendo conocido conozco conoc&iacute;",
    "conocer conociendo conocido conozco conoc\u00ed",
//cc     "contar contando contado cuento cont&eacute;",
    "contar contando contado cuento cont\u00e9",
//cc     "creer creyendo cre&iacute;do creo cre&iacute;",
    "creer creyendo cre\u00eddo creo cre\u00ed",
//cc     "cubrir cubriendo cubierto cubro cubr&iacute;",
    "cubrir cubriendo cubierto cubro cubr\u00ed",
    "decir diciendo dicho digo dije",
//cc     "distinguir distinguiendo distinguido distingo distingu&iacute;",
    "distinguir distinguiendo distinguido distingo distingu\u00ed",
//cc     "dormir durmiendo dormido duermo dorm&iacute;",
    "dormir durmiendo dormido duermo dorm\u00ed",
//cc     "empezar empezando empezado empiezo empec&eacute;",
    "empezar empezando empezado empiezo empec\u00e9",
//cc     "errar errando errado yerro err&eacute;",
    "errar errando errado yerro err\u00e9",
//cc     "esparcir esparciendo esparcido esparzo esparc&iacute;",
    "esparcir esparciendo esparcido esparzo esparc\u00ed",
    "estar estando estado estoy estuve",
//cc     "freir friendo frito fr&iacute;o fre&iacute;",
    "freir friendo frito fr\u00edo fre\u00ed",
    "haber habiendo habido he hube",
    "hacer haciendo hecho hago hice",
//cc     "huir huyendo huido huyo hu&iacute;",
    "huir huyendo huido huyo hu\u00ed",
//cc     "jugar jugando jugado juego jugu&eacute;",
    "jugar jugando jugado juego jugu\u00e9",
//cc     "leer leyendo le&iacute;do leo le&iacute;",
    "leer leyendo le\u00eddo leo le\u00ed",
//cc     "llover lloviendo llovido llueve llov&iacute;",  // should probably use 3rd person for weather verbs
    "llover lloviendo llovido llueve llov\u00ed",  // should probably use 3rd person for weather verbs
    "maldecir maldeciendo maldecido maldigo maldije",
//cc     "morir muriendo muerto muero mor&iacute;",
    "morir muriendo muerto muero mor\u00ed",
//cc     "nevar nevando nevado nievo nev&iacute;",
    "nevar nevando nevado nievo nev\u00ed",
//cc     "o&iacute;r oyendo o&iacute;do oigo o&iacute;",
    "o\u00edr oyendo o\u00eddo oigo o\u00ed",
//cc     "oler oliendo olido huelo ol&iacute;",
    "oler oliendo olido huelo ol\u00ed",
//cc     "pensar pensando pensado pienso pens&eacute;",
    "pensar pensando pensado pienso pens\u00e9",
//cc     "pedir pidiendo pedido pido ped&iacute;",
    "pedir pidiendo pedido pido ped\u00ed",
    "poder pudiendo podido puedo pude",
    "poner poniendo puesto pongo puse",
//cc     "proveer proveyendo provisto proveo prove&iacute;",
    "proveer proveyendo provisto proveo prove\u00ed",
    "querer queriendo querido quiero quise",
//cc     "re&iacute;r riendo re&iacute;do r&iacute;o re&iacute;",
    "re\u00edr riendo re\u00eddo r\u00edo re\u00ed",
//cc     "solver solviendo suelto suelvo solv&iacute;",  // a root for resolver,absolver,disolver
    "solver solviendo suelto suelvo solv\u00ed",  // a root for resolver,absolver,disolver
//cc     "romper rompiendo roto rompo romp&iacute;",
    "romper rompiendo roto rompo romp\u00ed",
//cc     "saber sabiendo sabido s&eacute; supe",
    "saber sabiendo sabido s\u00e9 supe",
//cc     "salir saliendo salido salgo sal&iacute;",
    "salir saliendo salido salgo sal\u00ed",
//cc     "seguir siguiendo seguido sigo segu&iacute;",
    "seguir siguiendo seguido sigo segu\u00ed",
//cc     "sentir sintiendo sentido siento sent&iacute;",
    "sentir sintiendo sentido siento sent\u00ed",
    "tener teniendo tenido tengo tuve",
//cc     "traer trayendo tra&iacute;do traigo traje",
    "traer trayendo tra\u00eddo traigo traje",
//cc     "valer valiendo valido valgo val&iacute;",
    "valer valiendo valido valgo val\u00ed",
    "venir veniendo venido vengo vine",
//cc     "ver viendo visto veo v&iacute;",
    "ver viendo visto veo v\u00ed",
//cc     "vestir vistiendo vestido visto vest&iacute;",
    "vestir vistiendo vestido visto vest\u00ed",
//cc     "volver volviendo vuelto vuelvo volv&iacute;",
    "volver volviendo vuelto vuelvo volv\u00ed",

        };

        initialize(paradigms, paradigmTable);

  // Define the rules:


  String[] rootRules = {//":unnamed",  // For unknown words (root-rules)

   // check whether form is in the Verb Lookup Table
 
   ".$Vowel #  -> TRY(!irrVerb)_",  //  (vine)

   // test input as INFINITIVE
   ".$Vowel + a r -> (:reg-ar-paradigm)_",  // (hablar)
   ".$Letter + e r -> (:reg-er-paradigm)_", // (comer)
   ".$Letter + i r -> (:reg-ir-paradigm)_", // (vivir)

   // test REFLEXIVE INFINITIVE
   ".$Vowel a|e|i r + s e -> TRY(!irrVerb)_,se",  // ()
   ".$Vowel + a r s e -> (:ar-reflex-imperative-gerund)_",    // (enojarse)
   ".$Vowel + e r s e -> (:er-reflex-imperative-gerund)_",    // (ponerse)
   ".$Vowel + i r s e -> (:ir-reflex-imperative-gerund)_",    // (dormirse)

   // test input as -ando PROGRESSIVE or -ado PARTICIPLE
   ".$Vowel a n d o # -> TRY(!irrVerb)_,s",      //  (pensando,pensado)
   ".$Letter + a n d o -> (:reg-ar-paradigm)_",  // (habladando,hablado)
   ".$Letter a d + o|a ?s -> TRY(!irrVerb)o",    // (pensada)
   ".$Letter + a d o|a ?s -> (:reg-ar-paradigm)_",  // (hablado,cuchada) Note: nouns meaning 'ful', i.e, 'spoonful'

   // test input as -iendo PROGRESSIVE
   ".$Letter i e n d o # -> TRY(!irrVerb)_",  //  (teniendo)
   ".$Letter + i e n d o -> (:ambig-er-ir-paradigm)_",  // (comiendo,viviendo)

   // test input as -ido PARTICIPLE
//cc    ".$Vowel i|&iacute; d + a|o ?s -> TRY(!irrVerb)o",  //  (tenido,re&iacute;do)
   ".$Vowel i|\u00ed d + a|o ?s -> TRY(!irrVerb)o",  //  (tenido,re\u00eddo)
//cc    ".$Vowel + i|&iacute; d a|o ?s -> (:ambig-er-ir-paradigm)_",  //  (comido)
   ".$Vowel + i|\u00ed d a|o ?s -> (:ambig-er-ir-paradigm)_",  //  (comido)

   // a few very irregular forms
   "s < e > p + a ?n|s -> TRY(!irrVerb)<u>/_e",   // (saber subj pres)
   "s < e > p + a m o s -> TRY(!irrVerb)<u>/_e",  // (saber subj pres)
//cc    "s < e > p + &aacute; i s -> TRY(!irrVerb)<u>/_e",    // (saber subj pres)
   "s < e > p + \u00e1 i s -> TRY(!irrVerb)<u>/_e",    // (saber subj pres)
   "h i + z o -> TRY(!irrVerb)ce",                // (hacer 3rd pret)
   "h a + y a ?n|s -> TRY(!irrVerb)ber",     // (haber subj pres)
//cc    "h a + y &aacute; i s -> TRY(!irrVerb)ber",      // (haber subj pres)
   "h a + y \u00e1 i s -> TRY(!irrVerb)ber",      // (haber subj pres)
   "h a + y a m o s -> TRY(!irrVerb)ber",    // (haber subj pres)
   "h + e -> TRY(!irrVerb)aber",             // (haber pres indic)
   "h + e m o s -> TRY(!irrVerb)aber",       // (haber pres indic)
   "h + a ?n|s -> TRY(!irrVerb)aber",        // (haber pres indic)
//cc    "d + i r &eacute; -> TRY(!irrVerb)ecir",         // (decir fut indic)
   "d + i r \u00e9 -> TRY(!irrVerb)ecir",         // (decir fut indic)
//cc    "d + i r &aacute; ?n|s -> TRY(!irrVerb)ecir",    // (decir fut indic)
   "d + i r \u00e1 ?n|s -> TRY(!irrVerb)ecir",    // (decir fut indic)
   "d + i r e m o s -> TRY(!irrVerb)ecir",   // (decir fut indic)
//cc    "d + i r &eacute; i s -> TRY(!irrVerb)ecir",     // (decir fut indic)
   "d + i r \u00e9 i s -> TRY(!irrVerb)ecir",     // (decir fut indic)
//cc    "d + i r &iacute; a ?n|s -> TRY(!irrVerb)ecir",  // (decir imperf)
   "d + i r \u00ed a ?n|s -> TRY(!irrVerb)ecir",  // (decir imperf)
//cc    "d + i r &iacute; a m o s -> TRY(!irrVerb)ecir", // (decir imperf)
   "d + i r \u00ed a m o s -> TRY(!irrVerb)ecir", // (decir imperf)
//cc    "d + i r &iacute; a i s -> TRY(!irrVerb)ecir",   // (decir imperf)
   "d + i r \u00ed a i s -> TRY(!irrVerb)ecir",   // (decir imperf)

   // test FUTURE -ar,-er,-ir forms

//cc    ".$Vowel .$Consonant + ?d r &eacute; -> TRY(!irrVerb)ir,TRY(!irrVerb)er",         // (sabr&eacute;,saldr&eacute;)
   ".$Vowel .$Consonant + ?d r \u00e9 -> TRY(!irrVerb)ir,TRY(!irrVerb)er",         // (sabr\u00e9,saldr\u00e9)
//cc    ".$Vowel .$Consonant + ?d r &aacute; ?s|n -> TRY(!irrVerb)ir,TRY(!irrVerb)er",    // (sabr&aacute;s,saldr&aacute;n)
   ".$Vowel .$Consonant + ?d r \u00e1 ?s|n -> TRY(!irrVerb)ir,TRY(!irrVerb)er",    // (sabr\u00e1s,saldr\u00e1n)
//cc    ".$Vowel .$Consonant + ?d r &eacute; i s -> TRY(!irrVerb)ir,TRY(!irrVerb)er",     // (sabr&eacute;is,saldr&eacute;is)
   ".$Vowel .$Consonant + ?d r \u00e9 i s -> TRY(!irrVerb)ir,TRY(!irrVerb)er",     // (sabr\u00e9is,saldr\u00e9is)
   ".$Vowel .$Consonant + ?d r e m o s -> TRY(!irrVerb)ir,TRY(!irrVerb)er",   // (sabremos,saldremos)

//cc    ".$Vowel .$Consonant a|e|i r + &eacute; -> TRY(!irrVerb)_",    // (romper&eacute;,reir&eacute;) Note - need to handle infinitive re&iacute;r
   ".$Vowel .$Consonant a|e|i r + \u00e9 -> TRY(!irrVerb)_",    // (romper\u00e9,reir\u00e9) Note - need to handle infinitive re\u00edr
//cc    ".$Vowel .$Consonant a|e|i r + &eacute; -> (:reg-paradigm)_",  // (hablar&eacute;,comer&eacute;,vivir&eacute;)
   ".$Vowel .$Consonant a|e|i r + \u00e9 -> (:reg-paradigm)_",  // (hablar\u00e9,comer\u00e9,vivir\u00e9)

//cc    ".$Vowel .$Consonant a|e|i r + &aacute; ?s|n -> TRY(!irrVerb)_",     // (dar&aacute;s,romper&aacute;n,dormir&aacute;)
   ".$Vowel .$Consonant a|e|i r + \u00e1 ?s|n -> TRY(!irrVerb)_",     // (dar\u00e1s,romper\u00e1n,dormir\u00e1)
//cc    ".$Vowel .$Consonant a|e|i r + &aacute; ?s|n -> (:reg-paradigm)_",   // (hablar&aacute;,comer&aacute;n,vivir&aacute;s)
   ".$Vowel .$Consonant a|e|i r + \u00e1 ?s|n -> (:reg-paradigm)_",   // (hablar\u00e1,comer\u00e1n,vivir\u00e1s)

//cc    ".$Vowel .$Consonant a|e|i r + &eacute; i s  -> TRY(!irrVerb)_",     // (errar&eacute;is,romer&eacute;is,dormir&eacute;is)
   ".$Vowel .$Consonant a|e|i r + \u00e9 i s  -> TRY(!irrVerb)_",     // (errar\u00e9is,romer\u00e9is,dormir\u00e9is)
//cc    ".$Vowel .$Consonant a|e|i r + &eacute; i s  -> (:reg-paradigm)_",   // (hablar&eacute;is,comer&eacute;is,vivir&eacute;is)
   ".$Vowel .$Consonant a|e|i r + \u00e9 i s  -> (:reg-paradigm)_",   // (hablar\u00e9is,comer\u00e9is,vivir\u00e9is)

   ".$Vowel .$Consonant a|e|i r + e m o s -> TRY(!irrVerb)_",    // (daremos,romperemos,dormiremos)
   ".$Vowel .$Consonant a|e|i r + e m o s -> (:reg-paradigm)_",  // (hablaremos,comeremos,viviremos)

//cc    ".$Vowel .$Consonant + &eacute; -> (:reg-ar-paradigm)_",  // (habl&eacute;) 1st sg preterite -ar verbs
   ".$Vowel .$Consonant + \u00e9 -> (:reg-ar-paradigm)_",  // (habl\u00e9) 1st sg preterite -ar verbs

   // test  CONDITIONAL -ar,-er,-ir forms
//cc    ".$Vowel .$Consonant + ?d r &iacute; a ?n|s -> TRY(!irrVerb)ir,TRY(!irrVerb)er",  // (sabr&iacute;a,saldr&iacute;an)
   ".$Vowel .$Consonant + ?d r \u00ed a ?n|s -> TRY(!irrVerb)ir,TRY(!irrVerb)er",  // (sabr\u00eda,saldr\u00edan)
//cc    ".$Vowel .$Consonant + ?d r &iacute; a m o s -> TRY(!irrVerb)er,TRY(!irrVerb)ir", // (saldr&iacute;amos,sabr&iacute;amos)
   ".$Vowel .$Consonant + ?d r \u00ed a m o s -> TRY(!irrVerb)er,TRY(!irrVerb)ir", // (saldr\u00edamos,sabr\u00edamos)
//cc    ".$Vowel .$Consonant + ?d r &iacute; a i s -> TRY(!irrVerb)er,TRY(!irrVerb)ir",   // (saldr&iacute;ais,sabr&iacute;ais)
   ".$Vowel .$Consonant + ?d r \u00ed a i s -> TRY(!irrVerb)er,TRY(!irrVerb)ir",   // (saldr\u00edais,sabr\u00edais)
   
//cc    ".$Vowel .$Consonant a|e|i r + &iacute; a ?n|s  -> TRY(!irrVerb)_",     // (pedir&iacute;a,traer&iacute;an)
   ".$Vowel .$Consonant a|e|i r + \u00ed a ?n|s  -> TRY(!irrVerb)_",     // (pedir\u00eda,traer\u00edan)
//cc    ".$Vowel .$Consonant a|e|i r + &iacute; a ?n|s  -> (:reg-paradigm)_",   // (habar&iacute;a,comer&iacute;as,vivir&iacute;an
   ".$Vowel .$Consonant a|e|i r + \u00ed a ?n|s  -> (:reg-paradigm)_",   // (habar\u00eda,comer\u00edas,vivir\u00edan

//cc    ".$Vowel .$Consonant a|e|i r + &iacute; a i s -> TRY(!irrVerb)_",    // (pedir&iacute;ais,traer&iacute;ais)
   ".$Vowel .$Consonant a|e|i r + \u00ed a i s -> TRY(!irrVerb)_",    // (pedir\u00edais,traer\u00edais)
//cc    ".$Vowel .$Consonant a|e|i r + &iacute; a i s -> (:reg-paradigm)_",  // (hablar&iacute;ais,comer&iacute;ais,vivir&iacute;ais)
   ".$Vowel .$Consonant a|e|i r + \u00ed a i s -> (:reg-paradigm)_",  // (hablar\u00edais,comer\u00edais,vivir\u00edais)

//cc    ".$Vowel .$Consonant a|e|i r + &iacute; a m o s -> TRY(!irrVerb)_",    // (pedir&iacute;amos,traer&iacute;amos)
   ".$Vowel .$Consonant a|e|i r + \u00ed a m o s -> TRY(!irrVerb)_",    // (pedir\u00edamos,traer\u00edamos)
//cc    ".$Vowel .$Consonant a|e|i r + &iacute; a m o s -> (:reg-paradigm)_",  // (hablar&iacute;amos,comer&iacute;amos,vivir&iacute;amos)
   ".$Vowel .$Consonant a|e|i r + \u00ed a m o s -> (:reg-paradigm)_",  // (hablar\u00edamos,comer\u00edamos,vivir\u00edamos)

 // test IMPERFECT SUBJUNCTIVE -er/-ir verbs
   ".$Vowel .$Consonant + i e r a ?n|s -> TRY(!irrVerb)er,TRY(!irrVerb)ir",  // (tuviera,salieran)
   ".$Vowel .$Consonant + i e r a ?n|s -> (:ambig-er-ir-paradigm)_",         // (comieras,vivieran)

//cc    ".$Vowel .$Consonant + i &eacute; r a m o s -> TRY(!irrVerb)er,TRY(!irrVerb)ir", // (sigui&eacute;ramos,oli&eacute;ramos)
   ".$Vowel .$Consonant + i \u00e9 r a m o s -> TRY(!irrVerb)er,TRY(!irrVerb)ir", // (sigui\u00e9ramos,oli\u00e9ramos)
//cc    ".$Vowel .$Consonant + i &eacute; r a m o s -> (:ambig-er-ir-paradigm)_",        // (comi&eacute;ramos,vivi&eacute;ramos)
   ".$Vowel .$Consonant + i \u00e9 r a m o s -> (:ambig-er-ir-paradigm)_",        // (comi\u00e9ramos,vivi\u00e9ramos)

   "< ?i > .$Consonant + i e r a i s -> TRY(!irrVerb)er,TRY(!irrVerb)<e>/_ir,TRY(!irrVerb)ir",  // (olierais,pidierais)
   ".$Vowel .$Consonant + i e r a i s -> (:ambig-er-ir-paradigm)_",          // (vivierais,comierais)

   ".$Vowel .$Consonant + i e s e ?n|s -> TRY(!irrVerb)er,TRY(!irrVerb)ir",  // (tuviese,saliesen)
   ".$Vowel .$Consonant + i e s e ?n|s -> (:ambig-er-ir-paradigm)_",         // (viviesen,comieses)

//cc    ".$Vowel .$Consonant + i &eacute; s e m o s -> TRY(!irrVerb)er,TRY(!irrVerb)ir", // (sali&eacute;semos,tuvi&eacute;semos)
   ".$Vowel .$Consonant + i \u00e9 s e m o s -> TRY(!irrVerb)er,TRY(!irrVerb)ir", // (sali\u00e9semos,tuvi\u00e9semos)
//cc    ".$Vowel .$Consonant + i &eacute; s e m o s -> (:ambig-er-ir-paradigm)_",        // (comi&eacute;semos,vivi&eacute;semos)
   ".$Vowel .$Consonant + i \u00e9 s e m o s -> (:ambig-er-ir-paradigm)_",        // (comi\u00e9semos,vivi\u00e9semos)

   ".$Vowel .$Consonant + i e s e i s -> TRY(!irrVerb)er,TRY(!irrVerb)ir",   // (tuvieseis,salieseis)
   ".$Vowel .$Consonant + i e s e i s -> (:ambig-er-ir-paradigm)_",          // (comieseis,vivieseis)

   // test PRESENT SUBJUNCTIVE/PRESENT INDICATIVE
//cc    "s < e > p + &aacute; i s -> TRY(!irrVerb)<u>/_e",      // (sep&aacute;is>
   "s < e > p + \u00e1 i s -> TRY(!irrVerb)<u>/_e",      // (sep\u00e1is>
//cc    ".$Letter + c &eacute; i s -> TRY(!irrVerb)zar",        // (almorc&eacute;is)
   ".$Letter + c \u00e9 i s -> TRY(!irrVerb)zar",        // (almorc\u00e9is)
//cc    ".$Letter + c &eacute; i s -> (:reg-ar-paradigm)z",     // (abrac&eacute;is)
   ".$Letter + c \u00e9 i s -> (:reg-ar-paradigm)z",     // (abrac\u00e9is)
//cc    ".$Letter + q u &eacute; i s -> TRY(!irrVerb)car",      // (busqu&eacute;is)
   ".$Letter + q u \u00e9 i s -> TRY(!irrVerb)car",      // (busqu\u00e9is)
//cc    ".$Letter + q u &eacute; i s -> (:reg-ar-paradigm)c",   // (toqu&eacute;is)  
   ".$Letter + q u \u00e9 i s -> (:reg-ar-paradigm)c",   // (toqu\u00e9is)  
//cc    ".$Letter + g u &eacute; i s -> TRY(!irrVerb)gar",      // ()
   ".$Letter + g u \u00e9 i s -> TRY(!irrVerb)gar",      // ()
//cc    ".$Letter + g u &eacute; i s -> (:reg-ar-paradigm)g",   // (pagu&eacute;is)  
   ".$Letter + g u \u00e9 i s -> (:reg-ar-paradigm)g",   // (pagu\u00e9is)  
//cc    ".$Letter + g &uuml; &eacute; i s -> TRY(!irrVerb)guar",     // ()
   ".$Letter + g \u00fc \u00e9 i s -> TRY(!irrVerb)guar",     // ()
//cc    ".$Letter + g &uuml; &eacute; i s -> (:reg-ar-paradigm)gu",  // (averig&uuml;&eacute;is)  
   ".$Letter + g \u00fc \u00e9 i s -> (:reg-ar-paradigm)gu",  // (averig\u00fc\u00e9is)  

//cc    ".$Vowel .$Consonant + &aacute;|&eacute; i s -> TRY(!irrVerb)ar,TRY(!irrVerb)er,TRY(!irrVerb)ir",   // (abr&aacute;is,pod&eacute;is,and&eacute;is)
   ".$Vowel .$Consonant + \u00e1|\u00e9 i s -> TRY(!irrVerb)ar,TRY(!irrVerb)er,TRY(!irrVerb)ir",   // (abr\u00e1is,pod\u00e9is,and\u00e9is)
//cc    ".$Vowel .$Consonant + &aacute; i s -> (:ambig-ar-er-ir-paradigm)_",  // (habl&aacute;is,com&aacute;is,viv&aacute;is)
   ".$Vowel .$Consonant + \u00e1 i s -> (:ambig-ar-er-ir-paradigm)_",  // (habl\u00e1is,com\u00e1is,viv\u00e1is)
//cc    ".$Vowel .$Consonant + &eacute; i s -> (:ambig-ar-er-paradigm)_",     // (habl&eacute;is,com&eacute;is) 
   ".$Vowel .$Consonant + \u00e9 i s -> (:ambig-ar-er-paradigm)_",     // (habl\u00e9is,com\u00e9is) 

   ".$Letter + c e ?s|n -> TRY(!irrVerb)zo",           // (almuerce -> almuerzo)
//cc    ".$Letter + c e|&eacute; ?s|n -> (:reg-ar-paradigm)z",     // (abrace -> abraz-ar)
   ".$Letter + c e|\u00e9 ?s|n -> (:reg-ar-paradigm)z",     // (abrace -> abraz-ar)
   ".$Letter + q u e ?s|n -> TRY(!irrVerb)co",         // (busque -> busco)
//cc    ".$Letter + q u e|&eacute; ?s|n -> (:reg-ar-paradigm)c",   // (toque -> toc-ar)
   ".$Letter + q u e|\u00e9 ?s|n -> (:reg-ar-paradigm)c",   // (toque -> toc-ar)
   ".$Letter + g u e ?s|n -> TRY(!irrVerb)go",         // ()
//cc    ".$Letter + g u e|&eacute; ?s|n -> (:reg-ar-paradigm)g",   // (pague -> pag-ar)
   ".$Letter + g u e|\u00e9 ?s|n -> (:reg-ar-paradigm)g",   // (pague -> pag-ar)
//cc    ".$Letter + g &uuml; e ?s|n -> TRY(!irrVerb)guo",        // ()
   ".$Letter + g \u00fc e ?s|n -> TRY(!irrVerb)guo",        // ()
//cc    ".$Letter + g &uuml; e|&eacute; ?s|n -> (:reg-ar-paradigm)gu",  // (averig&uuml;es -> averigu-ar)
   ".$Letter + g \u00fc e|\u00e9 ?s|n -> (:reg-ar-paradigm)gu",  // (averig\u00fces -> averigu-ar)


   // test IMPERFECT/FUTURE SUBJUNCTIVE -ar verbs
   ".$Vowel .$Consonant + a r a|e ?n|s  -> TRY(!irrVerb)ar",      // (contaren,contara))
   ".$Vowel .$Consonant + a r a|e ?n|s  -> (:reg-ar-paradigm)_",  // (pagaren,hablaras)

//cc    ".$Vowel .$Consonant + &aacute; r a|e m o s -> TRY(!irrVerb)ar",      // (jug&aacute;ramos,jug&aacute;remos)
   ".$Vowel .$Consonant + \u00e1 r a|e m o s -> TRY(!irrVerb)ar",      // (jug\u00e1ramos,jug\u00e1remos)
//cc    ".$Vowel .$Consonant + &aacute; r a|e m o s -> (:reg-ar-paradigm)_",  // (habl&aacute;remos)
   ".$Vowel .$Consonant + \u00e1 r a|e m o s -> (:reg-ar-paradigm)_",  // (habl\u00e1remos)

   ".$Vowel .$Consonant + a r a|e i s -> TRY(!irrVerb)ar",        // (pensarais,contareis)
   ".$Vowel .$Consonant + a r a|e i s -> (:reg-ar-paradigm)_",    // (hablarais,hablareis)

   ".$Vowel .$Consonant + a s e ?n|s  -> TRY(!irrVerb)ar",        // (contases)
   ".$Vowel .$Consonant + a s e ?n|s  -> (:reg-ar-paradigm)_",    // (ganase)

//cc    ".$Vowel .$Consonant + &aacute; s e m o s -> TRY(!irrVerb)ar",        // (pens&aacute;semos)
   ".$Vowel .$Consonant + \u00e1 s e m o s -> TRY(!irrVerb)ar",        // (pens\u00e1semos)
//cc    ".$Vowel .$Consonant + &aacute; s e m o s -> (:reg-ar-paradigm)_",    // (habl&aacute;semos)
   ".$Vowel .$Consonant + \u00e1 s e m o s -> (:reg-ar-paradigm)_",    // (habl\u00e1semos)

   ".$Vowel .$Consonant + a s e i s -> TRY(!irrVerb)ar",          // (pensaseis)
   ".$Vowel .$Consonant + a s e i s -> (:reg-ar-paradigm)_",      // (pagaseis)

   // test FUTURE SUBJUNCTIVE -ar/er-ir verbs
//cc    ".$Letter + ?i|y e r e ?n|s -> TRY(!irrVerb)&iacute;,TRY(!irrVerb)e",    // (tuve,abr&iacute;)
   ".$Letter + ?i|y e r e ?n|s -> TRY(!irrVerb)\u00ed,TRY(!irrVerb)e",    // (tuve,abr\u00ed)
   ".$Letter + ?i|y e r e ?n|s -> (:ambig-er-ir-paradigm)_",         // (comiere)

//cc    ".$Letter + ?i|y &eacute; r e m o s -> TRY(!irrVerb)&iacute;,TRY(!irrVerb)e",   // (tuve -> tuvi&eacute;remos)
   ".$Letter + ?i|y \u00e9 r e m o s -> TRY(!irrVerb)\u00ed,TRY(!irrVerb)e",   // (tuve -> tuvi\u00e9remos)
//cc    ".$Letter + ?i|y &eacute; r e m o s -> (:ambig-er-ir-paradigm)_",        // (viv&eacute;remos)
   ".$Letter + ?i|y \u00e9 r e m o s -> (:ambig-er-ir-paradigm)_",        // (viv\u00e9remos)

//cc    ".$Letter + ?i|y e r e i s -> TRY(!irrVerb)&iacute;,TRY(!irrVerb)e",     // (tuvereis)
   ".$Letter + ?i|y e r e i s -> TRY(!irrVerb)\u00ed,TRY(!irrVerb)e",     // (tuvereis)
   ".$Letter + ?i|y e r e i s -> (:ambig-er-ir-paradigm)_",          // (comereis)

//cc    ".$Letter + a r e ?n|s -> TRY(!irrVerb)&oacute;",        // (contaren)
   ".$Letter + a r e ?n|s -> TRY(!irrVerb)\u00f3",        // (contaren)
   ".$Letter + a r e ?n|s -> (:reg-ar-paradigm)_",   // (hablare,hablaren)

//cc    ".$Letter + &aacute; r e m o s -> TRY(!irrVerb)&oacute;",        // (pagaremos)
   ".$Letter + \u00e1 r e m o s -> TRY(!irrVerb)\u00f3",        // (pagaremos)
//cc    ".$Letter + &aacute; r e m o s -> (:reg-ar-paradigm)_",   // (hablaremos)
   ".$Letter + \u00e1 r e m o s -> (:reg-ar-paradigm)_",   // (hablaremos)

//cc    ".$Letter + a r e i s -> TRY(!irrVerb)&oacute;",         // (pensareis)
   ".$Letter + a r e i s -> TRY(!irrVerb)\u00f3",         // (pensareis)
   ".$Letter + a r e i s -> (:reg-ar-paradigm)_",    // (hablareis)

   // pres -er, pres-subj -ar, future -ar/er/ir
   ".$Vowel + g u e m o s -> TRY(!irrVerb)gar",       // (gu -> g)
   ".$Vowel + g u e m o s -> (:reg-ar-paradigm)g",    // (paguemos -> pag(ar))
//cc    ".$Vowel + g &uuml; e m o s -> TRY(!irrVerb)guar",      // (g&uuml; -> gu)
   ".$Vowel + g \u00fc e m o s -> TRY(!irrVerb)guar",      // (g\u00fc -> gu)
//cc    ".$Vowel + g &uuml; e m o s -> (:reg-ar-paradigm)gu",   // (averig&uuml;emos)
   ".$Vowel + g \u00fc e m o s -> (:reg-ar-paradigm)gu",   // (averig\u00fcemos)
   ".$Vowel + q u e m o s -> TRY(!irrVerb)car",       // (qu -> c)
   ".$Vowel + q u e m o s -> (:reg-ar-paradigm)c",    // (toquemos -> toc(ar))
   ".$Vowel + c e m o s -> TRY(!irrVerb)zar",         // (almorcemos)
   ".$Vowel + c e m o s -> (:reg-ar-paradigm)z",      // (abracemos -> abraz(ar))

   ".$Vowel + e m o s -> TRY(!irrVerb)er,TRY(!irrVerb)ar,TRY(!irrVerb)_", 
   ".$Vowel + e m o s -> (:ambig-ar-er-paradigm)_",   // (hablemos,comemos)

   // pret -ir/-er
//cc    ".$Vowel + i m o s -> TRY(!irrVerb)e,TRY(!irrVerb)&iacute;",     // (salimos,hicimos)
   ".$Vowel + i m o s -> TRY(!irrVerb)e,TRY(!irrVerb)\u00ed",     // (salimos,hicimos)
   ".$Vowel + i m o s -> (:ambig-er-ir-paradigm)_",          // (comimos,vivimos)

   ".$Vowel + a s t e -> TRY(!irrVerb)ar",      // (jugaste)
   ".$Vowel + a s t e -> (:reg-ar-paradigm)_",  // (hablaste)

   "< h i c > + i s t e -> TRY(!irrVerb)<hac>/_er",          // (hiciste)
//cc    ".$Vowel + i s t e -> TRY(!irrVerb)e,TRY(!irrVerb)&iacute;",     // (tuviste,sentiste) try to form 3rd sg preterite
   ".$Vowel + i s t e -> TRY(!irrVerb)e,TRY(!irrVerb)\u00ed",     // (tuviste,sentiste) try to form 3rd sg preterite
   
   // IMPERFECT -er/-ir
//cc    ".$Vowel + &iacute; a m o s -> TRY(!irrVerb)ir,TRY(!irrVerb)er",
   ".$Vowel + \u00ed a m o s -> TRY(!irrVerb)ir,TRY(!irrVerb)er",
//cc    ".$Vowel + &iacute; a m o s -> TRY(!irrVerb)ir,TRY(!irrVerb)er",
   ".$Vowel + \u00ed a m o s -> TRY(!irrVerb)ir,TRY(!irrVerb)er",
 
   // PRETERITE forms
   ".$Vowel .$Consonant + a s t e i s -> TRY(!irrVerb)ar",      // (jugasteis))
   ".$Vowel .$Consonant + a s t e i s -> (:reg-ar-paradigm)_",  // (hablasteis)

//cc    ".$Vowel .$Consonant + i s t e i s -> TRY(!irrVerb)&iacute;,TRY(!irrVerb)e",    // (abristeis,supe)
   ".$Vowel .$Consonant + i s t e i s -> TRY(!irrVerb)\u00ed,TRY(!irrVerb)e",    // (abristeis,supe)
   ".$Vowel .$Consonant + i s t e i s -> (:ambig-er-ir-paradigm)_",         // (comisteis)

   ".$Vowel .$Consonant + a r o n -> TRY(!irrVerb)ar",     // (jugaron)
   ".$Vowel .$Consonant + a r o n -> (:reg-ar-paradigm)_", // (hablaron)

   ".$Vowel j + e r o n -> TRY(!irrVerb)e",  // dijeron, trajeron
   ".$Vowel j + i s t e -> TRY(!irrVerb)e",  // dijiste trajiste
   ".$Vowel j + o -> TRY(!irrVerb)e",        // dijo, trajo

//cc    "< u > +$Consonant + i e r o n -> TRY(!irrVerb)<o>/_&iacute;,TRY(!irrVerb)e",  // (supieron,supe,murieron,mor&iacute;)
   "< u > +$Consonant + i e r o n -> TRY(!irrVerb)<o>/_\u00ed,TRY(!irrVerb)e",  // (supieron,supe,murieron,mor\u00ed)
//cc    "< i > +$Consonant + i e r o n -> TRY(!irrVerb)<e>/_&iacute;,TRY(!irrVerb)e",  // (sinieron,sent&iacute;)
   "< i > +$Consonant + i e r o n -> TRY(!irrVerb)<e>/_\u00ed,TRY(!irrVerb)e",  // (sinieron,sent\u00ed)
//cc    "$Letter + i e r o n -> TRY(!irrVerb)&iacute;,TRY(!irrVerb)e",                 // (vieron,valieron)
   "$Letter + i e r o n -> TRY(!irrVerb)\u00ed,TRY(!irrVerb)e",                 // (vieron,valieron)
   ".$Vowel .$Consonant + i e r o n -> (:ambig-er-ir-paradigm)_",          // (comieron,vivieron)

   // IMPERFECT -ar forms
   ".$Vowel .$Consonant + a b a i s -> TRY(!irrVerb)o",       // (jugabais)
   ".$Vowel .$Consonant + a b a i s -> (:reg-ar-paradigm)_",  // (hablabais)

   ".$Vowel .$Consonant + a b a ?n|s -> TRY(!irrVerb)o",        // (jugabas)
   ".$Vowel .$Consonant + a b a ?n|s -> (:reg-ar-paradigm)_",   // (hablaban)

//cc    ".$Vowel .$Consonant + &aacute; b a m o s -> TRY(!irrVerb)o",       // (and&aacute;bamos)
   ".$Vowel .$Consonant + \u00e1 b a m o s -> TRY(!irrVerb)o",       // (and\u00e1bamos)
//cc    ".$Vowel .$Consonant + &aacute; b a m o s -> (:reg-ar-paradigm)_",  // (habl&aacute;bamos)
   ".$Vowel .$Consonant + \u00e1 b a m o s -> (:reg-ar-paradigm)_",  // (habl\u00e1bamos)

   // test for IMPERFECT -er/-ir forms
//cc    ".$Vowel .$Consonant + &iacute; a ?n|s -> TRY(!irrVerb)ir,TRY(!irrVerb)er",     // (abr&iacute;a,romp&iacute;a)
   ".$Vowel .$Consonant + \u00ed a ?n|s -> TRY(!irrVerb)ir,TRY(!irrVerb)er",     // (abr\u00eda,romp\u00eda)
//cc    ".$Vowel .$Consonant + &iacute; a ?n|s -> (:ambig-er-ir-paradigm)_",            // (com&iacute;a,viv&iacute;an)
   ".$Vowel .$Consonant + \u00ed a ?n|s -> (:ambig-er-ir-paradigm)_",            // (com\u00eda,viv\u00edan)

   // PRETERITE -er/-ir
//cc    "< u > $Consonant + i &oacute; -> TRY(!irrVerb)<o>/_&iacute;",          // (muri&oacute; -> mor&iacute;)
   "< u > $Consonant + i \u00f3 -> TRY(!irrVerb)<o>/_\u00ed",          // (muri\u00f3 -> mor\u00ed)
//cc    "< i > $Consonant + i &oacute; -> TRY(!irrVerb)<e>/_&iacute;",          // (sinti&oacute; -> sent&iacute;)
   "< i > $Consonant + i \u00f3 -> TRY(!irrVerb)<e>/_\u00ed",          // (sinti\u00f3 -> sent\u00ed)
//cc    "< i > $Consonant + i &oacute; -> (:reg-ir-paradigm)<e>,(:reg-ir-paradigm)<i>",    // (hiri&oacute; -> herir,vivi&oacute;,vivir)
   "< i > $Consonant + i \u00f3 -> (:reg-ir-paradigm)<e>,(:reg-ir-paradigm)<i>",    // (hiri\u00f3 -> herir,vivi\u00f3,vivir)
//cc    ".$Letter + i &oacute; -> TRY(!irrVerb)&iacute;",                       // (vi&oacute; -> v&iacute;)
   ".$Letter + i \u00f3 -> TRY(!irrVerb)\u00ed",                       // (vi\u00f3 -> v\u00ed)
//cc    ".$Vowel .$Consonant + i &oacute; -> (:ambig-er-ir-paradigm)_",  // (comi&oacute;,vivi&oacute;)
   ".$Vowel .$Consonant + i \u00f3 -> (:ambig-er-ir-paradigm)_",  // (comi\u00f3,vivi\u00f3)

   // PRETERITE -ar
//cc    ".$Letter g + &oacute; -> TRY(!irrVerb)u&eacute;",               // (jug&oacute; -> jugu&eacute;)
   ".$Letter g + \u00f3 -> TRY(!irrVerb)u\u00e9",               // (jug\u00f3 -> jugu\u00e9)
//cc    ".$Letter + z &oacute; -> TRY(!irrVerb)c&eacute;",               // (almorz&oacute; -> almorc&eacute;)
   ".$Letter + z \u00f3 -> TRY(!irrVerb)c\u00e9",               // (almorz\u00f3 -> almorc\u00e9)
//cc    ".$Letter + c &oacute; -> TRY(!irrVerb)qu&eacute;",              // (busc&oacute; -> busqu&eacute;)
   ".$Letter + c \u00f3 -> TRY(!irrVerb)qu\u00e9",              // (busc\u00f3 -> busqu\u00e9)
//cc    ".$Vowel .$Consonant + &oacute; -> TRY(!irrVerb)&eacute;",       // (pens&oacute; -> pens&eacute;)
   ".$Vowel .$Consonant + \u00f3 -> TRY(!irrVerb)\u00e9",       // (pens\u00f3 -> pens\u00e9)
//cc    ".$Vowel .$Consonant + &oacute; -> (:reg-ar-paradigm)_",  // (habl&oacute;)
   ".$Vowel .$Consonant + \u00f3 -> (:reg-ar-paradigm)_",  // (habl\u00f3)

//cc    ".$Vowel .$Consonant + &iacute; a m o s -> TRY(!irrVerb)er,TRY(!irrVerb)ir",      // (romp&iacute;amos,abr&iacute;amos)
   ".$Vowel .$Consonant + \u00ed a m o s -> TRY(!irrVerb)er,TRY(!irrVerb)ir",      // (romp\u00edamos,abr\u00edamos)
//cc    ".$Vowel .$Consonant + &iacute; a m o s -> (:ambig-er-ir-paradigm)_",             // (viv&iacute;amos,com&iacute;amos)
   ".$Vowel .$Consonant + \u00ed a m o s -> (:ambig-er-ir-paradigm)_",             // (viv\u00edamos,com\u00edamos)

//cc    ".$Vowel .$Consonant + &iacute; a i s -> TRY(!irrVerb)er,TRY(!irrVerb)ir",        // (romp&iacute;ais,abr&iacute;ais)
   ".$Vowel .$Consonant + \u00ed a i s -> TRY(!irrVerb)er,TRY(!irrVerb)ir",        // (romp\u00edais,abr\u00edais)
//cc    ".$Vowel .$Consonant + &iacute; a i s -> (:ambig-er-ir-paradigm)_",               // (viv&iacute;ais,com&iacute;ais)
   ".$Vowel .$Consonant + \u00ed a i s -> (:ambig-er-ir-paradigm)_",               // (viv\u00edais,com\u00edais)

   // pres -ar or pres-subj -er/-ir
   ".$Vowel $Consonant + z a m o s -> TRY(!irrVerb)zar,TRY(!irrVerb)cer,TRY(!irrVerb)cir", 
   ".$Vowel $Consonant + z a m o s -> (:reg-ar-paradigm)z,(:ambig-er-ir-paradigm)c",   // (venzamos -> vencer)
   ".$Vowel $Consonant + j a m o s -> TRY(!irrVerb)jar,TRY(!irrVerb)ger,TRY(!irrVerb)gir", // (cojamos)
   ".$Vowel $Consonant + j a m o s -> (:reg-ar-paradigm)j,(:ambig-er-ir-paradigm)g",       // (bajamos,dirijamos)
   ".$Vowel $Consonant + g a m o s -> TRY(!irrVerb)gar,TRY(!irrVerb)guir",        // (disingamos)
   ".$Vowel $Consonant + g a m o s -> (:reg-ar-paradigm)g,(:reg-ir-paradigm)gu",  // (negamos)
   ".$Vowel $Consonant + c a m o s -> TRY(!irrVerb)car,TRY(!irrVerb)quir",        // (delincamos)
   ".$Vowel $Consonant + c a m o s -> (:reg-ar-paradigm)c,(:reg-ir-paradigm)qu",  // (tocamos)
   ".$Vowel $Consonant + z c a m o s -> TRY(!irrVerb)cer",      // (conozcamos)
   ".$Vowel $Consonant + z c a m o s -> (:reg-er-paradigm)c",   // (crezcamos)
   ".$Vowel + a m o s -> TRY(!irrVerb)ar,TRY(!irrVerb)er,TRY(!irrVerb)ir", 
   ".$Vowel + a m o s -> (:ambig-ar-er-ir-paradigm)_",   // (hablamos,comamos,vivamos)

//cc    ".$Vowel + z c &aacute; i s -> TRY(!irrVerb)cer",                                    // (conozc&aacute;is)
   ".$Vowel + z c \u00e1 i s -> TRY(!irrVerb)cer",                                    // (conozc\u00e1is)
//cc    ".$Vowel + z c &aacute; i s -> (:reg-er-paradigm)c",                                 // (crezc&aacute;is)
   ".$Vowel + z c \u00e1 i s -> (:reg-er-paradigm)c",                                 // (crezc\u00e1is)
//cc    ".$Vowel + z &aacute; i s -> TRY(!irrVerb)zar,TRY(!irrVerb)cer,TRY(!irrVerb)cir",    // ()
   ".$Vowel + z \u00e1 i s -> TRY(!irrVerb)zar,TRY(!irrVerb)cer,TRY(!irrVerb)cir",    // ()
//cc    ".$Vowel + z &aacute; i s -> (:reg-ar-paradigm)z,(:ambig-er-ir-paradigm)c",          // (venz&aacute;is)
   ".$Vowel + z \u00e1 i s -> (:reg-ar-paradigm)z,(:ambig-er-ir-paradigm)c",          // (venz\u00e1is)
//cc    ".$Vowel + j &aacute; i s -> TRY(!irrVerb)jar,TRY(!irrVerb)ger,TRY(!irrVerb)gir",    // (coj&aacute;is)
   ".$Vowel + j \u00e1 i s -> TRY(!irrVerb)jar,TRY(!irrVerb)ger,TRY(!irrVerb)gir",    // (coj\u00e1is)
//cc    ".$Vowel + j &aacute; i s -> (:reg-ar-paradigm)j,(:ambig-er-ir-paradigm)g",          // (baj&aacute;is,dirij&aacute;is)
   ".$Vowel + j \u00e1 i s -> (:reg-ar-paradigm)j,(:ambig-er-ir-paradigm)g",          // (baj\u00e1is,dirij\u00e1is)
//cc    ".$Vowel + g &aacute; i s -> TRY(!irrVerb)gar,TRY(!irrVerb)guir",                    // (disting&aacute;is)
   ".$Vowel + g \u00e1 i s -> TRY(!irrVerb)gar,TRY(!irrVerb)guir",                    // (disting\u00e1is)
//cc    ".$Vowel + g &aacute; i s -> (:reg-ar-paradigm)g,(:reg-ir-paradigm)gu",              // (neg&aacute;is)
   ".$Vowel + g \u00e1 i s -> (:reg-ar-paradigm)g,(:reg-ir-paradigm)gu",              // (neg\u00e1is)
//cc    ".$Vowel + c &aacute; i s -> TRY(!irrVerb)car,TRY(!irrVerb)quir",                    // (delinc&aacute;is)
   ".$Vowel + c \u00e1 i s -> TRY(!irrVerb)car,TRY(!irrVerb)quir",                    // (delinc\u00e1is)
//cc    ".$Vowel + c &aacute; i s -> (:reg-ar-paradigm)c,(:reg-ir-paradigm)qu",              // (toc&aacute;is)
   ".$Vowel + c \u00e1 i s -> (:reg-ar-paradigm)c,(:reg-ir-paradigm)qu",              // (toc\u00e1is)
//cc    ".$Vowel + &aacute; i s -> (:ambig-ar-er-ir-paradigm)_",                             // (habl&aacute;is,com&aacute;is)
   ".$Vowel + \u00e1 i s -> (:ambig-ar-er-ir-paradigm)_",                             // (habl\u00e1is,com\u00e1is)

   // reflexive verbs with pronouns, including gerund forms of these reflexives
//cc    ".$Letter + &aacute; n d o s e -> (:ar-reflex-imperative-gerund)_",
   ".$Letter + \u00e1 n d o s e -> (:ar-reflex-imperative-gerund)_",
   ".$Letter + a t e -> (:ar-reflex-imperative-gerund)_",
//cc    ".$Letter + &eacute; m o n o s -> (:ar-reflex-imperative-gerund)_",
   ".$Letter + \u00e9 m o n o s -> (:ar-reflex-imperative-gerund)_",
   ".$Letter + a o s -> (:ar-reflex-imperative-gerund)_",
   ".$Letter + e s e -> (:ar-reflex-imperative-gerund)_",
   ".$Letter + e n s e -> (:ar-reflex-imperative-gerund)_",

//cc    ".$Letter + i &eacute; n d o s e -> (:er-reflex-imperative-gerund)_,(:ir-reflex-imperative-gerund)_",
   ".$Letter + i \u00e9 n d o s e -> (:er-reflex-imperative-gerund)_,(:ir-reflex-imperative-gerund)_",

   ".$Letter a n t + a|e -> a,e,as,es", //(guante,guantes,elefanta,elefantes) 
   ".$Letter $Vowel e n t e # -> _,s",   // (fuente,valiente)
   ".$Letter r t e # -> _,s",           // (norte,deporte)

   ".$Letter + e t e -> (:er-reflex-imperative-gerund)_,(:ir-reflex-imperative-gerund)_",
   ".$Letter o n + t e -> (:er-reflex-imperative-gerund)_,(:ir-reflex-imperative-gerund)_",
   ".$Letter + e o s -> (:er-reflex-imperative-gerund)_",
//cc    ".$Letter + &iacute; o s -> (:ir-reflex-imperative-gerund)_",
   ".$Letter + \u00ed o s -> (:ir-reflex-imperative-gerund)_",
//cc    ".$Letter + &aacute; m o n o s -> (:er-reflex-imperative-gerund)_,(:ir-reflex-imperative-gerund)_",
   ".$Letter + \u00e1 m o n o s -> (:er-reflex-imperative-gerund)_,(:ir-reflex-imperative-gerund)_",
   ".$Letter + a s e -> (:er-reflex-imperative-gerund)_,(:ir-reflex-imperative-gerund)_",
   ".$Letter + a n s e -> (:er-reflex-imperative-gerund)_,(:ir-reflex-imperative-gerund)_",

   // test some salient ending patterns
   ".$Vowel m i e n t + o|a ?s -> o,a,os,as",  // (pensamiento)
   ".$Vowel m e n t e # -> _",         // (lentamente) adverbs
   ".$Vowel d u m b r e # -> _",       // (certidumbre)

   // diminutives
   ".$Vowel d o r + ?c i t a ?s -> ita,itas,cita,citas",   // (labradorita,labradorcita)
   ".$Vowel o r +  i ?s t a ?s -> ita,itas,ista,istas",    // (minoritas,minoristas)
   ".$Vowel ?c i t + o|a ?s -> o,a,os,as",    // (gatito,)
   ".$Vowel i l l + o|a ?s -> o,a,os,as",     // (pastilla)
   ".$Vowel z u e l + o|a ?s -> o,a,os,as",   // ()

   ".$Vowel b l + e ?s -> TRY(!irrVerb)e",                     // ()
   ".$Vowel b l + e ?s -> e,es,(:ambig-ar-er-ir-paradigm)_",   // (amable,amables,hables,comes,vives)

   ".$Vowel a|u c h + o|a ?s -> o,a,os,as",   // (casucha)
   ".$Vowel o t + e|a ?s -> e,a,es,as",       // (arbolote)
   ".$Vowel a c|j + o|a ?s -> o,a,os,as",     // ()
 
//cc    ".$Vowel a r q u &iacute; a # -> _,s",   // (monarqu&iacute;a)
   ".$Vowel a r q u \u00ed a # -> _,s",   // (monarqu\u00eda)
   ".$Vowel a r i o + s -> _,s",     // (bibliotecario)
   ".$Vowel c r a c i a # -> _,s",   // (democracia)
  
   ".$Vowel i c i + o|a ?s -> o,a,os,as",    // (avaricia)
   ".$Vowel i s m o + ?s -> _,s",            // (budismo,convencionalismos)
//cc    ".$Vowel &iacute; s i m + a|o ?s -> o,a,os,as",  // (grand&iacute;simo)
   ".$Vowel \u00ed s i m + a|o ?s -> o,a,os,as",  // (grand\u00edsimo)
   ".$Vowel i s t a + ?s -> _,s",            // (dentista)
   ".$Vowel i t i s # -> _",                 // (flebitis)
//cc    ".$Vowel f o n + o -> o,os,a,as",         // (telefonos,&aacute;fonas)
   ".$Vowel f o n + o -> o,os,a,as",         // (telefonos,\u00e1fonas)
   ".$Vowel f o b i a # -> _,s",             // (claustrafobia)
//cc    ".$Vowel + &oacute; f i l o ?s -> &oacute;filo,&oacute;filos,ofilia,ofilias,&oacute;fila,&oacute;filas",     // (cervantofilia)
   ".$Vowel + \u00f3 f i l o ?s -> \u00f3filo,\u00f3filos,ofilia,ofilias,\u00f3fila,\u00f3filas",     // (cervantofilia)
//cc    ".$Vowel + o f i l i a ?s -> &oacute;filo,&oacute;filos,&oacute;fila,&oacute;filas,ofilia,ofilias",   // (hal&oacute;fila)
   ".$Vowel + o f i l i a ?s -> \u00f3filo,\u00f3filos,\u00f3fila,\u00f3filas,ofilia,ofilias",   // (hal\u00f3fila)
//cc    ".$Vowel < &aacute; > f i l + o|a ?s -> <&aacute;>/_o,<&aacute;>/_os,<&aacute;>/_a,<&aacute;>/_as,<a>/_ia",  // (p&aacute;nfilo,panfilia)
   ".$Vowel < \u00e1 > f i l + o|a ?s -> <\u00e1>/_o,<\u00e1>/_os,<\u00e1>/_a,<\u00e1>/_as,<a>/_ia",  // (p\u00e1nfilo,panfilia)
   ".$Vowel f i l + o -> o,os,ia,ias,a,as",     // (bibliofilo)
   ".$Vowel f i l + i a -> ia,ias,a,as,o,os",   // (hispanofilia)
   ".$Vowel l l e n + o|a ?s -> o,os,a,as",     // (lleno,rellena)
   ".$Vowel l l a|o + ?s -> _,s",               // (semilla,caballo)
   ".$Letter l s o|a + ?s -> _,s",              // (bolsa)

   ".$Vowel ?d e r + o|a -> o,a,os,as",         // (lavandero,vaquero)

  // somemore  adjective endings
  ".$Vowel d i z + o|a ?s -> o,a,os,as",               // (acogedizo,acogediza)
//cc   ".$Vowel $Consonant i e n t + o|a ?s -> o,a,os,as",  // (so&ntilde;olienta,ceniciento)
  ".$Vowel $Consonant i e n t + o|a ?s -> o,a,os,as",  // (so\u00f1olienta,ceniciento)
  ".$Vowel $Consonant i c|z + o|a ?s -> o,a,os,as",    // (rojizo,rojiza)
  ".$Vowel  o s + o|a ?s -> o,a,os,as",                // (rocoso,famosa,graciosa)
  ".$Vowel t u d + e s -> _,es",                       // (altitud,latitud)
  ".$Vowel $Consonant u d + a|o ?s -> o,a,os,as",      // (peludo,caprichuda)
  ".$Vowel $Consonant u z c + o|a ?s -> o,a,os,as",    // (negruzco. blancuzca)

//cc   ".$Consonant + c i o n e s -> ci&oacute;n,iones",           // (canci&oacute;n,canciones)
  ".$Consonant + c i o n e s -> ci\u00f3n,iones",           // (canci\u00f3n,canciones)
//cc   ".$Consonant + c i &oacute; n  -> ci&oacute;n,ciones",             // (canci&oacute;n,canciones)
  ".$Consonant + c i \u00f3 n  -> ci\u00f3n,ciones",             // (canci\u00f3n,canciones)
//cc   ".$Vowel + &oacute; n -> &oacute;n,ones,ona,onas",                 // (llorones,juegetones)
  ".$Vowel + \u00f3 n -> \u00f3n,ones,ona,onas",                 // (llorones,juegetones)
//cc   ".$Vowel + o n a ?s -> &oacute;n,ones,ona,onas",            // (comodona,comod&oacute;n)
  ".$Vowel + o n a ?s -> \u00f3n,ones,ona,onas",            // (comodona,comod\u00f3n)
//cc   ".$Vowel + &eacute; s # -> &eacute;s,eses,esa,esas",               // (holand&eacute;s,ampurdanesas)
  ".$Vowel + \u00e9 s # -> \u00e9s,eses,esa,esas",               // (holand\u00e9s,ampurdanesas)
//cc   ".$Vowel + e s e s -> &eacute;s,eses,esa,esas",             // (ampurdaneses)
  ".$Vowel + e s e s -> \u00e9s,eses,esa,esas",             // (ampurdaneses)
  ".$Vowel a n c i a # -> _,s",                        // (discrepancia)

 // The following patterns generate nominal and/or adjectival forms

  ".$Consonant d|t a d + e s -> _,es",          // (ciudad, ciudades, facultades)
  ".$Consonant d|t a d # -> _,es",              // (ciudad, ciudades, facultad)

  ".$Consonant ~d + a d -> TRY(!irrVerb)ar",      // imperative -ar verb
  ".$Consonant ~d + a d -> (:reg-ar-paradigm)_",  // imperative -ar verb

  ".$Vowel .$Letter e d a # -> _,s",           // (mermeleda,rueda)
  ".$Consonant + e d -> TRY(!irrVerb)er",      // imperative -er verb
  ".$Consonant + e d -> (:reg-er-paradigm)_",  // imperative -er verb

  ".$Consonant + i d -> TRY(!irrVerb)ir",      // imperative -ir verb
  ".$Consonant + i d -> (:reg-ir-paradigm)_",  // imperative -ir verb
 
   // nouns meaning a kind of place
   ".$Vowel ~i a l # -> _,es",              // (naranjal,naranjales)
   ".$Vowel ~i a l|r + e s -> _,es",        // (pinares,pinar,naranjales,naranjal)
   // -anza makes noun from verb
//cc    ".$Vowel a n z + a ?s -> a,as",          // (ense&ntilde;anza)
   ".$Vowel a n z + a ?s -> a,as",          // (ense\u00f1anza)
   // -azo 'a blow'
   ".$Vowel a r i + o|a ?s -> o,a,os,as",   // (cesionario)
   ".$Vowel a z + a ?s -> a,as",            // (baraza)
   ".$Vowel a z + o ?s -> o,os",            // (barazo)
   // place where something is done
//cc    ".$Vowel e r &iacute; a + ?s -> _,s",           //(panader&iacute;a,ingenier&iacute;a)
   ".$Vowel e r \u00ed a + ?s -> _,s",           //(panader\u00eda,ingenier\u00eda)
   // absract noun suffixes -es, -eza
   ".$Vowel + z -> z,ces",                  // (voz, voces, lapiz, lapices)
   ".$Vowel $Vowel + c e s -> z,ces",       // (voz, voces, lapiz, lapices)
   ".$Vowel e z a + ?s -> _,s",             // (graneza)
   // suffix for arts and sciences
//cc    ".$Vowel .$Vowel .$Vowel $Consonant &iacute; a + ?s -> _,s", // (biologia, geometr&iacute;a) 
   ".$Vowel .$Vowel .$Vowel $Consonant \u00ed a + ?s -> _,s", // (biologia, geometr\u00eda) 
   // abstract noun suffix -ura
   ".$Vowel u r a + ?s -> _,s",             // (negrura,altura)
//cc    ".$Letter i n a + ?s -> _,s",            // (p&aacute;gina, piscina)
   ".$Letter i n a + ?s -> _,s",            // (p\u00e1gina, piscina)
   ".$Letter $Consonant i a + ?s -> _,s",   // (iglesia,lluvia)

  // ambiguous suffix between nouns/adjectives/verbs
  ".$Vowel $Consonant e r o|a + ?s -> _,s",  // (guerrero, librero)
  ".$Vowel ?d o r + ?a -> _,es,a,as",        // (hablador habladores habkadira)
//cc   ".$Vowel &aacute; # -> s",                        // (guaran&aacute;,guaran&aacute;s)
  ".$Vowel \u00e1 # -> s",                        // (guaran\u00e1,guaran\u00e1s)

//cc   ".$Vowel + &iacute; ?s -> TRY(!irrVerb)&iacute;",                // (o&iacute;,sent&iacute;)
  ".$Vowel + \u00ed ?s -> TRY(!irrVerb)\u00ed",                // (o\u00ed,sent\u00ed)
//cc   ".$Vowel + &iacute; -> &iacute;es,(:ambig-er-ir-paradigm)_",     // (rub&iacute;,rub&iacute;es,com&iacute;)
  ".$Vowel + \u00ed -> \u00edes,(:ambig-er-ir-paradigm)_",     // (rub\u00ed,rub\u00edes,com\u00ed)

  ".$Vowel y # -> es",                               // (ley,leyes)


   ".$Letter + j a ?n|s -> TRY(!irrVerb)jar,TRY(!irrVerb)ger,TRY(!irrVerb)gir",  // (cojan)
   ".$Letter + j a ?n|s -> (:reg-ar-paradigm)j,(:ambig-er-ir-paradigm)g",    // (bajan,dirijas)
   ".$Letter + c a ?n|s -> TRY(!irrVerb)car,TRY(!irrVerb)quir",              // (delincan)
   ".$Letter + c a ?n|s -> (:reg-ar-paradigm)c,(:reg-ir-paradigm)qu",        // (tocan)
   ".$Letter + g a ?n|s -> TRY(!irrVerb)gar,TRY(!irrVerb)guir",              // (distinga)
   ".$Letter + g a ?n|s -> (:reg-ar-paradigm)g,(:reg-ir-paradigm)gu",        // (llegas)
   ".$Letter + z a ?n|s -> TRY(!irrVerb)zar,TRY(!irrVerb)cer,TRY(!irrVerb)cir",  // ()
   ".$Letter + z a ?n|s -> (:reg-ar-paradigm)z,(:ambig-er-ir-paradigm)c",    // (venzan)
   ".$Letter + z c a ?n|s -> TRY(!irrVerb)cer",      // (conozcan)
   ".$Letter + z c a ?n|s -> (:reg-er-paradigm)c",   // (crezcan)

   ".$Vowel .$Consonant + a|e ?s -> TRY(!irrVerb)o", // (huya,huyo -> huir)
   ".$Vowel .$Consonant + a|e ?s -> (:noun-sg)as,(:ambig-ar-er-ir-paradigm)_", // (tontas,hablas,comas,vivas)
  
   ".$Vowel .$Consonant + a|e n -> TRY(!irrVerb)o,TRY(!irrVerb)e",  // (huye n-> huyo)
   ".$Vowel .$Consonant + a|e n -> (:ambig-ar-er-ir-paradigm)_",    // (hablen,comen,viven)

  

   // test for a basic noun
   ".$Consonant + o ?s -> o,os,(:ambig-ar-er-ir-paradigm)_",    //(gato,gatos,hablo,como,vivo)
   ".$Vowel $Consonant # -> _,es",        // (papel, papeles)

    // Otherwise, use the default rule, and generate all forms
   ".$Vowel -> (:all-forms)_", // (defaultRule)

        };


  // **** Named Rules  **** //

  // Generate Spanish plurals from singular nouns

  String[] NounPl = {//":noun-pl",
  // generates noun plurals
  // assumes the input is a singular noun

  ".$Consonant d a d # -> es",                              // (ciudades)
  ".$Vowel + z -> ces",                                     // (voz -> voces)
//cc   ".$Vowel &aacute;|&eacute;|&iacute;|y # -> es",                                // (rub&iacute; -> rub&iacute;es, ley -> leyes)
  ".$Vowel \u00e1|\u00e9|\u00ed|y # -> es",                                // (rub\u00ed -> rub\u00edes, ley -> leyes)
//cc   ".$Vowel + $Accent $Consonant # -> (:remove-accent)_",    // (patr&oacute;n,comp&aacute;s)
  ".$Vowel + $Accent $Consonant # -> (:remove-accent)_",    // (patr\u00f3n,comp\u00e1s)
  ".$Consonant i|e s # -> _",                               // (tesis, lunes)
  ".$Consonant o|a|e # -> s",                               // (gatos,estudiantes,casas)
  ".$Vowel $Consonant # -> es",                             // (mujeres)

        };

  // Remove accent over final vowel when forming plural
  String[] removeAccent = {//":remove-accent",
  // removes accent from an accented vowel when take final -es plural
  // assumes the input ends in accented vowel with final consonant

//cc   ".$Vowel < &aacute; > $Consonant # -> <a>/_es",           // (compases)
  ".$Vowel < \u00e1 > $Consonant # -> <a>/_es",           // (compases)
//cc   ".$Vowel < &eacute; > $Consonant # -> <e>/_es",           // (palafrenes)
  ".$Vowel < \u00e9 > $Consonant # -> <e>/_es",           // (palafrenes)
//cc   ".$Vowel < &iacute; > $Consonant # -> <i>/_es",           // (confines)
  ".$Vowel < \u00ed > $Consonant # -> <i>/_es",           // (confines)
//cc   ".$Vowel < &oacute; > $Consonant # -> <o>/_es",           // (patrones)
  ".$Vowel < \u00f3 > $Consonant # -> <o>/_es",           // (patrones)
//cc   ".$Vowel < &uacute; > $Consonant # -> <u>/_es",           // (betunes)
  ".$Vowel < \u00fa > $Consonant # -> <u>/_es",           // (betunes)

        };
 // Add Accent over final vowel when creating a singular 

  String[] addAccent = {//":add-accent",
  // add accent to an accented vowel
  // assumes the input ends in unaccented vowel with final consonant

//cc   ".$Vowel < a > $Consonant # -> <&aacute;>",           // (comp&aacute;s)
  ".$Vowel < a > $Consonant # -> <\u00e1>",           // (comp\u00e1s)
//cc   ".$Vowel < e > $Consonant # -> <&eacute;>",           // (palafr&eacute;n)
  ".$Vowel < e > $Consonant # -> <\u00e9>",           // (palafr\u00e9n)
//cc   ".$Vowel < i > $Consonant # -> <&iacute;>",           // (jard&iacute;n)
  ".$Vowel < i > $Consonant # -> <\u00ed>",           // (jard\u00edn)
//cc   ".$Vowel < o > $Consonant # -> <&oacute;>",           // (patr&oacute;n)
  ".$Vowel < o > $Consonant # -> <\u00f3>",           // (patr\u00f3n)
//cc   ".$Vowel < u > $Consonant # -> <&uacute;>",           // (bet&uacute;n)
  ".$Vowel < u > $Consonant # -> <\u00fa>",           // (bet\u00fan)

        };

  // Generate a singular noun form
  String[] NounSg = {//":noun-sg",
  // generates noun singular
  // assumes the input is a plural noun

  ".$Vowel + c e s -> z",                        // (voces, voz, lapices, lapiz)
//cc   ".$Vowel &aacute;|&eacute;|&iacute;|y + e s -> _",                  // (rub&iacute;es -> rub&iacute; , leyes -> ley)
  ".$Vowel \u00e1|\u00e9|\u00ed|y + e s -> _",                  // (rub\u00edes -> rub\u00ed , leyes -> ley)
//cc   ".$Consonant + i o n e s -> i&oacute;n",              // (canciones -> canci&oacute;n)
  ".$Consonant + i o n e s -> i\u00f3n",              // (canciones -> canci\u00f3n)
//cc   ".$Vowel + $Vowel n|r e s -> (:add-accent)_",  // (jardines, jard&iacute;n)
  ".$Vowel + $Vowel n|r e s -> (:add-accent)_",  // (jardines, jard\u00edn)
  ".$Consonant i|e s # -> _",                    // (tesis, lunes)
//cc   ".$Consonant + e s a ?s -> &eacute;s,eses,esa,esas",  // (ampurdanesa)
  ".$Consonant + e s a ?s -> \u00e9s,eses,esa,esas",  // (ampurdanesa)
  ".$Consonant o|a|e + ?s -> _",                 // (gato,estudiante,casa)
  ".$Vowel $Consonant + e s -> _,es",            // (mujer)

        };


  // Generate Spanish adjectives

  String[] Adj = {//":adj",
  // generates adjectives
  // assumes the input is either singular/plural form of adjective

  ".$Vowel + o|a ?s -> o,os,a,as",          // (alto,altos,alta,altas)
  ".$Vowel e + ?s -> _,s",                  // (intelligente,intelligentes)
  ".$Vowel $Consonant + e s -> _,es",       // (popular,populares)
  ".$Vowel $Consonant o r # -> _,a",        // (hablador, habladora)
//cc   ".$Vowel + &oacute; n -> _,ona",                 // (pregunt&oacute;n, preguntona)
  ".$Vowel + \u00f3 n -> _,ona",                 // (pregunt\u00f3n, preguntona)
//cc   ".$Vowel + &aacute; n -> _,ana",                 // (hogaz&aacute;n, hogazana) 
  ".$Vowel + \u00e1 n -> _,ana",                 // (hogaz\u00e1n, hogazana) 
  ".$Vowel $Consonant # -> _,es",           // (popular,populares)
  ".$Vowel .$Consonant # -> _",             // (verde,fuerte) invariant form

        };


  // Generate paradigm for ambiguous verb forms - don't know -ar/-er/-ir class
  String[] AmbigArErIrParadigm = {//":ambig-ar-er-ir-paradigm",
   // Assume: input is stem of verb
   ".$Vowel -> (:reg-ar-paradigm)_,(:ambig-er-ir-paradigm)_",

  };

  // Generate regular paradigm for -ar/-er/-ir, depending on the infinitive
  String[] RegParadigm = {//":reg-paradigm",
   // Assume: input is an infinitive
   ".$Letter + a r -> (:reg-ar-paradigm)_",
   ".$Letter + e r -> (:reg-er-paradigm)_",
   ".$Letter + i r -> (:reg-ir-paradigm)_",

  };
 

 // Generate the paradigm for regular -ar,-er,-ir verbs

 String[] RegArParadigm = {//":reg-ar-paradigm",
 // Assume: input is stem of infinitive form
 ".$Vowel -> (:ar-pres)_,(:ar-imperf)_,(:ar-pret)_,(:ar-pres-subj)_,(:ar-imp-subj)_,(:ar-fut-subj)_,(:all-cond)ar,(:all-future)ar,(:ar-imperative)_,(:ar-prog-part)_",

        };

 String[] RegErParadigm = {//":reg-er-paradigm",
 // Assume: input is stem of -er infinitive form
 ".$Vowel -> (:er-pres)_,(:er-pret)_,(:all-cond)er,(:all-future)er,(:er-pres-subj)_,(:er-imp-subj)_,(:er-fut-subj)_,(:er-prog-part)_,(:er-imperative)_,(:er-ir-imperf)_",

        };

 String[] RegIrParadigm = {//":reg-ir-paradigm",
 // Assume: input is stem of -ir infinitive form
 ".$Vowel -> (:ir-pres)_,(:ir-pret)_,(:all-cond)ir,(:all-future)ir,(:ir-pres-subj)_,(:ir-imp-subj)_,(:ir-fut-subj)_,(:ir-prog-part)_,(:ir-imperative)_,(:er-ir-imperf)_",

        };

 String[] AmbigArErParadigm = {//":ambig-ar-er-paradigm",
 // Assume: input is stem of -ar OR -er infinitive form
//cc  // This rule invoked when ambiguity exists, input can't resolve -ar vs. -er verbs, e.g., habl&eacute;is (ar) com&eacute;is (er)
 // This rule invoked when ambiguity exists, input can't resolve -ar vs. -er verbs, e.g., habl\u00e9is (ar) com\u00e9is (er)

 ".$Vowel -> (:reg-ar-paradigm)_,(:reg-er-paradigm)_",  // (hablar,comer)

        };

 String[] AmbigErIrParadigm = {//":ambig-er-ir-paradigm",
 // Assume: input is stem of -er OR -ir infinitive form 
 // This rule invoked when ambiguity exists, input can't resolve -er vs. -ir verbs, e.g., com-ido, viv-iendo

     ".$Vowel -> (:reg-er-paradigm)_,(:reg-ir-paradigm)_,(:er-ir-imperf)_",  // (comer, vivir)

        };


  // Generate present tense for -ar verbs
  String[] ArPres = {//":ar-pres",
  //generate present forms for -ar verbs, from verb stem
  //Assume: input has stripped infinitive -ar
//cc    " .$Letter + i -> &iacute;o,&iacute;as,&iacute;a,iamos,i&aacute;is,&iacute;an",      // (-iar verbs take stressed &iacute; in 1/2/3sg and 3pl env&iacute;o)
   " .$Letter + i -> \u00edo,\u00edas,\u00eda,iamos,i\u00e1is,\u00edan",      // (-iar verbs take stressed \u00ed in 1/2/3sg and 3pl env\u00edo)
//cc    " .$Letter ~g + u -> &uacute;o,&uacute;as,&uacute;a,uamos,u&aacute;is,&uacute;an",   // (-uar verbs take stressed &uacute; in 1/2/3sg and 3pl contin&uacute;o)
   " .$Letter ~g + u -> \u00fao,\u00faas,\u00faa,uamos,u\u00e1is,\u00faan",   // (-uar verbs take stressed \u00fa in 1/2/3sg and 3pl contin\u00fao)
//cc    "< e > l|n|r *$Consonant # -> <ie>/_o,<ie>/_as,<ie>/_a,amos,&aacute;is,<ie>/_an",
   "< e > l|n|r *$Consonant # -> <ie>/_o,<ie>/_as,<ie>/_a,amos,\u00e1is,<ie>/_an",
//cc    "< o > l|n|r|s *$Consonant # -> <ue>/_o,o,<ue>/_as,as,<ue>/_a,a,amos,&aacute;is,<ue>/_an,an",  // (cuento,honro)
   "< o > l|n|r|s *$Consonant # -> <ue>/_o,o,<ue>/_as,as,<ue>/_a,a,amos,\u00e1is,<ue>/_an,an",  // (cuento,honro)
//cc    ".$Vowel $Letter # -> o,as,a,amos,&aacute;is,an",  // (hablo,hablas,plateo)
   ".$Vowel $Letter # -> o,as,a,amos,\u00e1is,an",  // (hablo,hablas,plateo)

        };

  // Generate present tense for -er verbs
  String[] ErPres = {//":er-pres",
  //generate present forms for -er verbs, from verb stem
  //Assume: input has stripped infinitive -er

//cc    ".$Vowel +$Consonant + c -> zo,es,e,emos,&eacute;is,en", // (vencer -> venzo,vences)
   ".$Vowel +$Consonant + c -> zo,es,e,emos,\u00e9is,en", // (vencer -> venzo,vences)
//cc     "p r e n *$Consonant # -> o,es,e,emos,&eacute;is,en",  //(aprendo,comprendes)
    "p r e n *$Consonant # -> o,es,e,emos,\u00e9is,en",  //(aprendo,comprendes)
//cc     "< e > l|n|r *$Consonant # -> <ie>/_o,<ie>/_es,<ie>/_e,emos,&eacute;is,<ie>/_en",  //(perder)
    "< e > l|n|r *$Consonant # -> <ie>/_o,<ie>/_es,<ie>/_e,emos,\u00e9is,<ie>/_en",  //(perder)
//cc     "< o > l|n|r *$Consonant # -> <ue>/_o,o,<ue>/_es,es,<ue>/_e,e,emos,&eacute;is,<ue>/_en,en",  //(esconde,corres)
    "< o > l|n|r *$Consonant # -> <ue>/_o,o,<ue>/_es,es,<ue>/_e,e,emos,\u00e9is,<ue>/_en,en",  //(esconde,corres)
//cc     ".$Vowel + c # -> zco,ces,ce,cemos,c&eacute;is,cen",  // (conocer -> conozco,conoces)
    ".$Vowel + c # -> zco,ces,ce,cemos,c\u00e9is,cen",  // (conocer -> conozco,conoces)
//cc     ".$Vowel + g # -> jo,ges,ge,gemos,g&eacute;is,gen",   // (coger -> cojo,coges,cogemos)
    ".$Vowel + g # -> jo,ges,ge,gemos,g\u00e9is,gen",   // (coger -> cojo,coges,cogemos)
//cc     ".$Vowel $Consonant # -> o,es,e,emos,&eacute;is,en",  // (comer -> como,comemos)
    ".$Vowel $Consonant # -> o,es,e,emos,\u00e9is,en",  // (comer -> como,comemos)

        };

  // Generate present tense for -ir verbs
  String[] IrPres = {//":ir-pres",
  // generate present forms for -ir verbs, from verb stem
  // Assume: input has stripped infinitive -ir

//cc     ".$Vowel +$Consonant + c -> zo,es,e,emos,&eacute;is,en", // ()
    ".$Vowel +$Consonant + c -> zo,es,e,emos,\u00e9is,en", // ()
//cc     "< e > l|n|r *$Consonant # -> <ie>/_o,<ie>/_es,<ie>/_e,imos,&iacute;s,<ie>/_en", // (mentir)
    "< e > l|n|r *$Consonant # -> <ie>/_o,<ie>/_es,<ie>/_e,imos,\u00eds,<ie>/_en", // (mentir)
//cc     "< e > l|n|r *$Consonant # -> <i>/_o,<i>/_es,<i>/_e,imos,&iacute;s,<i>/_en",  // ()
    "< e > l|n|r *$Consonant # -> <i>/_o,<i>/_es,<i>/_e,imos,\u00eds,<i>/_en",  // ()
//cc     "< o > l|n|r *$Consonant # -> <ue>/_o,<ue>/_es,<ue>/_e,imos,&iacute;s,<ue>/_en",  // ()
    "< o > l|n|r *$Consonant # -> <ue>/_o,<ue>/_es,<ue>/_e,imos,\u00eds,<ue>/_en",  // ()
//cc     ".$Vowel g + u -> o,ues,ue,uimos,u&iacute;s,uen",    // (distinguir -> distingo)
    ".$Vowel g + u -> o,ues,ue,uimos,u\u00eds,uen",    // (distinguir -> distingo)
//cc     ".$Vowel + q u -> co,ques,que,quimos,qu&iacute;s,quen",  // (delinco,delinques)
    ".$Vowel + q u -> co,ques,que,quimos,qu\u00eds,quen",  // (delinco,delinques)
//cc     ".$Vowel + c # -> czo,ces,ce,cimos,c&iacute;s,cen",  // ()
    ".$Vowel + c # -> czo,ces,ce,cimos,c\u00eds,cen",  // ()
//cc     ".$Vowel + g # -> jo,ges,ge,gimos,g&iacute;s,gen",   // (dirigir -> dirijo)
    ".$Vowel + g # -> jo,ges,ge,gimos,g\u00eds,gen",   // (dirigir -> dirijo)
//cc     ".$Vowel $Consonant # -> o,es,e,imos,&iacute;s,en",  // (vivir -> vivo,vives,vivimos)
    ".$Vowel $Consonant # -> o,es,e,imos,\u00eds,en",  // (vivir -> vivo,vives,vivimos)

        };

  // Generate present tense (excluding 1st singular) for -ar Irregular Verbs
  String[] ArIrregPres = {//":ar-irreg-pres",
  // generate present forms for -ar verbs (except 1st sing)
  // Assume: input is 1st singular present from Verb Lookup Table

//cc    "< i e > l|n|r|z *$Consonant + o -> o,as,a,<e>/_amos,<e>/_&aacute;is,an",   // (pienso,piensa,pensamos,empiezo)
   "< i e > l|n|r|z *$Consonant + o -> o,as,a,<e>/_amos,<e>/_\u00e1is,an",   // (pienso,piensa,pensamos,empiezo)
//cc    "< u|&uuml; e > l|n|r *$Consonant + o -> o,as,a,<o>/_amos,<o>/_&aacute;is,an", // (ruego,ruegas,rogamos)
   "< u|\u00fc e > l|n|r *$Consonant + o -> o,as,a,<o>/_amos,<o>/_\u00e1is,an", // (ruego,ruegas,rogamos)
//cc    "< u e > g  + o -> o,as,a,<u>/_amos,<u>/_&aacute;is,an", // (juegas,jugamos)
   "< u e > g  + o -> o,as,a,<u>/_amos,<u>/_\u00e1is,an", // (juegas,jugamos)
//cc    "# d + o y -> oy,as,a,amos,&aacute;is,an",              // (doy,das)
   "# d + o y -> oy,as,a,amos,\u00e1is,an",              // (doy,das)
//cc    "# e s t  + o y -> oy,&aacute;s,&aacute;,amos,&aacute;is,&aacute;n",         // (est&aacute;s,estamos)
   "# e s t  + o y -> oy,\u00e1s,\u00e1,amos,\u00e1is,\u00e1n",         // (est\u00e1s,estamos)
//cc    ".$Vowel $Consonant + o -> o,as,a,amos,&aacute;is,an",  //(busco,busca,buscamos)
   ".$Vowel $Consonant + o -> o,as,a,amos,\u00e1is,an",  //(busco,busca,buscamos)

        };

  // Generate present tense (excluding 1st singular) for -er Irregular Verbs
  String[] ErIrregPres = {//":er-irreg-pres",
  // generate present forms for irreg -er verbs
  // Assume: input is 1st singular present from Verb Lookup Table

//cc     "< q u e p > + o -> o,<cab>/_es,<cab>/_e,<cab>/_emos,<cab>/_&eacute;is,<cab>/_en", // (quepo,cabes)
    "< q u e p > + o -> o,<cab>/_es,<cab>/_e,<cab>/_emos,<cab>/_\u00e9is,<cab>/_en", // (quepo,cabes)
//cc     "# p r o v e + o -> o,es,e,emos,&eacute;is,en",        // (porvees,proveemos)
    "# p r o v e + o -> o,es,e,emos,\u00e9is,en",        // (porvees,proveemos)
//cc     "# h + e -> e,as,a,emos,b&eacute;is,an",                // (he,has,ha,hemos,heb&eacute;is,han)
    "# h + e -> e,as,a,emos,b\u00e9is,an",                // (he,has,ha,hemos,heb\u00e9is,han)
//cc     "# s + &eacute; -> &eacute;,abes,abe,abemos,ab&eacute;is,aben",       // (s&eacute;,sabes)
    "# s + \u00e9 -> \u00e9,abes,abe,abemos,ab\u00e9is,aben",       // (s\u00e9,sabes)
//cc     "$Vowel l + g o -> go,es,e,emos,&eacute;is,en",                            // (valgo,vales)
    "$Vowel l + g o -> go,es,e,emos,\u00e9is,en",                            // (valgo,vales)
//cc     ".$Consonant + i g o -> igo,es,e,emos,&eacute;is,en",                      // (traigo,traes)
    ".$Consonant + i g o -> igo,es,e,emos,\u00e9is,en",                      // (traigo,traes)
//cc     "< e > l|n|r + g o -> go,<ie>/_es,<ie>/_e,emos,&eacute;is,<ie>/_en",         // (tengo,tienes)
    "< e > l|n|r + g o -> go,<ie>/_es,<ie>/_e,emos,\u00e9is,<ie>/_en",         // (tengo,tienes)
//cc     "< ?h u e > d|l|n|r *$Consonant + o -> o,es,e,<o>/_emos,<o>/_&eacute;is,en",    // (vuelvo,hueles,podemos)
    "< ?h u e > d|l|n|r *$Consonant + o -> o,es,e,<o>/_emos,<o>/_\u00e9is,en",    // (vuelvo,hueles,podemos)
//cc     "< u e z > + o -> o,<uec>/_es,<uec>/_e,<oc>/_emos,<oc>/_&eacute;is,<uez>/_en",  // (cuezo,cueces,cocemos)
    "< u e z > + o -> o,<uec>/_es,<uec>/_e,<oc>/_emos,<oc>/_\u00e9is,<uez>/_en",  // (cuezo,cueces,cocemos)
//cc     "< i e > +$Letter + o -> o,_es,e,<e>/_emos,<e>/_&eacute;is,en",                 // (quiero,queremos)
    "< i e > +$Letter + o -> o,_es,e,<e>/_emos,<e>/_\u00e9is,en",                 // (quiero,queremos)
//cc     "$Vowel n + g o -> go,es,e,emos,&eacute;is,en",                            // (pongo,pones)
    "$Vowel n + g o -> go,es,e,emos,\u00e9is,en",                            // (pongo,pones)
//cc     "$Vowel + g o -> go,ces,ce,cemos,c&eacute;is,cen",                          // (hago,haces)  
    "$Vowel + g o -> go,ces,ce,cemos,c\u00e9is,cen",                          // (hago,haces)  
//cc     ".$Vowel + z c o -> zco,ces,ce,cemos,c&eacute;is,cen",                         // (conozco,conocen)
    ".$Vowel + z c o -> zco,ces,ce,cemos,c\u00e9is,cen",                         // (conozco,conocen)
//cc     ".$Vowel + j o -> jo,ges,ge,gemos,g&eacute;",                               // (cojo,coges)           
    ".$Vowel + j o -> jo,ges,ge,gemos,g\u00e9",                               // (cojo,coges)           
    ".$Consonant e + o -> o,s,_,mos,is,n",                // (veo,veis)   
//cc     ".$Vowel + o -> o,es,e,emos,&eacute;is,en",                  // ()
    ".$Vowel + o -> o,es,e,emos,\u00e9is,en",                  // ()

        };

  // Generate present tense (exclusing 1st singular) for -ir Irregular Verbs
  String[] IrIrregPres = {//":ir-irreg-pres",
  // generate present forms for irreg -ir verbs
  // Assume: input is 1st singular present from Verb Lookup Table

//cc     "d < i > + g o -> go,ces,ce,<e>/_cimos,<e>/_c&iacute;s,cen",       // (digo,decimos)
    "d < i > + g o -> go,ces,ce,<e>/_cimos,<e>/_c\u00eds,cen",       // (digo,decimos)
//cc     "< e > n + g o -> go,<ie>/_es,<ie>/_e,imos,&iacute;s,<ie>/_en",    // (vengo,vienen)
    "< e > n + g o -> go,<ie>/_es,<ie>/_e,imos,\u00eds,<ie>/_en",    // (vengo,vienen)
//cc     ".$Vowel i n g + o -> o,ues,ue,uimos,u&iacute;s,uen",              //(distingo,distingues,distingu&iacute;s)  
    ".$Vowel i n g + o -> o,ues,ue,uimos,u\u00eds,uen",              //(distingo,distingues,distingu\u00eds)  
//cc     "$Vowel i + g o -> go,yes,ye,mos,&iacute;s,yen",                  // (oigo,oyes)
    "$Vowel i + g o -> go,yes,ye,mos,\u00eds,yen",                  // (oigo,oyes)
//cc     "$Vowel l + g o -> go,es,e,emos,&eacute;is,en",                   // (salgo,sales)
    "$Vowel l + g o -> go,es,e,emos,\u00e9is,en",                   // (salgo,sales)
//cc     ".$Consonant g + o -> go,ues,ue,uimos,u&iacute;s,uen",              // (sigo,sigues)
    ".$Consonant g + o -> go,ues,ue,uimos,u\u00eds,uen",              // (sigo,sigues)
    ".$Letter + j o -> jo,ges,ge,gimos,gis,gen",                 // (dirijo,diriges)
    "# v + o y -> oy,as,amos,ais,an",                            // (voy,vas)
//cc     ".$Letter u + y o -> yo,yes,ye,imos,&iacute;s,yen",                 // (arguyo,agu&iacute;s,huyo,hu&iacute;s)
    ".$Letter u + y o -> yo,yes,ye,imos,\u00eds,yen",                 // (arguyo,agu\u00eds,huyo,hu\u00eds)
//cc     "< i ?e > l|n|r *$Consonant + o -> o,es,e,<e>/_imos,<e>/_&iacute;s,en",    //(siento,sentimos,pido,pedimos)
    "< i ?e > l|n|r *$Consonant + o -> o,es,e,<e>/_imos,<e>/_\u00eds,en",    //(siento,sentimos,pido,pedimos)
//cc     "< &iacute; > + o -> o,es,e,<e>/_imos,<e>/_&iacute;s,en",                         // (r&iacute;o,re&iacute;mos)
    "< \u00ed > + o -> o,es,e,<e>/_imos,<e>/_\u00eds,en",                         // (r\u00edo,re\u00edmos)
//cc     "< u e > l|n|r *$Consonant + o -> o,es,e,<o>/_imos,<o>/_&iacute;s,en",     // () 
    "< u e > l|n|r *$Consonant + o -> o,es,e,<o>/_imos,<o>/_\u00eds,en",     // () 
//cc     ".$Vowel $Consonant + o -> o,es,e,imos,&iacute;s,en",                      // ()
    ".$Vowel $Consonant + o -> o,es,e,imos,\u00eds,en",                      // ()

        };


  // Generate Irregular progressive/past participles for -ar/-er/-ir verbs
  String[] IrregProgPart = {//":irreg-prog-part",
  // generate progressive/past participle forms for -ar/-er/-ir verbs
  // Assume: input is prog/past participle from Verb Lookup Table

    ".$Letter + o -> o,os,a,as",     // (jugando,diciendo,sabido)

        };

  // Generate progressive/past participles for regular -ar verbs
  String[] ArProgPart = {//":ar-prog-part",
  // Assume: input has stripped infinitive -ar, 
    ".$Vowel $Letter # -> ando,ado,ados,ada,adas",            // (hablar -> hablando,hablado,plateado)

        };
  // Generate progressive/past participles for regular -er verbs
  String[] ErProgPart = {//":er-prog-part",
  // Assume: input is stripped -er infinitive
    "e n d # -> iendo,ido,idos,ida,idas",                   // (aprendiendo,encendiendo)
//cc     ".$Letter $Vowel # -> yendo,&iacute;do,idos,&iacute;da,&iacute;das",         // (creer -> creyendo,cre&iacute;do)
    ".$Letter $Vowel # -> yendo,\u00eddo,idos,\u00edda,\u00eddas",         // (creer -> creyendo,cre\u00eddo)
    ".$Vowel $Consonant # -> iendo,ido,idos,ida,idas",      // (comer -> comiendo,comido)

        };

  // Generate progressive/past participles for regular -ir verbs
  String[] IrProgPart = {//":ir-prog-part",
  // generate progressive/past participle forms for -er/-ir verbs
  // Assume: input has stripped infinitive -er/-ir, or is stripped participle from Verb Lookup Table
    "e s c r i + b -> biendo,to,tos,ta,tas",                // (escribiendo,escrito)
    "< e > +$Consonant # -> <i>/_iendo,ido,idos,ida,idas",  // (sentir -> sintiendo,sentido)
    "< o > +$Consonant # -> <u>/_iendo,ido,idos,ida,idas",  // (morir -> durmiendo,dormido)
//cc     ".$Letter $Vowel # -> yendo,&iacute;do,idos,&iacute;da,&iacute;das",         // (huir -> huyendo,hu&iacute;do)
    ".$Letter $Vowel # -> yendo,\u00eddo,idos,\u00edda,\u00eddas",         // (huir -> huyendo,hu\u00eddo)
    ".$Vowel $Consonant # -> iendo,ido,idos,ida,idas",      // (vivir -> viviendo,vivido)

        };
   
   // Generate imperfect for all -ar verbs (reg/irreg)
   String[] ArImperf = {//":ar-imperf",
   // generate imperfect forms from the verb stem
   // Assume: input has stripped infinitive -ar

//cc     "$Letter + a  -> aba,abas,&aacute;bamos,abais,aban",           // (daba,caz&aacute;bamos)
    "$Letter + a  -> aba,abas,\u00e1bamos,abais,aban",           // (daba,caz\u00e1bamos)
//cc     ".$Vowel $Letter # -> aba,abas,&aacute;bamos,abais,aban",   // (hablaba,plateabas)
    ".$Vowel $Letter # -> aba,abas,\u00e1bamos,abais,aban",   // (hablaba,plateabas)

        };
   
   // Generate imperfect for all -er/-ir verbs (reg/irreg)
   String[] ErIrImperf = {//":er-ir-imperf",
   //generate imperfect forms from the verb stem
   //Assume: input has stripped infinitive -er/-ir

//cc     "# + i -> iba,ibas,&iacute;bamos,ibais,iban",    // (ir -> iba)
    "# + i -> iba,ibas,\u00edbamos,ibais,iban",    // (ir -> iba)
//cc     ".$Letter + ?&iacute; -> &iacute;a,&iacute;as,&iacute;amos,&iacute;ais,&iacute;an",     // (com&iacute;amos,ve&iacute;a,o&iacute;a)
    ".$Letter + ?\u00ed -> \u00eda,\u00edas,\u00edamos,\u00edais,\u00edan",     // (com\u00edamos,ve\u00eda,o\u00eda)

        };

   String[] ArPret = {//":ar-pret",
   // generate past forms from the verb stem 
   // Assume: input has stripped infinitive -ar
   // spelling rule: before e, c -> qu, g -> gu, z -> c

//cc    ".$Vowel + c -> qu&eacute;,caste,c&oacute;,camos,casteis,caron",    // ()
   ".$Vowel + c -> qu\u00e9,caste,c\u00f3,camos,casteis,caron",    // ()
//cc    ".$Vowel + g -> gu&eacute;,gaste,g&oacute;,gamos,gasteis,garon",    // ()
   ".$Vowel + g -> gu\u00e9,gaste,g\u00f3,gamos,gasteis,garon",    // ()
//cc    ".$Vowel + z -> c&eacute;,zaste,z&oacute;,zamos,zasteis,zaron",     // ()
   ".$Vowel + z -> c\u00e9,zaste,z\u00f3,zamos,zasteis,zaron",     // ()
//cc    ".$Vowel $Letter # -> &eacute;,aste,&oacute;,amos,asteis,aron",  // (habl&eacute;,habl&oacute;,plateaste)
   ".$Vowel $Letter # -> \u00e9,aste,\u00f3,amos,asteis,aron",  // (habl\u00e9,habl\u00f3,plateaste)

        };

  String[] ErPret = {//":er-pret",
   //generate past forms from the verb stem 
   //Assume: input has stripped infinitive -er

//cc    ".$Consonant a|e|o # -> &iacute;,&iacute;ste,y&oacute;,&iacute;mos,&iacute;steis,yeron",        // (creer -> crey&oacute;)
   ".$Consonant a|e|o # -> \u00ed,\u00edste,y\u00f3,\u00edmos,\u00edsteis,yeron",        // (creer -> crey\u00f3)
//cc    ".$Vowel $Consonant # -> &iacute;,iste,i&oacute;,imos,isteis,ieron",       // (comer -> com&iacute;,comi&oacute;)
   ".$Vowel $Consonant # -> \u00ed,iste,i\u00f3,imos,isteis,ieron",       // (comer -> com\u00ed,comi\u00f3)

        };

  String[] IrPret = {//":ir-pret",
   //generate past forms from the verb stem
   //Assume: input has stripped infinitive -ir 

//cc    "< e > +$Consonant # -> &iacute;,iste,<i>/_i&oacute;,imos,isteis,<i>/_ieron",
   "< e > +$Consonant # -> \u00ed,iste,<i>/_i\u00f3,imos,isteis,<i>/_ieron",
//cc    ".$Consonant o # -> &iacute;,&iacute;ste,y&oacute;,&iacute;mos,&iacute;steis,yeron",
   ".$Consonant o # -> \u00ed,\u00edste,y\u00f3,\u00edmos,\u00edsteis,yeron",
//cc    ".$Consonant u # -> &iacute;,iste,y&oacute;,imos,isteis,yeron", // (huir -> hu&iacute;,huiste,huimos)
   ".$Consonant u # -> \u00ed,iste,y\u00f3,imos,isteis,yeron", // (huir -> hu\u00ed,huiste,huimos)
//cc    "< o > +$Consonant # -> &iacute;,iste,<u>/_i&oacute;,imos,isteis,<u>/_ieron",
   "< o > +$Consonant # -> \u00ed,iste,<u>/_i\u00f3,imos,isteis,<u>/_ieron",
   "u + c -> je,jiste,jo,jimos,jisteis,jeron", // (producir -> produje,produjiste)
//cc    ".$Vowel $Consonant # -> &iacute;,iste,i&oacute;,imos,isteis,ieron",  // (vivir -> viv&iacute;,vivi&oacute;)
   ".$Vowel $Consonant # -> \u00ed,iste,i\u00f3,imos,isteis,ieron",  // (vivir -> viv\u00ed,vivi\u00f3)

        };

  String[] ArIrregPret = {//":ar-irreg-pret",
   // generate preterite forms from the preterite 1st singular for Irregular Verbs
   // Assume: input is 1st sing. preterite

//cc    ".$Vowel + g u &eacute; -> gu&eacute;,gaste,g&oacute;,gamos,gasteis,garon",     // ()
   ".$Vowel + g u \u00e9 -> gu\u00e9,gaste,g\u00f3,gamos,gasteis,garon",     // ()
//cc    ".$Vowel + q u &eacute; -> qu&eacute;,caste,c&oacute;,camos,casteis,caron",     // (busqu&eacute; -> buscaste)
   ".$Vowel + q u \u00e9 -> qu\u00e9,caste,c\u00f3,camos,casteis,caron",     // (busqu\u00e9 -> buscaste)
//cc    ".$Vowel + c &eacute; -> c&eacute;,zaste,z&oacute;,zamos,zasteis,zaron",        // ()
   ".$Vowel + c \u00e9 -> c\u00e9,zaste,z\u00f3,zamos,zasteis,zaron",        // ()
//cc    ".$Letter u v + e -> e,&iacute;ste,o,imos,isteis,ieron",          // (estuve,estuvo)
   ".$Letter u v + e -> e,\u00edste,o,imos,isteis,ieron",          // (estuve,estuvo)
//cc    ".$Letter + i -> i,&iacute;ste,io,imos,isteis,ieron",              // (di,diste)
   ".$Letter + i -> i,\u00edste,io,imos,isteis,ieron",              // (di,diste)
//cc    ".$Vowel $Consonant + &eacute; -> &eacute;,aste,&oacute;,amos,asteis,aron",   // (pens&eacute;,cont&eacute;)
   ".$Vowel $Consonant + \u00e9 -> \u00e9,aste,\u00f3,amos,asteis,aron",   // (pens\u00e9,cont\u00e9)
//cc    ".$Vowel $Consonant + e -> e,aste,&oacute;,amos,asteis,aron",   // (anduve)
   ".$Vowel $Consonant + e -> e,aste,\u00f3,amos,asteis,aron",   // (anduve)

        };

  String[] ErIrregPret = {//":er-irreg-pret",
   // generate preterite forms from preterite 1st sing. Irregular Verbs
   // Assume: input is 1st sing. preterite

//cc    ".$Consonant a|e|o + &iacute; -> &iacute;,&iacute;ste,y&oacute;,&iacute;mos,&iacute;steis,yeron",      // (cre&iacute;,cre&iacute;ste)
   ".$Consonant a|e|o + \u00ed -> \u00ed,\u00edste,y\u00f3,\u00edmos,\u00edsteis,yeron",      // (cre\u00ed,cre\u00edste)
//cc    ".$Consonant + &iacute; -> &iacute;,iste,i&oacute;,imos,isteis,ieron",            // ()
   ".$Consonant + \u00ed -> \u00ed,iste,i\u00f3,imos,isteis,ieron",            // ()
   ".$Vowel j + e -> e,iste,o,imos,isteis,eron",               // (traje,trajeron)
   ".$Vowel $Consonant + e -> e,iste,o,imos,isteis,ieron",     // (tuve,tuvieron)

        };

  String[] IrIrregPret = {//":ir-irreg-pret",
   // generate preterite forms from preterite 1st sing Irregular Verbs
   // Assume: input is 1st sing. preterite

//cc    "# d < o >  r m + &iacute; -> &iacute;,iste,<u>/_i&oacute;,imos,isteis,<u>/_ieron",  // (dormiste,durmi&oacute;)
   "# d < o >  r m + \u00ed -> \u00ed,iste,<u>/_i\u00f3,imos,isteis,<u>/_ieron",  // (dormiste,durmi\u00f3)
//cc    "< i > +$Consonant + &iacute; -> &iacute;,iste,i&oacute;,<e>/_imos,<e>/_isteis,ieron", // ()
   "< i > +$Consonant + \u00ed -> \u00ed,iste,i\u00f3,<e>/_imos,<e>/_isteis,ieron", // ()
//cc    "< u > +$Consonant + &iacute; -> &iacute;,iste,i&oacute;,<o>/_imos,<o>/_isteis,ieron",  // ()
   "< u > +$Consonant + \u00ed -> \u00ed,iste,i\u00f3,<o>/_imos,<o>/_isteis,ieron",  // ()
//cc    "< o > +$Consonant + &iacute; -> &iacute;,iste,<u>/_i&oacute;,imos,isteis,<u>/_ieron",  // (muri&oacute;,murieron)
   "< o > +$Consonant + \u00ed -> \u00ed,iste,<u>/_i\u00f3,imos,isteis,<u>/_ieron",  // (muri\u00f3,murieron)
//cc    "< e > *$Consonant + &iacute; -> &iacute;,iste,<i>/_i&oacute;,imos,isteis,<i>/_ieron",     // (ped&iacute;,pidi&oacute;,re&iacute;,ri&oacute;,sintieron)
   "< e > *$Consonant + \u00ed -> \u00ed,iste,<i>/_i\u00f3,imos,isteis,<i>/_ieron",     // (ped\u00ed,pidi\u00f3,re\u00ed,ri\u00f3,sintieron)
//cc    ".$Letter $Consonant + &iacute; -> &iacute;,iste,i&oacute;,isteis,ieron",         // (sal&iacute;,salieron)
   ".$Letter $Consonant + \u00ed -> \u00ed,iste,i\u00f3,isteis,ieron",         // (sal\u00ed,salieron)
//cc    ".$Vowel g u + &iacute; -> &iacute;,iste,i&oacute;,isteis,ieron",                 // (distingu&iacute;,distingui&oacute;)
   ".$Vowel g u + \u00ed -> \u00ed,iste,i\u00f3,isteis,ieron",                 // (distingu\u00ed,distingui\u00f3)
//cc    ".$Vowel + &uuml; &iacute; -> &uuml;&iacute;,&uuml;&iacute;ste,uy&oacute;,&uuml;imos,&uuml;isteis,uyeron",        // (arg&uuml;&iacute;s,arguyeron)
   ".$Vowel + \u00fc \u00ed -> \u00fc\u00ed,\u00fc\u00edste,uy\u00f3,\u00fcimos,\u00fcisteis,uyeron",        // (arg\u00fc\u00eds,arguyeron)
//cc    "*$Consonant o|u + &iacute; -> &iacute;,&iacute;ste,y&oacute;,&iacute;mos,&iacute;steis,yeron",        // (huir -> hu&iacute;,huy&oacute;,huimos,oy&oacute;,o&iacute;ste)
   "*$Consonant o|u + \u00ed -> \u00ed,\u00edste,y\u00f3,\u00edmos,\u00edsteis,yeron",        // (huir -> hu\u00ed,huy\u00f3,huimos,oy\u00f3,o\u00edste)
   ".$Consonant u + i -> i,iste,e,imos,isteis,eron",            // (fui,fueron)
   ".$Vowel + j e -> je,jiste,jo,jimos,jisteis,jeron",          // (produje,dijeron)
   ".$Vowel + c e -> ce,ciste,zo,cimos,cisteis,cieron",         // (hice,hizo)
   ".$Vowel $Consonant + e -> e,iste,o,imos,isteis,ieron",      // (vine, viste, vino)

        };


  String[] ArPresSubj = {//":ar-pres-subj",
  //generate present subjunctive for -ar regular verbs
  // Assume: input is infinitive stem

//cc     " .$Letter + &iacute;|i -> &iacute;e,&iacute;es,iemos,i&eacute;is,&iacute;en",                     // (env&iacute;e -iar verbs take stressed &iacute; in 1/2/3sg,3pl)
    " .$Letter + \u00ed|i -> \u00ede,\u00edes,iemos,i\u00e9is,\u00eden",                     // (env\u00ede -iar verbs take stressed \u00ed in 1/2/3sg,3pl)
//cc     " .$Letter ~g + u -> &uacute;e,&uacute;es,uemos,u&eacute;is,&uacute;en",                   // (contin&uacute;e -uar verbs take stressed &uacute; in 1/2/3sg,3pl)
    " .$Letter ~g + u -> \u00fae,\u00faes,uemos,u\u00e9is,\u00faen",                   // (contin\u00fae -uar verbs take stressed \u00fa in 1/2/3sg,3pl)
//cc     "< e > + z -> <ie>/_ce,<ie>/_ces,cemos,c&eacute;is,<ie>/_cen",        // (empezar -> empieces,empecemos)
    "< e > + z -> <ie>/_ce,<ie>/_ces,cemos,c\u00e9is,<ie>/_cen",        // (empezar -> empieces,empecemos)
//cc     "< e > n +$Consonant # -> <ie>/_e,<ie>/_es,emos,&eacute;is,<ie>/_en",  // (piense,pensemos)
    "< e > n +$Consonant # -> <ie>/_e,<ie>/_es,emos,\u00e9is,<ie>/_en",  // (piense,pensemos)
//cc     ".$Vowel + z -> ce,ces,cemos,c&eacute;is,cen",                         // (cazar -> caces)
    ".$Vowel + z -> ce,ces,cemos,c\u00e9is,cen",                         // (cazar -> caces)
//cc     "< o > + z -> <ue>/_ce,<ue>/_ces,cemos,c&eacute;is,<ue>/_cen",         // (almuerce,almorcemos,averg&uuml;eces))
    "< o > + z -> <ue>/_ce,<ue>/_ces,cemos,c\u00e9is,<ue>/_cen",         // (almuerce,almorcemos,averg\u00fceces))
//cc     "< o > n +$Consonant # -> <ue>/_e,<ue>/_es,emos,&eacute;is,<ue>/_en",    // (cuento,contemos)
    "< o > n +$Consonant # -> <ue>/_e,<ue>/_es,emos,\u00e9is,<ue>/_en",    // (cuento,contemos)
//cc     "< u > g # -> <ue>/_ue,<ue>/_ues,uemos,u&eacute;is,<ue>/_uen",         // (juego,juguemos)
    "< u > g # -> <ue>/_ue,<ue>/_ues,uemos,u\u00e9is,<ue>/_uen",         // (juego,juguemos)
//cc     "< o > l|n|r|s *$Consonant # -> <ue>/_e,<ue>/_es,emos,u&eacute;is,<ue>/_en",   // (cuestes,costemos)
    "< o > l|n|r|s *$Consonant # -> <ue>/_e,<ue>/_es,emos,u\u00e9is,<ue>/_en",   // (cuestes,costemos)
//cc     ".$Vowel g # -> ue,ues,uemos,u&eacute;is,uen",                                  // (pago -> pague,paguen)
    ".$Vowel g # -> ue,ues,uemos,u\u00e9is,uen",                                  // (pago -> pague,paguen)
//cc     ".$Vowel + c -> que,ques,quemos,qu&eacute;is,quen",                           // (busco -> busque,busquen)
    ".$Vowel + c -> que,ques,quemos,qu\u00e9is,quen",                           // (busco -> busque,busquen)
//cc     ".$Letter < e > +$Consonant # -> <ie>/_e,<ie>/_es,emos,&eacute;is,<ie>/_en",  // (calientes,calentemos)
    ".$Letter < e > +$Consonant # -> <ie>/_e,<ie>/_es,emos,\u00e9is,<ie>/_en",  // (calientes,calentemos)
//cc     ".$Vowel *$Consonant # -> e,es,emos,&eacute;is,en",                           // (hable,hables,hablemos,habl&eacute;s,hablen)
    ".$Vowel *$Consonant # -> e,es,emos,\u00e9is,en",                           // (hable,hables,hablemos,habl\u00e9s,hablen)

        };

 String[] ArIrregPresSubj = {//":ar-irreg-pres-subj",
  //generate present subjunctive for -ar irregular verbs
  // Assume: input is 1st sing present from Verb Lookup Table

//cc     "# d + o y -> &eacute;,es,emos,eis,en",                             // (d&eacute;,demos)
    "# d + o y -> \u00e9,es,emos,eis,en",                             // (d\u00e9,demos)
//cc     "# e s t + o y -> &eacute;,&eacute;s,emos,&eacute;is,&eacute;n",                         // (est&eacute;)
    "# e s t + o y -> \u00e9,\u00e9s,emos,\u00e9is,\u00e9n",                         // (est\u00e9)
//cc     " .$Letter + &iacute;|i ?o -> &iacute;e,&iacute;es,iemos,i&eacute;is,&iacute;en",               // (env&iacute;e -iar verbs take stressed &iacute; in 1/2/3sg,3pl)
    " .$Letter + \u00ed|i ?o -> \u00ede,\u00edes,iemos,i\u00e9is,\u00eden",               // (env\u00ede -iar verbs take stressed \u00ed in 1/2/3sg,3pl)
//cc     "< i e > + z ?o -> ce,ces,<e>/_cemos,<e>/_c&eacute;is,cen",         // (empieces,empecemos)
    "< i e > + z ?o -> ce,ces,<e>/_cemos,<e>/_c\u00e9is,cen",         // (empieces,empecemos)
//cc     "< i e > n +$Consonant + ?o -> e,es,<e>/_emos,<e>/_&eacute;is,en",  // (piense,pensemos)
    "< i e > n +$Consonant + ?o -> e,es,<e>/_emos,<e>/_\u00e9is,en",  // (piense,pensemos)
//cc     ".$Vowel + z ?o -> ce,ces,cemos,c&eacute;is,cen",                   // (cazar -> caces)
    ".$Vowel + z ?o -> ce,ces,cemos,c\u00e9is,cen",                   // (cazar -> caces)
//cc     "< u|&uuml; e > + z ?o -> ce,ces,<o>/_cemos,<o>/_c&eacute;is,_cen",      // (almuerce,almorcemos,averg&uuml;eces))
    "< u|\u00fc e > + z ?o -> ce,ces,<o>/_cemos,<o>/_c\u00e9is,_cen",      // (almuerce,almorcemos,averg\u00fceces))
//cc     "< u e > n +$Consonant + ?o -> e,es,<o>/_emos,<o>/_&eacute;is,en",  // (cuento,contemos)
    "< u e > n +$Consonant + ?o -> e,es,<o>/_emos,<o>/_\u00e9is,en",  // (cuento,contemos)
//cc     "< u e > g + ?o -> ue,ues,<u>/_uemos,<u>/_u&eacute;is,uen",         // (juego,juguemos)
    "< u e > g + ?o -> ue,ues,<u>/_uemos,<u>/_u\u00e9is,uen",         // (juego,juguemos)
//cc     ".$Vowel g + ?o -> ue,ues,uemos,u&eacute;is,uen",                   // (pago -> pague,paguen)
    ".$Vowel g + ?o -> ue,ues,uemos,u\u00e9is,uen",                   // (pago -> pague,paguen)
//cc     ".$Vowel + c ?o -> que,ques,quemos,qu&eacute;is,quen",              // (busco -> busque,busquen)
    ".$Vowel + c ?o -> que,ques,quemos,qu\u00e9is,quen",              // (busco -> busque,busquen)
//cc     ".$Vowel *$Consonant + ?o -> e,es,emos,&eacute;is,en",              // ()
    ".$Vowel *$Consonant + ?o -> e,es,emos,\u00e9is,en",              // ()

        };

 String[] ErPresSubj = {//":er-pres-subj",
  //generate present subjunctive for -er regular verbs
  // Assume: input is infinitive stem
    
//cc     ".$Vowel + c -> zca,zcas,zcamos,zc&aacute;is,zcan",                           // (crezca)
    ".$Vowel + c -> zca,zcas,zcamos,zc\u00e1is,zcan",                           // (crezca)
//cc     ".$Vowel + g -> ja,jas,jamos,j&aacute;is,jan",                                // (coja,cojas)
    ".$Vowel + g -> ja,jas,jamos,j\u00e1is,jan",                                // (coja,cojas)
//cc     "< e > l|n|r *$Consonant # -> <ie>/_a,<ie>/_as,amos,&aacute;is,<ie>/_an",     //(pierda,perd&aacute;is)
    "< e > l|n|r *$Consonant # -> <ie>/_a,<ie>/_as,amos,\u00e1is,<ie>/_an",     //(pierda,perd\u00e1is)
//cc     " < e > n d # -> <ie>/_a,a,<ie>/_as,as,amos,&aacute;is,<ie>/_an,an",          // (encienda, BUT aprenda)
    " < e > n d # -> <ie>/_a,a,<ie>/_as,as,amos,\u00e1is,<ie>/_an,an",          // (encienda, BUT aprenda)
//cc     "< o > d|l|n *$Consonant # -> <ue>/_a,a,<ue>/_as,as,amos,&aacute;is,<ue>/_an,an",     //(volvamos,huelas,corras,esconda)
    "< o > d|l|n *$Consonant # -> <ue>/_a,a,<ue>/_as,as,amos,\u00e1is,<ue>/_an,an",     //(volvamos,huelas,corras,esconda)
//cc     ".$Vowel *$Consonant # -> a,as,amos,&aacute;is,an",                           //(coma,comas)
    ".$Vowel *$Consonant # -> a,as,amos,\u00e1is,an",                           //(coma,comas)

        };


  String[] ErIrregPresSubj = {//":er-irreg-pres-subj",
  //generate present subjunctive for -er irregular verbs
  // Assume: input is 1st sing present from Verb Lookup Table
    
//cc     "< s &eacute; > # -> (:er-irreg-pres-subj)<sep>",                             // (s&eacute; -> sepa,sepas)
    "< s \u00e9 > # -> (:er-irreg-pres-subj)<sep>",                             // (s\u00e9 -> sepa,sepas)
//cc     "# h + e -> aya,ayas,ayamos,&aacute;yeis,ayan",                               // (he (haber) -> haya)
    "# h + e -> aya,ayas,ayamos,\u00e1yeis,ayan",                               // (he (haber) -> haya)
//cc     ".$Consonant < o > y # -> <a>/_a,<a>/_as,<a>/_amos,<&aacute;>/_eis,<a>/_an",  // (vaya,vayas)  
    ".$Consonant < o > y # -> <a>/_a,<a>/_as,<a>/_amos,<\u00e1>/_eis,<a>/_an",  // (vaya,vayas)  
//cc     "< i e > l|n|r *$Consonant + ?o -> a,as,<e>/_amos,<e>/_&aacute;is,an",        //(pierda,perd&aacute;is)
    "< i e > l|n|r *$Consonant + ?o -> a,as,<e>/_amos,<e>/_\u00e1is,an",        //(pierda,perd\u00e1is)
//cc     " < e > n d + ?o -> <ie>/_a,a,<ie>/_as,as,amos,&aacute;is,<ie>/_an,an",       // (encienda, BUT aprenda)
    " < e > n d + ?o -> <ie>/_a,a,<ie>/_as,as,amos,\u00e1is,<ie>/_an,an",       // (encienda, BUT aprenda)
//cc     "< u e > d|l|n *$Consonant + ?o -> a,as,<o>/_amos,<o>/_&aacute;is,_an",     //(volvamos,huelas,holamos,podamos)
    "< u e > d|l|n *$Consonant + ?o -> a,as,<o>/_amos,<o>/_\u00e1is,_an",     //(volvamos,huelas,holamos,podamos)
//cc     ".$Vowel *$Consonant + ?o -> a,as,amos,&aacute;is,an",                      //()
    ".$Vowel *$Consonant + ?o -> a,as,amos,\u00e1is,an",                      //()

        };

   String[] IrPresSubj = {//":ir-pres-subj",
  //generate present subjunctive for -ir regular verbs 
  // Assume: input is infinitive
    
//cc     "< e > n t # -> <ie>/_a,<ie>/_as,<i>/_amos,<i>/_&aacute;is,<ie>/_an",         // (siento,sintamos)
    "< e > n t # -> <ie>/_a,<ie>/_as,<i>/_amos,<i>/_\u00e1is,<ie>/_an",         // (siento,sintamos)
//cc     "< e > l|n|r *$Consonant # -> <ie>/_a,<ie>/_as,amos,&aacute;is,<ie>/_an",     //(pierda,perd&aacute;is)
    "< e > l|n|r *$Consonant # -> <ie>/_a,<ie>/_as,amos,\u00e1is,<ie>/_an",     //(pierda,perd\u00e1is)
//cc     "< e > n d + ?o -> <ie>/_a,a,<ie>/_as,as,amos,&aacute;is,<ie>/_an,an",        // (encienda, BUT aprenda)
    "< e > n d + ?o -> <ie>/_a,a,<ie>/_as,as,amos,\u00e1is,<ie>/_an,an",        // (encienda, BUT aprenda)
//cc     "< o > d|l|n *$Consonant # -> <ue>/_a,<ue>/_as,amos,&aacute;is,<ue>/_an",      //(volvamos,huelas,holamos,podamos)
    "< o > d|l|n *$Consonant # -> <ue>/_a,<ue>/_as,amos,\u00e1is,<ue>/_an",      //(volvamos,huelas,holamos,podamos)
//cc     "< o > *$Consonant # -> <ue>/_a,<ue>/_as,<u>/_amos,<o>/_amos,<u>/_&aacute;is,<o>/_&aacute;is,<ue>/_an",  // (muera,muramos,cueza,cozamos)
    "< o > *$Consonant # -> <ue>/_a,<ue>/_as,<u>/_amos,<o>/_amos,<u>/_\u00e1is,<o>/_\u00e1is,<ue>/_an",  // (muera,muramos,cueza,cozamos)
//cc     ".$Vowel $Consonant # -> a,as,amos,&aacute;is,an",                          // (viva,vivas)
    ".$Vowel $Consonant # -> a,as,amos,\u00e1is,an",                          // (viva,vivas)

        };

  String[] IrIrregPresSubj = {//":ir-irreg-pres-subj",
  //generate present subjunctive for -ir irregular verbs 
  // Assume: input is 1st sing present from Verb Lookup Table
    
//cc     "# d < u e > r m + ?o -> a,as,<u>/_amos,<u>/_&aacute;is,_an",                // (duerma,durmamos)
    "# d < u e > r m + ?o -> a,as,<u>/_amos,<u>/_\u00e1is,_an",                // (duerma,durmamos)
//cc     "f i n + g ?o -> ja,jas,jamos,j&aacute;is,jan",                               // (fingir -> finja)
    "f i n + g ?o -> ja,jas,jamos,j\u00e1is,jan",                               // (fingir -> finja)
//cc     ".$Consonant < o > y # -> <a>/_a,<a>/_as,<a>/_amos,<&aacute;>/_eis,<a>/_an",  // (vaya,vayas)  
    ".$Consonant < o > y # -> <a>/_a,<a>/_as,<a>/_amos,<\u00e1>/_eis,<a>/_an",  // (vaya,vayas)  
//cc     "< i e > n t + ?o -> a,as,<i>/_amos,<i>/_&aacute;is,an",                      // (siento,sintamos)
    "< i e > n t + ?o -> a,as,<i>/_amos,<i>/_\u00e1is,an",                      // (siento,sintamos)
//cc     "< i e > l|n|r *$Consonant + ?o -> a,as,<e>/_amos,<e>/_&aacute;is,an",        //(pierda,perd&aacute;is,encienda)
    "< i e > l|n|r *$Consonant + ?o -> a,as,<e>/_amos,<e>/_\u00e1is,an",        //(pierda,perd\u00e1is,encienda)
//cc     " < e > n d + ?o -> a,as,amos,&aacute;is,an",                                  // (aprenda)
    " < e > n d + ?o -> a,as,amos,\u00e1is,an",                                  // (aprenda)
//cc     "< ?h u e > d|l|n *$Consonant + ?o -> a,as,<o>/_amos,<o>/_&aacute;is,_an",     //(volvamos,huelas,holamos,podamos)
    "< ?h u e > d|l|n *$Consonant + ?o -> a,as,<o>/_amos,<o>/_\u00e1is,_an",     //(volvamos,huelas,holamos,podamos)
//cc     "< u e > *$Consonant + ?o -> a,as,<u>/_amos,<o>/_amos,<u>/_&aacute;is,<o>/_&aacute;is,an",           // (muera,muramos,cueza,cozamos)
    "< u e > *$Consonant + ?o -> a,as,<u>/_amos,<o>/_amos,<u>/_\u00e1is,<o>/_\u00e1is,an",           // (muera,muramos,cueza,cozamos)
//cc     ".$Letter + &iacute; ?o -> &iacute;a,&iacute;as,iamos,i&aacute;is,&iacute;an",                              // (r&iacute;a,riamos)
    ".$Letter + \u00ed ?o -> \u00eda,\u00edas,iamos,i\u00e1is,\u00edan",                              // (r\u00eda,riamos)
//cc     ".$Vowel *$Consonant + ?o -> a,as,amos,&aacute;is,an",                          // ()
    ".$Vowel *$Consonant + ?o -> a,as,amos,\u00e1is,an",                          // ()

        };

  String[] ArImpSubj = {//":ar-imp-subj",
  // generate imperfect subjunctive for -ar regular verbs
  // Assume: input is infinitive stem

//cc     ".$Vowel $Letter # -> ara,aras,&aacute;ramos,arais,aran,ase,ases,&aacute;semos,aseis,asen",    // (hablara,enviases)
    ".$Vowel $Letter # -> ara,aras,\u00e1ramos,arais,aran,ase,ases,\u00e1semos,aseis,asen",    // (hablara,enviases)

        };

  String[] ArIrregImpSubj = {//":ar-irreg-imp-subj",
  // generate imperfect subjunctive for -ar irregular verbs
  // Assume: input is 1st sing preterite

//cc     ".$Consonant + q u &eacute; -> cara,caras,c&aacute;ramos,carais,caran,case,cases,c&aacute;semos,caseis,casen",  // (busqu&eacute; -> buscara)
    ".$Consonant + q u \u00e9 -> cara,caras,c\u00e1ramos,carais,caran,case,cases,c\u00e1semos,caseis,casen",  // (busqu\u00e9 -> buscara)
//cc     ".$Consonant g + u &eacute; -> ara,aras,&aacute;ramos,arais,aran,ase,ases,&aacute;semos,aseis,asen",  // (pagu&eacute; -> pagara)
    ".$Consonant g + u \u00e9 -> ara,aras,\u00e1ramos,arais,aran,ase,ases,\u00e1semos,aseis,asen",  // (pagu\u00e9 -> pagara)
//cc     ".$Vowel + c ?&eacute;|e -> zara,zaras,z&aacute;ramos,zarais,zaran,zase,zases,z&aacute;semos,zaseis,zase,zasen,cara,caras,c&aacute;ramos,carais,caran,case,cases,c&aacute;semos,caseis,case,casen",  // (root z, z->c/e almorzara, c->c arrancar)
    ".$Vowel + c ?\u00e9|e -> zara,zaras,z\u00e1ramos,zarais,zaran,zase,zases,z\u00e1semos,zaseis,zase,zasen,cara,caras,c\u00e1ramos,carais,caran,case,cases,c\u00e1semos,caseis,case,casen",  // (root z, z->c/e almorzara, c->c arrancar)
//cc     ".$Letter u v + e -> iera,ieras,i&eacute;ramos,ierais,ieran,iese,ieses,i&eacute;semos,eiseis,iesen",  // (estuviera,estuviese)
    ".$Letter u v + e -> iera,ieras,i\u00e9ramos,ierais,ieran,iese,ieses,i\u00e9semos,eiseis,iesen",  // (estuviera,estuviese)
//cc     "# d + i -> iera,ieras,i&eacute;ramos,ierais,ieran,iese,ieses,i&eacute;semos,eiseis,iesen",     // (diere,dieses)
    "# d + i -> iera,ieras,i\u00e9ramos,ierais,ieran,iese,ieses,i\u00e9semos,eiseis,iesen",     // (diere,dieses)
//cc     ".$Vowel $Consonant + ?&eacute;|e -> ara,aras,&aacute;ramos,arais,aran,ase,ases,&aacute;semos,aseis,asen",
    ".$Vowel $Consonant + ?\u00e9|e -> ara,aras,\u00e1ramos,arais,aran,ase,ases,\u00e1semos,aseis,asen",
//cc     ".$Letter i + ?&eacute;|e -> ara,aras,&aacute;ramos,arais,aran,ase,ases,&aacute;semos,aseis,asen",  // (enviara,enviases)
    ".$Letter i + ?\u00e9|e -> ara,aras,\u00e1ramos,arais,aran,ase,ases,\u00e1semos,aseis,asen",  // (enviara,enviases)

        };

  String[] ErImpSubj = {//":er-imp-subj",
  // generate imperfect subjunctive for -er regular verbs
  // Assume: input is infinitive stem

//cc       //  ".$Consonant j + e -> era,eras,&eacute;ramos,erais,eran,ese,eses,&eacute;semos,eseis,esen",  // ()
      //  ".$Consonant j + e -> era,eras,\u00e9ramos,erais,eran,ese,eses,\u00e9semos,eseis,esen",  // ()
//cc       //  ".$Consonant u + i -> era,eras,&eacute;ramos,erais,eran,ese,eses,&eacute;semos,eseis,esen",  // ()
      //  ".$Consonant u + i -> era,eras,\u00e9ramos,erais,eran,ese,eses,\u00e9semos,eseis,esen",  // ()
//cc       //  ".$Consonant + &uuml; &iacute; -> uyera,uyeras,uy&eacute;ramos,uyerais,uyeran,uyese,uyeses,uy&eacute;semos,uyeseis,uyesen",  // ()
      //  ".$Consonant + \u00fc \u00ed -> uyera,uyeras,uy\u00e9ramos,uyerais,uyeran,uyese,uyeses,uy\u00e9semos,uyeseis,uyesen",  // ()
//cc    "*$Consonant $Vowel #  -> yera,yeras,y&eacute;ramos,yerais,yeran,yese,yeses,y&eacute;semos,yeseis,yesen",  //(le(er),leyiera)
   "*$Consonant $Vowel #  -> yera,yeras,y\u00e9ramos,yerais,yeran,yese,yeses,y\u00e9semos,yeseis,yesen",  //(le(er),leyiera)
//cc    ".$Letter $Consonant -> iera,ieras,i&eacute;ramos,ierais,ieran,iese,ieses,i&eacute;semos,ieseis,iesen",    // (comiera,metiese)
   ".$Letter $Consonant -> iera,ieras,i\u00e9ramos,ierais,ieran,iese,ieses,i\u00e9semos,ieseis,iesen",    // (comiera,metiese)

        };

  String[] ErIrregImpSubj = {//":er-irreg-imp-subj",
  // generate imperfect subjunctive for -er irregular verbs
  // Assume: input is 1st sing preterite

//cc    ".$Consonant j + e -> era,eras,&eacute;ramos,erais,eran,ese,eses,&eacute;semos,eseis,esen",  // (trajera,trajeras)
   ".$Consonant j + e -> era,eras,\u00e9ramos,erais,eran,ese,eses,\u00e9semos,eseis,esen",  // (trajera,trajeras)
//cc    ".$Consonant + &uuml; &iacute; -> uyera,uyeras,uy&eacute;ramos,uyerais,uyeran,uyese,uyeses,uy&eacute;semos,uyeseis,uyesen",  // ()
   ".$Consonant + \u00fc \u00ed -> uyera,uyeras,uy\u00e9ramos,uyerais,uyeran,uyese,uyeses,uy\u00e9semos,uyeseis,uyesen",  // ()
//cc    "*$Consonant $Vowel + &iacute; -> yera,yeras,y&eacute;ramos,yerais,yeran,yese,yeses,y&eacute;semos,yeseis,yesen",  //(proveyera)
   "*$Consonant $Vowel + \u00ed -> yera,yeras,y\u00e9ramos,yerais,yeran,yese,yeses,y\u00e9semos,yeseis,yesen",  //(proveyera)
//cc    ".$Letter + ?e|&iacute;|i|&eacute; -> iera,ieras,i&eacute;ramos,ierais,ieran,iese,ieses,i&eacute;semos,ieseis,iesen",  // (vi,hice)
   ".$Letter + ?e|\u00ed|i|\u00e9 -> iera,ieras,i\u00e9ramos,ierais,ieran,iese,ieses,i\u00e9semos,ieseis,iesen",  // (vi,hice)

        };

  String[] IrImpSubj = {//":ir-imp-subj",
  // generate imperfect subjunctive for -ir regular verbs
  // Assume: input is infinitive stem

   ".$Letter < e > +$Consonant # -> (:ir-imp-subj)<i>",        // (reg(ir) -> rigiera))
   ".$Letter < o > +$Consonant # -> (:ir-imp-subj)<o>",       // ()
//cc    ".$Consonant + &uuml; -> uyera,uyeras,uy&eacute;ramos,uyerais,uyeran,uyese,uyeses,uy&eacute;semos,uyeseis,uyesen",  // ()
   ".$Consonant + \u00fc -> uyera,uyeras,uy\u00e9ramos,uyerais,uyeran,uyese,uyeses,uy\u00e9semos,uyeseis,uyesen",  // ()
//cc    "*$Consonant $Vowel # -> yera,yeras,y&eacute;ramos,yerais,yeran,yese,yeses,y&eacute;semos,yeseis,yesen",  //()
   "*$Consonant $Vowel # -> yera,yeras,y\u00e9ramos,yerais,yeran,yese,yeses,y\u00e9semos,yeseis,yesen",  //()
//cc    ".$Letter # -> iera,ieras,i&eacute;ramos,ierais,ieran,iese,ieses,i&eacute;semos,ieseis,iesen",  // (viviera,viviese)
   ".$Letter # -> iera,ieras,i\u00e9ramos,ierais,ieran,iese,ieses,i\u00e9semos,ieseis,iesen",  // (viviera,viviese)

        };

  String[] IrIrregImpSubj = {//":ir-irreg-imp-subj",
  // generate imperfect subjunctive for irregular -ir verbs
  // Assume: input is 1st sing preterite

//cc    "< r e &iacute; > # -> (:ir-irreg-imp-subj)<rei>",                      // (re&iacute; -> reiera,reises)
   "< r e \u00ed > # -> (:ir-irreg-imp-subj)<rei>",                      // (re\u00ed -> reiera,reises)
//cc    ".$Letter < e > +$Consonant + &iacute; -> (:ir-irreg-imp-subj)<i>",       // (sent&iacute; -> sintiera)
   ".$Letter < e > +$Consonant + \u00ed -> (:ir-irreg-imp-subj)<i>",       // (sent\u00ed -> sintiera)
//cc    ".$Letter < o > +$Consonant + &iacute; -> (:ir-irreg-imp-subj)<u>",       // (muriera,durmiese)
   ".$Letter < o > +$Consonant + \u00ed -> (:ir-irreg-imp-subj)<u>",       // (muriera,durmiese)
//cc    ".$Consonant j + e -> era,eras,&eacute;ramos,erais,eran,ese,eses,&eacute;semos,eseis,esen",  // (trajera,trajeras)
   ".$Consonant j + e -> era,eras,\u00e9ramos,erais,eran,ese,eses,\u00e9semos,eseis,esen",  // (trajera,trajeras)
//cc    ".$Consonant u + i -> era,eras,&eacute;ramos,erais,eran,ese,eses,&eacute;semos,eseis,esen",  // (fuera,fuesen)
   ".$Consonant u + i -> era,eras,\u00e9ramos,erais,eran,ese,eses,\u00e9semos,eseis,esen",  // (fuera,fuesen)
//cc    ".$Consonant + &uuml; &iacute; -> uyera,uyeras,uy&eacute;ramos,uyerais,uyeran,uyese,uyeses,uy&eacute;semos,uyeseis,uyesen",  // (arguyera)
   ".$Consonant + \u00fc \u00ed -> uyera,uyeras,uy\u00e9ramos,uyerais,uyeran,uyese,uyeses,uy\u00e9semos,uyeseis,uyesen",  // (arguyera)
//cc    "*$Consonant $Vowel + &iacute; -> yera,yeras,y&eacute;ramos,yerais,yeran,yese,yeses,y&eacute;semos,yeseis,yesen",  //(huyera,proveyera,oyese)
   "*$Consonant $Vowel + \u00ed -> yera,yeras,y\u00e9ramos,yerais,yeran,yese,yeses,y\u00e9semos,yeseis,yesen",  //(huyera,proveyera,oyese)
//cc    ".$Letter + ?e|&iacute;|i|&eacute; -> iera,ieras,i&eacute;ramos,ierais,ieran,iese,ieses,i&eacute;semos,ieseis,iesen",     // ()
   ".$Letter + ?e|\u00ed|i|\u00e9 -> iera,ieras,i\u00e9ramos,ierais,ieran,iese,ieses,i\u00e9semos,ieseis,iesen",     // ()

        };


  String[] ArFutSubj = {//":ar-fut-subj",
  // generate future subjunctive for -ar verbs
  // Assume: input is stem of infinitive (all verbs)

//cc      "# e s t + a -> uviere,uvieres,uvi&eacute;remos,uviereis,uvieren",  // (estuviere,estuvieres)
     "# e s t + a -> uviere,uvieres,uvi\u00e9remos,uviereis,uvieren",  // (estuviere,estuvieres)
//cc      "# d + a -> iere,ieres,i&eacute;remos,iereis,ieren",        // (dar -> diere,dieres)
     "# d + a -> iere,ieres,i\u00e9remos,iereis,ieren",        // (dar -> diere,dieres)
//cc      ".$Vowel $Letter # -> are,ares,&aacute;remos,areis,aren",   // (hablares,hablaren,platearen)
     ".$Vowel $Letter # -> are,ares,\u00e1remos,areis,aren",   // (hablares,hablaren,platearen)

        };

  String[] ErFutSubj = {//":er-fut-subj",
  // generate future subjunctive for -er/ir verbs
  // Assume: input is stem of infinitive (all verbs)

     "< h a c > #  -> (:er-fut-subj)<hic>",      // (hiciere)
     "< h a b > # -> (:er-fut-subj)<hub>",       // (hubiere)
     "< t r a > # -> (:er-fut-subj)<traj>",      // (trajere,trajeres)
     "< c a b > # -> (:er-fut-subj)<cup>",       // (cupiere,cupieres)
     "< s a b > # -> (:er-fut-subj)<sup>",       // (supiere,supieres)
     "< t e n > # -> (:er-fut-subj)<tuv>",       // (tuviere)
     "< p o d > # -> (:er-fut-subj)<pud>",       // (pudiere)
     "< p o n > # -> (:er-fut-subj)<pus>",       // (pusiere)
     "< q u e r > # -> (:er-fut-subj)<quis>",    // (quisiere)  
//cc      ".$Letter j # -> ere,eres,&eacute;remos,ereis,eren",               // (trajeres)
     ".$Letter j # -> ere,eres,\u00e9remos,ereis,eren",               // (trajeres)
//cc      ".$Letter a|i|o|u # -> yere,yeres,y&eacute;remos,yereis,yeren",    // (cayere)
     ".$Letter a|i|o|u # -> yere,yeres,y\u00e9remos,yereis,yeren",    // (cayere)
//cc      ".$Letter # -> iere,ieres,i&eacute;remos,iereis,ieren"             // (comiere,cimieren)
     ".$Letter # -> iere,ieres,i\u00e9remos,iereis,ieren"             // (comiere,cimieren)

        };


  String[] IrFutSubj = {//":ir-fut-subj",
  // generate future subjunctive for -er/ir verbs
  // Assume: input is stem of infinitive (all verbs)

     "< d e c > # -> (:ir-fut-subj)<dij>",         // (dijieres)
     "< d o r m > # -> (:ir-fut-subj)<durm>",      // (durmiere)
     "< m o r > # -> (:ir-fut-subj)<mur>",         // (murieren)
     "< s e g u > # -> (:ir-fut-subj)<sigu>",      // (sigiere)
     "< s e n t > # -> (:ir-fut-subj)<sin>",       // (sintiere)
     "< v e n > # -> (:ir-fut-subj)<vin>",         // (viniere)
    
//cc      ".$Letter + &uuml; -> uyere,uyeres,uy&eacute;remos,uyereis,uyeren",     // (arguyere,arguy&eacute;remos)
     ".$Letter + \u00fc -> uyere,uyeres,uy\u00e9remos,uyereis,uyeren",     // (arguyere,arguy\u00e9remos)
//cc      ".$Vowel g u # -> iere,ieres,i&eacute;remos,iereis,ieren",         // (distinguiere)
     ".$Vowel g u # -> iere,ieres,i\u00e9remos,iereis,ieren",         // (distinguiere)
//cc      ".$Letter a|i|o|u # -> yere,yeres,y&eacute;remos,yereis,yeren",    // (huyere)
     ".$Letter a|i|o|u # -> yere,yeres,y\u00e9remos,yereis,yeren",    // (huyere)
//cc      ".$Letter # -> iere,ieres,i&eacute;remos,iereis,ieren"             // (viviere)
     ".$Letter # -> iere,ieres,i\u00e9remos,iereis,ieren"             // (viviere)

        };

  String[] ArImperative = {//":ar-imperative",
  // generate imperative form -ar regular verbs
  // Assume: input is infinitive stem
//cc     ".$Vowel + i -> &iacute;a,ies,ie,iad,iemos,i&eacute;is,ien",     // (enviad,env&iacute;a)
    ".$Vowel + i -> \u00eda,ies,ie,iad,iemos,i\u00e9is,ien",     // (enviad,env\u00eda)
//cc     ".$Vowel ~g + u -> &uacute;a,ues,ue,uad,uemos,u&eacute;is,uen",  // (continuad,contin&uacute;a))
    ".$Vowel ~g + u -> \u00faa,ues,ue,uad,uemos,u\u00e9is,uen",  // (continuad,contin\u00faa))
//cc     "$Consonant < o > +$Consonant # -> <ue>/_a,a,ad,<ue>/_es,es,<ue>/_e,e,emos,&eacute;is,<ue>/_en,en",   //(acuerda,aborda)
    "$Consonant < o > +$Consonant # -> <ue>/_a,a,ad,<ue>/_es,es,<ue>/_e,e,emos,\u00e9is,<ue>/_en,en",   //(acuerda,aborda)
//cc     "$Consonant < e > g # -> ad,<ie>/_a,a,<ie>/_ue,ue,<ie>/_ues,ues,<ie>/_uen,uen,uemos,u&eacute;is",     // (negad,niegue)
    "$Consonant < e > g # -> ad,<ie>/_a,a,<ie>/_ue,ue,<ie>/_ues,ues,<ie>/_uen,uen,uemos,u\u00e9is",     // (negad,niegue)
//cc     ".$Letter + c -> ad,a,que,ques,quemos,qu&eacute;is,quen",      // (acercad,acerques)
    ".$Letter + c -> ad,a,que,ques,quemos,qu\u00e9is,quen",      // (acercad,acerques)
//cc     ".$Vowel $Consonant # -> a,es,e,ad,emos,&eacute;is,en",        // (habla,hables)
    ".$Vowel $Consonant # -> a,es,e,ad,emos,\u00e9is,en",        // (habla,hables)

        };

 String[] ErImperative = {//":er-imperative",
  // generate imperative form -er regular verbs
  // Assume: input is infinitive stem 

//cc     "< e > n d # ->  <ie>/_e,<ie>/_as,<ie>/_a,amos,&aacute;is,<ie>/_an",                       // (entiende,entienda)
    "< e > n d # ->  <ie>/_e,<ie>/_as,<ie>/_a,amos,\u00e1is,<ie>/_an",                       // (entiende,entienda)
//cc     "$Consonant < o > $Consonant # -> ed,<ue>/_e,<ue>/_as,<ue>/_a,amos,&aacute;is,<ue>/_an",   // (moved,meuva)
    "$Consonant < o > $Consonant # -> ed,<ue>/_e,<ue>/_as,<ue>/_a,amos,\u00e1is,<ue>/_an",   // (moved,meuva)
//cc     ".$Vowel $Consonant #  -> ed,e,as,a,amos,&aacute;is,an",  // (comed,comamos)
    ".$Vowel $Consonant #  -> ed,e,as,a,amos,\u00e1is,an",  // (comed,comamos)
//cc     ".$Consonant $Vowel # -> d,as,a,amos,&aacute;is,an",      // ()
    ".$Consonant $Vowel # -> d,as,a,amos,\u00e1is,an",      // ()

        };

 String[] IrImperative = {//":ir-imperative",
  // generate imperative form -ir regular verbs
  // Assume: input is infinitive stem

//cc      "$Consonant < e > $Consonant # -> id,<i>/_e,<ie>/_e,<i>/_as,<ie>/_as,<i>/_a,<ie>/_a,amos,&aacute;is,<i>/_an,<ie>/_an",
     "$Consonant < e > $Consonant # -> id,<i>/_e,<ie>/_e,<i>/_as,<ie>/_as,<i>/_a,<ie>/_a,amos,\u00e1is,<i>/_an,<ie>/_an",
//cc      "$Consonant < o > $Consonant #  -> id,<ue>/_e,<ue>/_as,<ue>/_a,<o>/_amos,<o>/_&aacute;is,<ue>/_an",   // ()
     "$Consonant < o > $Consonant #  -> id,<ue>/_e,<ue>/_as,<ue>/_a,<o>/_amos,<o>/_\u00e1is,<ue>/_an",   // ()
//cc      ".$Vowel $Consonant #  -> id,e,as,a,amos,&aacute;is,an",     // (vivir -> vivid,vive)
     ".$Vowel $Consonant #  -> id,e,as,a,amos,\u00e1is,an",     // (vivir -> vivid,vive)
//cc      ".$Consonant $Vowel # -> d,as,a,amos,&aacute;is,an",         // ()
     ".$Consonant $Vowel # -> d,as,a,amos,\u00e1is,an",         // ()

        };

   String[] ArIrregImperative = {//":ar-irreg-imperative",
  // generate imperative form -ar irregular  verbs
  // Assume: input is present 1sg, Note: most imperatives forms same as present subjunctive

//cc     ".$Letter < i e > +$Consonant + o -> a,<e>/_ad,e,es,<e>/_emos,<e>/_&eacute;is,en",           // (pienso,pensad)
    ".$Letter < i e > +$Consonant + o -> a,<e>/_ad,e,es,<e>/_emos,<e>/_\u00e9is,en",           // (pienso,pensad)
//cc     ".$Vowel < u e > +$Consonant + z o -> za,<o>/_zad,ce,ces,<o>/_cemos,<o>/_c&eacute;is,cen",   // (almorza,almuercen)
    ".$Vowel < u e > +$Consonant + z o -> za,<o>/_zad,ce,ces,<o>/_cemos,<o>/_c\u00e9is,cen",   // (almorza,almuercen)
//cc     ".$Vowel < u e > +$Consonant + o -> a,<o>/_ad,e,es,<o>/_emos,<o>/_&eacute;is,en",   // (cuenta,contad)
    ".$Vowel < u e > +$Consonant + o -> a,<o>/_ad,e,es,<o>/_emos,<o>/_\u00e9is,en",   // (cuenta,contad)
//cc     ".$Letter + o -> a,ad,e,es,emos,&eacute;is,en",    // (anda,anden)
    ".$Letter + o -> a,ad,e,es,emos,\u00e9is,en",    // (anda,anden)
        };


  String[] ErIrregImperative = {//":er-irreg-imperative",
  // generate imperative form -er irregular  verbs
  // Assume: input is present 1sg, Note: most imperatives forms same as present subjunctive

//cc     ".$Letter $Vowel + i g o -> e,ed,iga,igas,igamos,ig&aacute;is,igan",    // (trae,traed,traigo)
    ".$Letter $Vowel + i g o -> e,ed,iga,igas,igamos,ig\u00e1is,igan",    // (trae,traed,traigo)
//cc     ".$Vowel $Consonant + g o -> _,e,ed,ga,gas,gamos,g&aacute;is,gan",      // (vale,valed,valgas,pon) 
    ".$Vowel $Consonant + g o -> _,e,ed,ga,gas,gamos,g\u00e1is,gan",      // (vale,valed,valgas,pon) 
//cc     ".$Consonant a|e + o -> e,ed,a,as,amos,&aacute;is,an",                  // (proveed)
    ".$Consonant a|e + o -> e,ed,a,as,amos,\u00e1is,an",                  // (proveed)
//cc     ".$Letter < i e > +$Consonant + o -> e,<e>/_ed,a,as,<e>/_amos,<e>/_&aacute;is,an",  // (entienda,entended)
    ".$Letter < i e > +$Consonant + o -> e,<e>/_ed,a,as,<e>/_amos,<e>/_\u00e1is,an",  // (entienda,entended)
//cc     ".$Vowel < u e > +$Consonant + o -> e,<o>/_ed,a,as,<o>/_amos,<o>/_&aacute;is,an",  // (pueda,poded)
    ".$Vowel < u e > +$Consonant + o -> e,<o>/_ed,a,as,<o>/_amos,<o>/_\u00e1is,an",  // (pueda,poded)

        };

  String[] IrIrregImperative = {//":ir-irreg-imperative",
  // generate imperative form -ir irregular  verbs
  // Assume: input is present,1sg,

//cc     "a r g < u > + y o -> <&uuml;>/_e,<&uuml;>/_id,a,as,amos,&aacute;is,an",                    // (arg&uuml;id,arguas) 
    "a r g < u > + y o -> <\u00fc>/_e,<\u00fc>/_id,a,as,amos,\u00e1is,an",                    // (arg\u00fcid,arguas) 
//cc     " u < y > + o -> e,<i>/_d,a,as,<>/_amos,&aacute;is,an",                            // (huye,huamos,huid)
    " u < y > + o -> e,<i>/_d,a,as,<>/_amos,\u00e1is,an",                            // (huye,huamos,huid)
//cc     ".$Letter < i j > + o -> <ig>/_e,<eg>/_id,a,as,<ig>/_amos,<ej>/_&aacute;is,an",    // (elige,elegid,alej&aacute;is)
    ".$Letter < i j > + o -> <ig>/_e,<eg>/_id,a,as,<ig>/_amos,<ej>/_\u00e1is,an",    // (elige,elegid,alej\u00e1is)
//cc     ".$Letter < i > + g o -> <y>/_e,<&iacute;>/_d,ga,gas,gamos,g&aacute;is,gan",               // (o&iacute;d,oye,oigan)
    ".$Letter < i > + g o -> <y>/_e,<\u00ed>/_d,ga,gas,gamos,g\u00e1is,gan",               // (o\u00edd,oye,oigan)
//cc     ".$Letter $Consonant + g o -> _,e,id,ga,gas,gamos,g&aacute;is,gan",                // (salgo,salid)
    ".$Letter $Consonant + g o -> _,e,id,ga,gas,gamos,g\u00e1is,gan",                // (salgo,salid)
//cc     ".$Letter < i > +$Consonant + o -> e,a,as,an,<e>/_amos,<i>/_&aacute;is,<e>/_id",   // (pida,pedid,vestamos,vistas)
    ".$Letter < i > +$Consonant + o -> e,a,as,an,<e>/_amos,<i>/_\u00e1is,<e>/_id",   // (pida,pedid,vestamos,vistas)
//cc     ".$Letter < i e > +$Consonant + o -> e,a,as,an,<i>/_amos,<i>/_&aacute;is,<e>/_id", // (sentid,sienta)
    ".$Letter < i e > +$Consonant + o -> e,a,as,an,<i>/_amos,<i>/_\u00e1is,<e>/_id", // (sentid,sienta)
//cc     ".$Letter < &iacute; > + o -> e,<e&iacute;>/_d,a,as,<i>/_amos,<i>/_&aacute;is,an",               // (re&iacute;d)
    ".$Letter < \u00ed > + o -> e,<e\u00ed>/_d,a,as,<i>/_amos,<i>/_\u00e1is,an",               // (re\u00edd)
//cc     ".$Letter < u e > +$Consonant + o -> e,a,as,an,<u>/_amos,<u>/_&aacute;is,<o>/_id", // (dormid,durm&aacute;is)
    ".$Letter < u e > +$Consonant + o -> e,a,as,an,<u>/_amos,<u>/_\u00e1is,<o>/_id", // (dormid,durm\u00e1is)

        };

     
 String[] ArReflexImperativeGerund =  {//":ar-reflex-imperative-gerund",
  // generate reflexive forms for -ar verbs, generates only forms where pronoun is connected to verb
  // Assume: stem of imperative form, vowel, consonant changes may be present

//cc      ".$Letter < u &eacute; > -> <o>/_arse,<o>/_&aacute;ndose,ate,<o>/_&aacute;monos,<o>/_aos,ese,ense",           // (acostarse,acuestese)
     ".$Letter < u \u00e9 > -> <o>/_arse,<o>/_\u00e1ndose,ate,<o>/_\u00e1monos,<o>/_aos,ese,ense",           // (acostarse,acuestese)
//cc      ".$Letter < i &eacute; > + g -> <e>/_arse,<e>/_&aacute;ndose,ate,<e>/_gu&eacute;monos,<e>/_aos,guese,guense", // (ni&eacute;gate,negaos)
     ".$Letter < i \u00e9 > + g -> <e>/_arse,<e>/_\u00e1ndose,ate,<e>/_gu\u00e9monos,<e>/_aos,guese,guense", // (ni\u00e9gate,negaos)
//cc      ".$Letter < i &eacute; > -> <e>/_arse,<e>/_&aacute;ndose,ate,<e>/_&aacute;monos,<e>/_aos,ese,ense",  // ()
     ".$Letter < i \u00e9 > -> <e>/_arse,<e>/_\u00e1ndose,ate,<e>/_\u00e1monos,<e>/_aos,ese,ense",  // ()
//cc      ".$Letter # -> arse,&aacute;ndose,ate,&eacute;monos,aos,ese,ense",  // (apearse,ap&eacute;ate,ap&eacute;ese)
     ".$Letter # -> arse,\u00e1ndose,ate,\u00e9monos,aos,ese,ense",  // (apearse,ap\u00e9ate,ap\u00e9ese)
    
        };


 String[] ErReflexImperativeGerund =  {//":er-reflex-imperative-gerund",
  // generate reflexive forms for -er verbs, generates only forms where pronoun is connected to verb
  // Assume: stem of imperative form, vowel, consonant changes may be present

//cc      ".$Letter < u &eacute; > -> <o>/_erse,<o>/_i&eacute;ndose,ete,<o>/_&aacute;monos,<o>/_&iacute;os,ase,anse",  // (mu&eacute;vete,mov&iacute;os)
     ".$Letter < u \u00e9 > -> <o>/_erse,<o>/_i\u00e9ndose,ete,<o>/_\u00e1monos,<o>/_\u00edos,ase,anse",  // (mu\u00e9vete,mov\u00edos)
//cc      ".$Letter < o|&oacute; > n + ?g -> <o>/_erse,<o>/_i&eacute;ndose,<o>/_te,<o>/_&aacute;monos,<o>/_eos,<&oacute;>/_gase,<&oacute;>/_ganse", //(ponte,p&oacute;ngase)
     ".$Letter < o|\u00f3 > n + ?g -> <o>/_erse,<o>/_i\u00e9ndose,<o>/_te,<o>/_\u00e1monos,<o>/_eos,<\u00f3>/_gase,<\u00f3>/_ganse", //(ponte,p\u00f3ngase)
//cc      ".$Letter < i &eacute; > -> <e>/_erse,<e>/_i&eacute;ndose,ete,<e>/_&aacute;monos,<e>/_eos,ase,anse",  // (?)
     ".$Letter < i \u00e9 > -> <e>/_erse,<e>/_i\u00e9ndose,ete,<e>/_\u00e1monos,<e>/_eos,ase,anse",  // (?)
//cc      ".$Letter # -> erse,i&eacute;ndose,te,&aacute;monos,eos,ase,anse",  // (?)
     ".$Letter # -> erse,i\u00e9ndose,te,\u00e1monos,eos,ase,anse",  // (?)
    
        };

 String[] IrReflexImperativeGerund =  {//":ir-reflex-imperative-gerund",
  // generate reflexive forms for -er verbs, generates only forms where pronoun is connected to verb
  // Assume: stem of imperative form, vowel, consonant changes may be present

//cc      ".$Letter < u &eacute; > -> <o>/_irse,<u>/_i&eacute;ndose,ete,<u>/_&aacute;monos,<o>/_&iacute;os,ase,anse",   // (du&eacute;rmete,dorm&iacute;os)
     ".$Letter < u \u00e9 > -> <o>/_irse,<u>/_i\u00e9ndose,ete,<u>/_\u00e1monos,<o>/_\u00edos,ase,anse",   // (du\u00e9rmete,dorm\u00edos)
//cc      ".$Letter < i &eacute; > -> <e>/_irse,<i>/_i&eacute;ndose,ete,<i>/_&aacute;monos,<e>/_&iacute;os,ase,anse",   // (si&eacute;ntete,sent&iacute;os)
     ".$Letter < i \u00e9 > -> <e>/_irse,<i>/_i\u00e9ndose,ete,<i>/_\u00e1monos,<e>/_\u00edos,ase,anse",   // (si\u00e9ntete,sent\u00edos)
//cc      ".$Letter # -> irse,i&eacute;ndose,ete,&aacute;monos,&iacute;os,ase,anse",  // (dec&iacute;date,dec&iacute;danse)
     ".$Letter # -> irse,i\u00e9ndose,ete,\u00e1monos,\u00edos,ase,anse",  // (dec\u00eddate,dec\u00eddanse)
    
        };

  String[] AllCond = {//":all-cond",
   //generate conditionals
   //Assume: input is infinitive form, -ar,-er,-ir
   "< v a l > + e r -> (:all-cond)<valdr>",              // (valer -> valdr)
   "< d e c > + i r -> (:all-cond)<dir>",                // (decir -> dir)
   "< h a c > + e r -> (:all-cond)<har>",                // (hacer -> har)
//cc    ".$Letter + b e r -> br&iacute;a,br&iacute;as,br&iacute;amos,br&iacute;ais,br&iacute;an",  // (cabr&iacute;a)
   ".$Letter + b e r -> br\u00eda,br\u00edas,br\u00edamos,br\u00edais,br\u00edan",  // (cabr\u00eda)
//cc    ".$Letter $Vowel + d e r -> dr&iacute;a,dr&iacute;as,dr&iacute;amos,dr&iacute;ais,dr&iacute;an",  // (podr&iacute;a) Need - aprender&iacute;a
   ".$Letter $Vowel + d e r -> dr\u00eda,dr\u00edas,dr\u00edamos,dr\u00edais,dr\u00edan",  // (podr\u00eda) Need - aprender\u00eda
//cc    ".$Letter n + e r -> dr&iacute;a,dr&iacute;as,dr&iacute;amos,dr&iacute;ais,dr&iacute;an",  // (pondr&iacute;a)
   ".$Letter n + e r -> dr\u00eda,dr\u00edas,dr\u00edamos,dr\u00edais,dr\u00edan",  // (pondr\u00eda)
//cc    ".$Letter l|n + i r -> dr&iacute;a,dr&iacute;as,dr&iacute;amos,dr&iacute;ais,dr&iacute;an",  // (salir -> saldr&iacute;a,venir -> vendr&iacute;a)
   ".$Letter l|n + i r -> dr\u00eda,dr\u00edas,dr\u00edamos,dr\u00edais,dr\u00edan",  // (salir -> saldr\u00eda,venir -> vendr\u00eda)
//cc    ".$Consonant r + e r  -> r&iacute;a,r&iacute;as,r&iacute;amos,r&iacute;ais,r&iacute;an",    // (querr&iacute;a)
   ".$Consonant r + e r  -> r\u00eda,r\u00edas,r\u00edamos,r\u00edais,r\u00edan",    // (querr\u00eda)
//cc    ".$Consonant a|e|i r # -> &iacute;a,&iacute;as,&iacute;amos,&iacute;ais,&iacute;an",  // (negar -> negar&iacute;a,negar&iacute;amos)
   ".$Consonant a|e|i r # -> \u00eda,\u00edas,\u00edamos,\u00edais,\u00edan",  // (negar -> negar\u00eda,negar\u00edamos)
//cc    ".$Letter # -> &iacute;a,&iacute;as,&iacute;amos,&iacute;ais,&iacute;an",             // (dir&iacute;a,valdr&iacute;as))
   ".$Letter # -> \u00eda,\u00edas,\u00edamos,\u00edais,\u00edan",             // (dir\u00eda,valdr\u00edas))

        };

  String[] AllFuture = {//":all-future",
   //generate conditionals
   //Assume: input is infinitive form, -ar,-er,-ir

   "< v a l > + e r -> (:all-future)<valdr>",   // (valer -> valdr)
   "< d e c > + i r -> (:all-future)<dir>",      // (decir -> dir)
   "< h a c > + e r -> (:all-future)<har>",      // (hacer -> har)
//cc    ".$Letter + b e r -> br&eacute;,br&aacute;s,br&aacute;,bremos,br&eacute;is,br&aacute;n",  // (cabr&eacute;)
   ".$Letter + b e r -> br\u00e9,br\u00e1s,br\u00e1,bremos,br\u00e9is,br\u00e1n",  // (cabr\u00e9)
//cc    ".$Letter $Vowel + d e r -> dr&eacute;,dr&aacute;s,dr&aacute;,dremos,dr&eacute;is,dr&aacute;n",  // (podr&aacute;n) Need - comprender&aacute;n
   ".$Letter $Vowel + d e r -> dr\u00e9,dr\u00e1s,dr\u00e1,dremos,dr\u00e9is,dr\u00e1n",  // (podr\u00e1n) Need - comprender\u00e1n
//cc    ".$Letter n +  e r -> dr&eacute;,dr&aacute;s,dr&aacute;,dremos,dr&eacute;is,dr&aacute;n",  // (pondr&aacute;n)
   ".$Letter n +  e r -> dr\u00e9,dr\u00e1s,dr\u00e1,dremos,dr\u00e9is,dr\u00e1n",  // (pondr\u00e1n)
//cc    ".$Letter l|n +  i r -> dr&eacute;,dr&aacute;s,dr&aacute;,dremos,dr&eacute;is,dr&aacute;n",  // (salir -> saldr&eacute;, venir -> vendr&eacute;)
   ".$Letter l|n +  i r -> dr\u00e9,dr\u00e1s,dr\u00e1,dremos,dr\u00e9is,dr\u00e1n",  // (salir -> saldr\u00e9, venir -> vendr\u00e9)
//cc    ".$Consonant r + e r -> r&eacute;,r&aacute;s,r&aacute;,remos,r&eacute;is,r&aacute;n",      // (querr&eacute;)
   ".$Consonant r + e r -> r\u00e9,r\u00e1s,r\u00e1,remos,r\u00e9is,r\u00e1n",      // (querr\u00e9)
//cc    ".$Consonant a|e|i r # -> &eacute;,&aacute;s,&aacute;,emos,&eacute;is,&aacute;n",  // (negar -> negar&eacute;,negaremos)
   ".$Consonant a|e|i r # -> \u00e9,\u00e1s,\u00e1,emos,\u00e9is,\u00e1n",  // (negar -> negar\u00e9,negaremos)
//cc    ".$Letter # -> &eacute;,&aacute;s,&aacute;,emos,&eacute;is,&aacute;n",             // (valdr&eacute;.dir&aacute;)
   ".$Letter # -> \u00e9,\u00e1s,\u00e1,emos,\u00e9is,\u00e1n",             // (valdr\u00e9.dir\u00e1)

        };

  String[] genLiteral = {//":literal",
  // method returns the form itself

  ".$Vowel -> _",

        };

   String[] AllForms = {//":all-forms",
  // generate all Spanish forms

  ".$Vowel -> (:reg-paradigm)_,(:noun-sg)_,(:noun-pl)_,(:adj)_",  // when all else fails

        };

 // defRules Statements

        defRules(":unnamed", rootRules);
        defRules(":literal", genLiteral);
        defRules(":noun-pl", NounPl);
        defRules(":noun-sg", NounSg);
        defRules(":remove-accent", removeAccent);
        defRules(":add-accent", addAccent);
        defRules(":adj", Adj);

        defRules(":reg-paradigm", RegParadigm);
        defRules(":reg-ar-paradigm", RegArParadigm);
        defRules(":reg-er-paradigm", RegErParadigm);
        defRules(":reg-ir-paradigm", RegIrParadigm);
        defRules(":ambig-er-ir-paradigm", AmbigErIrParadigm);
        defRules(":ambig-ar-er-paradigm", AmbigArErParadigm);
        defRules(":ambig-ar-er-ir-paradigm", AmbigArErIrParadigm);

        defRules(":ar-prog-part", ArProgPart);
        defRules(":er-prog-part", ErProgPart);
        defRules(":ir-prog-part", IrProgPart);
        defRules(":irreg-prog-part", IrregProgPart);

        defRules(":ar-pres", ArPres);
        defRules(":er-pres", ErPres);
        defRules(":ir-pres", IrPres);

        defRules(":ar-irreg-pres", ArIrregPres);
        defRules(":er-irreg-pres", ErIrregPres);
        defRules(":ir-irreg-pres", IrIrregPres);

        defRules(":ar-imperf", ArImperf);
        defRules(":er-ir-imperf", ErIrImperf);
        defRules(":all-cond", AllCond);
        defRules(":all-future", AllFuture);

        defRules(":ar-pret", ArPret);
        defRules(":er-pret", ErPret);
        defRules(":ir-pret", IrPret);

        defRules(":ar-irreg-pret", ArIrregPret);
        defRules(":er-irreg-pret", ErIrregPret);
        defRules(":ir-irreg-pret", IrIrregPret);

        defRules(":ar-pres-subj", ArPresSubj);
        defRules(":er-pres-subj", ErPresSubj);
        defRules(":ir-pres-subj", IrPresSubj);

        defRules(":ar-irreg-pres-subj", ArIrregPresSubj);
        defRules(":er-irreg-pres-subj", ErIrregPresSubj);
        defRules(":ir-irreg-pres-subj", IrIrregPresSubj);

        defRules(":ar-imp-subj", ArImpSubj);
        defRules(":er-imp-subj", ErImpSubj);
        defRules(":ir-imp-subj", IrImpSubj);

        defRules(":ar-irreg-imp-subj", ArIrregImpSubj);
        defRules(":er-irreg-imp-subj", ErIrregImpSubj);
        defRules(":ir-irreg-imp-subj", IrIrregImpSubj);

        defRules(":ar-fut-subj", ArFutSubj);
        defRules(":er-fut-subj", ErFutSubj);
        defRules(":ir-fut-subj", IrFutSubj);

        defRules(":ar-imperative", ArImperative);
        defRules(":er-imperative", ErImperative);
        defRules(":ir-imperative", IrImperative);

        defRules(":ar-irreg-imperative", ArIrregImperative);
        defRules(":er-irreg-imperative", ErIrregImperative);
        defRules(":ir-irreg-imperative", IrIrregImperative);

        defRules(":ar-reflex-imperative-gerund", ArReflexImperativeGerund);
        defRules(":er-reflex-imperative-gerund", ErReflexImperativeGerund);
        defRules(":ir-reflex-imperative-gerund", IrReflexImperativeGerund);

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
     * that the rules are well-formed.  It is a static final variable so
     * that the compiler will optimize the extra code away when the variable
     * is false so that the run-time class files will be smaller.  When
     * authorFlag is false, all of the code associated with the tracing
     * mechanism will automatically be eliminated by the compiler.
     *
     * See the LiteMorphRule.java file for more information.
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
        if ( authorFlag && traceFlag ) {
            System.out.println("LiteMorph_es: " + str);
        }
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
