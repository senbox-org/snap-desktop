package org.esa.snap.product.library.ui.v2.repository.remote.download;

import org.apache.http.auth.Credentials;
import org.esa.snap.engine_utilities.util.Pair;
import org.esa.snap.engine_utilities.util.ThreadNamePoolExecutor;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.DownloadProgressStatus;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductDownloader;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteRepositoriesSemaphore;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelperImpl;
import org.esa.snap.product.library.v2.database.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.SaveDownloadedProductData;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.*;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 17/10/2019.
 */
public class DownloadRemoteProductsHelper implements DownloadingProductProgressCallback {

    private static final Logger logger = Logger.getLogger(DownloadRemoteProductsHelper.class.getName());

    public static final boolean UNCOMPRESSED_DOWNLOADED_PRODUCTS = false;

    private final ProgressBarHelperImpl progressPanel;
    private final RemoteRepositoriesSemaphore remoteRepositoriesSemaphore;
    private final DownloadProductListener downloadProductListener;

    private ThreadNamePoolExecutor threadPoolExecutor;
    private Set<AbstractBackgroundDownloadRunnable> runningTasks;
    private Map<RepositoryProduct, DownloadProgressStatus> downloadingProductsProgressValue;
    private int threadId;
    private boolean uncompressedDownloadedProducts;

    public DownloadRemoteProductsHelper(ProgressBarHelperImpl progressPanel, RemoteRepositoriesSemaphore remoteRepositoriesSemaphore,
                                        DownloadProductListener downloadProductListener) {

        this.progressPanel = progressPanel;
        this.remoteRepositoriesSemaphore = remoteRepositoriesSemaphore;
        this.downloadProductListener = downloadProductListener;
        this.uncompressedDownloadedProducts = UNCOMPRESSED_DOWNLOADED_PRODUCTS;
    }

    @Override
    public DownloadProgressStatus getDownloadingProductsProgressValue(RepositoryProduct repositoryProduct) {
        if (repositoryProduct == null) {
            throw new NullPointerException("The repository product is null.");
        }
        return (this.downloadingProductsProgressValue == null) ? null : this.downloadingProductsProgressValue.get(repositoryProduct);
    }

    public void setUncompressedDownloadedProducts(boolean uncompressedDownloadedProducts) {
        this.uncompressedDownloadedProducts = uncompressedDownloadedProducts;
    }

    public RemoteRepositoriesSemaphore getRemoteRepositoriesSemaphore() {
        return remoteRepositoriesSemaphore;
    }

    public void downloadProductsQuickLookImageAsync(List<RepositoryProduct> productsWithoutQuickLookImage, RemoteProductsRepositoryProvider productsRepositoryProvider,
                                                    Credentials credentials, RepositoryOutputProductListPanel repositoryProductListPanel) {

        createThreadPoolExecutorIfNeeded();

        DownloadProductsQuickLookImageRunnable runnable = new DownloadProductsQuickLookImageRunnable(productsWithoutQuickLookImage, productsRepositoryProvider,
                                                                                                     credentials, this.remoteRepositoriesSemaphore, repositoryProductListPanel) {

            @Override
            protected void finishRunning() {
                super.finishRunning();
                finishRunningDownloadProductsQuickLookImageThreadLater(this);
            }
        };
        this.runningTasks.add(runnable);
        this.threadPoolExecutor.execute(runnable); // start the thread
    }

    public void downloadProductsAsync(RepositoryProduct[] productsToDownload, RemoteProductsRepositoryProvider remoteProductsRepositoryProvider,
                                      Path localRepositoryFolderPath, Credentials credentials, AllLocalFolderProductsRepository allLocalFolderProductsRepository) {

        createThreadPoolExecutorIfNeeded();

        for (int i=0; i<productsToDownload.length; i++) {
            DownloadProgressStatus downloadProgressStatus = this.downloadingProductsProgressValue.get(productsToDownload[i]);
            if (downloadProgressStatus == null || downloadProgressStatus.isCancelDownloading()) {
                downloadProgressStatus = new DownloadProgressStatus();
                this.downloadingProductsProgressValue.put(productsToDownload[i], downloadProgressStatus);

                RemoteProductDownloader remoteProductDownloader = new RemoteProductDownloader(remoteProductsRepositoryProvider, productsToDownload[i], localRepositoryFolderPath, credentials);

                DownloadProductRunnable runnable = new DownloadProductRunnable(remoteProductDownloader, this.remoteRepositoriesSemaphore, allLocalFolderProductsRepository, this.uncompressedDownloadedProducts) {
                    @Override
                    protected void startRunning() {
                        super.startRunning();
                        startRunningDownloadProductThreadLater();
                    }

                    @Override
                    public void cancelRunning() {
                        super.cancelRunning();
                        cancelRunningDownloadProductThreadLater(this);
                    }

                    @Override
                    protected void updateDownloadingProgressPercent(RepositoryProduct repositoryProduct, short progressPercent, Path downloadedPath) {
                        updateDownloadingProgressPercentLater(repositoryProduct, progressPercent, downloadedPath);
                    }

                    @Override
                    protected void finishRunning(SaveDownloadedProductData saveProductData, byte downloadStatus, Path productPath) {
                        super.finishRunning(saveProductData, downloadStatus, productPath);
                        finishRunningDownloadProductThreadLater(this, saveProductData, downloadStatus, productPath);
                    }
                };
                this.runningTasks.add(runnable);
                this.threadPoolExecutor.execute(runnable); // start the thread
            }
        }

        updateProgressBarDownloadedProducts();
    }

    public boolean isRunning() {
        return (this.threadPoolExecutor != null);
    }

    public void cancelDownloadingProducts() {
        if (this.runningTasks != null) {

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Stop downloading the products.");
            }

            for (AbstractBackgroundDownloadRunnable runnable : this.runningTasks) {
                if (runnable instanceof DownloadProductRunnable) {
                    runnable.cancelRunning();
                }
            }

            updateProgressBarDownloadedProducts();
        }
    }

    public void cancelDownloadingProductsQuickLookImage() {
        if (this.runningTasks != null) {

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Stop downloading the products quick look image.");
            }

            for (AbstractBackgroundDownloadRunnable runnable : this.runningTasks) {
                if (runnable instanceof DownloadProductsQuickLookImageRunnable) {
                    runnable.cancelRunning();
                }
            }
        }
    }

    public List<Pair<DownloadProductRunnable, DownloadProgressStatus>> findDownloadingProducts() {
        List<Pair<DownloadProductRunnable, DownloadProgressStatus>> downloadingProductRunnables;
        if (this.runningTasks != null && this.runningTasks.size() > 0) {
            downloadingProductRunnables = new ArrayList<>();
            for (AbstractBackgroundDownloadRunnable runnable : this.runningTasks) {
                if (runnable instanceof DownloadProductRunnable && !runnable.isFinished()) {
                    DownloadProductRunnable downloadProductRunnable = (DownloadProductRunnable)runnable;
                    DownloadProgressStatus downloadProgressStatus = this.downloadingProductsProgressValue.get(downloadProductRunnable.getProductToDownload());
                    if (downloadProgressStatus != null) {
                        Pair<DownloadProductRunnable, DownloadProgressStatus> pair = new Pair<>(downloadProductRunnable, downloadProgressStatus);
                        downloadingProductRunnables.add(pair);
                    }
                }
            }
        } else {
            downloadingProductRunnables = Collections.emptyList();
        }
        return downloadingProductRunnables;
    }

    private void createThreadPoolExecutorIfNeeded() {
        if (this.threadPoolExecutor == null) {
            this.runningTasks = new HashSet<>();
            this.downloadingProductsProgressValue = new HashMap<>();
            this.threadId = this.progressPanel.incrementAndGetCurrentThreadId();
            int maximumThreadCount = Runtime.getRuntime().availableProcessors() - 1;
            this.threadPoolExecutor = new ThreadNamePoolExecutor("product-library", maximumThreadCount);
        }
    }

    private void startRunningDownloadProductThreadLater() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                onStartRunningDownloadProductThread();
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void cancelRunningDownloadProductThreadLater(DownloadProductRunnable parentRunnableItem) {
        GenericRunnable<DownloadProductRunnable> runnable = new GenericRunnable<DownloadProductRunnable>(parentRunnableItem) {
            @Override
            protected void execute(DownloadProductRunnable item) {
                onCancelRunningDownloadProductThread(item);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void finishRunningDownloadProductThreadLater(DownloadProductRunnable parentRunnableItem, SaveDownloadedProductData saveProductDataItem,
                                                         byte downloadStatus, Path productPath) {

        FinishDownloadingProductStatusRunnable runnable = new FinishDownloadingProductStatusRunnable(parentRunnableItem, saveProductDataItem, downloadStatus, productPath) {
            @Override
            public void run() {
                onFinishRunningDownloadProductThread(this.downloadProductRunnable, this.saveDownloadedProductData, this.saveDownloadStatus, this.downloadedProductPath);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void finishRunningDownloadProductsQuickLookImageThreadLater(DownloadProductsQuickLookImageRunnable parentRunnableItem) {
        GenericRunnable<DownloadProductsQuickLookImageRunnable> runnable = new GenericRunnable<DownloadProductsQuickLookImageRunnable>(parentRunnableItem) {
            @Override
            protected void execute(DownloadProductsQuickLookImageRunnable item) {
                onFinishRunningDownloadProductQuickLookImageThread(item);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private boolean hasDownloadingProducts() {
        for (AbstractBackgroundDownloadRunnable runnable : this.runningTasks) {
            if (runnable instanceof DownloadProductRunnable) {
                return true;
            }
        }
        return false;
    }

    private DownloadProgressStatus resetDownloadProgressStatus(RepositoryProduct repositoryProduct) {
        return this.downloadingProductsProgressValue.remove(repositoryProduct);
    }

    private void onStartRunningDownloadProductThread() {
        if (hasDownloadingProducts()) {
            if (!this.progressPanel.isCurrentThread(this.threadId)) {
                this.threadId = this.progressPanel.incrementAndGetCurrentThreadId();
            }
            this.progressPanel.showProgressPanel(this.threadId); // show the progress panel
        }
    }

    private void onCancelRunningDownloadProductThread(DownloadProductRunnable parentRunnableItem) {
        if (this.runningTasks.contains(parentRunnableItem)) {
            // reset the download progress item from the map
            DownloadProgressStatus downloadProgressStatus = resetDownloadProgressStatus(parentRunnableItem.getProductToDownload());
            if (downloadProgressStatus != null) {
                if (downloadProgressStatus.isPendingDownload()) {
                    downloadProgressStatus = null; // reset the item
                } else {
                    downloadProgressStatus.setStatus(DownloadProgressStatus.CANCEL_DOWNLOADING);
                }
            }

            updateProgressBarDownloadedProducts();

            this.downloadProductListener.onCancelDownloadingProduct(parentRunnableItem, downloadProgressStatus);
        } else {
            throw new IllegalArgumentException("The parent thread parameter is wrong.");
        }
    }

    private void onFinishRunningDownloadProductThread(DownloadProductRunnable parentRunnable, SaveDownloadedProductData saveProductData,
                                                      byte saveDownloadStatus, Path downloadedProductPath) {

        if (this.runningTasks.remove(parentRunnable)) {
            // reset the download progress item from the map
            DownloadProgressStatus downloadProgressStatus = resetDownloadProgressStatus(parentRunnable.getProductToDownload());
            if (downloadProgressStatus != null) {
                downloadProgressStatus.setStatus(saveDownloadStatus);
                downloadProgressStatus.setDownloadedPath(downloadedProductPath);
            }

            updateProgressBarDownloadedProducts();

            boolean hasProductsToDownload = hasDownloadingProducts();

            this.downloadProductListener.onFinishDownloadingProduct(parentRunnable, downloadProgressStatus, saveProductData, hasProductsToDownload);

            if (!hasProductsToDownload) {
                // there are no downloading products and hide the progress panel if visible
                this.progressPanel.hideProgressPanel(this.threadId);
            }

            shutdownThreadPoolIfEmpty();
        } else {
            throw new IllegalArgumentException("The parent thread parameter is wrong.");
        }
    }

    private void onFinishRunningDownloadProductQuickLookImageThread(DownloadProductsQuickLookImageRunnable parentRunnable) {
        if (this.runningTasks.remove(parentRunnable)) {
            shutdownThreadPoolIfEmpty();
        } else {
            throw new IllegalArgumentException("The parent thread parameter is wrong.");
        }
    }

    private void shutdownThreadPoolIfEmpty() {
        if (this.runningTasks.size() == 0) {
            this.threadPoolExecutor.shutdown();
            this.threadPoolExecutor = null; // reset the thread pool
            this.runningTasks = null; // reset the running tasks
            this.downloadingProductsProgressValue = null;
        }
    }

    private void updateProgressBarDownloadedProducts() {
        if (this.progressPanel.isCurrentThread(this.threadId)) {
            int downloadingProductCount = 0;
            for (AbstractBackgroundDownloadRunnable runnable : this.runningTasks) {
                if (runnable instanceof DownloadProductRunnable && !runnable.isFinished()) {
                    downloadingProductCount++;
                }
            }
            int totalProductCountToDownload = this.downloadingProductsProgressValue.size();
            if (downloadingProductCount > totalProductCountToDownload) {
                throw new IllegalStateException("The downloading product count " + downloadingProductCount+" is greater than the total product count to download" + totalProductCountToDownload+".");
            } else {
                int totalDownloadedProductCount = totalProductCountToDownload - downloadingProductCount;
                String text = buildProgressBarDownloadingText(totalDownloadedProductCount, totalProductCountToDownload);
                this.progressPanel.updateProgressBarText(this.threadId, text);
            }
        }
    }

    private void updateDownloadingProgressPercentLater(RepositoryProduct repositoryProduct, short progressPercent, Path downloadedPath) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Update the downloading progress percent " + progressPercent + "% of the product '" + repositoryProduct.getName()+"' using the '" + repositoryProduct.getMission()+"' mission.");
        }

        Runnable runnable = new UpdateDownloadedProgressPercentRunnable(this.downloadProductListener, repositoryProduct, progressPercent, downloadedPath,
                                                                        this.downloadingProductsProgressValue) {
            @Override
            public void run() {
                if (progressPanel.isCurrentThread(threadId)) {
                    super.run();
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private static abstract class FinishDownloadingProductStatusRunnable implements Runnable {

        final DownloadProductRunnable downloadProductRunnable;
        final SaveDownloadedProductData saveDownloadedProductData;
        final byte saveDownloadStatus;
        final Path downloadedProductPath;

        private FinishDownloadingProductStatusRunnable(DownloadProductRunnable parentRunnableItem, SaveDownloadedProductData saveProductDataItem,
                                                       byte downloadStatus, Path productPath) {
            this.downloadProductRunnable = parentRunnableItem;
            this.saveDownloadedProductData = saveProductDataItem;
            this.saveDownloadStatus = downloadStatus;
            this.downloadedProductPath = productPath;
        }
    }

    private static class UpdateDownloadedProgressPercentRunnable implements Runnable {

        private final RepositoryProduct productToDownload;
        private final short progressPercent;
        private final Path downloadedPath;
        private final DownloadProductListener downloadProductListener;
        private final Map<RepositoryProduct, DownloadProgressStatus> downloadingProductsProgressValue;

        private UpdateDownloadedProgressPercentRunnable(DownloadProductListener downloadProductListener, RepositoryProduct productToDownload, short progressPercent,
                                                        Path downloadedPath, Map<RepositoryProduct, DownloadProgressStatus> downloadingProductsProgressValue) {

            this.productToDownload = productToDownload;
            this.downloadProductListener = downloadProductListener;
            this.progressPercent = progressPercent;
            this.downloadedPath = downloadedPath;
            this.downloadingProductsProgressValue = downloadingProductsProgressValue;
        }

        @Override
        public void run() {
            DownloadProgressStatus downloadProgressStatus = this.downloadingProductsProgressValue.get(this.productToDownload);
            if (downloadProgressStatus != null) {
                downloadProgressStatus.setValue(this.progressPercent);
                downloadProgressStatus.setDownloadedPath(this.downloadedPath);
                this.downloadProductListener.onUpdateProductDownloadProgress(this.productToDownload);
            }
        }
    }

    public static String buildProgressBarDownloadingText(int totalDownloaded, int totalProducts) {
        return "Downloading products: " + Integer.toString(totalDownloaded) + " out of " + Integer.toString(totalProducts);
    }
}
