package org.esa.snap.ui.tooladapter.validators;

/**
 * Simple class for validating that a text component has no null or empty input.
 *
 * @author Cosmin Cara
 */
public class RequiredFieldValidator extends TextFieldValidator {

    public RequiredFieldValidator(String message) {
        super(message);
    }

    @Override
    protected boolean verifyValue(String text) {
        return text != null && text.length() > 0;
    }
}
