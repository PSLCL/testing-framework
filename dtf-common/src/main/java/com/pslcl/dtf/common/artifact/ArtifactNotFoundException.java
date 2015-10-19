/**
 * 
 */
package com.pslcl.dtf.common.artifact;

/**
 * Exception indicating the data is not known. This is assumed to be a permanent failure.
 */
public class ArtifactNotFoundException extends Exception {
	private static final long serialVersionUID = 0x1e3d92d8c50e3738L;

	/**
	 * DataUnknownException constructor.
	 */
	public ArtifactNotFoundException() {
	}

	/**
	 * DataUnknownException constructor.
	 * 
	 * @param message The detail message
	 */
	public ArtifactNotFoundException(String message) {
		super(message);
	}

	/**
	 * DataUnknownException constructor.
	 * 
	 * @param cause The cause of of this exception
	 */
	public ArtifactNotFoundException(Throwable cause) {
		super(cause);
	}

	/**
	 * DataUnknownException constructor.
	 * 
	 * @param message The detail message
	 * @param cause The cause of of this exception
	 */
	public ArtifactNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}