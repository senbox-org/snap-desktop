/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.productlibrary.rcp.toolviews.model.dataprovider;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.quicklooks.Quicklook;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.productlibrary.db.ProductEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Comparator;

public class QuicklookProvider implements DataProvider {

    private final static Comparator quickLookComparator = new QuickLookComparator();
    private TableColumn quickLookColumn;
    private static final int preferredWidth = 150;
    private static final int preferredHeight = 100;

    public QuicklookProvider() {
    }

    /**
     * Returns the {@link java.util.Comparator} for the data provided by this <code>DataProvider</code>.
     *
     * @return the comparator.
     */
    public Comparator getComparator() {
        return quickLookComparator;
    }

    public TableColumn getTableColumn() {
        if (quickLookColumn == null) {
            try {
                quickLookColumn = new TableColumn();
                quickLookColumn.setHeaderValue("Quick Look");        /*I18N*/
                quickLookColumn.setPreferredWidth(preferredWidth);
                quickLookColumn.setResizable(true);
                quickLookColumn.setCellRenderer(new QuickLookRenderer(preferredHeight));
                quickLookColumn.setCellEditor(new QuickLookEditor());
            } catch (Throwable e) {
                SystemUtils.LOG.severe("QuicklookProvider: " + e.getMessage());
            }
        }
        return quickLookColumn;
    }

    private static BufferedImage getImage(final ProductEntry productEntry) {
        if (productEntry.quickLookExists()) {
            final Quicklook quicklook = productEntry.getQuickLook();
            if (!quicklook.hasProduct() && !quicklook.hasCachedImage() && quicklook.getProductFile() != null) {
                try {
                    quicklook.setProduct(ProductIO.readProduct(quicklook.getProductFile()));
                } catch (IOException e) {
                    SystemUtils.LOG.warning("Quicklook unable to load product " + quicklook.getProductFile());
                }
            }
            return quicklook.getImage(ProgressMonitor.NULL);
        }
        return null;
    }

    private static class QuickLookRenderer extends DefaultTableCellRenderer {

        private final int rowHeight;
        private JLabel tableComponent;

        public QuickLookRenderer(final int height) {
            rowHeight = height + 3;
        }

        @Override
        public Component getTableCellRendererComponent(final JTable table,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row,
                                                       final int column) {
            try {
                if (tableComponent == null) {
                    tableComponent = (JLabel) super.getTableCellRendererComponent(table,
                            value,
                            isSelected,
                            hasFocus,
                            row,
                            column);
                    tableComponent.setText("");
                    tableComponent.setVerticalAlignment(SwingConstants.CENTER);
                    tableComponent.setHorizontalAlignment(SwingConstants.CENTER);
                }

                setBackground(table, isSelected);

                if (value == null) {
                    tableComponent.setIcon(null);
                    tableComponent.setText("");
                    return tableComponent;
                }

                if (value instanceof ProductEntry) {
                    final BufferedImage image = getImage((ProductEntry) value);
                    if (image == null) {
                        tableComponent.setIcon(null);
                        tableComponent.setText("Not available!");
                    } else {
                        final TableColumn tableColumn = table.getColumnModel().getColumn(column);
                        int cellWidth = tableColumn.getWidth();
                        int cellHeight = tableColumn.getWidth();
                        if (image.getHeight() > image.getWidth())
                            cellWidth = -1;
                        else
                            cellHeight = -1;
                        tableComponent.setIcon(
                                new ImageIcon(image.getScaledInstance(cellWidth, cellHeight, BufferedImage.SCALE_FAST)));
                        tableComponent.setText("");
                        setTableRowHeight(table, row);
                    }
                } else {
                    tableComponent.setIcon(null);
                }
            } catch (Throwable e) {
                SystemUtils.LOG.severe("QuicklookRenderer: " + e.getMessage());
            }
            return tableComponent;
        }

        private void setBackground(final JTable table, final boolean isSelected) {
            if (tableComponent == null) return;

            Color backGroundColor = table.getBackground();
            if (isSelected) {
                backGroundColor = table.getSelectionBackground();
            }
            tableComponent.setBorder(BorderFactory.createLineBorder(backGroundColor, 3));
            tableComponent.setBackground(backGroundColor);
        }

        private void setTableRowHeight(final JTable table, final int row) {
            if (table.getRowHeight(row) < rowHeight) {
                table.setRowHeight(row, rowHeight);
            }
        }
    }

    public static class QuickLookEditor extends AbstractCellEditor implements TableCellEditor {

        private final JScrollPane scrollPane;

        public QuickLookEditor() {

            scrollPane = new JScrollPane();
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.getViewport().setOpaque(false);
        }

        public Component getTableCellEditorComponent(final JTable table,
                                                     final Object value,
                                                     final boolean isSelected,
                                                     final int row,
                                                     final int column) {
            try {
                if (!(value instanceof ProductEntry)) {
                    return scrollPane;
                }
                final BufferedImage image = getImage((ProductEntry) value);
                if (image == null) {
                    return scrollPane;
                }
                scrollPane.setViewportView(
                        new JLabel(new ImageIcon(image.getScaledInstance(-1, -1, BufferedImage.SCALE_AREA_AVERAGING))));
                final Color backgroundColor = table.getSelectionBackground();
                scrollPane.setBackground(backgroundColor);
                scrollPane.setBorder(BorderFactory.createLineBorder(backgroundColor, 3));
            } catch (Throwable e) {
                SystemUtils.LOG.severe("QuicklookEditor: " + e.getMessage());
            }
            return scrollPane;
        }

        public Object getCellEditorValue() {
            return null;
        }
    }


    private static class QuickLookComparator implements Comparator {

        public int compare(final Object o1, final Object o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            }

            final Quicklook image1 = ((ProductEntry) o1).getQuickLook();
            final Quicklook image2 = ((ProductEntry) o2).getQuickLook();

            if (image1 == null) {
                return -1;
            } else if (image2 == null) {
                return 1;
            }

            if (!image1.hasImage()) {
                return -1;
            } else if (!image2.hasImage()) {
                return 1;
            }

            return 0;
        }
    }
}
