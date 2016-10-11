package org.esa.snap.ui.tooladapter.dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @author Jean Coravu
 */
public class ForwardFocusAction extends AbstractAction {
    private final JComponent componentToReceiveFocus;

    public ForwardFocusAction(String actionName, JComponent componentToReceiveFocus) {
        super();

        this.componentToReceiveFocus = componentToReceiveFocus;

        putValue("actionName", actionName);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        this.componentToReceiveFocus.requestFocusInWindow();
    }

    public String getName() {
        return (String)getValue("actionName");
    }
}