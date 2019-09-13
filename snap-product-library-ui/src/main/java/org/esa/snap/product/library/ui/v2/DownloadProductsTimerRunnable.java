package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.repository.DownloadRemoteProductsQueue;
import org.esa.snap.product.library.ui.v2.repository.RemoteProductDownloader;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.ProductLibraryDAL;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.listener.ProgressListener;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.nio.file.Path;

/**
 * Created by jcoravu on 19/8/2019.
 */
public class DownloadProductsTimerRunnable extends AbstractProgressTimerRunnable<Void> {

    private final JComponent parentComponent;
    private final RemoteRepositoryProductListPanel repositoryProductListPanel;
    private final DownloadRemoteProductsQueue downloadRemoteProductsQueue;

    private RemoteProductDownloader currentRemoteProductDownloader;

    public DownloadProductsTimerRunnable(ProgressBarHelper progressPanel, int threadId, DownloadRemoteProductsQueue downloadRemoteProductsQueue,
                                         RemoteRepositoryProductListPanel repositoryProductListPanel, JComponent parentComponent) {

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
        while (remoteProductDownloader != null) {
            // download the product from the remote repository
            synchronized (this) {
                this.currentRemoteProductDownloader = remoteProductDownloader;
            }

            if (isRunning()) {
                updateDownloadedProgressPercentLater();

                RemoteProductProgressListener progressListener = new RemoteProductProgressListener(remoteProductDownloader.getProductToDownload()) {
                    @Override
                    public void notifyProgress(short progressPercent) {
                        updateDownloadedProgressPercentLater(getProductToDownload(), progressPercent);
                    }
                };
                updateDownloadedProgressPercentLater(progressListener.getProductToDownload(), (short)0);
                Path productFolderPath = remoteProductDownloader.download(progressListener);

                synchronized (this) {
                    this.currentRemoteProductDownloader = null; // reset
                }

                ProductLibraryDAL.saveProduct(remoteProductDownloader.getProductToDownload(), productFolderPath, null, remoteProductDownloader.getLocalRepositoryFolderPath());

                // successfully downloaded the product
                updateDownloadedProgressPercentLater(progressListener.getProductToDownload(), (short)100);

                synchronized (this.downloadRemoteProductsQueue) {
                    // remove the downloaded product from the queue
                    RemoteProductDownloader removedProduct = this.downloadRemoteProductsQueue.pop();
                    if (removedProduct != remoteProductDownloader) {
                        throw new IllegalStateException("The removed product from the queue does not match with the downloaded product.");
                    }
                    // get the product to download
                    remoteProductDownloader = this.downloadRemoteProductsQueue.peek();
                }

                updateDownloadedProgressPercentLater();
            }
        }
        return null;
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

    public void updateDownloadedProgressPercentLater() {
        int downloadedProducts;
        int totalProductsToDownload;
        synchronized (this.downloadRemoteProductsQueue) {
            downloadedProducts = this.downloadRemoteProductsQueue.getDownloadedProductCount();
            totalProductsToDownload = this.downloadRemoteProductsQueue.getTotalPushed();
        }
        String text = Integer.toString(downloadedProducts) + " out of " + Integer.toString(totalProductsToDownload);
        updateProgressBarTextLater(text);
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
        private final RemoteRepositoryProductListPanel repositoryProductListPanel;

        private UpdateDownloadedProgressPercentRunnable(RepositoryProduct productToDownload, short progressPercent, RemoteRepositoryProductListPanel repositoryProductListPanel) {
            this.productToDownload = productToDownload;
            this.progressPercent = progressPercent;
            this.repositoryProductListPanel = repositoryProductListPanel;
        }

        @Override
        public void run() {
            this.repositoryProductListPanel.setProductDownloadPercent(this.productToDownload, this.progressPercent);
        }
    }
}
