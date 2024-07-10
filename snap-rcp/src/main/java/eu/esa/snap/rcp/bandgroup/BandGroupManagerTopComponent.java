package eu.esa.snap.rcp.bandgroup;

import eu.esa.snap.core.datamodel.group.BandGroupsManager;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@TopComponent.Description(
        preferredID = "BandGroupManagerTopComponent",
        iconBase = "org/esa/snap/rcp/icons/BandGroupManager.gif",
        persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED
)
@TopComponent.Registration(
        mode = "output",
        openAtStartup = false,
        position = 11
)
@ActionID(category = "Window", id = "eu.esa.snap.rcp.bandgroup.BandGroupManagerTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows", position = 31),
        @ActionReference(path = "Toolbars/Tool Windows")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_BandGroupManagerTopComponent_Name",
        preferredID = "BandGroupManagerTopComponent"
)
@NbBundle.Messages({
        "CTL_BandGroupManagerTopComponent_Name=Band Groups Manager",
        "CTL_BandGroupManagerTopComponent_HelpId=showBandGroupManagerWnd"
})
public class BandGroupManagerTopComponent extends ToolTopComponent {

    private final BandGroupsManager bandGroupsManager;

    public BandGroupManagerTopComponent() throws IOException {
        // @todo 1 tb/tb initialize this at engine startup.
        final File appDataDir = SystemUtils.getApplicationDataDir();
        final Path appDataPath = Paths.get(appDataDir.getAbsolutePath());
        final Path configDir = appDataPath.resolve("config");
        BandGroupsManager.initialize(configDir);
        bandGroupsManager = BandGroupsManager.getInstance();

        initUi();
    }

    private void initUi() {
        setLayout(new BorderLayout());

        JPanel mainPane = new JPanel(new BorderLayout(4, 4));

        add(mainPane, BorderLayout.CENTER);
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_BandGroupManagerTopComponent_HelpId());
    }

    @Override
    protected void productSceneViewSelected(ProductSceneView view) {
        final Product product = view.getProduct();
        bandGroupsManager.addGroupsOfProduct(product);
    }

    @Override
    protected void productSceneViewDeselected(ProductSceneView view) {
        bandGroupsManager.removeGroupsOfProduct();
    }
}
