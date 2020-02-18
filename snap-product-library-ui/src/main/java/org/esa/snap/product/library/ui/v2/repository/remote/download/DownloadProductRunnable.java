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
        byte downloadStatus = DownloadProgressStatus.FAILED_DOWNLOADING;
        Path productPath = null;
        try {
            startRunning();

            if (isFinished()) {
                return; // nothing to return
            }

            productPath = downloadProduct();

            if (isFinished()) {
                return; // nothing to return
            }

            saveProductData = this.allLocalFolderProductsRepository.saveProduct(this.remoteProductDownloader.getProductToDownload(), productPath,
                                                                                this.remoteProductDownloader.getRepositoryName(),
                                                                                this.remoteProductDownloader.getLocalRepositoryFolderPath());

            downloadStatus = DownloadProgressStatus.SAVED;
        } catch (java.lang.InterruptedException exception) {
            downloadStatus = DownloadProgressStatus.CANCEL_DOWNLOADING;
            RepositoryProduct repositoryProduct = this.remoteProductDownloader.getProductToDownload();
            logger.log(Level.WARNING, "Stop downloading the product: name '" + repositoryProduct.getName()+"', mission '" + repositoryProduct.getMission() + "', remote pository '" + repositoryProduct.getRepositoryName() + "'.");
        } catch (IOException exception) {
            downloadStatus = DownloadProgressStatus.FAILED_DOWNLOADING;
            if (org.apache.commons.lang.StringUtils.containsIgnoreCase(exception.getMessage(), "is not online")) {
                downloadStatus = DownloadProgressStatus.NOT_AVAILABLE; // the product to download is not online
            }
            logger.log(Level.SEVERE, "Failed to download the remote product '" + this.remoteProductDownloader.getProductToDownload().getName() + "'.", exception);
        } catch (Exception exception) {
            downloadStatus = DownloadProgressStatus.FAILED_DOWNLOADING;
            logger.log(Level.SEVERE, "Failed to download the remote product '" + this.remoteProductDownloader.getProductToDownload().getName() + "'.", exception);
        } finally {
            finishRunning(saveProductData, downloadStatus, productPath);
        }
    }

    @Override
    public void cancelRunning() {
        super.cancelRunning();

        this.remoteProductDownloader.cancel();
    }

    protected void finishRunning(SaveDownloadedProductData saveProductData, byte downloadStatus, Path productPath) {
        setRunning(false);
    }

    protected void updateDownloadingProgressPercent(RepositoryProduct repositoryProduct, short progressPercent, Path downloadedPath) {
    }

    public RepositoryProduct getProductToDownload() {
        return this.remoteProductDownloader.getProductToDownload();
    }

    private Path downloadProduct() throws Exception {
        RemoteProductProgressListener progressListener = new RemoteProductProgressListener(this.remoteProductDownloader.getProductToDownload()) {
            @Override
            public void notifyProgress(short progressPercent) {
                updateDownloadingProgressPercent(getProductToDownload(), progressPercent, null); // 'null' => no download local path
            }
        };

        this.remoteRepositoriesSemaphore.acquirePermission(this.remoteProductDownloader.getRepositoryName(), this.remoteProductDownloader.getCredentials());
        try {
            if (isFinished()) {
                return null;
            }

            updateDownloadingProgressPercent(progressListener.getProductToDownload(), (short) 0, null); // 0%

            Path productPath = this.remoteProductDownloader.download(progressListener, this.uncompressedDownloadedProducts);

            // successfully downloaded and saved the product
            updateDownloadingProgressPercent(progressListener.getProductToDownload(), (short)100, productPath); // 100%

            return productPath;
        } finally {
            this.remoteRepositoriesSemaphore.releasePermission(this.remoteProductDownloader.getRepositoryName(), this.remoteProductDownloader.getCredentials());
        }
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
