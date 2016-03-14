package org.esa.snap.core.gpf.ui.resample;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
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
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.ui.DefaultIOParametersPanel;
import org.esa.snap.core.gpf.ui.OperatorMenu;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.core.gpf.ui.SingleTargetProductDialog;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.core.gpf.ui.TargetProductSelectorModel;
import org.esa.snap.ui.AppContext;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Tonio Fincke
 */
class ResamplingDialog extends SingleTargetProductDialog {
    private final String operatorName;
    private final OperatorDescriptor operatorDescriptor;
    private DefaultIOParametersPanel ioParametersPanel;
    private final OperatorParameterSupport parameterSupport;
    private final BindingContext bindingContext;

    private JTabbedPane form;
    private String targetProductNameSuffix;
    private ProductChangedHandler productChangedHandler;

    private Product targetProduct;
    private JComboBox<String> referenceBandNameBox;
    private JRadioButton referenceBandButton;
    private JRadioButton widthAndHeightButton;
    private JRadioButton resolutionButton;
    private JSpinner widthSpinner;
    private JSpinner heightSpinner;
    private JSpinner resolutionSpinner;
    private double targetWidthHeightRatio;
    private boolean updatingTargetWidthAndHeight;

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
        targetWidthHeightRatio = 1.0;
        updatingTargetWidthAndHeight = false;

        parameterSupport = new OperatorParameterSupport(operatorDescriptor);
        final ArrayList<SourceProductSelector> sourceProductSelectorList = ioParametersPanel.getSourceProductSelectorList();
        final PropertySet propertySet = parameterSupport.getPropertySet();
        bindingContext = new BindingContext(propertySet);
        final Property referenceBandNameProperty = bindingContext.getPropertySet().getProperty("referenceBandName");
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
        productChangedHandler = new ProductChangedHandler();
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
        targetProduct = GPF.createProduct(operatorName, parameterSupport.getParameterMap(), sourceProducts);
        return targetProduct;
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

        final JPanel defineTargetResolutionPanel = new JPanel(new GridLayout(3, 2));
        defineTargetResolutionPanel.setBorder(BorderFactory.createTitledBorder("Define size of resampled product"));
        final ButtonGroup targetSizeButtonGroup = new ButtonGroup();
        referenceBandButton = new JRadioButton("By reference band from source product:");
        widthAndHeightButton = new JRadioButton("By target width and height:");
        resolutionButton = new JRadioButton("By pixel resolution (in m):");
        targetSizeButtonGroup.add(referenceBandButton);
        targetSizeButtonGroup.add(widthAndHeightButton);
        targetSizeButtonGroup.add(resolutionButton);

        defineTargetResolutionPanel.add(referenceBandButton);
        referenceBandNameBox = new JComboBox<>();
        defineTargetResolutionPanel.add(referenceBandNameBox);

        defineTargetResolutionPanel.add(widthAndHeightButton);
        final JPanel widthAndHeightPanel = new JPanel(new GridLayout(2, 2));
        widthAndHeightPanel.add(new JLabel("Target Width:"));
        widthSpinner = new JSpinner(new SpinnerNumberModel(100, 0, 1000000, 1));
        widthSpinner.setEnabled(false);
        widthAndHeightPanel.add(widthSpinner);
        widthAndHeightPanel.add(new JLabel("Target Height:"));
        heightSpinner = new JSpinner(new SpinnerNumberModel(100, 0, 1000000, 1));
        heightSpinner.setEnabled(false);
        widthAndHeightPanel.add(heightSpinner);
        defineTargetResolutionPanel.add(widthAndHeightPanel);

        defineTargetResolutionPanel.add(resolutionButton);
        resolutionSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        resolutionSpinner.setEnabled(false);
        defineTargetResolutionPanel.add(resolutionSpinner);
        referenceBandButton.setSelected(true);

        referenceBandButton.addActionListener(e -> {
            if (referenceBandButton.isSelected()) {
                referenceBandNameBox.setEnabled(true);
                widthSpinner.setEnabled(false);
                heightSpinner.setEnabled(false);
                resolutionSpinner.setEnabled(false);
                updateReferenceBandName();
            }
        });
        referenceBandNameBox.addActionListener(e -> updateReferenceBandName());
        widthAndHeightButton.addActionListener(e -> {
            if (widthAndHeightButton.isSelected()) {
                referenceBandNameBox.setEnabled(false);
                widthSpinner.setEnabled(true);
                heightSpinner.setEnabled(true);
                resolutionSpinner.setEnabled(false);
                updateTargetWidthAndHeight();
            }
        });
        widthSpinner.addChangeListener(e -> updateTargetWidth());
        heightSpinner.addChangeListener(e -> updateTargetHeight());
        resolutionButton.addActionListener(e -> {
            if (resolutionButton.isSelected()) {
                referenceBandNameBox.setEnabled(false);
                widthSpinner.setEnabled(false);
                heightSpinner.setEnabled(false);
                resolutionSpinner.setEnabled(true);
                updateTargetResolution();
            }
        });
        resolutionSpinner.addChangeListener(e -> updateTargetResolution());

        final JPanel upsamplingMethodPanel = createPropertyPanel(propertySet, "upsamplingMethod", registry);
        final JPanel downsamplingMethodPanel = createPropertyPanel(propertySet, "downsamplingMethod", registry);
        final JPanel flagDownsamplingMethodPanel = createPropertyPanel(propertySet, "flagDownsamplingMethod", registry);
        final JPanel resampleOnPyramidLevelsPanel = createPropertyPanel(propertySet, "resampleOnPyramidLevels", registry);
        final JPanel parametersPanel = new JPanel(tableLayout);
        parametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        parametersPanel.add(defineTargetResolutionPanel);
        parametersPanel.add(upsamplingMethodPanel);
        parametersPanel.add(downsamplingMethodPanel);
        parametersPanel.add(flagDownsamplingMethodPanel);
        parametersPanel.add(resampleOnPyramidLevelsPanel);
        parametersPanel.add(tableLayout.createVerticalSpacer());
        return parametersPanel;
    }

    private void updateReferenceBandName() {
        if (referenceBandNameBox.getSelectedItem() != null) {
            bindingContext.getPropertySet().setValue("referenceBandName", referenceBandNameBox.getSelectedItem().toString());
        } else {
            bindingContext.getPropertySet().setValue("referenceBandName", null);
        }
        bindingContext.getPropertySet().setValue("targetWidth", null);
        bindingContext.getPropertySet().setValue("targetHeight", null);
        bindingContext.getPropertySet().setValue("targetResolution", null);
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
        bindingContext.getPropertySet().setValue("referenceBandName", null);
        bindingContext.getPropertySet().setValue("targetWidth", Integer.parseInt(widthSpinner.getValue().toString()));
        bindingContext.getPropertySet().setValue("targetHeight", Integer.parseInt(heightSpinner.getValue().toString()));
        bindingContext.getPropertySet().setValue("targetResolution", null);
    }

    private void updateTargetResolution() {
        bindingContext.getPropertySet().setValue("referenceBandName", null);
        bindingContext.getPropertySet().setValue("targetWidth", null);
        bindingContext.getPropertySet().setValue("targetHeight", null);
        bindingContext.getPropertySet().setValue("targetResolution", Integer.parseInt(resolutionSpinner.getValue().toString()));
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

    private OperatorMenu createDefaultMenuBar() {
        return new OperatorMenu(getJDialog(),
                                operatorDescriptor,
                                parameterSupport,
                                getAppContext(),
                                getHelpID());
    }

    private void reactToSourceProductChange(Product product) {
        referenceBandNameBox.removeAllItems();
        if (product != null) {
            String[] bandNames = product.getBandNames();
            bindingContext.getPropertySet().getProperty("referenceBandName").
                    getDescriptor().setValueSet(new ValueSet(bandNames));
            referenceBandNameBox.setModel(new DefaultComboBoxModel<>(bandNames));

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
            if (allowToSetWidthAndHeight) {
                targetWidthHeightRatio = product.getSceneRasterWidth() / (double) product.getSceneRasterHeight();
                widthSpinner.setValue(product.getSceneRasterWidth());
                heightSpinner.setValue(product.getSceneRasterHeight());
            } else {
                targetWidthHeightRatio = 1.0;
                widthSpinner.setValue(100);
                heightSpinner.setValue(100);
            }
            final GeoCoding sceneGeoCoding = product.getSceneGeoCoding();
            resolutionButton.setEnabled(sceneGeoCoding != null && sceneGeoCoding instanceof CrsGeoCoding);
        }
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

}
