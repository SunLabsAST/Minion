package com.sun.labs.minion.test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 */
public class DateTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SimpleDateFormat df = new SimpleDateFormat("E, d MMM y H:m:s z");
        for(String arg : args) {
            try {
                Date date = df.parse(arg);
                System.out.format("Parsed: %s\n", date);
            } catch(ParseException ex) {
                System.out.format("Unable to parse: %s\n", arg);
            }
        }
    }

}
