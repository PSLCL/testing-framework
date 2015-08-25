package com.pslcl.qa.platform.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * This class represents a single test instance, and relates to all of its
 * related information. Note that several of the fields in the database
 * are not represented here because during generation their values are not
 * relevant.
 */
class TestInstance {
    static class TemplateSorter implements Comparator<DescribedTemplate> {
        @Override
        public int compare(DescribedTemplate o1, DescribedTemplate o2) {
            return o1.getTemplate().compareTo( o2.getTemplate() );
        }

    }

    /**
     * This class represents all actions that can be taken in the using the test platform.
     * Implementers must be able to correctly modify a DescribedTemplate and attach necessary related
     * information that will be synchronized to the database.
     */
    static abstract class Action {
        static class ActionSorter implements Comparator<Action> {
            public int compare( Action o1, Action o2 ) {
                // Resources sort into first group, then sort by toString.
                // Remaining actions sort into the second group, but are equal (meaning no reordering done)
                int o1group, o2group;

                // Assign a group to each action
                try {
                    if ( o1.getIncludedTemplate() != null )
                        o1group = 0;
                    else if ( o1.getBoundResource() != null )
                        o1group = 1;
                    else
                        o1group = 2;

                    if ( o2.getIncludedTemplate() != null )
                        o2group = 0;
                    else if ( o2.getBoundResource() != null )
                        o2group = 1;
                    else
                        o2group = 2;

                    if ( o1group < o2group )
                        return -1;
                    else if ( o1group > o2group )
                        return +1;

                    // The groups are equal. Groups 0 and 1 sort by command. Group 2 never sorts.
                    if ( o1group == 2 )
                        return 0;
                    
                    return o1.getCommand( null ).compareTo( o2.getCommand( null ) );
                }
                catch ( Exception e ) {
                    return 0;
                }
            }
        }

        static public class ArtifactUses {
            private String reason;
            private Boolean primary;
            private Iterator<Artifact> artifacts;

            public ArtifactUses( String reason, Boolean primary, Iterator<Artifact> artifacts ) {
                this.reason = reason;
                this.primary = primary;
                this.artifacts = artifacts;
            }

            public String getReason() {
                return reason;
            }

            public Boolean getPrimary() {
                return primary;
            }

            public Iterator<Artifact> getArtifacts() {
                return artifacts;
            }
        }

        /**
         * Return the canonical command that will be added to the template.
         * @param t The template that the command is in. The command should be appropriate for the template.
         *      Resource and Template actions must accept a null parameter.
         * @return The canonical command.
         * @throws Exception Any error.
         */
        abstract String getCommand( Template t ) throws Exception;

        /**
         * Return an HTML snippet that describes the action in terms a human could
         * execute manually. This can be null if the platform knows how to generate
         * the string itself.
         * @return An array of documentation lines, each containing valid HTML suitable for inclusion as inner HTML to a list item.
         * @throws Exception Any error.
         */
        abstract String getDescription() throws Exception;

        /**
         * If the action uses artifacts, then return the set of artifacts used. These
         * can come from any set of versions. Relationships for the artifacts
         * @return A class that defines all resources used, or null if no artifacts used.
         * @throws Exception Any error.
         */
        abstract ArtifactUses getArtifactUses() throws Exception;

        /**
         * If the action binds a resource, then return the resource. A single
         * action can only ever use a single resource.
         * @return The resource used, or null if no resource is used.
         * @throws Exception Any error.
         */
        abstract Resource getBoundResource() throws Exception;
        
        /**
         * If the action references another template, then return the template. A single
         * action can only ever reference a single other template.
         * @return The template used, or null if no template is referenced.
         * @throws Exception Any error.
         */
        abstract DescribedTemplate getIncludedTemplate() throws Exception;
    }

    @SuppressWarnings("unused")
    private static class IncludeAction extends Action {
        private DescribedTemplate include;
        
        IncludeAction( DescribedTemplate include ) {
            this.include = include;
        }
        
        @Override
        String getCommand(Template t) throws Exception {
            return String.format("include %s", include.getTemplate().getHash().toString() );
        }

        @Override
        String getDescription() throws Exception {
            StringBuilder sb = new StringBuilder();
            sb.append( "<li>Follow the steps for template <tt>" );
            sb.append( include.getTemplate().getHash().toString() );
            sb.append( "</tt>.</li>" );
            return sb.toString();
        }

        @Override
        ArtifactUses getArtifactUses() throws Exception {
            return null;
        }

        @Override
        Resource getBoundResource() throws Exception {
            return null;
        }

        @Override
        DescribedTemplate getIncludedTemplate() throws Exception {
            return include;
        }
    }
    
    /**
     * The core to use for database and other common access. Note that the core
     * represents the generation of a single test, and it maintains the primary
     * key of that test. All TestInstance instances will relate to that test.
     */
    private Core core;

    /**
     * The primary key of this test instance, or zero if unknown.
     */
    long pk = 0;

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
    TestInstance( Core core ) {
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
    void addAction( Action action ) throws Exception{
        if ( actions == null ) {
            System.err.println( "ERROR: Attempt to add an action to a closed test instance." );
            return;
        }

        // Handle resources. We have to check that each resource is only bound once.
        Resource r = action.getBoundResource();
        if ( r != null ) {
            if ( boundResources.contains( r.instance ) ) {
                System.err.println( String.format("Resource %s (%s) (%s) rebound.", r.name, r.hash, r.instance));
            }

            boundResources.add( r.instance );
        }

        actions.add( action );
    }

    /**
     * Return the actions associated with a test instance.
     * @return The list of actions associated with the test instance.
     */
    List<Action> getActions() {
        return actions;
    }
    
    void pass() {
        result = true;
    }

    void fail() {
        result = false;
    }

    void dump() {
        System.err.println( "Test Instance:" );
        System.err.println( getTemplate() );
    }

    /**
     * Close the definition of a test instance. This causes the instance to reject
     * any further actions, and must be called prior to any synchronization efforts.
     */
    void close() {
        /*
         * We have a set of actions that were added in order. We need to create a set of templates that
         * represent those actions. The template itself can change the ordering of the actions that it holds,
         * and so the set of actions have to be divided into templates, sorted, and finally converted to
         * standard formats with associated documentation.
         */

        // Divide the set of actions into a set of related templates.
        //TODO: Implement breaking actions into templates.

        // Sort each set of actions (only one set for now)
        Collections.sort( actions, new Action.ActionSorter() );

        // Determine dependencies for each template (none for now)
        List<DescribedTemplate> dependencies = new ArrayList<DescribedTemplate>();

        // Sort the dependencies list.
        Collections.sort( dependencies, new TestInstance.TemplateSorter() );

        // Create a Template for each group (only one for now)..
        Template template = new Template( core, actions, dependencies );

        dtemplate = new DescribedTemplate( template, actions, dependencies );
    }

    DescribedTemplate getTemplate() {
        return dtemplate;
    }

    public Boolean getResult() {
        return result;
    }

    long getPK() {
        return pk;
    }

    void sync( long pk_test ) {
        if ( pk == 0 )
            pk = core.syncTestInstance( this, pk_test );
    }
}
