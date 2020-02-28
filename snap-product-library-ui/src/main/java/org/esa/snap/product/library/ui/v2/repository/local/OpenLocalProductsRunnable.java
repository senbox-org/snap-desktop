package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.AppContext;

import java.io.File;
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

                File productFile = new File(repositoryProduct.getURL());
                ProductReader productReader = ProductIO.getProductReaderForInput(productFile);
                if (productReader == null) {
                    // no product reader found in the application
                    updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.FAIL_OPENED_MISSING_PRODUCT_READER);
                } else {
                    // there is a product reader in the application
                    Product product = productReader.readProductNodes(productFile, null);
                    if (product == null) {
                        // the product has not been read
                        //updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.FAIL_OPENED);
                        throw new NullPointerException("The product '" + repositoryProduct.getName()+"' has not been read from '" + productFile.getAbsolutePath()+"'.");
                    } else {
                        this.appContext.getProductManager().addProduct(product);
                        updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.OPENED);
                    }
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
