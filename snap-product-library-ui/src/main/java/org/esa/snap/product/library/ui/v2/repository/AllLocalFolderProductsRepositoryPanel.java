package org.esa.snap.product.library.ui.v2.repository;

import java.awt.BorderLayout;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class AllLocalFolderProductsRepositoryPanel extends AbstractProductsRepositoryPanel {

    public AllLocalFolderProductsRepositoryPanel() {
        super(new BorderLayout());
    }

    @Override
    public String getName() {
        return "All Local Folders";
    }
}
