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

    public void updateDownloadingPercent(DownloadProgressStatus progressPercent, Color defaultForegroundColor) {
        Color foregroundColor = defaultForegroundColor;
        String percentText = " "; // set an empty space for the default text
        if (progressPercent != null) {
            // the product is pending download or downloading
            if (progressPercent.isDownloading()) {
                percentText = "Downloading: " + Integer.toString(progressPercent.getValue()) + "%";
            } else if (progressPercent.isPendingDownload()) {
                percentText = "Pending download";
            } else if (progressPercent.isStoppedDownload()) {
                percentText = "Downloading: " + Integer.toString(progressPercent.getValue()) + "% (stopped)";
            } else if (progressPercent.isDownloaded()) {
                percentText = "Downloaded";
                foregroundColor = Color.GREEN;
            } else if (progressPercent.isNotAvailable()) {
                percentText = "Not available to download";
                foregroundColor = Color.RED;
            } else if (progressPercent.isFailedDownload()) {
                percentText = "Downloading: " + Integer.toString(progressPercent.getValue()) + "% (failed)";
                foregroundColor = Color.RED;
            } else if (progressPercent.isFailedOpen()) {
                percentText = "Downloaded (failed open)";
                foregroundColor = Color.GREEN;
            } else if (progressPercent.isFailOpenedBecauseNoProductReader()) {
                percentText = "Downloaded (failed open because no product reader found)";
                foregroundColor = Color.GREEN;
            } else if (progressPercent.isPendingOpen()) {
                percentText = "Downloaded (pending open)";
                foregroundColor = Color.GREEN;
            } else if (progressPercent.isOpening()) {
                percentText = "Downloaded (opening)";
                foregroundColor = Color.GREEN;
            } else if (progressPercent.isOpened()) {
                percentText = "Downloaded (opened)";
                foregroundColor = Color.GREEN;
            } else {
                throw new IllegalStateException("The percent progress status is unknown. The value is " + progressPercent.getValue()+".");
            }
        }
        setForeground(foregroundColor);
        setText(percentText);
    }
}
