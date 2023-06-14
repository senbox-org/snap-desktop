package org.esa.snap.rcp.actions.help;

import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.help.HelpDisplayer;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

/**
 * Basic help action.
 *
 * @author Norman Fomferra
 */
@NbBundle.Messages({
        "CTL_HelpActionText=Help",
        "CTL_HelpActionToolTip=Invokes the help system."
})
public class HelpAction extends AbstractAction implements HelpCtx.Provider {
    private final HelpCtx helpCtx;
    private final HelpCtx.Provider delegateHelpCtx;

    public HelpAction() {
        this(HelpCtx.DEFAULT_HELP);
    }

    public HelpAction(HelpCtx.Provider delegateHelpCtx) {
        this.delegateHelpCtx = delegateHelpCtx;
        this.helpCtx = null;
        initProperties();
    }

    public HelpAction(HelpCtx helpCtx) {
        this.helpCtx = helpCtx;
        this.delegateHelpCtx = null;
        initProperties();
    }

    private void initProperties() {
        putValue(ACTION_COMMAND_KEY, "help");
        putValue(NAME, Bundle.CTL_HelpActionText());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_HelpActionToolTip());
        putValue(SMALL_ICON, TangoIcons.apps_help_browser(TangoIcons.Res.R16));
        putValue(LARGE_ICON_KEY, TangoIcons.apps_help_browser(TangoIcons.Res.R22));
    }

    @Override
    public HelpCtx getHelpCtx() {
        return delegateHelpCtx != null ? delegateHelpCtx.getHelpCtx() : helpCtx;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        HelpDisplayer.show(getHelpCtx());
    }

}
