package org.eclipse.jetty.uriqa.example;

import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.uriqa.UriqaHandler;
import org.eclipse.jetty.uriqa.stat.Messages;
import org.eclipse.jetty.uriqa.stat.UriqaConfig;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;
import org.eclipse.jetty.util.resource.Resource;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;

/**
 * @author venkatesh
 * @version $Id$
 */
public class TestServer
{
    /**
     * Starts a new Jetty Server with {@link UriqaHandler}. <br /> {@link StdErrLog} is the {@link Logger} with debug values
     * set by URIQA_DEBUG Port: 8080
     * 
     * @param args Configuration.xml file if provided is the only argument (optional)
     */
    public static void main(String[] args)
    {
        /**
         * By default, for being compatible with java name-classes, _ is replaced with __. To remove this, we are using
         * the XMLFriendlyReplacer
         */
        XmlFriendlyReplacer replacer = new XmlFriendlyReplacer("_-", "_");
        /**
         * XStream is being used for easy parsing and conversion of xml to POJO's.
         */
        XStream xstream = new XStream(new DomDriver("UTF-8", replacer));
        xstream.alias("config", UriqaConfig.class);
        UriqaConfig config = null;
        try {
            Resource news;
            if (args.length > 0) {
                /*
                 * Argument is present
                 */
                news = Resource.newSystemResource(args[1]);
            } else {
                /*
                 * Default configuration file.
                 */
                news = Resource.newSystemResource("org/eclipse/jetty/uriqa/configuration.xml");
            }
            if (news != null) {
                /*
                 * Resource successfully read.
                 */
                config = (UriqaConfig) xstream.fromXML(news.getInputStream());
                /*
                 * Set Debug to true or false using configuration file.
                 */
                System.setProperty("org.eclipse.jetty.util.log.DEBUG", String.valueOf(config.isURIQA_DEBUG()));
                /*
                 * Setting Logger.
                 */
                Log.setLog(new StdErrLog());
                if (Log.isDebugEnabled())
                    Log.debug("Using configuration.xml");
            } else {
                /*
                 * Set Debug to true or false using URIQA_DEBUG of messages.properties
                 */
                if (Boolean.parseBoolean(Messages.getString("URIQA_DEBUG")))
                    System.setProperty("org.eclipse.jetty.util.log.DEBUG", "true");
                /*
                 * Setting Logger.
                 */
                Log.setLog(new StdErrLog());
                if (Log.isDebugEnabled())
                    Log.debug("Using messages.properties");
            }
        } catch (IOException e) {
            System.out.println("Usage: java TestServer /home/user/configuration.xml");
            e.printStackTrace();
        }

        /*
         * Port set here.
         */
        Server server = new Server(8080);
        /*
         * Setting UriqaHandler
         */
        server.setHandler(new UriqaHandler(config));
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
