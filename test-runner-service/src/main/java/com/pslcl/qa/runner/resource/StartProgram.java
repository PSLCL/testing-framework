package com.pslcl.qa.runner.resource;

public interface StartProgram {

	/**
	 * Stop the program and return the result. The end result of a Start program does not affect the final result of the
	 * test.
	 * 
	 * @return The result of the start program.
	 */
	public int stop();
	
	/**
	 * Determine the running state of the program.
	 * 
	 * @return {@code True} if the program is running. {@code False} otherwise.
	 */
	public boolean isRunning();

}
