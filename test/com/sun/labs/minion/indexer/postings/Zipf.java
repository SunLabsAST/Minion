package com.sun.labs.minion.indexer.postings;

import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

/**
 * A class that can generate believable term frequencies based on Zipf's law, so
 * that our postings tests are a bit believable.
 */
public class Zipf {
    
    private static final Logger logger = Logger.getLogger(Zipf.class.getName());

    /**
     * The probability estimates for a Zipf distribution.
     */
    double[] estimates;
    
    double[] summedEstimates;

    Random r;
    
    public Zipf(int nOutcomes) {
        this(nOutcomes, new Random());
    }

    public Zipf(int nOutcomes, Random r) {
        estimates = new double[nOutcomes];
        summedEstimates = new double[nOutcomes];
        this.r = r;
        double sum = 0;
        for(int i = 0; i < estimates.length; i++) {
            estimates[i] = 1 / ((double) i+1);
            sum += estimates[i];
        }
        double esum = 0;
        for(int i = 0; i < estimates.length; i++) {
            estimates[i] /= sum;
            esum += estimates[i];
            summedEstimates[i] = esum;
        }
        
        logger.info(String.format("esum: %.3f", esum));
    }

    public int getOutcome() {
        double d = r.nextDouble();
        if(d > summedEstimates[summedEstimates.length - 1]) {
            return summedEstimates.length - 1;
        }
        int p = Arrays.binarySearch(summedEstimates, d);
        if(p > 0) {
            return p;
        }
        int ret =  -(p+1) + 1;
        if(ret >= summedEstimates.length) {
            ret--;
        }
        return ret;
    }

    public static void main(String args[]) throws Exception {
        int nOutcomes = args.length == 0 ? 64000 : Integer.parseInt(args[0]);
        Zipf z = new Zipf(nOutcomes, new Random());
        for(int i = 0; i < 20; i++) {
            System.out.format("%.3f ", z.estimates[i]);
        }

        System.out.println("");

        int[] c = new int[nOutcomes+1];
        for(int i = 0; i < 10000; i++) {
            c[z.getOutcome()]++;
        }

        for(int i = 0; i < c.length; i++) {
            if(c[i] > 0) {
                System.out.format("%5d %6d\n", i, c[i]);
            }
        }
    }
}
