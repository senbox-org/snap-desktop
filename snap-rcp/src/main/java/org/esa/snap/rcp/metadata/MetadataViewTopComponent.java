package org.esa.snap.rcp.metadata;

import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.ui.product.metadata.MetadataTableInnerElement;
import org.esa.snap.netbeans.docwin.DocumentTopComponent;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.netbeans.swing.outline.Outline;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.OutlineView;
import org.openide.nodes.Node;

import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;

public class MetadataViewTopComponent extends DocumentTopComponent<ProductNode> implements ExplorerManager.Provider {

    private static final String[] column_names = new String[]{"Value", "Value", "Type", "Type", "Unit", "Unit",
            "Description", "Description"};
    private static final int[] column_widths = {
            180, // 0
            180, // 1
            50, // 2
            40, // 3
            200 // 4
    };
    private static final String nodesColumnName = "Name";
    private ExplorerManager em = new ExplorerManager();

    public MetadataViewTopComponent(MetadataElement element) {
        super(element);
        updateDisplayName();
        setName(getDisplayName());
        setupView();
        final MetadataTableInnerElement tableInnerElement = new MetadataTableInnerElement(element);
        em.setRootContext(tableInnerElement.createNode());
    }

    private void setupView() {
        setLayout(new BorderLayout());
        OutlineView outlineView = new OutlineView(nodesColumnName);
        outlineView.setPropertyColumns(column_names);
        final Outline outline = outlineView.getOutline();
        outline.setRootVisible(false);
        outline.setDefaultRenderer(Node.Property.class, new MetadataOutlineCellRenderer());
        final TableColumnModel columnModel = outline.getColumnModel();
        columnModel.getColumn(0).setCellRenderer(new MetadataOutlineCellRenderer());
        final int[] columnWidths = column_widths;
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
