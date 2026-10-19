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
import org.esa.snap.worldwind.WWWorldViewToolView;
import org.openide.awt.Mnemonics;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

final class WorldWindOptionsPanel extends JPanel {

    private final JCheckBox useFlatEarthCheckBox = new JCheckBox();
    private final JTextArea wmsServersArea = new JTextArea(4, 50);

    WorldWindOptionsPanel(final WorldWindOptionsPanelController controller) {
        super(new GridBagLayout());
        initComponents();
        // listen to changes in form fields and call controller.changed()
        useFlatEarthCheckBox.addItemListener(e -> controller.changed());
        wmsServersArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                controller.changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                controller.changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                controller.changed();
            }
        });
    }

    private void initComponents() {
        Mnemonics.setLocalizedText(useFlatEarthCheckBox, "Use flat Earth projection");

        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);
        add(useFlatEarthCheckBox, c);

        c.gridy++;
        c.insets = new Insets(10, 2, 2, 2);
        add(new JLabel("WMS servers of \"Add WMS\" in the Analysis view, one address per line:"), c);
        c.gridy++;
        c.insets = new Insets(2, 2, 2, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        add(new JScrollPane(wmsServersArea), c);

        // push everything to the top
        c.gridy++;
        c.weighty = 1.0;
        add(new JPanel(), c);
    }

    void load() {
        useFlatEarthCheckBox.setSelected(
                Config.instance().preferences().getBoolean(WWWorldViewToolView.useFlatEarth, false));
        wmsServersArea.setText(String.join("\n", WorldWindPreferences.getWmsServers()));
    }

    void store() {
        final Preferences preferences = Config.instance().preferences();
        preferences.putBoolean(WWWorldViewToolView.useFlatEarth, useFlatEarthCheckBox.isSelected());
        WorldWindPreferences.setWmsServers(WorldWindPreferences.parseLines(wmsServersArea.getText()));

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
