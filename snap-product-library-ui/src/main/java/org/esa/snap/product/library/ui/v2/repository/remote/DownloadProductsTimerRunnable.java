package org.esa.snap.product.library.ui.v2.repository.remote;

import org.esa.snap.product.library.ui.v2.RepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.ProductLibraryDAL;
import org.esa.snap.product.library.v2.database.SaveDownloadedProductData;
import org.esa.snap.product.library.v2.database.SaveProductData;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.listener.ProgressListener;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 19/8/2019.
 */
public class DownloadProductsTimerRunnable extends AbstractProgressTimerRunnable<Void> {

    private static final Logger logger = Logger.getLogger(DownloadProductsTimerRunnable.class.getName());

    private final JComponent parentComponent;
    private final RepositoryProductListPanel repositoryProductListPanel;
    private final DownloadRemoteProductsQueue downloadRemoteProductsQueue;

    private RemoteProductDownloader currentRemoteProductDownloader;

    public DownloadProductsTimerRunnable(ProgressBarHelper progressPanel, int threadId, DownloadRemoteProductsQueue downloadRemoteProductsQueue,
                                         RepositoryProductListPanel repositoryProductListPanel, JComponent parentComponent) {

        super(progressPanel, threadId, 500);

        this.parentComponent = parentComponent;
        this.downloadRemoteProductsQueue = downloadRemoteProductsQueue;
        this.repositoryProductListPanel = repositoryProductListPanel;
    }

    @Override
    protected Void execute() throws Exception {
        RemoteProductDownloader remoteProductDownloader;
        synchronized (this.downloadRemoteProductsQueue) {
            // get the product to download
            remoteProductDownloader = this.downloadRemoteProductsQueue.peek();
        }
        while (remoteProductDownloader != null && isRunning()) {
            try {
                updateProgressBarDownloadedProductsLater();

                downloadAndSaveProduct(remoteProductDownloader);
            } catch (java.lang.InterruptedException exception) {
                updateStopDownloadingProductLater(remoteProductDownloader.getProductToDownload());
                throw exception;
            } catch (java.lang.Exception exception) {
                updateFailedDownloadingProductLater(remoteProductDownloader.getProductToDownload());
                logger.log(Level.SEVERE, "Failed to download the remote product '" + remoteProductDownloader.getProductToDownload().getName() + "'.", exception);
            } finally {
                synchronized (this.downloadRemoteProductsQueue) {
                    // remove the downloaded product from the queue
                    RemoteProductDownloader removedProduct = this.downloadRemoteProductsQueue.pop();
                    if (removedProduct != null && removedProduct != remoteProductDownloader) {
                        throw new IllegalStateException("The removed product from the queue does not match with the downloaded product.");
                    }
                    // get the product to download
                    remoteProductDownloader = this.downloadRemoteProductsQueue.peek();
                }

                updateProgressBarDownloadedProductsLater();
            }
        }
        return null;
    }

    private void downloadAndSaveProduct(RemoteProductDownloader remoteProductDownloader) throws IOException, SQLException, InterruptedException {
        RemoteProductProgressListener progressListener = new RemoteProductProgressListener(remoteProductDownloader.getProductToDownload()) {
            @Override
            public void notifyProgress(short progressPercent) {
                updateDownloadedProgressPercentLater(getProductToDownload(), progressPercent);
            }
        };

        updateDownloadedProgressPercentLater(progressListener.getProductToDownload(), (short)0); // 0%

        // download the product from the remote repository
        synchronized (this) {
            this.currentRemoteProductDownloader = remoteProductDownloader;
        }
        Path productPath = remoteProductDownloader.download(progressListener);
        synchronized (this) {
            this.currentRemoteProductDownloader = null; // reset
        }

        SaveDownloadedProductData saveProductData = ProductLibraryDAL.saveProduct(remoteProductDownloader.getProductToDownload(), productPath,
                                                     remoteProductDownloader.getRepositoryId(), remoteProductDownloader.getLocalRepositoryFolderPath());

        // successfully downloaded and saved the product
        updateDownloadedProgressPercentLater(progressListener.getProductToDownload(), (short)100); // 100%

        updateFinishSavingProductDataLater(saveProductData);
    }

    @Override
    public void stopRunning() {
        super.stopRunning();

        synchronized (this) {
            if (this.currentRemoteProductDownloader != null) {
                this.currentRemoteProductDownloader.cancel();
            }
        }
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to download the products.";
    }

    @Override
    protected void onFailed(Exception exception) {
        onShowErrorMessageDialog(this.parentComponent, "Failed to download the products.", "Error");
    }

    protected void onFinishSavingProduct(SaveDownloadedProductData saveProductData) {
    }

    public void updateProgressBarDownloadedProductsLater() {
        int downloadedProducts;
        int totalProductsToDownload;
        synchronized (this.downloadRemoteProductsQueue) {
            downloadedProducts = this.downloadRemoteProductsQueue.getDownloadedProductCount();
            totalProductsToDownload = this.downloadRemoteProductsQueue.getTotalPushed();
        }
        String text = Integer.toString(downloadedProducts) + " out of " + Integer.toString(totalProductsToDownload);
        updateProgressBarTextLater(text);
    }

    private void updateFinishSavingProductDataLater(SaveDownloadedProductData saveProductData) {
        GenericRunnable<SaveDownloadedProductData> runnable = new GenericRunnable<SaveDownloadedProductData>(saveProductData) {
            @Override
            protected void execute(SaveDownloadedProductData item) {
                onFinishSavingProduct(item);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void updateDownloadedProgressPercentLater(RepositoryProduct repositoryProduct, short progressPercent) {
        UpdateDownloadedProgressPercentRunnable runnable = new UpdateDownloadedProgressPercentRunnable(repositoryProduct, progressPercent, this.repositoryProductListPanel) {
            @Override
            public void run() {
                if (isCurrentProgressPanelThread()) {
                    super.run();
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void updateStopDownloadingProductLater(RepositoryProduct repositoryProduct) {
        Runnable runnable = new GenericRunnable<RepositoryProduct>(repositoryProduct) {
            @Override
            protected void execute(RepositoryProduct item) {
                if (isCurrentProgressPanelThread()) {
                    repositoryProductListPanel.getProductListPanel().setStopDownloadingProduct(item);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void updateFailedDownloadingProductLater(RepositoryProduct repositoryProduct) {
        Runnable runnable = new GenericRunnable<RepositoryProduct>(repositoryProduct) {
            @Override
            protected void execute(RepositoryProduct item) {
                if (isCurrentProgressPanelThread()) {
                    repositoryProductListPanel.getProductListPanel().setFailedDownloadingProduct(item);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private static abstract class RemoteProductProgressListener implements ProgressListener {

        private final RepositoryProduct productToDownload;

        private RemoteProductProgressListener(RepositoryProduct productToDownload) {
            this.productToDownload = productToDownload;
        }

        RepositoryProduct getProductToDownload() {
            return productToDownload;
        }
    }

    private static class UpdateDownloadedProgressPercentRunnable implements Runnable {

        private final RepositoryProduct productToDownload;
        private final short progressPercent;
        private final RepositoryProductListPanel repositoryProductListPanel;

        private UpdateDownloadedProgressPercentRunnable(RepositoryProduct productToDownload, short progressPercent, RepositoryProductListPanel repositoryProductListPanel) {
            this.productToDownload = productToDownload;
            this.progressPercent = progressPercent;
            this.repositoryProductListPanel = repositoryProductListPanel;
        }

        @Override
        public void run() {
            this.repositoryProductListPanel.getProductListPanel().setProductDownloadPercent(this.productToDownload, this.progressPercent);
        }
    }
}
