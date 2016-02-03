package com.pslcl.dtf.runner.template;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Response;

public class QAPaResponse {

    private Response qaPortalResponse;
    
    /**
     * 
     */
    public QAPaResponse() {
        this.qaPortalResponse = null;
    }
    
    /**
     * 
     * @param qaPortalResponse
     */
    public QAPaResponse(Response qaPortalResponse) {
        this.qaPortalResponse = qaPortalResponse;
    }
    
    /**
     * 
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    String getContentAsString() throws ClientProtocolException, IOException {
        String retString = this.qaPortalResponse.returnContent().asString();
        return retString;
    }
    
    /**
     * 
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    InputStream getContentAsStream() throws ClientProtocolException, IOException {
        InputStream retStream = this.qaPortalResponse.returnContent().asStream();
        return retStream;
    }

}