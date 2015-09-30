package com.pslcl.qa.runner.template;

public class ArtifactInfo {
    
    // static methods
    
    /**
     * 
     * @param strInstructionsHash
     * @return
     */
    public static String getContent(String strInstructionsHash) {
        String retString = new String("");
        // TODO: go to Artifact provider to retrieve file contents 
        return retString;
    }

    
    private String strArtifactName;
    private String strArtifactHash;
    
    public ArtifactInfo(String strArtifactName, String strArtifactHash) {
        this.strArtifactName = strArtifactName;
        this.strArtifactHash = strArtifactHash;
    }
    
}
