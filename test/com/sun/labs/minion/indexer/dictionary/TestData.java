package com.sun.labs.minion.indexer.dictionary;

import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.partition.DumpState;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.util.LabsLogFormatter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public class TestData {

    protected List<String> words = new ArrayList<String>();

    protected MemoryDictionary<String> md;

    protected DiskDictionary<String> dd;

    protected File dictFile;

    protected RandomAccessFile raf;

    protected SortedSet<String> uniq = new TreeSet<String>();

    public TestData(List<String> base) throws Exception {
        this(Math.max((int) (base.size() * 0.5), 1000), base);
    }

    public TestData(int n, List<String> base) throws Exception {
        Random r = new Random();
        for(int i = 0; i < n; i++) {
            words.add(base.get(r.nextInt(base.size())));
        }
        Collections.sort(words);
        init();
    }

    public TestData(String resourceName) throws Exception {
        InputStream is = DictionaryTest.class.getResourceAsStream(resourceName);
        if(is == null) {
            is = TestData.class.getResourceAsStream("/com/sun/labs/minion/indexer/dictionary/resource/"
                    + resourceName);
        }
        if(is == null) {
            throw new java.io.IOException(String.format(
                    "Couldn\'t find resource %s",
                    resourceName));
        }
        BufferedReader r;
        if(resourceName.endsWith(".gz")) {
            r = new BufferedReader(new InputStreamReader(new GZIPInputStream(
                    is), "utf-8"));
        } else {
            r = new BufferedReader(new InputStreamReader(is, "utf-8"));
        }
        String w;
        while((w = r.readLine()) != null) {
            words.add(w);
        }
        init();
    }

    public TestData(File dictFile) throws Exception {
        this.dictFile = dictFile;
        raf = new RandomAccessFile(dictFile, "r");
        dd = new DiskDictionary<String>(new EntryFactory<String>(
                Postings.Type.NONE),
                new StringNameHandler(), raf,
                new RandomAccessFile[0]);
        for(Entry<String> e : dd) {
            words.add(e.getName());
            uniq.add(e.getName());
        }
    }

    private void init() throws Exception {
        md = new MemoryDictionary<String>(new EntryFactory(Postings.Type.NONE));
        for(String w : words) {
            md.put(w);
            uniq.add(w);
        }
        DumpState dumpState = new DumpState(DictionaryTest.tmpDir);
        dumpState.renumber = MemoryDictionary.Renumber.RENUMBER;
        dumpState.idMap = MemoryDictionary.IDMap.NONE;
        dumpState.encoder = new StringNameHandler();
        md.dump(dumpState);
        dictFile = File.createTempFile("all", ".dict");
        dictFile.deleteOnExit();
        raf = new RandomAccessFile(dictFile, "rw");
        dumpState.fieldDictOut.flush(raf);
        dumpState.close();
        raf.close();
        raf = new RandomAccessFile(dictFile, "r");
        dd = new DiskDictionary<String>(new EntryFactory<String>(
                Postings.Type.NONE),
                new StringNameHandler(), raf,
                new RandomAccessFile[0]);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final TestData other = (TestData) obj;
        
        return this.uniq.equals(other.uniq);
    }

    @Override
    public int hashCode() {
        return uniq.hashCode();
    }
    
    public boolean checkConsistency(TestData... data) {
        SortedSet<String> mu = new TreeSet<String>();
        for(TestData td : data) {
            mu.addAll(td.uniq);
        }
        return uniq.equals(mu);
    }
    

    public File dump() throws Exception {
        File f = File.createTempFile("words", "");
        PrintWriter pw =
                new PrintWriter(new OutputStreamWriter(new FileOutputStream(f),
                "utf-8"));
        for(String w : words) {
            pw.println(w);
        }
        pw.close();
        return f;
    }

    public void close() throws Exception {
        raf.close();
    }
    
    public static void main(String[] args) throws Exception {
        Logger l = Logger.getLogger("");
        for(Handler h : l.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new LabsLogFormatter());
        }
        TestData td = new TestData("words.gz");
    }
}
