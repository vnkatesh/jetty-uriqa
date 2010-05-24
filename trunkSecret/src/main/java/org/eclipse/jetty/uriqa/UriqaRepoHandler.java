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
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
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

@SuppressWarnings("serial")
public class UriqaRepoHandler extends AbstractLifeCycle implements Serializable{

	private static UriqaRepoHandler sharedInstance = null;

	private static Model base = null;
	private static OntModel model = null;
	private URI baseURI= null;

	public UriqaRepoHandler(String baseURI) {
		if (Log.isDebugEnabled())
			Log.debug("UriqaRepoHandler():baseURI:: " + baseURI );
		try {
			if(baseURI != null)
				this.baseURI = new URI(baseURI);
			else
				this.baseURI = new URI(Messages.getString("URIQA_baseURI"));
			if (Log.isDebugEnabled())
				Log.debug("UriqaRepoHandler():baseURI:: " + this.baseURI.toString());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		try {
			this.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized static UriqaRepoHandler getDefault()
	{
		if (Log.isDebugEnabled())
			Log.debug("getDefault(): null" );
		return getDefault(null);
	}

	public synchronized static UriqaRepoHandler getDefault(String baseURI)
	{
		if (Log.isDebugEnabled())
			Log.debug("getDefault(): baseURI " + baseURI );
		if (sharedInstance == null) {
			try {
				sharedInstance = new UriqaRepoHandler(baseURI);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return sharedInstance;
	}


	@Override
	public void doStart() {
		if (Log.isDebugEnabled())
			Log.debug("doStart()" );
		//In-Memory Model
		if (!(new Boolean(Messages.getString("URIQA_TDB"))).booleanValue())
			this.initializeRepo();
		else
			this.initializeRepo(System.getProperty("user.dir").toString().hashCode());
		model.register(new UriqaModelChangedListener());
		if (Log.isDebugEnabled())
			Log.debug("doStart():modelListener:: registered" );
	}

	private void initializeRepo(int hash) {
		if (Log.isDebugEnabled())
			Log.debug("initializeRepo(): hash " + hash );
		if (hash == 0)
		{
			if (Log.isDebugEnabled())
				Log.debug("initializeRepo(): MemoryModel");
			if (model !=null)
			{
				model.enterCriticalSection(Lock.WRITE);
				try {
					model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MINI_RULE_INF);
				} finally {
					model.leaveCriticalSection();
				}
			}
			else
				model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MINI_RULE_INF);
		}
		else
		{
			String DBdirectory = Messages.getString("URIQA_dbDirectory")+"UriqaDB_"+Integer.toString(hash);
			base = TDBFactory.createModel(DBdirectory);
			model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MINI_RULE_INF, base);
			if (Log.isDebugEnabled())
				Log.debug("initializeRepo(): TDB Model at "+DBdirectory );
		}
		model.prepare();
		model.setDerivationLogging(true);
		model.rebind();
		if (model.isEmpty() && (new Boolean(Messages.getString("URIQA_LOAD"))).booleanValue()) {
			model.add(FileManager.get().loadModel(Messages.getString("URIQA_INITIAL_REPO")));
			if (Log.isDebugEnabled())
				Log.debug("initializeRepo(): FileManager.loadModel from file: "+ Messages.getString("URIQA_INITIAL_REPO"));
		}
		//TODO Prefix j.1 has to be removed. for further compatibility with CBD.
		HashMap<String, String> map = new HashMap<String, String>(8);
		map.put("rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		map.put("rdfs","http://www.w3.org/2000/01/rdf-schema#");
		map.put("owl","http://www.w3.org/2002/07/owl#");
		map.put("dc","http://purl.org/dc/elements/1.1/");
		map.put("dct","http://purl.org/dc/terms/");
		map.put("xsd","http://www.w3.org/2001/XMLSchema#");
		map.put("foaf","http://xmlns.com/foaf/0.1/");
		map.put(baseURI.getHost(),baseURI.toString());
		model.setNsPrefixes(map);
	}

	public String printModeltoConsole()
	{
		if (Log.isDebugEnabled())
			Log.debug("printModeltoConsole()" );
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		model.enterCriticalSection(Lock.READ);
		try {
			model.write(stream, UriqaConstants.Lang.RDFXML);
		} finally {
			model.leaveCriticalSection();
		}
		return stream.toString();

	}

	public void downloadRemoteModel(final String url, final File file)
	{
		if (Log.isDebugEnabled())
			Log.debug("downloadRemoteModel(): url file " + url + "," + file.getAbsolutePath() );
		try {
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

	public void doStop() {
		if (Log.isDebugEnabled())
			Log.debug("doStop()" );
		sharedInstance = null;
	}

	public void handleRequest(HttpServletRequest request, HttpServletResponse response, String method, HashMap<String, String> paramMap) {
		if (Log.isDebugEnabled())
			Log.debug("handleRequest():request,response,method,paramMap:: " + request.toString() + "," + response.toString() + "," + method + paramMap.toString());
		String baseURIpath = baseURI.toString()+(request.getPathInfo().startsWith("/")?request.getPathInfo():"/"+request.getPathInfo());
		if (method.equals(UriqaConstants.Methods.MGET))
		{
			try {
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
		if (method.equals(UriqaConstants.Methods.MPUT))
		{
			try {
				doPut(baseURI.toString(), request, paramMap, response);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (method.equals(UriqaConstants.Methods.MDELETE))
			doDelete(baseURIpath, response);
		if (method.equals(UriqaConstants.Methods.MTRACE))
		{
			try {
				doDerive(request, response);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		if (method.equals(UriqaConstants.Methods.MQUERY))
		{
			try {
				try {
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

	public void doDerive(HttpServletRequest request, HttpServletResponse response) throws IOException {

		if (Log.isDebugEnabled())
			Log.debug("doDerive():request,response:: " + request.toString() + "," + response.toString() );
		BufferedReader reader = request.getReader();
		String line;
		String queryString = new String();
		PrefixMappingImpl prefix = new PrefixMappingImpl();
		ByteArrayOutputStream bytearray = new ByteArrayOutputStream();
		PrintWriter out = new PrintWriter(bytearray);
		while ((line = reader.readLine()) != null)
		{
			queryString +=line;
			//TODO Create some actual parser maybe?
			//TODO Does not close properly. Just checked with others also. Same behaviour. Is that normal??
			if (line.toLowerCase().startsWith("prefix")) {
				if (Log.isDebugEnabled())
					Log.debug("doDerive():Prefix" );
				//tab character.
				prefix.setNsPrefix(line.split("	")[1].split(":")[0], line.split("	")[2].split(">")[0].split("<")[1]);
			} else if (line.toLowerCase().startsWith("trace") || line.toLowerCase().startsWith("\r\n") || line.toLowerCase().isEmpty()) {
				if (Log.isDebugEnabled())
					Log.debug("doDerive():Ignore" );
				//ignore
			} else {
				//tab
				String[] q = line.split("	");
				Resource subject = null;
				Property property = null;
				String literal = null;
				Resource object = null;
				subject = model.getResource(prefix.getNsPrefixURI(q[0].split(":")[0])+q[0].split(":")[1]);
				property = model.getProperty(prefix.getNsPrefixURI(q[1].split(":")[0])+q[1].split(":")[1]);
				if (q[2].contains(":"))
					object = model.getResource(prefix.getNsPrefixURI(q[2].split(":")[0])+q[2].split(":")[1]);
				else
					literal = q[2];
				if (object!=null) {
					if (Log.isDebugEnabled())
						Log.debug("doDerive():s,p,object" );
					if (model.listStatements(subject, property, object).toList().size() > 0) {
						if (Log.isDebugEnabled())
							Log.debug("doDerive(): 200" );
						response.setStatus(HttpServletResponse.SC_OK);
					} else {
						if (Log.isDebugEnabled())
							Log.debug("doDerive(): 404" );
						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						response.setContentLength(0);
						response.getOutputStream().close();
						out.close();
						bytearray.close();
						return;
					}
					for (StmtIterator i = model.listStatements(subject, property, object); i.hasNext(); ) {
						Statement s = i.nextStatement();
						//statement is:
						out.println(s);
						for (Iterator<Derivation> id = model.getDerivation(s); id.hasNext(); ) {
							Derivation deriv = (Derivation) id.next();
							deriv.printTrace(out, true);
						}
					}
				} else {
					if (Log.isDebugEnabled())
						Log.debug("doDerive():s,p,literal" );
					if (model.listStatements(subject, property, literal).toList().size() > 0) {
						if (Log.isDebugEnabled())
							Log.debug("doDerive(): 200" );
						response.setStatus(HttpServletResponse.SC_OK);
					} else {
						if (Log.isDebugEnabled())
							Log.debug("doDerive(): 404" );
						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						response.setContentLength(0);
						response.getOutputStream().close();
						out.close();
						bytearray.close();
						return;
					}
					for (StmtIterator i = model.listStatements(subject, property, literal); i.hasNext(); ) {
						Statement s = i.nextStatement();
						out.println(s);
						for (Iterator<Derivation> id = model.getDerivation(s); id.hasNext(); ) {
							Derivation deriv = (Derivation) id.next();
							deriv.printTrace(out, true);
						}
					}
				}
			}

			queryString +="\n";
			if (Log.isDebugEnabled())
				Log.debug("doDerive():queryString:: " +queryString );
		}

		reader.close();
		if (reader.read() >= 0)
			throw new IllegalStateException(Messages.getString("URIQA_readerErrorMesage"));

		out.flush();
		response.setContentType(MimeTypes.TEXT_PLAIN);
		response.setContentLength(bytearray.toByteArray().length);
		response.getOutputStream().write(bytearray.toByteArray());
		bytearray.close();
		out.close();
		response.getOutputStream().close();
	}

	public void doQuery(HttpServletRequest request, HttpServletResponse response, HashMap<String, String> paramMap)
	throws IOException, IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if (Log.isDebugEnabled())
			Log.debug("doQuery():request,response,paramMap:: " + request.toString() + "," + response.toString() + "," + paramMap.toString() );
		BufferedReader reader = request.getReader();
		int count = 0;
		String line;
		String queryString = new String();

		while ((line = reader.readLine()) != null)
		{
			queryString +=line;
			queryString +="\n";
			count += line.length();
		}

		reader.close();
		if (reader.read() >= 0)
			throw new IllegalStateException(Messages.getString("URIQA_readerErrorMesage")); 

		if (Log.isDebugEnabled())
			Log.debug("doQuery:queryString:: " + queryString);

		//TODO Any other efficient way to compare them?
		if (queryString.toUpperCase().contains(UriqaConstants.Query.INSERT) || queryString.toUpperCase().contains(UriqaConstants.Query.DELETE) 
				|| queryString.toUpperCase().contains(UriqaConstants.Query.MODIFY) || queryString.toUpperCase().contains(UriqaConstants.Query.LOAD) 
				|| queryString.toUpperCase().contains(UriqaConstants.Query.CLEAR) || queryString.toUpperCase().contains(UriqaConstants.Query.DROP)
				|| queryString.toUpperCase().contains(UriqaConstants.Query.CREATE)) {

			model.enterCriticalSection(Lock.WRITE);
			try {
				boolean inference = paramMap.get(UriqaConstants.Parameters.INFERENCE).equals(UriqaConstants.Values.INC);
				if (queryString.toUpperCase().contains(UriqaConstants.Query.INSERT)	|| queryString.toUpperCase().contains(UriqaConstants.Query.MODIFY)
						|| queryString.toUpperCase().contains(UriqaConstants.Query.LOAD) || queryString.toUpperCase().contains(UriqaConstants.Query.CREATE)) {
					OntModel tempmodel = ModelFactory.createOntologyModel();
					tempmodel.add(model);
					if (inference) {
						UpdateAction.parseExecute(queryString, tempmodel.getDeductionsModel());
					} else {
						UpdateAction.parseExecute(queryString, tempmodel.getRawModel());
					}
					if (!tempmodel.validate().isValid()) {
						if (Log.isDebugEnabled())
							Log.debug("doQuery:INSERT::conflict()");
						response.setStatus(HttpServletResponse.SC_CONFLICT);
						String error = "";
						for (Iterator<Report> i = tempmodel.validate().getReports(); i.hasNext(); ) {
							error+=" - " + i.next()+ "\r\n";
						}
						response.setContentLength(error.length());
						response.setContentType(MimeTypes.TEXT_PLAIN);
						response.getWriter().write(error);						
					} else {
						if (inference) {
							UpdateAction.parseExecute(queryString, model.getDeductionsModel());
						} else {
							UpdateAction.parseExecute(queryString, model.getRawModel());
						}
						//similar to doDelete().
						if (Log.isDebugEnabled())
							Log.debug("doQuery():INSERT::OK");
						response.setContentLength(0);
						response.setStatus(HttpServletResponse.SC_NO_CONTENT);
						model.notifyEvent(true);
					}
				} else {
					if (inference) {
						UpdateAction.parseExecute(queryString, model.getDeductionsModel());
					} else {
						UpdateAction.parseExecute(queryString, model.getRawModel());
					}
					if (Log.isDebugEnabled())
						Log.debug("doQuery:DELETE:OK");
					response.setContentLength(0);
					response.setStatus(HttpServletResponse.SC_NO_CONTENT);
					model.notifyEvent(true);
				}
			} finally {
				model.leaveCriticalSection();
			}
			return ;
		}

		Query query = QueryFactory.create(queryString) ;
		model.enterCriticalSection(Lock.READ) ;
		try {
			response.setStatus(HttpServletResponse.SC_OK);
			QueryExecution qexec = null;
			if (paramMap.get(UriqaConstants.Parameters.INFERENCE).equals(UriqaConstants.Values.INC)) {
				qexec = QueryExecutionFactory.create(query, model.getDeductionsModel());
			} else {
				qexec = QueryExecutionFactory.create(query, model.getRawModel());
			}
			if (query.isSelectType()) {
				if (Log.isDebugEnabled())
					Log.debug("doQuery:isSelect()");
				ResultSet results = qexec.execSelect();
				if (paramMap.get(UriqaConstants.Parameters.FORMAT).equals(UriqaConstants.Lang.RDFXML)) {
					response.setContentType(UriqaConstants.Lang.RDFXML);
					contentLengthPrint(response, ResultSetFormatter.class.getMethod("outputAsXML", new Class[] { OutputStream.class, ResultSet.class}), new Object[] {results}, null);
				}
				if (paramMap.get(UriqaConstants.Parameters.FORMAT).equals(UriqaConstants.Values.JSON)) {
					response.setContentType(MimeTypes.TEXT_JSON);
					contentLengthPrint(response, ResultSetFormatter.class.getMethod("outputAsJSON", new Class[] { OutputStream.class, ResultSet.class}), new Object[] {results}, null);
				}
			}
			if (query.isAskType()) {
				if (Log.isDebugEnabled())
					Log.debug("");
				Boolean answer = qexec.execAsk();
				if (paramMap.get(UriqaConstants.Parameters.FORMAT).equals(UriqaConstants.Lang.RDFXML)) {
					response.setContentType(UriqaConstants.Lang.RDFXML);
					contentLengthPrint(response, ResultSetFormatter.class.getMethod("outputAsXML", new Class[] { OutputStream.class, Boolean.class}), new Object[] {answer}, null);
				}
				if (paramMap.get(UriqaConstants.Parameters.FORMAT).equals(UriqaConstants.Values.JSON)) {
					response.setContentType(MimeTypes.TEXT_JSON);
					contentLengthPrint(response, ResultSetFormatter.class.getMethod("outputAsJSON", new Class[] { OutputStream.class, Boolean.class}), new Object[] {answer}, null);
				}
			}
			if (query.isConstructType()) {
				if (Log.isDebugEnabled())
					Log.debug("doQuery:isConstruct");
				Model tempmodel = qexec.execConstruct();
				response.setContentType(paramMap.get(UriqaConstants.Parameters.FORMAT));
				contentLengthPrint(response, Model.class.getMethod("write", new Class[] { OutputStream.class, String.class}),
						new Object[] {paramMap.get(UriqaConstants.Parameters.FORMAT), tempmodel}, null);
			}
			if (query.isDescribeType()) {
				if (Log.isDebugEnabled())
					Log.debug("doQuery:isDescribe()");
				Model tempmodel = qexec.execDescribe();
				response.setContentType(paramMap.get(UriqaConstants.Parameters.FORMAT));
				contentLengthPrint(response, Model.class.getMethod("write", new Class[] { OutputStream.class, String.class}),
						new Object[] {paramMap.get(UriqaConstants.Parameters.FORMAT), tempmodel}, null);
			}
			qexec.close();
		} finally {
			model.leaveCriticalSection() ;
		}		
	}

	private void contentLengthPrint(HttpServletResponse response, Method method, Object[] arguments, ByteArrayOutputStream out)
	throws IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (Log.isDebugEnabled())
			Log.debug("contentLengthPrint():response,method,arguments,out:: " + response.toString() + "," + method + "," + arguments.toString() + ", " + out.toString());
		if (Log.isDebugEnabled())
			Log.debug("contentLengthPrint():method:: " + method.getName() );
		boolean outProvided = true;
		if (out == null)
			outProvided = false;
		if (!outProvided)
			out = new ByteArrayOutputStream();
		if (Log.isDebugEnabled())
			Log.debug("contentLengthPrint():outProvided:: " + outProvided);
		ArrayList<Object> args = new ArrayList<Object>();
		if (Modifier.isStatic(method.getModifiers())){
			if (!outProvided)
				args.add(out);
			if (Log.isDebugEnabled())
				Log.debug("contentLengthPrint():isStatic()");
			args.addAll(Arrays.asList(arguments));
			method.invoke(null, args.toArray());
		} else {
			if (!outProvided)
				args.add(out);
			if (Log.isDebugEnabled())
				Log.debug("contentLengthPrint():isNotStatic()");
			args.addAll(Arrays.asList(arguments));
			args.remove(args.size()-1);
			method.invoke(arguments[arguments.length-1], args.toArray());
		}
		response.setContentLength(out.toByteArray().length);
		response.getOutputStream().write(out.toByteArray());
		if (Log.isDebugEnabled())
			Log.debug("contentPrintLength: output:\r\n " + out.toString());
	}

	public void doGet(String baseURIPath, HttpServletResponse response, HashMap<String, String> paramMap)
	throws IOException, TransformerException, SAXException, ParserConfigurationException, IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if (Log.isDebugEnabled())
			Log.debug("doGet():baseURIPath,response,paramMap " + baseURIPath + "," + response.toString() + "," + paramMap.toString());
		//TODO Custom printModel for CBD. Understand? Check for Anonnodes and print CBD of them inside itself. 
		//TODO Its still printing the NodeID thing. Should I remove that?
		model.enterCriticalSection(Lock.READ);
		try {
			if (!model.contains(model.getResource(baseURIPath), null, (RDFNode) null)) {
				//Not present in model.
				response.setContentLength(0);
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				if (Log.isDebugEnabled())
					Log.debug("doGet(): 404");
				return;
			}
			if (paramMap.get(UriqaConstants.Parameters.FORMAT).equals(UriqaConstants.Values.HTML)) {
				if (Log.isDebugEnabled())
					Log.debug("doGet():FORMAT::HTML");
				response.setContentType(MimeTypes.TEXT_HTML);
				if (paramMap.get(UriqaConstants.Parameters.INFERENCE).equals(UriqaConstants.Values.INC))
					rdf2html(getCBD(model.getResource(baseURIPath), model.getDeductionsModel()), response);
				else
					rdf2html(getCBD(model.getResource(baseURIPath), model.getRawModel()), response);
			} else {
				if (Log.isDebugEnabled())
					Log.debug("doGet():FORMAT:: " + paramMap.get(UriqaConstants.Parameters.FORMAT));
				response.setContentType(paramMap.get(UriqaConstants.Parameters.FORMAT));
				if (paramMap.get(UriqaConstants.Parameters.INFERENCE).equals(UriqaConstants.Values.INC))
					contentLengthPrint(response, Model.class.getMethod("write", new Class[] { OutputStream.class, String.class}),
							new Object[] {paramMap.get(UriqaConstants.Parameters.FORMAT), getCBD(model.getResource(baseURIPath), model.getDeductionsModel())}, null);
				else
					contentLengthPrint(response, Model.class.getMethod("write", new Class[] { OutputStream.class, String.class}),
							new Object[] {paramMap.get(UriqaConstants.Parameters.FORMAT), getCBD(model.getResource(baseURIPath), model.getRawModel())}, null);
			}
		} finally {
			model.leaveCriticalSection();
		}
	}

	private void rdf2html(Model data, HttpServletResponse response)
	throws IOException, TransformerException, SAXException, ParserConfigurationException, IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException 
	{
		if (Log.isDebugEnabled())
			Log.debug("rdf2html():data,response:: " + data.toString() + "," + response.toString());
		response.setCharacterEncoding("UTF-8");
		response.setContentType(MimeTypes.TEXT_HTML);

		InputSource iSource;
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		data.write(o, UriqaConstants.Lang.RDFXML);
		o.flush();
		String rdfxml = o.toString(Messages.getString("URIQA_ENCODING"));
		iSource = new InputSource(new StringReader(rdfxml));

		SAXParserFactory pFactory = SAXParserFactory.newInstance();
		pFactory.setNamespaceAware(true);
		pFactory.setValidating(false);
		XMLReader xmlReader = pFactory.newSAXParser().getXMLReader();

		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, Messages.getString("URIQA_DOCTYPE_PUBLIC"));
		transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, Messages.getString("URIQA_DOCTYPE_SYSTEM"));
		transformer.setOutputProperty(OutputKeys.ENCODING, Messages.getString("URIQA_ENCODING"));
		ByteArrayOutputStream out2 = new ByteArrayOutputStream();
		contentLengthPrint(response, Transformer.class.getMethod("transform", Source.class, Result.class),
				new Object[] { new SAXSource(xmlReader, iSource), new StreamResult(out2), transformer }, out2);
	}

	private Model getCBD(Resource r, Model data) {
		if (Log.isDebugEnabled())
			Log.debug("getCBD():resource,data:: " + r.toString() + "," + data.toString());
		data.enterCriticalSection(Lock.READ);
		try {
			StmtIterator iter = data.listStatements(r, null, (RDFNode) null);
			if (Log.isDebugEnabled())
				Log.debug("getCBD():Number of statments: "+ iter.toList().size());
			Model tempmodel = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
			while (iter.hasNext())
			{
				Statement stmt = iter.nextStatement();
				tempmodel.add(stmt);
				if (Log.isDebugEnabled())
					Log.debug("getCBD():addStatment: " + stmt.getResource().getURI() + "->" + stmt.getPredicate().getURI() + "->" + stmt.getObject().toString());
				if (stmt.getObject().isAnon()) {
					tempmodel.add(getClean((Resource) stmt.getObject(), data));
				}
				//TODO Reification producing orphan nodes. Is that OK? isDefinedBy() is not getting added. Required?
				//TODO I'm still getting RDF:Node. Can I remove that using custom PrintModel?
				RSIterator iter2 =  model.listReifiedStatements(stmt);
				if (Log.isDebugEnabled())
					Log.debug("getCBD():Number of reified: "+ iter2.toList().size());
				while(iter2.hasNext())
				{
					Statement stmt2 = iter2.nextRS().getStatement();
					tempmodel.createReifiedStatement(stmt2);
					if (Log.isDebugEnabled())
						Log.debug("getCBD():addStatment: " + stmt2.getResource().getURI() + "->" + stmt2.getPredicate().getURI() + "->" + stmt2.getObject().toString());
				}
			}
			return tempmodel;
		} finally {
			data.leaveCriticalSection();
		}
	}

	private static Model getClean(Resource r, Model data) {
		if (Log.isDebugEnabled())
			Log.debug("getClean():resource,data:: " + r.toString() + "," + data.toString());
		data.enterCriticalSection(Lock.READ);
		try {
			StmtIterator iter = data.listStatements( r, null, (RDFNode) null);
			if (Log.isDebugEnabled())
				Log.debug("getClean():Number of statments: "+ iter.toList().size());
			Model cleanModel = ModelFactory.createDefaultModel();
			while (iter.hasNext())
			{
				Statement stmt = iter.nextStatement();
				cleanModel.add(stmt);
				if (Log.isDebugEnabled())
					Log.debug("getClean():addStatment: " + stmt.getResource().getURI() + "->" + stmt.getPredicate().getURI() + "->" + stmt.getObject().toString());
				if (stmt.getObject().isAnon()) {
					cleanModel.add(getClean((Resource) stmt.getObject(), data));
				}
				//TODO Assuming Anonymous nodes don't have reified statements. Or, do they?
			}
			return cleanModel;
		} finally {
			data.leaveCriticalSection();
		}
	}

	public void doPut(String baseURI, HttpServletRequest request, HashMap<String, String> paramMap, HttpServletResponse response) throws IOException {
		if (Log.isDebugEnabled())
			Log.debug("doPut():baseURI,request,paramMap,response:: " + baseURI + "," + request.toString() + "," + paramMap.toString() + "," + response.toString());
		model.enterCriticalSection(Lock.WRITE);
		try {
			model.read(request.getInputStream(), baseURI, paramMap.get(UriqaConstants.Parameters.FORMAT));
			ValidityReport validity = model.validate();
			if (!validity.isValid()) {
				if (Log.isDebugEnabled())
					Log.debug("doPut: Conflict");
				response.setStatus(HttpServletResponse.SC_CONFLICT);
				String error = "";
				for (Iterator<Report> i = validity.getReports(); i.hasNext(); ) {
					error+=" - " + i.next()+ "\r\n";
				}
				response.setContentLength(error.length());
				response.setContentType(MimeTypes.TEXT_PLAIN);
				response.getWriter().write(error);
				Model tempmodel = ModelFactory.createDefaultModel();
				tempmodel.read(request.getInputStream(), baseURI, paramMap.get(UriqaConstants.Parameters.FORMAT) );
				model.removeSubModel(tempmodel, false);
			} else {
				if (Log.isDebugEnabled())
					Log.debug("doPut(): 200");
				response.setContentLength(0);
				response.setStatus(HttpServletResponse.SC_NO_CONTENT);
				model.notifyEvent(true);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JenaException e) {
			//TODO Premature end of file: response code something else.
		} finally {
			model.leaveCriticalSection();
		}
	}

	/**
	 * see TODO's of {@link UriqaRepoHandler#doGet(String, PrintWriter)}
	 */
	public void doDelete(String baseURIPath, HttpServletResponse response) {
		if (Log.isDebugEnabled())
			Log.debug("doDelete():baseURIpath,response:: " + baseURIPath + "," + response.toString());
		model.enterCriticalSection(Lock.WRITE);
		try {
			model.remove(getCBD(model.getResource(baseURIPath), model));
			model.notifyEvent(true);
		} finally {
			model.leaveCriticalSection();
		}
		if (Log.isDebugEnabled())
			Log.debug("doDelete(): 200");
		response.setContentLength(0);
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
		//TODO the rdf:NodeID's still exist. Is that correct?
		//TODO the reification statments, should that come in printModeltoConsole()?
	}

	/**
	 * Deprecated
	 * Use Jena's FileManager()
	 */
	@Deprecated
	public void loadFromUrl(final String url, String baseURI) {
		if (Log.isDebugEnabled())
			Log.debug("loadFromUrl():url,baseURI:: " + url + "," + baseURI);
		String tmpDir;
		try {
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
	 * Deprecated
	 * Use Jena's FileManager()
	 */
	@Deprecated
	public void loadFromFile(File file, String baseURI) {
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
	 * Deprecated
	 * Use Jena's FileManager()
	 */
	@Deprecated
	public void loadFromResource(String path, String baseURI) throws FileNotFoundException {
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
	 * 
	 * Use TDB.
	 */
	@Deprecated
	private void initializeRepo() {
		if (Log.isDebugEnabled())
			Log.debug("initializeRepo():deprecated");
		this.initializeRepo(0);
	}

	protected void finalize() throws Throwable {
		if (Log.isDebugEnabled())
			Log.debug("finalize()");
		try {
			TDB.sync(model);
			model.close();
			System.gc();
		} finally {
			super.finalize();
		}
	}
}