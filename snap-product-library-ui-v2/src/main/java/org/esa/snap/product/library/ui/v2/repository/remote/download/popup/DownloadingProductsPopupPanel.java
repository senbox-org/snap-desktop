package org.esa.snap.product.library.ui.v2.repository.remote.download.popup;

import org.esa.snap.engine_utilities.util.Pair;
import org.esa.snap.product.library.ui.v2.repository.remote.DownloadProgressStatus;
import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadProductRunnable;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.VerticalScrollablePanel;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.List;

/**
 * The panel containing the downloading products.
 *
 * Created by jcoravu on 11/2/2020.
 */
public class DownloadingProductsPopupPanel extends VerticalScrollablePanel {

    private int preferredScrollableHeight;

    public DownloadingProductsPopupPanel(List<Pair<DownloadProductRunnable, DownloadProgressStatus>> downloadingProductRunnables,
                                         int visibleProductCount, int gapBetweenRows, int gapBetweenColumns) {

        super(null);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Color separatorColor = UIManager.getColor("controlShadow");
        this.preferredScrollableHeight = 0;
        for (int i=0; i<downloadingProductRunnables.size(); i++) {
            Pair<DownloadProductRunnable, DownloadProgressStatus> pair = downloadingProductRunnables.get(i);
            DownloadingProductPanel downloadingProductPanel = new DownloadingProductPanel(pair.getFirst(), pair.getSecond(), gapBetweenColumns);

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

    public void updateProductDownloadProgress(RepositoryProduct repositoryProduct) {
        int productCount = getComponentCount();
        for (int i=0; i<productCount; i++) {
            DownloadingProductPanel downloadingProductPanel = (DownloadingProductPanel)getComponent(i);
            downloadingProductPanel.refreshDownloadStatus();
        }
    }

    public void stopDownloadingProduct(DownloadProductRunnable downloadProductRunnable) {
        int productCount = getComponentCount();
        for (int i=0; i<productCount; i++) {
            DownloadingProductPanel downloadingProductPanel = (DownloadingProductPanel)getComponent(i);
            if (downloadingProductPanel.stopDownloading(downloadProductRunnable)) {
                break;
            }
        }
    }

    public void refresh() {
        int productCount = getComponentCount();
        for (int i=0; i<productCount; i++) {
            DownloadingProductPanel downloadingProductPanel = (DownloadingProductPanel)getComponent(i);
            downloadingProductPanel.refreshDownloadStatus();
        }
    }
}
