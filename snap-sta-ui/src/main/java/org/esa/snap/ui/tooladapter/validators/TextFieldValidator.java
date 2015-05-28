package org.esa.snap.ui.tooladapter.validators;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * Created by kraftek on 5/5/2015.
 */
public abstract class TextFieldValidator extends InputVerifier {

    private Border initialBorder;
    private String initialToolTipText;
    private String errorMessage;

    public TextFieldValidator(String message) {
        errorMessage = message;
    }

    @Override
    public boolean verify(JComponent input) {
        if (initialBorder == null) {
            initialBorder = input.getBorder();
            initialToolTipText = input.getToolTipText();
        }
        if (input instanceof JTextComponent) {
            boolean isValid = false;
            String text = ((JTextComponent) input).getText();
            isValid = verifyValue(text);
            if (!isValid) {
                input.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
                input.setToolTipText(errorMessage);
            } else {
                input.setBorder(initialBorder);
                input.setToolTipText(initialToolTipText);
            }
            return isValid;
        } else {
            input.setBorder(initialBorder);
            input.setToolTipText(initialToolTipText);
            return true;
        }
    }

    protected abstract boolean verifyValue(String text);

}
