package org.esa.snap.product.library.ui.v2.repository.remote.download;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.product.library.ui.v2.repository.remote.DownloadProgressStatus;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductDownloader;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteRepositoriesSemaphore;
import org.esa.snap.product.library.v2.database.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.SaveProductData;
import org.esa.snap.remote.products.repository.RemoteMission;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.listener.ProgressListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 16/10/2019.
 */
public abstract class DownloadProductRunnable extends AbstractBackgroundDownloadRunnable implements ProgressListener {

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
        SaveProductData saveProductData = null;
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

            //TODO Jean temporary method until the Landsat8 product reader will be changed to read the product from a folder
            Path productPathToOpen = RemoteProductsRepositoryProvider.prepareProductPathToOpen(productPath, this.remoteProductDownloader.getProductToDownload());
            File productFileToOpen = productPathToOpen.toFile();

            //TODO Jean old code to get the product path to open
            //File productFileToOpen = productPath.toFile();

            Product product = ProductIO.readProduct(productFileToOpen);
            try {
                saveProductData = this.allLocalFolderProductsRepository.saveRemoteProduct(this.remoteProductDownloader.getProductToDownload(), productPath,
                                                                                          this.remoteProductDownloader.getRepositoryName(),
                                                                                          this.remoteProductDownloader.getLocalRepositoryFolderPath(), product);
            } finally {
                if (product != null) {
                    product.dispose();
                }
            }

            downloadStatus = DownloadProgressStatus.SAVED;
        } catch (java.lang.InterruptedException exception) {
            downloadStatus = DownloadProgressStatus.CANCEL_DOWNLOADING;
            RepositoryProduct repositoryProduct = this.remoteProductDownloader.getProductToDownload();
            RemoteMission remoteMission = repositoryProduct.getRemoteMission();
            logger.log(Level.WARNING, "Stop downloading the product: name '" + repositoryProduct.getName()+"', mission '" + remoteMission.getName() + "', remote pository '" + remoteMission.getRepositoryName() + "'.");
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

    @Override
    public final void notifyProgress(short progressPercent) {
        updateDownloadingProgressPercent(progressPercent, null); // 'null' => no download local path
    }

    protected void finishRunning(SaveProductData saveProductData, byte downloadStatus, Path productPath) {
        setRunning(false);
    }

    protected void updateDownloadingProgressPercent(short progressPercent, Path downloadedPath) {
    }

    public RepositoryProduct getProductToDownload() {
        return this.remoteProductDownloader.getProductToDownload();
    }

    private Path downloadProduct() throws Exception {
        this.remoteRepositoriesSemaphore.acquirePermission(this.remoteProductDownloader.getRepositoryName(), this.remoteProductDownloader.getCredentials());
        try {
            if (isFinished()) {
                return null;
            }

            updateDownloadingProgressPercent((short) 0, null); // 0%

            Path productPath = this.remoteProductDownloader.download(this, this.uncompressedDownloadedProducts);

            // successfully downloaded and saved the product
            updateDownloadingProgressPercent((short)100, productPath); // 100%

            return productPath;
        } finally {
            this.remoteRepositoriesSemaphore.releasePermission(this.remoteProductDownloader.getRepositoryName(), this.remoteProductDownloader.getCredentials());
        }
    }
}
