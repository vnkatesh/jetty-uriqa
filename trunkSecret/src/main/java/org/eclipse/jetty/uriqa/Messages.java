package org.eclipse.jetty.uriqa;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

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
    private static final String BUNDLE_NAME = "org.eclipse.jetty.uriqa.messages"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

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
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }
}
