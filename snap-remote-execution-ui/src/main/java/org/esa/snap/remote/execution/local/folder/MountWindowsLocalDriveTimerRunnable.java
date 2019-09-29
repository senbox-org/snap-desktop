package org.esa.snap.remote.execution.local.folder;

import org.esa.snap.remote.execution.utils.CommandExecutorUtils;
import org.esa.snap.ui.loading.AbstractTimerRunnable;
import org.esa.snap.ui.loading.ILoadingIndicator;
import org.esa.snap.ui.loading.IMessageDialog;

public class MountWindowsLocalDriveTimerRunnable extends AbstractTimerRunnable<Boolean> {

    private final IMessageDialog parentWindow;
    private final WindowsLocalSharedDrive windowsLocalSharedDrive;
    private final IMountLocalSharedFolderCallback callback;

    public MountWindowsLocalDriveTimerRunnable(IMessageDialog parentWindow, ILoadingIndicator loadingIndicator, int threadId,
                                               WindowsLocalSharedDrive windowsLocalSharedDrive, IMountLocalSharedFolderCallback callback) {

        super(loadingIndicator, threadId, 500);

        this.parentWindow = parentWindow;
        this.windowsLocalSharedDrive = windowsLocalSharedDrive;
        this.callback = callback;
    }

    @Override
    protected final void onTimerWakeUp(String messageToDisplay) {
        super.onTimerWakeUp("Mounting local shared drive...");
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to mount the local shared drive '"+this.windowsLocalSharedDrive.getLocalSharedDrive()+"'.";
    }

    @Override
    protected final void onFailed(Exception exception) {
        showErrorDialog();
    }

    @Override
    protected void onSuccessfullyFinish(Boolean mountedSharedDrive) {
        if (mountedSharedDrive.booleanValue()) {
            this.callback.onSuccessfullyFinishMountingLocalFolder(new WindowsLocalMachineMountDrive(this.windowsLocalSharedDrive, mountedSharedDrive));
        } else {
            showErrorDialog();
        }
    }

    @Override
    protected Boolean execute() throws Exception {
        return CommandExecutorUtils.mountWindowsLocalDrive(this.windowsLocalSharedDrive.getRemoteSharedFolderPath(), this.windowsLocalSharedDrive.getRemoteUsername(),
                                                                  this.windowsLocalSharedDrive.getRemotePassword(), this.windowsLocalSharedDrive.getLocalSharedDrive());
    }

    private void showErrorDialog() {
        this.parentWindow.showErrorDialog("Failed to mount the local shared drive '"+this.windowsLocalSharedDrive.getLocalSharedDrive()+"'.", "Failed");
    }
}
