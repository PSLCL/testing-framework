/*
 * This work is protected by Copyright, see COPYING.txt for more information.
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
