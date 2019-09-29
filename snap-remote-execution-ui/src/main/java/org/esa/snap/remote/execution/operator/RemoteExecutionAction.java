package org.esa.snap.remote.execution.operator;

import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.AppContext;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by jcoravu on 24/12/2018.
 */
public class RemoteExecutionAction extends AbstractSnapAction {

    private static final Set<String> KNOWN_KEYS = new HashSet<>(Arrays.asList("displayName", "operatorName", "dialogTitle", "helpId"));

    public RemoteExecutionAction() {
    }

    public static RemoteExecutionAction create(Map<String, Object> properties) {
        RemoteExecutionAction action = new RemoteExecutionAction();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (KNOWN_KEYS.contains(entry.getKey())) {
                action.putValue(entry.getKey(), entry.getValue());
            }
        }
        return action;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        AppContext appContext = getAppContext();
        RemoteExecutionDialog dialog = new RemoteExecutionDialog(appContext, appContext.getApplicationWindow());
        dialog.show();
    }
}
