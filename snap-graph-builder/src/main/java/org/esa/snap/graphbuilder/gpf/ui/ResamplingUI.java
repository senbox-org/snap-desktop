/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.graphbuilder.gpf.ui;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.internal.ComboBoxAdapter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

/**
 * User interface for Resampling
 */
public class ResamplingUI extends BaseOperatorUI {

    private final String REFERENCE_BAND_TOOLTIP_TEXT = "<html>Set the reference band.<br/>" +
            "All other bands will be resampled to match its size and resolution.</html>";
    private final String TARGET_WIDTH_AND_HEIGHT_TOOLTIP_TEXT =
            "<html>Set explicitly the width and height of the resampled product.<br/>" +
                    "This option is only available when all bands have the same offset.</html>";
    private final String TARGET_RESOLUTION_TOOLTIP_TEXT = "<html>Define the target resolution of the resampled product.<br/>" +
            "This option is only available for products with a geocoding based on a cartographic map CRS.</html>";

    private final String UPSAMPLING_METHOD_PARAMETER_NAME = "upsamplingMethod";
    private final String DOWNSAMPLING_METHOD_PARAMETER_NAME = "downsamplingMethod";
    private final String FLAGDOWNSAMPLING_METHOD_PARAMETER_NAME = "flagDownsamplingMethod";
    private final String PYRAMID_LEVELS_PARAMETER_NAME = "resampleOnPyramidLevels";

    private ArrayList<String> listBands = new ArrayList();
    int lastProductWidth = 0;
    int lastProductHeight = 0;

    private JRadioButton referenceBandButton;
    private JRadioButton widthAndHeightButton;
    private JRadioButton resolutionButton;
    private ReferenceBandNameBoxPanel referenceBandNameBoxPanel;
    private TargetWidthAndHeightPanel targetWidthAndHeightPanel;
    private TargetResolutionPanel targetResolutionPanel;
    private JComboBox upsamplingCombo = new JComboBox();
    private JComboBox downsamplingCombo = new JComboBox();
    private JComboBox flagDownsamplingCombo = new JComboBox();
    private JCheckBox pyramidLevelCheckBox = new JCheckBox("Resample on pyramid levels (for faster imaging)");
    private BindingContext bindingContext;
    private OperatorDescriptor operatorDescriptor;
    private OperatorParameterSupport parameterSupport;

    private boolean updatingTargetWidthAndHeight = false;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalArgumentException("No SPI found for operator name '" + operatorName + "'");
        }

        operatorDescriptor = operatorSpi.getOperatorDescriptor();

        parameterSupport = new OperatorParameterSupport(operatorDescriptor);
        final PropertySet propertySet = parameterSupport.getPropertySet();
        bindingContext = new BindingContext(propertySet);


        if (sourceProducts != null) {
            for (Band band : sourceProducts[0].getBands()) {
                listBands.add(band.getName());
            }
        }

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();

        initParameters();

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        if (hasSourceProducts()) {
            reactToSourceProductChange(sourceProducts[0]);
            referenceBandButton.setEnabled(true);

            if(referenceBandButton.isSelected() && sourceProducts[0].getBand((String) referenceBandNameBoxPanel.referenceBandNameBox.getSelectedItem()) == null) {
                referenceBandButton.setSelected(false);
                referenceBandNameBoxPanel.setEnabled(false);
                widthAndHeightButton.setSelected(true);
                targetWidthAndHeightPanel.setEnabled(true);
            }

        } else {
            referenceBandNameBoxPanel.reactToSourceProductChange(null);
            if(referenceBandButton.isSelected()) {
                referenceBandButton.setSelected(false);
                referenceBandButton.setEnabled(false);
                referenceBandNameBoxPanel.setEnabled(false);
                widthAndHeightButton.setSelected(true);
                targetWidthAndHeightPanel.setEnabled(true);
            }
        }

        updateParameters();
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        //if we use always target width and height because this way, there are no errors when changing sources (for example, the name of the band could not be found)
        if(hasSourceProducts()) {
            if (referenceBandButton.isSelected()) {
                //paramMap.put("targetWidth", Integer.parseInt(referenceBandNameBoxPanel.referenceBandTargetWidthLabel.getText()));
                //paramMap.put("targetHeight", Integer.parseInt(referenceBandNameBoxPanel.referenceBandTargetHeightLabel.getText()));
                paramMap.put("referenceBandName", referenceBandNameBoxPanel.referenceBandNameBox.getSelectedItem().toString());
                paramMap.remove("targetResolution");
                paramMap.remove("targetWidth");
                paramMap.remove("targetHeight");
            } else if (widthAndHeightButton.isSelected()) {
                paramMap.put("targetWidth", targetWidthAndHeightPanel.widthSpinner.getValue());
                paramMap.put("targetHeight", targetWidthAndHeightPanel.heightSpinner.getValue());
                paramMap.remove("targetResolution");
                paramMap.remove("referenceBand");
                paramMap.remove("referenceBandName");
            } else {
                //paramMap.put("targetWidth", Integer.parseInt(targetResolutionPanel.targetResolutionTargetWidthLabel.getText()));
                //paramMap.put("targetHeight", Integer.parseInt(targetResolutionPanel.targetResolutionTargetHeightLabel.getText()));
                paramMap.put("targetResolution", targetResolutionPanel.resolutionSpinner.getValue());
                paramMap.remove("referenceBand");
                paramMap.remove("referenceBandName");
                paramMap.remove("targetWidth");
                paramMap.remove("targetHeight");
            }
        } else {
            paramMap.put("targetWidth", 100);
            paramMap.put("targetHeight", 100);
        }

        //paramMap.remove("targetResolution");
        //paramMap.remove("referenceBand");
        //paramMap.remove("referenceBandName");
        paramMap.put(UPSAMPLING_METHOD_PARAMETER_NAME, upsamplingCombo.getSelectedItem());
        paramMap.put(DOWNSAMPLING_METHOD_PARAMETER_NAME, downsamplingCombo.getSelectedItem());
        paramMap.put(FLAGDOWNSAMPLING_METHOD_PARAMETER_NAME,flagDownsamplingCombo.getSelectedItem());
        paramMap.put(PYRAMID_LEVELS_PARAMETER_NAME, pyramidLevelCheckBox.isSelected());

    }


    private JComponent createPanel() {
        final PropertySet propertySet = bindingContext.getPropertySet();

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTablePadding(4, 4);

        final GridLayout defineTargetResolutionPanelLayout = new GridLayout(3, 2);
        defineTargetResolutionPanelLayout.setVgap(4);
        final JPanel defineTargetSizePanel = new JPanel(defineTargetResolutionPanelLayout);
        defineTargetSizePanel.setBorder(BorderFactory.createTitledBorder("Define size of resampled product"));
        final ButtonGroup targetSizeButtonGroup = new ButtonGroup();
        referenceBandButton = new JRadioButton("By reference band from source product:");
        referenceBandButton.setToolTipText(REFERENCE_BAND_TOOLTIP_TEXT);
        widthAndHeightButton = new JRadioButton("By target width and height:");
        widthAndHeightButton.setToolTipText(TARGET_WIDTH_AND_HEIGHT_TOOLTIP_TEXT);
        resolutionButton = new JRadioButton("By pixel resolution (in m):");
        resolutionButton.setToolTipText(TARGET_RESOLUTION_TOOLTIP_TEXT);
        targetSizeButtonGroup.add(referenceBandButton);
        targetSizeButtonGroup.add(widthAndHeightButton);
        targetSizeButtonGroup.add(resolutionButton);

        defineTargetSizePanel.add(referenceBandButton);
        referenceBandNameBoxPanel = new ReferenceBandNameBoxPanel();
        defineTargetSizePanel.add(referenceBandNameBoxPanel);

        defineTargetSizePanel.add(widthAndHeightButton);
        targetWidthAndHeightPanel = new TargetWidthAndHeightPanel();
        defineTargetSizePanel.add(targetWidthAndHeightPanel);

        defineTargetSizePanel.add(resolutionButton);
        targetResolutionPanel = new TargetResolutionPanel();
        defineTargetSizePanel.add(targetResolutionPanel);

        referenceBandButton.addActionListener(e -> {
            if (referenceBandButton.isSelected()) {
                referenceBandNameBoxPanel.setEnabled(true);
                targetWidthAndHeightPanel.setEnabled(false);
                targetResolutionPanel.setEnabled(false);
            }
        });
        widthAndHeightButton.addActionListener(e -> {
            if (widthAndHeightButton.isSelected()) {
                referenceBandNameBoxPanel.setEnabled(false);
                targetWidthAndHeightPanel.setEnabled(true);
                targetResolutionPanel.setEnabled(false);
            }
        });
        resolutionButton.addActionListener(e -> {
            if (resolutionButton.isSelected()) {
                referenceBandNameBoxPanel.setEnabled(false);
                targetWidthAndHeightPanel.setEnabled(false);
                targetResolutionPanel.setEnabled(true);
            }
        });

        referenceBandButton.setSelected(true);

        JPanel upsamplingMethodPanel = new JPanel(new GridLayout(1, 2));
        PropertyDescriptor descriptorUp = propertySet.getProperty(UPSAMPLING_METHOD_PARAMETER_NAME).getDescriptor();
        JLabel upsamplingMethodLabel = new JLabel(descriptorUp.getAttribute("displayName").toString());
        upsamplingMethodLabel.setToolTipText(descriptorUp.getAttribute("description").toString());
        upsamplingMethodPanel.add(upsamplingMethodLabel);
        ComponentAdapter adapterUp = new ComboBoxAdapter(upsamplingCombo);
        bindingContext.bind(descriptorUp.getName(), adapterUp);
        upsamplingMethodPanel.add(upsamplingCombo);

        JPanel downsamplingMethodPanel = new JPanel(new GridLayout(1, 2));
        PropertyDescriptor descriptorDown = propertySet.getProperty(DOWNSAMPLING_METHOD_PARAMETER_NAME).getDescriptor();
        JLabel downsamplingMethodLabel = new JLabel(descriptorDown.getAttribute("displayName").toString());
        downsamplingMethodPanel.setToolTipText(descriptorDown.getAttribute("description").toString());
        downsamplingMethodPanel.add(downsamplingMethodLabel);
        ComponentAdapter adapterDown = new ComboBoxAdapter(downsamplingCombo);
        bindingContext.bind(descriptorDown.getName(), adapterDown);
        downsamplingMethodPanel.add(downsamplingCombo);

        JPanel flagDownsamplingMethodPanel = new JPanel(new GridLayout(1, 2));
        PropertyDescriptor descriptorFlag = propertySet.getProperty(FLAGDOWNSAMPLING_METHOD_PARAMETER_NAME).getDescriptor();
        JLabel flagDownsamplingMethodLabel = new JLabel(descriptorFlag.getAttribute("displayName").toString());
        flagDownsamplingMethodPanel.setToolTipText(descriptorFlag.getAttribute("description").toString());
        flagDownsamplingMethodPanel.add(flagDownsamplingMethodLabel);
        ComponentAdapter adapterFlag = new ComboBoxAdapter(flagDownsamplingCombo);
        bindingContext.bind(descriptorFlag.getName(), adapterFlag);
        flagDownsamplingMethodPanel.add(flagDownsamplingCombo);

        JPanel resampleOnPyramidLevelsPanel = new JPanel(new GridLayout(1, 2));
        PropertyDescriptor descriptorResampleOnPyramidLevels = propertySet.getProperty(PYRAMID_LEVELS_PARAMETER_NAME).getDescriptor();
        pyramidLevelCheckBox.setSelected((boolean) descriptorResampleOnPyramidLevels.getAttribute("defaultValue"));
        pyramidLevelCheckBox.setText(descriptorResampleOnPyramidLevels.getAttribute("displayName").toString());
        pyramidLevelCheckBox.setToolTipText(descriptorResampleOnPyramidLevels.getAttribute("description").toString());
        resampleOnPyramidLevelsPanel.add(pyramidLevelCheckBox);

        final JPanel parametersPanel = new JPanel(tableLayout);
        parametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        parametersPanel.add(defineTargetSizePanel);
        parametersPanel.add(upsamplingMethodPanel);
        parametersPanel.add(downsamplingMethodPanel);
        parametersPanel.add(flagDownsamplingMethodPanel);
        parametersPanel.add(resampleOnPyramidLevelsPanel);
        parametersPanel.add(tableLayout.createVerticalSpacer());
        return parametersPanel;
    }

    private void reactToSourceProductChange(Product product) {
        if(hasChangedProductListBand(product)) {
            updateListBands(product);
            referenceBandNameBoxPanel.reactToSourceProductChange(product);
        }
        if(hasChangedProductSize(product)) {
            updateProductSize(product);
            targetWidthAndHeightPanel.reactToSourceProductChange(product);
            targetResolutionPanel.reactToSourceProductChange(product);
        }

        if (product != null) {
            referenceBandButton.setEnabled(product.getBandNames().length > 0);

            final ProductNodeGroup<Band> productBands = product.getBandGroup();
            final ProductNodeGroup<TiePointGrid> productTiePointGrids = product.getTiePointGridGroup();
            double xOffset = Double.NaN;
            double yOffset = Double.NaN;
            if (productBands.getNodeCount() > 0) {
                xOffset = productBands.get(0).getImageToModelTransform().getTranslateX();
                yOffset = productBands.get(0).getImageToModelTransform().getTranslateY();
            } else if (productTiePointGrids.getNodeCount() > 0) {
                xOffset = productTiePointGrids.get(0).getImageToModelTransform().getTranslateX();
                yOffset = productTiePointGrids.get(0).getImageToModelTransform().getTranslateY();
            }
            boolean allowToSetWidthAndHeight = true;
            if (!Double.isNaN(xOffset) && !Double.isNaN(yOffset)) {
                allowToSetWidthAndHeight = allOffsetsAreEqual(productBands, xOffset, yOffset) &&
                        allOffsetsAreEqual(productTiePointGrids, xOffset, yOffset);
            }
            widthAndHeightButton.setEnabled(allowToSetWidthAndHeight);
            final GeoCoding sceneGeoCoding = product.getSceneGeoCoding();

            boolean resolutionEnable = sceneGeoCoding != null && sceneGeoCoding instanceof CrsGeoCoding;
            resolutionButton.setEnabled(resolutionEnable);
            if(resolutionButton.isSelected() && !resolutionEnable) {
                targetResolutionPanel.setEnabled(false);
                targetWidthAndHeightPanel.setEnabled(true);
                widthAndHeightButton.setSelected(true);
            }

        }
    }

    private RasterDataNode getAnyRasterDataNode(Product product) {
        RasterDataNode node = null;
        if (product != null) {
            final ProductNodeGroup<Band> bandGroup = product.getBandGroup();
            if (bandGroup.getNodeCount() == 0) {
                final ProductNodeGroup<TiePointGrid> tiePointGridGroup = product.getTiePointGridGroup();
                if (tiePointGridGroup.getNodeCount() > 0) {
                    node = tiePointGridGroup.get(0);
                }
            } else {
                node = bandGroup.get(0);
            }
        }
        return node;
    }

    private boolean allOffsetsAreEqual(ProductNodeGroup productNodeGroup, double xOffset, double yOffset) {
        for (int i = 0; i < productNodeGroup.getNodeCount(); i++) {
            final double nodeXOffset = ((RasterDataNode) productNodeGroup.get(i)).getImageToModelTransform().getTranslateX();
            final double nodeYOffset = ((RasterDataNode) productNodeGroup.get(i)).getImageToModelTransform().getTranslateY();
            if (Math.abs(nodeXOffset - xOffset) > 1e-8 || Math.abs(nodeYOffset - yOffset) > 1e-8) {
                return false;
            }
        }
        return true;
    }

    private class TargetResolutionPanel extends JPanel {

        private JSpinner resolutionSpinner;
        private JLabel targetResolutionTargetWidthLabel;
        private JLabel targetResolutionTargetHeightLabel;
        private final JLabel targetResolutionTargetWidthNameLabel;
        private final JLabel targetResolutionNameTargetHeightLabel;

        TargetResolutionPanel() {
            setToolTipText(TARGET_RESOLUTION_TOOLTIP_TEXT);
            resolutionSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
            resolutionSpinner.setEnabled(false);
            resolutionSpinner.addChangeListener(e -> updateTargetResolutionTargetWidthAndHeight());
            final GridLayout layout = new GridLayout(3, 1);
            layout.setVgap(2);
            setLayout(layout);
            JPanel targetResolutionTargetWidthPanel = new JPanel(new GridLayout(1, 2));
            targetResolutionTargetWidthNameLabel = new JLabel("Resulting target width: ");
            targetResolutionTargetWidthNameLabel.setEnabled(false);
            targetResolutionTargetWidthPanel.add(targetResolutionTargetWidthNameLabel);
            targetResolutionTargetWidthLabel = new JLabel();
            targetResolutionTargetWidthLabel.setEnabled(false);
            targetResolutionTargetWidthPanel.add(targetResolutionTargetWidthLabel);
            JPanel targetResolutionTargetHeightPanel = new JPanel(new GridLayout(1, 2));
            targetResolutionNameTargetHeightLabel = new JLabel("Resulting target height: ");
            targetResolutionNameTargetHeightLabel.setEnabled(false);
            targetResolutionTargetHeightPanel.add(targetResolutionNameTargetHeightLabel);
            targetResolutionTargetHeightLabel = new JLabel();
            targetResolutionTargetHeightLabel.setEnabled(false);
            targetResolutionTargetHeightPanel.add(targetResolutionTargetHeightLabel);
            add(resolutionSpinner);
            add(targetResolutionTargetWidthPanel);
            add(targetResolutionTargetHeightPanel);
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            resolutionSpinner.setEnabled(enabled);
            targetResolutionTargetWidthLabel.setEnabled(enabled);
            targetResolutionTargetHeightLabel.setEnabled(enabled);
            targetResolutionTargetWidthNameLabel.setEnabled(enabled);
            targetResolutionNameTargetHeightLabel.setEnabled(enabled);
            if (enabled) {
                updateTargetResolutionTargetWidthAndHeight();
            }
        }

        private void updateTargetResolutionTargetWidthAndHeight() {
            if (hasSourceProducts()) {
                final Product selectedProduct = sourceProducts[0];
                final RasterDataNode node = getAnyRasterDataNode(selectedProduct);
                int targetWidth = 0;
                int targetHeight = 0;
                if (node != null) {
                    final int resolution = Integer.parseInt(resolutionSpinner.getValue().toString());
                    final double nodeResolution = node.getImageToModelTransform().getScaleX();
                    targetWidth = (int) (node.getRasterWidth() * (nodeResolution / resolution));
                    targetHeight = (int) (node.getRasterHeight() * (nodeResolution / resolution));
                }
                targetResolutionTargetWidthLabel.setText("" + targetWidth);
                targetResolutionTargetHeightLabel.setText("" + targetHeight);
            }
        }

        private void reactToSourceProductChange(Product product) {
            if (product != null) {
                resolutionSpinner.setValue(determineResolutionFromProduct(product));
            } else {
                resolutionSpinner.setValue(0);
            }
        }

        private int determineResolutionFromProduct(Product product) {
            final RasterDataNode node = getAnyRasterDataNode(product);
            if (node != null) {
                return (int) node.getImageToModelTransform().getScaleX();
            }
            return 1;
        }
    }



    private class TargetWidthAndHeightPanel extends JPanel {

        private JSpinner widthSpinner;
        private JSpinner heightSpinner;
        private double targetWidthHeightRatio;
        private final JLabel targetWidthNameLabel;
        private final JLabel targetHeightNameLabel;
        private final JLabel widthHeightRatioNameLabel;
        private JLabel widthHeightRatioLabel;

        TargetWidthAndHeightPanel() {
            setToolTipText(TARGET_WIDTH_AND_HEIGHT_TOOLTIP_TEXT);
            targetWidthHeightRatio = 1.0;
            final GridLayout layout = new GridLayout(3, 2);
            layout.setVgap(2);
            setLayout(layout);
            targetWidthNameLabel = new JLabel("Target width:");
            targetWidthNameLabel.setEnabled(false);
            add(targetWidthNameLabel);
            widthSpinner = new JSpinner(new SpinnerNumberModel(100, 0, 1000000, 1));
            widthSpinner.setEnabled(false);
            add(widthSpinner);
            targetHeightNameLabel = new JLabel("Target height:");
            targetHeightNameLabel.setEnabled(false);
            add(targetHeightNameLabel);
            heightSpinner = new JSpinner(new SpinnerNumberModel(100, 0, 1000000, 1));
            heightSpinner.setEnabled(false);
            add(heightSpinner);
            widthHeightRatioNameLabel = new JLabel("Width / height ratio: ");
            widthHeightRatioNameLabel.setEnabled(false);
            add(widthHeightRatioNameLabel);
            widthHeightRatioLabel = new JLabel();
            widthHeightRatioLabel.setEnabled(false);
            add(widthHeightRatioLabel);
            widthSpinner.addChangeListener(e -> updateTargetWidth());
            heightSpinner.addChangeListener(e -> updateTargetHeight());
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            widthSpinner.setEnabled(enabled);
            heightSpinner.setEnabled(enabled);
            widthHeightRatioLabel.setEnabled(enabled);
            targetWidthNameLabel.setEnabled(enabled);
            targetHeightNameLabel.setEnabled(enabled);
            widthHeightRatioNameLabel.setEnabled(enabled);
        }

        private void updateTargetWidth() {
            if (!updatingTargetWidthAndHeight) {
                updatingTargetWidthAndHeight = true;
                final int targetWidth = Integer.parseInt(widthSpinner.getValue().toString());
                final int targetHeight = (int) (targetWidth / targetWidthHeightRatio);
                heightSpinner.setValue(targetHeight);
                updatingTargetWidthAndHeight = false;
            }
        }

        private void updateTargetHeight() {
            if (!updatingTargetWidthAndHeight) {
                updatingTargetWidthAndHeight = true;
                final int targetHeight = Integer.parseInt(heightSpinner.getValue().toString());
                final int targetWidth = (int) (targetHeight * targetWidthHeightRatio);
                widthSpinner.setValue(targetWidth);
                updatingTargetWidthAndHeight = false;
            }
        }

        private void reactToSourceProductChange(Product product) {
            if (product != null) {
                targetWidthHeightRatio = product.getSceneRasterWidth() / (double) product.getSceneRasterHeight();
                widthSpinner.setValue(product.getSceneRasterWidth());
                heightSpinner.setValue(product.getSceneRasterHeight());

            } else {
                targetWidthHeightRatio = 1.0;
                widthSpinner.setValue(0);
                heightSpinner.setValue(0);
            }
            widthHeightRatioLabel.setText(String.format("%.5f", targetWidthHeightRatio));
        }

    }

    private class ReferenceBandNameBoxPanel extends JPanel {

        private JComboBox<String> referenceBandNameBox;
        private JLabel referenceBandTargetWidthLabel;
        private JLabel referenceBandTargetHeightLabel;
        private final JLabel referenceBandTargetHeightNameLabel;
        private final JLabel referenceBandTargetWidthNameLabel;

        ReferenceBandNameBoxPanel() {
            setToolTipText(REFERENCE_BAND_TOOLTIP_TEXT);
            referenceBandNameBox = new JComboBox<>();
            referenceBandNameBox.addActionListener(e -> {
                updateReferenceBandTargetWidthAndHeight();
            });
            final GridLayout referenceBandNameBoxPanelLayout = new GridLayout(3, 1);
            referenceBandNameBoxPanelLayout.setVgap(2);
            setLayout(referenceBandNameBoxPanelLayout);
            add(referenceBandNameBox);
            JPanel referenceBandNameTargetWidthPanel = new JPanel(new GridLayout(1, 2));
            referenceBandTargetWidthNameLabel = new JLabel("Resulting target width: ");
            referenceBandNameTargetWidthPanel.add(referenceBandTargetWidthNameLabel);
            referenceBandTargetWidthLabel = new JLabel();
            referenceBandNameTargetWidthPanel.add(referenceBandTargetWidthLabel);
            JPanel referenceBandNameTargetHeightPanel = new JPanel(new GridLayout(1, 2));
            referenceBandTargetHeightNameLabel = new JLabel("Resulting target height: ");
            referenceBandNameTargetHeightPanel.add(referenceBandTargetHeightNameLabel);
            referenceBandTargetHeightLabel = new JLabel();
            referenceBandNameTargetHeightPanel.add(referenceBandTargetHeightLabel);
            add(referenceBandNameTargetWidthPanel);
            add(referenceBandNameTargetHeightPanel);
            referenceBandNameBox.addActionListener(e -> updateReferenceBandName());
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            referenceBandNameBox.setEnabled(enabled);
            referenceBandTargetWidthLabel.setEnabled(enabled);
            referenceBandTargetHeightLabel.setEnabled(enabled);
            referenceBandTargetWidthNameLabel.setEnabled(enabled);
            referenceBandTargetHeightNameLabel.setEnabled(enabled);
            if (enabled) {
                updateReferenceBandName();
            }
        }

        private void updateReferenceBandTargetWidthAndHeight() {
            if (referenceBandNameBox.getSelectedItem() != null) {
                final String bandName = referenceBandNameBox.getSelectedItem().toString();
                final Band band = sourceProducts[0].getBand(bandName);
                referenceBandTargetWidthLabel.setText("" + band.getRasterWidth());
                referenceBandTargetHeightLabel.setText("" + band.getRasterHeight());
            }
        }

        private void updateReferenceBandName() {
            updateParameters();
        }

        private void reactToSourceProductChange(Product product) {
            Object selected = referenceBandNameBox.getSelectedItem();
            referenceBandNameBox.removeAllItems();
            String[] bandNames = new String[0];
            if (product != null) {
                bandNames = product.getBandNames();
            }

            referenceBandNameBox.setModel(new DefaultComboBoxModel<>(bandNames));
            referenceBandNameBox.setEditable(false);
            if(selected != null) {
                referenceBandNameBox.setSelectedItem(selected);
            }
            updateReferenceBandTargetWidthAndHeight();
        }
    }

    private boolean hasChangedProductSize(Product product) {
        return !(product.getSceneRasterWidth() == lastProductWidth && product.getSceneRasterHeight() == lastProductHeight);
    }

    private boolean hasChangedProductListBand(Product product) {
        if(product.getBands().length != listBands.size()) {
            return true;
        }

        for(String bandName : listBands) {
            if(product.getBand(bandName) == null) {
                return true;
            }
        }

        return false;
    }

    private void updateListBands(Product product) {
        listBands.clear();
        if(product == null) {
            return;
        }
        for(Band band : product.getBands()) {
            listBands.add(band.getName());
        }
    }

    private void updateProductSize(Product product) {
        if(product == null) {
            lastProductWidth = 0;
            lastProductHeight = 0;
            return;
        }
        lastProductWidth = product.getSceneRasterWidth();
        lastProductHeight = product.getSceneRasterHeight();
    }

}
