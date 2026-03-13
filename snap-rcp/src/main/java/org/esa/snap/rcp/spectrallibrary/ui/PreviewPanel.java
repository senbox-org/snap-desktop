package org.esa.snap.rcp.spectrallibrary.ui;

import org.esa.snap.rcp.spectrallibrary.util.ColorUtils;
import org.esa.snap.speclib.model.SpectralProfile;
import org.jfree.chart.*;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;


public class PreviewPanel extends JPanel {


    private final XYSeriesCollection dataset = new XYSeriesCollection();
    private final JFreeChart chart = ChartFactory.createXYLineChart("Preview", "Wavelength / Index", "Value", dataset, PlotOrientation.VERTICAL, true, true, false);

    private final ChartPanel chartPanel = new ChartPanel(chart);
    private final JPanel headerHost = new JPanel(new BorderLayout());

    private List<SpectralProfile> profiles = List.of();
    private UUID selectedProfileId;

    private double[] xAxisOrNull;
    private Consumer<UUID> selectionListener;

    private final Stroke normalStroke = new BasicStroke(1.8f);
    private final Stroke selectedStroke = new BasicStroke(3.2f);
    private final float normalAlpha = 0.65f;
    private final float selectedAlpha = 1.0f;

    private XYTitleAnnotation busyAnnotation;

    private final Map<UUID, Paint> basePaintById = new HashMap<>();

    private final DefaultListModel<LegendEntry> legendModel = new DefaultListModel<>();
    private final JList<LegendEntry> legendList = new JList<>(legendModel);
    private final JScrollPane legendScroll = new JScrollPane(legendList);

    private record LegendEntry(UUID id, String name) {}


    public PreviewPanel() {
        super(new BorderLayout(4, 4));

        chartPanel.setMinimumDrawHeight(0);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
        chart.removeLegend();
        legendScroll.setPreferredSize(new Dimension(1, 100));
        legendScroll.setVisible(false);

        add(headerHost, BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);
        add(legendScroll, BorderLayout.SOUTH);

        configureRenderer();
        installClickSelection();

        legendList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        legendList.setVisibleRowCount(-1);
        legendList.setCellRenderer((list, entry, index, isSelected, cellHasFocus) -> {
            JLabel l = new JLabel(entry == null ? "" : entry.name());
            l.setOpaque(true);
            l.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            l.setBackground(isSelected ? UIManager.getColor("List.selectionBackground") : UIManager.getColor("List.background"));
            l.setForeground(isSelected ? UIManager.getColor("List.selectionForeground") : UIManager.getColor("List.foreground"));

            if (entry != null) {
                Paint p = getOrCreatePaint(entry.id());
                Color c = (p instanceof Color cc) ? cc : Color.GRAY;
                l.setIcon(ColorUtils.makeColorIcon(c));
            }
            return l;
        });

        legendList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            LegendEntry le = legendList.getSelectedValue();
            if (le != null) {
                setSelectedProfileId(le.id());
                if (selectionListener != null) {
                    selectionListener.accept(le.id());
                }
            }
        });
    }


    public void setHeader(JComponent header) {
        headerHost.removeAll();
        if (header != null) {
            headerHost.add(header, BorderLayout.CENTER);
        }
        headerHost.revalidate();
        headerHost.repaint();
    }

    public void setSelectionListener(Consumer<UUID> selectionListener) {
        this.selectionListener = selectionListener;
    }

    public void setProfiles(List<SpectralProfile> profiles) {
        this.profiles = profiles == null ? List.of() : List.copyOf(profiles);
        updateLegendModel();
        rebuildDataset();
        applyHighlight();
    }

    public void setXAxis(double[] xAxisOrNull) {
        this.xAxisOrNull = xAxisOrNull;
        rebuildDataset();
        applyHighlight();
    }

    public void clearXAxis() {
        this.xAxisOrNull = null;
        rebuildDataset();
        applyHighlight();
    }

    public void setSelectedProfileId(UUID id) {
        this.selectedProfileId = id;
        applyHighlight();
    }

    public void clear() {
        profiles = List.of();
        selectedProfileId = null;
        dataset.removeAllSeries();
        legendList.clearSelection();
        updateLegendModel();
        applyHighlight();
    }

    public void showBusyMessage(String text) {
        XYPlot plot = chart.getXYPlot();
        plot.clearAnnotations();

        String msg = (text == null || text.isBlank()) ? "Working..." : text;

        TextTitle tt = new TextTitle(msg);
        tt.setTextAlignment(HorizontalAlignment.RIGHT);
        tt.setFont(chart.getLegend() != null ? chart.getLegend().getItemFont() : getFont());
        tt.setBackgroundPaint(new Color(200, 200, 255, 50));
        tt.setFrame(new BlockBorder(Color.white));
        tt.setPosition(RectangleEdge.BOTTOM);

        busyAnnotation = new XYTitleAnnotation(0.5, 0.5, tt, RectangleAnchor.CENTER);
        plot.addAnnotation(busyAnnotation);

        chartPanel.repaint();
    }

    public void clearBusyMessage() {
        XYPlot plot = chart.getXYPlot();
        if (busyAnnotation != null) {
            plot.removeAnnotation(busyAnnotation);
            busyAnnotation = null;
        } else {
            plot.clearAnnotations();
        }
        chartPanel.repaint();
    }


    private void configureRenderer() {
        XYPlot plot = chart.getXYPlot();
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);

        XYLineAndShapeRenderer r = new XYLineAndShapeRenderer(true, true);
        r.setDefaultStroke(normalStroke);

        r.setDefaultShape(new Ellipse2D.Double(-3, -3, 6, 6));
        r.setDefaultShapesFilled(true);
        r.setDrawSeriesLineAsPath(true);

        plot.setRenderer(r);
    }

    private void installClickSelection() {
        chartPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UUID id = pickProfileByEntity(e);
                if (id == null) {
                    id = pickProfileByNearestSeries(e);
                }
                if (id != null) {
                    setSelectedProfileId(id);
                    if (selectionListener != null) selectionListener.accept(id);
                }
            }
        });
    }

    private UUID pickProfileByEntity(MouseEvent e) {
        ChartEntity ent = chartPanel.getEntityForPoint(e.getX(), e.getY());
        if (!(ent instanceof XYItemEntity itemEnt)) {
            return null;
        }

        int series = itemEnt.getSeriesIndex();
        return profileIdBySeriesIndex(series);
    }

    private UUID pickProfileByNearestSeries(MouseEvent e) {
        XYPlot plot = chart.getXYPlot();
        XYDataset ds = plot.getDataset();
        if (ds == null || ds.getSeriesCount() == 0) {
            return null;
        }

        Rectangle2D dataArea = chartPanel.getScreenDataArea();
        if (dataArea == null) {
            return null;
        }

        double best = Double.POSITIVE_INFINITY;
        int bestSeries = -1;

        for (int s = 0; s < ds.getSeriesCount(); s++) {
            int n = ds.getItemCount(s);
            for (int i = 0; i < n; i++) {
                Number xn = ds.getX(s, i);
                Number yn = ds.getY(s, i);
                if (xn == null || yn == null) {
                    continue;
                }

                double xx = plot.getDomainAxis().valueToJava2D(
                        xn.doubleValue(), dataArea, plot.getDomainAxisEdge()
                );
                double yy = plot.getRangeAxis().valueToJava2D(
                        yn.doubleValue(), dataArea, plot.getRangeAxisEdge()
                );

                double dx = xx - e.getX();
                double dy = yy - e.getY();
                double d2 = dx * dx + dy * dy;

                if (d2 < best) {
                    best = d2;
                    bestSeries = s;
                }
            }
        }

        double thresholdPx = 28.0;
        if (bestSeries >= 0 && best <= thresholdPx * thresholdPx) {
            return profileIdBySeriesIndex(bestSeries);
        }
        return null;
    }

    private UUID profileIdBySeriesIndex(int seriesIndex) {
        if (seriesIndex < 0 || seriesIndex >= profiles.size()) {
            return null;
        }
        SpectralProfile p = profiles.get(seriesIndex);
        return p != null ? p.getId() : null;
    }


    private void rebuildDataset() {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        dataset.setNotify(false);
        try {
            dataset.removeAllSeries();

            for (SpectralProfile p : profiles) {
                if (p == null || p.getSignature() == null) {
                    continue;
                }
                final XYSeries s = new XYSeries(p.getName(), false, true);
                s.setNotify(false);

                try {
                    final double[] y = p.getSignature().getValues();
                    final int n = y.length;
                    final boolean useAxis = xAxisOrNull != null && xAxisOrNull.length == n;

                    for (int i = 0; i < n; i++) {
                        final double xv = useAxis ? xAxisOrNull[i] : (i + 1);
                        final double yv = y[i];
                        if (Double.isNaN(xv) || Double.isNaN(yv) || Double.isInfinite(xv) || Double.isInfinite(yv)) {
                            continue;
                        }

                        if (xv < minX) {
                            minX = xv;
                        }
                        if (xv > maxX) {
                            maxX = xv;
                        }
                        if (yv < minY) {
                            minY = yv;
                        }
                        if (yv > maxY) {
                            maxY = yv;
                        }

                        s.add(xv, yv);
                    }
                } finally {
                    s.setNotify(true);
                }
                dataset.addSeries(s);
            }
        } finally {
            final XYPlot plot = chart.getXYPlot();
            if (minX != Double.POSITIVE_INFINITY && minY != Double.POSITIVE_INFINITY) {
                double padX = (maxX > minX) ? (maxX - minX) * 0.02 : 1.0;
                double padY = (maxY > minY) ? (maxY - minY) * 0.02 : 1.0;

                final ValueAxis dx = plot.getDomainAxis();
                dx.setAutoRange(false);
                dx.setRange(minX - padX, maxX + padX);
                final ValueAxis ry = plot.getRangeAxis();
                ry.setAutoRange(false);
                ry.setRange(minY - padY, maxY + padY);
            } else {
                plot.getDomainAxis().setAutoRange(true);
                plot.getRangeAxis().setAutoRange(true);
            }

            dataset.setNotify(true);
        }
    }

    private void applyHighlight() {
        XYPlot plot = chart.getXYPlot();
        if (!(plot.getRenderer() instanceof XYLineAndShapeRenderer r)) {
            return;
        }

        int seriesCount = dataset.getSeriesCount();
        boolean showShapes = seriesCount <= 200;
        r.setDefaultShapesVisible(showShapes);
        r.setDefaultShapesFilled(showShapes);
        UUID sel = selectedProfileId;

        for (int s = 0; s < seriesCount; s++) {
            UUID pid = profileIdBySeriesIndex(s);
            boolean isSelected = sel != null && sel.equals(pid);

            r.setSeriesStroke(s, isSelected ? selectedStroke : normalStroke);
            Paint base = getOrCreatePaint(pid);

            r.setSeriesPaint(s, applyAlpha(base, isSelected ? selectedAlpha : normalAlpha));
        }

        chartPanel.repaint();
        legendList.repaint();
    }

    private Paint applyAlpha(Paint base, float alpha) {
        if (base instanceof Color c) {
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(alpha * 255f));
        }
        return base;
    }

    public void setProfilePaints(Map<UUID, ? extends Paint> paints) {
        basePaintById.clear();
        if (paints != null && !paints.isEmpty()) {
            basePaintById.putAll(paints);
        }
        applyHighlight();
    }

    private Paint getOrCreatePaint(UUID id) {
        if (id == null) {
            return Color.BLUE;
        }

        Paint p = basePaintById.get(id);
        if (p != null) {
            return p;
        }
        return ColorUtils.DEFAULT_PALETTE[(id.hashCode() & 0x7fffffff) % ColorUtils.DEFAULT_PALETTE.length];
    }


    private void updateLegendModel() {
        legendModel.clear();
        if (profiles == null || profiles.isEmpty()) {
            legendScroll.setVisible(false);
            legendScroll.revalidate();
            legendScroll.repaint();
            return;
        }

        List<LegendEntry> entries = new ArrayList<>(profiles.size());
        for (SpectralProfile p : profiles) {
            if (p != null) {
                entries.add(new LegendEntry(p.getId(), p.getName()));
            }
        }

        legendModel.addAll(entries);
        legendScroll.setVisible(!entries.isEmpty());
        legendScroll.revalidate();
        legendScroll.repaint();
    }
}
