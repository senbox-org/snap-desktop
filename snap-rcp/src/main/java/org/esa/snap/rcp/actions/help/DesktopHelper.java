package org.esa.snap.rcp.actions.help;

import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;

import java.awt.*;
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
            SnapDialogs.showError(String.format("Internal error: Invalid URI:\n%s", uriString));
            return;
        }

        try {
            desktop.browse(uri);
        } catch (IOException e) {
            SnapDialogs.showError(String.format("<html>Failed to open URL in browser:<br><a href=\"%s\">%s</a>",
                                                uriString, uriString));
        } catch (UnsupportedOperationException e) {
            SnapDialogs.showError("Sorry, it seems that there is no browser available.");
        }
    }


}
