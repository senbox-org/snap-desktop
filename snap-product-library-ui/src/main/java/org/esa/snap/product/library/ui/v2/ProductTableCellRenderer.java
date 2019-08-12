package org.esa.snap.product.library.ui.v2;

import ro.cs.tao.eodata.EOProduct;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.GridLayout;

/**
 * Created by jcoravu on 12/8/2019.
 */
public class ProductTableCellRenderer extends JPanel implements TableCellRenderer {

    private JLabel nameLabel;
    private JLabel sizeLabel;

    public ProductTableCellRenderer() {
        setLayout(new GridLayout(2, 1));

        this.nameLabel = new JLabel("Name");
        this.sizeLabel = new JLabel("Size");

        add(this.nameLabel);
        add(this.sizeLabel);

        setBorder(new EmptyBorder(5, 5, 5, 5));
        setOpaque(false);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        EOProduct product = (EOProduct)value;
        this.nameLabel.setText(product.getName());
        this.sizeLabel.setText(product.getAcquisitionDate().toString());
        return this;
    }
}
