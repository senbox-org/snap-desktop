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

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.validators.NotEmptyValidator;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.internal.TextFieldEditor;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.descriptor.SystemDependentVariable;
import org.esa.snap.core.gpf.descriptor.SystemVariable;
import org.esa.snap.core.gpf.descriptor.TemplateParameterDescriptor;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterConstants;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterIO;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.esa.snap.ui.tooladapter.model.AutoCompleteTextArea;
import org.esa.snap.ui.tooladapter.model.VariablesTable;
import org.esa.snap.ui.tooladapter.validators.RequiredFieldValidator;
import org.esa.snap.utils.SpringUtilities;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.IOException;
import java.text.MessageFormat;

import static org.esa.snap.utils.SpringUtilities.DEFAULT_PADDING;

/**
 * A dialog window used to edit an operator, or to create a new operator.
 * It shows details of an operator such as: descriptor details (name, alias, label, version, copyright,
 * authors, description), system variables, preprocessing tool, product writer, tool location,
 * operator working directory, command line template content, tool output patterns and parameters.
 *
 * @author Cosmin Cara
 */
public class ToolAdapterEditorDialog extends AbstractAdapterEditor {


    public ToolAdapterEditorDialog(AppContext appContext, JDialog parent, ToolAdapterOperatorDescriptor operatorDescriptor, boolean operatorIsNew) {
        super(appContext, parent, operatorDescriptor, operatorIsNew);
    }

    public ToolAdapterEditorDialog(AppContext appContext, JDialog parent, ToolAdapterOperatorDescriptor operatorDescriptor, int newNameIndex) {
        super(appContext, parent, operatorDescriptor, newNameIndex);
    }

    @Override
    protected JSplitPane createMainPanel() {
        JSplitPane mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainPanel.setOneTouchExpandable(false);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double widthRatio = 0.5;
        formWidth = Math.max((int) (Math.min(screenSize.width, MAX_4K_WIDTH) * widthRatio), MIN_WIDTH);
        double heightRatio = 0.6;
        int formHeight = Math.max((int) (Math.min(screenSize.height, MAX_4K_HEIGHT) * heightRatio), MIN_HEIGHT);

        getJDialog().setMinimumSize(new Dimension(formWidth + 16, formHeight + 72));

        // top panel first
        JSplitPane topPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        topPanel.setOneTouchExpandable(false);

        JPanel descriptorPanel = createDescriptorAndVariablesAndPreprocessingPanel();
        Dimension topPanelDimension = new Dimension((int)((formWidth - 3 * DEFAULT_PADDING) * 0.5), (int)((formHeight - 3 * DEFAULT_PADDING) * 0.75));
        descriptorPanel.setMinimumSize(topPanelDimension);
        descriptorPanel.setPreferredSize(topPanelDimension);
        topPanel.setLeftComponent(descriptorPanel);

        JPanel configurationPanel = createToolInfoPanel();
        configurationPanel.setMinimumSize(topPanelDimension);
        configurationPanel.setPreferredSize(topPanelDimension);
        topPanel.setRightComponent(configurationPanel);
        topPanel.setDividerLocation(0.5);

        // bottom panel last
        JPanel bottomPannel = createParametersPanel();
        Dimension bottomPanelDimension = new Dimension(formWidth - 2 * DEFAULT_PADDING, (int)((formHeight - 3 * DEFAULT_PADDING) * 0.25));
        bottomPannel.setMinimumSize(bottomPanelDimension);
        bottomPannel.setPreferredSize(bottomPanelDimension);

        mainPanel.setTopComponent(topPanel);
        mainPanel.setBottomComponent(bottomPannel);
        mainPanel.setDividerLocation(0.75);

        mainPanel.setPreferredSize(new Dimension(formWidth, formHeight));

        mainPanel.revalidate();

        return mainPanel;
    }

    @Override
    protected JPanel createDescriptorPanel() {
        final JPanel descriptorPanel = new JPanel(new SpringLayout());

        TextFieldEditor textEditor = new TextFieldEditor();

        addValidatedTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Alias_Text(), ToolAdapterConstants.ALIAS, "[^\\\\\\?%\\*:\\|\"<>\\./]*");
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_UniqueName_Text(), ToolAdapterConstants.NAME, true);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Label_Text(), ToolAdapterConstants.LABEL, true);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Version_Text(), ToolAdapterConstants.VERSION, true);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Copyright_Text(), ToolAdapterConstants.COPYRIGHT, false);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Authors_Text(), ToolAdapterConstants.AUTHORS, false);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Description_Text(), ToolAdapterConstants.DESCRIPTION, false);

        java.util.List<String> menus = getAvailableMenuOptions(null);
        addComboField(descriptorPanel, Bundle.CTL_Label_MenuLocation_Text(), ToolAdapterConstants.MENU_LOCATION, menus, true, true);

        TitledBorder title = BorderFactory.createTitledBorder(Bundle.CTL_Panel_OperatorDescriptor_Text());
        descriptorPanel.setBorder(title);

        SpringUtilities.makeCompactGrid(descriptorPanel, 8, 2, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        return descriptorPanel;
    }

    @Override
    protected JPanel createVariablesPanel() {
        JPanel variablesBorderPanel = new JPanel();
        BoxLayout layout = new BoxLayout(variablesBorderPanel, BoxLayout.PAGE_AXIS);
        variablesBorderPanel.setLayout(layout);
        variablesBorderPanel.setBorder(BorderFactory.createTitledBorder(Bundle.CTL_Panel_SysVar_Border_TitleText()));

        AbstractButton addVariableButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon(Bundle.Icon_Add()), false);
        addVariableButton.setText(Bundle.CTL_Button_Add_Variable_Text());
        addVariableButton.setMaximumSize(new Dimension(addVariableButton.getWidth(), controlHeight));
        addVariableButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        AbstractButton addDependentVariableButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon(Bundle.Icon_Add()), false);
        addDependentVariableButton.setText(Bundle.CTL_Button_Add_PDVariable_Text());
        addDependentVariableButton.setMaximumSize(new Dimension(addDependentVariableButton.getWidth(), controlHeight));
        addDependentVariableButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel buttonsPannel = new JPanel(new SpringLayout());
        buttonsPannel.add(addVariableButton);
        buttonsPannel.add(addDependentVariableButton);
        SpringUtilities.makeCompactGrid(buttonsPannel, 1, 2, 0, 0, 0, 0);
        buttonsPannel.setAlignmentX(Component.LEFT_ALIGNMENT);
        variablesBorderPanel.add(buttonsPannel);

        VariablesTable varTable = new VariablesTable(newOperatorDescriptor.getVariables(), context);
        varTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        varTable.setRowHeight(20);
        JScrollPane scrollPane = new JScrollPane(varTable);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        variablesBorderPanel.add(scrollPane);
        variablesBorderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension variablesPanelDimension = new Dimension((formWidth - 3 * DEFAULT_PADDING) / 2 - 2 * DEFAULT_PADDING, 130);
        variablesBorderPanel.setMinimumSize(variablesPanelDimension);
        variablesBorderPanel.setMaximumSize(variablesPanelDimension);
        variablesBorderPanel.setPreferredSize(variablesPanelDimension);

        addVariableButton.addActionListener(e -> {
            newOperatorDescriptor.getVariables().add(new SystemVariable("key", ""));
            varTable.revalidate();
        });

        addDependentVariableButton.addActionListener(e -> {
            newOperatorDescriptor.getVariables().add(new SystemDependentVariable("key", ""));
            varTable.revalidate();
        });

        return variablesBorderPanel;
    }

    @Override
    protected JPanel createPreProcessingPanel() {
        final JPanel preProcessingPanel = new JPanel(new SpringLayout());

        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("preprocessorExternalTool");
        PropertyEditor editor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        JComponent editorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);
        editorComponent.setMaximumSize(new Dimension(editorComponent.getMaximumSize().width, controlHeight));
        editorComponent.setPreferredSize(new Dimension(editorComponent.getPreferredSize().width, controlHeight));

        preProcessingPanel.add(createCheckboxComponent("preprocessTool", editorComponent, newOperatorDescriptor.getPreprocessTool()));
        preProcessingPanel.add(new JLabel(Bundle.CTL_Label_PreprocessingTool_Text()));
        preProcessingPanel.add(editorComponent);

        propertyDescriptor = propertyContainer.getDescriptor("processingWriter");
        editor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        editorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);
        editorComponent.setMaximumSize(new Dimension(editorComponent.getMaximumSize().width, controlHeight));
        editorComponent.setPreferredSize(new Dimension(editorComponent.getPreferredSize().width, controlHeight));

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

    @Override
    protected JPanel createToolInfoPanel() {
        JPanel container = new JPanel(new SpringLayout());

        JPanel configPanel = new JPanel(new SpringLayout());
        configPanel.setBorder(BorderFactory.createTitledBorder(Bundle.CTL_Panel_ConfigParams_Text()));

        JPanel panelToolFiles = new JPanel(new SpringLayout());

        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor(ToolAdapterConstants.MAIN_TOOL_FILE_LOCATION);
        propertyDescriptor.setValidator(new NotEmptyValidator());
        PropertyEditor editor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        JComponent editorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);
        editorComponent.setMaximumSize(new Dimension(editorComponent.getMaximumSize().width, controlHeight));
        editorComponent.setPreferredSize(new Dimension(editorComponent.getPreferredSize().width, controlHeight));

        panelToolFiles.add(new JLabel(Bundle.CTL_Label_ToolLocation_Text()));
        panelToolFiles.add(editorComponent);

        propertyDescriptor = propertyContainer.getDescriptor(ToolAdapterConstants.WORKING_DIR);
        propertyDescriptor.setAttribute("directory", true);
        propertyDescriptor.setValidator((property, value) -> {
            if (value == null || value.toString().trim().isEmpty()) {
                throw new ValidationException(MessageFormat.format("Value for ''{0}'' must not be empty.",
                        property.getDescriptor().getDisplayName()));
            }
        });
        editor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        editorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);
        editorComponent.setMaximumSize(new Dimension(editorComponent.getMaximumSize().width, controlHeight));
        editorComponent.setPreferredSize(new Dimension(editorComponent.getPreferredSize().width, controlHeight));

        panelToolFiles.add(new JLabel(Bundle.CTL_Label_WorkDir_Text()));
        panelToolFiles.add(editorComponent);

        SpringUtilities.makeCompactGrid(panelToolFiles, 2, 2, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        configPanel.add(panelToolFiles);

        JPanel checkPanel = new JPanel(new SpringLayout());

        propertyDescriptor = propertyContainer.getDescriptor(ToolAdapterConstants.HANDLE_OUTPUT);
        editor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        editorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);
        editorComponent.setMaximumSize(new Dimension(editorComponent.getMaximumSize().width, controlHeight));
        editorComponent.setPreferredSize(new Dimension(editorComponent.getPreferredSize().width, controlHeight));

        checkPanel.add(editorComponent);
        checkPanel.add(new JLabel("Tool produces the name of the output product"));

        SpringUtilities.makeCompactGrid(checkPanel, 1, 2, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        configPanel.add(checkPanel);

        JLabel label = new JLabel(Bundle.CTL_Label_CmdLineTemplate_Text());
        configPanel.add(label);

        templateContent = new AutoCompleteTextArea("", 15, 9);
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
        templateContent.setAutoCompleteEntries(getAutocompleteEntries());
        templateContent.setTriggerChar('$');
        JScrollPane scrollPane = new JScrollPane(templateContent);
        configPanel.add(scrollPane);

        SpringUtilities.makeCompactGrid(configPanel, 4, 1, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        container.add(configPanel);
        container.add(createPatternsPanel());
        SpringUtilities.makeCompactGrid(container, 2, 1, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        return container;
    }

    @Override
    protected JPanel createPatternsPanel() {
        JPanel patternsPanel = new JPanel(new SpringLayout());
        patternsPanel.setBorder(BorderFactory.createTitledBorder(Bundle.CTL_Panel_OutputPattern_Border_TitleText()));

        TextFieldEditor textEditor = new TextFieldEditor();
        addTextField(patternsPanel, textEditor, Bundle.CTL_Label_ProgressPattern(), ToolAdapterConstants.PROGRESS_PATTERN, false);
        addTextField(patternsPanel, textEditor, Bundle.CTL_Label_ErrorPattern(), ToolAdapterConstants.ERROR_PATTERN, false);

        SpringUtilities.makeCompactGrid(patternsPanel, 2, 2, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        return patternsPanel;
    }

    @Override
    protected JPanel createParametersPanel() {
        JPanel paramsPanel = new JPanel();
        BoxLayout layout = new BoxLayout(paramsPanel, BoxLayout.PAGE_AXIS);
        paramsPanel.setLayout(layout);
        AbstractButton addParamBut = ToolButtonFactory.createButton(UIUtils.loadImageIcon(Bundle.Icon_Add()), false);
        addParamBut.setAlignmentX(Component.LEFT_ALIGNMENT);
        addParamBut.setAlignmentY(Component.TOP_ALIGNMENT);
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

    private JPanel createDescriptorAndVariablesAndPreprocessingPanel() {
        JPanel descriptorAndVariablesPanel = new JPanel(new SpringLayout());

        descriptorAndVariablesPanel.add(createDescriptorPanel());
        descriptorAndVariablesPanel.add(createVariablesPanel());
        descriptorAndVariablesPanel.add(createPreProcessingPanel());

        SpringUtilities.makeCompactGrid(descriptorAndVariablesPanel, 3, 1, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        return descriptorAndVariablesPanel;
    }



}
