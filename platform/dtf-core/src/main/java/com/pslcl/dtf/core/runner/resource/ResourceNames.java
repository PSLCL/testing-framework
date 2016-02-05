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
    /* ****************************************************************************
     * Globals     
    ******************************************************************************/    
    public static final String PslclKeyBase = "pslcl.dtf";
    public static final String RunnerKeyBase = PslclKeyBase + ".runner";
    
    public static final String LogPathKey = PslclKeyBase + ".log-file-path";
    
    public static final String LogPathDefault = null;
    public static final String ShortMaxDelayDefault = "5000"; // 5 seconds
    public static final String ShortMaxRetriesDefault = "17"; // roughly 1 minute
    public static final String LongMaxDelayDefault = "15000"; // 15 seconds
    public static final String LongMaxRetriesDefault = "67";  // roughly 15 minutes
    
    /* ****************************************************************************
     * Portal declarations    
    ******************************************************************************/    
    public static final String PortalKeyBase = PslclKeyBase + ".portal";    
    public static final String PortalHostKey = PortalKeyBase + ".host";
    public static final String PortalContentDirKey = PortalKeyBase + ".content-dir";
    
    public static final String PortalHostDefault = null;
    public static final String PortalContentDirDefault = "content";

    /* ****************************************************************************
     * Message Queue    
    ******************************************************************************/    
    public static final String MsgQueNameKey = RunnerKeyBase + ".msg-queue-name";
    public static final String MsgQueClassKey = "pslcl.dtf.resource.mq-class";
    
    public static final String MsgQueClassDefault = "com.pslcl.dtf.resource.aws.Sqs";
    public static final String MsgQueNameDefault = null;

    /* ****************************************************************************
     * Database    
    ******************************************************************************/    
    public static final String DbBase = PslclKeyBase + ".db";
    public static final String DbHostKey = DbBase + ".host";
    public static final String DbUserKey = DbBase + ".user";
    public static final String DbPassKey = DbBase + ".password";
    
    public static final String DbHostDefault = null;
    public static final String DbUserDefault = null;
    public static final String DbPassDefault = null;

    /* ****************************************************************************
     * Staf    
    ******************************************************************************/
    public static final String StafKeyBase = PslclKeyBase + ".staf";
    public static final String StafLocalPingKey = StafKeyBase + ".local-ping";
    
    public static final String StafLocalPingDefault = "true";
    
    /* ****************************************************************************
     * Resource Providers    
    ******************************************************************************/
    public static final String ResourceKeyBase = PslclKeyBase + ".resource";    
    public static final String ResourceShortNameKey = ResourceKeyBase + ".short-name";
    public static final String ResourceManagerClassKey = ResourceKeyBase + ".resource-manager-class";
    
    public static final String ResourceShortNameDefault = "dtf";
    public static final String ResourceManagerClassDefault = "com.pslcl.dtf.resource.aws.AwsResourcesManager";

    /* ****************************************************************************
     * Hardware/image     
    ******************************************************************************/    
    public static final String MachineKeyBase = ResourceKeyBase + ".machine";    
    public static final String ImageBase = ResourceKeyBase + ".image";    
    public static final String MachineCoresKey = MachineKeyBase + ".cores";
    public static final String MachineMemoryKey = MachineKeyBase + ".memory-size";
    public static final String MachineDiskKey = MachineKeyBase + ".disk-size";
    public static final String ImageImageIdKey = ImageBase + ".id";
    public static final String ImageConfigKey = ImageBase + ".image";
    
    public static final String MachineCoresDefault = "2";
    public static final String MachineMemoryDefault = "4g";
    public static final String MachineDiskDefault = "10g";
    public static final String ImageImageIdDefault = null;
    public static final String ImageConfigDefault = null;

    /* ****************************************************************************
     * Deploy declarations    
    ******************************************************************************/    
    public static final String DepoyKeyBase = PslclKeyBase + ".deploy";
    public static final String DeployLinuxSandboxKey = DepoyKeyBase + ".linux-sandbox-path";
    public static final String DeployWinSandboxKey = DepoyKeyBase + ".win-sandbox-path";
    
    public static final String DeployLinuxSandboxDefault = "/opt/dtf/sandbox";
    public static final String DeployWinSandboxDefault = "c:\\opt\\dtf\\sandbox";
    
    /* ****************************************************************************
     * Inspect    
    ******************************************************************************/    
    public static final String InspectKeyBase = PslclKeyBase + ".inspect";
    
    // note that inspector is a base key and can be numbered from 0 on up, to add as many inspectors as desired
    public static final String InspectSenderKey = InspectKeyBase + ".sender";
    public static final String InspectReplyKey = InspectKeyBase + ".reply";
    public static final String InspectSubjectKey = InspectKeyBase + ".subject";
    public static final String InspectInspectorKey = InspectKeyBase + ".inspector";
    public static final String InspectMaxDelayKey = InspectKeyBase + ".max-delay";
    public static final String InspectMaxRetriesKey = InspectKeyBase + ".max-retries";
    
    public static final String InspectSenderDefault = null; // valid email address
    public static final String InspectReplyDefault = null; // valid email address
    public static final String InspectSubjectDefault = "dtf-runner inspect";
    public static final String InspectInspectorDefault = null;  // valid email address
    public static final String InspectMaxDelayDefault = ShortMaxDelayDefault;      
    public static final String InspectMaxRetriesDefault = ShortMaxRetriesDefault;  // about 1 min timeout
}