/*
 *
 *  * Copyright (C) 2016 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.ui.tooladapter.validators;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.validators.NotEmptyValidator;
import org.esa.snap.tango.TangoIcons;

import javax.swing.JLabel;

/**
 * @author kraftek
 * @date 3/15/2017
 */
public class DecoratedNotEmptyValidator implements Validator {
    private static final String[] excludedChars = { ".", "-", "+", " " };
    private JLabel label;
    private NotEmptyValidator validator;

    public DecoratedNotEmptyValidator(JLabel componentLabel) {
        this.validator = new NotEmptyValidator();
        this.label = componentLabel;
        this.label.setText("<html><font color=\"#"
                        + Integer.toHexString(this.label.getForeground().getRGB()).substring(2, 8)
                        + "\">"
                        + this.label.getText()
                        + "</font><font color=\"RED\">*</font></html>");
    }
    @Override
    public void validateValue(Property property, Object value) throws ValidationException {
        try {
            if (value != null) {
                String stringValue = value.toString();
                for (String exclChar : excludedChars) {
                    if (stringValue.contains(exclChar)) {
                        throw new ValidationException(String.format("Character '%s' not allowed", exclChar));
                    }
                }
            }
            this.validator.validateValue(property, value);
            this.label.setIcon(null);
            this.label.setToolTipText(null);
        } catch (ValidationException vex) {
            this.label.setIcon(TangoIcons.status_dialog_error(TangoIcons.Res.R16));
            throw vex;
        }
    }
}
