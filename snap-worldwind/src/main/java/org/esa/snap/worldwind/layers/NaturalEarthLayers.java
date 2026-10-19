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
package org.esa.snap.worldwind.layers;

import gov.nasa.worldwind.formats.shapefile.Shapefile;
import gov.nasa.worldwind.formats.shapefile.ShapefileLayerFactory;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ShapeAttributes;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.worldwind.LayerConfiguration;

import java.awt.Color;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * Country borders and place names that work offline, from the Natural Earth 1:50m data bundled with the module.
 * Both are overlays: they are drawn above the base maps and the product layers.
 */
public final class NaturalEarthLayers {

    public static final String BORDERS_NAME = "Borders (Natural Earth)";
    public static final String PLACE_NAMES_NAME = NaturalEarthPlaceNameLayer.NAME;

    static final String BORDERS_RESOURCE = "/org/esa/snap/worldwind/naturalearth/ne_50m_admin_0_boundary_lines_land";
    static final String FEATURE_CLASS = "FEATURECLA";
    static final String INTERNATIONAL_BOUNDARY = "International boundary (verify)";

    private static final Color BORDER_COLOR = new Color(255, 255, 200);

    private NaturalEarthLayers() {
    }

    /**
     * Adds the borders and the place names on top of the given layers.
     */
    public static void addTo(final LayerList layers) {
        final Layer borders = createBordersLayer();
        if (borders != null) {
            LayerConfiguration.markOverlay(borders);
            layers.add(borders);
        }
        final NaturalEarthPlaceNameLayer placeNames = new NaturalEarthPlaceNameLayer();
        LayerConfiguration.markOverlay(placeNames);
        layers.add(placeNames);
    }

    /**
     * The country borders: international boundaries as solid lines, disputed, indefinite and other lines dashed.
     * The shapes are loaded in the background. Returns null if the bundled data cannot be read.
     */
    public static Layer createBordersLayer() {
        final InputStream shp = NaturalEarthLayers.class.getResourceAsStream(BORDERS_RESOURCE + ".shp");
        final InputStream shx = NaturalEarthLayers.class.getResourceAsStream(BORDERS_RESOURCE + ".shx");
        final InputStream dbf = NaturalEarthLayers.class.getResourceAsStream(BORDERS_RESOURCE + ".dbf");
        final InputStream prj = NaturalEarthLayers.class.getResourceAsStream(BORDERS_RESOURCE + ".prj");
        if (shp == null || shx == null || dbf == null || prj == null) {
            SystemUtils.LOG.warning("WorldWind: bundled Natural Earth borders not found");
            return null;
        }
        try {
            final ShapeAttributes solid = createBorderAttributes(false);
            final ShapeAttributes dashed = createBorderAttributes(true);
            final ShapefileLayerFactory factory = new ShapefileLayerFactory();
            factory.setNormalShapeAttributes(solid);
            factory.setAttributeDelegate((shapefileRecord, renderableRecord) -> {
                final Object featureClass = shapefileRecord.getAttributes() != null ?
                        shapefileRecord.getAttributes().getValue(FEATURE_CLASS) : null;
                renderableRecord.setAttributes(isInternationalBoundary(featureClass) ? solid : dashed);
            });
            // the factory reads and closes the shapefile in the background
            final Layer layer = (Layer) factory.createFromShapefileSource(new Shapefile(shp, shx, dbf, prj));
            layer.setName(BORDERS_NAME);
            layer.setPickEnabled(false);
            return layer;
        } catch (Exception e) {
            SystemUtils.LOG.log(Level.WARNING, "WorldWind: unable to read the bundled Natural Earth borders", e);
            return null;
        }
    }

    static boolean isInternationalBoundary(final Object featureClass) {
        return featureClass == null || INTERNATIONAL_BOUNDARY.equals(featureClass.toString().trim());
    }

    private static ShapeAttributes createBorderAttributes(final boolean dashed) {
        final ShapeAttributes attributes = new BasicShapeAttributes();
        attributes.setDrawInterior(false);
        attributes.setDrawOutline(true);
        attributes.setOutlineMaterial(new Material(BORDER_COLOR));
        attributes.setOutlineOpacity(0.8);
        attributes.setOutlineWidth(1.2);
        if (dashed) {
            attributes.setOutlineStippleFactor(3);
            attributes.setOutlineStipplePattern((short) 0xAAAA);
        }
        return attributes;
    }
}
