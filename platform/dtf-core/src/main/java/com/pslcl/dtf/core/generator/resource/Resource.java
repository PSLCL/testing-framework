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

import java.util.Map;
import java.util.UUID;

import com.pslcl.dtf.core.generator.Generator;
import com.pslcl.dtf.core.generator.template.DescribedTemplate;
import com.pslcl.dtf.core.generator.template.Template;
import com.pslcl.dtf.core.generator.template.Template.AttributeParameter;
import com.pslcl.dtf.core.generator.template.TestInstance;

/**
 * This class represents a resource. Resources can represent any shared object, and are identified
 * by a codename (string) and a set of attributes. Resources are bound to become an instance of the resource.
 * At run time the resource may present additional attributes, but will always have at least the
 * attributes specified in the bind request. Thay can also be assigned a human readable name for
 * descriptions.
 */
public abstract class Resource
{
    /**
     * This class represents an action that requests a resource be bound.
     */
    private static class BindAction extends TestInstance.Action
    {
        private Resource r;
        private Attributes a;

        private BindAction(Resource r, Attributes a)
        {
            this.r = r;
            this.a = a;
        }

        @Override
        public String getCommand(Template t) throws Exception
        {
            return String.format("bind %s %s", r.getCodename().toString(), a.toString());
        }

        @Override
        public String getDescription() throws Exception
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Assign a resource <tt>");
            sb.append(r.getCodename().toString());
            sb.append("</tt> (");
            sb.append(r.getDescription());
            sb.append(")");
            if (a != null && a.getAttributes().size() > 0)
            {
                sb.append(" having attributes <tt>");
                sb.append(a);
                sb.append("</tt>");
            }

            sb.append(" and call it <em>");
            sb.append(r.name);
            sb.append("</em>.");
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
            return r;
        }

        @Override
        public DescribedTemplate getIncludedTemplate() throws Exception
        {
            return null;
        }

    }

    public final Generator generator;
    public final String codename;
    public final String name;
    public final UUID instance;
    //TODO: not thread safe
    BindAction bound;
    private Map<String, String> attributeMap;

    /**
     * Construct a resource definition.
     * @param generator
     * @param name
     * @param codename
     */
    Resource(Generator generator, String name, String codename)
    {
        this.instance = UUID.randomUUID();
        this.generator = generator;
        this.name = name;
        this.codename = codename;
    }

    /**
     * Bind a resource with no attributes. Each resource must be bound in each test instance before it may be used.
     * @throws Exception The bind failed.
     */
    public void bind() throws Exception
    {
        bind(new Attributes());
    }

    /**
     * Bind a resource. Each resource must be bound in each test instance before it may be used.
     * @param attributes The attributes that the resource must have to be acceptable.
     * @throws Exception The bind failed.
     */
    public void bind(Attributes attributes) throws Exception
    {
        if (bound != null)
        {
            System.err.println("Cannot bind the same resource twice.");
            return;
        }

        bound = new BindAction(this, attributes);
        generator.add(bound);
        this.attributeMap = attributes.getAttributes();
        if (Generator.trace)
            System.err.println(String.format("Resource %s (%s) bound with attributes '%s'.", name, instance, attributes));
    }

    /**
     * Return whether the resource is bound.
     * @return True if the resource is bound, false otherwise.
     */
    public boolean isBound()
    {
        return bound != null;
    }

    /**
     * Return the code name of the resource, which must be unique and understood by the resource provider.
     * @return The code name of the resource.
     */
    public String getCodename()
    {
        return codename;
    }

    /**
     * Return a short description of the resource to be used in documentation.
     * @return The short name of the resource.
     */
    public abstract String getDescription();

    /**
     * Get a reference to an attribute whose value may be used as a parameter in a program action.
     * 
     * This reference will be resolved by the Generator when the Template is generated. If the value of
     * the attribute is known at that time, then the reference will be replaced by the value. If the value
     * of the attribute will not be known until the test is run, then the reference will be replaced by
     * a value reference in the form of $(attribute <resource-ref> <attribute-name>).
     * 
     * @param attributeName The name of the attribute.
     * @return a String reference to the attribute.
     */
    public String getAttributeReference(String attributeName)
    {
        String uuid = UUID.randomUUID().toString().toUpperCase();
        AttributeParameter attribute;
        if (attributeMap != null && attributeMap.containsKey(attributeName))
        {
            attribute = new AttributeParameter(attributeName, attributeMap.get(attributeName));
        } else
        {
            attribute = new AttributeParameter(this, attributeName);
        }
        generator.addParameterReference(uuid, attribute);
        return uuid;
    }
}
