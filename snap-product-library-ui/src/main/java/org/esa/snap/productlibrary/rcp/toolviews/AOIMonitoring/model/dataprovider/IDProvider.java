/*
 * Copyright (C) 2017 Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.productlibrary.rcp.toolviews.AOIMonitoring.model.dataprovider;

import org.esa.snap.productlibrary.db.ProductEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Comparator;

public class IDProvider implements DataProvider {

    private final Comparator<ProductEntry> comparator = new IdComparator();
    private TableColumn column;

    public Comparator<ProductEntry> getComparator() {
        return comparator;
    }

    public void cleanUp(final ProductEntry entry) {
    }

    public TableColumn getTableColumn() {
        if (column == null) {
            column = new TableColumn();
            column.setHeaderValue("ID");
            column.setPreferredWidth(34);
            column.setResizable(false);
            column.setCellRenderer(new IDCellRenderer());
        }
        return column;
    }

    private static class IDCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(final JTable table,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row, final int column) {
            final ProductEntry entry = (ProductEntry) value;
            if (entry != null) {
                final String text = String.valueOf(entry.getId());

                final JLabel jlabel = (JLabel) super
                        .getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);

                jlabel.setFont(jlabel.getFont().deriveFont(Font.BOLD));
                if(entry.getFile() != null) {
                    jlabel.setToolTipText(entry.getFile().getAbsolutePath());
                }
                return jlabel;
            }
            return null;
        }
    }

    private static class IdComparator implements Comparator<ProductEntry> {

        public int compare(final ProductEntry o1, final ProductEntry o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            }

            if (o1.getId() < o2.getId())
                return -1;
            return 1;
        }
    }
}