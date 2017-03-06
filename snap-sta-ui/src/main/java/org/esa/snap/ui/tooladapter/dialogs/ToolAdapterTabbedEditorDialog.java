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
import org.esa.snap.core.gpf.descriptor.SystemDependentVariable;
import org.esa.snap.core.gpf.descriptor.SystemVariable;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.dependency.BundleType;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterActivator;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterConstants;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.esa.snap.ui.tooladapter.model.OperationType;
import org.esa.snap.ui.tooladapter.validators.RegexFieldValidator;
import org.esa.snap.utils.SpringUtilities;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;

import static org.esa.snap.utils.SpringUtilities.DEFAULT_PADDING;

/**
 * A tabbed dialog window used to edit an operator, or to create a new operator.
 * It shows details of an operator such as: descriptor details (name, alias, label, version, copyright,
 * authors, description), system variables, preprocessing tool, product writer, tool location,
 * operator working directory, command line template content, tool output patterns and parameters.
 *
 * @author Ramona Manda
 * @author Cosmin Cara
 */
public class ToolAdapterTabbedEditorDialog extends AbstractAdapterEditor {
    private JTabbedPane tabbedPane;

    public ToolAdapterTabbedEditorDialog(AppContext appContext, JDialog parent, ToolAdapterOperatorDescriptor operatorDescriptor, OperationType operation) {
        super(appContext, parent, operatorDescriptor, operation);
    }

    public ToolAdapterTabbedEditorDialog(AppContext appContext, JDialog parent, ToolAdapterOperatorDescriptor operatorDescriptor, int newNameIndex, OperationType operation) {
        super(appContext, parent, operatorDescriptor, newNameIndex, operation);
    }

    @Override
    protected JTabbedPane createMainPanel() {
        this.tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        this.tabbedPane.setBorder(BorderFactory.createEmptyBorder());
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double widthRatio = 0.5;
        formWidth = Math.max((int) (screenSize.width * widthRatio), MIN_TABBED_WIDTH);
        double heightRatio = 0.5;
        int formHeight = Math.max((int) (screenSize.height * heightRatio), MIN_TABBED_HEIGHT);
        tabbedPane.setPreferredSize(new Dimension(formWidth, formHeight));
        getJDialog().setMinimumSize(new Dimension(formWidth + 16, formHeight + 72));

        addTab(tabbedPane, Bundle.CTL_Panel_OperatorDescriptor_Text(), createDescriptorTab());
        addTab(tabbedPane, Bundle.CTL_Panel_ConfigParams_Text(), createToolInfoPanel());
        addTab(tabbedPane, Bundle.CTL_Panel_PreProcessing_Border_TitleText(), createPreProcessingTab());
        addTab(tabbedPane, Bundle.CTL_Panel_OpParams_Border_TitleText(), createParametersTab(formWidth));
        addTab(tabbedPane, Bundle.CTL_Panel_SysVar_Border_TitleText(), createVariablesPanel());
        addTab(tabbedPane, "Bundled Binaries", createBundlePanel());

        tabbedPane.setUI(new BasicTabbedPaneUI());

        formWidth = tabbedPane.getTabComponentAt(0).getWidth();

        return tabbedPane;
    }

    @Override
    protected JPanel createDescriptorPanel() {
        JPanel descriptorPanel = new JPanel(new SpringLayout());
        TextFieldEditor textEditor = new TextFieldEditor();

        addValidatedTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Alias_Text(), ToolAdapterConstants.ALIAS, "[^\\\\\\?%\\*:\\|\"<>\\./]*");
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_UniqueName_Text(), ToolAdapterConstants.NAME, true);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Label_Text(), ToolAdapterConstants.LABEL, true);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Version_Text(), ToolAdapterConstants.VERSION, true);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Copyright_Text(), ToolAdapterConstants.COPYRIGHT, false);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Authors_Text(), ToolAdapterConstants.AUTHORS, false);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Description_Text(), ToolAdapterConstants.DESCRIPTION, false);

        propertyContainer.addPropertyChangeListener(ToolAdapterConstants.ALIAS, evt -> propertyContainer.setValue(ToolAdapterConstants.NAME, ToolAdapterConstants.OPERATOR_NAMESPACE + evt.getNewValue().toString()));

        java.util.List<String> menus = getAvailableMenuOptions(null);
        addComboField(descriptorPanel, Bundle.CTL_Label_MenuLocation_Text(), ToolAdapterConstants.MENU_LOCATION, menus, true, true);

        addComboField(descriptorPanel, Bundle.CTL_Label_TemplateType_Text(), ToolAdapterConstants.TEMPLATE_TYPE, true, false);

        SpringUtilities.makeCompactGrid(descriptorPanel, 9, 2, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);
        return descriptorPanel;
    }

    @Override
    protected JPanel createVariablesPanel() {
        JPanel variablesBorderPanel = new JPanel();
        BoxLayout layout = new BoxLayout(variablesBorderPanel, BoxLayout.PAGE_AXIS);
        variablesBorderPanel.setLayout(layout);

        AbstractButton addVariableButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon(Bundle.Icon_Add()), false);
        addVariableButton.setText(Bundle.CTL_Button_Add_Variable_Text());
        addVariableButton.setMaximumSize(new Dimension(150, controlHeight));
        addVariableButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        AbstractButton addDependentVariableButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon(Bundle.Icon_Add()), false);
        addDependentVariableButton.setText(Bundle.CTL_Button_Add_PDVariable_Text());
        addDependentVariableButton.setMaximumSize(new Dimension(250, controlHeight));
        addDependentVariableButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel buttonsPannel = new JPanel(new SpringLayout());
        buttonsPannel.add(addVariableButton);
        buttonsPannel.add(addDependentVariableButton);
        SpringUtilities.makeCompactGrid(buttonsPannel, 1, 2, 0, 0, 0, 0);
        buttonsPannel.setAlignmentX(Component.LEFT_ALIGNMENT);
        variablesBorderPanel.add(buttonsPannel);

        varTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        varTable.setRowHeight(controlHeight);
        int widths[] = {controlHeight, (int) (formWidth * 0.3), (int) (formWidth * 0.7) - controlHeight};
        for (int i = 0; i < widths.length; i++) {
            varTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        }
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
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("preprocessorExternalTool");
        PropertyEditor editor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        JComponent firstEditorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);
        firstEditorComponent.setMaximumSize(new Dimension(firstEditorComponent.getMaximumSize().width, controlHeight));
        firstEditorComponent.setPreferredSize(new Dimension(firstEditorComponent.getPreferredSize().width, controlHeight));

        propertyDescriptor = propertyContainer.getDescriptor("processingWriter");
        editor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        JComponent secondEditorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);
        secondEditorComponent.setMaximumSize(new Dimension(secondEditorComponent.getMaximumSize().width, controlHeight));
        secondEditorComponent.setPreferredSize(new Dimension(secondEditorComponent.getPreferredSize().width, controlHeight));

        JCheckBox checkBoxComponent = (JCheckBox)createCheckboxComponent("preprocessTool", firstEditorComponent, newOperatorDescriptor.getPreprocessTool());
        checkBoxComponent.setText(Bundle.CTL_Label_PreprocessingTool_Text());

        JCheckBox writeComponent = (JCheckBox)createCheckboxComponent("writeForProcessing", secondEditorComponent, newOperatorDescriptor.shouldWriteBeforeProcessing());
        writeComponent.setText(Bundle.CTL_Label_WriteBefore_Text());

        JPanel preProcessingPanel = new JPanel(new SpringLayout());
        preProcessingPanel.add(checkBoxComponent);
        preProcessingPanel.add(firstEditorComponent);


        preProcessingPanel.add(writeComponent);
        preProcessingPanel.add(secondEditorComponent);

        SpringUtilities.makeCompactGrid(preProcessingPanel, 2, 2, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        forwardFocusWhenTabKeyReleased(checkBoxComponent, firstEditorComponent);
        forwardFocusWhenTabKeyReleased(firstEditorComponent, writeComponent);
        forwardFocusWhenTabKeyReleased(writeComponent, secondEditorComponent);
        forwardFocusWhenTabKeyReleased(secondEditorComponent, this.tabbedPane);

        return preProcessingPanel;
    }

    @Override
    protected JPanel createToolInfoPanel() {
        final JPanel configPanel = new JPanel(new SpringLayout());
        JPanel panelToolFiles = new JPanel(new SpringLayout());
        PropertyEditorRegistry editorRegistry = PropertyEditorRegistry.getInstance();

        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor(ToolAdapterConstants.MAIN_TOOL_FILE_LOCATION);
        propertyDescriptor.setValidator(new NotEmptyValidator());
        PropertyEditor editor = editorRegistry.findPropertyEditor(propertyDescriptor);
        JComponent editorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);
        editorComponent.setMaximumSize(new Dimension(editorComponent.getMaximumSize().width, controlHeight));
        editorComponent.setPreferredSize(new Dimension(editorComponent.getPreferredSize().width, controlHeight));
        org.esa.snap.utils.UIUtils.enableUndoRedo(editorComponent);
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
        editor = editorRegistry.findPropertyEditor(propertyDescriptor);
        editorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);
        editorComponent.setMaximumSize(new Dimension(editorComponent.getMaximumSize().width, controlHeight));
        editorComponent.setPreferredSize(new Dimension(editorComponent.getPreferredSize().width, controlHeight));
        org.esa.snap.utils.UIUtils.enableUndoRedo(editorComponent);
        panelToolFiles.add(new JLabel(Bundle.CTL_Label_WorkDir_Text()));
        panelToolFiles.add(editorComponent);

        SpringUtilities.makeCompactGrid(panelToolFiles, 2, 2, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        configPanel.add(panelToolFiles);

        JPanel checkPanel = new JPanel(new SpringLayout());

        propertyDescriptor = propertyContainer.getDescriptor(ToolAdapterConstants.HANDLE_OUTPUT);
        editor = editorRegistry.findPropertyEditor(propertyDescriptor);
        editorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);
        editorComponent.setMaximumSize(new Dimension(editorComponent.getMaximumSize().width, controlHeight));
        editorComponent.setPreferredSize(new Dimension(editorComponent.getPreferredSize().width, controlHeight));

        checkPanel.add(editorComponent);
        checkPanel.add(new JLabel("Tool produces the name of the output product"));

        SpringUtilities.makeCompactGrid(checkPanel, 1, 2, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        configPanel.add(checkPanel);

        JLabel label = new JLabel(Bundle.CTL_Label_CmdLineTemplate_Text());
        configPanel.add(label);

        JScrollPane scrollPane = new JScrollPane(createTemplateEditorField());
        configPanel.add(scrollPane);

        configPanel.add(createPatternsPanel());

        SpringUtilities.makeCompactGrid(configPanel, 5, 1, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        return configPanel;
    }

    @Override
    protected JPanel createPatternsPanel() {
        JPanel patternsPanel = new JPanel(new SpringLayout());
        TitledBorder titledBorder = BorderFactory.createTitledBorder(Bundle.CTL_Panel_OutputPattern_Border_TitleText());
        titledBorder.setTitleJustification(TitledBorder.CENTER);
        patternsPanel.setBorder(titledBorder);

        TextFieldEditor textEditor = new TextFieldEditor();
        addTextField(patternsPanel, textEditor, Bundle.CTL_Label_ProgressPattern(), ToolAdapterConstants.PROGRESS_PATTERN, false);
        propertyContainer.getDescriptor(ToolAdapterConstants.PROGRESS_PATTERN).setValidator(new RegexFieldValidator());
        addTextField(patternsPanel, textEditor, Bundle.CTL_Label_StepPattern(), ToolAdapterConstants.STEP_PATTERN, false);
        propertyContainer.getDescriptor(ToolAdapterConstants.STEP_PATTERN).setValidator(new RegexFieldValidator());
        addTextField(patternsPanel, textEditor, Bundle.CTL_Label_ErrorPattern(), ToolAdapterConstants.ERROR_PATTERN, false);
        propertyContainer.getDescriptor(ToolAdapterConstants.ERROR_PATTERN).setValidator(new RegexFieldValidator());

        SpringUtilities.makeCompactGrid(patternsPanel, 3, 2, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        return patternsPanel;
    }

    @Override
    protected JPanel createParametersPanel() {
        JPanel paramsPanel = new JPanel();
        BoxLayout layout = new BoxLayout(paramsPanel, BoxLayout.PAGE_AXIS);
        paramsPanel.setLayout(layout);
        AbstractButton addParamBut = ToolButtonFactory.createButton(UIUtils.loadImageIcon(Bundle.Icon_Add()), false);
        addParamBut.setText("New Parameter");
        addParamBut.setMaximumSize(new Dimension(150, controlHeight));
        addParamBut.setAlignmentX(Component.LEFT_ALIGNMENT);
        addParamBut.setAlignmentY(Component.TOP_ALIGNMENT);
        paramsPanel.add(addParamBut);
        JScrollPane tableScrollPane = new JScrollPane(paramsTable);
        tableScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        paramsPanel.add(tableScrollPane);
        addParamBut.addActionListener(e -> paramsTable.addParameterToTable());
        return paramsPanel;
    }

    @Override
    protected JPanel createBundlePanel() {
        org.esa.snap.core.gpf.descriptor.dependency.Bundle bundle = this.newOperatorDescriptor.getBundle();
        if (bundle == null) {
            bundle = new org.esa.snap.core.gpf.descriptor.dependency.Bundle(BundleType.NONE, null, null);
            bundle.setTargetLocation(SystemUtils.getAuxDataPath().toFile());
            this.newOperatorDescriptor.setBundle(bundle);
        }
        Map<String, FieldChangeTrigger[]> dependencyMap = new HashMap<String, FieldChangeTrigger[]>() {{
            put("source", new FieldChangeTrigger[]
                    {
                            FieldChangeTrigger.<File, String>create("entryPoint",
                                file -> {
                                    if (file != null) {
                                        try {
                                            Path path = SystemUtils.getApplicationDataDir().toPath()
                                                    .resolve("modules")
                                                    .resolve("lib");
                                            Files.createDirectories(path);
                                            Files.list(path).forEach(p -> {
                                                try {
                                                    Files.delete(p);
                                                } catch (IOException ignored) {
                                                }
                                            });
                                            path = path.resolve(file.getName());
                                            Files.copy(file.toPath(), path);
                                        } catch (Exception e) {
                                            logger.severe(e.getMessage());
                                        }
                                        return file.getName();
                                    }
                                    return null;
                                }),
                            FieldChangeTrigger.<File, File>create("targetLocation",
                                file -> {
                                    if (file != null) {
                                        return SystemUtils.getAuxDataPath().resolve(FileUtils.getFilenameWithoutExtension(file)).toFile();
                                    }
                                    return null;
                                })
                    }
            );
            put("bundleType", new FieldChangeTrigger[]
                    {
                            FieldChangeTrigger.<BundleType, File>create("source",
                                    bundleType -> null,
                                    BundleType.NONE::equals),
                            FieldChangeTrigger.<BundleType, File>create("targetLocation",
                                    bundleType -> null,
                                    BundleType.NONE::equals),
                            FieldChangeTrigger.<BundleType, String>create("entryPoint",
                                    bundleType -> null,
                                    BundleType.NONE::equals),
                            FieldChangeTrigger.<BundleType, String>create("arguments",
                                    bundleType -> null,
                                    BundleType.NONE::equals)
                    });
        }};
        Map<String, Function<org.esa.snap.core.gpf.descriptor.dependency.Bundle, Void>> actionMap =
                new HashMap<String, Function<org.esa.snap.core.gpf.descriptor.dependency.Bundle, Void>>() {{
            put("Install Now", changedBundle -> {
                if (changedBundle != null && !changedBundle.isInstalled()) {
                    newOperatorDescriptor.setBundle(changedBundle);
                    try {
                        ToolAdapterTabbedEditorDialog.this.getContent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        ToolAdapterActivator.installBundle(newOperatorDescriptor, false);
                        if (changedBundle.isInstalled()) {
                            Dialogs.showInformation("Installation completed");
                        } else {
                            Dialogs.showWarning("Installation failed");
                        }
                    } finally {
                        ToolAdapterTabbedEditorDialog.this.getContent().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
                return null;
            });
        }};
        bundleForm = new EntityForm<>(bundle, dependencyMap, actionMap);
        return bundleForm.getPanel();
    }

    private JPanel createPreProcessingTab() {
        JPanel preprocessAndPatternsPanel = new JPanel(new SpringLayout());
        preprocessAndPatternsPanel.add(createPreProcessingPanel());
        SpringUtilities.makeCompactGrid(preprocessAndPatternsPanel, 1, 1, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);
        preprocessAndPatternsPanel.setMaximumSize(preprocessAndPatternsPanel.getSize());
        return preprocessAndPatternsPanel;
    }

    private JPanel createDescriptorTab() {
        JPanel jPanel = new JPanel(new SpringLayout());
        jPanel.add(createDescriptorPanel());
        SpringUtilities.makeCompactGrid(jPanel, 1, 1, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);
        jPanel.setMaximumSize(jPanel.getSize());
        return jPanel;
    }

    private JPanel createParametersTab(int width) {
        JPanel paramsPanel = createParametersPanel();
        int tableWidth = width - 2 * DEFAULT_PADDING;
        int widths[] = {controlHeight, 5 * controlHeight, 5 * controlHeight, 3 * controlHeight, 3 * controlHeight, (int)(tableWidth * 0.3), 30};
        for(int i=0; i < widths.length; i++) {
            paramsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        }
        paramsTable.setRowHeight(controlHeight);
        return paramsPanel;
    }

    private void addTab(JTabbedPane tabControl, String title, JPanel content) {
        JLabel tabText = new JLabel(title, JLabel.LEFT);
        tabText.setPreferredSize(new Dimension(6 * controlHeight, controlHeight));
        TitledBorder titledBorder = BorderFactory.createTitledBorder(title);
        titledBorder.setTitleJustification(TitledBorder.CENTER);
        content.setBorder(titledBorder);
        tabControl.addTab(null, content);
        tabControl.setTabComponentAt(tabControl.getTabCount() - 1, tabText);
    }

    private static void forwardFocusWhenTabKeyReleased(JComponent aSenderComponent, JComponent aReceiverComponent) {
        ForwardFocusAction forwardFocusAction = new ForwardFocusAction("TabKeyAction", aReceiverComponent);
        aSenderComponent.setFocusTraversalKeys(0, new HashSet());
        int modifiers = 0; // '0' => no modifiers
        int keyCode = KeyEvent.VK_TAB;
        boolean onKeyRelease = true;
        KeyStroke tabKeyReleased = KeyStroke.getKeyStroke(keyCode, modifiers, onKeyRelease);
        aSenderComponent.getInputMap(1).put(tabKeyReleased, forwardFocusAction.getName());
        aSenderComponent.getActionMap().put(forwardFocusAction.getName(), forwardFocusAction);
    }
}
