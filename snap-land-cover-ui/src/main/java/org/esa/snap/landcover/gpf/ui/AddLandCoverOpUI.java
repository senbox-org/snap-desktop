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

import org.esa.snap.landcover.dataio.LandCoverFactory;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * User interface for AddLandCoverOp
 */
public class AddLandCoverOpUI extends BaseOperatorUI {

    private final JList<String> landCoverNamesList = new JList<>();

    private final JTextField externalFile = new JTextField("");
    private final JButton externalFileBrowseButton = new JButton("...");

    private final JComboBox resamplingMethodCombo = new JComboBox<>(ResamplingFactory.resamplingNames);

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
                final File file = Dialogs.requestFileForOpen("External File", false, null, "snap.external.landcoverDir");
                externalFile.setText(file.getAbsolutePath());
                landCoverNamesList.clearSelection();
            }
        });
        return new JScrollPane(panel);
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

        final File file = (File) paramMap.get("externalFile");
        if (file != null) {
            externalFile.setText(file.getAbsolutePath());
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
        }
        paramMap.put("resamplingMethod", resamplingMethodCombo.getSelectedItem());
        if (!externalFile.getText().isEmpty()) {
            paramMap.put("externalFile", new File(externalFile.getText()));
        }
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Land Cover Model:", landCoverNamesList);
        gbc.gridy++;
        externalFile.setColumns(50);
        DialogUtils.addInnerPanel(contentPane, gbc, new JLabel("External File"), externalFile, externalFileBrowseButton);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Resampling Method:", resamplingMethodCombo);
        gbc.gridy++;
        gbc.gridx = 1;
        contentPane.add(new JLabel("Integer data types will use nearest neighbour"), gbc);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

}