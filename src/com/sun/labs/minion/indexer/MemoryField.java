package com.sun.labs.minion.indexer;

import com.sun.labs.minion.indexer.dictionary.MemoryDictionaryBundle;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.Pipeline;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.partition.MemoryPartition;
import com.sun.labs.minion.indexer.partition.io.PartitionOutput;
import com.sun.labs.minion.pipeline.PipelineFactory;
import com.sun.labs.minion.pipeline.StageAdapter;
import com.sun.labs.minion.pipeline.Token;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that will hold all of the data for a field during indexing.
 */
public class MemoryField extends Field {
    
    public enum MarshallResult {
        NOTHING_DUMPED,
        DICTS_DUMPED,
        EVERYTHING_DUMPED,
    }

    static final Logger logger = Logger.getLogger(MemoryField.class.getName());
    
    /**
     * The dictionaries for this field.
     */
    private MemoryDictionaryBundle<Comparable> bundle;
    
    /**
     * The indexing pipeline for this field.
     */
    private Pipeline pipeline;

    private FieldStage fieldStage;

    public MemoryField(MemoryPartition partition, FieldInfo info) {
        super(partition, info);
        bundle = new MemoryDictionaryBundle<Comparable>(this);
        String pipelineFactoryName = info.getPipelineFactoryName();
        if(pipelineFactoryName != null) {
            PipelineFactory pf = (PipelineFactory) ((SearchEngineImpl) partition.getPartitionManager().getEngine()).getConfigurationManager().lookup(pipelineFactoryName);
            if(pf == null) {
                logger.log(Level.SEVERE, String.format("Unknown pipline name %s", pipelineFactoryName));
                throw new IllegalArgumentException(String.format("Unknown pipline name %s for field %s", pipelineFactoryName, info.getName()));
            }
            pipeline = pf.getPipeline();
            fieldStage = new FieldStage();
            pipeline.addStage(fieldStage);
            pipeline.setField(this);
        }
    }
    
    public String[] getPostingsChannelNames() {
        return bundle.getPostingsChannelNames();
    }

    public void startDocument(Entry docKey) {
        bundle.startDocument(docKey);
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
            for(Object o : (Collection) data) {
                addData(docID, o);
            }
            return;
        }
        
        if(saved) {
            save(docID, data);
        }

        if(pipeline != null) {
            fieldStage.setDocID(docID);
            pipeline.process(data.toString());
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
    public MarshallResult dump(PartitionOutput partOut) throws
            java.io.IOException {
        //
        // If there's nothing in the field, then call it a day.
        if(bundle.getMaxDocID() == 0) {
            return MarshallResult.NOTHING_DUMPED;
        }
        return bundle.marshall(partOut);
    }
    
    public void clear() {
        bundle.clear();
    }

    public MemoryDictionary getTermDictionary(boolean cased) {
        return bundle.getTermDictionary(cased);
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
