/*
 * Copyright (c) 2010-2015, Panasonic Corporation.
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.pslcl.dtf.core.generator.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.pslcl.dtf.core.Core;
import com.pslcl.dtf.core.artifact.Artifact;
import com.pslcl.dtf.core.generator.resource.Resource;
import com.pslcl.dtf.core.generator.template.TestInstance.Action;

/**
 * This class represents a single test instance, and relates to all of its
 * related information. Note that several of the fields in the database
 * are not represented here because during generation their values are not
 * relevant.
 */
public class TestInstance
{
    public static class TemplateSorter implements Comparator<DescribedTemplate>
    {
        @Override
        public int compare(DescribedTemplate o1, DescribedTemplate o2)
        {
            return o1.getTemplate().compareTo(o2.getTemplate());
        }

    }

    /**
     * This class represents all actions that can be taken in the using the test platform.
     * Implementers must be able to correctly modify a DescribedTemplate and attach necessary related
     * information that will be synchronized to the database.
     */
    public static abstract class Action
    {
        static class ActionSorter implements Comparator<Action>
        {
        	private Template template;
        	
        	public ActionSorter(){
        		this.template = null;
        	}
        	
        	public ActionSorter(Template template){
        		this.template = template;
        	}
        	
            @Override
            public int compare(Action o1, Action o2)
            {
                int o1SetID = o1.getSetID();
                int o2SetID = o2.getSetID();

                try
                {
                    if (o1SetID < o2SetID)
                        return -1;
                    else if (o1SetID > o2SetID)
                        return +1;

                    return o1.getCommand(template).compareTo(o2.getCommand(template));
                } catch (Exception e)
                {
                    return 0;
                }
            }
        }

        public static class ArtifactUses
        {
            private String reason;
            private Boolean primary;
            private Iterator<Artifact> artifacts;

            public ArtifactUses(String reason, Boolean primary, Iterator<Artifact> artifacts)
            {
                this.reason = reason;
                this.primary = primary;
                this.artifacts = artifacts;
            }

            public String getReason()
            {
                return reason;
            }

            public Boolean getPrimary()
            {
                return primary;
            }

            public Iterator<Artifact> getArtifacts()
            {
                return artifacts;
            }
        }
        
        private int setID = -1;
        protected List<Action> actionDependencies = new ArrayList<Action>();
        
        /**
         * Assign the set ID for this action. Actions which are in the same set may be executed in parallel.
         * All actions within a set must complete execution before the next set begins. Set IDs are executed
         * in increasing numeric order, beginning with 0. 
         * 
         * @param setID The set ID.
         */
        public void assignSetID(int setID){
        	this.setID = setID;
        }
        
        /**
         * Get the ID of the set to which this action is assigned.
         * 
         * @return The setID or -1 if a set ID has not been assigned.
         */
        public int getSetID(){
        	return setID;
        }

        /**
         * Return the canonical command that will be added to the template.
         * @param t The template that the command is in. The command should be appropriate for the template.
         *      Resource and Template actions must accept a null parameter.
         * @return The canonical command.
         * @throws Exception Any error.
         */
        public abstract String getCommand(Template t) throws Exception;

        /**
         * Return an HTML snippet that describes the action in terms a human could
         * execute manually. This can be null if the platform knows how to generate
         * the string itself.
         * @return An array of documentation lines, each containing valid HTML suitable for inclusion as inner HTML to a list item.
         * @throws Exception Any error.
         */
        public abstract String getDescription() throws Exception;

        /**
         * If the action uses artifacts, then return the set of artifacts used. These
         * can come from any set of versions. Relationships for the artifacts
         * @return A class that defines all resources used, or null if no artifacts used.
         * @throws Exception Any error.
         */
        public abstract ArtifactUses getArtifactUses() throws Exception;

        /**
         * If the action binds a resource, then return the resource. A single
         * action can only ever use a single resource.
         * @return The resource used, or null if no resource is used.
         * @throws Exception Any error.
         */
        public abstract Resource getBoundResource() throws Exception;

        /**
         * If the action references another template, then return the template. A single
         * action can only ever reference a single other template.
         * @return The template used, or null if no template is referenced.
         * @throws Exception Any error.
         */
        public abstract DescribedTemplate getIncludedTemplate() throws Exception;
        
        /**
         * Returns a list of actions that must be completed before this action may be
         * executed. For example, a deploy may require that a machine is bound.
         * 
         * @return A list of dependent actions. Returns an empty list if there are no dependencies.
         * @throws Exception Any error.
         */
		public List<Action> getActionDependencies() throws Exception {
			return actionDependencies;
		}
    }

    @SuppressWarnings("unused")
    private static class IncludeAction extends Action
    {
        private DescribedTemplate include;

        IncludeAction(DescribedTemplate include)
        {
            this.include = include;
        }

        @Override
        public String getCommand(Template t) throws Exception
        {
            return String.format("include %s", include.getTemplate().getHash().toString());
        }

        @Override
        public String getDescription() throws Exception
        {
            StringBuilder sb = new StringBuilder();
            sb.append("<li>Follow the steps for template <tt>");
            sb.append(include.getTemplate().getHash().toString());
            sb.append("</tt>.</li>");
            return sb.toString();
        }

        @Override
        public ArtifactUses getArtifactUses() throws Exception
        {
            return null;
        }

        @Override
        public Resource getBoundResource() throws Exception
        {
            return null;
        }

        @Override
        public DescribedTemplate getIncludedTemplate() throws Exception
        {
            return include;
        }

		@Override
		public List<Action> getActionDependencies() throws Exception {
			//Include actions may not depend on any other actions.
			return new ArrayList<Action>();
		}

		@Override
		public void assignSetID(int setID) {
			throw new UnsupportedOperationException("Inspect actions do not have a set ID");
		}
    }

    /**
     * The core to use for database and other common access. Note that the core
     * represents the generation of a single test, and it maintains the primary
     * key of that test. All TestInstance instances will relate to that test.
     */
    private final Core core;

    /**
     * The primary key of this test instance, or zero if unknown.
     */
    //TODO: needs thread safe
    public long pk = 0;

    /**
     * The related described template for this test instance, or null if it
     * does not exist. The primary key of the described template is held by
     * it locally.
     */
    private DescribedTemplate dtemplate;

    /**
     * During generation the result of a test instance may be specified directly
     * without the test instance actually being run. This variable is a tri-state,
     * with null indicating no result, false indicating failure, and true indicating success.
     */
    private Boolean result = null;

    /**
     * During generation the owner of a test instance may be specified directly
     * without the test instance being run. If set then a 'run' entry will be created.
     * This can work in combination with the 'result' field to create different combinations
     * of test status - although this is usually useful only for test data population.
     */
    private String owner = null;

    /**
     * During generation of test data the different testing dates can be set.
     */
    private Date start = null, ready = null, complete = null;

    /**
     * All actions related to a test instance are stored in this list. Once the
     * test instance is defined completely, indicated by {@link #close()} being
     * called, then the actions are distributed among templates.
     */
    private List<Action> actions = new ArrayList<Action>();

    /**
     * All resources used by a test instance will be included here. This is used
     * to ensure that resources are bound a single time.
     */
    private Set<UUID> boundResources = new HashSet<UUID>();

    /**
     * Create a new test instance related to a specific core, which itself is
     * related to a particular test.
     * @param core The core to use, which provides database access and common support.
     */
    public TestInstance(Core core)
    {
        this.core = core;

        //TODO: Cleanup
        //testResource = new Template( core, this );
        //parameters.clear();
        //versionsUsed.clear();
        //result = null;
    }

    /**
     * Add an action to this test instance. These actions are self describing, and
     * are used at the end to generate appropriate templates. The actions are added
     * in the order that they should effectively take place in.
     * @param action The action to add.
     * @throws Exception The action is invalid.
     */
    public void addAction(Action action) throws Exception
    {
        if (actions == null)
        {
            System.err.println("ERROR: Attempt to add an action to a closed test instance.");
            return;
        }

        // Handle resources. We have to check that each resource is only bound once.
        Resource r = action.getBoundResource();
        if (r != null)
        {
            if (boundResources.contains(r.instance))
            {
                System.err.println(String.format("Resource %s (%s) (%s) rebound.", r.name, r.codename, r.instance));
            }

            boundResources.add(r.instance);
        }

        actions.add(action);
    }

    /**
     * Return the actions associated with a test instance.
     * @return The list of actions associated with the test instance.
     */
    public List<Action> getActions()
    {
        return actions;
    }

    public void pass()
    {
        result = true;
    }

    public void fail()
    {
        result = false;
    }

    public void assign(String email)
    {
        owner = email;
    }

    public void setRunTimes(Date start, Date ready, Date complete)
    {
        this.start = start;
        this.ready = ready;
        this.complete = complete;
    }

    public void dump()
    {
        System.err.println("Test Instance:");
        System.err.println(getTemplate());
    }

    /**
     * Close the definition of a test instance. This causes the instance to reject
     * any further actions, and must be called prior to any synchronization efforts.
     * @throws Exception Any Error creating the template.
     */
    public void close() throws Exception
    {
        /*
         * We have a set of actions that were added in order. We need to create a set of templates that
         * represent those actions. The template itself can change the ordering of the actions that it holds,
         * and so the set of actions have to be divided into templates, sorted, and finally converted to
         * standard formats with associated documentation.
         */

        // Divide the set of actions into a set of related templates.
        //TODO: Implement breaking actions into templates.
    	
    	assignSetIDs();

        // Sort each set of actions (only one set for now)
        Collections.sort(actions, new Action.ActionSorter());

        // Determine dependencies for each template (none for now)
        List<DescribedTemplate> dependencies = new ArrayList<DescribedTemplate>();

        // Sort the dependencies list.
        Collections.sort(dependencies, new TestInstance.TemplateSorter());

        // Create a Template for each group (only one for now)..
        Template template = new Template(core, actions, dependencies);

        dtemplate = new DescribedTemplate(template, actions, dependencies);
    }
    
    private void assignSetIDs() throws Exception{
    	List<Action> unassignedActions = new ArrayList<Action>();
    	List<Action> assignedActions = new ArrayList<Action>();
    	unassignedActions.addAll(actions);
    	
    	int setID = 0;
    	while(unassignedActions.size() > 0){
    		List<Action> currentSet = new ArrayList<Action>();
    		for(Action action: unassignedActions){
    			if(action instanceof TestInstance.IncludeAction){
    				currentSet.add(action);
    			}
    			else if(action.getActionDependencies().size() == 0){
    				action.assignSetID(setID);
    				currentSet.add(action);
    			} else if(assignedActions.containsAll(action.getActionDependencies())){
    				action.assignSetID(setID);
    				currentSet.add(action);
    			}
    		}
    		unassignedActions.removeAll(currentSet);
    		assignedActions.addAll(currentSet);
    		if(currentSet.isEmpty()){
    			throw new Exception("Failed to find action dependencies and assigning set ID for action: " + unassignedActions.get(0).getDescription());
    		}
    		setID++;
    	}
    }

    public DescribedTemplate getTemplate()
    {
        return dtemplate;
    }

    public Boolean getResult()
    {
        return result;
    }

    public String getOwner()
    {
        return owner;
    }

    public Date getStart()
    {
        return start;
    }

    public Date getReady()
    {
        return ready;
    }

    public Date getComplete()
    {
        return complete;
    }

    public long getPK()
    {
        return pk;
    }

    public void sync(long pk_test)
    {
        if (pk == 0)
            pk = core.syncTestInstance(this, pk_test);
    }
}
