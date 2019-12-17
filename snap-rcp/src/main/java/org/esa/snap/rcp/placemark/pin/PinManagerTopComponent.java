package org.esa.snap.rcp.placemark.pin;

import org.esa.snap.core.datamodel.PinDescriptor;
import org.esa.snap.rcp.placemark.PlacemarkManagerTopComponent;
import org.esa.snap.ui.PackageDefaults;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

@TopComponent.Description(
        preferredID = "PinManagerTopComponent",
        iconBase = "org/esa/snap/rcp/icons/" + PackageDefaults.PIN_MANAGER_ICON,
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = "output",
        openAtStartup = false,
        position = 10
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.placemark.pin.PinManagerTopComponent")
@ActionReferences({
        @ActionReference(
                path = "Menu/" + PackageDefaults.PIN_MANAGER_MENU_PATH),
        @ActionReference(
                path = "Toolbars/" + PackageDefaults.PIN_MANAGER_TOOLBAR_NAME,
                position = PackageDefaults.PIN_MANAGER_TOOLBAR_POSITION)
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_PinManagerTopComponent_Name",
        preferredID = "PinManagerTopComponent"
)
@NbBundle.Messages({
        "CTL_PinManagerTopComponent_Name=" + PackageDefaults.PIN_MANAGER_NAME,
        "CTL_PinManagerTopComponent_HelpId=showPinManagerWnd"
})
public class PinManagerTopComponent extends PlacemarkManagerTopComponent {

    public PinManagerTopComponent() {
        super(PinDescriptor.getInstance(), PinTableModel::new);
    }

    @Override
    protected String getTitle() {
        return Bundle.CTL_PinManagerTopComponent_Name();
    }

    @Override
    protected String getHelpId() {
        return Bundle.CTL_PinManagerTopComponent_HelpId();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_PinManagerTopComponent_HelpId());
    }
}
