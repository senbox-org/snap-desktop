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

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.TerrainProfileLayer;
import gov.nasa.worldwind.util.measure.MeasureTool;
import gov.nasa.worldwind.util.measure.MeasureToolController;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Distance and area measurement on the globe, and the terrain profile along the measured line (or under the cursor
 * while nothing is measured). Uses WorldWind's {@link MeasureTool} and {@link TerrainProfileLayer}.
 */
class MeasurePanel extends JPanel {

    static final String TERRAIN_PROFILE_NAME = "Terrain Profile";

    private enum Shape {
        LINE("Line", MeasureTool.SHAPE_LINE),
        PATH("Path", MeasureTool.SHAPE_PATH),
        POLYGON("Polygon", MeasureTool.SHAPE_POLYGON),
        CIRCLE("Circle", MeasureTool.SHAPE_CIRCLE);

        final String label;
        final String type;

        Shape(String label, String type) {
            this.label = label;
            this.type = type;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final WorldWindow wwd;
    private final MeasureTool measureTool;
    private final TerrainProfileLayer profileLayer;
    private final JToggleButton measureButton = new JToggleButton("Measure");
    private final JLabel resultLabel = new JLabel(" ");
    private final JComboBox<Shape> shapeCombo = new JComboBox<>(Shape.values());

    MeasurePanel(final WorldWindow wwd) {
        super(new GridLayout(0, 1, 2, 2));
        this.wwd = wwd;
        setBorder(BorderFactory.createTitledBorder("Measure"));

        measureTool = new MeasureTool(wwd);
        measureTool.setController(new MeasureToolController());
        measureTool.setLineColor(Color.YELLOW);
        measureTool.setFillColor(new Color(255, 255, 0, 40));
        measureTool.setFollowTerrain(true);
        measureTool.setMeasureShapeType(Shape.LINE.type);
        LayerConfiguration.markOverlay(measureTool.getLayer());

        profileLayer = new TerrainProfileLayer();
        profileLayer.setName(TERRAIN_PROFILE_NAME);
        profileLayer.setEventSource(wwd);
        profileLayer.setFollow(TerrainProfileLayer.FOLLOW_CURSOR);
        profileLayer.setZeroBased(false);
        // the layer list is in the lower left corner; keep the graph above the scale bar on the right
        profileLayer.setPosition(AVKey.SOUTHEAST);
        profileLayer.setLocationOffset(new Vec4(0, 40, 0));
        profileLayer.setEnabled(false);
        LayerConfiguration.markOverlay(profileLayer);
        final LayerList layers = wwd.getModel().getLayers();
        layers.add(profileLayer);

        shapeCombo.addActionListener(e -> {
            measureTool.clear();
            measureTool.setMeasureShapeType(((Shape) shapeCombo.getSelectedItem()).type);
            measureButton.setSelected(false);
            measureTool.setArmed(false);
        });
        measureButton.addActionListener(e -> measureTool.setArmed(measureButton.isSelected()));
        final JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            measureTool.clear();
            measureButton.setSelected(false);
            measureTool.setArmed(false);
        });
        final JCheckBox profileCheck = new JCheckBox("Terrain profile", false);
        profileCheck.setToolTipText("Elevation along the measured line, or under the cursor");
        profileCheck.addActionListener(e -> {
            profileLayer.setEnabled(profileCheck.isSelected());
            wwd.redraw();
        });

        measureTool.addPropertyChangeListener(this::measureChanged);

        final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        row.add(shapeCombo);
        row.add(measureButton);
        row.add(clearButton);
        add(row);
        add(resultLabel);
        add(profileCheck);
    }

    private void measureChanged(final PropertyChangeEvent event) {
        if (MeasureTool.EVENT_ARMED.equals(event.getPropertyName())) {
            measureButton.setSelected(measureTool.isArmed());
        }
        updateResult();
        updateProfilePath();
    }

    private void updateResult() {
        final Shape shape = (Shape) shapeCombo.getSelectedItem();
        final double length = measureTool.getLength();
        final double area = measureTool.getArea();
        if (shape == Shape.POLYGON || shape == Shape.CIRCLE) {
            resultLabel.setText(area > 0 ? formatArea(area) + ", perimeter " + formatLength(length) : " ");
        } else {
            resultLabel.setText(length > 0 ? formatLength(length) : " ");
        }
    }

    private void updateProfilePath() {
        final Shape shape = (Shape) shapeCombo.getSelectedItem();
        final List<? extends Position> positions = measureTool.getPositions();
        if ((shape == Shape.LINE || shape == Shape.PATH) && positions != null && positions.size() >= 2) {
            profileLayer.setPathPositions(new ArrayList<>(positions));
            profileLayer.setFollow(TerrainProfileLayer.FOLLOW_PATH);
        } else {
            profileLayer.setFollow(TerrainProfileLayer.FOLLOW_CURSOR);
        }
        wwd.redraw();
    }

    static String formatLength(final double metres) {
        if (metres < 1000) {
            return String.format(Locale.ENGLISH, "%.0f m", metres);
        }
        return String.format(Locale.ENGLISH, metres < 100000 ? "%.2f km" : "%.0f km", metres / 1000);
    }

    static String formatArea(final double squareMetres) {
        if (squareMetres < 1.0e6) {
            return String.format(Locale.ENGLISH, "%.0f m²", squareMetres);
        }
        return String.format(Locale.ENGLISH, squareMetres < 1.0e8 ? "%.2f km²" : "%.0f km²", squareMetres / 1.0e6);
    }
}
