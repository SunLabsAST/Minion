package com.sun.labs.minion.indexer;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.pipeline.Token;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

/**
 * A class that will hold all of the data for a field during indexing.
 */
public class MemoryField extends Field {

    static final Logger logger = Logger.getLogger(MemoryField.class.getName());

    /**
     * The dictionaries for this field.
     */
    private MemoryDictionaryBundle<Comparable> dicts;

    /**
     * The maximum document ID for which we're storing data.
     */
    private int maximumDocumentID;

    public MemoryField(FieldInfo info, EntryFactory factory) {
        super(info);
        dicts = new MemoryDictionaryBundle<Comparable>(this, factory);
    }

    public void startDocument(String key) {
        dicts.startDocument(key);
    }

    /**
     * Adds data to this field, taking whatever actions are appropriate based on
     * the attributes of the field.
     * @param docID the ID of the document for which the data is being added.
     * @param data the data to add
     */
    public void addData(int docID, Object data) {
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
        return maximumDocumentID;
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
    public void dump(File path,
                     RandomAccessFile fieldDictFile,
                     PostingsOutput[] postOut,
                     RandomAccessFile termStatsDictFile,
                     RandomAccessFile vectorLengthsFile,
                     int maxID) throws
            java.io.IOException {
        dicts.dump(path, fieldDictFile, postOut, termStatsDictFile,
                   vectorLengthsFile, maxID);
    }

    public MemoryDictionary getTermDictionary(boolean cased) {
        return dicts.getTermDictionary(cased);
    }
}
