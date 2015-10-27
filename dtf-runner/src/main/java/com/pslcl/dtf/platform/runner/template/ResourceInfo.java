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
package com.pslcl.dtf.platform.runner.template;

import com.pslcl.dtf.platform.core.runner.resource.instance.ResourceInstance;

public class ResourceInfo implements Comparable<ResourceInfo> {
    private ResourceInstance resourceInstance;
    private ArtifactInfo artifactInfo = null;
    private String instructionsHash = null;
    private String network = null;
    private String runParams = null;

    private long getReference() {
        return resourceInstance.getReference();
    }
    
    // implement Comparable interface
    @Override
    public int compareTo(ResourceInfo ori) 
    {
        long delta = resourceInstance.getReference() - ori.resourceInstance.getReference(); 
        
        if(delta < 0)
            return -1;
        if(delta > 0)
            return 1;
        return 0;
    }

   
    // public methods
    
    /**
     * Constructor
     * @param resourceInstance
     */
    public ResourceInfo(ResourceInstance resourceInstance) {
        this.resourceInstance = resourceInstance;
    }

    public ResourceInstance getResourceInstance() {
        return resourceInstance;
    }
    
    public void setArtifactInfo(ArtifactInfo artifactInfo) {
        this.artifactInfo = artifactInfo;
    }
    
    public void setInstructionsHash(String instructionsHash) {
        this.instructionsHash = instructionsHash;
    }
    
    public void setNetwork(String network) {
        this.network = network;
    }
    
    public void setRunParams(String runParams) {
        this.runParams = runParams;
    }
}