package org.esa.snap.rcp.metadata;

import eu.esa.snap.netbeans.docwin.DocumentTopComponent;
import eu.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.ui.product.metadata.MetadataTableInnerElement;
import org.netbeans.swing.outline.Outline;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.OutlineView;
import org.openide.nodes.Node;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;

public class MetadataViewTopComponent extends DocumentTopComponent<MetadataElement, OutlineView> implements ExplorerManager.Provider {

    private static final String[] COLUMN_NAMES = new String[]{
            "Value", "Value",
            "Type", "Type",
            "Unit", "Unit",
            "Description", "Description"
    };

    private static final int[] COLUMN_WIDTHS = {
            180, // 0
            180, // 1
            50, // 2
            40, // 3
            200 // 4
    };

    private static final String nodesColumnName = "Name";
    private ExplorerManager em = new ExplorerManager();
    private OutlineView outlineView;

    public MetadataViewTopComponent(MetadataElement element) {
        super(element);
        updateDisplayName();
        setName(getDisplayName());
        initView();
        final MetadataTableInnerElement tableInnerElement = new MetadataTableInnerElement(element);
        em.setRootContext(tableInnerElement.createNode());
    }

    @Override
    public OutlineView getView() {
        return outlineView;
    }

    private void initView() {
        setLayout(new BorderLayout());
        outlineView = new OutlineView(nodesColumnName);
        outlineView.setPropertyColumns(COLUMN_NAMES);
        final Outline outline = outlineView.getOutline();
        outline.setRootVisible(false);
        DefaultTableCellRenderer decimalTableCellRenderer = new StringDecimalFormatRenderer();
        outline.setDefaultRenderer(Double.class, decimalTableCellRenderer);
        outline.setDefaultRenderer(Float.class, decimalTableCellRenderer);
        outline.setDefaultRenderer(Node.Property.class, new MetadataOutlineCellRenderer());
        final TableColumnModel columnModel = outline.getColumnModel();
        columnModel.getColumn(0).setCellRenderer(new MetadataOutlineCellRenderer());
        final int[] columnWidths = COLUMN_WIDTHS;
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setPreferredWidth(columnWidths[i]);
        }
        add(outlineView, BorderLayout.CENTER);
    }

    private void updateDisplayName() {
        setDisplayName(WindowUtilities.getUniqueTitle(getDocument().getDisplayName(),
                MetadataViewTopComponent.class));
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return em;
    }


    public static class StringDecimalFormatRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                label.setHorizontalAlignment(JLabel.LEFT);
                if (value instanceof Float || value instanceof Double) {
                    label.setText(String.valueOf(value));
                } else {
                    label.setText("n/a");
                }
            }
            return comp;
        }
    }
}
