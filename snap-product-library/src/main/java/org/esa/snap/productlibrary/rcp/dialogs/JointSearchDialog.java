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

import org.esa.snap.engine_utilities.download.opensearch.CopernicusProductQuery;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.ModalDialog;

import javax.swing.*;
import java.awt.*;

/**
 * Joint Search Dialog
 */
public class JointSearchDialog extends ModalDialog {

    private final JList<String> missionJList = new JList<>();
    private JTextField daysMinus = new JTextField("7");
    private JTextField daysPlus = new JTextField("7");
    private final JTextField cloudCoverField = new JTextField();

    private boolean ok = false;

    public JointSearchDialog(final String title) {
        super(SnapApp.getDefault().getMainFrame(), title, ModalDialog.ID_OK, null);

        initContent();
    }

    protected void initContent() {
        final JPanel content = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        missionJList.setListData(CopernicusProductQuery.instance().getAllMissions());
        DialogUtils.addComponent(content, gbc, "Mission:", missionJList);

        gbc.gridy++;
        DialogUtils.addComponent(content, gbc, "Minus (days):", daysMinus).setToolTipText("0 or +ve integer");

        gbc.gridy++;
        DialogUtils.addComponent(content, gbc, "Plus (days):", daysPlus).setToolTipText("0 or +ve integer");

        gbc.gridy++;
        DialogUtils.addComponent(content, gbc, "Cloud Cover %:", cloudCoverField).setToolTipText("Specify single integer value or a range, e.g., 10-70");

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
}
