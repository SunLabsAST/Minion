package com.sun.labs.minion.indexer;


import com.sun.labs.minion.Indexable;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

/**
 * A class of indexable documents that we can give to indexers when it's time
 * to flush their data to disk.
 */
public class FlushDocument implements Indexable {

    private static final Logger logger = Logger.getLogger(FlushDocument.class.getName());
    
    private CountDownLatch completion;
    
    /**
     * Creates a flush document.  If a completion is provided, then the flush
     * requested by the use of this document will be synchronous.
     * @param completion 
     */
    public FlushDocument(CountDownLatch completion) {
        this.completion = completion;
    }

    public CountDownLatch getCompletion() {
        return completion;
    }

    @Override
    public String getKey() {
        return "flush-document";
    }

    @Override
    public Map<String, Object> getMap() {
        return null;
    }
}
