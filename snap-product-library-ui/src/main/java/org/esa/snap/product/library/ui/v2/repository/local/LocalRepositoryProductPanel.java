package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductListModel;
import org.esa.snap.product.library.ui.v2.repository.AbstractRepositoryProductPanel;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanelBackground;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductResults;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.ImageIcon;
import java.awt.Color;

/**
 * Created by jcoravu on 27/9/2019.
 */
public class LocalRepositoryProductPanel extends AbstractRepositoryProductPanel {

    public LocalRepositoryProductPanel(RepositoryProductPanelBackground repositoryProductPanelBackground,
                                       ComponentDimension componentDimension, ImageIcon expandImageIcon, ImageIcon collapseImageIcon) {

        super(repositoryProductPanelBackground, componentDimension, expandImageIcon, collapseImageIcon);
    }

    @Override
    public void refresh(OutputProductResults outputProductResults) {
        super.refresh(outputProductResults);

        RepositoryProduct repositoryProduct = getRepositoryProduct();
        LocalProgressStatus localProgressStatus = outputProductResults.getOpeningProductStatus(repositoryProduct);
        updateProgressStatus(localProgressStatus);
    }

    private void updateProgressStatus(LocalProgressStatus localProgressStatus) {
        Color foregroundColor = getDefaultForegroundColor();
        String openText = " "; // set an empty space for the default text
        if (localProgressStatus != null) {
            if (localProgressStatus.isPendingOpen()) {
                openText = "Pending open...";
            } else if (localProgressStatus.isOpening()) {
                openText = "Opening...";
            } else if (localProgressStatus.isFailOpened()) {
                openText = "Failed open";
                foregroundColor = Color.RED;
            } else if (localProgressStatus.isFailOpenedBecauseNoProductReader()) {
                openText = "Failed open because no product reader found";
                foregroundColor = Color.RED;
            } else if (localProgressStatus.isOpened()) {
                openText = "Opened";
                foregroundColor = Color.GREEN;
            } else if (localProgressStatus.isPendingDelete()) {
                openText = "Pending delete...";
            } else if (localProgressStatus.isFailDeleted()) {
                openText = "Failed deleted";
                foregroundColor = Color.RED;
            } else if (localProgressStatus.isDeleting()) {
                openText = "Deleting...";
            } else if (localProgressStatus.isDeleted()) {
                openText = "Deleted";
                foregroundColor = Color.GREEN;
            } else {
                throw new IllegalStateException("Unknown status.");
            }
        }
        this.statusLabel.setForeground(foregroundColor);
        this.statusLabel.setText(openText);
    }
}
