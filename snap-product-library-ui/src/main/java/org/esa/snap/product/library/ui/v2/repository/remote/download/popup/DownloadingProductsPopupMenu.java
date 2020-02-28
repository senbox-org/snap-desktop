package org.esa.snap.product.library.ui.v2.repository.remote.download.popup;

import org.esa.snap.engine_utilities.util.Pair;
import org.esa.snap.product.library.ui.v2.repository.remote.DownloadProgressStatus;
import org.esa.snap.product.library.ui.v2.repository.remote.PopupDownloadProductsListener;
import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadProductRunnable;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Created by jcoravu on 11/2/2020.
 */
public class DownloadingProductsPopupMenu extends JPopupMenu implements PopupDownloadProductsListener {

    private final DownloadingProductsPopupPanel downloadingProductsPopupPanel;

    public DownloadingProductsPopupMenu(List<Pair<DownloadProductRunnable, DownloadProgressStatus>> downloadingProductRunnables,
                                        int gapBetweenRows, int gapBetweenColumns, Color backgroundColor) {

        this.downloadingProductsPopupPanel = new DownloadingProductsPopupPanel(downloadingProductRunnables, 5, gapBetweenRows, gapBetweenColumns);
        this.downloadingProductsPopupPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(this.downloadingProductsPopupPanel);
        scrollPane.setBackground(backgroundColor);
        scrollPane.setOpaque(true);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setOpaque(false);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void onUpdateProductDownloadProgress(RepositoryProduct repositoryProduct) {
        this.downloadingProductsPopupPanel.updateProductDownloadProgress(repositoryProduct);
    }

    @Override
    public void onStopDownloadingProduct(DownloadProductRunnable downloadProductRunnable) {
        this.downloadingProductsPopupPanel.stopDownloadingProduct(downloadProductRunnable);
    }

    public void refresh() {
        this.downloadingProductsPopupPanel.refresh();
    }
}
