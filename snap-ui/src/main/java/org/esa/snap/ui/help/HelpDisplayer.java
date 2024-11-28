package org.esa.snap.ui.help;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

import eu.esa.snap.netbeans.javahelp.api.Help;

/**
 * Helper class to invoke the help system.
 */
public class HelpDisplayer {

    /** URl for the online help. */
    private static final String DEFAULT_ONLINE_HELP_URL = "https://step.esa.int/main/doc/online-help";
	
    /** Relative URL for the version.json and toc.json files */
    private static final String VERSION_TOC_JSON_URL = "../../wp-content/help/versions/";
    
    /** The toc.json file name */
    private static final String TOC_JSON_FILE = "/toc.json";
    
    /**
     * Invoke the help system with the provided help ID.
     *
     * @param helpId the help ID to be shown, if null the default help will be shown
     */
    public static void show(String helpId) {
        show(helpId != null ? new HelpCtx(helpId) : null);
    }

    /**
     * Invoke the help system with the provided help context.
     *
     * @param helpCtx the context to be shown, if null the default help will be shown
     */
    public static void show(HelpCtx helpCtx) {
        final String serverURL = Config.instance().preferences().get("snap.online.help.url", DEFAULT_ONLINE_HELP_URL);
        final String version = SystemUtils.getReleaseVersion();
        String fullURL = serverURL + "?version=" + version;
        if (helpCtx != null) {
        	fullURL += "&helpid=" + helpCtx.getHelpID();
        }
        final String tocURL = serverURL + VERSION_TOC_JSON_URL + version + TOC_JSON_FILE;
        if (checkServer(tocURL, helpCtx != null ? helpCtx.getHelpID() : null) && browse(fullURL)) {
        	// Online help opened
        	return;
        }
    	
    	Help helpImpl = Lookup.getDefault().lookup(Help.class);
        if (helpImpl == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        helpImpl.showHelp(helpCtx);
    }

    /**
     * Open URL in default browser
     *
     * @param uriString the URL to open
     * @return true if the operation succeded, false if not
     */
    private static boolean browse(String uriString) {
        final Desktop desktop = Desktop.getDesktop();

        URI uri;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            return false;
        }

        try {
            desktop.browse(uri);
        } catch (IOException e) {
            return false;
        } catch (UnsupportedOperationException e) {
            return false;
        }

        return true;
    }

    private static boolean checkServer(final String serverURL, final String helpId) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(serverURL);

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(1000);
            connection.setInstanceFollowRedirects(true);

            // try to open the connection
            final int responseCode = connection.getResponseCode();
            if (responseCode >= 400) {
                return false;
            }
            
            if (helpId != null) {
                // get the content
            	final Pattern searchedHelpId = Pattern.compile(".*\"helpid\":\"" +helpId.replace("-", "\\-").replace(".", "\\.")+ "\".*");
                final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                if (reader.lines().filter(s -> searchedHelpId.matcher(s).matches()).count() == 0) {
                	return false;
                }
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return true;
    }
}
