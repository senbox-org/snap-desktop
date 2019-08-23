package org.esa.snap.product.library.ui.v2.thread;

/**
 * Created by jcoravu on 23/8/2019.
 */
public interface IProgressPanel {

    public boolean hideProgressPanel(int threadId);

    public boolean showProgressPanel(int threadId);

    public boolean isCurrentThread(int threadId);
}
