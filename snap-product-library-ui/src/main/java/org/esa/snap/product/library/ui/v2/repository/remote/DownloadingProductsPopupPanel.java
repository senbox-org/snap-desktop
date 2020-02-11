package org.esa.snap.product.library.ui.v2.repository.remote;

import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadProductListener;
import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadProductRunnable;
import org.esa.snap.product.library.v2.database.SaveDownloadedProductData;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.VerticalScrollablePanel;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by jcoravu on 11/2/2020.
 */
public class DownloadingProductsPopupPanel extends VerticalScrollablePanel {

    private final RemoteRepositoriesProductProgress remoteRepositoriesProductProgress;

    private int preferredScrollableHeight;

    public DownloadingProductsPopupPanel(RemoteRepositoriesProductProgress remoteRepositoriesProductProgress, List<DownloadProductRunnable> downloadingProductRunnables,
                                         int visibleProductCount, int gapBetweenRows, int gapBetweenColumns) {

        super(null);

        this.remoteRepositoriesProductProgress = remoteRepositoriesProductProgress;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Color separatorColor = UIManager.getColor("controlShadow");
        this.preferredScrollableHeight = 0;
        for (int i=0; i<downloadingProductRunnables.size(); i++) {
            DownloadingProductPanel downloadingProductPanel = new DownloadingProductPanel(downloadingProductRunnables.get(i), gapBetweenColumns);
            int topLineHeight = (i > 0) ? 1 : 0;
            Border outsideBorder = new MatteBorder(topLineHeight, 0, 0, 0, separatorColor);
            Border insideBorder = new EmptyBorder(gapBetweenRows, gapBetweenColumns, gapBetweenRows, gapBetweenColumns);
            downloadingProductPanel.setBorder(new CompoundBorder(outsideBorder, insideBorder));
            add(downloadingProductPanel);
            if (i < visibleProductCount) {
                this.preferredScrollableHeight += downloadingProductPanel.getPreferredSize().height;
            }
        }
    }

    @Override
    public final Dimension getPreferredScrollableViewportSize() {
        Dimension size = super.getPreferredScrollableViewportSize();

        size.height = this.preferredScrollableHeight;
        return size;
    }

    public void updateProductDownloadProgress(RepositoryProduct repositoryProduct, DownloadProgressStatus progressProgressStatus) {
        int productCount = getComponentCount();
        for (int i=0; i<productCount; i++) {
            DownloadingProductPanel downloadingProductPanel = (DownloadingProductPanel)getComponent(i);
            downloadingProductPanel.refresh(repositoryProduct, progressProgressStatus);
        }
    }

    public void finishDownloadingProduct(DownloadProductRunnable downloadProductRunnable) {
        int productCount = getComponentCount();
        for (int i=0; i<productCount; i++) {
            DownloadingProductPanel downloadingProductPanel = (DownloadingProductPanel)getComponent(i);
            if (downloadingProductPanel.finishDownloading(downloadProductRunnable)) {
                break;
            }
        }
    }

    public void refresh() {
        int productCount = getComponentCount();
        for (int i=0; i<productCount; i++) {
            DownloadingProductPanel downloadingProductPanel = (DownloadingProductPanel)getComponent(i);
            downloadingProductPanel.refresh(this.remoteRepositoriesProductProgress);
        }
    }
}
