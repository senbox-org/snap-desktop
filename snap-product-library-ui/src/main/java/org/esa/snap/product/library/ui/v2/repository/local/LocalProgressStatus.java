package org.esa.snap.product.library.ui.v2.repository.local;

/**
 * Created by jcoravu on 27/9/2019.
 */
public class LocalProgressStatus {

    public static final byte PENDING_OPEN = 1;
    public static final byte OPENING = 2;
    public static final byte OPENED = 3;
    public static final byte FAIL_OPENED = 4;
    public static final byte PENDING_DELETE = 5;
    public static final byte DELETING = 6;
    public static final byte DELETED = 7;
    public static final byte FAIL_DELETED = 8;

    private byte status;

    public LocalProgressStatus(byte status) {
        this.status = status;
    }

    public boolean isPendingOpen() {
        return (this.status == PENDING_OPEN);
    }

    public boolean isOpened() {
        return (this.status == OPENED);
    }

    public boolean isOpening() {
        return (this.status == OPENING);
    }

    public boolean isFailOpened() {
        return (this.status == FAIL_OPENED);
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public boolean isPendingDelete() {
        return (this.status == PENDING_DELETE);
    }

    public boolean isDeleting() {
        return (this.status == DELETING);
    }

    public boolean isDeleted() {
        return (this.status == DELETED);
    }

    public boolean isFailDeleted() {
        return (this.status == FAIL_DELETED);
    }
}
