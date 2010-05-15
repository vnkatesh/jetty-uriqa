package org.eclipse.jetty.uriqa;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.component.AbstractLifeCycle;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.shared.Lock;

@SuppressWarnings("serial")
public class UriqaRepoHandler extends AbstractLifeCycle implements Serializable{

	private static UriqaRepoHandler sharedInstance = null;

	//private ModelFactory modelfactory;
	private static Model model = null;
	private final String INITIAL_REPO="org/eclipse/jetty/uriqa/w3.rdf";
	private String baseURI="http://localhost";

	public UriqaRepoHandler(String baseURI) throws Exception {
		if(baseURI != null)
			this.baseURI=baseURI;
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
		//getDefault();
		this.initializeRepo();
		//initialize repositories.
	}

	private void initializeRepo() {
		model.enterCriticalSection(Lock.WRITE);
		try {
			model = ModelFactory.createDefaultModel();
		} finally {
			model.leaveCriticalSection();
		}
		//model.add(FileManager.get().loadModel(INITIAL_REPO));
		//TODO Prefix j.1 has to be removed. for further compatibility with CBD.
		//		HashMap<String, String> map = new HashMap<String, String>();
		//		map.put("xmlns:rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		//		map.put("xmlns:rdfs","http://www.w3.org/2000/01/rdf-schema#");
		//		map.put("xmlns:owl","http://www.w3.org/2002/07/owl#");
		//		map.put("xmlns:dc","http://purl.org/dc/elements/1.1/");
		//		map.put("xmlns:dct","http://purl.org/dc/terms/");
		//		map.put("xmlns:xsd","http://www.w3.org/2001/XMLSchema#");
		//		map.put("xmlns:foaf","http://xmlns.com/foaf/0.1/");
		//		map.put("xmlns:ex","http://localhost/");
		//		model.setNsPrefixes(map);
		//this.loadFromResource(INITIAL_REPO, baseURI);
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

	public void handleRequest(HttpServletRequest request, HttpServletResponse response, String method) {
		//TODO Better response code if the resource was not actually found.
		String baseURIpath = baseURI+(request.getPathInfo().startsWith("/")?request.getPathInfo():"/"+request.getPathInfo());
		if (method.equals(UriqaConstants.Methods.MGET))
		{
			try {
				doGet(baseURIpath, response.getWriter());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		if (method.equals(UriqaConstants.Methods.MPUT))
		{
			try {
				doPut(baseURI, request);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (method.equals(UriqaConstants.Methods.MDELETE))
			doDelete(baseURIpath);
		if (method.equals(UriqaConstants.Methods.MQUERY))
		{
			try {
				doQuery(request, response.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void doQuery(HttpServletRequest request, ServletOutputStream output) throws IOException {
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

		Query query = QueryFactory.create(queryString) ;
		// TODO Only select query, therefore - No Update queries and corresponding lock?
		model.enterCriticalSection(Lock.READ) ;
		try {
			QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
			ResultSet results = qexec.execSelect();
			ResultSetFormatter.out(output, results, query);
			qexec.close();
		} finally { model.leaveCriticalSection() ; }		
	}

	private static void doGet(String baseURIPath, PrintWriter writer)
	{
		System.out.println("getting resource "+baseURIPath);
		//Resource tempResource = model.getResource(baseURIPath);
		//TODO Ok. tempResource.getModel() gets the parent model, not a model creation of tempResource. That pisses me now.
		//tempResource.getModel().write(writer,UriqaConstants.Lang.RDFXML);
		//TODO Custom printModel for CBD. Understand?
		//TODO Its still printing the NodeID thing. Should I remove that?
		model.enterCriticalSection(Lock.READ);
		try {
			getCBD(model.getResource(baseURIPath)).write(writer, UriqaConstants.Lang.RDFXML);
		} finally {
			model.leaveCriticalSection();
		}
		//tempResource = null;
	}

	private static Model getCBD(Resource r) {
		model.enterCriticalSection(Lock.READ);
		try {
			StmtIterator iter = model.listStatements(r, null, (RDFNode) null);
			Model tempmodel = ModelFactory.createDefaultModel();
			while (iter.hasNext())
			{
				Statement stmt = iter.nextStatement();
				tempmodel.add(stmt);
				if (stmt.getObject().isAnon())
				{
					tempmodel.add(getClean((Resource) stmt.getObject()));
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
			model.leaveCriticalSection();
		}
	}

	private static Model getClean(Resource r) {
		model.enterCriticalSection(Lock.READ);
		try {
			StmtIterator iter = model.listStatements( r, null, (RDFNode) null);
			Model cleanModel = ModelFactory.createDefaultModel();
			while (iter.hasNext())
			{
				Statement stmt = iter.nextStatement();
				cleanModel.add(stmt);
				if (stmt.getObject().isAnon())
				{
					cleanModel.add(getClean((Resource) stmt.getObject()));
				}
			}
			return cleanModel;
		} finally {
			model.leaveCriticalSection();
		}
	}

	private static void doPut(String baseURI, HttpServletRequest request) throws IOException
	{
		System.out.println("putting resource.");
		model.enterCriticalSection(Lock.WRITE);
		try {
			model.read(request.getInputStream(), baseURI );
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
	private static void doDelete(String baseURIPath)
	{
		//		TODO synchronized(model)
		System.out.println("deleting resource"+baseURIPath);
		model.enterCriticalSection(Lock.WRITE);
		try {
			model.remove(getCBD(model.getResource(baseURIPath)));
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

}
