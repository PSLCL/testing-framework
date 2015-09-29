package com.pslcl.qa.runner.resource;

/**
 * Thrown when incompatible resources are asked to interact with each other. For example, A
 * AWS Machine implementation is asked to connect to some non-AWS network for which it has
 * no knowledge.
 */
public class IncompatibleResourceException extends Exception {
	private static final long serialVersionUID = 6407139875273823499L;

}
