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

package org.esa.snap.ui;

import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.BindingProblem;
import com.bc.ceres.swing.binding.BindingProblemListener;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

/**
 * Component which provides a panel where lat/lon bounds may be entered and bound to a given
 * {@link com.bc.ceres.swing.binding.BindingContext}.
 *
 * @author Marco Peters
 * @author Thomas Storm
 */
public class BoundsInputPanel {

    public static final String PROPERTY_WEST_BOUND = "westBound";
    public static final String PROPERTY_NORTH_BOUND = "northBound";
    public static final String PROPERTY_EAST_BOUND = "eastBound";
    public static final String PROPERTY_SOUTH_BOUND = "southBound";
    public static final String PROPERTY_PIXEL_SIZE_X = "pixelSizeX";
    public static final String PROPERTY_PIXEL_SIZE_Y = "pixelSizeY";

    static final DecimalFormatter DECIMAL_FORMATTER = new DecimalFormatter("0.0#######");

    private final BindingContext bindingContext;
    private final String enablePropertyKey;

    private JLabel pixelXUnit;
    private JLabel pixelYUnit;
    private JFormattedTextField pixelSizeXField;
    private JFormattedTextField pixelSizeYField;
    private final Map<String, Double> unitMap;
    private final String degreeSymbol = "°";

    /**
     * Default constructor.
     *
     * @param bindingContext    The binding context, in which the properties given by the constants
     *                          {@code PROPERTY_WEST_BOUND}, {@code PROPERTY_NORTH_BOUND},
     *                          {@code PROPERTY_EAST_BOUND}, and {@code PROPERTY_SOUTH_BOUND} are bound
     *                          accordingly.
     * @param enablePropertyKey The key for the property which specifies whether the sub-components of this component
     *                          are enabled.
     */
    public BoundsInputPanel(BindingContext bindingContext, String enablePropertyKey) {
        this.bindingContext = bindingContext;
        this.enablePropertyKey = enablePropertyKey;
        unitMap = new HashMap<>();
        unitMap.put("°", 0.05);
        unitMap.put("m", 1000.0);
        unitMap.put("km", 1.0);
    }

    /**
     * Creates the UI component. The enable state of the UI is controlled via the parameter {@code disableUIProperty}.
     * If it matches the value of the property provided in the constructor, the UI will be disabled.
     *
     * @param disableUIProperty Controls the enable state of the UI.
     * @return The UI component.
     */
    public JPanel createBoundsInputPanel(boolean disableUIProperty) {
        final TableLayout layout = new TableLayout(9);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setTablePadding(3, 3);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);
        layout.setColumnWeightX(2, 0.0);
        layout.setColumnWeightX(3, 0.0);
        layout.setColumnWeightX(4, 1.0);
        layout.setColumnWeightX(5, 0.0);
        layout.setColumnWeightX(6, 0.0);
        layout.setColumnWeightX(7, 1.0);
        layout.setColumnWeightX(8, 0.0);
        layout.setColumnPadding(2, new Insets(3, 0, 3, 12));
        layout.setColumnPadding(5, new Insets(3, 0, 3, 12));
        final JPanel panel = new JPanel(layout);
        pixelXUnit = new JLabel(degreeSymbol);
        pixelYUnit = new JLabel(degreeSymbol);

        panel.add(new JLabel("West:"));
        final JFormattedTextField westLonField = new JFormattedTextField(DECIMAL_FORMATTER);
        westLonField.setHorizontalAlignment(JTextField.RIGHT);
        bindingContext.bind(PROPERTY_WEST_BOUND, westLonField);
        bindingContext.bindEnabledState(PROPERTY_WEST_BOUND, false, enablePropertyKey, disableUIProperty);
        panel.add(westLonField);
        panel.add(new JLabel(degreeSymbol));
        panel.add(new JLabel("East:"));
        final JFormattedTextField eastLonField = new JFormattedTextField(DECIMAL_FORMATTER);
        eastLonField.setHorizontalAlignment(JTextField.RIGHT);
        bindingContext.bind(PROPERTY_EAST_BOUND, eastLonField);
        bindingContext.bindEnabledState(PROPERTY_EAST_BOUND, false, enablePropertyKey, disableUIProperty);
        panel.add(eastLonField);
        panel.add(new JLabel(degreeSymbol));
        panel.add(new JLabel("Pixel size X:"));
        pixelSizeXField = new JFormattedTextField(DECIMAL_FORMATTER);
        pixelSizeXField.setHorizontalAlignment(JTextField.RIGHT);
        pixelSizeXField.setValue(unitMap.get(degreeSymbol));
        bindingContext.bind(PROPERTY_PIXEL_SIZE_X, pixelSizeXField);
        bindingContext.bindEnabledState(PROPERTY_PIXEL_SIZE_X, false, enablePropertyKey, disableUIProperty);
        panel.add(pixelSizeXField);
        panel.add(pixelXUnit);

        panel.add(new JLabel("North:"));
        final JFormattedTextField northLatField = new JFormattedTextField(DECIMAL_FORMATTER);
        northLatField.setHorizontalAlignment(JTextField.RIGHT);
        bindingContext.bind(PROPERTY_NORTH_BOUND, northLatField);
        bindingContext.bindEnabledState(PROPERTY_NORTH_BOUND, false, enablePropertyKey, disableUIProperty);
        panel.add(northLatField);
        panel.add(new JLabel(degreeSymbol));
        panel.add(new JLabel("South:"));
        final JFormattedTextField southLatField = new JFormattedTextField(DECIMAL_FORMATTER);
        southLatField.setHorizontalAlignment(JTextField.RIGHT);
        bindingContext.bind(PROPERTY_SOUTH_BOUND, southLatField);
        bindingContext.bindEnabledState(PROPERTY_SOUTH_BOUND, false, enablePropertyKey, disableUIProperty);
        panel.add(southLatField);
        panel.add(new JLabel(degreeSymbol));
        panel.add(new JLabel("Pixel size Y:"));
        pixelSizeYField = new JFormattedTextField(DECIMAL_FORMATTER);
        pixelSizeYField.setHorizontalAlignment(JTextField.RIGHT);
        pixelSizeYField.setValue(unitMap.get(degreeSymbol));
        bindingContext.bind(PROPERTY_PIXEL_SIZE_Y, pixelSizeYField);
        bindingContext.bindEnabledState(PROPERTY_PIXEL_SIZE_Y, false, enablePropertyKey, disableUIProperty);
        panel.add(pixelSizeYField);
        panel.add(pixelYUnit);
        bindingContext.addProblemListener(new BindingProblemListener() {

            @Override
            public void problemReported(BindingProblem problem, BindingProblem ignored) {
                final String propertyName = problem.getBinding().getPropertyName();
                final boolean invalidBoundSet = propertyName.equals(PROPERTY_NORTH_BOUND) ||
                        propertyName.equals(PROPERTY_EAST_BOUND) ||
                        propertyName.equals(PROPERTY_SOUTH_BOUND) ||
                        propertyName.equals(PROPERTY_WEST_BOUND);
                if (invalidBoundSet) {
                    resetTextField(problem);
                }
            }

            private void resetTextField(BindingProblem problem) {
                problem.getBinding().getComponentAdapter().adjustComponents();
            }

            @Override
            public void problemCleared(BindingProblem ignored) {
                // do nothing
            }
        });
        return panel;
    }

    public void updatePixelUnit(CoordinateReferenceSystem crs) {
        final CoordinateSystem coordinateSystem = crs.getCoordinateSystem();
        final String unitX = coordinateSystem.getAxis(0).getUnit().toString();
        if (!unitX.equals(pixelXUnit.getText())) {
            pixelXUnit.setText(unitX);
            pixelSizeXField.setValue(unitMap.get("deg".equals(unitX) ? degreeSymbol : unitX));
        }
        final String unitY = coordinateSystem.getAxis(1).getUnit().toString();
        if (!unitY.equals(pixelYUnit.getText())) {
            pixelYUnit.setText(unitY);
            pixelSizeYField.setValue(unitMap.get("deg".equals(unitY) ? degreeSymbol : unitY));
        }
    }

}
