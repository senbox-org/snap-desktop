package org.esa.snap.remote.execution.local.folder;

import org.esa.snap.remote.execution.utils.CommandExecutorUtils;
import org.esa.snap.ui.loading.AbstractTimerRunnable;
import org.esa.snap.ui.loading.ILoadingIndicator;

public class UnmountWindowsLocalDriveTimerRunnable extends AbstractTimerRunnable<Void> {

    private final String localSharedDrive;
    private final IUnmountLocalSharedFolderCallback callback;

    public UnmountWindowsLocalDriveTimerRunnable(ILoadingIndicator loadingIndicator, int threadId, String localSharedDrive, IUnmountLocalSharedFolderCallback callback) {
        super(loadingIndicator, threadId, 500);

        this.localSharedDrive = localSharedDrive;
        this.callback = callback;
    }

    @Override
    protected final void onTimerWakeUp(String messageToDisplay) {
        super.onTimerWakeUp("Unmounting local shared drive...");
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to unmount the local shared drive '"+this.localSharedDrive +"'.";
    }

    @Override
    protected void onSuccessfullyFinish(Void result) {
        this.callback.onFinishUnmountingLocalFolder(null);
    }

    @Override
    protected void onFailed(Exception exception) {
        this.callback.onFinishUnmountingLocalFolder(exception);
    }

    @Override
    protected Void execute() throws Exception {
        CommandExecutorUtils.unmountWindowsLocalDrive(this.localSharedDrive);
        return null;
    }
}
