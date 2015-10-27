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
package com.pslcl.dtf.platform.core.artifact;

import java.io.InputStream;

import com.pslcl.dtf.platform.core.Hash;
import com.pslcl.dtf.platform.core.generator.template.Template;
/**
 * This class represents content. All content is identified by a hash, which allows
 * multiple references to the same content even if other metadata is different.
 */
public interface Content extends Template.Parameter
{
    /**
     * Obtain the hash of the contents.
     * @return The hash of the contents.
     */
    Hash getHash();

    /**
     * Obtain a stream that will return the content.
     * @return A stream that will return the content.
     */
    InputStream asStream();

    /**
     * Obtain the content as a byte array.
     * @return A byte array containing the content.
     */
    byte[] asBytes();
}
