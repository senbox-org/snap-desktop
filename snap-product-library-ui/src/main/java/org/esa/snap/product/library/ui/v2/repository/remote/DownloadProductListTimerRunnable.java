package org.esa.snap.product.library.ui.v2.repository.remote;

import org.apache.http.auth.Credentials;
import org.esa.snap.product.library.ui.v2.ProductLibraryToolViewV2;
import org.esa.snap.product.library.ui.v2.ProductListModel;
import org.esa.snap.product.library.ui.v2.RepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.ThreadListener;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.remote.products.repository.HTTPServerException;
import org.esa.snap.remote.products.repository.listener.ProductListDownloaderListener;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 9/8/2019.
 */
public class DownloadProductListTimerRunnable extends AbstractProgressTimerRunnable<Void> {

    private static final Logger logger = Logger.getLogger(DownloadProductListTimerRunnable.class.getName());

    private final String mission;
    private final Map<String, Object> parameterValues;
    private final String remoteRepositoryName;
    private final Credentials credentials;
    private final RepositoryProductListPanel repositoryProductListPanel;
    private final RemoteProductsRepositoryProvider productsRepositoryProvider;
    private final ThreadListener threadListener;
    private final RemoteRepositoriesSemaphore remoteRepositoriesSemaphore;

    public DownloadProductListTimerRunnable(ProgressBarHelper progressPanel, int threadId, Credentials credentials,
                                            RemoteProductsRepositoryProvider productsRepositoryProvider, ThreadListener threadListener,
                                            RemoteRepositoriesSemaphore remoteRepositoriesSemaphore, RepositoryProductListPanel repositoryProductListPanel,
                                            String remoteRepositoryName, String mission, Map<String, Object> parameterValues) {

        super(progressPanel, threadId, 500);

        this.mission = mission;
        this.productsRepositoryProvider = productsRepositoryProvider;
        this.parameterValues = parameterValues;
        this.remoteRepositoryName = remoteRepositoryName;
        this.credentials = credentials;
        this.threadListener = threadListener;
        this.repositoryProductListPanel = repositoryProductListPanel;
        this.remoteRepositoriesSemaphore = remoteRepositoriesSemaphore;
    }

    @Override
    protected Void execute() throws Exception {
        this.remoteRepositoriesSemaphore.acquirePermission(this.productsRepositoryProvider.getRepositoryName(), this.credentials);
        try {
            List<RepositoryProduct> productList = downloadProductList();
            if (isRunning()) {
                if (productList.size() > 0) {
                    hideProgressPanelLater();
                    downloadQuickLookImages(productList);
                }
            }
        } finally {
            this.remoteRepositoriesSemaphore.releasePermission(this.productsRepositoryProvider.getRepositoryName(), this.credentials);
        }
        return null; // nothing to return
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to retrieve the product list from '" + this.remoteRepositoryName + "'.";
    }

    @Override
    protected void onFailed(Exception exception) {
        onShowErrorMessageDialog(this.repositoryProductListPanel, "Failed to retrieve the product list from " + this.remoteRepositoryName + ".", "Error");
    }

    @Override
    protected void onStopExecuting() {
        this.threadListener.onStopExecuting();
    }

    private void downloadQuickLookImages(List<RepositoryProduct> productList) throws Exception {
        int maximumInternalServerErrorCount = 3;
        int internalServerErrorCount = 0;
        for (int i = 0; i < productList.size() && internalServerErrorCount < maximumInternalServerErrorCount; i++) {
            if (!isRunning()) {
                return; // nothing to return
            }

            RepositoryProduct repositoryProduct = productList.get(i);
            BufferedImage quickLookImage = null;
            if (repositoryProduct.getDownloadQuickLookImageURL() != null) {
                try {
                    quickLookImage = this.productsRepositoryProvider.downloadProductQuickLookImage(this.credentials, repositoryProduct.getDownloadQuickLookImageURL(), this);
                } catch (java.lang.InterruptedException exception) {
                    logger.log(Level.WARNING, "Stop downloading the product quick look image from url '" + repositoryProduct.getDownloadQuickLookImageURL() + "'.");
                    return; // nothing to return
                } catch (HTTPServerException exception) {
                    logger.log(Level.SEVERE, "Failed to download the product quick look image from url '" + repositoryProduct.getDownloadQuickLookImageURL() + "'.", exception);
                    if (exception.getStatusCodeResponse() >= HttpURLConnection.HTTP_INTERNAL_ERROR) {
                        internalServerErrorCount++;
                    }
                } catch (java.lang.Exception exception) {
                    logger.log(Level.SEVERE, "Failed to download the product quick look image from url '" + repositoryProduct.getDownloadQuickLookImageURL() + "'.", exception);
                }
            }
            setProductQuickLookImageLater(repositoryProduct, quickLookImage);
        }
    }

    private List<RepositoryProduct> downloadProductList() throws Exception {
        ProductListDownloaderListener downloaderListener = new ProductListDownloaderListener() {
            @Override
            public void notifyProductCount(long totalProductCount) {
                if (isRunning()) {
                    updateProductListSizeLater(totalProductCount);
                }
            }

            @Override
            public void notifyPageProducts(int pageNumber, List<RepositoryProduct> pageResults, long totalProductCount, int retrievedProductCount) {
                if (isRunning()) {
                    updatePageProductsLater(pageResults, totalProductCount, retrievedProductCount);
                }
            }
        };
        return this.productsRepositoryProvider.downloadProductList(this.credentials, this.mission, this.parameterValues, downloaderListener, this);
    }

    private void updateProductListSizeLater(long totalProductCount) {
        GenericRunnable<Long> runnable = new GenericRunnable<Long>(totalProductCount) {
            @Override
            protected void execute(Long totalProductCountValue) {
                if (isCurrentProgressPanelThread()) {
                    String text = buildProgressBarDownloadingText(0, totalProductCountValue.longValue());
                    onUpdateProgressBarText(text);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void updatePageProductsLater(List<RepositoryProduct> pageResults, long totalProductCount, int retrievedProductCount) {
        Runnable runnable = new ProductPageResultsRunnable(pageResults, totalProductCount, retrievedProductCount) {
            @Override
            protected void execute(List<RepositoryProduct> pageResultsValue, long totalProductCountValue, int retrievedProductCountValue) {
                if (isCurrentProgressPanelThread()) {
                    repositoryProductListPanel.addProducts(pageResultsValue);
                    String text = buildProgressBarDownloadingText(retrievedProductCountValue, totalProductCountValue);
                    onUpdateProgressBarText(text);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void setProductQuickLookImageLater(RepositoryProduct product, BufferedImage quickLookImage) {
        Runnable runnable = new ProductQuickLookImageRunnable(product, quickLookImage) {
            @Override
            protected void execute(RepositoryProduct productValue, BufferedImage quickLookImageValue) {
                ProductListModel productListModel = repositoryProductListPanel.getProductListPanel().getProductListModel();
                productListModel.setProductQuickLookImage(productValue, quickLookImageValue);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private static abstract class ProductQuickLookImageRunnable implements Runnable {

        private final RepositoryProduct product;
        private final BufferedImage quickLookImage;

        public ProductQuickLookImageRunnable(RepositoryProduct product, BufferedImage quickLookImage) {
            this.product = product;
            this.quickLookImage = quickLookImage;
        }

        protected abstract void execute(RepositoryProduct product, BufferedImage quickLookImage);

        @Override
        public void run() {
            execute(this.product, this.quickLookImage);
        }
    }

    private static abstract class ProductPageResultsRunnable implements Runnable {

        private final List<RepositoryProduct> pageResults;
        private final long totalProductCount;
        private final int retrievedProductCount;

        public ProductPageResultsRunnable(List<RepositoryProduct> pageResults, long totalProductCount, int retrievedProductCount) {
            this.pageResults = pageResults;
            this.totalProductCount = totalProductCount;
            this.retrievedProductCount = retrievedProductCount;
        }

        protected abstract void execute(List<RepositoryProduct> pageResults, long totalProductCount, int retrievedProductCount);

        @Override
        public void run() {
            execute(this.pageResults, this.totalProductCount, this.retrievedProductCount);
        }
    }

    public static String buildProgressBarDownloadingText(long totalDownloaded, long totalProducts) {
        return ProductLibraryToolViewV2.getSearchingProductListMessage() + ": " + Long.toString(totalDownloaded) + " out of " + Long.toString(totalProducts);
    }
}
