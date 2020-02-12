package org.esa.snap.product.library.ui.v2.repository.remote.download;

import org.esa.snap.product.library.ui.v2.repository.remote.DownloadProgressStatus;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductDownloader;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteRepositoriesSemaphore;
import org.esa.snap.product.library.v2.database.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.SaveDownloadedProductData;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.listener.ProgressListener;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 16/10/2019.
 */
public class DownloadProductRunnable extends AbstractBackgroundDownloadRunnable {

    private static final Logger logger = Logger.getLogger(DownloadProductRunnable.class.getName());

    private final RemoteProductDownloader remoteProductDownloader;
    private final RemoteRepositoriesSemaphore remoteRepositoriesSemaphore;
    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;
    private final boolean uncompressedDownloadedProducts;

    public DownloadProductRunnable(RemoteProductDownloader remoteProductDownloader, RemoteRepositoriesSemaphore remoteRepositoriesSemaphore,
                                   AllLocalFolderProductsRepository allLocalFolderProductsRepository, boolean uncompressedDownloadedProducts) {
        super();

        this.remoteProductDownloader = remoteProductDownloader;
        this.remoteRepositoriesSemaphore = remoteRepositoriesSemaphore;
        this.allLocalFolderProductsRepository = allLocalFolderProductsRepository;
        this.uncompressedDownloadedProducts = uncompressedDownloadedProducts;
    }

    @Override
    public final void run() {
        SaveDownloadedProductData saveProductData = null;
        try {
            startRunning();

            if (isFinished()) {
                return; // nothing to return
            }

            saveProductData = downloadAndSaveProduct();
        } catch (java.lang.InterruptedException exception) {
            RepositoryProduct repositoryProduct = this.remoteProductDownloader.getProductToDownload();
            logger.log(Level.WARNING, "Stop downloading the product: name '" + repositoryProduct.getName()+"', mission '" + repositoryProduct.getMission() + "'.");
            updateDownloadingProductStatus(repositoryProduct, DownloadProgressStatus.STOP_DOWNLOADING);
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
            finishRunning(saveProductData);
        }
    }

    @Override
    public void cancelRunning() {
        super.cancelRunning();

        this.remoteProductDownloader.cancel();
    }

    protected void updateDownloadingProductStatus(RepositoryProduct repositoryProduct, byte downloadStatus) {
    }

    protected void finishRunning(SaveDownloadedProductData saveProductData) {
        setRunning(false);
    }

    protected void updateDownloadingProgressPercent(RepositoryProduct repositoryProduct, short progressPercent, Path downloadedPath) {
    }

    public RepositoryProduct getProductToDownload() {
        return this.remoteProductDownloader.getProductToDownload();
    }

    private SaveDownloadedProductData downloadAndSaveProduct() throws Exception {
        RemoteProductProgressListener progressListener = new RemoteProductProgressListener(this.remoteProductDownloader.getProductToDownload()) {
            @Override
            public void notifyProgress(short progressPercent) {
                updateDownloadingProgressPercent(getProductToDownload(), progressPercent, null); // 'null' => no download local path
            }
        };

        Path productPath = null;
        this.remoteRepositoriesSemaphore.acquirePermission(this.remoteProductDownloader.getRepositoryName(), this.remoteProductDownloader.getCredentials());
        try {
            if (isFinished()) {
                return null;
            }

            updateDownloadingProgressPercent(progressListener.getProductToDownload(), (short) 0, null); // 0%

            productPath = this.remoteProductDownloader.download(progressListener, this.uncompressedDownloadedProducts);
        } finally {
            this.remoteRepositoriesSemaphore.releasePermission(this.remoteProductDownloader.getRepositoryName(), this.remoteProductDownloader.getCredentials());
        }

        if (isFinished()) {
            return null;
        }

        SaveDownloadedProductData saveProductData = this.allLocalFolderProductsRepository.saveProduct(this.remoteProductDownloader.getProductToDownload(), productPath,
                                                                                  this.remoteProductDownloader.getRepositoryName(),
                                                                                  this.remoteProductDownloader.getLocalRepositoryFolderPath());

        // successfully downloaded and saved the product
        updateDownloadingProgressPercent(progressListener.getProductToDownload(), (short)100, productPath); // 100%

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
