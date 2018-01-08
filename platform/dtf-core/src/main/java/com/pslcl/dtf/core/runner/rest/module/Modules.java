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
import com.pslcl.dtf.core.runner.rest.module.Module;

import java.util.List;

@SuppressWarnings("javadoc")
public class Modules
{
    public static final String FilterParam = "filter";
    public static final String OrderParam = "order";
    public static final String OrderNameValue = "name";
    public static final String OrderPlanValue = "plans";
    public static final String OrderTestValue = "tests";
    public static final char AscendingFlag = '<';
    public static final char DescendingFlag = '>';
    public static final String LimitParam = "limit";
    public static final String LimitAllValue = "all";
    public static final String OffsetParam = "offset";
    private static final Gson gson = new Gson();
    public final List<Module> modules;

    public Modules()
    {
        modules = null;
    }

    public Modules(List<Module> modules)
    {
        this.modules = modules;
    }

    public String toJson()
    {
        return gson.toJson(this);
    }
}