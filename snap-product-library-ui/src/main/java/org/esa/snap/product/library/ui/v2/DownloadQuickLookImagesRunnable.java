package org.esa.snap.product.library.ui.v2;

import org.apache.http.auth.Credentials;
import org.esa.snap.product.library.ui.v2.table.CustomTable;
import org.esa.snap.product.library.v2.IThread;
import org.esa.snap.product.library.v2.ProductLibraryItem;
import org.esa.snap.product.library.v2.SciHubDownloader;
import org.esa.snap.ui.loading.ILoadingIndicator;

import javax.swing.SwingUtilities;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 13/8/2019.
 */
public class DownloadQuickLookImagesRunnable implements Runnable {

    private static final Logger logger = Logger.getLogger(DownloadQuickLookImagesRunnable.class.getName());

    private final int threadId;
    private final ILoadingIndicator loadingIndicator;
    private final List<ProductLibraryItem> productList;
    private final Credentials credentials;
    private final CustomTable<ProductLibraryItem> productsTable;

    public DownloadQuickLookImagesRunnable(ILoadingIndicator loadingIndicator, int threadId, List<ProductLibraryItem> productList,
                                           Credentials credentials, CustomTable<ProductLibraryItem> productsTable) {

        this.loadingIndicator = loadingIndicator;
        this.threadId = threadId;
        this.productList = productList;
        this.credentials = credentials;
        this.productsTable = productsTable;
    }

    @Override
    public final void run() {
        try {
            IThread thread = new IThread() {
                @Override
                public boolean isRunning() {
                    return DownloadQuickLookImagesRunnable.this.isRunning();
                }
            };

            for (int i=0; i<this.productList.size(); i++) {
                if (!isRunning()) {
                    return;
                }

                ProductLibraryItem product = this.productList.get(i);
                BufferedImage quickLookImage = null;
                if (product.getQuickLookLocation() != null) {
                    try {
                        quickLookImage = SciHubDownloader.downloadQuickLookImage(product.getQuickLookLocation(), this.credentials, thread);
                        if (!isRunning()) {
                            return;
                        }
                    } catch (Exception exception) {
                        logger.log(Level.SEVERE, "Failed to download the product quick look image from url '" + product.getQuickLookLocation() + "'.", exception);
                    }
                }
                notifyDownloadedQuickLookImageLater(product, quickLookImage);
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Failed to download the quick look images.", exception);
        }
    }

    private boolean isRunning() {
        return this.loadingIndicator.isRunning(this.threadId);
    }

    private void notifyDownloadedQuickLookImageLater(ProductLibraryItem product, BufferedImage quickLookImage) {
        Runnable runnable = new ProductQuickLookImageRunnable(product, quickLookImage) {
            @Override
            protected void execute(ProductLibraryItem productItem, BufferedImage quickLookImageItem) {
                if (isRunning()) {
                    onDownloadedQuickLookImage(productItem, quickLookImageItem);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void onDownloadedQuickLookImage(ProductLibraryItem product, BufferedImage quickLookImage) {
        ProductsTableModel productsTableModel = (ProductsTableModel)this.productsTable.getModel();
        int[] selectedRows = this.productsTable.getSelectedRows();
        productsTableModel.setProductQuickLookImage(product, quickLookImage);
        productsTableModel.fireTableDataChanged();
        // selected again the rows
        for (int i=0; i<selectedRows.length; i++) {
            this.productsTable.setRowSelectionInterval(selectedRows[i], selectedRows[i]);
        }
    }

    private static abstract class ProductQuickLookImageRunnable implements Runnable {

        private final ProductLibraryItem product;
        private final BufferedImage quickLookImage;

        public ProductQuickLookImageRunnable(ProductLibraryItem product, BufferedImage quickLookImage) {
            this.product = product;
            this.quickLookImage = quickLookImage;
        }

        protected abstract void execute(ProductLibraryItem product, BufferedImage quickLookImage);

        @Override
        public void run() {
            execute(this.product, this.quickLookImage);
        }
    }
}
