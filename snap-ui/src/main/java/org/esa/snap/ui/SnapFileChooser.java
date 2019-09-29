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

import com.bc.ceres.binding.ConversionException;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.converters.RectangleConverter;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.runtime.Config;
import org.esa.snap.vfs.NioFile;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepositoriesController;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;
import org.esa.snap.vfs.remote.VFSPath;
import org.esa.snap.vfs.ui.file.chooser.VirtualFileSystemView;
import sun.swing.FilePane;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * The general SNAP file chooser.
 *
 * @author Norman Fomferra
 */
public class SnapFileChooser extends JFileChooser {

    private static final Object syncFileFiltersObject = new Object();
    private static final String PREFERENCES_BOUNDS_OPEN = "snap.fileChooser.dialogBounds.open";
    private static final String PREFERENCES_BOUNDS_SAVE = "snap.fileChooser.dialogBounds.save";
    private static final String PREFERENCES_BOUNDS_CUSTOM = "snap.fileChooser.dialogBounds.custom";
    private static final String PREFERENCES_VIEW_TYPE = "snap.fileChooser.viewType";
    private final ResizeHandler resizeHandler;
    private final CloseHandler windowCloseHandler;
    private final Preferences snapPreferences;
    private String lastFilename;
    private Rectangle dialogBounds;

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

        snapPreferences = Config.instance("snap").preferences();
        resizeHandler = new ResizeHandler();
        windowCloseHandler = new CloseHandler();
        init();
    }

    @Override
    public void setFileSystemView(FileSystemView fileSystemView) {
        if (fileSystemView == null) {
            throw new NullPointerException("fileSystemView is null");
        }
        Path configFile = VFSRemoteFileRepositoriesController.getDefaultConfigFilePath();
        List<VFSRemoteFileRepository> vfsRepositories = null;
        try {
            vfsRepositories = VFSRemoteFileRepositoriesController.getVFSRemoteFileRepositories(configFile);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        if (vfsRepositories != null && !vfsRepositories.isEmpty()) {
            VirtualFileSystemView fileSystemViewWrapper = new VirtualFileSystemView(fileSystemView, vfsRepositories) {
                @Override
                protected void notifyUser(String title, String message) {
                    showMessageDialog(title, message);
                }
            };
            super.setFileSystemView(fileSystemViewWrapper);
        } else {
            super.setFileSystemView(fileSystemView);
        }
    }

    private void showMessageDialog(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public Icon getIcon(File f) {
        Icon icon = null;
        if (f != null) {
            if (f instanceof NioFile && f.toPath() instanceof VFSPath) {
                icon = getFileSystemView().getSystemIcon(f);
            } else {
                icon = super.getIcon(f);
                if (f.isDirectory() && isCompoundDocument(f)) {
                    return new CompoundDocumentIcon(icon);
                }
            }
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
        Rectangle dialogBounds = loadDialogBounds();
        if (dialogBounds != null) {
            dialog.setBounds(dialogBounds);
        }
        dialog.addComponentListener(resizeHandler);
        dialog.addWindowListener(windowCloseHandler);
        initViewType();

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
        this.dialogBounds = rectangle;
        storeDialogBounds(dialogBounds);
    }

    /**
     * @return The current filename, or {@code null}.
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
     * @param currentFilename The current filename, or {@code null}.
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
     * @return the current SNAP file filter, or {@code null}
     */
    public SnapFileFilter getSnapFileFilter() {
        FileFilter ff = getFileFilter();
        if (ff instanceof SnapFileFilter) {
            return (SnapFileFilter) ff;
        }
        return null;
    }

    /**
     * @return The current extension or {@code null} if it is unknown.
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
     * @return {@code true}, if the given file has a "known" extension
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

    @Override
    public String getName(File f) {
        String filename = null;
        if (f != null) {
            if (f instanceof NioFile && f.toPath() instanceof VFSPath) {
                filename = getFileSystemView().getSystemDisplayName(f);
            } else {
                filename = super.getName(f);
            }
        }
        return filename;
    }

    /**
     * Utility method which returns this file chooser's parent window.
     *
     * @return the parent window or {@code null}
     */
    protected Window getWindow() {
        Container w = this;
        while (!(w instanceof Window)) {
            w = w.getParent();
        }
        return (Window) w;
    }

    ///////////////////////////////////////////////////////////////////////////
    // private stuff
    ///////////////////////////////////////////////////////////////////////////

    private void init() {
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

    private void storeDialogBounds(Rectangle bounds) {
        int dialogType = getDialogType();
        switch (dialogType) {
            case OPEN_DIALOG:
                putRectangleToPreferences(PREFERENCES_BOUNDS_OPEN, bounds);
                break;
            case SAVE_DIALOG:
                putRectangleToPreferences(PREFERENCES_BOUNDS_SAVE, bounds);
                break;
            case CUSTOM_DIALOG:
            default:
                putRectangleToPreferences(PREFERENCES_BOUNDS_CUSTOM, bounds);
        }
    }

    private Rectangle loadDialogBounds() {
        switch (getDialogType()) {
            case OPEN_DIALOG:
                return getRectangleFromPreferences(PREFERENCES_BOUNDS_OPEN);
            case SAVE_DIALOG:
                return getRectangleFromPreferences(PREFERENCES_BOUNDS_SAVE);
            case CUSTOM_DIALOG:
            default:
                return getRectangleFromPreferences(PREFERENCES_BOUNDS_CUSTOM);
        }

    }

    private void putRectangleToPreferences(String key, Rectangle bounds) {
        snapPreferences.put(key, new RectangleConverter().format(bounds));
    }

    private Rectangle getRectangleFromPreferences(String key) {
        String rectString = snapPreferences.get(key, null);
        if (rectString != null) {
            try {
                Rectangle rectangle = new RectangleConverter().parse(rectString);
                GraphicsDevice[] screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
                for (GraphicsDevice screenDevice : screenDevices) {
                    if (screenDevice.getDefaultConfiguration().getBounds().contains(rectangle.getLocation())) {
                        return rectangle;
                    }
                }
            } catch (ConversionException e) {
                SystemUtils.LOG.log(Level.WARNING, "Not able to parse preferences value: " + rectString, e);
            }
        }
        return null;
    }

    private void initViewType() {
        FilePane filePane = findFilePane(this);
        if (filePane != null) {
            int viewType = snapPreferences.getInt(PREFERENCES_VIEW_TYPE, FilePane.VIEWTYPE_LIST);
            filePane.setViewType(viewType);
        }
    }

    private FilePane findFilePane(Container root) {
        Component[] components = root.getComponents();
        for (Component component : components) {
            if (component instanceof FilePane) {
                return (FilePane) component;
            }
            if (component instanceof Container) {
                FilePane filePane = findFilePane((Container) component);
                if (filePane != null) {
                    return filePane;
                }
            }
        }
        return null;
    }

    private static class CompoundDocumentIcon implements Icon {

        private static final Icon compoundDocumentIcon = new ImageIcon(CompoundDocumentIcon.class.getResource("CompoundDocument12.png"));
        private final Icon baseIcon;

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

    private class CloseHandler extends WindowAdapter {

        @Override
        public void windowClosed(WindowEvent e) {
            FilePane filePane = findFilePane(SnapFileChooser.this);
            if (filePane != null) {
                snapPreferences.putInt(PREFERENCES_VIEW_TYPE, filePane.getViewType());
                flushPreferences();
            }
        }

        private void flushPreferences() {
            try {
                snapPreferences.flush();
            } catch (BackingStoreException bse) {
                SystemUtils.LOG.severe("Could not store preferences: " + bse.getMessage());
            }
        }
    }
}
