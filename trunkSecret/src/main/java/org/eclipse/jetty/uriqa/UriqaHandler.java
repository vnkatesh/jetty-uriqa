package org.eclipse.jetty.uriqa;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;
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
		if (baseURI == null) {
			baseURI = baseRequest.getScheme()+"://"+baseRequest.getServerName();
			UriqaRepoHandler.getDefault(baseURI);
		}
		System.out.println("****************Handler***************");
		//System.out.println("getLocalHost(): "+baseRequest.getConnection().getEndPoint().getLocalHost());
		//TODO use UriqaMethods ENUM Matching.
		//TODO what about MSEARCH or something? inferencing or querying model??
		//TODO MQUERY? -> should return the query element names/id's or something. -> which the browser asks back if required.
		if(baseRequest.getMethod().equals(UriqaConstants.Methods.MGET) || baseRequest.getMethod().equals(UriqaConstants.Methods.MPUT)
				|| baseRequest.getMethod().equals(UriqaConstants.Methods.MDELETE) || baseRequest.getMethod().equals(UriqaConstants.Methods.MQUERY))
		{
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(MimeTypes.TEXT_XML);
			UriqaRepoHandler.getDefault().handleRequest(request,response, baseRequest.getMethod());
			//UriqaRepoHandler.getDefault().printModeltoConsole();
			//response should be content type of mime binary data or ascii-n3-notations.
			//TODO Repository in a filesystem??
			//TODO Or is it baseRequest.setHandled()??
			((Request)request).setHandled(true);
			baseRequest.setHandled(true);
			//TODO check after installing other Handlers also..
		}
		//TODO How should the content body structure be like? And how shall I put it as the content? Inputstream?
		//baseRequest.getReader()
		//request.getReader()
		//just code if fail or not -> check uriqa definition pages again.

	}

}
