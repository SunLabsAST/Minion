package com.sun.labs.minion.retrieval.facet;


import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 *
 */
public class ProgressiveDateCollapser implements Collapser<Date,String> {

    private static final Logger logger = Logger.getLogger(ProgressiveDateCollapser.class.getName());
    
    private long now = System.currentTimeMillis();
    
    private static String[] labels = new String[] {
        "Last 24 hours", 
        "Last 7 days",
        "Last 30 days",
        "Last year",
        "Earlier",
    };
    
    private static long dayInMillis = TimeUnit.DAYS.toMillis(1);
    
    
    private long[] cutoffs = new long[] {
        now - dayInMillis,
        now - 7 * dayInMillis,
        now - 30 * dayInMillis,
        now - 365 * dayInMillis,
        0
    };
    
    @Override
    public String collapse(Date name) {
        long nm = name.getTime();
        for(int i = 0; i < cutoffs.length; i++) {
            if(nm > cutoffs[i]) {
                return labels[i];
            }
        }
        return labels[labels.length-1];
    }
}
