package org.esa.snap.gui;

import org.esa.snap.tango.TangoIcons;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.*;
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
        "LBL_DoNotShowThisMessage=Don't show this message anymore." ,
        "LBL_QuestionRemember=Remember my decision and don't ask again."
})
public class SnapDialogs {

    private static final String PREF_KEY_SUFFIX_DECISION = ".decision";
    private static final String PREF_KEY_SUFFIX_DONTSHOW = ".dontShow";
    private static final String PREF_VALUE_YES = "yes";
    private static final String PREF_VALUE_NO = "no";
    private static final String PREF_VALUE_TRUE = "true";

    public static void showInformation(String message, String preferencesKey) {
        showInformation(Bundle.LBL_Information(), message, preferencesKey);
    }

    public static void showInformation(String title, String message, String preferencesKey) {
        showMessage(title, message, JOptionPane.INFORMATION_MESSAGE, preferencesKey);
    }

    public static void showMessage(String title, String message, int messageType, String preferencesKey) {
        title = String.format("%s - %s", SnapApp.getDefault().getInstanceName(), title != null ? title : Bundle.LBL_Message());
        if (preferencesKey != null) {
            String decision = getPreferences().get(preferencesKey + PREF_KEY_SUFFIX_DONTSHOW, "");
            if (decision.equals(PREF_VALUE_TRUE)) {
                return;
            }
            JPanel panel = new JPanel(new BorderLayout(4, 4));
            panel.add(new JLabel(message), BorderLayout.CENTER);
            JCheckBox dontShowCheckBox = new JCheckBox(Bundle.LBL_DoNotShowThisMessage(), false);
            panel.add(dontShowCheckBox, BorderLayout.SOUTH);
            NotifyDescriptor d = new NotifyDescriptor(panel, title, NotifyDescriptor.DEFAULT_OPTION, messageType, null, null);
            DialogDisplayer.getDefault().notify(d);
            boolean storeResult = dontShowCheckBox.isSelected();
            if (storeResult) {
                getPreferences().put(preferencesKey + PREF_KEY_SUFFIX_DONTSHOW, PREF_VALUE_TRUE);
            }
        } else {
            NotifyDescriptor d = new NotifyDescriptor(message, title, NotifyDescriptor.DEFAULT_OPTION, messageType, null, null);
            DialogDisplayer.getDefault().notify(d);
        }
    }

    /**
     * Displays a modal dialog which requests a decision from the user.
     *
     * @param title The dialog title. May be {@code null}.
     * @param message The question text to be displayed.
     * @param allowCancel If {@code true}, the dialog also offers a cancel button.
     * @param preferencesKey If not {@code null}, a checkbox is displayed, and if checked the dialog will not be displayed again which lets users store the answer
     * @return {@code JOptionPane.YES_OPTION}, {@code JOptionPane.NO_OPTION}, or {@code JOptionPane.CANCEL_OPTION}.
     */
    public static int requestDecision(String title, String message, boolean allowCancel, String preferencesKey) {
        Object result;
        boolean storeResult;
        int optionType = allowCancel ? NotifyDescriptor.YES_NO_CANCEL_OPTION : NotifyDescriptor.YES_NO_OPTION;
        title = String.format("%s - %s", SnapApp.getDefault().getInstanceName(), title != null ? title : Bundle.LBL_Question());
        if (preferencesKey != null) {
            String decision = getPreferences().get(preferencesKey + PREF_KEY_SUFFIX_DECISION, "");
            if (decision.equals(PREF_VALUE_YES)) {
                return JOptionPane.YES_OPTION;
            } else if (decision.equals(PREF_VALUE_NO)) {
                return JOptionPane.NO_OPTION;
            }
            JPanel panel = new JPanel(new BorderLayout(4, 4));
            panel.add(new JLabel(message), BorderLayout.CENTER);
            JCheckBox decisionCheckBox = new JCheckBox(Bundle.LBL_QuestionRemember(), false);
            panel.add(decisionCheckBox, BorderLayout.SOUTH);
            NotifyDescriptor d = new NotifyDescriptor.Confirmation(panel, title, optionType);
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
            return JOptionPane.YES_OPTION;
        } else if (NotifyDescriptor.NO_OPTION.equals(result)) {
            if (storeResult) {
                getPreferences().put(preferencesKey + PREF_KEY_SUFFIX_DECISION, PREF_VALUE_NO);
            }
            return JOptionPane.NO_OPTION;
        } else {
            return JOptionPane.CANCEL_OPTION;
        }
    }

    public static void showError(String title, String message) {
        // todo - finalize this code here
        NotifyDescriptor nd = new NotifyDescriptor(message,
                                                   title,
                                                   JOptionPane.OK_OPTION,
                                                   NotifyDescriptor.ERROR_MESSAGE,
                                                   null,
                                                   null);
        DialogDisplayer.getDefault().notify(nd);

        ImageIcon icon = TangoIcons.status_dialog_error(TangoIcons.Res.R16);
        JLabel balloonDetails = new JLabel(message);
        JButton popupDetails = new JButton("Call ESA");
        NotificationDisplayer.getDefault().notify(title,
                                                  icon,
                                                  balloonDetails,
                                                  popupDetails,
                                                  NotificationDisplayer.Priority.HIGH,
                                                  NotificationDisplayer.Category.ERROR);
    }

    private static Preferences getPreferences() {
        return SnapApp.getDefault().getPreferences().node("dialogs");
    }
}
