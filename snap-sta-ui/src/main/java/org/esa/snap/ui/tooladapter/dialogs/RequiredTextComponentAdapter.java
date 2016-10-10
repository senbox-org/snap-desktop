package org.esa.snap.ui.tooladapter.dialogs;

import com.bc.ceres.swing.binding.internal.TextComponentAdapter;
import org.esa.snap.rcp.util.Dialogs;

import javax.swing.*;
import javax.swing.text.JTextComponent;

/**
 * Created by jcoravu on 9/23/2016.
 */
public class RequiredTextComponentAdapter extends TextComponentAdapter {
    private final String messageToDisplay;

    public RequiredTextComponentAdapter(JTextComponent textComponent, String messageToDisplay) {
        super(textComponent);

        this.messageToDisplay = messageToDisplay;
    }

    @Override
    public InputVerifier createInputVerifier() {
        return new RequiredTextVerifier();
    }

    private class RequiredTextVerifier extends InputVerifier {

        private RequiredTextVerifier() {
        }

        @Override
        public boolean verify(JComponent input) {
            String text = ((JTextComponent) input).getText();
            if (text != null && text.length() > 0) {
                actionPerformed(null);
                return getBinding().getProblem() == null;
            }
            Dialogs.showError(messageToDisplay);
            return false;
        }
    }
}
