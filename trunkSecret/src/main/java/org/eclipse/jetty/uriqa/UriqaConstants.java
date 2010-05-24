package org.eclipse.jetty.uriqa;

import java.io.OutputStream;

import org.eclipse.jetty.server.Request;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.UpdateAction;

/**
 * UriqaConstants contains a number of subclasses which are constants. They are grouped as <br>
 * <ul>
 * <li>Lang: Model Output/Input Languages/formats</li>
 * <li>Methods: Uriqa Methods </li
 * <li>Parameters: Uriqa Header parameters</li>
 * <li>Values: Possible header-parameters values for Uriqa</li>
 * <li>Query: Query types for MQuery</li>
 * </ul>
 * 
 * @author venkatesh
 * @version $Id$
 */
public class UriqaConstants
{

    /**
     * {@link UriqaConstants.Lang} for Model Input/Output Language formats. Used by Jena for
     * {@link Model#write(OutputStream, String)}
     * 
     * @version $Id$
     */
    public class Lang
    {
        public final static String RDFXML = "RDF/XML", RDFXMLA = "RDF/XML-ABBREV", NTRIPLE = "N-TRIPLE",
            TURTLE = "TURTLE", TTL = "TTL", N3 = "N3";
    }

    /**
     * {@link UriqaConstants.Methods} for matching {@link Request#getMethod()} with Uriqa headers
     * 
     * @version $Id$
     */
    public class Methods
    {
        public final static String MGET = "MGET", MPUT = "MPUT", MDELETE = "MDELETE", MQUERY = "MQUERY",
            MTRACE = "MTRACE";
    }

    /**
     * {@link UriqaConstants.Parameters} for Uriqa Headers parameters.
     * 
     * @version $Id$
     */
    public class Parameters
    {
        public final static String FORMAT = "format", NAMING = "naming", INFERENCE = "inference", URI = "URIQA-uri";
    }

    /**
     * {@link UriqaConstants.Values} for values of corresponding headers provided by {@link UriqaConstants.Parameters}
     * 
     * @version $Id$
     */
    public class Values
    {
        public final static String RDFXML = "application/rdf+xml", HTML = "text/html", JSON = "text/json",
            FACET = "application/rdf-facets", LABEL = "label", URI = "uri", INC = "include", EXC = "exclude";
    }

    /**
     * {@link UriqaConstants.Query} are all the varied possible query types for {@link com.hp.hpl.jena.query.Query} This
     * also includes the possiblity of the new {@link UpdateAction} Query types.
     * 
     * @version $Id$
     */
    public class Query
    {
        public final static String INSERT = "INSERT", MODIFY = "MODIFY", DELETE = "DELETE", SELECT = "SELECT",
            ASK = "ASK", CONSTRUCT = "CONSTRUCT", DESCRIBE = "DESCRIBE", LOAD = "LOAD", CLEAR = "CLEAR", DROP = "DROP",
            CREATE = "CREATE";
    }

}
