package org.esa.snap.product.library.ui.v2.repository.remote.download.popup;

import org.esa.snap.product.library.ui.v2.repository.remote.DownloadProgressStatus;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductStatusLabel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteRepositoryProductPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadProductRunnable;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The panel to show the data of a downloading product.
 *
 * Created by jcoravu on 11/2/2020.
 */
public class DownloadingProductPanel extends JPanel {

    private final JButton stopButton;
    private final JLabel nameLabel;
    private final JLabel acquisitionDateLabel;
    private final JLabel sizeLabel;
    private final JLabel repositoryLabel;
    private final JLabel missionLabel;
    private final RemoteProductStatusLabel downloadStatusLabel;
    private final DownloadProductRunnable downloadProductRunnable;
    private final DownloadProgressStatus downloadProgressStatus;

    public DownloadingProductPanel(DownloadProductRunnable downloadProductRunnable, DownloadProgressStatus downloadProgressStatus, int gapBetweenColumns) {
        super(new GridBagLayout());

        if (downloadProductRunnable == null) {
            throw new NullPointerException("The download runnable is null.");
        }
        if (downloadProgressStatus == null) {
            throw new NullPointerException("The download progress status is null.");
        }

        this.downloadProductRunnable = downloadProductRunnable;
        this.downloadProgressStatus = downloadProgressStatus;

        setOpaque(false);

        RepositoryProduct productToDownload = this.downloadProductRunnable.getProductToDownload();

        this.nameLabel = new JLabel(productToDownload.getName());
        this.acquisitionDateLabel = new JLabel(RemoteRepositoryProductPanel.buildAcquisitionDateLabelText(productToDownload.getAcquisitionDate()));
        this.repositoryLabel = new JLabel(RemoteRepositoryProductPanel.buildRepositoryLabelText(productToDownload.getRemoteMission().getRepositoryName()));
        this.missionLabel = new JLabel(RemoteRepositoryProductPanel.buildMissionLabelText(productToDownload.getRemoteMission().getName()));
        this.sizeLabel = new JLabel(RemoteRepositoryProductPanel.buildSizeLabelText(productToDownload.getApproximateSize()));
        this.downloadStatusLabel = new RemoteProductStatusLabel();

        int stopButtonSize = (int)(1.5f *  this.nameLabel.getPreferredSize().height);
        Dimension buttonSize = new Dimension(stopButtonSize, stopButtonSize);
        this.stopButton = SwingUtils.buildButton("/org/esa/snap/product/library/ui/v2/icons/stop20.gif", null, buttonSize, 1);
        this.stopButton.setToolTipText("Stop downloading the product");
        this.stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                stopButtonPressed();
            }
        });

        int gapBetweenRows = 0;
        int number = 3;

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 2, 1, 0, 0);
        add(this.nameLabel, c);
        c = SwingUtils.buildConstraints(2, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 4, gapBetweenRows, 30 * gapBetweenColumns);
        add(this.stopButton, c);

        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        add(this.repositoryLabel, c);
        c = SwingUtils.buildConstraints(1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, number * gapBetweenColumns);
        add(this.missionLabel, c);

        c = SwingUtils.buildConstraints(0, 2, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        add(this.acquisitionDateLabel, c);

        c = SwingUtils.buildConstraints(0, 3, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        add(this.sizeLabel, c);
        c = SwingUtils.buildConstraints(1, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 2, 1, gapBetweenRows, number * gapBetweenColumns);
        add(this.downloadStatusLabel, c);

        refreshDownloadStatus();
    }

    public void refreshDownloadStatus() {
        if (this.downloadProductRunnable.isFinished()) {
            disableComponents();
        }
        this.downloadStatusLabel.updateDownloadingPercent(this.downloadProgressStatus, this.sizeLabel.getForeground());
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
        if (this.downloadProductRunnable.isFinished()) {
            disableComponents();
        } else {
            throw new IllegalStateException("The thread to download the product has not finished yet.");
        }
    }

    private void disableComponents() {
        this.nameLabel.setEnabled(false);
        this.stopButton.setEnabled(false);
        this.repositoryLabel.setEnabled(false);
        this.missionLabel.setEnabled(false);
        this.acquisitionDateLabel.setEnabled(false);
        this.sizeLabel.setEnabled(false);
        this.downloadStatusLabel.setEnabled(false);
    }
}
