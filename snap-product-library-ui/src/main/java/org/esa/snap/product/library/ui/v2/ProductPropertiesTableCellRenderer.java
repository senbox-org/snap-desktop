package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.table.AbstractTableCellRenderer;
import org.esa.snap.product.library.v2.ProductLibraryItem;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by jcoravu on 12/8/2019.
 */
public class ProductPropertiesTableCellRenderer extends AbstractTableCellRenderer<JPanel> {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

    private final JLabel nameLabel;
    private final JLabel acquisitionDateLabel;
    private final JLabel sizeLabel;

    public ProductPropertiesTableCellRenderer() {
        super(new JPanel());

        this.nameLabel = new JLabel("");
        this.acquisitionDateLabel = new JLabel("");
        this.sizeLabel = new JLabel("");

        this.cellComponent.setLayout(new GridBagLayout());

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 0, 0);
        this.cellComponent.add(this.nameLabel, c);

        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 0, 0);
        this.cellComponent.add(this.acquisitionDateLabel, c);

        c = SwingUtils.buildConstraints(0, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 0, 0);
        this.cellComponent.add(this.sizeLabel, c);
    }

    @Override
    public JPanel getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        ProductLibraryItem product = (ProductLibraryItem)value;
        this.nameLabel.setText(product.getName());

        String dateAsString = DATE_FORMAT.format(product.getAcquisitionDate());
        this.acquisitionDateLabel.setText(dateAsString);

        String size = "    ("  + Long.toString(product.getApproximateSize()) + " MB)";
        this.sizeLabel.setText(size);

        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    public Dimension getPreferredSize() {
        return this.cellComponent.getPreferredSize();
    }
}
