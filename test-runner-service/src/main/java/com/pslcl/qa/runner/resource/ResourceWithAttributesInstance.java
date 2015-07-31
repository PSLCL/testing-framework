package com.pslcl.qa.runner.resource;

import java.util.Map;

public class ResourceWithAttributesInstance implements ResourceWithAttributes {
    private String hash;
    private Map<String, String> attributes;
    private int reference;
    
    public ResourceWithAttributesInstance(String hash, Map<String, String> attributes, int reference) {
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
    
    /**
     * 
     * @param rwa Must not be null
     * @return
     */
    public boolean matches(ResourceWithAttributes rwa) {
        // match: reference, hash and attributes are equal
        if (this.reference == rwa.getReference() && this.hash.equals(rwa.getHash())) {
            // match the attribute sets to each other
            Map<String, String> rwaAttributes = rwa.getAttributes();
            if (this.attributes.size() != rwaAttributes.size())
                return false;
            // tHese keys and values might be empty strings, but they will not be null; keys are unique in each Map
            for (String key : this.attributes.keySet()) {
                if (rwaAttributes.containsKey(key)) {
                    String value = this.attributes.get(key);
                    if (value.equals(rwaAttributes.get(key)))
                        continue;
                }
                return false;
            }
        }
        return true; // every check succeeded
    }

}