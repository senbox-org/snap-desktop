package org.esa.snap.product.library.ui.v2.repository.input;

import org.jdesktop.swingx.JXDatePicker;

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

    private final JXDatePicker datePicker;

    public DateParameterComponent(String parameterName, String parameterLabelText, boolean required, int textFieldPreferredHeight, Color backgroundColor) {
        super(parameterName, parameterLabelText, required);

        this.datePicker = new JXDatePicker();
        this.datePicker.setBackground(backgroundColor);
        this.datePicker.getEditor().setOpaque(false);
        this.datePicker.setOpaque(true);
        this.datePicker.setFormats(DATE_FORMAT);

        Dimension preferredSize = this.datePicker.getPreferredSize();
        preferredSize.height = textFieldPreferredHeight;
        this.datePicker.setPreferredSize(preferredSize);
        this.datePicker.setMinimumSize(preferredSize);
    }

    @Override
    public JComponent getComponent() {
        return this.datePicker;
    }

    @Override
    public Date getParameterValue() {
        return this.datePicker.getDate();
    }

    @Override
    public void setParameterValue(Object value) {
        if (value == null) {
            clearParameterValue();
        } else if (value instanceof Date) {
            this.datePicker.setDate((Date)value);
        } else {
            throw new ClassCastException("The parameter value type '" + value + "' must be '" + Date.class+"'.");
        }
    }

    @Override
    public void clearParameterValue() {
        this.datePicker.setDate(null);
    }

    @Override
    public Boolean hasValidValue() {
        String dateAsString = getText();
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
        return "The '" + getLabel().getText()+"' parameter value '"+getText()+"' is invalid.";
    }

    private String getText() {
        return this.datePicker.getEditor().getText().trim();
    }
}
