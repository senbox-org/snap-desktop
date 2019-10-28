package org.esa.snap.product.library.ui.v2.repository.remote;

import org.esa.snap.engine_utilities.util.ThreadNamePoolExecutor;
import org.esa.snap.product.library.ui.v2.ProductListModel;
import org.esa.snap.product.library.ui.v2.RepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelperImpl;
import org.esa.snap.product.library.v2.database.SaveDownloadedProductData;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.SwingUtilities;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by jcoravu on 17/10/2019.
 */
public class DownloadRemoteProductsHelper {

    private final ProgressBarHelperImpl progressPanel;
    private final RemoteRepositoriesSemaphore remoteRepositoriesSemaphore;
    private final RepositoryProductListPanel repositoryProductListPanel;

    private ThreadNamePoolExecutor threadPoolExecutor;
    private Set<DownloadProductRunnable> runningTasks;
    private int currentStartRunningCount;
    private int threadId;
    private int totalProducts;
    private int totalDownloaded;

    public DownloadRemoteProductsHelper(ProgressBarHelperImpl progressPanel, RemoteRepositoriesSemaphore remoteRepositoriesSemaphore, RepositoryProductListPanel repositoryProductListPanel) {
        this.progressPanel = progressPanel;
        this.remoteRepositoriesSemaphore = remoteRepositoriesSemaphore;
        this.repositoryProductListPanel = repositoryProductListPanel;
    }

    public void downloadProductsAsync(RemoteProductDownloader[] remoteProductDownloaders) {
        if (this.threadPoolExecutor == null) {
            this.totalProducts = 0;
            this.totalDownloaded = 0;
            this.currentStartRunningCount = 0;
            this.threadId = this.progressPanel.incrementAndGetCurrentThreadId();
            int maximumThreadCount = Runtime.getRuntime().availableProcessors() - 1;
            this.threadPoolExecutor = new ThreadNamePoolExecutor("product-library", maximumThreadCount);
            this.runningTasks = new HashSet<>();
        }

        for (int i=0; i<remoteProductDownloaders.length; i++) {
            DownloadProductRunnable runnable = new DownloadProductRunnable(remoteProductDownloaders[i], this.remoteRepositoriesSemaphore) {
                @Override
                protected void startRunningThread() {
                    startRunningThreadLater();
                }

                @Override
                protected void updateDownloadingProductStatus(RepositoryProduct repositoryProduct, byte downloadStatus) {
                    updateDownloadingProductStatusLater(repositoryProduct, downloadStatus);
                }

                @Override
                protected void finishRunningThread(SaveDownloadedProductData saveProductData) {
                    finishRunningThreadLater(this, saveProductData);
                }

                @Override
                protected void updateDownloadingProgressPercent(RepositoryProduct repositoryProduct, short progressPercent) {
                    updateDownloadingProgressPercentLater(repositoryProduct, progressPercent);
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

    public void stopRunning() {
        for (DownloadProductRunnable runnable : this.runningTasks) {
            runnable.stopRunning();
        }
    }

    private void startRunningThreadLater() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                onStartRunningThread();
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void finishRunningThreadLater(DownloadProductRunnable parentRunnableItem, SaveDownloadedProductData saveProductDataItem) {
        Runnable runnable = new FinishRunningThreadRunnable(parentRunnableItem, saveProductDataItem) {
            @Override
            public void run() {
                onFinishRunningThread(this.parentRunnable, this.saveProductData);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void onStartRunningThread() {
        this.currentStartRunningCount++;
        if (this.currentStartRunningCount == 1) {
            this.progressPanel.showProgressPanel(this.threadId); // show the progress panel
        }
    }

    private void onFinishRunningThread(DownloadProductRunnable parentRunnable, SaveDownloadedProductData saveProductData) {
        if (this.runningTasks.remove(parentRunnable)) {
            this.totalDownloaded++;

            onUpdateProgressBarDownloadedProducts();

            if (saveProductData != null) {
                onFinishSavingProduct(saveProductData);
            }
            if (this.runningTasks.size() == 0) {
                this.progressPanel.hideProgressPanel(this.threadId); // hide the progress panel

                this.threadPoolExecutor.shutdown();
                this.threadPoolExecutor = null; // reset the thread pool
                this.runningTasks = null;
            }
        } else {
            throw new IllegalArgumentException("The parent thread parameter is wrong.");
        }
    }

    private void onUpdateProgressBarDownloadedProducts() {
        if (this.progressPanel.isCurrentThread(this.threadId)) {
            String text = buildProgressBarDownloadingText(this.totalDownloaded, this.totalProducts);
            this.progressPanel.updateProgressBarText(this.threadId, text);
        }
    }

    private void updateDownloadingProgressPercentLater(RepositoryProduct repositoryProduct, short progressPercent) {
        Runnable runnable = new UpdateDownloadedProgressPercentRunnable(repositoryProduct, progressPercent, this.repositoryProductListPanel) {
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

    private static abstract class FinishRunningThreadRunnable implements Runnable {

        final DownloadProductRunnable parentRunnable;
        final SaveDownloadedProductData saveProductData;

        public FinishRunningThreadRunnable(DownloadProductRunnable parentRunnable, SaveDownloadedProductData saveProductData) {
            this.parentRunnable = parentRunnable;
            this.saveProductData = saveProductData;
        }
    }

    private static class UpdateDownloadingProductStatusRunnable implements Runnable {

        private final RepositoryProduct repositoryProduct;
        private final byte downloadStatus;
        private final RepositoryProductListPanel repositoryProductListPanel;

        private UpdateDownloadingProductStatusRunnable(RepositoryProduct repositoryProduct, byte downloadStatus, RepositoryProductListPanel repositoryProductListPanel) {
            this.repositoryProduct = repositoryProduct;
            this.downloadStatus = downloadStatus;
            this.repositoryProductListPanel = repositoryProductListPanel;
        }

        @Override
        public void run() {
            ProductListModel productListModel = this.repositoryProductListPanel.getProductListPanel().getProductListModel();
            productListModel.setProductDownloadStatus(this.repositoryProduct, this.downloadStatus);
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
            ProductListModel productListModel = this.repositoryProductListPanel.getProductListPanel().getProductListModel();
            productListModel.setProductDownloadPercent(this.productToDownload, this.progressPercent);
        }
    }

    public static String buildProgressBarDownloadingText(int totalDownloaded, int totalProducts) {
        return "Downloading products: " + Integer.toString(totalDownloaded) + " out of " + Integer.toString(totalProducts);
    }
}
