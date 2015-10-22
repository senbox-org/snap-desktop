package org.esa.snap.ui.tooladapter.validators;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;

import java.text.MessageFormat;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Simple validator that checks a regex is compilable.
 *
 * @author Cosmin Cara
 */
public class RegexFieldValidator implements Validator {
    @Override
    public void validateValue(Property property, Object value) throws ValidationException {
        if (value != null && !value.toString().trim().isEmpty()) {
            try {
                Pattern.compile(value.toString());
            } catch (PatternSyntaxException e) {
                throw new ValidationException(MessageFormat.format("The regular expression for ''{0}'' is not valid [{1}]",
                        property.getDescriptor().getDisplayName(), e.getMessage()));
            }
        }
    }
}
