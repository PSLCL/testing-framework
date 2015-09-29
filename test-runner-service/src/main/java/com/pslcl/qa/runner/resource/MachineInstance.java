package com.pslcl.qa.runner.resource;

import java.util.concurrent.Future;

import com.pslcl.qa.runner.ArtifactNotFoundException;

/**
 * Represents a Machine Resource instance.
 */
public interface MachineInstance extends ResourceInstance {

	/**
	 * Place an artifact on a machine.
	 * 
	 * @param filename
	 *            The target filename for the artifact.
	 * @param artifactHash
	 *            The hash of the artifact.
	 */
	void deploy(String filename, String artifactHash) throws ArtifactNotFoundException;

	/**
	 * Connect a machine to a network.
	 *
	 * @param network
	 *            The NetworkInstance to which the machine should connect.
	 * 
	 */
	Future<CableInstance> connect(NetworkInstance network) throws IncompatibleResourceException;

	/**
	 * Run an executable command on a machine and return a Future with the result of the set command once execution has
	 * completed. A non-zero result indicates that the test has failed.
	 * 
	 * This method is the equivalent of a template {@code run} command and cannot modify the machine.
	 * 
	 * @param command
	 *            An executable command, including arguments, to be run on the machine.
	 *
	 * @return A {@link Future<Integer>} containing the result of the command once execution has completed.
	 */
	Future<Integer> run(String command);

	/**
	 * Run an executable command on a machine that modifies the machine in some way. Returns a Future with the result of
	 * the command set once execution has completed.
	 * 
	 * This method is the equivalent of a template {@code configure} command and may modify the machine.
	 * 
	 * @param command
	 *            An executable command, including arguments, to be run on the machine.
	 *
	 * @return A {@link Future<Integer>} containing the result of the command once execution has completed.
	 */
	Future<Integer> configure(String command);

	/**
	 * Start an executable command on a machine and return a Future with the {@link StartProgram} set once the program
	 * has successfully started.
	 * 
	 * This method is the equivalent of a template {@code start} command and cannot modify the machine.
	 * 
	 * @param command
	 *            An executable command, including arguments, to be run on the machine.
	 *
	 * @return A {@link Future<Integer>} containing the result of the command once execution has completed.
	 */
	Future<StartProgram> start(String command);

}