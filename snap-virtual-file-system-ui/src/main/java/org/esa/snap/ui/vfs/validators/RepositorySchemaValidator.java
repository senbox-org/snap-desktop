package org.esa.snap.ui.vfs.validators;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;

import java.util.regex.Pattern;

public class RepositorySchemaValidator implements Validator {

    private static final String REPOSITORY_SCHEMA_VALIDATOR_PATTERN = "^([a-z0-9]+:)$";

    @Override
    public void validateValue(Property property, Object value) throws ValidationException {
        if (!isValid((String) value)) {
            throw new ValidationException("Invalid VFS repository schema! Please check if it meets following requirements:\n- It must be one from the following list: (\"s3://\";\"http://\";\"oss://\")");
        }
    }

    public boolean isValid(String repositorySchema) {
        return Pattern.compile(REPOSITORY_SCHEMA_VALIDATOR_PATTERN).matcher(repositorySchema).matches();
    }

}
