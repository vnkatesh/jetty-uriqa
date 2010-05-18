package org.eclipse.jetty.uriqa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

public class TestMQuery {

	public static void main(String[] args) {

		try {
			String query = "PREFIX dc:	<http://purl.org/dc/elements/1.1/>\r\n" +
			"\r\n" +
			"SELECT ?book ?author\r\n" +
			"WHERE\r\n" +
			"{ ?book dc:creator ?author .\r\n" +
			"}\r\n";
			String hostname = "localhost";
			int port = 8080;
			InetAddress  addr = InetAddress.getByName(hostname);
			Socket sock = new Socket(addr, port);
			String path = "/doesntMatter";
			BufferedWriter  wr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(),"UTF-8"));
			wr.write("MQUERY " + path + " HTTP/1.0\r\n");
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

