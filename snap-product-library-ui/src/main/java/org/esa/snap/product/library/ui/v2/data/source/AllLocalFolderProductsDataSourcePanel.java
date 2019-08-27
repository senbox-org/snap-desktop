package org.esa.snap.product.library.ui.v2.data.source;

import java.awt.BorderLayout;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class AllLocalFolderProductsDataSourcePanel extends AbstractProductsDataSourcePanel {

    public AllLocalFolderProductsDataSourcePanel() {
        super(new BorderLayout());
    }

    @Override
    public String getName() {
        return "All Local Folders";
    }
}
