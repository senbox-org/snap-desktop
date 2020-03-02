package org.esa.snap.remote.execution.local.folder;

import org.esa.snap.remote.execution.utils.CommandExecutorUtils;
import org.esa.snap.remote.execution.utils.UnixMountLocalFolderResult;
import org.esa.snap.ui.loading.LoadingIndicator;

public class UnmountLinuxLocalFolderTimerRunnable extends UnmountMacLocalFolderTimerRunnable {

    private final String localPassword;

    public UnmountLinuxLocalFolderTimerRunnable(LoadingIndicator loadingIndicator, int threadId, String localSharedFolderPath,
                                                String localPassword, UnixMountLocalFolderResult localMachineLinuxMountFolder, IUnmountLocalSharedFolderCallback callback) {

        super(loadingIndicator, threadId, localSharedFolderPath, localMachineLinuxMountFolder, callback);

        this.localPassword = localPassword;
    }

    @Override
    protected Void execute() throws Exception {
        CommandExecutorUtils.unmountLinuxLocalFolder(this.localSharedFolderPath, this.localPassword, this.localMachineLinuxMountFolder);
        return null;
    }
}
