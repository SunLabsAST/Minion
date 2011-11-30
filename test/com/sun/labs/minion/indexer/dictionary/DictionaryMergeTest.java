/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.labs.minion.indexer.dictionary;

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
    
    static final Logger logger = Logger.getLogger(DictionaryTest.class.getName());
    
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
    
    private File mergeExact(int aSize, int bSize) throws Exception {
        TestData td1 = new TestData(aSize, all.words);
        TestData td2 = new TestData(bSize, all.words);
        File mf = merge(td1, td2);
        td1.close();
        td2.close();
        return mf;
    }
    
    private File merge(double proportion) throws Exception {
        int nw = (int) (all.words.size() * proportion);
        TestData td1 = new TestData(nw, all.words);
        TestData td2 = new TestData(nw, all.words);
        File mf = merge(td1, td2);
        td1.close();
        td2.close();
        return mf;
    }

    /**
     * Merges together the dictionaries from a number of test data instances.
     * 
     * @param testData the test data to merge
     * @return the file for the merged dictionary.
     * @throws Exception 
     */
    private File merge(TestData... testData) throws Exception {
        Set<String> common = new HashSet<String>();
        for(TestData td : testData) {
            common.addAll(td.uniq);
        }
        
        File f = File.createTempFile("merge", ".dict");
        f.deleteOnExit();
        
        RandomAccessFile mf = new RandomAccessFile(f, "rw");
        DiskDictionary[] dicts = new DiskDictionary[testData.length];
        for(int i = 0; i < dicts.length; i++) {
            dicts[i] = testData[i].dd;
        }
        
        DiskDictionary.merge(tmpDir,
                new StringNameHandler(),
                dicts,
                null,
                new int[dicts.length],
                new int[dicts.length][], mf,
                new PostingsOutput[0], true);
        
        mf.close();
        return f;
        
    }
    
    @Test
    public void testSingleWordMerge() throws Exception {
        TestData td1 = new TestData(1, all.words);
        File f = merge(td1, td1);
        TestData mtd = new TestData(f);
        td1.checkConsistency(mtd);
        td1.close();
    }
    
    @Test
    public void testSmallMerges() throws Exception {
        for(int i = 1; i <= 10; i++) {
            logger.info(String.format("Checking merge for dictionaries with %d words", i));
            TestData td1 = new TestData(i, all.words);
            TestData td2 = new TestData(i, all.words);
            File mf = merge(td1, td2);
            TestData mtd = new TestData(mf);
            mtd.checkConsistency(td1, td2);
            td1.close();
            td2.close();
        }
    }
    
    private void testNWayMergeProportion(int nways, double proportion) throws Exception {
        TestData[] testData = new TestData[nways];
        int nw = (int) (all.words.size() * proportion);
        for(int i = 0; i < testData.length; i++) {
            testData[i] = new TestData(nw, all.words);
            File mf = merge(testData);
            TestData mtd = new TestData(mf);
            mtd.checkConsistency(testData);
            for(TestData td : testData) {
                td.close();
            }
        }
    }
    
    @Test
    public void testTinyDualMerge() throws Exception {
        for(int i = 0; i < 10; i++) {
            testNWayMergeProportion(2, 0.0001);
        }
    }
    
    @Test
    public void testOnePercentMerge() throws Exception {
        for(int i = 0; i < 10; i++) {
            testNWayMergeProportion(2, 0.01);
        }
    }
    
    @Test
    public void testTenPercentMerge() throws Exception {
        for(int i = 0; i < 10; i++) {
            testNWayMergeProportion(2, 0.1);
        }
    }
    
    @Test
    public void testFortyPercentMerge() throws Exception {
        for(int i = 0; i < 5; i++) {
            testNWayMergeProportion(2, 0.4);
        }
    }
}
