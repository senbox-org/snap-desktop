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
package org.esa.snap.raster.gpf.ui;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIUtils;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User interface for CreateLandMaskOp
 */
public class CreateLandMaskOpUI extends BaseOperatorUI {

    private final JList bandList = new JList();
    private final JComboBox<String> geometries = new JComboBox();

    private final JRadioButton landMask = new JRadioButton("Mask out the Land");
    private final JRadioButton seaMask = new JRadioButton("Mask out the Sea");
    private final JCheckBox useSRTMCheckBox = new JCheckBox("Use SRTM 3sec");
    private final JRadioButton geometryMask = new JRadioButton("Use Vector as Mask");
    private final JCheckBox invertGeometryCheckBox = new JCheckBox("Invert Vector");
    private final JTextField shorelineExtensionTextField = new JTextField();
    private boolean invertGeometry = false;
    private boolean useSRTM = true;

    private boolean uiReady = false;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {
        this.operatorName = operatorName;
        validateParamMap(parameterMap);
        this.paramMap = parameterMap;

        if (sourceProducts != null || paramMap.isEmpty()) {
            initializeOperatorUI(operatorName, parameterMap);
            initParameters();
        }

        JComponent panel = createPanel();

        useSRTMCheckBox.addItemListener(e -> useSRTM = (e.getStateChange() == ItemEvent.SELECTED));
        invertGeometryCheckBox.addItemListener(e -> invertGeometry = (e.getStateChange() == ItemEvent.SELECTED));

        final RadioListener myListener = new RadioListener();
        landMask.addActionListener(myListener);
        seaMask.addActionListener(myListener);
        geometryMask.addActionListener(myListener);

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {
        OperatorUIUtils.initParamList(bandList, getBandNames());

        String[] selectedBands = (String[]) paramMap.get(OperatorUIUtils.SOURCE_BAND_NAMES);
        if (selectedBands != null) {
            setSelectedBands(bandList, selectedBands);
        }

        final Boolean doLandMask = (Boolean) paramMap.get("landMask");
        if (doLandMask != null && doLandMask) {
            landMask.setSelected(true);
        } else {
            seaMask.setSelected(true);
        }
        geometries.removeAllItems();
        final String[] geometryNames = getGeometries();
        for (String g : geometryNames) {
            geometries.addItem(g);
        }
        final String selectedGeometry = (String) paramMap.get("geometry");
        if (selectedGeometry != null) {
            geometryMask.setSelected(true);
            geometries.setSelectedItem(selectedGeometry);
        }

        useSRTM = (Boolean) paramMap.get("useSRTM");
        useSRTMCheckBox.setSelected(useSRTM);

        Integer shorelineExtension = (Integer) paramMap.get("shorelineExtension");
        shorelineExtensionTextField.setText(shorelineExtension == null ? "0" : shorelineExtension.toString());

        this.uiReady = true;
    }

    private void setSelectedBands(JList bandList, String[] selectedBands) {
        ListModel<String> model = bandList.getModel();
        List<Integer> idxs = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++) {
            for (String name : selectedBands) {
                if (name.equals(model.getElementAt(i))) {
                    idxs.add(i);
                }
            }
        }
        int[] indices = idxs.stream().mapToInt(Integer::intValue).toArray();
        bandList.setSelectedIndices(indices);
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {
        if (!uiReady) return;

        OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);

        paramMap.put("landMask", landMask.isSelected());
        if (geometryMask.isSelected()) {
            paramMap.put("geometry", geometries.getSelectedItem());
            paramMap.put("invertGeometry", invertGeometry);
        } else {
            paramMap.remove("geometry");
            paramMap.remove("invertGeometry");
        }

        int shorelineExtension = 0;
        try {
            shorelineExtension = Integer.parseInt(shorelineExtensionTextField.getText());
        }catch (Exception ignored) {
        }
        paramMap.put("shorelineExtension", shorelineExtension);
        paramMap.put("useSRTM", useSRTM);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Source Bands:", new JScrollPane(bandList));

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(landMask, gbc);
        gbc.gridy++;
        contentPane.add(seaMask, gbc);
        gbc.gridy++;
        contentPane.add(useSRTMCheckBox, gbc);
        gbc.gridy++;
        contentPane.add(geometryMask, gbc);
        gbc.gridy++;
        gbc.gridx = 1;
        contentPane.add(geometries, gbc);
        gbc.gridy++;
        contentPane.add(invertGeometryCheckBox, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Extend shoreline by [pixels]:", shorelineExtensionTextField);

        final ButtonGroup group = new ButtonGroup();
        group.add(landMask);
        group.add(seaMask);
        group.add(geometryMask);

        geometries.setEnabled(false);
        invertGeometryCheckBox.setEnabled(false);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private void validateParamMap(Map<String, Object> parameterMap) {
        if (parameterMap.containsKey("sourceBands")) {
            Object sourceBands = parameterMap.get("sourceBands");
            parameterMap.put(OperatorUIUtils.SOURCE_BAND_NAMES, sourceBands);
            parameterMap.remove("sourceBands");
        }
    }

    @Override
    public void setSourceProducts(Product[] products) {
        super.setSourceProducts(products);
        if (getOperatorName() != null && !getOperatorName().isEmpty()) {
            initializeOperatorUI(getOperatorName(), getParameters());
            initParameters();
        }
    }

    private class RadioListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {

            boolean b = geometryMask.isSelected();
            geometries.setEnabled(b);
            invertGeometryCheckBox.setEnabled(b);
        }
    }
}
