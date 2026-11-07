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

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Model;
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
import gov.nasa.worldwind.util.PlacemarkClutterFilter;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.nasa.worldwind.view.orbit.FlatOrbitView;
import gov.nasa.worldwindx.examples.ClickAndGoSelectListener;
import org.esa.snap.worldwind.layers.NaturalEarthLayers;

import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * World Wind App Panel
 */
public class AppPanel extends JPanel {

    // display name of the Landsat layer in SNAP's layer configuration
    public static final String LANDSAT_LAYER_NAME = "i-cubed Landsat";

    private WorldWindowGLCanvas wwd = null;
    private StatusBar statusBar = null;

    public AppPanel(final WorldWindowGLCanvas shareWith, final boolean includeStatusBar, final boolean flatWorld,
                    final boolean removeExtraLayers) {
        this(shareWith, includeStatusBar, flatWorld, createModel(flatWorld, removeExtraLayers));
    }

    /**
     * Creates the panel around a model prepared by {@link #createModel(boolean, boolean)}. Must be called on the EDT.
     */
    public AppPanel(final WorldWindowGLCanvas shareWith, final boolean includeStatusBar, final boolean flatWorld,
                    final Model model) {
        super(new BorderLayout());

        this.wwd = new WorldWindowGLCanvas(shareWith);
        this.wwd.setModel(model);
        this.wwd.setView(flatWorld ? new FlatOrbitView() : new BasicOrbitView());
        // local DEM files as terrain, if chosen in the preferences
        LocalDemTerrain.attach(model.getGlobe(), this.wwd);

        // Setup a select listener for the worldmap click-and-go feature (no effect without a world map layer)
        this.wwd.addSelectListener(new ClickAndGoSelectListener(wwd, WorldMapLayer.class));

        this.wwd.getSceneController().setClutterFilter(new PlacemarkClutterFilter());

        this.add(this.wwd, BorderLayout.CENTER);

        if (includeStatusBar) {
            this.statusBar = new MinimalStatusBar();
            this.add(statusBar, BorderLayout.PAGE_END);
            this.statusBar.setEventSource(wwd);
        }
    }

    /**
     * Creates the model from SNAP's layer configuration, not WorldWind's defaults. This is the expensive part of
     * setting up a WorldWind view (configuration, globe, layers) and does not touch Swing or OpenGL, so it may be
     * called from a background thread.
     */
    public static Model createModel(final boolean flatWorld, final boolean removeExtraLayers) {
        final Model m = new BasicModel(flatWorld ? new EarthFlat() : new Earth(), LayerConfiguration.createLayers());

        if (removeExtraLayers) {
            final LayerList layerList = m.getLayers();
            for (Layer layer : layerList) {
                if (layer instanceof CompassLayer || layer instanceof WorldMapLayer || layer instanceof StarsLayer ||
                        layer instanceof LandsatI3WMSLayer || layer instanceof SkyGradientLayer ||
                        LANDSAT_LAYER_NAME.equals(layer.getName()))
                    layerList.remove(layer);
            }
        }
        return m;
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
     * Adds the offline country borders and place names (Natural Earth) as overlays.
     */
    public static void addReferenceLayers(final LayerList layerList) {
        NaturalEarthLayers.addTo(layerList);
    }

    private static class MinimalStatusBar extends StatusBar {

        public MinimalStatusBar() {
            super();
            this.remove(altDisplay);
        }
    }
}
