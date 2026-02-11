package org.esa.snap.rcp.spectrallibrary.ui;

import org.esa.snap.speclib.model.SpectralProfile;
import org.jfree.chart.*;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;


public class PreviewPanel extends JPanel {


    private final XYSeriesCollection dataset = new XYSeriesCollection();
    private final JFreeChart chart = ChartFactory.createXYLineChart(
            "Preview",
            "Wavelength / Index",
            "Value",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
    );

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
    private final List<Paint> basePaints = new ArrayList<>();


    public PreviewPanel() {
        super(new BorderLayout(4, 4));

        chartPanel.setMinimumDrawHeight(0);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);

        add(headerHost, BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);

        configureRenderer();
        installClickSelection();
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

    public List<SpectralProfile> getProfilesSnapshot() {
        return new ArrayList<>(profiles);
    }

    public void setSelectedProfileId(UUID id) {
        this.selectedProfileId = id;
        applyHighlight();
    }

    public UUID getSelectedProfileId() {
        return selectedProfileId;
    }

    public List<SpectralProfile> getSelectedProfiles() {
        if (selectedProfileId == null) {
            return List.of();
        }
        for (SpectralProfile p : profiles) {
            if (selectedProfileId.equals(p.getId())) {
                return List.of(p);
            }
        }
        return List.of();
    }

    public void clear() {
        profiles = List.of();
        selectedProfileId = null;
        dataset.removeAllSeries();
        applyHighlight();
    }


    private void configureRenderer() {
        XYPlot plot = chart.getXYPlot();
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);

        XYLineAndShapeRenderer r = new XYLineAndShapeRenderer(true, true);
        r.setDefaultStroke(normalStroke);

        r.setDefaultShape(new Ellipse2D.Double(-3, -3, 6, 6));
        r.setDefaultShapesFilled(true);

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
        dataset.removeAllSeries();
        basePaints.clear();

        XYPlot plot = chart.getXYPlot();
        DrawingSupplier ds = plot.getDrawingSupplier();

        for (SpectralProfile p : profiles) {
            XYSeries s = new XYSeries(p.getName(), false, true);

            double[] y = p.getSignature().getValues();
            int n = y.length;
            boolean useAxis = xAxisOrNull != null && xAxisOrNull.length == n;

            for (int i = 0; i < n; i++) {
                double xv = useAxis ? xAxisOrNull[i] : (i + 1);
                double yv = y[i];
                if (Double.isNaN(xv) || Double.isNaN(yv)) {
                    continue;
                }
                if (Double.isInfinite(xv) || Double.isInfinite(yv)) {
                    continue;
                }
                s.add(xv, yv);
            }

            dataset.addSeries(s);
            basePaints.add(ds.getNextPaint());
        }

        applyHighlight();
    }

    private void applyHighlight() {
        XYPlot plot = chart.getXYPlot();
        if (!(plot.getRenderer() instanceof XYLineAndShapeRenderer r)) {
            return;
        }

        int seriesCount = dataset.getSeriesCount();
        UUID sel = selectedProfileId;

        for (int s = 0; s < seriesCount; s++) {
            UUID pid = profileIdBySeriesIndex(s);
            boolean isSelected = sel != null && sel.equals(pid);

            r.setSeriesStroke(s, isSelected ? selectedStroke : normalStroke);

            Paint base = (s < basePaints.size() && basePaints.get(s) != null)
                    ? basePaints.get(s)
                    : r.getDefaultPaint();

            r.setSeriesPaint(s, applyAlpha(base, isSelected ? selectedAlpha : normalAlpha));
        }

        chartPanel.repaint();
    }

    private Paint applyAlpha(Paint base, float alpha) {
        if (base instanceof Color c) {
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(alpha * 255f));
        }
        return base;
    }
}
