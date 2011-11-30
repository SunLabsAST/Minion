package com.sun.labs.minion.indexer.postings;

import java.util.Random;

/**
 * A class that can generate believable term frequencies based on Zipf's law, so
 * that our postings tests are a bit believable.
 */
public class Zipf {

    /**
     * The probability estimates for a Zipf distribution.
     */
    double[] estimates;

    Random r;

    public Zipf(int nOutcomes, Random r) {
        estimates = new double[nOutcomes];
        this.r = r;
        double sum = 0;
        for(int i = 0; i < estimates.length; i++) {
            estimates[i] = 1 / ((double) i+1);
            sum += estimates[i];
        }
        for(int i = 0; i < estimates.length; i++) {
            estimates[i] /= sum;
        }
    }

    public int getOutcome() {
        double d = r.nextDouble();
        for(int i = 0; i < estimates.length; i++) {
            d -= estimates[i];
            if(d < 0) {
                return i+1;
            }
        }
        return estimates.length;
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
