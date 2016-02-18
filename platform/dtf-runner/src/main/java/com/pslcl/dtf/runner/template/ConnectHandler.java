package com.pslcl.dtf.runner.template;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.instance.CableInstance;
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.NetworkInstance;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;

public class ConnectHandler {
	private InstancedTemplate iT;
	private List<String> setSteps;
    private boolean done;
    private StepSetOffsets stepSetOffsets; 
	private List<ConnectInfo> connectInfos;
    private List<Future<? extends CableInstance>> futuresOfConnects;
    private final Logger log;
    private final String simpleName;
//    private Map<Future<? extends CableInstance>, MachineInstance> mapFuturesToMachineInstance;
//    private Map<Future<? extends CableInstance>, NetworkInstance> mapFuturesToNetworkInstance;
    
	/**
	 * Constructor: Identify consecutive connect steps in a set of steps
	 * @param iT
	 * @param setSteps
	 */
	public ConnectHandler(InstancedTemplate iT, List<String> setSteps, int initialSetStepCount) throws NumberFormatException {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
		this.iT = iT;
		this.setSteps = setSteps;
		this.connectInfos = null;
		this.futuresOfConnects = new ArrayList<>();
        this.done = false;
        this.stepSetOffsets = new StepSetOffsets("connect", setSteps, initialSetStepCount);
//		this.mapFuturesToMachineInstance = new HashMap<Future<? extends CableInstance>, MachineInstance>();
	}
	
    /**
     * 
     * @return
     */
    boolean isDone() {
        return done;
    }
	
    /**
     * 
     * @return
     * @throws Exception
     */
    int getConnectRequestCount() throws Exception {
        if (this.connectInfos != null) {
            int retCount = this.connectInfos.size();
            if (retCount > 0)
                return retCount;
        }
        throw new Exception("ConnectHandler unexpectedly finds no inpect requests");
    }	

	public int computeConnectRequests() throws Exception {
		this.connectInfos = new ArrayList<>();
        int beginSetOffset = this.stepSetOffsets.getBeginSetOffset();
        if (beginSetOffset >= 0) {
            for (int i=beginSetOffset; i<=this.stepSetOffsets.getFinalSetOffset(); i++) {
            	String connectStep = setSteps.get(i);
            	SetStep parsedSetStep = new SetStep(connectStep); // setID connect 0-based-machine-ref 0-based-network-reference
                                                                  // 9 connect 0 1
            	log.debug(simpleName + "computeConnectRequests() finds connect in stepSet " + parsedSetStep.getSetID() + ": " + connectStep);
            	ResourceInstance machineInstance = null;
            	ResourceInstance networkInstance = null;
        		try {
					String strMachineReference = parsedSetStep.getParameter(0);
					machineInstance = iT.getResourceInstance(strMachineReference);
					if (machineInstance != null) {
	            		// Note: In bind handling (that came before), we haven't had an indication as to what this resourceInstance would be used for, and we haven't been able to know its type (Machine vs. Person vs. Network).
	            		//       Now that we know it is used for the first parameter of connect, check resourceInstance for required type: machine
	            		// riRP: resourceInstanceResourceProvider, which has self-knowledge of resource-provider type
	            		ResourceProvider riRP = machineInstance.getResourceProvider();
	            		String resourceType = ResourceProvider.getTypeName(riRP);
	            		if (resourceType==null || resourceType!=ResourceProvider.MachineName)
	            			throw new Exception("ConnectHandler processing asked to connect a non 'machine' resource");	
					} else {
	            		throw new Exception("ConnectHandler.computeConnectRequests() finds null bound MachineInstance at reference " + strMachineReference);
					}
					
					String strNetworkReference = parsedSetStep.getParameter(1);
					networkInstance = iT.getResourceInstance(strNetworkReference);					
					if (networkInstance != null) {
	            		// Note: In bind handling (that came before), we haven't had an indication as to what this resourceInstance would be used for, and we haven't been able to know its type (Machine vs. Person vs. Network).
	            		//       Now that we know it is used for the second parameter of connect, check resourceInstance for required type: network
	            		// riRP: resourceInstanceResourceProvider, which has self-knowledge of resource-provider type
	            		ResourceProvider riRP = networkInstance.getResourceProvider();
	            		String resourceType = ResourceProvider.getTypeName(riRP);
	            		if (resourceType==null || resourceType!=ResourceProvider.NetworkName)
	            			throw new Exception("ConnectHandler processing asked to connect a machine to a non 'network' resource");	
					} else {
	            		throw new Exception("ConnectHandler.computeConnectRequests() finds null bound NetworkInstance at reference " + strNetworkReference);
					}
					this.connectInfos.add(new ConnectInfo(machineInstance, networkInstance));
				} catch (Exception e) {
                    log.debug(simpleName + " connect step improperly specifies machine reference or network reference");
					throw e;
				}
            }
		}
		return this.connectInfos.size();		
	}	
	
    /**
     * Proceed to apply this.configureInfos to bound machines, then return. Set done when configures complete or error out.
     *
     * @throws Exception
     */
	List<CableInstance> proceed() throws Exception {
        if (this.connectInfos==null || this.connectInfos.isEmpty()) {
        	this.done = true;
            throw new Exception("ConnectHandler processing has no connectInfo");
        }
        
        List<CableInstance> retList = new ArrayList<CableInstance>();
        try {
            while (!done) {
            	if (this.futuresOfConnects.isEmpty()) {
	    			// The pattern is that this first work, accomplished at the first .proceed() call, must not block. We return before performing any blocking work, knowing that .proceed() will be called again.
					//     initiate multiple asynch connects, this must add to this.futuresOfConnects: it will because this.connectInfos is NOT empty
            		for (ConnectInfo connectInfo : this.connectInfos) {
            			ResourceInstance machineInstance = connectInfo.getMachineInstance();
            			// We know that machineInstance is a MachineInstance, because a connect step must never direct its connecting resource to be anything except a MachineInstance.
            			//     Still, check that condition to avoid problems that arise when template steps are improper.
            			if (!MachineInstance.class.                                                                        // interface MachineInstance
            				                       isAssignableFrom(machineInstance.getClass())) // class AWSMachineInstance implements MachineInstance
            				throw new Exception("Specified connecting resource is not a MachineInstance");
            			MachineInstance mi = MachineInstance.class.cast(machineInstance);
            			
            			ResourceInstance networkInstance = connectInfo.getNetworkInstance();
            			// We know that networkInstance is a NetworkInstance, because a connect step must never connect to anything except a NetworkInstance.
            			//     Still, check that condition to avoid problems that arise when template steps are improper. 
            			if (!NetworkInstance.class.isAssignableFrom(networkInstance.getClass()))
            				throw new Exception("Specified connection is not a NetworkInstance");
            			NetworkInstance ni = NetworkInstance.class.cast(networkInstance);
            			
            			Future<CableInstance> future = mi.connect(ni);
//            			this.mapFuturesToMachineInstance.put(future, mi); // to establish mi for future use
//            			this.mapFuturesToNetworkInstance.put(future, ni); // to establish ni for future use
            			futuresOfConnects.add(future);
            		}
        			return retList;	// Fulfill the pattern that this first work, accomplished at the first .proceed() call, returns before performing any work that blocks. 
            	} else {
            		// Each list element of futuresOfConnects:
            		//     can be a null (connect failed while in the act of creating a Future), or
            		//     can be a Future<CableInstance>, for which future.get():
            		//        returns a CableInstance on connect success, or
            		//        throws an exception on connect failure

                    // complete work by blocking until all the futures complete
			        retList = this.waitComplete();
			        this.done = true;
            	}
			} // end while(!done)
        } catch (Exception e) {
			this.done = true;
			throw e;
        }
        return retList;
    }
	
	/**
	 * Wait for all connects to complete, for the single setID that is being processed.
	 * Mark connect errors in the returned CableInstance list.
	 * 
	 * @note thread blocks
 	 * @note No checked exceptions are thrown. The caller must catch Exception to handle unchecked exceptions
 	 * 
 	 * @throws Exception
	 */
	public List<CableInstance> waitComplete() throws Exception {
		// At this moment, this.futuresOfConnects is filled. It's Future's each give us a CableInstance. Our API is with each of these extracted CableInstance's.
		//     Although this code receives Futures, it does not cancel them or test their characteristics (.isDone(), .isCanceled()).

        // Note: As an initial working step, this block is coded to a safe algorithm:
        //       We expect full connect success, and otherwise we back out and disconnect whatever other connections had been put in place, along the way.
		
		// Gather all results from futuresOfConnects, a list of Futures. This thread yields, in waiting for each of the multiple asynch connect calls to complete.
		List<CableInstance> retList = new ArrayList<>();
    	ConnectInfo.markAllConnectedSuccess(true); // this required setup allow negation by any pass through the loop 
        for (Future<? extends CableInstance> future : this.futuresOfConnects) {
        	if (future != null) { // null: connect failed early so move along; do not add to retList
                try {
					CableInstance cableInstance = future.get(); // blocks until asynch answer comes, or exception, or timeout
					retList.add(cableInstance);
				} catch (InterruptedException | ExecutionException ioreE) {
                    Throwable t = ioreE.getCause();
                    String msg = ioreE.getLocalizedMessage();
                    if(t != null)
                        msg = t.getLocalizedMessage();
                	log.warn(simpleName + "waitComplete(), connect failed: " + msg, ioreE);
                    ConnectInfo.markAllConnectedSuccess(false);
				}
        	} else {
        		ConnectInfo.markAllConnectedSuccess(false);
	            log.warn(simpleName + "waitComplete(), connect errored out with a failed future");
        		throw new Exception("Future.get() failed");
        	}
        }
        
//      boolean allConnected = (retList.size() == this.futuresOfConnects.size()); // this catches the case of future==null
//      if (!allConnected) {
//      	// disconnect all machines that did connect
//      	for (CableInstance ci: retList)
//      		ci.getMachineInstance().disconnect(ci.getNetworkInstance());
//      	// we cleaned up our part; an arrangement with the caller could perhaps recognize that the test has failed, and therefore cleanup all machines and networks
//      	throw new Exception(exception);
//      }
        
        return retList;
	}
	
}