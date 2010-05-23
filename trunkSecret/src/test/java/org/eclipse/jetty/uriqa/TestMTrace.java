package org.eclipse.jetty.uriqa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

public class TestMTrace {

	public static void main(String[] args) {

		try {
			String query = "PREFIX	dc:	<http://purl.org/dc/elements/1.1/>\r\n" +
			"PREFIX	rdf:	<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n" +
			"PREFIX	rdfs:	<http://www.w3.org/2000/01/rdf-schema#>\r\n" +
			"\r\n" +
			"TRACE\r\n" +
			"dc:title	rdf:type	rdfs:Resource\r\n" +
			"\r\n";
			String hostname = "localhost";
			int port = 8080;
			InetAddress  addr = InetAddress.getByName(hostname);
			Socket sock = new Socket(addr, port);
			String path = "/doesntMatter";
			BufferedWriter  wr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(),"UTF-8"));
			wr.write("MTRACE " + path + " HTTP/1.1\r\n");
			wr.write("Host: localhost:8080\r\n");
			wr.write("Content-Length: " + query.length() + "\r\n");
			wr.write("Content-Type: text/xml; charset=\"utf-8\"\r\n");
			wr.write("\r\n");

			wr.write(query);
			wr.flush();

			BufferedReader rd = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			String line;
			while((line = rd.readLine()) != null)
				System.out.println(line);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

