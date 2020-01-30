package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.AppContext;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 27/9/2019.
 */
public class OpenLocalProductsRunnable extends AbstractProcessLocalProductsRunnable {

    private static final Logger logger = Logger.getLogger(OpenLocalProductsRunnable.class.getName());

    public OpenLocalProductsRunnable(AppContext appContext, RepositoryOutputProductListPanel repositoryProductListPanel, List<RepositoryProduct> productsToOpen) {
        super(appContext, repositoryProductListPanel, productsToOpen);
    }

    @Override
    protected Void execute() throws Exception {
        for (int i = 0; i<this.productsToProcess.size(); i++) {
            RepositoryProduct repositoryProduct = this.productsToProcess.get(i);
            try {
                updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.OPENING);

                Product product = ProductIO.readProduct(repositoryProduct.getURL());
                if (product == null) {
                    updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.FAIL_OPENED);
                } else {
                    this.appContext.getProductManager().addProduct(product);
                    updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.OPENED);
                }
            } catch (Exception exception) {
                updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.FAIL_OPENED);
                logger.log(Level.SEVERE, "Failed to open the local product '" + repositoryProduct.getURL() + "'.", exception);
            }
        }
        return null;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to open the local products.";
    }
}
