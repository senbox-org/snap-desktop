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
import org.esa.snap.productlibrary.rcp.toolviews.AOIMonitoring.model.AOI;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Comparator;

/**
 * Description of AOIDetailsProvider
 */
public class AOIDetailsProvider implements DataProvider {

    private static final DecimalFormat df = new DecimalFormat("#.##");

    private static final String[] detailsLables = new String[]{
            "Name:",
            "Input Folder:",
            "Output Folder:",
            "Processing Graph"
    };

    private final Comparator<AOI> aoiComparator = new AOIComparator();
    private TableColumn detailsColumn;

    public Comparator<AOI> getComparator() {
        return aoiComparator;
    }

    public void cleanUp(final ProductEntry entry) {
    }

    public TableColumn getTableColumn() {
        if (detailsColumn == null) {
            detailsColumn = new TableColumn();
            detailsColumn.setResizable(true);
            detailsColumn.setPreferredWidth(350);
            detailsColumn.setHeaderValue("Areas of Interest");
            detailsColumn.setCellRenderer(new AOIDetailsRenderer());
        }
        return detailsColumn;
    }

    private static class AOIDetailsRenderer extends JTable implements TableCellRenderer {

        private static final int ROW_HEIGHT = 100;
        private final JPanel centeringPanel = new JPanel(new BorderLayout());
        private final Font valueFont;

        public AOIDetailsRenderer() {
            final DefaultTableModel dataModel = new DefaultTableModel();
            dataModel.setColumnCount(2);
            dataModel.setRowCount(detailsLables.length);

            for (int i = 0; i < detailsLables.length; i++) {
                dataModel.setValueAt(detailsLables[i], i, 0);
                dataModel.setValueAt("", i, 1);
            }

            setModel(dataModel);
            valueFont = getFont().deriveFont(Font.BOLD);
            getColumnModel().getColumn(1).setCellRenderer(new PropertyValueCellRenderer(valueFont));
            getColumnModel().getColumn(0).setMaxWidth(120);

            //this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            getTableHeader().setVisible(false);
            setShowHorizontalLines(false);
            setShowVerticalLines(false);
        }

        public Component getTableCellRendererComponent(final JTable table,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row, final int column) {
            String[] values = null;
            String toolTip = "";
            if (value instanceof AOI) {
                final AOI entry = (AOI) value;

                values = new String[]{
                        entry.getName(),
                        entry.getInputFolder(),
                        entry.getOutputFolder(),
                        entry.getProcessingGraph()
                };
                for (int i = 0; i < values.length; i++) {
                    setValueAt(values[i], i, 1);
                }
                //toolTip = file.getAbsolutePath();
            } else if (value == null) {
                for (int i = 0; i < detailsLables.length; i++) {
                    setValueAt(null, i, 1);
                }
            }

            final Color backgroundColor;
            final Color foregroundColor;
            if (isSelected) {
                backgroundColor = table.getSelectionBackground();
                foregroundColor = table.getSelectionForeground();
            } else {
                backgroundColor = table.getBackground();
                foregroundColor = table.getForeground();
            }
            setForeground(foregroundColor);
            setBackground(backgroundColor);
            centeringPanel.setForeground(foregroundColor);
            centeringPanel.setBackground(backgroundColor);
            centeringPanel.setBorder(BorderFactory.createLineBorder(backgroundColor, 3));
            centeringPanel.add(this, BorderLayout.CENTER);

            centeringPanel.setToolTipText(toolTip);
            adjustCellSize(table, row, column, values);
            return centeringPanel;
        }

        private void adjustCellSize(JTable table, int row, int column, String[] values) {
            setRowHeight(table, row, ROW_HEIGHT);

            final int lablesLength = getMaxStringLength(detailsLables, getFontMetrics(getFont()));
            int columnIndex = 0;
            increasePreferredColumnWidth(getColumnModel().getColumn(columnIndex), lablesLength);

            int valuesLength = 50;
            if (values != null) {
                valuesLength = Math.min(300, getMaxStringLength(values, getFontMetrics(valueFont)));
                increasePreferredColumnWidth(getColumnModel().getColumn(1), valuesLength);
            }
            int preferredWidth = lablesLength + valuesLength;
            //preferredWidth = (int) (preferredWidth + (preferredWidth * 0.01f));
            final TableColumn valueColumn = table.getColumnModel().getColumn(column);
            final int valueColWidth = Math.max(valueColumn.getWidth(), preferredWidth);
            increasePreferredColumnWidth(valueColumn, valueColWidth);
        }

        private static void increasePreferredColumnWidth(TableColumn column, int length) {
            if (column.getPreferredWidth() < length) {
                column.setPreferredWidth(length);
            }
        }

        private static void setRowHeight(final JTable table, final int row, final int rowHeight) {
            final int currentRowHeight = table.getRowHeight(row);
            if (currentRowHeight < rowHeight) {
                table.setRowHeight(rowHeight);
            }
        }

        private static int getMaxStringLength(final String[] strings, final FontMetrics fontMetrics) {
            int maxWidth = Integer.MIN_VALUE;
            for (String string : strings) {
                if (string == null) {
                    string = String.valueOf(string);
                }
                final int width = SwingUtilities.computeStringWidth(fontMetrics, string);
                maxWidth = Math.max(width, maxWidth);
            }
            return maxWidth;
        }

        private static class PropertyValueCellRenderer extends DefaultTableCellRenderer {

            private final Font _font;

            public PropertyValueCellRenderer(final Font font) {
                _font = font;
            }

            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                           final boolean isSelected, final boolean hasFocus,
                                                           final int row, final int column) {
                final JLabel jLabel = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                                                                                   row, column);
                jLabel.setHorizontalAlignment(JLabel.LEFT);
                if (row == 0)
                    jLabel.setFont(_font);
                return jLabel;
            }
        }
    }

    private static class AOIComparator implements Comparator<AOI> {

        public int compare(final AOI o1, final AOI o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            }

            return o1.getName().compareTo(o2.getName());
        }
    }
}