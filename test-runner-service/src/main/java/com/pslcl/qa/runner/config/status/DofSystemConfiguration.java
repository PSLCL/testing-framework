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
package com.pslcl.qa.runner.config.status;

import java.io.File;

import org.opendof.core.oal.DOFCredentials;
import org.opendof.core.oal.DOFSystem;

import com.pslcl.qa.runner.config.RunnerServiceConfig;


@SuppressWarnings("javadoc")
public class DofSystemConfiguration
{
    public static final String CredentialFileKey = "pslcl.dof.system.credentials";
    public static final String TunnelKey = "pslcl.dof.system.tunnel";
    
    public static final String TunnelDefault = "false";

    public static DOFSystem.Config propertiesToConfig(RunnerServiceConfig config) throws Exception
    {
        String msg = "ok";
        try
        {
            msg = "invalid boolean value for tunnel";
            String value = config.properties.getProperty(TunnelKey, TunnelDefault);
            config.initsb.ttl(TunnelKey, "=", value);
            boolean tunnel = Boolean.parseBoolean(value);

            msg = "invalid credentials file";
            String credFile = config.properties.getProperty(CredentialFileKey);
            config.initsb.ttl(CredentialFileKey, "=", credFile);
            
            DOFCredentials credentials = null;
            if(credFile != null)
                credentials = DOFCredentials.create(new File(credFile));

            msg = "DOFSystem.Builder.build failed";
            DOFSystem.Config.Builder builder = new DOFSystem.Config.Builder()
                .setTunnelDomains(tunnel);
            if(credentials != null)
                builder.setCredentials(credentials);
            config.properties.remove(CredentialFileKey);
            config.properties.remove(TunnelKey);
            return builder.build();
        } catch (Exception e)
        {
            config.initsb.ttl(msg);
            throw e;
        }
    }
}
