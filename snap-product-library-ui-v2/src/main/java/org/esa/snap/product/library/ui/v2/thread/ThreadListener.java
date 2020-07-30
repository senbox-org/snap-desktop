package org.esa.snap.product.library.ui.v2.thread;

/**
 * The listener interface for receiving the event when a thread has stopped.
 *
 * Created by jcoravu on 28/8/2019.
 */
public interface ThreadListener {

    public void onStopExecuting(Runnable invokerThread);
}
