package org.esa.snap.product.library.ui.v2.repository.remote;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanelBackground;
import org.esa.snap.product.library.ui.v2.repository.AbstractRepositoryProductPanel;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductResults;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.*;

/**
 * Created by jcoravu on 27/9/2019.
 */
public class RemoteRepositoryProductPanel extends AbstractRepositoryProductPanel {

    public RemoteRepositoryProductPanel(RepositoryProductPanelBackground repositoryProductPanelBackground,
                                        ComponentDimension componentDimension, ImageIcon expandImageIcon, ImageIcon collapseImageIcon) {

        super(repositoryProductPanelBackground, componentDimension, expandImageIcon, collapseImageIcon);
    }

    @Override
    protected JLabel buildStatusLabel() {
        return new RemoteProductStatusLabel();
    }

    @Override
    public void refresh(OutputProductResults outputProductResults) {
        super.refresh(outputProductResults);

        RepositoryProduct repositoryProduct = getRepositoryProduct();
        DownloadProgressStatus progressPercent = outputProductResults.getDownloadingProductProgressValue(repositoryProduct);
        ((RemoteProductStatusLabel)this.statusLabel).updateDownloadingPercent(progressPercent, getDefaultForegroundColor());
    }
}
