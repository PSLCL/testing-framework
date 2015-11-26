package com.pslcl.dtf.runner.template;

/**
 * Represent a template step of the type used in step sets
 * 
 * @note A set step has this wire format: setID command spaceSeparatedParam1 spaceSeparatedParam2 ...
 */
public class SetStep {
	private String[] setStep; // [setID, command, spaceSeparatedParam1, spaceSeparatedParam2, ...]
	
	/**
	 * @param step Must not be null
	 */
	public SetStep(String step) {
		setStep = step.split(" ");
	}
	
	/**
	 * 
	 * @return
	 * @throws NumberFormatException
	 */
	public int getSetID() throws NumberFormatException {
		return Integer.valueOf(setStep[0]).intValue(); // [0] always exists, NumberFormatException thrown if it is an empty string or for other numeric problems
	}
	
	/**
	 * 
	 * @return
	 */
	public String getCommand() throws ArrayIndexOutOfBoundsException {
		return setStep[1];
	}
	
	/**
	 * Get count of space-separated Strings that follow this step's setID and command  
	 * @return
	 * @throws Exception
	 */
	public int getParameterCount() throws Exception {
		int retCount = setStep.length-2;
		if (retCount >= 0)
			return retCount;
		throw new Exception("This set step does not have the minimum required setID and command");
	}
	
	/**
	 * 
	 * @param parameterIndex 0-based index to requested space-separated Strings that follow this step's setID and command
	 * @return
	 * @throws Exception
	 */
	public String getParameter(int parameterIndex) throws IndexOutOfBoundsException {
		return setStep[2+parameterIndex];
	}	
	
}