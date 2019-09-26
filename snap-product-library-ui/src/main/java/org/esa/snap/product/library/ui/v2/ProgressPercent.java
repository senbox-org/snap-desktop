package org.esa.snap.product.library.ui.v2;

/**
 * Created by jcoravu on 13/9/2019.
 */
public class ProgressPercent {

    public static final byte PENDING_DOWNLOAD = 1;
    public static final byte DOWNLOADING = 2;
    public static final byte STOP_DOWNLOADING = 3;
    public static final byte DOWNLOADED = 4;
    public static final byte FAILED_DOWNLOADING = 5;

    private short value;
    private byte status;

    public ProgressPercent() {
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

    public void setStopDownloading() {
        this.status = STOP_DOWNLOADING;
    }

    public void setFailedDownloading() {
        this.status = FAILED_DOWNLOADING;
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
}
