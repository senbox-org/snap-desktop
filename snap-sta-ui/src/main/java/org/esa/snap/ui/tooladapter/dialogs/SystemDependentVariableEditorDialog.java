package org.esa.snap.ui.tooladapter.dialogs;

import org.esa.snap.core.gpf.descriptor.SystemDependentVariable;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.tooladapter.actions.EscapeAction;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * A simple editor window for system-dependent variables
 *
 * @author Cosmin Cara
 */
public class SystemDependentVariableEditorDialog extends ModalDialog {

    private SystemDependentVariable oldVariable;
    private SystemDependentVariable newVariable;

    private Logger logger;

    public SystemDependentVariableEditorDialog(Window parent, SystemDependentVariable variable, String helpID) {
        super(parent, String.format("Edit %s", variable.getKey()), ID_OK_CANCEL, helpID);
        oldVariable = variable;
        newVariable = (SystemDependentVariable)oldVariable.createCopy();
        newVariable.setTransient(true);
        logger = Logger.getLogger(ToolAdapterEditorDialog.class.getName());
        setContent(createPanel());
        EscapeAction.register(getJDialog());
    }

    @Override
    protected void onOK() {
        super.onOK();
        oldVariable.setWindows(newVariable.getWindows());
        oldVariable.setLinux(newVariable.getLinux());
        oldVariable.setMacosx(newVariable.getMacosx());
    }

    private JPanel createPanel() {
        GridBagLayout layout = new GridBagLayout();
        layout.columnWidths = new int[]{100, 390};
        JPanel mainPanel = new JPanel(layout);
        addTextEditor(mainPanel, "Windows value:", newVariable.getWindows(), "windows", 0);
        addTextEditor(mainPanel, "Linux value:", newVariable.getLinux(), "linux", 1);
        addTextEditor(mainPanel, "MacOSX value:", newVariable.getMacosx(), "macosx", 2);
        return mainPanel;
    }

    private void addTextEditor(JPanel parent, String label, String value, String fieldName, int line){
        parent.add(new JLabel(label), getConstraints(line, 0, 1));
        JTextField textField = new JTextField(value);
        textField.getDocument().addDocumentListener(new TextChangeListener(textField, newVariable, fieldName));
        parent.add(textField, getConstraints(line, 1, 1));
    }

    private GridBagConstraints getConstraints(int row, int col, int noCells) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = col;
        c.gridy = row;
        if(noCells != -1){
            c.gridwidth = noCells;
        }
        c.insets = new Insets(2, 10, 2, 10);
        return c;
    }

    private class TextChangeListener implements DocumentListener {

        private JTextField parent;
        private SystemDependentVariable instance;
        private Method fieldSetter;

        TextChangeListener(JTextField parentControl, SystemDependentVariable instance, String fieldName) {
            this.parent = parentControl;
            this.instance = instance;
            try {
                this.fieldSetter = instance.getClass().getDeclaredMethod("set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1), String.class);
            } catch (NoSuchMethodException e) {
                logger.severe(e.getMessage());
            }
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            updateProperty();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updateProperty();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            updateProperty();
        }

        private void updateProperty() {
            EventQueue.invokeLater(() -> {
                try {
                    fieldSetter.invoke(instance, parent.getText());
                } catch (Exception e1) {
                    logger.severe(e1.getMessage());
                }
            });
        }
    }

}
