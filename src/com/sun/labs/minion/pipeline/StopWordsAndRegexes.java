package com.sun.labs.minion.pipeline;


import com.sun.labs.util.props.ConfigStringList;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A class that contains stop words and stop regexes.
 */
public class StopWordsAndRegexes extends StopWords {

    private static final Logger logger = Logger.getLogger(StopWordsAndRegexes.class.getName());

    @ConfigStringList(mandatory = false)
    public static final String PROP_REGEX_FILES = "regex_files";
    
    private List<Pattern> patterns = new ArrayList<Pattern>();

    public void addRegexFile(String regexFile) {
        BufferedReader reader = null;
        try {
            InputStream is = getClass().getResourceAsStream(regexFile);
            if(is != null) {
                reader = new BufferedReader(new InputStreamReader(is));
            } else {
                reader = new BufferedReader(new FileReader(regexFile));
            }
            for(String curr = reader.readLine();
                    curr != null; curr = reader.readLine()) {
                curr = curr.trim();
                if(!curr.isEmpty() && curr.charAt(0) != '#') {
                    try {
                        patterns.add(Pattern.compile(curr));
                    } catch(PatternSyntaxException ex) {
                        logger.warning(String.format(
                                "Invalid pattern %s in %s, ignoring", curr,
                                                     regexFile));
                    }
                }
            }
        } catch(Exception ex) {
            logger.warning("Error reading stop regexes: " + ex.getMessage());
        } finally {
            try {
                if(reader != null) {
                    reader.close();
                }
            } catch(IOException ex) {
            }
        }

    }

    @Override
    public boolean isStop(String s) {
        if(super.isStop(s)) {
            return true;
        }

        for(Pattern p : patterns) {
            if(p.matcher(s).matches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        List<String> rf = ps.getStringList(PROP_REGEX_FILES);
        for(String regexFile : rf) {
            addRegexFile(regexFile);
        }
    }
}
