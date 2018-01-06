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
package com.pslcl.dtf.core.runner.rest.runRates;

import com.google.gson.Gson;

import java.util.List;

@SuppressWarnings("javadoc")
public class RunRates
{
    public static final String FromParam = "from";
    public static final String ToParam = "to";
    public static final String BucketParam = "bucket";

    private static final Gson gson = new Gson();
    public final String from;
    public final String to;
    public final Long fromTimestamp;
    public final Long toTimestamp;

    public final Long bucket;
    public final List<RunRate> starting;
    public final List<RunRate> running;
    public final List<RunRate> complete;

    public RunRates()
    {
        from = null;
        to = null;
        fromTimestamp = null;
        toTimestamp = null;
        bucket = null;
        starting = null;
        running = null;
        complete = null;
    }

    public RunRates(
            String from,
            String to,
            Long fromTimestamp,
            Long toTimestamp,
            Long bucket,
            List<RunRate> starting,
            List<RunRate> running,
            List<RunRate> complete)
    {
        this.from = from;
        this.to = to;
        this.fromTimestamp = fromTimestamp;
        this.toTimestamp = toTimestamp;
        this.bucket = bucket;
        this.starting = starting;
        this.running = running;
        this.complete = complete;
    }

    public String toJson()
    {
        return gson.toJson(this);
    }
}