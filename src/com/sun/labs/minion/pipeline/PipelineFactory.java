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

package com.sun.labs.minion.pipeline;

import com.sun.labs.minion.HLPipeline;
import com.sun.labs.minion.Indexable;
import com.sun.labs.minion.Pipeline;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.util.props.ConfigComponent;
import com.sun.labs.util.props.ConfigComponentList;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.ConfigurationManager;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import com.sun.labs.minion.indexer.partition.Dumper;
import com.sun.labs.minion.util.MinionLog;

/**
 *
 * A configurable factor class for pipelines.  This factor class can be used to
 * get synchronous or asynchronous pipeline stages.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class PipelineFactory implements Configurable {
    
    /**
     * The name of the factory.
     */
    private String name;
    
    /**
     * The manager that configured this factory.
     */
    ConfigurationManager cm;
    
    protected static MinionLog log = MinionLog.getLog();
    
    protected static String logTag = "PF";
    
    /**
     * Creates a pipeline factory.  This class should be configured using the configuration
     * manager for the indexer.
     */
    public PipelineFactory() {
    }
    
    /**
     * Gets a synchronous pipeline configured according to the configuration of the indexer.
     */
    public Pipeline getSynchronousPipeline(SearchEngine engine) {
        return new SyncPipelineImpl(this, engine, getPipeline(stages), dumper);
    }
    
    /**
     * Gets an asynchronous pipeline configured according to the configuration of the indexer.
     *
     * @param indexingQueue a queue from which the pipeline will draw the documents that
     * it will process
     */
    public Pipeline getAsynchronousPipeline(SearchEngine engine,
            BlockingQueue<Indexable> indexingQueue) {
        return new AsyncPipelineImpl(this, engine, getPipeline(stages), dumper, indexingQueue);
    }
    
    /**
     * Gets a highlighting pipeline configured according to the configuration.
     *
     */
    public HLPipeline getHLPipeline(SearchEngine engine) {
        return new HLPipelineImpl(this, engine, getPipeline(hlStages));
    }
    
    /**
     * Gets a set of new instances for the configured stages.  The stages will
     * be connected together into a pipeline.
     *
     * @return a list of stages, connected together in a pipeline.
     */
    private List<Stage> getPipeline(List p) {
        List<Stage> ret = new ArrayList<Stage>();
        Stage prev = null;
        for(Iterator i = p.iterator(); i.hasNext(); ) {
            Stage s = (Stage) i.next();
            try {
                
                //
                // Get a new instance of this stage and configure it.
                Stage next = s.getClass().newInstance();
                next.newProperties(cm.getPropertySheet(s.getName()));
                if(prev != null) {
                    prev.setDownstream(next);
                }
                ret.add(next);
                prev = next;
            } catch (IllegalAccessException ex) {
                log.error(logTag, 1, "Error instantiating: " + s);
            } catch (InstantiationException ex) {
                log.error(logTag, 1, "Error instantiating: " + s);
            } catch (PropertyException pe) {
                log.error(logTag, 1, "Error configuring: " + s);
            }
        }
        
        return ret;
    }
    
    /**
     * Gets a new indexing stage, so that such stages can be handed off to
     * someone else for dumping while indexing proceeds.
     */
    public Stage getIndexingStage() {
        
        Stage s = (Stage) stages.get(stages.size() -1);
        try {
            Stage ret = s.getClass().newInstance();
            ret.newProperties(cm.getPropertySheet(s.getName()));
            return ret;
        } catch (IllegalAccessException ex) {
            log.error(logTag, 1, "Error instantiating: " + s);
        } catch (InstantiationException ex) {
            log.error(logTag, 1, "Error instantiating: " + s);
        } catch (PropertyException pe) {
            log.error(logTag, 1, "Error configuring: " + s);
        }
        return null;
    }
    
    
     public void newProperties(PropertySheet ps) throws PropertyException {        
        cm = ps.getConfigurationManager();
        stages = ps.getComponentList(PROP_STAGES);
        hlStages = ps.getComponentList(PROP_HL_STAGES);
        dumper = (Dumper) ps.getComponent(PROP_DUMPER);
    }
    
    public String getName() {
        return name;
    }
    
    @ConfigComponentList(type=com.sun.labs.minion.pipeline.Stage.class)
    public static final String PROP_STAGES = "stages";
    
    private List stages;
    
    @ConfigComponentList(type=com.sun.labs.minion.pipeline.Stage.class)
    public static final String PROP_HL_STAGES = "hl_stages";
    
    private List hlStages;

    @ConfigComponent(type=com.sun.labs.minion.indexer.partition.Dumper.class)
    public static final String PROP_DUMPER = "dumper";

    private Dumper dumper;
    
}
