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

import org.esa.snap.classification.gpf.knn.KNNClassifierOp;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;

import javax.swing.*;

/**
 * User interface for KNN
 */
public class KNNClassifierOpUI extends BaseClassifierOpUI {
    
    private final JTextField numNeighbours = new JTextField("");

    public KNNClassifierOpUI() {
        this(KNNClassifierOp.CLASSIFIER_TYPE);
    }

    public KNNClassifierOpUI(String type) {
        super(type);
    }

    @Override
    public void initParameters() {
        super.initParameters();

        numNeighbours.setText(String.valueOf(paramMap.get("numNeighbours")));
    }

    @Override
    public void updateParameters() {
        super.updateParameters();

        paramMap.put("numNeighbours", Integer.parseInt(numNeighbours.getText()));
    }

    @Override
    protected JPanel createPanel() {

        final JPanel contentPane = super.createPanel();

        classifiergbc.gridy++;
        DialogUtils.addComponent(classifierPanel, classifiergbc, "Number of neighbours:", numNeighbours);

        return contentPane;
    }

    @Override
    protected void setEnabled(final boolean enabled) {
        numNeighbours.setEnabled(enabled);
    }
}
