package org.esa.snap.ui.vfs.validators;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;

import java.util.regex.Pattern;

public class RepositoryNameValidator implements Validator {

    private static final String REPOSITORY_NAME_VALIDATOR_PATTERN = "^([\\w]{3,25})$";

    @Override
    public void validateValue(Property property, Object value) throws ValidationException {
        if (!isValid((String) value)) {
            throw new ValidationException("Invalid VFS repository name! Please check if it meets following requirements:\n- It must be alphanumeric.\n- Underscores are allowed.\n- Length is between 3 and 25 characters.");
        }
    }

    public boolean isValid(String repositoryName) {
        return Pattern.compile(REPOSITORY_NAME_VALIDATOR_PATTERN).matcher(repositoryName).matches();
    }

}
