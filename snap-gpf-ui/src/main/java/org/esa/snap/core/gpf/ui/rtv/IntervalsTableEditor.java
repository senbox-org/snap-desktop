package org.esa.snap.core.gpf.ui.rtv;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import com.bc.ceres.swing.binding.PropertyEditor;

import javax.swing.*;

public class IntervalsTableEditor extends PropertyEditor {
    private IntervalsTablePanel intervalsTablePanel;

    @Override
    public JComponent createEditorComponent(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        intervalsTablePanel = new IntervalsTablePanel();

        ComponentAdapter adapter = new IntervalsTableAdapter(intervalsTablePanel);
        bindingContext.bind(propertyDescriptor.getName(), adapter);
        return intervalsTablePanel;
    }
    @Override
    public boolean isValidFor(PropertyDescriptor propertyDescriptor) {
        ValueSet valueSet = propertyDescriptor.getValueSet();
        Class<?> type = propertyDescriptor.getType();
        return valueSet != null && !type.isArray();
    }

    public boolean validateIntervals(){
        return  (intervalsTablePanel == null) ? true : intervalsTablePanel.validateIntervals();
    }

    public String getLastErrorMessage(){
        return (intervalsTablePanel ==null) ? null : intervalsTablePanel.getLastErrorMessage();
    }

}
