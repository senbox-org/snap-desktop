package org.esa.snap.rcp.statistics;

import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.texture.TextureIO;
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
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.primitives.pickable.Pickable;
import org.jzy3d.plot3d.primitives.pickable.PickablePoint;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.scene.Graph;
import org.jzy3d.plot3d.rendering.view.Camera;
import org.jzy3d.plot3d.rendering.view.View;
import org.jzy3d.plot3d.rendering.view.modes.CameraMode;
import org.jzy3d.plot3d.text.DrawableTextWrapper;
import org.jzy3d.plot3d.text.renderers.TextBitmapRenderer;
import org.jzy3d.plot3d.transform.Transform;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
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
    private Scatter scatter;
    private ScatterPlot3DProjectionScatter projectionScatter;
    private JLabel titleLabel;
    private PickingSupport pickingSupport;
    private Range xRange;
    private Range yRange;
    private Range zRange;
    private DrawableTextWrapper textWrapper;
    private LineStrip xLine;
    private LineStrip yLine;
    private LineStrip zLine;
    private Coord3d currentVertex;
    private static final Color PROJECTION_COLOR = new Color(0.8f, 0.8f, 0.8f, 0.25f);

    void init() {
        chart = NewtChartComponentFactory.chart(Quality.Nicest, "newt");
        pickingSupport = new ScatterPlot3DPickingSupport();
        textWrapper = new DrawableTextWrapper("", new Coord3d(0, 0, 0),
                Color.BLACK, new TextBitmapRenderer());
        pickingSupport.addObjectPickedListener((vertices, picking) -> {
            for (Object vertex : vertices) {
                final Coord3d vertexCoords = (Coord3d) vertex;
                textWrapper.setText("x = " + vertexCoords.x + ", y = " + vertexCoords.y + ", z = " + vertexCoords.z);
                textWrapper.setPosition(vertexCoords);
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
        scatter = new Scatter();
        scatter.setWidth(10.0f);
        chart.getScene().add(scatter);
        chart.add(textWrapper);
        setLayout(new BorderLayout());
        ScatterPlot3dCameraMouseController cameraController =
                new ScatterPlot3dCameraMouseController(this.chart, pickingSupport);
        titleLabel = new JLabel();
        add(titleLabel, BorderLayout.NORTH);
        add((Component) this.chart.getCanvas(), BorderLayout.CENTER);
    }

    private void updateLine() {
        xLine.get(0).setCoord(currentVertex);
        xLine.get(1).setCoord(new Coord3d(getXAxisCoord(), currentVertex.y, currentVertex.z));
        yLine.get(0).setCoord(currentVertex);
        yLine.get(1).setCoord(new Coord3d(currentVertex.x, getYAxisCoord(), currentVertex.z));
        zLine.get(0).setCoord(currentVertex);
        zLine.get(1).setCoord(new Coord3d(currentVertex.x, currentVertex.y, getZAxisCoord()));
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

    void setChartTitle(String chartTitle) {
        titleLabel.setText(chartTitle);
    }

    void setLabelNames(String xLabelName, String yLabelName, String zLabelName) {
        chart.getAxeLayout().setXAxeLabel("x =" + xLabelName);
        chart.getAxeLayout().setYAxeLabel("y = " + yLabelName);
        chart.getAxeLayout().setZAxeLabel("z = " + zLabelName);
        chart.getAxeLayout().setFaceDisplayed(true);
    }

    void setChartData(List<Float> xData, List<Float> yData, List<Float> zData) {
        final int size = xData.size();
        Coord3d[] points = new Coord3d[size];
        for (int i = 0; i < size; i++) {
            points[i] = new Coord3d(xData.get(i), yData.get(i), zData.get(i));
            PickablePoint pickablePoint = new PickablePoint(points[i]);
            pickingSupport.registerPickableObject(pickablePoint, points[i]);
        }
        scatter.setData(points);
        projectionScatter.setData(points);
        adjustProjectionScatterToBounds();
    }

    void setColors(List<Integer> colorData) {
        Color[] colors = new Color[colorData.size() / 3];
        for (int i = 0; i < colorData.size() / 3; i++) {
            colors[i] = new Color(colorData.get(3 * i), colorData.get(3 * i + 1), colorData.get(3 * i + 2), 63);
        }
        scatter.setColors(colors);
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
            } else {
                int x = e.getX();
                int y = e.getY();
                int yflip = -y + targets.get(0).getCanvas().getRendererHeight();
                prevMouse.x = x;
                prevMouse.y = y;// yflip;
                View view = targets.get(0).getView();
                GL gl = chart().getView().getCurrentGL();

//                will trigger vertex selection event to those subscribing to
                pickingSupport.pickObjects(gl, glu, view, null, new IntegerCoord2d(x, yflip));
                chart().getView().getCurrentContext().release();
            }
        }
    }

    private class ScatterPlot3DPickingSupport extends PickingSupport {

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
                    for (Pickable pickable : pickables.values()) {
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
            final Pickable pickable = pickables.get(pickableIndex);

            // Trigger an event
            List<Object> clickedObjects = new ArrayList<>(hits);
            Object vertex = pickableTargets.get(pickable);
            clickedObjects.add(vertex);
            perf.toc();

            fireObjectPicked(clickedObjects);
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
