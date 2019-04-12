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

/**
 * @author olafd
 *
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

        JScrollPane licensesScrollPane = createScrollPane(createLicensesTable());
        panel.add(licensesScrollPane, BorderLayout.CENTER);
        setVisible(true);

        return panel;
    }

    private JScrollPane createScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        return scrollPane;
    }

    private JTable createLicensesTable() {
        licenses = new ThirdPartyLicense[0];
        licenses = getThirdPartyLicensesFromInstallDir();

        ThirdPartyLicensesTableModel licensesTableModel = new ThirdPartyLicensesTableModel(licenses, licenses.length);
        JTable licensesTable = new JTable(licensesTableModel);

        MouseMotionListener motionListener = new MouseMotionListener() {
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

        final JTableHeader tableHeader = licensesTable.getTableHeader();
        final int fontSize = tableHeader.getFont().getSize();
        tableHeader.setFont(new Font(null, Font.BOLD, fontSize));
        tableHeader.setResizingAllowed(true);
        final int tableHeaderWidth = tableHeader.getPreferredSize().width;
        final int tableHeaderHeight = tableHeader.getPreferredSize().height;
        tableHeader.setPreferredSize(new Dimension(tableHeaderWidth, 2*tableHeaderHeight));
        for (int i = 0; i < licensesTable.getColumnCount(); i++) {
            licensesTable.getColumnModel().getColumn(i).setCellRenderer(new LineWrapCellRenderer());
        }

        licensesTable.addMouseListener(new MouseAdapter() {
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


        });

        final TableColumnModel tableColumnModel = licensesTable.getColumnModel();

        final TableColumn nameColumn = tableColumnModel.getColumn(ThirdPartyLicensesTableModel.NAME_COL_INDEX);
        final TableColumn descrUseColumn = tableColumnModel.getColumn(ThirdPartyLicensesTableModel.DESCRIPTION_USE_COL_INDEX);
        final TableColumn iprOwnerColumn = tableColumnModel.getColumn(ThirdPartyLicensesTableModel.IPR_OWNER_COL_INDEX);
        final TableColumn licenseColumn = tableColumnModel.getColumn(ThirdPartyLicensesTableModel.LICENSE_COL_INDEX);
        final TableColumn licenseCompatibleColumn =
                tableColumnModel.getColumn(ThirdPartyLicensesTableModel.COMPATIBLE_WITH_SNAP_GPL_COL_INDEX);
        final TableColumn commentsColumn = tableColumnModel.getColumn(ThirdPartyLicensesTableModel.COMMENTS_COL_INDEX);

        nameColumn.setMinWidth(10);
        nameColumn.setPreferredWidth(80);

        descrUseColumn.setMinWidth(10);
        descrUseColumn.setPreferredWidth(100);

        iprOwnerColumn.setMinWidth(10);
        iprOwnerColumn.setPreferredWidth(80);

        licenseColumn.setMinWidth(10);
        licenseColumn.setPreferredWidth(80);

        licenseCompatibleColumn.setMinWidth(10);
        licenseCompatibleColumn.setPreferredWidth(80);

        commentsColumn.setMinWidth(10);
        commentsColumn.setPreferredWidth(110);

        return licensesTable;
    }

    private ThirdPartyLicense[] getThirdPartyLicensesFromInstallDir() {
        final ThirdPartyLicensesCsvTable licensesCsvTable = new ThirdPartyLicensesCsvTable();
        final int numLicenses = licensesCsvTable.getLength();
        ThirdPartyLicense[] thirdPartyLicenses = new ThirdPartyLicense[numLicenses];
        for (int i = 0; i < numLicenses; i++) {
            String name = licensesCsvTable.getName(i);
            String descriptionUse = licensesCsvTable.getDescrUse(i);
            String iprOwner = licensesCsvTable.getIprOwner(i);
            String license =
                    licensesCsvTable.getLicense(i).toUpperCase().equals("NONE") ? null : licensesCsvTable.getLicense(i);
            String isSnapCompatible = licensesCsvTable.getCompatibleWithSnapGpl(i).toUpperCase().equals("NONE") ? null :
                    licensesCsvTable.getCompatibleWithSnapGpl(i);
            String comments = licensesCsvTable.getComment(i).toUpperCase().equals("NONE") ? null :
                    licensesCsvTable.getComment(i);
            String iprOwnerUrl = licensesCsvTable.getIprOwnerUrl(i).toUpperCase().equals("NONE") ? null :
                    licensesCsvTable.getIprOwnerUrl(i);
            String licenseUrl = licensesCsvTable.getLicenseUrl(i).toUpperCase().equals("NONE") ? null :
                    licensesCsvTable.getLicenseUrl(i);
            String commentsUrl = licensesCsvTable.getCommentUrl(i).toUpperCase().equals("NONE") ? null :
                    licensesCsvTable.getCommentUrl(i);

            thirdPartyLicenses[i] = new ThirdPartyLicense(name, descriptionUse, iprOwner, license, isSnapCompatible,
                    comments, iprOwnerUrl, licenseUrl, commentsUrl);
        }

        return thirdPartyLicenses;
    }

    private class LineWrapCellRenderer extends JTextArea implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            this.setText((String)value);
            this.setWrapStyleWord(true);
            this.setLineWrap(true);

            Font boldFont = new Font(null, Font.BOLD, this.getFont().getSize());

            final String columnName = table.getModel().getColumnName(column);
            if (licenses[row].getUrlByTableColumnName(columnName) != null) {
                this.setFont(boldFont);
                this.setForeground(Color.blue);
                this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                this.setFont(table.getFont());
                this.setForeground(table.getForeground());
            }

            int fontHeight = this.getFontMetrics(this.getFont()).getHeight();
            int textLength = this.getText().length();
            int lines = textLength / this.getColumnWidth();
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