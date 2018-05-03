package org.esa.snap.rcp.statistics;

import com.bc.ceres.swing.TableLayout;
import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.texture.TextureIO;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.ui.SnapFileChooser;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.controllers.mouse.NewtMouseUtilities;
import org.jzy3d.chart.controllers.mouse.camera.NewtCameraMouseController;
import org.jzy3d.chart.controllers.thread.camera.CameraThreadController;
import org.jzy3d.chart.factories.NewtChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord2d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.IntegerCoord2d;
import org.jzy3d.maths.Range;
import org.jzy3d.maths.Scale;
import org.jzy3d.picking.PickingSupport;
import org.jzy3d.plot3d.primitives.LineStrip;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.primitives.pickable.Pickable;
import org.jzy3d.plot3d.primitives.pickable.PickablePoint;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.scene.Graph;
import org.jzy3d.plot3d.rendering.view.Camera;
import org.jzy3d.plot3d.rendering.view.View;
import org.jzy3d.plot3d.rendering.view.modes.CameraMode;
import org.jzy3d.plot3d.transform.Transform;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tonio Fincke
 */
class ScatterPlot3dJzyPanel extends JPanel {

    private Chart chart;
    private ScatterPlot3DScatter scatter;
    private ScatterPlot3DProjectionScatter projectionScatter;
    private JLabel titleLabel;
    private ScatterPlot3DPickingSupport pickingSupport;
    private Range xRange;
    private Range yRange;
    private Range zRange;
    private LineStrip xLine;
    private LineStrip yLine;
    private LineStrip zLine;
    private Coord3d currentVertex;
    private static final Color PROJECTION_COLOR = new Color(0.8f, 0.8f, 0.8f, 0.25f);
    private boolean displayOnlyDataInAxisBounds;
    private JPanel pickingInfoPanel;
    private JLabel xDimLabel;
    private JLabel yDimLabel;
    private JLabel zDimLabel;
    private JLabel xValueLabel;
    private JLabel yValueLabel;
    private JLabel zValueLabel;

    void init() {
        displayOnlyDataInAxisBounds = false;
        chart = NewtChartComponentFactory.chart(Quality.Nicest, "newt");
        pickingSupport = new ScatterPlot3DPickingSupport();
        setLayout(new BorderLayout());
        TableLayout pickingInfoPanelLayout = new TableLayout(3);
        pickingInfoPanelLayout.setTableAnchor(TableLayout.Anchor.SOUTHEAST);
        pickingInfoPanelLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        pickingInfoPanelLayout.setColumnWeightX(0, 1.0);
        pickingInfoPanelLayout.setColumnWeightX(1, 0.0);
        pickingInfoPanelLayout.setColumnWeightX(2, 0.0);
        pickingInfoPanelLayout.setTablePadding(2, 2);
        pickingInfoPanel = new JPanel(pickingInfoPanelLayout);
        pickingInfoPanel.setOpaque(true);
        pickingInfoPanel.setBackground(java.awt.Color.WHITE);
        xDimLabel = new JLabel();
        xDimLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        xDimLabel.setHorizontalAlignment(SwingConstants.LEFT);
        yDimLabel = new JLabel();
        yDimLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        yDimLabel.setHorizontalAlignment(SwingConstants.LEFT);
        zDimLabel = new JLabel();
        zDimLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        zDimLabel.setHorizontalAlignment(SwingConstants.LEFT);
        xValueLabel = new JLabel();
        xValueLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        yValueLabel = new JLabel();
        yValueLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        zValueLabel = new JLabel();
        zValueLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        pickingInfoPanel.add(new JLabel());
        pickingInfoPanel.add(xDimLabel);
        pickingInfoPanel.add(xValueLabel);
        pickingInfoPanel.add(new JLabel());
        pickingInfoPanel.add(yDimLabel);
        pickingInfoPanel.add(yValueLabel);
        pickingInfoPanel.add(new JLabel());
        pickingInfoPanel.add(zDimLabel);
        pickingInfoPanel.add(zValueLabel);
        pickingSupport.addObjectPickedListener((vertices, picking) -> {
            if (vertices.isEmpty()) {
                remove(pickingInfoPanel);
                return;
            }
            add(pickingInfoPanel, BorderLayout.SOUTH);
            for (Object vertex : vertices) {
                final Coord3d vertexCoords = (Coord3d) vertex;
                xDimLabel.setText(chart.getAxeLayout().getXAxeLabel().split("=")[1].trim() + ":");
                yDimLabel.setText(chart.getAxeLayout().getYAxeLabel().split("=")[1].trim() + ":");
                zDimLabel.setText(chart.getAxeLayout().getZAxeLabel().split("=")[1].trim() + ":");
                xValueLabel.setText("" + vertexCoords.x);
                yValueLabel.setText("" + vertexCoords.y);
                zValueLabel.setText("" + vertexCoords.z);
            }
        });
        currentVertex = new Coord3d(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        pickingSupport.addObjectPickedListener((vertices, picking) -> {
            for (Object vertex : vertices) {
                currentVertex = (Coord3d) vertex;
            }
            updateLine();
        });
        projectionScatter = new ScatterPlot3DProjectionScatter();
        projectionScatter.setColor(PROJECTION_COLOR);
        chart.getScene().add(projectionScatter);
        xLine = new LineStrip();
        xLine.setWireframeColor(new Color(0.8f, 0.0f, 0.0f));
        xLine.setWidth(2.f);
        xLine.add(new Point(currentVertex));
        xLine.add(new Point(currentVertex));
        chart.getScene().add(xLine);
        yLine = new LineStrip();
        yLine.setWireframeColor(new Color(0.8f, 0.0f, 0.0f));
        yLine.setWidth(2.f);
        yLine.add(new Point(currentVertex));
        yLine.add(new Point(currentVertex));
        chart.getScene().add(yLine);
        zLine = new LineStrip();
        zLine.setWireframeColor(new Color(0.8f, 0.0f, 0.0f));
        zLine.setWidth(2.f);
        zLine.add(new Point(currentVertex));
        zLine.add(new Point(currentVertex));
        chart.getScene().add(zLine);
        scatter = new ScatterPlot3DScatter();
        scatter.setWidth(10.0f);
        chart.getScene().add(scatter);
        ScatterPlot3dCameraMouseController cameraController =
                new ScatterPlot3dCameraMouseController(this.chart, pickingSupport);
        titleLabel = new JLabel("3D Scatter Plot");
        titleLabel.setOpaque(true);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        titleLabel.setBackground(java.awt.Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);
        add((Component) this.chart.getCanvas(), BorderLayout.CENTER);
    }

    private void updateLine() {
        BoundingBox3d viewBounds = chart.getView().getBounds();
        boolean inXRange = currentVertex.x > viewBounds.getXmin() && currentVertex.x < viewBounds.getXmax();
        boolean inYRange = currentVertex.y > viewBounds.getYmin() && currentVertex.y < viewBounds.getYmax();
        boolean inZRange = currentVertex.z > viewBounds.getZmin() && currentVertex.z < viewBounds.getZmax();
        xLine.get(0).setCoord(currentVertex);
        if (inYRange && inZRange && (!displayOnlyDataInAxisBounds || inXRange)) {
            xLine.get(1).setCoord(new Coord3d(getXAxisCoord(), currentVertex.y, currentVertex.z));
        } else {
            xLine.get(1).setCoord(currentVertex);
        }
        yLine.get(0).setCoord(currentVertex);
        if (inXRange && inZRange && (!displayOnlyDataInAxisBounds || inYRange)) {
            yLine.get(1).setCoord(new Coord3d(currentVertex.x, getYAxisCoord(), currentVertex.z));
        } else {
            yLine.get(1).setCoord(currentVertex);
        }
        zLine.get(0).setCoord(currentVertex);
        if (inXRange && inYRange && (!displayOnlyDataInAxisBounds || inZRange)) {
            zLine.get(1).setCoord(new Coord3d(currentVertex.x, currentVertex.y, getZAxisCoord()));
        } else {
            zLine.get(1).setCoord(currentVertex);
        }
    }

    private float getXAxisCoord() {
        View view = chart.getView();
        if (Math.cos(view.getViewPoint().x) > 0) {
            return view.getBounds().getXmin();
        }
        return view.getBounds().getXmax();
    }

    private float getYAxisCoord() {
        View view = chart.getView();
        if (Math.sin(view.getViewPoint().x) > 0) {
            return view.getBounds().getYmin();
        }
        return view.getBounds().getYmax();
    }

    private float getZAxisCoord() {
        View view = chart.getView();
        if (view.getViewPoint().y > 0) {
            return view.getBounds().getZmin();
        }
        return view.getBounds().getZmax();
    }

    void setChartTitle(boolean showTitle, String chartTitle, Font titleFont, java.awt.Color titleColor) {
        if (showTitle) {
            add(titleLabel, BorderLayout.NORTH);
            titleLabel.setText(chartTitle);
            titleLabel.setFont(titleFont);
            titleLabel.setForeground(titleColor);
        } else {
            remove(titleLabel);
        }
    }

    void updateChart(String xLabelName, String yLabelName, String zLabelName, List<Float> xData, List<Float> yData,
                     List<Float> zData) {
        String newXLabelName = "x = " + xLabelName;
        String newYLabelName = "y = " + yLabelName;
        String newZLabelName = "z = " + zLabelName;
        boolean dimsHaveChanged = false;
        if (!chart.getAxeLayout().getXAxeLabel().equals(newXLabelName)) {
            chart.getAxeLayout().setXAxeLabel(newXLabelName);
            dimsHaveChanged = true;
        }
        if (!chart.getAxeLayout().getYAxeLabel().equals(newYLabelName)) {
            chart.getAxeLayout().setYAxeLabel(newYLabelName);
            dimsHaveChanged = true;
        }
        if (!chart.getAxeLayout().getZAxeLabel().equals(newZLabelName)) {
            chart.getAxeLayout().setZAxeLabel(newZLabelName);
            dimsHaveChanged = true;
        }
        chart.getAxeLayout().setFaceDisplayed(true);
        scatter.clear();
        projectionScatter.clear();
        final int size = xData.size();
        Coord3d[] points = new Coord3d[size];
        PickablePointWithTarget[] pickablePointWithTargets = new PickablePointWithTarget[size];
        boolean currentVertexStillIncluded = false;
        for (int i = 0; i < size; i++) {
            points[i] = new Coord3d(xData.get(i), yData.get(i), zData.get(i));
            if (!currentVertexStillIncluded && currentVertex.equals(points[i])) {
                currentVertexStillIncluded = true;
            }
            pickablePointWithTargets[i] = new PickablePointWithTarget(points[i], points[i], i);
        }
        pickingSupport.registerPickableObjects(pickablePointWithTargets);
        scatter.setData(points);
        projectionScatter.setData(points);
        adjustProjectionScatterToBounds();
        if (dimsHaveChanged || !currentVertexStillIncluded) {
            currentVertex.set(Float.NaN, Float.NaN, Float.NaN);
            updateLine();
            remove(pickingInfoPanel);
            updateUI();
        }
    }

    void setColors(List<Integer> colorData) {
        Color[] colors = new Color[colorData.size() / 3];
        for (int i = 0; i < colorData.size() / 3; i++) {
            colors[i] = new Color(colorData.get(3 * i), colorData.get(3 * i + 1), colorData.get(3 * i + 2), 63);
        }
        scatter.setColors(colors);
    }

    void setColor(java.awt.Color color) {
        scatter.setColors(null);
        scatter.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()));
    }

    void projectToX(boolean projectToX) {
        projectionScatter.projectToX(projectToX);
    }

    void projectToY(boolean projectToY) {
        projectionScatter.projectToY(projectToY);
    }

    void projectToZ(boolean projectToZ) {
        projectionScatter.projectToZ(projectToZ);
    }

    private void adjustAxisBounds() {
        BoundingBox3d bounds = chart.getView().getBounds();
        scatter.setAxisBounds(bounds);
        projectionScatter.setAxisBounds(bounds);
    }

    private void adjustProjectionScatterToBounds() {
        projectionScatter.setEdgeCoords(getXAxisCoord(), getYAxisCoord(), getZAxisCoord());
    }

    void setChartBounds(float xMin, float xMax, float yMin, float yMax, float zMin, float zMax) {
        xRange = new Range(xMin, xMax);
        yRange = new Range(yMin, yMax);
        zRange = new Range(zMin, zMax);
        final BoundingBox3d box = new BoundingBox3d(xRange, yRange, zRange);
        chart.getView().setBoundManual(box);
        updateLine();
        projectionScatter.resetScale();
        projectionScatter.setEdgeCoords(getXAxisCoord(), getYAxisCoord(), getZAxisCoord());
        adjustAxisBounds();
    }

    void renderChart() {
        chart.render();
    }

    void doSaveAs() throws IOException {
        SnapFileChooser snapFileChooser = new SnapFileChooser();
        snapFileChooser.addChoosableFileFilter(new SnapFileFilter("PNG", "png",
                "PNG Image Files"));
        int result = snapFileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = snapFileChooser.getSelectedFile();
            if (!file.getParentFile().exists())
                file.mkdirs();
            TextureIO.write(chart.screenshot(), file);
        }
    }

    void displayOnlyDataInAxisBounds(boolean displayOnlyDataInAxisBounds) {
        scatter.displayOnlyDataInAxisBounds(displayOnlyDataInAxisBounds);
        this.displayOnlyDataInAxisBounds = displayOnlyDataInAxisBounds;
    }

    private class ScatterPlot3dCameraMouseController extends NewtCameraMouseController {

        private final PickingSupport pickingSupport;
        private final GLU glu;
        private boolean isAnimating;

        ScatterPlot3dCameraMouseController(Chart chart, PickingSupport pickingSupport) {
            super(chart);
            removeSlaveThreadController();
            addSlaveThreadController(new ScatterPlot3DCameraThreadController(chart));
            this.pickingSupport = pickingSupport;
            glu = new GLU();
            isAnimating = false;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Coord2d mouse = new Coord2d(e.getX(), e.getY());
            // Rotate
            if (isLeftDown(e)) {
                Coord2d move = mouse.sub(prevMouse).div(100);
                rotate(move);
                updateLine();
                adjustProjectionScatterToBounds();
            }
            // Shift
            else if (isRightDown(e)) {
                final View view = chart.getView();
                final Coord3d prevMouse3D = view.projectMouse((int) prevMouse.x, (int) prevMouse.y);
                final Coord3d move3D = view.projectMouse((int) mouse.x, (int) mouse.y);
                final Coord3d diff = prevMouse3D.sub(move3D);
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
                updateLine();
                adjustAxisBounds();
                adjustProjectionScatterToBounds();
            }

            prevMouse = mouse;
        }

        @Override
        public void mouseWheelMoved(MouseEvent e) {
            stopThreadController();

            float factor = NewtMouseUtilities.convertWheelRotation(e, 1.0f, 10.0f);
            projectionScatter.scale(factor);
            zoomX(factor);
            zoomY(factor);
            zoomZ(factor);
            updateLine();
            adjustAxisBounds();
            adjustProjectionScatterToBounds();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && this.threadController != null) {
                if (!isAnimating) {
                    this.threadController.start();
                } else {
                    this.threadController.stop();
                }
                isAnimating = !isAnimating;
            } else if(e.isShiftDown()){
                int x = e.getX();
                int y = e.getY();
                int yflip = -y + targets.get(0).getCanvas().getRendererHeight();
                prevMouse.x = x;
                prevMouse.y = y;
                View view = targets.get(0).getView();
                GL gl = chart().getView().getCurrentGL();
                pickingSupport.pickObjects(gl, glu, view, null, new IntegerCoord2d(x, yflip));
                chart().getView().getCurrentContext().release();
            }
        }
    }

    private class ScatterPlot3DPickingSupport extends PickingSupport {

        private PickablePointWithTarget[] pickablePoints;

        ScatterPlot3DPickingSupport() {
            super();
            pickablePoints = new PickablePointWithTarget[0];
        }

        void registerPickableObjects(PickablePointWithTarget[] pickablePoints) {
            this.pickablePoints = pickablePoints;
        }

        @Override
        public void pickObjects(GL gl, GLU glu, View view, Graph graph, IntegerCoord2d pickPoint) {
            perf.tic();

            int viewport[] = new int[4];
            int selectBuf[] = new int[bufferSize]; // TODO: move @ construction
            IntBuffer selectBuffer = Buffers.newDirectIntBuffer(bufferSize);


            if (!gl.isGL2()) throw new UnsupportedOperationException();

            // Prepare selection data
            gl.getGL2().glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
            gl.getGL2().glSelectBuffer(bufferSize, selectBuffer);
            gl.getGL2().glRenderMode(GL2.GL_SELECT);
            gl.getGL2().glInitNames();
            gl.getGL2().glPushName(0);

            // Retrieve view settings
            Camera camera = view.getCamera();
            CameraMode cMode = view.getCameraMode();
            Coord3d viewScaling = view.getLastViewScaling();
            Transform viewTransform = new Transform(new org.jzy3d.plot3d.transform.Scale(viewScaling));
            double xpick = pickPoint.x;
            double ypick = pickPoint.y;

            // Setup projection matrix
            gl.getGL2().glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            gl.getGL2().glPushMatrix();
            {
                gl.getGL2().glLoadIdentity();
                // Setup picking matrix, and update view frustrum
                glu.gluPickMatrix(xpick, ypick, brushSize, brushSize, viewport, 0);
                camera.doShoot(gl, glu, cMode);

                // Draw each pickable element in select buffer
                gl.getGL2().glMatrixMode(GLMatrixFunc.GL_MODELVIEW);

                synchronized (this) {
                    for (Pickable pickable : pickablePoints) {
                        setCurrentName(gl, pickable);
                        pickable.setTransform(viewTransform);
                        pickable.draw(gl, glu, camera);
                        releaseCurrentName(gl);
                    }
                }
                // Back to projection matrix
                gl.getGL2().glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            }
            gl.getGL2().glPopMatrix();
            gl.glFlush();

            // Process hits
            int hits = gl.getGL2().glRenderMode(GL2.GL_RENDER);
            if (hits < 1) {
                return;
            }
            selectBuffer.get(selectBuf);
            int minZ = Integer.MAX_VALUE;
            int pickableIndex = -1;
            for (int j = 0; j < hits; j++) {
                if (selectBuf[4 * j + 1] < minZ) {
                    minZ = selectBuf[4 * j + 1];
                    pickableIndex = selectBuf[4 * j + 3];
                }
            }
            final PickablePointWithTarget pickable = pickablePoints[pickableIndex];

            // Trigger an event
            List<Object> clickedObjects = new ArrayList<>(1);
            Object vertex = pickable.getTarget();
            clickedObjects.add(vertex);
            perf.toc();

            fireObjectPicked(clickedObjects);
        }

    }

    private class PickablePointWithTarget extends PickablePoint {

        private final Object target;

        PickablePointWithTarget(Coord3d xyz, Object target, int id) {
            super(xyz);
            this.target = target;
            setPickingId(id);
        }

        Object getTarget() {
            return target;
        }
    }

    public class ScatterPlot3DCameraThreadController extends CameraThreadController {

        public ScatterPlot3DCameraThreadController(Chart chart) {
            super(chart);
        }

        @Override
        protected void rotate(Coord2d move) {
            super.rotate(move);
            updateLine();
            adjustProjectionScatterToBounds();
        }

    }

}
