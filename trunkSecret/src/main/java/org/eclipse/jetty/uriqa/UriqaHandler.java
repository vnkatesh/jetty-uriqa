package org.eclipse.jetty.uriqa;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * The main handler which extends {@link AbstractHandler} <br /> {@link Server} has a {@link QueuedThreadPool} implementation.
 * 
 * @author venkatesh
 * @version $Id$
 */
public class UriqaHandler extends AbstractHandler
{

    /**
     * baseURI to be set at {@link UriqaRepoHandler}
     */
    private String baseURI = null;

    /**
     * Parameter Map from {@link Request} headers.
     */
    HashMap<String, String> paramMap = new HashMap<String, String>(3);

    /**
     * Constructor. Sets default {@link UriqaHandler#paramMap} values.
     */
    public UriqaHandler()
    {
        paramMap.put(UriqaConstants.Parameters.FORMAT, UriqaConstants.Lang.RDFXML);
        paramMap.put(UriqaConstants.Parameters.NAMING, UriqaConstants.Values.LABEL);
        paramMap.put(UriqaConstants.Parameters.INFERENCE, UriqaConstants.Values.EXC);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jetty.server.Handler#handle(java.lang.String, org.eclipse.jetty.server.Request,
     *      javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException
    {

        /*
         * If handled already, return the Request.
         */
        if (baseRequest.isHandled())
            return;

        /*
         * Set the baseURI, the first time it is called.
         */
        if (baseURI == null) {
            baseURI = baseRequest.getScheme() + "://" + baseRequest.getServerName();
            UriqaRepoHandler.getDefault(baseURI);
            if (Log.isDebugEnabled())
                Log.debug("handle():baseURI:: " + baseURI);
        }

        if (Log.isDebugEnabled())
            Log.debug("****************Handler***************");

        /*
         * Set paramMap values for corresponding headers if present.
         */
        if (request.getHeader(UriqaConstants.Parameters.URI) != null) {
            URL uri = new URL(request.getHeader(UriqaConstants.Parameters.URI));
            baseRequest.setPathInfo(uri.getPath());
            if (Log.isDebugEnabled())
                Log.debug("handle():URI:: " + request.getHeader(UriqaConstants.Parameters.URI));
        }
        if (request.getHeader(UriqaConstants.Parameters.FORMAT) != null) {
            paramMap.remove(UriqaConstants.Parameters.FORMAT);
            paramMap.put(UriqaConstants.Parameters.FORMAT, request.getHeader(UriqaConstants.Parameters.FORMAT).equals(
                UriqaConstants.Values.RDFXML) ? UriqaConstants.Lang.RDFXML : request
                .getHeader(UriqaConstants.Parameters.FORMAT));
            if (Log.isDebugEnabled())
                Log.debug("handle():FORMAT:: " + request.getHeader(UriqaConstants.Parameters.FORMAT));
        }
        if (request.getHeader(UriqaConstants.Parameters.NAMING) != null) {
            paramMap.remove(UriqaConstants.Parameters.NAMING);
            paramMap.put(UriqaConstants.Parameters.NAMING, request.getHeader(UriqaConstants.Parameters.NAMING));
            if (Log.isDebugEnabled())
                Log.debug("handle():NAMING:: " + request.getHeader(UriqaConstants.Parameters.NAMING));
        }
        if (request.getHeader(UriqaConstants.Parameters.INFERENCE) != null) {
            paramMap.remove(UriqaConstants.Parameters.INFERENCE);
            paramMap.put(UriqaConstants.Parameters.INFERENCE, request.getHeader(UriqaConstants.Parameters.INFERENCE));
            if (Log.isDebugEnabled())
                Log.debug("handle():INFERENCE:: " + request.getHeader(UriqaConstants.Parameters.INFERENCE));
        }
        /*
         * Passing the request to UriqaRepoHandler's Default instance.
         */
        UriqaRepoHandler.getDefault().handleRequest(request, response, baseRequest.getMethod(), paramMap);
        if (Log.isDebugEnabled())
            Log.debug("handle():Model Now is:: \r\n" + UriqaRepoHandler.getDefault().printModeltoConsole());

        /*
         * Set the request as handled if the method is one of MGET/MPUT/MDELETE/MQUERY/MTRACE
         */
        if (UriqaConstants.Methods.map.containsValue(baseRequest.getMethod())) {

            ((Request) request).setHandled(true);
            baseRequest.setHandled(true);
            if (Log.isDebugEnabled())
                Log.debug("handle():setHanlded()");
        }
    }
}
