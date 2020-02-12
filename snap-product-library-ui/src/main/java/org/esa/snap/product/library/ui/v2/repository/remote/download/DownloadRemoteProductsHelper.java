package org.esa.snap.product.library.ui.v2.repository.remote.download;

import org.apache.http.auth.Credentials;
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

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 17/10/2019.
 */
public class DownloadRemoteProductsHelper {

    private static final Logger logger = Logger.getLogger(DownloadRemoteProductsHelper.class.getName());

    public static final boolean UNCOMPRESSED_DOWNLOADED_PRODUCTS = false;

    private final ProgressBarHelperImpl progressPanel;
    private final RemoteRepositoriesSemaphore remoteRepositoriesSemaphore;
    private final DownloadProductListener downloadProductListener;

    private ThreadNamePoolExecutor threadPoolExecutor;
    private Set<AbstractBackgroundDownloadRunnable> runningTasks;
    private int threadId;
    private int totalDownloadingProducts;
    private int totalDownloadedProducts;
    private boolean uncompressedDownloadedProducts;

    public DownloadRemoteProductsHelper(ProgressBarHelperImpl progressPanel, RemoteRepositoriesSemaphore remoteRepositoriesSemaphore,
                                        DownloadProductListener downloadProductListener) {

        this.progressPanel = progressPanel;
        this.remoteRepositoriesSemaphore = remoteRepositoriesSemaphore;
        this.downloadProductListener = downloadProductListener;
        this.uncompressedDownloadedProducts = UNCOMPRESSED_DOWNLOADED_PRODUCTS;
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

    public void downloadProductsAsync(RemoteProductDownloader[] remoteProductDownloaders, AllLocalFolderProductsRepository allLocalFolderProductsRepository) {
        createThreadPoolExecutorIfNeeded();

        for (int i=0; i<remoteProductDownloaders.length; i++) {
            DownloadProductRunnable runnable = new DownloadProductRunnable(remoteProductDownloaders[i], this.remoteRepositoriesSemaphore,
                                                                           allLocalFolderProductsRepository, this.uncompressedDownloadedProducts) {
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
                protected void updateDownloadingProductStatus(RepositoryProduct repositoryProduct, byte downloadStatus) {
                    updateDownloadingProductStatusLater(repositoryProduct, downloadStatus);
                }

                @Override
                protected void updateDownloadingProgressPercent(RepositoryProduct repositoryProduct, short progressPercent, Path downloadedPath) {
                    updateDownloadingProgressPercentLater(repositoryProduct, progressPercent, downloadedPath);
                }

                @Override
                protected void finishRunning(SaveDownloadedProductData saveProductData) {
                    super.finishRunning(saveProductData);
                    finishRunningDownloadProductThreadLater(this, saveProductData);
                }
            };
            this.runningTasks.add(runnable);
            this.threadPoolExecutor.execute(runnable); // start the thread
            this.totalDownloadingProducts++;
        }

        onUpdateProgressBarDownloadedProducts();
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

    public List<DownloadProductRunnable> findDownloadingProducts() {
        List<DownloadProductRunnable> downloadingProductRunnables;
        if (this.runningTasks != null && this.runningTasks.size() > 0) {
            downloadingProductRunnables = new ArrayList<>();
            for (AbstractBackgroundDownloadRunnable runnable : this.runningTasks) {
                if (runnable instanceof DownloadProductRunnable) {
                    downloadingProductRunnables.add((DownloadProductRunnable)runnable);
                }
            }
        } else {
            downloadingProductRunnables = Collections.emptyList();
        }
        return downloadingProductRunnables;
    }

    private void createThreadPoolExecutorIfNeeded() {
        if (this.threadPoolExecutor == null) {
            this.totalDownloadingProducts = 0;
            this.totalDownloadedProducts = 0;
            this.runningTasks = new HashSet<>();
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
        Runnable runnable = new PairRunnable<DownloadProductRunnable, Void>(parentRunnableItem, null) {
            @Override
            public void run() {
                onCancelRunningDownloadProductThread(this.first);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void onCancelRunningDownloadProductThread(DownloadProductRunnable parentRunnableItem) {
        this.downloadProductListener.onCancelDownloadingProduct(parentRunnableItem);
    }

    private void finishRunningDownloadProductThreadLater(DownloadProductRunnable parentRunnableItem, SaveDownloadedProductData saveProductDataItem) {
        Runnable runnable = new PairRunnable<DownloadProductRunnable, SaveDownloadedProductData>(parentRunnableItem, saveProductDataItem) {
            @Override
            public void run() {
                onFinishRunningDownloadProductThread(this.first, this.second);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void finishRunningDownloadProductsQuickLookImageThreadLater(DownloadProductsQuickLookImageRunnable parentRunnableItem) {
        Runnable runnable = new PairRunnable<DownloadProductsQuickLookImageRunnable, Void>(parentRunnableItem, null) {
            @Override
            public void run() {
                onFinishRunningDownloadProductQuickLookImageThread(this.first);
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

    private void onStartRunningDownloadProductThread() {
        if (hasDownloadingProducts()) {
            if (!this.progressPanel.isCurrentThread(this.threadId)) {
                this.threadId = this.progressPanel.incrementAndGetCurrentThreadId();
            }
            this.progressPanel.showProgressPanel(this.threadId); // show the progress panel
        }
    }

    private void onFinishRunningDownloadProductThread(DownloadProductRunnable parentRunnable, SaveDownloadedProductData saveProductData) {
        if (this.runningTasks.remove(parentRunnable)) {
            this.totalDownloadedProducts++;

            onUpdateProgressBarDownloadedProducts();

            boolean hasProductsToDownload = hasDownloadingProducts();

            this.downloadProductListener.onFinishDownloadingProduct(parentRunnable, saveProductData, hasProductsToDownload);

            if (!hasProductsToDownload) {
                // there are no downloading products and hide the progress panel if visible
                this.progressPanel.hideProgressPanel(this.threadId);
            }

            if (this.runningTasks.size() == 0) {
                shutdownThreadPoolExecutor();
            }
        } else {
            throw new IllegalArgumentException("The parent thread parameter is wrong.");
        }
    }

    private void onFinishRunningDownloadProductQuickLookImageThread(DownloadProductsQuickLookImageRunnable parentRunnable) {
        if (this.runningTasks.remove(parentRunnable)) {
            if (this.runningTasks.size() == 0) {
                shutdownThreadPoolExecutor();
            }
        } else {
            throw new IllegalArgumentException("The parent thread parameter is wrong.");
        }
    }

    private void shutdownThreadPoolExecutor() {
        this.threadPoolExecutor.shutdown();
        this.threadPoolExecutor = null; // reset the thread pool
        this.runningTasks = null; // reset the running tasks
    }

    private void onUpdateProgressBarDownloadedProducts() {
        if (this.progressPanel.isCurrentThread(this.threadId)) {
            String text = buildProgressBarDownloadingText(this.totalDownloadedProducts, this.totalDownloadingProducts);
            this.progressPanel.updateProgressBarText(this.threadId, text);
        }
    }

    private void updateDownloadingProgressPercentLater(RepositoryProduct repositoryProduct, short progressPercent, Path downloadedPath) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Update the downloading progress percent " + progressPercent + "% of the product '" + repositoryProduct.getName()+"' using the '" + repositoryProduct.getMission()+"' mission.");
        }

        Runnable runnable = new UpdateDownloadedProgressPercentRunnable(repositoryProduct, progressPercent, downloadedPath, this.downloadProductListener) {
            @Override
            public void run() {
                if (progressPanel.isCurrentThread(threadId)) {
                    super.run();
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void updateDownloadingProductStatusLater(RepositoryProduct repositoryProduct, byte downloadStatus) {
        Runnable runnable = new UpdateDownloadingProductStatusRunnable(repositoryProduct, downloadStatus, this.downloadProductListener) {
            @Override
            public void run() {
                if (progressPanel.isCurrentThread(threadId)) {
                    super.run();
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    public static abstract class PairRunnable<First, Second> implements Runnable {

        final First first;
        final Second second;

        public PairRunnable(First first, Second second) {
            this.first = first;
            this.second = second;
        }
    }

    private static class UpdateDownloadingProductStatusRunnable implements Runnable {

        private final RepositoryProduct repositoryProduct;
        private final byte downloadStatus;
        private final DownloadProductListener downloadProductListener;

        private UpdateDownloadingProductStatusRunnable(RepositoryProduct repositoryProduct, byte downloadStatus, DownloadProductListener downloadProductListener) {
            this.repositoryProduct = repositoryProduct;
            this.downloadStatus = downloadStatus;
            this.downloadProductListener = downloadProductListener;
        }

        @Override
        public void run() {
            this.downloadProductListener.onUpdateProductDownloadStatus(this.repositoryProduct, this.downloadStatus);
        }
    }

    private static class UpdateDownloadedProgressPercentRunnable implements Runnable {

        private final RepositoryProduct productToDownload;
        private final short progressPercent;
        private final Path downloadedPath;
        private final DownloadProductListener downloadProductListener;

        private UpdateDownloadedProgressPercentRunnable(RepositoryProduct productToDownload, short progressPercent, Path downloadedPath,
                                                        DownloadProductListener downloadProductListener) {

            this.productToDownload = productToDownload;
            this.downloadProductListener = downloadProductListener;
            this.progressPercent = progressPercent;
            this.downloadedPath = downloadedPath;
        }

        @Override
        public void run() {
            this.downloadProductListener.onUpdateProductDownloadPercent(this.productToDownload, this.progressPercent, this.downloadedPath);
        }
    }

    public static String buildProgressBarDownloadingText(int totalDownloaded, int totalProducts) {
        return "Downloading products: " + Integer.toString(totalDownloaded) + " out of " + Integer.toString(totalProducts);
    }
}
