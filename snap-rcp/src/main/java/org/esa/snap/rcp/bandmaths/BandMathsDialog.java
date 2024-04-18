/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.rcp.bandmaths;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.internal.CheckBoxEditor;
import com.bc.ceres.swing.binding.internal.NumericEditor;
import com.bc.ceres.swing.binding.internal.SingleSelectionEditor;
import com.bc.ceres.swing.binding.internal.TextComponentAdapter;
import com.bc.ceres.swing.binding.internal.TextFieldEditor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.ProductNodeList;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.dataop.barithm.RasterDataSymbol;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.util.PreferencesPropertyMap;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.window.OpenImageViewAction;
import org.esa.snap.rcp.nodes.UndoableProductNodeInsertion;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.product.ProductExpressionPane;
import org.openide.awt.UndoRedo;
import org.openide.util.NbBundle;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import static org.esa.snap.rcp.SnapApp.SelectionSourceHint.EXPLORER;

@NbBundle.Messages({
        "CTL_BandMathsDialog_Title=Band Maths",
        "CTL_BandMathsDialog_ErrBandNotCreated=The band could not be created.\nAn expression parse error occurred:\n",
        "CTL_BandMathsDialog_ErrExpressionNotValid=Please check the band maths expression you have entered.\nIt is not valid.",
        "CTL_BandMathsDialog_ErrBandCannotBeReferenced=You cannot reference the target band ''{0}'' within the expression.",
        "CTL_BandMathsDialog_LblExpression=Band maths expression:",
})
class BandMathsDialog extends ModalDialog {

    static final String PREF_KEY_AUTO_SHOW_NEW_BANDS = "BandMaths.autoShowNewBands";
    static final String PREF_KEY_LAST_EXPRESSION_PATH = "BandMaths.lastExpressionPath";

    private static final String PROPERTY_NAME_PRODUCT = "productName";
    private static final String PROPERTY_NAME_EXPRESSION = "expression";
    private static final String PROPERTY_NAME_NO_DATA_VALUE = "noDataValue";
    private static final String PROPERTY_NAME_NO_DATA_VALUE_USED = "noDataValueUsed";
    private static final String PROPERTY_NAME_SAVE_EXPRESSION_ONLY = "saveExpressionOnly";
    private static final String PROPERTY_NAME_GENERATE_UNCERTAINTY_BAND = "generateUncertaintyBand";
    private static final String PROPERTY_NAME_BAND_NAME = "bandName";
    private static final String PROPERTY_NAME_BAND_DESC = "bandDescription";
    private static final String PROPERTY_NAME_BAND_UNIT = "bandUnit";
    private static final String PROPERTY_NAME_BAND_WAVELENGTH = "bandWavelength";

    private final ProductNodeList<Product> productsList;
    private final BindingContext bindingContext;
    private Product targetProduct;

    private String productName;
    @SuppressWarnings("FieldCanBeLocal")
    private String expression;

    @SuppressWarnings("UnusedDeclaration")
    private double noDataValue;
    @SuppressWarnings("UnusedDeclaration")
    private boolean noDataValueUsed;
    @SuppressWarnings("UnusedDeclaration")
    private boolean saveExpressionOnly;
    @SuppressWarnings("UnusedDeclaration")
    private boolean generateUncertaintyBand;
    @SuppressWarnings("UnusedDeclaration")
    private String bandName;
    @SuppressWarnings("FieldCanBeLocal")
    private String bandDescription;
    @SuppressWarnings("FieldCanBeLocal")
    private String bandUnit;
    @SuppressWarnings("UnusedDeclaration")
    private float bandWavelength;

    private static int numNewBands = 0;

    public BandMathsDialog(Product currentProduct, ProductNodeList<Product> productsList, String expression, String helpId) {
        super(SnapApp.getDefault().getMainFrame(), Bundle.CTL_BandMathsDialog_Title(), ID_OK_CANCEL_HELP, helpId);
        Assert.notNull(expression, "expression");
        Assert.notNull(currentProduct, "currentProduct");
        Assert.notNull(productsList, "productsList");
        Assert.argument(productsList.size() > 0, "productsList must be not empty");
        targetProduct = currentProduct;
        this.productsList = productsList;
        bindingContext = createBindingContext();

        this.expression = expression;
        bandDescription = "";
        bandUnit = "";
        makeUI();
    }

    @Override
    protected void onOK() {
        final String validMaskExpression;
        int width = targetProduct.getSceneRasterWidth();
        int height = targetProduct.getSceneRasterHeight();
        RasterDataNode prototypeRasterDataNode = null;
        try {
            Product[] products = getCompatibleProducts();
            int defaultProductIndex = Arrays.asList(products).indexOf(targetProduct);
            validMaskExpression = BandArithmetic.getValidMaskExpression(getExpression(), products, defaultProductIndex, null);
            final RasterDataNode[] refRasters = BandArithmetic.getRefRasters(getExpression(), products, defaultProductIndex);
            if (refRasters.length > 0) {
                prototypeRasterDataNode = refRasters[0];
                width = prototypeRasterDataNode.getRasterWidth();
                height = prototypeRasterDataNode.getRasterHeight();
            }
        } catch (ParseException e) {
            String errorMessage = Bundle.CTL_BandMathsDialog_ErrBandNotCreated() + e.getMessage();
            Dialogs.showError(Bundle.CTL_BandMathsDialog_Title() + " - Error", errorMessage);
            hide();
            return;
        }

        Band band;
        if (saveExpressionOnly) {
            band = new VirtualBand(getBandName(), ProductData.TYPE_FLOAT32, width, height, getExpression());
            setBandProperties(band, validMaskExpression);
        } else {
            band = new Band(getBandName(), ProductData.TYPE_FLOAT32, width, height);
            setBandProperties(band, "");
        }

        ProductNodeGroup<Band> bandGroup = targetProduct.getBandGroup();
        bandGroup.add(band);

        if (prototypeRasterDataNode != null) {
            ProductUtils.copyImageGeometry(prototypeRasterDataNode, band, false);
        }

        if (saveExpressionOnly) {
            checkExpressionForExternalReferences(getExpression());
        } else {
            String expression = getExpression();
            if (validMaskExpression != null && !validMaskExpression.isEmpty()) {
                expression = "(" + validMaskExpression + ") ? (" + expression + ") : NaN";
            }
            band.setSourceImage(VirtualBand.createSourceImage(band, expression));
        }

        UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(targetProduct);
        if (undoManager != null) {
            undoManager.addEdit(new UndoableProductNodeInsertion<>(bandGroup, band));
        }

        hide();
        band.setModified(true);
        if (SnapApp.getDefault().getPreferences().getBoolean(PREF_KEY_AUTO_SHOW_NEW_BANDS, true)) {
            OpenImageViewAction.openImageView(band);
        }

        if (generateUncertaintyBand) {
            if (band instanceof VirtualBand) {
                VirtualBand virtualBand = (VirtualBand) band;
                PropagateUncertaintyAction uncertaintyAction = new PropagateUncertaintyAction(virtualBand);
                uncertaintyAction.actionPerformed(null);
            }
        }
    }

    private void setBandProperties(Band band, String validMaskExpression) {
        band.setDescription(bandDescription);
        band.setUnit(bandUnit);
        band.setSpectralWavelength(bandWavelength);
        band.setGeophysicalNoDataValue(noDataValue);
        band.setNoDataValueUsed(noDataValueUsed);
        band.setValidPixelExpression(validMaskExpression);
    }

    @Override
    protected boolean verifyUserInput() {
        if (!isValidExpression()) {
            showErrorDialog(Bundle.CTL_BandMathsDialog_ErrExpressionNotValid());
            return false;
        }

        if (isTargetBandReferencedInExpression()) {
            showErrorDialog(Bundle.CTL_BandMathsDialog_ErrBandCannotBeReferenced(getBandName()));
            return false;
        }
        return super.verifyUserInput();
    }

    private void makeUI() {
        JButton loadExpressionButton = new JButton("Load...");
        loadExpressionButton.setName("loadExpressionButton");
        loadExpressionButton.addActionListener(createLoadExpressionButtonListener());

        JButton saveExpressionButton = new JButton("Save...");
        saveExpressionButton.setName("saveExpressionButton");
        saveExpressionButton.addActionListener(createSaveExpressionButtonListener());

        JButton editExpressionButton = new JButton("Edit Expression...");
        editExpressionButton.setName("editExpressionButton");
        editExpressionButton.addActionListener(createEditExpressionButtonListener());

        final JPanel panel = GridBagUtils.createPanel();
        int line = 0;
        GridBagConstraints gbc = new GridBagConstraints();

        JComponent[] components = createComponents(PROPERTY_NAME_PRODUCT, SingleSelectionEditor.class);
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, components[1], gbc, "gridwidth=3, fill=BOTH, weightx=1");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, components[0], gbc, "insets.top=3, gridwidth=3, fill=BOTH, anchor=WEST");

        gbc.gridy = ++line;
        components = createComponents(PROPERTY_NAME_BAND_NAME, TextFieldEditor.class);
        GridBagUtils.addToPanel(panel, components[1], gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(panel, components[0], gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        components = createComponents(PROPERTY_NAME_BAND_DESC, TextFieldEditor.class);
        GridBagUtils.addToPanel(panel, components[1], gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(panel, components[0], gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        components = createComponents(PROPERTY_NAME_BAND_UNIT, TextFieldEditor.class);
        GridBagUtils.addToPanel(panel, components[1], gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(panel, components[0], gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        components = createComponents(PROPERTY_NAME_BAND_WAVELENGTH, TextFieldEditor.class);
        GridBagUtils.addToPanel(panel, components[1], gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(panel, components[0], gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        components = createComponents(PROPERTY_NAME_SAVE_EXPRESSION_ONLY, CheckBoxEditor.class);
        GridBagUtils.addToPanel(panel, components[0], gbc, "insets.top=3, gridwidth=3, fill=HORIZONTAL, anchor=EAST");

        gbc.gridy = ++line;
        JPanel nodataPanel = new JPanel(new BorderLayout());
        components = createComponents(PROPERTY_NAME_NO_DATA_VALUE_USED, CheckBoxEditor.class);
        nodataPanel.add(components[0], BorderLayout.WEST);
        components = createComponents(PROPERTY_NAME_NO_DATA_VALUE, NumericEditor.class);
        nodataPanel.add(components[0]);
        GridBagUtils.addToPanel(panel, nodataPanel, gbc,
                                "weightx=1, insets.top=3, gridwidth=3, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        components = createComponents(PROPERTY_NAME_GENERATE_UNCERTAINTY_BAND, CheckBoxEditor.class);
        GridBagUtils.addToPanel(panel, components[0], gbc, "insets.top=3, gridwidth=3, fill=HORIZONTAL, anchor=EAST");

        gbc.gridy = ++line;
        JLabel expressionLabel = new JLabel(Bundle.CTL_BandMathsDialog_LblExpression());
        JTextArea expressionArea = new JTextArea();
        expressionArea.setRows(3);
        TextComponentAdapter textComponentAdapter = new TextComponentAdapter(expressionArea);
        bindingContext.bind(PROPERTY_NAME_EXPRESSION, textComponentAdapter);

        GridBagUtils.addToPanel(panel, expressionLabel, gbc, "insets.top=3, gridwidth=3, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, expressionArea, gbc,
                                "weighty=1, insets.top=3, gridwidth=3, fill=BOTH, anchor=WEST");
        gbc.gridy = ++line;
        final JPanel loadSavePanel = new JPanel();
        loadSavePanel.add(loadExpressionButton);
        loadSavePanel.add(saveExpressionButton);
        GridBagUtils.addToPanel(panel, loadSavePanel, gbc,
                                "weighty=0, insets.top=3, gridwidth=2, fill=NONE, anchor=WEST");
        GridBagUtils.addToPanel(panel, editExpressionButton, gbc,
                                "weighty=1, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=EAST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, new JLabel(""), gbc,
                                "insets.top=10, weightx=1, weighty=1, gridwidth=3, fill=BOTH, anchor=WEST");

        setContent(panel);

        expressionArea.selectAll();
        expressionArea.requestFocus();
    }

    private ActionListener createLoadExpressionButtonListener() {
        return e -> {
            try {
                final File file = Dialogs.requestFileForOpen(
                        "Load Band Maths Expression", false, null, PREF_KEY_LAST_EXPRESSION_PATH);
                if (file != null) {
                    expression = new String(Files.readAllBytes(file.toPath()));
                    bindingContext.getBinding(PROPERTY_NAME_EXPRESSION).setPropertyValue(expression);
                    bindingContext.getBinding(PROPERTY_NAME_EXPRESSION).adjustComponents();
                }
            } catch (IOException ex) {
                showErrorDialog(ex.getMessage());
            }
        };
    }

    private ActionListener createSaveExpressionButtonListener() {
        return e -> {
            try {
                final File file = Dialogs.requestFileForSave(
                        "Save Band Maths Expression", false, null, ".txt", "myExpression", null, PREF_KEY_LAST_EXPRESSION_PATH);

                if (file != null) {
                    final FileOutputStream out = new FileOutputStream(file.getAbsolutePath(), false);
                    PrintStream p = new PrintStream(out);
                    p.print(getExpression());
                }
            } catch (IOException ex) {
                showErrorDialog(ex.getMessage());
            }
        };
    }

    private JComponent[] createComponents(String propertyName, Class<? extends PropertyEditor> editorClass) {
        PropertyDescriptor descriptor = bindingContext.getPropertySet().getDescriptor(propertyName);
        PropertyEditor editor = PropertyEditorRegistry.getInstance().getPropertyEditor(editorClass.getName());
        return editor.createComponents(descriptor, bindingContext);
    }

    private BindingContext createBindingContext() {
        final PropertyContainer container = PropertyContainer.createObjectBacked(this);
        final BindingContext context = new BindingContext(container);

        container.addPropertyChangeListener(PROPERTY_NAME_PRODUCT, evt -> targetProduct = productsList.getByDisplayName(productName));

        productName = targetProduct.getDisplayName();
        PropertyDescriptor descriptor = container.getDescriptor(PROPERTY_NAME_PRODUCT);
        descriptor.setValueSet(new ValueSet(productsList.getDisplayNames()));
        descriptor.setDisplayName("Target product");

        descriptor = container.getDescriptor(PROPERTY_NAME_BAND_NAME);
        descriptor.setDisplayName("Name");
        descriptor.setDescription("The name for the new band.");
        descriptor.setNotEmpty(true);
        descriptor.setValidator(new ProductNodeNameValidator(targetProduct));
        String newBandName;
        do {
            numNewBands++;
            newBandName = "new_band_" + numNewBands;
        } while (targetProduct.containsRasterDataNode(newBandName));
        descriptor.setDefaultValue("new_band_" + (numNewBands));

        descriptor = container.getDescriptor(PROPERTY_NAME_BAND_DESC);
        descriptor.setDisplayName("Description");
        descriptor.setDescription("The description for the new band.");

        descriptor = container.getDescriptor(PROPERTY_NAME_BAND_UNIT);
        descriptor.setDisplayName("Unit");
        descriptor.setDescription("The physical unit for the new band.");

        descriptor = container.getDescriptor(PROPERTY_NAME_BAND_WAVELENGTH);
        descriptor.setDisplayName("Spectral wavelength");
        descriptor.setDescription("The physical unit for the new band.");

        descriptor = container.getDescriptor(PROPERTY_NAME_EXPRESSION);
        descriptor.setDisplayName("Band maths expression");
        descriptor.setDescription("Band maths expression");
        descriptor.setNotEmpty(true);

        descriptor = container.getDescriptor(PROPERTY_NAME_SAVE_EXPRESSION_ONLY);
        descriptor.setDisplayName("Virtual (save expression only, don't store data)");
        descriptor.setDefaultValue(Boolean.TRUE);

        descriptor = container.getDescriptor(PROPERTY_NAME_NO_DATA_VALUE_USED);
        descriptor.setDisplayName("Replace NaN and infinity results by");
        descriptor.setDefaultValue(Boolean.TRUE);

        descriptor = container.getDescriptor(PROPERTY_NAME_NO_DATA_VALUE);
        descriptor.setDefaultValue(Double.NaN);

        descriptor = container.getDescriptor(PROPERTY_NAME_GENERATE_UNCERTAINTY_BAND);
        descriptor.setDisplayName("Generate associated uncertainty band");
        descriptor.setDefaultValue(Boolean.FALSE);

        container.setDefaultValues();


        context.addPropertyChangeListener(PROPERTY_NAME_SAVE_EXPRESSION_ONLY, evt -> {
            final boolean saveExpressionOnly1 = (Boolean) context.getBinding(
                    PROPERTY_NAME_SAVE_EXPRESSION_ONLY).getPropertyValue();
            if (!saveExpressionOnly1) {
                context.getBinding(PROPERTY_NAME_NO_DATA_VALUE_USED).setPropertyValue(true);
            }
        });
        context.bindEnabledState(PROPERTY_NAME_NO_DATA_VALUE_USED, false,
                                 PROPERTY_NAME_SAVE_EXPRESSION_ONLY, Boolean.FALSE);
        context.bindEnabledState(PROPERTY_NAME_NO_DATA_VALUE, true,
                                 PROPERTY_NAME_NO_DATA_VALUE_USED, Boolean.TRUE);
        context.bindEnabledState(PROPERTY_NAME_GENERATE_UNCERTAINTY_BAND, true,
                                 PROPERTY_NAME_SAVE_EXPRESSION_ONLY, Boolean.TRUE);

        return context;
    }

    private String getBandName() {
        return bandName.trim();
    }

    private String getExpression() {
        return expression.trim();
    }

    private Product[] getCompatibleProducts() {
        List<Product> compatibleProducts = new ArrayList<>(productsList.size());
        compatibleProducts.add(targetProduct);
        for (int i = 0; i < productsList.size(); i++) {
            final Product product = productsList.getAt(i);
            if (targetProduct != product) {
                if (targetProduct.getSceneRasterWidth() == product.getSceneRasterWidth()
                        && targetProduct.getSceneRasterHeight() == product.getSceneRasterHeight()) {
                    compatibleProducts.add(product);
                }
            }
        }
        return compatibleProducts.toArray(new Product[compatibleProducts.size()]);
    }

    private ActionListener createEditExpressionButtonListener() {
        return e -> {
            Product[] compatibleProducts = getCompatibleProducts();

            final Preferences preferences = SnapApp.getDefault().getPreferences();
            final PreferencesPropertyMap preferencesPropertyMap = new PreferencesPropertyMap(preferences);
            ProductExpressionPane pep = ProductExpressionPane.createGeneralExpressionPane(compatibleProducts,
                                                                                          targetProduct,
                                                                                          preferencesPropertyMap);
            pep.setCode(getExpression());
            int status = pep.showModalDialog(getJDialog(), "Band Maths Expression Editor");
            if (status == ModalDialog.ID_OK) {
                bindingContext.getBinding(PROPERTY_NAME_EXPRESSION).setPropertyValue(pep.getCode());
            }
            pep.dispose();
        };
    }

    private void checkExpressionForExternalReferences(String expression) {
        final Product[] compatibleProducts = getCompatibleProducts();
        if (compatibleProducts.length > 1) {
            int defaultIndex = Arrays.asList(compatibleProducts).indexOf(targetProduct);
            RasterDataNode[] rasters = null;
            try {
                rasters = BandArithmetic.getRefRasters(expression, compatibleProducts, defaultIndex);
            } catch (ParseException ignored) {
            }
            if (rasters != null && rasters.length > 0) {
                Set<Product> externalProducts = new HashSet<>(compatibleProducts.length);
                for (RasterDataNode rdn : rasters) {
                    Product product = rdn.getProduct();
                    if (product != targetProduct) {
                        externalProducts.add(product);
                    }
                }
                if (!externalProducts.isEmpty()) {
                    String message = "The entered maths expression references multiple products.\n"
                            + "It will cause problems unless the session is restored as is.\n\n"
                            + "Note: You can save the session from the file menu.";
                    Dialogs.showWarning(message);
                }
            }
        }
    }

    private boolean isValidExpression() {
        final Product[] products = getCompatibleProducts();
        if (products.length == 0 || getExpression().isEmpty()) {
            return false;
        }

        final int defaultIndex = Arrays.asList(products).indexOf(targetProduct);
        try {
            BandArithmetic.parseExpression(getExpression(), products, defaultIndex == -1 ? 0 : defaultIndex);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private boolean isTargetBandReferencedInExpression() {
        final Product[] products = getCompatibleProducts();

        final int defaultIndex = Arrays.asList(products).indexOf(SnapApp.getDefault().getSelectedProduct(EXPLORER));
        try {
            final Term term = BandArithmetic.parseExpression(getExpression(),
                                                             products, defaultIndex == -1 ? 0 : defaultIndex);
            final RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);
            String bName = getBandName();
            if (targetProduct.containsRasterDataNode(bName)) {
                for (final RasterDataSymbol refRasterDataSymbol : refRasterDataSymbols) {
                    final String refRasterName = refRasterDataSymbol.getRaster().getName();
                    if (bName.equalsIgnoreCase(refRasterName)) {
                        return true;
                    }
                }
            }
        } catch (ParseException e) {
            return false;
        }
        return false;
    }

}
