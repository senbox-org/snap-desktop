package org.esa.snap.product.library.ui.v2;

import javax.swing.JPanel;

/**
 * Created by jcoravu on 5/8/2019.
 */
public abstract class AbstractProductsDataSource extends JPanel {

    protected AbstractProductsDataSource() {
    }

    public abstract String getName();
}
