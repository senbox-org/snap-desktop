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

package org.esa.snap.ui.product;

import com.bc.ceres.swing.figure.AbstractShapeFigure;
import com.bc.ceres.swing.figure.FigureStyle;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Lineal;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Polygonal;
import com.vividsolutions.jts.geom.Puntal;
import org.esa.snap.core.datamodel.SceneTransformProvider;
import org.esa.snap.core.util.AwtGeomToJtsGeomConverter;
import org.esa.snap.core.util.Debug;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.LiteShape2;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Shape;

/**
 * A figure representing shape features.
 *
 * @author Norman Fomferra
 */
public class SimpleFeatureShapeFigure extends AbstractShapeFigure implements SimpleFeatureFigure {

    private SimpleFeature simpleFeature;
    private Shape shape;
    private final Class<?> geometryType;
    private SceneTransformProvider sceneTransformProvider;
    private static final Shape EMPTY_SHAPE = new java.awt.Polygon(new int[0], new int[0], 0);

    public SimpleFeatureShapeFigure(SimpleFeature simpleFeature, SceneTransformProvider provider, FigureStyle style) {
        this(simpleFeature, provider, style, style);
    }

    public SimpleFeatureShapeFigure(SimpleFeature simpleFeature, SceneTransformProvider provider,
                                    FigureStyle normalStyle, FigureStyle selectedStyle) {
        super(getRank(simpleFeature), normalStyle, selectedStyle);
        this.simpleFeature = simpleFeature;
        this.geometryType = simpleFeature.getDefaultGeometry().getClass();
        this.shape = null;
        sceneTransformProvider = provider;
    }

    @Override
    public Object createMemento() {
        return getGeometry().clone();
    }

    @Override
    public void setMemento(Object memento) {
        simpleFeature.setDefaultGeometry(memento);
        forceRegeneration();
        fireFigureChanged();
    }

    @Override
    public SimpleFeature getSimpleFeature() {
        return simpleFeature;
    }

    @Override
    public Geometry getGeometry() {
        return (Geometry) simpleFeature.getDefaultGeometry();
    }

    @Override
    public void setGeometry(Geometry geometry) {
        if (!geometryType.isAssignableFrom(geometry.getClass())) {
            Debug.trace("WARNING: Assigning a geometry of type " + geometry.getClass() + ", should actually be a " + geometryType);
        }
        simpleFeature.setDefaultGeometry(geometry);
        forceRegeneration();
        fireFigureChanged();
    }

    @Override
    public Shape getShape() {
        if (shape == null) {
            final Geometry featureGeometry = (Geometry) simpleFeature.getDefaultGeometry();
            try {
                Geometry modelGeometry = sceneTransformProvider.getSceneToModelTransform().transform(featureGeometry);
                shape = new LiteShape2(modelGeometry, null, null, true);
            } catch (TransformException | FactoryException e) {
                //return empty shape for drawing
                return EMPTY_SHAPE;
            }
        }
        return shape;
    }

    @Override
    public void forceRegeneration() {
        shape = null;
    }

    @Override
    public void setShape(Shape shape) {
        this.shape = shape;
        Geometry modelGeometry = getGeometryFromShape(shape);
        try {
            final Geometry sceneGeometry = sceneTransformProvider.getModelToSceneTransform().transform(modelGeometry);
            simpleFeature.setDefaultGeometry(sceneGeometry);
            fireFigureChanged();
        } catch (TransformException e) {
            this.shape = null;
        }
    }

    private Geometry getGeometryFromShape(Shape shape) {
        AwtGeomToJtsGeomConverter converter = new AwtGeomToJtsGeomConverter();
        Geometry geometry;
        // May need to handle more cases here in the future!  (nf)
        if (Polygon.class.isAssignableFrom(geometryType)) {
            geometry = converter.createPolygon(shape);
        } else if (MultiPolygon.class.isAssignableFrom(geometryType)) {
            geometry = converter.createMultiPolygon(shape);
        } else if (LinearRing.class.isAssignableFrom(geometryType)) {
            geometry = converter.createLinearRingList(shape).get(0);
        } else if (LineString.class.isAssignableFrom(geometryType)) {
            geometry = converter.createLineStringList(shape).get(0);
        } else {
            geometry = converter.createMultiLineString(shape);
        }
        return geometry;
    }

    @Override
    public Object clone() {
        SimpleFeatureShapeFigure clone = (SimpleFeatureShapeFigure) super.clone();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeature.getFeatureType());
        builder.init(simpleFeature);
        clone.simpleFeature = builder.buildFeature(null);
        clone.simpleFeature.setDefaultGeometry(getGeometry().clone());
        clone.shape = getShape();
        return clone;
    }

    static Rank getRank(SimpleFeature simpleFeature) {
        final Object geometry = simpleFeature.getDefaultGeometry();
        if (!(geometry instanceof Geometry)) {
            throw new IllegalArgumentException("simpleFeature: geometry type must be a " + Geometry.class);
        }
        return getRank((Geometry) geometry);
    }

    static Rank getRank(Geometry geometry) {
        if (geometry instanceof Puntal) {
            return Rank.POINT;
        } else if (geometry instanceof Lineal) {
            return Rank.LINE;
        } else if (geometry instanceof Polygonal) {
            return Rank.AREA;
        } else {
            return Rank.NOT_SPECIFIED;
        }
    }
}
