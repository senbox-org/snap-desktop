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
package org.esa.snap.graphbuilder.rcp.dialogs.support;

import com.bc.ceres.swing.TableLayout;
import org.esa.snap.core.gpf.ui.TargetProductSelector;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * Target folder selector
 */
public class TargetFolderSelector extends TargetProductSelector {

    private JCheckBox skipExistingCBox = new JCheckBox("Skip existing target files");

    public JPanel createPanel() {

        final JPanel subPanel2 = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        subPanel2.add(new JLabel("Save as:  "));
        subPanel2.add(getFormatNameComboBox());

        final JPanel subPanel3 = new JPanel(new BorderLayout(3, 3));
        subPanel3.add(getProductDirLabel(), BorderLayout.NORTH);
        subPanel3.add(getProductDirTextField(), BorderLayout.CENTER);
        subPanel3.add(getProductDirChooserButton(), BorderLayout.EAST);

        final JPanel subPanel4 = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        subPanel4.add(skipExistingCBox);

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);

        tableLayout.setCellPadding(0, 0, new Insets(3, 3, 3, 3));
        tableLayout.setCellPadding(1, 0, new Insets(3, 3, 3, 3));
        tableLayout.setCellPadding(2, 0, new Insets(3, 3, 3, 3));
        tableLayout.setCellPadding(3, 0, new Insets(0, 24, 3, 3));
        tableLayout.setCellPadding(4, 0, new Insets(3, 3, 3, 3));

        final JPanel panel = new JPanel(tableLayout);
        panel.setBorder(BorderFactory.createTitledBorder("Target Folder"));
        panel.add(subPanel2);
        panel.add(subPanel3);
        panel.add(subPanel4);
        panel.add(getOpenInAppCheckBox());

        return panel;
    }

    public boolean isSkippingExistingTargetFiles() {
        return skipExistingCBox.isSelected();
    }
}
