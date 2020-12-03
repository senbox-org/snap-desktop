package org.esa.snap.rcp.statistics;

import org.esa.snap.ui.PackageDefaults;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

@TopComponent.Description(
        preferredID = "StatisticsTopComponent",
        iconBase = "org/esa/snap/rcp/icons/" + PackageDefaults.STATISTICS_ICON,
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = PackageDefaults.STATISTICS_WS_MODE,
        openAtStartup = PackageDefaults.STATISTICS_WS_OPEN,
        position = PackageDefaults.STATISTICS_WS_POSITION
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.statistics.StatisticsTopComponent")
@ActionReferences({
        @ActionReference(
                path = "Menu/" + PackageDefaults.STATISTICS_MENU_PATH,
                position = PackageDefaults.STATISTICS_MENU_POSITION),
        @ActionReference(
                path = "Toolbars/" + PackageDefaults.STATISTICS_TOOLBAR_PATH,
                position = PackageDefaults.STATISTICS_TOOLBAR_POSITION)
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_StatisticsTopComponent_Name",
        preferredID = "StatisticsTopComponent"
)
@NbBundle.Messages({
        "CTL_StatisticsTopComponent_Name=" + PackageDefaults.STATISTICS_NAME,
        "CTL_StatisticsTopComponent_HelpId=statisticsDialog"
})
/**
 * @author Tonio Fincke
 */
public class StatisticsTopComponent extends AbstractStatisticsTopComponent {

    @Override
    protected PagePanel createPagePanel() {
        return new StatisticsPanel(this, Bundle.CTL_StatisticsTopComponent_HelpId());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_StatisticsTopComponent_HelpId());
    }
}
