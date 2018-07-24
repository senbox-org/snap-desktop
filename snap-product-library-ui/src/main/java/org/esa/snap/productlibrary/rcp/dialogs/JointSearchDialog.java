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
package org.esa.snap.productlibrary.rcp.dialogs;

import org.esa.snap.productlibrary.db.DBQuery;
import org.esa.snap.productlibrary.db.SQLUtils;
import org.esa.snap.productlibrary.opensearch.CopernicusProductQuery;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.ModalDialog;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

/**
 * Joint Search Dialog
 */
public class JointSearchDialog extends ModalDialog {

    private final JList<String> missionJList = new JList<>();
    private JTextField daysMinus = new JTextField("7");
    private JTextField daysPlus = new JTextField("7");
    private final JTextField cloudCoverField = new JTextField();
    private final JComboBox<String> acquisitionModeCombo = new JComboBox<>(new String[]{DBQuery.ALL_MODES});
    private final JList<String> productTypeJList = new JList<>();

    private boolean ok = false;

    public JointSearchDialog(final String title, final String mission) {
        super(SnapApp.getDefault().getMainFrame(), title, ModalDialog.ID_OK, null);

        //System.out.println("JointSearchDialog mission = " + mission);

        missionJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        missionJList.setListData(CopernicusProductQuery.instance().getAllMissions());

        initMissionList(mission);

        missionJList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (event.getValueIsAdjusting()) {
                    final int[] selectedIndices = missionJList.getSelectedIndices();
                    if (selectedIndices.length == 0) {
                        // if nothing is selected, go back to default for that mission
                        initMissionList(mission);
                    } else  {
                        boolean isOpticalOnly = true;
                        for (int i : selectedIndices) {
                            if (i == 0) {
                                isOpticalOnly = false;
                                break;
                            }
                        }
                        cloudCoverField.setEnabled(isOpticalOnly);
                        if (selectedIndices.length > 1) {
                            acquisitionModeCombo.setSelectedIndex(0);
                            acquisitionModeCombo.setEnabled(false);
                            productTypeJList.removeAll();
                            productTypeJList.setListData(new String[] {DBQuery.ALL_PRODUCT_TYPES});
                            productTypeJList.setEnabled(false);
                            /*
                            // If we ever want to get more than one selected mission, this is how to do it...
                            String[] selectedMissions = new String[selectedIndices.length];
                            for (int i = 0; i < selectedIndices.length; i++) {
                                selectedMissions[i] = missionJList.getModel().getElementAt(selectedIndices[i]);
                            }
                            */
                        } else {
                            final String selectedMission = missionJList.getSelectedValue();
                            updateAcquisitionModeCombo(selectedMission);
                            acquisitionModeCombo.setEnabled(true);
                            updateProductTypeList(new String[] {selectedMission});
                            productTypeJList.setEnabled(true);
                        }
                    }
                }
            }
        });

        initContent();
    }

    protected void initContent() {
        final JPanel content = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(content, gbc, "Mission:", missionJList);

        gbc.gridy++;
        DialogUtils.addComponent(content, gbc, "Minus (days):", daysMinus).setToolTipText("0 or +ve integer");

        gbc.gridy++;
        DialogUtils.addComponent(content, gbc, "Plus (days):", daysPlus).setToolTipText("0 or +ve integer");

        gbc.gridy++;
        DialogUtils.addComponent(content, gbc, "Cloud Cover %:", cloudCoverField).setToolTipText("Specify single integer value or a range, e.g., 10-70");

        gbc.gridy++;
        DialogUtils.addComponent(content, gbc, "Acquisition Mode: ", acquisitionModeCombo);

        gbc.gridy++;
        DialogUtils.addComponent(content, gbc, "Product Type: ", new JScrollPane(productTypeJList));

        DialogUtils.fillPanel(content, gbc);

        getJDialog().setMinimumSize(new Dimension(250, 100));

        setContent(content);
    }

    protected void onOK() {
        ok = true;
        hide();
    }

    protected void onCancel() {
        ok = false;
        hide();
    }

    public boolean IsOK() {
        return ok;
    }

    public String[] getMissions() {
        return toStringArray(missionJList.getSelectedValuesList());
    }

    public int getDaysMinus() {
        final int days = getIntFromString(daysMinus.getText());
        return (days > -1) ? days : -1;
    }

    public int getDaysPlus() {
        final int days = getIntFromString(daysPlus.getText());
        return (days > -1) ? days : -1;
    }

    public String getCloudCover() {
        return cloudCoverField.getText();
    }

    public String getAcquisitionMode() {
        return (String) acquisitionModeCombo.getSelectedItem();
    }

    public String[] getProductTypes() {
        java.util.List<String> selectedProductTypes = productTypeJList.getSelectedValuesList();
        return selectedProductTypes.toArray(new String[selectedProductTypes.size()]);
    }

    private static int getIntFromString(final String s) {
        try {
            final int intVal = Integer.parseInt(s);
            return intVal;
        } catch (Exception e) {
            return -1;
        }
    }

    private static String[] toStringArray(java.util.List<String> list) {
        return list.toArray(new String[list.size()]);
    }

    private void initMissionList(final String mission) {
        if(mission.toUpperCase().contains("SENTINEL-1")) {
            // User has clicked a Sentinel-1 product and want to search other optical products (i.e., Sentinel-2 or Sentinel-3)
            missionJList.setSelectedIndex(1); // assume the missions are Sentinel-1, Sentinel-2 and Sentinel-3, so index 1 is Sentinel-2
            cloudCoverField.setEnabled(true);
        } else {
            // User has clicked an optical product and want to search for S1 product
            missionJList.setSelectedIndex(0); // assume the missions are Sentinel-1, Sentinel-2 and Sentinel-3, so index 0 is Sentinel-1
            cloudCoverField.setEnabled(false);
        }
        final String selectedMission = missionJList.getSelectedValue();
        updateAcquisitionModeCombo(selectedMission);
        updateProductTypeList(new String[] {selectedMission});
    }

    private void updateAcquisitionModeCombo(final String mission) {
        acquisitionModeCombo.removeAllItems();
        acquisitionModeCombo.addItem(DBQuery.ALL_MODES);
        String[] acqModes = CopernicusProductQuery.instance().getAllAcquisitionModes(new String[]{mission});
        for (String mode : acqModes) {
            acquisitionModeCombo.addItem(mode);
        }
    }

    private void updateProductTypeList(final String[] missions) {
        /*
        System.out.println("updateProductTypeList: #missons = " + missions.length);
        for (String s : missions) {
            System.out.println("updateProductTypeList: s = " + s);
        }
        */
        productTypeJList.removeAll();
        productTypeJList.setListData(SQLUtils.prependString(
                DBQuery.ALL_PRODUCT_TYPES,
                CopernicusProductQuery.instance().getAllProductTypes(missions)));
    }
}
