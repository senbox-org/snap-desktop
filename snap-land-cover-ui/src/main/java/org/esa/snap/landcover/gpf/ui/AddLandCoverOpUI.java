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
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.SnapFileChooser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * User interface for AddLandCoverOp
 */
public class AddLandCoverOpUI extends BaseOperatorUI {

    private final JList<String> landCoverNamesList = new JList<>();

    private final JList<File> externalFileList = new JList();
    private final JButton externalFileBrowseButton = new JButton("...");

    private final JComboBox<String> resamplingMethodCombo = new JComboBox<>(ResamplingFactory.resamplingNames);

    private static final String lastLandcoverPathKey = "snap.external.landcoverDir";

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();

        String[] Names = LandCoverFactory.getNameList();
        // sort the list
        final java.util.List<String> sortedNames = Arrays.asList(Names);
        java.util.Collections.sort(sortedNames);
        Names = sortedNames.toArray(new String[sortedNames.size()]);
        landCoverNamesList.setListData(Names);

        initParameters();

        externalFileBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                final File[] files = getSelectedFiles();
                if(files != null) {
                    externalFileList.removeAll();
                    externalFileList.setListData(files);
                    externalFileList.setSelectionInterval(0, externalFileList.getModel().getSize()-1);
                    landCoverNamesList.clearSelection();
                }
            }
        });
        return new JScrollPane(panel);
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
            int[] sel = getListIndices(selectedLandCoverNames, LandCoverFactory.getNameList());
            landCoverNamesList.setSelectedIndices(sel);
            int[] s = landCoverNamesList.getSelectedIndices();
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

    private static int[] getListIndices(final String[] selectedList, final String[] fullList) {
        int[] selectionIndices = new int[selectedList.length];
        int j = 0;
        for (String n : selectedList) {
            for (int i = 0; i < fullList.length; ++i) {
                if (fullList[i].equals(n)) {
                    selectionIndices[j++] = i;
                    break;
                }
            }
        }
        return selectionIndices;
    }

    private static String[] getListStrings(final int[] selectedList, final String[] fullList) {
        final ArrayList<String> stringList = new ArrayList<>(selectedList.length);
        for (int i : selectedList) {
            stringList.add(fullList[i]);
        }
        return stringList.toArray(new String[stringList.size()]);
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {
        if (!hasSourceProducts()) return;

        final String[] names = getListStrings(landCoverNamesList.getSelectedIndices(), LandCoverFactory.getNameList());
        if (names.length > 0) {
            paramMap.put("landCoverNames", names);
        } else {
            paramMap.put("landCoverNames", new String[] {});
        }
        paramMap.put("resamplingMethod", resamplingMethodCombo.getSelectedItem());
        if (!externalFileList.getSelectedValuesList().isEmpty()) {
            final File[] files = externalFileList.getSelectedValuesList().toArray(new File[externalFileList.getSelectedValuesList().size()]);
            paramMap.put("externalFiles", files);
        }
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Land Cover Model:", landCoverNamesList);
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