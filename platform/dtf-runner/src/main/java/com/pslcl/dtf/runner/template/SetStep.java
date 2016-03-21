package com.pslcl.dtf.runner.template;

/**
 * Represent a template step of the type used in step sets
 * 
 * Note: A set step has this wire format: setID command spaceSeparatedParam1 spaceSeparatedParam2 ...
 */
public class SetStep {
	
	/**
	 * 
	 * @param param
	 * @return
	 */
	static boolean isValueReference(String param) {
		int openingIndex = param.indexOf("$(");
		int endIndex = param.indexOf(")");
		boolean retBoolean = (openingIndex>=0 && endIndex>openingIndex);
		return retBoolean;
	}
	
	private String[] setStep; // [setID, command, spaceSeparatedParam1, spaceSeparatedParam2, ...]
	
	/**
	 * Constructor
	 * 
	 * Note: Param step is composed of space separated elements, including certain elements that are URL encoded (meaning spaces are replaced with alternate characters). We separate elements by spaces, as given to us.    
	 * Note: The end result is that the URL encoded elements preserve their integrity as a single element of this template step, yet they also preserve their included (but encoded) spaces.  
	 * @param step Must not be null
	 */
	public SetStep(String step) {
		this.setStep = step.split(" ");
	}
	
	/**
	 * Return the setID of this SetStep
	 * 
	 * @return The setID
	 * @throws NumberFormatException if this stepStep's setID string does not represent a number
	 */
	public int getSetID() throws NumberFormatException {
		return Integer.valueOf(setStep[0]).intValue(); // [0] always exists, NumberFormatException thrown if it is an empty string or for other numeric problems
	}
	
	/**
	 * 
	 * @return The command string
	 */
	public String getCommand() throws ArrayIndexOutOfBoundsException {
		return setStep[1];
	}
	
	/**
	 * Get count of space-separated Strings that follow this step's setID and command  
	 * @return The count
	 * @throws Exception on invalid data in the SetStep
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
	 * @return The parameter as a String
	 * @throws IndexOutOfBoundsException on invalid parameterIndex
	 */
	public String getParameter(int parameterIndex) throws IndexOutOfBoundsException {
		return setStep[2+parameterIndex];
	}	
	
}