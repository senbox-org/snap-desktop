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

package org.esa.snap.rcp.colormanip;
import org.openide.awt.ColorComboBox;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.util.NamingConvention;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.color.ColorComboBoxAdapter;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class MoreOptionsForm {

    static final String NO_DATA_COLOR_PROPERTY = "noDataColor";
    static final String HISTOGRAM_MATCHING_PROPERTY = "histogramMatching";

    private JPanel contentPanel;
    private GridBagConstraints constraints;
    private BindingContext bindingContext;

    private ColorManipulationChildForm childForm;
    private boolean hasHistogramMatching;

    private List<Row> contentRows;

    private static class Row {
        final JComponent label;
        final JComponent editor;

        private Row(JComponent label, JComponent editor) {
            this.label = label;
            this.editor = editor;
        }
    }


    MoreOptionsForm(ColorManipulationChildForm childForm, boolean hasHistogramMatching) {
        this.childForm = childForm;
        PropertyContainer propertyContainer = new PropertyContainer();
        propertyContainer.addProperty(Property.create(NO_DATA_COLOR_PROPERTY, ImageInfo.NO_COLOR));

        this.hasHistogramMatching = hasHistogramMatching;
        if (this.hasHistogramMatching) {
            propertyContainer.addProperty(Property.create(HISTOGRAM_MATCHING_PROPERTY, ImageInfo.HistogramMatching.None));
            propertyContainer.getDescriptor(HISTOGRAM_MATCHING_PROPERTY).setNotNull(true);
            propertyContainer.getDescriptor(HISTOGRAM_MATCHING_PROPERTY).setValueSet(
                    new ValueSet(
                            new ImageInfo.HistogramMatching[]{
                                    ImageInfo.HistogramMatching.None,
                                    ImageInfo.HistogramMatching.Equalize,
                                    ImageInfo.HistogramMatching.Normalize,
                            }
                    )
            );
        }

        contentPanel = new JPanel(new GridBagLayout());
        contentRows = new ArrayList<>();

        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.weightx = 0.5;
        constraints.weighty = 0.0;
        constraints.insets = new Insets(1, 0, 1, 0);

        bindingContext = new BindingContext(propertyContainer);

        JLabel noDataColorLabel = new JLabel("No-data " + NamingConvention.COLOR_LOWER_CASE + ": ");
        ColorComboBox noDataColorComboBox = new ColorComboBox();
        Binding noDataColorBinding = bindingContext.bind(NO_DATA_COLOR_PROPERTY, new ColorComboBoxAdapter(noDataColorComboBox));
        noDataColorBinding.addComponent(noDataColorLabel);
        addRow(noDataColorLabel, noDataColorComboBox);

        if (this.hasHistogramMatching) {
            JLabel histogramMatchingLabel = new JLabel("Histogram matching: ");
            JComboBox histogramMatchingBox = new JComboBox();
            Binding histogramMatchingBinding = bindingContext.bind(HISTOGRAM_MATCHING_PROPERTY, histogramMatchingBox);
            histogramMatchingBinding.addComponent(histogramMatchingLabel);
            addRow(histogramMatchingLabel, histogramMatchingBox);
        }

        bindingContext.addPropertyChangeListener(evt -> {
            final ImageInfo.HistogramMatching matching = getHistogramMatching();
            if (matching != null && matching != ImageInfo.HistogramMatching.None) {
                final String message = "<html>Histogram matching will be applied to the currently displayed image.<br/>" +
                                       "Sample values of the " + NamingConvention.COLOR_LOWER_CASE + " palette will no longer translate into<br/>" +
                                       "their associated " + NamingConvention.COLOR_LOWER_CASE + "s.</html>";
                Dialogs.showInformation("Histogram Matching", message, "warningHistogramMatching");
            }
            updateModel();
        });
    }

    private ImageInfo getImageInfo() {
        return getParentForm().getFormModel().getModifiedImageInfo();
    }

    public ColorManipulationForm getParentForm() {
        return childForm.getParentForm();
    }

    public ColorManipulationChildForm getChildForm() {
        return childForm;
    }

    public BindingContext getBindingContext() {
        return bindingContext;
    }

    public void insertRow(int index, JLabel label, JComponent editor) {
        if (contentRows != null) {
            contentRows.add(index, new Row(label, editor));
        } else {
            addRowImpl(label, editor);
        }
    }

    public void addRow(JLabel label, JComponent editor) {
        if (contentRows != null) {
            contentRows.add(new Row(label, editor));
        } else {
            addRowImpl(label, editor);
        }
    }

    public void addRow(JComponent editor) {
        if (contentRows != null) {
            contentRows.add(new Row(null, editor));
        } else {
            addRowImpl(null, editor);
        }
    }

    private void addRowImpl(JComponent label, JComponent editor) {
        constraints.gridy++;
        constraints.gridx = 0;
        if (label == null){
            constraints.gridwidth = 2;
            contentPanel.add(editor, constraints);
        } else {
            constraints.gridwidth = 1;
            contentPanel.add(label, constraints);
            constraints.gridx = 1;
            contentPanel.add(editor, constraints);
        }
    }

    public void updateForm() {
        setNoDataColor(getImageInfo().getNoDataColor());
        if (hasHistogramMatching) {
            setHistogramMatching(getImageInfo().getHistogramMatching());
        }
        getParentForm().getFormModel().updateMoreOptionsFromImageInfo(this);
    }

    public void updateModel() {
        getImageInfo().setNoDataColor(getNoDataColor());
        if (hasHistogramMatching) {
            getImageInfo().setHistogramMatching(getHistogramMatching());
        }
        getParentForm().getFormModel().updateImageInfoFromMoreOptions(this);
        getParentForm().applyChanges();
    }

    public JPanel getContentPanel() {
        if (contentRows != null) {
            Row[] rows = contentRows.toArray(new Row[contentRows.size()]);
            for (Row row : rows) {
                addRowImpl(row.label, row.editor);
            }
            contentRows.clear();
            contentRows = null;
        }
        return contentPanel;
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        bindingContext.addPropertyChangeListener(propertyChangeListener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener) {
        bindingContext.addPropertyChangeListener(propertyName, propertyChangeListener);
    }

    private Color getNoDataColor() {
        return (Color) getBindingContext().getBinding(NO_DATA_COLOR_PROPERTY).getPropertyValue();
    }

    private void setNoDataColor(Color color) {
        getBindingContext().getBinding(NO_DATA_COLOR_PROPERTY).setPropertyValue(color);
    }

    private ImageInfo.HistogramMatching getHistogramMatching() {
        Binding binding = getBindingContext().getBinding(HISTOGRAM_MATCHING_PROPERTY);
        return binding != null ? (ImageInfo.HistogramMatching) binding.getPropertyValue() : null;
    }

    private void setHistogramMatching(ImageInfo.HistogramMatching histogramMatching) {
        Binding binding = getBindingContext().getBinding(HISTOGRAM_MATCHING_PROPERTY);
        if (binding != null) {
            binding.setPropertyValue(histogramMatching);
        }
    }
}
