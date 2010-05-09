package org.eclipse.jetty.uriqa;

import org.eclipse.jetty.server.Server;

public class TestServer {
	public static void main( String[] args )
	{
		Server server = new Server(8080);
		server.setHandler(new UriqaHandler());
		try {
			server.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
