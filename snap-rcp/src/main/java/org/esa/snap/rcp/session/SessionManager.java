package org.esa.snap.rcp.session;

import org.esa.snap.util.SystemUtils;
import org.esa.snap.util.io.SnapFileFilter;

import java.io.File;

/**
 * Session Manager handling one and only active session file
 * @author Muhammad
 */
class SessionManager  {
    private static SessionManager instance = new SessionManager();

    private File sessionFile;

    public static SessionManager getDefault() {
        return instance;
    }

    public SnapFileFilter getSessionFileFilter() {
        return new SnapFileFilter("SESSION",
                                  new String[]{String.format(".%s", SystemUtils.getApplicationContextId()), ".snap"},
                                  String.format("%s Session file", SystemUtils.getApplicationName()));
    }

    public File getSessionFile() {
        return sessionFile;
    }

    public void setSessionFile(File sessionFile) {
        this.sessionFile = sessionFile;
    }

}
