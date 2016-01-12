package com.pslcl.dtf.runner;

import java.io.IOException;
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
	
	QAPortalAccess() {
		this.executor = Executor.newInstance();
	}
	
	void init(RunnerConfig runnerConfig) {
		this.hostQAPortal = runnerConfig.properties.getProperty(RunnerQAPortalHostKey, "https://testing.opendof.org/#/dashboard");
	}
	
	public String getContent(String contentSpecifier) throws Exception {
		try {
			String urlAsString = this.formArtifactHashSpecifiedURL(contentSpecifier).toString();
			
			// Note: Without adding a header, this path will return a String, of the entire web page at the indicated web page.
			Request request = Request.Get(urlAsString)
                                     .connectTimeout(1000)
                                     .socketTimeout(1000);
			Response response =	this.executor.execute(request); // submits the request and collects the response
			String retString = response.returnContent().asString();
			return retString;
		} catch (IOException e) {
			throw new Exception(e);
		}
	}

	public URL formArtifactHashSpecifiedURL(String artifactHash) throws Exception {
		// Note: When requesting https://testing.opendof.org, answer comes as: https://testing.opendof.org/#/dashboard
		
		URIBuilder b = new URIBuilder(this.hostQAPortal);
		// for submitted ""
        // scheme: null
        // host: null
        // path: null
        // fragment: null
		
		// for submitted https://testing.opendof.org
        // scheme: https
        // host: testing.opendof.org
        // path: ""
        // fragment: null

		// for submitted https://testing.opendof.org/#/dashboard    // dashboard is a fragment, representing an implied client side request
        // scheme: https
        // host: testing.opendof.org
        // path: "/"
        // fragment: /dashboard

		if (b.getHost() == null)
			throw new Exception("QA Portal host not configured");
		if (b.getScheme() == null)
			b.setScheme("https");
        // adding a header informs QA Portal that it should return something more specific than its web page
		b.addParameter("content", artifactHash);
		
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