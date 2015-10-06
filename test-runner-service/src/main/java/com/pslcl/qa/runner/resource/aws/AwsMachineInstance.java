package com.pslcl.qa.runner.resource.aws;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import com.pslcl.qa.runner.resource.CableInstance;
import com.pslcl.qa.runner.resource.IncompatibleResourceException;
import com.pslcl.qa.runner.resource.MachineInstance;
import com.pslcl.qa.runner.resource.NetworkInstance;
import com.pslcl.qa.runner.resource.ReservedResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceProvider;
import com.pslcl.qa.runner.resource.StartProgram;

public class AwsMachineInstance implements MachineInstance
{
    private String name;
    private Map<String, String> attributes;
    private String description;
    private ResourceProvider resourceProvider;
    private int timeoutSeconds;
    private int reference;

    /**
     * constructor for the use case where resource was previously reserved
     * 
     * @param resource
     */
    public AwsMachineInstance(ReservedResourceWithAttributes reservedResourceWithAttributes)
    {
        name = reservedResourceWithAttributes.getName();
        attributes = reservedResourceWithAttributes.getAttributes();
        // description = ;
        resourceProvider = reservedResourceWithAttributes.getResourceProvider();
        reference = reservedResourceWithAttributes.getReference();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Map<String, String> getAttributes()
    {
        return new HashMap<String, String>(attributes);
    }

    @Override
    public ResourceProvider getResourceProvider()
    {
        return resourceProvider;
    }

    @Override
    public int getReference()
    {
        return reference;
    }

    @Override
    public Future<CableInstance> connect(NetworkInstance network) throws IncompatibleResourceException
    {
        return null;
    }

    @Override
    public Future<Integer> run(String command)
    {
        return null;
    }

    @Override
    public Future<Integer> configure(String command)
    {
        return null;
    }

    @Override
    public Future<StartProgram> start(String command)
    {
        return null;
    }

    @Override
    public Future<Void> deploy(String filename, String artifactHash)
    {
        return null;
    }

    @Override
    public Future<Void> delete(String filename)
    {
        return null;
    }

    @Override
    public Future<Void> disconnect(NetworkInstance network)
    {
        return null;
    }
}
