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


import com.pslcl.dtf.core.Core;
import com.pslcl.dtf.core.Hash;
import com.pslcl.dtf.core.artifact.Content;
import com.pslcl.dtf.core.generator.resource.Resource;
import com.pslcl.dtf.core.generator.template.TestInstance.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Template implements Comparable<Template>
{
    public static interface Exportable
    {
        String getTag();

        void export(UUID tag);
    }

    /**
     * This interface represents a parameter that will be evaluated in the context of a template
     * at a later time.
     */
    public static interface Parameter
    {
        /**
         * Return the value of the parameter in the context of the specified template.
         * @param template The template in which to evaluate the parameter.
         * @return The string value of the parameter.
         * @throws Exception Thrown if the value cannot be determined.
         */
        String getValue(Template template) throws Exception;
    }

    public static class ResourceParameter implements Parameter
    {
        Resource resource;

        public ResourceParameter(Resource resource)
        {
            this.resource = resource;
        }

        @Override
        public String getValue(Template template) throws Exception
        {
            if (template != null)
                return template.getReference(resource);
            return "unknownValue"; // this replaces the null ptr exception that used to happen
            // note that final template processing seems to overwrite this line "unknownValue"
            //      if true, that would mean that we encounter this null template ONLY early in the processing stage
        }
    }

    public static class StringParameter implements Parameter
    {
        String parameter;

        public StringParameter(String parameter)
        {
            this.parameter = parameter;
        }

        @Override
        public String getValue(Template template)
        {
            try
            {
                return URLEncoder.encode(parameter, "UTF-8");
            } catch (Exception e)
            {
                // UTF-8 is required, will never happen.
                return "";
            }
        }
    }

    public static class ExportParameter implements Parameter
    {
        Exportable exportable;

        public ExportParameter(Exportable exportable)
        {
            this.exportable = exportable;
        }

        @Override
        public String getValue(Template template) throws Exception
        {
            return "[" + exportable.getTag() + "]";
        }
    }

    public static class AttributeParameter implements Parameter
    {
        ResourceParameter resourceParameter;
        String attribute;
        String value;

        /**
         * Create an AttributeParameter with a known value.
         * 
         * @param attribute the attribute name.
         * @param value the value of the attribute.
         */
        public AttributeParameter(String attribute, String value)
        {
            this.attribute = attribute;
            this.value = value;
        }

        /**
         * Create an AttributeParameter if the value is unknown.
         * 
         * @param resource The resource associated with the attribute.
         * @param attribute The attribute name.
         */
        public AttributeParameter(Resource resource, String attribute)
        {
            this.resourceParameter = new ResourceParameter(resource);
            this.attribute = attribute;
        }

        /**
         * Get the value of the attribute. If the value of the attribute is known, then it will be returned.
         * If the value of the attribute will not be known until the test is run, then a value reference in the 
         * form of $(attribute resource-ref attribute-name) will be returned instead.
         * 
         * @return The value or value reference.
         */
        @Override
        public String getValue(Template template) throws Exception
        {
            String attributeValue;
            if (value != null)
            {
                attributeValue = value;
            } else{
                attributeValue = "$(attribute " + resourceParameter.getValue(template) + " " + attribute + ")";
            }
            try
            {
                return URLEncoder.encode(attributeValue, "UTF-8");
            } catch (Exception e)
            {
                // UTF-8 is required, will never happen.
                return "";
            }
        }
    }

    @SuppressWarnings("unused")
    private class Command
    {
        private String command;
        private Parameter[] parameters;
        private String result = null;

        private Command(String command, Parameter... parameters)
        {
            this.command = command;
            this.parameters = parameters;
        }

        private void finalize(Template template) throws Exception
        {
            StringBuilder sb = new StringBuilder();
            sb.append(command);
            for (Parameter p : parameters)
            {
                String v = p.getValue(template);
                if (v.length() > 0)
                {
                    sb.append(" ");
                    sb.append(v);
                }
            }

            result = sb.toString();
        }

        @Override
        public String toString()
        {
            if (result == null)
                throw new IllegalStateException("Command cannot be converted to string until finalized.");

            return result;
        }
    }

    @Override
    public int compareTo(Template o2)
    {
        return getHash().compareTo(o2.getHash());
    }

    private final Logger log;
    private Core core;
    private List<TestInstance.Action> actions;
    private List<DescribedTemplate> dependencies;
    private Map<UUID, String> references = new HashMap<UUID, String>();

    //TODO: Cleanup
    //private Generator generator;
    //private List<Template> templates = new ArrayList<Template>();
    //TODO: threadsafe
    public List<Template> allTemplates = new ArrayList<Template>();
    //private List<Resource> resources = new ArrayList<Resource>();
    public List<Content> artifacts = new ArrayList<Content>();
    //private List<Command> commands = new ArrayList<Command>();
    Hash hash = null;
    public String std_string = null;
    private long pk;

    /**
     * Construct a Template from a list of actions, specifying any dependencies.
     * @param core The core used to synchronize the Template.
     * @param actions The actions that the template will perform.
     * @param dependencies Any dependent templates. May be null or an empty list if none.
     * @throws Exception on constructor error
     */
    public Template(Core core, List<TestInstance.Action> actions, List<DescribedTemplate> dependencies) throws Exception
    {
        this.log = LoggerFactory.getLogger(getClass());
        this.core = core;
        this.actions = actions;
        actions.sort(new Action.ActionSorter());

        if (dependencies == null)
            this.dependencies = new ArrayList<DescribedTemplate>();
        else
            this.dependencies = dependencies;

        buildStrings();
    }

    /**
     * Build the string representation of the template. This format is canonical, and
     * must be strictly followed. The first section contains all dependencies, which are sorted
     * textually. Next come the actions, which are sorted textually, beginning with the set ID.
     * @throws Exception on error
     */
    private void buildStrings() throws Exception
    {
        try
        {
            StringBuilder standardString = new StringBuilder();
            for (DescribedTemplate T : dependencies)
            {
                standardString.append("include ");
                standardString.append(T.getTemplate().getHash().toString());
                standardString.append("\n");
            }

            int offset = dependencies.size();
            int currentSetID = 0;
            for (int i = 0; i < actions.size(); i++)
            {
                Action action = actions.get(i);
                standardString.append(action.getCommand(this));
                standardString.append("\n");

                Resource resource = action.getBoundResource();
                if (resource != null)
                    references.put(resource.instance, Integer.toString(offset + i));
                
                //Sort again if we are about to move to next set. This allows resource references assigned in the current set to be used in the sort.
                //This should only change the order of actions that we have not yet gotten to.
                if(i < actions.size() - 1 && actions.get(i + 1).getSetID() > currentSetID){
                    actions.sort(new Action.ActionSorter(this));
                }
            }

            std_string = standardString.toString();
            hash = Hash.fromContent(std_string);
        } catch (Exception e)
        {
            this.log.error("<internal> Template.buildStrings(): Failed to build template string: " + e);
            std_string = "";
            hash = null;
            throw e;
        }
    }

    public String toStandardString()
    {
        return std_string;
    }

    @Override
    public String toString()
    {
        return String.format("Template " + getHash().toString());
    }

    public Hash getHash()
    {
        return hash;
    }

    public void add(Content a)
    {
        artifacts.add(a);
    }

    public Collection<Template> getTemplates()
    {
        return allTemplates;
    }

    public String getReference(Resource resource) throws Exception
    {
        if (references.containsKey(resource.instance))
            return references.get(resource.instance);

        int dep = 0;
        for (DescribedTemplate t : dependencies)
        {
            String ref = t.getTemplate().getReference(resource);
            if (ref != null)
            {
                ref = String.format("%d/%s", dep, ref);
                references.put(resource.instance, ref);
                return ref;
            }
        }

        throw new Exception("Resource cannot be found.");
    }

    public long getPK()
    {
        return pk;
    }

    public void sync() {
        if (this.pk == 0)
            try {
                this.pk = core.getStorage().syncTemplate(this);
            } catch (SQLException sqle) {
                // this.pk remains 0
                this.log.error(".syncTemplate(): Couldn't synchronize template, msg: " + sqle);
            }
    }

    public void syncRelationships()
    {
        core.syncTemplateRelationships(this);
    }
}
