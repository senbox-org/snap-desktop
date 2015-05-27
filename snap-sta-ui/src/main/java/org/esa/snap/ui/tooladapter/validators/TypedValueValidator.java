package org.esa.snap.ui.tooladapter.validators;

/**
 * Created by kraftek on 5/5/2015.
 */
public class TypedValueValidator extends TextFieldValidator {

    private Class type;

    public TypedValueValidator(String message, Class clazz) {
        super(message);
        this.type = clazz;
    }

    @Override
    protected boolean verifyValue(String text) {
        boolean isValid = false;
        if (text != null) {
            try {
                Object cast = this.type.cast(text);
                isValid = true;
            } catch (Exception ignored) {
            }
        }
        return isValid;
    }
}
