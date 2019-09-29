package org.esa.snap.remote.execution.local.folder;

/**
 * Created by jcoravu on 23/5/2019.
 */
public abstract class AbstractLocalSharedFolder {

    private final String remoteSharedFolderPath;
    private final String remoteUsername;
    private final String remotePassword;

    protected AbstractLocalSharedFolder(String remoteSharedFolderPath, String remoteUsername, String remotePassword) {
        this.remoteSharedFolderPath = remoteSharedFolderPath;
        this.remoteUsername = remoteUsername;
        this.remotePassword = remotePassword;
    }

    public String getRemoteSharedFolderPath() {
        return remoteSharedFolderPath;
    }

    public String getRemotePassword() {
        return remotePassword;
    }

    public String getRemoteUsername() {
        return remoteUsername;
    }

    public boolean hasChangedParameters(AbstractLocalSharedFolder newLocalSharedFolder) {
        if (this.remoteSharedFolderPath.equals(newLocalSharedFolder.getRemoteSharedFolderPath())) {
            if (this.remoteUsername.equals(newLocalSharedFolder.getRemoteUsername())) {
                if (this.remotePassword.equals(newLocalSharedFolder.getRemotePassword())) {
                    return false;
                }
            }
        }
        return true; // the parameters has changed
    }
}
