
package org.esa.snap.remote.execution.local.folder;

import org.esa.snap.remote.execution.utils.CommandExecutorUtils;
import org.esa.snap.remote.execution.utils.UnixMountLocalFolderResult;
import org.esa.snap.ui.loading.AbstractTimerRunnable;
import org.esa.snap.ui.loading.ILoadingIndicator;
import org.esa.snap.ui.loading.IMessageDialog;

public class MountMacLocalFolderTimerRunnable extends AbstractTimerRunnable<UnixMountLocalFolderResult> {

    private final IMessageDialog parentWindow;

    protected final MacLocalSharedFolder macLocalSharedDrive;
    protected final IMountLocalSharedFolderCallback callback;

    public MountMacLocalFolderTimerRunnable(IMessageDialog parentWindow, ILoadingIndicator loadingIndicator, int threadId,
                                            MacLocalSharedFolder macLocalSharedDrive, IMountLocalSharedFolderCallback callback) {

        super(loadingIndicator, threadId, 500);

        this.parentWindow = parentWindow;
        this.macLocalSharedDrive = macLocalSharedDrive;
        this.callback = callback;
    }

    @Override
    protected final void onTimerWakeUp(String messageToDisplay) {
        super.onTimerWakeUp("Mounting local shared folder...");
    }

    @Override
    protected final String getExceptionLoggingMessage() {
        return "Failed to mount the local shared folder '"+this.macLocalSharedDrive.getLocalSharedFolderPath()+"'.";
    }

    @Override
    protected final void onFailed(Exception exception) {
        showErrorDialog();
    }

    @Override
    protected void onSuccessfullyFinish(UnixMountLocalFolderResult result) {
        if (result.isSharedFolderMounted()) {
            this.callback.onSuccessfullyFinishMountingLocalFolder(new MacLocalMachineMountFolder(this.macLocalSharedDrive, result));
        } else {
            showErrorDialog();
        }
    }

    @Override
    protected UnixMountLocalFolderResult execute() throws Exception {
        return CommandExecutorUtils.mountMacLocalFolder(this.macLocalSharedDrive.getRemoteSharedFolderPath(),
                                                               this.macLocalSharedDrive.getRemoteUsername(), this.macLocalSharedDrive.getRemotePassword(),
                                                               this.macLocalSharedDrive.getLocalSharedFolderPath());
    }

    protected final void showErrorDialog() {
        this.parentWindow.showErrorDialog("Failed to mount the local shared folder '"+this.macLocalSharedDrive.getLocalSharedFolderPath()+"'.", "Failed");
    }
}
