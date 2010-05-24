package org.eclipse.jetty.uriqa;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;

/**
 * @author venkatesh
 * @version $Id$
 */
public class TestServer
{
    /**
     * Starts a new Jetty Server with UriqaHandler(). <br />
     * StdErrLog() is the Logger with debug values set by URIQA_DEBUG Port: 8080
     * 
     * @param args Not Used.
     */
    public static void main(String[] args)
    {
        /*
         * Set Debug to true or false using URIQA_DEBUG of messages.properties
         */
        if ((new Boolean(Messages.getString("URIQA_DEBUG"))).booleanValue())
            System.setProperty("org.eclipse.jetty.util.log.DEBUG", "true");
        /*
         * Setting Logger.
         */
        Log.setLog(new StdErrLog());
        /*
         * Port set here.
         */
        Server server = new Server(8080);
        /*
         * Setting UriqaHandler
         */
        server.setHandler(new UriqaHandler());
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
