package org.esa.snap.ui.classfraction;

import com.bc.ceres.core.ProgressMonitor;
import eu.esa.snap.core.renderer.classfraction.ClassFractionBandStatistics;
import eu.esa.snap.core.renderer.classfraction.ClassFractionStatistics;
import eu.esa.snap.core.renderer.classfraction.ClassFractionStatisticsResult;
import eu.esa.snap.core.renderer.classfraction.ClassFractionValueDistribution;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.ui.help.HelpDisplayer;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;


/**
 * Displays diagnostic original-band statistics and lets the user recompute them for a compatible ROI.
 */
public final class ClassFractionStatisticsDialog extends JDialog {


    private static final Object WHOLE_SCENE = "Whole scene";
    private final ClassFractionStatisticsController controller;
    private final JComboBox<Object> roiCombo;
    private final JLabel statusLabel;
    private final StatisticsTableModel tableModel;
    private final JTable table;
    private final HistogramPanel histogramPanel;
    private final JButton refreshButton;
    private final JButton exportButton;
    private final JButton helpButton;
    private Mask displayedRoi;
    private ClassFractionStatisticsResult result;
    private boolean computing;


    public ClassFractionStatisticsDialog(Window parent, ClassFractionStatisticsController controller, ClassFractionStatisticsResult initialResult) {
        super(parent, "Class Fraction / Probability Statistics", ModalityType.MODELESS);
        this.controller = controller;
        this.roiCombo = new JComboBox<>();
        roiCombo.addItem(WHOLE_SCENE);

        for (Mask mask : controller.getCompatibleRoiMasks()) {
            roiCombo.addItem(mask);
        }

        roiCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, value instanceof Mask ? ((Mask) value).getName() : value, index, selected, focus);
            }
        });

        this.statusLabel = new JLabel();
        this.tableModel = new StatisticsTableModel();
        this.table = new JTable(tableModel);
        this.histogramPanel = new HistogramPanel();
        this.refreshButton = new JButton("Refresh statistics");
        this.exportButton = new JButton("Export CSV...");
        this.helpButton = new JButton("Help");
        refreshButton.addActionListener(event -> computeSelectedRoi());
        exportButton.addActionListener(event -> exportCsv());
        helpButton.addActionListener(event -> HelpDisplayer.show("classFractionStatistics"));
        roiCombo.addActionListener(event -> updateRefreshEnablement());

        setContentPane(createContent());
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateHistogramForSelection();
            }
        });
        setResult(initialResult, null);
        pack();
        setLocationRelativeTo(parent);
    }

    private JPanel createContent() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        controls.add(new JLabel("ROI:"));
        controls.add(roiCombo);
        controls.add(refreshButton);
        controls.add(exportButton);
        controls.add(helpButton);
        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.add(statusLabel, BorderLayout.NORTH);
        bottom.add(histogramPanel, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(980, 570));
        return panel;
    }

    private void computeSelectedRoi() {
        Mask roi = getSelectedRoi();
        computing = true;
        refreshButton.setEnabled(false);
        new SwingWorker<ClassFractionStatisticsResult, Void>() {
            @Override
            protected ClassFractionStatisticsResult doInBackground() {
                return controller.compute(roi, ClassFractionStatistics.DEFAULT_HISTOGRAM_BIN_COUNT, ProgressMonitor.NULL);
            }

            @Override
            protected void done() {
                computing = false;
                try {
                    setResult(get(), roi);
                } catch (Exception e) {
                    statusLabel.setText("Statistics could not be calculated: " + e.getMessage());
                    updateRefreshEnablement();
                }
            }
        }.execute();
    }

    private Mask getSelectedRoi() {
        return roiCombo.getSelectedItem() instanceof Mask ? (Mask) roiCombo.getSelectedItem() : null;
    }

    private void setResult(ClassFractionStatisticsResult result, Mask roi) {
        this.result = result;
        this.displayedRoi = roi;
        tableModel.setResult(result);
        if (tableModel.getRowCount() > 0) {
            table.getSelectionModel().setSelectionInterval(0, 0);
        }
        statusLabel.setText(statusText(result));
        exportButton.setEnabled(true);
        updateRefreshEnablement();
    }

    private String statusText(ClassFractionStatisticsResult statistics) {
        String scope = statistics.getRoiName() == null ? "Whole scene" : "ROI '" + statistics.getRoiName() + "'";
        if (statistics.getSelectedPixelCount() == 0) {
            return scope + ": no pixels are selected.";
        }
        if (statistics.getValidPixelCount() == 0) {
            return scope + ": no common-valid pixels remain after applying the selected validity rule.";
        }
        String text = scope + " — selected: " + statistics.getSelectedPixelCount()
                + ", common-valid: " + statistics.getValidPixelCount()
                + ", excluded by common validity: " + statistics.getInvalidPixelCount() + ".";
        if (controller.getConfiguration().getClassBandGroup().getValidityPolicy()
                == eu.esa.snap.core.renderer.classfraction.ClassBandGroupConfig.ValidityPolicy.FINITE_AND_DECLARED_NODATA) {
            text += " Source validity masks are ignored by the selected policy.";
        }
        return text;
    }

    private void updateRefreshEnablement() {
        refreshButton.setEnabled(!computing && result != null && getSelectedRoi() != displayedRoi);
    }

    private void updateHistogramForSelection() {
        histogramPanel.setStatistics(tableModel.getRow(table.getSelectedRow()));
    }

    private void exportCsv() {
        if (result == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Class Fraction / Probability Statistics");
        chooser.setSelectedFile(new File("class-fraction-statistics.csv"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
            return;
        }

        File file = ensureCsvExtension(chooser.getSelectedFile());
        if (file.exists() && JOptionPane.showConfirmDialog(this, "Overwrite existing file?", "Export CSV",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            Files.writeString(file.toPath(), toCsv(result), StandardCharsets.UTF_8);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "The statistics could not be exported: " + e.getMessage(), "Export CSV", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static File ensureCsvExtension(File file) {
        return file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".csv") ? file : new File(file.getParentFile(), file.getName() + ".csv");
    }

    static String toCsv(ClassFractionStatisticsResult result) {
        StringBuilder csv = new StringBuilder();
        csv.append("Metric,Value\n");

        if (!result.getBandStatistics().isEmpty()) {
            appendCsvRow(csv, "Product", result.getBandStatistics().get(0).getClassBand().getProductName());
        }
        appendCsvRow(csv, "ROI", result.getRoiName() == null ? "Whole scene" : result.getRoiName());
        appendCsvRow(csv, "Validity rule", result.getValidityRule());
        appendCsvRow(csv, "Selected pixels", Long.toString(result.getSelectedPixelCount()));
        appendCsvRow(csv, "Common-valid pixels", Long.toString(result.getValidPixelCount()));
        appendCsvRow(csv, "Excluded pixels", Long.toString(result.getInvalidPixelCount()));
        csv.append('\n');
        csv.append("Class,Source band,Valid pixels,Minimum,Maximum,Mean,Std. deviation,P05,P25,Median,P75,P95\n");

        for (ClassFractionBandStatistics statistics : result.getBandStatistics()) {
            appendCsvRow(csv, statistics.getClassBand().getDisplayName(), statistics.getClassBand().getBandName(),
                    Long.toString(statistics.getValidPixelCount()), Double.toString(statistics.getMinimum()),
                    Double.toString(statistics.getMaximum()), Double.toString(statistics.getMean()),
                    Double.toString(statistics.getStandardDeviation()), Double.toString(statistics.getPercentile05()),
                    Double.toString(statistics.getPercentile25()), Double.toString(statistics.getMedian()),
                    Double.toString(statistics.getPercentile75()), Double.toString(statistics.getPercentile95()));
        }
        csv.append('\n');
        csv.append("Class,Histogram bin minimum,Histogram bin maximum,Pixel count\n");

        for (ClassFractionBandStatistics statistics : result.getBandStatistics()) {
            ClassFractionValueDistribution distribution = statistics.getDistribution();
            for (int i = 0; i < distribution.getBinCount(); i++) {
                appendCsvRow(csv, statistics.getClassBand().getDisplayName(),
                        Double.toString(distribution.getBinMinimum(i)), Double.toString(distribution.getBinMaximum(i)),
                        Long.toString(distribution.getBinCount(i)));
            }
        }
        return csv.toString();
    }

    private static void appendCsvRow(StringBuilder csv, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append('"').append(values[i].replace("\"", "\"\"")).append('"');
        }
        csv.append('\n');
    }

    private static String format(double value) {
        return Double.isFinite(value) ? String.format("%.6g", value) : "n/a";
    }


    private static final class StatisticsTableModel extends AbstractTableModel {

        private static final String[] COLUMNS = {"Class", "Valid pixels", "Minimum", "Maximum", "Mean", "Std. deviation", "P05", "P25", "Median", "P75", "P95"};
        private List<ClassFractionBandStatistics> rows = Collections.emptyList();

        void setResult(ClassFractionStatisticsResult result) {
            rows = result.getBandStatistics();
            fireTableDataChanged();
        }

        ClassFractionBandStatistics getRow(int row) {
            return row >= 0 && row < rows.size() ? rows.get(row) : null;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }
        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }
        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            ClassFractionBandStatistics statistics = rows.get(row);
            return switch (column) {
                case 0 -> statistics.getClassBand().getDisplayName();
                case 1 -> statistics.getValidPixelCount();
                case 2 -> statistics.getMinimum();
                case 3 -> statistics.getMaximum();
                case 4 -> statistics.getMean();
                case 5 -> statistics.getStandardDeviation();
                case 6 -> statistics.getPercentile05();
                case 7 -> statistics.getPercentile25();
                case 8 -> statistics.getMedian();
                case 9 -> statistics.getPercentile75();
                case 10 -> statistics.getPercentile95();
                default -> throw new IllegalArgumentException("Unknown column: " + column);
            };
        }
    }

    private static final class HistogramPanel extends JPanel {

        private ClassFractionBandStatistics statistics;

        private HistogramPanel() {
            setPreferredSize(new Dimension(900, 210));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createTitledBorder("Histogram"));
        }

        void setStatistics(ClassFractionBandStatistics statistics) {
            this.statistics = statistics;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();

            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int left = 58;
                int right = 18;
                int top = 46;
                int bottom = 38;
                int width = getWidth() - left - right;
                int height = getHeight() - top - bottom;

                if (statistics == null || width <= 0 || height <= 0) {
                    g.setColor(Color.DARK_GRAY);
                    g.drawString("Select a class row to display its histogram.", left, top + 18);
                    return;
                }

                ClassFractionValueDistribution distribution = statistics.getDistribution();
                long maximumCount = 0;
                for (long count : distribution.getBinCounts()) {
                    maximumCount = Math.max(maximumCount, count);
                }

                g.setColor(Color.DARK_GRAY);
                g.drawString(statistics.getClassBand().getDisplayName(), left, 34);
                g.drawLine(left, top + height, left + width, top + height);
                g.drawLine(left, top, left, top + height);
                g.drawString("0", left - 12, top + height + 14);
                g.drawString(Long.toString(maximumCount), 4, top + 5);
                g.drawString(format(distribution.getMinimum()), left, top + height + 30);
                String maximum = format(distribution.getMaximum());
                g.drawString(maximum, left + width - g.getFontMetrics().stringWidth(maximum), top + height + 30);

                if (maximumCount == 0) {
                    return;
                }
                Color classColor = new Color(statistics.getClassBand().getArgb(), true);
                g.setColor(new Color(classColor.getRed(), classColor.getGreen(), classColor.getBlue(), 185));
                double barWidth = (double) width / distribution.getBinCount();

                for (int i = 0; i < distribution.getBinCount(); i++) {
                    int barHeight = (int) Math.round(height * distribution.getBinCount(i) / (double) maximumCount);
                    int x = left + (int) Math.floor(i * barWidth);
                    int nextX = left + (int) Math.floor((i + 1) * barWidth);
                    g.fillRect(x, top + height - barHeight, Math.max(1, nextX - x), barHeight);
                }
                g.setColor(Color.DARK_GRAY);
                g.setStroke(new BasicStroke(1f));
                g.drawRect(left, top, width, height);
            } finally {
                g.dispose();
            }
        }
    }
}
