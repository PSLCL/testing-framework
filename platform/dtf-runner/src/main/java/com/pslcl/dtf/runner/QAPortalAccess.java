package com.pslcl.dtf.runner;

import java.io.InputStream;
import java.net.URL;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;

import com.pslcl.dtf.core.runner.config.RunnerConfig;

public class QAPortalAccess {

    private static final String RunnerQAPortalHostKey = "pslcl.dtf.runner.qa.portal.host";

	private final Executor executor; // is pooling, uses PoolingHttpClientConnectionManager 
	private volatile String hostQAPortal = null;
	
	private Response request(String contentSpecifier) throws Exception {
		String urlAsString = this.formArtifactHashSpecifiedURL(contentSpecifier).toString();
		Request request = Request.Get(urlAsString)
                .connectTimeout(1000)
                .socketTimeout(1000);
		Response retResponse =	this.executor.execute(request); // submits the request and collects the response
		return retResponse;
	}
	
	/**
	 * 
	 */
	QAPortalAccess() {
		this.executor = Executor.newInstance();
	}
	
	/**
	 * 
	 * @param runnerConfig
	 */
	void init(RunnerConfig runnerConfig) {
		this.hostQAPortal = runnerConfig.properties.getProperty(RunnerQAPortalHostKey, "https://testing.opendof.org");
	}
	
	/**
	 * 
	 * @param contentSpecifier
	 * @return
	 * @throws Exception
	 */
	public String getContentAsString(String contentSpecifier) throws Exception {
		Response response = this.request(contentSpecifier); 
		String retString = response.returnContent().asString();
		return retString;
	}
	
	/**
	 * 
	 * @param contentSpecifier
	 * @return
	 * @throws Exception
	 */
	public InputStream getContentAsStream(String contentSpecifier) throws Exception {
		Response response = this.request(contentSpecifier); 
		InputStream retStream = response.returnContent().asStream();
		return retStream;
	}

	/**
	 * 
	 * @param artifactHash
	 * @return
	 * @throws Exception
	 */
	public URL formArtifactHashSpecifiedURL(String artifactHash) throws Exception {
		// Note: When requesting https://testing.opendof.org, answer comes as: https://testing.opendof.org/#/dashboard
		
		URIBuilder b = new URIBuilder(this.hostQAPortal);
		// for parameter ""
        // scheme: null
        // host: null
        // path: null
        // fragment: null
		
		// for parameter https://testing.opendof.org
        // scheme: https
        // host: testing.opendof.org
        // path: ""
        // fragment: null

		// for parameter https://testing.opendof.org/#/dashboard    // dashboard is a fragment, representing an implied client side request
        // scheme: https
        // host: testing.opendof.org
        // path: "/"
        // fragment: /dashboard

		if (b.getHost() == null)
			throw new Exception("QA Portal host not configured");
		if (b.getScheme() == null)
			b.setScheme("https");
		
		// http://54.85.89.189/content/2320BADC148B562CC6F6D914CC0122E310BA047B48A7C8E378054F903919D2E7
		String contentPath = "/" + "content/" + artifactHash;
		b.setPath(contentPath);
		// on artifact not found, the eventual http request returns "org.apache.http.client.HttpResponseException: Not Found"

		// Note: results from alternative setters 
		// http://54.85.89.189?content=2320BADC148B562CC6F6D914CC0122E310BA047B48A7C8E378054F903919D2E7
//		b.addParameter("content", artifactHash);
		
		// http://54.85.89.189?content=2320BADC148B562CC6F6D914CC0122E310BA047B48A7C8E378054F903919D2E7
//		b.setParameter("content", artifactHash);
		
		// http://54.85.89.189##/content/2320BADC148B562CC6F6D914CC0122E310BA047B48A7C8E378054F903919D2E7
//		b.setFragment("/" + "content/" + artifactHash);

		//http://%2Fcontent%2F2320BADC148B562CC6F6D914CC0122E310BA047B48A7C8E378054F903919D2E7@54.85.89.189
//		b.setUserInfo("/" + "content/" + artifactHash);

		URL retURL = b.build().toURL();
		return retURL;


//		File file = new File(artifactHash); // someArtifactHash
//		URI uri = file.toURI();         // uri.path is C:/gitdtf/ws/apps/someArtifactHash, apparently builds onto the base directory of the launching java app
//		URL url = uri.toURL();          // url.path is C:/gitdtf/ws/apps/someArtifactHash
//	
//		String alternateFilename = url.getFile(); // filename is: C:/gitdtf/ws/apps/someArtifactHash
//		file = new File(alternateFilename);       // filename is: C:/gitdtf/ws/apps/someArtifactHash
	}
}

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
			
			
			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		return null;
//	}