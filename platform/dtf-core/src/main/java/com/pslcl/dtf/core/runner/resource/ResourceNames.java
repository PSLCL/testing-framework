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
package com.pslcl.dtf.core.runner.resource;

@SuppressWarnings("javadoc")
public class ResourceNames
{
    public static final String PslclKeyBase = "pslcl.dtf";
    
    /* ****************************************************************************
     * Portal declarations    
    ******************************************************************************/    
    public static final String PortalKeyBase = PslclKeyBase + ".portal";
    public static final String PortalHostKey = PortalKeyBase + ".host";
    public static final String PortalHostDefault = "https://testing.opendof.org";
    
    public static final String PortalContentDirKey = PortalKeyBase + ".content-dir";
    public static final String PortalContentDirDefault = "content";

    /* ****************************************************************************
     * Deploy declarations    
    ******************************************************************************/    
    public static final String DepoyKeyBase = PslclKeyBase + ".deploy";
    
    public static final String DeployLinuxSandboxKey = DepoyKeyBase + ".linux-sandbox-path";
    public static final String DeployWinSandboxKey = DepoyKeyBase + ".win-sandbox-path";
    public static final String DeployStafPingKey = DepoyKeyBase + ".staf-ping";
    
    public static final String DeployLinuxSandboxDefault = "/opt/dtf/sandbox";
    public static final String DeployWinSandboxDefault = "c:\\opt\\dtf\\sandbox";
    public static final String DeployStafPingDefault = "true";
}