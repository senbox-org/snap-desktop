package org.esa.snap.ui.loading;

import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Created by jcoravu on 11/1/2019.
 */
public class CustomFileChooser extends JFileChooser {

    public static final String FILE_CHOOSER_READ_ONLY_KEY = "FileChooser.readOnly";

    private final boolean previousReadOnlyFlag;
    private final PropertyChangeListener propertyChangeListener;

    private JTextComponent textField;

    public CustomFileChooser(boolean previousReadOnlyFlag) {
        super();

        this.previousReadOnlyFlag = previousReadOnlyFlag;
        this.propertyChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equalsIgnoreCase(event.getPropertyName())) {
                    if (getFileSelectionMode() == JFileChooser.FILES_ONLY) {
                        resetSelectedFile();
                    }
                } else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equalsIgnoreCase(event.getPropertyName())
                            || JFileChooser.SELECTED_FILES_CHANGED_PROPERTY.equalsIgnoreCase(event.getPropertyName())) {

                    if (getFileSelectionMode() == JFileChooser.FILES_ONLY && event.getNewValue() == null) {
                        resetSelectedFile();
                    }
                }
            }
        };
    }

    @Override
    protected JDialog createDialog(Component parent) throws HeadlessException {
        JDialog dialog = super.createDialog(parent);

        dialog.setMinimumSize(new Dimension(450, 350));
        addPropertyChangeListener(this.propertyChangeListener);

        this.textField = findTextField(this);
        List<JComboBox<?>> comboBoxes = findComboBoxes(this);
        if (this.textField != null && comboBoxes.size() > 0) {
            Dimension preferredTextFieldSize = this.textField.getPreferredSize();
            int maximumHeight = preferredTextFieldSize.height;
            for (int i=0; i<comboBoxes.size(); i++) {
                Dimension preferredComboBoxSize = comboBoxes.get(i).getPreferredSize();
                maximumHeight = Math.max(maximumHeight, preferredComboBoxSize.height);
            }
            preferredTextFieldSize.height = maximumHeight;
            this.textField.setPreferredSize(preferredTextFieldSize);
            for (int i=0; i<comboBoxes.size(); i++) {
                JComboBox<?> comboBox = comboBoxes.get(i);
                Dimension preferredComboBoxSize = comboBox.getPreferredSize();
                preferredComboBoxSize.height = maximumHeight;
                comboBox.setPreferredSize(preferredComboBoxSize);
            }
        }

        return dialog;
    }

    @Override
    public int showDialog(Component parent, String approveButtonText) throws HeadlessException {
        int returnCode = super.showDialog(parent, approveButtonText);

        UIManager.getDefaults().put(FILE_CHOOSER_READ_ONLY_KEY, this.previousReadOnlyFlag);

        return returnCode;
    }

    public Path getSelectedPath() {
        File file = super.getSelectedFile();
        return Paths.get(file.toURI());
    }

    public void setCurrentDirectoryPath(Path directoryPath) {
        super.setCurrentDirectory(directoryPath.toFile());
    }

    public void setSelectedPath(Path path) {
        super.setSelectedFile(path.toFile());
    }

    private void resetSelectedFile() {
        removePropertyChangeListener(this.propertyChangeListener);
        try {
            setSelectedFile(null);
            setSelectedFiles(null);
            if (this.textField != null) {
                this.textField.setText("");
            }
        } finally {
            addPropertyChangeListener(this.propertyChangeListener);
        }
    }

    private static JTextComponent findTextField(Container root) {
        Component[] components = root.getComponents();
        for (Component component : components) {
            if (component instanceof JTextComponent) {
                return (JTextComponent) component;
            }
            if (component instanceof Container) {
                JTextComponent filePane = findTextField((Container) component);
                if (filePane != null) {
                    return filePane;
                }
            }
        }
        return null;
    }

    private static List<JComboBox<?>> findComboBoxes(Container root) {
        List<JComboBox<?>> comboBoxes = new ArrayList<JComboBox<?>>();
        Stack<Container> stack = new Stack<Container>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Container container = stack.pop();
            Component[] components = container.getComponents();
            for (Component component : components) {
                if (component instanceof JComboBox<?>) {
                    comboBoxes.add((JComboBox<?>) component);
                } else if (component instanceof Container) {
                    stack.push((Container) component);
                }
            }
        }
        return comboBoxes;
    }

    public static FileFilter buildXMLFileFilter() {
        return new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return true;
                }
                return StringUtils.endsWithIgnoreCase(file.getName(), ".xml");
            }

            @Override
            public String getDescription() {
                return "*.xml";
            }
        };
    }

    public static CustomFileChooser buildFileChooser(String dialogTitle, boolean multiSelectionEnabled, int fileSelectionMode) {
        boolean previousReadOnlyFlag = UIManager.getDefaults().getBoolean(CustomFileChooser.FILE_CHOOSER_READ_ONLY_KEY);
        UIManager.getDefaults().put(CustomFileChooser.FILE_CHOOSER_READ_ONLY_KEY, true);

        CustomFileChooser fileChooser = new CustomFileChooser(previousReadOnlyFlag);
        fileChooser.setDialogTitle(dialogTitle);
        fileChooser.setMultiSelectionEnabled(multiSelectionEnabled);
        fileChooser.setFileSelectionMode(fileSelectionMode);
        return fileChooser;
    }
}
