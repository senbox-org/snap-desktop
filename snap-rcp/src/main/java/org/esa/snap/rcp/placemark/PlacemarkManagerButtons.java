/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.rcp.placemark;

import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.help.HelpDisplayer;
import org.esa.snap.ui.tool.ToolButtonFactory;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class PlacemarkManagerButtons extends JPanel {

    private final AbstractButton newButton;
    private final AbstractButton copyButton;
    private final AbstractButton editButton;
    private final AbstractButton removeButton;
    private final AbstractButton importButton;
    private final AbstractButton exportButton;
    private final AbstractButton filterButton;
    private final AbstractButton exportTableButton;
    private final AbstractButton zoomToPlacemarkButton;
    private final AbstractButton transferPlacemarkButton;

    public PlacemarkManagerButtons(final PlacemarkManagerTopComponent topComponent) {
        super(new GridBagLayout());

        newButton = createButton("icons/New24.gif");
        newButton.setName("newButton");
        final String placemarkLabel = topComponent.getPlacemarkDescriptor().getRoleLabel();
        newButton.setToolTipText("Create and add new " + placemarkLabel + "."); /*I18N*/
        newButton.addActionListener(e -> topComponent.newPin());

        copyButton = createButton("icons/Copy24.gif");
        copyButton.setName("copyButton");
        copyButton.setToolTipText("Copy an existing " + placemarkLabel + "."); /*I18N*/
        copyButton.addActionListener(e -> topComponent.copyActivePlacemark());

        editButton = createButton("icons/Edit24.gif");
        editButton.setName("editButton");
        editButton.setToolTipText("Edit selected " + placemarkLabel + "."); /*I18N*/
        editButton.addActionListener(e -> topComponent.editActivePin());

        removeButton = createButton("icons/Remove24.gif");
        removeButton.setName("removeButton");
        removeButton.setToolTipText("Remove selected " + placemarkLabel + "."); /*I18N*/
        removeButton.addActionListener(e -> topComponent.removeSelectedPins());

        importButton = createButton("icons/Import24.gif");
        importButton.setName("importButton");
        importButton.setToolTipText("Import all " + placemarkLabel + "s from XML or text file."); /*I18N*/
        importButton.addActionListener(e -> {
            topComponent.importPlacemarks(true);
            topComponent.updateUIState();
        });

        exportButton = createButton("icons/Export24.gif");
        exportButton.setName("exportButton");
        exportButton.setToolTipText("Export selected " + placemarkLabel + "s to XML file."); /*I18N*/
        exportButton.addActionListener(e -> {
            topComponent.exportPlacemarks();
            topComponent.updateUIState();
        });

        filterButton = createButton("icons/Filter24.gif");
        filterButton.setName("filterButton");
        filterButton.setToolTipText("Filter pixel data to be displayed in table."); /*I18N*/
        filterButton.addActionListener(e -> {
            topComponent.applyFilteredGrids();
            topComponent.updateUIState();
        });

        exportTableButton = createButton("icons/ExportTable.gif");
        exportTableButton.setName("exportTableButton");
        exportTableButton.setToolTipText("Export selected data to flat text file."); /*I18N*/
        exportTableButton.addActionListener(e -> {
            topComponent.exportPlacemarkDataTable();
            topComponent.updateUIState();
        });

        zoomToPlacemarkButton = createButton("icons/ZoomTo24.gif");
        zoomToPlacemarkButton.setName("zoomToButton");
        zoomToPlacemarkButton.setToolTipText("Zoom to selected " + placemarkLabel + "."); /*I18N*/
        zoomToPlacemarkButton.addActionListener(e -> topComponent.zoomToActivePin());

        transferPlacemarkButton = createButton("icons/MultiAssignProducts24.gif");
        transferPlacemarkButton.setName("transferButton");
        transferPlacemarkButton.setToolTipText("Transfer the selected " + placemarkLabel + "s to other products.");
        transferPlacemarkButton.addActionListener(e -> topComponent.transferPlacemarks());

        final AbstractButton helpButton = createButton("icons/Help22.png");
        helpButton.setToolTipText("Help."); /*I18N*/
        helpButton.setName("helpButton");
        helpButton.addActionListener(e -> HelpDisplayer.show(topComponent.getHelpCtx()));

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.5;
        gbc.gridy++;
        add(newButton, gbc);
        add(copyButton, gbc);
        gbc.gridy++;
        add(editButton, gbc);
        add(removeButton, gbc);
        gbc.gridy++;
        add(importButton, gbc);
        add(exportButton, gbc);
        gbc.gridy++;
        add(filterButton, gbc);
        add(exportTableButton, gbc);
        gbc.gridy++;
        add(zoomToPlacemarkButton, gbc);
        add(transferPlacemarkButton, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.gridx = 1;
        gbc.gridy++;
        gbc.gridwidth = 1;
        add(helpButton, gbc);
    }

    void updateUIState(final boolean productSelected, int numPins, final int numSelectedPins) {

        boolean pinsAvailable = numPins > 0;
        boolean hasSelectedPins = numSelectedPins > 0;
        boolean hasActivePin = numSelectedPins == 1;

        newButton.setEnabled(productSelected);
        copyButton.setEnabled(hasActivePin);
        editButton.setEnabled(hasActivePin);
        removeButton.setEnabled(hasSelectedPins);
        zoomToPlacemarkButton.setEnabled(hasActivePin);
        transferPlacemarkButton.setEnabled(pinsAvailable && SnapApp.getDefault().getProductManager().getProductCount() > 1);
        importButton.setEnabled(productSelected);
        exportButton.setEnabled(pinsAvailable);
        exportTableButton.setEnabled(pinsAvailable);
        filterButton.setEnabled(productSelected);
    }

    private static AbstractButton createButton(String path) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(path), false);
    }
}
