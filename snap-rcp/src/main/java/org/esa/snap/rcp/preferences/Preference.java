/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.rcp.preferences;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import org.esa.snap.runtime.Config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for user preferences. Use is mandatory if preferences are supplied via a bean, see
 * {@link DefaultConfigController#createPropertySet()} and
 * {@link DefaultConfigController#createPropertySet(Object)}.
 *
 * @see DefaultConfigController
 *
 * @author thomas
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Preference {

    /**
     * @return The label of the property. Must not be empty.
     */
    String label();

    /**
     * @return The property key. Must not be empty.
     */
    String key();

    /**
     * @return The configuration name. If not set, the NetBeans preferences are used, otherwise preferences from a
     * named configuration are used ({@link Config#preferences() Config.instance(<i>config</i>).preferences()}).
     */
    String config() default "";

    /**
     * @return The set of allowed values.
     */
    String[] valueSet() default {};

    /**
     * Gets the valid interval for numeric parameters, e.g. {@code "[10,20)"}: in the range 10 (inclusive) to 20 (exclusive).
     *
     * @return The valid interval. Defaults to empty string (= not set).
     */
    String interval() default "";

    /**
     * Description text (used for tooltips).
     */
    String description() default "";

    /**
     * @return The validator class.
     */
    Class<? extends Validator> validatorClass() default NullValidator.class;

    class NullValidator implements Validator {

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            // ok
        }
    }
}
