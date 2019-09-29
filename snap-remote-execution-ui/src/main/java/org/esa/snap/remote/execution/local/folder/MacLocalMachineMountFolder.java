package org.esa.snap.remote.execution.local.folder;

import org.esa.snap.remote.execution.utils.CommandExecutorUtils;
import org.esa.snap.remote.execution.utils.UnixMountLocalFolderResult;
import org.esa.snap.ui.loading.ILoadingIndicator;

import java.io.IOException;

/**
 * Created by jcoravu on 1/3/2019.
 */
public class MacLocalMachineMountFolder implements IMountLocalSharedFolderResult {

    private final MacLocalSharedFolder macLocalSharedDrive;
    private final UnixMountLocalFolderResult localMachineLinuxMountFolder;

    public MacLocalMachineMountFolder(MacLocalSharedFolder macLocalSharedDrive, UnixMountLocalFolderResult localMachineLinuxMountFolder) {
        this.macLocalSharedDrive = macLocalSharedDrive;
        this.localMachineLinuxMountFolder = localMachineLinuxMountFolder;
    }

    @Override
    public MacLocalSharedFolder getLocalSharedDrive() {
        return this.macLocalSharedDrive;
    }

    @Override
    public void unmountLocalSharedFolderAsync(ILoadingIndicator loadingIndicator, int threadId, IUnmountLocalSharedFolderCallback callback) {
        if (canUnmountLocalSharedFolder()) {
            UnmountMacLocalFolderTimerRunnable runnable = new UnmountMacLocalFolderTimerRunnable(loadingIndicator, threadId, this.macLocalSharedDrive.getLocalSharedFolderPath(),
                                                                                                 this.localMachineLinuxMountFolder, callback);
            runnable.executeAsync();
        } else {
            callback.onFinishUnmountingLocalFolder(null);
        }
    }

    @Override
    public void unmountLocalSharedFolder(String currentLocalSharedFolderPath, String currentLocalPassword) throws IOException {
        if (canUnmountLocalSharedFolder()) {
            CommandExecutorUtils.unmountMacLocalFolder(this.macLocalSharedDrive.getLocalSharedFolderPath(), this.localMachineLinuxMountFolder);
        }
    }

    private boolean canUnmountLocalSharedFolder() {
        return (this.localMachineLinuxMountFolder.isSharedFolderCreated() || this.localMachineLinuxMountFolder.isSharedFolderMounted());
    }
}
