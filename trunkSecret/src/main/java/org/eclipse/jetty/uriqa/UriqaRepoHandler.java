package org.eclipse.jetty.uriqa;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.util.component.AbstractLifeCycle;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

@SuppressWarnings("serial")
public class UriqaRepoHandler extends AbstractLifeCycle implements Serializable{

	private static UriqaRepoHandler sharedInstance = null;

	//private ModelFactory modelfactory;
	private static Model model = null;
	//private final String INITIAL_REPO="org/eclipse/jetty/uriqa/commonwikiparsertestrdf.rdf";
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
		model = ModelFactory.createDefaultModel();
		//		try {
		//			this.loadFromResource(INITIAL_REPO, baseURI);
		//		} catch (FileNotFoundException e) {
		//			e.printStackTrace();
		//		}
	}

	public void printModeltoOutput()
	{
		//TODO Language as a parameter and dynamic output.
		model.write(System.out, UriqaConstants.Lang.TURTLE);
	}

	public void loadFromResource(String path, String baseURI) throws FileNotFoundException
	{
		InputStream pathStream = getClass().getClassLoader().getResourceAsStream(path);
		if (pathStream == null)
		{
			throw new FileNotFoundException(path);
		}
		model.read(getClass().getClassLoader().getResourceAsStream(path), baseURI);
	}

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

	public void loadFromFile(File file, String baseURI)
	{
		try {
			model.read(new FileReader(file), baseURI);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void doStop() {
		sharedInstance = null;
	}

	public void handleRequest(HttpServletRequest request, PrintWriter writer, String method) {
		//TODO Better response code if the resource was not actually found.
		String baseURIpath = baseURI+(request.getPathInfo().startsWith("/")?request.getPathInfo():"/"+request.getPathInfo());
		if (method.equals(UriqaConstants.Methods.MGET))
			doGet(baseURIpath, writer);
		if (method.equals(UriqaConstants.Methods.MPUT))
			doPut(baseURIpath, request);
		if (method.equals(UriqaConstants.Methods.MDELETE))
			doDelete(baseURIpath);
	}

	public static void doGet(String baseURIPath, PrintWriter writer)
	{
		Resource tempResource = model.getResource(baseURIPath);
		tempResource.getModel().write(writer,UriqaConstants.Lang.RDFXML);
		tempResource = null;
	}

	public static void doPut(String baseURIPath, HttpServletRequest request)
	{
		try {
			model.read(request.getInputStream(), baseURIPath );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void doDelete(String baseURIpath)
	{
		Resource tempResource = model.getResource(baseURIpath);
		synchronized(model)
		{
			model.remove(tempResource.getModel());
		}
		tempResource = null;
	}

}
