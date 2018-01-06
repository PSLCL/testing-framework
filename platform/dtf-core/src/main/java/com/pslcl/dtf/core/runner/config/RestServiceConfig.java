package com.pslcl.dtf.core.runner.config;

import com.pslcl.dtf.core.runner.resource.ResourceNames;
import com.pslcl.dtf.core.util.StrH;

import java.util.HashSet;
import java.util.Set;

public class RestServiceConfig
{
    public final String host;
    public final String identityServiceUrl;
    public final String prefix;

    private RestServiceConfig(String host, String identityServiceUrl, String prefix)
    {
        this.host = host;
        this.identityServiceUrl = identityServiceUrl;
        this.prefix = prefix;
    }

    public static RestServiceConfig propertiesToConfig(RunnerConfig config) throws Exception
    {
        String msg = "ok";
        try
        {
            String host = config.properties.getProperty(ResourceNames.PortalHostKey, ResourceNames.PortalHostDefault);
            if(host == null)
                throw new Exception(ResourceNames.PortalHostKey + " is required");
            host = StrH.trim(host);
            config.initsb.ttl(ResourceNames.PortalHostKey, "=", host);
            String idHost = config.properties.getProperty(ResourceNames.PortalIdServiceKey, ResourceNames.PortalIdServiceDefault);
            if(idHost == null)
                throw new Exception(ResourceNames.PortalIdServiceKey + " is required");
            idHost = StrH.trim(idHost);
            config.initsb.ttl(ResourceNames.PortalIdServiceKey, "=", idHost);
            String prefix = config.properties.getProperty(ResourceNames.PortalUrlPrefixKey, ResourceNames.PortalUrlPrefixDefault);
            if(prefix != null)
                prefix = StrH.trim(prefix);
            config.initsb.ttl(ResourceNames.PortalUrlPrefixKey, "='", prefix, "'");

//            config.properties.remove(HostKey);
            config.properties.remove(ResourceNames.PortalIdServiceKey);
            config.properties.remove(ResourceNames.PortalUrlPrefixKey);
            return new RestServiceConfig(host, idHost, prefix);
        }catch(Exception e)
        {
            config.initsb.ttl(msg);
            throw e;
        }
    }
}
