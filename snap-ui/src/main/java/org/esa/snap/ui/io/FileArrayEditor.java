/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.ui.io;

import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.SnapFileChooser;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * An UI-Component which represents a product file list with the ability to add and remove files.
 */
public class FileArrayEditor {

    private static final Dimension LIST_PREFERRED_SIZE = new Dimension(500, 200);

    private JPanel basePanel;
    private JFileChooser fileDialog;
    private FileArrayEditorListener listener;

    private final JList<File> listComponent;
    private final List<File> fileList;
    private final EditorParent parent;
    private final String label;

    /**
     * Constructs the object with default values
     *
     * @param parent the parent editor
     * @param label  the label for this editor
     */
    public FileArrayEditor(final EditorParent parent, String label) {
        Guardian.assertNotNullOrEmpty("label", label);
        this.parent = parent;
        this.label = label;
        fileList = new ArrayList<>();
        listComponent = new JList<>();
        setName(listComponent, this.label);
    }

    protected final EditorParent getParent() {
        return parent;
    }

    /**
     * Retrieves the editor UI.
     *
     * @return the editor UI
     */
    public JComponent getUI() {
        if (basePanel == null) {
            createUI();
        }

        return basePanel;
    }

    /**
     * Sets the list of files to be edited. The list currently held is overwritten.
     *
     * @param files {@code List} of {@code File}s to be set
     */
    public void setFiles(final List<File> files) {
        Guardian.assertNotNull("files", files);
        fileList.clear();
        fileList.addAll(files);
        listComponent.setListData(fileList.toArray(new File[fileList.size()]));
        notifyListener();
    }

    /**
     * Retrieves the list of files currently edited
     *
     * @return a {@code List} of currently edited {@code File}s
     */
    public List<File> getFiles() {
        return fileList;
    }

    /**
     * Sets the listener for this class
     *
     * @param listener the listener to associate with this editor
     */
    public void setListener(final FileArrayEditorListener listener) {
        this.listener = listener;
    }

    ///////////////////////////////////////////////////////////////////////////
    ////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Creates the user interface
     */
    private void createUI() {
        // the label
        final JLabel label = new JLabel(this.label + ":");
        setName(label, this.label);

        // the list
        JComponent scrollPane = createFileArrayComponent();

        // the add button
        final JButton addButton = createAddFileButton();
        // the remove button
        final JButton removeButton = createRemoveFileButton();

        // the button panel
        final JPanel buttonPanel = new JPanel();
        setName(buttonPanel, this.label);
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);

        // the base panel
        basePanel = GridBagUtils.createDefaultEmptyBorderPanel();
        setName(basePanel, this.label);
        final GridBagConstraints gbc = GridBagUtils.createConstraints(null);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;

        gbc.gridy++;
        basePanel.add(label, gbc);
        gbc.anchor = GridBagConstraints.EAST;
        basePanel.add(buttonPanel, gbc);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        basePanel.add(scrollPane, gbc);

    }

    public JButton createRemoveFileButton() {
        final JButton removeButton = (JButton) ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Minus16.gif"), false);
        setName(removeButton, "removeButton");
        removeButton.addActionListener(e -> onRemoveButton());
        return removeButton;
    }

    public JButton createAddFileButton() {
        final JButton addButton = (JButton) ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Plus16.gif"), false);
        setName(addButton, "addButton");
        addButton.addActionListener(e -> onAddButton());
        return addButton;
    }

    public JComponent createFileArrayComponent() {
        JScrollPane scrollPane = new JScrollPane(listComponent);
        setName(scrollPane, label);
        scrollPane.setPreferredSize(LIST_PREFERRED_SIZE);
        return scrollPane;
    }

    private void setName(final Component comp, String name) {
        comp.setName(name);
    }

    /*
     * Callback invoked by the add button
     */

    private void onAddButton() {
        fileDialog = getFileDialogSafe();
        final File userInputDir = parent.getUserInputDir();
        final int retVal;

        fileDialog.setCurrentDirectory(userInputDir);
        retVal = fileDialog.showOpenDialog(basePanel);

        if (retVal == JFileChooser.APPROVE_OPTION) {
            File[] selected = fileDialog.getSelectedFiles();

            fileList.addAll(Arrays.asList(selected));

            listComponent.setListData(fileList.toArray(new File[fileList.size()]));
            notifyListener();
            parent.setUserInputDir(fileDialog.getCurrentDirectory());
        }
    }

    /*
     * Callback invoked by the remove button
     */

    private void onRemoveButton() {
        final List<File> selectedFiles = listComponent.getSelectedValuesList();
        selectedFiles.forEach(fileList::remove);
        listComponent.setListData(fileList.toArray(new File[fileList.size()]));
        notifyListener();
    }

    /*
     * Retrieves the file chooser object. If none is present, an object is constructed
     */

    private JFileChooser getFileDialogSafe() {
        if (fileDialog == null) {
            fileDialog = createFileChooserDialog();
        }

        return fileDialog;
    }

    protected JFileChooser createFileChooserDialog() {
        final JFileChooser chooser = new SnapFileChooser();
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setMultiSelectionEnabled(true);

        final Iterator<ProductReaderPlugIn> iterator = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
        List<SnapFileFilter> sortedFileFilters = SnapFileFilter.getSortedFileFilters(iterator);
        sortedFileFilters.forEach(chooser::addChoosableFileFilter);
        chooser.setFileFilter(chooser.getAcceptAllFileFilter());

        return chooser;
    }

    /*
     * Calls the listener about changes - if necessary
     */

    private void notifyListener() {
        if ((listener != null)) {
            listener.updatedList(fileList.toArray(new File[fileList.size()]));
        }
    }

    public interface EditorParent {

        File getUserInputDir();

        void setUserInputDir(File newDir);
    }

    public interface FileArrayEditorListener {

        void updatedList(File[] files);
    }
}
