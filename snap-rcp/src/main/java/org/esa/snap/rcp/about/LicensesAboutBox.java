/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.about;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static org.esa.snap.core.util.SystemUtils.*;

/**
 * Provides a 'Licenses' tab in AboutBox which contains a table displaying all third party licenses
 * used in SNAP and the Toolboxes..
 *
 * @author olafd
 */
class LicensesAboutBox extends JPanel {

    private static final String THIRD_PARTY_LICENSE_DEFAULT_FILE_NAME = "THIRDPARTY_LICENSES.txt";

    private ThirdPartyLicense[] licenses;

    LicensesAboutBox() {
        super(new BorderLayout(4, 4));
        setBorder(new EmptyBorder(4, 4, 4, 4));
        createLicensesPanel();
    }

    private void createLicensesPanel() {
        JLabel infoText = new JLabel("<html>"
                                     + "<b>The table below gives an overview of all 3rd-party licenses used by SNAP "
                                     + "and the ESA Toolboxes.<br>"
                                     + "The dependencies of other plugins are not considered in this list.</b>"
        );

        JComponent licensesComponent;
        Path licensesFile = getApplicationHomeDir().toPath().resolve(THIRD_PARTY_LICENSE_DEFAULT_FILE_NAME);
        try {
            JTable table = createLicensesTable(licensesFile);
            licensesComponent = createScrollPane(table);
        } catch (IOException e) {
            String msg = "Error: Cloud not read licenses from " + licensesFile.toAbsolutePath().toString();
            LOG.log(Level.WARNING, msg, e);
            JLabel msgLabel = new JLabel(msg);
            msgLabel.setVerticalAlignment(SwingConstants.TOP);
            licensesComponent = msgLabel;
        }


        add(infoText, BorderLayout.NORTH);
        add(licensesComponent, BorderLayout.CENTER);

        setVisible(true);
    }

    private JScrollPane createScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table,
                                                 JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        Dimension d = table.getPreferredSize();
        scrollPane.setPreferredSize(new Dimension(d.width, table.getRowHeight() * 10));

        return scrollPane;
    }

    private JTable createLicensesTable(Path licensesFile) throws IOException {
        licenses = getThirdPartyLicensesFromCsvTable(licensesFile);

        ThirdPartyLicensesTableModel licensesTableModel = new ThirdPartyLicensesTableModel(licenses);
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

        // set proper default column sizes
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
        licensesTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        return licensesTable;
    }

    private ThirdPartyLicense[] getThirdPartyLicensesFromCsvTable(Path licensesFile) throws IOException {
        try (BufferedReader licensesReader = Files.newBufferedReader(licensesFile)) {
            final ThirdPartyLicensesCsvTable licensesCsvTable = new ThirdPartyLicensesCsvTable(licensesReader);
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

    }

    /**
     * Class providing a proper line wrapping in table cells.
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