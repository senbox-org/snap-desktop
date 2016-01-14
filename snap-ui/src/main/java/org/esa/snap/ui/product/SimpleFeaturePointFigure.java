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

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.AbstractPointFigure;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.Handle;
import com.bc.ceres.swing.figure.Symbol;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.figure.support.NamedSymbol;
import com.bc.ceres.swing.figure.support.PointHandle;
import com.bc.ceres.swing.figure.support.ShapeSymbol;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.SceneRasterTransform;
import org.esa.snap.core.util.AwtGeomToJtsGeomConverter;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


/**
 * A figure representing point features.
 *
 * @author Norman Fomferra
 */
public class SimpleFeaturePointFigure extends AbstractPointFigure implements SimpleFeatureFigure {

    private static final Font labelFont = new Font("Helvetica", Font.BOLD, 14);
    private static final int[] labelOutlineAlphas = new int[]{64, 128, 192, 255};
    private static final Stroke[] labelOutlineStrokes = new Stroke[labelOutlineAlphas.length];
    private static final Color[] labelOutlineColors = new Color[labelOutlineAlphas.length];
    private static final Color labelFontColor = Color.WHITE;
    private static final Color labelOutlineColor = Color.BLACK;
    private static final String[] labelAttributeNames = new String[] {
            Placemark.PROPERTY_NAME_LABEL,
            "Label",
    };

    private SceneRasterTransform sceneRasterTransform;
    private SimpleFeature simpleFeature;
    private Point geometry;

    static {
        for (int i = 0; i < labelOutlineAlphas.length; i++) {
            labelOutlineStrokes[i] = new BasicStroke((labelOutlineAlphas.length - i));
            labelOutlineColors[i] = new Color(labelOutlineColor.getRed(),
                                              labelOutlineColor.getGreen(),
                                              labelOutlineColor.getBlue(),
                                              labelOutlineAlphas[i]);
        }
    }

    public SimpleFeaturePointFigure(SimpleFeature simpleFeature, SceneRasterTransform sceneRasterTransform, FigureStyle style) {
        this(simpleFeature, sceneRasterTransform, style, style);
    }

    public SimpleFeaturePointFigure(SimpleFeature simpleFeature, SceneRasterTransform sceneRasterTransform,
                                    FigureStyle normalStyle, FigureStyle selectedStyle) {
        super(normalStyle, selectedStyle);
        this.simpleFeature = simpleFeature;
        this.sceneRasterTransform = sceneRasterTransform;
        Object o = simpleFeature.getDefaultGeometry();
        if (!(o instanceof Point)) {
            throw new IllegalArgumentException("simpleFeature");
        }
        setGeometry((Point) o);
        setSelectable(true);
    }

    @Override
    public Object createMemento() {
        return getGeometry().clone();
    }

    @Override
    public void setMemento(Object memento) {
        Point point = (Point) memento;
        if (sceneRasterTransform == null) {
            return;
        }
        final MathTransform2D forward = sceneRasterTransform.getForward();
        if (forward == null) {
            return;
        }
        try {
            simpleFeature.setDefaultGeometry(forward.transform(new PixelPos(point.getX(), point.getY()), new PixelPos()));
            geometry = point;
            fireFigureChanged();
        } catch (TransformException e) {
            return;
        }
    }

    @Override
    public SimpleFeature getSimpleFeature() {
        return simpleFeature;
    }

    @Override
    public Point getGeometry() {
        return geometry;
    }

    @Override
    public void setGeometry(Geometry geometry) {
        if (sceneRasterTransform == null) {
            return;
        }
        Point point = (Point) geometry;
        final PixelPos startPos = new PixelPos(point.getX(), point.getY());
        PixelPos targetPos = new PixelPos();
        final MathTransform2D inverse = sceneRasterTransform.getInverse();
        if (inverse == null) {
            return;
        }
        try {
            inverse.transform(startPos, targetPos);
        } catch (TransformException e) {
            throw new IllegalStateException("simpleFeature", e);
        }
        Coordinate coordinate = new Coordinate(targetPos.getX(), targetPos.getY());
        this.geometry = new Point(new CoordinateArraySequence(new Coordinate[]{coordinate}), point.getFactory());
    }

    @Override
    public void forceRegeneration() {
        setGeometry((Geometry) simpleFeature.getDefaultGeometry());
    }

    @Override
    public double getX() {
        return geometry.getX();
    }

    @Override
    public double getY() {
        return geometry.getY();
    }

    @Override
    public void setLocation(double x, double y) {
        if (sceneRasterTransform == null) {
            return;
        }
        Coordinate coordinate = geometry.getCoordinate();
        coordinate.x = x;
        coordinate.y = y;
        try {
            final Point2D transform = sceneRasterTransform.getForward().transform(new PixelPos(x, y), new PixelPos());
            simpleFeature.setDefaultGeometry(new AwtGeomToJtsGeomConverter().createPoint(transform));
        } catch (TransformException e) {
            throw new IllegalStateException("simpleFeature", e);
        }
        geometry.geometryChanged();
        fireFigureChanged();
    }

    @Override
    public double getRadius() {
        return 1E-10; // = any small, non-zero value will be ok
    }

    @Override
    public Object clone() {
        SimpleFeaturePointFigure clone = (SimpleFeaturePointFigure) super.clone();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeature.getFeatureType());
        builder.init(simpleFeature);
        clone.simpleFeature = builder.buildFeature(null);
        clone.simpleFeature.setDefaultGeometry(getGeometry().clone());
        clone.geometry = (Point) clone.simpleFeature.getDefaultGeometry();
        return clone;
    }

    @Override
    protected void drawPoint(Rendering rendering) {
        super.drawPoint(rendering);
        String label = getLabel();
        if (label != null && !label.trim().isEmpty()) {
            drawLabel(rendering, label);
        }
    }

    private String getLabel() {
        for (String labelAttributeName : labelAttributeNames) {
            Object labelAttribute = simpleFeature.getAttribute(labelAttributeName);
            if (labelAttribute instanceof String) {
                return (String) labelAttribute;
            }
        }
        return null;
    }

    private void drawLabel(Rendering rendering, String label) {

        final Graphics2D graphics = rendering.getGraphics();
        final Font oldFont = graphics.getFont();
        final Stroke oldStroke = graphics.getStroke();
        final Paint oldPaint = graphics.getPaint();

        try {
            graphics.setFont(labelFont);
            GlyphVector glyphVector = labelFont.createGlyphVector(graphics.getFontRenderContext(), label);
            Rectangle2D logicalBounds = glyphVector.getLogicalBounds();
            float tx = (float) (logicalBounds.getX() - 0.5 * logicalBounds.getWidth());
            float ty = (float) (getSymbol().getBounds().getMaxY() + logicalBounds.getHeight() + 1.0);
            Shape labelOutline = glyphVector.getOutline(tx, ty);

            for (int i = 0; i < labelOutlineAlphas.length; i++) {
                graphics.setStroke(labelOutlineStrokes[i]);
                graphics.setPaint(labelOutlineColors[i]);
                graphics.draw(labelOutline);
            }

            graphics.setPaint(labelFontColor);
            graphics.fill(labelOutline);
        } finally {
            graphics.setPaint(oldPaint);
            graphics.setStroke(oldStroke);
            graphics.setFont(oldFont);
        }
    }

    @Override
    public int getMaxSelectionStage() {
        return 1;
    }

    @Override
    public Handle[] createHandles(int selectionStage) {
        if (selectionStage == 1) {
            DefaultFigureStyle handleStyle = new DefaultFigureStyle();
            handleStyle.setStrokeColor(Color.YELLOW);
            handleStyle.setStrokeOpacity(0.8);
            handleStyle.setStrokeWidth(1.0);
            handleStyle.setFillColor(Color.YELLOW);
            handleStyle.setFillOpacity(0.4);
            Symbol symbol = getSymbol();
            if (symbol instanceof NamedSymbol) {
                NamedSymbol namedSymbol = (NamedSymbol) symbol;
                symbol = namedSymbol.getSymbol();
            }
            if (symbol instanceof ShapeSymbol) {
                ShapeSymbol shapeSymbol = (ShapeSymbol) symbol;
                return new Handle[]{new PointHandle(this, handleStyle, shapeSymbol.getShape())};
            }
            return new Handle[]{new PointHandle(this, handleStyle)};
        }
        return super.createHandles(selectionStage);
    }
}
