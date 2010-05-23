package org.eclipse.jetty.uriqa;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class UriqaHandler extends AbstractHandler {

	private String baseURI = null;
	HashMap<String, String> paramMap = new HashMap<String, String>(3);
	//getServer().

	public UriqaHandler() {
		paramMap.put(UriqaConstants.Parameters.FORMAT, UriqaConstants.Lang.RDFXML);
		paramMap.put(UriqaConstants.Parameters.NAMING, UriqaConstants.Values.LABEL);
		paramMap.put(UriqaConstants.Parameters.INFERENCE, UriqaConstants.Values.EXC);
	}

	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {

		if (baseRequest.isHandled())
			return;

		if (baseURI == null) {
			baseURI = baseRequest.getScheme()+"://"+baseRequest.getServerName();
			UriqaRepoHandler.getDefault(baseURI);
		}
		//TODO use UriqaMethods ENUM Matching. Better DS required probably.

		if (request.getHeader(UriqaConstants.Parameters.URI) != null)
		{
			URL uri = new URL(request.getHeader(UriqaConstants.Parameters.URI));
			baseRequest.setPathInfo(uri.getPath());
		}
		if (request.getHeader(UriqaConstants.Parameters.FORMAT) != null) {
			paramMap.remove(UriqaConstants.Parameters.FORMAT);
			paramMap.put(UriqaConstants.Parameters.FORMAT,
					request.getHeader(UriqaConstants.Parameters.FORMAT).equals(UriqaConstants.Values.RDFXML) ? UriqaConstants.Lang.RDFXML : request.getHeader(UriqaConstants.Parameters.FORMAT));
		}
		if (request.getHeader(UriqaConstants.Parameters.NAMING) != null) {
			paramMap.remove(UriqaConstants.Parameters.NAMING);
			paramMap.put(UriqaConstants.Parameters.NAMING, request.getHeader(UriqaConstants.Parameters.NAMING));
		}
		if (request.getHeader(UriqaConstants.Parameters.INFERENCE) != null) {
			paramMap.remove(UriqaConstants.Parameters.INFERENCE);
			paramMap.put(UriqaConstants.Parameters.INFERENCE, request.getHeader(UriqaConstants.Parameters.INFERENCE));
		}
		UriqaRepoHandler.getDefault().handleRequest(request,response, baseRequest.getMethod(), paramMap);
		//UriqaRepoHandler.getDefault().printModeltoConsole();

		if(baseRequest.getMethod().equals(UriqaConstants.Methods.MGET) || baseRequest.getMethod().equals(UriqaConstants.Methods.MPUT)
				|| baseRequest.getMethod().equals(UriqaConstants.Methods.MDELETE) || baseRequest.getMethod().equals(UriqaConstants.Methods.MQUERY) || baseRequest.getMethod().equals(UriqaConstants.Methods.MTRACE)) {
			
			((Request)request).setHandled(true);
			baseRequest.setHandled(true);
		}
	}
}
