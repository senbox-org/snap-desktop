package org.esa.snap.rcp.imgfilter;


import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.imgfilter.model.Filter;
import org.openide.util.NbBundle;

import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

/**
 * Represents a window that lets users inspect and edit a single image {@link Filter}.
 *
 * @author Norman
 */
@NbBundle.Messages({
        "LBL_FilterWindow_Title=Image Filter",
        "LBL_FilterWindow_Kernel=Filter Kernel",
        "LBL_FilterWindow_Properties=Filter Properties",
        "TXT_FilterWindow_Hint=<html>Right-clicking into the kernel editor canvas<br>" +
                "opens a context menu with <b>more options</b>",
})
public class FilterWindow implements FilterEditor {

    private Window parentWindow;
    private JDialog dialog;
    private FilterKernelForm kernelForm;
    private Filter filter;
    private FilterPropertiesForm propertiesForm;

    public FilterWindow(Window parentWindow) {
        this.parentWindow = parentWindow;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public void setFilter(Filter filter) {
        this.filter = filter;
        if (kernelForm != null) {
            kernelForm.setFilter(filter);
        }
        if (propertiesForm != null) {
            propertiesForm.setFilter(filter);
        }
    }

    @Override
    public void show() {
        if (dialog == null) {
            kernelForm = new FilterKernelForm(filter);
            propertiesForm = new FilterPropertiesForm(filter);
            dialog = new JDialog(parentWindow, Bundle.LBL_FilterWindow_Title(), Dialog.ModalityType.MODELESS);
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.addTab(Bundle.LBL_FilterWindow_Kernel(), kernelForm);
            tabbedPane.addTab(Bundle.LBL_FilterWindow_Properties(), propertiesForm);
            dialog.setContentPane(tabbedPane);

            Preferences filterWindowPrefs = SnapApp.getDefault().getPreferences().node("filterWindow");
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    filterWindowPrefs.putInt("x", e.getWindow().getX());
                    filterWindowPrefs.putInt("y", e.getWindow().getY());
                    filterWindowPrefs.putInt("width", e.getWindow().getWidth());
                    filterWindowPrefs.putInt("height", e.getWindow().getHeight());
                }
            });
            Dimension preferredSize = dialog.getPreferredSize();
            int x = filterWindowPrefs.getInt("x", 100);
            int y = filterWindowPrefs.getInt("y", 100);
            int w = filterWindowPrefs.getInt("width", preferredSize.width);
            int h = filterWindowPrefs.getInt("height", preferredSize.height);
            dialog.setBounds(x, y, w, h);

            SnapDialogs.showInformation(Bundle.LBL_FilterWindow_Title(),
                                        Bundle.TXT_FilterWindow_Hint(),
                                        "filterWindow.moreOptions");
        }
        dialog.setVisible(true);
    }

    @Override
    public void hide() {
        if (dialog != null) {
            dialog.setVisible(false);
        }
    }

}
