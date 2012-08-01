package com.sun.labs.minion;

import com.sun.labs.util.props.ConfigComponent;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;

/**
 * A factory interface for stemmers.
 */
public class StemmerFactory implements Configurable {

    @ConfigComponent(type=com.sun.labs.minion.Stemmer.class,defaultClass=com.sun.labs.minion.util.PorterStemmer.class)
    public static final String PROP_STEMMER = "stemmer";
    
    private Stemmer stemmer;
    
    public Stemmer getStemmer() {
        return stemmer.clone();
    }

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        stemmer = (Stemmer) ps.getComponent(PROP_STEMMER);
    }

}
