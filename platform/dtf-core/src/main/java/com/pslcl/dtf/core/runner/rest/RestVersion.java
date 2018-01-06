package com.pslcl.dtf.core.runner.rest;

import com.google.gson.Gson;

public class RestVersion
{
    private static final Gson gson = new Gson();

    public final String version;
    public final String apiVersion;

    @SuppressWarnings("unused")
    public RestVersion()
    {
        version = null;
        apiVersion = null;
    }

    public RestVersion(String version, String apiVersion)
    {
        this.version = version;
        this.apiVersion = apiVersion;
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

        RestVersion version1 = (RestVersion)o;

        return (version != null ? version.equals(version1.version) : version1.version == null) && (apiVersion != null ? apiVersion.equals(version1.apiVersion) : version1.apiVersion == null);
    }

    @Override
    public int hashCode()
    {
        int result = version != null ? version.hashCode() : 0;
        result = 31 * result + (apiVersion != null ? apiVersion.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "Version{" + "version='" + version + "\\" + ", apiVersion='" + apiVersion + "\\" + "}";
    }
}
