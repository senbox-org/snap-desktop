package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.RepositoryProductListPanel;
import org.esa.snap.product.library.v2.database.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.LocalRepositoryProduct;
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

    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;

    public DeleteLocalProductsRunnable(AppContext appContext, RepositoryProductListPanel repositoryProductListPanel, List<RepositoryProduct> productsToDelete,
                                       AllLocalFolderProductsRepository allLocalFolderProductsRepository) {

        super(appContext, repositoryProductListPanel, productsToDelete);

        this.allLocalFolderProductsRepository = allLocalFolderProductsRepository;
    }

    @Override
    protected Void execute() throws Exception {
        for (int i = 0; i<this.productsToProcess.size(); i++) {
            LocalRepositoryProduct repositoryProduct = (LocalRepositoryProduct)this.productsToProcess.get(i);
            try {
                updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.DELETING);

                //FileIOUtils.deleteFolder(repositoryProduct.getPath());

                this.allLocalFolderProductsRepository.deleteProduct(repositoryProduct);
                updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.DELETED);
            } catch (Exception exception) {
                updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.FAIL_DELETED);
                logger.log(Level.SEVERE, "Failed to delete the local product '" + repositoryProduct.getURL() + "'.", exception);
            }
        }
        return null;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to delete the product from the local repository folder and the database.";
    }
}
