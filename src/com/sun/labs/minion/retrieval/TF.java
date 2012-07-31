package com.sun.labs.minion.retrieval;


import java.util.logging.Logger;

/**
 * A weighting function that just returns the term frequency in a document
 * as the weight.
 */
public class TF implements WeightingFunction {

    private static final Logger logger = Logger.getLogger(TF.class.getName());

    @Override
    public float initTerm(WeightingComponents wc) {
        return 1;
    }

    @Override
    public float termWeight(WeightingComponents wc) {
        return wc.fdt;
    }
}
