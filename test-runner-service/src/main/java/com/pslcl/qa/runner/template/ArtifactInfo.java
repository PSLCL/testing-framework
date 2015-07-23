package com.pslcl.qa.runner.template;

public class ArtifactInfo {
    private String strComponentName;
    private String strArtifactName;
    private String strArtifactHash;
    
    public ArtifactInfo(String strComponentName, String strArtifactName, String strArtifactHash) {
        this.strComponentName = strComponentName;
        this.strArtifactName = strArtifactName;
        this.strArtifactHash = strArtifactHash;
    }
    
}
