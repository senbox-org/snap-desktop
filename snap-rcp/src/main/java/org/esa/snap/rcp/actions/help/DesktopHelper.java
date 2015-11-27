package org.esa.snap.rcp.actions.help;

import org.esa.snap.rcp.util.Dialogs;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Norman on 30.05.2015.
 */
class DesktopHelper {
    public static void browse(String uriString) {
        final Desktop desktop = Desktop.getDesktop();

        URI uri;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            Dialogs.showError(String.format("Internal error: Invalid URI:\n%s", uriString));
            return;
        }

        try {
            desktop.browse(uri);
        } catch (IOException e) {
            Dialogs.showError(String.format("<html>Failed to open URL in browser:<br><a href=\"%s\">%s</a>",
                                            uriString, uriString));
        } catch (UnsupportedOperationException e) {
            Dialogs.showError("Sorry, it seems that there is no browser available.");
        }
    }


}
