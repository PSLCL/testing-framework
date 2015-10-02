package com.pslcl.qa.runner.resource.aws;

import java.util.concurrent.Callable;

import com.pslcl.qa.runner.resource.MachineInstance;
import com.pslcl.qa.runner.resource.ReservedResourceWithAttributes;

public class AwsMachineInstanceFuture implements Callable<MachineInstance>
{
    ReservedResourceWithAttributes resource;

    public AwsMachineInstanceFuture(ReservedResourceWithAttributes resource)
    {
        this.resource = resource;
    }

    @Override
    public MachineInstance call() throws Exception
    {
        // temporary, to allow progress: return AWSMachineInstance that matches "resource"; in other words, pretend
        MachineInstance retMachineInstance = new AwsMachineInstance(ReservedResourceWithAttributes.class.cast(resource));
        return retMachineInstance;
    }
}
