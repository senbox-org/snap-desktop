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

package org.esa.snap.collocation.visat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.collocation.ResamplingType;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.core.gpf.ui.TargetProductSelector;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.product.SourceProductList;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.BorderLayout;
import java.awt.Insets;

/**
 * Form for geographic collocation dialog.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class CollocationForm extends JPanel {

    private static final String DEFAULT_TARGET_PRODUCT_NAME = "collocate";

    private SourceProductSelector masterProductSelector;
    private SourceProductList slaveProductList;

    private JCheckBox renameMasterComponentsCheckBox;
    private JCheckBox renameSlaveComponentsCheckBox;
    private JTextField masterComponentPatternField;
    private JTextField slaveComponentPatternField;
    private JComboBox<ResamplingType> resamplingComboBox;
    private DefaultComboBoxModel<ResamplingType> resamplingComboBoxModel;
    private TargetProductSelector targetProductSelector;
    private BindingContext sbc;

    public CollocationForm(PropertySet propertySet, TargetProductSelector targetProductSelector, AppContext appContext) {
        this.targetProductSelector = targetProductSelector;
        masterProductSelector = new SourceProductSelector(appContext, "Master (pixel values are conserved):");


        ListDataListener changeListener = new ListDataListener() {

            @Override
            public void contentsChanged(ListDataEvent event) {
                final Product[] sourceProducts = slaveProductList.getSourceProducts();
                propertySet.setValue("sourceProducts", sourceProducts);
            }

            @Override
            public void intervalAdded(ListDataEvent e) {
                contentsChanged(e);
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                contentsChanged(e);
            }
        };

        propertySet.addProperty(createTransientProperty("sourceProducts", Product[].class));

        slaveProductList = new SourceProductList(appContext);
        slaveProductList.addChangeListener(changeListener);
        slaveProductList.setXAxis(false);

        renameMasterComponentsCheckBox = new JCheckBox("Rename master components:");
        renameSlaveComponentsCheckBox = new JCheckBox("Rename slave components:");
        masterComponentPatternField = new JTextField();
        slaveComponentPatternField = new JTextField();
        resamplingComboBoxModel = new DefaultComboBoxModel<>(ResamplingType.values());
        resamplingComboBox = new JComboBox<>(resamplingComboBoxModel);

        ListDataListener myListener = new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                contentsChanged(e);
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                contentsChanged(e);
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                boolean validPixelExpressionUsed = false;
                for (Product product : slaveProductList.getSourceProducts()){
                    if(isValidPixelExpressionUsed(product)) {
                        validPixelExpressionUsed = true;
                        break;
                    }
                }
                adaptResamplingComboBoxModel(resamplingComboBoxModel, validPixelExpressionUsed);
            }
        };
        slaveProductList.addChangeListener(myListener);

        createComponents();
        sbc = new BindingContext(propertySet);
        bindComponents(propertySet);
    }

    public void prepareShow() {
        masterProductSelector.initProducts();
        if (masterProductSelector.getProductCount() > 0) {
            masterProductSelector.setSelectedIndex(0);
        }
    }

    private static Property createTransientProperty(String name, Class type) {
        final DefaultPropertyAccessor defaultAccessor = new DefaultPropertyAccessor();
        final PropertyDescriptor descriptor = new PropertyDescriptor(name, type);
        descriptor.setTransient(true);
        descriptor.setDefaultConverter();
        return new Property(descriptor, defaultAccessor);
    }

    public void prepareHide() {
        masterProductSelector.releaseProducts();
    }

    Product getMasterProduct() {
        return masterProductSelector.getSelectedProduct();
    }

    String[] getSourceProductPaths() {
        final Property property = sbc.getPropertySet().getProperty("sourceProductPaths");
        if (property != null) {
            return (String[]) property.getValue();
        }
        return null;
    }

    Product[] getSlaveProducts() {
        return slaveProductList.getSourceProducts();
    }


    private void createComponents() {
        //setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setTablePadding(3, 3);
        setLayout(tableLayout);
        tableLayout.setRowWeightY(0, 1.0);
        setLayout(tableLayout);

        add(createSourceProductPanel());
        add(createTargetProductPanel());
        add(createRenamingPanel());
        add(createResamplingPanel());
    }

    private void bindComponents(PropertySet propertySet) {
        //final BindingContext sbc = new BindingContext(propertySet);
        sbc.bind("renameMasterComponents", renameMasterComponentsCheckBox);
        sbc.bind("renameSlaveComponents", renameSlaveComponentsCheckBox);
        sbc.bind("masterComponentPattern", masterComponentPatternField);
        sbc.bind("slaveComponentPattern", slaveComponentPatternField);
        sbc.bind("resamplingType", resamplingComboBox);
        sbc.bind("sourceProductPaths", slaveProductList);
        sbc.bindEnabledState("masterComponentPattern", true, "renameMasterComponents", true);
        sbc.bindEnabledState("slaveComponentPattern", true, "renameSlaveComponents", true);
    }

    private JPanel createSourceProductPanel() {
        final JPanel masterPanel = new JPanel(new BorderLayout(3, 3));
        masterPanel.add(masterProductSelector.getProductNameLabel(), BorderLayout.NORTH);
        masterProductSelector.getProductNameComboBox().setPrototypeDisplayValue(
                "MER_RR__1PPBCM20030730_071000_000003972018_00321_07389_0000.N1");
        masterPanel.add(masterProductSelector.getProductNameComboBox(), BorderLayout.CENTER);
        masterPanel.add(masterProductSelector.getProductFileChooserButton(), BorderLayout.EAST);

        JComponent[] panels = slaveProductList.getComponents();
        JPanel listPanel = new JPanel(new BorderLayout());

        listPanel.add(panels[0], BorderLayout.CENTER);

        BorderLayout layout1 = new BorderLayout();
        final JPanel slavePanel = new JPanel(layout1);
        slavePanel.setBorder(BorderFactory.createTitledBorder("Slave Products"));
        slavePanel.add(listPanel, BorderLayout.CENTER);
        slavePanel.add(panels[1], BorderLayout.EAST);



        final TableLayout layout = new TableLayout(1);
        layout.setRowWeightX(0, 1.0);
        layout.setRowWeightX(1, 1.0);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setCellPadding(0, 0, new Insets(3, 3, 3, 3));
        layout.setCellPadding(1, 0, new Insets(3, 3, 3, 3));

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Source Products"));
        panel.add(masterPanel);
        panel.add(slavePanel);

        return panel;
    }

    private JPanel createTargetProductPanel() {
        targetProductSelector.getModel().setProductName(DEFAULT_TARGET_PRODUCT_NAME);
        return targetProductSelector.createDefaultPanel();
    }

    private JPanel createRenamingPanel() {
        final TableLayout layout = new TableLayout(2);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);
        layout.setCellPadding(0, 0, new Insets(3, 3, 3, 3));
        layout.setCellPadding(1, 0, new Insets(3, 3, 3, 3));

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Renaming of Source Product Components"));
        panel.add(renameMasterComponentsCheckBox);
        panel.add(masterComponentPatternField);
        panel.add(renameSlaveComponentsCheckBox);
        panel.add(slaveComponentPatternField);

        return panel;
    }

    private JPanel createResamplingPanel() {
        final TableLayout layout = new TableLayout(3);
        layout.setTableAnchor(TableLayout.Anchor.LINE_START);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 0.0);
        layout.setColumnWeightX(2, 1.0);
        layout.setCellPadding(0, 0, new Insets(3, 3, 3, 3));

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Resampling"));
        panel.add(new JLabel("Method:"));
        panel.add(resamplingComboBox);
        panel.add(new JLabel());

        return panel;
    }

    static void adaptResamplingComboBoxModel(DefaultComboBoxModel<ResamplingType> comboBoxModel, boolean isValidPixelExpressionUsed) {
        if (isValidPixelExpressionUsed) {
            if (comboBoxModel.getSize() == 5) {
                comboBoxModel.removeElement(ResamplingType.BICUBIC_CONVOLUTION);
                comboBoxModel.removeElement(ResamplingType.BISINC_CONVOLUTION);
                comboBoxModel.removeElement(ResamplingType.CUBIC_CONVOLUTION);
                comboBoxModel.removeElement(ResamplingType.BILINEAR_INTERPOLATION);
                comboBoxModel.setSelectedItem(ResamplingType.NEAREST_NEIGHBOUR);
            }
        } else {
            if (comboBoxModel.getSize() == 1) {
                comboBoxModel.addElement(ResamplingType.BILINEAR_INTERPOLATION);
                comboBoxModel.addElement(ResamplingType.CUBIC_CONVOLUTION);
                comboBoxModel.addElement(ResamplingType.BICUBIC_CONVOLUTION);
                comboBoxModel.addElement(ResamplingType.BISINC_CONVOLUTION);
            }
        }
    }

    static  boolean isValidPixelExpressionUsed(Product product) {
        if (product != null) {
            for (final Band band : product.getBands()) {
                final String expression = band.getValidPixelExpression();
                if (expression != null && !expression.trim().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
