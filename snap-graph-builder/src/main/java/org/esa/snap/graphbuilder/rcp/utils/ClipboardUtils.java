/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.graphbuilder.rcp.utils;

import org.esa.snap.core.util.SystemUtils;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.nio.file.Path;
import java.io.IOException;
import java.util.List;

/**
 * Utils for using the clipboard
 */
public class ClipboardUtils {

    /**
     * Copies the given text to the system clipboard.
     *
     * @param text the text to copy
     */
    public static void copyToClipboard(final String text) {
        final StringSelection selection = new StringSelection(text == null ? "" : text);
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clipboard != null) {
            clipboard.setContents(selection, selection);
        } else {
            SystemUtils.LOG.severe("failed to obtain clipboard instance");
        }
    }

    /**
     * Retrieves text from the system clipboard.
     *
     * @return string
     */
    public static String getClipboardString() throws IOException, UnsupportedFlavorException {
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clipboard != null) {
            return (String) clipboard.getData(DataFlavor.stringFlavor);
        }
        return null;
    }

    /**
     * Copies the given file list to the system clipboard.
     *
     * @param fileList the list to copy
     */
    public static void copyToClipboard(final Path[] fileList) {
        final FileListSelection selection = new FileListSelection(fileList);
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clipboard != null) {
            clipboard.setContents(selection, selection);
        } else {
            SystemUtils.LOG.severe("failed to obtain clipboard instance");
        }
    }

    /**
     * Retrieves a list of files from the system clipboard.
     *
     * @return file[] list
     */
    public static Path[] getClipboardFileList() throws IOException, UnsupportedFlavorException {
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clipboard != null) {
            final Object clipboardData = clipboard.getData(DataFlavor.javaFileListFlavor);
            if (clipboardData instanceof List<?> list) {
                final Path[] fileList = new Path[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    fileList[i] = (Path) list.get(i);
                }
                return fileList;
            }
        }
        return new Path[0];
    }
}
