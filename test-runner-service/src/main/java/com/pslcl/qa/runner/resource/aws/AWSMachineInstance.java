package com.pslcl.qa.runner.resource.aws;

import java.util.Map;
import java.util.concurrent.Future;

import com.pslcl.qa.runner.resource.CableInstance;
import com.pslcl.qa.runner.resource.IncompatibleResourceException;
import com.pslcl.qa.runner.resource.MachineInstance;
import com.pslcl.qa.runner.resource.NetworkInstance;
import com.pslcl.qa.runner.resource.ReservedResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceProvider;
import com.pslcl.qa.runner.resource.StartProgram;

public class AWSMachineInstance implements MachineInstance {

	private String name;
	private Map<String, String> attributes;
	private String description;
	private ResourceProvider rp;
	private int timeoutSeconds;
	private int reference;

	/**
	 * constructor for the use case where resource was previously reserved
	 * 
	 * @param resource
	 */
	public AWSMachineInstance(ReservedResourceWithAttributes rrwa) {
		name = rrwa.getName();
		attributes = rrwa.getAttributes();
		// description = ;
		rp = rrwa.getResourceProvider();
		reference = rrwa.getReference();
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourceProvider getResourceProvider() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getReference() {
		return this.reference;
	}

	@Override
	public Future<CableInstance> connect(NetworkInstance network) throws IncompatibleResourceException {
		return null;
		//TODO
	}

	@Override
	public Future<Integer> run(String command) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<Integer> configure(String command) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<StartProgram> start(String command) {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public Future<Void> deploy(String filename, String artifactHash)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<Void> delete(String filename)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<Void> disconnect(NetworkInstance network)
    {
        // TODO Auto-generated method stub
        return null;
    }

}
