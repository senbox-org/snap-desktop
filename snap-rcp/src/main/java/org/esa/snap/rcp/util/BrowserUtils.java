/*
 * Copyright (C) 2016 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.rcp.util;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Methods for opening a URL in a browser.
 * Handles the case that {@link Desktop} is not supporting this.
 */
public class BrowserUtils {

    public static void openInBrowser(URI uri) {
        boolean desktopSupported = Desktop.isDesktopSupported();
        boolean browseSupported = Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
        if (desktopSupported && browseSupported) {
            try {
                Desktop.getDesktop().browse(uri);
            } catch (Throwable t) {
                Dialogs.showError(String.format("Failed to open URL:\n%s:\n%s", uri, t.getMessage()));
            }
        } else {
            SystemUtils.copyToClipboard(uri.toString());
            Dialogs.showInformation("The URL has been copied to your Clipboard\n");
        }
    }

    public static class URLClickAdaptor extends MouseAdapter {
        private final String address;
        public URLClickAdaptor(String address) {
            this.address = address;
        }
        @Override
        public void mouseClicked(MouseEvent event) {
            try {
                openInBrowser(new URI(address));
            } catch (URISyntaxException e) {
                SnapApp.getDefault().handleError("Unable to follow link", e);
            }
        }
    }
}
