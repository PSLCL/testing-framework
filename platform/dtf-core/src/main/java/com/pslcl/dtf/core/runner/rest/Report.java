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
package com.pslcl.dtf.core.runner.rest;

import com.google.gson.Gson;

@SuppressWarnings("javadoc")
public class Report
{
    private static final Gson gson = new Gson();
    public final Long pk_test_plan;
    public final Long pk_test;
    public final Long pk_test_instance;
    public final String description;
    public final String modules;
    public final Boolean result;
    public final Long end_time;

    public Report()
    {
        pk_test_plan = null;
        pk_test = null;
        pk_test_instance = null;
        description = null;
        modules = null;
        result = null;
        end_time = null;
    }

    public Report(Long pk_test_plan, Long pk_test, Long pk_test_instance, String description, String modules, Boolean result, Long end_time)
    {
        this.pk_test_plan = pk_test_plan;
        this.pk_test = pk_test;
        this.pk_test_instance = pk_test_instance;
        this.description = description;
        this.modules = modules;
        this.result = result;
        this.end_time = end_time;
    }

    public String toJson()
    {
        return gson.toJson(this);
    }
}