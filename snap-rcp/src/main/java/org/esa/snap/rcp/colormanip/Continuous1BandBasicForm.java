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

import com.bc.ceres.swing.TableLayout;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.util.math.Range;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

public class Continuous1BandBasicForm implements ColorManipulationChildForm {

    private final ColorManipulationForm parentForm;
    private final JPanel contentPanel;
    private final AbstractButton logDisplayButton;
    private final MoreOptionsForm moreOptionsForm;
    private final ColorPaletteChooser colorPaletteChooser;
    private final JFormattedTextField minField;
    private final JFormattedTextField maxField;
    private String currentMinFieldValue = "";
    private String currentMaxFieldValue = "";
    private final DiscreteCheckBox discreteCheckBox;


    final Boolean[] minFieldActivated = {new Boolean(false)};
    final Boolean[] maxFieldActivated = {new Boolean(false)};
    final Boolean[] listenToLogDisplayButtonEnabled = {true};
    final Boolean[] basicSwitcherIsActive;


    private enum RangeKey {FromPaletteSource, FromData, FromMinMaxFields, FromCurrentPalette, ToggleLog, InvertPalette, Dummy}
    private boolean shouldFireChooserEvent;
    private boolean hidden = false;

    Continuous1BandBasicForm(final ColorManipulationForm parentForm, final Boolean[] basicSwitcherIsActive) {
        ColorPaletteManager.getDefault().loadAvailableColorPalettes(parentForm.getIODir().toFile());

        this.parentForm = parentForm;
        this.basicSwitcherIsActive = basicSwitcherIsActive;

        final TableLayout layout = new TableLayout();
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setTablePadding(2, 2);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableAnchor(TableLayout.Anchor.NORTH);
        layout.setCellPadding(0, 0, new Insets(8, 2, 2, 2));
        layout.setCellPadding(2, 0, new Insets(13, 2, 5, 2));

        final JPanel editorPanel = new JPanel(layout);
        editorPanel.add(new JLabel("Colour ramp:"));
        colorPaletteChooser = new ColorPaletteChooser();
        editorPanel.add(colorPaletteChooser);
        editorPanel.add(new JLabel("Display range"));

        minField = getNumberTextField(0.00001);
        maxField = getNumberTextField(1);

        final JPanel minPanel = new JPanel(new BorderLayout(5, 2));
        minPanel.add(new JLabel("Min:"), BorderLayout.WEST);
        minPanel.add(minField, BorderLayout.SOUTH);
        final JPanel maxPanel = new JPanel(new BorderLayout(5, 2));
        maxPanel.add(new JLabel("Max:"), BorderLayout.EAST);
        maxPanel.add(maxField, BorderLayout.SOUTH);

        final JPanel minMaxPanel = new JPanel(new BorderLayout(5, 5));
        minMaxPanel.add(minPanel, BorderLayout.WEST);
        minMaxPanel.add(maxPanel, BorderLayout.EAST);
        editorPanel.add(minMaxPanel);

        final JButton fromFile = new JButton("Range from File");
        final JButton fromData = new JButton("Range from Data");

        final JPanel buttonPanel = new JPanel(new BorderLayout(5, 10));
        buttonPanel.add(fromFile, BorderLayout.WEST);
        buttonPanel.add(fromData, BorderLayout.EAST);
        editorPanel.add(new JLabel(" "));
        editorPanel.add(buttonPanel);

        shouldFireChooserEvent = true;

        colorPaletteChooser.addActionListener(createListener(RangeKey.FromCurrentPalette));
//        minField.addActionListener(createListener(RangeKey.FromMinMaxFields));
//        maxField.addActionListener(createListener(RangeKey.FromMinMaxFields));
        maxField.getDocument().addDocumentListener(new DocumentListener() {
            @Override

            public void insertUpdate(DocumentEvent documentEvent) {
                handleMaxTextfield();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
            }
        });

        minField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                handleMinTextfield();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
            }
        });



        fromFile.addActionListener(createListener(RangeKey.FromPaletteSource));
        fromData.addActionListener(createListener(RangeKey.FromData));

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(editorPanel, BorderLayout.NORTH);
        moreOptionsForm = new MoreOptionsForm(this, parentForm.getFormModel().canUseHistogramMatching());
        discreteCheckBox = new DiscreteCheckBox(parentForm);
        moreOptionsForm.addRow(discreteCheckBox);
        parentForm.getFormModel().modifyMoreOptionsForm(moreOptionsForm);

        logDisplayButton = LogDisplay.createButton();
        logDisplayButton.addActionListener(e -> {
            if (listenToLogDisplayButtonEnabled[0]) {
                listenToLogDisplayButtonEnabled[0] = false;
                logDisplayButton.setSelected(!logDisplayButton.isSelected());

                applyChanges(RangeKey.ToggleLog);
                listenToLogDisplayButtonEnabled[0] = true;
            }
        });
    }

    private void handleMaxTextfield() {

        if (!currentMaxFieldValue.equals(maxField.getText().toString())) {
            if (!maxFieldActivated[0] && !basicSwitcherIsActive[0]) {
                maxFieldActivated[0] = true;
                applyChanges(RangeKey.FromMinMaxFields);
                maxFieldActivated[0] = false;
            }
        }
    }

    private void handleMinTextfield() {

        if (!currentMinFieldValue.equals(minField.getText().toString())) {
            if (!minFieldActivated[0] && !basicSwitcherIsActive[0]) {
                minFieldActivated[0] = true;
                applyChanges(RangeKey.FromMinMaxFields);
                minFieldActivated[0] = false;
            }
        }
    }

    @Override
    public Component getContentPanel() {
        return contentPanel;
    }

    @Override
    public ColorManipulationForm getParentForm() {
        return parentForm;
    }

    @Override
    public void handleFormShown(ColorFormModel formModel) {
        hidden = false;
        updateFormModel(formModel);
    }

    @Override
    public void handleFormHidden(ColorFormModel formModel) {
        hidden = true;
    }

    @Override
    public void updateFormModel(ColorFormModel formModel) {
        if (!hidden) {
            ColorPaletteManager.getDefault().loadAvailableColorPalettes(parentForm.getIODir().toFile());
            colorPaletteChooser.reloadPalettes();
        }

        final ImageInfo imageInfo = formModel.getOriginalImageInfo();
        final ColorPaletteDef cpd = imageInfo.getColorPaletteDef();

        final boolean logScaled = imageInfo.isLogScaled();
        final boolean discrete = cpd.isDiscrete();

        colorPaletteChooser.setLog10Display(logScaled);
        colorPaletteChooser.setDiscreteDisplay(discrete);
        shouldFireChooserEvent = false;
        colorPaletteChooser.setSelectedColorPaletteDefinition(cpd);
        shouldFireChooserEvent = true;

        discreteCheckBox.setDiscreteColorsMode(discrete);
        logDisplayButton.setSelected(logScaled);
        parentForm.revalidateToolViewPaneControl();
        if (!minFieldActivated[0]) {
            minField.setValue(cpd.getMinDisplaySample());
            currentMinFieldValue = minField.getText().toString();
        }

        if (!maxFieldActivated[0]) {
            maxField.setValue(cpd.getMaxDisplaySample());
            currentMaxFieldValue = maxField.getText().toString();
        }
//        minField.setValue(cpd.getMinDisplaySample());
//        maxField.setValue(cpd.getMaxDisplaySample());
    }

    @Override
    public void resetFormModel(ColorFormModel formModel) {
        updateFormModel(formModel);
        parentForm.revalidateToolViewPaneControl();
    }

    @Override
    public void handleRasterPropertyChange(ProductNodeEvent event, RasterDataNode raster) {
        if (event.getPropertyName().equals(RasterDataNode.PROPERTY_NAME_STX)) {
            updateFormModel(parentForm.getFormModel());
        }
    }

    @Override
    public RasterDataNode[] getRasters() {
        return parentForm.getFormModel().getRasters();
    }

    @Override
    public MoreOptionsForm getMoreOptionsForm() {
        return moreOptionsForm;
    }

    @Override
    public AbstractButton[] getToolButtons() {
        return new AbstractButton[]{
                    logDisplayButton,
        };
    }

    private ActionListener createListener(final RangeKey key) {
        return e -> applyChanges(key);
    }

    private JFormattedTextField getNumberTextField(double value) {
        final NumberFormatter formatter = new NumberFormatter(new DecimalFormat("0.0############"));
        formatter.setValueClass(Double.class); // to ensure that double values are returned
        final JFormattedTextField numberField = new JFormattedTextField(formatter);
        numberField.setValue(value);
        final Dimension preferredSize = numberField.getPreferredSize();
        preferredSize.width = 70;
        numberField.setPreferredSize(preferredSize);
        return numberField;
    }

    private void applyChanges(RangeKey key) {
        if (shouldFireChooserEvent) {
            boolean checksOut = true;

            final ColorPaletteDef selectedCPD = colorPaletteChooser.getSelectedColorPaletteDefinition();
            final ImageInfo currentInfo = parentForm.getFormModel().getModifiedImageInfo();
            final ColorPaletteDef currentCPD = currentInfo.getColorPaletteDef();
            final ColorPaletteDef deepCopy = selectedCPD.createDeepCopy();
            deepCopy.setDiscrete(currentCPD.isDiscrete());

            final double min;
            final double max;
            final boolean isSourceLogScaled;
            final boolean isTargetLogScaled;
            final ColorPaletteDef cpd;
            final boolean autoDistribute;

            switch (key) {
            case FromPaletteSource:
                final Range rangeFromFile = colorPaletteChooser.getRangeFromFile();
                isSourceLogScaled = currentInfo.isLogScaled();
                isTargetLogScaled = currentInfo.isLogScaled();
                min = rangeFromFile.getMin();
                max = rangeFromFile.getMax();
                cpd = currentCPD;
                autoDistribute = true;
                break;
            case FromData:
                final Stx stx = parentForm.getStx(parentForm.getFormModel().getRaster());
                isSourceLogScaled = currentInfo.isLogScaled();
                isTargetLogScaled = currentInfo.isLogScaled();
                min = stx.getMinimum();
                max = stx.getMaximum();
                cpd = currentCPD;
                autoDistribute = true;
                break;
            case FromMinMaxFields:
                isSourceLogScaled = currentInfo.isLogScaled();
                isTargetLogScaled = currentInfo.isLogScaled();
//                parentForm.getImageInfo().getColorPaletteSourcesInfo().setAlteredScheme(true);


                if (ColorUtils.checkRangeCompatibility(minField.getText().toString(), maxField.getText().toString())) {
                    min = Double.parseDouble(minField.getText().toString());
                    max = Double.parseDouble(maxField.getText().toString());
                } else {
                    checksOut = false;
                    min = 0; //bogus unused values set just so it is initialized to make idea happy
                    max = 0; //bogus unused values set just so it is initialized to make idea happy
                }

                cpd = currentCPD;
                autoDistribute = true;
                break;
            case ToggleLog:
                isSourceLogScaled = currentInfo.isLogScaled();
                isTargetLogScaled = !currentInfo.isLogScaled();
//                parentForm.getImageInfo().getColorPaletteSourcesInfo().setAlteredScheme(true);

                min = currentCPD.getMinDisplaySample();
                max = currentCPD.getMaxDisplaySample();
                cpd = currentCPD;

                autoDistribute = true;
                break;
            default:
                isSourceLogScaled = selectedCPD.isLogScaled();
                isTargetLogScaled = currentInfo.isLogScaled();
                min = currentCPD.getMinDisplaySample();
                max = currentCPD.getMaxDisplaySample();
                cpd = deepCopy;
                autoDistribute = true;

            }

            if (checksOut && ColorUtils.checkRangeCompatibility(min, max, isTargetLogScaled)) {
//                if (key == RangeKey.InvertPalette) {
//                    currentInfo.setColorPaletteDefInvert(cpd);
//                } else {
//                    currentInfo.setColorPaletteDef(cpd, min, max, autoDistribute, isSourceLogScaled, isTargetLogScaled);
//                }
                currentInfo.setColorPaletteDef(cpd, min, max, autoDistribute, isSourceLogScaled, isTargetLogScaled);

                if (key == RangeKey.ToggleLog) {
                    currentInfo.setLogScaled(isTargetLogScaled);
                    colorPaletteChooser.setLog10Display(isTargetLogScaled);
                }
                currentMinFieldValue = Double.toString(min);
                currentMaxFieldValue = Double.toString(max);
                parentForm.applyChanges();
            }
        }
    }




}
