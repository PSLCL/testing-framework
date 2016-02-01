package com.pslcl.dtf.runner;

import java.io.InputStream;
import java.net.URL;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.RunnerConfig;

public class QAPortalAccess { 

    private static final String RunnerQAPortalHostKey = "pslcl.dtf.runner.qa.portal.host";

	private final Executor executor; // is pooling, uses PoolingHttpClientConnectionManager 
	private volatile String hostQAPortal = null;
    private final Logger log;
    private final String simpleName;
	
    /**
     * 
     * @param contentSpecifier
     * @return
     * @throws Exception
     */
    private Response request(String contentSpecifier) throws Exception {
		Response retResponse = null;
		try {
			String urlAsString = this.formArtifactHashSpecifiedURL(contentSpecifier).toString();
			Request request = Request.Get(urlAsString)
			        .connectTimeout(1000)
			        .socketTimeout(1000);
			retResponse =	this.executor.execute(request); // submits the request and collects the response
		} catch (Exception e) {
			log.debug(this.simpleName + "http content request exception for contentSpecifier " + contentSpecifier + ", exception message: " + e.getMessage());
			throw e;
		}
		return retResponse;
	}
	
	/**
	 * Constructor
	 */
	QAPortalAccess() {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
		this.executor = Executor.newInstance();
	}
	
	/**
	 * 
	 * @param runnerConfig
	 */
	void init(RunnerConfig runnerConfig) throws Exception {
		this.hostQAPortal = runnerConfig.properties.getProperty(RunnerQAPortalHostKey);
		if(hostQAPortal == null){
			this.log.error("Missing required property: " + RunnerQAPortalHostKey);
			throw new Exception("Missing required property: " + RunnerQAPortalHostKey);
		}
	}
	
	/**
	 * 
	 * @param contentSpecifier
	 * @return
	 * @throws Exception
	 */
	public String getContentAsString(String contentSpecifier) throws Exception {
		String retString = null;
		try {
			Response response = this.request(contentSpecifier); 
			retString = response.returnContent().asString();
		} catch (Exception e) {
			log.debug(this.simpleName + "getContentAsString() http content request exception for contentSpecifier " + contentSpecifier + ", exception message: " + e.getMessage());
			throw e;
		}
		return retString;
	}
	
	/**
	 * 
	 * @param contentSpecifier
	 * @return
	 * @throws Exception
	 */
	public InputStream getContentAsStream(String contentSpecifier) throws Exception {
		InputStream retStream = null;
		try {
			Response response = this.request(contentSpecifier); 
			retStream = response.returnContent().asStream();
		} catch (Exception e) {
			log.debug(this.simpleName + "getContentAsStream() http content request exception for contentSpecifier " + contentSpecifier + ", exception message: " + e.getMessage());
			throw e;
		}
		return retStream;
	}

	/**
	 * 
	 * @param artifactHash
	 * @return
	 * @throws Exception
	 */
	public URL formArtifactHashSpecifiedURL(String artifactHash) throws Exception {
		URIBuilder b = new URIBuilder(this.hostQAPortal);
		if (b.getHost() == null)
			throw new Exception("QA Portal host not configured");
		if (b.getScheme() == null)
			b.setScheme("https");
		b.setPath("/" + "content/" + artifactHash);
		// http://54.85.89.189/content/2320BADC148B562CC6F6D914CC0122E310BA047B48A7C8E378054F903919D2E7
		// Note: on artifact not found, the eventual http request returns "org.apache.http.client.HttpResponseException: Not Found"

		//b.addParameter("content", artifactHash);
		// http://54.85.89.189?content=2320BADC148B562CC6F6D914CC0122E310BA047B48A7C8E378054F903919D2E7
		
		//b.setParameter("content", artifactHash);
		// http://54.85.89.189?content=2320BADC148B562CC6F6D914CC0122E310BA047B48A7C8E378054F903919D2E7
		
		//b.setFragment("/" + "content/" + artifactHash);
		// http://54.85.89.189##/content/2320BADC148B562CC6F6D914CC0122E310BA047B48A7C8E378054F903919D2E7

		//b.setUserInfo("/" + "content/" + artifactHash);
		//http://%2Fcontent%2F2320BADC148B562CC6F6D914CC0122E310BA047B48A7C8E378054F903919D2E7@54.85.89.189

		URL retURL = b.build().toURL();
		return retURL;
	}
}

//File file = new File(artifactHash); // someArtifactHash
//URI uri = file.toURI();         // uri.path is C:/gitdtf/ws/apps/someArtifactHash, apparently builds onto the base directory of the launching java app
//URL url = uri.toURL();          // url.path is C:/gitdtf/ws/apps/someArtifactHash
//
//String alternateFilename = url.getFile(); // filename is: C:/gitdtf/ws/apps/someArtifactHash
//file = new File(alternateFilename);       // filename is: C:/gitdtf/ws/apps/someArtifactHash


//private ResponseHandler rh = null;

//			Document result;
//			result = Request.Get("").execute().handleResponse(new ResponseHandler<Document>() {
//				
//				public Document handleResponse(HttpResponse response) {
//					
//					
//					DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
//					try {
//						
//						HttpEntity entity = response.getEntity();
//						ContentType contentType = ContentType.getOrDefault(entity);
//						if (!contentType.equals(ContentType.APPLICATION_XML)) {
//							//throw 
//						}
//						String charset = contentType.getCharset().toString();
//						
//						DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
//						return docBuilder.parse(entity.getContent(), charset);
//					} catch (Exception e) {
//						
//					}
//					return null;
//				}
//			});
//			
//			if (result != null)
//				return result.toString();