package org.esa.snap.product.library.ui.v2.repository.remote;

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

    public DownloadingProductsPopupMenu(List<DownloadProductRunnable> downloadingProductRunnables, RemoteRepositoriesProductProgress remoteRepositoriesProductProgress,
                                        int gapBetweenRows, int gapBetweenColumns, Color backgroundColor) {

        this.downloadingProductsPopupPanel = new DownloadingProductsPopupPanel(remoteRepositoriesProductProgress, downloadingProductRunnables, 5, gapBetweenRows, gapBetweenColumns);
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
    public void onUpdateProductDownloadProgress(RepositoryProduct repositoryProduct, DownloadProgressStatus progressProgressStatus) {
        this.downloadingProductsPopupPanel.updateProductDownloadProgress(repositoryProduct, progressProgressStatus);
    }

    @Override
    public void onFinishDownloadingProduct(DownloadProductRunnable downloadProductRunnable) {
        this.downloadingProductsPopupPanel.finishDownloadingProduct(downloadProductRunnable);
    }

    public void refresh() {
        this.downloadingProductsPopupPanel.refresh();
    }
}
