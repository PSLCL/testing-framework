package com.pslcl.qa.runner.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;

import com.pslcl.qa.runner.config.RunnerServiceConfig;
import com.pslcl.qa.runner.config.util.PropertiesFile;
import com.pslcl.qa.runner.config.util.StrH.StringPair;
import com.pslcl.qa.runner.resource.ReservedResource;
import com.pslcl.qa.runner.resource.ResourceQueryResult;
import com.pslcl.qa.runner.resource.ResourceDescription;
import com.pslcl.qa.runner.resource.exception.ResourceNotReservedException;
import com.pslcl.qa.runner.resource.exception.ResourceNotFoundException;
import com.pslcl.qa.runner.resource.instance.ResourceInstance;
import com.pslcl.qa.runner.resource.instance.ResourceWithAttributesInstance;
import com.pslcl.qa.runner.resource.provider.MachineProvider;
import com.pslcl.qa.runner.resource.provider.NetworkProvider;
import com.pslcl.qa.runner.resource.provider.PersonProvider;
import com.pslcl.qa.runner.resource.provider.ResourceProvider;

/**
 * Contains ResourceProvider instantiated objects and supplies access to them 
 */
public class ResourceProviders implements ResourceProvider {

    public static final String MachineProviderClassKey = "pslcl.qa.runner.template.machine-provider-class"; 
    public static final String PersonProviderClassKey = "pslcl.qa.runner.template.person-provider-class"; 
    public static final String NetworkProviderClassKey = "pslcl.qa.runner.template.network-provider-class";
    
    public static final String MachineProviderClassDefault = "com.pslcl.qa.runner.resource.aws.provider.AwsMachineProvider";
    public static final String PersonProviderClassDefault = "com.pslcl.qa.runner.resource.aws.provider.AwsPersonProvider";
    public static final String NetworkProviderClassDefault = "com.pslcl.qa.runner.resource.aws.provider.AwsNetworkProvider";
    
    private final List<ResourceProvider> resourceProviders;

    /**
     * constructor
     */
    public ResourceProviders() {
        resourceProviders = new ArrayList<>(); // list of class objects that implement the ResourceProvider interface

    }
    
    public List<ResourceProvider> getProviders()
    {
        return new ArrayList<ResourceProvider>(resourceProviders);
    }
    
    
    @Override
    public void init(RunnerServiceConfig config) throws Exception
    {
        config.initsb.level.incrementAndGet();
        config.initsb.ttl(getClass().getSimpleName(), " Initialization");
        config.initsb.level.incrementAndGet();
        
        // Note: Do not include ResourceProviders in this list
        config.initsb.ttl(MachineProvider.class.getSimpleName(), " Initialization");
        config.initsb.level.incrementAndGet();
        configToProviders(config, MachineProviderClassKey, MachineProviderClassDefault);
        config.initsb.level.decrementAndGet();
        
        config.initsb.ttl(PersonProvider.class.getSimpleName(), " Initialization");
        config.initsb.level.incrementAndGet();
        configToProviders(config, PersonProviderClassKey, PersonProviderClassDefault);
        config.initsb.level.decrementAndGet();
        
        config.initsb.ttl(NetworkProvider.class.getSimpleName(), " Initialization");
        config.initsb.level.incrementAndGet();
        configToProviders(config, NetworkProviderClassKey, NetworkProviderClassDefault);
        config.initsb.level.decrementAndGet();
        config.initsb.level.decrementAndGet();
        config.initsb.level.decrementAndGet();
    }
    
    private void configToProviders(RunnerServiceConfig config, String key, String defaultValue) throws Exception
    {
        List<Entry<String, String>> list = PropertiesFile.getPropertiesForBaseKey(key, config.properties);
        int size = list.size();
        if(size == 0)
        {
            // add the default
            StringPair pair = new StringPair(key, defaultValue);
            list.add(pair);
            ++size;
        }
        
        for(int i=0; i < size; i++)
        {
            Entry<String,String> entry = list.get(i);
            config.initsb.ttl(entry.getKey(), " = ", entry.getValue());
            ResourceProvider rp = (ResourceProvider)Class.forName(entry.getValue()).newInstance();
            rp.init(config);
            resourceProviders.add(rp);
        }
    }
    
    @Override
    public void destroy() 
    {
        try
        {
            int size = resourceProviders.size();
            for(int i=0; i < size; i++)
                resourceProviders.get(i).destroy();
            resourceProviders.clear();
        }catch(Exception e)
        {
            LoggerFactory.getLogger(getClass()).error(getClass().getSimpleName() + ".destroy failed", e);
        }
    }
    

    @Override
    public ResourceQueryResult reserveIfAvailable(List<ResourceDescription> reserveResourceRequests, int timeoutSeconds) {
        
        // Identify each ResourceProvider (like AWSMachineProvider) that understands specific requirements of individual elements of param ResourceWithAttributes and can reserve the corresponding resource.
        //    This class has a list of all types of ResourceProvider (like AWSMachineProvider).

        // Current solution- ask each ResourceProvider, in turn. TODO: Perhaps optimize by asking each ResourceProvider directly, taking advantage of knowledge of each resource provider's hash and attributes?

        // start retRqr with empty lists; afterward merge reservation results into retRqr
        ResourceQueryResult retRqr = new ResourceQueryResult(new ArrayList<ReservedResource>(),
                                                             new ArrayList<ResourceDescription>(),
                                                             new ArrayList<ResourceDescription>(),
                                                             new ArrayList<ResourceDescription>());

        // invite every ResourceProvider to reserve each resource in param reserveResourceRequests, as it can and as it will
        for (ResourceProvider rp : resourceProviders) {
            // but 1st, to avoid multiple reservations: for any past success in filling any of the several rrwa S in retRqr, strip param reserveResourceRequests of that entry
            for (ReservedResource rrwa : retRqr.getReservedResources()) {
                for (ResourceDescription rwa : reserveResourceRequests) {
                    ResourceWithAttributesInstance rwai = ResourceWithAttributesInstance.class.cast(rwa);
                    if (rwai.matches(rrwa)) {
                        reserveResourceRequests.remove(rwa);
                        break; // done with this rwa; as we leave the for loop, we also avoid the trouble caused by altering reserveResourceRequests 
                    }
                }                
            }
            if (reserveResourceRequests.isEmpty()) {
                // The act of finding reserveResourceRequests to be empty means all requested resources are reserved.
                //     retRqr reflects that case: it holds all original reservation requests. 
                //     We do not not need to check further- not for this rp and not for any remaining rp that have not been checked yet.
                break;
            }

            // this rp reserves each resource in param resources, as it can and as it will 
            ResourceQueryResult localRqr = rp.reserveIfAvailable(reserveResourceRequests, timeoutSeconds);
            retRqr.merge(localRqr); // merge localRqr into retRqr
        }
        return retRqr;
    }

    @Override
    public Future<? extends ResourceInstance> bind(ReservedResource reservedResourceWithAttributes) throws ResourceNotReservedException {
        // Note: Current implementation assumes only one instance is running of test-runner-service; so a resource reserved by a provider automatically matches this test-runner-service
        System.out.println("ResourceProviders.bind() called with resourceName/resourceAttributes: " + reservedResourceWithAttributes.getName() + " / " + reservedResourceWithAttributes.getAttributes());

        Future<? extends ResourceInstance> retRI = null;
        try {
            // note that this bind call  must fill the return ResourceInstance with not only something like an implementation of MachineInstance or a PersonInstance, but it must fill reference, which comes from param resourceWithAttributes
            retRI = reservedResourceWithAttributes.getResourceProvider().bind(reservedResourceWithAttributes);
        } catch (ResourceNotReservedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return retRI;
    }

    @Override
    public List<Future<? extends ResourceInstance>> bind(List<ReservedResource> resources) throws ResourceNotReservedException {
        // Note: Current implementation assumes only one instance is running of test-runner-service; so a resource reserved by a provider automatically matches this test-runner-service
        List<Future<? extends ResourceInstance>> retRiList = new ArrayList<>();
        for(int i=0; i<resources.size(); i++) {
            try {
                retRiList.add(this.bind(resources.get(i)));
            } catch (ResourceNotReservedException e) {
                retRiList.add(null);
                System.out.println("ResourceProviders.bind(List<> resources) stores null entry");
            }
        }
        return retRiList;
    }  

    @Override
    public void releaseReservedResource(ReservedResource resource) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isAvailable(ResourceDescription resource) throws ResourceNotFoundException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ResourceQueryResult queryResourceAvailability(List<ResourceDescription> resources) {
        // TODO Auto-generated method stub
        return null;
    }

//TODO: AWS implementation does not know what to do with these, chad commented them out of th interface for now    
//    @Override
//    public Map<String, String> getAttributes(String hash) {
//        // ResourceProviders does not supply hashes, attributes or descriptions.
//        return null;
//    }
//
//    @Override
//    public List<String> getNames() {
//        // TODO Auto-generated method stub
//        return null;
//    }

    @Override
    public void release(ResourceInstance resource, boolean isReusable) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getName()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getAttributes()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
}