package com.sun.labs.minion.util;


import java.util.logging.Logger;

/**
 * A utility class to manipulate strings.
 */
public class StringUtil {

    private static final Logger logger = Logger.getLogger(StringUtil.class.getName());

    public static String truncate(String s) {
        return truncate(s, 65);
    }

    public static String truncate(String s, int len) {
        if(s.length() <= len) {
            return s;
        }
        int p = s.lastIndexOf(' ', len);
        if(p < 0) {
            return s.substring(0, len);
        } else {
            return s.substring(0, p) + "...";
        }
    }

    public static String wrap(String s) {
        return wrap(s, "  ", 65);
    }

    public static String wrap(String s, String prefix, int len) {
        if(s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        String[] ws = s.split("\\s+");
        sb.append(prefix);
        int l = prefix.length();
        boolean newLine = true;
        for(String w : ws) {
            if(l + w.length() + 1 >= len) {
                sb.append("\n");
                sb.append(prefix);
                newLine = true;
                l = prefix.length();
            }
            if(!newLine) {
                sb.append(" ");
            }
            sb.append(w);
            newLine = false;
            l += ((newLine ? 0 : 1) + w.length());
        }
        return sb.toString();
    }

    public static String toString(String[] s) {
        return toString(s, " ");
    }

    public static String toString(String[] s, String join) {
        if(s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < s.length; i++) {
            if(i > 0) {
                sb.append(join);
            }
            sb.append(s[i]);
        }
        return sb.toString();
    }
}
