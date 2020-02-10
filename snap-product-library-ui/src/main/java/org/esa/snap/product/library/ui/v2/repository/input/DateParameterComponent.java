package org.esa.snap.product.library.ui.v2.repository.input;

import org.jdesktop.swingx.JXDatePicker;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jcoravu on 7/8/2019.
 */
public class DateParameterComponent extends AbstractParameterComponent<Date> {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

    private final JXDatePicker component;

    public DateParameterComponent(String parameterName, String parameterLabelText, boolean required, int textFieldPreferredHeight, Color backgroundColor) {
        super(parameterName, parameterLabelText, required);

        this.component = new JXDatePicker();
        this.component.setBackground(backgroundColor);
        this.component.getEditor().setOpaque(false);
        this.component.setOpaque(true);
        this.component.setFormats(DATE_FORMAT);

        Dimension preferredSize = this.component.getPreferredSize();
        preferredSize.height = textFieldPreferredHeight;
        this.component.setPreferredSize(preferredSize);
        this.component.setMinimumSize(preferredSize);
    }

    @Override
    public JComponent getComponent() {
        return this.component;
    }

    @Override
    public Date getParameterValue() {
        return this.component.getDate();
    }

    @Override
    public void setParameterValue(Object value) {
        if (value == null) {
            clearParameterValue();
        } else if (value instanceof Date) {
            this.component.setDate((Date)value);
        } else {
            throw new ClassCastException("The parameter value type '" + value + "' must be '" + Date.class+"'.");
        }
    }

    @Override
    public void clearParameterValue() {
        this.component.setDate(null);
    }
}
