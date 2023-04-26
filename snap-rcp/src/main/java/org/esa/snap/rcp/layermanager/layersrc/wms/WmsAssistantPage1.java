/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.layermanager.layersrc.wms;

import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.layermanager.layersrc.HistoryComboBoxModel;
import org.esa.snap.ui.UserInputHistory;
import org.esa.snap.ui.layer.AbstractLayerSourceAssistantPage;
import org.esa.snap.ui.layer.LayerSourcePageContext;
import org.geotools.ows.wms.WMSCapabilities;
import org.geotools.ows.wms.WebMapServer;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;

class WmsAssistantPage1 extends AbstractLayerSourceAssistantPage {

    private JComboBox wmsUrlBox;
    private static final String PROPERTY_WMS_HISTORY = "WmsAssistant.wms.history";
    private UserInputHistory history;

    WmsAssistantPage1() {
        super("Select WMS");
    }

    @Override
    public boolean validatePage() {
        if (wmsUrlBox.getSelectedItem() != null) {
            String wmsUrl = wmsUrlBox.getSelectedItem().toString();
            return wmsUrl != null && !wmsUrl.trim().isEmpty();
        }
        return false;
    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public AbstractLayerSourceAssistantPage getNextPage() {
        LayerSourcePageContext pageContext = getContext();
        WebMapServer wms = null;
        WMSCapabilities wmsCapabilities = null;

        String wmsUrl = wmsUrlBox.getSelectedItem().toString();
        if (wmsUrl != null && !wmsUrl.isEmpty()) {
            try {
                wms = getWms(pageContext.getWindow(), wmsUrl);
                wmsCapabilities = wms.getCapabilities();
            } catch (Exception e) {
                e.printStackTrace();
                pageContext.showErrorDialog("Failed to access WMS:\n" + e.getMessage());
            }
        }

        history.copyInto(SnapApp.getDefault().getPreferences());

        if (wms != null && wmsCapabilities != null) {
            pageContext.setPropertyValue(WmsLayerSource.PROPERTY_NAME_WMS, wms);
            pageContext.setPropertyValue(WmsLayerSource.PROPERTY_NAME_WMS_CAPABILITIES, wmsCapabilities);
            return new WmsAssistantPage2();
        } else {
            return null;
        }
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public Component createPageComponent() {
        GridBagConstraints gbc = new GridBagConstraints();
        final JPanel panel = new JPanel(new GridBagLayout());

        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        panel.add(new JLabel("URL for WMS (e.g. http://<host>/<server>):"), gbc);

        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;
        history = new UserInputHistory(8, PROPERTY_WMS_HISTORY);
        history.initBy(SnapApp.getDefault().getPreferences());
        if (history.getNumEntries() == 0) {
            history.push("http://geoservice.dlr.de/basemap/wms");
            history.push("https://www.geoseaportal.de/wss/service/StaticInformation_Background/guest");
        }
        wmsUrlBox = new JComboBox(new HistoryComboBoxModel(history));
        wmsUrlBox.setEditable(true);
        panel.add(wmsUrlBox, gbc);
        wmsUrlBox.addItemListener(new MyItemListener());

        return panel;
    }

    private WebMapServer getWms(Window window, String wmsUrl) throws Exception {
        WebMapServer wms;
        try {
            window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            URL url = new URL(wmsUrl);
            wms = new WebMapServer(url);
            getContext().setPropertyValue(WmsLayerSource.PROPERTY_NAME_WMS_URL, url);
        } finally {
            window.setCursor(Cursor.getDefaultCursor());
        }
        return wms;
    }


    private class MyItemListener implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            getContext().updateState();
        }
    }

}
