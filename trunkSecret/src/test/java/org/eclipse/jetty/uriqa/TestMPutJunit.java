package org.eclipse.jetty.uriqa;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.uriqa.stat.UriqaConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @version $Id$
 */
public class TestMPutJunit
{
    private Server _server;

    @Before
    public void startServer()
    {
        UriqaConfig _config =
            new UriqaConfig("http://localhost", "/home/venkatesh/", "-//W3C//DTD XHTML+RDFa 1.0//EN",
                "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd", "UTF8", "Reader Error", false, false, "", false,
                "OWL_MEM_MINI_RULE_INF");
        _server = new Server(8080);
        _server.setHandler(new UriqaHandler(_config));
        try {
            _server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @After
    public void stopServer()
    {
        try {
            _server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void PutTest() throws IOException
    {
        String xmldata =
            "<?xml version=\"1.0\"?>\r\n"
                + "<rdf:RDF\r\n"
                + "   xmlns:rdf  =\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\r\n"
                + "   xmlns:rdfs =\"http://www.w3.org/2000/01/rdf-schema#\"\r\n"
                + "   xmlns:owl  =\"http://www.w3.org/2002/07/owl#\"\r\n"
                + "   xmlns:dc   =\"http://purl.org/dc/elements/1.1/\"\r\n"
                + "   xmlns:dct  =\"http://purl.org/dc/terms/\"\r\n"
                + "   xmlns:xsd  =\"http://www.w3.org/2001/XMLSchema#\"\r\n"
                + "   xmlns:foaf =\"http://xmlns.com/foaf/0.1/\"\r\n"
                + "   xmlns:ex   =\"http://localhost/\">\r\n"
                + "   <rdf:Description rdf:about=\"http://localhost/aReallyGreatBook\">\r\n"
                + "      <dc:title>A Really Great Book</dc:title>\r\n"
                + "      <dc:publisher>Examples-R-Us</dc:publisher>\r\n"
                + "      <dc:creator>\r\n"
                + "         <rdf:Description>\r\n"
                + "            <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>\r\n"
                + "            <foaf:name>John Doe</foaf:name>\r\n"
                + "            <foaf:mbox>john@localhost</foaf:mbox>\r\n"
                + "            <foaf:img>\r\n"
                + "               <rdf:Description rdf:about=\"http://localhost/john.jpg\">\r\n"
                + "                  <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Image\"/>\r\n"
                + "                  <dc:format>image/jpeg</dc:format>\r\n"
                + "                  <dc:extent>1234</dc:extent>\r\n"
                + "               </rdf:Description>\r\n"
                + "            </foaf:img>\r\n"
                + "            <foaf:phone rdf:resource=\"tel:+1-999-555-1234\"/>\r\n"
                + "         </rdf:Description>\r\n"
                + "      </dc:creator>\r\n"
                + "      <dc:contributor>\r\n"
                + "         <rdf:Description>\r\n"
                + "            <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>\r\n"
                + "            <foaf:name>Jane Doe</foaf:name>\r\n"
                + "         </rdf:Description>\r\n"
                + "      </dc:contributor>\r\n"
                + "      <dc:language>en</dc:language>\r\n"
                + "      <dc:format>application/pdf</dc:format>\r\n"
                + "      <dc:rights>Copyright (C) 2004 Examples-R-Us. All rights reserved.</dc:rights>\r\n"
                + "      <dct:issued rdf:datatype=\"http://www.w3.org/2001/XMLSchema#date\">2004-01-19</dct:issued>\r\n"
                + "      <rdfs:seeAlso rdf:resource=\"http://localhost/anotherGreatBook\"/>\r\n"
                + "   </rdf:Description>\r\n"
                + "   <rdf:Statement>\r\n"
                + "      <rdf:subject rdf:resource=\"http://localhost/aReallyGreatBook\"/>\r\n"
                + "      <rdf:predicate rdf:resource=\"http://purl.org/dc/elements/1.1/format\"/> \r\n"
                + "      <rdf:object>application/pdf</rdf:object>\r\n"
                + "      <rdfs:isDefinedBy rdf:resource=\"http://localhost/book-formats.rdf\"/>\r\n"
                + "   </rdf:Statement>\r\n"
                + "   <rdf:Statement>\r\n"
                + "      <rdf:subject rdf:resource=\"http://xmlns.com/foaf/0.1/Image\"/>\r\n"
                + "      <rdf:predicate rdf:resource=\"http://purl.org/dc/elements/1.1/format\"/> \r\n"
                + "      <rdf:object>image/jpeg</rdf:object>\r\n"
                + "      <rdfs:isDefinedBy rdf:resource=\"http://localhost/image-formats.rdf\"/>\r\n"
                + "   </rdf:Statement>\r\n"
                + "   <rdf:Description rdf:about=\"http://localhost/anotherGreatBook\">\r\n"
                + "      <dc:title>Another Great Book</dc:title>\r\n"
                + "      <dc:publisher>Examples-R-Us</dc:publisher>\r\n"
                + "      <dc:creator>June Doe (june@localhost)</dc:creator>\r\n"
                + "      <dc:format>application/pdf</dc:format>\r\n"
                + "      <dc:language>en</dc:language>\r\n"
                + "      <dc:rights>Copyright (C) 2004 Examples-R-Us. All rights reserved.</dc:rights>\r\n"
                + "      <dct:issued rdf:datatype=\"http://www.w3.org/2001/XMLSchema#date\">2004-05-03</dct:issued>\r\n"
                + "      <rdfs:seeAlso rdf:resource=\"http://localhost/aReallyGreatBook\"/>\r\n"
                + "   </rdf:Description>\r\n" + "   <rdf:Description rdf:about=\"http://localhost/aBookCritic\">\r\n"
                + "      <ex:likes rdf:resource=\"http://localhost/aReallyGreatBook\"/>\r\n"
                + "      <ex:dislikes rdf:resource=\"http://localhost/anotherGreatBook\"/>\r\n"
                + "   </rdf:Description>\r\n" + "   <rdf:Property rdf:about=\"http://xmlns.com/foaf/0.1/mbox\">\r\n"
                + "      <rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#InverseFunctionalProperty\"/>\r\n"
                + "   </rdf:Property>\r\n" + "</rdf:RDF>\r\n";
        String hostname = "localhost";
        int port = 8080;
        InetAddress addr = InetAddress.getByName(hostname);
        Socket sock = new Socket(addr, port);
        String path = "/doesntMatter";
        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), "UTF-8"));
        wr.write("MPUT " + path + " HTTP/1.1\r\n");
        wr.write("Host: localhost:8080\r\n");
        wr.write("Content-Length: " + xmldata.length() + "\r\n");
        wr.write("Content-Type: text/xml; charset=\"utf-8\"\r\n");
        wr.write("Connection: close\r\n");
        wr.write("\r\n");

        wr.write(xmldata);
        wr.flush();

        BufferedReader rd = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        String line;

        line = rd.readLine();
        assertEquals("HTTP/1.1 204 No Content", line);

        line = rd.readLine();
        assertEquals("Content-Length: 0", line);

        while ((line = rd.readLine()) != null)
            ;
    }

    @Test
    public void retrieveTest() throws IOException
    {
        PutTest();
        TestMGet.test404();
        TestMGet.FormatTest();
        TestMGet.HTMLTest();
    }

    @Test
    public void TurtleInputTest() throws IOException
    {
        String xmldata =
            "@prefix dc:      <http://purl.org/dc/elements/1.1/> .\r\n"
                + "@prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#> .\r\n"
                + "@prefix foaf:    <http://xmlns.com/foaf/0.1/> .\r\n"
                + "@prefix dct:     <http://purl.org/dc/terms/> .\r\n"
                + "@prefix xsd:     <http://www.w3.org/2001/XMLSchema#> .\r\n"
                + "@prefix owl:     <http://www.w3.org/2002/07/owl#> .\r\n"
                + "@prefix rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\r\n"
                + "@prefix localhost:  <http://localhost> .\r\n" + "\r\n" + "foaf:mbox\r\n"
                + "      a       rdf:Property , owl:InverseFunctionalProperty .\r\n" + "\r\n"
                + "[]    a       rdf:Statement ;\r\n" + "      rdf:object \"image/jpeg\" ;\r\n"
                + "      rdf:predicate dc:format ;\r\n" + "      rdf:subject foaf:Image ;\r\n"
                + "      rdfs:isDefinedBy <http://localhost/image-formats.rdf> .\r\n" + "\r\n"
                + "<http://localhost/aBookCritic>\r\n" + "      <http://localhost/dislikes>\r\n"
                + "              <http://localhost/anotherGreatBook> ;\r\n" + "      <http://localhost/likes>\r\n"
                + "              <http://localhost/aReallyGreatBook> .\r\n" + "\r\n"
                + "<http://localhost/aReallyGreatBook>\r\n"
                + "      rdfs:seeAlso <http://localhost/anotherGreatBook> ;\r\n" + "      dc:contributor\r\n"
                + "              [ a       foaf:Person ;\r\n" + "                foaf:name \"Jane Doe\"\r\n"
                + "              ] ;\r\n" + "      dc:creator\r\n" + "              [ a       foaf:Person ;\r\n"
                + "                foaf:img <http://localhost/john.jpg> ;\r\n"
                + "                foaf:mbox \"john@localhost\" ;\r\n" + "                foaf:name \"John Doe\" ;\r\n"
                + "                foaf:phone <tel:+1-999-555-1234>\r\n" + "              ] ;\r\n"
                + "      dc:format \"application/pdf\" ;\r\n" + "      dc:language \"en\" ;\r\n"
                + "      dc:publisher \"Examples-R-Us\" ;\r\n"
                + "      dc:rights \"Copyright (C) 2004 Examples-R-Us. All rights reserved.\" ;\r\n"
                + "      dc:title \"A Really Great Book\" ;\r\n" + "      dct:issued \"2004-01-19\"^^xsd:date .\r\n"
                + "\r\n" + "[]    a       rdf:Statement ;\r\n" + "      rdf:object \"application/pdf\" ;\r\n"
                + "      rdf:predicate dc:format ;\r\n" + "      rdf:subject <http://localhost/aReallyGreatBook> ;\r\n"
                + "      rdfs:isDefinedBy <http://localhost/book-formats.rdf> .\r\n" + "\r\n"
                + "<http://localhost/john.jpg>\r\n" + "      a       foaf:Image ;\r\n"
                + "      dc:extent \"1234\" ;\r\n" + "      dc:format \"image/jpeg\" .\r\n" + "\r\n"
                + "<http://localhost/anotherGreatBook>\r\n"
                + "      rdfs:seeAlso <http://localhost/aReallyGreatBook> ;\r\n"
                + "      dc:creator \"June Doe (june@localhost)\" ;\r\n" + "      dc:format \"application/pdf\" ;\r\n"
                + "      dc:language \"en\" ;\r\n" + "      dc:publisher \"Examples-R-Us\" ;\r\n"
                + "      dc:rights \"Copyright (C) 2004 Examples-R-Us. All rights reserved.\" ;\r\n"
                + "      dc:title \"Another Great Book\" ;\r\n" + "      dct:issued \"2004-05-03\"^^xsd:date .\r\n";
        String hostname = "localhost";
        int port = 8080;
        InetAddress addr = InetAddress.getByName(hostname);
        Socket sock = new Socket(addr, port);
        String path = "/doesntMatter";
        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), "UTF-8"));
        wr.write("MPUT " + path + " HTTP/1.1\r\n");
        wr.write("Host: localhost:8080\r\n");
        wr.write("Content-Length: " + xmldata.length() + "\r\n");
        wr.write("Content-Type: text/xml; charset=\"utf-8\"\r\n");
        wr.write("Connection: close\r\n");
        wr.write("format: TURTLE\r\n");
        wr.write("\r\n");

        wr.write(xmldata);
        wr.flush();

        BufferedReader rd = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        String line;

        line = rd.readLine();
        assertEquals("HTTP/1.1 204 No Content", line);

        line = rd.readLine();
        assertEquals("Content-Length: 0", line);

        while ((line = rd.readLine()) != null)
            ;
        TestMGet.HTMLTest();
        TestMGet.test404();
        TestMGet.FormatTest();
    }

    @Test
    public void ConflictTest() throws IOException
    {
        PutTest();
        String xmldata =
            "@prefix foaf:    <http://xmlns.com/foaf/0.1/> .\r\n" + "@prefix localhost:  <http://localhost> .\r\n"
                + "\r\n" + "<http://localhost/aReallyGreatBook>\r\n" + "      a foaf:Person .\r\n";
        String hostname = "localhost";
        int port = 8080;
        InetAddress addr = InetAddress.getByName(hostname);
        Socket sock = new Socket(addr, port);
        String path = "/doesntMatter";
        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), "UTF-8"));
        wr.write("MPUT " + path + " HTTP/1.1\r\n");
        wr.write("Host: localhost:8080\r\n");
        wr.write("Content-Length: " + xmldata.length() + "\r\n");
        wr.write("Content-Type: text/xml; charset=\"utf-8\"\r\n");
        wr.write("Connection: close\r\n");
        wr.write("format: TURTLE\r\n");
        wr.write("\r\n");

        wr.write(xmldata);
        wr.flush();

        BufferedReader rd = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        String line;

        while ((line = rd.readLine()) != null)
            System.out.println(line);
    }
}
