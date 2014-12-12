/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.gui.preferences;

import javax.swing.JLabel;

/**
 * @author Thomas Storm
 */
public class SnapOptionsPanel extends javax.swing.JPanel {

    private final SnapOptionsPanelController controller;

    SnapOptionsPanel(SnapOptionsPanelController controller) {
        this.controller = controller;
        init();
    }

    private void init() {
        add(new JLabel("test"));
    }

    @Override
    public void repaint() {
        System.out.println("SnapOptionsPanel.repaint");
        super.repaint();
    }
}
