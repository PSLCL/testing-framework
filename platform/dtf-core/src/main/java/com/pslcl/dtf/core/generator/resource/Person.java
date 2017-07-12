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
package com.pslcl.dtf.core.generator.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.pslcl.dtf.core.artifact.Artifact;
import com.pslcl.dtf.core.artifact.Content;
import com.pslcl.dtf.core.generator.Generator;
import com.pslcl.dtf.core.generator.template.DescribedTemplate;
import com.pslcl.dtf.core.generator.template.Template;
import com.pslcl.dtf.core.generator.template.Template.Parameter;
import com.pslcl.dtf.core.generator.template.Template.StringParameter;
import com.pslcl.dtf.core.generator.template.TestInstance;
import com.pslcl.dtf.core.generator.template.TestInstance.Action;

/**
 * This class represents a person, which is a resource that can perform manual inspections.
 */
public class Person extends Resource
{
    private static class InspectAction extends TestInstance.Action
    {
        private Resource inspector;
        private Content body;
        private Artifact[] attachments;
        
        private InspectAction(Resource inspector, Content body, Artifact[] attachments, List<Action> actionDependencies)
        {
            this.inspector = inspector;
            this.body = body;
            this.attachments = attachments;

            if(actionDependencies != null){
                this.actionDependencies.addAll(actionDependencies);
            }
            Action inspectorBindAction = inspector.getBindAction();
            if(!this.actionDependencies.contains(inspectorBindAction)){
                this.actionDependencies.add(inspectorBindAction);
            }
        }

        /*        @Override
        void apply(DescribedTemplate t) {
            this.t = t.getTemplate();
            t.add( this );
        }
         */
        @Override
        public String getCommand(Template t) throws Exception
        {
            Template.Parameter[] params = new Template.Parameter[2 + 2*attachments.length];
            params[0] = new Template.ResourceParameter(inspector);
            params[1] = body;
            int i = 2;
            for (Artifact a : attachments)
            {
                params[i++] = new StringParameter(a.getName()); // the artifact name, intended to be the artifact destination
                params[i++] = a.getContent(); // the artifact hash
            }

            StringBuilder sb = new StringBuilder();
            sb.append(getSetID());
            sb.append(" inspect");
            for (Parameter p : params)
            {
                String v = p.getValue(t);
                if (v.length() > 0)
                {
                    sb.append(" ");
                    sb.append(v);
                }
            }

            return sb.toString();
        }

        @Override
        public String getDescription() throws Exception
        {
            StringBuilder sb = new StringBuilder();
            if (attachments.length > 0)
            {
                sb.append("Create an archive of the following files called <tt>attachments.tar.gzip</tt>:<ul>");
                for (Artifact a : attachments)
                {
                    sb.append("<li><tt>");
                    sb.append(a.getName());
                    sb.append("</tt> from ");
                    sb.append(a.getModule().getName());
                    sb.append(":");
                    sb.append(a.getModule().getVersion());
                    sb.append("</li>");
                }

                sb.append("</ul>\n");

                sb.append("Send <tt>attachments.tar.gzip</tt> to <em>");
                sb.append(inspector.name);
                sb.append("</em> with the following instructions:\n");
            } else
            {
                sb.append("<li>Have <em>");
                sb.append(inspector.name);
                sb.append("</em> follow these directions:");
            }

            sb.append(body.asBytes());

            return sb.toString();
        }

        @Override
        public ArtifactUses getArtifactUses() throws Exception
        {
            return new ArtifactUses("inspection", true, Arrays.asList(attachments).iterator());
        }

        @Override
        public Resource getBoundResource() throws Exception
        {
            return null;
        }

        @Override
        public DescribedTemplate getIncludedTemplate() throws Exception
        {
            return null;
        }

    }

    private static final String codename = "person";

    /**
     * Define a new person for use by a generator and with a given name.
     * @param generator The generator that can use the person.
     * @param name The name of the person for logging and debugging.
     */
    public Person(Generator generator, String name)
    {
        super(generator, name, codename);
    }

    /**
     * Have a person inspect artifacts according to specified directions.
     * @param body Content that contains an HTML snippet that will be included in the instructions
     * to the person performing the inspection.
     * @param attachments Artifacts that will be passed to the inspector.
     * @param A list of actions that should be completed before the inspect is performed.
     * @throws Exception If the inspection request is invalid.
     */
    public TestInstance.Action inspect(Content body, Artifact[] attachments, List<Action> actionDependencies) throws Exception
    {
        if(attachments == null){
            attachments = new Artifact[0];
        }
        InspectAction inspectAction = new InspectAction(this, body, attachments, actionDependencies);
        generator.add(inspectAction);
        return inspectAction;
    }
    
    /**
     * Have a person inspect artifacts according to specified directions.
     * @param body Content that contains an HTML snippet that will be included in the instructions
     * to the person performing the inspection.
     * @param attachments Artifacts that will be passed to the inspector.
     * @throws Exception If the inspection request is invalid.
     */
    public TestInstance.Action inspect(Content body, Artifact[] attachments) throws Exception
    {
        return inspect(body, attachments, null);
    }

    @Override
    public String getDescription()
    {
        return "Person";
    }
}
