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
import org.esa.snap.ui.loading.PairRunnable;

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
    private Map<AbstractBackgroundDownloadRunnable, Pair<DownloadProgressStatus, Boolean>> runningTasks;
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
        if (this.runningTasks != null && this.runningTasks.size() > 0) {
            for (Map.Entry<AbstractBackgroundDownloadRunnable, Pair<DownloadProgressStatus, Boolean>> entry : this.runningTasks.entrySet()) {
                AbstractBackgroundDownloadRunnable runnable = entry.getKey();
                if (runnable instanceof DownloadProductRunnable) {
                    DownloadProductRunnable downloadProductRunnable = (DownloadProductRunnable)runnable;
                    if (downloadProductRunnable.getProductToDownload() == repositoryProduct) {
                        Pair<DownloadProgressStatus, Boolean> value = entry.getValue();
                        if (value == null) {
                            throw new NullPointerException("The value is null.");
                        }
                        return value.getFirst();
                    }
                }
            }
        }
        return null;
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
        this.runningTasks.put(runnable, null);
        this.threadPoolExecutor.execute(runnable); // start the thread
    }

    public void downloadProductsAsync(RepositoryProduct[] productsToDownload, RemoteProductsRepositoryProvider remoteProductsRepositoryProvider,
                                      Path localRepositoryFolderPath, Credentials credentials, AllLocalFolderProductsRepository allLocalFolderProductsRepository) {

        createThreadPoolExecutorIfNeeded();

        for (int i=0; i<productsToDownload.length; i++) {
            DownloadProgressStatus downloadProgressStatus = getDownloadingProductsProgressValue(productsToDownload[i]);
            if (downloadProgressStatus == null || downloadProgressStatus.isCancelDownloading()) {
                downloadProgressStatus = new DownloadProgressStatus();

                RemoteProductDownloader remoteProductDownloader = new RemoteProductDownloader(remoteProductsRepositoryProvider, productsToDownload[i], localRepositoryFolderPath, credentials);

                DownloadProductRunnable runnable = new DownloadProductRunnable(remoteProductDownloader, this.remoteRepositoriesSemaphore, allLocalFolderProductsRepository, this.uncompressedDownloadedProducts) {
                    @Override
                    protected void startRunning() {
                        super.startRunning();
                        startRunningDownloadProductThreadLater(this);
                    }

                    @Override
                    public void cancelRunning() {
                        super.cancelRunning();
                        cancelRunningDownloadProductThreadLater(this);
                    }

                    @Override
                    protected void updateDownloadingProgressPercent(short progressPercent, Path downloadedPath) {
                        updateDownloadingProgressPercentLater(this, progressPercent, downloadedPath);
                    }

                    @Override
                    protected void finishRunning(SaveDownloadedProductData saveProductData, byte downloadStatus, Path productPath) {
                        super.finishRunning(saveProductData, downloadStatus, productPath);
                        finishRunningDownloadProductThreadLater(this, saveProductData, downloadStatus, productPath);
                    }
                };

                this.runningTasks.put(runnable, new Pair(downloadProgressStatus, true));
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

            for (AbstractBackgroundDownloadRunnable runnable : this.runningTasks.keySet()) {
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

            for (AbstractBackgroundDownloadRunnable runnable : this.runningTasks.keySet()) {
                if (runnable instanceof DownloadProductsQuickLookImageRunnable) {
                    runnable.cancelRunning();
                }
            }
        }
    }

    public List<Pair<DownloadProductRunnable, DownloadProgressStatus>> findDownloadingProducts() {
        List<Pair<DownloadProductRunnable, DownloadProgressStatus>> downloadingProductRunnables;
        if (this.runningTasks != null && this.runningTasks.size() > 0) {
            downloadingProductRunnables = new ArrayList<>(this.runningTasks.size());
            for (Map.Entry<AbstractBackgroundDownloadRunnable, Pair<DownloadProgressStatus, Boolean>> entry : this.runningTasks.entrySet()) {
                AbstractBackgroundDownloadRunnable runnable = entry.getKey();
                if (runnable instanceof DownloadProductRunnable) {
                    Pair<DownloadProgressStatus, Boolean> value = entry.getValue();
                    Pair<DownloadProductRunnable, DownloadProgressStatus> pair = new Pair(runnable, value.getFirst());
                    downloadingProductRunnables.add(pair);
                }
            }
        } else {
            downloadingProductRunnables = Collections.emptyList();
        }
        return downloadingProductRunnables;
    }

    private void startRunningDownloadProductThreadLater(DownloadProductRunnable parentRunnableItem) {
        GenericRunnable<DownloadProductRunnable> runnable = new GenericRunnable<DownloadProductRunnable>(parentRunnableItem) {
            @Override
            protected void execute(DownloadProductRunnable item) {
                onStartRunningDownloadProductThread(item);
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

    private void updateDownloadingProgressPercentLater(DownloadProductRunnable parentRunnableItem, short progressPercentValue, Path downloadedPath) {
        if (logger.isLoggable(Level.FINE)) {
            RepositoryProduct repositoryProduct = parentRunnableItem.getProductToDownload();
            logger.log(Level.FINE, "Update the downloading progress percent " + progressPercentValue + "% of the product '" + repositoryProduct.getName()+"' using the '" + repositoryProduct.getMission()+"' mission.");
        }

        Runnable runnable = new UpdateDownloadingProgressPercentRunnable(parentRunnableItem, progressPercentValue, downloadedPath) {
            @Override
            public void run() {
                if (progressPanel.isCurrentThread(threadId)) {
                    onUpdateDownloadingProgressPercent(this.downloadProductRunnable, this.progressPercent, this.downloadedProductPath);
                }
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
        for (AbstractBackgroundDownloadRunnable runnable : this.runningTasks.keySet()) {
            if (runnable instanceof DownloadProductRunnable && !runnable.isFinished()) {
                return true;
            }
        }
        return false;
    }

    private void onStartRunningDownloadProductThread(DownloadProductRunnable parentRunnableItem) {
        if (this.runningTasks.containsKey(parentRunnableItem)) {
            if (hasDownloadingProducts()) {
                if (!this.progressPanel.isCurrentThread(this.threadId)) {
                    this.threadId = this.progressPanel.incrementAndGetCurrentThreadId();
                }
                this.progressPanel.showProgressPanel(this.threadId, null); // 'null' => do not reset the progress bar message
                updateProgressBarDownloadedProducts();
            }
        } else {
            throw new IllegalArgumentException("The parent thread parameter is wrong.");
        }
    }

    private void onCancelRunningDownloadProductThread(DownloadProductRunnable parentRunnableItem) {
        if (this.runningTasks.containsKey(parentRunnableItem)) {
            DownloadProgressStatus downloadProgressStatus = getDownloadingProductsProgressValue(parentRunnableItem.getProductToDownload());
            if (downloadProgressStatus != null) {
                downloadProgressStatus.setStatus(DownloadProgressStatus.CANCEL_DOWNLOADING);
            }

            updateProgressBarDownloadedProducts();

            this.downloadProductListener.onUpdateProductDownloadProgress(parentRunnableItem.getProductToDownload());
        }
    }

    private void onFinishRunningDownloadProductThread(DownloadProductRunnable parentRunnable, SaveDownloadedProductData saveProductData,
                                                      byte saveDownloadStatus, Path downloadedProductPath) {

        Pair<DownloadProgressStatus, Boolean> value = this.runningTasks.get(parentRunnable);
        if (value == null) {
            throw new NullPointerException("The value is null.");
        } else {
            value.setSecond(false);

            DownloadProgressStatus downloadProgressStatus = value.getFirst();
            downloadProgressStatus.setStatus(saveDownloadStatus);
            downloadProgressStatus.setDownloadedPath(downloadedProductPath);

            updateProgressBarDownloadedProducts();

            boolean hasProductsToDownload = hasDownloadingProducts();

            this.downloadProductListener.onFinishDownloadingProduct(parentRunnable, downloadProgressStatus, saveProductData, hasProductsToDownload);

            if (!hasProductsToDownload) {
                // there are no downloading products and hide the progress panel if visible
                this.progressPanel.hideProgressPanel(this.threadId);
            }

            shutdownThreadPoolIfEmpty();
        }
    }

    private void onFinishRunningDownloadProductQuickLookImageThread(DownloadProductsQuickLookImageRunnable parentRunnable) {
        if (this.runningTasks.containsKey(parentRunnable)) {
            this.runningTasks.remove(parentRunnable);
            shutdownThreadPoolIfEmpty();
        } else {
            throw new IllegalArgumentException("The parent thread parameter is wrong.");
        }
    }

    private void shutdownThreadPoolIfEmpty() {
        boolean canCloseThreadPool = true;
        for (Map.Entry<AbstractBackgroundDownloadRunnable, Pair<DownloadProgressStatus, Boolean>> entry : this.runningTasks.entrySet()) {
            AbstractBackgroundDownloadRunnable runnable = entry.getKey();
            if (runnable instanceof DownloadProductRunnable) {
                Pair<DownloadProgressStatus, Boolean> value = entry.getValue();
                if (value == null) {
                    throw new NullPointerException("The value is null.");
                } else if (value.getSecond().booleanValue() == true) {
                    canCloseThreadPool = false;
                }
            } else {
                canCloseThreadPool = false;
            }
        }
        if (canCloseThreadPool) {
            this.threadPoolExecutor.shutdown();
            this.threadPoolExecutor = null; // reset the thread pool
            this.runningTasks = null; // reset the running tasks
        }
    }

    private void createThreadPoolExecutorIfNeeded() {
        if (this.threadPoolExecutor == null) {
            this.runningTasks = new HashMap<>();
            this.threadId = this.progressPanel.incrementAndGetCurrentThreadId();
            int maximumThreadCount = Math.max(3, Runtime.getRuntime().availableProcessors());
            this.threadPoolExecutor = new ThreadNamePoolExecutor("product-library", maximumThreadCount);
        }
    }

    private void updateProgressBarDownloadedProducts() {
        if (this.progressPanel.isCurrentThread(this.threadId)) {
            int totalProductCountToDownload = 0;
            int totalDownloadedProductCount = 0;
            for (AbstractBackgroundDownloadRunnable runnable : this.runningTasks.keySet()) {
                if (runnable instanceof DownloadProductRunnable) {
                    totalProductCountToDownload++;
                    if (runnable.isFinished()) {
                        totalDownloadedProductCount++;
                    }
                }
            }
            if (totalDownloadedProductCount > totalProductCountToDownload) {
                throw new IllegalStateException("The downloaded product count " + totalDownloadedProductCount+" is greater than the total product count to download" + totalProductCountToDownload+".");
            } else {
                String text = buildProgressBarDownloadingText(totalDownloadedProductCount, totalProductCountToDownload);
                this.progressPanel.updateProgressBarText(this.threadId, text);
            }
        }
    }

    private void onUpdateDownloadingProgressPercent(DownloadProductRunnable parentRunnable, short progressPercent, Path downloadedPath) {
        Pair<DownloadProgressStatus, Boolean> value = this.runningTasks.get(parentRunnable);
        if (value == null) {
            throw new NullPointerException("The value is null.");
        } else {
            DownloadProgressStatus downloadProgressStatus = value.getFirst();
            downloadProgressStatus.setValue(progressPercent);
            downloadProgressStatus.setDownloadedPath(downloadedPath);
            this.downloadProductListener.onUpdateProductDownloadProgress(parentRunnable.getProductToDownload());
        }
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

    private static abstract class UpdateDownloadingProgressPercentRunnable implements Runnable {

        final DownloadProductRunnable downloadProductRunnable;
        final short progressPercent;
        final Path downloadedProductPath;

        private UpdateDownloadingProgressPercentRunnable(DownloadProductRunnable parentRunnableItem, short progressPercent, Path productPath) {
            this.downloadProductRunnable = parentRunnableItem;
            this.progressPercent = progressPercent;
            this.downloadedProductPath = productPath;
        }
    }

    public static String buildProgressBarDownloadingText(int totalDownloaded, int totalProducts) {
        return "Downloading products: " + Integer.toString(totalDownloaded) + " out of " + Integer.toString(totalProducts);
    }
}
