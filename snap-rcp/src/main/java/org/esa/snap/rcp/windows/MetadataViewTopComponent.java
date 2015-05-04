package org.esa.snap.rcp.windows;

import org.esa.snap.framework.datamodel.MetadataAttribute;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.ui.product.ProductMetadataView;
import org.esa.snap.framework.ui.product.metadata.MetadataElementChildFactory;
import org.esa.snap.framework.ui.product.metadata.MetadataView;
import org.esa.snap.netbeans.docwin.DocumentTopComponent;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.OutlineView;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import java.awt.BorderLayout;

/**
 * @author Tonio Fincke
 */
@TopComponent.Description(
        preferredID = "MetadataViewTopComponent",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "navigator",
        openAtStartup = true,
        position = 0
)
//@ActionID(category = "Window", id = "org.esa.snap.rcp.window.MetadataViewTopComponent")
//@ActionReference(path = "Menu/Window/Tool Windows", position = 0)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_MetadataViewTopComponentName",
        preferredID = "MetadataViewTopComponent"
)
@NbBundle.Messages({
        "CTL_MetadataViewTopComponentName=MetadataView",
})
public class MetadataViewTopComponent extends DocumentTopComponent<ProductNode> implements ExplorerManager.Provider {

    private ExplorerManager em = new ExplorerManager();
//    public MetadataElement[] metadataElements;
    Children children;

    public MetadataViewTopComponent(MetadataView document) {
        super(document.getRootElement());
        updateDisplayName();
        setName(getDisplayName());
        setLayout(new BorderLayout());

        OutlineView outlineView = new OutlineView(document.getNodesColumnName());
        outlineView.getOutline().setRootVisible(false);

        outlineView.setPropertyColumns(document.getColumnNames());
//        metadataElements = document.getMetadataElements();
//        MetadataAttribute[] metadataAttributes = document.getMetadataAttributes();
        add(outlineView, BorderLayout.CENTER);

        children = Children.create(new MetadataElementChildFactory(document.getMetadataTableElements()), true);
        em.setRootContext(new AbstractNode(children));
    }

    private void updateDisplayName() {
        setDisplayName(WindowUtilities.getUniqueTitle(getDocument().getName(),
                ProductSceneViewTopComponent.class));
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return em;
    }

}
