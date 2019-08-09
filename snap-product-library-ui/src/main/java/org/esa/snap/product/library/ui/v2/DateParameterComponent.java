package org.esa.snap.product.library.ui.v2;

import org.jdesktop.swingx.JXDatePicker;

import java.util.Date;

/**
 * Created by jcoravu on 7/8/2019.
 */
public class DateParameterComponent extends AbstractParameterComponent<Date> {

    private final JXDatePicker component;

    public DateParameterComponent(String parameterName) {
        super(parameterName);

        this.component = new JXDatePicker();
    }

    @Override
    public JXDatePicker getComponent() {
        return this.component;
    }

    @Override
    public Date getParameterValue() {
        return this.component.getDate();
    }
}
