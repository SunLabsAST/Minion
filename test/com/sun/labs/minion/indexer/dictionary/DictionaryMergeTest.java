/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.labs.minion.indexer.dictionary;

import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for dictionary merges.
 */
public class DictionaryMergeTest {

    static File tmpDir;

    static Logger logger = Logger.getLogger(DictionaryTest.class.getName());

    static String[] otherData = new String[]{
        "words1.gz",};

    static TestData all;

    public DictionaryMergeTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        tmpDir = new File(System.getProperty("java.io.tmpdir"));
        all = new TestData("words.gz");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        all.close();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private File merge(double p) throws Exception {
        int nw = (int) (all.words.size() * p);
        TestData td1 = new TestData(nw, all.words);
        TestData td2 = new TestData(nw, all.words);

        Set<String> common = new HashSet<String>(td1.uniq);
        common.retainAll(td2.uniq);
        logger.info(String.format("d1: %d d2: %d common: %d",
                td1.dd.size(),
                td2.dd.size(),
                common.size()));

        File f = File.createTempFile("merge", ".dict");
        f.deleteOnExit();

        RandomAccessFile mf = new RandomAccessFile(f, "rw");

        DiskDictionary.merge(tmpDir,
                             new StringNameHandler(),
                             new DiskDictionary[]{td1.dd, td2.dd},
                             null, new int[2], new int[2][], mf,
                             new PostingsOutput[0], true);

        mf.close();
        td1.close();
        td2.close();
        return f;
    }

    @Test
    public void testTinyMerge() throws Exception {
        File mf = merge(0.001);
        TestData md = new TestData(mf);
    }

    @Test
    public void testOnePercentMerge() throws Exception {
        File mf = merge(0.01);
        TestData md = new TestData(mf);
    }

    @Test
    public void testTenPercentMerge() throws Exception {
        File mf = merge(0.1);
        TestData md = new TestData(mf);
    }

    @Test
    public void testFortyPercentMerge() throws Exception {
        File mf = merge(0.4);
        TestData md = new TestData(mf);
    }

}