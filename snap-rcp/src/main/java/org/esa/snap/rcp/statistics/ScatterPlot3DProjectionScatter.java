package org.esa.snap.rcp.statistics;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.glu.GLU;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.rendering.compat.GLES2CompatUtils;
import org.jzy3d.plot3d.rendering.view.Camera;

import java.util.ArrayList;
import java.util.List;

import static com.jogamp.opengl.GL2GL3.GL_FILL;

class ScatterPlot3DProjectionScatter extends Scatter {

    private static final int NUM_SEGMENTS = 12;
    private static float[] SINES;
    private static float[] COSINES;
    private float scaleFactor;
    private boolean projectToX;
    private boolean projectToY;
    private boolean projectToZ;
    private float xEdgeCoord;
    private float yEdgeCoord;
    private float zEdgeCoord;
    private BoundingBox3d axisBounds;

    ScatterPlot3DProjectionScatter() {
        super();
        scaleFactor = 1f;
        this.axisBounds = new BoundingBox3d();
        initSinesAndCosines();
    }

    void setAxisBounds(BoundingBox3d axisBounds) {
        this.axisBounds = axisBounds;
    }

    private void initSinesAndCosines() {
        SINES = new float[NUM_SEGMENTS + 1];
        COSINES = new float[NUM_SEGMENTS + 1];
        float theta = (2 * (float) Math.PI) / NUM_SEGMENTS;
        for (int i = 0; i < NUM_SEGMENTS; i++) {
            SINES[i] = (float) Math.sin(theta * i);
            COSINES[i] = (float) Math.cos(theta * i);
        }
        SINES[NUM_SEGMENTS] = SINES[0];
        COSINES[NUM_SEGMENTS] = COSINES[0];
    }

    @Override
    public void draw(GL gl, GLU glu, Camera cam) {
        this.doTransform(gl, glu, cam);
        if (gl.isGL2()) {
            this.drawGL2(gl);
        } else {
            this.drawGLES2();
        }

        this.doDrawBounds(gl, glu, cam);
    }

    @Override
    public void drawGLES2() {
        List<Projector> projectors = getProjectors();
        if (coordinates != null && !projectors.isEmpty()) {
            if (colors == null)
                GLES2CompatUtils.glColor4f(rgb.r, rgb.g, rgb.b, rgb.a);
            for (Projector projector : projectors) {
                projector.prepareDrawing();
            }
            int k = 0;
            for (Coord3d c : coordinates) {
                if (axisBounds.contains(c)) {
                    GLES2CompatUtils.glDisable(GL.GL_CULL_FACE);
                    GLES2CompatUtils.glPolygonMode(GL.GL_FRONT_AND_BACK, GL_FILL);
                    if (colors != null) {
                        GLES2CompatUtils.glColor4f(colors[k].r, colors[k].g, colors[k].b, colors[k].a);
                        k++;
                    }
                    for (Projector projector : projectors) {
                        projector.drawGLES2(c);
                    }
                }
            }
        }
    }

    @Override
    public void drawGL2(GL gl) {
        List<Projector> projectors = getProjectors();
        if (coordinates != null && !projectors.isEmpty()) {
            if (colors == null)
                gl.getGL2().glColor4f(rgb.r, rgb.g, rgb.b, rgb.a);
            for (Projector projector : projectors) {
                projector.prepareDrawing();
            }
            int k = 0;
            for (Coord3d c : coordinates) {
                if (axisBounds.contains(c)) {
                    gl.getGL2().glDisable(GL.GL_CULL_FACE);
                    gl.getGL2().glPolygonMode(GL.GL_FRONT_AND_BACK, GL_FILL);
                    if (colors != null) {
                        gl.getGL2().glColor4f(colors[k].r, colors[k].g, colors[k].b, colors[k].a);
                        k++;
                    }
                    for (Projector projector : projectors) {
                        projector.drawGL2(c, gl);
                    }
                }
            }
        }
    }

    void scale(float scaleFactor) {
        this.scaleFactor *= scaleFactor;
    }

    public void resetScale() {
        this.scaleFactor = 1f;
    }

    void setEdgeCoords(float xEdgeCoord, float yEdgeCoord, float zEdgeCoord) {
        this.xEdgeCoord = xEdgeCoord;
        this.yEdgeCoord = yEdgeCoord;
        this.zEdgeCoord = zEdgeCoord;
    }

    void projectToX(boolean projectToX) {
        this.projectToX = projectToX;
    }

    void projectToY(boolean projectToY) {
        this.projectToY = projectToY;
    }

    void projectToZ(boolean projectToZ) {
        this.projectToZ = projectToZ;
    }

    private List<Projector> getProjectors() {
        List<Projector> projectors = new ArrayList<>();
        if (projectToX) {
            projectors.add(new XProjector(xEdgeCoord));
        }
        if (projectToY) {
            projectors.add(new YProjector(yEdgeCoord));
        }
        if (projectToZ) {
            projectors.add(new ZProjector(zEdgeCoord));
        }
        return projectors;
    }

    private abstract class Projector {

        float edgeCoord;

        Projector(float edgeCoord) {
            this.edgeCoord = edgeCoord;
        }

        abstract void prepareDrawing();

        abstract void drawGLES2(Coord3d c);

        abstract void drawGL2(Coord3d c, GL gl);

    }

    private class XProjector extends Projector {

        private float[][] circleEdges;

        XProjector(float xEdgeCoord) {
            super(xEdgeCoord);
        }

        public void prepareDrawing() {
            float yWidth = (bbox.getYmax() - bbox.getYmin()) / 100 * scaleFactor;
            float zWidth = (bbox.getZmax() - bbox.getZmin()) / 100 * scaleFactor;
            circleEdges = new float[NUM_SEGMENTS + 1][2];
            for (int i = 0; i <= NUM_SEGMENTS; i++) {
                circleEdges[i][0] = yWidth * COSINES[i];
                circleEdges[i][1] = zWidth * SINES[i];
            }
        }

        @Override
        public void drawGLES2(Coord3d c) {
            GLES2CompatUtils.glBegin(GL.GL_TRIANGLE_FAN);
            GLES2CompatUtils.glVertex3d(edgeCoord, c.y, c.z);
            for (float[] circleEdge : circleEdges) {
                GLES2CompatUtils.glVertex3d(edgeCoord, circleEdge[0] + c.y, circleEdge[1] + c.z);
            }
            GLES2CompatUtils.glEnd();
        }

        @Override
        void drawGL2(Coord3d c, GL gl) {
            gl.getGL2().glBegin(GL.GL_TRIANGLE_FAN);
            gl.getGL2().glVertex3d(edgeCoord, c.y, c.z);
            for (float[] circleEdge : circleEdges) {
                gl.getGL2().glVertex3d(edgeCoord, circleEdge[0] + c.y, circleEdge[1] + c.z);
            }
            gl.getGL2().glEnd();
        }
    }

    private class YProjector extends Projector {

        private float[][] circleEdges;

        YProjector(float yEdgeCoord) {
            super(yEdgeCoord);
        }

        public void prepareDrawing() {
            float xWidth = (bbox.getXmax() - bbox.getXmin()) / 100 * scaleFactor;
            float zWidth = (bbox.getZmax() - bbox.getZmin()) / 100 * scaleFactor;
            circleEdges = new float[NUM_SEGMENTS + 1][2];
            for (int i = 0; i <= NUM_SEGMENTS; i++) {
                circleEdges[i][0] = xWidth * COSINES[i];
                circleEdges[i][1] = zWidth * SINES[i];
            }
        }

        @Override
        public void drawGLES2(Coord3d c) {
            GLES2CompatUtils.glBegin(GL.GL_TRIANGLE_FAN);
            GLES2CompatUtils.glVertex3d(c.x, edgeCoord, c.z);
            for (float[] circleEdge : circleEdges) {
                GLES2CompatUtils.glVertex3d(circleEdge[0] + c.x, edgeCoord, circleEdge[1] + c.z);
            }
            GLES2CompatUtils.glEnd();
        }

        @Override
        void drawGL2(Coord3d c, GL gl) {
            gl.getGL2().glBegin(GL.GL_TRIANGLE_FAN);
            gl.getGL2().glVertex3d(c.x, edgeCoord, c.z);
            for (float[] circleEdge : circleEdges) {
                gl.getGL2().glVertex3d(circleEdge[0] + c.x, edgeCoord, circleEdge[1] + c.z);
            }
            gl.getGL2().glEnd();
        }
    }

    private class ZProjector extends Projector {

        private float[][] circleEdges;

        ZProjector(float zEdgeCoord) {
            super(zEdgeCoord);
        }

        public void prepareDrawing() {
            float xWidth = (bbox.getXmax() - bbox.getXmin()) / 100 * scaleFactor;
            float yWidth = (bbox.getYmax() - bbox.getYmin()) / 100 * scaleFactor;
            circleEdges = new float[NUM_SEGMENTS + 1][2];
            for (int i = 0; i <= NUM_SEGMENTS; i++) {
                circleEdges[i][0] = xWidth * COSINES[i];
                circleEdges[i][1] = yWidth * SINES[i];
            }
        }

        @Override
        public void drawGLES2(Coord3d c) {
            GLES2CompatUtils.glBegin(GL.GL_TRIANGLE_FAN);
            GLES2CompatUtils.glVertex3d(c.x, c.y, edgeCoord);
            for (float[] circleEdge : circleEdges) {
                GLES2CompatUtils.glVertex3d(circleEdge[0] + c.x, circleEdge[1] + c.y, edgeCoord);
            }
            GLES2CompatUtils.glEnd();
        }

        @Override
        void drawGL2(Coord3d c, GL gl) {
            gl.getGL2().glBegin(GL.GL_TRIANGLE_FAN);
            gl.getGL2().glVertex3d(c.x, c.y, edgeCoord);
            for (float[] circleEdge : circleEdges) {
                gl.getGL2().glVertex3d(circleEdge[0] + c.x, circleEdge[1] + c.y, edgeCoord);
            }
            gl.getGL2().glEnd();
        }
    }

}
