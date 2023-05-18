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

package org.esa.snap.core.gpf.ui.reproject;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.ui.CollocationCrsForm;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.core.gpf.ui.TargetProductSelector;
import org.esa.snap.core.gpf.ui.TargetProductSelectorModel;
import org.esa.snap.core.param.ParamParseException;
import org.esa.snap.core.param.ParamValidateException;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.DemSelector;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.crs.CrsForm;
import org.esa.snap.ui.crs.CrsSelectionPanel;
import org.esa.snap.ui.crs.CustomCrsForm;
import org.esa.snap.ui.crs.OutputGeometryForm;
import org.esa.snap.ui.crs.OutputGeometryFormModel;
import org.esa.snap.ui.crs.PredefinedCrsForm;
import org.esa.snap.ui.product.ProductExpressionPane;
import org.geotools.referencing.AbstractReferenceSystem;
import org.geotools.referencing.CRS;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Projection;

import javax.swing.*;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import static org.esa.snap.rcp.preferences.general.ReprojectionController.*;

/**
 * @author Marco Zuehlke
 * @author Marco Peters
 * @since BEAM 4.7
 */
class ReprojectionForm extends JTabbedPane {

    private static final String[] RESAMPLING_IDENTIFIER = {"Nearest", "Bilinear", "Bicubic"};

    private final boolean orthoMode;
    private final String targetProductSuffix;
    private final AppContext appContext;
    private final SourceProductSelector sourceProductSelector;
    private final TargetProductSelector targetProductSelector;
    private final Model reprojectionModel;
    private final PropertyContainer reprojectionContainer;

    private DemSelector demSelector;
    private CrsSelectionPanel crsSelectionPanel;

    private OutputGeometryFormModel outputGeometryModel;

    private JButton outputParamButton;
    private InfoForm infoForm;
    private CoordinateReferenceSystem crs;
    private CollocationCrsForm collocationCrsUI;
    private CustomCrsForm customCrsUI;

    private boolean applyValidPixelExpression;
    private JCheckBox applyValidPixelExpressionCheckBox;



    private JPanel maskExpressionPanel;

    private JButton editExpressionButton;
    private JTextArea expressionArea;
    private JTabbedPane t = this;

    ReprojectionForm(TargetProductSelector targetProductSelector, boolean orthorectify, AppContext appContext) {
        this.targetProductSelector = targetProductSelector;
        this.orthoMode = orthorectify;
        this.appContext = appContext;
        this.sourceProductSelector = new SourceProductSelector(appContext, "Source Product:");
        if (orthoMode) {
            targetProductSuffix = "orthorectified";
            this.sourceProductSelector.setProductFilter(new OrthorectifyProductFilter());
        } else {
            targetProductSuffix = "reprojected";
            this.sourceProductSelector.setProductFilter(new GeoCodingProductFilter());
        }
        this.reprojectionModel = new Model();
        this.reprojectionContainer = PropertyContainer.createObjectBacked(reprojectionModel);
        createUI();
    }



    void updateParameterMap(Map<String, Object> parameterMap) {
        parameterMap.clear();
        parameterMap.put("resamplingName", reprojectionModel.resamplingName);
        parameterMap.put("includeTiePointGrids", reprojectionModel.includeTiePointGrids);
        parameterMap.put("addDeltaBands", reprojectionModel.addDeltaBands);
        parameterMap.put("noDataValue", reprojectionModel.noDataValue);
        parameterMap.put("retainValidPixelExpression", reprojectionModel.retainValidPixelExpression);
        if (!collocationCrsUI.getRadioButton().isSelected()) {
            CoordinateReferenceSystem selectedCrs = getSelectedCrs();
            if (selectedCrs != null) {
                if(selectedCrs instanceof AbstractReferenceSystem) {
                    // Sometimes it can happen that strict mode fails. But the WKT is still valid and usable. Strict mode is anabled by default.
                    parameterMap.put("crs", ((AbstractReferenceSystem)selectedCrs).toWKT(2, false));
                }else {
                    parameterMap.put("crs", selectedCrs.toWKT());
                }
            }
        }
        if (orthoMode) {
            parameterMap.put("orthorectify", orthoMode);
            if (demSelector.isUsingExternalDem()) {
                parameterMap.put("elevationModelName", demSelector.getDemName());
            } else {
                parameterMap.put("elevationModelName", null);
            }
        }

        if (!reprojectionModel.preserveResolution && outputGeometryModel != null) {
            PropertySet container = outputGeometryModel.getPropertySet();
            parameterMap.put("referencePixelX", container.getValue("referencePixelX"));
            parameterMap.put("referencePixelY", container.getValue("referencePixelY"));
            parameterMap.put("easting", container.getValue("easting"));
            parameterMap.put("northing", container.getValue("northing"));
            parameterMap.put("orientation", container.getValue("orientation"));
            parameterMap.put("pixelSizeX", container.getValue("pixelSizeX"));
            parameterMap.put("pixelSizeY", container.getValue("pixelSizeY"));
            parameterMap.put("width", container.getValue("width"));
            parameterMap.put("height", container.getValue("height"));
        }

        applyValidPixelExpression = applyValidPixelExpressionCheckBox.isSelected();
        parameterMap.put("applyValidPixelExpression", applyValidPixelExpression);


        if (expressionArea.getText() != null) {
            parameterMap.put("maskExpression", expressionArea.getText());
        }
    }

    public void updateFormModel(Map<String, Object> parameterMap) throws ValidationException, ConversionException {
        Property[] properties = reprojectionContainer.getProperties();
        for (Property property : properties) {
            String propertyName = property.getName();
            Object newValue = parameterMap.get(propertyName);
            if (newValue != null) {
                property.setValue(newValue);
            }
        }
        if (orthoMode) {
            Object elevationModelName = parameterMap.get("elevationModelName");
            if (elevationModelName instanceof String) {
                try {
                    demSelector.setDemName((String) elevationModelName);
                } catch (ParamValidateException e) {
                    throw new ValidationException(e.getMessage(), e);
                } catch (ParamParseException e) {
                    throw new ConversionException(e.getMessage(), e);
                }
            }
        }
        Object crsAsWKT = parameterMap.get("crs");
        if (crsAsWKT instanceof String) {
            try {
                CoordinateReferenceSystem crs;
                crs = CRS.parseWKT((String) crsAsWKT);
                if (crs instanceof ProjectedCRS) {
                    ProjectedCRS projectedCRS = (ProjectedCRS) crs;
                    Projection conversionFromBase = projectedCRS.getConversionFromBase();
                    OperationMethod operationMethod = conversionFromBase.getMethod();
                    ParameterValueGroup parameterValues = conversionFromBase.getParameterValues();
                    GeodeticDatum geodeticDatum = projectedCRS.getDatum();
                    customCrsUI.setCustom(geodeticDatum, operationMethod, parameterValues);
                } else {
                    throw new ConversionException("Failed to convert CRS from WKT.");
                }
            } catch (FactoryException e) {
                throw new ConversionException("Failed to convert CRS from WKT.", e);
            }

        }
        if (parameterMap.containsKey("referencePixelX")) {
            PropertyContainer propertySet = PropertyContainer.createMapBacked(parameterMap);
            outputGeometryModel = new OutputGeometryFormModel(propertySet);
            reprojectionContainer.setValue(Model.PRESERVE_RESOLUTION, false);
        } else {
            outputGeometryModel = null;
            reprojectionContainer.setValue(Model.PRESERVE_RESOLUTION, true);
        }
        updateCRS();
    }

    Map<String, Product> getProductMap() {
        final Map<String, Product> productMap = new HashMap<>(5);
        productMap.put("source", getSourceProduct());
        if (collocationCrsUI.getRadioButton().isSelected()) {
            productMap.put("collocateWith", collocationCrsUI.getCollocationProduct());
        }
        return productMap;
    }

    Product getSourceProduct() {
        return sourceProductSelector.getSelectedProduct();
    }

    CoordinateReferenceSystem getSelectedCrs() {
        return crs;
    }

    void prepareShow() {
        sourceProductSelector.initProducts();
        crsSelectionPanel.prepareShow();
    }

    void prepareHide() {
        sourceProductSelector.releaseProducts();
        crsSelectionPanel.prepareHide();
        if (outputGeometryModel != null) {
            outputGeometryModel.setSourceProduct(null);
        }
    }

    String getExternalDemName() {
        if (orthoMode && demSelector.isUsingExternalDem()) {
            return demSelector.getDemName();
        }
        return null;
    }

    private void createUI() {
        addTab("I/O Parameters", createIOPanel());
        addTab("Reprojection Parameters", createParametersPanel());
//        addTab("Validation Masking", createMaskPanel());

    }

    private JPanel createIOPanel() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(3, 3);

        final JPanel ioPanel = new JPanel(tableLayout);
        ioPanel.add(createSourceProductPanel());
        ioPanel.add(targetProductSelector.createDefaultPanel());
        ioPanel.add(tableLayout.createVerticalSpacer());
        return ioPanel;
    }

    private JPanel createParametersPanel() {
        final JPanel parameterPanel = new JPanel();
        final TableLayout layout = new TableLayout(1);
        layout.setTablePadding(4, 4);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableWeightX(1.0);
        parameterPanel.setLayout(layout);
        customCrsUI = new CustomCrsForm(appContext);
        CrsForm predefinedCrsUI = new PredefinedCrsForm(appContext);
        collocationCrsUI = new CollocationCrsForm(appContext);
        CrsForm[] crsForms = new CrsForm[]{customCrsUI, predefinedCrsUI, collocationCrsUI};
        crsSelectionPanel = new CrsSelectionPanel(crsForms);
        sourceProductSelector.addSelectionChangeListener(new AbstractSelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                final Product product = (Product) event.getSelection().getSelectedValue();
                crsSelectionPanel.setReferenceProduct(product);
            }
        });

        parameterPanel.add(crsSelectionPanel);
        if (orthoMode) {
            demSelector = new DemSelector();
            parameterPanel.add(demSelector);
        }

        parameterPanel.add(createMaskSettingsPanel());


        parameterPanel.add(createOutputSettingsPanel());


        infoForm = new InfoForm();
        parameterPanel.add(infoForm.createUI());



        crsSelectionPanel.addPropertyChangeListener("crs", evt -> updateCRS());
        updateCRS();
        return parameterPanel;
    }




    private void updateCRS() {
        final Product sourceProduct = getSourceProduct();
        try {
            if (sourceProduct != null) {
                crs = crsSelectionPanel.getCrs(ProductUtils.getCenterGeoPos(sourceProduct));
                if (crs != null) {
                    infoForm.setCrsInfoText(crs.getName().getCode(), crs.toString());
                } else {
                    infoForm.setCrsErrorText("No valid 'Coordinate Reference System' selected.");
                }
            } else {
                infoForm.setCrsErrorText("No source product selected.");
                crs = null;
            }
        } catch (FactoryException e) {
            infoForm.setCrsErrorText(e.getMessage());
            crs = null;
        }
        if (outputGeometryModel != null) {
            outputGeometryModel.setTargetCrs(crs);
        }
        updateOutputParameterState();
    }

    private void updateProductSize() {
        int width = 0;
        int height = 0;
        final Product sourceProduct = getSourceProduct();
        if (sourceProduct != null && crs != null) {
            if (!reprojectionModel.preserveResolution && outputGeometryModel != null) {
                PropertySet container = outputGeometryModel.getPropertySet();
                width = container.getValue("width");
                height = container.getValue("height");
            } else {
                ImageGeometry iGeometry;
                final Product collocationProduct = collocationCrsUI.getCollocationProduct();
                if (collocationCrsUI.getRadioButton().isSelected() && collocationProduct != null) {
                    iGeometry = ImageGeometry.createCollocationTargetGeometry(sourceProduct, collocationProduct);
                } else {
                    iGeometry = ImageGeometry.createTargetGeometry(sourceProduct, crs,
                            null, null, null, null,
                            null, null, null, null,
                            null);

                }
                Rectangle imageRect = iGeometry.getImageRect();
                width = imageRect.width;
                height = imageRect.height;
            }
        }
        infoForm.setWidth(width);
        infoForm.setHeight(height);
    }

    private class InfoForm {

        private JLabel widthLabel;
        private JLabel heightLabel;
        private JLabel centerLatLabel;
        private JLabel centerLonLabel;
        private JLabel crsLabel;
        private String wkt;
        private JButton wktButton;

        void setWidth(int width) {
            widthLabel.setText(Integer.toString(width));
        }

        void setHeight(int height) {
            heightLabel.setText(Integer.toString(height));
        }

        void setCenterPos(GeoPos geoPos) {
            if (geoPos != null) {
                centerLatLabel.setText(geoPos.getLatString());
                centerLonLabel.setText(geoPos.getLonString());
            } else {
                centerLatLabel.setText("");
                centerLonLabel.setText("");
            }
        }

        void setCrsErrorText(String infoText) {
            setCrsInfoText("<html><b>" + infoText + "</b>", null);
        }

        void setCrsInfoText(String infoText, String wkt) {
            this.wkt = wkt;
            crsLabel.setText(infoText);
            boolean hasWKT = (wkt != null);
            wktButton.setEnabled(hasWKT);
        }

        JPanel createUI() {
            widthLabel = new JLabel();
            heightLabel = new JLabel();
            centerLatLabel = new JLabel();
            centerLonLabel = new JLabel();
            crsLabel = new JLabel();

            final TableLayout tableLayout = new TableLayout(5);
            tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
            tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
            tableLayout.setTablePadding(4, 4);
            tableLayout.setColumnWeightX(0, 0.0);
            tableLayout.setColumnWeightX(1, 0.0);
            tableLayout.setColumnWeightX(2, 1.0);
            tableLayout.setColumnWeightX(3, 0.0);
            tableLayout.setColumnWeightX(4, 1.0);
            tableLayout.setCellColspan(2, 1, 3);
            tableLayout.setCellPadding(0, 3, new Insets(4, 24, 4, 20));
            tableLayout.setCellPadding(1, 3, new Insets(4, 24, 4, 20));


            final JPanel panel = new JPanel(tableLayout);
            panel.setBorder(BorderFactory.createTitledBorder("Output Information"));
            panel.add(new JLabel("Scene width:"));
            panel.add(widthLabel);
            panel.add(new JLabel("pixel"));
            panel.add(new JLabel("Center longitude:"));
            panel.add(centerLonLabel);

            panel.add(new JLabel("Scene height:"));
            panel.add(heightLabel);
            panel.add(new JLabel("pixel"));
            panel.add(new JLabel("Center latitude:"));
            panel.add(centerLatLabel);

            panel.add(new JLabel("CRS:"));
            panel.add(crsLabel);
            wktButton = new JButton("Show WKT");
            wktButton.addActionListener(e -> {
                JTextArea wktArea = new JTextArea(30, 40);
                wktArea.setEditable(false);
                wktArea.setText(wkt);
                final JScrollPane scrollPane = new JScrollPane(wktArea);
                final ModalDialog dialog = new ModalDialog(appContext.getApplicationWindow(),
                        "Coordinate reference system as well known text",
                        scrollPane,
                        ModalDialog.ID_OK, null);
                dialog.show();
            });
            wktButton.setEnabled(false);
            panel.add(wktButton);
            return panel;
        }
    }

    private JPanel createOutputSettingsPanel() {

        Preferences preferences = SnapApp.getDefault().getPreferences();


        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);


        final JPanel outputSettingsPanel = new JPanel(tableLayout);
        outputSettingsPanel.setBorder(BorderFactory.createTitledBorder(PROPERTY_OUTPUT_SETTINGS_SECTION_LABEL));

        final BindingContext context = new BindingContext(reprojectionContainer);



        final TableLayout resolutionTableLayout = new TableLayout(2);
        resolutionTableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        resolutionTableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        resolutionTableLayout.setColumnFill(0, TableLayout.Fill.NONE);
        resolutionTableLayout.setTablePadding(4, 4);

        final JPanel resolutionPanel = new JPanel(resolutionTableLayout);
        resolutionPanel.setBorder(BorderFactory.createTitledBorder("Resolution"));

        final JCheckBox preserveResolutionCheckBox = new JCheckBox(PROPERTY_PRESERVE_RESOLUTION_LABEL);
        preserveResolutionCheckBox.setToolTipText(PROPERTY_PRESERVE_RESOLUTION_TOOLTIP);
        context.bind(Model.PRESERVE_RESOLUTION, preserveResolutionCheckBox);
        collocationCrsUI.getCrsUI().addPropertyChangeListener("collocate", evt -> {
            final boolean collocate = (Boolean) evt.getNewValue();
            reprojectionContainer.setValue(Model.PRESERVE_RESOLUTION,
                    collocate || reprojectionModel.preserveResolution);
            preserveResolutionCheckBox.setEnabled(!collocate);
        });
        resolutionPanel.add(preserveResolutionCheckBox);

        preserveResolutionCheckBox.addActionListener(e -> {
            if (preserveResolutionCheckBox.isSelected()) {
                outputParamButton.setEnabled(false);
            } else {
                outputParamButton.setEnabled(true);
            }

        });

        outputParamButton = new JButton(PROPERTY_RESOLUTION_PARAMETERS_BUTTON_NAME);
        outputParamButton.setEnabled(!reprojectionModel.preserveResolution);
        outputParamButton.addActionListener(new OutputParamActionListener());
        resolutionPanel.add(outputParamButton);


        outputSettingsPanel.add(resolutionPanel);


        // Resampling Method Component
        final TableLayout resamplingTableLayout = new TableLayout(2);
        resamplingTableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        resamplingTableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        resamplingTableLayout.setColumnFill(0, TableLayout.Fill.NONE);
        resamplingTableLayout.setTablePadding(4, 4);
        final JPanel resamplingPanel = new JPanel(resamplingTableLayout);

        JLabel resamplingMethodLabel = new JLabel(PROPERTY_RESAMPLING_METHOD_LABEL);
        String resamplingMethodPreference = preferences.get(PROPERTY_RESAMPLING_METHOD_KEY, PROPERTY_RESAMPLING_METHOD_DEFAULT);
        JComboBox<String> resampleComboBox = new JComboBox<>(PROPERTY_RESAMPLING_METHOD_OPTIONS);
        resampleComboBox.setPrototypeDisplayValue(resamplingMethodPreference);
        resampleComboBox.setSelectedItem(resamplingMethodPreference);
        resamplingMethodLabel.setToolTipText(PROPERTY_RESAMPLING_METHOD_TOOLTIP);
        resampleComboBox.setToolTipText(PROPERTY_RESAMPLING_METHOD_TOOLTIP);
        context.bind(Model.RESAMPLING_NAME, resampleComboBox);
        resamplingPanel.add(resamplingMethodLabel);
        resamplingPanel.add(resampleComboBox);

        outputSettingsPanel.add(resamplingPanel);





        // No-Data Component
        final TableLayout noDataTableLayout = new TableLayout(2);
        noDataTableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        noDataTableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        noDataTableLayout.setColumnFill(0, TableLayout.Fill.NONE);
        noDataTableLayout.setColumnFill(1, TableLayout.Fill.NONE);
        noDataTableLayout.setColumnAnchor(0, TableLayout.Anchor.EAST);
        noDataTableLayout.setColumnAnchor(1, TableLayout.Anchor.WEST);
        noDataTableLayout.setCellWeightX(0,0,0.0);
        noDataTableLayout.setCellWeightX(0,1,1.0);

        noDataTableLayout.setTablePadding(4, 4);
        final JPanel noDataPanel = new JPanel(noDataTableLayout);

        JLabel noDataLabel = new JLabel(PROPERTY_NO_DATA_VALUE_LABEL);
        noDataLabel.setToolTipText(PROPERTY_NO_DATA_VALUE_TOOLTIP);
        final JTextField noDataField = new JTextField("12345678");
        noDataField.setMinimumSize(noDataField.getPreferredSize());
        noDataField.setPreferredSize(noDataField.getPreferredSize());
        noDataField.setToolTipText(PROPERTY_NO_DATA_VALUE_TOOLTIP);
        context.bind(Model.NO_DATA_VALUE, noDataField);
        noDataPanel.add(noDataLabel);
        noDataPanel.add(noDataField);

        outputSettingsPanel.add(noDataPanel);



        JCheckBox retainValidPixelExpressionCheckBox = new JCheckBox(PROPERTY_RETAIN_VALID_PIXEL_EXPRESSION_LABEL);
        retainValidPixelExpressionCheckBox.setToolTipText(PROPERTY_RETAIN_VALID_PIXEL_EXPRESSION_TOOLTIP);
        outputSettingsPanel.add(retainValidPixelExpressionCheckBox);
        context.bind(Model.RETAIN_VALID_PIXEL_EXPRESSION, retainValidPixelExpressionCheckBox);



        // Tie-Point Grids
        JCheckBox includeTPcheck = new JCheckBox(PROPERTY_INCLUDE_TIE_POINT_GRIDS_LABEL);
        includeTPcheck.setToolTipText(PROPERTY_INCLUDE_TIE_POINT_GRIDS_TOOLTIP);
        context.bind(Model.REPROJ_TIEPOINTS, includeTPcheck);
        outputSettingsPanel.add(includeTPcheck);


        // Add Delta Bands Component
        JCheckBox addDeltaBandsChecker = new JCheckBox(PROPERTY_ADD_DELTA_BANDS_LABEL);
        addDeltaBandsChecker.setToolTipText(PROPERTY_ADD_DELTA_BANDS_TOOLTIP);
        outputSettingsPanel.add(addDeltaBandsChecker);
        context.bind(Model.ADD_DELTA_BANDS, addDeltaBandsChecker);




        reprojectionContainer.addPropertyChangeListener(Model.PRESERVE_RESOLUTION, evt -> updateOutputParameterState());

        return outputSettingsPanel;
    }

    private void updateOutputParameterState() {
        outputParamButton.setEnabled(!reprojectionModel.preserveResolution && (crs != null));
        updateProductSize();
    }

    private JPanel createMaskSettingsPanel() {
        Preferences preferences = SnapApp.getDefault().getPreferences();

        final TableLayout maskExpressionLayout = new TableLayout(2);
        maskExpressionLayout.setTablePadding(4, 0);
        maskExpressionLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        maskExpressionLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        maskExpressionLayout.setTableWeightX(1.0);

        maskExpressionPanel = new JPanel(maskExpressionLayout);

        editExpressionButton = new JButton(PROPERTY_MASK_EXPRESSION_BUTTON_NAME);
        editExpressionButton.setPreferredSize(editExpressionButton.getPreferredSize());
        editExpressionButton.setMaximumSize(editExpressionButton.getPreferredSize());
        editExpressionButton.setMinimumSize(editExpressionButton.getPreferredSize());
        final Window parentWindow = SwingUtilities.getWindowAncestor(maskExpressionPanel);
        editExpressionButton.addActionListener(new EditExpressionActionListener(parentWindow));
        expressionArea = new JTextArea(3, 40);
        expressionArea.setLineWrap(true);

        JLabel maskExpressionLabel = new JLabel(PROPERTY_MASK_EXPRESSION_LABEL);
        maskExpressionPanel.add(maskExpressionLabel);
        maskExpressionPanel.add(new JScrollPane(expressionArea));

        maskExpressionPanel.setToolTipText(PROPERTY_MASK_EXPRESSION_TOOLTIP);
        maskExpressionLabel.setToolTipText(PROPERTY_MASK_EXPRESSION_TOOLTIP);
        editExpressionButton.setToolTipText(PROPERTY_MASK_EXPRESSION_TOOLTIP);
        expressionArea.setToolTipText(PROPERTY_MASK_EXPRESSION_TOOLTIP);

        String maskExpressionText = preferences.get(PROPERTY_MASK_EXPRESSION_KEY, PROPERTY_MASK_EXPRESSION_DEFAULT);
        expressionArea.setText(maskExpressionText);

        boolean applyValidPixelExpressionPreference = preferences.getBoolean(PROPERTY_APPLY_VALID_PIXEL_EXPRESSION_KEY, PROPERTY_APPLY_VALID_PIXEL_EXPRESSION_DEFAULT);
        applyValidPixelExpressionCheckBox = new JCheckBox(PROPERTY_APPLY_VALID_PIXEL_EXPRESSION_LABEL);
        applyValidPixelExpressionCheckBox.setToolTipText(PROPERTY_APPLY_VALID_PIXEL_EXPRESSION_TOOLTIP);
        applyValidPixelExpressionCheckBox.setSelected(applyValidPixelExpressionPreference);

        final TableLayout secondRowLayout = new TableLayout(3);
        secondRowLayout.setTablePadding(4, 0);
        secondRowLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        secondRowLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        secondRowLayout.setTableWeightX(1.0);

        final JPanel secondRowPanel = new JPanel(secondRowLayout);
        secondRowPanel.add(applyValidPixelExpressionCheckBox);

        secondRowPanel.add(secondRowLayout.createHorizontalSpacer());

        secondRowLayout.setTableAnchor(TableLayout.Anchor.NORTHEAST);
        secondRowPanel.setLayout(secondRowLayout);
        secondRowPanel.add(editExpressionButton);

        final TableLayout layout = new TableLayout(1);
        layout.setTablePadding(4, 4);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTableWeightX(1.0);

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder(PROPERTY_MASKING_SECTION_LABEL));
        panel.add(maskExpressionPanel);
        panel.add(secondRowPanel);

        return panel;
    }



    private JPanel createSourceProductPanel() {
        final JPanel panel = sourceProductSelector.createDefaultPanel();
        sourceProductSelector.getProductNameLabel().setText("Name:");
        sourceProductSelector.getProductNameComboBox().setPrototypeDisplayValue(
                "MER_RR__1PPBCM20030730_071000_000003972018_00321_07389_0000.N1");
        sourceProductSelector.addSelectionChangeListener(new AbstractSelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                final Product sourceProduct = getSourceProduct();
                updateTargetProductName(sourceProduct);
                GeoPos centerGeoPos = null;
                if (sourceProduct != null) {
                    centerGeoPos = ProductUtils.getCenterGeoPos(sourceProduct);
                }
                infoForm.setCenterPos(centerGeoPos);
                if (outputGeometryModel != null) {
                    outputGeometryModel.setSourceProduct(sourceProduct);
                }
                updateCRS();
            }
        });
        return panel;
    }

    private void updateTargetProductName(Product selectedProduct) {
        final TargetProductSelectorModel selectorModel = targetProductSelector.getModel();
        if (selectedProduct != null) {
            final String productName = MessageFormat.format("{0}_" + targetProductSuffix, selectedProduct.getName());
            selectorModel.setProductName(productName);
        } else if (selectorModel.getProductName() == null) {
            selectorModel.setProductName(targetProductSuffix);
        }
    }

    private class OutputParamActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            try {
                final Product sourceProduct = getSourceProduct();
                if (sourceProduct == null) {
                    showWarningMessage("Please select a product to reproject.\n");
                    return;
                }
                if (crs == null) {
                    showWarningMessage("Please specify a 'Coordinate Reference System' first.\n");
                    return;
                }
                OutputGeometryFormModel workCopy;
                if (outputGeometryModel != null) {
                    workCopy = new OutputGeometryFormModel(outputGeometryModel);
                } else {
                    final Product collocationProduct = collocationCrsUI.getCollocationProduct();
                    if (collocationCrsUI.getRadioButton().isSelected() && collocationProduct != null) {
                        workCopy = new OutputGeometryFormModel(sourceProduct, collocationProduct);
                    } else {
                        workCopy = new OutputGeometryFormModel(sourceProduct, crs);
                    }
                }
                final OutputGeometryForm form = new OutputGeometryForm(workCopy);
                final ModalDialog outputParametersDialog = new OutputParametersDialog(appContext.getApplicationWindow(),
                        sourceProduct, workCopy);
                outputParametersDialog.setContent(form);
                if (outputParametersDialog.show() == ModalDialog.ID_OK) {
                    outputGeometryModel = workCopy;
                    updateProductSize();
                }
            } catch (Exception e) {
                appContext.handleError("Could not create a 'Coordinate Reference System'.\n" +
                        e.getMessage(), e);
            }
        }

    }

    private void showWarningMessage(String message) {
        AbstractDialog.showWarningDialog(getParent(), message, "Reprojection");
    }

    private class OutputParametersDialog extends ModalDialog {

        private static final String TITLE = "Output Parameters";

        private final Product sourceProduct;
        private final OutputGeometryFormModel outputGeometryFormModel;

        public OutputParametersDialog(Window parent, Product sourceProduct,
                                      OutputGeometryFormModel outputGeometryFormModel) {
            super(parent, TITLE, ModalDialog.ID_OK_CANCEL | ModalDialog.ID_RESET, null);
            this.sourceProduct = sourceProduct;
            this.outputGeometryFormModel = outputGeometryFormModel;
        }

        @Override
        protected void onReset() {
            final Product collocationProduct = collocationCrsUI.getCollocationProduct();
            ImageGeometry imageGeometry;
            if (collocationCrsUI.getRadioButton().isSelected() && collocationProduct != null) {
                imageGeometry = ImageGeometry.createCollocationTargetGeometry(sourceProduct, collocationProduct);
            } else {
                imageGeometry = ImageGeometry.createTargetGeometry(sourceProduct, crs,
                        null, null, null, null,
                        null, null, null, null, null);
            }
            outputGeometryFormModel.resetToDefaults(imageGeometry);
        }
    }

    private static class Model {

        private static final String PRESERVE_RESOLUTION = "preserveResolution";
        private static final String REPROJ_TIEPOINTS = "includeTiePointGrids";
        private static final String ADD_DELTA_BANDS = "addDeltaBands";
        private static final String NO_DATA_VALUE = "noDataValue";
        private static final String RESAMPLING_NAME = "resamplingName";
        private static final String RETAIN_VALID_PIXEL_EXPRESSION = "retainValidPixelExpression";


        Preferences preferences = SnapApp.getDefault().getPreferences();

        private boolean preserveResolution = preferences.getBoolean(PROPERTY_PRESERVE_RESOLUTION_KEY, PROPERTY_PRESERVE_RESOLUTION_DEFAULT);
        private boolean retainValidPixelExpression = preferences.getBoolean(PROPERTY_RETAIN_VALID_PIXEL_EXPRESSION_KEY, PROPERTY_RETAIN_VALID_PIXEL_EXPRESSION_DEFAULT);
        private boolean includeTiePointGrids = preferences.getBoolean(PROPERTY_INCLUDE_TIE_POINT_GRIDS_KEY, PROPERTY_INCLUDE_TIE_POINT_GRIDS_DEFAULT);
        private boolean addDeltaBands = preferences.getBoolean(PROPERTY_ADD_DELTA_BANDS_KEY, PROPERTY_ADD_DELTA_BANDS_DEFAULT);
        private double noDataValue = preferences.getDouble(PROPERTY_NO_DATA_VALUE_KEY, PROPERTY_NO_DATA_VALUE_DEFAULT);
        private String resamplingName = preferences.get(PROPERTY_RESAMPLING_METHOD_KEY, PROPERTY_RESAMPLING_METHOD_DEFAULT);;
    }


    private static class OrthorectifyProductFilter implements ProductFilter {

        @Override
        public boolean accept(Product product) {
            return product.canBeOrthorectified();
        }
    }

    private static class GeoCodingProductFilter implements ProductFilter {

        @Override
        public boolean accept(Product product) {
            final GeoCoding geoCoding = product.getSceneGeoCoding();
            return geoCoding != null && geoCoding.canGetGeoPos() && geoCoding.canGetPixelPos();
        }
    }

    public class EditExpressionActionListener implements ActionListener {

        private final Window parentWindow;

        private EditExpressionActionListener(Window parentWindow) {
            this.parentWindow = parentWindow;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ProductExpressionPane pep = ProductExpressionPane.createBooleanExpressionPane(new Product[]{getSourceProduct()},
                    getSourceProduct(),
                    appContext.getPreferences());
            pep.setCode(expressionArea.getText());
            final int i = pep.showModalDialog(parentWindow, "Mask Expression Editor");
            if (i == ModalDialog.ID_OK) {
                expressionArea.setText(pep.getCode());
            }

        }
    }

}