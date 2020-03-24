package org.esa.snap.product.library.ui.v2.repository.remote;

import javax.swing.*;
import java.awt.*;

/**
 * Created by jcoravu on 11/2/2020.
 */
public class RemoteProductStatusLabel extends JLabel {

    public RemoteProductStatusLabel() {
        super("");
    }

    public void updateDownloadingPercent(DownloadProgressStatus downloadProgressStatus, Color defaultForegroundColor) {
        Color foregroundColor = defaultForegroundColor;
        String percentText = " "; // set an empty space for the default text
        if (downloadProgressStatus != null) {
            // the product is pending download or downloading
            if (downloadProgressStatus.isDownloading()) {
                percentText = "Downloading: " + Integer.toString(downloadProgressStatus.getValue()) + "%";
            } else if (downloadProgressStatus.isPendingDownload()) {
                percentText = "Pending download";
            } else if (downloadProgressStatus.isCancelDownloading()) {
                percentText = "Downloading: " + Integer.toString(downloadProgressStatus.getValue()) + "% (stopped)";
            } else if (downloadProgressStatus.isDownloaded()) {
                percentText = "Downloaded";
                foregroundColor = Color.GREEN;
            } else if (downloadProgressStatus.isSaved()) {
                percentText = "Downloaded";
                foregroundColor = Color.GREEN;
            } else if (downloadProgressStatus.isNotAvailable()) {
                percentText = "Not available to download";
                foregroundColor = Color.RED;
            } else if (downloadProgressStatus.isFailedDownload()) {
                percentText = "Downloading: " + Integer.toString(downloadProgressStatus.getValue()) + "% (failed)";
                foregroundColor = Color.RED;
            } else if (downloadProgressStatus.isFailedOpen()) {
                percentText = "Downloaded (failed open)";
                foregroundColor = Color.GREEN;
            } else if (downloadProgressStatus.isFailOpenedBecauseNoProductReader()) {
                percentText = "Downloaded (failed open because no product reader found)";
                foregroundColor = Color.GREEN;
            } else if (downloadProgressStatus.isPendingOpen()) {
                percentText = "Downloaded (pending open)";
                foregroundColor = Color.GREEN;
            } else if (downloadProgressStatus.isOpening()) {
                percentText = "Downloaded (opening)";
                foregroundColor = Color.GREEN;
            } else if (downloadProgressStatus.isOpened()) {
                percentText = "Downloaded (opened)";
                foregroundColor = Color.GREEN;
            } else {
                throw new IllegalStateException("The percent progress status is unknown. The value is " + downloadProgressStatus.getValue()+".");
            }
        }
        setForeground(foregroundColor);
        setText(percentText);
    }
}
