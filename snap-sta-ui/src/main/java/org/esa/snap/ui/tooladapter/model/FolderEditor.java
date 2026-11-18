package org.esa.snap.ui.tooladapter.model;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.internal.TextComponentAdapter;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

/**
 * Folder picker editor.
 *
 * @author Cosmin Cara
 */
public class FolderEditor extends PropertyEditor {

    @Override
    public boolean isValidFor(PropertyDescriptor propertyDescriptor) {
        return Path.class.isAssignableFrom(propertyDescriptor.getType())
                && Boolean.TRUE.equals(propertyDescriptor.getAttribute("directory"));
    }

    @Override
    public JComponent createEditorComponent(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        final JTextField textField = new JTextField();
        final ComponentAdapter adapter = new TextComponentAdapter(textField);
        final Binding binding = bindingContext.bind(propertyDescriptor.getName(), adapter);
        final JPanel editorPanel = new JPanel(new BorderLayout(2, 2));
        editorPanel.add(textField, BorderLayout.CENTER);
        final JButton etcButton = new JButton("...");
        final Dimension size = new Dimension(26, 16);
        etcButton.setPreferredSize(size);
        etcButton.setMinimumSize(size);
        etcButton.addActionListener(e -> {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            final Path currentFolder = (Path) binding.getPropertyValue();
            if (currentFolder != null) {
                fileChooser.setSelectedFile(currentFolder.toFile());
            } else {
                Path selectedFolder = null;
                Object value = propertyDescriptor.getDefaultValue();
                if (value instanceof Path valuePath) {
                    selectedFolder = valuePath;
                } else if (value != null) {
                    selectedFolder = Path.of(value.toString());
                }
                if (selectedFolder != null) {
                    fileChooser.setSelectedFile(selectedFolder.toFile());
                }
            }
            int i = fileChooser.showDialog(editorPanel, "Select");
            if (i == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile() != null) {
                binding.setPropertyValue(fileChooser.getSelectedFile());
            }
        });
        editorPanel.add(etcButton, BorderLayout.EAST);
        return editorPanel;
    }

}
