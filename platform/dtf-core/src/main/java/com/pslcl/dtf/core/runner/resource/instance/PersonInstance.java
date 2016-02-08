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

import java.io.InputStream;
import java.util.concurrent.Future;

/**
 * Represents a Person Resource instance.
 */
public interface PersonInstance extends ResourceInstance {

	/**
	 * Ask a person to follow a set of instructions. Include an archive file of relevant artifacts. 
	 * 
	 * This method is equivalent to a template Inspect command.
	 * 
	 * @note The name of the archive file to send to the person is nominally attachments.tar.gz.
	 * @note This call blocks until the instructions and archive file are deemed to have been sent to the person.
	 * 
	 * @param instructions
	 *            An HTML-formatted list of instructions to be sent to the person.
	 * 
	 * @param fileContent
	 *            Byte content of artifacts to be placed in the archive file to include. Null means that no artifacts are provided.
	 *            
	 * @param fileName
	 *            Name of the archive file to include.
     *
	 * @return A Future<Void> which returns once the message has been sent to the person. The Future will throw an exception if the message cannot be sent. 
	 */
	Future<Void> inspect(String instructions, InputStream fileContent, String fileName);

}