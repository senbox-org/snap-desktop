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

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
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

    private final SourceProductSelector referenceProductSelector;
    private final SourceProductList secondaryProductList;

    private final JCheckBox copySecondaryMetadata;
    private final JCheckBox renameReferenceComponentsCheckBox;
    private final JCheckBox renameSecondaryComponentsCheckBox;
    private final JTextField referenceComponentPatternField;
    private final JTextField secondaryComponentPatternField;
    private final JComboBox<ResamplingType> resamplingComboBox;
    private final DefaultComboBoxModel<ResamplingType> resamplingComboBoxModel;
    private final TargetProductSelector targetProductSelector;
    private final BindingContext sbc;

    public CollocationForm(PropertySet propertySet, TargetProductSelector targetProductSelector, AppContext appContext) {
        this.targetProductSelector = targetProductSelector;
        referenceProductSelector = new SourceProductSelector(appContext, "Reference (pixel values are conserved):");


        ListDataListener changeListener = new ListDataListener() {

            @Override
            public void contentsChanged(ListDataEvent event) {
                final Product[] sourceProducts = secondaryProductList.getSourceProducts();
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

        secondaryProductList = new SourceProductList(appContext);
        secondaryProductList.addChangeListener(changeListener);
        secondaryProductList.setXAxis(false);

        copySecondaryMetadata = new JCheckBox("Include metadata of secondary in result");
        renameReferenceComponentsCheckBox = new JCheckBox("Rename reference components:");
        renameSecondaryComponentsCheckBox = new JCheckBox("Rename secondary components:");
        referenceComponentPatternField = new JTextField();
        secondaryComponentPatternField = new JTextField();
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
                for (Product product : secondaryProductList.getSourceProducts()){
                    if(isValidPixelExpressionUsed(product)) {
                        validPixelExpressionUsed = true;
                        break;
                    }
                }
                adaptResamplingComboBoxModel(resamplingComboBoxModel, validPixelExpressionUsed);
            }
        };
        secondaryProductList.addChangeListener(myListener);

        createComponents();
        sbc = new BindingContext(propertySet);
        bindComponents(propertySet);
    }

    public void prepareShow() {
        referenceProductSelector.initProducts();
        if (referenceProductSelector.getProductCount() > 0) {
            referenceProductSelector.setSelectedIndex(0);
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
        referenceProductSelector.releaseProducts();
    }

    Product getMasterProduct() {
        return referenceProductSelector.getSelectedProduct();
    }

    String[] getSourceProductPaths() {
        final Property property = sbc.getPropertySet().getProperty("sourceProductPaths");
        if (property != null) {
            return (String[]) property.getValue();
        }
        return null;
    }

    Product[] getSlaveProducts() {
        return secondaryProductList.getSourceProducts();
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
        sbc.bind("copySecondaryMetadata", copySecondaryMetadata);
        sbc.bind("renameReferenceComponents", renameReferenceComponentsCheckBox);
        sbc.bind("renameSecondaryComponents", renameSecondaryComponentsCheckBox);
        sbc.bind("referenceComponentPattern", referenceComponentPatternField);
        sbc.bind("secondaryComponentPattern", secondaryComponentPatternField);
        sbc.bind("resamplingType", resamplingComboBox);
        sbc.bind("sourceProductPaths", secondaryProductList);
        sbc.bindEnabledState("referenceComponentPattern", true, "renameReferenceComponents", true);
        sbc.bindEnabledState("secondaryComponentPattern", true, "renameSecondaryComponents", true);
    }

    private JPanel createSourceProductPanel() {
        final JPanel masterPanel = new JPanel(new BorderLayout(3, 3));
        masterPanel.add(referenceProductSelector.getProductNameLabel(), BorderLayout.NORTH);
        referenceProductSelector.getProductNameComboBox().setPrototypeDisplayValue(
                "MER_RR__1PPBCM20030730_071000_000003972018_00321_07389_0000.N1");
        masterPanel.add(referenceProductSelector.getProductNameComboBox(), BorderLayout.CENTER);
        masterPanel.add(referenceProductSelector.getProductFileChooserButton(), BorderLayout.EAST);

        JComponent[] panels = secondaryProductList.getComponents();
        JPanel listPanel = new JPanel(new BorderLayout());

        listPanel.add(panels[0], BorderLayout.CENTER);

        BorderLayout layout1 = new BorderLayout();
        final JPanel slavePanel = new JPanel(layout1);
        slavePanel.setBorder(BorderFactory.createTitledBorder("Secondary Products"));
        slavePanel.add(listPanel, BorderLayout.CENTER);
        slavePanel.add(panels[1], BorderLayout.EAST);
        slavePanel.add(copySecondaryMetadata, BorderLayout.SOUTH);

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
        panel.add(renameReferenceComponentsCheckBox);
        panel.add(referenceComponentPatternField);
        panel.add(renameSecondaryComponentsCheckBox);
        panel.add(secondaryComponentPatternField);

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
