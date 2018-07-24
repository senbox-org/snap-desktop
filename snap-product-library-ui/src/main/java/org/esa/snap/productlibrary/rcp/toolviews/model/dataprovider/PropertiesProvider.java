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

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.productlibrary.db.ProductEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Comparator;

/**
 * Description of ProductPropertiesProvider
 */
public class PropertiesProvider implements DataProvider {

    private final DecimalFormat df = new DecimalFormat("#.##");
    private final DateFormat dateFormat = ProductData.UTC.createDateFormat("dd-MMM-yyyy");

    private static final String[] propertyLabels = new String[]{
            "File:",
            "Name:",
            "Type:",
            "Acquired:",
            "File Format:"
    };

    private final Comparator productPropertiesComparator;
    private TableColumn propertiesColumn;
    private final boolean minimalView;

    public PropertiesProvider(boolean minimalView) {
        this.minimalView = minimalView;
        this.productPropertiesComparator = new ProductPropertiesComparator();
    }

    public Comparator getComparator() {
        return productPropertiesComparator;
    }

    public void cleanUp(final ProductEntry entry) {
    }

    public TableColumn getTableColumn() {
        if (propertiesColumn == null) {
            try {
                propertiesColumn = new TableColumn();
                propertiesColumn.setResizable(true);
                propertiesColumn.setPreferredWidth(250);
                propertiesColumn.setHeaderValue("Product Properties");
                propertiesColumn.setCellRenderer(new ProductPropertiesRenderer());
            } catch (Throwable e) {
                SystemUtils.LOG.severe("PropertiesProvider: " + e.getMessage());
            }
        }
        return propertiesColumn;
    }

    private class ProductPropertiesRenderer extends JTable implements TableCellRenderer {

        private final int ROW_HEIGHT = minimalView ? 34 : 68;
        private final JPanel centeringPanel = new JPanel(new BorderLayout());
        private final Font valueFont, boldFont;

        public ProductPropertiesRenderer() {
            final DefaultTableModel dataModel = new DefaultTableModel();
            dataModel.setColumnCount(1);
            setRowHeight(14);
            dataModel.setRowCount(propertyLabels.length);

            setModel(dataModel);
            valueFont = getFont().deriveFont(Font.PLAIN, 12);
            boldFont = valueFont.deriveFont(Font.BOLD);
            getColumnModel().getColumn(0).setCellRenderer(new PropertyValueCellRenderer(valueFont, boldFont));

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
            try {
                String[] values = null;
                String toolTip = "";
                if (value instanceof ProductEntry) {
                    final ProductEntry entry = (ProductEntry) value;

                    String pixelSpacing = "";
                    if (entry.getRangeSpacing() > 0 && entry.getAzimuthSpacing() > 0) {
                        pixelSpacing = df.format(entry.getRangeSpacing()) + " x " +
                                df.format(entry.getAzimuthSpacing()) + " m";
                    }
                    final File file = entry.getFile();
                    final String fileSize = (entry.getFileSizeString() == null) ? "(" + (entry.getFileSize() / (1024 * 1024)) + " MB)"
                            : "(" + entry.getFileSizeString() + ")";

                    final String dateString = dateFormat.format(entry.getFirstLineTime().getAsDate());

                    String polStr = "";
                    final MetadataElement absRoot = entry.getMetadata();
                    if (absRoot != null) {
                        final String pol1 = absRoot.getAttributeString(AbstractMetadata.mds1_tx_rx_polar);
                        final String pol2 = absRoot.getAttributeString(AbstractMetadata.mds2_tx_rx_polar);
                        final String pol3 = absRoot.getAttributeString(AbstractMetadata.mds3_tx_rx_polar);
                        final String pol4 = absRoot.getAttributeString(AbstractMetadata.mds4_tx_rx_polar);
                        polStr = pol1;
                        if (!pol2.equals(AbstractMetadata.NO_METADATA_STRING))
                            polStr += ' ' + pol2;
                        if (!pol3.equals(AbstractMetadata.NO_METADATA_STRING))
                            polStr += ' ' + pol3;
                        if (!pol4.equals(AbstractMetadata.NO_METADATA_STRING))
                            polStr += ' ' + pol4;
                    } else {
                        polStr = entry.getAcquisitionMode(); // SciHub search result contains polarization mode here
                    }

                    if (minimalView) {
                        values = new String[]{
                                entry.getName(),
                                entry.getMission() + "   " + entry.getProductType() + "   " + entry.getPass() + "  " + polStr
                                        + "   " + dateString + "   " + pixelSpacing,
                        };
                    } else {
                        if (file != null) {
                            values = new String[]{
                                    file.getName(),
                                    entry.getName(),
                                    entry.getMission() + "   " + entry.getProductType() + "   " + entry.getPass() + "  " + polStr,
                                    dateString + "   " + pixelSpacing,
                                    entry.getFileFormat() + "   " + fileSize
                            };
                        } else {
                            values = new String[]{
                                    entry.getName(),
                                    entry.getMission() + "   " + entry.getProductType() + "   " + entry.getPass() + "  " + polStr,
                                    dateString + "   " + pixelSpacing,
                                    entry.getFileFormat() + "   " + fileSize
                            };
                        }
                    }

                    for (int i = 0; i < values.length; i++) {
                        setValueAt(values[i], i, 0);
                    }
                    if (file != null) {
                        toolTip = file.getAbsolutePath();
                    }
                } else if (value == null) {
                    for (int i = 0; i < propertyLabels.length; i++) {
                        setValueAt(null, i, 0);
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
            } catch (Throwable e) {
                SystemUtils.LOG.severe("ProductPropertiesRenderer: " + e.getMessage());
            }
            return centeringPanel;
        }

        private void adjustCellSize(JTable table, int row, int column, String[] values) {
            setRowHeight(table, row, ROW_HEIGHT);

            final int labelsLength = getMaxStringLength(propertyLabels, getFontMetrics(getFont()));
            int valuesLength = 50;
            if (values != null) {
                valuesLength = Math.min(200, getMaxStringLength(values, getFontMetrics(valueFont)));
            }
            int preferredWidth = labelsLength + valuesLength;
            final TableColumn valueColumn = table.getColumnModel().getColumn(column);
            final int valueColWidth = Math.max(valueColumn.getWidth(), preferredWidth);
            increasePreferredColumnWidth(valueColumn, valueColWidth);
        }

        private void increasePreferredColumnWidth(TableColumn column, int length) {
            if (column.getPreferredWidth() < length) {
                column.setPreferredWidth(length);
            }
        }

        private void setRowHeight(final JTable table, final int row, final int rowHeight) {
            final int currentRowHeight = table.getRowHeight(row);
            if (currentRowHeight < rowHeight) {
                table.setRowHeight(rowHeight);
            }
        }

        private int getMaxStringLength(final String[] strings, final FontMetrics fontMetrics) {
            int maxWidth = Integer.MIN_VALUE;
            for (String string : strings) {
                if (string == null)
                    continue;
                final int width = SwingUtilities.computeStringWidth(fontMetrics, string);
                maxWidth = Math.max(width, maxWidth);
            }
            return maxWidth;
        }

        private class PropertyValueCellRenderer extends DefaultTableCellRenderer {

            private final Font font, boldFont;

            public PropertyValueCellRenderer(final Font font, final Font boldFont) {
                this.font = font;
                this.boldFont = boldFont;
            }

            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                           final boolean isSelected, final boolean hasFocus,
                                                           final int row, final int column) {
                try {
                    final JLabel jLabel = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                            row, column);
                    if (jLabel != null) {
                        jLabel.setHorizontalAlignment(JLabel.LEFT);
                        if (row == 0 && column == 0)
                            jLabel.setFont(boldFont);
                        else
                            jLabel.setFont(font);
                        return jLabel;
                    }
                } catch (Throwable e) {
                    SystemUtils.LOG.severe("PropertyValueCellRenderer: " + e.getMessage());
                }
                return null;
            }
        }
    }

    private static class ProductPropertiesComparator implements Comparator {

        public int compare(final Object o1, final Object o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            }

            final ProductEntry s1 = (ProductEntry) o1;
            final ProductEntry s2 = (ProductEntry) o2;

            return s1.getName().compareTo(s2.getName());
        }
    }
}
