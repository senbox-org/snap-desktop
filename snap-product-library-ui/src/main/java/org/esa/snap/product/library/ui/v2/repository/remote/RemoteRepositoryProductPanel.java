package org.esa.snap.product.library.ui.v2.repository.remote;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.ProductListModel;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanel;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanelBackground;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.ImageIcon;
import java.awt.Color;

/**
 * Created by jcoravu on 27/9/2019.
 */
public class RemoteRepositoryProductPanel extends RepositoryProductPanel {

    public RemoteRepositoryProductPanel(RepositoryProductPanelBackground repositoryProductPanelBackground,
                                        ComponentDimension componentDimension, ImageIcon expandImageIcon, ImageIcon collapseImageIcon) {

        super(repositoryProductPanelBackground, componentDimension, expandImageIcon, collapseImageIcon);
    }

    @Override
    public void refresh(int index, ProductListModel productListModel) {
        super.refresh(index, productListModel);

        RepositoryProduct repositoryProduct = productListModel.getProductAt(index);
        DownloadProgressStatus progressPercent = productListModel.getProductDownloadPercent(repositoryProduct);
        updateDownloadingPercent(progressPercent);
    }

    private void updateDownloadingPercent(DownloadProgressStatus progressPercent) {
        Color foregroundColor = getDefaultForegroundColor();
        String percentText = "";
        if (progressPercent != null) {
            // the product is pending download or downloading
            if (progressPercent.isDownloading()) {
                percentText = "Downloading: " + Integer.toString(progressPercent.getValue()) + "%";
            } else if (progressPercent.isPendingDownload()) {
                percentText = "Pending download";
            } else if (progressPercent.isStoppedDownload()) {
                percentText = "Downloading: " + Integer.toString(progressPercent.getValue()) + "% (stopped)";
            } else if (progressPercent.isDownloaded()) {
                percentText = "Downloaded";
                foregroundColor = Color.GREEN;
            } else if (progressPercent.isNotAvailable()) {
                percentText = "Not available to download";
                foregroundColor = Color.RED;
            } else if (progressPercent.isFailedDownload()) {
                percentText = "Downloading: " + Integer.toString(progressPercent.getValue()) + "% (failed)";
                foregroundColor = Color.RED;
            } else {
                throw new IllegalStateException("The percent progress status is unknown. The value is " + progressPercent.getValue());
            }
        }
        this.statusLabel.setForeground(foregroundColor);
        this.statusLabel.setText(percentText);
    }
}
