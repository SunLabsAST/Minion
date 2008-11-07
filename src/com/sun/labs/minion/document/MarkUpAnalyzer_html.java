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

package com.sun.labs.minion.document;

import java.io.Reader;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.StringTokenizer;
import com.sun.labs.minion.document.tokenizer.UniversalTokenizer;
import com.sun.labs.minion.FieldInfo;
import java.util.HashSet;
import java.util.Set;
import com.sun.labs.minion.pipeline.Stage;
import com.sun.labs.minion.util.MinionLog;
import com.sun.labs.minion.util.Util;

/**
 * A <code>MarkUpAnalyzer</code> for HTML.  The heavy lifting is done by
 * the HTML parser in the Swing toolkit.  We just transform their events
 * into ours.
 */
public class MarkUpAnalyzer_html extends MarkUpAnalyzer {

    /**
     * A buffer to hold characters read from the file.
     */
    protected char[] buffer = new char[4096];

    /**
     * A single character array to pass down entities.
     */
    protected char[] entChar = new char[1];

    /**
     * The end of the current fileBuffer.  We can't be sure to read a
     * buffer full of characters each time.
     */
    protected int bufferEnd;

    /**
     * Whether we should index text or not.
     */
    protected boolean indexText = true;

    protected boolean stopIndexAtEnd = false;

    /**
     * The head of the pipeline that we'll use for processing text.
     */
    protected Stage stage;

    protected static com.sun.labs.minion.util.MinionLog log = com.sun.labs.minion.util.MinionLog.getLog();
    protected static String logTag = "HTML";

    public MarkUpAnalyzer_html(Reader r, int pos, String key) {
        super(r, pos, key);
    }

    public MarkUpAnalyzer_html(String s, String key) {
        super(s, key);
    }

    public char read()
            throws java.io.IOException {

        if(bufferPos >= bufferEnd) {

            try {
                bufferEnd = r.read(buffer, 0, buffer.length);
            } catch(java.io.IOException ioe) {

                //
                // NON-FATAL ERROR REPORT.
                // IOException reading document, discontinuing.  Just act like
                // we hit EOF.
                MinionLog.getLog().
                        error("HTML", 1, "Error reading " + key + " " + pos);
                bufferEnd = -1;
            }

            if(bufferEnd < 0) {
                return (char) -1;
            }

            bufferPos = 0;
        }

        pos++;
        return buffer[bufferPos++];
    }


    public void addCharToText(char c) {
        if(textPos + 1 >= text.length) {
            text = Util.expandChar(text, text.length * 2);
        }
        text[textPos++] = c;
    }

    /**
     * Analyzes the document, passing the text onto the given tokenizer.
     */
    public void analyze(Stage stage)
            throws java.io.IOException {

        this.stage = stage;

        char c;

        //
        // Process text until the end of file is encountered.
        while((c = read()) != (char) -1) {

            switch(c) {

                case '<':
                    //
                    // Check to see whether we need to parse out the addresses
                    // for mailing lists and store them as a multivalued field.
                    if(atAddrTag) {
                        if(textPos > 0) {
                            String addrs =
                                    new String(text, 0, textPos);
                            StringTokenizer st =
                                    new StringTokenizer(addrs, ", ");
                            while(st.hasMoreTokens()) {
                                String addr = st.nextToken();
                                stage.startField(addrField);
                                stage.text(addr.toCharArray(), 0, addr.length());
                                stage.endField(addrField);
                            }
                        }
                        textPos = 0;
                        atAddrTag = false;
                    } else {
                        tokenizeBuffer();
                    }
                    parseTag();
                    textFilePos = pos;
                    break;
                case '&':
                    tokenizeBuffer();
                    parseEntity();
                    textFilePos = pos;
                    break;
                default:
                    addCharToText(c);
                    break;
            }
        }
        tokenizeBuffer();
    }

    /**
     * Parses an HTML tag, either begin or end.  We don't care about
     * structure or verification, just position.
     *
     * @throws java.io.IOException If there is any error reading from the
     * file.
     */
    protected void parseTag()
            throws java.io.IOException {

        char c;
        boolean end = false;
        int tagLen = 0;
        boolean firstChar = true;
        boolean done = false;
        String tagString = null;

        int eBegin = pos - 1;
        int eEnd;

        c = read();

        //
        // If the first character is a !, then this is a comment tag.
        if(c == '!') {
            parseComment();
            return;
        }

        //
        // If the next character is /, then this is an end tag.
        if(c == '/') {
            end = true;
            c = read();
            if(c == (char) -1) {
                addCharToText('<');
                addCharToText('/');
                return;
            }
        }

        //
        // If the next character is not a letter, we collect and go.
        if(!Character.isLetter(c)) {
            addCharToText('<');
            if(end) {
                addCharToText('/');
            }
            addCharToText(c);
            return;
        }

        //
        // Read characters until the next whitespace.  This is the tag.
        while(c != (char) -1 && c != '>' &&
              !UniversalTokenizer.isWhitespace(c)) {
            if(tagLen >= tag.length) {
                tag = Util.expandChar(tag, tag.length * 2);
            }
            tag[tagLen++] = c;
            c = read();
        }

        //
        // Parse to the tag close.  We collect the attributes so that we
        // can catch things like link targets.
        int attrLen = 0;
        while(c != (char) -1 && c != '>') {
            if(attrLen >= attr.length) {
                attr = Util.expandChar(attr, attr.length * 2);
            }
            attr[attrLen++] = c;
            c = read();
        }

        eEnd = pos;

        tagString = (new String(tag, 0, tagLen)).toLowerCase();
        
        //
        // Space like things add a space.
        if(spaceSet.contains(tagString)) {
            addCharToText(' ');
            return;
        }

        //
        // If we're at the start of the address tag, then note that fact.
        atAddrTag = tagString.equals(addrTag) && !end;

        //
        // Check to see if we're indexing attributes of a meta tag
        if (!end && tagString.equals("meta")) {
            String name = null;
            String content = null;
            String attrStr = new String(attr, 0, attrLen);
            while (attrStr.contains("=")) {
                //
                // Read the first attribute name
                int eq = attrStr.indexOf("=");
                String param = attrStr.substring(0, eq);
                
                //
                // Get the next quoted string out
                int quote1 = attrStr.indexOf("\"");
                if (quote1 < 0) {
                    break;
                }
                int quote2 = attrStr.indexOf("\"", quote1 + 1);
                if (quote2 < 0) {
                    break;
                }
                String val = attrStr.substring(quote1 + 1, quote2);
                
                //
                // Strip out the part of attrStr we've parsed
                if (attrStr.length() > quote2) {
                    attrStr = attrStr.substring(quote2 + 1, attrStr.length());
                } else {
                    attrStr = "";
                }

                if (param.trim().toLowerCase().equals("name")) {
                    name = val;
                } else if (param.trim().toLowerCase().equals("content")) {
                    content = val;
                }
            }
            if (name != null && content != null) {
                FieldInfo fi = fieldMap.get(name);
                if (fi != null) {
                    stage.startField(fi);
                    stage.text(content.toCharArray(), 0, content.length());
                    stage.endField(fi);
                }
            }
        }
        
        //
        // See if we're supposed to index the content of this tag
        FieldInfo fi = fieldMap.get(tagString);
        if(fi != null) {
            if(end) {
                stage.endField(fi);
                if(stopIndexAtEnd) {
                    indexText = false;
                    stopIndexAtEnd = false;
                }
            } else {
                stage.startField(fi);
                if(!indexText) {
                    indexText = true;
                    stopIndexAtEnd = true;
                }
            }
        }
    }

    /**
     * Parses and HTML comment, keeping an eye out for MHonarc comments.
     *
     * @throws java.io.IOException If there is any error reading from the
     * file.
     */
    protected void parseComment()
            throws java.io.IOException {

        int comLen = 0;
        char c = read();
        char p1;
        char p2;

        //
        // Check for the double dash
        p1 = read();
        if(c != '-' || p1 != '-') {

            //
            // We didn't find it, scan for the closing angle bracket, but
            // check for end of file!
            c = read();
            while(c != (char) -1 && c != '>') {
                c = read();
            }
            return;
        }

        //
        // Read characters until the closing angle bracket and save as a
        // comment.
        p1 = (char) 0;
        p2 = (char) 0;
        c = read();
        while(c != (char) -1) {

            //
            // Look for the closing angle bracket with double dashes preceeding.
            if(c == '>' && p1 == '-' && p2 == '-') {
                //
                // Remove the closing dashes.
                comLen -= 2;
                break;
            }
            if(comLen >= tag.length) {
                tag = Util.expandChar(tag, tag.length * 2);
            }
            tag[comLen++] = c;
            p2 = p1;
            p1 = c;
            c = read();
        }

        //
        // Look for stop/start index comments.
        if(Util.indexOf(tag, 0, comLen, indStop, 0, indStop.length, true) >= 0) {
            indexText = false;
        } else if(Util.indexOf(tag, 0, comLen, indStart, 0, indStart.length,
                               true) >= 0) {
            indexText = true;
        } else {

            //
            // Look for MHonarc comments.
            for(int i = 0; i < mhonarcComments.length;
                    i++) {
                int l = mhonarcComments[i].length;
                if(Util.startsWith(tag, 0, comLen, mhonarcComments[i], 0, l,
                                   false)) {
                    if(i < mhonarcComments.length - 1) {
                        indexText = false;

                        //
                        // Un-entify the characters and get back the new end
                        // offset.
                        comLen = unEntify(tag, l, comLen);
                        stage.startField(mhonarcFields[i]);
                        stage.text(tag, l, comLen);
                        stage.endField(mhonarcFields[i]);
                    } else {
                        indexText = true;
                    }
                    break;
                }
            }
        }
    }

    protected int unEntify(char[] a, int b, int e) {
        char c;
        int wp = b;
        byte[] encoded = null;

        //
        // Get the ISO entities.
        for(int i = b; i < e;) {
            c = a[i++];
            if(c == '&') {
                Character val = null;
                int save = i;
                c = a[i++];
                if(c == '#') {
                    int cv = 0;
                    c = a[i++];
                    while(c != ';' && i < e) {
                        cv = (cv * 10) + (c - '0');
                        c = a[i++];
                    }
                    val = new Character((char) cv);
                } else {
                    int ap = 0;
                    while(c != ';' && i < e) {
                        attr[ap++] = c;
                        c = a[i++];
                    }
                    val =   (Character) entityMap.get(new String(attr, 0, ap));
                    if(val == null) {

                        //
                        // There was a & with no following entity that we
                        // recognize, so we'll just put the characters in.
                        a[wp++] = '&';
                        i = save;
                        continue;
                    }
                }
                a[wp++] = val.charValue();
            } else {
                a[wp++] = c;
            }
        }

        //
        // Get the ISO quoted-printable encoded stuff.  We need to go
        // through a second time because the q-p stuff has been entified.
        e = wp;
        wp = b;
        for(int i = b; i < e;) {
            c = a[i++];
            if(c == '=' && a[i] == '?') {

                //
                // See if this is an ISO encoded word.  We'll quit if we
                // ever exceed the allowable length for such an encoded
                // word, which is 75, per RFC 1522:
                //
                //   http://www.faqs.org/rfcs/rfc1522.html
                //
                // For now we only handle the printed-quotable data
                // encoding.
                boolean good = true;
                int savei = i;
                int s = 0;
                int end = ++i;
                isoencoding:
                {
                    int es = end;

                    //
                    // Get the character encoding type.
                    for(s = 0; end < e && a[end] != '?' && s < 75;
                            s++, end++) {
                        ;
                    }
                    if(s >= 75 || end >= e) {
                        if(end >= e) {
                            end = e;
                        }
                        good = false;
                        break isoencoding;
                    }

                    String enc = new String(a, es, end - es);

                    //
                    // Get the data encoding type, either quoted-printable
                    // or base-64.
                    char type = Character.toLowerCase(a[++end]);

                    //
                    // Make sure we got a valid data encoding type and that
                    // it's followed by a question mark.
                    if((type != 'q' && type != 'b') || a[++end] != '?') {
                        good = false;
                        break isoencoding;
                    }

                    //
                    // OK, now we can decode the data as a set of bytes.
                    end++;
                    int bw = 0;
                    if(encoded == null) {
                        encoded = new byte[75];
                    }
                    for(s = 0; end < e && a[end] != '?' && s < 75;
                            s++, end++) {
                        char k = a[end];
                        byte bk;

                        //
                        // Decode this character.
                        switch(k) {
                            case '=':

                                //
                                // Get the two hex digits and make them a character.
                                bk =    (byte) (Character.digit(a[++end], 16) * 16 +
                                         Character.digit(a[++end], 16));
                                break;
                            case '_':

                                //
                                // ASCII space character.
                                bk = 32;
                                break;
                            default:
                                bk = (byte) k;
                                break;
                        }

                        encoded[bw++] = bk;
                    }

                    if(s >= 75 || end >= e || a[++end] != '=') {
                        if(end >= e) {
                            end = e;
                        }
                        good = false;
                        break isoencoding;
                    }

                    //
                    // Decode the bytes to characters, using the given
                    // encoding and then copy them to our character array.
                    String word = null;
                    try {
                        word = new String(encoded, 0, bw, enc);
                    } catch(java.io.UnsupportedEncodingException uee) {

                        //
                        // We don't know this encoding!
                        good = false;
                        break isoencoding;
                    }

                    word.getChars(0, word.length(), a, wp);
                    wp += word.length();
                    end++;
                }

                i = end;

                //
                // If we didn't find a valid encoding, copy over the
                // characters that we saw.
                if(!good) {
                    a[wp++] = c;
                    System.arraycopy(a, savei, a, wp, i - savei);
                    wp += i - savei;
                }
            } else {
                a[wp++] = c;
            }
        }

        //
        // Return the new end offset.
        return wp;
    }

    /**
     * Parses an HTML entity out of the data.
     *
     * @throws java.io.IOException If there is any error reading the file.
     */
    protected void parseEntity()
            throws java.io.IOException {
        char c = 0;
        int entLen = 0;

        c = read();

        //
        // Treat a bare & as ampersand.
        if(Character.isWhitespace(c)) {
            addCharToText('&');
            addCharToText(c);
            return;
        }

        while(c != (char) -1 && c != ';') {
            if(entLen >= tag.length) {
                tag = Util.expandChar(tag, tag.length * 2);
            }
            tag[entLen++] = c;
            c = read();
        }

        if(!indexText) {
            return;
        }

        Character val;

        //
        // Check for a numerical entity.
        if(tag[0] == '#') {

            //
            // Figure out the actual character.
            int charVal = 0;
            for(int i = 1; i < entLen;
                    i++) {
                charVal = (charVal * 10) + (tag[i] - '0');
            }

            val = new Character((char) charVal);
        } else {

            //
            // Alphabetic entity.  Look it up in our hash.
            val =   (Character) entityMap.get(new String(tag, 0, entLen));

            //
            // If we got null, pass along the entity as separate characters.
            if(val == null) {
                if(textPos + entLen >= text.length) {
                    text =  Util.expandChar(text,
                                            Math.max(textPos + entLen,
                                                     text.length * 2));
                }
                System.arraycopy(tag, 0, text, textPos, entLen);
                textPos += entLen;
                return;
            }
        }

        //
        // We need to account for the & and ; on the entity in our position
        // calculations.
        entLen += 2;

        //
        // Send the event down the pipe.
        entChar[0] = val.charValue();
        stage.text(entChar, 0, 1);
    }

    /**
     * Pass the contents of the buffer to the tokenizer.
     */
    protected void tokenizeBuffer() {

        //
        // Call the tokenizer.
        if(textPos > 0) {
            if(indexText) {
                stage.text(text, 0, textPos);
            }
            textPos = 0;
        }
    }

    /**
     * A buffer to hold text parsed from the file.
     */
    protected char[] text = new char[1024];

    /**
     * A buffer to hold a tag parsed from the file.
     */
    protected char[] tag = new char[32];

    /**
     * A buffer to hold tag attributes.
     */
    protected char[] attr = new char[128];

    /**
     * The current position in the buffer of characters read from the file.
     */
    protected int bufferPos = 0;

    /**
     * The current position in the text buffer that we will tokenize.
     */
    protected int textPos = 0;

    /**
     * The position of the first character of the text buffer in the file.
     */
    protected int textFilePos = 0;

    /**
     * Whether we're parsing the addresses tag.
     */
    protected boolean atAddrTag;

    /**
     * Index stopping comment.
     */
    protected static char[] indStop;

    /**
     * Index starting comment.
     */
    protected static char[] indStart;

    /**
     * The tag for mailing list addresses in techmail.
     */
    protected static String addrTag;

    /**
     * The field for the addresses tag.
     */
    protected static FieldInfo addrField;

    protected static FieldInfo dateField;

    protected static FieldInfo encField;

    /**
     * MHonArc meta-data comments.
     */
    protected static char[][] mhonarcComments;

    /**
     * MHonArc meta-data comment field information objects.
     */
    protected static FieldInfo[] mhonarcFields;

    /**
     * A hashtable that maps html tags to subtypes of our begin and end
     * markup events.
     */
    protected static HashMap pairedTagMap = null;

    /**
     * A hashtable that maps simple html tags to subtypes of our point
     * markup events.
     */
    protected static HashMap simpleTagMap = null;

    /**
     * A hashtable mapping entity names to the corresponding characters.
     */
    protected static HashMap entityMap = null;

    /**
     * A hashtable from markup event sub types to field names.
     */
    protected static HashMap<String,FieldInfo> fieldMap = null;
    
    protected static Set<String> spaceSet = null;

    int lastType;
    int lastSubType;
    boolean lastWasBegin = false;
    //
    // Static data for a tag mapping from HTML to our markup events.  This
    // is gross, but it's only done once, and then tag lookup will be
    // fast.
    static {

        indStop = "stopindex".toCharArray();
        indStart = "startindex".toCharArray();
        addrTag = "x-to-addrs";

        //
        // Some sets of attributes that we can reuse.
        EnumSet<FieldInfo.Attribute> all =
                EnumSet.allOf(FieldInfo.Attribute.class);
        all.remove(FieldInfo.Attribute.CASE_SENSITIVE);
        EnumSet<FieldInfo.Attribute> nv = all.clone();
        nv.remove(FieldInfo.Attribute.VECTORED);
        EnumSet<FieldInfo.Attribute> st =
                EnumSet.of(FieldInfo.Attribute.SAVED,
                           FieldInfo.Attribute.TRIMMED);

        addrField =
                new FieldInfo("to", st, FieldInfo.Type.STRING);

        //
        // Stuff for MHonArc mailing lists.
        mhonarcComments = new char[8][];
        mhonarcComments[0] = "X-Subject: ".toCharArray();
        mhonarcComments[1] = "X-From: ".toCharArray();
        mhonarcComments[2] = "X-Date: ".toCharArray();
        mhonarcComments[3] = "X-Derived: ".toCharArray();
        mhonarcComments[4] = "X-Message-Id: ".toCharArray();
        mhonarcComments[5] = "X-Reference: ".toCharArray();
        mhonarcComments[6] = "X-Handled-By: ".toCharArray();
        mhonarcComments[7] = "X-Body-of-Message".toCharArray();
        mhonarcFields = new FieldInfo[7];
        mhonarcFields[0] =
                new FieldInfo("subject", all,
                              FieldInfo.Type.STRING);
        mhonarcFields[1] =
                new FieldInfo("from", nv, FieldInfo.Type.STRING);
        mhonarcFields[2] =
                new FieldInfo("msg-date", st, FieldInfo.Type.DATE);
        mhonarcFields[3] =
                new FieldInfo("attach", st, FieldInfo.Type.STRING);

        mhonarcFields[4] =
                new FieldInfo("msg-id", st, FieldInfo.Type.STRING);

        mhonarcFields[5] =
                new FieldInfo("reference", st,
                              FieldInfo.Type.STRING);

        mhonarcFields[6] =
                new FieldInfo("handled", st,
                              FieldInfo.Type.STRING);

        fieldMap = new HashMap<String,FieldInfo>();
        fieldMap.put("title",
                     new FieldInfo("title", all,
                                   FieldInfo.Type.STRING));
        fieldMap.put("keywords",
                     new FieldInfo("keywords", all,
                                   FieldInfo.Type.STRING));
        fieldMap.put("description",
                     new FieldInfo("description", all,
                                   FieldInfo.Type.STRING));
        fieldMap.put("date",
                     new FieldInfo("date", st,
                                   FieldInfo.Type.DATE));
        fieldMap.put("a",
                     new FieldInfo("anchor", all,
                                   FieldInfo.Type.STRING));
        fieldMap.put("b",
                     new FieldInfo("bold", all,
                                   FieldInfo.Type.STRING));
        fieldMap.put("i",
                     new FieldInfo("italic", all,
                                   FieldInfo.Type.STRING));
        
        spaceSet = new HashSet<String>();
        spaceSet.add("br /");
        spaceSet.add("br");
        spaceSet.add("p");
        spaceSet.add("p /");

        entityMap = new HashMap();
        entityMap.put("quot", new Character((char) 34));
        entityMap.put("amp", new Character((char) 38));
        entityMap.put("lt", new Character((char) 60));
        entityMap.put("gt", new Character((char) 62));
        entityMap.put("nbsp", new Character((char) 160));
        entityMap.put("iexcl", new Character((char) 161));
        entityMap.put("cent", new Character((char) 162));
        entityMap.put("pound", new Character((char) 163));
        entityMap.put("curren", new Character((char) 164));
        entityMap.put("yen", new Character((char) 165));
        entityMap.put("brvbar", new Character((char) 166));
        entityMap.put("sect", new Character((char) 167));
        entityMap.put("uml", new Character((char) 168));
        entityMap.put("copy", new Character((char) 169));
        entityMap.put("ordf", new Character((char) 170));
        entityMap.put("laquo", new Character((char) 171));
        entityMap.put("not", new Character((char) 172));
        entityMap.put("shy", new Character((char) 173));
        entityMap.put("reg", new Character((char) 174));
        entityMap.put("macr", new Character((char) 175));
        entityMap.put("deg", new Character((char) 176));
        entityMap.put("plusmn", new Character((char) 177));
        entityMap.put("sup2", new Character((char) 178));
        entityMap.put("sup3", new Character((char) 179));
        entityMap.put("acute", new Character((char) 180));
        entityMap.put("micro", new Character((char) 181));
        entityMap.put("para", new Character((char) 182));
        entityMap.put("middot", new Character((char) 183));
        entityMap.put("cedil", new Character((char) 184));
        entityMap.put("sup1", new Character((char) 185));
        entityMap.put("ordm", new Character((char) 186));
        entityMap.put("raquo", new Character((char) 187));
        entityMap.put("frac14", new Character((char) 188));
        entityMap.put("frac12", new Character((char) 189));
        entityMap.put("frac34", new Character((char) 190));
        entityMap.put("iquest", new Character((char) 191));
        entityMap.put("Agrave", new Character((char) 192));
        entityMap.put("Aacute", new Character((char) 193));
        entityMap.put("circ", new Character((char) 194));
        entityMap.put("Atilde", new Character((char) 195));
        entityMap.put("Auml", new Character((char) 196));
        entityMap.put("ring", new Character((char) 197));
        entityMap.put("AElig", new Character((char) 198));
        entityMap.put("Ccedil", new Character((char) 199));
        entityMap.put("Egrave", new Character((char) 200));
        entityMap.put("Eacute", new Character((char) 201));
        entityMap.put("Ecirc", new Character((char) 202));
        entityMap.put("Euml", new Character((char) 203));
        entityMap.put("Igrave", new Character((char) 204));
        entityMap.put("Iacute", new Character((char) 205));
        entityMap.put("Icirc", new Character((char) 206));
        entityMap.put("Iuml", new Character((char) 207));
        entityMap.put("ETH", new Character((char) 208));
        entityMap.put("Ntilde", new Character((char) 209));
        entityMap.put("Ograve", new Character((char) 210));
        entityMap.put("Oacute", new Character((char) 211));
        entityMap.put("Ocirc", new Character((char) 212));
        entityMap.put("Otilde", new Character((char) 213));
        entityMap.put("Ouml", new Character((char) 214));
        entityMap.put("times", new Character((char) 215));
        entityMap.put("Oslash", new Character((char) 216));
        entityMap.put("Ugrave", new Character((char) 217));
        entityMap.put("Uacute", new Character((char) 218));
        entityMap.put("Ucirc", new Character((char) 219));
        entityMap.put("Uuml", new Character((char) 220));
        entityMap.put("Yacute", new Character((char) 221));
        entityMap.put("THORN", new Character((char) 222));
        entityMap.put("szlig", new Character((char) 223));
        entityMap.put("agrave", new Character((char) 224));
        entityMap.put("aacute", new Character((char) 225));
        entityMap.put("acirc", new Character((char) 226));
        entityMap.put("atilde", new Character((char) 227));
        entityMap.put("auml", new Character((char) 228));
        entityMap.put("aring", new Character((char) 229));
        entityMap.put("aelig", new Character((char) 230));
        entityMap.put("ccedil", new Character((char) 231));
        entityMap.put("egrave", new Character((char) 232));
        entityMap.put("eacute", new Character((char) 233));
        entityMap.put("ecirc", new Character((char) 234));
        entityMap.put("euml", new Character((char) 235));
        entityMap.put("igrave", new Character((char) 236));
        entityMap.put("iacute", new Character((char) 237));
        entityMap.put("icirc", new Character((char) 238));
        entityMap.put("iuml", new Character((char) 239));
        entityMap.put("ieth", new Character((char) 240));
        entityMap.put("ntilde", new Character((char) 241));
        entityMap.put("ograve", new Character((char) 242));
        entityMap.put("oacute", new Character((char) 243));
        entityMap.put("ocirc", new Character((char) 244));
        entityMap.put("otilde", new Character((char) 245));
        entityMap.put("ouml", new Character((char) 246));
        entityMap.put("divide", new Character((char) 247));
        entityMap.put("oslash", new Character((char) 248));
        entityMap.put("ugrave", new Character((char) 249));
        entityMap.put("uacute", new Character((char) 250));
        entityMap.put("ucirc", new Character((char) 251));
        entityMap.put("uuml", new Character((char) 252));
        entityMap.put("yacute", new Character((char) 253));
        entityMap.put("thorn", new Character((char) 254));
        entityMap.put("yuml", new Character((char) 255));
    }
} // MarkUpAnalyzer_html
