/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Objects;
import java.util.prefs.Preferences;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.converters.RectangleConverter;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.openide.util.NbPreferences;

/**
 * The general SNAP file chooser.
 *
 * @author Norman Fomferra
 */
public class SnapFileChooser extends JFileChooser {

    private static final Object syncFileFiltersObject = new Object();
    public static final int OPEN_DIALOG = 0;
    public static final int SAVE_DIALOG = 1;
    public static final String PREPERENCES_OPEN_DIALOG = "open.dialog.bounds";
    private static final String PREPERENCES_SAVE_DIALOG = "save.dialog.bounds";
    private String lastFilename;
    private Rectangle dialogBounds;
    private RectangleConverter rectangleConverter = new RectangleConverter();


    private ResizeHandler resizeHandler;
    private Preferences preferences;

    public SnapFileChooser() {
        this(null, null);
    }

    public SnapFileChooser(FileSystemView fsv) {
        this(null, fsv);
    }

    public SnapFileChooser(File currentDirectory) {
        this(currentDirectory, null);
    }

    public SnapFileChooser(File currentDirectory, FileSystemView fsv) {
        super(currentDirectory, fsv);
        init();
    }

    @Override
    public Icon getIcon(File f) {
        final Icon icon = super.getIcon(f);
        if (f.isDirectory() && isCompoundDocument(f)) {
            return new CompoundDocumentIcon(icon);
        }
        return icon;
    }

    @Override
    public boolean isTraversable(File f) {
        return f.isDirectory() && !isCompoundDocument(f);
    }

    /**
     * Overridden in order to recognize dialog size changes.
     *
     * @param parent the parent
     * @return the dialog
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns true.
     */
    @Override
    protected JDialog createDialog(Component parent) throws HeadlessException {
        final JDialog dialog = super.createDialog(parent);
        preferences = Objects.isNull(preferences) ? NbPreferences.forModule(getClass()) : preferences;
        dialog.addComponentListener(resizeHandler);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                setDialogPosition();
            }
        });
        Rectangle rectangleFromPreferences = getRectangleFromPreferences();
        if (Objects.nonNull(rectangleFromPreferences)) {
            setDialogBounds(rectangleFromPreferences);
        }

        if (Objects.nonNull(dialogBounds)) {
            dialog.setBounds(dialogBounds);
        }
        return dialog;
    }

    /**
     * Called by the UI when the user hits the Approve button (labeled "Open" or "Save", by default). This can also be
     * called by the programmer.
     */
    @Override
    public void approveSelection() {
        Debug.trace("SnapFileChooser: approveSelection(): selectedFile = " + getSelectedFile());
        Debug.trace("SnapFileChooser: approveSelection(): currentFilename = " + getCurrentFilename());
        Debug.trace("SnapFileChooser: approveSelection(): currentDirectory = " + getCurrentDirectory());
        if (getDialogType() != JFileChooser.OPEN_DIALOG) {
            ensureSelectedFileHasValidExtension();
        }
        super.approveSelection();
    }

    /**
     * Gets the dialog bounds to be used for the next {@link #showDialog(java.awt.Component, String)} call.
     *
     * @return the dialog bounds
     */
    public Rectangle getDialogBounds() {
        return dialogBounds;
    }

    /**
     * Sets the dialog bounds to be used for the next {@link #showDialog(java.awt.Component, String)} call.
     *
     * @param rectangle the dialog bounds
     */
    public void setDialogBounds(Rectangle rectangle) {
        if (rectangle.getX() < 0 || rectangle.getY() < 0 || rectangle.getWidth() < 0 || rectangle.getHeight() < 0) {
            GraphicsDevice[] screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            if (screenDevices.length > 1) {
                this.dialogBounds = rectangle;
            } else {
                this.dialogBounds = null;
            }
            return;
        }
        this.dialogBounds = rectangle;
    }

    /**
     * @return The current filename, or <code>null</code>.
     */
    public String getCurrentFilename() {
        File selectedFile = getSelectedFile();
        if (selectedFile != null) {
            return selectedFile.getName();
        }
        return null;
    }

    /**
     * Sets the current filename.
     *
     * @param currentFilename The current filename, or <code>null</code>.
     */
    public void setCurrentFilename(String currentFilename) {
        Debug.trace("SnapFileChooser: setCurrentFilename(\"" + currentFilename + "\")");
        String defaultExtension = getDefaultExtension();

        if (getDialogType() != JFileChooser.OPEN_DIALOG) {
            if (currentFilename != null && defaultExtension != null) {
                FileFilter fileFilter = getFileFilter();
                if (fileFilter instanceof SnapFileFilter) {
                    SnapFileFilter filter = (SnapFileFilter) fileFilter;
                    if (!filter.checkExtension(currentFilename)) {
                        currentFilename = FileUtils.exchangeExtension(currentFilename, defaultExtension);
                    }
                } else if (fileFilter instanceof FileNameExtensionFilter) {
                    FileNameExtensionFilter filter = (FileNameExtensionFilter) fileFilter;
                    if (!SnapFileFilter.checkExtensions(currentFilename, filter.getExtensions())) {
                        currentFilename = FileUtils.exchangeExtension(currentFilename, defaultExtension);
                    }
                }
            }
        }

        if (currentFilename != null && currentFilename.length() > 0) {
            setSelectedFile(new File(getCurrentDirectory(), currentFilename));
        }
    }

    /**
     * Returns the currently selected SNAP file filter.
     *
     * @return the current SNAP file filter, or <code>null</code>
     */
    public SnapFileFilter getSnapFileFilter() {
        FileFilter ff = getFileFilter();
        if (ff instanceof SnapFileFilter) {
            return (SnapFileFilter) ff;
        }
        return null;
    }

    /**
     * @return The current extension or <code>null</code> if it is unknown.
     */
    public String getDefaultExtension() {
        if (getSnapFileFilter() != null) {
            return getSnapFileFilter().getDefaultExtension();
        }
        return null;
    }


    /**
     * Checks whether or not the given filename with one of the known file extensions. The known file extension of this
     * file chooser are those, which are registered using a {@link SnapFileFilter}.
     *
     * @param filename the filename to be checked
     * @return <code>true</code>, if the given file has a "known" extension
     * @see SnapFileFilter
     */
    public boolean checkExtension(String filename) {
        if (filename != null) {
            FileFilter[] fileFilters = getChoosableFileFilters();
            if (fileFilters != null) {
                for (FileFilter filter : fileFilters) {
                    if (filter instanceof SnapFileFilter) {
                        final SnapFileFilter snapFileFilter = (SnapFileFilter) filter;
                        if (snapFileFilter.checkExtension(filename)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Utility method which returns this file chooser's parent window.
     *
     * @return the parent window or <code>null</code>
     */
    protected Window getWindow() {
        Container w = this;
        while (!(w instanceof Window)) {
            w = w.getParent();
        }
        return (Window) w;
    }

    private Rectangle getRectangleFromPreferences() {
        String rectanglePreferences = null;
        Rectangle rectangle = null;
        int dialogType = getDialogType();
        if (dialogType == OPEN_DIALOG) {
            rectanglePreferences = preferences.get(PREPERENCES_OPEN_DIALOG, null);
        } else if (dialogType == SAVE_DIALOG) {
            rectanglePreferences = preferences.get(PREPERENCES_SAVE_DIALOG, null);
        }
        if (Objects.isNull(rectanglePreferences)) {
            return null;
        }
        try {
            rectangle = rectangleConverter.parse(rectanglePreferences);
        } catch (com.bc.ceres.binding.ConversionException e) {
            e.printStackTrace();
        }
        return rectangle;
    }
    ///////////////////////////////////////////////////////////////////////////
    // private stuff
    ///////////////////////////////////////////////////////////////////////////

    private void init() {
        resizeHandler = new ResizeHandler();
        setAcceptAllFileFilterUsed(false);

        addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, evt -> {
            Object newValue = evt.getNewValue();
            if (newValue instanceof File) {
                lastFilename = ((File) newValue).getName();
            }
        });
        addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, evt -> {
            final SnapFileFilter snapFileFilter = getSnapFileFilter();
            if (snapFileFilter != null) {
                setFileSelectionMode(snapFileFilter.getFileSelectionMode().getValue());
            } else {
                setFileSelectionMode(FILES_ONLY);
            }

            if (getSelectedFile() != null) {
                return;
            }
            if (lastFilename == null || lastFilename.length() == 0) {
                return;
            }
            setCurrentFilename(lastFilename);
        });
        addActionListener(e -> {
            setDialogPosition();
        });
    }

    private void setDialogPosition() {
        Rectangle dialogBounds = getDialogBounds();
        int dialogType = getDialogType();
        if (dialogType == OPEN_DIALOG) {
            preferences.put(PREPERENCES_OPEN_DIALOG, dialogBounds.toString());
        } else if (dialogType == SAVE_DIALOG) {
            preferences.put(PREPERENCES_SAVE_DIALOG, dialogBounds.toString());
        }
    }

    private boolean isCompoundDocument(File file) {
        final FileFilter[] filters = getChoosableFileFilters();
        for (FileFilter fileFilter : filters) {
            if (fileFilter instanceof SnapFileFilter) {
                SnapFileFilter snapFileFilter = (SnapFileFilter) fileFilter;
                if (snapFileFilter.isCompoundDocument(file)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public FileFilter[] getChoosableFileFilters() {
        synchronized (syncFileFiltersObject) {
            return super.getChoosableFileFilters();
        }
    }

    @Override
    public void addChoosableFileFilter(FileFilter filter) {
        synchronized (syncFileFiltersObject) {
            super.addChoosableFileFilter(filter);
        }
    }

    private void ensureSelectedFileHasValidExtension() {
        File selectedFile = getSelectedFile();
        if (selectedFile != null) {
            SnapFileFilter mff = getSnapFileFilter();
            if (mff != null
                    && mff.getDefaultExtension() != null
                    && !mff.checkExtension(selectedFile)) {
                selectedFile = FileUtils.exchangeExtension(selectedFile, mff.getDefaultExtension());
                Debug.trace("mod. selected file: " + selectedFile.getPath());
                setSelectedFile(selectedFile);
            }
        }
    }

    private class ResizeHandler extends ComponentAdapter {

        @Override
        public void componentMoved(ComponentEvent e) {
            setDialogBounds(e.getComponent().getBounds());
        }

        @Override
        public void componentResized(ComponentEvent e) {
            setDialogBounds(e.getComponent().getBounds());
        }
    }

    private static class CompoundDocumentIcon implements Icon {

        private final Icon baseIcon;
        private static final Icon compoundDocumentIcon = new ImageIcon(
                CompoundDocumentIcon.class.getResource("CompoundDocument12.png"));

        public CompoundDocumentIcon(Icon baseIcon) {
            this.baseIcon = baseIcon;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            baseIcon.paintIcon(c, g, x, y);
            compoundDocumentIcon.paintIcon(c, g,
                    x + baseIcon.getIconWidth() - compoundDocumentIcon.getIconWidth(),
                    y + baseIcon.getIconHeight() - compoundDocumentIcon.getIconHeight());
        }

        @Override
        public int getIconWidth() {
            return baseIcon.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return baseIcon.getIconHeight();
        }
    }
}
