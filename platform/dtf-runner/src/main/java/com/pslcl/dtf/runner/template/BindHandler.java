package com.pslcl.dtf.runner.template;

public class BindHandler {
	private InstancedTemplate iT;
	private StepsParser stepsParser;
	
	
	public BindHandler(InstancedTemplate iT, StepsParser stepsParser, int stepOffset) {
		this.stepsParser = stepsParser;
	}
	
	void process() {

		// should use stepsParser.computeResourceDescription()?
		
     
      // each returned list entry is self-referenced by steps line number, from 0...n
//      List<ResourceDescription> reserveResourceRequests = stepsParser.computeResourceDescription(); // each element of returned list has its stepReference stored
//      int stepsReference = reserveResourceRequests.size();
//      int originalReserveResourceRequestsSize = stepsReference;
//      
//      // reserve the resource specified by each bind step, with 360 second timeout for each reservation
//      ResourceQueryResult rqr = resourceProviders.reserveIfAvailable(reserveResourceRequests, 360);
//      if (rqr != null) {
//          // analyze the success/failure of each reserved resource, one resource for each bind step
//          List<ResourceDescription> invalidResources = rqr.getInvalidResources(); // list is not in order
//          if (invalidResources!=null && !invalidResources.isEmpty()) {
//              System.out.println("TemplateProvider.getInstancedTemplate() finds " + invalidResources.size() + " reports of invalid resource reserve requests");
//          }
//
//          List<ResourceDescription> unavailableResources = rqr.getUnavailableResources(); // list is not in order
//          if (unavailableResources!=null && !unavailableResources.isEmpty()) {
//              System.out.println("TemplateProvider.getInstancedTemplate() finds " + unavailableResources.size() + " reports of unavailable resources for the given reserve requests");
//          }
//          
//          List<ReservedResource> reservedResources = rqr.getReservedResources(); // list is not in order, but each element of returned list has its stepReference stored
//          if (reservedResources!=null) {
//              System.out.println("TemplateProvider.getInstancedTemplate() finds " + reservedResources.size() + " successful ReservedResource requests" + (reservedResources.size() <= 0 ? "" : "; they are now reserved"));
//          }
//          
//          // Note: The number of reservedResources (i.e. reservedResources.size()) is our primary interest. For any one resource provider, coding is intended that any reservedResource has no entries in the other three lists.
//          //       However, rqr may be filled with entries from multiple resource providers, each answering their status of reserved, unavailable, or invalid.
//          //       So the ultimate rule is this: an entry in the reservedResource list means that the resource is reserved, independent of what the other ResourceProviders may have placed in the alternate lists.
//          
//          // Note: As an initial working step, this block is coded to expect full reserved success and full bind success, and otherwise to release whatever reservations and binds had succeeded along the way
//          if (reservedResources.size() == originalReserveResourceRequestsSize) {
//              // bind all resources of reservedResources, and receive a ResourceInstance for each one
//              List<Future<? extends ResourceInstance>> resourceInstances;
//              // Start multiple asynch binds. Each bind is performed by one specific resource provider, the same one that reserved the resource in the first place. 
//				resourceInstances = resourceProviders.bind(reservedResources);
//              // resourceInstances is a Future list that is returned quickly; i.e. without waiting for all the asynch binds to complete
//				//     each element of the Future list:
//				//         can be a null (bind failed while in the act of creating a Future), or
//				//         can be a Future, for which future.get():
//				//             returns a ResourceInstance on bind success
//				//             throws an exception on bind failure
//
//				// Note: This code receives Futures for temporary use, but does cancel them or test their characteristics (.isDone(), .isCanceled()).
//				//       Our API is with each ResourceInstance derived from the Futures.
//				//           For any actual ResourceInstance ri, ri.getResourceProvider().release(ri, isReusable) announces that this template no longer needs the bound resource instance.  
//				
//				// Gather all the actual bound resource instances from resourceInstances, a list of Futures. This thread yields, in waiting for each of the multiple asynch bind calls to complete.
//				//    Note: As an initial working step, this section is written to gather all bind results, expecting success for every entry in reservedResources. Any failed bind will engage a recovery response, at the end.
//              List<ResourceInstance> listRI = new ArrayList<>();
//              for (Future<? extends ResourceInstance> future : resourceInstances) {
//              	if (future != null) { // null: bind failed early so move along; do not add to listRI
//                      try {
//							ResourceInstance resourceInstance = future.get(); // blocks until asynch answer comes, or exception, or timeout
//							listRI.add(resourceInstance);
//						} catch (InterruptedException ee) {
//                          Throwable t = ee.getCause();
//                          String msg = ee.getLocalizedMessage();
//                          if(t != null)
//                              msg = t.getLocalizedMessage();
//                          LoggerFactory.getLogger(getClass()).info(ResourceInstance.class.getSimpleName() + " bind failed: " + msg, ee);
//						} catch (ExecutionException e) {
//                          LoggerFactory.getLogger(getClass()).info("Executor pool shutdown");
//						}
//              	}
//              }
//              // We discard our information for each future (i.e. Future<? extends ResourceInstance>). Our API is with each ResourceInstance in listRI.
//              
//              boolean allBound = (listRI.size() == originalReserveResourceRequestsSize);
//              if (allBound) {
//              	processPostBindSteps(iT, stepsParser, stepsReference);
//              	// TODO: use all these bound resources
//              } else {
//              	// release all resources that did bind
//              	for (ResourceInstance ri: listRI) {
//              		ResourceProvider rp = ri.getResourceProvider();
//              		rp.release(ri, false);
//              	}
//              }                    
//          } else {
//          	// release all the reserved resources
//          	for (ReservedResource rr : reservedResources) {
//          		ResourceProvider rp = rr.getResourceProvider();
//          		rp.releaseReservedResource(rr);
//          	}
//          }
//      } else {
//          System.out.println("TemplateProvider.getInstancedTemplate() finds null ResourceQueryRequest");
//      }
//		
    }

}
