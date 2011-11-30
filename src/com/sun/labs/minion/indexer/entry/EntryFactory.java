package com.sun.labs.minion.indexer.entry;

import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.Postings.Type;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.util.props.ConfigEnum;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.util.logging.Logger;

/**
 * A factory for index entries.
 */
public class EntryFactory<N extends Comparable> implements Configurable {
    
    private static final Logger logger = Logger.getLogger(EntryFactory.class.getName());

    @ConfigEnum(type=Postings.Type.class,defaultValue="ID_FREQ")
    public static final String POST_TYPE = "post_type";
    
    protected Type type;
    
    /**
     * An exemplar postings.
     */
    protected Postings post;
    
    public EntryFactory() {
    }

    public EntryFactory(Postings.Type type) {
        this.type = type;
        post = Postings.Type.getPostings(type);
    }

    public IndexEntry getIndexEntry(N name, int id) {
        return new IndexEntry(name, id, Type.getPostings(type));
    }

    public QueryEntry getQueryEntry(N name, ReadableBuffer b) {
        return new QueryEntry(name, type, b);
    }
    
    /**
     * Gets the names of the postings channels that this entry is going to need
     * for reading or writing it's postings.
     */
    public String[] getPostingsChannelNames() {
        return post.getChannelNames();
    }

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        type = (Type) ps.getEnum(POST_TYPE);
    }
}
