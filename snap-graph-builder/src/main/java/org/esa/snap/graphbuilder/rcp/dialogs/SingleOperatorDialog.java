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
package org.esa.snap.graphbuilder.rcp.dialogs;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.experimental.Output;
import org.esa.snap.core.gpf.internal.OperatorExecutor;
import org.esa.snap.core.gpf.internal.OperatorProductReader;
import org.esa.snap.core.gpf.internal.RasterDataNodeValues;
import org.esa.snap.core.gpf.ui.DefaultIOParametersPanel;
import org.esa.snap.core.gpf.ui.OperatorMenu;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.core.gpf.ui.SingleTargetProductDialog;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.core.gpf.ui.TargetProductSelectorModel;
import org.esa.snap.engine_utilities.db.CommonReaders;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIRegistry;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.actions.file.SaveProductAsAction;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.engine_utilities.util.MemUtils;
import org.esa.snap.engine_utilities.util.ProductFunctions;

import javax.media.jai.JAI;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 */
public class SingleOperatorDialog extends SingleTargetProductDialog {

    private final OperatorUI opUI;
    private JLabel statusLabel;
    private JComponent parametersPanel;

    private final String operatorName;
    private final OperatorDescriptor operatorDescriptor;
    private DefaultIOParametersPanel ioParametersPanel;
    private final OperatorParameterSupport parameterSupport;
    private final BindingContext bindingContext;

    private JTabbedPane form;
    private PropertyDescriptor[] rasterDataNodeTypeProperties;
    private String targetProductNameSuffix;
    private ProductChangedHandler productChangedHandler;

    public SingleOperatorDialog(String operatorName, AppContext appContext, String title, String helpID) {
        super(appContext, title, ID_APPLY_CLOSE, helpID);
        this.operatorName = operatorName;
        targetProductNameSuffix = "";

        OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalArgumentException("No SPI found for operator name '" + operatorName + "'");
        }

        operatorDescriptor = operatorSpi.getOperatorDescriptor();
        ioParametersPanel = new DefaultIOParametersPanel(getAppContext(), operatorDescriptor, getTargetProductSelector());

        parameterSupport = new OperatorParameterSupport(operatorDescriptor);
        final ArrayList<SourceProductSelector> sourceProductSelectorList = ioParametersPanel.getSourceProductSelectorList();
        final PropertySet propertySet = parameterSupport.getPropertySet();
        bindingContext = new BindingContext(propertySet);

        if (propertySet.getProperties().length > 0) {
            if (!sourceProductSelectorList.isEmpty()) {
                Property[] properties = propertySet.getProperties();
                List<PropertyDescriptor> rdnTypeProperties = new ArrayList<>(properties.length);
                for (Property property : properties) {
                    PropertyDescriptor parameterDescriptor = property.getDescriptor();
                    if (parameterDescriptor.getAttribute(RasterDataNodeValues.ATTRIBUTE_NAME) != null) {
                        rdnTypeProperties.add(parameterDescriptor);
                    }
                }
                rasterDataNodeTypeProperties = rdnTypeProperties.toArray(
                        new PropertyDescriptor[rdnTypeProperties.size()]);
            }
        }
        productChangedHandler = new ProductChangedHandler();
        if (!sourceProductSelectorList.isEmpty()) {
            sourceProductSelectorList.get(0).addSelectionChangeListener(productChangedHandler);
        }

        opUI = OperatorUIRegistry.CreateOperatorUI(operatorName);

        addParameters();

        getJDialog().setMinimumSize(new Dimension(450, 450));

        statusLabel = new JLabel("");
        statusLabel.setForeground(new Color(255, 0, 0));
        this.getJDialog().getContentPane().add(statusLabel, BorderLayout.NORTH);
    }

    @Override
    public int show() {
        ioParametersPanel.initSourceProductSelectors();
        if (form == null) {
            initForm();
            if (getJDialog().getJMenuBar() == null) {
                final OperatorMenu operatorMenu = createDefaultMenuBar();
                getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
            }
        }
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
    protected Product createTargetProduct() throws Exception {
        if (validateUI()) {
            MemUtils.freeAllMemory();

            opUI.updateParameters();

            final HashMap<String, Product> sourceProducts = ioParametersPanel.createSourceProductsMap();
            return GPF.createProduct(operatorName, parameterSupport.getParameterMap(), sourceProducts);
        }
        return null;
    }

    public String getTargetProductNameSuffix() {
        return targetProductNameSuffix;
    }

    public void setTargetProductNameSuffix(String suffix) {
        targetProductNameSuffix = suffix;
    }

    public BindingContext getBindingContext() {
        return bindingContext;
    }

    private void initForm() {
        form = new JTabbedPane();
        form.add("I/O Parameters", ioParametersPanel);

        //if (bindingContext.getPropertySet().getProperties().length > 0) {
        //    final PropertyPane parametersPane = new PropertyPane(bindingContext);
        //    final JPanel parametersPanel = parametersPane.createPanel();
        //    parametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        //    form.add("Processing Parameters", new JScrollPane(parametersPanel));
        //    updateSourceProduct();
        //}
        parametersPanel = opUI.CreateOpTab(operatorName, parameterSupport.getParameterMap(), appContext);
        parametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        form.add("Processing Parameters", new JScrollPane(parametersPanel));
    }

    private OperatorMenu createDefaultMenuBar() {
        return new OperatorMenu(getJDialog(),
                operatorDescriptor,
                parameterSupport,
                getAppContext(),
                getHelpID());
    }

    private void updateSourceProduct() {
        try {
            Property property = bindingContext.getPropertySet().getProperty(UIUtils.PROPERTY_SOURCE_PRODUCT);
            if (property != null) {
                property.setValue(productChangedHandler.currentProduct);
            }
        } catch (ValidationException e) {
            throw new IllegalStateException("Property '" + UIUtils.PROPERTY_SOURCE_PRODUCT + "' must be of type " + Product.class + ".", e);
        }
    }

    private void addParameters() {
        final PropertySet propertySet = parameterSupport.getPropertySet();
        final List<SourceProductSelector> sourceProductSelectorList = ioParametersPanel.getSourceProductSelectorList();

        if (sourceProductSelectorList.isEmpty()) {
            SnapDialogs.showError("SourceProduct @Parameter not found in operator");
        } else {

            sourceProductSelectorList.get(0).addSelectionChangeListener(new AbstractSelectionChangeListener() {

                @Override
                public void selectionChanged(SelectionChangeEvent event) {
                    final Product selectedProduct = (Product) event.getSelection().getSelectedValue();
                    if (selectedProduct != null) { //&& form != null) {
                        final TargetProductSelectorModel targetProductSelectorModel = getTargetProductSelector().getModel();
                        targetProductSelectorModel.setProductName(selectedProduct.getName() + getTargetProductNameSuffix());
                        opUI.setSourceProducts(new Product[]{selectedProduct});
                    }
                }
            });
        }

        if (propertySet.getProperties().length > 0) {
            if (!sourceProductSelectorList.isEmpty()) {
                Property[] properties = propertySet.getProperties();
                List<PropertyDescriptor> rdnTypeProperties = new ArrayList<>(properties.length);
                for (Property property : properties) {
                    PropertyDescriptor parameterDescriptor = property.getDescriptor();
                    if (parameterDescriptor.getAttribute(RasterDataNodeValues.ATTRIBUTE_NAME) != null) {
                        rdnTypeProperties.add(parameterDescriptor);
                    }
                }
                rasterDataNodeTypeProperties = rdnTypeProperties.toArray(
                        new PropertyDescriptor[rdnTypeProperties.size()]);
            }
        }
    }

    private boolean validateUI() {
        final UIValidation validation = opUI.validateParameters();
        if (validation.getState() == UIValidation.State.WARNING) {
            final String msg = "Warning: " + validation.getMsg() +
                    "\n\nWould you like to continue?";
            return SnapDialogs.requestDecision("Warning", msg, false, null) == SnapDialogs.Answer.YES;
        } else if (validation.getState() == UIValidation.State.ERROR) {
            final String msg = "Error: " + validation.getMsg();
            SnapDialogs.showError(msg);
            return false;
        }
        return true;
    }

    @Override
    protected void onApply() {
        if (!canApply()) {
            return;
        }

        String productDir = targetProductSelector.getModel().getProductDir().getAbsolutePath();
        SnapApp.getDefault().getPreferences().put(SaveProductAsAction.PREFERENCES_KEY_LAST_PRODUCT_DIR, productDir);

        Product targetProduct = null;
        try {
            targetProduct = createTargetProduct();
            if (targetProduct == null) {
                //throw new NullPointerException("Target product is null.");
            }
        } catch (Throwable t) {
            handleInitialisationError(t);
        }
        if (targetProduct == null) {
            return;
        }

        targetProduct.setName(targetProductSelector.getModel().getProductName());
        if (targetProductSelector.getModel().isSaveToFileSelected()) {
            targetProduct.setFileLocation(targetProductSelector.getModel().getProductFile());
            final ProgressMonitorSwingWorker worker = new ProductWriterWorker(targetProduct);
            //worker.executeWithBlocking();
            worker.execute();
        } else if (targetProductSelector.getModel().isOpenInAppSelected()) {
            appContext.getProductManager().addProduct(targetProduct);
            showOpenInAppInfo();
        }
    }

    private class ProductWriterWorker extends ProgressMonitorSwingWorker<Product, Object> {

        private final Product targetProduct;
        private Date executeStartTime;

        private ProductWriterWorker(Product targetProduct) {
            super(getJDialog(), "Writing Target Product");
            this.targetProduct = targetProduct;
        }

        @Override
        protected Product doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
            final TargetProductSelectorModel model = getTargetProductSelector().getModel();
            pm.beginTask("Writing...", model.isOpenInAppSelected() ? 100 : 95);

            Product product = null;
            try {
                // free cache	// NESTMOD
                JAI.getDefaultInstance().getTileCache().flush();
                System.gc();

                executeStartTime = Calendar.getInstance().getTime();
                long t0 = System.currentTimeMillis();
                Operator operator = null;
                if (targetProduct.getProductReader() instanceof OperatorProductReader) {
                    final OperatorProductReader opReader = (OperatorProductReader) targetProduct.getProductReader();
                    if (opReader.getOperatorContext().getOperator() instanceof Output) {
                        operator = opReader.getOperatorContext().getOperator();
                    }
                }
                if (operator == null) {
                    WriteOp writeOp = new WriteOp(targetProduct, model.getProductFile(), model.getFormatName());
                    writeOp.setDeleteOutputOnFailure(true);
                    writeOp.setWriteEntireTileRows(true);
                    writeOp.setClearCacheAfterRowWrite(false);
                    operator = writeOp;
                }
                final OperatorExecutor executor = OperatorExecutor.create(operator);
                executor.execute(SubProgressMonitor.create(pm, 95));

                File targetFile = model.getProductFile();
                if (model.isOpenInAppSelected() && targetFile.exists()) {
                    product = CommonReaders.readProduct(targetFile);
                    if (product == null) {
                        product = targetProduct; // todo - check - this cannot be ok!!! (nf)
                    }
                    pm.worked(5);
                }
            } finally {
                // free cache
                JAI.getDefaultInstance().getTileCache().flush();
                System.gc();

                pm.done();

                if (product != targetProduct) {
                    targetProduct.dispose();
                }
            }
            return product;
        }

        @Override
        protected void done() {
            final TargetProductSelectorModel model = getTargetProductSelector().getModel();
            try {
                final Product targetProduct = get();
                if(targetProduct != null) {
                    final Date now = Calendar.getInstance().getTime();
                    final long totalSeconds = (now.getTime() - executeStartTime.getTime()) / 1000;
                    final long totalBytes = ProductFunctions.getRawStorageSize(targetProduct);
                    final long totalPixels = ProductFunctions.getTotalPixels(targetProduct);

                    statusLabel.setText(ProductFunctions.getProcessingStatistics(totalSeconds, totalBytes, totalPixels));

                    if (model.isOpenInAppSelected()) {
                        appContext.getProductManager().addProduct(targetProduct);
                        //showSaveAndOpenInAppInfo(saveTime);
                    } else {
                        //showSaveInfo(saveTime);
                    }
                }
            } catch (InterruptedException e) {
                // ignore
            } catch (ExecutionException e) {
                handleProcessingError(e.getCause());
            } catch (Throwable t) {
                handleProcessingError(t);
            }
        }
    }

    private class ProductChangedHandler extends AbstractSelectionChangeListener implements ProductNodeListener {

        private Product currentProduct;

        public void releaseProduct() {
            if (currentProduct != null) {
                currentProduct.removeProductNodeListener(this);
                currentProduct = null;
                updateSourceProduct();
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
                    updateTargetProductName();
                    updateValueSets(currentProduct);
                    updateSourceProduct();
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
            targetProductSelectorModel.setProductName(productName + getTargetProductNameSuffix());
        }

        private void handleProductNodeEvent() {
            updateValueSets(currentProduct);
        }

        private void updateValueSets(Product product) {
            if (rasterDataNodeTypeProperties != null) {
                for (PropertyDescriptor propertyDescriptor : rasterDataNodeTypeProperties) {
                    updateValueSet(propertyDescriptor, product);
                }
            }
        }
    }

    private static void updateValueSet(PropertyDescriptor propertyDescriptor, Product product) {
        String[] values = new String[0];
        if (product != null) {
            Object object = propertyDescriptor.getAttribute(RasterDataNodeValues.ATTRIBUTE_NAME);
            if (object != null) {
                @SuppressWarnings("unchecked")
                Class<? extends RasterDataNode> rasterDataNodeType = (Class<? extends RasterDataNode>) object;
                boolean includeEmptyValue = !propertyDescriptor.isNotNull() && !propertyDescriptor.isNotEmpty() &&
                        !propertyDescriptor.getType().isArray();
                values = RasterDataNodeValues.getNames(product, rasterDataNodeType, includeEmptyValue);
            }
        }
        propertyDescriptor.setValueSet(new ValueSet(values));
    }
}
