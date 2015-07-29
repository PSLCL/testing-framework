package com.pslcl.qa.runner.resource;

import java.util.Map;

public class ResourceWithAttributesImpl implements ResourceWithAttributes {
    private String hash;
    private Map<String, String> attributes;
    private int reference;
    
    public ResourceWithAttributesImpl(String hash, Map<String, String> attributes, int reference) {
        this.hash = hash;
        this.attributes = attributes;
        this.reference = reference;
    }

    @Override
    public String getHash() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getAttributes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getReference() {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public boolean match(ResourceWithAttributes rwa) {
        // TODO: match is that hash, attributes, and reference are equal
        return false;
    }

}
