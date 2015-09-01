package org.esa.snap.rcp.statistics;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

@TopComponent.Description(
        preferredID = "StatisticsTopComponent",
        iconBase = "org/esa/snap/rcp/icons/Statistics.gif",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = "Statistics",
        openAtStartup = false,
        position = 40
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.statistics.StatisticsTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/Analysis",position = 60),
        @ActionReference(path = "Toolbars/Analysis")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_StatisticsTopComponent_Name",
        preferredID = "StatisticsTopComponent"
)
@NbBundle.Messages({
        "CTL_StatisticsTopComponent_Name=Statistics",
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
