package org.esa.snap.remote.execution.topology;

import org.esa.snap.ui.loading.AbstractTimerRunnable;
import org.esa.snap.ui.loading.LoadingIndicator;
import org.esa.snap.ui.loading.MessageDialog;

import java.nio.file.Path;

/**
 * Created by jcoravu on 19/12/2018.
 */
public class ReadRemoteTopologyTimerRunnable extends AbstractTimerRunnable<RemoteTopology> {

    private final MessageDialog parentWindow;
    private final Path remoteTopologyFilePath;

    public ReadRemoteTopologyTimerRunnable(MessageDialog parentWindow, LoadingIndicator loadingIndicator, int threadId, Path remoteTopologyFilePath) {
        super(loadingIndicator, threadId, 500);

        this.parentWindow = parentWindow;
        this.remoteTopologyFilePath = remoteTopologyFilePath;
    }

    @Override
    protected final void onTimerWakeUp(String messageToDisplay) {
        super.onTimerWakeUp("Loading...");
    }

    @Override
    protected RemoteTopology execute() throws Exception {
        return RemoteTopologyUtils.readTopology(this.remoteTopologyFilePath);
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to read the remote topology from the file.";
    }

    @Override
    protected final void onFailed(Exception exception) {
        this.parentWindow.showErrorDialog("Failed to read the remote topology from the file.", "Failed");
    }
}
