package org.esa.snap.product.library.ui.v2;

import org.apache.http.auth.Credentials;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.product.library.v2.RepositoryProduct;
import org.esa.snap.product.library.v2.repository.ProductsRepositoryProvider;

import javax.swing.SwingUtilities;
import java.awt.Image;
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
    private final ProductsRepositoryProvider productsRepositoryProvider;
    private final QueryProductResultsPanel productResultsPanel;
    private final ThreadListener threadListener;
    private final AbstractProductsRepositoryPanel productsRepositoryPanel;

    public DownloadQuickLookImagesRunnable(List<RepositoryProduct> productList, Credentials credentials, ThreadListener threadListener,
                                           AbstractProductsRepositoryPanel productsRepositoryPanel, ProductsRepositoryProvider productsRepositoryProvider,
                                           QueryProductResultsPanel productResultsPanel) {

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
        int iconWidth = ProductListCellRenderer.EMPTY_ICON.getIconWidth();
        int iconHeight = ProductListCellRenderer.EMPTY_ICON.getIconHeight();
        for (int i=0; i<this.productList.size(); i++) {
            if (!isRunning()) {
                return null; // nothing to return
            }

            RepositoryProduct product = this.productList.get(i);
            Image scaledQuickLookImage = null;
            if (product.getQuickLookLocation() != null) {
                try {
                    BufferedImage quickLookImage = this.productsRepositoryProvider.downloadProductQuickLookImage(this.credentials, product.getQuickLookLocation(), this);

                    if (!isRunning()) {
                        return null; // nothing to return
                    }

                    if (quickLookImage != null) {
                        scaledQuickLookImage = quickLookImage.getScaledInstance(iconWidth, iconHeight, BufferedImage.SCALE_FAST);
                    }
                } catch (InterruptedException exception) {
                    logger.log(Level.SEVERE, "Stop downloading the product quick look image from url '" + product.getQuickLookLocation() + "'.", exception);
                    return null; // nothing to return
                } catch (Exception exception) {
                    logger.log(Level.SEVERE, "Failed to download the product quick look image from url '" + product.getQuickLookLocation() + "'.", exception);
                }
            }
            notifyDownloadedQuickLookImageLater(product, scaledQuickLookImage);
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

    private void notifyDownloadedQuickLookImageLater(RepositoryProduct product, Image quickLookImage) {
        Runnable runnable = new ProductQuickLookImageRunnable(product, quickLookImage) {
            @Override
            protected void execute(RepositoryProduct productValue, Image quickLookImageValue) {
                productResultsPanel.setProductQuickLookImage(productValue, quickLookImageValue);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private static abstract class ProductQuickLookImageRunnable implements Runnable {

        private final RepositoryProduct product;
        private final Image quickLookImage;

        public ProductQuickLookImageRunnable(RepositoryProduct product, Image quickLookImage) {
            this.product = product;
            this.quickLookImage = quickLookImage;
        }

        protected abstract void execute(RepositoryProduct product, Image quickLookImage);

        @Override
        public void run() {
            execute(this.product, this.quickLookImage);
        }
    }
}
