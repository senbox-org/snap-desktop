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
package org.esa.snap.worldwind;

import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Earth.LandsatI3WMSLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.SkyGradientLayer;
import gov.nasa.worldwind.layers.StarsLayer;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;
import gov.nasa.worldwind.util.PlacemarkClutterFilter;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.nasa.worldwind.view.orbit.FlatOrbitView;
import gov.nasa.worldwindx.examples.ClickAndGoSelectListener;
import org.esa.snap.worldwind.layers.FixingPlaceNameLayer;

import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * World Wind App Panel
 */
public class AppPanel extends JPanel {

    // display name of the Landsat layer in the WorldWind 2.2.1 layer configuration (LandsatI3WMSLayer2.xml)
    public static final String LANDSAT_LAYER_NAME = "i-cubed Landsat";

    private WorldWindowGLCanvas wwd = null;
    private StatusBar statusBar = null;

    public AppPanel(final WorldWindowGLCanvas shareWith, final boolean includeStatusBar, final boolean flatWorld,
                    final boolean removeExtraLayers) {
        super(new BorderLayout());

        this.wwd = new WorldWindowGLCanvas(shareWith);

        // Create the default model as described in the current worldwind properties.
        final Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
        this.wwd.setModel(m);
        if (flatWorld) {
            m.setGlobe(new EarthFlat());
            this.wwd.setView(new FlatOrbitView());
        } else {
            m.setGlobe(new Earth());
            this.wwd.setView(new BasicOrbitView());
        }

        if (removeExtraLayers) {
            final LayerList layerList = m.getLayers();
            for (Layer layer : layerList) {
                if (layer instanceof CompassLayer || layer instanceof WorldMapLayer || layer instanceof StarsLayer ||
                        layer instanceof LandsatI3WMSLayer || layer instanceof SkyGradientLayer ||
                        LANDSAT_LAYER_NAME.equals(layer.getName()))
                    layerList.remove(layer);
            }
        } else {
            // Setup a select listener for the worldmap click-and-go feature
            this.wwd.addSelectListener(new ClickAndGoSelectListener(wwd, WorldMapLayer.class));
        }

        this.wwd.getSceneController().setClutterFilter(new PlacemarkClutterFilter());

        this.add(this.wwd, BorderLayout.CENTER);

        if (includeStatusBar) {
            this.statusBar = new MinimalStatusBar();
            this.add(statusBar, BorderLayout.PAGE_END);
            this.statusBar.setEventSource(wwd);
        }
    }

    public void addLayerPanelLayer() {
        wwd.getModel().getLayers().add(new LayerPanelLayer(getWwd()));
    }

    public final WorldWindowGLCanvas getWwd() {
        return wwd;
    }

    public final StatusBar getStatusBar() {
        return statusBar;
    }

    /**
     * Enables a layer of the default configuration if it exists.
     */
    public static void enableLayer(final LayerList layerList, final String name) {
        final Layer layer = layerList.getLayerByName(name);
        if (layer != null) {
            layer.setEnabled(true);
        }
    }

    /**
     * Replaces the default place name layer by {@link FixingPlaceNameLayer}, which corrects some names
     * (https://senbox.atlassian.net/browse/SNAP-1476).
     * The layer is added disabled: the NASA place name WFS server (worldwind22.arc.nasa.gov) it relies on no longer
     * responds, so enabling it by default only produces failing requests. Users can still enable it in the layer list.
     */
    public static void replacePlaceNameLayer(final LayerList layerList) {
        for (Layer layer : layerList) {
            if (layer instanceof PlaceNameLayer) {
                layerList.remove(layer);
            }
        }
        final FixingPlaceNameLayer fixingPlaceNameLayer = new FixingPlaceNameLayer();
        fixingPlaceNameLayer.setEnabled(false);
        layerList.add(fixingPlaceNameLayer);
    }

    private static class MinimalStatusBar extends StatusBar {

        public MinimalStatusBar() {
            super();
            this.remove(altDisplay);
        }
    }
}
