package org.eclipse.jetty.uriqa.stat;

import java.util.HashMap;

import org.eclipse.jetty.util.log.Log;

/**
 * Generic Uriqa Configuration class. They can be set by both messages.properties and configuration.xml files. To
 * retrieve values, either use the getters or the {@link UriqaConfig#map()} function. To set the values, use the 2
 * available constructors.
 * 
 * @author venkatesh
 * @version $Id$
 */
public class UriqaConfig
{

    /*
     * BaseURI that has to be set according to hostname.
     */
    private String URIQA_baseURI;

    /*
     * TDB based Models would be stored at {$URIQA_dbDirectory}/UriqaDB_<hash>
     */

    private String URIQA_dbDirectory;

    /*
     * Headers required for Format: text/html
     */
    private String URIQA_DOCTYPE_PUBLIC;

    private String URIQA_DOCTYPE_SYSTEM;

    private String URIQA_ENCODING;

    /*
     * Custom Error Message
     */
    private String URIQA_readerErrorMesage;

    /*
     * Use File-based TDB Model
     */
    private boolean URIQA_TDB;

    /*
     * Load Initial repository?
     */
    private boolean URIQA_LOAD;

    /*
     * If URIQA_LOAD=true AND model.isEmpty(), then which file to load?
     */
    private String URIQA_INITIAL_REPO;

    /*
     * Log.isDebugEnabled() is set by URIQA_DEBUG
     */
    private boolean URIQA_DEBUG;

    /*
     * Uriqa Ont Model reasoner specifications.
     */
    private String URIQA_ONT_MODEL_SPEC;

    /**
     * @return the uRIQA_baseURI
     */
    public final String getURIQA_baseURI()
    {
        if (Log.isDebugEnabled())
            Log.debug("getURIQA_baseURI()");
        return URIQA_baseURI;
    }

    /**
     * @param uRIQABaseURI the uRIQA_baseURI to set
     */
    public final void setURIQA_baseURI(String uRIQABaseURI)
    {
        if (Log.isDebugEnabled())
            Log.debug("setURIQA_baseURI(String): " + uRIQABaseURI);
        URIQA_baseURI = uRIQABaseURI;
    }

    /**
     * @return the uRIQA_dbDirectory
     */
    public final String getURIQA_dbDirectory()
    {
        if (Log.isDebugEnabled())
            Log.debug("getURIQA_dbDirectory()");
        return URIQA_dbDirectory;
    }

    /**
     * @param uRIQADbDirectory the uRIQA_dbDirectory to set
     */
    public final void setURIQA_dbDirectory(String uRIQADbDirectory)
    {
        if (Log.isDebugEnabled())
            Log.debug("setURIQA_dbDirectory(String): " + uRIQADbDirectory);
        URIQA_dbDirectory = uRIQADbDirectory;
    }

    /**
     * @return the uRIQA_DOCTYPE_PUBLIC
     */
    public final String getURIQA_DOCTYPE_PUBLIC()
    {
        if (Log.isDebugEnabled())
            Log.debug("getURIQA_DOCTYPE_PUBLIC()");
        return URIQA_DOCTYPE_PUBLIC;
    }

    /**
     * @param uRIQADOCTYPEPUBLIC the uRIQA_DOCTYPE_PUBLIC to set
     */
    public final void setURIQA_DOCTYPE_PUBLIC(String uRIQADOCTYPEPUBLIC)
    {
        if (Log.isDebugEnabled())
            Log.debug("setURIQA_DOCTYPE_PUBLIC(String): " + uRIQADOCTYPEPUBLIC);
        URIQA_DOCTYPE_PUBLIC = uRIQADOCTYPEPUBLIC;
    }

    /**
     * @return the uRIQA_DOCTYPE_SYSTEM
     */
    public final String getURIQA_DOCTYPE_SYSTEM()
    {
        if (Log.isDebugEnabled())
            Log.debug("getURIQA_DOCTYPE_SYSTEM()");
        return URIQA_DOCTYPE_SYSTEM;
    }

    /**
     * @param uRIQADOCTYPESYSTEM the uRIQA_DOCTYPE_SYSTEM to set
     */
    public final void setURIQA_DOCTYPE_SYSTEM(String uRIQADOCTYPESYSTEM)
    {
        if (Log.isDebugEnabled())
            Log.debug("setURIQA_DOCTYPE_SYSTEM(String): " + uRIQADOCTYPESYSTEM);
        URIQA_DOCTYPE_SYSTEM = uRIQADOCTYPESYSTEM;
    }

    /**
     * @return the uRIQA_ENCODING
     */
    public final String getURIQA_ENCODING()
    {
        if (Log.isDebugEnabled())
            Log.debug("getURIQA_ENCODING()");
        return URIQA_ENCODING;
    }

    /**
     * @param uRIQAENCODING the uRIQA_ENCODING to set
     */
    public final void setURIQA_ENCODING(String uRIQAENCODING)
    {
        if (Log.isDebugEnabled())
            Log.debug("setURIQA_ENCODING(String): " + uRIQAENCODING);
        URIQA_ENCODING = uRIQAENCODING;
    }

    /**
     * @return the uRIQA_readerErrorMesage
     */
    public final String getURIQA_readerErrorMesage()
    {
        if (Log.isDebugEnabled())
            Log.debug("getURIQA_readerErrorMessage()");
        return URIQA_readerErrorMesage;
    }

    /**
     * @param uRIQAReaderErrorMesage the uRIQA_readerErrorMesage to set
     */
    public final void setURIQA_readerErrorMesage(String uRIQAReaderErrorMesage)
    {
        if (Log.isDebugEnabled())
            Log.debug("setURIQA_readerErrorMessage(String): " + uRIQAReaderErrorMesage);
        URIQA_readerErrorMesage = uRIQAReaderErrorMesage;
    }

    /**
     * @return the uRIQA_TDB
     */
    public final boolean isURIQA_TDB()
    {
        if (Log.isDebugEnabled())
            Log.debug("isURIQA_TDB()");
        return URIQA_TDB;
    }

    /**
     * @param uRIQATDB the uRIQA_TDB to set
     */
    public final void setURIQA_TDB(boolean uRIQATDB)
    {
        if (Log.isDebugEnabled())
            Log.debug("setURIQA_TDB(boolean): " + uRIQATDB);
        URIQA_TDB = uRIQATDB;
    }

    /**
     * @return the uRIQA_LOAD
     */
    public final boolean isURIQA_LOAD()
    {
        if (Log.isDebugEnabled())
            Log.debug("isURIQA_LOAD()");
        return URIQA_LOAD;
    }

    /**
     * @param uRIQALOAD the uRIQA_LOAD to set
     */
    public final void setURIQA_LOAD(boolean uRIQALOAD)
    {
        if (Log.isDebugEnabled())
            Log.debug("setURIQA_LOAD(boolean): " + uRIQALOAD);
        URIQA_LOAD = uRIQALOAD;
    }

    /**
     * @return the uRIQA_INITIAL_REPO
     */
    public final String getURIQA_INITIAL_REPO()
    {
        if (Log.isDebugEnabled())
            Log.debug("getURIQA_INITIAL_REPO()");
        return URIQA_INITIAL_REPO;
    }

    /**
     * @param uRIQAINITIALREPO the uRIQA_INITIAL_REPO to set
     */
    public final void setURIQA_INITIAL_REPO(String uRIQAINITIALREPO)
    {
        if (Log.isDebugEnabled())
            Log.debug("setURIQA_INITIAL_REPO(String): " + uRIQAINITIALREPO);
        URIQA_INITIAL_REPO = uRIQAINITIALREPO;
    }

    /**
     * @return the uRIQA_DEBUG
     */
    public final boolean isURIQA_DEBUG()
    {
        if (Log.isDebugEnabled())
            Log.debug("isURIQA_DEBUG()");
        return URIQA_DEBUG;
    }

    /**
     * @param uRIQADEBUG the uRIQA_DEBUG to set
     */
    public final void setURIQA_DEBUG(boolean uRIQADEBUG)
    {
        if (Log.isDebugEnabled())
            Log.debug("setURIQA_DEBUG(boolean): " + uRIQADEBUG);
        URIQA_DEBUG = uRIQADEBUG;
    }

    /**
     * @return the uRIQA_ONT_MODEL_SPEC
     */
    public final String getURIQA_ONT_MODEL_SPEC()
    {
        if (Log.isDebugEnabled())
            Log.debug("getURIQA_ONT_MODEL_SPEC()");
        return URIQA_ONT_MODEL_SPEC;
    }

    /**
     * @param uRIQAONTMODELSPEC the uRIQA_ONT_MODEL_SPEC to set
     */
    public final void setURIQA_ONT_MODEL_SPEC(String uRIQAONTMODELSPEC)
    {
        if (Log.isDebugEnabled())
            Log.debug("setURIQA_ONT_MODEL_SPEC(String): " + uRIQAONTMODELSPEC);
        URIQA_ONT_MODEL_SPEC = uRIQAONTMODELSPEC;
    }

    /**
     * Return the hashmap-values of all the configuration parameters.
     * 
     * @return HashMap of all the values.
     */
    public HashMap<String, String> map()
    {
        return new HashMap<String, String>()
        {
            {
                put("URIQA_baseURI", URIQA_baseURI);

                put("URIQA_dbDirectory", URIQA_dbDirectory);

                put("URIQA_DOCTYPE_PUBLIC", URIQA_DOCTYPE_PUBLIC);

                put("URIQA_DOCTYPE_SYSTEM", URIQA_DOCTYPE_SYSTEM);

                put("URIQA_ENCODING", URIQA_ENCODING);

                put("URIQA_readerErrorMesage", URIQA_readerErrorMesage);

                put("URIQA_TDB", String.valueOf(URIQA_TDB));

                put("URIQA_LOAD", String.valueOf(URIQA_LOAD));

                put("URIQA_INITIAL_REPO", URIQA_INITIAL_REPO);

                put("URIQA_DEBUG", String.valueOf(URIQA_DEBUG));

                put("URIQA_ONT_MODEL_SPEC", URIQA_ONT_MODEL_SPEC);

            }
        };

    }

    /**
     * Constructor.
     * 
     * @param uRIQABaseURI
     * @param uRIQADbDirectory
     * @param uRIQADOCTYPEPUBLIC
     * @param uRIQADOCTYPESYSTEM
     * @param uRIQAENCODING
     * @param uRIQAReaderErrorMesage
     * @param uRIQATDB
     * @param uRIQALOAD
     * @param uRIQAINITIALREPO
     * @param uRIQADEBUG
     * @param uRIQAONTMODELSPEC
     */
    public UriqaConfig(String uRIQABaseURI, String uRIQADbDirectory, String uRIQADOCTYPEPUBLIC,
        String uRIQADOCTYPESYSTEM, String uRIQAENCODING, String uRIQAReaderErrorMesage, boolean uRIQATDB,
        boolean uRIQALOAD, String uRIQAINITIALREPO, boolean uRIQADEBUG, String uRIQAONTMODELSPEC)
    {
        URIQA_baseURI = uRIQABaseURI;
        URIQA_dbDirectory = uRIQADbDirectory;
        URIQA_DOCTYPE_PUBLIC = uRIQADOCTYPEPUBLIC;
        URIQA_DOCTYPE_SYSTEM = uRIQADOCTYPESYSTEM;
        URIQA_ENCODING = uRIQAENCODING;
        URIQA_readerErrorMesage = uRIQAReaderErrorMesage;
        URIQA_TDB = uRIQATDB;
        URIQA_LOAD = uRIQALOAD;
        URIQA_INITIAL_REPO = uRIQAINITIALREPO;
        URIQA_DEBUG = uRIQADEBUG;
        URIQA_ONT_MODEL_SPEC = uRIQAONTMODELSPEC;
    }

    /**
     * Constructor from a HashMap. This calls
     * {@link UriqaConfig#UriqaConfig(String, String, String, String, String, String, boolean, boolean, String, boolean, String)}
     * 
     * @param map HashMap from where values are read.
     */
    public UriqaConfig(HashMap<String, String> map)
    {
        this(map.get("URIQA_baseURI"), map.get("URIQA_dbDirectory"), map.get("URIQA_DOCTYPE_PUBLIC"), map
            .get("URIQA_DOCTYPE_SYSTEM"), map.get("URIQA_ENCODING"), map.get("URIQA_readerErrorMesage"), Boolean
            .parseBoolean(map.get("URIQA_TDB")), Boolean.parseBoolean(map.get("URIQA_LOAD")), map
            .get("URIQA_INITIAL_REPO"), Boolean.parseBoolean(map.get("URIQA_DEBUG")), map.get("URIQA_ONT_MODEL_SPEC"));
    }
}
