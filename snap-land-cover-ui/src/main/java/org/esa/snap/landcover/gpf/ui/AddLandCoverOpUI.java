/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.landcover.gpf.ui;

import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.landcover.dataio.LandCoverFactory;
import org.esa.snap.landcover.dataio.LandCoverModelDescriptor;
import org.esa.snap.landcover.dataio.LandCoverModelRegistry;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.SnapFileChooser;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User interface for AddLandCoverOp
 */
public class AddLandCoverOpUI extends BaseOperatorUI {

    private final DefaultMutableTreeNode landCoverNamesRoot = new DefaultMutableTreeNode("Land Cover Models");
    private final Map<String, DefaultMutableTreeNode> folderMap = new HashMap<>();
    final JTree landCoverNamesTree = new JTree(landCoverNamesRoot);

    final JList<File> externalFileList = new JList<>();
    private final JButton externalFileBrowseButton = new JButton("...");

    final JComboBox<String> resamplingMethodCombo = new JComboBox<>(ResamplingFactory.resamplingNames);

    private static final String lastLandcoverPathKey = "snap.external.landcoverDir";

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();

        populateNamesTree();

        initParameters();

        externalFileBrowseButton.addActionListener(e -> {

            final File[] files = getSelectedFiles();
            if(files != null) {
                externalFileList.removeAll();
                externalFileList.setListData(files);
                externalFileList.setSelectionInterval(0, externalFileList.getModel().getSize()-1);
                landCoverNamesTree.clearSelection();
            }
        });
        return new JScrollPane(panel);
    }

    void populateNamesTree() {
        landCoverNamesTree.removeAll();

        String[] names = LandCoverFactory.getNameList();
        // sort the list
        final java.util.List<String> sortedNames = Arrays.asList(names);
        java.util.Collections.sort(sortedNames);
        names = sortedNames.toArray(new String[0]);

        final LandCoverModelRegistry registry = LandCoverModelRegistry.getInstance();
        for(String name : names) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
            final LandCoverModelDescriptor descriptor = registry.getDescriptor(name);
            String grouping = descriptor.getGrouping();
            if(grouping != null) {
                DefaultMutableTreeNode folderNode = folderMap.get(grouping);
                if(folderNode == null) {
                    folderNode = new DefaultMutableTreeNode(grouping);
                    landCoverNamesRoot.add(folderNode);
                    folderMap.put(grouping, folderNode);
                }
                folderNode.add(node);
            } else {
                landCoverNamesRoot.add(node);
            }
        }
        landCoverNamesTree.setRootVisible(true);
        landCoverNamesTree.expandRow(0);
    }

    private static File[] getSelectedFiles() {
        final JFileChooser fileChooser = createFileChooserDialog(lastLandcoverPathKey);
        int result = fileChooser.showOpenDialog(SnapApp.getDefault().getMainFrame());
        if (fileChooser.getCurrentDirectory() != null) {
            SnapApp.getDefault().getPreferences().put(lastLandcoverPathKey, fileChooser.getCurrentDirectory().getPath());
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFiles();
        }
        return null;
    }

    private static JFileChooser createFileChooserDialog(final String preferencesKey) {
        final JFileChooser chooser = new SnapFileChooser();
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setMultiSelectionEnabled(true);

        final String lastDir = SnapApp.getDefault().getPreferences().get(preferencesKey, SystemUtils.getUserHomeDir().getPath());
        chooser.setCurrentDirectory(new File(lastDir));

        final Iterator<ProductReaderPlugIn> iterator = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
        java.util.List<SnapFileFilter> sortedFileFilters = SnapFileFilter.getSortedFileFilters(iterator);
        sortedFileFilters.forEach(chooser::addChoosableFileFilter);
        chooser.setFileFilter(chooser.getAcceptAllFileFilter());

        return chooser;
    }

    @Override
    public void initParameters() {

        final String[] selectedLandCoverNames = (String[]) paramMap.get("landCoverNames");
        if (selectedLandCoverNames != null) {

            List<TreePath> paths = new ArrayList<>();
            for(String name : selectedLandCoverNames) {
                TreePath path = new TreePath(landCoverNamesRoot);
                DefaultMutableTreeNode folderNode = folderMap.get(name);
                if(folderNode != null) {
                    path = path.pathByAddingChild(folderNode);
                }
                path = path.pathByAddingChild(new DefaultMutableTreeNode(name));

                paths.add(path);
            }
            landCoverNamesTree.setSelectionPaths(paths.toArray(new TreePath[0]));
        }
        resamplingMethodCombo.setSelectedItem(paramMap.get("resamplingMethod"));

        final File[] files = (File[]) paramMap.get("externalFiles");
        if (files != null) {
            externalFileList.setListData(files);
        } else {
            // backward compatibility
            final File file = (File) paramMap.get("externalFile");
            if(file != null) {
                externalFileList.setListData(new File[] {file});
            }
        }
        if(externalFileList.getModel().getSize() > 0) {
            externalFileList.setSelectionInterval(0, externalFileList.getModel().getSize() - 1);
        }
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {
        if (!hasSourceProducts()) return;

        TreePath[] treePaths = landCoverNamesTree.getSelectionPaths();
        List<String> names = new ArrayList<>();
        for(TreePath treePath : treePaths) {
            names.add(treePath.getLastPathComponent().toString());
        }
        if (!names.isEmpty()) {
            paramMap.put("landCoverNames", names.toArray(new String[0]));
        } else {
            paramMap.put("landCoverNames", new String[] {});
        }
        paramMap.put("resamplingMethod", resamplingMethodCombo.getSelectedItem());
        if (!externalFileList.getSelectedValuesList().isEmpty()) {
            final File[] files = externalFileList.getSelectedValuesList().toArray(new File[0]);
            paramMap.put("externalFiles", files);
        }
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        landCoverNamesTree.setVisibleRowCount(10);
        DialogUtils.addComponent(contentPane, gbc, "Land Cover Model:", landCoverNamesTree);
        gbc.gridy++;
        externalFileList.setFixedCellWidth(500);
        DialogUtils.addInnerPanel(contentPane, gbc, new JLabel("External Files"), externalFileList, externalFileBrowseButton);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Resampling Method:", resamplingMethodCombo);
        gbc.gridy++;
        gbc.gridx = 1;
        contentPane.add(new JLabel("Integer data types will use nearest neighbour"), gbc);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

}