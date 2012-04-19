package com.sun.labs.minion.util;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MBeanServer;

public class HeapDumper {
    // This is the name of the HotSpot Diagnostic MBean
    private static final String HOTSPOT_BEAN_NAME =
         "com.sun.management:type=HotSpotDiagnostic";

    // field to store the hotspot diagnostic MBean 
    private static volatile HotSpotDiagnosticMXBean hotspotMBean;
    
    private static final Logger logger = Logger.getLogger(HeapDumper.class.getName());
    
    static {
        try {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        hotspotMBean = ManagementFactory.newPlatformMXBeanProxy(server,
                HOTSPOT_BEAN_NAME, HotSpotDiagnosticMXBean.class);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, String.format("Error getting mbean for heap dumper"), ex);
        }
    }

    /**
     * Call this method from your application whenever you 
     * want to dump the heap snapshot into a file.
     *
     * @param fileName name of the heap dump file
     * @param live flag that tells whether to dump
     *             only the live objects
     */
    public static void dumpHeap(String fileName, boolean live) {
        try {
            logger.info(String.format("Dumping heap into %s", fileName));
            hotspotMBean.dumpHeap(fileName, live);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception exp) {
            throw new RuntimeException(exp);
        }
    }

    public static void main(String[] args) {
        // default heap dump file name
        String fileName = "heap.bin";
        // by default dump only the live objects
        boolean live = true;

        // simple command line options
        switch (args.length) {
            case 2:
                live = args[1].equals("true");
            case 1:
                fileName = args[0];
        }

        // dump the heap
        dumpHeap(fileName, live);
    }
}
