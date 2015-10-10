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

import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.engine_utilities.db.CommonReaders;
import org.esa.snap.ui.AppContext;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Reader OperatorUI
 */
public class SourceUI extends BaseOperatorUI {

    SourceProductSelector sourceProductSelector = null;
    private JComboBox<String> formatNameComboBox;

    private static final String FILE_PARAMETER = "file";
    private static final String FORMAT_PARAMETER = "formatName";
    private static final String ANY_FORMAT = "Any Format";

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        paramMap = parameterMap;
        sourceProductSelector = new SourceProductSelector(appContext);

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(3, 3);

        final JPanel sourcePanel = sourceProductSelector.createDefaultPanel();

        formatNameComboBox = new JComboBox<>();
        formatNameComboBox.setToolTipText("Select 'Any Format' to let SNAP decide");

        final JPanel formatPanel = new JPanel();
        formatPanel.add(new JLabel("Data Format:      "));
        formatPanel.add(formatNameComboBox);
        sourcePanel.add(formatPanel);

        final JPanel ioParametersPanel = new JPanel(tableLayout);
        ioParametersPanel.add(sourcePanel);

        ioParametersPanel.add(tableLayout.createVerticalSpacer());

        sourceProductSelector.initProducts();
        sourceProductSelector.addSelectionChangeListener(new SourceSelectionChangeListener());

        final Product selectedProduct = sourceProductSelector.getSelectedProduct();
        if(selectedProduct != null) {
            updateFormatNamesCombo(selectedProduct.getFileLocation());
        }

        initParameters();

        return ioParametersPanel;
    }

    private void updateFormatNamesCombo(final File file) {
        if(file == null) {
            return;
        }
        final List<String> formatNameList = getFormatsForFile(file);

        formatNameComboBox.removeAllItems();
        for(String format : formatNameList) {
            formatNameComboBox.addItem(format);
        }
    }

    private static List<String> getFormatsForFile(final File file) {
        final Iterator<ProductReaderPlugIn> allReaderPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
        final List<String> formatNameList = new ArrayList<>();

        while (allReaderPlugIns.hasNext()) {
            ProductReaderPlugIn reader = allReaderPlugIns.next();
            String[] formatNames = reader.getFormatNames();
            for (String formatName : formatNames) {
                if(file == null || reader.getDecodeQualification(file) != DecodeQualification.UNABLE &&
                        !formatNameList.contains(formatName)) {
                    formatNameList.add(formatName);
                }
            }
        }
        formatNameList.sort(String::compareTo);
        formatNameList.add(0, ANY_FORMAT);

        return formatNameList;
    }

    @Override
    public void initParameters() {
        assert (paramMap != null);
        final Object fileValue = paramMap.get(FILE_PARAMETER);
        if (fileValue != null) {
            try {
                final Product product = CommonReaders.readProduct((File) fileValue);
                sourceProductSelector.setSelectedProduct(product);
            } catch (IOException e) {
                // do nothing
            }
        }
        final Object formatValue = paramMap.get(FORMAT_PARAMETER);
        if (formatValue != null) {
            formatNameComboBox.setSelectedItem(formatValue);
        } else {
            formatNameComboBox.setSelectedItem(ANY_FORMAT);
        }
    }

    @Override
    public UIValidation validateParameters() {
        if (sourceProductSelector != null) {
            if (sourceProductSelector.getSelectedProduct() == null)
                return new UIValidation(UIValidation.State.ERROR, "Source product not selected");
        }
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {
        if (sourceProductSelector != null) {
            final Product prod = sourceProductSelector.getSelectedProduct();
            if (prod != null && prod.getFileLocation() != null) {
                paramMap.put(FILE_PARAMETER, prod.getFileLocation());
            }
        }
        String selectedFormat = (String)formatNameComboBox.getSelectedItem();
        if(selectedFormat.equals(ANY_FORMAT)) {
            selectedFormat = null;
        }
        paramMap.put(FORMAT_PARAMETER, selectedFormat);
    }

    public void setSourceProduct(final Product product) {
        if (sourceProductSelector != null) {
            sourceProductSelector.setSelectedProduct(product);
            if (product != null && product.getFileLocation() != null) {
                paramMap.put(FILE_PARAMETER, product.getFileLocation());
            }
        }
    }

    private class SourceSelectionChangeListener implements SelectionChangeListener {

        public void selectionChanged(SelectionChangeEvent event) {
            final Object selected = event.getSelection().getSelectedValue();
            if (selected != null && selected instanceof Product) {
                Product product = (Product) selected;
                if (product.getFileLocation() != null) {
                    updateFormatNamesCombo(product.getFileLocation());
                }
            }
        }

        public void selectionContextChanged(SelectionChangeEvent event) {
        }
    }
}
