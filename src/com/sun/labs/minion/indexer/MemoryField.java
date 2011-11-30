package com.sun.labs.minion.indexer;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.Pipeline;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.partition.MemoryPartition;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.pipeline.PipelineFactory;
import com.sun.labs.minion.pipeline.StageAdapter;
import com.sun.labs.minion.pipeline.Token;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that will hold all of the data for a field during indexing.
 */
public class MemoryField extends Field {
    
    public enum DumpResult {
        NOTHING_DUMPED,
        DICTS_DUMPED,
        EVERYTHING_DUMPED,
    }

    static final Logger logger = Logger.getLogger(MemoryField.class.getName());
    
    /**
     * The dictionaries for this field.
     */
    private MemoryDictionaryBundle<Comparable> dicts;
    
    /**
     * The indexing pipeline for this field.
     */
    private Pipeline pipeline;

    private FieldStage fieldStage;

    public MemoryField(MemoryPartition partition, FieldInfo info, EntryFactory factory) {
        super(partition, info);
        dicts = new MemoryDictionaryBundle<Comparable>(this, factory);
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
        }
    }

    public void startDocument(Entry docKey) {
        dicts.startDocument(docKey);
    }

    /**
     * Adds data to this field, taking whatever actions are appropriate based on
     * the attributes of the field.
     * @param docID the ID of the document for which the data is being added.
     * @param data the data to add
     */
    public void addData(int docID, Object data) {
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
        dicts.save(docID, data);
    }

    /**
     * Adds a token to the appropriate dictionaries for this field.
     * @param t the token to add.
     */
    public void token(Token t) {
        dicts.token(t);
    }

    public int getMaximumDocumentID() {
        return dicts.getMaxDocID();
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
    public DumpResult dump(File path,
                     RandomAccessFile fieldDictFile,
                     PostingsOutput[] postOut,
                     RandomAccessFile termStatsDictFile,
                     RandomAccessFile vectorLengthsFile,
                     int maxID) throws
            java.io.IOException {
        //
        // If there's nothing in the field, then call it a day.
        if(dicts.getMaxDocID() == 0) {
            return DumpResult.NOTHING_DUMPED;
        }
        return dicts.dump(path, fieldDictFile, postOut, termStatsDictFile,
                   vectorLengthsFile, maxID);
    }

    public MemoryDictionary getTermDictionary(boolean cased) {
        return dicts.getTermDictionary(cased);
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
