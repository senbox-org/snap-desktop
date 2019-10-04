package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.engine_utilities.util.FileIOUtils;
import org.esa.snap.product.library.ui.v2.RepositoryProductListPanel;
import org.esa.snap.product.library.v2.database.LocalRepositoryProduct;
import org.esa.snap.product.library.v2.database.ProductLibraryDAL;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.AppContext;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 27/9/2019.
 */
public class DeleteLocalProductsRunnable extends AbstractProcessLocalProductsRunnable {

    private static final Logger logger = Logger.getLogger(DeleteLocalProductsRunnable.class.getName());

    public DeleteLocalProductsRunnable(AppContext appContext, RepositoryProductListPanel repositoryProductListPanel, List<RepositoryProduct> productsToDelete) {
        super(appContext, repositoryProductListPanel, productsToDelete);
    }

    @Override
    protected Void execute() throws Exception {
        for (int i = 0; i<this.productsToProcess.size(); i++) {
            LocalRepositoryProduct repositoryProduct = (LocalRepositoryProduct)this.productsToProcess.get(i);
            try {
                updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.DELETING);

                FileIOUtils.deleteFolder(repositoryProduct.getPath());

                ProductLibraryDAL.deleteProduct(repositoryProduct);
                updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.DELETED);
            } catch (Exception exception) {
                updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.FAIL_DELETED);
                logger.log(Level.SEVERE, "Failed to delete the local product '" + repositoryProduct.getDownloadURL() + "'.", exception);
            }
        }
        return null;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to delete the product from the local repository folder and the database.";
    }
}
