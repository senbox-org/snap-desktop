package org.esa.snap.product.library.ui.v2.thread;

/**
 * Created by jcoravu on 28/8/2019.
 */
public interface ThreadListener {

    public void onStopExecuting(Runnable invokerThread);
}
