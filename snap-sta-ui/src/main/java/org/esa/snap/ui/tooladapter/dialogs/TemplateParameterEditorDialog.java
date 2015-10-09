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
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterIO;
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
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private ToolAdapterOperatorDescriptor fakeDescriptor;
    private ToolAdapterOperatorDescriptor parentDescriptor;
    private PropertyMemberUIWrapper fileWrapper;
    private AppContext appContext;
    private AutoCompleteTextArea fileContentArea = new AutoCompleteTextArea("", 10, 10);
    OperatorParametersTable paramsTable;
    private Logger logger;

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
        this.fakeDescriptor = new ToolAdapterOperatorDescriptor("OperatorForParameters", ToolAdapterOp.class);
        for(ToolParameterDescriptor param : parameter.getToolParameterDescriptors()) {
            this.fakeDescriptor.getToolParameterDescriptors().add(new TemplateParameterDescriptor(param));
        }
        this.fileWrapper = fileWrapper;
        setContent(createMainPanel());
    }

    public JPanel createParametersPanel() {
        JPanel paramsPanel = new JPanel();
        BoxLayout layout = new BoxLayout(paramsPanel, BoxLayout.PAGE_AXIS);
        paramsPanel.setLayout(layout);
        AbstractButton addParamBut = ToolButtonFactory.createButton(UIUtils.loadImageIcon("/org/esa/snap/resources/images/icons/Add16.png"),
                false);
        addParamBut.setAlignmentX(Component.LEFT_ALIGNMENT);
        paramsPanel.add(addParamBut);

        paramsTable =  new OperatorParametersTable(this.fakeDescriptor, appContext);
        JScrollPane tableScrollPane = new JScrollPane(paramsTable);
        tableScrollPane.setPreferredSize(new Dimension(500, 130));
        tableScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        paramsPanel.add(tableScrollPane);
        addParamBut.addActionListener((ActionEvent e) -> paramsTable.addParameterToTable(new TemplateParameterDescriptor("parameterName", String.class)));
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
        this.fileWrapper.getContext().addPropertyChangeListener(evt -> updateFileAreaContent());

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
            File defaultValue = ToolAdapterIO.ensureLocalCopy(fileWrapper.getContext().getPropertySet().getProperty(this.parameter.getName()).getValue(),
                                                              parentDescriptor.getAlias());
            fileWrapper.getContext().getPropertySet().getProperty(this.parameter.getName()).setValue(defaultValue);
            if(defaultValue.exists()) {
                byte[] encoded = Files.readAllBytes(Paths.get((defaultValue).getAbsolutePath()));
                result = new String(encoded, Charset.defaultCharset());
            } else {
                //if the file does not exist, it keeps the old content, in case the user wants to save in the new file
                result = fileContentArea.getText();
            }
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
        if(result != null){
            fileContentArea.setText(result);
            fileContentArea.setCaretPosition(0);
        } else {
            fileContentArea.setText("[no content]");
        }
    }

    @Override
    protected void onOK() {
        super.onOK();
        //set value
        File defaultValue = ToolAdapterIO.ensureLocalCopy(fileWrapper.getContext().getPropertySet().getProperty(this.parameter.getName()).getValue(),
                parentDescriptor.getAlias());
        this.parameter.setDefaultValue(defaultValue.getName());
        //save parameters
        parameter.getToolParameterDescriptors().clear();
        for (TemplateParameterDescriptor subparameter : fakeDescriptor.getToolParameterDescriptors()){
            if (paramsTable.getBindingContext().getBinding(subparameter.getName()) != null){
                Object propertyValue = paramsTable.getBindingContext().getBinding(subparameter.getName()).getPropertyValue();
                if (propertyValue != null) {
                    subparameter.setDefaultValue(propertyValue.toString());
                }
            }
            parameter.addParameterDescriptor(subparameter);
        }
        //save file content
        try {
            ToolAdapterIO.saveFileContent(defaultValue, fileContentArea.getText());
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
    }

    private java.util.List<String> getAutocompleteEntries() {
        java.util.List<String> entries = new ArrayList<>();
        entries.addAll(parentDescriptor.getVariables().stream().map(SystemVariable::getKey).collect(Collectors.toList()));
        for (ParameterDescriptor parameterDescriptor : fakeDescriptor.getParameterDescriptors()) {
            entries.add(parameterDescriptor.getName());
        }
        entries.sort(Comparator.<String>naturalOrder());
        return entries;
    }
}
