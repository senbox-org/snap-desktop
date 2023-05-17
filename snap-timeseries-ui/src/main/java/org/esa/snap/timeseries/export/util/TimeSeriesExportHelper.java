package org.esa.snap.timeseries.export.util;

import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.SnapFileChooser;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Enumeration;

/**
 * @author Marco Peters
 */
public class TimeSeriesExportHelper {

    public static class FileWithLevel {

        public FileWithLevel(File file, int level) {
            this.file = file;
            this.level = level;
        }

        public File file;
        public int level;
    }

    public static FileWithLevel getOutputFileWithLevelOption(RasterDataNode raster,
                                                             String title, String fileNamePrefix, String dirPreferencesKey,
                                                             SnapFileFilter fileFilter, String helpId) {
        SnapApp snapApp = SnapApp.getDefault();
        final String lastDir = snapApp.getPreferences().get(dirPreferencesKey, SystemUtils.getUserHomeDir().getPath());
        final File currentDir = new File(lastDir);

        final SnapFileChooser fileChooser = new SnapFileChooser();
        if (helpId != null) {
            HelpCtx.setHelpIDString(fileChooser, helpId);
        }
        fileChooser.setCurrentDirectory(currentDir);
        fileChooser.addChoosableFileFilter(fileFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);

        fileChooser.setDialogTitle(snapApp.getInstanceName() + " - " + title);
        fileChooser.setCurrentFilename(fileNamePrefix + raster.getName());

        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        Dimension fileChooserSize = fileChooser.getPreferredSize();
        if (fileChooserSize != null) {
            fileChooser.setPreferredSize(new Dimension(
                    fileChooserSize.width + 120, fileChooserSize.height));
        } else {
            fileChooser.setPreferredSize(new Dimension(512, 256));
        }

        int maxLevel = raster.getSourceImage().getModel().getLevelCount() - 1;
        maxLevel = maxLevel > 10 ? 10 : maxLevel;

        final JPanel levelPanel = new JPanel(new GridLayout(maxLevel, 1));
        levelPanel.setBorder(BorderFactory.createTitledBorder("Resolution Level"));
        ButtonGroup buttonGroup = new ButtonGroup();
        for (int i = 0; i < maxLevel; i++) {
            String buttonText = Integer.toString(i);
            if (i == 0) {
                buttonText += " (high, very slow)";
            } else if (i == maxLevel - 1) {
                buttonText += " (low, fast)";
            }
            final JRadioButton button = new JRadioButton(buttonText, true);
            buttonGroup.add(button);
            levelPanel.add(button);
            button.setSelected(true);
        }


        final JPanel accessory = new JPanel();
        accessory.setLayout(new BoxLayout(accessory, BoxLayout.Y_AXIS));
        accessory.add(levelPanel);
        fileChooser.setAccessory(accessory);

        int result = fileChooser.showSaveDialog(snapApp.getMainFrame());
        File file = fileChooser.getSelectedFile();

        final File currentDirectory = fileChooser.getCurrentDirectory();
        if (currentDirectory != null) {
            snapApp.getPreferences().get(dirPreferencesKey, currentDirectory.getPath());
        }
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        if (file == null || file.getName().isEmpty()) {
            return null;
        }

        if (!promptForOverwrite(file)) {
            return null;
        }

        int level = parseLevel(buttonGroup);
        return new FileWithLevel(file, level);
    }

    public static boolean promptForOverwrite(File file) {
        return !file.exists() || Dialogs.Answer.YES == Dialogs.requestDecision("File Exists",
                "The file\n" + "'" + file.getPath() + "'\n" + "already exists.\n\n" + "Do you really want to overwrite it?\n",
                false, null);
    }

    private static int parseLevel(ButtonGroup buttonGroup) {
        Enumeration<AbstractButton> buttonEnumeration = buttonGroup.getElements();
        while (buttonEnumeration.hasMoreElements()) {
            AbstractButton abstractButton = buttonEnumeration.nextElement();
            if (abstractButton.isSelected()) {
                String buttonText = abstractButton.getText();
                final int index = buttonText.indexOf(" (");
                if (index != -1) {
                    buttonText = buttonText.substring(0, index);
                }
                return Integer.parseInt(buttonText);
            }
        }
        return-1;
    }

}
