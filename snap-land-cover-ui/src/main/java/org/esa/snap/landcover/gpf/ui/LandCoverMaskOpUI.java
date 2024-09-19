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

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIUtils;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User interface for Land Cover Mask
 */
public class LandCoverMaskOpUI extends BaseOperatorUI {

    private final JList<String> bandList = new JList<>();

    final JComboBox<String> landCoverBandCombo = new JComboBox<>();
    private final JLabel validLandCoverClassesLabel = new JLabel("Valid land cover classes:");
    private final JList<String> validLandCoverClassesList = new JList<>();
    private final JScrollPane validLandCoverClassesScroll = new JScrollPane(validLandCoverClassesList);
    private final JLabel validPixelExpressionLabel = new JLabel("Valid pixel expression:");
    final JTextArea validPixelExpressionText = new JTextArea();

    final JCheckBox includeOtherBandsCheckBox = new JCheckBox("Include all other bands");
    private final Map<Integer, Integer> classMap = new HashMap<>();
    private final Map<Integer, String> classNameMap = new HashMap<>();

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();

        landCoverBandCombo.addItemListener(e -> {
            validLandCoverClassesList.removeAll();
            String[] classes = getLandCoverClasses((String) landCoverBandCombo.getSelectedItem());
            if(classes.length > 0) {
                validLandCoverClassesList.setListData(classes);

                final int[] selClasses = (int[]) paramMap.get("validLandCoverClasses");
                if (selClasses != null) {
                    validLandCoverClassesList.setSelectedIndices(getClassIndexes(selClasses));
                }
                showLandCoverClasses(true);
            } else {

                showLandCoverClasses(false);
            }
        });

        initParameters();
        return new JScrollPane(panel);
    }

    private void showLandCoverClasses(final boolean flag) {
        validLandCoverClassesScroll.setVisible(flag);
        validLandCoverClassesLabel.setVisible(flag);
        validLandCoverClassesList.setVisible(flag);
        validPixelExpressionLabel.setVisible(!flag);
        validPixelExpressionText.setVisible(!flag);
    }

    @Override
    public void initParameters() {

        final String[] bandNames = getBandNames();
        OperatorUIUtils.initParamList(bandList, bandNames, (Object[]) paramMap.get("sourceBands"));

        landCoverBandCombo.removeAllItems();
        final String[] landCoverBandNames = getLandCoverNames();
        for (String bandName : landCoverBandNames) {
            landCoverBandCombo.addItem(bandName);
        }
        final String landCoverBand = (String) paramMap.get("landCoverBand");
        if (landCoverBand != null && StringUtils.contains(landCoverBandNames, landCoverBand)) {
            landCoverBandCombo.setSelectedItem(landCoverBand);
        }

        final String validPixelExpression = (String) paramMap.get("validPixelExpression");
        if (validPixelExpression != null && !validPixelExpression.isEmpty()) {
            validPixelExpressionText.setText(validPixelExpression);
        }

        final Boolean includeOtherBands = (Boolean) paramMap.get("includeOtherBands");
        if (includeOtherBands != null) {
            includeOtherBandsCheckBox.setSelected(includeOtherBands);
        }
    }

    private String[] getLandCoverNames() {
        final List<String> namesList = new ArrayList<>();
        try {
            if (sourceProducts != null) {
                for (Band b : sourceProducts[0].getBands()) {
                    if (b.getUnit() != null && b.getUnit().equals(Unit.CLASS)) {
                        namesList.add(b.getName());
                    }
                }
            }
        } catch (Exception e) {
            // return empty namesList
        }
        return namesList.toArray(new String[0]);
    }

    String[] getLandCoverClasses(final String landCoverBandName) {
        final List<String> classList = new ArrayList<>();
        if (landCoverBandName != null) {
            final Band srcBand = sourceProducts[0].getBand(landCoverBandName);
            if (srcBand != null) {
                final IndexCoding indexCoding = srcBand.getIndexCoding();
                if (indexCoding != null) {
                    final String[] indexNames = indexCoding.getIndexNames();
                    int i = 0;
                    for (String indexName : indexNames) {
                        classList.add(indexName);
                        int classVal = indexCoding.getIndexValue(indexName);
                        classMap.put(i++, classVal);
                        classNameMap.put(classVal, indexName);
                    }
                }
            }
        }
        return classList.toArray(new String[0]);
    }

    private int[] getClassIndexes(final int[] selClasses) {
        final List<Integer> indexList = new ArrayList<>(selClasses.length);
        for (int i = 0; i < validLandCoverClassesList.getModel().getSize(); ++i) {
            final String listName = validLandCoverClassesList.getModel().getElementAt(i);
            for (int selClass : selClasses) {
                final String selClassName = classNameMap.get(selClass);
                if (listName.equals(selClassName)) {
                    indexList.add(i);
                }
            }
        }

        final int[] index = new int[indexList.size()];
        int i = 0;
        for (Integer val : indexList) {
            index[i++] = val;
        }
        return index;
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {
        if (!hasSourceProducts()) return;

        OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);

        paramMap.put("landCoverBand", landCoverBandCombo.getSelectedItem());
        paramMap.put("validLandCoverClasses", getSelectedClasses());
        paramMap.put("validPixelExpression", validPixelExpressionText.getText());
        paramMap.put("includeOtherBands", includeOtherBandsCheckBox.isSelected());
    }

    private int[] getSelectedClasses() {
        final int[] selIndex = validLandCoverClassesList.getSelectedIndices();
        final int[] classList = new int[selIndex.length];
        for (int i = 0; i < selIndex.length; ++i) {
            classList[i] = classMap.get(selIndex[i]);
        }
        return classList;
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        contentPane.add(new JLabel("Source Bands:"), gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        contentPane.add(new JScrollPane(bandList), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Land Cover Band:", landCoverBandCombo);
        gbc.gridy++;
        contentPane.add(validLandCoverClassesLabel, gbc);
        gbc.gridx = 1;
        contentPane.add(validLandCoverClassesScroll, gbc);
        gbc.gridx = 0;
        contentPane.add(validPixelExpressionLabel, gbc);
        gbc.gridx = 1;
        contentPane.add(validPixelExpressionText, gbc);

        gbc.gridy++;
        gbc.gridx = 1;
        contentPane.add(includeOtherBandsCheckBox, gbc);

        DialogUtils.fillPanel(contentPane, gbc);

        showLandCoverClasses(true);

        return contentPane;
    }
}