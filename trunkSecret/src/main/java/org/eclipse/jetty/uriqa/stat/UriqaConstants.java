package org.eclipse.jetty.uriqa.stat;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.uriqa.UriqaHandler;
import org.eclipse.jetty.uriqa.UriqaRepoHandler;

import com.hp.hpl.jena.rdf.model.InfModel;
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
    public static class Methods
    {
        public final static String MGET = "MGET", MPUT = "MPUT", MDELETE = "MDELETE", MQUERY = "MQUERY",
            MTRACE = "MTRACE";

        /**
         * This Map makes is easier/efficient while doing a compare for Methods.
         * 
         * @see UriqaHandler#handle(String, Request, javax.servlet.http.HttpServletRequest,
         *      javax.servlet.http.HttpServletResponse) where {@link Methods#map}'s {@link Map#containsValue(Object)} is
         *      being used.
         */
        public final static HashMap<String, String> map = new HashMap<String, String>(5)
        {
            {
                put("MGET", MGET);
                put("MPUT", MPUT);
                put("MDELETE", MDELETE);
                put("MQUERY", MQUERY);
                put("MTRACE", MTRACE);
            }
        };
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
    public static class Query
    {
        public final static String INSERT = "INSERT", MODIFY = "MODIFY", DELETE = "DELETE", SELECT = "SELECT",
            ASK = "ASK", CONSTRUCT = "CONSTRUCT", DESCRIBE = "DESCRIBE", LOAD = "LOAD", CLEAR = "CLEAR", DROP = "DROP",
            CREATE = "CREATE";

        /**
         * Hashmap of all the possible query types.
         */
        public final static HashMap<String, String> map = new HashMap<String, String>(11)
        {
            {
                put("INSERT", INSERT);
                put("MODIFY", MODIFY);
                put("DELETE", DELETE);
                put("SELECT", SELECT);
                put("ASK", ASK);
                put("CONSTRUCT", CONSTRUCT);
                put("DESCRIBE", DESCRIBE);
                put("LOAD", LOAD);
                put("CLEAR", CLEAR);
                put("DROP", DROP);
                put("CREATE", CREATE);
            }
        };

        /**
         * This Map makes is easier/efficient while doing a compare for Query types. This map contains all queries that
         * require {@link UpdateAction} Method to execute it. Check
         * {@link UriqaRepoHandler#doQuery(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, HashMap)}
         * where {@link Map#containsValue(Object)} is being used for efficient comparison.
         */
        public final static HashMap<String, String> UpdateActionQueries = new HashMap<String, String>(11)
        {
            {
                putAll(map);
                remove(ASK);
                remove(SELECT);
                remove(CONSTRUCT);
                remove(DESCRIBE);
            }
        };

        /**
         * Those queries which can be executed using only {@link UpdateAction} and do some sort of insertion/updation
         * (but NOT deletion/removal) from the model and therefore require {@link InfModel#rebind()} to be called later
         * upon.
         */
        public final static HashMap<String, String> rebindModelUpdateActionQueries = new HashMap<String, String>(11)
        {
            {
                put("INSERT", INSERT);
                put("MODIFY", MODIFY);
                put("LOAD", LOAD);
                put("CREATE", CREATE);
            }
        };
    }

}
