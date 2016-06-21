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
package com.pslcl.dtf.core.runner.resource.instance;

import java.util.concurrent.Future;

/**
 * Represents a Machine Resource instance.
 */
public interface MachineInstance extends ResourceInstance
{
    /**
     * Place a file on a machine.
     * <p>An example of use would be 
     * <code>partialDestPath = "lib/dof-oal.jar", url = "http://someqaportalhost/content/somehash"</code>
     * <p>A destination sandbox url is a configurable value.  It's key and default values are declared in
     * ResourceNames.  If the sandbox url is /opt/dtf/sandbox, then the whole path would be 
     * /opt/dtf/sandbox/lib/dof-oal.jar 
     *  
     * @param partialDestPath
     *            The partial destination path to be copied to the destination sandbox.
     * @param url
     *            The URL of the artifact.
     * 
     * @return A Future which returns once the deploy is complete. The Future will throw an exception if the deploy fails.
     * @throws Exception  if deploy fails
     */
    Future<Void> deploy(String partialDestPath, String url) throws Exception;

    /**
     * Delete a file from a machine.
     * <p>An example of use would be 
     * <code>partialDestPath = "lib/dof-oal.jar"</code>
     * <p>A destination sandbox url is a configurable value.  It's key and default values are declared in
     * ResourceNames.  If the sandbox url is /opt/dtf/sandbox, then the file to be deleted would be 
     * /opt/dtf/sandbox/lib/dof-oal.jar
     * <p>if partialDestPath is null then the sandbox is deleted.
     *  
     * @param partialDestPath if non-null the partial destination file to be deleted from the destination sandbox
     * otherwise the sandbox is deleted.
     * @throws Exception  if delete fails
     * @return A Future which returns once the delete is complete. The Future will throw an exception if the delete fails. 
     */
    Future<Void> delete(String partialDestPath) throws Exception;

    /**
     * Connect a machine to a network.
     *
     * @param network
     *            The NetworkInstance to which the machine should connect.
     *            
     * @return A Future which returns once the machine is connected to the network.
     */
    Future<CableInstance> connect(NetworkInstance network);// throws IncompatibleResourceException;

    /**
     * Disconnect a machine from a network. 
     *
     * @param network The NetworkInstance to which the machine should be disconnected.
     * If null disconnect any/all.
     * @return a Future which returns once the machine is disconnected from the network.
     */
    Future<Void> disconnect(NetworkInstance network);

    /**
     * Run an executable command on a machine and return a Future with the {@link RunnableProgram} set command once execution has
     * completed. Within the return object is a program run result; non-zero indicates that the test has failed.
     * 
     * This method is the equivalent of a template {@code run} command and cannot modify the machine.
     * 
     * @param command
     *            An executable command, including arguments, to be run on the machine.
     *
     * @throws Exception  if run fails
     * @return A {@link RunnableProgram} containing the information about the command once execution has completed.
     */
    Future<RunnableProgram> run(String command) throws Exception;

    /**
     * Run an executable command on a machine that modifies the machine in some way. Returns a Future with the result of
     * the command set once execution has completed.
     * 
     * This method is the equivalent of a template {@code configure} command and may modify the machine.
     * 
     * @param command
     *            An executable command, including arguments, to be run on the machine.
     *
     * @throws Exception  if configure fails
     * @return A {@link RunnableProgram} containing the information about the command once execution has completed.
     */
    Future<RunnableProgram> configure(String command) throws Exception;

    /**
     * Start an executable command on a machine and return a Future with the {@link RunnableProgram} set once the program
     * has successfully started.
     * 
     * This method is the equivalent of a template {@code start} command and cannot modify the machine.
     * 
     * @param command
     *            An executable command, including arguments, to be run on the machine.
     *
     * @throws Exception  if start fails
     * @return A {@link RunnableProgram} containing the information about the command once execution has completed.
     */
    Future<RunnableProgram> start(String command) throws Exception;

}