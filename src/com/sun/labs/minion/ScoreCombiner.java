package com.sun.labs.minion;


/**
 * An interface for combining weights.
 */
public interface ScoreCombiner {
    
    /**
     * Combines two float values, returning the combination.
     * @param w1 the first weight
     * @param w2 the second weight
     * @return the combination of the weights.
     */
    public float combine(float w1, float w2);

    /**
     * Combines two double weights, returning the combination.
     *
     * @param w1 the first weight
     * @param w2 the second weight
     * @return the combination of the weights.
     */
    public double combine(double w1, double w2);
    
    public static final ScoreCombiner MAX_WEIGHT_COMBINER = new ScoreCombiner() {

        @Override
        public float combine(float w1, float w2) {
            return Math.max(w1, w2);
        }

        @Override
        public double combine(double w1, double w2) {
            return Math.max(w1, w2);
        }
    };
    
    public static final ScoreCombiner SUM_WEIGHT_COMBINER = new ScoreCombiner() {

        @Override
        public float combine(float w1, float w2) {
            return w1 + w2;
        }

        @Override
        public double combine(double w1, double w2) {
            return w1 + w2;
        }
    };
}
