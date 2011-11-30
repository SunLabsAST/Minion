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
import com.sun.labs.minion.Pipeline;
import com.sun.labs.util.props.ConfigComponentList;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.ConfigurationManager;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * A configurable factor class for pipelines.  This factor class can be used to
 * get synchronous or asynchronous pipeline stages.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class PipelineFactory implements Configurable {

    static final Logger logger = Logger.getLogger(
            PipelineFactory.class.getName());

    @ConfigComponentList(type = com.sun.labs.minion.pipeline.Stage.class)
    public static final String PROP_STAGES = "stages";

    private List<Stage> stages;

    @ConfigComponentList(type = com.sun.labs.minion.pipeline.Stage.class)
    public static final String PROP_HL_STAGES = "hl_stages";

    private List<Stage> hlStages;

    private String name;

    /**
     * The manager that configured this factory.
     */
    private ConfigurationManager cm;


    /**
     * Creates a pipeline factory.  This class should be configured using the configuration
     * manager for the indexer.
     */
    public PipelineFactory() {
    }

    /**
     * Gets a synchronous pipeline configured according to the configuration of the indexer.
     */
    public Pipeline getPipeline() {
        return new PipelineImpl(getPipeline(stages));
    }

    /**
     * Gets a highlighting pipeline configured according to the configuration.
     *
     */
    public HLPipeline getHLPipeline() {
        return new HLPipelineImpl(getPipeline(hlStages));
    }

    /**
     * Adds a stage to the pipelines that this factory will generate.  Note that
     * new instances of this stage will be generated when the factory method is
     * called.
     *
     * @param s the stage to add to the factory.
     */
    public void addStage(Stage s) {
        if(stages == null) {
            stages = new ArrayList<Stage>();
        }
        stages.add(s);
    }

    /**
     * Gets a set of new instances for the configured stages.  The stages will
     * be connected together into a pipeline.
     *
     * @return a list of stages, connected together in a pipeline.
     */
    private List<Stage> getPipeline(List<Stage> p) {
        List<Stage> ret = new ArrayList<Stage>();
        Stage prev = null;
        for(Stage s : p) {
            try {

                //
                // Get a new instance of this stage and configure it.
                Stage next = s.getClass().newInstance();
                if(cm != null && s.getName() != null) {
                    next.newProperties(cm.getPropertySheet(s.getName()));
                }
                if(prev != null) {
                    prev.setDownstream(next);
                }
                ret.add(next);
                prev = next;
            } catch(IllegalAccessException ex) {
                logger.severe(String.format("Error instantiating: %s", s));
            } catch(InstantiationException ex) {
                logger.severe(String.format("Error instantiating: %s", s));
            } catch(PropertyException pe) {
                logger.severe(String.format("Error instantiating: %s", s));
            }
        }

        return ret;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        name = ps.getInstanceName();
        cm = ps.getConfigurationManager();
        stages = (List<Stage>) ps.getComponentList(PROP_STAGES);
        hlStages = (List<Stage>) ps.getComponentList(PROP_HL_STAGES);
    }

}
