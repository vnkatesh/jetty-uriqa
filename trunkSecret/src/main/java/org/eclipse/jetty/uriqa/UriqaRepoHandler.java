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

import org.eclipse.jetty.util.component.AbstractLifeCycle;
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
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Derivation;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.update.UpdateAction;

@SuppressWarnings("serial")
public class UriqaRepoHandler extends AbstractLifeCycle implements Serializable{

	private static UriqaRepoHandler sharedInstance = null;

	private static Model base = null;
	private static OntModel model = null;
	//private final String INITIAL_REPO="org/eclipse/jetty/uriqa/w3.rdf";
	private URI baseURI=new URI("http://localhost");
	private String DBdirectory;

	public UriqaRepoHandler(String baseURI) throws Exception {
		if(baseURI != null)
			this.baseURI=new URI(baseURI);
		this.start();
	}

	public synchronized static UriqaRepoHandler getDefault()
	{
		return getDefault(null);
	}

	public synchronized static UriqaRepoHandler getDefault(String baseURI)
	{
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
		//In-Memory Model
		//this.initializeRepo();
		//TDB Model
		this.initializeRepo(System.getProperty("user.dir").toString().hashCode());
		model.register(new UriqaModelChangedListener());
	}

	private void initializeRepo(int hash) {
		if (hash == 0)
		{
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
			DBdirectory = "/home/venkatesh/UriqaDB_"+Integer.toString(hash);
			base = TDBFactory.createModel(DBdirectory);
			model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MINI_RULE_INF, base);
		}
		model.prepare();
		//TODO Confusion with setDerivationLogging, reasoner.setDerivationLogging, PROPderivationLogging
		model.setDerivationLogging(true);
		model.rebind();
		//model.add(FileManager.get().loadModel(INITIAL_REPO));
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

	public static void printModeltoConsole()
	{
		//TODO Language as a parameter and dynamic output.
		model.enterCriticalSection(Lock.READ);
		try {
			model.write(System.out, UriqaConstants.Lang.RDFXML);
		} finally {
			model.leaveCriticalSection();
		}

	}

	public void downloadRemoteModel(final String url, final File file)
	{
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
		sharedInstance = null;
	}

	public void handleRequest(HttpServletRequest request, HttpServletResponse response, String method, HashMap<String, String> paramMap) {
		//TODO Better response code if the resource was not actually found.
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
				doPut(baseURI.toString(), request, paramMap);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (method.equals(UriqaConstants.Methods.MDELETE))
			doDelete(baseURIpath);
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

		BufferedReader reader = request.getReader();
		int count = 0;
		String line;
		String queryString = new String();
		PrefixMappingImpl prefix = new PrefixMappingImpl();
		ByteArrayOutputStream bytearray = new ByteArrayOutputStream();
		PrintWriter out = new PrintWriter(bytearray);
		while ((line = reader.readLine()) != null)
		{
			queryString +=line;
			//TODO Create some actual parser maybe?
			//TODO Does not close properly!
			if (line.toLowerCase().startsWith("prefix")) {
				//tab character.
				prefix.setNsPrefix(line.split("	")[1].split(":")[0], line.split("	")[2].split(">")[0].split("<")[1]);
			} else if (line.toLowerCase().startsWith("trace") || line.toLowerCase().startsWith("\r\n") || line.toLowerCase().isEmpty()) {
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
					for (StmtIterator i = model.listStatements(subject, property, object); i.hasNext(); ) {
						Statement s = i.nextStatement();
						//statement is:
						out.println(s);
						for (Iterator id = model.getDerivation(s); id.hasNext(); ) {
							Derivation deriv = (Derivation) id.next();
							deriv.printTrace(out, true);
						}
					}
				} else {
					for (StmtIterator i = model.listStatements(subject, property, literal); i.hasNext(); ) {
						Statement s = i.nextStatement();
						out.println(s);
						for (Iterator id = model.getDerivation(s); id.hasNext(); ) {
							Derivation deriv = (Derivation) id.next();
							deriv.printTrace(out, true);
						}
					}
				}
			}

			queryString +="\n";
			count += line.length();
		}

		reader.close();
		if (reader.read() >= 0)
			throw new IllegalStateException("Not closed");

		out.flush();
		response.setContentLength(bytearray.toByteArray().length);
		response.getOutputStream().write(bytearray.toByteArray());
		response.getOutputStream().close();
	}

	public void doQuery(HttpServletRequest request, HttpServletResponse response, HashMap<String, String> paramMap)
	throws IOException, IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
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
			throw new IllegalStateException("Not closed");

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
						//TODO Error codes and response and validity report statements.
					} else {
						if (inference) {
							UpdateAction.parseExecute(queryString, model.getDeductionsModel());
						} else {
							UpdateAction.parseExecute(queryString, model.getRawModel());
						}
						model.notifyEvent(true);
					}
				} else {
					if (inference) {
						UpdateAction.parseExecute(queryString, model.getDeductionsModel());
					} else {
						UpdateAction.parseExecute(queryString, model.getRawModel());
					}
					model.notifyEvent(true);
				}
			} finally {
				model.leaveCriticalSection();
			}
			return ;
		}

		Query query = QueryFactory.create(queryString) ;
		// TODO Only select query, therefore - No Update queries and corresponding lock?
		model.enterCriticalSection(Lock.READ) ;
		try {
			QueryExecution qexec = null;
			if (paramMap.get(UriqaConstants.Parameters.INFERENCE).equals(UriqaConstants.Values.INC)) {
				qexec = QueryExecutionFactory.create(query, model.getDeductionsModel());
			} else {
				qexec = QueryExecutionFactory.create(query, model.getRawModel());
			}
			if (query.isSelectType())
			{
				ResultSet results = qexec.execSelect();
				if (paramMap.get(UriqaConstants.Parameters.FORMAT).equals(UriqaConstants.Lang.RDFXML)) {
					contentLengthPrint(response, ResultSetFormatter.class.getMethod("outputAsXML", new Class[] { OutputStream.class, ResultSet.class}), new Object[] {results}, null);
				}
				if (paramMap.get(UriqaConstants.Parameters.FORMAT).equals(UriqaConstants.Values.JSON))
					contentLengthPrint(response, ResultSetFormatter.class.getMethod("outputAsJSON", new Class[] { OutputStream.class, ResultSet.class}), new Object[] {results}, null);
			}
			if (query.isAskType())
			{
				Boolean answer = qexec.execAsk();
				if (paramMap.get(UriqaConstants.Parameters.FORMAT).equals(UriqaConstants.Lang.RDFXML))
					contentLengthPrint(response, ResultSetFormatter.class.getMethod("outputAsXML", new Class[] { OutputStream.class, Boolean.class}), new Object[] {answer}, null);
				if (paramMap.get(UriqaConstants.Parameters.FORMAT).equals(UriqaConstants.Values.JSON))
					contentLengthPrint(response, ResultSetFormatter.class.getMethod("outputAsJSON", new Class[] { OutputStream.class, Boolean.class}), new Object[] {answer}, null);
			}
			if (query.isConstructType()) {
				Model tempmodel = qexec.execConstruct();
				contentLengthPrint(response, Model.class.getMethod("write", new Class[] { OutputStream.class, String.class}),
						new Object[] {paramMap.get(UriqaConstants.Parameters.FORMAT), tempmodel}, null);
			}
			if (query.isDescribeType()) {
				Model tempmodel = qexec.execDescribe();
				contentLengthPrint(response, Model.class.getMethod("write", new Class[] { OutputStream.class, String.class}),
						new Object[] {paramMap.get(UriqaConstants.Parameters.FORMAT), tempmodel}, null);
			}
			qexec.close();
		} finally { model.leaveCriticalSection() ; }		
	}

	private void contentLengthPrint(HttpServletResponse response, Method method, Object[] arguments, ByteArrayOutputStream out)
	throws IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		boolean outProvided = true;
		if (out == null)
			outProvided = false;
		if (!outProvided)
			out = new ByteArrayOutputStream();
		ArrayList<Object> args = new ArrayList<Object>();
		if (Modifier.isStatic(method.getModifiers())){
			if (!outProvided)
				args.add(out);
			args.addAll(Arrays.asList(arguments));
			method.invoke(null, args.toArray());
		} else {
			if (!outProvided)
				args.add(out);
			args.addAll(Arrays.asList(arguments));
			args.remove(args.size()-1);
			method.invoke(arguments[arguments.length-1], args.toArray());
		}
		response.setContentLength(out.toByteArray().length);
		response.getOutputStream().write(out.toByteArray());
	}

	public void doGet(String baseURIPath, HttpServletResponse response, HashMap<String, String> paramMap)
	throws IOException, TransformerException, SAXException, ParserConfigurationException, IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
	{
		System.out.println("getting resource "+baseURIPath);
		//TODO Custom printModel for CBD. Understand? Check for Anonnodes and print CBD of them inside itself. 
		//TODO Its still printing the NodeID thing. Should I remove that?
		model.enterCriticalSection(Lock.READ);
		try {
			if (paramMap.get(UriqaConstants.Parameters.FORMAT).equals(UriqaConstants.Values.HTML))
			{
				if (paramMap.get(UriqaConstants.Parameters.INFERENCE).equals(UriqaConstants.Values.INC))
					rdf2html(getCBD(model.getResource(baseURIPath), model.getDeductionsModel()), response);
				else
					rdf2html(getCBD(model.getResource(baseURIPath), model.getRawModel()), response);
			}
			else
			{
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
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");

		InputSource iSource;
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		data.write(o, UriqaConstants.Lang.RDFXML);
		o.flush();
		String rdfxml = o.toString("UTF8");
		iSource = new InputSource(new StringReader(rdfxml));

		SAXParserFactory pFactory = SAXParserFactory.newInstance();
		pFactory.setNamespaceAware(true);
		pFactory.setValidating(false);
		XMLReader xmlReader = pFactory.newSAXParser().getXMLReader();

		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//W3C//DTD XHTML+RDFa 1.0//EN");
		transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF8");
		//TODO make contentPrintLength even more generalized to incorporate the below code:-
		ByteArrayOutputStream out2 = new ByteArrayOutputStream();
		contentLengthPrint(response, Transformer.class.getMethod("transform", Source.class, Result.class),
				new Object[] { new SAXSource(xmlReader, iSource), new StreamResult(out2), transformer }, out2);
	}

	private Model getCBD(Resource r, Model data) {
		data.enterCriticalSection(Lock.READ);
		try {
			StmtIterator iter = data.listStatements(r, null, (RDFNode) null);
			Model tempmodel = ModelFactory.createDefaultModel();
			while (iter.hasNext())
			{
				Statement stmt = iter.nextStatement();
				tempmodel.add(stmt);
				if (stmt.getObject().isAnon())
				{
					tempmodel.add(getClean((Resource) stmt.getObject(), data));
				}
				//TODO Reification stuff.
				//Maybe this link can help: http://jena.sourceforge.net/how-to/reification.html
				//TODO I'm still getting RDF:Node. Can I remove that using custom PrintModel?
				//TODO Remove getClean if it is redundant and same as getCBD.
				//			RSIterator iter2 =  model.listReifiedStatements(stmt);
				//			while(iter2.hasNext())
				//			{
				//				Statement stmt2 = iter2.nextRS().getStatement();
				//				System.out.println("Adding: "+stmt2.getSubject().getURI()+" -> "+ stmt2.getPredicate().getURI()+" -> "+stmt2.getObject().toString());
				//				tempmodel.add(stmt2);
				//			}
			}
			return tempmodel;
		} finally {
			data.leaveCriticalSection();
		}
	}

	private static Model getClean(Resource r, Model data) {
		data.enterCriticalSection(Lock.READ);
		try {
			StmtIterator iter = data.listStatements( r, null, (RDFNode) null);
			Model cleanModel = ModelFactory.createDefaultModel();
			while (iter.hasNext())
			{
				Statement stmt = iter.nextStatement();
				cleanModel.add(stmt);
				if (stmt.getObject().isAnon())
				{
					cleanModel.add(getClean((Resource) stmt.getObject(), data));
				}
			}
			return cleanModel;
		} finally {
			data.leaveCriticalSection();
		}
	}

	public void doPut(String baseURI, HttpServletRequest request, HashMap<String, String> paramMap) throws IOException
	{
		System.out.println("putting resource.");
		model.enterCriticalSection(Lock.WRITE);
		try {
			model.read(request.getInputStream(), baseURI, paramMap.get(UriqaConstants.Parameters.FORMAT));
			ValidityReport validity = model.validate();
			if (!validity.isValid()) {
				//TODO set response code.
				//TODO set response output as the errors.
				Model tempmodel = ModelFactory.createDefaultModel();
				tempmodel.read(request.getInputStream(), baseURI, paramMap.get(UriqaConstants.Parameters.FORMAT) );
				model.removeSubModel(tempmodel, false);
			}
			else {
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
	public void doDelete(String baseURIPath)
	{
		System.out.println("deleting resource"+baseURIPath);
		model.enterCriticalSection(Lock.WRITE);
		try {
			model.remove(getCBD(model.getResource(baseURIPath), model));
			model.notifyEvent(true);
		} finally {
			model.leaveCriticalSection();
		}
		//TODO the rdf:NodeID's still exist. Is that correct?
		//TODO the reification statments, should that come in printModeltoConsole()?
	}

	/**
	 * Deprecated
	 * Use Jena's FileManager()
	 */
	@Deprecated
	public void loadFromUrl(final String url, String baseURI)
	{
		String tmpDir;
		try {
			tmpDir = File.createTempFile("uriqa", "").getParent();
			String[] split_url = url.split("/");
			final File file = new File(tmpDir + "/" + split_url[split_url.length - 1]);
			if (!file.exists()) {
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
	public void loadFromFile(File file, String baseURI)
	{
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
	public void loadFromResource(String path, String baseURI) throws FileNotFoundException
	{
		InputStream pathStream = getClass().getClassLoader().getResourceAsStream(path);
		if (pathStream == null)
		{
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
	private void initializeRepo()
	{
		this.initializeRepo(0);
	}

	protected void finalize() throws Throwable {
		try {
			TDB.sync(model);
			model.close();
			System.gc();
		} finally {
			super.finalize();
		}
	}
}