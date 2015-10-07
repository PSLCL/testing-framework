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
package com.pslcl.qa.runner.resource;

import java.util.Map;

/**
 * A ResourceQuery is a resource hash and a map of attributes.
 */
public interface ResourceWithAttributes {
	
	/**
	 * Get the name of the resource.
	 * @return The name.
	 */
	String getName();	
	
	/**
	 * Get the set of attributes for the resource.
	 * @return A map of strings representing the set of attributes for the resource.
	 */
	Map<String, String> getAttributes();
	
	/**
	 * Get a reference matching this object to the original resource request.
	 * In the context of a template, this reference may be the line number holding the original resource bind request.
	 * @returns The resource reference. 
	 */
	int getReference();

}