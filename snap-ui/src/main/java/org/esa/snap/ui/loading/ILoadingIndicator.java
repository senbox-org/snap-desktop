package org.esa.snap.ui.loading;

/**
 * Created by jcoravu on 31/12/2018.
 */
public interface ILoadingIndicator {

    public boolean isRunning(int threadId);

    public boolean onDisplay(int threadId, String messageToDisplay);

    public boolean onHide(int threadId);
}
