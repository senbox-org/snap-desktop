package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.ui.v2.thread.ThreadCallback;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryProduct;
import org.esa.snap.productlibrary.db.ProductEntry;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by jcoravu on 21/7/2020.
 */
public class ReadLocalProductsTimerRunnable extends AbstractProgressTimerRunnable<ProductEntry[]> {

    private final RepositoryProduct[] productsToRead;
    private final ThreadCallback<ProductEntry[]> threadCallback;

    public ReadLocalProductsTimerRunnable(ProgressBarHelper progressPanel, int threadId, RepositoryProduct[] productsToRead,
                                          ThreadCallback<ProductEntry[]> threadCallback) {

        super(progressPanel, threadId, 500);

        this.productsToRead = productsToRead;
        this.threadCallback = threadCallback;
    }

    @Override
    protected boolean onTimerWakeUp(String message) {
        return super.onTimerWakeUp("Read "+this.productsToRead.length+" local products...");
    }

    @Override
    protected ProductEntry[] execute() throws Exception {
        ProductEntry[] productEntries = new ProductEntry[this.productsToRead.length];
        for (int i=0; i<this.productsToRead.length; i++) {
            LocalRepositoryProduct repositoryProduct = (LocalRepositoryProduct)this.productsToRead[i];
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
                    throw new IllegalStateException("No product reader found in the application for file '" + productFile.getAbsolutePath()+"'.");
                } else {
                    // there is a product reader in the application
                    Product product = productReader.readProductNodes(productFile, null);
                    if (product == null) {
                        // the product has not been read
                        throw new NullPointerException("The product '" + repositoryProduct.getName() + "' has not been read from '" + productFile.getAbsolutePath()+"'.");
                    } else {
                        // the product has been read
                        productEntries[i] = new ProductEntry(product);
                    }
                }
            } else {
                // the product does not exist into the local repository folder
                throw new IllegalStateException("The product '" + repositoryProduct.getPath() + "' does not exist into the local repository folder.");
            }
        }
        return productEntries;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to read the local products.";
    }

    @Override
    protected void onFailed(Exception exception) {
        this.threadCallback.onFailed(exception);
    }

    @Override
    protected void onSuccessfullyFinish(ProductEntry[] result) {
        this.threadCallback.onSuccessfullyFinish(result);
    }
}
