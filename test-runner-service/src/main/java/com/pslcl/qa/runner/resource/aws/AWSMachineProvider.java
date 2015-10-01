package com.pslcl.qa.runner.resource.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.pslcl.qa.runner.resource.BindResourceFailedException;
import com.pslcl.qa.runner.resource.MachineInstance;
import com.pslcl.qa.runner.resource.MachineProvider;
import com.pslcl.qa.runner.resource.ReservedResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceInstance;
import com.pslcl.qa.runner.resource.ResourceNotFoundException;
import com.pslcl.qa.runner.resource.ResourceQueryResult;
import com.pslcl.qa.runner.resource.ResourceStatusCallback;
import com.pslcl.qa.runner.resource.ResourceWithAttributes;

/**
 * Reserve, bind, control and release instances of AWS machines.
 */
public class AWSMachineProvider implements MachineProvider
{
    private final AmazonEC2Client ec2Client;
    private final AWSResourceProviderProperties properties;
    private final List<AWSMachineInstance> instances = new ArrayList<AWSMachineInstance>();
    private final ExecutorService executor;

    public AWSMachineProvider(ExecutorService executor, AWSResourceProviderProperties properties)
    {
        this.executor = executor;
        this.properties = properties;
        DefaultAWSCredentialsProviderChain providerChain = new DefaultAWSCredentialsProviderChain();
        ec2Client = new AmazonEC2Client(providerChain);
    }

    // implement MachineProvider interface

    @Override
    public Future<MachineInstance> bind(ReservedResourceWithAttributes resource, ResourceStatusCallback statusCallback) throws BindResourceFailedException
    {
        return executor.submit(new AWSMachineInstanceFuture(resource));
    }

    @Override
    public List<Future<? extends ResourceInstance>> bind(List<ReservedResourceWithAttributes> resources, ResourceStatusCallback statusCallback)
    {
        // TODO Auto-generated method stub
        return null;
    }

    // implement ResourceProvider interface

    @Override
    public void releaseReservedResource(ReservedResourceWithAttributes resource)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isAvailable(ResourceWithAttributes resource) throws ResourceNotFoundException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ResourceQueryResult queryResourceAvailability(List<ResourceWithAttributes> resources)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResourceQueryResult reserveIfAvailable(List<ResourceWithAttributes> resources, int timeoutSeconds)
    {
        // temporary, to allow progress: add one simulated reserved reservation, place it in retRqr, and return that
        ResourceQueryResult retRqr = new ResourceQueryResult(new ArrayList<ReservedResourceWithAttributes>(), new ArrayList<ResourceWithAttributes>(), new ArrayList<ResourceWithAttributes>(), new ArrayList<ResourceWithAttributes>());
        for (ResourceWithAttributes rwa : resources)
        {
            // TODO: actually reserve whatever is requested in parameter resources

            // temporary, to allow progress: return an artificially reserved resource
            ReservedResourceWithAttributes simulatedReservation = new ReservedResourceWithAttributes(rwa, this, timeoutSeconds);
            retRqr.getReservedResources().add(simulatedReservation);
        }
        return retRqr;
    }

    @Override
    public List<String> getNames()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getAttributes(String name)
    {
        // TODO Auto-generated method stub
        return null;
    }

    // implement ArtifactConsumer interface

    @Override
    public void updateArtifact(String component, String version, String platform, String name, String artifactHash)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeArtifact(String component, String version, String platform, String name)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void invalidateArtifacts(String component, String version)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void release(ResourceInstance resource, boolean isReusable)
    {
        // TODO Auto-generated method stub

    }

}
