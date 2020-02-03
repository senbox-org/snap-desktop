package org.esa.snap.product.library.ui.v2.repository.remote.download;

import org.apache.http.auth.Credentials;
import org.esa.snap.engine_utilities.util.ThreadNamePoolExecutor;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductListModel;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductDownloader;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteRepositoriesSemaphore;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelperImpl;
import org.esa.snap.product.library.v2.database.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.SaveDownloadedProductData;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 17/10/2019.
 */
public class DownloadRemoteProductsHelper {

    private static final Logger logger = Logger.getLogger(DownloadRemoteProductsHelper.class.getName());

    private final ProgressBarHelperImpl progressPanel;
    private final RemoteRepositoriesSemaphore remoteRepositoriesSemaphore;
    private final RepositoryOutputProductListPanel repositoryProductListPanel;

    private ThreadNamePoolExecutor threadPoolExecutor;
    private Set<AbstractBackgroundDownloadRunnable> runningTasks;
    private int currentStartRunningCount;
    private int threadId;
    private int totalProducts;
    private int totalDownloaded;

    public DownloadRemoteProductsHelper(ProgressBarHelperImpl progressPanel, RemoteRepositoriesSemaphore remoteRepositoriesSemaphore, RepositoryOutputProductListPanel repositoryProductListPanel) {
        this.progressPanel = progressPanel;
        this.remoteRepositoriesSemaphore = remoteRepositoriesSemaphore;
        this.repositoryProductListPanel = repositoryProductListPanel;
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
                finishRunningDownloadProductsQuickLookImageThreadLater(this);
            }
        };
        this.runningTasks.add(runnable);
        this.threadPoolExecutor.execute(runnable); // start the thread
    }

    public void downloadProductsAsync(RemoteProductDownloader[] remoteProductDownloaders, AllLocalFolderProductsRepository allLocalFolderProductsRepository) {
        createThreadPoolExecutorIfNeeded();

        for (int i=0; i<remoteProductDownloaders.length; i++) {
            DownloadProductRunnable runnable = new DownloadProductRunnable(remoteProductDownloaders[i], this.remoteRepositoriesSemaphore, allLocalFolderProductsRepository) {
                @Override
                protected void startRunning() {
                    super.startRunning();
                    startRunningDownloadProductThreadLater();
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
                    finishRunningDownloadProductThreadLater(this, saveProductData);
                }
            };
            this.runningTasks.add(runnable);
            this.totalProducts++;
            this.threadPoolExecutor.execute(runnable); // start the thread
        }

        onUpdateProgressBarDownloadedProducts();
    }

    protected void onFinishSavingProduct(SaveDownloadedProductData saveProductData) {
    }

    public boolean isRunning() {
        return (this.threadPoolExecutor != null);
    }

    public void stopDownloadingProducts() {
        if (this.runningTasks != null) {

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Stop downloading the products.");
            }

            for (AbstractBackgroundDownloadRunnable runnable : this.runningTasks) {
                if (runnable instanceof DownloadProductRunnable) {
                    runnable.stopRunning();
                }
            }
        }
    }

    public void stopDownloadingProductsQuickLookImage() {
        if (this.runningTasks != null) {

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Stop downloading the products quick look image.");
            }

            for (AbstractBackgroundDownloadRunnable runnable : this.runningTasks) {
                if (runnable instanceof DownloadProductsQuickLookImageRunnable) {
                    runnable.stopRunning();
                }
            }
        }
    }

    private void createThreadPoolExecutorIfNeeded() {
        if (this.threadPoolExecutor == null) {
            this.totalProducts = 0;
            this.totalDownloaded = 0;
            this.currentStartRunningCount = 0;
            this.threadId = this.progressPanel.incrementAndGetCurrentThreadId();
            int maximumThreadCount = Runtime.getRuntime().availableProcessors() - 1;
            this.threadPoolExecutor = new ThreadNamePoolExecutor("product-library", maximumThreadCount);
            this.runningTasks = new HashSet<>();
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

    private void onStartRunningDownloadProductThread() {
        this.currentStartRunningCount++;
        if (this.currentStartRunningCount == 1) {
            this.progressPanel.showProgressPanel(this.threadId); // show the progress panel
        }
    }

    private void onFinishRunningDownloadProductThread(DownloadProductRunnable parentRunnable, SaveDownloadedProductData saveProductData) {
        if (this.runningTasks.remove(parentRunnable)) {
            this.totalDownloaded++;

            onUpdateProgressBarDownloadedProducts();

            if (saveProductData != null) {
                onFinishSavingProduct(saveProductData);
            }
            if (this.runningTasks.size() == 0) {
                this.progressPanel.hideProgressPanel(this.threadId); // hide the progress panel
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
        this.runningTasks = null;
    }

    private void onUpdateProgressBarDownloadedProducts() {
        if (this.progressPanel.isCurrentThread(this.threadId)) {
            String text = buildProgressBarDownloadingText(this.totalDownloaded, this.totalProducts);
            this.progressPanel.updateProgressBarText(this.threadId, text);
        }
    }

    private void updateDownloadingProgressPercentLater(RepositoryProduct repositoryProduct, short progressPercent, Path downloadedPath) {
        Runnable runnable = new UpdateDownloadedProgressPercentRunnable(repositoryProduct, progressPercent, this.repositoryProductListPanel, downloadedPath) {
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
        Runnable runnable = new UpdateDownloadingProductStatusRunnable(repositoryProduct, downloadStatus, this.repositoryProductListPanel) {
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
        private final RepositoryOutputProductListPanel repositoryProductListPanel;

        private UpdateDownloadingProductStatusRunnable(RepositoryProduct repositoryProduct, byte downloadStatus, RepositoryOutputProductListPanel repositoryProductListPanel) {
            this.repositoryProduct = repositoryProduct;
            this.downloadStatus = downloadStatus;
            this.repositoryProductListPanel = repositoryProductListPanel;
        }

        @Override
        public void run() {
            OutputProductListModel productListModel = this.repositoryProductListPanel.getProductListPanel().getProductListModel();
            productListModel.setProductDownloadStatus(this.repositoryProduct, this.downloadStatus);
        }
    }

    private static class UpdateDownloadedProgressPercentRunnable implements Runnable {

        private final RepositoryProduct productToDownload;
        private final short progressPercent;
        private final RepositoryOutputProductListPanel repositoryProductListPanel;
        private final Path downloadedPath;

        private UpdateDownloadedProgressPercentRunnable(RepositoryProduct productToDownload, short progressPercent,
                                                        RepositoryOutputProductListPanel repositoryProductListPanel, Path downloadedPath) {

            this.productToDownload = productToDownload;
            this.progressPercent = progressPercent;
            this.repositoryProductListPanel = repositoryProductListPanel;
            this.downloadedPath = downloadedPath;
        }

        @Override
        public void run() {
            OutputProductListModel productListModel = this.repositoryProductListPanel.getProductListPanel().getProductListModel();
            productListModel.setProductDownloadPercent(this.productToDownload, this.progressPercent, this.downloadedPath);
        }
    }

    public static String buildProgressBarDownloadingText(int totalDownloaded, int totalProducts) {
        return "Downloading products: " + Integer.toString(totalDownloaded) + " out of " + Integer.toString(totalProducts);
    }
}
