/*
 *
 *  * Copyright (C) 2015 CS SI
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */
package org.esa.snap.ui.tooladapter.dialogs;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.binding.PropertyPane;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterConstants;
import org.esa.snap.framework.gpf.ui.DefaultIOParametersPanel;
import org.esa.snap.framework.gpf.ui.SourceProductSelector;
import org.esa.snap.framework.gpf.ui.TargetProductSelector;
import org.esa.snap.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.snap.framework.ui.AppContext;
import org.esa.snap.util.io.FileUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Form for displaying execution parameters.
 * This form is part of <code>ToolAdapterExecutionDialog</code>.
 *
 * @author Ramona Manda
 */
class ToolExecutionForm extends JTabbedPane {
    private AppContext appContext;
    private ToolAdapterOperatorDescriptor operatorDescriptor;
    private PropertySet propertySet;
    private TargetProductSelector targetProductSelector;
    private DefaultIOParametersPanel ioParamPanel;
    private String fileExtension;

    public ToolExecutionForm(AppContext appContext, ToolAdapterOperatorDescriptor descriptor, PropertySet propertySet,
                             TargetProductSelector targetProductSelector) {
        this.appContext = appContext;
        this.operatorDescriptor = descriptor;
        this.propertySet = propertySet;
        this.targetProductSelector = targetProductSelector;

        //before executing, the sourceProduct and sourceProductFile must be removed from the list, since they cannot be edited
        Property sourceProperty = this.propertySet.getProperty(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_FILE);
        if(sourceProperty != null) {
            this.propertySet.removeProperty(sourceProperty);
        }
        sourceProperty = this.propertySet.getProperty(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID);
        if(sourceProperty != null) {
            this.propertySet.removeProperty(sourceProperty);
        }

        //initialise the target product's directory to the working directory
        final TargetProductSelectorModel targetProductSelectorModel = targetProductSelector.getModel();
        targetProductSelectorModel.setProductDir(this.operatorDescriptor.getWorkingDir());

        ioParamPanel = createIOParamTab();
        addTab("I/O Parameters", ioParamPanel);
        addTab("Processing Parameters", createProcessingParamTab());
        updateTargetProductFields();
    }

    /**
     * Method called before actually showing the form, in which additional
     * initialisation may occur.
     */
    public void prepareShow() {
        ioParamPanel.initSourceProductSelectors();
    }

    /**
     * Method called before hiding the form, in which additional
     * cleanup may be performed.
     */
    public void prepareHide() {
        ioParamPanel.releaseSourceProductSelectors();
    }

    /**
     * Fetches the list of products selected in UI
     *
     * @return  The list of selected products to be used as input source
     */
    public Product[] getSourceProducts() {
        List<Product> sourceProducts = new ArrayList<>();
        ArrayList<SourceProductSelector> sourceProductSelectorList = ioParamPanel.getSourceProductSelectorList();
        sourceProducts.addAll(sourceProductSelectorList.stream().map(SourceProductSelector::getSelectedProduct).collect(Collectors.toList()));
        return sourceProducts.toArray(new Product[sourceProducts.size()]);
    }

    /**
     * Gets the target (destination) product file
     *
     * @return  The target product file
     */
    public File getTargetProductFile() {
        return targetProductSelector.getModel().getProductFile();
    }

    private DefaultIOParametersPanel createIOParamTab() {
        final DefaultIOParametersPanel ioPanel = new DefaultIOParametersPanel(appContext, operatorDescriptor,
                targetProductSelector);
        final ArrayList<SourceProductSelector> sourceProductSelectorList = ioPanel.getSourceProductSelectorList();
        if (!sourceProductSelectorList.isEmpty()) {
            final SourceProductSelector sourceProductSelector = sourceProductSelectorList.get(0);
            sourceProductSelector.addSelectionChangeListener(new SourceProductChangeListener());
        }
        return ioPanel;
    }

    private JScrollPane createProcessingParamTab() {

        PropertyPane parametersPane = new PropertyPane(propertySet);
        final JPanel parametersPanel = parametersPane.createPanel();
        parametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        return new JScrollPane(parametersPanel);
    }

    private void updateTargetProductFields() {
        File file = new File(propertySet.getProperty(ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE).getValueAsText());
        String productName = FileUtils.getFilenameWithoutExtension(file);
        if (fileExtension == null) {
            fileExtension = FileUtils.getExtension(file);
        }
        TargetProductSelectorModel model = targetProductSelector.getModel();
        model.setProductName(productName);
        model.setSaveToFileSelected(false);
        targetProductSelector.getProductDirTextField().setEnabled(false);
    }

    private class SourceProductChangeListener extends AbstractSelectionChangeListener {

        private static final String TARGET_PRODUCT_NAME_SUFFIX = "_processed";

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            String productName = "";
            final Product selectedProduct = (Product) event.getSelection().getSelectedValue();
            if (selectedProduct != null) {
                productName = FileUtils.getFilenameWithoutExtension(selectedProduct.getName());
            }
            final TargetProductSelectorModel targetProductSelectorModel = targetProductSelector.getModel();
            productName += TARGET_PRODUCT_NAME_SUFFIX;
            targetProductSelectorModel.setProductName(productName);
            Property targetProperty = propertySet.getProperty(ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE);
            Object value = targetProperty.getValue();
            File oldValue = value instanceof File ? (File) value : new File((String) value);
            propertySet.setValue(ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE, new File(oldValue.getParentFile().getAbsolutePath(), productName + fileExtension));
        }
    }

}
