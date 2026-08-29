/*
 * Copyright (C) 2026 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.rcp.mask;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.jexp.impl.Tokenizer;

import javax.swing.BorderFactory;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

class RangeThresholdSliderPanel extends JPanel {


    static final int SLIDER_MINIMUM = -1000;
    static final int SLIDER_MAXIMUM = 1000;
    static final int SLIDER_CENTER = 0;

    private final MaskForm maskForm;
    private final ThresholdControl minimumControl;
    private final ThresholdControl maximumControl;
    private Mask selectedMask;
    private boolean updatingUi;


    RangeThresholdSliderPanel(MaskForm maskForm) {
        super(new BorderLayout(4, 4));
        this.maskForm = maskForm;
        this.minimumControl = new ThresholdControl("Min", true);
        this.maximumControl = new ThresholdControl("Max", false);

        setBorder(BorderFactory.createTitledBorder("Range threshold"));
        add(createContent(), BorderLayout.CENTER);
        setEnabled(false);
    }

    void updateSelection() {
        Mask[] selectedMasks = maskForm.getSelectedMasks();
        selectedMask = selectedMasks.length == 1 && isRangeMask(selectedMasks[0]) ? selectedMasks[0] : null;
        updateUiFromMask();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (minimumControl != null) {
            minimumControl.setEnabled(enabled);
            maximumControl.setEnabled(enabled);
        }
    }

    private JPanel createContent() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridy = 0;
        addControl(panel, gbc, minimumControl);
        gbc.gridy++;
        addControl(panel, gbc, maximumControl);
        return panel;
    }

    private void addControl(JPanel panel, GridBagConstraints gbc, ThresholdControl control) {
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        panel.add(control.label, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(control.slider, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        panel.add(control.valueField, gbc);
    }

    private void updateUiFromMask() {
        updatingUi = true;
        try {
            boolean enabled = selectedMask != null;
            setEnabled(enabled);
            if (enabled) {
                minimumControl.setValue(Mask.RangeType.getMinimum(selectedMask));
                maximumControl.setValue(Mask.RangeType.getMaximum(selectedMask));
            } else {
                minimumControl.clearValue();
                maximumControl.clearValue();
            }
        } finally {
            updatingUi = false;
        }
    }

    private void updateThreshold(ThresholdControl control) {
        if (updatingUi || selectedMask == null) {
            return;
        }
        if (!control.adjusting && control.slider.getValueIsAdjusting()) {
            control.startAdjusting();
        }
        if (control.slider.getValueIsAdjusting()) {
            double threshold = control.computeThreshold();
            applyThreshold(selectedMask, control.minimumThreshold, threshold, maskForm.getProduct());
            control.setValue(control.getCurrentValue());
            refreshMaskTable();
        } else if (control.adjusting) {
            control.finishAdjusting();
            updateUiFromMask();
        }
    }

    private void refreshMaskTable() {
        JTable maskTable = maskForm.getMaskTable();
        int selectedRow = maskForm.getSelectedRow();
        if (selectedRow >= 0) {
            ((AbstractTableModel) maskTable.getModel()).fireTableRowsUpdated(selectedRow, selectedRow);
        } else {
            maskTable.repaint();
        }
    }

    static boolean isRangeMask(Mask mask) {
        return mask != null && mask.getImageType() == Mask.RangeType.INSTANCE;
    }

    static double computeRelativeThreshold(double startValue, double lowerBound, double upperBound, int sliderValue) {
        if (sliderValue == SLIDER_CENTER) {
            return startValue;
        }
        double normalized = Math.min(1.0, Math.abs(sliderValue) / (double) SLIDER_MAXIMUM);
        double relative = (Math.pow(100.0, normalized) - 1.0) / 99.0;
        if (sliderValue < SLIDER_CENTER) {
            return startValue - relative * (startValue - lowerBound);
        }
        return startValue + relative * (upperBound - startValue);
    }

    static void applyThreshold(Mask mask, boolean minimumThreshold, double threshold) {
        applyThreshold(mask, minimumThreshold, threshold, null);
    }

    static void applyThreshold(Mask mask, boolean minimumThreshold, double threshold, Product product) {
        Range rasterRange = getRasterRange(mask, product);
        double minimum = Mask.RangeType.getMinimum(mask);
        double maximum = Mask.RangeType.getMaximum(mask);
        if (rasterRange != null) {
            minimum = clamp(minimum, rasterRange);
            maximum = clamp(maximum, rasterRange);
            threshold = clamp(threshold, rasterRange);
        }
        if (minimumThreshold) {
            minimum = Math.min(threshold, maximum);
        } else {
            maximum = Math.max(threshold, minimum);
        }
        Mask.RangeType.setMinimum(mask, minimum);
        Mask.RangeType.setMaximum(mask, maximum);
        mask.setDescription(Mask.RangeType.getExpression(mask));
        mask.setSourceImage((com.bc.ceres.multilevel.MultiLevelImage) null);
    }

    private static double clamp(double value, Range range) {
        return Math.max(range.minimum, Math.min(range.maximum, value));
    }

    private class ThresholdControl {

        private final JLabel label;
        private final JSlider slider;
        private final JFormattedTextField valueField;
        private final boolean minimumThreshold;
        private boolean adjusting;
        private double startValue;
        private double lowerBound;
        private double upperBound;

        private ThresholdControl(String name, boolean minimumThreshold) {
            this.label = new JLabel(name + ":");
            this.slider = new JSlider(SwingConstants.HORIZONTAL, SLIDER_MINIMUM, SLIDER_MAXIMUM, SLIDER_CENTER);
            this.valueField = new JFormattedTextField(new DecimalFormat("0.0#####",
                    DecimalFormatSymbols.getInstance(Locale.ENGLISH)));
            this.minimumThreshold = minimumThreshold;
            valueField.setColumns(10);
            valueField.setEditable(true);
            valueField.setFocusLostBehavior(JFormattedTextField.COMMIT);
            valueField.addActionListener(e -> applyValueField());
            valueField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    applyValueField();
                }
            });
            slider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    updateThreshold(ThresholdControl.this);
                }
            });
        }

        private void setEnabled(boolean enabled) {
            label.setEnabled(enabled);
            slider.setEnabled(enabled);
            valueField.setEnabled(enabled);
        }

        private void setValue(double value) {
            valueField.setValue(value);
        }

        private void clearValue() {
            valueField.setValue(null);
            slider.setValue(SLIDER_CENTER);
        }

        private void startAdjusting() {
            adjusting = true;
            slider.setValue(SLIDER_CENTER);
            double minimum = Mask.RangeType.getMinimum(selectedMask);
            double maximum = Mask.RangeType.getMaximum(selectedMask);
            startValue = minimumThreshold ? minimum : maximum;
            Range rasterRange = getRasterRange(selectedMask, maskForm.getProduct());
            if (minimumThreshold) {
                lowerBound = rasterRange != null ? rasterRange.minimum : createFallbackLowerBound(startValue, minimum, maximum);
                upperBound = maximum;
            } else {
                lowerBound = minimum;
                upperBound = rasterRange != null ? rasterRange.maximum : createFallbackUpperBound(startValue, minimum, maximum);
            }
        }

        private double computeThreshold() {
            return computeRelativeThreshold(startValue, lowerBound, upperBound, slider.getValue());
        }

        private double getCurrentValue() {
            return minimumThreshold ? Mask.RangeType.getMinimum(selectedMask) : Mask.RangeType.getMaximum(selectedMask);
        }

        private void applyValueField() {
            if (updatingUi || selectedMask == null) {
                return;
            }
            try {
                valueField.commitEdit();
                Number value = (Number) valueField.getValue();
                if (value != null) {
                    applyThreshold(selectedMask, minimumThreshold, value.doubleValue(), maskForm.getProduct());
                    setValue(getCurrentValue());
                    refreshMaskTable();
                }
            } catch (ParseException e) {
                setValue(getCurrentValue());
            }
        }

        private void finishAdjusting() {
            adjusting = false;
            slider.setValue(SLIDER_CENTER);
        }
    }

    private static double createFallbackLowerBound(double startValue, double minimum, double maximum) {
        double range = Math.max(Math.abs(maximum - minimum), Math.max(Math.abs(startValue) * 0.1, 1.0));
        return startValue - range * 10.0;
    }

    private static double createFallbackUpperBound(double startValue, double minimum, double maximum) {
        double range = Math.max(Math.abs(maximum - minimum), Math.max(Math.abs(startValue) * 0.1, 1.0));
        return startValue + range * 10.0;
    }

    static Range getRasterRange(Mask mask, Product product) {
        RasterDataNode raster = getReferencedRaster(mask, product);
        if (raster == null) {
            return null;
        }
        Stx stx = raster.getStx(false, ProgressMonitor.NULL);
        return new Range(stx.getMinimum(), stx.getMaximum());
    }

    static RasterDataNode getReferencedRaster(Mask mask, Product product) {
        if (mask == null || product == null) {
            return null;
        }
        String externalRasterName = Mask.RangeType.getRasterName(mask);
        for (RasterDataNode raster : product.getRasterDataNodes()) {
            if (Tokenizer.createExternalName(raster.getName()).equals(externalRasterName)) {
                return raster;
            }
        }
        return null;
    }

    static class Range {

        final double minimum;
        final double maximum;

        Range(double minimum, double maximum) {
            this.minimum = minimum;
            this.maximum = maximum;
        }
    }
}
