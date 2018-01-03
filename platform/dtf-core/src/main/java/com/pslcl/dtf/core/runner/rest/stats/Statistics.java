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
package com.pslcl.dtf.core.runner.rest.stats;

import com.google.gson.Gson;

@SuppressWarnings("javadoc")
public class Statistics
{
    private static final Gson gson = new Gson();

    public final Integer module_count;
    public final Integer test_plan_count;
    public final Integer test_count;
    public final Integer artifact_count;
    public final Integer ti_count;
    public final Integer ti_pending;
    public final Integer ti_running;
    public final Integer untested_module_count;

    public Statistics()
    {
        module_count = null;
        test_plan_count = null;
        test_count = null;
        artifact_count = null;
        ti_count = null;
        ti_pending = null;
        ti_running = null;
        untested_module_count = null;
    }

    public Statistics(
            Integer moduleCount,
            Integer testPlanCount,
            Integer testCount,
            Integer artifactCount,
            Integer tiCount,
            Integer tiPending,
            Integer tiRunning,
            Integer untestedModuleCount)
    {
        this.module_count = moduleCount;
        this.test_plan_count = testPlanCount;
        this.test_count = testCount;
        this.artifact_count = artifactCount;
        this.ti_count = tiCount;
        this.ti_pending = tiPending;
        this.ti_running = tiRunning;
        this.untested_module_count = untestedModuleCount;
    }

    public String toJson()
    {
        return gson.toJson(this);
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;

        Statistics that = (Statistics)o;

        if(module_count != null ? !module_count.equals(that.module_count) : that.module_count != null)
            return false;
        if(test_plan_count != null ? !test_plan_count.equals(that.test_plan_count) : that.test_plan_count != null)
            return false;
        if(test_count != null ? !test_count.equals(that.test_count) : that.test_count != null)
            return false;
        if(artifact_count != null ? !artifact_count.equals(that.artifact_count) : that.artifact_count != null)
            return false;
        if(ti_count != null ? !ti_count.equals(that.ti_count) : that.ti_count != null)
            return false;
        if(ti_pending != null ? !ti_pending.equals(that.ti_pending) : that.ti_pending != null)
            return false;
        if(ti_running != null ? !ti_running.equals(that.ti_running) : that.ti_running != null)
            return false;
        return untested_module_count != null ? untested_module_count.equals(that.untested_module_count) : that.untested_module_count == null;
    }

    @Override
    public int hashCode()
    {
        int result = module_count != null ? module_count.hashCode() : 0;
        result = 31 * result + (test_plan_count != null ? test_plan_count.hashCode() : 0);
        result = 31 * result + (test_count != null ? test_count.hashCode() : 0);
        result = 31 * result + (artifact_count != null ? artifact_count.hashCode() : 0);
        result = 31 * result + (ti_count != null ? ti_count.hashCode() : 0);
        result = 31 * result + (ti_pending != null ? ti_pending.hashCode() : 0);
        result = 31 * result + (ti_running != null ? ti_running.hashCode() : 0);
        result = 31 * result + (untested_module_count != null ? untested_module_count.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "Statistics{" + "module_count=" + module_count + ", test_plan_count=" + test_plan_count + ", test_count=" + test_count + ", artifact_count=" + artifact_count + ", ti_count=" + ti_count + ", ti_pending=" + ti_pending + ", ti_running=" + ti_running + ", untested_module_count=" + untested_module_count + '}';
    }
}