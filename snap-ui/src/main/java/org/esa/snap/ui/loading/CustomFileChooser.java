package org.esa.snap.ui.loading;

import org.apache.commons.lang3.StringUtils;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.HeadlessException;
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
        this.propertyChangeListener = event -> {
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
        };
    }

    private static List<JComboBox<?>> findComboBoxes(Container root) {
        List<JComboBox<?>> comboBoxes = new ArrayList<>();
        Stack<Container> stack = new Stack<>();
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
            for (JComboBox<?> box : comboBoxes) {
                Dimension preferredComboBoxSize = box.getPreferredSize();
                maximumHeight = Math.max(maximumHeight, preferredComboBoxSize.height);
            }
            preferredTextFieldSize.height = maximumHeight;
            this.textField.setPreferredSize(preferredTextFieldSize);
            for (JComboBox<?> comboBox : comboBoxes) {
                Dimension preferredComboBoxSize = comboBox.getPreferredSize();
                preferredComboBoxSize.height = maximumHeight;
                comboBox.setPreferredSize(preferredComboBoxSize);
            }
        }

        return dialog;
    }

    public static FileFilter buildFileFilter(String extension, String description) {
        return new CustomFileFilter(extension, description);
    }

    public static CustomFileChooser buildFileChooser(String dialogTitle, boolean multiSelectionEnabled, int fileSelectionMode) {
        return buildFileChooser(dialogTitle, multiSelectionEnabled, fileSelectionMode, true);
    }

    public static CustomFileChooser buildFileChooser(String dialogTitle, boolean multiSelectionEnabled, int fileSelectionMode, boolean readOnly) {
        boolean previousReadOnlyFlag = UIManager.getDefaults().getBoolean(CustomFileChooser.FILE_CHOOSER_READ_ONLY_KEY);
        UIManager.getDefaults().put(CustomFileChooser.FILE_CHOOSER_READ_ONLY_KEY, readOnly);

        CustomFileChooser fileChooser = new CustomFileChooser(previousReadOnlyFlag);
        fileChooser.setDialogTitle(dialogTitle);
        fileChooser.setMultiSelectionEnabled(multiSelectionEnabled);
        fileChooser.setFileSelectionMode(fileSelectionMode);
        return fileChooser;
    }

    private static class CustomFileFilter extends FileFilter {

        private final String extension;
        private final String description;

        private CustomFileFilter(String extension, String description) {
            if (StringUtils.isBlank(extension)) {
                throw new NullPointerException("The extension is null or empty.");
            }
            if (StringUtils.isBlank(description)) {
                throw new NullPointerException("The description is null or empty.");
            }
            this.extension = extension;
            this.description = description;
        }

        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return true;
            }
            return StringUtils.endsWithIgnoreCase(file.getName(), this.extension);
        }

        @Override
        public String getDescription() {
            return this.description;
        }
    }
}
