package org.eclipse.jetty.uriqa;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.uriqa.UriqaConstants.Methods;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.hp.hpl.jena.Jena;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RSIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Derivation;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.reasoner.ValidityReport.Report;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.util.FileManager;

/**
 * The {@link UriqaRepoHandler} is the most important class of jetty-uriqa package <br />
 * It handles the request from the {@link UriqaHandler}. <br />
 * Extends {@link AbstractLifeCycle} and implements {@link Serializable} <br />
 * There is a default static instance of this Class, and all the important functions can be accessed via the default
 * {@link UriqaRepoHandler#getDefault()} Method
 * 
 * @author venkatesh
 * @version $Id$
 */
@SuppressWarnings("serial")
public class UriqaRepoHandler extends AbstractLifeCycle implements Serializable
{

    /**
     * The shared static instance that can be retrieved using {@link UriqaRepoHandler#getDefault()}
     */
    private static UriqaRepoHandler sharedInstance = null;

    /**
     * The base {@link Model}, used to create {@link TDB} based model. <br /> {@link OntModel} {@link UriqaRepoHandler#model}
     * is created on top of this.
     */
    private static Model base = null;

    /**
     * {@link OntModel} model is the base static model. It is the core model of {@link Jena} which is being used by all
     * the handlers.
     */
    private static OntModel model = null;

    /**
     * The base {@link URI} for the model. Set initially by the first call to
     * {@link UriqaRepoHandler#getDefault(String)}
     */
    private URI baseURI = null;

    /**
     * Constructor. Should be called only once.
     * 
     * @param baseURI The value of {@link UriqaRepoHandler#baseURI} to be set. If null, then
     *            {@link Messages#getString(String)} is used to set it via URIQA_baseURI key.
     */
    public UriqaRepoHandler(String baseURI)
    {
        if (Log.isDebugEnabled())
            Log.debug("UriqaRepoHandler():baseURI:: " + baseURI);
        try {
            if (baseURI != null)
                this.baseURI = new URI(baseURI);
            else
                this.baseURI = new URI(Messages.getString("URIQA_baseURI"));
            if (Log.isDebugEnabled())
                Log.debug("UriqaRepoHandler():baseURI:: " + this.baseURI.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        try {
            /*
             * AbstractLifeCycle in turn calls doStart()
             */
            this.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructor. <br />
     * Calls getDefault(null)
     * 
     * @see UriqaRepoHandler#getDefault(String)
     * @return Shared static instance of {@link UriqaRepoHandler}
     */
    public synchronized static UriqaRepoHandler getDefault()
    {
        if (Log.isDebugEnabled())
            Log.debug("getDefault(): null");
        return getDefault(null);
    }

    /**
     * If {@link UriqaRepoHandler#sharedInstance} not already intitialized, then initializes it and returns, else just
     * returns sharedInstance.
     * 
     * @param baseURI
     * @return Shared static instance of {@link UriqaRepoHandler}
     */
    public synchronized static UriqaRepoHandler getDefault(String baseURI)
    {
        if (Log.isDebugEnabled())
            Log.debug("getDefault(): baseURI " + baseURI);
        if (sharedInstance == null) {
            try {
                /*
                 * To be intialized
                 */
                sharedInstance = new UriqaRepoHandler(baseURI);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        /*
         * Already initialized
         */
        return sharedInstance;
    }

    /**
     * {@inheritDoc} Intializes repostitory. <br />
     * Registers the {@link UriqaModelChangedListener}
     * 
     * @see org.eclipse.jetty.util.component.AbstractLifeCycle#doStart()
     */
    @Override
    public void doStart()
    {
        if (Log.isDebugEnabled())
            Log.debug("doStart()");
        /*
         * In-Memory Model or TDB Model.
         */
        if (!(new Boolean(Messages.getString("URIQA_TDB"))).booleanValue())
            this.initializeRepo();
        else
            this.initializeRepo(System.getProperty("user.dir").toString().hashCode());
        model.register(new UriqaModelChangedListener());
        if (Log.isDebugEnabled())
            Log.debug("doStart():modelListener:: registered");
    }

    /**
     * Initialize a memory based/TDB model.
     * 
     * @param hash Unique hash for this particular instance of {@link UriqaRepoHandler} Preferred value:
     *            {@link System#getProperty(String)} "user.dir". If hash equals 0, then creates a memory-based model,
     *            else TDB is used along with the hash to compute the unique directory where the model is to be stored
     */
    private void initializeRepo(int hash)
    {
        // TODO Reasoner to be exported out to URIQA_message.properties remove OntModelSpec.
        if (Log.isDebugEnabled())
            Log.debug("initializeRepo(): hash " + hash);
        if (hash == 0) {
            /*
             * Memory based Model.
             */
            if (Log.isDebugEnabled())
                Log.debug("initializeRepo(): MemoryModel");
            if (model != null) {
                /*
                 * Create new model
                 */
                model.enterCriticalSection(Lock.WRITE);
                try {
                    model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MINI_RULE_INF);
                } finally {
                    model.leaveCriticalSection();
                }
            } else
                model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MINI_RULE_INF);
        } else {
            /*
             * TDB Based Model. Unique DBdirectory value is using concatenated URIQA_dbDirectory in messages.properties
             * and hash-value.
             */
            String DBdirectory = Messages.getString("URIQA_dbDirectory") + "UriqaDB_" + Integer.toString(hash);
            base = TDBFactory.createModel(DBdirectory);
            model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MINI_RULE_INF, base);
            if (Log.isDebugEnabled())
                Log.debug("initializeRepo(): TDB Model at " + DBdirectory);
        }
        model.prepare();
        /*
         * For easier retrieval of derivation traces for model.getDerivations()
         */
        model.setDerivationLogging(true);
        /*
         * Initial inferencing rebinding.
         */
        model.rebind();
        /*
         * Load an Initial Repository if model is Empty and URIQA_LOAD in messages.properties is true The Model to be
         * loaded from file is specified by URIQA_INITIAL_REPO in messages.properties.
         */
        if (model.isEmpty() && (new Boolean(Messages.getString("URIQA_LOAD"))).booleanValue()) {
            model.add(FileManager.get().loadModel(Messages.getString("URIQA_INITIAL_REPO")));
            if (Log.isDebugEnabled())
                Log.debug("initializeRepo(): FileManager.loadModel from file: "
                    + Messages.getString("URIQA_INITIAL_REPO"));
        }
        // TODO Prefix j.1 has to be removed. for further compatibility with CBD.
        /*
         * Setting some known/common Namespace prefixes for the model.
         */
        HashMap<String, String> map = new HashMap<String, String>(8);
        map.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        map.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        map.put("owl", "http://www.w3.org/2002/07/owl#");
        map.put("dc", "http://purl.org/dc/elements/1.1/");
        map.put("dct", "http://purl.org/dc/terms/");
        map.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        map.put("foaf", "http://xmlns.com/foaf/0.1/");
        map.put(baseURI.getHost(), baseURI.toString());
        model.setNsPrefixes(map);
    }

    /**
     * A {@link Log} {@link Log#isDebugEnabled()} functionality which is called after every
     * {@link UriqaRepoHandler#handleRequest(HttpServletRequest, HttpServletResponse, String, HashMap)} <br />
     * Just prints the current status of the model as RDF-XML.
     * 
     * @return Model's RDFXML representation as a String.
     */
    public String printModeltoConsole()
    {
        if (Log.isDebugEnabled())
            Log.debug("printModeltoConsole()");
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        model.enterCriticalSection(Lock.READ);
        try {
            /*
             * Write to stream the current model
             */
            model.write(stream, UriqaConstants.Lang.RDFXML);
        } finally {
            model.leaveCriticalSection();
        }
        return stream.toString();

    }

    /**
     * Download an external URI model and save it to a given file.
     * 
     * @param url Url of the file to be downloaded.
     * @param file file to be created and saved to
     */
    public void downloadRemoteModel(final String url, final File file)
    {
        if (Log.isDebugEnabled())
            Log.debug("downloadRemoteModel(): url file " + url + "," + file.getAbsolutePath());
        try {
            /*
             * Create URLConnection, use it as a BufferedInputStream. Create new File using the file parameters, create
             * new BufferedOutputStream with it. Copy (write()) bytes from one stream to another till EOF.
             */
            URL attachmentURL = new URL(url);
            URLConnection attachmentConnection = attachmentURL.openConnection();
            BufferedInputStream bis = new BufferedInputStream(attachmentConnection.getInputStream());
            file.createNewFile();
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            byte[] bytes = new byte[1024];
            int count = 0;
            while ((count = bis.read(bytes)) != -1) {
                bos.write(bytes, 0, count);
            }
            bis.close();
            bos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when {@link AbstractLifeCycle#stop()} is called. {@inheritDoc}
     * 
     * @see org.eclipse.jetty.util.component.AbstractLifeCycle#doStop()
     */
    public void doStop()
    {
        if (Log.isDebugEnabled())
            Log.debug("doStop()");
        /*
         * Delete sharedInstance.
         */
        sharedInstance = null;
    }

    /**
     * The main {@link UriqaRepoHandler#handleRequest(HttpServletRequest, HttpServletResponse, String, HashMap)} method. <br />
     * Transfers the request with the required parameters and arguments to doGet(), doPut(), etc according to method
     * type.
     * 
     * @param request The request from
     *            {@link UriqaHandler#handle(String, org.eclipse.jetty.server.Request, HttpServletRequest, HttpServletResponse)}
     * @param response The response from
     *            {@link UriqaHandler#handle(String, org.eclipse.jetty.server.Request, HttpServletRequest, HttpServletResponse)}
     * @param method The method, either MGET/MTRACE/MPUT/MTRACE/MQUERY. Accordingly the do*() methods are called.
     * @param paramMap The headers parameter Map from {@link UriqaHandler#paramMap}
     */
    public void handleRequest(HttpServletRequest request, HttpServletResponse response, String method,
        HashMap<String, String> paramMap)
    {
        if (Log.isDebugEnabled())
            Log.debug("handleRequest():request,response,method,paramMap:: " + request.toString() + ","
                + response.toString() + "," + method + paramMap.toString());
        /*
         * baseURIpath is the URI of the resource to be MGET/MDELETE etc and is calculated here.
         */
        // TODO remove baseURIpath from MPUT, all the doesntmatter calls.
        String baseURIpath =
            baseURI.toString()
                + (request.getPathInfo().startsWith("/") ? request.getPathInfo() : "/" + request.getPathInfo());
        if (method.equals(UriqaConstants.Methods.MGET)) {
            try {
                /*
                 * MGET Method.
                 */
                doGet(baseURIpath, response, paramMap);
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        if (method.equals(UriqaConstants.Methods.MPUT)) {
            try {
                /*
                 * MPUT Method.
                 */
                doPut(baseURI.toString(), request, paramMap, response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /*
         * MDELTE Method
         */
        if (method.equals(UriqaConstants.Methods.MDELETE))
            doDelete(baseURIpath, response);
        if (method.equals(UriqaConstants.Methods.MTRACE)) {
            try {
                /*
                 * MTRACE Method
                 */
                doDerive(request, response);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        if (method.equals(UriqaConstants.Methods.MQUERY)) {
            try {
                try {
                    /*
                     * MQUERY Method
                     */
                    doQuery(request, response, paramMap);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * For MTRACE Methods {@link Methods#MTRACE}<br />
     * Get's the request String, the prefixes, creates custom-queries out of it; either S,P,O or S,P,L and uses the
     * {@link InfModel#getDerivation(Statement)} to print a {@link Derivation#printTrace(PrintWriter, boolean)};
     * 
     * @param request The request @see
     *            {@link UriqaRepoHandler#handleRequest(HttpServletRequest, HttpServletResponse, String, HashMap)}
     * @param response The response @see
     *            {@link UriqaRepoHandler#handleRequest(HttpServletRequest, HttpServletResponse, String, HashMap)}
     * @throws IOException
     */
    public void doDerive(HttpServletRequest request, HttpServletResponse response) throws IOException
    {

        if (Log.isDebugEnabled())
            Log.debug("doDerive():request,response:: " + request.toString() + "," + response.toString());
        BufferedReader reader = request.getReader();
        String line;
        String queryString = new String();
        PrefixMappingImpl prefix = new PrefixMappingImpl();
        ByteArrayOutputStream bytearray = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(bytearray);
        /*
         * Format for querystring:
         * [PREFIX:\tDC:\t<http://uri/>\r\n]*[\r\n]+TRACE\r\n[prefix:path\tprefix2:path2\tprefix3:path3\r\n]*\r\n
         */
        while ((line = reader.readLine()) != null) {
            /*
             * real Line and save it.
             */
            queryString += line;
            // TODO Create some actual parser maybe?
            // TODO Does not close properly. Just checked with others also. Same behaviour. Is that normal??
            /*
             * Line starts with PREFIX. Store values in PrefixMappingImpl
             */
            if (line.toLowerCase().startsWith("prefix")) {
                if (Log.isDebugEnabled())
                    Log.debug("doDerive():Prefix");
                /*
                 * Format:
                 */
                prefix.setNsPrefix(line.split("	")[1].split(":")[0], line.split("	")[2].split(">")[0].split("<")[1]);
            } else if (line.toLowerCase().startsWith("trace") || line.toLowerCase().startsWith("\r\n")
                || line.toLowerCase().isEmpty()) {
                /*
                 * Lines to be ignored, insignificant.
                 */
                if (Log.isDebugEnabled())
                    Log.debug("doDerive():Ignore");
            } else {
                /*
                 * Getting subject, property values
                 */
                String[] q = line.split("	");
                Resource subject = null;
                Property property = null;
                String literal = null;
                Resource object = null;
                subject = model.getResource(prefix.getNsPrefixURI(q[0].split(":")[0]) + q[0].split(":")[1]);
                property = model.getProperty(prefix.getNsPrefixURI(q[1].split(":")[0]) + q[1].split(":")[1]);
                /*
                 * If 3rd value contains ":" then, is either URI or is prefix:path. Therefore, should be a resource
                 */
                if (q[2].contains(":"))
                    object = model.getResource(prefix.getNsPrefixURI(q[2].split(":")[0]) + q[2].split(":")[1]);
                else
                    literal = q[2];
                if (object != null) {
                    /*
                     * Object is set, S,P,O Statement
                     */
                    if (Log.isDebugEnabled())
                        Log.debug("doDerive():s,p,object");
                    /*
                     * Checking if such a statement exists in the model.
                     */
                    if (model.listStatements(subject, property, object).toList().size() > 0) {
                        /*
                         * Exists.
                         */
                        if (Log.isDebugEnabled())
                            Log.debug("doDerive(): 200");
                        response.setStatus(HttpServletResponse.SC_OK);
                    } else {
                        if (Log.isDebugEnabled())
                            Log.debug("doDerive(): 404");
                        /*
                         * Does not exist, therefore 404
                         */
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.setContentLength(0);
                        response.getOutputStream().close();
                        out.close();
                        bytearray.close();
                        return;
                    }
                    for (StmtIterator i = model.listStatements(subject, property, object); i.hasNext();) {
                        Statement s = i.nextStatement();
                        /*
                         * Get the Iterator, get the statement, and the loop for the trace. Custom output format:
                         * PrintStatment\r\nPrintTrace.
                         */
                        out.println(s);
                        for (Iterator<Derivation> id = model.getDerivation(s); id.hasNext();) {
                            Derivation deriv = (Derivation) id.next();
                            deriv.printTrace(out, true);
                        }
                    }
                } else {
                    /*
                     * Object is null, it is a literal. S,P, L
                     */
                    if (Log.isDebugEnabled())
                        Log.debug("doDerive():s,p,literal");
                    /*
                     * Checking if such a statement exists in the model
                     */
                    if (model.listStatements(subject, property, literal).toList().size() > 0) {
                        /*
                         * Exists
                         */
                        if (Log.isDebugEnabled())
                            Log.debug("doDerive(): 200");
                        response.setStatus(HttpServletResponse.SC_OK);
                    } else {
                        /*
                         * Does not exist, 404.
                         */
                        if (Log.isDebugEnabled())
                            Log.debug("doDerive(): 404");
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.setContentLength(0);
                        response.getOutputStream().close();
                        out.close();
                        bytearray.close();
                        return;
                    }
                    for (StmtIterator i = model.listStatements(subject, property, literal); i.hasNext();) {
                        /*
                         * Get the Iterator, get the statement, and the loop for the trace. Custom output format:
                         * PrintStatment\r\nPrintTrace.
                         */
                        Statement s = i.nextStatement();
                        out.println(s);
                        for (Iterator<Derivation> id = model.getDerivation(s); id.hasNext();) {
                            Derivation deriv = (Derivation) id.next();
                            deriv.printTrace(out, true);
                        }
                    }
                }
            }

            queryString += "\n";
            if (Log.isDebugEnabled())
                Log.debug("doDerive():queryString:: " + queryString);
        }

        reader.close();
        if (reader.read() >= 0)
            throw new IllegalStateException(Messages.getString("URIQA_readerErrorMesage"));

        /*
         * Flushing all outputs, setting response-headers, closing streams.
         */
        out.flush();
        response.setContentType(MimeTypes.TEXT_PLAIN);
        response.setContentLength(bytearray.toByteArray().length);
        response.getOutputStream().write(bytearray.toByteArray());
        bytearray.close();
        out.close();
        response.getOutputStream().close();
    }

    /**
     * Handles MQUERY Method {@link Methods#MQUERY} <br />
     * Get's the query string, check the {@link Query#getQueryType()}, and executes corresponding Query Action.<br />
     * There are two type of query groups: One that writes to the {@link Model} and the other that just read's or
     * performs non-updating queries. They have been handled appropriately. <br />
     * The response prints the XML/JSON data for {@link Query#isSelectType()} or {@link Query#isAskType()} <br />
     * or {@link Model#write(OutputStream, String)} for {@link Query#isDescribeType()} or
     * {@link Query#isConstructType()} <br />
     * The response just performs the update-queries and validates them (and rollbacks if conflict). <br />
     * The response headers/content-type/content-length are appropriately set.
     * 
     * @param request The {@link Request}
     *            {@link UriqaRepoHandler#handleRequest(HttpServletRequest, HttpServletResponse, String, HashMap)}
     * @param response The {@link Response}
     *            {@link UriqaRepoHandler#handleRequest(HttpServletRequest, HttpServletResponse, String, HashMap)}
     * @param paramMap The parameter values of the header
     *            {@link UriqaRepoHandler#handleRequest(HttpServletRequest, HttpServletResponse, String, HashMap)}
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public void doQuery(HttpServletRequest request, HttpServletResponse response, HashMap<String, String> paramMap)
        throws IOException, IllegalArgumentException, SecurityException, IllegalAccessException,
        InvocationTargetException, NoSuchMethodException
    {
        if (Log.isDebugEnabled())
            Log.debug("doQuery():request,response,paramMap:: " + request.toString() + "," + response.toString() + ","
                + paramMap.toString());
        BufferedReader reader = request.getReader();
        int count = 0;
        String line;
        String queryString = new String();

        while ((line = reader.readLine()) != null) {
            queryString += line;
            queryString += "\n";
            count += line.length();
        }

        /*
         * Read the content to queryString.
         */
        reader.close();
        if (reader.read() >= 0)
            throw new IllegalStateException(Messages.getString("URIQA_readerErrorMesage"));

        if (Log.isDebugEnabled())
            Log.debug("doQuery:queryString:: " + queryString);

        // TODO Any other efficient way to compare them?
        if (queryString.toUpperCase().contains(UriqaConstants.Query.INSERT)
            || queryString.toUpperCase().contains(UriqaConstants.Query.DELETE)
            || queryString.toUpperCase().contains(UriqaConstants.Query.MODIFY)
            || queryString.toUpperCase().contains(UriqaConstants.Query.LOAD)
            || queryString.toUpperCase().contains(UriqaConstants.Query.CLEAR)
            || queryString.toUpperCase().contains(UriqaConstants.Query.DROP)
            || queryString.toUpperCase().contains(UriqaConstants.Query.CREATE)) {

            /*
             * Update-Queries, therefore Lock.Write
             */
            model.enterCriticalSection(Lock.WRITE);
            try {
                /*
                 * Inference boolean parameter
                 */
                boolean inference = paramMap.get(UriqaConstants.Parameters.INFERENCE).equals(UriqaConstants.Values.INC);
                if (queryString.toUpperCase().contains(UriqaConstants.Query.INSERT)
                    || queryString.toUpperCase().contains(UriqaConstants.Query.MODIFY)
                    || queryString.toUpperCase().contains(UriqaConstants.Query.LOAD)
                    || queryString.toUpperCase().contains(UriqaConstants.Query.CREATE)) {
                    /*
                     * Those update-queries that insert data into the model. They need to be validated before actual
                     * commit(). Since model.supportTransaction() is false (true only for SDB), I've used tempmodel to
                     * insert all the data into, validate and then perform the actual query on the model if there is no
                     * validation errors. Else just to perform the query.
                     */
                    OntModel tempmodel = ModelFactory.createOntologyModel();
                    /*
                     * Adding all the data to tempmodel
                     */
                    tempmodel.add(model);
                    if (inference) {
                        /*
                         * Inferred model, using tempmodel.getDeductionsModel() for including inferred statements.
                         * Executing update-query
                         */
                        UpdateAction.parseExecute(queryString, tempmodel.getDeductionsModel());
                    } else {
                        /*
                         * Executing update-query for non-inferred model.
                         */
                        UpdateAction.parseExecute(queryString, tempmodel.getRawModel());
                    }
                    if (!tempmodel.validate().isValid()) {
                        /*
                         * Conflict. Print the ValidityReport Errors to the response and set CONFLICT Header. Do not
                         * perform update-query on the actual model.
                         */
                        if (Log.isDebugEnabled())
                            Log.debug("doQuery:INSERT::conflict()");
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        String error = "";
                        for (Iterator<Report> i = tempmodel.validate().getReports(); i.hasNext();) {
                            error += " - " + i.next() + "\r\n";
                        }
                        response.setContentLength(error.length());
                        response.setContentType(MimeTypes.TEXT_PLAIN);
                        response.getWriter().write(error);
                    } else {
                        /*
                         * There is not error in the validation. Go ahead and perform the actual query in the model.
                         */
                        if (inference) {
                            UpdateAction.parseExecute(queryString, model.getDeductionsModel());
                        } else {
                            UpdateAction.parseExecute(queryString, model.getRawModel());
                        }
                        if (Log.isDebugEnabled())
                            Log.debug("doQuery():INSERT::OK");
                        /*
                         * Set NO-CONTENT Header. And perform new Model-Rebinding.
                         */
                        response.setContentLength(0);
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        model.notifyEvent(true);
                    }
                } else {
                    /*
                     * The Update-queries that just Delete/DROP data. They do not require to be validated.
                     */
                    if (inference) {
                        UpdateAction.parseExecute(queryString, model.getDeductionsModel());
                    } else {
                        UpdateAction.parseExecute(queryString, model.getRawModel());
                    }
                    /*
                     * Logic is similary to UriqaRepoHandler.doDelete().
                     */
                    if (Log.isDebugEnabled())
                        Log.debug("doQuery:DELETE:OK");
                    /*
                     * Set NO-CONTENT Header. And perform new Model-Rebinding.
                     */
                    response.setContentLength(0);
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    model.notifyEvent(true);
                }
            } finally {
                model.leaveCriticalSection();
            }
            return;
        }

        /*
         * The queries are of type select/ask/desribe/construct. Do not actually change the data. Therefore, using
         * Lock.READ
         */
        Query query = QueryFactory.create(queryString);
        model.enterCriticalSection(Lock.READ);
        try {
            /*
             * Status always OK.
             */
            response.setStatus(HttpServletResponse.SC_OK);
            QueryExecution qexec = null;
            /*
             * Creating QueryExecution with or without Inference according to Parameter-value.
             */
            if (paramMap.get(UriqaConstants.Parameters.INFERENCE).equals(UriqaConstants.Values.INC)) {
                qexec = QueryExecutionFactory.create(query, model.getDeductionsModel());
            } else {
                qexec = QueryExecutionFactory.create(query, model.getRawModel());
            }
            if (query.isSelectType()) {
                /*
                 * SELECT
                 */
                if (Log.isDebugEnabled())
                    Log.debug("doQuery:isSelect()");
                /*
                 * Execute query
                 */
                ResultSet results = qexec.execSelect();
                if (paramMap.get(UriqaConstants.Parameters.FORMAT).equals(UriqaConstants.Lang.RDFXML)) {
                    response.setContentType(UriqaConstants.Lang.RDFXML);
                    /*
                     * Output the results to response as RDFXML
                     */
                    contentLengthPrint(response, ResultSetFormatter.class.getMethod("outputAsXML", new Class[] {
                    OutputStream.class, ResultSet.class}), new Object[] {results}, null);
                }
                if (paramMap.get(UriqaConstants.Parameters.FORMAT).equals(UriqaConstants.Values.JSON)) {
                    response.setContentType(MimeTypes.TEXT_JSON);
                    /*
                     * Output the results to response as JSON.
                     */
                    contentLengthPrint(response, ResultSetFormatter.class.getMethod("outputAsJSON", new Class[] {
                    OutputStream.class, ResultSet.class}), new Object[] {results}, null);
                }
            }
            if (query.isAskType()) {
                /*
                 * ASK
                 */
                if (Log.isDebugEnabled())
                    Log.debug("");
                /*
                 * Execute
                 */
                Boolean answer = qexec.execAsk();
                if (paramMap.get(UriqaConstants.Parameters.FORMAT).equals(UriqaConstants.Lang.RDFXML)) {
                    response.setContentType(UriqaConstants.Lang.RDFXML);
                    /*
                     * Output the results to response as RDFXML
                     */
                    contentLengthPrint(response, ResultSetFormatter.class.getMethod("outputAsXML", new Class[] {
                    OutputStream.class, Boolean.class}), new Object[] {answer}, null);
                }
                if (paramMap.get(UriqaConstants.Parameters.FORMAT).equals(UriqaConstants.Values.JSON)) {
                    response.setContentType(MimeTypes.TEXT_JSON);
                    /*
                     * Output the results to response as JSON
                     */
                    contentLengthPrint(response, ResultSetFormatter.class.getMethod("outputAsJSON", new Class[] {
                    OutputStream.class, Boolean.class}), new Object[] {answer}, null);
                }
            }
            if (query.isConstructType()) {
                /*
                 * CONSTRUCT
                 */
                if (Log.isDebugEnabled())
                    Log.debug("doQuery:isConstruct");
                /*
                 * Execute
                 */
                Model tempmodel = qexec.execConstruct();
                response.setContentType(paramMap.get(UriqaConstants.Parameters.FORMAT));
                /*
                 * Set response-content-type and printing to the output using Model.write() method.
                 */
                contentLengthPrint(response, Model.class.getMethod("write", new Class[] {OutputStream.class,
                String.class}), new Object[] {paramMap.get(UriqaConstants.Parameters.FORMAT), tempmodel}, null);
            }
            if (query.isDescribeType()) {
                /*
                 * DESCRIBE
                 */
                if (Log.isDebugEnabled())
                    Log.debug("doQuery:isDescribe()");
                /*
                 * EXECUTE
                 */
                Model tempmodel = qexec.execDescribe();
                response.setContentType(paramMap.get(UriqaConstants.Parameters.FORMAT));
                /*
                 * Set response-content-type and printing to the output using Model.write() method.
                 */
                contentLengthPrint(response, Model.class.getMethod("write", new Class[] {OutputStream.class,
                String.class}), new Object[] {paramMap.get(UriqaConstants.Parameters.FORMAT), tempmodel}, null);
            }
            qexec.close();
        } finally {
            model.leaveCriticalSection();
        }
    }

    /**
     * Generalized Reflect Method for Executing a {@link Method} (either static or non-static) and printing the output
     * to the given {@link Response}, along with content-length headers for response. <br />
     * e.g <br />
     * <b>model#.write(OutputStream response.getOutputStream(), String lang)</b> becomes <br />
     * <b>contentLengthPrint(HttpServletResponse response, Model.class.getMethod("write", new Class[]
     * {OutputStream.class, String.class}), new Object[] {paramMap.get(UriqaConstants.Parameters.FORMAT}, null}</b> <br/>
     * See implementation of {@link UriqaRepoHandler#doGet(String, HttpServletResponse, HashMap)} <br />
     * <br />
     * The more generalized methods (without the first parameter as OutputStream) can use the param 'out' and provided
     * it as not null.<br />
     * See implementation of {@link UriqaRepoHandler#rdf2html(Model, HttpServletResponse)} <br />
     * 
     * @param response The response for the final output to be copied to {@link HttpServletResponse}
     * @param method The method to be executed {@link Method}
     * @param arguments The arguments required by method
     * @param out The outputStream from where the response has to be copied. Already provided, or maybe null
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void contentLengthPrint(HttpServletResponse response, Method method, Object[] arguments,
        ByteArrayOutputStream out) throws IOException, IllegalArgumentException, IllegalAccessException,
        InvocationTargetException
    {
        if (Log.isDebugEnabled())
            Log.debug("contentLengthPrint():response,method,arguments,out:: " + response.toString() + "," + method
                + "," + arguments.toString() + ", " + out.toString());
        if (Log.isDebugEnabled())
            Log.debug("contentLengthPrint():method:: " + method.getName());
        /*
         * outProvided is set a true if out is not null, i.e more generalized method is being called here and provides
         * its own outputStream
         */
        boolean outProvided = true;
        if (out == null)
            outProvided = false;
        if (!outProvided)
            out = new ByteArrayOutputStream();
        if (Log.isDebugEnabled())
            Log.debug("contentLengthPrint():outProvided:: " + outProvided);
        ArrayList<Object> args = new ArrayList<Object>();
        if (Modifier.isStatic(method.getModifiers())) {
            /*
             * Static Method. Adding ByteArrayOutputStream if out not Provided.
             */
            if (!outProvided)
                args.add(out);
            if (Log.isDebugEnabled())
                Log.debug("contentLengthPrint():isStatic()");
            /*
             * Adding the rest of the arguments and invoking static method with 'null' object.
             */
            args.addAll(Arrays.asList(arguments));
            method.invoke(null, args.toArray());
        } else {
            /*
             * Not Static. Adding ByteArrayOutputStream if out not provided
             */
            if (!outProvided)
                args.add(out);
            if (Log.isDebugEnabled())
                Log.debug("contentLengthPrint():isNotStatic()");
            /*
             * Last element is the object which invokes the method. That is removed from the array, and used in
             * Method.invoke(object, arguments);
             */
            args.addAll(Arrays.asList(arguments));
            args.remove(args.size() - 1);
            method.invoke(arguments[arguments.length - 1], args.toArray());
        }
        /*
         * Setting content-length header.
         */
        response.setContentLength(out.toByteArray().length);
        response.getOutputStream().write(out.toByteArray());
        if (Log.isDebugEnabled())
            Log.debug("contentPrintLength: output:\r\n " + out.toString());
    }

    /**
     * MGET Method {@link Methods#MGET}<br />
     * Get's the concise bound description of the {@link Resource} denoted by the {@link URI} (baseURIPath) from the
     * model, transforms it to required formats, inferencing and prints it to the response.OutputStream<br />
     * Writes the proper response codes, CBD-model of required format/inference or return 404 if not found.
     * 
     * @param baseURIPath URI of the resource to be retrieved.
     * @param response The Response for the Connection {@link Response}
     * @param paramMap The parameter Map for the request {@link UriqaHandler#paramMap}
     * @throws IOException
     * @throws TransformerException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public void doGet(String baseURIPath, HttpServletResponse response, HashMap<String, String> paramMap)
        throws IOException, TransformerException, SAXException, ParserConfigurationException, IllegalArgumentException,
        SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
    {
        if (Log.isDebugEnabled())
            Log.debug("doGet():baseURIPath,response,paramMap " + baseURIPath + "," + response.toString() + ","
                + paramMap.toString());
        // TODO Custom printModel for CBD. Understand? Check for Anonnodes and print CBD of them inside itself.
        // TODO Its still printing the NodeID thing. Should I remove that?
        model.enterCriticalSection(Lock.READ);
        try {
            if (!model.contains(model.getResource(baseURIPath), null, (RDFNode) null)) {
                /*
                 * Not Present in Model. Return 404 in response.
                 */
                response.setContentLength(0);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                if (Log.isDebugEnabled())
                    Log.debug("doGet(): 404");
                return;
            }
            if (paramMap.get(UriqaConstants.Parameters.FORMAT).equals(UriqaConstants.Values.HTML)) {
                /*
                 * HTML format output.
                 */
                if (Log.isDebugEnabled())
                    Log.debug("doGet():FORMAT::HTML");
                /*
                 * Setting response content-type header
                 */
                response.setContentType(MimeTypes.TEXT_HTML);
                /*
                 * Checking for inference and using rdf2html to print the required model to response output.
                 */
                if (paramMap.get(UriqaConstants.Parameters.INFERENCE).equals(UriqaConstants.Values.INC))
                    rdf2html(getCBD(model.getResource(baseURIPath), model.getDeductionsModel()), response);
                else
                    rdf2html(getCBD(model.getResource(baseURIPath), model.getRawModel()), response);
            } else {
                /*
                 * Other formats.
                 */
                if (Log.isDebugEnabled())
                    Log.debug("doGet():FORMAT:: " + paramMap.get(UriqaConstants.Parameters.FORMAT));
                /*
                 * Setting response content-type header
                 */
                response.setContentType(paramMap.get(UriqaConstants.Parameters.FORMAT));
                /*
                 * Checking for inference and using getCBD() and contentLengthPrint() to print the required output to
                 * response OutStream
                 */
                if (paramMap.get(UriqaConstants.Parameters.INFERENCE).equals(UriqaConstants.Values.INC))
                    contentLengthPrint(response, Model.class.getMethod("write", new Class[] {OutputStream.class,
                    String.class}), new Object[] {paramMap.get(UriqaConstants.Parameters.FORMAT),
                    getCBD(model.getResource(baseURIPath), model.getDeductionsModel())}, null);
                else
                    contentLengthPrint(response, Model.class.getMethod("write", new Class[] {OutputStream.class,
                    String.class}), new Object[] {paramMap.get(UriqaConstants.Parameters.FORMAT),
                    getCBD(model.getResource(baseURIPath), model.getRawModel())}, null);
            }
        } finally {
            model.leaveCriticalSection();
        }
    }

    /**
     * Given a model 'data' with RDF-Statements, this function transforms RDF-XML stream to an HTML stream using
     * {@link Transformer} and writes it to {@link Response} <br />
     * <br /> {@link UriqaRepoHandler#contentLengthPrint(HttpServletResponse, Method, Object[], ByteArrayOutputStream)} is
     * used by this method to further print the content-length to the response
     * 
     * @param data The model to be transformed to HTML output
     * @param response The response, {@link HttpServletResponse}
     * @throws IOException
     * @throws TransformerException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    private void rdf2html(Model data, HttpServletResponse response) throws IOException, TransformerException,
        SAXException, ParserConfigurationException, IllegalArgumentException, SecurityException,
        IllegalAccessException, InvocationTargetException, NoSuchMethodException
    {
        if (Log.isDebugEnabled())
            Log.debug("rdf2html():data,response:: " + data.toString() + "," + response.toString());
        /*
         * Set response-headers.
         */
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MimeTypes.TEXT_HTML);

        /*
         * Read the model into ByteArrayOutputStream and create a new InputSource using that.
         */
        InputSource iSource;
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        data.write(o, UriqaConstants.Lang.RDFXML);
        o.flush();
        String rdfxml = o.toString(Messages.getString("URIQA_ENCODING"));
        iSource = new InputSource(new StringReader(rdfxml));

        /*
         * Create a new instance of SAXParser which would parse the XML of the InputSource created above.
         */
        SAXParserFactory pFactory = SAXParserFactory.newInstance();
        pFactory.setNamespaceAware(true);
        pFactory.setValidating(false);
        XMLReader xmlReader = pFactory.newSAXParser().getXMLReader();

        /*
         * New Transformer instance. Set OutputProperites for HTML
         */
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, Messages.getString("URIQA_DOCTYPE_PUBLIC"));
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, Messages.getString("URIQA_DOCTYPE_SYSTEM"));
        transformer.setOutputProperty(OutputKeys.ENCODING, Messages.getString("URIQA_ENCODING"));

        /*
         * Tranform using Transformer.transformer() method with the SAXParser (xmlReader), InputSource from above to
         * write the output to ByteArrayStream out2 which would further be processed by ContentPrintLength() to output
         * to Response
         */
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        contentLengthPrint(response, Transformer.class.getMethod("transform", Source.class, Result.class),
            new Object[] {new SAXSource(xmlReader, iSource), new StreamResult(out2), transformer}, out2);
    }

    /**
     * Get Concise Bound Description of {@link Resource} r from {@link Model} data <br />
     * 
     * @see http://www.w3.org/Submission/CBD/
     * @param r The {@link Resource} whose CBD is required.
     * @param data The {@link Model} from where the resource resides.
     * @return
     */
    private Model getCBD(Resource r, Model data)
    {
        if (!data.contains(r, null, (RDFNode) null)) {
            /*
             * Not Present in Model
             */
            if (Log.isDebugEnabled())
                Log.debug("getCBD(): returning Null");
            return null;
        }
        if (Log.isDebugEnabled())
            Log.debug("getCBD():resource,data:: " + r.toString() + "," + data.toString());
        data.enterCriticalSection(Lock.READ);
        try {
            /*
             * Getting the statements that that have the resource as Subject.
             */
            StmtIterator iter = data.listStatements(r, null, (RDFNode) null);
            if (Log.isDebugEnabled())
                Log.debug("getCBD():Number of statments: " + iter.toList().size());
            /*
             * CBD requires Reified statements to be shown too. Creating tempmodel.
             */
            Model tempmodel = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
            while (iter.hasNext()) {
                Statement stmt = iter.nextStatement();
                /*
                 * Adding the statement to the tempmodel.
                 */
                tempmodel.add(stmt);
                if (Log.isDebugEnabled())
                    Log.debug("getCBD():addStatment: " + stmt.getResource().getURI() + "->"
                        + stmt.getPredicate().getURI() + "->" + stmt.getObject().toString());
                if (stmt.getObject().isAnon()) {
                    /*
                     * If the Object is anonymous, using getClean() to do a recursive add of Statements to tempmodel.
                     * Where the subject is the anonymous node.
                     */
                    tempmodel.add(getClean((Resource) stmt.getObject(), data));
                }
                // TODO Reification producing orphan nodes. Is that OK? isDefinedBy() is not getting added. Required?
                // TODO I'm still getting RDF:Node. Can I remove that using custom PrintModel?
                // TODO Remove getClean() somehow?
                /*
                 * Checking for reified statements in parent model.
                 */
                RSIterator iter2 = data.listReifiedStatements(stmt);
                if (Log.isDebugEnabled())
                    Log.debug("getCBD():Number of reified: " + iter2.toList().size());
                while (iter2.hasNext()) {
                    Statement stmt2 = iter2.nextRS().getStatement();
                    /*
                     * Adding the reified statement to tempModel.
                     */
                    tempmodel.createReifiedStatement(stmt2);
                    if (Log.isDebugEnabled())
                        Log.debug("getCBD():addStatment: " + stmt2.getResource().getURI() + "->"
                            + stmt2.getPredicate().getURI() + "->" + stmt2.getObject().toString());
                }
            }
            return tempmodel;
        } finally {
            data.leaveCriticalSection();
        }
    }

    /**
     * Implementation of getClean() is same as getCBD(), only the reified statements are not added. <br />
     * Null model is never returned.
     * 
     * @see UriqaRepoHandler#getCBD(Resource, Model)
     * @param r The {@link Resource} whose CBD is required.
     * @param data The {@link Model} from where the resource resides.
     * @return
     */
    private static Model getClean(Resource r, Model data)
    {
        if (Log.isDebugEnabled())
            Log.debug("getClean():resource,data:: " + r.toString() + "," + data.toString());
        data.enterCriticalSection(Lock.READ);
        try {
            /*
             * Getting the statements that that have the resource as Subject.
             */
            StmtIterator iter = data.listStatements(r, null, (RDFNode) null);
            if (Log.isDebugEnabled())
                Log.debug("getClean():Number of statments: " + iter.toList().size());
            /*
             * Creating cleanmodel.
             */
            Model cleanModel = ModelFactory.createDefaultModel();
            while (iter.hasNext()) {
                Statement stmt = iter.nextStatement();
                /*
                 * Adding the statement to the cleanmodel.
                 */
                cleanModel.add(stmt);
                if (Log.isDebugEnabled())
                    Log.debug("getClean():addStatment: " + stmt.getResource().getURI() + "->"
                        + stmt.getPredicate().getURI() + "->" + stmt.getObject().toString());
                if (stmt.getObject().isAnon()) {
                    cleanModel.add(getClean((Resource) stmt.getObject(), data));
                }
                // TODO Assuming Anonymous nodes don't have reified statements. Or, do they?
            }
            return cleanModel;
        } finally {
            data.leaveCriticalSection();
        }
    }

    /**
     * MPUT Method {@link Methods#MPUT}<br />
     * Reads the content from the response, adds it to the model. <br />
     * If Conflict occurs, rollsback <br />
     * Reads the content according to the format specified by the headers <br />
     * 
     * @param baseURI The baseURI required by {@link Model#read(InputStream, String)}
     * @param request The request {@link HttpServletRequest}
     * @param paramMap The parameters set by the request {@link UriqaHandler#paramMap}
     * @param response The response {@link HttpServletResponse}
     * @throws IOException
     */
    public void doPut(String baseURI, HttpServletRequest request, HashMap<String, String> paramMap,
        HttpServletResponse response) throws IOException
    {
        if (Log.isDebugEnabled())
            Log.debug("doPut():baseURI,request,paramMap,response:: " + baseURI + "," + request.toString() + ","
                + paramMap.toString() + "," + response.toString());
        model.enterCriticalSection(Lock.WRITE);
        try {
            /*
             * Adds the request content statements to the model according to the given format and baseURI.
             */
            model.read(request.getInputStream(), baseURI, paramMap.get(UriqaConstants.Parameters.FORMAT));
            ValidityReport validity = model.validate();
            if (!validity.isValid()) {
                /*
                 * Not Valid, Conflict. Set response header as conflict.
                 */
                if (Log.isDebugEnabled())
                    Log.debug("doPut: Conflict");
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                String error = "";
                for (Iterator<Report> i = validity.getReports(); i.hasNext();) {
                    error += " - " + i.next() + "\r\n";
                }
                /*
                 * Write the content as the error-reports
                 */
                response.setContentLength(error.length());
                response.setContentType(MimeTypes.TEXT_PLAIN);
                response.getWriter().write(error);
                Model tempmodel = ModelFactory.createDefaultModel();
                tempmodel.read(request.getInputStream(), baseURI, paramMap.get(UriqaConstants.Parameters.FORMAT));
                model.removeSubModel(tempmodel, false);
            } else {
                if (Log.isDebugEnabled())
                    Log.debug("doPut(): 200");
                /*
                 * Not conflict, set no-content status. Rebind Model.
                 */
                response.setContentLength(0);
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                model.notifyEvent(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JenaException e) {
            // TODO Premature end of file: response code something else.
        } finally {
            model.leaveCriticalSection();
        }
    }

    /**
     * Delete a {@link Resource} CBD from the base model
     * 
     * @param baseURIPath The URI String of the resource CBD to be removed.
     * @param response The response {@link HttpServletResponse}
     */
    public void doDelete(String baseURIPath, HttpServletResponse response)
    {
        if (Log.isDebugEnabled())
            Log.debug("doDelete():baseURIpath,response:: " + baseURIPath + "," + response.toString());
        model.enterCriticalSection(Lock.WRITE);
        try {
            /*
             * Remove the statements in the model from the CBD. Rebind model.
             */
            model.remove(getCBD(model.getResource(baseURIPath), model));
            model.notifyEvent(true);
        } finally {
            model.leaveCriticalSection();
        }
        if (Log.isDebugEnabled())
            Log.debug("doDelete(): 200");
        /*
         * Set response status as no-content.
         */
        response.setContentLength(0);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        // TODO the rdf:NodeID's still exist. Is that correct?
        // TODO the reification statments, should that come in printModeltoConsole()?
    }

    /**
     * Deprecated Use {@link Jena} {@link FileManager#loadModel(String)}<br />
     * <br />
     * Download (if not already exisiting) a remote model denoted by the url with baseURI
     * 
     * @param url The URL
     * @param baseURI the BaseURI
     */
    @Deprecated
    public void loadFromUrl(final String url, String baseURI)
    {
        if (Log.isDebugEnabled())
            Log.debug("loadFromUrl():url,baseURI:: " + url + "," + baseURI);
        String tmpDir;
        try {
            /*
             * Create temp file, if not already existing, download from the remote url and create file. Then, use
             * loadFromFile() method to load the file statements to the base model
             */
            tmpDir = File.createTempFile("uriqa", "").getParent();
            String[] split_url = url.split("/");
            final File file = new File(tmpDir + "/" + split_url[split_url.length - 1]);
            if (!file.exists()) {
                if (Log.isDebugEnabled())
                    Log.debug("loadFromUrl:File does not exist");
                downloadRemoteModel(url, file);
            }
            this.loadFromFile(file, baseURI);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Deprecated Use {@link Jena} {@link FileManager#loadModel(String)} Load into the base model the statements in the
     * file.
     * 
     * @param file The file, whose's statment's are to be loaded
     * @param baseURI the baseURI for the model.
     */
    @Deprecated
    public void loadFromFile(File file, String baseURI)
    {
        if (Log.isDebugEnabled())
            Log.debug("loadFromFile:file,baseURI:: " + file.getAbsolutePath() + "," + baseURI);
        model.enterCriticalSection(Lock.WRITE);
        try {
            model.read(new FileReader(file), baseURI);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            model.leaveCriticalSection();
        }
    }

    /**
     * Deprecated Use {@link Jena} {@link FileManager#loadModel(String)} Load from a file resource into the base model.
     * 
     * @param path The path of the file to be loaded.
     * @param baseURI The baseURI for the model.
     * @throws FileNotFoundException
     */
    @Deprecated
    public void loadFromResource(String path, String baseURI) throws FileNotFoundException
    {
        if (Log.isDebugEnabled())
            Log.debug("loadFromResource():path,baseURI:: " + path + "," + baseURI);
        InputStream pathStream = getClass().getClassLoader().getResourceAsStream(path);
        if (pathStream == null) {
            throw new FileNotFoundException(path);
        }
        model.enterCriticalSection(Lock.WRITE);
        try {
            model.read(getClass().getClassLoader().getResourceAsStream(path), baseURI);
        } finally {
            model.leaveCriticalSection();
        }
    }

    /**
     * Memory-model Initialization method. <br />
     * int hash for {@link UriqaRepoHandler#initializeRepo(int)} should be 0
     * 
     * @see UriqaRepoHandler#initializeRepo(int)
     */
    @Deprecated
    private void initializeRepo()
    {
        if (Log.isDebugEnabled())
            Log.debug("initializeRepo():deprecated");
        this.initializeRepo(0);
    }

    /**
     * Finalize method. Called when everything is shutting down. Do final syncs, commits.<br />
     * Specifically sync TDBModel to the filesystem, close base model, call system garbage collector. <br /> {@inheritDoc}
     * 
     * @see java.lang.Object#finalize()
     */
    protected void finalize() throws Throwable
    {
        if (Log.isDebugEnabled())
            Log.debug("finalize()");
        try {
            /*
             * Sync TDBmodel. Close base Model. Call System garbageCollector.
             */
            TDB.sync(model);
            model.close();
            System.gc();
        } finally {
            super.finalize();
        }
    }
}
