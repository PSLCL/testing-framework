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
	private int iBeginSetOffset = -1;
	private int iFinalSetOffset = -1; // always non-negative when iBegin... becomes non-negative; never less than iBegin...
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
	public ConnectHandler(InstancedTemplate iT, List<String> setSteps, int iBeginSetOffset) throws NumberFormatException {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
		this.iT = iT;
		this.setSteps = setSteps;
//		this.mapFuturesToMachineInstance = new HashMap<Future<? extends CableInstance>, MachineInstance>();
		
		for (int i=iBeginSetOffset; i<setSteps.size(); i++) {
			SetStep setStep = new SetStep(setSteps.get(i));
			if (!setStep.getCommand().equals("connect"))
				break;
			if (this.iBeginSetOffset == -1)
				this.iBeginSetOffset = iBeginSetOffset;
			this.iFinalSetOffset = i;
		}
	}


	public List<ConnectInfo> computeConnectRequests() throws Exception {
    	List<ConnectInfo> retList = new ArrayList<>();
        if (this.iBeginSetOffset != -1) {
            for (int i=this.iBeginSetOffset; i<=this.iFinalSetOffset; i++) {
            	String connectStep = setSteps.get(i);
            	SetStep parsedSetStep = new SetStep(connectStep); // setID connect 0-based-machine-ref 0-based-network-reference
                                                                  // 9 connect 0 1
            	log.debug(simpleName + "computeConnectRequests() finds connect in stepSet " + parsedSetStep.getSetID() + ": " + connectStep);
            	ResourceInstance machineInstance = null;
            	ResourceInstance networkInstance = null;
        		try {
					String strMachineReference = parsedSetStep.getParameter(0);
					int machineReference = Integer.valueOf(strMachineReference).intValue();
					machineInstance = iT.getResourceInstance(machineReference);
					if (machineInstance != null) {
	            		// Note: In bind handling (that came before), we haven't had an indication as to what this resourceInstance would be used for, and we haven't been able to know its type (Machine vs. Person vs. Network).
	            		//       Now that we know it is used for the first parameter of connect, check resourceInstance for required type: machine
	            		// riRP: resourceInstanceResourceProvider, which has self-knowledge of resource-provider type
	            		ResourceProvider riRP = machineInstance.getResourceProvider();
	            		String resourceType = ResourceProvider.getTypeName(riRP);
	            		if (resourceType==null || resourceType!=ResourceProvider.MachineName)
	            			throw new Exception("ConnectHandler processing asked to connect a non 'machine' resource");	
					} else {
	            		throw new Exception("ConnectHandler.computeConnectRequests() finds null bound ResourceInstance at reference " + strMachineReference);
					}
					
					String strNetworkReference = parsedSetStep.getParameter(1);
					int networkReference = Integer.valueOf(strNetworkReference).intValue();
					networkInstance = iT.getResourceInstance(networkReference);
					if (networkInstance != null) {
	            		// Note: In bind handling (that came before), we haven't had an indication as to what this resourceInstance would be used for, and we haven't been able to know its type (Machine vs. Person vs. Network).
	            		//       Now that we know it is used for the second parameter of connect, check resourceInstance for required type: network
	            		// riRP: resourceInstanceResourceProvider, which has self-knowledge of resource-provider type
	            		ResourceProvider riRP = networkInstance.getResourceProvider();
	            		String resourceType = ResourceProvider.getTypeName(riRP);
	            		if (resourceType==null || resourceType!=ResourceProvider.NetworkName)
	            			throw new Exception("ConnectHandler processing asked to connect a machine to a non 'network' resource");	
					} else {
	            		throw new Exception("ConnectHandler.computeConnectRequests() finds null bound ResourceInstance at reference " + strNetworkReference);
					}
					retList.add(new ConnectInfo(machineInstance, networkInstance));
				} catch (Exception e) {
                    log.debug(simpleName + " connect step does not specify machine reference or network reference");
					throw e;
				}
            }
		}
		return retList;		
	}	
	
	void initiateConnect(List<ConnectInfo> connectInfos) throws Exception {
		
        // start multiple asynch connects
		for (ConnectInfo connectInfo : connectInfos) {
			ResourceInstance machineInstance = connectInfo.getMachineInstance();
			// We know that machineInstance is a MachineInstance, because a connect step must never direct its connecting resource to be anything except a MachineInstance.
			//     Still, check that condition to avoid problems that arise when template steps are improper.
			Class cmi = machineInstance.getClass();
			Class cMI = MachineInstance.class;
			boolean b = cmi.isAssignableFrom(cMI);
			
			
			if (!machineInstance.getClass().isAssignableFrom(MachineInstance.class))
				throw new Exception("Specified connecting resource is not a MachineInstance");
			MachineInstance mi = MachineInstance.class.cast(machineInstance);
			
			ResourceInstance networkInstance = connectInfo.getNetworkInstance();
			// We know that networkInstance is a NetworkInstance, because a connect step must never connect to anything except a NetworkInstance.
			//     Still, check that condition to avoid problems that arise when template steps are improper. 
			if (!networkInstance.getClass().isAssignableFrom(NetworkInstance.class))
				throw new Exception("Specified connection is not a NetworkInstance");
			NetworkInstance ni = NetworkInstance.class.cast(networkInstance);
			Future<CableInstance> future = mi.connect(ni);
//			this.mapFuturesToMachineInstance.put(future, mi); // to establish mi for future use
//			this.mapFuturesToNetworkInstance.put(future, ni); // to establish ni for future use
			futuresOfConnects.add(future);
		}
		// Each list element of futuresOfConnects:
		//     can be a null (connect failed while in the act of creating a Future), or
		//     can be a Future<CableInstance>, for which future.get():
		//        returns a CableInstance on connect success, or
		//        throws an exception on connect failure
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
		Exception exception = null;
		List<CableInstance> retList = new ArrayList<>();
    	ConnectInfo.markAllConnectedSuccess(true); // this required setup allow negation by any pass through the loop 
        for (Future<? extends CableInstance> future : this.futuresOfConnects) {
        	if (future != null) { // null: connect failed early so move along; do not add to retList
                try {
					CableInstance cableInstance = future.get(); // blocks until asynch answer comes, or exception, or timeout
					retList.add(cableInstance);
				} catch (InterruptedException ee) {
					exception = ee;
                    Throwable t = ee.getCause();
                    String msg = ee.getLocalizedMessage();
                    if(t != null)
                        msg = t.getLocalizedMessage();
                	log.warn(simpleName + "waitComplete(), connect failed: " + msg, ee);
                    ConnectInfo.markAllConnectedSuccess(false);
				} catch (ExecutionException e) {
					exception = e;
                	log.warn(simpleName + "waitComplete() Executor pool shutdown"); // TODO: need new msg
                    ConnectInfo.markAllConnectedSuccess(false);
				}
        	} else {
        		ConnectInfo.markAllConnectedSuccess(false);
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