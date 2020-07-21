package org.esa.snap.product.library.ui.v2.thread;

/**
 * Created by jcoravu on 23/8/2019.
 */
public interface ProgressBarHelper {

    public boolean hideProgressPanel(int threadId);

    public boolean showProgressPanel(int threadId, String message);

    public boolean isCurrentThread(int threadId);

    public boolean updateProgressBarText(int threadId, String message);

    public boolean beginProgressBarTask(int threadId, String message, int totalWork);

    public boolean updateProgressBarValue(int threadId, int valueToAdd);
}
