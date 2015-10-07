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
