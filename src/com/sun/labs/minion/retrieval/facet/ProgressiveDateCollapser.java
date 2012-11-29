package com.sun.labs.minion.retrieval.facet;

import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 *
 */
public class ProgressiveDateCollapser implements Collapser<Date,ProgressiveDateCollapser.Label> {

    private static final Logger logger = Logger.getLogger(ProgressiveDateCollapser.class.getName());
    
    private long now = System.currentTimeMillis();
    
    private static long dayInMillis = TimeUnit.DAYS.toMillis(1);

    public enum Label {
        LAST_24_HOURS("Last 24 Hours"),
        LAST_7_DAYS("Last 7 Days"),
        LAST_30_DAYS("Last 30 Days"),
        LAST_YEAR("Last Year"),
        EARLIER("Earlier");
        
        Label(String label) {
            this.label = label;
        }
        
        private String label;

        public String toString() {
            return label;
        }
    }
    
    private EnumMap<Label, Long> cutoffs = new EnumMap<Label, Long>(Label.class);
    
    public ProgressiveDateCollapser() {
        cutoffs.put(Label.LAST_24_HOURS, now - dayInMillis);
        cutoffs.put(Label.LAST_7_DAYS, now - 7 * dayInMillis);
        cutoffs.put(Label.LAST_30_DAYS, now - 30 * dayInMillis);
        cutoffs.put(Label.LAST_YEAR, now - 365 * dayInMillis);
        cutoffs.put(Label.EARLIER, 0L);
    }
    
    public long getCutoff(Label label) {
        return cutoffs.get(label);
    }
    
    @Override
    public Label collapse(Date name) {
        long nm = name.getTime();
        for(Map.Entry<Label,Long> e : cutoffs.entrySet()) {
            if(nm > e.getValue()) {
                return e.getKey();
            }
        }
        return Label.EARLIER;
    }
}
