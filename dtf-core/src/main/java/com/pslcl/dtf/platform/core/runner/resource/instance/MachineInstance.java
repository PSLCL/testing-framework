/*
 * Copyright (c) 2010-2015, Panasonic Corporation.
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.pslcl.dtf.platform.core.runner.resource.instance;

import java.util.concurrent.Future;

import com.pslcl.dtf.platform.core.runner.resource.exception.IncompatibleResourceException;

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
	 * 
	 * @return A Future<Void> which returns once the deploy is complete. The Future will throw an exception if the deploy fails. 
	 */
	Future<Void> deploy(String filename, String artifactHash);
	
	/**
	 * Delete an artifact from a machine.
	 * 
	 * @param filename
	 *            The filename of the artifact to delete.
	 *            
	 * @return A Future<Void> which returns once the delete is complete. The Future will throw an exception if the delete fails. 
	 */
	Future<Void> delete(String filename);

	/**
	 * Connect a machine to a network.
	 *
	 * @param network
	 *            The NetworkInstance to which the machine should connect.
	 *            
	 * @return A Future<CableInstance> which returns once the machine is connected to the network.
	 * 
	 */
	Future<CableInstance> connect(NetworkInstance network) throws IncompatibleResourceException;

	/**
	 * Disconnect a machine from a network. 
	 *
	 * @param network
	 *            The NetworkInstance to which the machine should be disconnected.
	 * 
	 * @return a Future<Void> which returns once the machine is disconnected from the network.
	 */
	Future<Void> disconnect(NetworkInstance network);

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