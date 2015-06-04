/**
 * 
 */
package com.pslcl.qa.platform;

/**
 * Exception indicating the data is not known. This is assumed to be a permanent failure.
 */
public class ResourceNotFoundException extends Exception {
	private static final long serialVersionUID = 0x238322fb087bc9eaL;

	/**
	 * DataUnknownException constructor.
	 */
	public ResourceNotFoundException() {
	}

	/**
	 * DataUnknownException constructor.
	 * 
	 * @param message The detail message
	 */
	public ResourceNotFoundException(String message) {
		super(message);
	}

	/**
	 * DataUnknownException constructor.
	 * 
	 * @param cause The cause of of this exception
	 */
	public ResourceNotFoundException(Throwable cause) {
		super(cause);
	}

	/**
	 * DataUnknownException constructor.
	 * 
	 * @param message The detail message
	 * @param cause The cause of of this exception
	 */
	public ResourceNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}