/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.about;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
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
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
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

        DefaultTableCellRenderer tableCellRenderer = new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value, boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                Font font = new Font(null, Font.BOLD, c.getFont().getSize());
                final String columnName = licensesTableModel.getColumnName(column);
                if (licenses[row].getUrlByTableColumnName(columnName) != null) {
                    c.setFont(font);
                    c.setForeground(Color.blue);
                    c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    c.setFont(licensesTable.getFont());
                    c.setForeground(licensesTable.getForeground());
                }
                return c;
            }

        };

        final int fontSize = licensesTable.getTableHeader().getFont().getSize();
        licensesTable.getTableHeader().setFont(new Font(null, Font.BOLD, fontSize));
        for (int i = 0; i < licensesTable.getColumnCount(); i++) {
            if (licensesTableModel.containsURLs(i)) {
                licensesTable.getColumnModel().getColumn(i).setCellRenderer(tableCellRenderer);
            }
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

        // todo: setup proper table layout (i.e. size)
//        final TableColumnModel tableColumnModel = licensesTable.getColumnModel();
//
//        final TableColumn nameColumn = tableColumnModel.getColumn(ThirdPartyLicensesTableModel.NAME_COL_INDEX);
//        final TableColumn descrUseColumn = tableColumnModel.getColumn(ThirdPartyLicensesTableModel.DESCRIPTION_USE_COL_INDEX);
//        final TableColumn iprOwnerColumn = tableColumnModel.getColumn(ThirdPartyLicensesTableModel.IPR_OWNER_COL_INDEX);
//        final TableColumn licenseColumn = tableColumnModel.getColumn(ThirdPartyLicensesTableModel.LICENSE_COL_INDEX);
//        final TableColumn licenseCompatibleColumn =
//                tableColumnModel.getColumn(ThirdPartyLicensesTableModel.COMPATIBLE_WITH_SNAP_GPL_COL_INDEX);
//        final TableColumn commentsColumn = tableColumnModel.getColumn(ThirdPartyLicensesTableModel.COMMENTS_COL_INDEX);
//
//        nameColumn.setMinWidth(120);
//        nameColumn.setPreferredWidth(120);
//
//        descrUseColumn.setMinWidth(120);
//        descrUseColumn.setPreferredWidth(120);
//
//        iprOwnerColumn.setMinWidth(120);
//        iprOwnerColumn.setPreferredWidth(120);
//
//        licenseColumn.setMinWidth(60);
//        licenseColumn.setPreferredWidth(60);
//
//        licenseCompatibleColumn.setMinWidth(50);
//        licenseCompatibleColumn.setPreferredWidth(50);
//
//        commentsColumn.setMinWidth(60);
//        commentsColumn.setPreferredWidth(60);

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

}
