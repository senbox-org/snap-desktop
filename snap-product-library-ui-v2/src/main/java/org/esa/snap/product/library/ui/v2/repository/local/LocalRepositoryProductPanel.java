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
            } else if (localProgressStatus.getStatus() == LocalProgressStatus.PENDING_COPY) {
                openText = "Pending copy...";
            } else if (localProgressStatus.getStatus() == LocalProgressStatus.COPYING) {
                openText = "Copying...";
            } else if (localProgressStatus.getStatus() == LocalProgressStatus.COPIED) {
                openText = "Copied";
                foregroundColor = Color.GREEN;
            } else if (localProgressStatus.getStatus() == LocalProgressStatus.FAIL_COPIED) {
                openText = "Failed copied";
                foregroundColor = Color.RED;
            } else if (localProgressStatus.getStatus() == LocalProgressStatus.PENDING_MOVE) {
                openText = "Pending move...";
            } else if (localProgressStatus.getStatus() == LocalProgressStatus.MOVING) {
                openText = "Moving...";
            } else if (localProgressStatus.getStatus() == LocalProgressStatus.MOVED) {
                openText = "Moved";
                foregroundColor = Color.GREEN;
            } else if (localProgressStatus.getStatus() == LocalProgressStatus.FAIL_MOVED) {
                openText = "Failed moved";
                foregroundColor = Color.RED;
            } else if (localProgressStatus.getStatus() == LocalProgressStatus.MISSING_PRODUCT_FROM_REPOSITORY) {
                openText = "Missing product from repository";
                foregroundColor = Color.RED;
            } else {
                throw new IllegalStateException("Unknown status " + localProgressStatus.getStatus() + ".");
            }
        }
        this.statusLabel.setForeground(foregroundColor);
        this.statusLabel.setText(openText);
    }
}
