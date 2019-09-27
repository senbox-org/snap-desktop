package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.product.library.ui.v2.ProductListModel;
import org.esa.snap.product.library.ui.v2.RepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.AppContext;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 27/9/2019.
 */
public class OpenProductRunnable extends AbstractRunnable<Void> {

    private static final Logger logger = Logger.getLogger(OpenProductRunnable.class.getName());

    private final AppContext appContext;
    private final RepositoryProductListPanel repositoryProductListPanel;
    private final List<RepositoryProduct> productsToOpen;

    public OpenProductRunnable(AppContext appContext, RepositoryProductListPanel repositoryProductListPanel, List<RepositoryProduct> productsToOpen) {
        this.appContext = appContext;
        this.repositoryProductListPanel = repositoryProductListPanel;
        this.productsToOpen = productsToOpen;
    }

    @Override
    protected Void execute() throws Exception {
        for (int i=0; i<this.productsToOpen.size(); i++) {
            RepositoryProduct repositoryProduct = this.productsToOpen.get(i);
            try {
                updateOpenProductStatusLater(repositoryProduct, OpenProgressStatus.OPENING);

                Product product = ProductIO.readProduct(repositoryProduct.getDownloadURL());
                if (product == null) {
                    updateOpenProductStatusLater(repositoryProduct, OpenProgressStatus.FAILED);
                } else {
                    this.appContext.getProductManager().addProduct(product);
                    updateOpenProductStatusLater(repositoryProduct, OpenProgressStatus.OPENED);
                }
            } catch (Exception exception) {
                updateOpenProductStatusLater(repositoryProduct, OpenProgressStatus.FAILED);
                logger.log(Level.SEVERE, "Failed to open the local product '" + repositoryProduct.getDownloadURL() + "'.", exception);
            }
        }
        return null;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to open the product.";
    }

    private void updateOpenProductStatusLater(RepositoryProduct repositoryProduct, byte openStatus) {
        UpdateOpenProgressRunnable runnable = new UpdateOpenProgressRunnable(repositoryProduct, openStatus, this.repositoryProductListPanel);
        SwingUtilities.invokeLater(runnable);
    }

    private static class UpdateOpenProgressRunnable implements Runnable {

        private final RepositoryProduct productToOpen;
        private final byte openStatus;
        private final RepositoryProductListPanel repositoryProductListPanel;

        private UpdateOpenProgressRunnable(RepositoryProduct productToOpen, byte openStatus, RepositoryProductListPanel repositoryProductListPanel) {
            this.productToOpen = productToOpen;
            this.openStatus = openStatus;
            this.repositoryProductListPanel = repositoryProductListPanel;
        }

        @Override
        public void run() {
            ProductListModel productListModel = repositoryProductListPanel.getProductListPanel().getProductListModel();
            productListModel.setOpenProductStatus(this.productToOpen, this.openStatus);
        }
    }
}
