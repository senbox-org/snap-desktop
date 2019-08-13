package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.table.CustomTable;
import org.esa.snap.product.library.v2.IProductsDownloaderListener;
import org.esa.snap.product.library.v2.ProductLibraryItem;
import org.esa.snap.product.library.v2.SciHubDownloader;
import org.esa.snap.ui.loading.AbstractTimerRunnable;
import org.esa.snap.ui.loading.GenericRunnable;
import org.esa.snap.ui.loading.ILoadingIndicator;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 9/8/2019.
 */
public class DownloadProductListTimerRunnable extends AbstractTimerRunnable<List<ProductLibraryItem>> {

    private static final Logger logger = Logger.getLogger(DownloadProductListTimerRunnable.class.getName());

    private final String sensor;
    private final Map<String, Object> parametersValues;
    private final String dataSourceName;
    private final CustomTable<ProductLibraryItem> productsTable;
    private final JComponent parentComponent;

    public DownloadProductListTimerRunnable(ILoadingIndicator loadingIndicator, int threadId, JComponent parentComponent, CustomTable<ProductLibraryItem> productsTable,
                                            String dataSourceName, String sensor, Map<String, Object> parametersValues) {

        super(loadingIndicator, threadId, 500);

        this.sensor = sensor;
        this.parametersValues = parametersValues;
        this.dataSourceName = dataSourceName;
        this.parentComponent = parentComponent;
        this.productsTable = productsTable;
    }

    @Override
    protected List<ProductLibraryItem> execute() throws Exception {
        IProductsDownloaderListener downloaderListener = new IProductsDownloaderListener() {
            @Override
            public void notifyProductCount(long productCount) {
                String loadingIndicatorMessage;
                if (productCount == 1) {
                    loadingIndicatorMessage = "Download " + productCount + " product from " + dataSourceName + "...";
                } else {
                    loadingIndicatorMessage = "Download " + productCount + " products from " + dataSourceName + "...";
                }
                updateLoadingIndicatorMessageLater(loadingIndicatorMessage);
            }

            @Override
            public void notifyPageProducts(int pageNumber, List<ProductLibraryItem> pageResults) {
                notifyPageProductsLater(pageResults);
            }
        };

        SciHubDownloader sciHubDownloader = new SciHubDownloader("jcoravu", "jcoravu@yahoo.com", this.sensor, this.parametersValues);
        List<ProductLibraryItem> results = sciHubDownloader.downloadProductList(downloaderListener);

        for (int i=0; i<results.size(); i++) {
            ProductLibraryItem product = results.get(i);
            Image scaledQuickLookImage = null;
            if (product.getQuickLookLocation() != null) {
                try {
                    BufferedImage image = sciHubDownloader.downloadQuickLookImage(product.getQuickLookLocation());
                    if (image != null) {
                        scaledQuickLookImage = image.getScaledInstance(ProductLibraryToolViewV2.QUICK_LOOK_IMAGE_WIDTH, ProductLibraryToolViewV2.QUICK_LOOK_IMAGE_HEIGHT, BufferedImage.SCALE_FAST);
                    }
                } catch (Exception exception) {
                    logger.log(Level.SEVERE, "Failed to download the image quick look from url '" + product.getQuickLookLocation() + "'.", exception);
                }
            }
            notifyProductQuickLookLater(product, scaledQuickLookImage);
        }

        return results;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to download the product list.";
    }

    @Override
    protected void onTimerWakeUp(String messageToDisplay) {
        onDisplayLoadingIndicatorMessage("Searching products to " + this.dataSourceName+"...");
    }

    @Override
    protected void onSuccessfullyFinish(List<ProductLibraryItem> results) {
        if (results.size() > 0) {
            // do nothing
        } else {
            onShowInformationDialog("No product available according to the filter values.", "Information");
        }
    }

    @Override
    protected void onFailed(Exception exception) {
        onShowErrorDialog("Failed to download the list containing the products.", "Error");
    }

    private void notifyPageProductsLater(List<ProductLibraryItem> pageResults) {
        Runnable runnable = new GenericRunnable<List<ProductLibraryItem>>(pageResults) {
            @Override
            protected void execute(List<ProductLibraryItem> results) {
                onDownloadPageProducts(results);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void notifyProductQuickLookLater(ProductLibraryItem product, Image quickLookImage) {
        Runnable runnable = new ProductQuickLookImageRunnable(product, quickLookImage) {
            @Override
            protected void execute(ProductLibraryItem productItem, Image quickLookImageItem) {
                onDownloadProductQuickLookImage(productItem, quickLookImageItem);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void onDownloadPageProducts(List<ProductLibraryItem> pageResults) {
        productsTable.getModel().addRecordsAndFireEvent(pageResults);
    }

    private void onDownloadProductQuickLookImage(ProductLibraryItem product, Image quickLookImage) {
        ProductsTableModel tableModel = (ProductsTableModel)this.productsTable.getModel();
        tableModel.setProductQuickLookImage(product, quickLookImage);
        tableModel.fireTableDataChanged();
    }

    private void onShowErrorDialog(String message, String title) {
        JOptionPane.showMessageDialog(this.parentComponent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void onShowInformationDialog(String message, String title) {
        JOptionPane.showMessageDialog(this.parentComponent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private static abstract class ProductQuickLookImageRunnable implements Runnable {

        private final ProductLibraryItem product;
        private final Image quickLookImage;

        public ProductQuickLookImageRunnable(ProductLibraryItem product, Image quickLookImage) {
            this.product = product;
            this.quickLookImage = quickLookImage;
        }

        protected abstract void execute(ProductLibraryItem product, Image quickLookImage);

        @Override
        public void run() {
            execute(this.product, this.quickLookImage);
        }
    }
}
