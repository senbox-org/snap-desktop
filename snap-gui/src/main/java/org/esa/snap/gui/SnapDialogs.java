package org.esa.snap.gui;

import org.esa.snap.tango.TangoIcons;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Norman on 14.01.2015.
 */
@NbBundle.Messages({
        "LBL_Information=Information",
        "LBL_DoNotShowThisMessage=Don't show this message anymore." ,
        "LBL_QuestionRemember=Remember my decision and don't ask again."
})
public class SnapDialogs {

    public static void showMessageDialog(String title, String message, int messageType, String preferencesKey) {
        if (preferencesKey != null) {
            String decision = SnapApp.getDefault().getPreferences().get(preferencesKey + ".dontShow", "");
            if (decision.equals("true")) {
                return;
            }
            JPanel panel = new JPanel(new BorderLayout(4, 4));
            panel.add(new JLabel(message), BorderLayout.CENTER);
            JCheckBox dontShowCheckBox = new JCheckBox(Bundle.LBL_DoNotShowThisMessage(), false);
            panel.add(dontShowCheckBox, BorderLayout.SOUTH);
            NotifyDescriptor d = new NotifyDescriptor(panel, SnapApp.getDefault().getInstanceName() + " - " + title, NotifyDescriptor.DEFAULT_OPTION, messageType, null, null);
            DialogDisplayer.getDefault().notify(d);
            boolean storeResult = dontShowCheckBox.isSelected();
            if (storeResult) {
                SnapApp.getDefault().getPreferences().put(preferencesKey + ".dontShow", "true");
            }
        } else {
            NotifyDescriptor d = new NotifyDescriptor(message, SnapApp.getDefault().getInstanceName() + " - " + title, NotifyDescriptor.DEFAULT_OPTION, messageType, null, null);
            DialogDisplayer.getDefault().notify(d);
        }
    }

    public static void showInfoDialog(String title, String message, String preferencesKey) {
        showMessageDialog(title, message, JOptionPane.INFORMATION_MESSAGE, preferencesKey);
    }

    public static void showInfoDialog(String message, String preferencesKey) {
        showInfoDialog(Bundle.LBL_Information(), message, preferencesKey);
    }

    public static int showQuestionDialog(String title, String message, boolean allowCancel, String preferencesKey) {
        Object result;
        boolean storeResult;
        if (preferencesKey != null) {
            String decision = SnapApp.getDefault().getPreferences().get(preferencesKey + ".confirmed", "");
            if (decision.equals("yes")) {
                return JOptionPane.YES_OPTION;
            } else if (decision.equals("no")) {
                return JOptionPane.NO_OPTION;
            }
            JPanel panel = new JPanel(new BorderLayout(4, 4));
            panel.add(new JLabel(message), BorderLayout.CENTER);
            JCheckBox decisionCheckBox = new JCheckBox(Bundle.LBL_QuestionRemember(), false);
            panel.add(decisionCheckBox, BorderLayout.SOUTH);
            NotifyDescriptor d = new NotifyDescriptor.Confirmation(panel, SnapApp.getDefault().getInstanceName() + " - " + title, allowCancel ? NotifyDescriptor.YES_NO_CANCEL_OPTION : NotifyDescriptor.YES_NO_OPTION);
            result = DialogDisplayer.getDefault().notify(d);
            storeResult = decisionCheckBox.isSelected();
        } else {
            NotifyDescriptor d = new NotifyDescriptor.Confirmation(message, SnapApp.getDefault().getInstanceName() + " - " + title, allowCancel ? NotifyDescriptor.YES_NO_CANCEL_OPTION : NotifyDescriptor.YES_NO_OPTION);
            result = DialogDisplayer.getDefault().notify(d);
            storeResult = false;
        }
        if (NotifyDescriptor.YES_OPTION.equals(result)) {
            if (storeResult) {
                SnapApp.getDefault().getPreferences().put(preferencesKey + ".confirmed", "yes");
            }
            return JOptionPane.YES_OPTION;
        } else if (NotifyDescriptor.NO_OPTION.equals(result)) {
            if (storeResult) {
                SnapApp.getDefault().getPreferences().put(preferencesKey + ".confirmed", "no");
            }
            return JOptionPane.NO_OPTION;
        } else {
            return JOptionPane.CANCEL_OPTION;
        }
    }

    public static int showQuestionDialog(String title, String message, String preferencesKey) {
        return showQuestionDialog(title, message, false, preferencesKey);
    }

    public static void showErrorDialog(String title, String message) {
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

    public static void showOutOfMemoryErrorDialog(String message) {
        showErrorDialog("Out of Memory", message + "\n" + "Please save your work and quit the application.");
    }
}
