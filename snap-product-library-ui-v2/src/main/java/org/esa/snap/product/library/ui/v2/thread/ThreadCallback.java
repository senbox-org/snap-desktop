package org.esa.snap.product.library.ui.v2.thread;

/**
 * The listener interface for receiving events when a thread has finished.
 *
 * Created by jcoravu on 28/8/2019.
 */
public interface ThreadCallback<OutputType> {

    public void onFailed(Exception exception);

    public void onSuccessfullyFinish(OutputType productEntries);
}
