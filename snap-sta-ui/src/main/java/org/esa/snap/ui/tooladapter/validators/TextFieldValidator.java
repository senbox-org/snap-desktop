package org.esa.snap.ui.tooladapter.validators;

import org.esa.snap.rcp.SnapDialogs;

import javax.swing.*;
import javax.swing.text.JTextComponent;

/**
 * Base validator for text-input components
 *
 * @author Cosmin Cara
 */
public abstract class TextFieldValidator extends InputVerifier {

    private String errorMessage;

    public TextFieldValidator(String message) {
        errorMessage = message;
    }

    @Override
    public boolean verify(JComponent input) {
        if (input instanceof JTextComponent) {
            boolean isValid = false;
            String text = ((JTextComponent) input).getText();
            isValid = verifyValue(text);
            if (!isValid) {
                SnapDialogs.showError(errorMessage);
            }
            return isValid;
        } else {
            return true;
        }
    }

    protected abstract boolean verifyValue(String text);

}
