package org.esa.snap.product.library.ui.v2.repository.local;

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
public class LocalRepositoryProductPanel extends RepositoryProductPanel {

    public LocalRepositoryProductPanel(RepositoryProductPanelBackground repositoryProductPanelBackground,
                                       ComponentDimension componentDimension, ImageIcon expandImageIcon, ImageIcon collapseImageIcon) {

        super(repositoryProductPanelBackground, componentDimension, expandImageIcon, collapseImageIcon);
    }

    @Override
    public void refresh(int index, ProductListModel productListModel) {
        super.refresh(index, productListModel);

        RepositoryProduct repositoryProduct = productListModel.getProductAt(index);
        OpenProgressStatus openProgressStatus = productListModel.getOpeningProductStatus(repositoryProduct);
        updateOpeningStatus(openProgressStatus);
    }

    private void updateOpeningStatus(OpenProgressStatus openProgressStatus) {
        Color foregroundColor = getDefaultForegroundColor();
        String openText = "";
        if (openProgressStatus != null) {
            // the product is opening or opened
            if (openProgressStatus.isPending()) {
                openText = "Pending open...";
            } else if (openProgressStatus.isOpening()) {
                openText = "Opening...";
            } else if (openProgressStatus.isFailed()) {
                openText = "Failed";
                foregroundColor = Color.RED;
            } else if (openProgressStatus.isOpened()) {
                openText = "Opened";
                foregroundColor = Color.GREEN;
            }
        }
        this.statusLabel.setForeground(foregroundColor);
        this.statusLabel.setText(openText);
    }
}
