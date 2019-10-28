package org.esa.snap.product.library.ui.v2.repository.remote;

import org.esa.snap.product.library.v2.database.ProductLibraryDAL;
import org.esa.snap.product.library.v2.database.SaveDownloadedProductData;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.listener.ProgressListener;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 16/10/2019.
 */
public class DownloadProductRunnable implements Runnable {

    private static final Logger logger = Logger.getLogger(DownloadProductRunnable.class.getName());

    private final RemoteProductDownloader remoteProductDownloader;
    private final RemoteRepositoriesSemaphore remoteRepositoriesSemaphore;
    private Boolean isRunning;

    public DownloadProductRunnable(RemoteProductDownloader remoteProductDownloader, RemoteRepositoriesSemaphore remoteRepositoriesSemaphore) {
        this.remoteProductDownloader = remoteProductDownloader;
        this.remoteRepositoriesSemaphore = remoteRepositoriesSemaphore;
        this.isRunning = true;
    }

    @Override
    public void run() {
        SaveDownloadedProductData saveProductData = null;
        try {
            startRunningThread();
            if (isRunning()) {
                saveProductData = downloadAndSaveProduct();
            }
        } catch (java.lang.InterruptedException exception) {
            updateDownloadingProductStatus(remoteProductDownloader.getProductToDownload(), DownloadProgressStatus.STOP_DOWNLOADING);
        } catch (IOException exception) {
            byte downloadStatus = DownloadProgressStatus.FAILED_DOWNLOADING;
            if (org.apache.commons.lang.StringUtils.containsIgnoreCase(exception.getMessage(), "is not online")) {
                downloadStatus = DownloadProgressStatus.NOT_AVAILABLE; // the product to download is not online
            }
            updateDownloadingProductStatus(remoteProductDownloader.getProductToDownload(), downloadStatus);
            logger.log(Level.SEVERE, "Failed to download the remote product '" + this.remoteProductDownloader.getProductToDownload().getName() + "'.", exception);
        } catch (Exception exception) {
            updateDownloadingProductStatus(remoteProductDownloader.getProductToDownload(), DownloadProgressStatus.FAILED_DOWNLOADING);
            logger.log(Level.SEVERE, "Failed to download the remote product '" + this.remoteProductDownloader.getProductToDownload().getName() + "'.", exception);
        } finally {
            finishRunningThread(saveProductData);
        }
    }

    protected void updateDownloadingProductStatus(RepositoryProduct repositoryProduct, byte downloadStatus) {
    }

    protected void startRunningThread() {
    }

    protected void finishRunningThread(SaveDownloadedProductData saveProductData) {
    }

    protected void updateDownloadingProgressPercent(RepositoryProduct repositoryProduct, short progressPercent) {
    }

    private boolean isRunning() {
        synchronized (this) {
            return this.isRunning;
        }
    }

    public void stopRunning() {
        synchronized (this) {
            this.isRunning = false;
        }
        this.remoteProductDownloader.cancel();
    }

    private SaveDownloadedProductData downloadAndSaveProduct() throws IOException, SQLException, InterruptedException {
        RemoteProductProgressListener progressListener = new RemoteProductProgressListener(this.remoteProductDownloader.getProductToDownload()) {
            @Override
            public void notifyProgress(short progressPercent) {
                updateDownloadingProgressPercent(getProductToDownload(), progressPercent);
            }
        };

        Path productPath = null;
        this.remoteRepositoriesSemaphore.acquirePermission(this.remoteProductDownloader.getRepositoryId(), this.remoteProductDownloader.getCredentials());
        try {
            if (!isRunning()) {
                return null;
            }

            updateDownloadingProgressPercent(progressListener.getProductToDownload(), (short) 0); // 0%

            productPath = this.remoteProductDownloader.download(progressListener);

        } finally {
            this.remoteRepositoriesSemaphore.releasePermission(this.remoteProductDownloader.getRepositoryId(), this.remoteProductDownloader.getCredentials());
        }

        SaveDownloadedProductData saveProductData = ProductLibraryDAL.saveProduct(this.remoteProductDownloader.getProductToDownload(), productPath,
                                                                                  this.remoteProductDownloader.getRepositoryId(),
                                                                                  this.remoteProductDownloader.getLocalRepositoryFolderPath());

        // successfully downloaded and saved the product
        updateDownloadingProgressPercent(progressListener.getProductToDownload(), (short)100); // 100%

        return saveProductData;
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
}
