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

package org.esa.snap.rcp.preferences.layer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.layer.WorldMapLayerType;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel handling world map layer preferences. Sub-panel of the "Layer"-panel.
 *
 * @author thomas
 */
@OptionsPanelController.SubRegistration(location = "LayerPreferences",
        displayName = "#Options_DisplayName_LayerWorldMap",
        keywords = "#Options_Keywords_LayerWorldMap",
        keywordsCategory = "Layer",
        id = "LayerWorldMap")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_LayerWorldMap=World Map Layer",
        "Options_Keywords_LayerWorldMap=layer, worldmap"
})
public final class WorldMapLayerController extends DefaultConfigController {

    /**
     * Preferences key for the world map type
     */
    public static final String PROPERTY_KEY_WORLDMAP_TYPE = "worldmap.type";

    protected PropertySet createPropertySet() {
        WorldMapBean bean = new WorldMapBean();
        String persisted = SnapApp.getDefault().getPreferences().get(PROPERTY_KEY_WORLDMAP_TYPE, bean.worldMapLayerType);
        bean.worldMapLayerType = persisted;
        return createPropertySet(bean);
    }

    @Override
    protected JPanel createPanel(BindingContext context) {
        List<WorldMapLayerType> worldMapLayerTypes = new ArrayList<>();
        for (LayerType layerType : LayerTypeRegistry.getLayerTypes()) {
            if (layerType instanceof WorldMapLayerType) {
                WorldMapLayerType worldMapLayerType = (WorldMapLayerType) layerType;
                worldMapLayerTypes.add(worldMapLayerType);
            }
        }

        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTablePadding(new Insets(4, 10, 0, 0));
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setColumnWeightX(1, 1.0);

        JPanel pageUI = new JPanel(tableLayout);
        Property property = context.getPropertySet().getProperty(PROPERTY_KEY_WORLDMAP_TYPE);

        JComboBox<WorldMapLayerType> box = new JComboBox<>();
        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected,
                                                                                 cellHasFocus);
                if (value instanceof WorldMapLayerType && rendererComponent instanceof JLabel) {
                    WorldMapLayerType worldMapLayerType = (WorldMapLayerType) value;
                    JLabel label = (JLabel) rendererComponent;
                    label.setText(worldMapLayerType.getLabel());
                }
                return rendererComponent;
            }
        });
        box.addActionListener(e -> {
            try {
                updateProperty(box, property);
            } catch (ValidationException e1) {
                SnapApp.getDefault().getLogger().severe(e1.getMessage());
            }
        });
        DefaultComboBoxModel<WorldMapLayerType> model = new DefaultComboBoxModel<>(worldMapLayerTypes.toArray(new WorldMapLayerType[worldMapLayerTypes.size()]));
        box.setModel(model);
        for (WorldMapLayerType layerType : worldMapLayerTypes) {
            if (layerType.getName().equals(property.getValue())) {
                box.setSelectedItem(layerType);
            }
        }

        pageUI.add(new JLabel(property.getDescriptor().getDisplayName() + ":"));
        pageUI.add(box);
        pageUI.add(tableLayout.createVerticalSpacer());

        JPanel parent = new JPanel(new BorderLayout());
        parent.add(pageUI, BorderLayout.CENTER);
        parent.add(Box.createHorizontalStrut(100), BorderLayout.EAST);
        return parent;
    }

    static void updateProperty(JComboBox<WorldMapLayerType> box, Property property) throws ValidationException {
        final WorldMapLayerType selectedItem = (WorldMapLayerType) box.getSelectedItem();
        if (selectedItem != null) {
            property.setValue(selectedItem.getName());
        }
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("options-worldmaplayer");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class WorldMapBean {

        @Preference(label = "World Map Layer", key = PROPERTY_KEY_WORLDMAP_TYPE)
        String worldMapLayerType = "BlueMarbleLayerType";
    }

}
