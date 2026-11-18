package org.esa.snap.ui.tooladapter.model;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.internal.ListSelectionAdapter;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.esa.snap.utils.SpringUtilities;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Editor for properties of type Path[].
 *
 * @author Cosmin Cara
 */
public class FileListEditor extends PropertyEditor {

    @Override
    public boolean isValidFor(PropertyDescriptor propertyDescriptor) {
        return Path[].class.isAssignableFrom(propertyDescriptor.getType());
    }

    @Override
    public JComponent createEditorComponent(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        JPanel panel = new JPanel(new SpringLayout());
        DefaultListModel<Path> listModel = new DefaultListModel<>();
        JList<Path> list = new JList<>(listModel);
        ComponentAdapter adapter = new ListSelectionAdapter(list) {
            public void valueChanged(ListSelectionEvent event) {
                if (event.getValueIsAdjusting() || getBinding().isAdjustingComponents()) {
                    return;
                }
                final Property model = getBinding().getContext().getPropertySet().getProperty(getBinding().getPropertyName());
                try {
                    List<Path> selectedValuesList = list.getSelectedValuesList();
                    model.setValue(selectedValuesList.toArray(new Path[0]));
                    // Now model is in sync with UI
                    getBinding().clearProblem();
                } catch (ValidationException e) {
                    getBinding().reportProblem(e);
                }
            }
        };
        final Binding binding = bindingContext.bind(propertyDescriptor.getName(), adapter);
        list.setMinimumSize(new Dimension(250, 24*5));
        list.setPreferredSize(new Dimension(250, 24 * 5));
        panel.add(new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        AbstractButton addFileBtn = ToolButtonFactory.createButton(UIUtils.loadImageIcon("/org/esa/snap/resources/images/icons/Add16.png"), false);
        addFileBtn.setMaximumSize(new Dimension(20, 20));
        addFileBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addFileBtn.addActionListener(e -> {
            final JFileChooser fileChooser = new JFileChooser();
            int i = fileChooser.showDialog(panel, "Select");
            if (i == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile() != null) {
                final Path selectedFile = fileChooser.getSelectedFile().toPath();
                listModel.addElement(selectedFile);
                syncPropertyValue(binding, listModel);
            }
        });
        AbstractButton removeFileBtn = ToolButtonFactory.createButton(UIUtils.loadImageIcon("/org/esa/snap/resources/images/icons/Remove16.png"), false);
        removeFileBtn.setMaximumSize(new Dimension(20, 20));
        removeFileBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        removeFileBtn.addActionListener(e -> {
            Object selection = list.getSelectedValue();
            if (selection != null) {
                listModel.removeElement(selection);
                syncPropertyValue(binding, listModel);
            }
        });

        JPanel buttonsPannel = new JPanel(new SpringLayout());
        buttonsPannel.add(addFileBtn);
        buttonsPannel.add(removeFileBtn);
        SpringUtilities.makeCompactGrid(buttonsPannel, 2, 1, 0, 0, 0, 0);

        panel.add(buttonsPannel);

        SpringUtilities.makeCompactGrid(panel, 1, 2, 0, 0, 0, 0);

        return panel;
    }

    private void syncPropertyValue(Binding binding, DefaultListModel<Path> listModel) {
        Object[] objects = listModel.toArray();
        binding.setPropertyValue(Arrays.stream(objects)
                                        .map(item -> Path.of(item.toString()))
                                        .toList()
                                        .toArray(new Path[objects.length]));
    }

}