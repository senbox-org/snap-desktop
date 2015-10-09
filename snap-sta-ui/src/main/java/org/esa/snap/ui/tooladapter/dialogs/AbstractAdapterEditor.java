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

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.converters.ArrayConverter;
import com.bc.ceres.binding.validators.NotEmptyValidator;
import com.bc.ceres.binding.validators.PatternValidator;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.internal.TextFieldEditor;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.descriptor.*;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterConstants;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterIO;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterOpSpi;
import org.esa.snap.modules.ModulePackager;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.tooladapter.actions.EscapeAction;
import org.esa.snap.ui.tooladapter.actions.ToolAdapterActionRegistrar;
import org.esa.snap.ui.tooladapter.model.AutoCompleteTextArea;
import org.esa.snap.ui.tooladapter.model.OperatorParametersTable;
import org.esa.snap.ui.tooladapter.preferences.ToolAdapterOptionsController;
import org.esa.snap.ui.tooladapter.validators.RequiredFieldValidator;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.esa.snap.utils.SpringUtilities.DEFAULT_PADDING;
import static org.esa.snap.utils.SpringUtilities.makeCompactGrid;

/**
 * A dialog window used to edit an operator, or to create a new operator.
 * It shows details of an operator such as: descriptor details (name, alias, label, version, copyright,
 * authors, description), system variables, preprocessing tool, product writer, tool location,
 * operator working directory, command line template content, tool output patterns and parameters.
 *
 * @author Cosmin Cara
 */
@NbBundle.Messages({
        "CTL_Label_Alias_Text=Alias:",
        "CTL_Label_UniqueName_Text=Unique name:",
        "CTL_Label_Label_Text=Label:",
        "CTL_Label_Version_Text=Version:",
        "CTL_Label_Copyright_Text=Copyright:",
        "CTL_Label_Authors_Text=Authors:",
        "CTL_Label_Description_Text=Description:",
        "CTL_Label_MenuLocation_Text=Menu location:",
        "CTL_Panel_OperatorDescriptor_Text=Operator Descriptor",
        "CTL_Label_PreprocessingTool_Text=Preprocessing tool",
        "CTL_Label_WriteBefore_Text=Write before processing using:",
        "CTL_Panel_PreProcessing_Border_TitleText=Preprocessing",
        "CTL_Panel_ConfigParams_Text=Configuration Parameters",
        "CTL_Label_ToolLocation_Text=Tool location: ",
        "CTL_Label_WorkDir_Text=Working directory: ",
        "CTL_Label_CmdLineTemplate_Text=Command line template:",
        "CTL_Panel_OutputPattern_Border_TitleText=Tool Output Patterns",
        "CTL_Label_ProgressPattern=Progress pattern:",
        "CTL_Label_ErrorPattern=Error pattern:",
        "CTL_Panel_SysVar_Border_TitleText=System Variables",
        "CTL_Label_RadioButton_ExistingMenus=Existing Menus",
        "CTL_Label_RadioButton_NewMenu=Create Menu",
        "Icon_Add=/org/esa/snap/resources/images/icons/Add16.png",
        "CTL_Panel_OpParams_Border_TitleText=Operator Parameters",
        "CTL_Button_Export_Text=Export as Module",
        "CTL_Button_Add_Variable_Text=Add Variable",
        "CTL_Button_Add_PDVariable_Text=Add Platform-Dependent Variable",
        "MSG_Export_Complete_Text=The adapter was exported as a NetBeans module in %s",
        "MSG_Inexistent_Tool_Path_Text=The tool executable does not exist.\n" +
                "Please specify the location of an existing executable.",
        "MSG_Inexistent_WorkDir_Text=The working directory does not exist.\n" +
                "Please specify a valid location.",
        "MSG_Inexistem_Parameter_Value_Text=The file or folder for parameter %s does not exist.\n" +
                "Please specify a valid location or change the %s property of the parameter.",
        "MSG_Wrong_Value_Text=One or more form parameters have invalid values.\n" +
                "Please correct them before saving the adapter.",
        "MSG_Wrong_Usage_Array_Text=You have used array notation for source products, but only one product will be used.\n" +
                "Please correct the problem before saving the adapter.",
        "MSG_Empty_Variable_Text=The variable %s has no value set",
        "MSG_Empty_MenuLocation_Text=Value of 'Menu location' cannot be empty"
})
public abstract class AbstractAdapterEditor extends ModalDialog {

    protected static final String MESSAGE_REQUIRED = "This field is required";
    protected static final int MIN_WIDTH = 720;
    protected static final int MIN_HEIGHT = 580;
    protected static final int MIN_TABBED_WIDTH = 640;
    protected static final int MIN_TABBED_HEIGHT = 512;
    protected static int MAX_4K_WIDTH = 4096;
    protected static int MAX_4K_HEIGHT = 2160;
    protected ToolAdapterOperatorDescriptor oldOperatorDescriptor;
    protected ToolAdapterOperatorDescriptor newOperatorDescriptor;
    protected boolean operatorIsNew = false;
    protected int newNameIndex = -1;
    protected PropertyContainer propertyContainer;
    protected BindingContext bindingContext;
    protected AutoCompleteTextArea templateContent;
    protected OperatorParametersTable paramsTable;
    protected AppContext context;
    protected Logger logger;
    protected JTextField customMenuLocation;
    protected JRadioButton rbMenuNew;
    public static final String helpID = "sta_editor";

    protected int formWidth;
    protected int controlHeight = 24;

    public static AbstractAdapterEditor createEditorDialog(AppContext appContext, JDialog parent, ToolAdapterOperatorDescriptor operatorDescriptor, boolean operatorIsNew) {
        AbstractAdapterEditor dialog;
        if (useTabsForEditorDialog()) {
            dialog = new ToolAdapterTabbedEditorDialog(appContext, parent, operatorDescriptor, operatorIsNew);
        } else {
            dialog = new ToolAdapterEditorDialog(appContext, parent, operatorDescriptor, operatorIsNew);
        }
        return dialog;
    }

    public static AbstractAdapterEditor createEditorDialog(AppContext appContext, JDialog parent, ToolAdapterOperatorDescriptor operatorDescriptor, int newNameIndex) {
        AbstractAdapterEditor dialog;
        if (useTabsForEditorDialog()) {
            dialog = new ToolAdapterTabbedEditorDialog(appContext, parent, operatorDescriptor, newNameIndex);
        } else {
            dialog = new ToolAdapterEditorDialog(appContext, parent, operatorDescriptor, newNameIndex);
        }
        return dialog;
    }

    private static boolean useTabsForEditorDialog() {
        return Boolean.parseBoolean(NbPreferences.forModule(SnapDialogs.class).get(ToolAdapterOptionsController.PREFERENCE_KEY_TABBED_WINDOW, "false"));
    }

    private AbstractAdapterEditor(AppContext appContext, JDialog parent, String title) {
        super(parent.getOwner(), title, ID_OK_CANCEL_HELP, new Object[] { new JButton(Bundle.CTL_Button_Export_Text()) }, helpID);
        this.context = appContext;
        this.logger = Logger.getLogger(ToolAdapterEditorDialog.class.getName());
        this.registerButton(ID_OTHER, new JButton(Bundle.CTL_Button_Export_Text()));
        controlHeight = (int)((getJDialog().getFont().getSize() + 1) * 2);
    }

    private AbstractAdapterEditor(AppContext appContext, JDialog parent, ToolAdapterOperatorDescriptor operatorDescriptor) {
        this(appContext, parent, operatorDescriptor.getAlias());
        this.oldOperatorDescriptor = operatorDescriptor;
        this.newOperatorDescriptor = new ToolAdapterOperatorDescriptor(this.oldOperatorDescriptor);

        //see if all necessary parameters are present:
        if (newOperatorDescriptor.getToolParameterDescriptors().stream().filter(p -> p.getName().equals(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID)).count() == 0){
            newOperatorDescriptor.getToolParameterDescriptors().add(new TemplateParameterDescriptor(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID, Product[].class));
        }
        if (newOperatorDescriptor.getToolParameterDescriptors().stream().filter(p -> p.getName().equals(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_FILE)).count() == 0){
            newOperatorDescriptor.getToolParameterDescriptors().add(new TemplateParameterDescriptor(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_FILE, File[].class));
        }
        if (newOperatorDescriptor.getToolParameterDescriptors().stream().filter(p -> p.getName().equals(ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE)).count() == 0){
            TemplateParameterDescriptor parameterDescriptor = new TemplateParameterDescriptor(ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE, File.class);
            parameterDescriptor.setNotNull(false);
            newOperatorDescriptor.getToolParameterDescriptors().add(parameterDescriptor);
        }

        propertyContainer = PropertyContainer.createObjectBacked(newOperatorDescriptor);
        ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();
        String[] writers = registry.getAllProductWriterFormatStrings();
        Arrays.sort(writers);
        propertyContainer.getDescriptor(ToolAdapterConstants.PROCESSING_WRITER).setValueSet(new ValueSet(writers));
        Set<OperatorSpi> spis = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpis();
        java.util.List<String> toolboxSpis = new ArrayList<>();
        spis.stream().filter(p -> (p instanceof ToolAdapterOpSpi)
                && (p.getOperatorDescriptor().getClass() != AnnotationOperatorDescriptor.class)
                && !p.getOperatorAlias().equals(oldOperatorDescriptor.getAlias()))
                .forEach(operator -> toolboxSpis.add(operator.getOperatorDescriptor().getAlias()));
        toolboxSpis.sort(Comparator.<String>naturalOrder());
        propertyContainer.getDescriptor(ToolAdapterConstants.PREPROCESSOR_EXTERNAL_TOOL).setValueSet(new ValueSet(toolboxSpis.toArray(new String[toolboxSpis.size()])));

        bindingContext = new BindingContext(propertyContainer);

        paramsTable = new OperatorParametersTable(newOperatorDescriptor, appContext);
    }

    /**
     * Constructs a new window for editing the operator
     * @param appContext the application context
     * @param operatorDescriptor the descriptor of the operator to be edited
     * @param operatorIsNew true if the operator was not previously registered (so it is a new operator) and false if the operator was registered and the editing operation is requested
     */
    public AbstractAdapterEditor(AppContext appContext, JDialog parent, ToolAdapterOperatorDescriptor operatorDescriptor, boolean operatorIsNew) {
        this(appContext, parent, operatorDescriptor);
        this.operatorIsNew = operatorIsNew;
        this.newNameIndex = -1;
        setContent(createMainPanel());
        EscapeAction.register(this.getJDialog());
    }

    /**
     * Constructs a new window for editing the operator
     * @param appContext the application context
     * @param operatorDescriptor the descriptor of the operator to be edited
     * @param newNameIndex an integer value representing the suffix for the new operator name; if this value is less than 1, the editing operation of the current operator is executed; if the value is equal to or greater than 1, the operator is duplicated and the index value is used to compute the name of the new operator
     */
    public AbstractAdapterEditor(AppContext appContext, JDialog parent, ToolAdapterOperatorDescriptor operatorDescriptor, int newNameIndex) {
        this(appContext, parent, operatorDescriptor);
        this.newNameIndex = newNameIndex;
        this.operatorIsNew = this.newNameIndex >= 1;
        if(this.newNameIndex >= 1) {
            this.newOperatorDescriptor.setName(this.oldOperatorDescriptor.getName() + ToolAdapterConstants.OPERATOR_GENERATED_NAME_SEPARATOR + this.newNameIndex);
            this.newOperatorDescriptor.setAlias(this.oldOperatorDescriptor.getAlias() + ToolAdapterConstants.OPERATOR_GENERATED_NAME_SEPARATOR + this.newNameIndex);
        }
        setContent(createMainPanel());
        EscapeAction.register(this.getJDialog());
    }

    ToolAdapterOperatorDescriptor getUpdatedOperatorDescriptor() { return this.newOperatorDescriptor; }

    protected abstract JComponent createMainPanel();

    protected abstract JPanel createDescriptorPanel();

    protected abstract JPanel createVariablesPanel();

    protected abstract JPanel createPreProcessingPanel();

    protected abstract JPanel createToolInfoPanel();

    protected abstract JPanel createPatternsPanel();

    protected abstract JPanel createParametersPanel();

    protected boolean shouldValidate() {
        String value = NbPreferences.forModule(SnapDialogs.class).get(ToolAdapterOptionsController.PREFERENCE_KEY_VALIDATE_PATHS, null);
        return value == null || Boolean.parseBoolean(value);
    }

    @Override
    protected boolean verifyUserInput() {
        /**
         * Verify the existence of the tool executable
         */
        if (shouldValidate()) {
            File file = newOperatorDescriptor.getMainToolFileLocation();
            if (file == null) {
                // should not come here unless, somehow, the property value was not set by binding
                Object value = bindingContext.getBinding(ToolAdapterConstants.MAIN_TOOL_FILE_LOCATION).getPropertyValue();
                if (value != null) {
                    file = value instanceof File ? (File) value : new File(value.toString());
                }
            }
            if (file == null) {
                SnapDialogs.showWarning(Bundle.MSG_Inexistent_Tool_Path_Text());
                return false;
            }

            Path toolLocation = newOperatorDescriptor.resolveVariables(newOperatorDescriptor.getMainToolFileLocation()).toPath();
            if (!(Files.exists(toolLocation) && Files.isExecutable(toolLocation))) {
                SnapDialogs.showWarning(Bundle.MSG_Inexistent_Tool_Path_Text());
                return false;
            }
            /**
             * Verify the existence of the working directory
             */
            File workingDir = newOperatorDescriptor.resolveVariables(newOperatorDescriptor.getWorkingDir());
            if (!(workingDir != null && workingDir.exists() && workingDir.isDirectory())) {
                SnapDialogs.showWarning(Bundle.MSG_Inexistent_WorkDir_Text());
                return false;
            }
        }
        /**
         * Verify that there is no System Variable without value
         */
        java.util.List<SystemVariable> variables = newOperatorDescriptor.getVariables();
        if (variables != null) {
            for (SystemVariable variable : variables) {
                String value = variable.getValue();
                if (value == null || value.isEmpty()) {
                    SnapDialogs.showWarning(String.format(Bundle.MSG_Empty_Variable_Text(), variable.getKey()));
                    return false;
                }
            }
        }
        if (shouldValidate()) {
            /**
             * Verify the existence of files for File parameter values that are marked as Not Null or Not Empty
             */
            ParameterDescriptor[] parameterDescriptors = newOperatorDescriptor.getParameterDescriptors();
            if (parameterDescriptors != null && parameterDescriptors.length > 0) {
                for (ParameterDescriptor parameterDescriptor : parameterDescriptors) {
                    Class<?> dataType = parameterDescriptor.getDataType();
                    String defaultValue = parameterDescriptor.getDefaultValue();
                    if (File.class.isAssignableFrom(dataType) &&
                            (parameterDescriptor.isNotNull() || parameterDescriptor.isNotEmpty()) &&
                            (defaultValue == null || defaultValue.isEmpty() || !Files.exists(Paths.get(defaultValue)))) {
                        SnapDialogs.showWarning(String.format(Bundle.MSG_Inexistem_Parameter_Value_Text(),
                                parameterDescriptor.getName(), parameterDescriptor.isNotNull() ? ToolAdapterConstants.NOT_NULL : ToolAdapterConstants.NOT_EMPTY));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected void onOK() {
        if (!verifyUserInput()) {
            SnapDialogs.showWarning(Bundle.MSG_Wrong_Value_Text());
            this.getJDialog().requestFocus();
        } else {
            String templateContent = this.templateContent.getText();
            if (!resolveTemplateProductCount(templateContent)) {
                SnapDialogs.showWarning(Bundle.MSG_Wrong_Usage_Array_Text());
                this.getJDialog().requestFocus();
            } else {
                super.onOK();
                if (!this.operatorIsNew) {
                    ToolAdapterActionRegistrar.removeOperatorMenu(oldOperatorDescriptor);
                    ToolAdapterIO.removeOperator(oldOperatorDescriptor, false);
                }
                if (!newOperatorDescriptor.isFromPackage()) {
                    newOperatorDescriptor.setSource(ToolAdapterOperatorDescriptor.SOURCE_USER);
                }
                Map<File, String> templates = new HashMap<>();
                newOperatorDescriptor.setTemplateFileLocation(newOperatorDescriptor.getAlias() + ToolAdapterConstants.TOOL_VELO_TEMPLATE_SUFIX);
                java.util.List<TemplateParameterDescriptor> toolParameterDescriptors = newOperatorDescriptor.getToolParameterDescriptors();
                toolParameterDescriptors.stream().filter(param -> paramsTable.getBindingContext().getBinding(param.getName()) != null)
                        .filter(param -> paramsTable.getBindingContext().getBinding(param.getName()).getPropertyValue() != null)
                        .forEach(param -> {
                            Object propertyValue = paramsTable.getBindingContext().getBinding(param.getName()).getPropertyValue();
                            if (param.isTemplateBefore() || param.isTemplateAfter()) {
                                final File paramTemplateFile = new File(propertyValue.toString());
                                param.setDefaultValue(paramTemplateFile.getName());
                                File fileToAdd;
                                if (!newOperatorDescriptor.getAlias().equals(oldOperatorDescriptor.getAlias())) {
                                    File oldFile = ToolAdapterIO.ensureLocalCopy(paramTemplateFile, oldOperatorDescriptor.getAlias());
                                    fileToAdd = ToolAdapterIO.ensureLocalCopy(oldFile, newOperatorDescriptor.getAlias());
                                } else {
                                    fileToAdd = ToolAdapterIO.ensureLocalCopy(paramTemplateFile, newOperatorDescriptor.getAlias());
                                }
                                try {
                                    templates.put(fileToAdd, new String(Files.readAllBytes(Paths.get(fileToAdd.toURI()))));
                                } catch (IOException e) {
                                    logger.severe(e.getMessage());
                                }
                            } else {
                                String defaultValueString = "";
                                if (propertyValue.getClass().isArray()) {
                                    defaultValueString = String.join(ArrayConverter.SEPARATOR,
                                            Arrays.asList((Object[]) propertyValue).stream().map(Object::toString).collect(Collectors.toList()));
                                } else {
                                    defaultValueString = propertyValue.toString();
                                }
                                param.setDefaultValue(defaultValueString);
                            }
                        });
                java.util.List<TemplateParameterDescriptor> remParameters = toolParameterDescriptors.stream().filter(param ->
                        (ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID.equals(param.getName()) || ToolAdapterConstants.TOOL_SOURCE_PRODUCT_FILE.equals(param.getName()))).
                        collect(Collectors.toList());
                newOperatorDescriptor.removeParamDescriptors(remParameters);
                try {
                    if (rbMenuNew.isSelected()) {
                        String customMenuLocationText = customMenuLocation.getText();
                        if (customMenuLocationText != null && !customMenuLocationText.isEmpty()) {
                            newOperatorDescriptor.setMenuLocation(customMenuLocationText);
                        }
                    }
                    String menuLocation = newOperatorDescriptor.getMenuLocation();
                    if (menuLocation != null && !menuLocation.startsWith("Menu/")) {
                        newOperatorDescriptor.setMenuLocation("Menu/" + menuLocation);
                    }
                    ToolAdapterIO.removeOperator(oldOperatorDescriptor, true);
                    ToolAdapterIO.saveAndRegisterOperator(newOperatorDescriptor, templateContent);
                    templates.keySet().stream().forEach(k -> {
                        if (!k.exists()) {
                            try {
                                if (k.createNewFile()) {
                                    Files.write(Paths.get(k.toURI()), templates.get(k).getBytes(), StandardOpenOption.WRITE);
                                }
                            } catch (IOException e) {
                                logger.severe(e.getMessage());
                            }
                        }
                    });
                    ToolAdapterActionRegistrar.registerOperatorMenu(newOperatorDescriptor);
                } catch (Exception e) {
                    logger.warning(e.getMessage());
                    SnapDialogs.showError(e.getMessage());
                }
            }
        }
    }

    @Override
    public int show() {
        getJDialog().revalidate();
        return super.show();
    }

    @Override
    protected void onOther() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showOpenDialog(getButton(ID_OTHER)) == JFileChooser.APPROVE_OPTION) {
                File targetFolder = fileChooser.getSelectedFile();
                newOperatorDescriptor.setSource(ToolAdapterOperatorDescriptor.SOURCE_PACKAGE);
                onOK();
                ModulePackager.packModule(newOperatorDescriptor, new File(targetFolder, newOperatorDescriptor.getAlias() + ".nbm"));
                SnapDialogs.showInformation(String.format(Bundle.MSG_Export_Complete_Text(), targetFolder.getAbsolutePath()), null);
            }
        } catch (IOException e) {
            logger.warning(e.getMessage());
            SnapDialogs.showError(e.getMessage());
        }
    }

    protected JComponent createCheckboxComponent(String memberName, JComponent toogleComponentEnabled, Boolean value) {
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor(memberName);
        PropertyEditor editor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        JComponent editorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);

        if (editorComponent instanceof JCheckBox && toogleComponentEnabled != null) {
            ((JCheckBox) editorComponent).setSelected(value);
            toogleComponentEnabled.setEnabled(value);
            ((JCheckBox) editorComponent).addActionListener(e -> toogleComponentEnabled.setEnabled(((JCheckBox) editorComponent).isSelected()));
        }

        return editorComponent;
    }

    protected void addValidatedTextField(JPanel parent, TextFieldEditor textEditor, String labelText, String propertyName, String validatorRegex) {
        if(validatorRegex == null || validatorRegex.isEmpty()){
            addTextField(parent, textEditor, labelText, propertyName, false);
        } else {
            parent.add(new JLabel(labelText));
            PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor(propertyName);
            propertyDescriptor.setValidator(new PatternValidator(Pattern.compile(validatorRegex)));
            JComponent editorComponent = textEditor.createEditorComponent(propertyDescriptor, bindingContext);
            editorComponent.setPreferredSize(new Dimension(editorComponent.getPreferredSize().width, controlHeight));
            editorComponent.setMaximumSize(new Dimension(editorComponent.getMaximumSize().width, controlHeight));
            parent.add(editorComponent);
        }
    }

    protected void addTextField(JPanel parent, TextFieldEditor textEditor, String labelText, String propertyName, boolean isRequired) {
        parent.add(new JLabel(labelText));
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor(propertyName);
        if (isRequired) {
            propertyDescriptor.setValidator(new NotEmptyValidator());
        }
        JComponent editorComponent = textEditor.createEditorComponent(propertyDescriptor, bindingContext);
        editorComponent.setPreferredSize(new Dimension(editorComponent.getPreferredSize().width, controlHeight));
        editorComponent.setMaximumSize(new Dimension(editorComponent.getMaximumSize().width, controlHeight));
        parent.add(editorComponent);
    }

    protected void addComboField(JPanel parent, String labelText, String propertyName, java.util.List<String> values, boolean sortValues, boolean isRequired) {
        parent.add(new JLabel(labelText));

        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor(propertyName);
        propertyDescriptor.setNotEmpty(isRequired);

        if (sortValues) {
            values.sort(Comparator.<String>naturalOrder());
        }
        propertyDescriptor.setValueSet(new ValueSet(values.toArray()));
        PropertyEditor editor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        JComponent editorComp = editor.createEditorComponent(propertyDescriptor, bindingContext);
        if (editorComp instanceof JComboBox) {
            JComboBox comboBox = (JComboBox)editorComp;
            comboBox.setEditable(true);
        }
        editorComp.setMaximumSize(new Dimension(editorComp.getMaximumSize().width, controlHeight));

        customMenuLocation = new JTextField();
        customMenuLocation.setInputVerifier(new RequiredFieldValidator(Bundle.MSG_Empty_MenuLocation_Text()));
        //customMenuLocation.setPreferredSize(new Dimension(250, controlHeight));
        customMenuLocation.setEnabled(false);

        JPanel subPanel = new JPanel(new SpringLayout());
        subPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        JRadioButton rbExistingMenu = new JRadioButton(Bundle.CTL_Label_RadioButton_ExistingMenus(), true);
        rbMenuNew = new JRadioButton(Bundle.CTL_Label_RadioButton_NewMenu());
        ButtonGroup rbGroup = new ButtonGroup();
        rbGroup.add(rbExistingMenu);
        rbGroup.add(rbMenuNew);
        // this radio button should be able to capture focus even when the validator
        // of the rbMenuNew says otherwise
        rbExistingMenu.setVerifyInputWhenFocusTarget(false);
        rbExistingMenu.addItemListener(e -> {
            editorComp.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            customMenuLocation.setEnabled(e.getStateChange() == ItemEvent.DESELECTED);
        });
        subPanel.add(rbExistingMenu);
        subPanel.add(rbMenuNew);
        subPanel.add(editorComp);
        subPanel.add(customMenuLocation);

        Dimension dimension = new Dimension(parent.getWidth() / 2, controlHeight);
        editorComp.setPreferredSize(dimension);
        customMenuLocation.setPreferredSize(dimension);

        subPanel.setPreferredSize(new Dimension(subPanel.getWidth(), (int)(2.5 * controlHeight)));
        subPanel.setMaximumSize(new Dimension(subPanel.getWidth(), (int) (2.5 * controlHeight)));

        makeCompactGrid(subPanel, 2, 2, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        parent.add(subPanel);
    }

    protected java.util.List<String> getAvailableMenuOptions(FileObject current) {
        java.util.List<String> resultList = new ArrayList<>();
        if (current == null) {
            current = FileUtil.getConfigRoot().getFileObject("Menu");
        }
        FileObject[] children = current.getChildren();
        for (FileObject child : children) {
            String entry = child.getPath();
            if (!(entry.endsWith(".instance") ||
                    entry.endsWith(".shadow") ||
                    entry.endsWith(".xml"))) {
                resultList.add(entry);
                resultList.addAll(getAvailableMenuOptions(child));
            }
        }
        return resultList;
    }

    protected boolean resolveTemplateProductCount(String templateContent) {
        boolean success = true;
        int idx = templateContent.lastIndexOf(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID + "[");
        if (idx > 0) {
            String value = templateContent.substring(idx + (ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID + "[").length(), templateContent.indexOf("]", idx));
            int maxNum = Integer.valueOf(value) + 1;
            if (maxNum > 1) {
                newOperatorDescriptor.setSourceProductCount(maxNum);
            } else {
                success = false;
            }
        } else {
            idx = templateContent.lastIndexOf(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_FILE + "[");
            if (idx > 0) {
                String value = templateContent.substring(idx + (ToolAdapterConstants.TOOL_SOURCE_PRODUCT_FILE + "[").length(), templateContent.indexOf("]", idx));
                int maxNum = Integer.valueOf(value) + 1;
                if (maxNum > 1) {
                    newOperatorDescriptor.setSourceProductCount(maxNum);
                } else {
                    success = false;
                }
            } else {
                newOperatorDescriptor.setSourceProductCount(1);
            }
        }
        return success;
    }

    protected java.util.List<String> getAutocompleteEntries() {
        java.util.List<String> entries = new ArrayList<>();
        entries.addAll(newOperatorDescriptor.getVariables().stream().map(SystemVariable::getKey).collect(Collectors.toList()));
        for (ParameterDescriptor parameterDescriptor : newOperatorDescriptor.getParameterDescriptors()) {
            entries.add(parameterDescriptor.getName());
        }
        entries.sort(Comparator.<String>naturalOrder());
        return entries;
    }
}
