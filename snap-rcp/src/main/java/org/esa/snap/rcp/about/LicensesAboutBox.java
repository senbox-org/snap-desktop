/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.about;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a 'Licenses' tab in AboutBox which contains a table displaying all third party licenses
 * used in SNAP and the Toolboxes..
 *
 * @author olafd
 */
@AboutBox(displayName = "Licenses", position = 30)
class LicensesAboutBox extends JPanel {

    private ThirdPartyLicense[] licenses;

    LicensesAboutBox() {
        super(new BorderLayout(4, 4));
        setBorder(new EmptyBorder(4, 4, 4, 4));
        add(createLicensesPanel(), BorderLayout.SOUTH);
    }

    private JPanel createLicensesPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());


        JLabel infoText = new JLabel("<html>"
                                     + "The table below gives an overview of all 3rd-party licenses used by SNAP "
                                     + "and the Toolboxes.<br>"
        );

        Font boldFont = new Font(null, Font.BOLD, 12);
        infoText.setFont(boldFont);

        JScrollPane licensesScrollPane = createScrollPane(createLicensesTable());

        JPanel innerPanel = new JPanel(new BorderLayout(4, 4));
        innerPanel.add(infoText, BorderLayout.NORTH);
        innerPanel.add(licensesScrollPane, BorderLayout.SOUTH);

        panel.add(innerPanel, BorderLayout.CENTER);
        setVisible(true);

        return panel;
    }

    private JScrollPane createScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table,
                                                 JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        return scrollPane;
    }

    private JTable createLicensesTable() {
        licenses = new ThirdPartyLicense[0];
        licenses = getThirdPartyLicensesFromCsvTable();

        ThirdPartyLicensesTableModel licensesTableModel = new ThirdPartyLicensesTableModel(licenses, licenses.length);
        JTable licensesTable = new JTable(licensesTableModel);

        // add specific properties to table
        final JTableHeader tableHeader = licensesTable.getTableHeader();
        final int fontSize = tableHeader.getFont().getSize();
        tableHeader.setFont(new Font(null, Font.BOLD, fontSize));
        tableHeader.setOpaque(false);
        tableHeader.setBackground(Color.lightGray);
        for (int i = 0; i < licensesTable.getColumnCount(); i++) {
            licensesTable.getColumnModel().getColumn(i).setCellRenderer(new LineWrapCellRenderer());
        }

        // set proper default colunm sizes
        final TableColumnModel tableColumnModel = licensesTable.getColumnModel();
        final TableColumn nameColumn =
                tableColumnModel.getColumn(ThirdPartyLicensesTableModel.NAME_COL_INDEX);
        final TableColumn descrUseColumn =
                tableColumnModel.getColumn(ThirdPartyLicensesTableModel.DESCRIPTION_USE_COL_INDEX);
        final TableColumn iprOwnerColumn =
                tableColumnModel.getColumn(ThirdPartyLicensesTableModel.IPR_OWNER_COL_INDEX);
        final TableColumn licenseColumn =
                tableColumnModel.getColumn(ThirdPartyLicensesTableModel.LICENSE_COL_INDEX);

        // with regard to overall dialog size, a good sum of preferred widths is ~530
        nameColumn.setMinWidth(10);
        nameColumn.setPreferredWidth(150);

        descrUseColumn.setMinWidth(10);
        descrUseColumn.setPreferredWidth(160);

        iprOwnerColumn.setMinWidth(10);
        iprOwnerColumn.setPreferredWidth(110);

        licenseColumn.setMinWidth(10);
        licenseColumn.setPreferredWidth(110);

        // add sorter: enable sorting by String except for Description/Use column
        licensesTable.setAutoCreateRowSorter(true);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(licensesTable.getModel());
        licensesTable.setRowSorter(sorter);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(ThirdPartyLicensesTableModel.NAME_COL_INDEX, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        sorter.setSortable(ThirdPartyLicensesTableModel.DESCRIPTION_USE_COL_INDEX, false);
        sorter.sort();

        // add mouse motion listener to set hand cursor over cells which contain a link
        final MouseMotionListener motionListener = new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int column = licensesTable.columnAtPoint(e.getPoint());
                int row = licensesTable.rowAtPoint(e.getPoint());
                final String columnName = licensesTableModel.getColumnName(column);
                if (licenses[row].getUrlByTableColumnName(columnName) != null) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        };
        licensesTable.addMouseMotionListener(motionListener);

        // add mouse adapter to invoke external browser when clicking on a cell which contains a link
        final MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = licensesTable.rowAtPoint(e.getPoint());
                int column = licensesTable.columnAtPoint(e.getPoint());
                if (licensesTableModel.containsURLs(column)) {
                    final String columnName = licensesTableModel.getColumnName(column);
                    final String urlString = licenses[row].getUrlByTableColumnName(columnName);
                    if (urlString != null) {
                        try {
                            URI uri = new URI(urlString);
                            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                                try {
                                    Desktop.getDesktop().browse(uri);
                                } catch (Throwable e2) {
                                    JOptionPane.showMessageDialog(licensesTable.getParent(),
                                                                  "Failed to open URL:\n" + uri + ":\n" + e2.getMessage(),
                                                                  "Error",
                                                                  JOptionPane.ERROR_MESSAGE);
                                }
                            } else {
                                JOptionPane.showMessageDialog(licensesTable.getParent(),
                                                              "The desktop command 'browse' is not supported.",
                                                              "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (URISyntaxException e1) {
                            JOptionPane.showMessageDialog(licensesTable.getParent(),
                                                          "Cannot create URL from given String:\n" + urlString + ":\n" +
                                                                  e1.getMessage(),
                                                          "Error",
                                                          JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        };
        licensesTable.addMouseListener(mouseAdapter);

        return licensesTable;
    }

    private ThirdPartyLicense[] getThirdPartyLicensesFromCsvTable() {
        final ThirdPartyLicensesCsvTable licensesCsvTable = new ThirdPartyLicensesCsvTable();
        final int numLicenses = licensesCsvTable.getName().size();
        ThirdPartyLicense[] thirdPartyLicenses = new ThirdPartyLicense[numLicenses];
        for (int i = 0; i < numLicenses; i++) {
            if (licensesCsvTable.getName(i) != null) {
                String name = licensesCsvTable.getName(i);
                String descriptionUse = licensesCsvTable.getDescrUse(i);
                String iprOwner = licensesCsvTable.getIprOwner(i);
                String license =
                        licensesCsvTable.getLicense(i).toUpperCase().equals("NONE") ? null :
                                licensesCsvTable.getLicense(i);
                String iprOwnerUrl = licensesCsvTable.getIprOwnerUrl(i).toUpperCase().equals("NONE") ? null :
                        licensesCsvTable.getIprOwnerUrl(i);
                String licenseUrl = licensesCsvTable.getLicenseUrl(i).toUpperCase().equals("NONE") ? null :
                        licensesCsvTable.getLicenseUrl(i);

                thirdPartyLicenses[i] = new ThirdPartyLicense(name, descriptionUse, iprOwner, license,
                                                              iprOwnerUrl, licenseUrl);
            }
        }

        return thirdPartyLicenses;
    }

    /**
     * Class providing a proper line wrapping in table cells.
     *
     */
    private class LineWrapCellRenderer extends JTextArea implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            this.setText((String) value);
            this.setWrapStyleWord(true);
            this.setLineWrap(true);

            Font boldFont = new Font(null, Font.BOLD, this.getFont().getSize());

            final String columnName = table.getModel().getColumnName(column);
            if (licenses[row].getUrlByTableColumnName(columnName) != null) {
                this.setFont(boldFont);
                this.setForeground(Color.blue);
                this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else if (column == ThirdPartyLicensesTableModel.NAME_COL_INDEX) {
                this.setFont(boldFont);
                this.setForeground(table.getForeground());
            } else {
                this.setFont(table.getFont());
                this.setForeground(table.getForeground());
            }

            final FontMetrics fontMetrics = this.getFontMetrics(this.getFont());
            int fontHeight = fontMetrics.getHeight();
            int textLength = fontMetrics.stringWidth(this.getText());
            final int columnWidth = table.getColumnModel().getColumn(column).getWidth();
            int lines = textLength / columnWidth + 1;
            if (lines == 0) {
                lines = 1;
            }

            int height = fontHeight * lines + 5;
            if (height > table.getRowHeight(row)) {
                table.setRowHeight(row, height);
            }

            return this;
        }
    }
}