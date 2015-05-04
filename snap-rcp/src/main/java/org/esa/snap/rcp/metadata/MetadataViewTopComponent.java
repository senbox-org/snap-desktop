package org.esa.snap.rcp.metadata;

import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.ui.product.metadata.MetadataElementChildFactory;
import org.esa.snap.framework.ui.product.metadata.MetadataView;
import org.esa.snap.netbeans.docwin.DocumentTopComponent;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.netbeans.swing.outline.Outline;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.OutlineView;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.table.TableColumnModel;
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
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_MetadataViewTopComponentName",
        preferredID = "MetadataViewTopComponent"
)
@NbBundle.Messages({
        "CTL_MetadataViewTopComponentName=MetadataView",
})
public class MetadataViewTopComponent extends DocumentTopComponent<ProductNode> implements ExplorerManager.Provider {

    private ExplorerManager em = new ExplorerManager();

    public MetadataViewTopComponent(MetadataView document) {
        super(document.getRootElement());
        updateDisplayName();
        setName(getDisplayName());
        setupView(document);

        Children children = Children.create(new MetadataElementChildFactory(document.getMetadataTableElements()), true);
        em.setRootContext(new AbstractNode(children));
    }

    private void setupView(MetadataView document) {
        setLayout(new BorderLayout());
        OutlineView outlineView = new OutlineView(document.getNodesColumnName());
        outlineView.setPropertyColumns(document.getColumnNames());
        final Outline outline = outlineView.getOutline();
        outline.setRootVisible(false);
        outline.setDefaultRenderer(Node.Property.class, new MetadataOutlineCellRenderer());
        final TableColumnModel columnModel = outline.getColumnModel();
        columnModel.getColumn(0).setCellRenderer(new MetadataOutlineCellRenderer());
        final int[] columnWidths = document.getColumnWidths();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setPreferredWidth(columnWidths[i]);
        }
        add(outlineView, BorderLayout.CENTER);
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
