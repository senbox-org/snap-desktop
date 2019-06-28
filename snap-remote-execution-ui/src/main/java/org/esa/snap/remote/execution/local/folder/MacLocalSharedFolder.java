package org.esa.snap.remote.execution.local.folder;

/**
 * Created by jcoravu on 23/5/2019.
 */
public class MacLocalSharedFolder extends AbstractLocalSharedFolder {

    private final String localSharedFolderPath;

    public MacLocalSharedFolder(String remoteSharedFolderPath, String remoteUsername, String remotePassword, String localSharedFolderPath) {
        super(remoteSharedFolderPath, remoteUsername, remotePassword);

        this.localSharedFolderPath = localSharedFolderPath;
    }

    @Override
    public boolean hasChangedParameters(AbstractLocalSharedFolder newLocalSharedFolder) {
        boolean changedParameters = super.hasChangedParameters(newLocalSharedFolder);

        if (!changedParameters) {
            MacLocalSharedFolder newMacLocalSharedDrive = (MacLocalSharedFolder)newLocalSharedFolder;
            changedParameters = !this.localSharedFolderPath.equals(newMacLocalSharedDrive.getLocalSharedFolderPath());
        }
        return changedParameters;
    }

    public String getLocalSharedFolderPath() {
        return localSharedFolderPath;
    }
}
