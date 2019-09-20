package org.esa.snap.product.library.ui.v2.repository.remote;

import org.apache.http.auth.Credentials;
import org.esa.snap.product.library.ui.v2.RepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.ThreadListener;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.remote.products.repository.listener.ProductListDownloaderListener;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
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
    private final String dataSourceName;
    private final Credentials credentials;
    private final RepositoryProductListPanel repositoryProductListPanel;
    private final RemoteProductsRepositoryProvider productsRepositoryProvider;
    private final ThreadListener threadListener;

    public DownloadProductListTimerRunnable(ProgressBarHelper progressPanel, int threadId, Credentials credentials,
                                            RemoteProductsRepositoryProvider productsRepositoryProvider, ThreadListener threadListener,
                                            RepositoryProductListPanel repositoryProductListPanel,
                                            String dataSourceName, String mission, Map<String, Object> parameterValues) {

        super(progressPanel, threadId, 500);

        this.mission = mission;
        this.productsRepositoryProvider = productsRepositoryProvider;
        this.parameterValues = parameterValues;
        this.dataSourceName = dataSourceName;
        this.credentials = credentials;
        this.threadListener = threadListener;
        this.repositoryProductListPanel = repositoryProductListPanel;
    }

    @Override
    protected Void execute() throws Exception {
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
        List<RepositoryProduct> productList = this.productsRepositoryProvider.downloadProductList(this.credentials, this.mission, this.parameterValues, downloaderListener, this);

        if (isRunning()) {
            if (productList.size() > 0) {
                hideProgressPanelLater();

                for (int i=0; i<productList.size(); i++) {
                    if (!isRunning()) {
                        return null; // nothing to return
                    }

                    RepositoryProduct repositoryProduct = productList.get(i);
                    BufferedImage quickLookImage = null;
                    if (repositoryProduct.getDownloadQuickLookImageURL() != null) {
                        try {
                            quickLookImage = this.productsRepositoryProvider.downloadProductQuickLookImage(this.credentials, repositoryProduct.getDownloadQuickLookImageURL(), this);
                        } catch (InterruptedException exception) {
                            logger.log(Level.SEVERE, "Stop downloading the product quick look image from url '" + repositoryProduct.getDownloadQuickLookImageURL() + "'.", exception);
                            return null; // nothing to return
                        } catch (Exception exception) {
                            logger.log(Level.SEVERE, "Failed to download the product quick look image from url '" + repositoryProduct.getDownloadQuickLookImageURL() + "'.", exception);
                        }
                    }
                    setProductQuickLookImageLater(repositoryProduct, quickLookImage);
                }
            }
        }
        return null;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to retrieve the product list from '" + this.dataSourceName + "'.";
    }

    @Override
    protected boolean onHideProgressPanelLater() {
        boolean hidden = super.onHideProgressPanelLater();
        if (hidden) {
            this.repositoryProductListPanel.finishDownloadingProductList();
        }
        return hidden;
    }

    @Override
    protected boolean onTimerWakeUp() {
        boolean progressPanelVisible = super.onTimerWakeUp();
        if (progressPanelVisible) {
            this.repositoryProductListPanel.startSearchingProductList(this.dataSourceName);
        }
        return progressPanelVisible;
    }

    @Override
    protected void onFailed(Exception exception) {
        onShowErrorMessageDialog(this.repositoryProductListPanel, "Failed to retrieve the product list from " + this.dataSourceName + ".", "Error");
    }

    @Override
    protected void onStopExecuting() {
        this.threadListener.onStopExecuting();
    }

    private void updateProductListSizeLater(long totalProductCount) {
        GenericRunnable<Long> runnable = new GenericRunnable<Long>(totalProductCount) {
            @Override
            protected void execute(Long totalProductCountValue) {
                if (isCurrentProgressPanelThread()) {
                    repositoryProductListPanel.startDownloadingProductList(totalProductCountValue.longValue(), dataSourceName);
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
                    repositoryProductListPanel.addProducts(pageResultsValue, totalProductCountValue, retrievedProductCountValue, dataSourceName);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void setProductQuickLookImageLater(RepositoryProduct product, BufferedImage quickLookImage) {
        Runnable runnable = new ProductQuickLookImageRunnable(product, quickLookImage) {
            @Override
            protected void execute(RepositoryProduct productValue, BufferedImage quickLookImageValue) {
                productValue.setQuickLookImage(quickLookImageValue);
                repositoryProductListPanel.repaint();
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
}
