package com.pslcl.dtf.runner.template;

import java.util.List;

import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;

/**
 * 
 *
 */
public class ProgramInfo {

	private ResourceInstance machineInstance;
	private String strProgramName;
	private List<String> parameters;

	/**
	 * 
	 * @param machineInstance
	 * @param strProgramName
	 * @param parameters
	 */
	public ProgramInfo(ResourceInstance machineInstance, String strProgramName, List<String> parameters) {
		this.machineInstance = machineInstance;
		this.strProgramName = strProgramName;
		this.parameters = parameters;
	}
	
	/**
	 * 
	 * @return
	 */
	ResourceInstance getResourceInstance() {
		return machineInstance;
	}
	
	/**
	 * 
	 * @return
	 */
	String getProgramName() {
		return strProgramName;
	}
	
	/**
	 * 
	 * @return
	 */
	List<String> getParameters() {
		return parameters;
	}
}