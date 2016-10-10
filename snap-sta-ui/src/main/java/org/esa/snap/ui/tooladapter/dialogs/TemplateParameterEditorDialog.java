package org.esa.snap.ui.tooladapter.dialogs;

import com.bc.ceres.binding.*;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.internal.FileEditor;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
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
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Form for displaying and editing details of a tool adapter parameter of File Template type.
 *
 * @author Ramona Manda
 */
public class TemplateParameterEditorDialog extends ModalDialog {
    private static final Logger logger = Logger.getLogger(TemplateParameterEditorDialog.class.getName());

    private static String EMPTY_FILE_CONTENT = "[no content]";

    private TemplateParameterDescriptor parameter;
    private ToolAdapterOperatorDescriptor fakeOperatorDescriptor;
    private ToolAdapterOperatorDescriptor parentDescriptor;
    private AppContext appContext;
    private AutoCompleteTextArea fileContentArea;
    private OperatorParametersTable paramsTable;
    private BindingContext paramContext;

    public TemplateParameterEditorDialog(AppContext appContext, TemplateParameterDescriptor parameter, ToolAdapterOperatorDescriptor parent) {
        super(appContext.getApplicationWindow(), parameter.getName(), ID_OK_CANCEL, "");

        this.appContext = appContext;
        EscapeAction.register(getJDialog());

        this.fileContentArea = new AutoCompleteTextArea("", 10, 10);
        this.parameter = parameter;
        this.parentDescriptor = parent;

        try {
            PropertyDescriptor propertyDescriptor = ParameterDescriptorFactory.convert(this.parameter, new ParameterDescriptorFactory().getSourceProductMap());
            DefaultPropertySetDescriptor propertySetDescriptor = new DefaultPropertySetDescriptor();
            propertySetDescriptor.addPropertyDescriptor(propertyDescriptor);
            PropertyContainer paramContainer = PropertyContainer.createMapBacked(new HashMap<>(), propertySetDescriptor);
            this.paramContext = new BindingContext(paramContainer);
        } catch (ConversionException e) {
            logger.warning(e.getMessage());
        }

        try {
            parameter.setTemplateEngine(parentDescriptor.getTemplateEngine());
        } catch (TemplateException e) {
            e.printStackTrace();
            logger.warning(e.getMessage());
        }

        this.fakeOperatorDescriptor = new ToolAdapterOperatorDescriptor("OperatorForParameters", ToolAdapterOp.class);
        for (ToolParameterDescriptor param : parameter.getParameterDescriptors()) {
            this.fakeOperatorDescriptor.getToolParameterDescriptors().add(new ToolParameterDescriptor(param));
        }
        PropertyChangeListener pcListener = evt -> updateFileAreaContent();
        this.paramContext.addPropertyChangeListener(pcListener);

        addComponents();
    }

    private JPanel createParametersPanel() {
        JPanel paramsPanel = new JPanel();
        BoxLayout layout = new BoxLayout(paramsPanel, BoxLayout.PAGE_AXIS);
        paramsPanel.setLayout(layout);
        AbstractButton addParamBut = ToolButtonFactory.createButton(UIUtils.loadImageIcon("/org/esa/snap/resources/images/icons/Add16.png"), false);
        addParamBut.setAlignmentX(Component.LEFT_ALIGNMENT);
        paramsPanel.add(addParamBut);

        this.paramsTable =  new OperatorParametersTable(this.fakeOperatorDescriptor, appContext);
        JScrollPane tableScrollPane = new JScrollPane(paramsTable);
        tableScrollPane.setPreferredSize(new Dimension(500, 130));
        tableScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        paramsPanel.add(tableScrollPane);
        addParamBut.addActionListener((ActionEvent e) -> paramsTable.addParameterToTable());
        TitledBorder title = BorderFactory.createTitledBorder("Template Parameters");
        paramsPanel.setBorder(title);
        return paramsPanel;
    }

    private Property getProperty() {
        Property[] properties = this.paramContext.getPropertySet().getProperties();
        return properties[0];
    }

    private void addComponents() {
        Property property = getProperty();
        try {
            property.setValue(this.parameter.getTemplate().getTemplatePath());
        } catch (ValidationException e) {
            logger.warning(e.getMessage());
        }

        FileEditor fileEditor = new FileEditor();
        JComponent filePathComponent = fileEditor.createEditorComponent(property.getDescriptor(), this.paramContext);
        filePathComponent.setPreferredSize(new Dimension(770, 25));

        JPanel filePanel = new JPanel();
        filePanel.add(new JLabel("File:"));
        filePanel.add(filePathComponent);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(800, 550));
        mainPanel.add(filePanel, BorderLayout.PAGE_START);

        //to create UI component for outputFile
        fileContentArea.setAutoCompleteEntries(getAutocompleteEntries());
        fileContentArea.setTriggerChar('$');
        mainPanel.add(new JScrollPane(fileContentArea), BorderLayout.CENTER);

        updateFileAreaContent();

        mainPanel.add(createParametersPanel(), BorderLayout.PAGE_END);

        setContent(mainPanel);
    }

    private void updateFileAreaContent() {
        String result = null;
        try {
            File file = getProperty().getValue();
            this.parameter.getTemplate().setFileName(file.getName());
            if (!file.isAbsolute()) {
                file = this.parameter.getTemplate().getTemplatePath();
            }
            if (file.exists()) {
                result = new String(Files.readAllBytes(file.toPath()));
            }
        } catch (Exception e) {
            logger.warning(e.getMessage());
            showWarningDialog("There was an error loading the template file: " + e.getMessage());
        }
        if (result != null){
            this.fileContentArea.setText(result);
            this.fileContentArea.setCaretPosition(0);
        } else {
            this.fileContentArea.setText(EMPTY_FILE_CONTENT);
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
            String content = fileContentArea.getText();
            if (!content.equals(EMPTY_FILE_CONTENT)) {
                template.setContents(content, true);
                template.save();
            }
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

