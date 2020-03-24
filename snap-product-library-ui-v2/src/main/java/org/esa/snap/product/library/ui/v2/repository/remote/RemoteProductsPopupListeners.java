package org.esa.snap.product.library.ui.v2.repository.remote;

import java.awt.event.ActionListener;

/**
 * Created by jcoravu on 17/3/2020.
 */
public class RemoteProductsPopupListeners {

    private final ActionListener downloadRemoteProductListener;
    private final ActionListener openDownloadedRemoteProductListener;
    private final ActionListener jointSearchCriteriaListener;

    public RemoteProductsPopupListeners(ActionListener downloadRemoteProductListener, ActionListener openDownloadedRemoteProductListener, ActionListener jointSearchCriteriaListener) {
        this.downloadRemoteProductListener = downloadRemoteProductListener;
        this.openDownloadedRemoteProductListener = openDownloadedRemoteProductListener;
        this.jointSearchCriteriaListener = jointSearchCriteriaListener;
    }

    public ActionListener getJointSearchCriteriaListener() {
        return jointSearchCriteriaListener;
    }

    public ActionListener getDownloadRemoteProductListener() {
        return downloadRemoteProductListener;
    }

    public ActionListener getOpenDownloadedRemoteProductListener() {
        return openDownloadedRemoteProductListener;
    }
}
