package org.esa.snap.product.library.ui.v2.repository.local;

import java.awt.event.ActionListener;

/**
 * Created by jcoravu on 2/10/2019.
 */
public class LocalProductsPopupListeners {

    private final ActionListener openProductListener;
    private final ActionListener deleteProductListener;
    private final ActionListener batchProcessingListener;
    private final ActionListener showInExplorerListener;

    private ActionListener selectAllListener;
    private ActionListener selectNoneListener;
    private ActionListener copyListener;
    private ActionListener copyToListener;
    private ActionListener moveToListener;
    private ActionListener exportListListener;
    private ActionListener jointSearchCriteriaListener;

    public LocalProductsPopupListeners(ActionListener openProductListener, ActionListener deleteProductListener, ActionListener batchProcessingListener, ActionListener showInExplorerListener) {
        this.openProductListener = openProductListener;
        this.deleteProductListener = deleteProductListener;
        this.batchProcessingListener = batchProcessingListener;
        this.showInExplorerListener = showInExplorerListener;
    }

    public void setJointSearchCriteriaListener(ActionListener jointSearchCriteriaListener) {
        this.jointSearchCriteriaListener = jointSearchCriteriaListener;
    }

    public ActionListener getJointSearchCriteriaListener() {
        return jointSearchCriteriaListener;
    }

    public ActionListener getBatchProcessingListener() {
        return batchProcessingListener;
    }

    public ActionListener getDeleteProductListener() {
        return deleteProductListener;
    }

    public ActionListener getOpenProductListener() {
        return openProductListener;
    }

    public ActionListener getShowInExplorerListener() {
        return showInExplorerListener;
    }

    public ActionListener getSelectAllListener() {
        return selectAllListener;
    }

    public void setSelectAllListener(ActionListener selectAllListener) {
        this.selectAllListener = selectAllListener;
    }

    public ActionListener getSelectNoneListener() {
        return selectNoneListener;
    }

    public void setSelectNoneListener(ActionListener selectNoneListener) {
        this.selectNoneListener = selectNoneListener;
    }

    public ActionListener getCopyListener() {
        return copyListener;
    }

    public void setCopyListener(ActionListener copyListener) {
        this.copyListener = copyListener;
    }

    public ActionListener getCopyToListener() {
        return copyToListener;
    }

    public void setCopyToListener(ActionListener copyToListener) {
        this.copyToListener = copyToListener;
    }

    public ActionListener getMoveToListener() {
        return moveToListener;
    }

    public void setMoveToListener(ActionListener moveToListener) {
        this.moveToListener = moveToListener;
    }

    public ActionListener getExportListListener() {
        return exportListListener;
    }

    public void setExportListListener(ActionListener exportListListener) {
        this.exportListListener = exportListListener;
    }
}
