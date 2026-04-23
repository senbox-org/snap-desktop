package org.esa.snap.rcp.statistics;

import org.esa.snap.core.util.StringUtils;
import org.esa.snap.ui.GridBagUtils;
import org.openide.windows.TopComponent;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;

//import org.esa.snap.ui.application.ToolView;

/**
 * Created by knowles on 4/19/17.
 */
class StatisticsSpreadsheetPagePanel extends PagePanel {

    private static final String TITLE_PREFIX = "Statistics Spreadsheet";

    private final StatisticsSpreadsheetPagePanel.PopupHandler popupHandler;
    private final StringBuilder resultText;
    private boolean init;

    private Object[][] statsSpreadsheet;
    private StatisticsCriteriaPanel statisticsCriteriaPanel;
    private StatisticsPanel statisticsPanel;


    public StatisticsSpreadsheetPagePanel(final TopComponent parentDialog, String helpID, StatisticsCriteriaPanel statisticsCriteriaPanel, Object[][] statsSpreadsheet, StatisticsPanel statisticsPanel) {
        super(parentDialog, helpID, TITLE_PREFIX);

        this.statsSpreadsheet = statsSpreadsheet;
        this.statisticsCriteriaPanel = statisticsCriteriaPanel;
        this.statisticsPanel = statisticsPanel;

//        setMinimumSize(new Dimension(1000, 390));
        resultText = new StringBuilder("");
        popupHandler = new StatisticsSpreadsheetPagePanel.PopupHandler();
//        if (visatApp != null) {
//            this.configuration = visatApp.getPreferences();
//        }

    }

    @Override
    protected void initComponents() {
        init = true;

        JPanel statsSpreadsheetPane = statsSpreadsheetPanel();

        add(statsSpreadsheetPane);
        setPreferredSize(statsSpreadsheetPane.getPreferredSize());

        resultText.setLength(0);
        resultText.append(createText());

    }


    @Override
    protected void updateComponents() {
        if (!init) {
            initComponents();
        }

    }

    @Override
    protected String getDataAsText() {
        return resultText.toString();
    }


    private class PopupHandler extends MouseAdapter {

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == 2 || e.isPopupTrigger()) {
                final JPopupMenu menu = new JPopupMenu();
                menu.add(createCopyDataToClipboardMenuItem());
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }


    private JPanel statsSpreadsheetPanel() {

        JPanel pane = GridBagUtils.createPanel();

        if (statsSpreadsheet == null) {
            return pane;
        }



        TableModel tableModel = new DefaultTableModel(statsSpreadsheet, statsSpreadsheet[0]) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex <= 1 ? String.class : Number.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };


        final JTable table = new JTable(tableModel);
        table.setDefaultRenderer(Number.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Float || value instanceof Double) {
                    setHorizontalTextPosition(RIGHT);
                    setText(getFormattedValue((Number) value));
                }
                return label;
            }

            private String getFormattedValue(Number value) {
                if (value.doubleValue() < 0.001 && value.doubleValue() > -0.001 && value.doubleValue() != 0.0) {
                    return new DecimalFormat("0.####E0").format(value.doubleValue());
                }
                String format = "%." + Integer.toString(statisticsCriteriaPanel.decimalPlaces()) + "f";

                return String.format(format, value.doubleValue());
            }
        });
        table.addMouseListener(popupHandler);


        FontMetrics fm = table.getFontMetrics(table.getFont());
        TableColumn column = null;

        // int colPreferredWidthData = fm.stringWidth("StandardDeviation(LogBinned):") + 10;

        StringBuilder sampleEntry = new StringBuilder("");
        for (int i = 1; i < statisticsCriteriaPanel.colCharWidth(); i++) {
            sampleEntry.append("n");
        }

        int colPreferredWidthData = fm.stringWidth(sampleEntry.toString());
        int bufferWidth = fm.stringWidth("nn");


        for (int fieldIdx = 0; fieldIdx < statsSpreadsheet[0].length; fieldIdx++) {
            column = table.getColumnModel().getColumn(fieldIdx);
            int width = 0;

            for (int rowIdx = 0; rowIdx < statsSpreadsheet.length; rowIdx++) {
                Object spreadsheetCell = statsSpreadsheet[rowIdx][fieldIdx];
                int currWidth = 0;

                if (spreadsheetCell != null && StringUtils.isNotNullAndNotEmpty(spreadsheetCell.toString())) {

                    if (spreadsheetCell instanceof Float || spreadsheetCell instanceof  Double) {
                        if (statisticsCriteriaPanel.colCharWidth() < 10) {
                            String valueFormatted = getFormattedValue((Number) spreadsheetCell);
                            currWidth = fm.stringWidth(valueFormatted.trim()) + bufferWidth;
                        } else {
                            currWidth = colPreferredWidthData;
                        }

                    } else {
                        if (statisticsCriteriaPanel.colCharWidth() < 10) {
                            currWidth = fm.stringWidth(spreadsheetCell.toString()) + bufferWidth;
                        } else {
                            currWidth = colPreferredWidthData;
                        }
                    }

                    if (currWidth > 0 && currWidth > width) {
                        width = currWidth;
                    }
                }
            }

            column.setPreferredWidth(width);
            column.setMaxWidth(width);
            column.setMinWidth(width);
        }






        //  table.setPreferredSize(new Dimension(tableWidth, table.getRowCount() * table.getRowHeight()));
        //     pane.setBorder(UIUtils.createGroupBorder("Statistics Spreadsheet")); /*I18N*/
        GridBagConstraints gbcMain = GridBagUtils.createConstraints("");
        gbcMain.gridx = 0;
        gbcMain.gridy = 0;
        gbcMain.weighty = 1.0;
        gbcMain.insets.bottom = 5;
        gbcMain.anchor = GridBagConstraints.NORTHWEST;
        pane.add(table, gbcMain);
        //  pane.setMinimumSize(new Dimension(tableWidth, table.getRowCount() * table.getRowHeight()));


        return pane;
    }


    private String createText() {

        if (statsSpreadsheet == null || statsSpreadsheet.length == 0 || statsSpreadsheet[0].length == 0) {
            return "No Statistics Processed";
        }

        final StringBuilder sb = new StringBuilder();


        int numRows = statsSpreadsheet.length;
        int numCols = statsSpreadsheet[0].length;

        for (int rowIdx = 0; rowIdx < statsSpreadsheet.length; rowIdx++) {

            for (int colIdx = 0; colIdx < statsSpreadsheet[0].length; colIdx++) {
                Object valueObject = statsSpreadsheet[rowIdx][colIdx];

                if (valueObject == null) {
                    sb.append("");
                } else if (valueObject instanceof Float || valueObject instanceof Double) {
                    String valueFormatted = getFormattedValue((Number) valueObject);
                    sb.append(valueFormatted);
                } else {
                    sb.append(valueObject.toString());
                }

                if (colIdx < statsSpreadsheet[0].length - 1) {
                    sb.append("\t");
                }

            }

            sb.append("\n");
        }


        return sb.toString();
    }


    private String getFormattedValue(Number value) {
        if (value.doubleValue() < 0.001 && value.doubleValue() > -0.001 && value.doubleValue() != 0.0) {
            return new DecimalFormat("0.####E0").format(value.doubleValue());
        }
        String format = "%." + Integer.toString(statisticsCriteriaPanel.decimalPlaces()) + "f";

        return String.format(format, value.doubleValue());
    }

}