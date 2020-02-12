package org.esa.snap.product.library.ui.v2.repository.remote.download.popup;

import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.DownloadProgressStatus;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductStatusLabel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteRepositoriesProductProgress;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteRepositoryProductPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadProductRunnable;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by jcoravu on 11/2/2020.
 */
public class DownloadingProductPanel extends JPanel {

    private final JButton stopButton;
    private final JLabel nameLabel;
    private final JLabel acquisitionDateLabel;
    private final JLabel sizeLabel;
    private final JLabel missionLabel;
    private final RemoteProductStatusLabel downloadStatusLabel;
    private final DownloadProductRunnable downloadProductRunnable;

    public DownloadingProductPanel(DownloadProductRunnable downloadProductRunnable, int gapBetweenColumns) {
        super(new GridBagLayout());

        setOpaque(false);

        this.downloadProductRunnable = downloadProductRunnable;

        RepositoryProduct productToDownload = this.downloadProductRunnable.getProductToDownload();

        this.nameLabel = new JLabel(productToDownload.getName());
        this.acquisitionDateLabel = new JLabel(RemoteRepositoryProductPanel.buildAcquisitionDateLabelText(productToDownload.getAcquisitionDate()));
        this.missionLabel = new JLabel(RemoteRepositoryProductPanel.buildMissionLabelText(productToDownload.getMission()));
        this.sizeLabel = new JLabel(RemoteRepositoryProductPanel.buildSizeLabelText(productToDownload.getApproximateSize()));
        this.downloadStatusLabel = new RemoteProductStatusLabel();

        // set fixed width for 'size' label
        int preferredSizeWidth = this.acquisitionDateLabel.getPreferredSize().width;
        Dimension preferredSize = this.sizeLabel.getPreferredSize();
        preferredSize.width = preferredSizeWidth;
        this.sizeLabel.setPreferredSize(preferredSize);
        this.sizeLabel.setMaximumSize(preferredSize);
        this.sizeLabel.setMinimumSize(preferredSize);

        int stopButtonSize = (int)(1.5f *  this.nameLabel.getPreferredSize().height);
        Dimension buttonSize = new Dimension(stopButtonSize, stopButtonSize);
        this.stopButton = RepositorySelectionPanel.buildButton("/org/esa/snap/productlibrary/icons/stop20.gif", null, buttonSize, 1);
        this.stopButton.setToolTipText("Stop downloading the product");
        this.stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                stopButtonPressed();
            }
        });

        int gapBetweenRows = 0;
        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 2, 1, 0, 0);
        add(this.nameLabel, c);
        c = SwingUtils.buildConstraints(2, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 4, gapBetweenRows, 3 * gapBetweenColumns);
        add(this.stopButton, c);
        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 3, 1, gapBetweenRows, 0);
        add(this.missionLabel, c);
        c = SwingUtils.buildConstraints(0, 2, GridBagConstraints.NONE, GridBagConstraints.WEST, 3, 1, gapBetweenRows, 0);
        add(this.acquisitionDateLabel, c);
        c = SwingUtils.buildConstraints(0, 3, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        add(this.sizeLabel, c);
        c = SwingUtils.buildConstraints(1, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        add(this.downloadStatusLabel, c);
    }

    public void refreshDownloadStatus(RemoteRepositoriesProductProgress remoteRepositoriesProductProgress) {
        if (!this.downloadProductRunnable.isRunning()) {
            disableComponents();
        }
        RepositoryProduct productToDownload = this.downloadProductRunnable.getProductToDownload();
        DownloadProgressStatus progressProgressStatus = remoteRepositoriesProductProgress.findRemoteRepositoryProductDownloadProgress(productToDownload);
        if (progressProgressStatus != null) {
            this.downloadStatusLabel.updateDownloadingPercent(progressProgressStatus, this.sizeLabel.getForeground());
        }
    }

    public boolean refreshDownloadStatus(RepositoryProduct repositoryProduct, DownloadProgressStatus progressProgressStatus) {
        if (!this.downloadProductRunnable.isRunning()) {
            disableComponents();
        }
        if (repositoryProduct == this.downloadProductRunnable.getProductToDownload()) {
            this.downloadStatusLabel.updateDownloadingPercent(progressProgressStatus, this.sizeLabel.getForeground());
            return true;
        }
        return false;
    }

    public boolean stopDownloading(DownloadProductRunnable downloadProductRunnable) {
        if (this.downloadProductRunnable == downloadProductRunnable) {
            disablePanel();
            return true;
        }
        return false;
    }

    private void stopButtonPressed() {
        this.downloadProductRunnable.cancelRunning();
        disablePanel();
    }

    private void disablePanel() {
        if (this.downloadProductRunnable.isRunning()) {
            throw new IllegalStateException("The thread to download the product is still running.");
        } else {
            disableComponents();
        }
    }

    private void disableComponents() {
        this.nameLabel.setEnabled(false);
        this.stopButton.setEnabled(false);
        this.missionLabel.setEnabled(false);
        this.acquisitionDateLabel.setEnabled(false);
        this.sizeLabel.setEnabled(false);
        this.downloadStatusLabel.setEnabled(false);
    }
}
