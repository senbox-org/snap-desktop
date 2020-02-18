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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.TableLayout;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.ui.SimpleDialog;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.DecimalFormat;

import static org.esa.snap.core.datamodel.ColorSchemeDefaults.*;
import static org.esa.snap.core.datamodel.ColorSchemeDefaults.PROPERTY_SCHEME_RANGE_DEFAULT;

/**
 * @author Brockmann Consult
 * @author Daniel Knowles (NASA)
 * @author Bing Yang (NASA)
 */
// OCT 2019 - Knowles / Yang
//          - Added DocumentListener to minField and maxField to make sure that the values are being updated.
//            Previously the values would only be updated if the user hit enter and a lose focus event would not
//            trigger a value update.
//          - Fixes log scaling bug where the log scaling was not affecting the palette values.  This was achieved
//            by tracking the source and target log scaling and passing this information to the method
//            setColorPaletteDef() in the class ImageInfo.
//          - Added numerical checks on the minField and maxField.
//
// NOV 2019 - Knowles / Yang
//          - Added color scheme logic
//          - Added capability to reverse the color palette
//          - Added capabiltiy to load exact values of the cpd file within any mathematical interpolation applied.
// DEC 2019 - Knowles / Yang
//          - Added capability to load scheme with data range values
//          - An empty min or empty max field within the schemes text will result in use of statistical min/max
// JAN 2020 - Knowles
//          - Added notification to user in the GUI when a scheme has been used in a non-nominal state (if the preferences altered)
//          - Implemented ColorSchemeManager
//          - Added verbose options to the scheme selector
//          - Added call to store and retrieve color scheme selector settings from ImageInfo
// FEB 2020 - Knowles
//          - Added functionality to select 'none' from the scheme selector
//          - Color scheme will be red if it is a duplicate scheme
//          - Popup window will notify a user why a color scheme is disabled (missing cpd file, etc.)
//          - Modifications to how min and max textfield listen to user input to enable a warning prompt for bad entries


public class Continuous1BandBasicForm implements ColorManipulationChildForm {

    private final ColorManipulationForm parentForm;
    private final JPanel contentPanel;
    private final AbstractButton logDisplayButton;
    private final JLabel schemeInfoLabel;
    private final MoreOptionsForm moreOptionsForm;
    private final ColorPaletteChooser colorPaletteChooser;
    private final JFormattedTextField minField;
    private final JFormattedTextField maxField;
    private final JButton fromFile;
    private final JButton fromData;
    private String currentMinFieldValue = "";
    private String currentMaxFieldValue = "";
    private final DiscreteCheckBox discreteCheckBox;
    private final JCheckBox loadWithCPDFileValuesCheckBox;
    //    private final JCheckBox loadPaletteOnlyCheckBox;
    private final ColorSchemeManager standardColorPaletteSchemes;
    private JLabel colorSchemeJLabel;
    private JButton paletteInversionButton;

    DocumentListener documentListener;


    final Boolean[] minFieldActivated = {new Boolean(false)};
    final Boolean[] maxFieldActivated = {new Boolean(false)};
    final Boolean[] listenToLogDisplayButtonEnabled = {true};
    final Boolean[] basicSwitcherIsActive;

    PropertyMap configuration = null;


    private enum RangeKey {FromCpdFile, FromData, FromMinMaxFields, FromMinField, FromMaxField, FromPaletteChooser, FromLogButton, InvertPalette, Dummy}

    private boolean shouldFireChooserEvent;
    private boolean hidden = false;

    Continuous1BandBasicForm(final ColorManipulationForm parentForm, final Boolean[] basicSwitcherIsActive) {
        ColorPaletteManager.getDefault().loadAvailableColorPalettes(parentForm.getIODir().toFile());

        this.parentForm = parentForm;
        this.basicSwitcherIsActive = basicSwitcherIsActive;


        if (parentForm.getFormModel().getProductSceneView() != null && parentForm.getFormModel().getProductSceneView().getSceneImage() != null) {
            configuration = parentForm.getFormModel().getProductSceneView().getSceneImage().getConfiguration();
        }


        colorSchemeJLabel = new JLabel("");
        colorSchemeJLabel.setToolTipText("The color data is stored in the band.  Astericks suffix (*) denotes that some parameters have been altered");

        schemeInfoLabel = new JLabel("TEST");


        standardColorPaletteSchemes = ColorSchemeManager.getDefault();


        loadWithCPDFileValuesCheckBox = new JCheckBox("Load exact values", false);
        loadWithCPDFileValuesCheckBox.setToolTipText("When loading a new cpd file, use it's actual value and overwrite user min/max values");

        paletteInversionButton = new JButton("Reverse");
        paletteInversionButton.setToolTipText("Reverse (invert) palette"); /*I18N*/
        paletteInversionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                applyChanges(RangeKey.InvertPalette);
            }
        });
        paletteInversionButton.setEnabled(true);

        colorPaletteChooser = new ColorPaletteChooser();

        minField = getNumberTextField(0.0000001);
        maxField = getNumberTextField(1.0000001);

        fromFile = new JButton("Cpd Range");
        fromData = new JButton("Data Range");


        final TableLayout layout = new TableLayout();
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setTablePadding(2, 2);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableAnchor(TableLayout.Anchor.NORTH);
        layout.setCellPadding(0, 0, new Insets(8, 2, 2, 2));
        layout.setCellPadding(1, 0, new Insets(8, 2, 2, 2));
        layout.setCellPadding(2, 0, new Insets(8, 2, 2, 2));
        layout.setCellPadding(3, 0, new Insets(13, 2, 5, 2));

        final JPanel editorPanel = new JPanel(layout);

        JPanel schemePanel = getSchemePanel("Scheme");
        editorPanel.add(schemePanel);

        JPanel palettePanel = getPalettePanel("Palette");
        editorPanel.add(palettePanel);

        JPanel rangePanel = getRangePanel("Range");
        editorPanel.add(rangePanel);

        shouldFireChooserEvent = false;

        colorPaletteChooser.addActionListener(createListener(RangeKey.FromPaletteChooser));


        documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                assist();
            }

            private void assist() {
                Runnable doAssist = new Runnable() {
                    @Override
                    public void run() {
                        handleMaxTextfield();
                    }
                };
                SwingUtilities.invokeLater(doAssist);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {

            }

            @Override
            public void changedUpdate(DocumentEvent e) {

            }
        };


        maxField.getDocument().addDocumentListener(documentListener);


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


        fromFile.addActionListener(createListener(RangeKey.FromCpdFile));
        fromData.addActionListener(createListener(RangeKey.FromData));
        fromData.setToolTipText("Set range from data");
        fromFile.setToolTipText("Set range from cpd file");

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

                applyChanges(RangeKey.FromLogButton);
                listenToLogDisplayButtonEnabled[0] = true;
            }
        });


        standardColorPaletteSchemes.getjComboBox().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                if (standardColorPaletteSchemes.getjComboBox().getSelectedIndex() != 0) {
                if (standardColorPaletteSchemes.isjComboBoxShouldFire()) {
                    standardColorPaletteSchemes.setjComboBoxShouldFire(false);
                    ColorSchemeDefaults.debug("Inside standardColorPaletteSchemes listener");
                    handleColorPaletteInfoComboBoxSelection();
                    standardColorPaletteSchemes.setjComboBoxShouldFire(true);
                }
//                }
            }
        });

        shouldFireChooserEvent = true;
    }


    private double getMaxValueWithTesting() throws NumberFormatException {

        if (maxField.getText().length() > 0) {
            boolean valid = true;

            final ImageInfo currentInfo = parentForm.getFormModel().getModifiedImageInfo();
            boolean logScaled = currentInfo.isLogScaled();
            double min = currentInfo.getColorPaletteDef().getMinDisplaySample();
            double max = 0.0; // bogus default

            try {
                max = Double.parseDouble(maxField.getText());
            } catch (NumberFormatException e) {
                valid = false;
                String errorMessage = e.getMessage();
                ColorUtils.showErrorDialog("ERROR!: Color Manipulation: \nMax field is not a number\":\n" + errorMessage);
            }

            if (valid && max <= min) {
                valid = false;
                ColorUtils.showErrorDialog("ERROR!: Color Manipulation: \nMax field must be greater than Min field\"");
            }

            if (valid && logScaled && max <= 0) {
                valid = false;
                ColorUtils.showErrorDialog("ERROR!: Color Manipulation: \nMax field cannot be zero with log scaling\"");
            }

            if (valid) {
                return max;
            } else {
                maxField.setValue(currentInfo.getColorPaletteDef().getMaxDisplaySample());
            }
        }

        maxFieldActivated[0] = false;

        throw new NumberFormatException("Problem");
    }
//
//    private void handleMaxTextfield() {
//
//        if (!currentMaxFieldValue.equals(maxField.getText())) {
//            if (!maxFieldActivated[0] && !basicSwitcherIsActive[0]) {
//                maxFieldActivated[0] = true;
//
//                if (maxField.getText().length() > 0) {
//                    boolean valid = true;
//
//                    final ImageInfo currentInfo = parentForm.getFormModel().getModifiedImageInfo();
//                    boolean logScaled = currentInfo.isLogScaled();
//                    double min = currentInfo.getColorPaletteDef().getMinDisplaySample();
//                    double max = 0.0; // bogus default
//
//                    try {
//                        max = Double.parseDouble(maxField.getText());
//                    } catch (NumberFormatException e) {
//                        valid = false;
//                        String errorMessage = e.getMessage();
//                        ColorUtils.showErrorDialog("ERROR!: Color Manipulation: \nMax field is not a number\":\n" + errorMessage);
//                    }
//
//                    if (valid && max <= min) {
//                        valid = false;
//                        ColorUtils.showErrorDialog("ERROR!: Color Manipulation: \nMax field must be greater than Min field\"");
//                    }
//
//                    if (valid && logScaled && max <= 0) {
//                        valid = false;
//                        ColorUtils.showErrorDialog("ERROR!: Color Manipulation: \nMax field cannot be zero with log scaling\"");
//                    }
//
//                    if (valid) {
//                        applyChanges(RangeKey.FromMaxField);
//                    } else {
//                        maxField.setValue(currentInfo.getColorPaletteDef().getMaxDisplaySample());
//                    }
//                }
//
//                maxFieldActivated[0] = false;
//            }
//        }
//    }


    private void handleMaxTextfield() {

        if (!currentMaxFieldValue.equals(maxField.getText())) {
            if (!maxFieldActivated[0] && !basicSwitcherIsActive[0]) {
                maxFieldActivated[0] = true;

                if (maxField.getText().length() > 0) {
                    applyChanges(RangeKey.FromMaxField);
                }

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

        ColorSchemeDefaults.debug("updateFormMode() in BasicForm");

        final ImageInfo imageInfo = formModel.getOriginalImageInfo();
        final ColorPaletteDef cpd = imageInfo.getColorPaletteDef();

        final boolean logScaled = imageInfo.isLogScaled();
        final boolean discrete = cpd.isDiscrete();

        colorPaletteChooser.setLog10Display(logScaled);
        colorPaletteChooser.setDiscreteDisplay(discrete);

        shouldFireChooserEvent = false;
        colorPaletteChooser.setSelectedColorPaletteDefinition(cpd);

        discreteCheckBox.setDiscreteColorsMode(discrete);
        logDisplayButton.setSelected(logScaled);

        PropertyMap configuration = parentForm.getFormModel().getProductSceneView().getSceneImage().getConfiguration();

        boolean schemeApply = configuration.getPropertyBool(PROPERTY_SCHEME_AUTO_APPLY_KEY, PROPERTY_SCHEME_AUTO_APPLY_DEFAULT);
        String schemeLogScaling = configuration.getPropertyString(PROPERTY_SCHEME_LOG_KEY, PROPERTY_SCHEME_LOG_DEFAULT);
        String schemeRange = configuration.getPropertyString(PROPERTY_SCHEME_RANGE_KEY, PROPERTY_SCHEME_RANGE_DEFAULT);
        String schemeCpd = configuration.getPropertyString(PROPERTY_SCHEME_CPD_KEY, PROPERTY_SCHEME_CPD_DEFAULT);


        schemeInfoLabel.setText("<html>*Modified scheme");
        schemeInfoLabel.setToolTipText("Not using exact scheme default: see preferences");

        boolean visible = false;


        ColorSchemeManager colorPaletteSchemes = ColorSchemeManager.getDefault();
        colorPaletteSchemes.setSelected(imageInfo.getColorSchemeInfo());

        if (colorPaletteSchemes.isSchemeSet() &&
                schemeApply &&
                (!PROPERTY_SCHEME_CPD_DEFAULT.equals(schemeCpd) ||
                        !PROPERTY_SCHEME_RANGE_DEFAULT.equals(schemeRange) ||
                        !PROPERTY_SCHEME_LOG_DEFAULT.equals(schemeLogScaling))
        ) {
            visible = true;
        }
        schemeInfoLabel.setVisible(visible);


        colorPaletteSchemes.checkPreferences(configuration);


        parentForm.revalidateToolViewPaneControl();

        if (!minFieldActivated[0]) {
            minField.setValue(cpd.getMinDisplaySample());
            currentMinFieldValue = minField.getText().toString();
        }

        if (!maxFieldActivated[0]) {
            maxField.setValue(cpd.getMaxDisplaySample());
            currentMaxFieldValue = maxField.getText().toString();
        }

        shouldFireChooserEvent = true;
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
        numberField.setPreferredSize(numberField.getPreferredSize());
        return numberField;
    }

    private void applyChanges(RangeKey key) {
        if (shouldFireChooserEvent) {
            boolean valid = true;
            String errorMessage = "";

            final ColorPaletteDef selectedCPD = colorPaletteChooser.getSelectedColorPaletteDefinition();
            final ImageInfo currentInfo = parentForm.getFormModel().getModifiedImageInfo();
            final ColorPaletteDef currentCPD = currentInfo.getColorPaletteDef();
            final ColorPaletteDef selectedCpdDeepCopy = selectedCPD.createDeepCopy();
            selectedCpdDeepCopy.setDiscrete(currentCPD.isDiscrete());


            // Set source and target values to be the same as the current values by default
            ColorPaletteDef sourceCpd = currentCPD;
            boolean sourceLogScaled = currentInfo.isLogScaled();
            double targetMin = currentCPD.getMinDisplaySample();
            double targetMax = currentCPD.getMaxDisplaySample();
            boolean targetLogScaled = currentInfo.isLogScaled();
            boolean targetAutoDistribute = currentCPD.isAutoDistribute();


            // Set any target values which will be changed from the source values
            switch (key) {
                case FromCpdFile:
                    targetMin = currentCPD.getSourceFileMin();
                    targetMax = currentCPD.getSourceFileMax();
                    targetAutoDistribute = true;
                    break;
                case FromData:
                    final Stx stx = parentForm.getStx(parentForm.getFormModel().getRaster());
                    targetMin = stx.getMinimum();
                    targetMax = stx.getMaximum();
                    targetAutoDistribute = true;
                    break;
                case FromMinMaxFields:
                    if (ColorUtils.checkRangeCompatibility(minField.getText().toString(), maxField.getText().toString())) {
                        targetMin = Double.parseDouble(minField.getText().toString());
                        targetMax = Double.parseDouble(maxField.getText().toString());
                    } else {
                        valid = false;
                    }
                    targetAutoDistribute = true;
                    break;
                case FromMaxField:
                    try {
                        targetMax = getMaxValueWithTesting();
                    } catch (NumberFormatException e) {
                        valid = false;
                    }
                    targetAutoDistribute = true;
                    break;
                case FromLogButton:
                    targetLogScaled = !currentInfo.isLogScaled();
                    targetAutoDistribute = true;
                    break;
                case InvertPalette:
                    targetAutoDistribute = true;
                    break;
                case FromPaletteChooser:
                    // the source needs to reflect the palette instead of the current imageInfo
                    sourceLogScaled = selectedCPD.isLogScaled();
                    sourceCpd = selectedCpdDeepCopy;

                    if (loadWithCPDFileValuesCheckBox.isSelected()) {
                        targetLogScaled = selectedCPD.isLogScaled();
                        targetMin = selectedCPD.getSourceFileMin();
                        targetMax = selectedCPD.getSourceFileMax();
                        targetAutoDistribute = false;
                    } else {
                        targetAutoDistribute = true;
                    }
                    break;
                default:
                    // this will not be reached
            }


            if (valid && !ColorUtils.checkRangeCompatibility(targetMin, targetMax, targetLogScaled)) {
                valid = false;
                errorMessage = "A problem exists due to min, max, and/or log scaling conflicts";
            }

            if (valid) {
                if (key == RangeKey.InvertPalette) {
                    currentInfo.setColorPaletteDefInvert(sourceCpd);
                } else {
                    currentInfo.setColorPaletteDef(sourceCpd, targetMin, targetMax, targetAutoDistribute, sourceLogScaled, targetLogScaled);
                }

                if (key == RangeKey.FromLogButton) {
                    currentInfo.setLogScaled(targetLogScaled);
                    colorPaletteChooser.setLog10Display(targetLogScaled);
                }

                standardColorPaletteSchemes.reset();
                currentInfo.setColorSchemeInfo(standardColorPaletteSchemes.getNoneColorSchemeInfo());

                currentMinFieldValue = Double.toString(targetMin);
                currentMaxFieldValue = Double.toString(targetMax);
                parentForm.applyChanges();
//            } else if (key != RangeKey.FromMinMaxFields) {
            } else {
                // Show error for all case except the min and max fields because those use a document listener which causes problems
                // The error will also be display at the bottom of the GUI

                if (key != RangeKey.FromMinMaxFields) {
                    minFieldActivated[0] = false;
                    maxFieldActivated[0] = false;

                    minField.setValue(currentCPD.getMinDisplaySample());
                    maxField.setValue(currentCPD.getMaxDisplaySample());

                    minFieldActivated[0] = true;
                    maxFieldActivated[0] = true;
                }
                ColorUtils.showErrorDialog("Color Manipulation: Color Scheme\":\n" + errorMessage);  /*I18N*/

//                SimpleDialog dialog = new SimpleDialog("Color Manipulation: Color Scheme", errorMessage, standardColorPaletteSchemes.getjComboBox());
//                dialog.setVisible(true);
//                dialog.setEnabled(true);
            }
        }
    }

    private JPanel getSchemePanel(String title) {
        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.setBorder(BorderFactory.createTitledBorder(title));
        jPanel.setToolTipText("Load a preset color scheme (sets the color-palette, min, max, and log fields)");
        GridBagConstraints gbc = new GridBagConstraints();


        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 4, 4, 4);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        jPanel.add(colorSchemeJLabel, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        jPanel.add(ColorSchemeManager.getDefault().getjComboBox(), gbc);

        gbc.gridy++;
        jPanel.add(schemeInfoLabel, gbc);

        return jPanel;
    }


    private JPanel getPalettePanel(String title) {
        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.setBorder(BorderFactory.createTitledBorder(title));
        jPanel.setToolTipText("");
        GridBagConstraints gbc = new GridBagConstraints();


        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 4, 4, 4);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        jPanel.add(colorPaletteChooser, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        final JPanel row2Panel = new JPanel(new BorderLayout(0, 0));
        row2Panel.add(loadWithCPDFileValuesCheckBox, BorderLayout.WEST);
        row2Panel.add(paletteInversionButton, BorderLayout.EAST);

        jPanel.add(row2Panel, gbc);


        return jPanel;
    }


    private JPanel getRangePanel(String title) {

        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.setBorder(BorderFactory.createTitledBorder(title));
        jPanel.setToolTipText("");
        GridBagConstraints gbc = new GridBagConstraints();


        final JPanel minPanel = new JPanel(new BorderLayout(0, 0));
        minPanel.add(new JLabel("Min:"), BorderLayout.WEST);
        minPanel.add(minField, BorderLayout.EAST);

        final JPanel maxPanel = new JPanel(new BorderLayout(0, 0));
        maxPanel.add(new JLabel("Max:"), BorderLayout.WEST);
        maxPanel.add(maxField, BorderLayout.EAST);

        final JPanel minMaxPanel = new JPanel(new BorderLayout(0, 0));
        minMaxPanel.add(minPanel, BorderLayout.WEST);
        minMaxPanel.add(maxPanel, BorderLayout.EAST);


        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        jPanel.add(minMaxPanel, gbc);


        final JPanel buttonPanel = new JPanel(new BorderLayout(5, 10));
        buttonPanel.add(fromFile, BorderLayout.WEST);
        buttonPanel.add(fromData, BorderLayout.EAST);


        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 4, 4, 4);


        jPanel.add(buttonPanel, gbc);

        return jPanel;
    }

    private ImageInfo createDefaultImageInfo() {
        try {
            return ProductUtils.createImageInfo(parentForm.getFormModel().getRasters(), false, ProgressMonitor.NULL);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getContentPanel(),
                    "Failed to create default image settings:\n" + e.getMessage(),
                    "I/O Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void handleColorPaletteInfoComboBoxSelection() {
        ColorSchemeInfo colorSchemeInfo = (ColorSchemeInfo) standardColorPaletteSchemes.getjComboBox().getSelectedItem();

        if (colorSchemeInfo != null) {
            if (colorSchemeInfo.isEnabled() && !colorSchemeInfo.isDivider()) {
                if (standardColorPaletteSchemes.isNoneScheme(colorSchemeInfo)) {
                    ColorSchemeUtils.setImageInfoToGeneralColor(configuration, createDefaultImageInfo(), parentForm.getFormModel().getProductSceneView());
                } else {
                    ColorSchemeUtils.setImageInfoToColorScheme(colorSchemeInfo, parentForm.getFormModel().getProductSceneView());
                }
                parentForm.getFormModel().setModifiedImageInfo(parentForm.getFormModel().getProductSceneView().getImageInfo());

            } else {
                if (!colorSchemeInfo.isDivider()) {
                    ColorSchemeDefaults.debug("Add a notification message window");
                    String message = standardColorPaletteSchemes.checkScheme(colorSchemeInfo);
                    ColorSchemeDefaults.debug(message);

                    SimpleDialog dialog = new SimpleDialog("Color Manipulation: Color Scheme", message, standardColorPaletteSchemes.getjComboBox());
                    dialog.setVisible(true);
                    dialog.setEnabled(true);
                }
            }
        }
        parentForm.applyChanges();


//        resetFormModel(parentForm.getFormModel());
    }


    private void applyChanges(double min,
                              double max,
                              ColorPaletteDef selectedCPD,
                              boolean isSourceLogScaled,
                              boolean isTargetLogScaled,
                              String colorSchemaName,
                              boolean isDefaultList) {


        final ImageInfo currentInfo = parentForm.getFormModel().getProductSceneView().getImageInfo();
        final ColorPaletteDef currentCPD = currentInfo.getColorPaletteDef();
        final ColorPaletteDef deepCopy = selectedCPD.createDeepCopy();
        deepCopy.setDiscrete(currentCPD.isDiscrete());
        deepCopy.setAutoDistribute(true);

        final boolean autoDistribute = true;
        currentInfo.setLogScaled(isTargetLogScaled);

        currentInfo.setColorPaletteDef(selectedCPD, min, max, autoDistribute, isSourceLogScaled, isTargetLogScaled);

        currentMinFieldValue = Double.toString(min);
        currentMaxFieldValue = Double.toString(max);

        parentForm.getFormModel().setModifiedImageInfo(currentInfo);
        parentForm.applyChanges();
    }
}
