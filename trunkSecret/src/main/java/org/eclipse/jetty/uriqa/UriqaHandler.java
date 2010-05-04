package org.eclipse.jetty.uriqa;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class UriqaHandler extends AbstractHandler {

	private String baseURI = null;
	//getServer().
	
	public UriqaHandler() {
		
	}

	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
//		if (baseURI == null) {
//			baseURI = baseRequest.getScheme()+"://"+baseRequest.getServerName();
//			UriqaRepoHandler.getDefault(baseURI);
//		}
//		System.out.println("now the handlers are:: ");
//		for (Handler handler : getServer().getHandlers())
//		{
//			System.out.println("handler name: "+handler.toString());
//		}
		System.out.println("****************Handler***************");
		//System.out.println("baseURI: "+baseURI);
		//System.out.println("getLocalHost(): "+baseRequest.getConnection().getEndPoint().getLocalHost());
		//TODO use UriqaMethods ENUM Matching.
		//TODO what about MSEARCH or something? inferencing or querying model??
		//TODO MQUERY? -> should return the query element names/id's or something. -> which the browser asks back if required.
		if(baseRequest.getMethod().equals(UriqaConstants.Methods.MGET))
		{
			System.out.println("MGET");
			//System.out.println("and the HttpServletRequest getMethod() gives: "+request.getMethod());
			//response.setStatus(HttpServletResponse.SC_OK);
			//response.setContentType(MimeTypes.TEXT_XML);
			//UriqaRepoHandler.getDefault().handleMget(request.getPathInfo().startsWith("/")?request.getPathInfo():"/"+request.getPathInfo(),response.getWriter());
			//UriqaRepoHandler.getDefault().printModeltoOutput();
			//response should be content type of mime binary data or ascii-n3-notations.
			//handleMget static call.
			// Mget (object name, response??)
			//TODO Create the Basic Handler via extending AbstractHandler.
			//TODO Then, think of thread and implementation of runnable
			//TODO then, think of repository
			//TODO then, think of how they shall interact.
			
			((Request)request).setHandled(true);
			baseRequest.setHandled(true);
			//TODO Or is it baseRequest.setHandled()??
			//TODO check after installing other Handlers also..
		}
		if(baseRequest.getMethod().equals(UriqaConstants.Methods.MPUT))
		{
			System.out.println("MPUT");
			//there should be content body here
			//TODO How should the content body structure be like? And how shall I put it as the content? Inputstream?
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(MimeTypes.TEXT_XML);
			UriqaRepoHandler.getDefault().handleMput(request.getPathInfo().startsWith("/")?request.getPathInfo():"/"+request.getPathInfo(),request);
			//baseRequest.getReader()
			//request.getReader()
			//handleMput static call
			((Request)request).setHandled(true);
		}
		if(baseRequest.getMethod().equals(UriqaConstants.Methods.MDELETE))
		{
			System.out.println("MDELETE");
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(MimeTypes.TEXT_XML);
			UriqaRepoHandler.getDefault().handleMdelete(request.getPathInfo().startsWith("/")?request.getPathInfo():"/"+request.getPathInfo());
			//no content body
			//no response body
			//just code if fail or not -> check uriqa definition pages again.
			//handleMdelete static call
			((Request)request).setHandled(true);
			baseRequest.setHandled(true);
		}
		//System.out.println()
		//System.out.println("VENKATESH DEBUG: target: "+target+" baseMethod: "+baseRequest.getMethod());
        //response.setContentType("text/html");
        //response.setStatus(HttpServletResponse.SC_OK);
        //response.getWriter().println("<h1>Hello</h1>");
        
		
	}

}
