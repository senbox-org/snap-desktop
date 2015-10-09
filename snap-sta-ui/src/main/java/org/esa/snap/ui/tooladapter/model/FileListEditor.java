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
import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by kraftek on 10/7/2015.
 */
public class FileListEditor extends PropertyEditor {

    private int lastIndex = -1;

    @Override
    public boolean isValidFor(PropertyDescriptor propertyDescriptor) {
        return File[].class.isAssignableFrom(propertyDescriptor.getType());
    }

    @Override
    public JComponent createEditorComponent(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        JPanel panel = new JPanel(new SpringLayout());
        DefaultListModel listModel = new DefaultListModel();
        JList list = new JList(listModel);
        ComponentAdapter adapter = new ListSelectionAdapter(list) {
            public void valueChanged(ListSelectionEvent event) {
                if (event.getValueIsAdjusting()) {
                    return;
                }
                if (getBinding().isAdjustingComponents()) {
                    return;
                }
                final Property model = getBinding().getContext().getPropertySet().getProperty(getBinding().getPropertyName());
                Object[] values = listModel.toArray();
                File[] files = Arrays.stream(values)
                        .map(item -> new File(item.toString()))
                        .collect(Collectors.toList())
                        .toArray(new File[values.length]);
                //Object[] selectedValues = list.getSelectedValues();
//                Object array = Array.newInstance(File.class, selectedValues.length);
//                for (int i = 0; i < selectedValues.length; i++) {
//                    Array.set(array, i, selectedValues[i]);
//                }
                try {
                    model.setValue(files);
                    // Now model is in sync with UI
                    getBinding().clearProblem();
                } catch (ValidationException e) {
                    getBinding().reportProblem(e);
                }
            }
        };
        final Binding binding = bindingContext.bind(propertyDescriptor.getName(), adapter);
        panel.add(new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));

        for (int i = 0; i < 4; i++)
            listModel.addElement("");
        list.setMinimumSize(new Dimension(250, 24*5));
        list.setPreferredSize(new Dimension(250, 24 * 5));
        AbstractButton addFileBtn = ToolButtonFactory.createButton(UIUtils.loadImageIcon("/org/esa/snap/resources/images/icons/Add16.png"), false);
        addFileBtn.setMaximumSize(new Dimension(20, 20));
        addFileBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        lastIndex = -1;
        addFileBtn.addActionListener(e -> {
            final JFileChooser fileChooser = new JFileChooser();
            if (lastIndex != -1) {
                fileChooser.setSelectedFile(new File(listModel.get(lastIndex).toString()));
            }
            int i = fileChooser.showDialog(panel, "Select");
            final File selectedFile = fileChooser.getSelectedFile();
            if (i == JFileChooser.APPROVE_OPTION && selectedFile != null) {
                if (++lastIndex < 4) {
                    listModel.set(lastIndex, selectedFile.getAbsolutePath());
                } else {
                    listModel.addElement(selectedFile.getAbsolutePath());
                    lastIndex++;
                }
                final Object[] objects = listModel.toArray();
                binding.setPropertyValue(Arrays.stream(objects).map(item -> new File(item.toString())).collect(Collectors.toList()).toArray(new File[objects.length]));
            }
        });
        AbstractButton removeFileBtn = ToolButtonFactory.createButton(UIUtils.loadImageIcon("/org/esa/snap/resources/images/icons/Remove16.png"), false);
        removeFileBtn.setMaximumSize(new Dimension(20, 20));
        removeFileBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        removeFileBtn.addActionListener(e -> {
            Object selection = list.getSelectedValue();
            if (selection != null) {
                listModel.removeElement(selection);
                binding.setPropertyValue(Arrays.stream(listModel.toArray()).map(item -> new File(item.toString())).collect(Collectors.toList()).toArray());
            }
        });

        JPanel buttonsPannel = new JPanel(new SpringLayout());
        buttonsPannel.add(addFileBtn);
        buttonsPannel.add(removeFileBtn);
        SpringUtilities.makeCompactGrid(buttonsPannel, 2, 1, 0, 0, 0, 0);
        //buttonsPannel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(buttonsPannel);

        SpringUtilities.makeCompactGrid(panel, 1, 2, 0, 0, 0, 0);

        return panel;
    }

}