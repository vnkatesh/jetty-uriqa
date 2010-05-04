package org.eclipse.jetty.uriqa;

public class UriqaConstants {

	public class Lang {
		public final static String
		RDFXML="RDF/XML", 
		RDFXMLA="RDF/XML-ABBREV",
		NTRIPLE="N-TRIPLE",
		TURTLE="TURTLE",
		TTL="TTL",
		N3="N3";
	}

	public class Methods
	{
		//	    public enum CACHE {
		//	    	MGET,MPUT,MDELETE
		//		}

		public final static String MGET= "MGET",
		MPUT= "MPUT",
		MDELETE= "MDELETE";

		//	    public final static int MGET_ORDINAL= 1,
		//	        MPUT_ORDINAL= 2,
		//	        MDELETE_ORDINAL= 3;

		//	    public final static BufferCache CACHE= new BufferCache();
		//
		//	    public final static Buffer 
		//	        MGET_BUFFER= CACHE.add(MGET, MGET_ORDINAL),
		//	        MPUT_BUFFER= CACHE.add(MPUT, MPUT_ORDINAL),
		//	        MDELETE_BUFFER= CACHE.add(MDELETE, MDELETE_ORDINAL);

	}

}
