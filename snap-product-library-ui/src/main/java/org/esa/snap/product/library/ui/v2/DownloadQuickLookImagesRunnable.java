package org.esa.snap.product.library.ui.v2;

import org.apache.http.auth.Credentials;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.product.library.v2.DataSourceResultsDownloader;
import org.esa.snap.product.library.v2.ProductLibraryItem;

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

    private final List<ProductLibraryItem> productList;
    private final Credentials credentials;
    private final DataSourceResultsDownloader dataSourceResults;
    private final QueryProductResultsPanel productResultsPanel;

    public DownloadQuickLookImagesRunnable(List<ProductLibraryItem> productList, Credentials credentials,
                                           DataSourceResultsDownloader dataSourceResults, QueryProductResultsPanel productResultsPanel) {

        super();

        this.dataSourceResults = dataSourceResults;
        this.productList = productList;
        this.credentials = credentials;
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

            ProductLibraryItem product = this.productList.get(i);
            Image scaledQuickLookImage = null;
            if (product.getQuickLookLocation() != null) {
                try {
                    BufferedImage quickLookImage = this.dataSourceResults.downloadProductQuickLookImage(this.credentials, product.getQuickLookLocation(), this);

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
    }

    private void notifyDownloadedQuickLookImageLater(ProductLibraryItem product, Image quickLookImage) {
        Runnable runnable = new ProductQuickLookImageRunnable(product, quickLookImage) {
            @Override
            protected void execute(ProductLibraryItem productValue, Image quickLookImageValue) {
                productResultsPanel.setProductQuickLookImage(productValue, quickLookImageValue);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private static abstract class ProductQuickLookImageRunnable implements Runnable {

        private final ProductLibraryItem product;
        private final Image quickLookImage;

        public ProductQuickLookImageRunnable(ProductLibraryItem product, Image quickLookImage) {
            this.product = product;
            this.quickLookImage = quickLookImage;
        }

        protected abstract void execute(ProductLibraryItem product, Image quickLookImage);

        @Override
        public void run() {
            execute(this.product, this.quickLookImage);
        }
    }
}
