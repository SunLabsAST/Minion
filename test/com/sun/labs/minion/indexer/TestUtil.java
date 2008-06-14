/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.labs.minion.indexer;

import java.io.File;
import static org.junit.Assert.*;

/**
 * A set of test utilities for testing.
 */
public class TestUtil {

    public static void deleteDirectory(File indexDir) {
        File[] fs = indexDir.listFiles();
        for(File f : fs) {
            if(f.isDirectory()) {
                deleteDirectory(f);
            } else {
                assertTrue(f.delete());
            }
        }
        assertTrue(indexDir.delete());
    }
}
