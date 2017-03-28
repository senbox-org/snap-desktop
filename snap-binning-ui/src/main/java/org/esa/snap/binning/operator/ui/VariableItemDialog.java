package org.esa.snap.binning.operator.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.internal.TextComponentAdapter;
import com.bc.ceres.swing.binding.internal.TextFieldEditor;
import org.esa.snap.binning.operator.VariableConfig;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.product.ProductExpressionPane;

import javax.swing.*;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class VariableItemDialog extends ModalDialog {

    private static final String PROPERTY_VARIABLE_NAME = "name";
    private static final String PROPERTY_EXPRESSION = "expr";
    private static final String PROPERTY_VALID_EXPRESSION = "validExpr";

    private final VariableItem variableItem;
    private final boolean newVariable;
    private final Product contextProduct;
    private final BindingContext bindingContext;


    VariableItemDialog(final Window parent, VariableItem variableItem, boolean createNewVariable, Product contextProduct) {
        super(parent, "Intermediate Source Band", ID_OK_CANCEL, null);
        this.variableItem = variableItem;
        newVariable = createNewVariable;
        this.contextProduct = contextProduct;
        bindingContext = createBindingContext();
        makeUI();
    }

    @Override
    protected boolean verifyUserInput() {
        String expression = variableItem.variableConfig.getExpr() != null ? variableItem.variableConfig.getExpr().trim() : "";
        if (StringUtils.isNullOrEmpty(expression)) {
            AbstractDialog.showInformationDialog(getParent(), "The source band could not be created. The expression is empty.", "Information");
            return false;
        }
        String variableName = variableItem.variableConfig.getName() != null ? variableItem.variableConfig.getName().trim() : "";
        if (StringUtils.isNullOrEmpty(variableName)) {
            AbstractDialog.showInformationDialog(getParent(), "The source band could not be created. The name is empty.", "Information");
            return false;
        }
        if (newVariable && contextProduct.containsBand(variableName)) {
            String message = String.format("A source band or band with the name '%s' is already defined", variableName);
            AbstractDialog.showInformationDialog(getParent(), message, "Information");
            return false;
        }
        try {
            BandArithmetic.getValidMaskExpression(expression, contextProduct, null);
        } catch (ParseException e) {
            String errorMessage = "The source band could not be created.\nThe expression could not be parsed:\n" + e.getMessage(); /*I18N*/
            AbstractDialog.showErrorDialog(getParent(), errorMessage, "Error");
            return false;
        }
        String validExpression = variableItem.variableConfig.getValidExpr() != null ? variableItem.variableConfig.getValidExpr().trim() : "";
        if (!validExpression.isEmpty()) {
            try {
                BandArithmetic.parseExpression(validExpression, new Product[]{contextProduct}, 0);
            } catch (ParseException e) {
                String errorMessage = "The valid pixel expression could not be parsed:\n" + e.getMessage(); /*I18N*/
                JOptionPane.showMessageDialog(getParent(), errorMessage);
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onOK() {
        VariableConfig vc = variableItem.variableConfig;
        vc.setName(vc.getName().trim());
        vc.setExpr(vc.getExpr().trim());
        vc.setValidExpr(vc.getValidExpr() != null ? vc.getValidExpr().trim() : "");
        super.onOK();
    }

    VariableItem getVariableItem() {
        return variableItem;
    }

    private BindingContext createBindingContext() {
        final PropertyContainer container = PropertyContainer.createObjectBacked(variableItem.variableConfig, new ParameterDescriptorFactory());
        final BindingContext context = new BindingContext(container);

        PropertyDescriptor descriptor = container.getDescriptor(PROPERTY_VARIABLE_NAME);
        descriptor.setDescription("The name for the source band.");
        descriptor.setValidator(new VariableNameValidator());
        container.setDefaultValues();

        return context;
    }

    private void makeUI() {
        JComponent[] variableComponents = createComponents(PROPERTY_VARIABLE_NAME, TextFieldEditor.class);

        final TableLayout layout = new TableLayout(3);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTablePadding(4, 3);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableWeightX(0.0);

        layout.setCellWeightX(0, 1, 1.0);
        layout.setCellWeightX(1, 1, 1.0);
        layout.setCellWeightX(3, 2, 1.0);
        layout.setCellWeightX(4, 1, 1.0);
        layout.setCellWeightX(6, 2, 1.0);

        layout.setCellColspan(0, 1, 2);
        layout.setCellColspan(1, 1, 2);
        layout.setCellColspan(2, 0, 3);
        layout.setCellColspan(3, 0, 2);
        layout.setCellColspan(4, 1, 2);
        layout.setCellColspan(5, 0, 3);
        layout.setCellColspan(6, 0, 2);
        final JPanel panel = new JPanel(layout);

        // row 0
        panel.add(variableComponents[1]);
//        panel.add(layout.createHorizontalSpacer());
        panel.add(variableComponents[0]);

        JLabel expressionLabel = new JLabel("Variable expression:");
        JTextArea expressionArea = new JTextArea();
        expressionArea.setRows(3);
        expressionArea.setColumns(80);
        expressionArea.setLineWrap(true);
        expressionArea.setWrapStyleWord(true);
        bindingContext.bind(PROPERTY_EXPRESSION, new TextComponentAdapter(expressionArea));
        // row 1
        panel.add(expressionLabel);
        panel.add(layout.createHorizontalSpacer());
        // row 2
        panel.add(expressionArea);

        JButton editExpressionButton = new JButton("Edit...");
        editExpressionButton.setName("editExpressionButton");
        editExpressionButton.addActionListener(createEditExpressionButtonListener());
        // row 3
        panel.add(layout.createHorizontalSpacer());
        panel.add(editExpressionButton);

        JLabel validExpressionLabel = new JLabel("Valid-pixel expression:");
        JTextArea validExpressionArea = new JTextArea();
        validExpressionArea.setRows(3);
        validExpressionArea.setColumns(80);
        validExpressionArea.setLineWrap(true);
        validExpressionArea.setWrapStyleWord(true);
        bindingContext.bind(PROPERTY_VALID_EXPRESSION, new TextComponentAdapter(validExpressionArea));
        // row 4
        panel.add(validExpressionLabel);
        panel.add(layout.createHorizontalSpacer());
        // row 5
        panel.add(validExpressionArea);

        JButton editValidExpressionButton = new JButton("Edit...");
        editValidExpressionButton.setName("editValidExpressionButton");
        editValidExpressionButton.addActionListener(createEditValidExpressionButtonListener());
        // row 6
        panel.add(layout.createHorizontalSpacer());
        panel.add(editValidExpressionButton);
        setContent(panel);
    }

    private JComponent[] createComponents(String propertyName, Class<? extends PropertyEditor> editorClass) {
        PropertyDescriptor descriptor = bindingContext.getPropertySet().getDescriptor(propertyName);
        PropertyEditor editor = PropertyEditorRegistry.getInstance().getPropertyEditor(editorClass.getName());
        return editor.createComponents(descriptor, bindingContext);
    }

    private ActionListener createEditExpressionButtonListener() {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ProductExpressionPane expressionPane =
                        ProductExpressionPane.createGeneralExpressionPane(new Product[]{contextProduct},
                                                                          contextProduct,
                                                                          null);
                expressionPane.setCode(variableItem.variableConfig.getExpr());
                int status = expressionPane.showModalDialog(getJDialog(), "Expression Editor");
                if (status == ModalDialog.ID_OK) {
                    bindingContext.getBinding(PROPERTY_EXPRESSION).setPropertyValue(expressionPane.getCode());
                }
                expressionPane.dispose();
            }
        };
    }

    private ActionListener createEditValidExpressionButtonListener() {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ProductExpressionPane expressionPane =
                        ProductExpressionPane.createBooleanExpressionPane(new Product[]{contextProduct},
                                                                          contextProduct,
                                                                          null);
                expressionPane.setCode(variableItem.variableConfig.getValidExpr());
                int status = expressionPane.showModalDialog(getJDialog(), "Valid Expression Editor");
                if (status == ModalDialog.ID_OK) {
                    bindingContext.getBinding(PROPERTY_VALID_EXPRESSION).setPropertyValue(expressionPane.getCode());
                }
                expressionPane.dispose();
            }
        };
    }

    private class VariableNameValidator implements Validator {

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            final String name = (String) value;
            if (contextProduct.containsRasterDataNode(name)) {
                throw new ValidationException("The source band name must be unique.");
            }
        }
    }

}
