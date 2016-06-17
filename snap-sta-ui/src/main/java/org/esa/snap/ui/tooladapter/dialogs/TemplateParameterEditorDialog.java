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

import org.esa.snap.core.gpf.descriptor.*;
import org.esa.snap.core.gpf.descriptor.template.TemplateException;
import org.esa.snap.core.gpf.descriptor.template.TemplateFile;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterOp;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.esa.snap.ui.tooladapter.actions.EscapeAction;
import org.esa.snap.ui.tooladapter.model.AutoCompleteTextArea;
import org.esa.snap.ui.tooladapter.model.OperatorParametersTable;
import org.esa.snap.ui.tooladapter.model.PropertyMemberUIWrapper;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Form for displaying and editing details of a tool adapter parameter of File Template type.
 *
 * @author Ramona Manda
 */
public class TemplateParameterEditorDialog extends ModalDialog {

    private TemplateParameterDescriptor parameter;
    private ToolAdapterOperatorDescriptor fakeOperatorDescriptor;
    private ToolAdapterOperatorDescriptor parentDescriptor;
    private PropertyMemberUIWrapper fileWrapper;
    private AppContext appContext;
    private AutoCompleteTextArea fileContentArea = new AutoCompleteTextArea("", 10, 10);
    OperatorParametersTable paramsTable;
    private Logger logger;
    private PropertyChangeListener pcListener;

    public TemplateParameterEditorDialog(AppContext appContext, String title, String helpID) {
        super(appContext.getApplicationWindow(), title, ID_OK_CANCEL, helpID);
        this.appContext = appContext;
        this.logger = Logger.getLogger(TemplateParameterEditorDialog.class.getName());
        EscapeAction.register(getJDialog());
    }

    public TemplateParameterEditorDialog(AppContext appContext, String helpID, TemplateParameterDescriptor parameter, PropertyMemberUIWrapper fileWrapper, ToolAdapterOperatorDescriptor parent) {
        this(appContext, parameter.getName(), helpID);
        this.parameter = parameter;
        this.parentDescriptor = parent;
        this.fakeOperatorDescriptor = new ToolAdapterOperatorDescriptor("OperatorForParameters", ToolAdapterOp.class);
        for(ToolParameterDescriptor param : parameter.getParameterDescriptors()) {
            this.fakeOperatorDescriptor.getToolParameterDescriptors().add(new ToolParameterDescriptor(param));
        }
        this.fileWrapper = fileWrapper;
        setContent(createMainPanel());
        pcListener = evt -> updateFileAreaContent();
    }

    public JPanel createParametersPanel() {
        JPanel paramsPanel = new JPanel();
        BoxLayout layout = new BoxLayout(paramsPanel, BoxLayout.PAGE_AXIS);
        paramsPanel.setLayout(layout);
        AbstractButton addParamBut = ToolButtonFactory.createButton(UIUtils.loadImageIcon("/org/esa/snap/resources/images/icons/Add16.png"),
                false);
        addParamBut.setAlignmentX(Component.LEFT_ALIGNMENT);
        paramsPanel.add(addParamBut);

        paramsTable =  new OperatorParametersTable(this.fakeOperatorDescriptor, appContext);
        JScrollPane tableScrollPane = new JScrollPane(paramsTable);
        tableScrollPane.setPreferredSize(new Dimension(500, 130));
        tableScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        paramsPanel.add(tableScrollPane);
        addParamBut.addActionListener((ActionEvent e) -> paramsTable.addParameterToTable(new ToolParameterDescriptor("parameterName", String.class)));
        TitledBorder title = BorderFactory.createTitledBorder("Template Parameters");
        paramsPanel.setBorder(title);
        return paramsPanel;
    }

    private JPanel createMainPanel(){

        BorderLayout layout = new BorderLayout();
        JPanel mainPanel = new JPanel(layout);
        mainPanel.setPreferredSize(new Dimension(800, 550));

        JPanel filePanel = new JPanel();
        filePanel.add(new JLabel("File:"));
        try {
            JComponent fileEditor = this.fileWrapper.getUIComponent();
            fileEditor.setPreferredSize(new Dimension(770, 25));
            filePanel.add(fileEditor);
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
        this.fileWrapper.getContext().addPropertyChangeListener(pcListener);

        mainPanel.add(filePanel, BorderLayout.PAGE_START);
        fileContentArea.setAutoCompleteEntries(getAutocompleteEntries());
        fileContentArea.setTriggerChar('$');
        mainPanel.add(new JScrollPane(fileContentArea), BorderLayout.CENTER);
        updateFileAreaContent();
        mainPanel.add(createParametersPanel(), BorderLayout.PAGE_END);

        return mainPanel;
    }

    private void updateFileAreaContent(){
        String result = null;
        try {
            /*File defaultValue = ToolAdapterIO.ensureLocalCopy(fileWrapper.getContext().getPropertySet().getProperty(this.parameter.getName()).getValue(),
                                                              parentDescriptor.getAlias());*/
            File templatePath = parameter.getTemplate().getTemplatePath();
            File actualValue = fileWrapper.getContext().getPropertySet().getProperty(parameter.getName()).getValue();
            if (actualValue.getName().equals(templatePath.getName()) && !actualValue.isAbsolute()) {
                actualValue = templatePath;
                fileWrapper.getContext().removePropertyChangeListener(pcListener);
                fileWrapper.getContext().getPropertySet().getProperty(parameter.getName()).setValue(actualValue);
                fileWrapper.getContext().addPropertyChangeListener(pcListener);
                result = parameter.getTemplate().getContents();
            } else {
                if (!actualValue.exists()) {
                    if (templatePath.exists()) {
                        Files.copy(templatePath.toPath(), actualValue.toPath());
                    } else {
                        actualValue.createNewFile();
                    }
                }
                if (actualValue.length() > 0) {
                    parameter.setTemplate(TemplateFile.fromFile(actualValue.toString()));
                    result = parameter.getTemplate().getContents();
                } else {
                    parameter.getTemplate().setFileName(actualValue.toString());
                    result = fileContentArea.getText();
                }
            }
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
        if (result != null){
            fileContentArea.setText(result);
            fileContentArea.setCaretPosition(0);
        } else {
            fileContentArea.setText("[no content]");
        }
    }

    @Override
    protected void onOK() {
        super.onOK();
        TemplateFile template = this.parameter.getTemplate();
        this.parameter.setDefaultValue(template.getFileName());
        //save parameters
        parameter.getParameterDescriptors().clear();
        for (ToolParameterDescriptor subparameter : fakeOperatorDescriptor.getToolParameterDescriptors()) {
            if (paramsTable.getBindingContext().getBinding(subparameter.getName()) != null) {
                Object propertyValue = paramsTable.getBindingContext().getBinding(subparameter.getName()).getPropertyValue();
                if (propertyValue != null) {
                    subparameter.setDefaultValue(propertyValue.toString());
                }
            }
            parameter.addParameterDescriptor(subparameter);
        }
        try {
            template.setContents(fileContentArea.getText(), true);
            template.save();
        } catch (IOException | TemplateException e) {
            logger.warning(e.getMessage());
        }
    }

    private java.util.List<String> getAutocompleteEntries() {
        java.util.List<String> entries = new ArrayList<>();
        entries.addAll(parentDescriptor.getVariables().stream().map(SystemVariable::getKey).collect(Collectors.toList()));
        for (ParameterDescriptor parameterDescriptor : fakeOperatorDescriptor.getParameterDescriptors()) {
            entries.add(parameterDescriptor.getName());
        }
        entries.sort(Comparator.<String>naturalOrder());
        return entries;
    }
}
