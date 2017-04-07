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
import org.esa.snap.core.gpf.descriptor.dependency.BundleInstaller;
import org.esa.snap.core.gpf.descriptor.dependency.BundleLocation;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterConstants;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.esa.snap.ui.tooladapter.dialogs.components.AnchorLabel;
import org.esa.snap.ui.tooladapter.dialogs.progress.ProgressHandler;
import org.esa.snap.ui.tooladapter.model.OperationType;
import org.esa.snap.ui.tooladapter.validators.RegexFieldValidator;
import org.esa.snap.utils.SpringUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

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
    private static final String ALIAS_PATTERN = "[^\\\\\\?%\\*:\\|\"<>\\./]*";
    private JTabbedPane tabbedPane;
    private int currentIndex;

    ToolAdapterTabbedEditorDialog(AppContext appContext, JDialog parent, ToolAdapterOperatorDescriptor operatorDescriptor, OperationType operation) {
        super(appContext, parent, operatorDescriptor, operation);
    }

    ToolAdapterTabbedEditorDialog(AppContext appContext, JDialog parent, ToolAdapterOperatorDescriptor operatorDescriptor, int newNameIndex, OperationType operation) {
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
        currentIndex++;
        addTab(tabbedPane, Bundle.CTL_Panel_ConfigParams_Text(), createToolInfoPanel());
        currentIndex++;
        addTab(tabbedPane, Bundle.CTL_Panel_PreProcessing_Border_TitleText(), createPreProcessingTab());
        currentIndex++;
        addTab(tabbedPane, Bundle.CTL_Panel_OpParams_Border_TitleText(), createParametersTab(formWidth));
        currentIndex++;
        addTab(tabbedPane, Bundle.CTL_Panel_SysVar_Border_TitleText(), createVariablesPanel());
        currentIndex++;
        addTab(tabbedPane, Bundle.CTL_Panel_Bundle_TitleText(), createBundlePanel());
        currentIndex++;

        tabbedPane.setUI(new BasicTabbedPaneUI());

        formWidth = tabbedPane.getTabComponentAt(0).getWidth();

        return tabbedPane;
    }

    @Override
    protected JPanel createDescriptorPanel() {
        JPanel descriptorPanel = new JPanel(new SpringLayout());
        TextFieldEditor textEditor = new TextFieldEditor();

        addValidatedTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Alias_Text(),
                              ToolAdapterConstants.ALIAS, ALIAS_PATTERN);
        JComponent component = addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_UniqueName_Text(),
                                            ToolAdapterConstants.NAME, true);
        anchorLabels.put(ToolAdapterConstants.NAME, new AnchorLabel("Name is not unique",
                                                                    tabbedPane, currentIndex, component));
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Label_Text(),
                     ToolAdapterConstants.LABEL, true);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Version_Text(),
                     ToolAdapterConstants.VERSION, true);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Copyright_Text(),
                     ToolAdapterConstants.COPYRIGHT, false);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Authors_Text(),
                     ToolAdapterConstants.AUTHORS, false);
        addTextField(descriptorPanel, textEditor, Bundle.CTL_Label_Description_Text(),
                     ToolAdapterConstants.DESCRIPTION, false);

        propertyContainer
                .addPropertyChangeListener(ToolAdapterConstants.ALIAS,
                                           evt -> propertyContainer.setValue(ToolAdapterConstants.NAME,
                                                                             ToolAdapterConstants.OPERATOR_NAMESPACE +
                                                                                     evt.getNewValue().toString()));

        List<String> menus = getAvailableMenuOptions(null);
        addComboField(descriptorPanel, Bundle.CTL_Label_MenuLocation_Text(), ToolAdapterConstants.MENU_LOCATION, menus);

        addComboField(descriptorPanel, Bundle.CTL_Label_TemplateType_Text(), ToolAdapterConstants.TEMPLATE_TYPE, true, false);

        SpringUtilities.makeCompactGrid(descriptorPanel, 9, 2, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);
        return descriptorPanel;
    }

    @Override
    protected JPanel createVariablesPanel() {
        JPanel variablesBorderPanel = new JPanel();
        BoxLayout layout = new BoxLayout(variablesBorderPanel, BoxLayout.PAGE_AXIS);
        variablesBorderPanel.setLayout(layout);

        AbstractButton addVariableButton =
                ToolButtonFactory.createButton(UIUtils.loadImageIcon(Bundle.Icon_Add()), false);
        addVariableButton.setText(Bundle.CTL_Button_Add_Variable_Text());
        addVariableButton.setMaximumSize(new Dimension(150, controlHeight));
        addVariableButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        AbstractButton addDependentVariableButton =
                ToolButtonFactory.createButton(UIUtils.loadImageIcon(Bundle.Icon_Add()), false);
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
        int widths[] = {controlHeight, 3 * controlHeight, 10 * controlHeight};
        for (int i = 0; i < widths.length; i++) {
            TableColumn column = varTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(widths[i]);
            column.setWidth(widths[i]);

        }
        JScrollPane scrollPane = new JScrollPane(varTable);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        variablesBorderPanel.add(scrollPane);
        variablesBorderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension variablesPanelDimension =
                new Dimension((formWidth - 3 * DEFAULT_PADDING) / 2 - 2 * DEFAULT_PADDING, 130);
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
        firstEditorComponent.setMaximumSize(new Dimension(firstEditorComponent.getMaximumSize().width,
                                                          controlHeight));
        firstEditorComponent.setPreferredSize(new Dimension(firstEditorComponent.getPreferredSize().width,
                                                            controlHeight));

        propertyDescriptor = propertyContainer.getDescriptor("processingWriter");
        editor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        JComponent secondEditorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);
        secondEditorComponent.setMaximumSize(new Dimension(secondEditorComponent.getMaximumSize().width,
                                                           controlHeight));
        secondEditorComponent.setPreferredSize(new Dimension(secondEditorComponent.getPreferredSize().width,
                                                             controlHeight));

        JCheckBox checkBoxComponent =
                (JCheckBox)createCheckboxComponent("preprocessTool", firstEditorComponent,
                                                   newOperatorDescriptor.getPreprocessTool());
        checkBoxComponent.setText(Bundle.CTL_Label_PreprocessingTool_Text());

        JCheckBox writeComponent =
                (JCheckBox)createCheckboxComponent("writeForProcessing", secondEditorComponent,
                                                   newOperatorDescriptor.shouldWriteBeforeProcessing());
        writeComponent.setText(Bundle.CTL_Label_WriteBefore_Text());

        JPanel preProcessingPanel = new JPanel(new SpringLayout());
        preProcessingPanel.add(checkBoxComponent);
        preProcessingPanel.add(firstEditorComponent);


        preProcessingPanel.add(writeComponent);
        preProcessingPanel.add(secondEditorComponent);

        SpringUtilities.makeCompactGrid(preProcessingPanel, 2, 2,
                                        DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

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

        PropertyDescriptor propertyDescriptor =
                propertyContainer.getDescriptor(ToolAdapterConstants.MAIN_TOOL_FILE_LOCATION);
        propertyDescriptor.setValidator(new NotEmptyValidator());
        PropertyEditor editor = editorRegistry.findPropertyEditor(propertyDescriptor);
        JComponent editorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);
        editorComponent.setMaximumSize(new Dimension(editorComponent.getMaximumSize().width, controlHeight));
        editorComponent.setPreferredSize(new Dimension(editorComponent.getPreferredSize().width, controlHeight));
        org.esa.snap.utils.UIUtils.enableUndoRedo(editorComponent);
        JLabel jLabel = new JLabel(Bundle.CTL_Label_ToolLocation_Text());
        panelToolFiles.add(jLabel);
        jLabel.setLabelFor(editorComponent);
        panelToolFiles.add(editorComponent);

        anchorLabels.put(ToolAdapterConstants.MAIN_TOOL_FILE_LOCATION,
                         new AnchorLabel(Bundle.MSG_Inexistent_Tool_Path_Text(),
                                         this.tabbedPane, this.currentIndex, editorComponent));

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
        jLabel = new JLabel(Bundle.CTL_Label_WorkDir_Text());
        panelToolFiles.add(jLabel);
        jLabel.setLabelFor(editorComponent);

        anchorLabels.put(ToolAdapterConstants.WORKING_DIR,
                         new AnchorLabel(Bundle.MSG_Inexistent_WorkDir_Text(),
                                         this.tabbedPane, this.currentIndex, editorComponent));

        panelToolFiles.add(editorComponent);

        SpringUtilities.makeCompactGrid(panelToolFiles, 2, 2,
                                        DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        configPanel.add(panelToolFiles);

        JPanel checkPanel = new JPanel(new SpringLayout());

        propertyDescriptor = propertyContainer.getDescriptor(ToolAdapterConstants.HANDLE_OUTPUT);
        editor = editorRegistry.findPropertyEditor(propertyDescriptor);
        editorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);
        editorComponent.setMaximumSize(new Dimension(editorComponent.getMaximumSize().width, controlHeight));
        editorComponent.setPreferredSize(new Dimension(editorComponent.getPreferredSize().width, controlHeight));

        checkPanel.add(editorComponent);
        checkPanel.add(new JLabel("Tool produces the name of the output product"));

        SpringUtilities.makeCompactGrid(checkPanel, 1, 2,
                                        DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        configPanel.add(checkPanel);

        JLabel label = new JLabel(Bundle.CTL_Label_CmdLineTemplate_Text());
        configPanel.add(label);

        JScrollPane scrollPane = new JScrollPane(createTemplateEditorField());
        configPanel.add(scrollPane);

        configPanel.add(createPatternsPanel());

        SpringUtilities.makeCompactGrid(configPanel, 5, 1,
                                        DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

        return configPanel;
    }

    @Override
    protected JPanel createPatternsPanel() {
        JPanel patternsPanel = new JPanel(new SpringLayout());
        TitledBorder titledBorder = BorderFactory.createTitledBorder(Bundle.CTL_Panel_OutputPattern_Border_TitleText());
        titledBorder.setTitleJustification(TitledBorder.CENTER);
        patternsPanel.setBorder(titledBorder);

        TextFieldEditor textEditor = new TextFieldEditor();
        addTextField(patternsPanel, textEditor, Bundle.CTL_Label_ProgressPattern(),
                     ToolAdapterConstants.PROGRESS_PATTERN, false);
        propertyContainer.getDescriptor(ToolAdapterConstants.PROGRESS_PATTERN)
                .setValidator(new RegexFieldValidator());
        addTextField(patternsPanel, textEditor, Bundle.CTL_Label_StepPattern(),
                     ToolAdapterConstants.STEP_PATTERN, false);
        propertyContainer.getDescriptor(ToolAdapterConstants.STEP_PATTERN)
                .setValidator(new RegexFieldValidator());
        addTextField(patternsPanel, textEditor, Bundle.CTL_Label_ErrorPattern(),
                     ToolAdapterConstants.ERROR_PATTERN, false);
        propertyContainer.getDescriptor(ToolAdapterConstants.ERROR_PATTERN)
                .setValidator(new RegexFieldValidator());

        SpringUtilities.makeCompactGrid(patternsPanel, 3, 2,
                                        DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

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
        JPanel bundlePanel = new JPanel(new SpringLayout());
        int rows = 0;
        bundleForm = new BundleForm(this.context,
                                    newOperatorDescriptor.getWindowsBundle(),
                                    newOperatorDescriptor.getLinuxBundle(),
                                    newOperatorDescriptor.getMacosxBundle(),
                                    newOperatorDescriptor.getVariables());
        bundlePanel.add(bundleForm);
        rows++;
        org.esa.snap.core.gpf.descriptor.dependency.Bundle currentBundle = newOperatorDescriptor.getBundle();
        if (currentBundle != null &&
                !currentBundle.isInstalled() && BundleInstaller.isBundleFileAvailable(currentBundle)) {
            JButton installButton = new JButton() {
                @Override
                public void setText(String text) {
                    super.setText(text);
                    adjustDimension(this);
                }
            };
            installButton.setText((currentBundle.getLocation() == BundleLocation.REMOTE ?
                    "Download and " :
                    "") + "Install Now");
            installButton.setToolTipText(currentBundle.getLocation() == BundleLocation.REMOTE ?
                                                 currentBundle.getDownloadURL() : currentBundle.getSource().toString());
            installButton.setMaximumSize(installButton.getPreferredSize());
            JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.setMaximumSize(new Dimension(buttonPanel.getWidth(), controlHeight));
            buttonPanel.add(installButton, BorderLayout.EAST);
            bundlePanel.add(buttonPanel);
            installButton.addActionListener((ActionEvent e) -> {
                newOperatorDescriptor.setBundles(bundleForm.applyChanges());
                org.esa.snap.core.gpf.descriptor.dependency.Bundle modifiedBundle = newOperatorDescriptor.getBundle();
                try (BundleInstaller installer = new BundleInstaller(newOperatorDescriptor)) {
                    ProgressHandle progressHandle = ProgressHandleFactory.createSystemHandle("Installing bundle");
                    installer.setProgressMonitor(new ProgressHandler(progressHandle, false));
                    installer.setCallback(() -> {
                        if (modifiedBundle.isInstalled()) {
                            Path path = newOperatorDescriptor.resolveVariables(modifiedBundle.getTargetLocation())
                                    .toPath()
                                    .resolve(FileUtils.getFilenameWithoutExtension(modifiedBundle.getEntryPoint()));
                            SwingUtilities.invokeLater(() -> {
                                progressHandle.finish();
                                Dialogs.showInformation(String.format("Bundle was installed in location:\n%s", path));
                                installButton.setVisible(false);
                                bundlePanel.revalidate();
                            });
                            String updateVariable = modifiedBundle.getUpdateVariable();
                            if (updateVariable != null) {
                                Optional<SystemVariable> variable = newOperatorDescriptor.getVariables()
                                        .stream()
                                        .filter(v -> v.getKey().equals(updateVariable))
                                        .findFirst();
                                variable.ifPresent(systemVariable -> {
                                    systemVariable.setShared(true);
                                    systemVariable.setValue(path.toString());
                                });
                                varTable.revalidate();
                            }
                        } else {
                            SwingUtilities.invokeLater(() -> {
                                progressHandle.finish();
                                Dialogs.showInformation("Bundle installation failed. \n" +
                                                                "Please see the application log for details.");
                                bundlePanel.revalidate();
                            });
                        }
                        return null;
                    });
                    installer.install(true);
                } catch (Exception ex) {
                    logger.warning(ex.getMessage());
                }
            });
            this.downloadAction = () -> {
                tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
                installButton.requestFocusInWindow();
                installButton.doClick();
                return null;
            };
            rows++;
        }
        SpringUtilities.makeCompactGrid(bundlePanel, rows, 1,
                                        DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);
        return bundlePanel;
    }

    private JPanel createPreProcessingTab() {
        JPanel preprocessAndPatternsPanel = new JPanel(new SpringLayout());
        preprocessAndPatternsPanel.add(createPreProcessingPanel());
        SpringUtilities.makeCompactGrid(preprocessAndPatternsPanel, 1, 1,
                                        DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);
        preprocessAndPatternsPanel.setMaximumSize(preprocessAndPatternsPanel.getSize());
        return preprocessAndPatternsPanel;
    }

    private JPanel createDescriptorTab() {
        JPanel jPanel = new JPanel(new SpringLayout());
        jPanel.add(createDescriptorPanel());
        SpringUtilities.makeCompactGrid(jPanel, 1, 1,
                                        DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);
        jPanel.setMaximumSize(jPanel.getSize());
        return jPanel;
    }

    private JPanel createParametersTab(int width) {
        JPanel paramsPanel = createParametersPanel();
        int tableWidth = width - 2 * DEFAULT_PADDING;
        int widths[] = {controlHeight, 5 * controlHeight, 5 * controlHeight,
                3 * controlHeight, 3 * controlHeight, (int)(tableWidth * 0.3), 30};
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
        KeyStroke tabKeyReleased = KeyStroke.getKeyStroke(keyCode, modifiers, true);
        aSenderComponent.getInputMap(1).put(tabKeyReleased, forwardFocusAction.getName());
        aSenderComponent.getActionMap().put(forwardFocusAction.getName(), forwardFocusAction);
    }
}
