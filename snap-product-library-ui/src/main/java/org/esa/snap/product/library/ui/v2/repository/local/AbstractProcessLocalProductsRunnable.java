package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.ProductListModel;
import org.esa.snap.product.library.ui.v2.RepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.AppContext;

import javax.swing.SwingUtilities;
import java.util.List;

/**
 * Created by jcoravu on 27/9/2019.
 */
public abstract class AbstractProcessLocalProductsRunnable extends AbstractRunnable<Void> {

    protected final AppContext appContext;
    protected final List<RepositoryProduct> productsToProcess;

    private final RepositoryProductListPanel repositoryProductListPanel;

    protected AbstractProcessLocalProductsRunnable(AppContext appContext, RepositoryProductListPanel repositoryProductListPanel, List<RepositoryProduct> productsToProcess) {
        this.appContext = appContext;
        this.repositoryProductListPanel = repositoryProductListPanel;
        this.productsToProcess = productsToProcess;
    }

    protected final void updateProductProgressStatusLater(RepositoryProduct repositoryProduct, byte localStatus) {
        UpdateLocalProgressStatusRunnable runnable = new UpdateLocalProgressStatusRunnable(repositoryProduct, localStatus, this.repositoryProductListPanel);
        SwingUtilities.invokeLater(runnable);
    }

    private static class UpdateLocalProgressStatusRunnable implements Runnable {

        private final RepositoryProduct productToOpen;
        private final byte localStatus;
        private final RepositoryProductListPanel repositoryProductListPanel;

        private UpdateLocalProgressStatusRunnable(RepositoryProduct productToOpen, byte localStatus, RepositoryProductListPanel repositoryProductListPanel) {
            this.productToOpen = productToOpen;
            this.localStatus = localStatus;
            this.repositoryProductListPanel = repositoryProductListPanel;
        }

        @Override
        public void run() {
            ProductListModel productListModel = this.repositoryProductListPanel.getProductListPanel().getProductListModel();
            productListModel.setLocalProductStatus(this.productToOpen, this.localStatus);
        }
    }
}
