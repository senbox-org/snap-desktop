package org.esa.snap.ui.help;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

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
	
    /** Relative URL for the version.json file */
    private static final String VERSION_JSON_URL = "../../wp-content/help/versions/";
    
    /** The version.json file name */
    private static final String VERSION_JSON_FILE = "/version.json";
    
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
        final String fullURL = serverURL + "?helpid=" + helpCtx.getHelpID() + "&version=" + version;
        final String versionURL = serverURL + VERSION_JSON_URL + version + VERSION_JSON_FILE;
        if (checkServer(versionURL) && browse(fullURL)) {
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

    private static boolean checkServer(final String serverURL) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(serverURL);

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(1000);

            // try to open the connection
            final int responseCode = connection.getResponseCode();
            if (responseCode >= 400) {
                return false;
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
