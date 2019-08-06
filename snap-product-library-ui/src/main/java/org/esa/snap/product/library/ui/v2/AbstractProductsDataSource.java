package org.esa.snap.product.library.ui.v2;

import javax.swing.JPanel;

/**
 * Created by jcoravu on 5/8/2019.
 */
public abstract class AbstractProductsDataSource {

    protected AbstractProductsDataSource() {
    }

    public abstract String getName();

    public String[] getSupportedSensors() {
        return null;
    }

    public JPanel buildParametersPanel() {
        return null;
    }
}
