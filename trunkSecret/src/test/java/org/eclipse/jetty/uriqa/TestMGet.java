package org.eclipse.jetty.uriqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.uriqa.stat.UriqaConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestMGet
{
    private static Server _server;

    @BeforeClass
    public static void startServer()
    {
        UriqaConfig _config =
            new UriqaConfig("http://localhost", "/home/venkatesh/", "-//W3C//DTD XHTML+RDFa 1.0//EN",
                "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd", "UTF8", "Reader Error", false, true,
                "org/eclipse/jetty/uriqa/w3.rdf", false, "OWL_MEM_MINI_RULE_INF");
        _server = new Server(8080);
        _server.setHandler(new UriqaHandler(_config));
        try {
            _server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @AfterClass
    public static void stopServer()
    {
        try {
            _server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public static void test404() throws IOException
    {
        String hostname = "localhost";
        int port = 8080;
        InetAddress addr = InetAddress.getByName(hostname);
        Socket sock = new Socket(addr, port);
        String path = "/shouldnotExist";
        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), "UTF-8"));
        wr.write("MGET " + path + " HTTP/1.1\r\n");
        wr.write("Host: localhost:8080\r\n");
        wr.write("Connection: close\r\n");
        wr.write("\r\n");
        wr.flush();

        BufferedReader rd = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        String line;

        line = rd.readLine();
        assertEquals("HTTP/1.1 404 Not Found", line);

        line = rd.readLine();
        assertEquals("Content-Length: 0", line);
        while ((line = rd.readLine()) != null)
            ;
    }

    @Test
    public static void HTMLTest() throws IOException
    {
        String hostname = "localhost";
        int port = 8080;
        InetAddress addr = InetAddress.getByName(hostname);
        Socket sock = new Socket(addr, port);
        String path = "/aReallyGreatBook";
        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), "UTF-8"));
        wr.write("MGET " + path + " HTTP/1.1\r\n");
        wr.write("Host: localhost:8080\r\n");
        wr.write("format: text/html\r\n");
        wr.write("Connection: close\r\n");
        wr.write("\r\n");
        wr.flush();

        BufferedReader rd = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        String line;

        line = rd.readLine();
        assertEquals("HTTP/1.1 200 OK", line);

        line = rd.readLine();
        assertEquals("Content-Type: text/html;charset=UTF-8", line);

        boolean html_headers = false;
        while ((line = rd.readLine()) != null) {
            if (line
                .equals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE rdf:RDF PUBLIC \"-//W3C//DTD XHTML+RDFa 1.0//EN\" \"http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd\">"))
                html_headers = true;
        }
        assertTrue(html_headers);
    }

    @Test
    public static void FormatTest() throws IOException
    {
        String hostname = "localhost";
        int port = 8080;
        InetAddress addr = InetAddress.getByName(hostname);
        Socket sock = new Socket(addr, port);
        String path = "/aReallyGreatBook";
        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), "UTF-8"));
        wr.write("MGET " + path + " HTTP/1.1\r\n");
        wr.write("Host: localhost:8080\r\n");
        wr.write("format: TURTLE\r\n");
        wr.write("Connection: close\r\n");
        wr.write("\r\n");
        wr.flush();

        BufferedReader rd = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        String line;

        line = rd.readLine();
        assertEquals("HTTP/1.1 200 OK", line);

        line = rd.readLine();
        assertEquals("Content-Type: TURTLE", line);

        while ((line = rd.readLine()) != null)
            ;
    }
}
