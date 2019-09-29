package org.esa.snap.remote.execution.local.folder;

import org.esa.snap.remote.execution.utils.CommandExecutorUtils;
import org.esa.snap.remote.execution.utils.UnixMountLocalFolderResult;
import org.esa.snap.ui.loading.AbstractTimerRunnable;
import org.esa.snap.ui.loading.ILoadingIndicator;

public class UnmountMacLocalFolderTimerRunnable extends AbstractTimerRunnable<Void> {

    protected final String localSharedFolderPath;
    protected final UnixMountLocalFolderResult localMachineLinuxMountFolder;

    private final IUnmountLocalSharedFolderCallback callback;

    public UnmountMacLocalFolderTimerRunnable(ILoadingIndicator loadingIndicator, int threadId, String localSharedFolderPath,
                                              UnixMountLocalFolderResult localMachineLinuxMountFolder, IUnmountLocalSharedFolderCallback callback) {

        super(loadingIndicator, threadId, 500);

        this.localSharedFolderPath = localSharedFolderPath;
        this.localMachineLinuxMountFolder = localMachineLinuxMountFolder;
        this.callback = callback;
    }

    @Override
    protected final void onTimerWakeUp(String messageToDisplay) {
        super.onTimerWakeUp("Unmounting local shared folder...");
    }

    @Override
    protected final String getExceptionLoggingMessage() {
        return "Failed to unmount the local shared folder '"+this.localSharedFolderPath+"'.";
    }

    @Override
    protected final void onSuccessfullyFinish(Void result) {
        this.callback.onFinishUnmountingLocalFolder(null);
    }

    @Override
    protected final void onFailed(Exception exception) {
        this.callback.onFinishUnmountingLocalFolder(exception);
    }

    @Override
    protected Void execute() throws Exception {
        CommandExecutorUtils.unmountMacLocalFolder(this.localSharedFolderPath, this.localMachineLinuxMountFolder);
        return null;
    }
}
