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
import java.util.concurrent.BlockingQueue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

@SuppressWarnings("serial")
public class UriqaRepoHandler extends AbstractLifeCycle implements Serializable/*, ThreadPool*/ {

	private static UriqaRepoHandler sharedInstance = null;
	private BlockingQueue<Runnable> _jobQ;

	//	private static class Singleton {
	private final QueuedThreadPool __pool;
	//		static
	//		{
	//			try{
	//				System.out.println("starting SingleTon");
	//				__pool.start();}
	//			catch(Exception e){Log.warn(e); System.exit(1);}
	//		}
	//	}

	//private ModelFactory modelfactory;
	private static Model model = null;
	private final String INITIAL_REPO="org/eclipse/jetty/uriqa/commonwikiparsertestrdf.rdf";
	private String baseURI="http://localhost";

	public UriqaRepoHandler() throws Exception {
		__pool = new QueuedThreadPool(200);
		__pool.setName("venkatesh");
		this.start();
		__pool.start();
	}

	public UriqaRepoHandler(String baseURI) throws Exception {
		__pool = new QueuedThreadPool(200);
		__pool.setName("venkatesh");
		this.baseURI=baseURI;
		this.start();
		__pool.start();
	}

	public synchronized static UriqaRepoHandler getDefault()
	{
		if (sharedInstance == null) {
			try {
				sharedInstance = new UriqaRepoHandler();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return sharedInstance;
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void doStop() {
		sharedInstance = null;
	}

	//	public boolean dispatch(Runnable job) {
	//		// TODO Auto-generated method stub
	//		return false;
	//	}
	//
	//	public int getIdleThreads() {
	//		// TODO Auto-generated method stub
	//		return 0;
	//	}
	//
	//	public int getThreads() {
	//		// TODO Auto-generated method stub
	//		return 0;
	//	}
	//
	//	public boolean isLowOnThreads() {
	//		// TODO Auto-generated method stub
	//		return false;
	//	}
	//
	//	public void join() throws InterruptedException {
	//		// TODO Auto-generated method stub
	//		
	//	}

//	//TODO WTF is synchronized?? How do I use it for threads?
//	public void handleMget(String path, PrintWriter writer) {
//		//TODO Better response code if the resource was not actually found.
//		System.out.println("Requested resource: "+baseURI+path);
//		try{
//			//System.out.println("WTF");
//			//Thread.sleep(5000);
//			UriqaJob job=new UriqaJob(baseURI+path,null,writer,UriqaConstants.Methods.MGET);
//			//System.out.println("WFT1.5");
//			//Thread.sleep(5000);
//			//System.out.println("WTF2");
//			//TODO Draw and understand how this whole threads and synchronized things work.
//			//Thread.sleep(5000);
//			if (!/*Singleton.*/__pool.dispatch(job))
//			{
//				//System.out.println("WTF3");
//				//Thread.sleep(5000);
//				job.run();
//				//System.out.println("WTF4");
//				//Thread.sleep(5000);
//				//System.out.println("WTF5");
//			}
//			//System.out.println("WTF6");
//			//Thread.sleep(5000);
//			//System.out.println("WTF7");
//		}
//		catch(Exception e)
//		{
//			Log.warn(e);
//		}
//		//doGet(baseURI+path, writer);
//	}

//	public static void doGet(String baseURIPath, PrintWriter writer)
//	{
//		Resource tempResource = model.getResource(baseURIPath);
//		tempResource.getModel().write(writer,UriqaConstants.Lang.RDFXML);
//		tempResource = null;
//	}
//
//	public void handleMput(String path, HttpServletRequest request)
//	{
//		//TODO Better response code.
//		System.out.println("Putting resource: "+baseURI+path);
//		try{
//			UriqaJob job=new UriqaJob(baseURI+path,request,null,UriqaConstants.Methods.MPUT);			//TODO Draw and understand how this whole threads and synchronized things work.
//			if (!/*Singleton.*/__pool.dispatch(job))
//				job.run();
//		}
//		catch(Exception e)
//		{
//			Log.warn(e);
//		}
//
//	}
//
//	public static void doPut(String baseURIPath, HttpServletRequest request)
//	{
//		try {
//			model.read(request.getInputStream(), baseURIPath );
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public void handleMdelete(String path)
//	{
//		//TODO Better response code.
//		System.out.println("Deleting resource: "+baseURI+path);
//		try{
//			UriqaJob job=new UriqaJob(baseURI+path,null,null,UriqaConstants.Methods.MDELETE);
//			//TODO Draw and understand how this whole threads and synchronized things work.
//			if (!/*Singleton.*/__pool.dispatch(job))
//				job.run();
//		}
//		catch(Exception e)
//		{
//			Log.warn(e);
//		}
//	}
//
//	public static void doDelete(String baseURIpath)
//	{
//		Resource tempResource = model.getResource(baseURIpath);
//		synchronized(model)
//		{
//			model.remove(tempResource.getModel());
//		}
//		tempResource = null;
//	}

//	public class UriqaJob implements Runnable {
//
//		private String baseURIpath = null;
//		private HttpServletRequest request = null;
//		private PrintWriter writer = null;
//		private String method = null;
//
//		public UriqaJob (String baseURIpath, HttpServletRequest request, PrintWriter writer, String method)
//		{
//			this.baseURIpath = baseURIpath;
//			this.request = request;
//			this.writer = writer;
//			this.method = method;
//		}
//
//		public void run() {
//			System.out.println("JOB RUNNING");
//			if (method.equals(UriqaConstants.Methods.MGET))
//				doGet(baseURIpath,writer);
//			if (method.equals(UriqaConstants.Methods.MPUT))
//				doPut(baseURIpath,request);
//			if (method.equals(UriqaConstants.Methods.MDELETE))
//				doDelete(baseURIpath);
//		}
//
//	}
	
	public static void doGet2(Request baseRequest, HttpServletRequest request,
			PrintWriter writer)
	{
		System.out.println("DOGET2");
		Resource tempResource = model.getResource(request.getPathInfo().startsWith("/")?request.getPathInfo():"/"+request.getPathInfo());
		tempResource.getModel().write(writer,UriqaConstants.Lang.RDFXML);
		baseRequest.setHandled(true);
		((Request)request).setHandled(true);
		tempResource = null;
	}

	public class UriqaJob2 implements Runnable {

		private Request baseRequest = null;
		private HttpServletRequest request = null;
		private PrintWriter writer = null;
		private String method = null;

		public UriqaJob2(Request baseRequest, HttpServletRequest request,
				PrintWriter writer, String method)
		{
			this.baseRequest = baseRequest;
			this.request = request;
			this.writer = writer;
			this.method = method;
		}

		public void run() {
			System.out.println("JOB RUNNING");
			if (method.equals(UriqaConstants.Methods.MGET))
				doGet2(baseRequest, request, writer);
		}

	}
	
	public void handleMget2(Request baseRequest, HttpServletRequest request,
			PrintWriter writer) {
//		try{
			UriqaJob2 job=new UriqaJob2(baseRequest, request, writer,UriqaConstants.Methods.MGET);
//			if (!__pool.dispatch(job))
				job.run();
//		}
//		catch(Exception e)
//		{
//			Log.warn(e);
//		}
	}

}
