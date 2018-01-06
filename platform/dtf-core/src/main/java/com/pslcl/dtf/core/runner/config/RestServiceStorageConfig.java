package com.pslcl.dtf.core.runner.config;

import com.pslcl.dtf.core.runner.resource.ResourceNames;
import com.pslcl.dtf.core.util.StrH;

public class RestServiceStorageConfig
{
    public final String host;
    public final int port;
    public final String user;
    public final String password;
    public final String schema;

    private RestServiceStorageConfig(String host, int port, String user, String password, String schema)
    {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.schema = schema;
    }

    public static RestServiceStorageConfig propertiesToConfig(RunnerConfig config) throws Exception
    {
        String msg = "ok";
        try
        {
            String host = config.properties.getProperty(ResourceNames.StorageHostKey, ResourceNames.StorageHostDefault);
            if(host == null)
                throw new Exception(ResourceNames.StorageHostKey + " is required");
            host = StrH.trim(host);
            config.initsb.ttl(ResourceNames.StorageHostKey, "=", host);
            String value = config.properties.getProperty(ResourceNames.StoragePortKey, ResourceNames.StoragePortDefault);
            value = StrH.trim(value);
            int port = Integer.parseInt(value);
            config.initsb.ttl(ResourceNames.StoragePortKey, "=", value);
            String user = config.properties.getProperty(ResourceNames.StorageUserKey, ResourceNames.StorageUserDefault);
            if(user == null)
                throw new Exception(ResourceNames.StorageUserKey + " is required");
            user = StrH.trim(user);
            config.initsb.ttl(ResourceNames.StorageUserKey, "=", user);
            String password = config.properties.getProperty(ResourceNames.StoragePasswordKey, ResourceNames.StoragePasswordDefault);
            if(password == null)
                throw new Exception(ResourceNames.StoragePasswordKey + " is required");
            password = StrH.trim(password);

            String schema = config.properties.getProperty(ResourceNames.StorageSchemaKey, ResourceNames.StorageSchemaDefault);
            schema = StrH.trim(schema);
           config.initsb.ttl(ResourceNames.StorageSchemaKey, "=", schema);

            config.properties.remove(ResourceNames.StorageHostKey);
            config.properties.remove(ResourceNames.StoragePortKey);
            config.properties.remove(ResourceNames.StorageUserKey);
            config.properties.remove(ResourceNames.StoragePasswordKey);
            config.properties.remove(ResourceNames.StorageSchemaKey);
            return new RestServiceStorageConfig(host, port, user, password, schema);

        }catch(Exception e)
        {
            config.initsb.ttl(msg);
            throw e;
        }
    }
}
