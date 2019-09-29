package org.esa.snap.remote.execution.local.folder;

import org.apache.commons.lang.StringUtils;
import org.esa.snap.remote.execution.utils.CommandExecutorUtils;
import org.esa.snap.ui.loading.ILoadingIndicator;

import java.io.IOException;

/**
 * Created by jcoravu on 1/3/2019.
 */
public class WindowsLocalMachineMountDrive implements IMountLocalSharedFolderResult {

    private final WindowsLocalSharedDrive windowsLocalSharedDrive;
    private final boolean mountedSharedDrive;

    public WindowsLocalMachineMountDrive(WindowsLocalSharedDrive windowsLocalSharedDrive, boolean mountedSharedDrive) {
        this.windowsLocalSharedDrive = windowsLocalSharedDrive;
        this.mountedSharedDrive = mountedSharedDrive;
    }

    @Override
    public void unmountLocalSharedFolderAsync(ILoadingIndicator loadingIndicator, int threadId, IUnmountLocalSharedFolderCallback callback) {
        if (canUnmountLocalSharedDrive()) {
            UnmountWindowsLocalDriveTimerRunnable runnable = new UnmountWindowsLocalDriveTimerRunnable(loadingIndicator, threadId, this.windowsLocalSharedDrive.getLocalSharedDrive(), callback);
            runnable.executeAsync();
        } else {
            callback.onFinishUnmountingLocalFolder(null);
        }
    }

    @Override
    public void unmountLocalSharedFolder(String currentLocalSharedDrive, String currentLocalPassword) throws IOException {
        if (canUnmountLocalSharedDrive()) {
            CommandExecutorUtils.unmountWindowsLocalDrive(this.windowsLocalSharedDrive.getLocalSharedDrive());
        }
    }

    @Override
    public WindowsLocalSharedDrive getLocalSharedDrive() {
        return windowsLocalSharedDrive;
    }

    private boolean canUnmountLocalSharedDrive() {
        return (!StringUtils.isBlank(this.windowsLocalSharedDrive.getLocalSharedDrive()) && this.mountedSharedDrive);
    }
}
