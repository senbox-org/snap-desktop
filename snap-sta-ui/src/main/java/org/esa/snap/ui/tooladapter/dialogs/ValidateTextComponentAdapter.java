package org.esa.snap.ui.tooladapter.dialogs;

import com.bc.ceres.swing.binding.internal.TextComponentAdapter;

import javax.swing.*;
import javax.swing.text.JTextComponent;

/**
 * Created by jcoravu on 9/28/2016.
 */
public abstract class ValidateTextComponentAdapter extends TextComponentAdapter {

    public ValidateTextComponentAdapter(JTextComponent textComponent) {
        super(textComponent);
    }

    protected abstract boolean validateText(String text);

    @Override
    public InputVerifier createInputVerifier() {
        return new ValidateTextVerifier();
    }

    private class ValidateTextVerifier extends InputVerifier {

        private ValidateTextVerifier() {
        }

        @Override
        public boolean verify(JComponent input) {
            String text = ((JTextComponent) input).getText();
            if (!validateText(text)) {
                return false;
            }
            actionPerformed(null);
            return getBinding().getProblem() == null;
        }
    }
}
