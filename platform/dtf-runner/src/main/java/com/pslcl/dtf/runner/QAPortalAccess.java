package com.pslcl.dtf.runner;

import java.io.IOException;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

import com.pslcl.dtf.core.runner.config.RunnerConfig;

public class QAPortalAccess {
	
	private final Executor executor; // is pooling, uses PoolingHttpClientConnectionManager 
	private volatile String hostQAPortal = null;
	//private ResponseHandler rh = null;
	
	QAPortalAccess() {
		this.executor = Executor.newInstance();
	}
	
	void init(RunnerConfig runnerConfig) {
		this.hostQAPortal = runnerConfig.properties.getProperty(null, "https://testing.opendof.org/#/dashboard"); // TODO: figure out a property key to place here
	}
	
	public String getContent(String contentDescriptor, String contentSpecifier) throws Exception {
		try {
			// Note: Without adding a header, this path will return a String, of the entire web page at the indicated web page.
			Request request = Request.Get(this.hostQAPortal)
                                     .connectTimeout(1000)
                                     .socketTimeout(1000)
                                     // adding a header informs QA Portal that it should return something more specific than its web page
                                     .addHeader(contentDescriptor, contentSpecifier); // See routes.js of QA Portal Web Server. /artifact/:artifactid is in place; /content/<hexid> or /content/<hexhash> is coming; why the : difference?
			Response response =	this.executor.execute(request); // submits the request and collects the response
			String retString = response.returnContent().asString();
			return retString;
		} catch (IOException e) {
			throw new Exception(e);
		}
	}
	
}

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