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

package org.esa.snap.timeseries.ui;

import com.bc.ceres.swing.TableLayout;
import com.jidesoft.swing.FolderChooser;
import org.esa.snap.core.dataio.ProductIOPlugIn;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.timeseries.core.timeseries.datamodel.ProductLocation;
import org.esa.snap.timeseries.core.timeseries.datamodel.ProductLocationType;
import org.esa.snap.ui.SnapFileChooser;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Iterator;
import java.util.prefs.Preferences;

/**
 * UI element for managing product locations.
 * @author Marco Peters
 */
public class ProductLocationsPane extends JPanel {

    private static final String PROPERTY_KEY_LAST_OPEN_TS_DIR = "timeseries.file.lastOpenDir";
    public static final String ALL_FILES = "ALL_FILES";

    private ProductLocationsPaneModel model;
    private JList sourceList;
    private AbstractButton addButton;
    private AbstractButton removeButton;

    public ProductLocationsPane() {
        this(new DefaultProductLocationsPaneModel());
    }

    public ProductLocationsPane(ProductLocationsPaneModel model) {
        this.model = model;
        createPane();
    }

    public void setModel(ProductLocationsPaneModel model, boolean enabled) {
        this.model = model;
        updatePane(enabled);
    }

    private void createPane() {
        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setColumnFill(0, TableLayout.Fill.BOTH);
        tableLayout.setColumnFill(1, TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightY(1.0);
        tableLayout.setColumnWeightX(0, 1.0);
        tableLayout.setColumnWeightX(1, 0.0);
        tableLayout.setColumnWeightY(1, 0.0);
        tableLayout.setCellRowspan(0, 0, 2);
        setLayout(tableLayout);
        sourceList = new JList(model);
        sourceList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                if (value instanceof ProductLocation) {
                    final ProductLocation location = (ProductLocation) value;
                    String path = location.getPath();
                    if (location.getProductLocationType() != ProductLocationType.FILE) {
                        if (!path.endsWith(File.separator)) {
                            path += File.separator;
                        }
                    }
                    if (location.getProductLocationType() == ProductLocationType.DIRECTORY) {
                        path += "*";
                    }
                    if (location.getProductLocationType() == ProductLocationType.DIRECTORY_REC) {
                        path += "**";
                    }

                    label.setText(path);
                }
                return label;

            }
        });


        addButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Plus24.gif"), false);
        addButton.addActionListener(e -> {
            final JPopupMenu popup = new JPopupMenu("Add");
            final Rectangle buttonBounds = addButton.getBounds();
            popup.add(new AddDirectoryAction(false));
            popup.add(new AddDirectoryAction(true));
            popup.add(new AddFileAction());
            popup.show(addButton, 1, buttonBounds.height + 1);
        });

        removeButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Minus24.gif"), false);
        removeButton.addActionListener(e -> model.remove(sourceList.getSelectedIndices()));
        add(new JScrollPane(sourceList));
        add(addButton);
        add(removeButton, TableLayout.cell(1, 1));
    }

    private void updatePane(boolean enabled) {
        sourceList.setModel(model);
        addButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
    }

    private class AddDirectoryAction extends AbstractAction {

        private boolean recursive;

        private AddDirectoryAction(boolean recursive) {
            this("Add Directory" + (recursive ? " Recursive" : ""));
            this.recursive = recursive;
        }

        protected AddDirectoryAction(String title) {
            super(title);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final FolderChooser folderChooser = new FolderChooser();
            final Preferences preferences = SnapApp.getDefault().getPreferences();
            String lastDir = preferences.get(PROPERTY_KEY_LAST_OPEN_TS_DIR, SystemUtils.getUserHomeDir().getPath());

            folderChooser.setCurrentDirectory(new File(lastDir));

            final int result = folderChooser.showOpenDialog(ProductLocationsPane.this);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File currentDir = folderChooser.getSelectedFolder();
            model.addDirectory(currentDir, recursive);
            if (currentDir != null) {
                preferences.put(PROPERTY_KEY_LAST_OPEN_TS_DIR, currentDir.getAbsolutePath());
            }

        }
    }

    private class AddFileAction extends AbstractAction {

        private AddFileAction() {
            super("Add Product(s)");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final SnapApp snapApp = SnapApp.getDefault();
            final Preferences preferences = snapApp.getPreferences();
            String lastDir = preferences.get(PROPERTY_KEY_LAST_OPEN_TS_DIR, SystemUtils.getUserHomeDir().getPath());
            String lastFormat = preferences.get("app.file.lastOpenDir", ALL_FILES);
            // todo - decide where to put those properties
//            String lastFormat = preferences.get(VisatApp.PROPERTY_KEY_APP_LAST_OPEN_DIR, VisatApp.ALL_FILES_IDENTIFIER);
            SnapFileChooser fileChooser = new SnapFileChooser();
            fileChooser.setCurrentDirectory(new File(lastDir));
            fileChooser.setAcceptAllFileFilterUsed(true);
            fileChooser.setDialogTitle("Select Product(s)");
            fileChooser.setMultiSelectionEnabled(true);

            FileFilter actualFileFilter = fileChooser.getAcceptAllFileFilter();
            Iterator allReaderPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
            while (allReaderPlugIns.hasNext()) {
                final ProductIOPlugIn plugIn = (ProductIOPlugIn) allReaderPlugIns.next();
                SnapFileFilter productFileFilter = plugIn.getProductFileFilter();
                fileChooser.addChoosableFileFilter(productFileFilter);
                if (!ALL_FILES.equals(lastFormat) &&
                        productFileFilter.getFormatName().equals(lastFormat)) {
                    actualFileFilter = productFileFilter;
                }
            }
            fileChooser.setFileFilter(actualFileFilter);

            int result = fileChooser.showDialog(snapApp.getMainFrame(), "Open Product");    /*I18N*/
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }

            String currentDir = fileChooser.getCurrentDirectory().getAbsolutePath();
            preferences.put(PROPERTY_KEY_LAST_OPEN_TS_DIR, currentDir);

            if (fileChooser.getFileFilter() instanceof SnapFileFilter) {
                String currentFormat = ((SnapFileFilter) fileChooser.getFileFilter()).getFormatName();
                if (currentFormat != null) {
                    preferences.put("app.file.lastOpenFormat", currentFormat);
                    // todo - decide where to put those properties
//                    preferences.put(VisatApp.PROPERTY_KEY_APP_LAST_OPEN_FORMAT, currentFormat);
                }
            } else {
                preferences.put("app.file.lastOpenFormat", ALL_FILES);
            }
            final File[] selectedFiles = fileChooser.getSelectedFiles();
            model.addFiles(selectedFiles);
        }
    }
}
