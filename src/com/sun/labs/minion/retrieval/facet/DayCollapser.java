package com.sun.labs.minion.retrieval.facet;


import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

/**
 * A collapser that will collapse millisecond resolution dates into days.
 */
public class DayCollapser implements Collapser<Date,Date> {

    private static final Logger logger = Logger.getLogger(DayCollapser.class.getName());

    @Override
    public Date collapse(Date name) {
        Calendar c = Calendar.getInstance();
        c.setTime(name);
        Calendar r = Calendar.getInstance();
        r.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), 0, 0, 0);
        r.set(Calendar.MILLISECOND, 0);
        Date ret = r.getTime();
        return ret;
    }
}
