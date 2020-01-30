package org.esa.snap.product.library.ui.v2.repository.remote;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductListModel;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.AppContext;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 12/11/2019.
 */
public class OpenDownloadedProductsRunnable extends AbstractRunnable<Void> {

    private static final Logger logger = Logger.getLogger(OpenDownloadedProductsRunnable.class.getName());

    private final AppContext appContext;
    private final Map<RepositoryProduct, Path> productsToOpen;
    private final RepositoryOutputProductListPanel repositoryProductListPanel;

    public OpenDownloadedProductsRunnable(AppContext appContext, RepositoryOutputProductListPanel repositoryProductListPanel, Map<RepositoryProduct, Path> productsToOpen) {
        this.appContext = appContext;
        this.repositoryProductListPanel = repositoryProductListPanel;
        this.productsToOpen = productsToOpen;
    }

    @Override
    protected Void execute() throws Exception {
        for (Map.Entry<RepositoryProduct, Path> entry : this.productsToOpen.entrySet()) {
            RepositoryProduct repositoryProduct = entry.getKey();
            try {
                updateProductProgressStatusLater(repositoryProduct, DownloadProgressStatus.OPENING);

                Product product = ProductIO.readProduct(entry.getValue().toString());
                if (product == null) {
                    updateProductProgressStatusLater(repositoryProduct, DownloadProgressStatus.FAIL_OPENED);
                } else {
                    this.appContext.getProductManager().addProduct(product);
                    updateProductProgressStatusLater(repositoryProduct, DownloadProgressStatus.OPENED);
                }
            } catch (Exception exception) {
                updateProductProgressStatusLater(repositoryProduct, DownloadProgressStatus.FAIL_OPENED);
                logger.log(Level.SEVERE, "Failed to open the downloaded product '" + repositoryProduct.getURL() + "'.", exception);
            }
        }
        return null;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to open the downloaded products.";
    }

    private void updateProductProgressStatusLater(RepositoryProduct repositoryProduct, byte openStatus) {
        UpdateProgressStatusRunnable runnable = new UpdateProgressStatusRunnable(repositoryProduct, openStatus, this.repositoryProductListPanel);
        SwingUtilities.invokeLater(runnable);
    }

    private static class UpdateProgressStatusRunnable implements Runnable {

        private final RepositoryProduct productToOpen;
        private final byte openStatus;
        private final RepositoryOutputProductListPanel repositoryProductListPanel;

        private UpdateProgressStatusRunnable(RepositoryProduct productToOpen, byte openStatus, RepositoryOutputProductListPanel repositoryProductListPanel) {
            this.productToOpen = productToOpen;
            this.openStatus = openStatus;
            this.repositoryProductListPanel = repositoryProductListPanel;
        }

        @Override
        public void run() {
            OutputProductListModel productListModel = this.repositoryProductListPanel.getProductListPanel().getProductListModel();
            productListModel.setOpenDownloadedProductStatus(this.productToOpen, this.openStatus);
        }
    }
}
