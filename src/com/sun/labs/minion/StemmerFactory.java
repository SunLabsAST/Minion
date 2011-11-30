package com.sun.labs.minion;

import com.sun.labs.util.props.Component;

/**
 * A factory interface for stemmers.
 */
public interface StemmerFactory extends Component {

    public Stemmer getStemmer();

}
