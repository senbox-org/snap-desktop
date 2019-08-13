package org.esa.snap.product.library.ui.v2.table;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.awt.Dimension;

/**
 * Created by jcoravu on 8/8/2019.
 */
public class CustomTable<RecordType> extends JTable {

    private int visibleRowCount;

    public CustomTable(CustomTableModel<RecordType> tableModel) {
        super(tableModel);

        this.visibleRowCount = 0;
    }

    @Override
    public final Dimension getPreferredScrollableViewportSize() {
        Dimension size = super.getPreferredScrollableViewportSize();

        if (this.visibleRowCount > 0) {
            size.height = this.visibleRowCount * getRowHeight();
        }
        return size;
    }

    @Override
    public CustomTableModel<RecordType> getModel() {
        return (CustomTableModel<RecordType>)super.getModel();
    }

    public void setVisibleRowCount(int visibleRowCount) {
        this.visibleRowCount = visibleRowCount;
    }
}
