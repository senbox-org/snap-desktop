package org.esa.snap.product.library.ui.v2.repository.local;

/**
 * Created by jcoravu on 27/9/2019.
 */
public class OpenProgressStatus {

    public static final byte PENDING = 1;
    public static final byte OPENING = 2;
    public static final byte OPENED = 3;
    public static final byte FAILED = 4;

    private byte status;

    public OpenProgressStatus() {
        this.status = PENDING;
    }

    public boolean isPending() {
        return (this.status == PENDING);
    }

    public boolean isOpened() {
        return (this.status == OPENED);
    }

    public boolean isOpening() {
        return (this.status == OPENING);
    }

    public boolean isFailed() {
        return (this.status == FAILED);
    }

    public void setOpened() {
        this.status = OPENED;
    }

    public void setOpening() {
        this.status = OPENING;
    }

    public void setFailed() {
        this.status = FAILED;
    }

    public void setStatus(byte status) {
        this.status = status;
    }
}
