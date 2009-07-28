/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package com.sun.labs.minion.indexer.partition;

import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.util.props.PropertyException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import com.sun.labs.util.props.PropertySheet;
import java.util.EnumSet;
import java.util.List;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.lexmorph.Lexicon;
import com.sun.labs.minion.lextax.MemoryTaxonomy;
import com.sun.labs.minion.pipeline.Stage;
import com.sun.labs.minion.pipeline.Token;
import com.sun.labs.minion.FieldInfo;
import java.util.Map;
import com.sun.labs.minion.classification.ClassificationResult;
import com.sun.labs.minion.indexer.MetaFile;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.entry.CasedPostingsEntry;
import com.sun.labs.minion.indexer.dictionary.MemoryFieldStore;
import com.sun.labs.minion.indexer.dictionary.MemoryBiGramDictionary;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.StreamPostingsOutput;
import com.sun.labs.minion.classification.ClassifierManager;
import com.sun.labs.minion.classification.Profiler;
import com.sun.labs.minion.util.FileLockException;
import com.sun.labs.minion.util.StopWatch;
import java.util.logging.Level;

/**
 *
 * @author ja151348
 */
public class InvFileMemoryPartition extends MemoryPartition implements Stage {

    /**
     * The field store.
     */
    protected MemoryFieldStore fields;

    /**
     * The taxonomy.
     */
    protected MemoryTaxonomy taxonomy;

    /**
     * The lexicon.
     */
    protected Lexicon lexicon;

    /**
     * Our downstream stage.
     */
    protected Stage downstream;

    /**
     * The number of documents we've indexed into this partition.
     */
    protected int partDocs;

    protected String name;

    /**
     * The tag for this module.
     */
    protected static String logTag = "IFMP";
    
    public InvFileMemoryPartition() {
    }

    /**
     * Gets the files associated with the field store for a partition.
     *
     * @return an array of files.  The first is for the dictionary, and the
     * remaining are for the postings files.
     */
    protected File[] getFieldFiles() {
        return InvFilePartitionUtils.getFieldFiles(manager, partNumber);
    }

    /**
     * Gets the files associated with the bigram postings for a partition.
     *
     * @return an array of files.  The first is for the dictionary, and the
     * remaining are for the postings files.
     */
    protected File[] getBigramFiles() {
        return InvFilePartitionUtils.getBigramFiles(manager, partNumber);
    }

    /**
     * Gets all the files associated with a partition, including those specific to
     * the inverted file.
     *
     * @return an array of files
     */
    protected File[] getAllFiles() {
        return InvFilePartitionUtils.getAllFiles(manager, partNumber);
    }

    /**
     * Starts a new document in this partition.
     *
     * @param key The key for this document, which is added to the
     * dictionary.
     */
    public void startDocument(String key) {

        dockey = (DocKeyEntry) docDict.newEntry(key);
        Entry old = docDict.put(key, dockey);
        
        //
        // If there's already a term, get rid of it!
        if(old != null) {

            logger.warning("Duplicate dockey in partition: " + key +
                    " deleting old version: " + old.getID());

            //
            // Delete the previous document.
            del.delete(old.getID());
            deleted.add(old.getID());
        }

        fields.startDocument(dockey.getID());
        nWords = 0;
    }

    /**
     * Gets the number of documents in this partition.
     *
     * @return the number of documents indexed into this partition.
     */
    public int getNDocs() {
        return partDocs - del.getNDeleted();
    }

    /**
     * Takes a token from the pipeline and adds it to this partition.
     *
     * @param token The token we want to add.
     */
    public void token(Token token) {

        //
        // Don't index unless we're supposed to!
        if(fields.shouldIndex()) {

            //
            // Set the word count for this document and set the active
            // fields in the token.
            nWords = token.getWordNum();

            String name = token.getToken();

            //
            // We'll get the entry for this term.
            IndexEntry mde = (IndexEntry) mainDict.get(name);

            //
            // If we didn't get anything, then make a term.
            if(mde == null) {
                mde = mainDict.newEntry(name);
                mainDict.put(name, mde);

                //
                // If we have a taxonomy, and the token shouldn't be ignored, then analyze the current word and add
                // it to the taxonomy.
                if(taxonomy != null && !taxonomy.shouldIgnore(token)) {
                    taxonomy.classify(name.toLowerCase());
                }
            }

            //
            // Pathalogical case:  the count could be zero, but we need at least 1.
            if(token.getCount() <= 0) {
                token.setCount(1);
            }
            
            //
            // Set up the token as an occurrence.
            token.setID(dockey.getID());
            if(token.getFields() == null) {
                token.setFields(fields.getActiveFields());
            }

            //
            // Add this occurrence.
            mde.add(token);

            //
            // If we're vectoring the current field, then add an occurence
            // to the document dictionary for the current term.  If this
            // term is case sensitive, then we'll get the case insensitive
            // ID, since we want to store vectors as case insensitive
            // terms.
            //
            // We do, however, need to watch out for "uncased" terms that
            // have the same upper and lower case.
            if(fields.shouldVector()) {
                if(mde instanceof CasedPostingsEntry) {
                    Entry cie =
                            ((CasedPostingsEntry) mde).getCaseInsensitiveEntry();
                    if(cie != null) {
                        ddo.setEntry(cie);
                    } else {
                        ddo.setEntry(mde);
                    }
                } else {
                    ddo.setEntry(mde);
                }
                ddo.setCount(token.getCount());
                ddo.setFields(token.getFields());
                dockey.add(ddo);
            }
        }
    }

    /**
     * Processes text passed in from the upstream stage.  The text is simply
     * processed as a token.
     */
    public void text(char[] t, int b, int e) {
        token(new Token(new String(t, b, e - b), 1));
    }

    /**
     * Indicates to the field store that a field has started.  We'll keep
     * track of how deep we are in embedded fields.
     * @param fi The {@link com.sun.labs.minion.FieldInfo} object that describes
     * the field that is starting.
     */
    public void startField(FieldInfo fi) {
        fields.startField(fi);
    }

    /**
     * Saves data for a field.
     */
    public void savedData(Object d) {
        fields.saveData(dockey.getID(), d);
    }

    /**
     * Saves data for a field.
     */
    public void savedData(FieldInfo fi, int docID, Object d) {
        fields.saveData(fi, docID, d);
    }

    /**
     * Indicates to the field store that a field has ended.  We'll keep
     * track of how deep we are in embedded fields.
     *
     * @param fi The {@link com.sun.labs.minion.FieldInfo} object that describes
     * the field that is ending.
     */
    public void endField(FieldInfo fi) {
        fields.endField();
    }

    /**
     * Signals the end of a document.  Stores the current document length.
     */
    public void endDocument(long size) {
        fields.endDocument();
        partDocs++;
        stats.nTokens += nWords;
    }


    //
    // From here down is stage stuff for the indexing pipeline.
    /**
     * Sets the downstream stage of this stage.
     */
    public void setDownstream(Stage s) {
        downstream = s;
    }

    /**
     * Gets the downstream stage of this stage.
     */
    public Stage getDownstream() {
        return downstream;
    }

    /**
     * Defines a field into which an application will index data.
     *
     * @param fi The {@link com.sun.labs.minion.FieldInfo} object that describes
     * the field we want defined.
     */
    public FieldInfo defineField(FieldInfo fi) {
        return fields.defineField(fi);
    }

    /**
     * Passes punctuation onto any downstream stage, if there is one.
     *
     * @param p The punctuation to process.
     */
    public void punctuation(Token p) {
    }

    /**
     * Dumps the data that is specific to the inverted file memory partition.
     * That is, it dumps the bigram dictionary and the field store to disk.
     *
     * @param sorted a sorted listed of main dictionary entries
     */
    protected void dumpCustom(Entry[] sorted)
            throws java.io.IOException {

        //
        // See if we need to perform classification, then proceed
        // to dump as usual.
        if(manager == null) {
            logger.info("Null manager?");
        }

        if(manager != null && manager.getEngine() == null) {
            logger.info("Null engine?");
        }
        
        ClassifierManager classManager =
                ((SearchEngineImpl) manager.getEngine()).getClassifierManager();
        if((mainDict.size() > 0) && (classManager != null) &&
                classManager.doClassification()) {
            //
            // A class manager exists, so perform classification if we're supposed to do that.
            DiskPartition sdp = manager.newDiskPartition(partNumber, manager);
            try {
                DocumentVectorLengths.calculate(sdp, getManager().getTermStatsDict(), true);
            } catch (FileLockException ex) {
                logger.log(Level.SEVERE, "Error calculating document vector lengths", ex);
            }
            Map<String, ClassificationResult> results = classManager.classify(sdp);
            sdp.close();
            MetaFile mf = manager.getMetaFile();

            //
            // OK, now let's look at the results of the classifier and add data
            // to the field store.
            for(Map.Entry<String, ClassificationResult> e : results.entrySet()) {

                String fieldName = e.getKey();
                FieldInfo cfi = mf.getFieldInfo(fieldName);
                if(cfi == null) {
                    try {
                        cfi = mf.defineField(new FieldInfo(fieldName,
                                EnumSet.of(FieldInfo.Attribute.SAVED),
                                FieldInfo.Type.STRING));
                    } catch(SearchEngineException see) {
                        logger.log(Level.SEVERE, "Unable to define classification field " + fieldName, see);
                        continue;
                    }

                }
                FieldInfo sfi = null;
                if(indexConfig.storeClassifierScores()) {
                    sfi = mf.getFieldInfo(fieldName + "-score");
                    if(sfi == null) {
                        try {
                            sfi = mf.defineField(new FieldInfo(fieldName +
                                    "-score",
                                    EnumSet.of(FieldInfo.Attribute.SAVED),
                                    FieldInfo.Type.FLOAT));
                        } catch(SearchEngineException see) {
                            logger.log(Level.SEVERE, "Unable to define classification score field " + fieldName, see);
                            continue;
                        }
                    }
                }
		FieldInfo ncfi = null;
		FieldInfo ncsfi = null;
		if(indexConfig.storeNonClassified()) {
		    try {
			ncfi = mf.defineField(new FieldInfo("non-" + fieldName,
							    EnumSet.of(FieldInfo.Attribute.SAVED),
							    FieldInfo.Type.STRING));
			ncsfi = mf.defineField(new FieldInfo("non-" + fieldName +
							     "-score",
							     EnumSet.of(FieldInfo.Attribute.SAVED),
							     FieldInfo.Type.FLOAT));
		    } catch(SearchEngineException see) {
			logger.log(Level.SEVERE, "Unable to define classification score field " + fieldName, see);
			continue;
		    }
		}

		if(ncfi != null) {
		    for(ClassificationResult.DocResult r : e.getValue().getResults()) {
			if(r.getScore() > 0) {
			    fields.saveData(cfi, r.getID(), r.getValue());
			    if(sfi != null) {
				fields.saveData(sfi, r.getID(), r.getScore());
			    }
			} else if(r.getScore() < 0) {
                            float score = Math.abs(r.getScore());
                            if(score > 0) {
                                fields.saveData(ncfi, r.getID(), r.getValue());
                                fields.saveData(ncsfi, r.getID(), score);
                            }
			}
		    }
		} else {
                    fields.saveData(cfi, sfi, e.getValue());
		}
            }
        }

        try {
            //
            // Make a bigram dictionary from the main dictionary.
            MemoryBiGramDictionary bi =
                    new MemoryBiGramDictionary(sorted);
            StopWatch sw = new StopWatch();
            sw.start();
            InvFilePartitionUtils.writeBigramDictionary(bi, manager, partNumber);
            sw.stop();
            logger.fine("main bigram dump: " + sw.getTime());

            partDocs = 0;

            if(taxonomy != null) {
                taxonomy.dump(manager.getIndexDir(),
                        manager.makeTaxonomyFile(partNumber));
            }

            //
            // Make a first go at dumping the fields.  Note that this *will not*
            // clear out the dictionaries therein.  We might need to modify the
            // in-memory field store and redump it.
            sw.reset();
            sw.start();
            dumpFields();
            sw.stop();
            logger.fine("field store dump: " + sw.getTime());

            //
            // Run any profilers that are defined.  This is a bit of a hack.  The
            // profilers may need the data in the field store, so they have to run
            // after the field store's been dumped.  At the same time, they might
            // modify the field store, so we'll need to re-dump it after they're
            // done.  Don't ask me what'll happen if one profiler wants to depend
            // on the results of another.
            //
            // We probably need to implement this at the search engine level and
            // give the profilers the opportunity to run queries, pull documents,
            // and then redump them, but we'll leave that for later.
            List<Profiler> profilers =
                    ((SearchEngineImpl) manager.getEngine()).getProfilers();
            if(profilers.size() > 0) {
                boolean redump = false;
                DiskPartition ndp =
                        manager.newDiskPartition(partNumber, manager);
                for(Profiler p : profilers) {
                    if(p.profile((SearchEngineImpl) manager.getEngine(), this,
                            (InvFileDiskPartition) ndp)) {
                        redump = true;
                    }
                }
                if(redump) {
                    dumpFields();
                }
            }


            //
            // Now we need to clear out the in-memory saved field data.
            fields.clear();
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "Error during custom dump", ioe);
            throw ioe;
        }
    }

    private void dumpFields()
            throws java.io.IOException {
        //
        // Now dump the field store out.
        // Get channels for the saved field data.
        File[] files = getFieldFiles();
        RandomAccessFile fieldDictFile =
                new RandomAccessFile(files[0], "rw");
        BufferedOutputStream fieldPostStream = new BufferedOutputStream(new FileOutputStream(files[1]),
                8196);

        //
        // And dump the data out to disk
        fields.dump(manager.getIndexDir(), fieldDictFile,
                new PostingsOutput[]{new StreamPostingsOutput(fieldPostStream)});
        fieldDictFile.close();
        fieldPostStream.close();
    }

    public MemoryFieldStore getFieldStore() {
        return fields;
    }
    
    public void newProperties(PropertySheet ps)
            throws PropertyException {
        super.newProperties(ps);

        //
        // The field store.
        fields =
                new MemoryFieldStore(manager.metaFile);

        //
        // Should we build a taxonomy?
        if(indexConfig.taxonomyEnabled()) {
            taxonomy = new MemoryTaxonomy();
            lexicon =
                    indexConfig.getLexicon();
            if(lexicon != null) {
                taxonomy.setLexicon(lexicon);
            }
        }
    }
}
