package org.esa.snap.rcp;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.ui.SnapFileChooser;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.io.File;
import java.text.MessageFormat;
import java.util.prefs.Preferences;

/**
 * Utility class which is used to display various commonly and frequently used message dialogs.
 *
 * @author Marco, Norman
 * @since 2.0
 */
@NbBundle.Messages({
        "LBL_Information=Information",
        "LBL_Question=Question",
        "LBL_Message=Message",
        "LBL_Warning=Warning",
        "LBL_Error=Error",
        "LBL_DoNotShowThisMessage=Don't show this message anymore.",
        "LBL_QuestionRemember=Remember my decision and don't ask again."
})
public class SnapDialogs {

    public enum Answer {
        YES,
        NO,
        CANCELLED
    }

    private static final String PREF_KEY_SUFFIX_DECISION = ".decision";
    private static final String PREF_KEY_SUFFIX_DONTSHOW = ".dontShow";
    private static final String PREF_VALUE_YES = "yes";
    private static final String PREF_VALUE_NO = "no";
    private static final String PREF_VALUE_TRUE = "true";

    private SnapDialogs() {
    }

    /**
     * Displays a modal dialog with the provided information message text.
     *
     * @param message        The message text to be displayed.
     */
    public static void showInformation(String message) {
        showInformation(message, null);
    }

    /**
     * Displays a modal dialog with the provided information message text.
     *
     * @param message        The message text to be displayed.
     * @param preferencesKey If not {@code null}, a checkbox is displayed, and if checked the dialog will not be displayed again which lets users store the answer
     */
    public static void showInformation(String message, String preferencesKey) {
        showInformation(Bundle.LBL_Information(), message, preferencesKey);
    }

    /**
     * Displays a modal dialog with the provided information message text.
     *
     * @param title          The dialog title. May be {@code null}.
     * @param message        The information message text to be displayed.
     * @param preferencesKey If not {@code null}, a checkbox is displayed, and if checked the dialog will not be displayed again which lets users store the answer
     */
    public static void showInformation(String title, String message, String preferencesKey) {
        showMessage(title != null ? title : Bundle.LBL_Information(), message, JOptionPane.INFORMATION_MESSAGE, preferencesKey);
    }

    /**
     * Displays a modal dialog with the provided warning message text.
     *
     * @param message The information message text to be displayed.
     */
    public static void showWarning(String message) {
        showWarning(null, message, null);
    }

    /**
     * Displays a modal dialog with the provided warning message text.
     *
     * @param title          The dialog title. May be {@code null}.
     * @param message        The warning message text to be displayed.
     * @param preferencesKey If not {@code null}, a checkbox is displayed, and if checked the dialog will not be displayed again which lets users store the answer
     */
    public static void showWarning(String title, String message, String preferencesKey) {
        showMessage(title != null ? title : Bundle.LBL_Warning(), message, JOptionPane.WARNING_MESSAGE, preferencesKey);
    }

    /**
     * Displays a modal dialog with the provided error message text.
     *
     * @param message The error message text to be displayed.
     */
    public static void showError(String message) {
        showError(null, message);
    }

    /**
     * Displays a modal dialog with the provided error message text.
     *
     * @param title   The dialog title. May be {@code null}.
     * @param message The error message text to be displayed.
     */
    public static void showError(String title, String message) {
        showMessage(title != null ? title : Bundle.LBL_Error(), message, JOptionPane.ERROR_MESSAGE, null);
    }

    /**
     * Displays a modal dialog indicating an 'Out of Memory'-error with the
     * provided error message text. It also displays a hint how to solve the problem.
     *
     * @param message The error message text to be displayed.
     */
    public static void showOutOfMemoryError(String message) {
        showError("Out of Memory", String.format("%s\n\n" +
                                                 "You can try to release memory by closing products or image views which\n" +
                                                 "you currently not really need.", message));
    }

    /**
     * Displays a modal dialog with the provided message text.
     *
     * @param title          The dialog title. May be {@code null}.
     * @param message        The message text to be displayed.
     * @param messageType    The type of the message.
     * @param preferencesKey If not {@code null}, a checkbox is displayed, and if checked the dialog will not be displayed again which lets users store the answer
     */
    public static void showMessage(String title, String message, int messageType, String preferencesKey) {
        title = getDialogTitle(title != null ? title : Bundle.LBL_Message());
        if (preferencesKey != null) {
            String decision = getPreferences().get(preferencesKey + PREF_KEY_SUFFIX_DONTSHOW, "");
            if (decision.equals(PREF_VALUE_TRUE)) {
                return;
            }
            JPanel panel = new JPanel(new BorderLayout(4, 4));
            panel.add(new JLabel(message), BorderLayout.CENTER);
            JCheckBox dontShowCheckBox = new JCheckBox(Bundle.LBL_DoNotShowThisMessage(), false);
            panel.add(dontShowCheckBox, BorderLayout.SOUTH);
            NotifyDescriptor d = new NotifyDescriptor(panel, title, JOptionPane.DEFAULT_OPTION, messageType, null, null);
            DialogDisplayer.getDefault().notify(d);
            if (d.getValue() != NotifyDescriptor.CANCEL_OPTION) {
                boolean storeResult = dontShowCheckBox.isSelected();
                if (storeResult) {
                    getPreferences().put(preferencesKey + PREF_KEY_SUFFIX_DONTSHOW, PREF_VALUE_TRUE);
                }
            }
        } else {
            NotifyDescriptor d = new NotifyDescriptor(message, title, JOptionPane.DEFAULT_OPTION, messageType, null, null);
            DialogDisplayer.getDefault().notify(d);
        }
    }

    /**
     * Displays a modal dialog which requests a decision from the user.
     *
     * @param title          The dialog title. May be {@code null}.
     * @param message        The question text to be displayed.
     * @param allowCancel    If {@code true}, the dialog also offers a cancel button.
     * @param preferencesKey If not {@code null}, a checkbox is displayed, and if checked the dialog will not be displayed again which lets users store the answer
     * @return {@link Answer#YES}, {@link Answer#NO}, or {@link Answer#CANCELLED}.
     */
    public static Answer requestDecision(String title, String message, boolean allowCancel, String preferencesKey) {
        Object result;
        boolean storeResult;
        int optionType = allowCancel ? NotifyDescriptor.YES_NO_CANCEL_OPTION : NotifyDescriptor.YES_NO_OPTION;
        title = getDialogTitle(title != null ? title : Bundle.LBL_Question());
        if (preferencesKey != null) {
            String decision = getPreferences().get(preferencesKey + PREF_KEY_SUFFIX_DECISION, "");
            if (decision.equals(PREF_VALUE_YES)) {
                return Answer.YES;
            } else if (decision.equals(PREF_VALUE_NO)) {
                return Answer.NO;
            }
            JCheckBox decisionCheckBox = new JCheckBox(Bundle.LBL_QuestionRemember(), false);
            NotifyDescriptor d = new NotifyDescriptor.Confirmation(new Object[]{message, decisionCheckBox}, title, optionType);
            result = DialogDisplayer.getDefault().notify(d);
            storeResult = decisionCheckBox.isSelected();
        } else {
            NotifyDescriptor d = new NotifyDescriptor.Confirmation(message, title, optionType);
            result = DialogDisplayer.getDefault().notify(d);
            storeResult = false;
        }
        if (NotifyDescriptor.YES_OPTION.equals(result)) {
            if (storeResult) {
                getPreferences().put(preferencesKey + PREF_KEY_SUFFIX_DECISION, PREF_VALUE_YES);
            }
            return Answer.YES;
        } else if (NotifyDescriptor.NO_OPTION.equals(result)) {
            if (storeResult) {
                getPreferences().put(preferencesKey + PREF_KEY_SUFFIX_DECISION, PREF_VALUE_NO);
            }
            return Answer.NO;
        } else {
            return Answer.CANCELLED;
        }
    }

    /**
     * Opens question dialog asking the user whether or not to overwrite an existing file. If the given
     * file does not exists, the question dialog is not shown.
     *
     * @param file the file to check for existance
     * @return <code>True</code> if the user confirms the dialog with 'yes' or the given file does not exist.<br>
     * <code>False</code> if the user does not want to overwrite the existing file.<br>
     * <code>null</code> if the user canceled the operation.<br>
     */
    public static Boolean requestOverwriteDecision(String title, File file) {
        if (!file.exists()) {
            return Boolean.TRUE;
        }
        Answer answer = requestDecision(getDialogTitle(title),
                                        MessageFormat.format(
                                                "The file ''{0}'' already exists.\nDo you wish to overwrite it?",
                                                file),
                                        true, null);
        return answer == Answer.YES ? Boolean.TRUE : answer == Answer.NO ? Boolean.FALSE : null;
    }

    /**
     * Opens a standard file-open dialog box.
     *
     * @param title          a dialog-box title
     * @param dirsOnly       whether or not to select only directories
     * @param fileFilter     the file filter to be used, can be <code>null</code>
     * @param preferencesKey the key under which the last directory the user visited is stored
     * @return the file selected by the user or <code>null</code> if the user canceled file selection
     */
    public static File requestFileForOpen(String title,
                                          boolean dirsOnly,
                                          FileFilter fileFilter,
                                          String preferencesKey) {
        Assert.notNull(preferencesKey, "preferencesKey");

        String lastDir = getPreferences().get(preferencesKey, SystemUtils.getUserHomeDir().getPath());
        File currentDir = new File(lastDir);

        SnapFileChooser fileChooser = new SnapFileChooser();
        fileChooser.setCurrentDirectory(currentDir);
        if (fileFilter != null) {
            fileChooser.setFileFilter(fileFilter);
        }
        fileChooser.setDialogTitle(getDialogTitle(title));
        fileChooser.setFileSelectionMode(dirsOnly ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(SnapApp.getDefault().getMainFrame());
        if (fileChooser.getCurrentDirectory() != null) {
            getPreferences().put(preferencesKey, fileChooser.getCurrentDirectory().getPath());
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file == null || file.getName().equals("")) {
                return null;
            }
            return file;
        }
        return null;
    }

    /**
     * Opens a standard save dialog box.
     *
     * @param title            A dialog-box title.
     * @param dirsOnly         Whether or not to select only directories.
     * @param fileFilter       The file filter to be used, can be <code>null</code>.
     * @param defaultExtension The extension used as default.
     * @param fileName         The initial filename.
     * @param accessory        An accessory UI component to be shown in the {@link JFileChooser#setAccessory(JComponent) file chooser},
     *                         can be <code>null</code>.
     * @param preferenceKey    The key under which the last directory the user visited is stored.
     * @return The file selected by the user or <code>null</code> if the user cancelled the file selection.
     */
    public static File requestFileForSave(String title,
                                          boolean dirsOnly,
                                          FileFilter fileFilter,
                                          String defaultExtension,
                                          String fileName,
                                          JComponent accessory,
                                          String preferenceKey) {

        // Loop while the user does not want to overwrite a selected, existing file
        // or if the user presses "Cancel"
        //
        File file;
        do {
            file = requestFileForSave2(title, dirsOnly, fileFilter, defaultExtension, fileName, accessory, preferenceKey);
            if (file == null) {
                return null; // Cancelled
            } else if (file.exists()) {
                Boolean overwrite = requestOverwriteDecision(title, file);
                if (overwrite == null) {
                    return null;
                } else if (!overwrite) {
                    file = null; // No, do not overwrite, let user select another file
                }
            }
        } while (file == null);
        return file;
    }

    private static File requestFileForSave2(String title,
                                            boolean dirsOnly,
                                            FileFilter fileFilter,
                                            String defaultExtension,
                                            final String fileName,
                                            JComponent accessory,
                                            final String preferenceKey) {

        Assert.notNull(preferenceKey, "preferenceKey");

        String lastDir = getPreferences().get(preferenceKey, SystemUtils.getUserHomeDir().getPath());
        File currentDir = new File(lastDir);

        SnapFileChooser fileChooser = new SnapFileChooser();
        fileChooser.setCurrentDirectory(currentDir);
        if (fileFilter != null) {
            fileChooser.setFileFilter(fileFilter);
        }
        if (fileName != null) {
            fileChooser.setSelectedFile(new File(FileUtils.exchangeExtension(fileName, defaultExtension)));
        }
        fileChooser.setDialogTitle(getDialogTitle(title));
        fileChooser.setFileSelectionMode(dirsOnly ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
        if (accessory != null) {
            fileChooser.setAccessory(accessory);
        }
        int result = fileChooser.showSaveDialog(SnapApp.getDefault().getMainFrame());
        if (fileChooser.getCurrentDirectory() != null) {
            getPreferences().put(preferenceKey, fileChooser.getCurrentDirectory().getPath());
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file == null || file.getName().equals("")) {
                return null;
            }
            String path = file.getPath();
            if (defaultExtension != null) {
                if (!path.toLowerCase().endsWith(defaultExtension.toLowerCase())) {
                    path = path.concat(defaultExtension);
                }
            }
            return new File(path);
        }
        return null;
    }


    public static String getDialogTitle(String titleText) {
        return MessageFormat.format("{0} - {1}", SnapApp.getDefault().getInstanceName(), titleText);
    }

    private static Preferences getPreferences() {
        return SnapApp.getDefault().getPreferences();
    }
}
