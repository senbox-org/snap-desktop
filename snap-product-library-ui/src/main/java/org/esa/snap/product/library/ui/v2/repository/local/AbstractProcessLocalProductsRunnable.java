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

    protected final void updateProductProgressStatusLater(RepositoryProduct repositoryProduct, byte openStatus) {
        UpdateProgressStatusRunnable runnable = new UpdateProgressStatusRunnable(repositoryProduct, openStatus, this.repositoryProductListPanel);
        SwingUtilities.invokeLater(runnable);
    }

    private static class UpdateProgressStatusRunnable implements Runnable {

        private final RepositoryProduct productToOpen;
        private final byte openStatus;
        private final RepositoryProductListPanel repositoryProductListPanel;

        private UpdateProgressStatusRunnable(RepositoryProduct productToOpen, byte openStatus, RepositoryProductListPanel repositoryProductListPanel) {
            this.productToOpen = productToOpen;
            this.openStatus = openStatus;
            this.repositoryProductListPanel = repositoryProductListPanel;
        }

        @Override
        public void run() {
            ProductListModel productListModel = this.repositoryProductListPanel.getProductListPanel().getProductListModel();
            productListModel.setLocalProductStatus(this.productToOpen, this.openStatus);
        }
    }
}
