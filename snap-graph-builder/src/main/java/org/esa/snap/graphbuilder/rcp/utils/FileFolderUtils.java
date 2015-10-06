package org.esa.snap.graphbuilder.rcp.utils;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.file.SaveProductAsAction;
import org.esa.snap.ui.SnapFileChooser;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import java.io.File;

/**
 * To be removed
 */
public class FileFolderUtils {

    public static File GetFilePath(final String title, final String formatName, final String extension,
                                   final String fileName, final String description, final boolean isSave) {
        return GetFilePath(title, formatName, extension, fileName, description, isSave,
                           SaveProductAsAction.PREFERENCES_KEY_LAST_PRODUCT_DIR,
                           FileSystemView.getFileSystemView().getRoots()[0].getAbsolutePath());
    }

    public static File GetSaveFilePath(final String title, final String formatName, final String extension,
                                       final String fileName, final String description) {
        return GetFilePath(title, formatName, extension, fileName, description, true,
                           SaveProductAsAction.PREFERENCES_KEY_LAST_PRODUCT_DIR,
                           FileSystemView.getFileSystemView().getRoots()[0].getAbsolutePath());
    }

    public static File GetFilePath(final String title, final String formatName, final String extension,
                                   final String fileName, final String description, final boolean isSave,
                                   final String lastDirPropertyKey, final String defaultPath) {
        SnapFileFilter fileFilter = null;
        if (!extension.isEmpty()) {
            fileFilter = new SnapFileFilter(formatName, extension, description);
        }
        File file = null;
        if (isSave) {
            final String lastDir = SnapApp.getDefault().getPreferences().get(
                    lastDirPropertyKey, SystemUtils.getUserHomeDir().getPath());
            final File currentDir = new File(lastDir);

            final SnapFileChooser fileChooser = new SnapFileChooser();
            fileChooser.setCurrentDirectory(currentDir);
            fileChooser.addChoosableFileFilter(new SnapFileFilter(formatName, extension, description));
            fileChooser.setAcceptAllFileFilterUsed(false);

            fileChooser.setDialogTitle("Save");
            fileChooser.setCurrentFilename(fileName);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            final int result = fileChooser.showSaveDialog(SnapApp.getDefault().getMainFrame());
            if (result == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.getSelectedFile();

                final File currentDirectory = fileChooser.getCurrentDirectory();
                if (currentDirectory != null) {
                    SnapApp.getDefault().getPreferences().put(
                            lastDirPropertyKey, currentDirectory.getPath());
                }
            }
        } else {
            String lastDir = SnapApp.getDefault().getPreferences().get(lastDirPropertyKey, defaultPath);
            if (fileName == null) {
                file = showFileOpenDialog(title, false, fileFilter, lastDir, lastDirPropertyKey);
            } else {
                file = showFileOpenDialog(title, false, fileFilter, fileName, lastDirPropertyKey);
            }
        }

        return file == null ? null : FileUtils.ensureExtension(file, extension);
    }

    /**
     * allows the choice of picking directories only
     *
     * @param title
     * @param dirsOnly
     * @param fileFilter
     * @param currentDir
     * @param lastDirPropertyKey
     * @return
     */
    private static File showFileOpenDialog(String title,
                                           boolean dirsOnly,
                                           FileFilter fileFilter,
                                           String currentDir,
                                           String lastDirPropertyKey) {
        final SnapFileChooser fileChooser = new SnapFileChooser();
        fileChooser.setCurrentDirectory(new File(currentDir));
        if (fileFilter != null) {
            fileChooser.setFileFilter(fileFilter);
        }
        fileChooser.setDialogTitle(title);
        fileChooser.setFileSelectionMode(dirsOnly ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(SnapApp.getDefault().getMainFrame());
        if (fileChooser.getCurrentDirectory() != null) {
            final String lastDirPath = fileChooser.getCurrentDirectory().getAbsolutePath();
            if (lastDirPath != null) {
                SnapApp.getDefault().getPreferences().put(lastDirPropertyKey, lastDirPath);
            }
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file == null || file.getName().isEmpty()) {
                return null;
            }
            return file.getAbsoluteFile();
        }
        return null;
    }
}
