package org.esa.snap.core.gpf.ui.resample;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.common.resample.BandResamplingPreset;
import org.esa.snap.core.gpf.common.resample.ResampleUtils;
import org.esa.snap.core.gpf.common.resample.ResamplingPreset;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.ui.DefaultIOParametersPanel;
import org.esa.snap.core.gpf.ui.OperatorMenu;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.core.gpf.ui.ParameterUpdater;
import org.esa.snap.core.gpf.ui.SingleTargetProductDialog;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.core.gpf.ui.TargetProductSelectorModel;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.SnapFileChooser;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.Outline;
import org.netbeans.swing.outline.OutlineModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Tonio Fincke
 */
class ResamplingDialog extends SingleTargetProductDialog {

    private final String operatorName;
    private final OperatorDescriptor operatorDescriptor;
    private final String REFERENCE_BAND_TOOLTIP_TEXT = "<html>Set the reference band.<br/>" +
            "All other bands will be resampled to match its size and resolution.</html>";
    private final String TARGET_WIDTH_AND_HEIGHT_TOOLTIP_TEXT =
            "<html>Set explicitly the width and height of the resampled product.<br/>" +
                    "This option is only available when all bands have the same offset.</html>";
    private final String TARGET_RESOLUTION_TOOLTIP_TEXT = "<html>Define the target resolution of the resampled product.<br/>" +
            "This option is only available for products with a geocoding based on a cartographic map CRS.</html>";
    private static final String REFERENCE_BAND_NAME_PROPERTY_NAME = "referenceBandName";
    private static final String TARGET_WIDTH_PROPERTY_NAME = "targetWidth";
    private static final String TARGET_HEIGHT_PROPERTY_NAME = "targetHeight";
    private static final String TARGET_RESOLUTION_PROPERTY_NAME = "targetResolution";

    private static final int REFERENCE_BAND_NAME_PANEL_INDEX = 0;
    private static final int TARGET_WIDTH_AND_HEIGHT_PANEL_INDEX = 1;
    private static final int TARGET_RESOLUTION_PANEL_INDEX = 2;

    private DefaultIOParametersPanel ioParametersPanel;
    private final OperatorParameterSupport parameterSupport;
    private final BindingContext bindingContext;

    private JTabbedPane form;
    private String targetProductNameSuffix;
    private ProductChangedHandler productChangedHandler;

    private Product targetProduct;
    private JRadioButton referenceBandButton;
    private JRadioButton widthAndHeightButton;
    private JRadioButton resolutionButton;
    private ReferenceBandNameBoxPanel referenceBandNameBoxPanel;
    private TargetWidthAndHeightPanel targetWidthAndHeightPanel;
    private TargetResolutionPanel targetResolutionPanel;
    private JPanel advancedMethodDefinitionPanel;
    private JPanel loadPresetPanel;
    JCheckBox advancedMethodCheckBox;

    JPanel upsamplingMethodPanel;
    JPanel downsamplingMethodPanel;
    JPanel flagDownsamplingMethodPanel;

    private BandResamplingPreset[] bandResamplingPresets;
    OutlineModel mdl = null;
    Outline outline1 = null;
    ResamplingRowModel resamplingRowModel;

    ResamplingDialog(AppContext appContext, Product product, boolean modal) {
        super(appContext, "Resampling", ID_APPLY_CLOSE, "resampleAction");
        this.operatorName = "Resample";
        targetProductNameSuffix = "_resampled";
        getTargetProductSelector().getModel().setSaveToFileSelected(false);
        getJDialog().setModal(modal);
        OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalArgumentException("No SPI found for operator name '" + operatorName + "'");
        }

        operatorDescriptor = operatorSpi.getOperatorDescriptor();
        ioParametersPanel = new DefaultIOParametersPanel(getAppContext(), operatorDescriptor, getTargetProductSelector(), true);
        targetProduct = null;

        parameterSupport = new OperatorParameterSupport(operatorDescriptor, null, null, new ResamplingParameterUpdater());
        final ArrayList<SourceProductSelector> sourceProductSelectorList = ioParametersPanel.getSourceProductSelectorList();
        final PropertySet propertySet = parameterSupport.getPropertySet();
        bindingContext = new BindingContext(propertySet);
        final Property referenceBandNameProperty = bindingContext.getPropertySet().getProperty(REFERENCE_BAND_NAME_PROPERTY_NAME);
        referenceBandNameProperty.getDescriptor().addAttributeChangeListener(evt -> {
            if (evt.getPropertyName().equals("valueSet")) {
                final Object[] valueSetItems = ((ValueSet) evt.getNewValue()).getItems();
                if (valueSetItems.length > 0) {
                    try {
                        referenceBandNameProperty.setValue(valueSetItems[0].toString());
                    } catch (ValidationException e) {
                        //don't set it then
                    }
                }
            }
        });
        final ValueRange valueRange = new ValueRange(0, Integer.MAX_VALUE);
        bindingContext.getPropertySet().getProperty(TARGET_WIDTH_PROPERTY_NAME).getDescriptor().setValueRange(valueRange);
        bindingContext.getPropertySet().getProperty(TARGET_HEIGHT_PROPERTY_NAME).getDescriptor().setValueRange(valueRange);
        bindingContext.getPropertySet().getProperty(TARGET_RESOLUTION_PROPERTY_NAME).getDescriptor().setValueRange(valueRange);
        productChangedHandler = new ProductChangedHandler();
        sourceProductSelectorList.get(0).initProducts();
        sourceProductSelectorList.get(0).setSelectedProduct(product);
        sourceProductSelectorList.get(0).addSelectionChangeListener(productChangedHandler);
    }

    @Override
    public int show() {
        if (form == null) {
            initForm();
            if (getJDialog().getJMenuBar() == null) {
                final OperatorMenu operatorMenu = createDefaultMenuBar();
                getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
            }
        }
        ioParametersPanel.initSourceProductSelectors();
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        productChangedHandler.releaseProduct();
        ioParametersPanel.releaseSourceProductSelectors();
        super.hide();
    }

    @Override
    protected void onApply() {
        super.onApply();
        if (targetProduct != null && getJDialog().isModal()) {
            close();
        }
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final HashMap<String, Product> sourceProducts = ioParametersPanel.createSourceProductsMap();
        final HashMap<String, Object> resampParams = new HashMap<>();
        if(referenceBandButton.isSelected()) {
            resampParams.put("referenceBandName", referenceBandNameBoxPanel.getSelectedReferenceBand());
        } else if (widthAndHeightButton.isSelected()) {
            resampParams.put("targetWidth", targetWidthAndHeightPanel.getWidthSelected());
            resampParams.put("targetHeight", targetWidthAndHeightPanel.getHeightSelected());
        } else if (resolutionButton.isSelected()) {
            resampParams.put("targetResolution", targetResolutionPanel.getResolutionSelected());
        }
        resampParams.put("upsamplingMethod", parameterSupport.getParameterMap().get("upsamplingMethod"));
        resampParams.put("downsamplingMethod", parameterSupport.getParameterMap().get("downsamplingMethod"));
        resampParams.put("flagDownsamplingMethod", parameterSupport.getParameterMap().get("flagDownsamplingMethod"));

        if(advancedMethodCheckBox.isSelected()) {
            resampParams.put("bandResamplings", generateBandResamplings(sourceProducts.get("sourceProduct")));
        }

        resampParams.put("resampleOnPyramidLevels", parameterSupport.getParameterMap().get("resampleOnPyramidLevels"));

        targetProduct = GPF.createProduct(operatorName, resampParams, sourceProducts);
        return targetProduct;
    }

    private String generateBandResamplings(Product sourceProduct) {
        updateResamplingPreset();
        String bandResamplingsString = "";
        for(String bandName : sourceProduct.getBandNames()) {
            for(BandResamplingPreset bandResamplingPreset : bandResamplingPresets) {
                if(bandResamplingPreset.getBandName().equals(bandName)){
                    if(!bandResamplingsString.isEmpty()) {
                        bandResamplingsString = bandResamplingsString + ResamplingPreset.STRING_SEPARATOR;
                    }
                    bandResamplingsString = bandResamplingsString + bandResamplingPreset.getBandName() + BandResamplingPreset.SEPARATOR
                            + bandResamplingPreset.getDownsamplingAlias() + BandResamplingPreset.SEPARATOR + bandResamplingPreset.getUpsamplingAlias();

                }
            }
        }
        return bandResamplingsString;
    }

    Product getTargetProduct() {
        return targetProduct;
    }

    private void initForm() {
        form = new JTabbedPane();
        form.add("I/O Parameters", ioParametersPanel);
        form.add("Resampling Parameters", new JScrollPane(createParametersPanel()));
        reactToSourceProductChange(ioParametersPanel.getSourceProductSelectorList().get(0).getSelectedProduct());
    }

    private JPanel createParametersPanel() {
        final PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();
        final PropertySet propertySet = bindingContext.getPropertySet();

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTablePadding(4, 4);

        final TableLayout defineTargetResolutionPanelLayout = new TableLayout(2);
        defineTargetResolutionPanelLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        defineTargetResolutionPanelLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        defineTargetResolutionPanelLayout.setColumnWeightX(1, 1.0);
        defineTargetResolutionPanelLayout.setTablePadding(4, 5);
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
                enablePanel(REFERENCE_BAND_NAME_PANEL_INDEX);
            }
        });
        widthAndHeightButton.addActionListener(e -> {
            if (widthAndHeightButton.isSelected()) {
                enablePanel(TARGET_WIDTH_AND_HEIGHT_PANEL_INDEX);
            }
        });
        resolutionButton.addActionListener(e -> {
            if (resolutionButton.isSelected()) {
                enablePanel(TARGET_RESOLUTION_PANEL_INDEX);
            }
        });

        referenceBandButton.setSelected(true);


        final TableLayout tableLayoutMethodDefinition = new TableLayout(1);
        tableLayoutMethodDefinition.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayoutMethodDefinition.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayoutMethodDefinition.setTableWeightX(1.0);
        tableLayoutMethodDefinition.setTablePadding(4, 4);
        JPanel methodDefinitionPanel = new JPanel(tableLayoutMethodDefinition);
        methodDefinitionPanel.setBorder(BorderFactory.createTitledBorder("Define resampling algorithm"));


        upsamplingMethodPanel = createPropertyPanel(propertySet, "upsamplingMethod", registry);
        downsamplingMethodPanel = createPropertyPanel(propertySet, "downsamplingMethod", registry);
        flagDownsamplingMethodPanel = createPropertyPanel(propertySet, "flagDownsamplingMethod", registry);


        //Create advancedMethodDefinitionPanel
        advancedMethodDefinitionPanel = createAdvancedMethodDefinitionPanel();

        //Create load preset panel
        loadPresetPanel = createLoadSavePresetPanel();

        methodDefinitionPanel.add(upsamplingMethodPanel);
        methodDefinitionPanel.add(tableLayout.createVerticalSpacer());
        methodDefinitionPanel.add(downsamplingMethodPanel);
        methodDefinitionPanel.add(tableLayout.createVerticalSpacer());
        methodDefinitionPanel.add(flagDownsamplingMethodPanel);
        methodDefinitionPanel.add(tableLayout.createVerticalSpacer());
        methodDefinitionPanel.add(createAdvancedCheckBoxPanel());
        methodDefinitionPanel.add(advancedMethodDefinitionPanel);
        methodDefinitionPanel.add(loadPresetPanel);

        final JPanel resampleOnPyramidLevelsPanel = createPropertyPanel(propertySet, "resampleOnPyramidLevels", registry);

        //Add all panels
        final JPanel parametersPanel = new JPanel(tableLayout);
        parametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        parametersPanel.add(defineTargetSizePanel);
        parametersPanel.add(methodDefinitionPanel);
        parametersPanel.add(resampleOnPyramidLevelsPanel);
        parametersPanel.add(tableLayout.createVerticalSpacer());
        return parametersPanel;
    }


    private void enablePanel(int panelIndex) {
        referenceBandNameBoxPanel.setEnabled(panelIndex == 0);
        targetWidthAndHeightPanel.setEnabled(panelIndex == 1);
        targetResolutionPanel.setEnabled(panelIndex == 2);
    }

    private JPanel createPropertyPanel(PropertySet propertySet, String propertyName, PropertyEditorRegistry registry) {
        final PropertyDescriptor descriptor = propertySet.getProperty(propertyName).getDescriptor();
        PropertyEditor propertyEditor = registry.findPropertyEditor(descriptor);
        JComponent[] components = propertyEditor.createComponents(descriptor, bindingContext);
        final JPanel propertyPanel = new JPanel(new GridLayout(1, components.length));
        for (int i = components.length - 1; i >= 0; i--) {
            propertyPanel.add(components[i]);
        }
        return propertyPanel;
    }

    private void setEnableRec(Component container, boolean enable){
        container.setEnabled(enable);

        try {
            Component[] components= ((Container) container).getComponents();
            for (int i = 0; i < components.length; i++) {
                setEnableRec(components[i], enable);
            }
        } catch (ClassCastException e) {

        }
    }


    private JPanel createAdvancedCheckBoxPanel() {
        advancedMethodCheckBox = new JCheckBox("Advanced Method Definition by Band", false);

        advancedMethodCheckBox.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    advancedMethodDefinitionPanel.setVisible(true);
                    loadPresetPanel.setVisible(true);
                    setEnableRec(upsamplingMethodPanel,false);
                    setEnableRec(downsamplingMethodPanel,false);
                    setEnableRec(flagDownsamplingMethodPanel,false);

                } else {
                    advancedMethodDefinitionPanel.setVisible(false);
                    loadPresetPanel.setVisible(false);
                    setEnableRec(upsamplingMethodPanel,true);
                    setEnableRec(downsamplingMethodPanel,true);
                    setEnableRec(flagDownsamplingMethodPanel,true);
                }

            }
        });

        final JPanel propertyPanel = new JPanel(new GridLayout(1, 1));
        propertyPanel.add(advancedMethodCheckBox);

        return propertyPanel;
    }

    private JPanel createAdvancedMethodDefinitionPanel() {
        final TableLayout tableLayoutMethodDefinition = new TableLayout(1);
        tableLayoutMethodDefinition.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayoutMethodDefinition.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayoutMethodDefinition.setTableWeightX(1.0);
        tableLayoutMethodDefinition.setTablePadding(4, 4);
        JPanel panel = new JPanel(tableLayoutMethodDefinition);
        if(ioParametersPanel.getSourceProductSelectorList().get(0).getSelectedProduct() != null) {
            BandsTreeModel myModel = new BandsTreeModel(ioParametersPanel.getSourceProductSelectorList().get(0).getSelectedProduct());

            bandResamplingPresets = new BandResamplingPreset[myModel.getTotalRows()];
            for(int i = 0 ; i < myModel.getTotalRows() ; i++) {
                bandResamplingPresets[i] = new BandResamplingPreset(myModel.getRows()[i], (String) (parameterSupport.getParameterMap().get("downsamplingMethod")) , (String) (parameterSupport.getParameterMap().get("upsamplingMethod")));
            }

            resamplingRowModel = new ResamplingRowModel(bandResamplingPresets, myModel);

            mdl = DefaultOutlineModel.createOutlineModel(
                    myModel, resamplingRowModel, true, "Bands");

            outline1 = new Outline();
            outline1.setRootVisible(false);
            outline1.setModel(mdl);

            ResamplingUtils.setUpUpsamplingColumn(outline1,outline1.getColumnModel().getColumn(1), (String) parameterSupport.getParameterMap().get("upsamplingMethod"));
            ResamplingUtils.setUpDownsamplingColumn(outline1,outline1.getColumnModel().getColumn(2), (String) parameterSupport.getParameterMap().get("downsamplingMethod"));
            JScrollPane tableContainer = new JScrollPane(outline1);
            panel.add(tableContainer);

            panel.setVisible(false);
        }
        return panel;
    }


    private JPanel createLoadSavePresetPanel() {
        final TableLayout tableLayoutMethodDefinition = new TableLayout(2);
        tableLayoutMethodDefinition.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayoutMethodDefinition.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayoutMethodDefinition.setTableWeightX(1.0);
        tableLayoutMethodDefinition.setTablePadding(4, 4);
        JPanel panel = new JPanel(tableLayoutMethodDefinition);

        //Add Load preset button
        final ImageIcon loadIcon = TangoIcons.actions_document_open(TangoIcons.Res.R22);
        JButton loadButton = new JButton("Import Preset...", loadIcon);
        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                SnapFileChooser fileChooser = new SnapFileChooser(SystemUtils.getAuxDataPath().resolve(ResamplingUtils.RESAMPLING_PRESET_FOLDER).toFile());
                fileChooser.setAcceptAllFileFilterUsed(true);
                fileChooser.setDialogTitle("Select resampling preset");
                SnapFileFilter fileFilter = new SnapFileFilter("ResPreset", ".res", "Resampling preset files");

                fileChooser.addChoosableFileFilter(fileFilter);

                fileChooser.setFileFilter(fileFilter);

                fileChooser.setDialogType(SnapFileChooser.OPEN_DIALOG);

                File selectedFile;
                while (true) {
                    int i = fileChooser.showDialog(panel, null);
                    if (i == SnapFileChooser.APPROVE_OPTION) {
                        selectedFile = fileChooser.getSelectedFile();
                        try {
                            ResamplingPreset resamplingPreset = ResamplingPreset.loadResamplingPreset(selectedFile);

                            //check that bands corresponds with opened product
                            if(!resamplingPreset.isCompatibleWithProduct(ioParametersPanel.getSourceProductSelectorList().get(0).getSelectedProduct())) {
                                AbstractDialog.showWarningDialog(panel,
                                                                 "Resampling preset incompatible with selected input product.",
                                                                 "Resampling preset incompatibility");
                                break;
                            }
                            //TODO that check upsampling and resampling method exist

                            bandResamplingPresets = resamplingPreset.getBandResamplingPresets().toArray(new BandResamplingPreset[resamplingPreset.getBandResamplingPresets().size()]);

                            for(BandResamplingPreset bandResamplingPreset : resamplingPreset.getBandResamplingPresets()) {
                                resamplingRowModel.setValueFor(bandResamplingPreset.getBandName(),0,bandResamplingPreset.getUpsamplingAlias());
                                resamplingRowModel.setValueFor(bandResamplingPreset.getBandName(),1,bandResamplingPreset.getDownsamplingAlias());
                                advancedMethodDefinitionPanel.repaint();
                            }
                        } catch (IOException e1) {
                            AbstractDialog.showWarningDialog(panel,
                                                             "Cannot load resampling preset.",
                                                             "Cannot load resampling preset.");
                        }
                        break;
                    } else {
                        // Canceled
                        selectedFile = null;
                        break;
                    }
                }

            }
        });
        panel.add(loadButton);


        //Add Save preset button
        final ImageIcon saveIcon = TangoIcons.actions_document_save_as(TangoIcons.Res.R22);
        JButton saveButton = new JButton("Save Preset", saveIcon);
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                SnapFileChooser fileChooser = new SnapFileChooser(SystemUtils.getAuxDataPath().resolve(ResamplingUtils.RESAMPLING_PRESET_FOLDER).toFile());
                fileChooser.setAcceptAllFileFilterUsed(true);
                fileChooser.setDialogTitle("Select resampling preset");
                SnapFileFilter fileFilter = new SnapFileFilter("ResPreset", ".res", "Resampling preset files");

                fileChooser.addChoosableFileFilter(fileFilter);

                fileChooser.setFileFilter(fileFilter);

                fileChooser.setDialogType(SnapFileChooser.SAVE_DIALOG);

                File selectedFile;
                while (true) {
                    int i = fileChooser.showDialog(panel, null);
                    if (i == SnapFileChooser.APPROVE_OPTION) {
                        selectedFile = fileChooser.getSelectedFile();
                        if (!selectedFile.exists()) {
                            break;
                        }
                        i = JOptionPane.showConfirmDialog(panel,
                                                          "The file\n" + selectedFile + "\nalready exists.\nOverwrite?",
                                                          "File exists", JOptionPane.YES_NO_CANCEL_OPTION);
                        if (i == JOptionPane.CANCEL_OPTION) {
                            // Canceled
                            selectedFile = null;
                            break;
                        } else if (i == JOptionPane.YES_OPTION) {
                            // Overwrite existing file
                            break;
                        }
                    } else {
                        // Canceled
                        selectedFile = null;
                        break;
                    }
                }
                if(selectedFile != null) {
                    //update BandResamplingPresets with values of table
                    updateResamplingPreset();
                    ResamplingPreset auxPreset = new ResamplingPreset(selectedFile.getName(),bandResamplingPresets);
                    auxPreset.saveToFile(selectedFile, ioParametersPanel.getSourceProductSelectorList().get(0).getSelectedProduct());
                }
            }
        });
        panel.add(saveButton);

        panel.setVisible(false);
        return panel;
    }


    private OperatorMenu createDefaultMenuBar() {
        return new OperatorMenu(getJDialog(),
                                operatorDescriptor,
                                parameterSupport,
                                getAppContext(),
                                getHelpID());
    }

    private void reactToSourceProductChange(Product product) {
        referenceBandNameBoxPanel.reactToSourceProductChange(product);
        targetWidthAndHeightPanel.reactToSourceProductChange(product);
        targetResolutionPanel.reactToSourceProductChange(product);
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
            resolutionButton.setEnabled(sceneGeoCoding instanceof CrsGeoCoding &&
                    ResampleUtils.allGridsAlignAtUpperLeftPixel(product));
        }
        if (referenceBandButton.isEnabled()) {
            referenceBandButton.setSelected(true);
            referenceBandNameBoxPanel.setEnabled(true);
        } else if (widthAndHeightButton.isEnabled()) {
            widthAndHeightButton.setSelected(true);
        } else if (resolutionButton.isEnabled()) {
            resolutionButton.setSelected(true);
        }

        if(product != null) {
            advancedMethodDefinitionPanel.removeAll();
            BandsTreeModel myModel = new BandsTreeModel(product);

            bandResamplingPresets = new BandResamplingPreset[myModel.getTotalRows()];
            for(int i = 0 ; i < myModel.getTotalRows() ; i++) {
                bandResamplingPresets[i] = new BandResamplingPreset(myModel.getRows()[i], (String) (parameterSupport.getParameterMap().get("downsamplingMethod")),(String) (parameterSupport.getParameterMap().get("upsamplingMethod")));
            }

            resamplingRowModel = new ResamplingRowModel(bandResamplingPresets, myModel);
            //Create the Outline's model, consisting of the TreeModel and the RowModel
            mdl = DefaultOutlineModel.createOutlineModel(
                    myModel, resamplingRowModel, true, "Products");

            Outline outline1 = new Outline();
            outline1.setRootVisible(false);
            outline1.setModel(mdl);

            ResamplingUtils.setUpUpsamplingColumn(outline1,outline1.getColumnModel().getColumn(1), (String)  parameterSupport.getParameterMap().get("upsamplingMethod"));
            ResamplingUtils.setUpDownsamplingColumn(outline1,outline1.getColumnModel().getColumn(2), (String) parameterSupport.getParameterMap().get("downsamplingMethod"));

            JScrollPane tableContainer = new JScrollPane(outline1);
            advancedMethodDefinitionPanel.add(tableContainer);
            advancedMethodDefinitionPanel.revalidate();
            advancedMethodDefinitionPanel.setVisible(advancedMethodCheckBox.isSelected());
        }
    }

    private void updateResamplingPreset() {
        if(bandResamplingPresets == null) {
            return;
        }
        for (BandResamplingPreset bandResamplingPreset : bandResamplingPresets) {
            bandResamplingPreset.setUpsamplingAlias((String) resamplingRowModel.getValueFor(bandResamplingPreset.getBandName(),0));
            bandResamplingPreset.setDownsamplingAlias((String) resamplingRowModel.getValueFor(bandResamplingPreset.getBandName(),1));
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
            resolutionSpinner.addChangeListener(e -> updateTargetResolution());
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
                updateTargetResolution();
            }
        }

        private void updateTargetResolution() {
            bindingContext.getPropertySet().setValue(REFERENCE_BAND_NAME_PROPERTY_NAME, null);
            bindingContext.getPropertySet().setValue(TARGET_WIDTH_PROPERTY_NAME, null);
            bindingContext.getPropertySet().setValue(TARGET_HEIGHT_PROPERTY_NAME, null);
            bindingContext.getPropertySet().setValue(TARGET_RESOLUTION_PROPERTY_NAME, Integer.parseInt(resolutionSpinner.getValue().toString()));
            updateTargetResolutionTargetWidthAndHeight();
        }

        private void updateTargetResolutionTargetWidthAndHeight() {
            final Product selectedProduct = ioParametersPanel.getSourceProductSelectorList().get(0).getSelectedProduct();
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

        private void reactToSourceProductChange(Product product) {
            if (product != null) {
                resolutionSpinner.setValue(new Double(determineResolutionFromProduct(product)));
            } else {
                resolutionSpinner.setValue(0.0);
            }
        }

        private void handleParameterLoadRequest(Map<String, Object> parameterMap) {
            if (parameterMap.containsKey(TARGET_RESOLUTION_PROPERTY_NAME)) {
                resolutionSpinner.setValue(parameterMap.get(TARGET_RESOLUTION_PROPERTY_NAME));
                resolutionButton.setSelected(true);
                enablePanel(TARGET_RESOLUTION_PANEL_INDEX);
            }
        }

        private int determineResolutionFromProduct(Product product) {
            final RasterDataNode node = getAnyRasterDataNode(product);
            if (node != null) {
                return (int) node.getImageToModelTransform().getScaleX();
            }
            return 1;
        }

        public Object getResolutionSelected() {
            return resolutionSpinner.getValue();
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
        private boolean updatingTargetWidthAndHeight;

        TargetWidthAndHeightPanel() {
            setToolTipText(TARGET_WIDTH_AND_HEIGHT_TOOLTIP_TEXT);
            updatingTargetWidthAndHeight = false;
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
            if (enabled) {
                updateTargetWidthAndHeight();
            }
        }

        private void updateTargetWidth() {
            if (!updatingTargetWidthAndHeight) {
                updatingTargetWidthAndHeight = true;
                final int targetWidth = Integer.parseInt(widthSpinner.getValue().toString());
                final int targetHeight = (int) (targetWidth / targetWidthHeightRatio);
                heightSpinner.setValue(targetHeight);
                updateTargetWidthAndHeight();
                updatingTargetWidthAndHeight = false;
            }
        }

        private void updateTargetHeight() {
            if (!updatingTargetWidthAndHeight) {
                updatingTargetWidthAndHeight = true;
                final int targetHeight = Integer.parseInt(heightSpinner.getValue().toString());
                final int targetWidth = (int) (targetHeight * targetWidthHeightRatio);
                widthSpinner.setValue(targetWidth);
                updateTargetWidthAndHeight();
                updatingTargetWidthAndHeight = false;
            }
        }

        private void updateTargetWidthAndHeight() {
            bindingContext.getPropertySet().setValue(REFERENCE_BAND_NAME_PROPERTY_NAME, null);
            bindingContext.getPropertySet().setValue(TARGET_WIDTH_PROPERTY_NAME, Integer.parseInt(widthSpinner.getValue().toString()));
            bindingContext.getPropertySet().setValue(TARGET_HEIGHT_PROPERTY_NAME, Integer.parseInt(heightSpinner.getValue().toString()));
            bindingContext.getPropertySet().setValue(TARGET_RESOLUTION_PROPERTY_NAME, null);
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

        private void handleParameterLoadRequest(Map<String, Object> parameterMap) {
            if (parameterMap.containsKey(TARGET_WIDTH_PROPERTY_NAME) &&
                    parameterMap.containsKey(TARGET_HEIGHT_PROPERTY_NAME)) {
                widthSpinner.setValue(parameterMap.get(TARGET_WIDTH_PROPERTY_NAME));
                heightSpinner.setValue(parameterMap.get(TARGET_HEIGHT_PROPERTY_NAME));
                widthAndHeightButton.setSelected(true);
                enablePanel(TARGET_WIDTH_AND_HEIGHT_PANEL_INDEX);
            }
        }

        public Object getWidthSelected(){
            return widthSpinner.getValue();
        }

        public Object getHeightSelected(){
            return heightSpinner.getValue();
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
                final Band band = ioParametersPanel.getSourceProductSelectorList().get(0).getSelectedProduct().getBand(bandName);
                referenceBandTargetWidthLabel.setText("" + band.getRasterWidth());
                referenceBandTargetHeightLabel.setText("" + band.getRasterHeight());
            }
        }

        private void updateReferenceBandName() {
            if (referenceBandNameBox.getSelectedItem() != null) {
                bindingContext.getPropertySet().setValue(REFERENCE_BAND_NAME_PROPERTY_NAME, referenceBandNameBox.getSelectedItem().toString());
            } else {
                bindingContext.getPropertySet().setValue(REFERENCE_BAND_NAME_PROPERTY_NAME, null);
            }
            bindingContext.getPropertySet().setValue(TARGET_WIDTH_PROPERTY_NAME, null);
            bindingContext.getPropertySet().setValue(TARGET_HEIGHT_PROPERTY_NAME, null);
            bindingContext.getPropertySet().setValue(TARGET_RESOLUTION_PROPERTY_NAME, null);
        }

        private void reactToSourceProductChange(Product product) {
            referenceBandNameBox.removeAllItems();
            String[] bandNames = new String[0];
            if (product != null) {
                bandNames = product.getBandNames();
            }
            bindingContext.getPropertySet().getProperty(REFERENCE_BAND_NAME_PROPERTY_NAME).getDescriptor().setValueSet(new ValueSet(bandNames));
            referenceBandNameBox.setModel(new DefaultComboBoxModel<>(bandNames));
            updateReferenceBandTargetWidthAndHeight();
        }

        private void handleParameterLoadRequest(Map<String, Object> parameterMap) {
            if (parameterMap.containsKey(REFERENCE_BAND_NAME_PROPERTY_NAME)) {
                referenceBandNameBox.setSelectedItem(parameterMap.get(REFERENCE_BAND_NAME_PROPERTY_NAME));
                referenceBandButton.setSelected(true);
                enablePanel(REFERENCE_BAND_NAME_PANEL_INDEX);
            }
        }

        public String getSelectedReferenceBand() {
            return referenceBandNameBox.getSelectedItem().toString();
        }
    }

    private class ProductChangedHandler extends AbstractSelectionChangeListener implements ProductNodeListener {

        private Product currentProduct;

        public void releaseProduct() {
            if (currentProduct != null) {
                currentProduct.removeProductNodeListener(this);
                currentProduct = null;
            }
        }

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            Selection selection = event.getSelection();
            if (selection != null) {
                final Product selectedProduct = (Product) selection.getSelectedValue();
                if (selectedProduct != currentProduct) {
                    if (currentProduct != null) {
                        currentProduct.removeProductNodeListener(this);
                    }
                    currentProduct = selectedProduct;
                    if (currentProduct != null) {
                        currentProduct.addProductNodeListener(this);
                    }
                    if (getTargetProductSelector() != null) {
                        updateTargetProductName();
                    }
                    reactToSourceProductChange(currentProduct);
                }
            }
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            handleProductNodeEvent();
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            handleProductNodeEvent();
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            handleProductNodeEvent();
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            handleProductNodeEvent();
        }

        private void updateTargetProductName() {
            String productName = "";
            if (currentProduct != null) {
                productName = currentProduct.getName();
            }
            final TargetProductSelectorModel targetProductSelectorModel = getTargetProductSelector().getModel();
            targetProductSelectorModel.setProductName(productName + targetProductNameSuffix);
        }

        private void handleProductNodeEvent() {
            reactToSourceProductChange(currentProduct);
        }

    }

    private class ResamplingParameterUpdater implements ParameterUpdater {

        @Override
        public void handleParameterSaveRequest(Map<String, Object> parameterMap) throws ValidationException, ConversionException {
        }

        @Override
        public void handleParameterLoadRequest(Map<String, Object> parameterMap) throws ValidationException, ConversionException {
            referenceBandNameBoxPanel.handleParameterLoadRequest(parameterMap);
            targetWidthAndHeightPanel.handleParameterLoadRequest(parameterMap);
            targetResolutionPanel.handleParameterLoadRequest(parameterMap);
        }
    }

}
