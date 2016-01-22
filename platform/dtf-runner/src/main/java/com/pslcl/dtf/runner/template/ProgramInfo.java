package com.pslcl.dtf.runner.template;

import java.util.List;

import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;

/**
 * 
 *
 */
public class ProgramInfo {

	private ResourceInstance resourceInstance;
	private String strProgramName;
	private List<String> parameters;

	/**
	 * 
	 * @param resourceInstance
	 * @param strProgramName
	 * @param parameters
	 */
	public ProgramInfo(ResourceInstance resourceInstance, String strProgramName, List<String> parameters) {
		this.resourceInstance = resourceInstance;
		this.strProgramName = strProgramName;
		this.parameters = parameters;
	}
	
	// TODO: rename this method
	MachineInstance computeProgramRunInformation() throws Exception {
		// We know resourceInstance is a MachineInstance, because a configure/start/run step always directs its work to MachineInstance.
		//     Still, check that condition to avoid problems that arise when template steps are improper. 
		if (!MachineInstance.class.isAssignableFrom(resourceInstance.getClass())) {
			throw new Exception("Specified program run target is not a MachineInstance");
		}
		MachineInstance mi = MachineInstance.class.cast(resourceInstance);		
		return mi;
	}
	
	String getComputedCommandLine() {
		String retProgramCommandLine = strProgramName;
		for (String param: parameters) {
			retProgramCommandLine += ' ';
			retProgramCommandLine += param;
		}
		return retProgramCommandLine;
	}
	
//	/**
//	 * 
//	 * @return
//	 */
//	ResourceInstance getResourceInstance() {
//		return resourceInstance;
//	}
	
//	/**
//	 * 
//	 * @return
//	 */
//	String getProgramName() {
//		return strProgramName;
//	}
	
//	/**
//	 * 
//	 * @return
//	 */
//	List<String> getParameters() {
//		return parameters;
//	}

}