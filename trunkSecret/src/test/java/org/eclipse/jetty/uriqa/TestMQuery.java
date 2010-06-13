package org.eclipse.jetty.uriqa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.UpdateAction;

/**
 * Java Application to test MQUERY. <br />
 * Creates query, connects to {@link Server}, writes data, reads {@link Response} and prints to {@link System#out} <br />
 * query.length() can be easily determined and set as content-length header <br />
 * query here is a SELECT query with set PREFIX, given variables and WHERE conditions. <br />
 * Expected output is a {@link MimeTypes#TEXT_HTML} or {@link MimeTypes#TEXT_JSON} output for the {@link Query} if ( <br />
 * or a {@link Model#write(java.io.OutputStream, String)} for @link {@link Query#isConstructType()} or @link
 * {@link Query#isDescribeType()} <br />
 * or an {@link UpdateAction} {@link Query} may just execute the query returning just {@link Response} Headers.
 * 
 * @author venkatesh
 * @version $Id$
 */
public class TestMQuery
{

    public static void main(String[] args)
    {

        try {
            String query =
                "PREFIX dc:	<http://purl.org/dc/elements/1.1/>\r\n" + "\r\n" + "SELECT ?book ?author\r\n" + "WHERE\r\n"
                    + "{ ?book dc:creator ?author .\r\n" + "}\r\n";
            String hostname = "localhost";
            int port = 8080;
            InetAddress addr = InetAddress.getByName(hostname);
            Socket sock = new Socket(addr, port);
            String path = "/doesntMatter";
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), "UTF-8"));
            wr.write("MQUERY " + path + " HTTP/1.1\r\n");
            wr.write("Host: localhost:8080\r\n");
            wr.write("Content-Length: " + query.length() + "\r\n");
            wr.write("Content-Type: text/xml; charset=\"utf-8\"\r\n");
            // TODO inferencing is creating problems?? INFERENCING KILLS!! MAJOR BUG!!
            // wr.write("inference: include\r\n");
            wr.write("format: text/json\r\n");
            wr.write("\r\n");

            wr.write(query);
            wr.flush();

            BufferedReader rd = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null)
                System.out.println(line);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
