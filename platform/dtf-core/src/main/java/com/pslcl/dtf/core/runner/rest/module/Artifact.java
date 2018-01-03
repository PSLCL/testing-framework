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
public class Artifact
{
    private static final Gson gson = new Gson();
    public final Long pk_artifact;
    public final String name;
    public final String configuration;
    public final String derived_from_artifact;
    public final String merged_from_module;
    public final Integer tests;
    public final Integer plans;

    public Artifact()
    {
        pk_artifact = null;
        name = null;
        configuration = null;
        derived_from_artifact = null;
        merged_from_module = null;
        tests = null;
        plans = null;
    }

    public Artifact(Long pk_artifact, String name, String configuration, String derived_from_artifact, String merged_from_module, Integer tests, Integer plans)
    {
        this.pk_artifact = pk_artifact;
        this.name = name;
        this.configuration = configuration;
        this.derived_from_artifact = derived_from_artifact;
        this.merged_from_module = merged_from_module;
        this.tests = tests;
        this.plans = plans;
    }

    public String toJson()
    {
        return gson.toJson(this);
    }
}