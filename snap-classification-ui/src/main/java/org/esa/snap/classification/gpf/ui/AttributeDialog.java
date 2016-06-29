/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.classification.gpf.ui;

import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;

import javax.swing.*;
import java.awt.*;

/**
    Attribute selection
 */
public class AttributeDialog extends ModalDialog {

    private final JList<String> listControl;
    private boolean ok = false;

    public AttributeDialog(final String title, final String[] listData, final String defaultValue) {
        super(SnapApp.getDefault().getMainFrame(), title, ModalDialog.ID_OK, null);

        listControl = new JList<>(listData);
        if(defaultValue != null) {
            listControl.setSelectedValue(defaultValue, true);
        }

        final JPanel content = GridBagUtils.createPanel();
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.insets.top = 2;
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(listControl);
        content.add(scrollPane, gbc);

        getJDialog().setMinimumSize(new Dimension(400, 100));

        setContent(content);
    }

    public String getValue() {
        return listControl.getSelectedValue();
    }

    protected void onOK() {
        ok = true;
        hide();
    }

    public boolean IsOK() {
        return ok;
    }
}
