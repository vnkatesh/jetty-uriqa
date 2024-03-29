/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
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
import org.eclipse.jetty.uriqa.stat.UriqaConfig;
import org.eclipse.jetty.uriqa.stat.UriqaConstants;
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

    /*
     * TODO Change license to something appropriate. And apply to all source files. TODO Check for new java docs to be
     * added and log.debugs to be added from june 13th commit onwards
     */

    /**
     * baseURI to be set at {@link UriqaRepoHandler}
     */
    private static String baseURI = null;

    /**
     * The {@link UriqaConfig} configuration stored and used by {@link UriqaRepoHandler}.
     */
    private static UriqaConfig config = null;

    /**
     * Parameter Map from {@link Request} headers.
     */
    HashMap<String, String> paramMap = new HashMap<String, String>(3);

    /**
     * Constructor. Sets default {@link UriqaHandler#paramMap} values.
     * 
     * @param config
     */
    public UriqaHandler(UriqaConfig config)
    {
        if (Log.isDebugEnabled())
            Log.debug("UriqaHandler(config): " + ((config == null) ? "null" : config.toString()));
        if (config != null)
            UriqaHandler.config = config;
        setParamMapDefault();
    }

    /**
     * Sets the base default parameters. Called with every call of handle()
     */
    private void setParamMapDefault()
    {
        if (Log.isDebugEnabled())
            Log.debug("setParamMapDefault()");
        paramMap.clear();
        paramMap.put(UriqaConstants.Parameters.FORMAT, UriqaConstants.Lang.RDFXML);
        paramMap.put(UriqaConstants.Parameters.NAMING, UriqaConstants.Values.LABEL);
        paramMap.put(UriqaConstants.Parameters.INFERENCE, UriqaConstants.Values.EXC);
    }

    /**
     * The constuctor if {@link UriqaConfig} is not available. Calls {@link UriqaHandler#UriqaHandler(UriqaConfig)} with
     * null parameter.
     */
    public UriqaHandler()
    {
        this(null);
        if (Log.isDebugEnabled())
            Log.debug("UriqaHandler()");
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
            UriqaRepoHandler.getDefault(baseURI, config);
            if (Log.isDebugEnabled())
                Log.debug("handle():baseURI:: " + baseURI);
        }

        if (Log.isDebugEnabled())
            Log.debug("****************Handler***************");

        /*
         * Setting base Parameters.
         */
        setParamMapDefault();

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
                Log.debug("handle():setHandled()");
        }
    }
}
