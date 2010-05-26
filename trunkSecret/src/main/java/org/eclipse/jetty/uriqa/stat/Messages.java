package org.eclipse.jetty.uriqa.stat;

import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jetty.util.log.Log;

/**
 * See also: {@link Messages#getString(String)}
 * 
 * @see Messages#getString(String)
 * @author venkatesh
 * @version $Id$
 */
public class Messages
{
    /**
     * The key-value configuration file path.
     */
    private static final String BUNDLE_NAME = "org.eclipse.jetty.uriqa.stat.messages"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    /**
     * Blank constructor
     */
    private Messages()
    {
    }

    /**
     * Returns the value of the key stored in the messages.properties file.
     * 
     * @param key
     * @return The value described by the key in the messages.properties file, !key! if not found
     */
    public static String getString(String key)
    {
        if (Log.isDebugEnabled())
            Log.debug("getString(key): " + key);
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    /**
     * Returns the hashmap of values from the resource-bundle.
     * 
     * @return Hashmap of values from the messages.properties file
     */
    public static HashMap<String, String> getMap()
    {
        if (Log.isDebugEnabled())
            Log.debug("getMap()");
        return new HashMap<String, String>()
        {
            {
                for (String key : RESOURCE_BUNDLE.keySet())
                    put(key, RESOURCE_BUNDLE.getString(key));
            }
        };
    }
}
