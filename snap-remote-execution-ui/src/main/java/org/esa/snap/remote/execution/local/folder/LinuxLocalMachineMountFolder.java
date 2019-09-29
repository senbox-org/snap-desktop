package org.esa.snap.remote.execution.local.folder;

import org.esa.snap.remote.execution.utils.CommandExecutorUtils;
import org.esa.snap.remote.execution.utils.UnixMountLocalFolderResult;
import org.esa.snap.ui.loading.ILoadingIndicator;

import java.io.IOException;

/**
 * Created by jcoravu on 1/3/2019.
 */
public class LinuxLocalMachineMountFolder implements IMountLocalSharedFolderResult {

    private final LinuxLocalSharedFolder linuxLocalSharedDrive;
    private final UnixMountLocalFolderResult localMachineLinuxMountFolder;

    public LinuxLocalMachineMountFolder(LinuxLocalSharedFolder linuxLocalSharedDrive, UnixMountLocalFolderResult localMachineLinuxMountFolder) {
        this.linuxLocalSharedDrive = linuxLocalSharedDrive;
        this.localMachineLinuxMountFolder = localMachineLinuxMountFolder;
    }

    @Override
    public LinuxLocalSharedFolder getLocalSharedDrive() {
        return this.linuxLocalSharedDrive;
    }

    @Override
    public void unmountLocalSharedFolderAsync(ILoadingIndicator loadingIndicator, int threadId, IUnmountLocalSharedFolderCallback callback) {
        if (canUnmountLocalSharedFolder()) {
            UnmountLinuxLocalFolderTimerRunnable runnable = new UnmountLinuxLocalFolderTimerRunnable(loadingIndicator, threadId, this.linuxLocalSharedDrive.getLocalSharedFolderPath(),
                                                                                            this.linuxLocalSharedDrive.getLocalPassword(), this.localMachineLinuxMountFolder, callback);
            runnable.executeAsync();
        } else {
            callback.onFinishUnmountingLocalFolder(null);
        }
    }

    @Override
    public void unmountLocalSharedFolder(String currentLocalSharedFolderPath, String currentLocalPassword) throws IOException {
        if (canUnmountLocalSharedFolder()) {
            CommandExecutorUtils.unmountLinuxLocalFolder(this.linuxLocalSharedDrive.getLocalSharedFolderPath(),
                                                this.linuxLocalSharedDrive.getLocalPassword(), this.localMachineLinuxMountFolder);
        }
    }

    private boolean canUnmountLocalSharedFolder() {
        return (this.localMachineLinuxMountFolder.isSharedFolderCreated() || this.localMachineLinuxMountFolder.isSharedFolderMounted());
    }
}
