package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryProduct;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.loading.PairRunnable;

import javax.swing.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The thread to open in the application a list with local products.
 *
 * Created by jcoravu on 27/9/2019.
 */
public class OpenLocalProductsRunnable extends AbstractProcessLocalProductsRunnable {

    private static final Logger logger = Logger.getLogger(OpenLocalProductsRunnable.class.getName());

    private final AppContext appContext;

    public OpenLocalProductsRunnable(ProgressBarHelper progressPanel, int threadId, RepositoryOutputProductListPanel repositoryProductListPanel,
                                     AppContext appContext, List<RepositoryProduct> productsToOpen) {

        super(progressPanel, threadId, repositoryProductListPanel, productsToOpen);

        this.appContext = appContext;
    }

    @Override
    protected boolean onTimerWakeUp(String message) {
        return super.onTimerWakeUp("Open local products...");
    }

    @Override
    protected Void execute() throws Exception {
        for (int i = 0; i<this.productsToProcess.size(); i++) {
            LocalRepositoryProduct repositoryProduct = (LocalRepositoryProduct)this.productsToProcess.get(i);
            try {
                updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.OPENING);

                // check if the local product exists on the disk
                if (Files.exists(repositoryProduct.getPath())) {
                    // the product exists on the local disk

                    //TODO Jean temporary method until the Landsat8 product reader will be changed to read the product from a folder
                    Path productPath = RemoteProductsRepositoryProvider.prepareProductPathToOpen(repositoryProduct.getPath(), repositoryProduct);
                    File productFile = productPath.toFile();

                    //TODO Jean old code to get the product path to open
                    //File productFile = repositoryProduct.getPath().toFile();

                    ProductReader productReader = ProductIO.getProductReaderForInput(productFile);
                    if (productReader == null) {
                        // no product reader found in the application
                        updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.FAIL_OPENED_MISSING_PRODUCT_READER);
                    } else {
                        // there is a product reader in the application
                        Product product = productReader.readProductNodes(productFile, null);
                        if (product == null) {
                            // the product has not been read
                            throw new NullPointerException("The product '" + repositoryProduct.getName()+"' has not been read from '" + productFile.getAbsolutePath()+"'.");
                        } else {
                            // open the product in the application
                            Runnable runnable = new PairRunnable<LocalRepositoryProduct, Product>(repositoryProduct, product) {
                                @Override
                                protected void execute(LocalRepositoryProduct localRepositoryProduct, Product productToOpen) {
                                    appContext.getProductManager().addProduct(productToOpen); // open the product in the application
                                    updateProductProgressStatusLater(localRepositoryProduct, LocalProgressStatus.OPENED);
                                }
                            };
                            SwingUtilities.invokeLater(runnable);
                        }
                    }
                } else {
                    // the product does not exist into the local repository folder
                    updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.MISSING_PRODUCT_FROM_REPOSITORY);
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
