package com.pslcl.qa.platform;

//import java.io.IOException;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import org.eclipse.jetty.server.Request;
//import org.eclipse.jetty.server.handler.AbstractHandler;
//
//public class ExampleHandler extends AbstractHandler {
//    
//    final String greeting;
//    final String body;
// 
//    public ExampleHandler()
//    {
//        this("Hello World from 'ExampleHandler')");
//    }
// 
//    public ExampleHandler( String greeting )
//    {
//        this(greeting, null);
//    }
// 
//    public ExampleHandler( String greeting, String body )
//    {
//        this.greeting = greeting;
//        this.body = body;
//    }
// 
//    public void handle( String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException
//    {
//        response.setContentType("text/html; charset=utf-8");
//        response.setStatus(HttpServletResponse.SC_OK);
//        response.getWriter().println("<h1>" + greeting + "</h1>");
//        if (body != null)
//            response.getWriter().println(body);
//        baseRequest.setHandled(true);
//    }
//}
