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

	public class Methods {
		public final static String MGET= "MGET",
		MPUT= "MPUT",
		MDELETE= "MDELETE",
		MQUERY= "MQUERY",
		MTRACE="MTRACE";
	}

	public class Parameters {
		public final static String FORMAT="format",
		NAMING= "naming",
		INFERENCE= "inference",
		URI="URIQA-uri";
	}

	public class Values {
		public final static String RDFXML="application/rdf+xml",
		HTML="text/html",
		JSON="text/json",
		FACET="application/rdf-facets",
		LABEL="label",
		URI="uri",
		INC="include",
		EXC="exclude";
	}
	
	public class Query {
		public final static String INSERT="INSERT",
		MODIFY="MODIFY",
		DELETE="DELETE",
		SELECT="SELECT",
		ASK="ASK",
		CONSTRUCT="CONSTRUCT",
		DESCRIBE="DESCRIBE",
		LOAD="LOAD",
		CLEAR="CLEAR",
		DROP="DROP",
		CREATE="CREATE";
	}
	
}
