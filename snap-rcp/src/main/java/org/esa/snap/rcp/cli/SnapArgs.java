package org.esa.snap.rcp.cli;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * SNAP's command-line arguments.
 *
 * @author Norman Fomferra
 */
public class SnapArgs {

    public static final SnapArgs INSTANCE = new SnapArgs();

    private Path sessionFile;
    private List<Path> fileList;

    public static SnapArgs getDefault() {
        return INSTANCE;
    }

    public Path getSessionFile() {
        return sessionFile;
    }

    void setSessionFile(Path sessionFile) {
        this.sessionFile = sessionFile;
    }

    public List<Path> getFileList() {
        return fileList != null ? fileList : Collections.<Path>emptyList();
    }

    void setFileList(List<Path> fileList) {
        this.fileList = fileList;
    }
}
