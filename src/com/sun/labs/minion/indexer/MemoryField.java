package com.sun.labs.minion.indexer;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.Pipeline;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.dictionary.Dictionary;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionaryBundle;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.partition.MemoryPartition;
import com.sun.labs.minion.indexer.partition.io.PartitionOutput;
import com.sun.labs.minion.pipeline.PipelineFactory;
import com.sun.labs.minion.pipeline.StageAdapter;
import com.sun.labs.minion.pipeline.Token;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that will hold all of the data for a field during indexing.
 */
public class MemoryField extends Field {
    
    private static final Logger logger = Logger.getLogger(MemoryField.class.getName());
    
    public static final int MULTIVALUE_GAP = 300;

    public enum MarshalResult {
        NOTHING_DUMPED,
        DICTS_DUMPED,
        EVERYTHING_DUMPED,
    }

    /**
     * The dictionaries for this field.
     */
    private MemoryDictionaryBundle<Comparable> bundle;
    
    /**
     * The indexing pipeline for this field.
     */
    private Pipeline pipeline;

    private FieldStage fieldStage;
    
    private IndexEntry<String> currDoc;
    
    private boolean debug;

    public MemoryField(MemoryPartition partition, FieldInfo info) {
        super(partition, info);
        bundle = new MemoryDictionaryBundle<Comparable>(this);
        if(indexed) {
            String pipelineFactoryName = info.getPipelineFactoryName();
            if(pipelineFactoryName != null) {
                PipelineFactory pf =
                        (PipelineFactory) ((SearchEngineImpl) partition.
                        getPartitionManager().getEngine()).
                        getConfigurationManager().
                        lookup(pipelineFactoryName);
                if(pf == null) {
                    logger.log(Level.SEVERE, String.format(
                            "Unknown pipline factory name %s",
                            pipelineFactoryName));
                    throw new IllegalArgumentException(String.format(
                            "Unknown pipline factory name %s for field %s",
                            pipelineFactoryName,
                            info.getName()));
                }
                pipeline = pf.getPipeline();
                fieldStage = new MemoryField.FieldStage();
                pipeline.addStage(fieldStage);
                pipeline.setField(this);
            }
        }
    }
    
    public String[] getPostingsChannelNames() {
        return bundle.getPostingsChannelNames();
    } 
    
    public Dictionary getDictionary(DictionaryType type) {
        return bundle.getDictionary(type);
    }

    public void startDocument(IndexEntry<String> docKey) {
        if(pipeline != null) {
            pipeline.reset();
        }
        bundle.startDocument(docKey);
        currDoc = docKey;
    }

    /**
     * Adds data to this field, taking whatever actions are appropriate based on
     * the attributes of the field.
     * @param docID the ID of the document for which the data is being added.
     * @param data the data to add
     */
    public void addData(int docID, Object data) {
        
        //
        // Handle a collection passed as a field value.
        if(data instanceof Collection) {
            try {
                for(Object o : (Collection) data) {
                    addData(docID, o);
                }
            } catch(ConcurrentModificationException ex) {
                logger.log(Level.SEVERE, String.format("CME in %s for field %s",
                                                       currDoc, info.getName()),
                           ex);
            }
            return;
        }
        
        if(saved) {
            save(docID, data);
        }

        if(pipeline != null) {
            fieldStage.setDocID(docID);
            pipeline.process(data.toString());
            int lastPos = pipeline.getHead().getLastWordPosition();
            pipeline.reset();
            pipeline.getHead().setNextWordPosition(lastPos+MULTIVALUE_GAP);
        }
    }

    /**
     * Saves data in this field.  If this field is not saved, then an
     * exception is thrown.
     *
     * @param docID the document ID for the document containing the saved
     * data
     * @param data The actual field data.
     */
    public void save(int docID, Object data) {
        bundle.save(docID, data);
    }

    /**
     * Adds a token to the appropriate dictionaries for this field.
     * @param t the token to add.
     */
    public void token(Token t) {
        bundle.token(t);
    }

    @Override
    public int getMaximumDocumentID() {
        return bundle.getMaxDocID();
    }

    /**
     * Writes the field to the provided files.
     *
     * @param path The path of the index directory.
     * @param fieldDictFile The file where the dictionaries will be written.
     * @param termStatsDictFile a file where a new term statistics dictionary
     * will be written
     * @param vectorLengthsFile a file where vector lengths for the documents
     * in this field will be written
     * @param postOut A place to write the postings associated with the
     * dictionaries.
     * @param maxID The maximum document ID for this partition.
     * @throws java.io.IOException if there is an error during the
     * writing.
     */
    public MarshalResult marshal(PartitionOutput partOut) throws
            java.io.IOException {
        //
        // If there's nothing in the field, then call it a day.
        if(!bundle.anyData()) {
            return MarshalResult.NOTHING_DUMPED;
        }
        return bundle.marshal(partOut);
    }
    
    public void clear() {
        bundle.clear();
    }

    private class FieldStage extends StageAdapter {

        private int docID;

        public void setDocID(int docID) {
            this.docID = docID;
        }

        @Override
        public void token(Token t) {
            t.setID(docID);
            MemoryField.this.token(t);
        }

    }

    @Override
    public String toString() {
        return String.format("MemoryField %s", info);
    }


}
