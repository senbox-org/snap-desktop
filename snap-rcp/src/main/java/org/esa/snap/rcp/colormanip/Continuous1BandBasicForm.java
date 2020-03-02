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

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;

import static org.esa.snap.core.datamodel.ColorManipulationDefaults.*;
import static org.esa.snap.core.datamodel.ColorManipulationDefaults.PROPERTY_SCHEME_RANGE_DEFAULT;

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
//          - Reduced some event handling by checking if components have changed before updating them


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
    private final DiscreteCheckBox discreteCheckBox;
    private final JCheckBox loadWithCPDFileValuesCheckBox;
    private final ColorSchemeManager colorSchemeManager;
    private JLabel colorSchemeJLabel;
    private JButton paletteInversionButton;


    final Boolean[] minTextFieldListenerEnabled = {new Boolean(true)};
    final Boolean[] maxTextFieldListenerEnabled = {new Boolean(true)};
    final Boolean[] logButtonListenerEnabled = {true};
    final Boolean[] basicSwitcherIsActive;

    PropertyMap configuration = null;


    private enum RangeKey {FromCpdFile, FromData, FromMinField, FromMaxField, FromPaletteChooser, FromLogButton, InvertPalette, Dummy}

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

        schemeInfoLabel = new JLabel("");


        colorSchemeManager = ColorSchemeManager.getDefault();


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

        fromFile = new JButton("From Palette");
        fromData = new JButton("From Data");


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


        maxField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (shouldFireChooserEvent) {
                    ColorManipulationDefaults.debug("Inside maxField listener");
                    assist();
                }
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
            public void removeUpdate(DocumentEvent e) {}

            @Override
            public void changedUpdate(DocumentEvent e) {}
        });


        minField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (shouldFireChooserEvent) {
                    ColorManipulationDefaults.debug("Inside minField listener");
                    assist();
                }
            }

            private void assist() {
                Runnable doAssist = new Runnable() {
                    @Override
                    public void run() {
                        handleMinTextfield();
                    }
                };
                SwingUtilities.invokeLater(doAssist);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {}

            @Override
            public void changedUpdate(DocumentEvent e) {}
        });


        fromFile.addActionListener(createListener(RangeKey.FromCpdFile));
        fromData.addActionListener(createListener(RangeKey.FromData));
        fromData.setToolTipText("Set range from data");
        fromFile.setToolTipText("Set range from palette file");

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(editorPanel, BorderLayout.NORTH);
        moreOptionsForm = new MoreOptionsForm(this, parentForm.getFormModel().canUseHistogramMatching());
        discreteCheckBox = new DiscreteCheckBox(parentForm);
        moreOptionsForm.addRow(discreteCheckBox);
        parentForm.getFormModel().modifyMoreOptionsForm(moreOptionsForm);

        logDisplayButton = LogDisplay.createButton();
        logDisplayButton.addActionListener(e -> {
            if (shouldFireChooserEvent) {
                if (logButtonListenerEnabled[0]) {
                    logButtonListenerEnabled[0] = false;
                    applyChanges(RangeKey.FromLogButton);
                    logButtonListenerEnabled[0] = true;
                }
            }
        });


        colorSchemeManager.getjComboBox().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (colorSchemeManager.isjComboBoxShouldFire()) {
                    colorSchemeManager.setjComboBoxShouldFire(false);
                    ColorManipulationDefaults.debug("Inside standardColorPaletteSchemes listener");
                    handleColorSchemeSelector();
                    colorSchemeManager.setjComboBoxShouldFire(true);
                }
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
                ColorUtils.showErrorDialog("ERROR!: " + TOOLNAME_COLOR_MANIPULATION + ": \nMax field is not a number\":\n" + errorMessage);
            }

            if (valid && max <= min) {
                valid = false;
                ColorUtils.showErrorDialog("ERROR!: " + TOOLNAME_COLOR_MANIPULATION + ": \nMax field must be greater than Min field\"");
            }

            if (valid && logScaled && max <= 0) {
                valid = false;
                ColorUtils.showErrorDialog("ERROR!: " + TOOLNAME_COLOR_MANIPULATION + ": \nMax field cannot be less than or equal to zero with log scaling\"");
            }

            if (valid) {
                return max;
            }
        }

        throw new NumberFormatException("Zero length max field");
    }


    private boolean testMinMaxAgainstCurrentLog(double min, double max) {
        boolean valid = true;

        final ImageInfo currentInfo = parentForm.getFormModel().getModifiedImageInfo();
        boolean logScaled = currentInfo.isLogScaled();

        if (logScaled && min <= 0) {
            valid = false;
            ColorUtils.showErrorDialog("ERROR!: " + TOOLNAME_COLOR_MANIPULATION + ": \nMin field cannot be set to less than or equal to zero with log scaling\"");
        } else if (logScaled && max <= 0) {
            valid = false;
            ColorUtils.showErrorDialog("ERROR!: " + TOOLNAME_COLOR_MANIPULATION + ": \nMax field cannot be set to less than or equal to zero with log scaling\"");
        }

        return valid;
    }


    private double getMinValueWithTesting() throws NumberFormatException {

        if (minField.getText().length() > 0) {
            boolean valid = true;

            final ImageInfo currentInfo = parentForm.getFormModel().getModifiedImageInfo();
            boolean logScaled = currentInfo.isLogScaled();
            double min = 0.0; // bogus default
            double max = currentInfo.getColorPaletteDef().getMaxDisplaySample();

            try {
                min = Double.parseDouble(minField.getText());
            } catch (NumberFormatException e) {
                valid = false;
                String errorMessage = e.getMessage();
                ColorUtils.showErrorDialog("ERROR!: " + TOOLNAME_COLOR_MANIPULATION + ": \nMin field is not a number\":\n" + errorMessage);
            }

            if (valid && max <= min) {
                valid = false;
                ColorUtils.showErrorDialog("ERROR!: " + TOOLNAME_COLOR_MANIPULATION + ": \nMin field must be less than Max field\"");
            }

            if (valid && logScaled && min <= 0) {
                valid = false;
                ColorUtils.showErrorDialog("ERROR!: " + TOOLNAME_COLOR_MANIPULATION + ": \nMin field cannot be less than or equal to zero with log scaling\"");
            }

            if (valid) {
                return min;
            }
        }

        throw new NumberFormatException("Zero length min field");
    }


    // returns true if this could be number if the user types more
    private boolean inCompleteNumber(String number) {
        if (number == null) {
            return true;
        }

        number = number.trim();

        if (number.length() == 0 || number.equals("-")) {
            return true;
        }

        return false;
    }


    private void handleMaxTextfield() {
        if (maxTextFieldListenerEnabled[0] && !basicSwitcherIsActive[0]) {
            maxTextFieldListenerEnabled[0] = false;
            if (!inCompleteNumber(maxField.getText())) {
                applyChanges(RangeKey.FromMaxField);
            }
            maxTextFieldListenerEnabled[0] = true;
        }
    }

    private void handleMinTextfield() {
        if (minTextFieldListenerEnabled[0] && !basicSwitcherIsActive[0]) {
            ColorManipulationDefaults.debug("Inside handleMinTextfield 1");

            minTextFieldListenerEnabled[0] = false;
            if (!inCompleteNumber(minField.getText())) {
                ColorManipulationDefaults.debug("Iside handleMinTextfield 2");

                applyChanges(RangeKey.FromMinField);
            }
            minTextFieldListenerEnabled[0] = true;
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

        discreteCheckBox.setDiscreteColorsMode(discrete);

        if (logScaled != logDisplayButton.isSelected()) {
            logDisplayButton.setSelected(logScaled);
        }

        PropertyMap configuration = parentForm.getFormModel().getProductSceneView().getSceneImage().getConfiguration();

        boolean schemeApply = configuration.getPropertyBool(PROPERTY_SCHEME_AUTO_APPLY_KEY, PROPERTY_SCHEME_AUTO_APPLY_DEFAULT);
        String schemeLogScaling = configuration.getPropertyString(PROPERTY_SCHEME_LOG_KEY, PROPERTY_SCHEME_LOG_DEFAULT);
        String schemeRange = configuration.getPropertyString(PROPERTY_SCHEME_RANGE_KEY, PROPERTY_SCHEME_RANGE_DEFAULT);
        String schemeCpd = configuration.getPropertyString(PROPERTY_SCHEME_PALETTE_KEY, PROPERTY_SCHEME_PALETTE_DEFAULT);


        schemeInfoLabel.setText("<html>*Modified scheme");
        schemeInfoLabel.setToolTipText("Not using exact scheme default: see preferences");

        boolean visible = false;


        ColorSchemeManager colorPaletteSchemes = ColorSchemeManager.getDefault();
        colorPaletteSchemes.setSelected(imageInfo.getColorSchemeInfo());

        if (colorPaletteSchemes.isSchemeSet() &&
                schemeApply &&
                (!PROPERTY_SCHEME_PALETTE_DEFAULT.equals(schemeCpd) ||
                        !PROPERTY_SCHEME_RANGE_DEFAULT.equals(schemeRange) ||
                        !PROPERTY_SCHEME_LOG_DEFAULT.equals(schemeLogScaling))
        ) {
            visible = true;
        }
        schemeInfoLabel.setVisible(visible);


        colorPaletteSchemes.checkPreferences(configuration);


        parentForm.revalidateToolViewPaneControl();


        if (cpd.getMinDisplaySample() != (Double) minField.getValue()) {
            minField.setValue(cpd.getMinDisplaySample());
        }

        if (cpd.getMaxDisplaySample() != (Double) maxField.getValue()) {
            maxField.setValue(cpd.getMaxDisplaySample());
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


    private boolean testLogScalingAgainstCurrentMinMax(boolean logScaled) {

        boolean valid = true;

        if (logScaled) {
            final ImageInfo currentInfo = parentForm.getFormModel().getModifiedImageInfo();
            double min = currentInfo.getColorPaletteDef().getMinDisplaySample();
            double max = currentInfo.getColorPaletteDef().getMaxDisplaySample();

            if (min <= 0) {
                valid = false;
                ColorUtils.showErrorDialog("ERROR!: " + TOOLNAME_COLOR_MANIPULATION + ": \nMin field cannot be less than or equal to zero with log scaling\"");
            } else if (max <= 0) {
                valid = false;
                ColorUtils.showErrorDialog("ERROR!: " + TOOLNAME_COLOR_MANIPULATION + ": \nMax field cannot be less than or equal to zero with log scaling\"");
            }
        }

        return valid;
    }


    private void applyChanges(RangeKey key) {
        if (shouldFireChooserEvent) {
            // The 'valid' variable is used to determine whether the component entries are valid.  For the cases
            // where the component entry is not valid, no update will occur, the user will be prompted, and the
            // offending component will be reset to the last valid value
            boolean valid = true;

            // The 'valueChange' variable is used when the GUI components match the current image info and hence no
            // change is needed.  Excess events are undesirable here as the scheme selector will reset to "none"
            // for any component change event.
            boolean valueChange = true;


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

            // Set 'targetAutoDistribute' = true as default because only the case of loading a new palette
            // with exact values will need false
            boolean targetAutoDistribute = true;


            // Set any target values which will be changed from the source values
            switch (key) {
                case FromCpdFile:
                    // Updates targetMin and targetMax
                    targetMin = currentCPD.getSourceFileMin();
                    targetMax = currentCPD.getSourceFileMax();
                    valid = testMinMaxAgainstCurrentLog(targetMin, targetMax);

                    if (valid) {
                        if (targetMin == currentCPD.getMinDisplaySample() ||
                                targetMax == currentCPD.getMaxDisplaySample()) {
                            valueChange = false;
                        }
                    }
                    break;

                case FromData:
                    // Updates targetMin and targetMax
                    final Stx stx = parentForm.getStx(parentForm.getFormModel().getRaster());

                    if (stx != null) {
                        targetMin = stx.getMinimum();
                        targetMax = stx.getMaximum();
                        valid = testMinMaxAgainstCurrentLog(targetMin, targetMax);

                        if (valid) {
                            if (targetMin == currentCPD.getMinDisplaySample() ||
                                   targetMax == currentCPD.getMaxDisplaySample()) {
                                valueChange = false;
                            }
                        }
                    } else {
                        valid = false;
                    }
                    break;

                case FromMinField:
                    // Updates targetMin
                    try {
                        targetMin = getMinValueWithTesting();

                        if (targetMin == currentCPD.getMinDisplaySample()) {
                            valueChange = false;
                        }
                    } catch (NumberFormatException e) {
                        valid = false;
                        // restore to a valid setting
                        shouldFireChooserEvent = false;
                        minField.setValue(currentInfo.getColorPaletteDef().getMinDisplaySample());
                        shouldFireChooserEvent = true;
                    }

                    break;

                case FromMaxField:
                    // Updates targetMax
                    try {
                        targetMax = getMaxValueWithTesting();

                        if (targetMax == currentCPD.getMaxDisplaySample()) {
                            valueChange = false;
                        }
                    } catch (NumberFormatException e) {
                        valid = false;
                        // restore to a valid setting
                        shouldFireChooserEvent = false;
                        maxField.setValue(currentInfo.getColorPaletteDef().getMaxDisplaySample());
                        shouldFireChooserEvent = true;
                    }

                    break;

                case FromLogButton:
                    // Updates targetLogScaled
                    if (logDisplayButton.isSelected()) {
                        valid = testLogScalingAgainstCurrentMinMax(logDisplayButton.isSelected());
                        if (valid) {
                            colorPaletteChooser.setLog10Display(targetLogScaled);
                        } else {
                            // restore to a valid setting
                            shouldFireChooserEvent = false;
                            logDisplayButton.setSelected(false);
                            shouldFireChooserEvent = true;
                        }
                    }
                    targetLogScaled = logDisplayButton.isSelected();

                    if (targetLogScaled == currentInfo.isLogScaled()) {
                        valueChange = false;
                    }
                    break;

                case InvertPalette:
                    // This case is dealt with after the switch statement
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
                    }
                    break;

                default:
                    // this will not be reached
            }


            if (valid && valueChange) {
                ColorManipulationDefaults.debug("Is valid");

                if (key == RangeKey.InvertPalette) {
                    currentInfo.setColorPaletteDefInvert(sourceCpd);
                } else {
                    currentInfo.setColorPaletteDef(sourceCpd, targetMin, targetMax, targetAutoDistribute, sourceLogScaled, targetLogScaled);
                }

                colorSchemeManager.reset();
                currentInfo.setColorSchemeInfo(colorSchemeManager.getNoneColorSchemeInfo());

                parentForm.applyChanges();
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
        } catch (Exception e) {
            JOptionPane.showMessageDialog(getContentPanel(),
                    "Failed to create default image settings:\n" + e.getMessage(),
                    "I/O Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }


    private void handleColorSchemeSelector() {
        ColorSchemeInfo colorSchemeInfo = (ColorSchemeInfo) colorSchemeManager.getjComboBox().getSelectedItem();

        if (colorSchemeInfo != null) {
            if (colorSchemeInfo.isEnabled() && !colorSchemeInfo.isDivider()) {
                if (colorSchemeManager.isNoneScheme(colorSchemeInfo)) {
                    ColorSchemeUtils.setImageInfoToGeneralColor(configuration, createDefaultImageInfo(), parentForm.getFormModel().getProductSceneView());
                } else {
                    ColorSchemeUtils.setImageInfoToColorScheme(colorSchemeInfo, parentForm.getFormModel().getProductSceneView());
                }
                parentForm.getFormModel().setModifiedImageInfo(parentForm.getFormModel().getProductSceneView().getImageInfo());

            } else {
                if (!colorSchemeInfo.isDivider()) {
                    ColorManipulationDefaults.debug("Add a notification message window");
                    String message = colorSchemeManager.checkScheme(colorSchemeInfo);
                    ColorManipulationDefaults.debug(message);

                    ColorUtils.showErrorDialog(message);
                }
            }
        }
        parentForm.applyChanges();
    }

}
