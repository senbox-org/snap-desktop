package org.esa.snap.core.gpf.ui.rtv;

import org.esa.snap.core.gpf.ui.DefaultOperatorAction;
import org.esa.snap.ui.ModelessDialog;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class QuantizationAction extends DefaultOperatorAction {

    private static final Set<String> KNOWN_KEYS = new HashSet<>(Arrays.asList("displayName", "operatorName", "dialogTitle", "helpId", "targetProductNameSuffix"));

    public QuantizationAction(){
        super();
    }

    public static QuantizationAction create(Map<String, Object> properties) {
        QuantizationAction action = new QuantizationAction();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (KNOWN_KEYS.contains(entry.getKey())) {
                action.putValue(entry.getKey(), entry.getValue());
            }
        }
        return action;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        ModelessDialog dialog = createOperatorDialog();
        dialog.show();
    }

    @Override
    protected ModelessDialog createOperatorDialog() {
        QuantizationDialog productDialog = new QuantizationDialog(getOperatorName(), getAppContext(), getDialogTitle(), getHelpId());
        if (getTargetProductNameSuffix() != null) {
            productDialog.setTargetProductNameSuffix(getTargetProductNameSuffix());
        }
        return productDialog;
    }
}
