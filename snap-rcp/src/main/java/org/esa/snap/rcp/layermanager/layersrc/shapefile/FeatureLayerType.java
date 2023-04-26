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

package org.esa.snap.rcp.layermanager.layersrc.shapefile;

import com.bc.ceres.binding.*;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.annotations.LayerTypeMetadata;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.XppReader;
import org.esa.snap.core.util.FeatureUtils;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.Style;
import org.geotools.xml.styling.SLDParser;
import org.geotools.xml.styling.SLDTransformer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * The type of a {@link FeatureLayer}.
 * <p>
 * Unstable API. Use at own risk.
 */
@LayerTypeMetadata(name = "FeatureLayerType",
        aliasNames = {"FeatureLayerType"})
public class FeatureLayerType extends LayerType {

    public static final String PROPERTY_NAME_SLD_STYLE = "sldStyle";
    public static final String PROPERTY_NAME_FEATURE_COLLECTION = "featureCollection";
    public static final String PROPERTY_NAME_FEATURE_COLLECTION_URL = "featureCollectionUrl";
    public static final String PROPERTY_NAME_FEATURE_COLLECTION_CRS = "featureCollectionTargetCrs";
    public static final String PROPERTY_NAME_FEATURE_COLLECTION_CLIP_GEOMETRY = "featureCollectionClipGeometry";

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return true;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        CoordinateReferenceSystem targetCrs = null;
        if (ctx != null) {
            targetCrs = (CoordinateReferenceSystem) ctx.getCoordinateReferenceSystem();
        }
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc;
        fc = (FeatureCollection<SimpleFeatureType, SimpleFeature>) configuration.getValue(FeatureLayerType.PROPERTY_NAME_FEATURE_COLLECTION);
        if (fc == null) {
            try {
                final URL url = (URL) configuration.getValue(FeatureLayerType.PROPERTY_NAME_FEATURE_COLLECTION_URL);
                FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = FeatureUtils.getFeatureSource(url);
                fc = featureSource.getFeatures();
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
        final CoordinateReferenceSystem featureCrs = (CoordinateReferenceSystem) configuration.getValue(
                FeatureLayerType.PROPERTY_NAME_FEATURE_COLLECTION_CRS);
        final Geometry clipGeometry = (Geometry) configuration.getValue(
                FeatureLayerType.PROPERTY_NAME_FEATURE_COLLECTION_CLIP_GEOMETRY);

        fc = FeatureUtils.clipCollection(fc,
                featureCrs,
                clipGeometry,
                DefaultGeographicCRS.WGS84,
                null,
                targetCrs,
                ProgressMonitor.NULL);

        return new FeatureLayer(this, fc, configuration);
    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertyContainer configuration = new PropertyContainer();

        // Mandatory Parameters

        configuration.addProperty(Property.create(PROPERTY_NAME_FEATURE_COLLECTION, FeatureCollection.class));
        configuration.getDescriptor(PROPERTY_NAME_FEATURE_COLLECTION).setTransient(true);

        configuration.addProperty(Property.create(PROPERTY_NAME_SLD_STYLE, Style.class));
        configuration.getDescriptor(PROPERTY_NAME_SLD_STYLE).setDomConverter(new StyleDomConverter());
        configuration.getDescriptor(PROPERTY_NAME_SLD_STYLE).setNotNull(true);

        // Optional Parameters

        configuration.addProperty(Property.create(PROPERTY_NAME_FEATURE_COLLECTION_CLIP_GEOMETRY, Geometry.class));
        configuration.getDescriptor(PROPERTY_NAME_FEATURE_COLLECTION_CLIP_GEOMETRY).setDomConverter(
                new GeometryDomConverter());

        configuration.addProperty(Property.create(PROPERTY_NAME_FEATURE_COLLECTION_URL, URL.class));

        configuration.addProperty(Property.create(PROPERTY_NAME_FEATURE_COLLECTION_CRS, CoordinateReferenceSystem.class));
        configuration.getDescriptor(PROPERTY_NAME_FEATURE_COLLECTION_CRS).setDomConverter(new CRSDomConverter());

        return configuration;
    }

    private static class StyleDomConverter implements DomConverter {

        @Override
        public Class<?> getValueType() {
            return Style.class;
        }

        @Override
        public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                ValidationException {
            final DomElement child = parentElement.getChild(0);
            SLDParser s = new SLDParser(CommonFactoryFinder.getStyleFactory(null), new StringReader(child.toXml()));
            final Style[] styles = s.readXML();
            return styles[0];
        }

        @Override
        public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
            Style style = (Style) value;
            final SLDTransformer transformer = new SLDTransformer();
            transformer.setIndentation(2);
            try {
                final String s = transformer.transform(style);
                XppDomWriter domWriter = new XppDomWriter();
                new HierarchicalStreamCopier().copy(new XppReader(new StringReader(s)), domWriter);
                parentElement.addChild(new XppDomElement(domWriter.getConfiguration()));
            } catch (TransformerException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    private static class CRSDomConverter implements DomConverter {

        @Override
        public Class<?> getValueType() {
            return null;
        }

        @Override
        public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                ValidationException {
            try {
                value = CRS.parseWKT(parentElement.getValue());
            } catch (FactoryException e) {
                throw new IllegalArgumentException(e);
            }
            return value;
        }

        @Override
        public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
            CoordinateReferenceSystem crs = (CoordinateReferenceSystem) value;
            parentElement.setValue(crs.toWKT());

        }
    }

    private static class GeometryDomConverter implements DomConverter {

        @Override
        public Class<?> getValueType() {
            return Geometry.class;
        }

        @Override
        public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                ValidationException {
            org.locationtech.jts.geom.GeometryFactory gf = new org.locationtech.jts.geom.GeometryFactory();
            final DefaultDomConverter domConverter = new DefaultDomConverter(Coordinate.class);
            final DomElement[] children = parentElement.getChildren("coordinate");
            List<Coordinate> coordList = new ArrayList<Coordinate>();
            for (DomElement child : children) {
                final Coordinate coordinate = (Coordinate) domConverter.convertDomToValue(child, null);
                coordList.add(coordinate);
            }
            return gf.createPolygon(gf.createLinearRing(coordList.toArray(new Coordinate[coordList.size()])), null);
        }

        @Override
        public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
            Geometry geom = (Geometry) value;
            final Coordinate[] coordinates = geom.getCoordinates();
            final DefaultDomConverter domConverter = new DefaultDomConverter(Coordinate.class);
            for (Coordinate coordinate : coordinates) {
                final DomElement child = parentElement.createChild("coordinate");
                domConverter.convertValueToDom(coordinate, child);
            }
        }

    }


}
