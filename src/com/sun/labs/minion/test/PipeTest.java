package com.sun.labs.minion.test;

import com.sun.labs.minion.Pipeline;
import com.sun.labs.minion.document.tokenizer.UniversalTokenizer;
import com.sun.labs.minion.pipeline.LowerCaseStage;
import com.sun.labs.minion.pipeline.PipelineImpl;
import com.sun.labs.minion.pipeline.PrintStage;
import com.sun.labs.minion.pipeline.Stage;
import com.sun.labs.minion.pipeline.StemStage;
import com.sun.labs.minion.pipeline.StopWords;
import com.sun.labs.minion.pipeline.StopWordsStage;
import com.sun.labs.minion.pipeline.Token;
import com.sun.labs.minion.pipeline.TokenCollectorStage;
import com.sun.labs.util.LabsLogFormatter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class PipeTest {

    private static final Logger logger = Logger.getLogger(PipeTest.class.
            getName());
    
    private Pipeline p;

    public PipeTest() {
        
        //
        // Make the stages.
        Stage tokenizer = new UniversalTokenizer();
        Stage lc = new LowerCaseStage();
        Stage stopper = new StopWordsStage(new StopWords("stopwords"));
        Stage stemmer = new StemStage();
        Stage collector = new TokenCollectorStage();
        
        //
        // Connect them.
        tokenizer.setDownstream(lc);
        lc.setDownstream(stopper);
        stopper.setDownstream(stemmer);
        stemmer.setDownstream(collector);
        
        //
        // Make a pipeline.
        p = new PipelineImpl(Arrays.asList(new Stage[] {tokenizer, lc, stopper, stemmer, collector}));
    }
    
    public Token[] process(String text) {
        p.reset();
        p.process(text);
        return ((TokenCollectorStage) p.getTail()).getTokens();
    }
    
    public static void main(String[] args) throws Exception {
        PipeTest pt = new PipeTest();
        Logger l = Logger.getLogger("");
        for(Handler h : l.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new LabsLogFormatter());
        }
        for(String arg : args) {
            Token[] tokens = pt.process(arg);
            System.out.format("Tokens for %s:\n", arg);
            for(Token t : tokens) {
                System.out.format(" Token: %s\n", t);
            }
        }
    }
}
