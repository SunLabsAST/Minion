package com.sun.labs.minion.indexer;


import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.Indexable;
import com.sun.labs.minion.Passage;
import com.sun.labs.minion.PassageBuilder;
import com.sun.labs.minion.Pipeline;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.pipeline.PipelineFactory;
import com.sun.labs.minion.retrieval.ArrayGroup;
import com.sun.labs.minion.pipeline.HighlightStage;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An indexer that can be used to process documents for highlighting.  This
 * class does some of what {@link SearchEngineImpl.index} does, along with 
 * some of what {@link MemoryField} does, but skips the actual indexing.
 * 
 */
public class HighlightDocumentProcessor implements PassageBuilder {

    private static final Logger logger = Logger.getLogger(HighlightDocumentProcessor.class.getName());
    
    private SearchEngine engine;
    
    private Map<String, Pipeline> pipelines = new HashMap<String, Pipeline>();
    
    private ArrayGroup ag;
    
    private String[] queryTerms;
    
    private int doc;
    
    /**
     * Creates a processor for documents that runs the data in the document 
     * through the highlighting pipelines for the fields.
     * @param engine the search engine on whose behalf we're doing the highlighting.
     */
    public HighlightDocumentProcessor(SearchEngine engine) {
        this.engine = engine;
    }
    
    @Override
    public void process(Indexable doc) {
        for(Pipeline pipeline : pipelines.values()) {
            pipeline.reset();
        }
        for(Map.Entry<String, Object> field : doc.getMap().entrySet()) {
            FieldInfo fi = engine.getFieldInfo(field.getKey());
            if(fi == null) {
                logger.warning(String.format("Unknown field: %s in %s", field.
                        getKey(), doc.getKey()));
                continue;
            }
            addField(fi, field.getValue());
        }
    }
    
    public Pipeline getPipeline(String fieldName) {
        return getPipeline(engine.getFieldInfo(fieldName));
    }
    
    protected Pipeline getPipeline(FieldInfo info) {
        
        Pipeline pipeline = pipelines.get(info.getName());
        if(pipeline == null) {
            String pipelineFactoryName = info.getPipelineFactoryName();
            if(pipelineFactoryName != null) {
                PipelineFactory pf = (PipelineFactory) ((SearchEngineImpl) engine).
                        getConfigurationManager().lookup(pipelineFactoryName);
                if(pf == null) {
                    logger.log(Level.SEVERE, String.format(
                            "Unknown pipline name %s", pipelineFactoryName));
                    throw new IllegalArgumentException(String.format(
                            "Unknown pipline name %s for field %s",
                            pipelineFactoryName,
                            info.getName()));
                }
                pipeline = pf.getHLPipeline();
                ((HighlightStage) pipeline.getTail()).setField(info);
                ((HighlightStage) pipeline.getTail()).reset(ag, doc, queryTerms);
                pipelines.put(info.getName(), pipeline);
            }

        }
        
        return pipeline;
    }
    
    public void addField(FieldInfo info, Object data) {
        Pipeline pipeline = getPipeline(info);
        
        if(info.getName().equals("full-text")) {
            logger.info(String.format("%s: %s", info.getName(), pipeline));
        }
        if(pipeline == null) {
            return;
        }

        //
        // Handle a collection passed as a field value.
        if(data instanceof Collection) {
            for(Object o : (Collection) data) {
                addField(info, o);
            }
            return;
        }
            
        if(pipeline != null) {
            pipeline.process(data.toString());
            int lastPos = pipeline.getHead().getLastWordPosition();
            pipeline.reset();
            pipeline.getHead().setNextWordPosition(lastPos + MemoryField.MULTIVALUE_GAP);
        }
    }
    
    /**
     * Sets up for processing a new document.
     */
    public void reset(ArrayGroup ag, int doc, String[] queryTerms) {
        this.ag = ag;
        this.doc = doc;
        this.queryTerms = queryTerms;
        for(Pipeline pipeline : pipelines.values()) {
            pipeline.reset();
            ((HighlightStage) pipeline.getTail()).reset(ag, doc, queryTerms);
        }
    }
    
    @Override
    public void addPassageField(String fieldName) {
        addPassageField(fieldName, 
                        com.sun.labs.minion.Passage.Type.JOIN,
                        -1, -1, false);
    }

    @Override
    public void addPassageField(String fieldName,
                                com.sun.labs.minion.Passage.Type type,
                                int context, int maxSize,
                                boolean doSort) {
        Pipeline pipeline = getPipeline(fieldName);
        ((HighlightStage) pipeline.getTail()).addPassage(type, context,
                         maxSize, doSort);
    }

    @Override
    public Map<String, List<Passage>> getPassages() {
        
        Map<String, List<Passage>> ret = new HashMap<String, List<Passage>>();
        for(Map.Entry<String, Pipeline> e : pipelines.entrySet()) {
            ret.put(e.getKey(), ((HighlightStage) e.getValue().getTail()).getPassages());
        }
        return ret;
    }

}
