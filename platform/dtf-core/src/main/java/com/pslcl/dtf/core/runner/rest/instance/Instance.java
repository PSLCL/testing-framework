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
package com.pslcl.dtf.core.runner.rest.instance;

import com.google.gson.Gson;
import javafx.scene.chart.Chart;

import java.util.List;

@SuppressWarnings("javadoc")
public class Instance
{
    private static final Gson gson = new Gson();
    public final String key;
    public final String name;
    public final String description;
    public final Summary summary;
    public final Chart chart;
    public final List<Value> values;

    public Instance()
    {
        key = null;
        name = null;
        description = null;
        summary = null;
        chart = null;
        values = null;
    }

    public Instance(String key, String name, String description, Summary summary, Chart chart, List<Value> values)
    {
        this.key = key;
        this.name = name;
        this.description = description;
        this.summary = summary;
        this.chart = chart;
        this.values = values;
    }

    public String toJson()
    {
        return gson.toJson(this);
    }
}