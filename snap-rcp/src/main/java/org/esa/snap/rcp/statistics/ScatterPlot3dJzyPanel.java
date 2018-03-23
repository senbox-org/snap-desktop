package org.esa.snap.rcp.statistics;

import org.jzy3d.chart.Chart;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.rendering.canvas.Quality;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;

//import org.jzy3d.plot3d.primitives.pickable.PickablePoint;

/**
 * @author Tonio Fincke
 */
class ScatterPlot3dJzyPanel extends JPanel {

    private Chart chart;
    private Scatter scatter;
    private JLabel titleLabel;

    void init() {
        chart = AWTChartComponentFactory.chart(Quality.Nicest, "newt");
        scatter = new Scatter();
        scatter.setWidth(10.0f);
        chart.getScene().add(scatter);
        setLayout(new BorderLayout());
        chart.addMouseCameraController();
        titleLabel = new JLabel();
        add(titleLabel, BorderLayout.NORTH);
        add((Component) chart.getCanvas(), BorderLayout.CENTER);
    }

    void setChartTitle(String chartTitle) {
        titleLabel.setText(chartTitle);
    }

    void setLabelNames(String xLabelName, String yLabelName, String zLabelName) {
        chart.getAxeLayout().setXAxeLabel(xLabelName);
        chart.getAxeLayout().setYAxeLabel(yLabelName);
        chart.getAxeLayout().setZAxeLabel(zLabelName);
    }

    void updateChart(float[] xData, float[] yData, float[] zData, double xScale, double yScale, double zScale,
                     float xMin, float xMax, float yMin, float yMax, float zMin, float zMax) {
        final int size = xData.length;
        Coord3d[] points = new Coord3d[size];
        Color[] colors = new Color[size];
        for (int i = 0; i < size; i++) {
            points[i] = new Coord3d(xData[i] * xScale, yData[i] * yScale, zData[i] * zScale);
            colors[i] = Color.CYAN;
        }
        scatter.setData(points);
        scatter.setColors(colors);
        final BoundingBox3d box = new BoundingBox3d(xMin, xMax, yMin, yMax, zMin, zMax);
        chart.getView().setBoundManual(box);
        renderChart();
    }

    void renderChart() {
        chart.render();
    }

}
