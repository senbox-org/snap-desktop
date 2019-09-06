package org.esa.snap.product.library.ui.v2;

import org.apache.http.auth.Credentials;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;

import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 13/8/2019.
 */
public class DownloadQuickLookImagesRunnable extends AbstractRunnable<Void> {

    private static final Logger logger = Logger.getLogger(DownloadQuickLookImagesRunnable.class.getName());

    private final List<RepositoryProduct> productList;
    private final Credentials credentials;
    private final RemoteProductsRepositoryProvider productsRepositoryProvider;
    private final RemoteRepositoryProductListPanel productResultsPanel;
    private final ThreadListener threadListener;
    private final AbstractProductsRepositoryPanel productsRepositoryPanel;

    public DownloadQuickLookImagesRunnable(List<RepositoryProduct> productList, Credentials credentials, ThreadListener threadListener,
                                           AbstractProductsRepositoryPanel productsRepositoryPanel, RemoteProductsRepositoryProvider productsRepositoryProvider,
                                           RemoteRepositoryProductListPanel productResultsPanel) {

        super();

        this.productsRepositoryProvider = productsRepositoryProvider;
        this.productList = productList;
        this.credentials = credentials;
        this.threadListener = threadListener;
        this.productsRepositoryPanel = productsRepositoryPanel;
        this.productResultsPanel = productResultsPanel;
    }

    @Override
    protected Void execute() throws Exception {
        for (int i=0; i<this.productList.size(); i++) {
            if (!isRunning()) {
                return null; // nothing to return
            }

            RepositoryProduct product = this.productList.get(i);
            BufferedImage quickLookImage = null;
            if (product.getDownloadQuickLookImageURL() != null) {
                try {
                    quickLookImage = this.productsRepositoryProvider.downloadProductQuickLookImage(this.credentials, product.getDownloadQuickLookImageURL(), this);
                } catch (InterruptedException exception) {
                    logger.log(Level.SEVERE, "Stop downloading the product quick look image from url '" + product.getDownloadQuickLookImageURL() + "'.", exception);
                    return null; // nothing to return
                } catch (Exception exception) {
                    logger.log(Level.SEVERE, "Failed to download the product quick look image from url '" + product.getDownloadQuickLookImageURL() + "'.", exception);
                }
            }
            setProductQuickLookImageLater(product, quickLookImage);
        }

        return null; // nothing to return
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to download the quick look images.";
    }

    @Override
    public void stopRunning() {
        super.stopRunning();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                onStopExecuting();
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    protected void onStopExecuting() {
        this.threadListener.onStopExecuting(this.productsRepositoryPanel);
    }

    private void setProductQuickLookImageLater(RepositoryProduct product, BufferedImage quickLookImage) {
        Runnable runnable = new ProductQuickLookImageRunnable(product, quickLookImage) {
            @Override
            protected void execute(RepositoryProduct productValue, BufferedImage quickLookImageValue) {
                productValue.setQuickLookImage(quickLookImageValue);
                productResultsPanel.repaint();
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private static abstract class ProductQuickLookImageRunnable implements Runnable {

        private final RepositoryProduct product;
        private final BufferedImage quickLookImage;

        public ProductQuickLookImageRunnable(RepositoryProduct product, BufferedImage quickLookImage) {
            this.product = product;
            this.quickLookImage = quickLookImage;
        }

        protected abstract void execute(RepositoryProduct product, BufferedImage quickLookImage);

        @Override
        public void run() {
            execute(this.product, this.quickLookImage);
        }
    }
}
