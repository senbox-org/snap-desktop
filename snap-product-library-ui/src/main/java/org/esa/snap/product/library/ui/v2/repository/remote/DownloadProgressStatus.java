package org.esa.snap.product.library.ui.v2.repository.remote;

import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.nio.file.Path;

/**
 * Created by jcoravu on 13/9/2019.
 */
public class DownloadProgressStatus {

    public static final byte PENDING_DOWNLOAD = 1;
    public static final byte DOWNLOADING = 2;
    public static final byte STOP_DOWNLOADING = 3;
    public static final byte DOWNLOADED = 4;
    public static final byte FAILED_DOWNLOADING = 5;
    public static final byte NOT_AVAILABLE = 6;
    public static final byte PENDING_OPEN = 7;
    public static final byte OPENING = 8;
    public static final byte OPENED = 9;
    public static final byte FAIL_OPENED = 10;

    private short value;
    private byte status;
    private Path downloadedPath;

    public DownloadProgressStatus() {
        this.value = 0;
        this.status = PENDING_DOWNLOAD;
    }

    public short getValue() {
        return value;
    }

    public void setValue(short value) {
        if (value >=0 && value <= 100) {
            this.value = value;
            this.status = (value == 100) ? DOWNLOADED : DOWNLOADING;
        } else {
            throw new IllegalArgumentException("The progress percent value " + value + " is out of bounds.");
        }
    }

    public Path getDownloadedPath() {
        return downloadedPath;
    }

    public void setDownloadedPath(Path downloadedPath) {
        this.downloadedPath = downloadedPath;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public boolean isOpened() {
        return (this.status == OPENED);
    }

    public boolean isOpening() {
        return (this.status == OPENING);
    }

    public boolean isPendingOpen() {
        return (this.status == PENDING_OPEN);
    }

    public boolean isPendingDownload() {
        return (this.status == PENDING_DOWNLOAD);
    }

    public boolean isStoppedDownload() {
        return (this.status == STOP_DOWNLOADING);
    }

    public boolean isDownloading() {
        return (this.status == DOWNLOADING);
    }

    public boolean isDownloaded() {
        return (this.status == DOWNLOADED);
    }

    public boolean isFailedDownload() {
        return (this.status == FAILED_DOWNLOADING);
    }

    public boolean isFailedOpen() {
        return (this.status == FAIL_OPENED);
    }

    public boolean isNotAvailable() {
        return (this.status == NOT_AVAILABLE);
    }

    public boolean canOpen() {
        return (this.value == 100 && this.downloadedPath != null);
    }
}
