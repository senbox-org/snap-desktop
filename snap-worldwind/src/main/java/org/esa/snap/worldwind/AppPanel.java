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

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Earth.LandsatI3WMSLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.SkyGradientLayer;
import gov.nasa.worldwind.layers.StarsLayer;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.terrain.WMSBasicElevationModel;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.PlacemarkClutterFilter;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.nasa.worldwind.view.orbit.FlatOrbitView;
import gov.nasa.worldwind.wms.CapabilitiesRequest;
import gov.nasa.worldwindx.examples.ClickAndGoSelectListener;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * World Wind App Panel
 */
public class AppPanel extends JPanel {
    private WorldWindowGLCanvas wwd = null;
    private StatusBar statusBar = null;

    public AppPanel(final Dimension canvasSize, final boolean includeStatusBar, final boolean flatWorld,
                    final boolean removeExtraLayers) {
        super(new BorderLayout());

        this.wwd = new WorldWindowGLCanvas();
        //this.wwd.setPreferredSize(canvasSize);

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
                        layer instanceof LandsatI3WMSLayer || layer instanceof SkyGradientLayer)
                    layerList.remove(layer);
            }
        }

        // Setup a select listener for the worldmap click-and-go feature
        this.wwd.addSelectListener(new ClickAndGoSelectListener(wwd, WorldMapLayer.class));

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

    public void addElevation() {
        try {
            final ElevationModel em = makeElevationModel();
            wwd.getModel().getGlobe().setElevationModel(em);
        } catch (Exception ignore) {
        }
    }

    public final WorldWindowGLCanvas getWwd() {
        return wwd;
    }

    public final StatusBar getStatusBar() {
        return statusBar;
    }

    private static ElevationModel makeElevationModel() throws URISyntaxException, ParserConfigurationException,
            IOException, SAXException {
        final URI serverURI = new URI("http://www.nasa.network.com/elev");

        final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        if (Configuration.getJavaVersion() >= 1.6) {
            try {
                docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (ParserConfigurationException e) {   // Note it and continue on. Some Java5 parsers don't support the feature.
                String message = Logging.getMessage("XML.NonvalidatingNotSupported");
                Logging.logger().finest(message);
            }
        }
        final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

        // Request the capabilities document from the server.
        final CapabilitiesRequest req = new CapabilitiesRequest(serverURI);
        final Document doc = docBuilder.parse(req.toString());

        // Parse the DOM as a capabilities document.
        // CHANGED
        //final Capabilities caps = Capabilities.parse(doc);
        final WMSCapabilities caps = new WMSCapabilities(doc);

        final double HEIGHT_OF_MT_EVEREST = 8850d; // meters
        final double DEPTH_OF_MARIANAS_TRENCH = -11000d; // meters

        // Set up and instantiate the elevation model
        final AVList params = new AVListImpl();
        params.setValue(AVKey.LAYER_NAMES, "|srtm3");
        params.setValue(AVKey.TILE_WIDTH, 150);
        params.setValue(AVKey.TILE_HEIGHT, 150);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, LatLon.fromDegrees(20, 20));
        params.setValue(AVKey.NUM_LEVELS, 8);
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        params.setValue(AVKey.ELEVATION_MIN, DEPTH_OF_MARIANAS_TRENCH);
        params.setValue(AVKey.ELEVATION_MAX, HEIGHT_OF_MT_EVEREST);

        final CompoundElevationModel cem = new CompoundElevationModel();
        cem.addElevationModel(new WMSBasicElevationModel(caps, params));

        return cem;
    }

    private static class MinimalStatusBar extends StatusBar {

        public MinimalStatusBar() {
            super();
            this.remove(altDisplay);
        }
    }
}