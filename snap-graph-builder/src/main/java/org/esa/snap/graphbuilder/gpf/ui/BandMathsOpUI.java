package org.esa.snap.graphbuilder.gpf.ui;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeList;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.gpf.common.BandMathsOp;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.param.ParamChangeEvent;
import org.esa.snap.core.param.ParamChangeListener;
import org.esa.snap.core.param.ParamProperties;
import org.esa.snap.core.param.Parameter;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.product.ProductExpressionPane;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Vector;

/**
User interface for BandMaths Operator
 */
public class BandMathsOpUI extends BaseOperatorUI {

    private static final String _PARAM_NAME_BAND = "targetBand";

    private Parameter paramBand = null;
    private Parameter paramBandType = null;
    private Parameter paramBandUnit = null;
    private Parameter paramNoDataValue = null;
    private Parameter paramExpression = null;
    private Product targetProduct = null;
    private Band targetBand = null;
    private ProductNodeList<Product> productsList = null;
    private JButton editExpressionButton = null;
    private JComponent panel = null;
    private String errorText = "";
    private AppContext appContext;

    private BandMathsOp.BandDescriptor bandDesc = new BandMathsOp.BandDescriptor();

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {
        this.appContext = appContext;
        initializeOperatorUI(operatorName, parameterMap);
        initVariables();
        panel = createUI();
        initParameters();

        return panel;
    }

    @Override
    public void initParameters() {

        Object[] bandDescriptors = (Object[])paramMap.get("targetBands");
        if(bandDescriptors == null)
            bandDescriptors = (Object[])paramMap.get("targetBandDescriptors");
        if(bandDescriptors != null && bandDescriptors.length > 0) {
            bandDesc = (BandMathsOp.BandDescriptor)(bandDescriptors[0]);
            bandDesc.type = ProductData.TYPESTRING_FLOAT32;

            try {
                paramBand.setValueAsText(bandDesc.name);
                paramBandType.setValueAsText(bandDesc.type);
                paramBandUnit.setValueAsText(bandDesc.unit != null ? bandDesc.unit : "");
                paramNoDataValue.setValueAsText(String.valueOf(bandDesc.noDataValue));
                paramExpression.setValueAsText(bandDesc.expression);
            } catch(Exception e) {
                SystemUtils.LOG.warning(e.getMessage());
            }
        }
        if(sourceProducts != null && sourceProducts.length > 0) {
            targetProduct = sourceProducts[0];

            targetBand = new Band(bandDesc.name, ProductData.TYPE_FLOAT32,
                    targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight());
            targetBand.setDescription("");
            //targetBand.setUnit(dialog.getNewBandsUnit());

            productsList = new ProductNodeList<Product>();
            for (Product prod : sourceProducts) {
                productsList.add(prod);
            }
        } else {
            targetProduct = null;
            targetBand = null;
        }
        updateUIState(paramBand.getName());
    }

    @Override
    public UIValidation validateParameters() {
        if(!(targetProduct == null || isValidExpression()))
            return new UIValidation(UIValidation.State.ERROR, "Expression is invalid. "+ errorText);
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        bandDesc.name = paramBand.getValueAsText();
        bandDesc.type = paramBandType.getValueAsText();
        bandDesc.unit = paramBandUnit.getValueAsText();
        String noDataValueStr = paramNoDataValue.getValueAsText();
        bandDesc.noDataValue = noDataValueStr.isEmpty() ? 0 : Double.parseDouble(noDataValueStr);
        bandDesc.expression = paramExpression.getValueAsText();

        final BandMathsOp.BandDescriptor[] bandDescriptors = new BandMathsOp.BandDescriptor[1];
        bandDescriptors[0] = bandDesc;
        paramMap.put("targetBandDescriptors", bandDescriptors);
    }

    private void initVariables() {
        final ParamChangeListener paramChangeListener = createParamChangeListener();

        BandMathsOp.BandDescriptor[] bandDescriptors = (BandMathsOp.BandDescriptor[])paramMap.get("targetBandDescriptors");
        if(bandDescriptors != null && bandDescriptors.length > 0) {
            bandDesc = bandDescriptors[0];
        } else {
            bandDesc.name = "newBand";
            bandDesc.type = "float32";
        }

        paramBand = new Parameter(_PARAM_NAME_BAND, bandDesc.name);
        paramBand.getProperties().setValueSetBound(false);
        paramBand.getProperties().setLabel("Target Band"); /*I18N*/
        paramBand.addParamChangeListener(paramChangeListener);

        paramBandType = new Parameter("bandType", bandDesc.type);
        paramBandType.getProperties().setValueSetBound(false);
        paramBandType.getProperties().setLabel("Target Band Type"); /*I18N*/
        paramBandType.addParamChangeListener(paramChangeListener);

        paramBandUnit = new Parameter("bandUnit", bandDesc.unit);
        paramBandUnit.getProperties().setValueSetBound(false);
        paramBandUnit.getProperties().setLabel("Band Unit"); /*I18N*/
        paramBandUnit.addParamChangeListener(paramChangeListener);

        paramNoDataValue = new Parameter("bandNodataValue", bandDesc.noDataValue);
        paramNoDataValue.getProperties().setValueSetBound(false);
        paramNoDataValue.getProperties().setLabel("No-Data Value"); /*I18N*/
        paramNoDataValue.addParamChangeListener(paramChangeListener);

        paramExpression = new Parameter("arithmetikExpr", bandDesc.expression);
        paramExpression.getProperties().setLabel("Expression"); /*I18N*/
        paramExpression.getProperties().setDescription("Arithmetic expression"); /*I18N*/
        paramExpression.getProperties().setNumRows(5);
//        paramExpression.getProperties().setEditorClass(ArithmetikExpressionEditor.class);
//        paramExpression.getProperties().setValidatorClass(BandArithmeticExprValidator.class);

        setArithmetikValues();
    }

    private JComponent createUI() {

        editExpressionButton = new JButton("Edit Expression...");
        editExpressionButton.setName("editExpressionButton");
        editExpressionButton.addActionListener(createEditExpressionButtonListener());

        final JPanel gridPanel = GridBagUtils.createPanel();
        int line = 0;
        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(gridPanel, paramBand.getEditor().getLabelComponent(), gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(gridPanel, paramBand.getEditor().getComponent(), gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(gridPanel, paramBandType.getEditor().getLabelComponent(), gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(gridPanel, paramBandType.getEditor().getComponent(), gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(gridPanel, paramBandUnit.getEditor().getLabelComponent(), gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(gridPanel, paramBandUnit.getEditor().getComponent(), gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(gridPanel, paramNoDataValue.getEditor().getLabelComponent(), gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(gridPanel, paramNoDataValue.getEditor().getComponent(), gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(gridPanel, paramExpression.getEditor().getLabelComponent(), gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=NORTHWEST");
        GridBagUtils.addToPanel(gridPanel, paramExpression.getEditor().getComponent(), gbc,
                                "weightx=1, weighty=1, insets.top=3, gridwidth=2, fill=BOTH, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(gridPanel, editExpressionButton, gbc,
                                "weighty=0, insets.top=3, gridwidth=3, fill=NONE, anchor=EAST");

        return gridPanel;
    }

    private void setArithmetikValues() {
        final ParamProperties props = paramExpression.getProperties();
        props.setPropertyValue(ParamProperties.COMP_PRODUCTS_FOR_BAND_ARITHMETHIK_KEY, getCompatibleProducts());
        props.setPropertyValue(ParamProperties.SEL_PRODUCT_FOR_BAND_ARITHMETHIK_KEY, targetProduct);
    }

     private ParamChangeListener createParamChangeListener() {
        return new ParamChangeListener() {

            public void parameterValueChanged(ParamChangeEvent event) {
                updateUIState(event.getParameter().getName());
            }
        };
    }

    private Product[] getCompatibleProducts() {
        if (targetProduct == null) {
            return null;
        }
        final Vector<Product> compatibleProducts = new Vector<>();
        compatibleProducts.add(targetProduct);
            final float geolocationEps = 180;
            Debug.trace("BandArithmetikDialog.geolocationEps = " + geolocationEps);
            Debug.trace("BandArithmetikDialog.getCompatibleProducts:");
            Debug.trace("  comparing: " + targetProduct.getName());
            for (int i = 0; i < productsList.size(); i++) {
                final Product product = productsList.getAt(i);
                if (targetProduct != product) {
                    Debug.trace("  with:      " + product.getDisplayName());
                    final boolean compatibleProduct = targetProduct.isCompatibleProduct(product, geolocationEps);
                    Debug.trace("  result:    " + compatibleProduct);
                    if (compatibleProduct) {
                        compatibleProducts.add(product);
                    }
                }
            }
        return compatibleProducts.toArray(new Product[compatibleProducts.size()]);
    }

    private ActionListener createEditExpressionButtonListener() {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ProductExpressionPane pep = ProductExpressionPane.createGeneralExpressionPane(getCompatibleProducts(),
                        targetProduct, appContext.getPreferences());
                pep.setCode(paramExpression.getValueAsText());
                int status = pep.showModalDialog(SwingUtilities.getWindowAncestor(panel), "Arithmetic Expression Editor");
                if (status == ModalDialog.ID_OK) {
                    paramExpression.setValue(pep.getCode(), null);
                    Debug.trace("BandArithmetikDialog: expression is: " + pep.getCode());

                    bandDesc.expression = paramExpression.getValueAsText();
                }
                pep.dispose();
                pep = null;
            }
        };
    }

    private boolean isValidExpression() {
        errorText = "";
        final Product[] products = getCompatibleProducts();
        if (products == null || products.length == 0) {
            return false;
        }

        String expression = paramExpression.getValueAsText();
        if (expression == null || expression.length() == 0) {
            return false;
        }

        try {
            BandArithmetic.parseExpression(expression, products, 0);
        } catch (ParseException e) {
            errorText = e.getMessage();
            return false;
        }
        return true;
    }

    private void updateUIState(String parameterName) {

        if (parameterName == null) {
            return;
        }

        if (parameterName.equals(_PARAM_NAME_BAND)) {
            final boolean b = targetProduct != null;
            paramExpression.setUIEnabled(b);
            editExpressionButton.setEnabled(b);
            paramBand.setUIEnabled(b);
            paramBandType.setUIEnabled(b);
            paramBandUnit.setUIEnabled(b);
            paramNoDataValue.setUIEnabled(b);
            if (b) {
                setArithmetikValues();
            }

            final String selectedBandName = paramBand.getValueAsText();
            if (b) {
                if (selectedBandName != null && selectedBandName.length() > 0) {
                    targetBand = targetProduct.getBand(selectedBandName);
                }
            }
        }
    }

}
