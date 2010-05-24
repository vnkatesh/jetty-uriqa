package org.eclipse.jetty.uriqa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Java Application to test MPUT. <br />
 * Creates query, connects to {@link Server}, writes data, reads {@link Response} and prints to {@link System#out} <br />
 * query.length() can be easily determined and set as content-length header <br /> {@link Query} here puts an XML-Data as the
 * content and the {@link Model} at the server side adds the {@link Statement} in the data to its own model. Responds
 * with either {@link HttpServletResponse#SC_OK} or {@link HttpServletResponse#SC_CONFLICT}
 * 
 * @author venkatesh
 * @version $Id$
 */
public class TestMPut
{

    public static void main(String[] args)
    {

        try {
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
                    + "   </rdf:Description>\r\n"
                    + "   <rdf:Description rdf:about=\"http://localhost/aBookCritic\">\r\n"
                    + "      <ex:likes rdf:resource=\"http://localhost/aReallyGreatBook\"/>\r\n"
                    + "      <ex:dislikes rdf:resource=\"http://localhost/anotherGreatBook\"/>\r\n"
                    + "   </rdf:Description>\r\n"
                    + "   <rdf:Property rdf:about=\"http://xmlns.com/foaf/0.1/mbox\">\r\n"
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
            wr.write("\r\n");

            wr.write(xmldata);
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
