package org.esa.snap.product.library.ui.v2.repository.remote;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanelBackground;
import org.esa.snap.product.library.ui.v2.repository.AbstractRepositoryProductPanel;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductResults;
import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadingProductProgressCallback;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.*;

/**
 * The panel to show the data of a remote repository product.
 *
 * Created by jcoravu on 27/9/2019.
 */
public class RemoteRepositoryProductPanel extends AbstractRepositoryProductPanel {

    private final DownloadingProductProgressCallback downloadingProductProgressCallback;

    public RemoteRepositoryProductPanel(RepositoryProductPanelBackground repositoryProductPanelBackground, DownloadingProductProgressCallback downloadingProductProgressCallback,
                                        ComponentDimension componentDimension) {

        super(repositoryProductPanelBackground, componentDimension);

        if (downloadingProductProgressCallback == null) {
            throw new NullPointerException("The downloading product callback is null.");
        }

        this.downloadingProductProgressCallback = downloadingProductProgressCallback;
    }

    @Override
    protected JLabel buildStatusLabel() {
        return new RemoteProductStatusLabel();
    }

    @Override
    public void refresh(OutputProductResults outputProductResults) {
        super.refresh(outputProductResults);

        RepositoryProduct repositoryProduct = getRepositoryProduct();
        DownloadProgressStatus downloadProgressStatus = this.downloadingProductProgressCallback.getDownloadingProductsProgressValue(repositoryProduct);
        if (downloadProgressStatus == null) {
            downloadProgressStatus = outputProductResults.getDownloadedProductProgress(repositoryProduct);
        }
        ((RemoteProductStatusLabel)this.statusLabel).updateDownloadingPercent(downloadProgressStatus, getDefaultForegroundColor());
    }
}
