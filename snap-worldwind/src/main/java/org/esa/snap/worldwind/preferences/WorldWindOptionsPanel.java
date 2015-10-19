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
package org.esa.snap.worldwind.preferences;

import org.esa.snap.rcp.SnapApp;
import org.esa.snap.runtime.Config;
import org.esa.snap.worldwind.WWWorldMapToolView;
import org.openide.awt.Mnemonics;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

final class WorldWindOptionsPanel extends javax.swing.JPanel {

    private javax.swing.JCheckBox useFlatEarthCheckBox;

    WorldWindOptionsPanel(final WorldWindOptionsPanelController controller) {
        initComponents();
        // listen to changes in form fields and call controller.changed()
        useFlatEarthCheckBox.addItemListener(e -> controller.changed());

    }

    private void initComponents() {
        useFlatEarthCheckBox = new javax.swing.JCheckBox();
        Mnemonics.setLocalizedText(useFlatEarthCheckBox, "Use flat Earth projection");


        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                          .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                            .addComponent(useFlatEarthCheckBox)
                                                            .addGap(0, 512, Short.MAX_VALUE)
                                          ).addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                          .addComponent(useFlatEarthCheckBox)
                                          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                          .addContainerGap())
        );
    }

    void load() {
        useFlatEarthCheckBox.setSelected(
                Config.instance().preferences().getBoolean(WWWorldMapToolView.useFlatEarth, false));
    }

    void store() {
        final Preferences preferences = Config.instance().preferences();
        preferences.putBoolean(WWWorldMapToolView.useFlatEarth, useFlatEarthCheckBox.isSelected());

        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            SnapApp.getDefault().getLogger().severe(e.getMessage());
        }
    }

    boolean valid() {
        // Check whether form is consistent and complete
        return true;
    }
}
