package com.sun.labs.minion.indexer.postings;

import com.sun.labs.minion.indexer.postings.io.PostingsInput;
import java.io.IOException;

/**
 *
 */
public interface PostingsTest {

    public Postings getPostings();

    public Postings getPostings(PostingsInput[] postIn, long[] offsets, int[] sizes) throws java.io.IOException;

    public void checkPostingsEncoding(Postings p, TestData testData, long[] offsets, int[] sizes) throws IOException;
}
