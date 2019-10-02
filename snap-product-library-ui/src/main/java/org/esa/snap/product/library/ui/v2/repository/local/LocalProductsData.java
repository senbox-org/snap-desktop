package org.esa.snap.product.library.ui.v2.repository.local;

import java.awt.event.ActionListener;

/**
 * Created by jcoravu on 2/10/2019.
 */
public class LocalProductsData {

    private final ActionListener openProductListener;
    private final ActionListener deleteProductListener;
    private final ActionListener batchProcessingListener;
    private final ActionListener showInExplorerListener;

    public LocalProductsData(ActionListener openProductListener, ActionListener deleteProductListener, ActionListener batchProcessingListener, ActionListener showInExplorerListener) {
        this.openProductListener = openProductListener;
        this.deleteProductListener = deleteProductListener;
        this.batchProcessingListener = batchProcessingListener;
        this.showInExplorerListener = showInExplorerListener;
    }

    public ActionListener getBatchProcessingListener() {
        return batchProcessingListener;
    }

    public ActionListener getDeleteProductListener() {
        return deleteProductListener;
    }

    public ActionListener getOpenProductListener() {
        return openProductListener;
    }

    public ActionListener getShowInExplorerListener() {
        return showInExplorerListener;
    }
}
