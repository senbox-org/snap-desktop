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
import com.bc.ceres.binding.validators.NotEmptyValidator;
import com.bc.ceres.binding.validators.PatternValidator;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.internal.TextFieldEditor;
import org.esa.snap.framework.dataio.ProductIOPlugInManager;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.gpf.GPF;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.descriptor.*;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterConstants;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterIO;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterOpSpi;
import org.esa.snap.framework.ui.AppContext;
import org.esa.snap.framework.ui.ModalDialog;
import org.esa.snap.framework.ui.UIUtils;
import org.esa.snap.framework.ui.tool.ToolButtonFactory;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.ui.tooladapter.actions.ExecuteToolAdapterAction;
import org.esa.snap.ui.tooladapter.actions.ToolAdapterActionRegistrar;
import org.esa.snap.ui.tooladapter.model.OperatorParametersTable;
import org.esa.snap.ui.tooladapter.model.VariablesTable;
import org.esa.snap.ui.tooladapter.validators.RequiredFieldValidator;
import org.esa.snap.utils.JarPackager;
import org.esa.snap.utils.SpringUtilities;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A dialog window used to edit an operator, or to create a new operator.
 * It shows details of an operator such as: descriptor details (name, alias, label, version, copyright,
 * authors, description), system variables, preprocessing tool, product writer, tool location,
 * operator working directory, command line template content, tool output patterns and parameters.
 *
 * @author Ramona Manda
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
        "CTL_Panel_SysVar_Border_TitleText=System variables",
        "Icon_Add=/org/esa/snap/resources/images/icons/Add16.png",
        "CTL_Panel_OpParams_Border_TitleText=Operator Parameters",
        "CTL_Button_Export_Text=Export as module",
        "MSG_Export_Complete_Text=The adapter was exported as a NetBeans module in %s",
        "MSG_Inexistent_Tool_Path_Text=The tool executable does not exist.\n" +
                "Please specify the location of an existing executable.",
        "MSG_Inexistent_WorkDir_Text=The working directory does not exist.\n" +
                "Please specify a valid location.",
        "MSG_Inexistem_Parameter_Value_Text=The file or folder for parameter %s does not exist.\n" +
                "Please specify a valid location or change the %s property of the parameter."
})
public class ToolAdapterEditorDialog extends ModalDialog {

    public static final String MESSAGE_REQUIRED = "This field is required";
    private ToolAdapterOperatorDescriptor oldOperatorDescriptor;
    private ToolAdapterOperatorDescriptor newOperatorDescriptor;
    private boolean operatorIsNew = false;
    private int newNameIndex = -1;
    private PropertyContainer propertyContainer;
    private BindingContext bindingContext;
    private JTextArea templateContent;
    private OperatorParametersTable paramsTable;
    private Logger logger;
    public static final String helpID = "sta_editor";

    private int formWidth;
    private final int DEFAULT_PADDING = 3;

    private ToolAdapterEditorDialog(AppContext appContext, String title) {
        super(appContext.getApplicationWindow(), title, ID_OK_CANCEL_HELP, new Object[] { new JButton(Bundle.CTL_Button_Export_Text()) }, helpID);
        //super(appContext.getApplicationWindow(), title, ID_OK_CANCEL, null, helpID);
        this.logger = Logger.getLogger(ToolAdapterEditorDialog.class.getName());
        getJDialog().setResizable(false);
        this.registerButton(ID_OTHER, new JButton(Bundle.CTL_Button_Export_Text()));
    }

    private ToolAdapterEditorDialog(AppContext appContext, ToolAdapterOperatorDescriptor operatorDescriptor) {
        this(appContext, operatorDescriptor.getAlias());
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
            newOperatorDescriptor.getToolParameterDescriptors().add(new TemplateParameterDescriptor(ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE, File.class));
        }

        propertyContainer = PropertyContainer.createObjectBacked(newOperatorDescriptor);
        ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();
        String[] writers = registry.getAllProductWriterFormatStrings();
        Arrays.sort(writers);
        propertyContainer.getDescriptor("processingWriter").setValueSet(new ValueSet(writers));
        Set<OperatorSpi> spis = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpis();
        java.util.List<String> toolboxSpis = new ArrayList<>();
        spis.stream().filter(p -> (p instanceof ToolAdapterOpSpi)
                                && (p.getOperatorDescriptor().getClass() != AnnotationOperatorDescriptor.class)
                                && !p.getOperatorAlias().equals(oldOperatorDescriptor.getAlias()))
                     .forEach(operator -> toolboxSpis.add(operator.getOperatorDescriptor().getAlias()));
        toolboxSpis.sort(Comparator.<String>naturalOrder());
        propertyContainer.getDescriptor("preprocessorExternalTool").setValueSet(new ValueSet(toolboxSpis.toArray(new String[toolboxSpis.size()])));

        bindingContext = new BindingContext(propertyContainer);

        paramsTable =  new OperatorParametersTable(newOperatorDescriptor, appContext);
    }

    /**
     * Constructs a new window for editing the operator
     * @param appContext the application context
     * @param operatorDescriptor the descriptor of the operator to be edited
     * @param operatorIsNew true if the operator was not previously registered (so it is a new operator) and false if the operator was registered and the editing operation is requested
     */
    public ToolAdapterEditorDialog(AppContext appContext, ToolAdapterOperatorDescriptor operatorDescriptor, boolean operatorIsNew) {
        this(appContext, operatorDescriptor);
        this.operatorIsNew = operatorIsNew;
        this.newNameIndex = -1;
        setContent(createMainPanel());
    }

    /**
     * Constructs a new window for editing the operator
     * @param appContext the application context
     * @param operatorDescriptor the descriptor of the operator to be edited
     * @param newNameIndex an integer value representing the suffix for the new operator name; if this value is less than 1, the editing operation of the current operator is executed; if the value is equal to or greater than 1, the operator is duplicated and the index value is used to compute the name of the new operator
     */
    public ToolAdapterEditorDialog(AppContext appContext, ToolAdapterOperatorDescriptor operatorDescriptor, int newNameIndex) {
        this(appContext, operatorDescriptor);
        this.newNameIndex = newNameIndex;
        this.operatorIsNew = this.newNameIndex >= 1;
        if(this.newNameIndex >= 1) {
            this.newOperatorDescriptor.setName(this.oldOperatorDescriptor.getName() + ToolAdapterConstants.OPERATOR_GENERATED_NAME_SEPARATOR + this.newNameIndex);
            this.newOperatorDescriptor.setAlias(this.oldOperatorDescriptor.getAlias() + ToolAdapterConstants.OPERATOR_GENERATED_NAME_SEPARATOR + this.newNameIndex);
        }
        setContent(createMainPanel());
    }

    @Override
    protected boolean verifyUserInput() {
        Path toolLocation = newOperatorDescriptor.getExpandedLocation(newOperatorDescriptor.getMainToolFileLocation()).toPath();
        if (!(Files.exists(toolLocation) && Files.isExecutable(toolLocation))) {
            SnapDialogs.showWarning(Bundle.MSG_Inexistent_Tool_Path_Text());
            return false;
        }
        File workingDir = newOperatorDescriptor.getExpandedLocation(newOperatorDescriptor.getWorkingDir());
        if (!(workingDir != null && workingDir.exists() && workingDir.isDirectory())) {
            SnapDialogs.showWarning(Bundle.MSG_Inexistent_WorkDir_Text());
            return false;
        }
        ParameterDescriptor[] parameterDescriptors = newOperatorDescriptor.getParameterDescriptors();
        if (parameterDescriptors != null && parameterDescriptors.length > 0) {
            for (ParameterDescriptor parameterDescriptor : parameterDescriptors) {
                Class<?> dataType = parameterDescriptor.getDataType();
                String defaultValue = parameterDescriptor.getDefaultValue();
                if (File.class.isAssignableFrom(dataType) &&
                        (parameterDescriptor.isNotNull() || parameterDescriptor.isNotEmpty()) &&
                        (defaultValue == null || defaultValue.isEmpty() || !Files.exists(Paths.get(defaultValue)))) {
                    SnapDialogs.showWarning(String.format(Bundle.MSG_Inexistem_Parameter_Value_Text(),
                            parameterDescriptor.getName(), parameterDescriptor.isNotNull() ? "NotNull" : "NotEmpty"));
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onOK() {
        if (!verifyUserInput()) {

            this.getJDialog().requestFocus();
        } else {
            super.onOK();
            if (!this.operatorIsNew) {
                ToolAdapterActionRegistrar.removeOperatorMenu(oldOperatorDescriptor);
                ToolAdapterIO.removeOperator(oldOperatorDescriptor);
            }
            String oldOperatorName = oldOperatorDescriptor.getName();
            if (oldOperatorDescriptor.isSystem() && oldOperatorName.equals(newOperatorDescriptor.getName())) {
                newOperatorDescriptor.setName(newOperatorDescriptor.getName() + ".custom");
                newOperatorDescriptor.setAlias(newOperatorDescriptor.getAlias() + "-custom");
            }
            newOperatorDescriptor.setSystem(false);
            newOperatorDescriptor.setTemplateFileLocation(newOperatorDescriptor.getAlias() + ToolAdapterConstants.TOOL_VELO_TEMPLATE_SUFIX);
            java.util.List<TemplateParameterDescriptor> toolParameterDescriptors = newOperatorDescriptor.getToolParameterDescriptors();
            toolParameterDescriptors.stream().filter(param -> paramsTable.getBindingContext().getBinding(param.getName()) != null)
                    .filter(param -> paramsTable.getBindingContext().getBinding(param.getName()).getPropertyValue() != null)
                    .forEach(param -> param.setDefaultValue(paramsTable.getBindingContext().getBinding(param.getName())
                            .getPropertyValue().toString()));
            java.util.List<TemplateParameterDescriptor> remParameters = toolParameterDescriptors.stream().filter(param -> ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID.equals(param.getName())).
                    collect(Collectors.toList());
            newOperatorDescriptor.removeParamDescriptors(remParameters);
            try {
                String menuLocation = newOperatorDescriptor.getMenuLocation();
                if (menuLocation != null && !menuLocation.startsWith("Menu/")) {
                    newOperatorDescriptor.setMenuLocation("Menu/" + menuLocation);
                }
                String templateContent = this.templateContent.getText();
                int idx = templateContent.lastIndexOf(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID + "[");
                if (idx > 0) {
                    String value = templateContent.substring(idx + (ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID + "[").length(), templateContent.indexOf("]", idx));
                    int maxNum = Integer.valueOf(value) + 1;
                    newOperatorDescriptor.setSourceProductCount(maxNum);
                } else {
                    idx = templateContent.lastIndexOf(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_FILE + "[");
                    if (idx > 0) {
                        String value = templateContent.substring(idx + (ToolAdapterConstants.TOOL_SOURCE_PRODUCT_FILE + "[").length(), templateContent.indexOf("]", idx));
                        int maxNum = Integer.valueOf(value) + 1;
                        newOperatorDescriptor.setSourceProductCount(maxNum);
                    }
                }
                ToolAdapterIO.saveAndRegisterOperator(newOperatorDescriptor, templateContent);
                ToolAdapterActionRegistrar.registerOperatorMenu(newOperatorDescriptor);
            } catch (Exception e) {
                logger.warning(e.getMessage());
                SnapDialogs.showError(e.getMessage());
            }
        }
    }

    @Override
    protected void onOther() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showOpenDialog(getButton(ID_OTHER)) == JFileChooser.APPROVE_OPTION) {
                File targetFolder = fileChooser.getSelectedFile();
                JarPackager.packAdapterJar(newOperatorDescriptor, new File(targetFolder, newOperatorDescriptor.getAlias() + ".jar"), ExecuteToolAdapterAction.class);
                SnapDialogs.showInformation(String.format(Bundle.MSG_Export_Complete_Text(), targetFolder.getAbsolutePath()), null);
            }
        } catch (IOException e) {
            logger.warning(e.getMessage());
            SnapDialogs.showError(e.getMessage());
        }
    }

    private JPanel createMainPanel() {
        JPanel toolDescriptorPanel = new JPanel();

        SpringLayout springLayout = new SpringLayout();
        toolDescriptorPanel.setLayout(springLayout);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double widthRatio = 0.5;
        formWidth = (int) (screenSize.width * widthRatio);
        double heightRatio = 0.6;
        int formHeight = (int) (screenSize.height * heightRatio);
        toolDescriptorPanel.setPreferredSize(new Dimension(formWidth, formHeight));

        JPanel topLeftPanel = createDescriptorAndVariablesAndPreprocessingPanel();
        Dimension topPanelDimension = new Dimension((int)((formWidth - 3 * DEFAULT_PADDING) * 0.5), (int)((formHeight - 3 * DEFAULT_PADDING) * 0.66 ));
        topLeftPanel.setMinimumSize(topPanelDimension);
        topLeftPanel.setMaximumSize(topPanelDimension);
        topLeftPanel.setPreferredSize(topPanelDimension);
        toolDescriptorPanel.add(topLeftPanel);

        JPanel topRightPanel = createProcessingPanel();
        topRightPanel.setMinimumSize(topPanelDimension);
        topRightPanel.setMaximumSize(topPanelDimension);
        topRightPanel.setPreferredSize(topPanelDimension);
        toolDescriptorPanel.add(topRightPanel);

        JPanel bottomPannel = createParametersPanel();
        Dimension bottomPanelDimension = new Dimension((int)(formWidth - 2 * DEFAULT_PADDING), (int)((formHeight - 3 * DEFAULT_PADDING) * 0.34));
        bottomPannel.setMinimumSize(bottomPanelDimension);
        bottomPannel.setMaximumSize(bottomPanelDimension);
        bottomPannel.setPreferredSize(bottomPanelDimension);
        toolDescriptorPanel.add(bottomPannel);

        springLayout.putConstraint(SpringLayout.WEST, topLeftPanel, DEFAULT_PADDING, SpringLayout.WEST, toolDescriptorPanel);
        springLayout.putConstraint(SpringLayout.WEST, topRightPanel, DEFAULT_PADDING, SpringLayout.EAST, topLeftPanel);
        springLayout.putConstraint(SpringLayout.EAST, topRightPanel, DEFAULT_PADDING, SpringLayout.EAST, toolDescriptorPanel);
        springLayout.putConstraint(SpringLayout.SOUTH, topLeftPanel, 0, SpringLayout.SOUTH, topRightPanel);
        springLayout.putConstraint(SpringLayout.WEST, bottomPannel, DEFAULT_PADDING, SpringLayout.WEST, toolDescriptorPanel);
        springLayout.putConstraint(SpringLayout.EAST, bottomPannel, DEFAULT_PADDING, SpringLayout.EAST, toolDescriptorPanel);

        springLayout.putConstraint(SpringLayout.NORTH, topLeftPanel, DEFAULT_PADDING, SpringLayout.NORTH, toolDescriptorPanel);
        springLayout.putConstraint(SpringLayout.NORTH, topRightPanel, DEFAULT_PADDING, SpringLayout.NORTH, toolDescriptorPanel);

        springLayout.putConstraint(SpringLayout.NORTH, bottomPannel, DEFAULT_PADDING, SpringLayout.SOUTH, topLeftPanel);
        springLayout.putConstraint(SpringLayout.SOUTH, bottomPannel, DEFAULT_PADDING, SpringLayout.SOUTH, toolDescriptorPanel);

        return toolDescriptorPanel;
    }

    private JPanel createOperatorDescriptorPanel() {
        final JPanel descriptorPanel = new JPanel(new SpringLayout());

        TextFieldEditor textEditor = new TextFieldEditor();

        addValidatedTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Alias_Text(), "alias", 0, "[^\\\\\\?%\\*:\\|\"<>\\./]*");
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_UniqueName_Text(), "name", 1, true);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Label_Text(), "label", 2, true);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Version_Text(), "version", 3, true);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Copyright_Text(), "copyright", 4, false);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Authors_Text(), "authors", 5, false);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Description_Text(), "description", 6, false);

        java.util.List<String> menus = new ArrayList<>();
        getAvailableMenuOptions(null, menus);
        addComboField(descriptorPanel, textEditor, Bundle.CTL_Label_MenuLocation_Text(), "menuLocation", menus, true, 7, true);

        TitledBorder title = BorderFactory.createTitledBorder(Bundle.CTL_Panel_OperatorDescriptor_Text());
        descriptorPanel.setBorder(title);
        SpringUtilities.makeCompactGrid(descriptorPanel, 8, 2, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        return descriptorPanel;
    }

    private JPanel createPreProcessingPanel(){
        final JPanel preProcessingPanel = new JPanel(new SpringLayout());

        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("preprocessorExternalTool");
        PropertyEditor editor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        JComponent editorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);

        preProcessingPanel.add(createCheckboxComponent("preprocessTool", editorComponent, newOperatorDescriptor.getPreprocessTool()));
        preProcessingPanel.add(new JLabel(Bundle.CTL_Label_PreprocessingTool_Text()));
        preProcessingPanel.add(editorComponent);

        propertyDescriptor = propertyContainer.getDescriptor("processingWriter");
        editor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        editorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);

        JComponent writeComponent = createCheckboxComponent("writeForProcessing", editorComponent, newOperatorDescriptor.shouldWriteBeforeProcessing());
        if(writeComponent instanceof JCheckBox){
            ((JCheckBox) writeComponent).addActionListener(e -> {
                //noinspection StatementWithEmptyBody
                if (((JCheckBox) writeComponent).isSelected()){

                }
            });
        }
        preProcessingPanel.add(writeComponent);
        preProcessingPanel.add(new JLabel(Bundle.CTL_Label_WriteBefore_Text()));
        preProcessingPanel.add(editorComponent);

        TitledBorder title = BorderFactory.createTitledBorder(Bundle.CTL_Panel_PreProcessing_Border_TitleText());
        preProcessingPanel.setBorder(title);

        SpringUtilities.makeCompactGrid(preProcessingPanel, 2, 3, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        return preProcessingPanel;
    }

    private JPanel createProcessingPanel() {
        final JPanel processingPanel = new JPanel(new SpringLayout());

        JPanel configPanel = new JPanel(new SpringLayout());
        configPanel.setBorder(BorderFactory.createTitledBorder(Bundle.CTL_Panel_ConfigParams_Text()));

        JPanel panelToolFiles = new JPanel(new SpringLayout());

        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("mainToolFileLocation");
        propertyDescriptor.setValidator(new NotEmptyValidator());
        PropertyEditor editor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        JComponent editorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);

        panelToolFiles.add(new JLabel(Bundle.CTL_Label_ToolLocation_Text()));
        panelToolFiles.add(editorComponent);

        propertyDescriptor = propertyContainer.getDescriptor("workingDir");
        propertyDescriptor.setAttribute("directory", true);
        propertyDescriptor.setValidator(new NotEmptyValidator());
        editor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        editorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);

        panelToolFiles.add(new JLabel(Bundle.CTL_Label_WorkDir_Text()));
        panelToolFiles.add(editorComponent);

        Dimension panelToolDimension = new Dimension(configPanel.getWidth(), 50);
        panelToolFiles.setMinimumSize(panelToolDimension);
        panelToolFiles.setMaximumSize(panelToolDimension);
        panelToolFiles.setPreferredSize(panelToolDimension);

        SpringUtilities.makeCompactGrid(panelToolFiles, 2, 2, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        configPanel.add(panelToolFiles);
        configPanel.add(new JLabel(Bundle.CTL_Label_CmdLineTemplate_Text()));

        templateContent = new JTextArea("", 16, 9);
        try {
            if (operatorIsNew) {
                if (oldOperatorDescriptor.getTemplateFileLocation() != null) {
                    templateContent.setText(ToolAdapterIO.readOperatorTemplate(oldOperatorDescriptor.getName()));
                }
            } else {
                templateContent.setText(ToolAdapterIO.readOperatorTemplate(newOperatorDescriptor.getName()));
            }
        } catch (IOException | OperatorException e) {
            logger.warning(e.getMessage());
        }
        templateContent.setInputVerifier(new RequiredFieldValidator(MESSAGE_REQUIRED));
        configPanel.add(new JScrollPane(templateContent));

        SpringUtilities.makeCompactGrid(configPanel, 3, 1, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        processingPanel.add(configPanel);
        processingPanel.add(createProgressPatternsPanel());

        SpringUtilities.makeCompactGrid(processingPanel, 2, 1, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        return processingPanel;
    }

    private JPanel createProgressPatternsPanel(){
        JPanel patternsPanel = new JPanel(new SpringLayout());
        patternsPanel.setBorder(BorderFactory.createTitledBorder(Bundle.CTL_Panel_OutputPattern_Border_TitleText()));

        TextFieldEditor textEditor = new TextFieldEditor();
        addTextField(patternsPanel, textEditor, Bundle.CTL_Label_ProgressPattern(), "progressPattern", 0, false);
        addTextField(patternsPanel, textEditor, Bundle.CTL_Label_ErrorPattern(), "errorPattern", 1, false);

        SpringUtilities.makeCompactGrid(patternsPanel, 2, 2, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        return patternsPanel;
    }

    private JPanel createDescriptorAndVariablesAndPreprocessingPanel() {
        JPanel descriptorAndVariablesPanel = new JPanel(new SpringLayout());

        JPanel descriptorPanel = createOperatorDescriptorPanel();
        descriptorAndVariablesPanel.add(descriptorPanel);

        JPanel variablesBorderPanel = new JPanel();
        BoxLayout layout = new BoxLayout(variablesBorderPanel, BoxLayout.PAGE_AXIS);
        variablesBorderPanel.setLayout(layout);
        variablesBorderPanel.setBorder(BorderFactory.createTitledBorder(Bundle.CTL_Panel_SysVar_Border_TitleText()));
        AbstractButton addVariableBut = ToolButtonFactory.createButton(UIUtils.loadImageIcon(Bundle.Icon_Add()), false);
        addVariableBut.setMaximumSize(new Dimension(20, 20));
        addVariableBut.setAlignmentX(Component.LEFT_ALIGNMENT);
        variablesBorderPanel.add(addVariableBut);
        VariablesTable varTable = new VariablesTable(newOperatorDescriptor.getVariables());
        varTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        varTable.setRowHeight(20);
        JScrollPane scrollPane = new JScrollPane(varTable);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        variablesBorderPanel.add(scrollPane);
        variablesBorderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension variablesPanelDimension = new Dimension((formWidth - 3 * DEFAULT_PADDING) / 2 - 2 * DEFAULT_PADDING, 120);
        variablesBorderPanel.setMinimumSize(variablesPanelDimension);
        variablesBorderPanel.setMaximumSize(variablesPanelDimension);
        variablesBorderPanel.setPreferredSize(variablesPanelDimension);

        descriptorAndVariablesPanel.add(variablesBorderPanel);

        JPanel preprocessingPanel = createPreProcessingPanel();
        descriptorAndVariablesPanel.add(preprocessingPanel);

        addVariableBut.addActionListener(e -> {
            newOperatorDescriptor.getVariables().add(new SystemVariable("key", ""));
            varTable.revalidate();
        });

        SpringUtilities.makeCompactGrid(descriptorAndVariablesPanel, 3, 1, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        return descriptorAndVariablesPanel;
    }

    private JPanel createParametersPanel() {
        JPanel paramsPanel = new JPanel();
        BoxLayout layout = new BoxLayout(paramsPanel, BoxLayout.PAGE_AXIS);
        paramsPanel.setLayout(layout);
        AbstractButton addParamBut = ToolButtonFactory.createButton(UIUtils.loadImageIcon(Bundle.Icon_Add()), false);
        addParamBut.setAlignmentX(Component.LEFT_ALIGNMENT);
        paramsPanel.add(addParamBut);
        int tableWidth = (formWidth - 2 * DEFAULT_PADDING);
        int widths[] = {27, 120, (int)(tableWidth * 0.25), (int)(tableWidth * 0.1), 100, (int)(tableWidth * 0.32), 30};
        for(int i=0; i < widths.length; i++) {
            paramsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        }
        JScrollPane tableScrollPane = new JScrollPane(paramsTable);
        tableScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        paramsPanel.add(tableScrollPane);
        addParamBut.addActionListener(e -> paramsTable.addParameterToTable(new TemplateParameterDescriptor("parameterName", String.class)));
        TitledBorder title = BorderFactory.createTitledBorder(Bundle.CTL_Panel_OpParams_Border_TitleText());
        paramsPanel.setBorder(title);
        return paramsPanel;
    }

    private JComponent createCheckboxComponent(String memberName, JComponent toogleComponentEnabled, Boolean value) {
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

    private void addValidatedTextField(JPanel parent, TextFieldEditor textEditor, String labelText, String propertyName, int row, String validatorRegex) {
        if(validatorRegex == null || validatorRegex.isEmpty()){
            addTextField(parent, textEditor, labelText, propertyName, row, false);
        } else {
            parent.add(new JLabel(labelText));
            PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor(propertyName);
            propertyDescriptor.setValidator(new PatternValidator(Pattern.compile(validatorRegex)));
            JComponent editorComponent = textEditor.createEditorComponent(propertyDescriptor, bindingContext);
            parent.add(editorComponent);
        }
    }

    private void addTextField(JPanel parent, TextFieldEditor textEditor, String labelText, String propertyName, int row, boolean isRequired) {
        parent.add(new JLabel(labelText));
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor(propertyName);
        if (isRequired) {
            propertyDescriptor.setValidator(new NotEmptyValidator());
        }
        JComponent editorComponent = textEditor.createEditorComponent(propertyDescriptor, bindingContext);
        parent.add(editorComponent);
    }

    private void addComboField(JPanel parent, TextFieldEditor textEditor, String labelText, String propertyName, java.util.List<String> values, boolean sortValues, int row, boolean isRequired) {
        parent.add(new JLabel(labelText));
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor(propertyName);
        if (isRequired) {
            propertyDescriptor.setValidator(new NotEmptyValidator());
        }
        if (sortValues) {
            values.sort(Comparator.<String>naturalOrder());
        }
        propertyDescriptor.setValueSet(new ValueSet(values.toArray()));
        PropertyEditor editor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        JComponent editorComp = editor.createEditorComponent(propertyDescriptor, bindingContext);
        parent.add(editorComp);
    }

    private void getAvailableMenuOptions(FileObject current, java.util.List<String> resultList) {
        if (resultList == null) {
            resultList = new ArrayList<>();
        }
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
                getAvailableMenuOptions(child, resultList);
            }
        }
    }
}
