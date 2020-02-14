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
package org.esa.snap.grapheditor.gpf.panels;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.util.ProductFunctions;
import org.esa.snap.grapheditor.gpf.panels.support.FileTable;
import org.esa.snap.grapheditor.gpf.panels.support.FileTableModel;
import org.esa.snap.grapheditor.gpf.panels.support.TargetFolderSelector;
import org.esa.snap.grapheditor.gpf.utils.DialogUtils;
import org.esa.snap.productlibrary.db.DBSearch;
import org.esa.snap.productlibrary.db.ProductEntry;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.file.OpenProductAction;
import org.esa.snap.rcp.actions.file.SaveProductAsAction;
import org.esa.snap.rcp.util.ProgressHandleMonitor;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.FileChooserFactory;
import org.esa.snap.ui.GridLayout2;
import org.netbeans.api.progress.ProgressUtils;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

/**
 * ProductSet Panel to handle source and target selection
 * User: lveci
 * Date: Feb 5, 2009
 */
public class ProductSetPanel extends JPanel implements TableModelListener {

    private final FileTable productSetTable;
    private final AppContext appContext;

    private JButton addButton = null, addAllOpenButton = null, dbQueryButton = null, removeButton = null;
    private JButton moveTopButton = null, moveUpButton = null, moveDownButton = null, moveBottomButton = null;
    private JButton refreshButton = null, clearButton = null;

    private final JLabel countLabel = new JLabel();

    private static final ImageIcon addIcon = TangoIcons.actions_list_add(TangoIcons.Res.R22);
    private static final ImageIcon addOpenedIcon = new ImageIcon(Objects.requireNonNull(ProductSetPanel.class.getClassLoader().
            getResource("org/esa/snap/grapheditor/icons/add-opened22.png")));
    private static final ImageIcon removeIcon = TangoIcons.actions_list_remove(TangoIcons.Res.R22);
    private static final ImageIcon searchIcon = TangoIcons.actions_system_search(TangoIcons.Res.R22);
    private static final ImageIcon moveTopIcon = TangoIcons.actions_go_top(TangoIcons.Res.R22);
    private static final ImageIcon moveUpIcon = TangoIcons.actions_go_up(TangoIcons.Res.R22);
    private static final ImageIcon moveDownIcon = TangoIcons.actions_go_down(TangoIcons.Res.R22);
    private static final ImageIcon moveBottomIcon = TangoIcons.actions_go_bottom(TangoIcons.Res.R22);
    private static final ImageIcon refreshIcon = TangoIcons.actions_view_refresh(TangoIcons.Res.R22);
    private static final ImageIcon clearIcon = TangoIcons.actions_edit_clear(TangoIcons.Res.R22);

    // Number of products to populate the table details with. More than this may slow down the user's experience
    private static final int AUTO_POPULATE_DETAILS_LIMIT = 100;

    public ProductSetPanel(final AppContext theAppContext, final String title, final FileTable fileTable,
                           final boolean incTrgProduct, final boolean incButtonPanel) {
        super(new BorderLayout());
        this.appContext = theAppContext;
        this.productSetTable = fileTable;
        setBorderTitle(title);

        final JPanel productSetContent = createComponent(productSetTable);
        if (incButtonPanel) {
            JPanel buttonPanel = createButtonPanel(productSetTable);
            productSetContent.add(buttonPanel, BorderLayout.EAST);
        }
        this.add(productSetContent, BorderLayout.CENTER);

        TargetFolderSelector targetProductSelector;
        if (incTrgProduct) {
            targetProductSelector = new TargetFolderSelector();
            final String homeDirPath = SystemUtils.getUserHomeDir().getPath();
            final String saveDir = SnapApp.getDefault().getPreferences().get(SaveProductAsAction.PREFERENCES_KEY_LAST_PRODUCT_DIR, homeDirPath);
            targetProductSelector.getModel().setProductDir(new File(saveDir));
            targetProductSelector.getOpenInAppCheckBox().setText("Open in " + theAppContext.getApplicationName());
            targetProductSelector.getOpenInAppCheckBox().setVisible(false);

            this.add(targetProductSelector.createPanel(), BorderLayout.SOUTH);
        }
        fileTable.getModel().addTableModelListener(this);

        updateComponents();
    }

    protected void setBorderTitle(final String title) {
        if (title != null)
            setBorder(BorderFactory.createTitledBorder(title));
    }

    private static JPanel createComponent(final FileTable table) {

        final JPanel fileListPanel = new JPanel(new BorderLayout(4, 4));

        final JScrollPane scrollPane = new JScrollPane(table);
        fileListPanel.add(scrollPane, BorderLayout.CENTER);

        return fileListPanel;
    }

    private void updateComponents() {

        final int rowCount = productSetTable.getFileCount();

        final boolean enableButtons = (rowCount > 0);
        if (dbQueryButton != null)
            dbQueryButton.setEnabled(enableButtons);
        if (removeButton != null)
            removeButton.setEnabled(enableButtons);
        if (moveTopButton != null)
            moveTopButton.setEnabled(rowCount > 1);
        if (moveUpButton != null)
            moveUpButton.setEnabled(rowCount > 1);
        if (moveDownButton != null)
            moveDownButton.setEnabled(rowCount > 1);
        if (moveBottomButton != null)
            moveBottomButton.setEnabled(rowCount > 1);
        if (refreshButton != null)
            refreshButton.setEnabled(rowCount > 1);
        if (clearButton != null)
            clearButton.setEnabled(enableButtons);

        if (addAllOpenButton != null) {
            addAllOpenButton.setEnabled(SnapApp.getDefault().getProductManager().getProducts().length > 0);
        }

        String cntMsg;
        if (rowCount == 1) {
            cntMsg = rowCount + " Product";
        } else {
            cntMsg = rowCount + " Products";
        }
        countLabel.setText(cntMsg);
    }

    private JPanel createButtonPanel(final FileTable table) {
        final FileTableModel tableModel = table.getModel();

        final JPanel panel = new JPanel(new GridLayout2(20, 1));

        addButton = DialogUtils.createButton("addButton", "Add", addIcon, panel, DialogUtils.ButtonStyle.Icon);
        addButton.addActionListener(e -> {
            final File[] files = getFilePath(addButton);
            if (files != null) {
                addProducts(tableModel, files);
            }
        });

        addAllOpenButton = DialogUtils.createButton("addAllOpenButton", "Add Opened", addOpenedIcon, panel, DialogUtils.ButtonStyle.Icon);
        addAllOpenButton.addActionListener(e -> addAllOpenProducts(tableModel));

        dbQueryButton = DialogUtils.createButton("dbQueryButton", "DB Query", searchIcon, panel, DialogUtils.ButtonStyle.Icon);
        dbQueryButton.addActionListener(e -> {
            try {
                final File mstFile = tableModel.getFileAt(0);
                if (mstFile.exists()) {
                    final ProductEntry[] entryList = DBSearch.search(mstFile);
                    for (ProductEntry entry : entryList) {
                        if (tableModel.getIndexOf(entry.getFile()) < 0)
                            tableModel.addFile(entry);
                    }
                }
            } catch (Exception ex) {
                appContext.handleError("Unable to query DB", ex);
            }
        });

        removeButton = DialogUtils.createButton("removeButton", "Remove", removeIcon, panel, DialogUtils.ButtonStyle.Icon);
        removeButton.addActionListener(e -> {
            final int rowCount = productSetTable.getFileCount();
            if (rowCount == 1) {
                tableModel.clear();
                return;
            }
            final int[] selRows = table.getSelectedRows();
            final java.util.List<File> filesToRemove = new ArrayList<>(selRows.length);
            for (int row : selRows) {
                filesToRemove.add(tableModel.getFileAt(row));
            }
            for (File file : filesToRemove) {
                int index = tableModel.getIndexOf(file);
                tableModel.removeFile(index);
            }
        });

        moveTopButton = DialogUtils.createButton("moveTopButton", "Move Top", moveTopIcon, panel, DialogUtils.ButtonStyle.Icon);
        moveTopButton.addActionListener(new MoveButtonActionListener(table, tableModel, MOVE.TOP));

        moveUpButton = DialogUtils.createButton("moveUpButton", "Move Up", moveUpIcon, panel, DialogUtils.ButtonStyle.Icon);
        moveUpButton.addActionListener(new MoveButtonActionListener(table, tableModel, MOVE.UP));

        moveDownButton = DialogUtils.createButton("moveDownButton", "Move Down", moveDownIcon, panel, DialogUtils.ButtonStyle.Icon);
        moveDownButton.addActionListener(new MoveButtonActionListener(table, tableModel, MOVE.DOWN));

        moveBottomButton = DialogUtils.createButton("moveBottomButton", "Move Bottom", moveBottomIcon, panel, DialogUtils.ButtonStyle.Icon);
        moveBottomButton.addActionListener(new MoveButtonActionListener(table, tableModel, MOVE.BOTTOM));

        refreshButton = DialogUtils.createButton("refreshButton", "Refresh", refreshIcon, panel, DialogUtils.ButtonStyle.Icon);
        refreshButton.addActionListener(e -> tableModel.refresh());

        clearButton = DialogUtils.createButton("clearButton", "Clear", clearIcon, panel, DialogUtils.ButtonStyle.Icon);
        clearButton.addActionListener(e -> tableModel.clear());

        panel.add(addButton);
        panel.add(addAllOpenButton);
        panel.add(removeButton);
        //panel.add(dbQueryButton); //todo
        panel.add(moveTopButton);
        panel.add(moveUpButton);
        panel.add(moveDownButton);
        panel.add(moveBottomButton);
        panel.add(refreshButton);
        panel.add(clearButton);
        panel.add(countLabel);

        return panel;
    }

    private static void addProducts(final FileTableModel tableModel, final File[] files) {
        final ProgressHandleMonitor pm = ProgressHandleMonitor.create("Populating table");
        Runnable operation = () -> {
            pm.beginTask("Populating table...", files.length);
            for (File file : files) {
                if (ProductFunctions.isValidProduct(file)) {
                    tableModel.addFile(file);
                }
                pm.worked(1);
            }
            if(files.length < AUTO_POPULATE_DETAILS_LIMIT) {
                tableModel.refresh();
            }
            pm.done();
        };

        ProgressUtils.runOffEventThreadWithProgressDialog(operation, "Adding Products", pm.getProgressHandle(), true, 50, 1000);
    }

    private static void addAllOpenProducts(final FileTableModel tableModel) {
        final ProgressHandleMonitor pm = ProgressHandleMonitor.create("Populating table");
        Runnable operation = () -> {
            final Product[] products = SnapApp.getDefault().getProductManager().getProducts();
            pm.beginTask("Populating table...", products.length);
            for (Product prod : products) {
                final File file = prod.getFileLocation();
                if (file != null && file.exists()) {
                    tableModel.addFile(file);
                }
                pm.worked(1);
            }
            if(products.length < AUTO_POPULATE_DETAILS_LIMIT) {
                tableModel.refresh();
            }

            pm.done();
        };

        ProgressUtils.runOffEventThreadWithProgressDialog(operation, "Adding Products", pm.getProgressHandle(), true, 50, 1000);
    }

    /**
     * This fine grain notification tells listeners the exact range
     * of cells, rows, or columns that changed.
     */
    public void tableChanged(TableModelEvent e) {
        updateComponents();
    }

    private static File[] getFilePath(Component component) {

        File[] files = null;
        final File openDir = new File(SnapApp.getDefault().getPreferences().
                get(OpenProductAction.PREFERENCES_KEY_LAST_PRODUCT_DIR, "."));
        final JFileChooser chooser = FileChooserFactory.getInstance().createFileChooser(openDir);
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Add Product");
        if (chooser.showDialog(component, "OK") == JFileChooser.APPROVE_OPTION) {
            files = chooser.getSelectedFiles();

            SnapApp.getDefault().getPreferences().
                    put(OpenProductAction.PREFERENCES_KEY_LAST_PRODUCT_DIR, chooser.getCurrentDirectory().getAbsolutePath());
        }
        return files;
    }

    private enum MOVE { UP, DOWN, TOP, BOTTOM }

    private static class MoveButtonActionListener implements ActionListener {

        private final FileTable table;
        private final FileTableModel tableModel;
        private final MOVE movement;

        MoveButtonActionListener(FileTable table, FileTableModel tableModel, MOVE movement) {
            this.table = table;
            this.tableModel = tableModel;
            this.movement = movement;
        }

        public void actionPerformed(final ActionEvent e) {
            final int[] selRows = table.getSelectedRows();
            final java.util.List<File> filesToMove = new ArrayList<>(selRows.length);
            for (int row : selRows) {
                filesToMove.add(tableModel.getFileAt(row));
            }
            int pos = 0;
            for (File file : filesToMove) {
                int index = tableModel.getIndexOf(file);
                if (index > 0 && movement.equals(MOVE.TOP)) {
                    tableModel.move(index, pos++);
                } else if (index > 0 && movement.equals(MOVE.UP)) {
                    tableModel.move(index, index - 1);
                } else if (index < tableModel.getRowCount() && movement.equals(MOVE.DOWN)) {
                    tableModel.move(index, index + 1);
                } else if (index < tableModel.getRowCount() && movement.equals(MOVE.BOTTOM)) {
                    tableModel.move(index, tableModel.getRowCount()-1);
                }
            }
        }
    }
}
