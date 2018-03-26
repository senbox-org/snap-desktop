package org.esa.snap.rcp.statistics;

import com.jogamp.newt.event.MouseEvent;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.controllers.mouse.NewtMouseUtilities;
import org.jzy3d.chart.controllers.mouse.camera.NewtCameraMouseController;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord2d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Range;
import org.jzy3d.maths.Scale;
import org.jzy3d.picking.PickingSupport;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.primitives.pickable.PickablePoint;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.view.View;
import org.jzy3d.plot3d.text.DrawableTextWrapper;
import org.jzy3d.plot3d.text.renderers.TextBitmapRenderer;

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
    private PickingSupport pickingSupport;
    private Range xRange;
    private Range yRange;
    private Range zRange;
    private DrawableTextWrapper textWrapper;

    void init() {
        chart = AWTChartComponentFactory.chart(Quality.Nicest, "newt");
//        pickingSupport = new PickingSupport();
        textWrapper = new DrawableTextWrapper("", new Coord3d(0, 0, 0),
                                              Color.BLACK, new TextBitmapRenderer());
        pickingSupport.addObjectPickedListener((vertices, picking) -> {
            for (Object vertex : vertices) {
                final Coord3d vertexCoords = (Coord3d) vertex;
                textWrapper.setText("x = " + vertexCoords.x + ", y = " + vertexCoords.y + ", z = " + vertexCoords.z);
                textWrapper.setPosition(vertexCoords);
            }
            renderChart();
        });
        scatter = new Scatter();
        scatter.setWidth(10.0f);
        chart.getScene().add(scatter);
        setLayout(new BorderLayout());
        new ScatterPlot3dCameraMouseController(chart);
//        chart.addMouseCameraController();
//        IMousePickingController pickingController = chart.addMousePickingController(10);
//        pickingController.setPickingSupport(pickingSupport);
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
//        chart.clear();
        scatter.clear();
//        pickingSupport.unRegisterAllPickableObjects();
        final int size = xData.length;
        Coord3d[] points = new Coord3d[size];
        Color[] colors = new Color[size];
        for (int i = 0; i < size; i++) {
            points[i] = new Coord3d(xData[i] * xScale, yData[i] * yScale, zData[i] * zScale);
            PickablePoint pickablePoint = new PickablePoint(points[i]);
//            pickingSupport.registerPickableObject(pickablePoint, points[i]);
            colors[i] = Color.CYAN;
        }
        scatter.setData(points);
        scatter.setColors(colors);
        xRange = new Range(xMin, xMax);
        yRange = new Range(yMin, yMax);
        zRange = new Range(zMin, zMax);
        final BoundingBox3d box = new BoundingBox3d(xRange, yRange, zRange);
        chart.getView().setBoundManual(box);
        renderChart();
    }

    void renderChart() {
        chart.render();
    }

    private class ScatterPlot3dCameraMouseController extends NewtCameraMouseController {

        ScatterPlot3dCameraMouseController(Chart chart) {
            super(chart);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Coord2d mouse = new Coord2d(e.getX(), e.getY());
            // Rotate
            if(isLeftDown(e)){
                Coord2d move  = mouse.sub(prevMouse).div(100);
                rotate( move );
            }
            // Shift
            else if(isRightDown(e)){
                final View view = chart.getView();
                final Coord3d prevMouse3D = view.projectMouse((int) prevMouse.x, (int) prevMouse.y);
                final Coord3d move3D = view.projectMouse((int) mouse.x, (int) mouse.y);
                final Coord3d diff = prevMouse3D.sub(move3D).div(100);
                final Scale xScale = new Scale(view.getBounds().getXmin(), view.getBounds().getXmax());
                final Scale yScale = new Scale(view.getBounds().getYmin(), view.getBounds().getYmax());
                final Scale zScale = new Scale(view.getBounds().getZmin(), view.getBounds().getZmax());
                float xFactor = xRange.getRange() / xScale.getRange();
                float yFactor = yRange.getRange() / yScale.getRange();
                float zFactor = zRange.getRange() / zScale.getRange();
                final Scale newXScale = xScale.add(xScale.getRange() * diff.x * xFactor);
                final Scale newYScale = yScale.add(yScale.getRange() * diff.y * yFactor);
                final Scale newZScale = zScale.add(zScale.getRange() * -diff.z * zFactor);
                view.setScaleX(newXScale);
                view.setScaleY(newYScale);
                view.setScaleZ(newZScale);
            }

            prevMouse = mouse;
        }

        @Override
        public void mouseWheelMoved(MouseEvent e) {
            stopThreadController();

            float factor = NewtMouseUtilities.convertWheelRotation(e, 1.0f, 10.0f);
            zoomX(factor);
            zoomY(factor);
            zoomZ(factor);
        }
    }

}
