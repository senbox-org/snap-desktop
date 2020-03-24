package org.esa.snap.product.library.ui.v2.repository.input;

import org.esa.snap.ui.components.DatePickerComboBox;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jcoravu on 7/8/2019.
 */
public class DateParameterComponent extends AbstractParameterComponent<Date> {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

    private final DatePickerComboBox datePickerComboBox;

    public DateParameterComponent(String parameterName, String parameterLabelText, boolean required, int textFieldPreferredHeight, Color backgroundColor) {
        super(parameterName, parameterLabelText, required);

        this.datePickerComboBox = new DatePickerComboBox(textFieldPreferredHeight, backgroundColor, DATE_FORMAT);
    }

    @Override
    public JComponent getComponent() {
        return this.datePickerComboBox;
    }

    @Override
    public Date getParameterValue() {
        return this.datePickerComboBox.getDate();
    }

    @Override
    public void setParameterValue(Object value) {
        if (value == null) {
            clearParameterValue();
        } else if (value instanceof Date) {
            this.datePickerComboBox.setDate((Date)value);
        } else {
            throw new ClassCastException("The parameter value type '" + value + "' must be '" + Date.class+"'.");
        }
    }

    @Override
    public void clearParameterValue() {
        this.datePickerComboBox.setDate(null);
    }

    @Override
    public Boolean hasValidValue() {
        String dateAsString = this.datePickerComboBox.getEnteredDateAsString();
        if (dateAsString.length() > 0) {
            try {
                DATE_FORMAT.parse(dateAsString);
                return true;
            } catch (ParseException e) {
                return false;
            }
        }
        return null; // the value is not specified
    }

    @Override
    public String getInvalidValueErrorDialogMessage() {
        return "The '" + getLabel().getText()+"' parameter value '"+this.datePickerComboBox.getEnteredDateAsString()+"' is invalid.";
    }
}
