package org.esa.snap.product.library.ui.v2.repository.input;

import org.jdesktop.swingx.JXDatePicker;

import java.awt.Dimension;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jcoravu on 7/8/2019.
 */
public class DateParameterComponent extends AbstractParameterComponent<Date> {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

    private final JXDatePicker component;

    public DateParameterComponent(String parameterName, String parameterLabelText, boolean required, int textFieldPreferredHeight) {
        super(parameterName, parameterLabelText, required);

        this.component = new JXDatePicker();
        this.component.setFormats(DATE_FORMAT);

        Dimension preferredSize = this.component.getPreferredSize();
        preferredSize.height = textFieldPreferredHeight;
        this.component.setPreferredSize(preferredSize);
        this.component.setMinimumSize(preferredSize);
    }

    @Override
    public JXDatePicker getComponent() {
        return this.component;
    }

    @Override
    public Date getParameterValue() {
        return this.component.getDate();
    }

    @Override
    public void clearParameterValue() {
        this.component.setDate(null);
    }
}
