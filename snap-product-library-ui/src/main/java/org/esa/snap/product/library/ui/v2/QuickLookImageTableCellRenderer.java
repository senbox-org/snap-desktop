//package org.esa.snap.product.library.ui.v2;
//
//import org.esa.snap.product.library.ui.v2.table.AbstractTableCellRenderer;
//
//import javax.swing.BorderFactory;
//import javax.swing.ImageIcon;
//import javax.swing.JLabel;
//import javax.swing.JTable;
//import javax.swing.border.Border;
//import javax.swing.table.TableColumn;
//import java.awt.image.BufferedImage;
//
///**
// * Created by jcoravu on 8/8/2019.
// */
//public class QuickLookImageTableCellRenderer extends AbstractTableCellRenderer<JLabel> {
//
//    public QuickLookImageTableCellRenderer(int horizontalAlignment, int verticalAlignment) {
//        super(new JLabel(""));
//
//        this.cellComponent.setHorizontalAlignment(horizontalAlignment);
//        this.cellComponent.setVerticalAlignment(verticalAlignment);
//    }
//
//    @Override
//    public JLabel getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//        TableColumn tableColumn = table.getColumnModel().getColumn(column);
//
//        BufferedImage image = (BufferedImage)value;
//        ImageIcon imageIcon = null;
//        String text = null;
//        if (image == ProductsTableModel.EMPTY_ICON) {
//            text = "Not available!";
//        } else if (image != null) {
//            int cellWidth = tableColumn.getWidth();
//            int cellHeight = table.getRowHeight();
//            if (image.getHeight() > image.getWidth()) {
//                cellWidth = -1;
//            } else {
//                cellHeight = -1;
//            }
//            imageIcon = new ImageIcon(image.getScaledInstance(cellWidth, cellHeight, BufferedImage.SCALE_FAST));
//        }
//        this.cellComponent.setIcon(imageIcon);
//        this.cellComponent.setText(text);
//
//        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//    }
//}
