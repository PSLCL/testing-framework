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
package com.pslcl.dtf.core.runner.rest.module;

import com.google.gson.Gson;

@SuppressWarnings("javadoc")
public class Module
{
    private static final Gson gson = new Gson();
    public final Integer pk_module;
    public final String organization;
    public final String name;
    public final String attributes;
    public final String version;
    public final String sequence;
    public final Integer tests;
    public final Integer plans;

    public Module()
    {
        pk_module = null;
        organization = null;
        name = null;
        attributes = null;
        version = null;
        sequence = null;
        tests = null;
        plans = null;
    }

    public Module(Integer pk_module, String organization, String name, String attributes, String version, String sequence, Integer tests, Integer plans)
    {
        this.pk_module = pk_module;
        this.organization = organization;
        this.name = name;
        this.attributes = attributes;
        this.version = version;
        this.sequence = sequence;
        this.tests = tests;
        this.plans = plans;
    }

    public String toJson()
    {
        return gson.toJson(this);
    }
}