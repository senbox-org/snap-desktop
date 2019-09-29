package org.esa.snap.remote.execution.local.folder;

import org.esa.snap.remote.execution.utils.CommandExecutorUtils;
import org.esa.snap.remote.execution.utils.UnixMountLocalFolderResult;
import org.esa.snap.ui.loading.ILoadingIndicator;
import org.esa.snap.ui.loading.IMessageDialog;

public class MountLinuxLocalFolderTimerRunnable extends MountMacLocalFolderTimerRunnable {

    public MountLinuxLocalFolderTimerRunnable(IMessageDialog parentWindow, ILoadingIndicator loadingIndicator, int threadId,
                                              LinuxLocalSharedFolder linuxLocalSharedDrive, IMountLocalSharedFolderCallback callback) {

        super(parentWindow, loadingIndicator, threadId, linuxLocalSharedDrive, callback);
    }

    @Override
    protected void onSuccessfullyFinish(UnixMountLocalFolderResult result) {
        if (result.isSharedFolderMounted()) {
            LinuxLocalSharedFolder linuxLocalSharedDrive = (LinuxLocalSharedFolder)this.macLocalSharedDrive;
            LinuxLocalMachineMountFolder mountFolder = new LinuxLocalMachineMountFolder(linuxLocalSharedDrive, result);
            this.callback.onSuccessfullyFinishMountingLocalFolder(mountFolder);
        } else {
            showErrorDialog();
        }
    }

    @Override
    protected UnixMountLocalFolderResult execute() throws Exception {
        LinuxLocalSharedFolder linuxLocalSharedDrive = (LinuxLocalSharedFolder)this.macLocalSharedDrive;
        return CommandExecutorUtils.mountLinuxLocalFolder(linuxLocalSharedDrive.getRemoteSharedFolderPath(), linuxLocalSharedDrive.getRemoteUsername(),
                                                                 linuxLocalSharedDrive.getRemotePassword(), linuxLocalSharedDrive.getLocalSharedFolderPath(),
                                                                 linuxLocalSharedDrive.getLocalPassword());
    }
}
