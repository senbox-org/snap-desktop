/*
 * Copyright (C) 2026 by SkyWatch Space Applications Inc. http://www.skywatch.com
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

import gov.nasa.worldwind.BasicFactory;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.AbstractGraticuleLayer;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.ScalebarLayer;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwindx.examples.util.LayerManagerLayer;
import org.esa.snap.worldwind.preferences.WorldWindPreferences;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * The initial layers of SNAP's WorldWind views, read from SNAP's own layer configuration
 * ({@code config/snap-worldwind-layers.xml}) instead of WorldWind's built-in defaults, so that the views only use
 * services SNAP has checked.
 * <p>
 * The layer list has base layers first and overlays (grids, world map, scale bar, compass, layer panel, place names)
 * last;
 * SNAP's layers are inserted between them with {@link #insertBelowOverlays}.
 */
public final class LayerConfiguration {

    static final String RESOURCE = "config/snap-worldwind-layers.xml";
    static final String IMAGERY_DATE_TOKEN = "${imageryDate}";

    /**
     * Layer value marking a layer drawn above SNAP's product layers.
     */
    public static final String OVERLAY = "org.esa.snap.worldwind.Overlay";

    private LayerConfiguration() {
    }

    /**
     * Creates the initial layers, with daily imagery of the last complete UTC day.
     */
    public static LayerList createLayers() {
        // the first layers may be created before the deferred module activator has run
        WorldWindPreferences.applyAtStartup();
        return createLayers(LocalDate.now(ZoneOffset.UTC).minusDays(1));
    }

    static LayerList createLayers(final LocalDate imageryDate) {
        final String xml = readResource().replace(IMAGERY_DATE_TOKEN, imageryDate.toString());
        final Document document;
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid WorldWind layer configuration " + RESOURCE, e);
        }

        final Object created = BasicFactory.create(AVKey.LAYER_FACTORY, document.getDocumentElement());
        final LayerList layers;
        if (created instanceof LayerList) {
            layers = (LayerList) created;
        } else if (created instanceof LayerList[]) {
            layers = LayerList.collapseLists((LayerList[]) created);
        } else {
            throw new IllegalStateException("WorldWind layer configuration " + RESOURCE + " contains no layer list");
        }
        for (Layer layer : layers) {
            if (layer instanceof WorldMapLayer || layer instanceof ScalebarLayer || layer instanceof CompassLayer ||
                    layer instanceof AbstractGraticuleLayer) {
                markOverlay(layer);
            }
        }
        return layers;
    }

    private static String readResource() {
        try (InputStream in = LayerConfiguration.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("WorldWind layer configuration " + RESOURCE + " not found");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read WorldWind layer configuration " + RESOURCE, e);
        }
    }

    public static void markOverlay(final Layer layer) {
        layer.setValue(OVERLAY, Boolean.TRUE);
    }

    public static boolean isOverlay(final Layer layer) {
        return Boolean.TRUE.equals(layer.getValue(OVERLAY)) || layer instanceof LayerManagerLayer;
    }

    /**
     * Adds a layer above the base layers and below the overlays. Layers added one after the other keep their order.
     */
    public static void insertBelowOverlays(final LayerList layers, final Layer layer) {
        int position = layers.size();
        for (int i = 0; i < layers.size(); i++) {
            if (isOverlay(layers.get(i))) {
                position = i;
                break;
            }
        }
        layers.add(position, layer);
    }
}
