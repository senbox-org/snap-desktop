package org.esa.snap.rcp.statistics;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.glu.GLU;
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
    private List<Projector> ACTIVE_PROJECTORS;

    private float scaleFactor;
    private Projector xProjector;
    private Projector yProjector;
    private Projector zProjector;

    ScatterPlot3DProjectionScatter() {
        super();
        scaleFactor = 1f;
        xProjector = new XProjector();
        yProjector = new YProjector();
        zProjector = new ZProjector();
        ACTIVE_PROJECTORS = new ArrayList<>();
        initSinesAndCosines();
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
        if (coordinates != null && !ACTIVE_PROJECTORS.isEmpty()) {
            if (colors == null)
                GLES2CompatUtils.glColor4f(rgb.r, rgb.g, rgb.b, rgb.a);
            for (Projector projector : ACTIVE_PROJECTORS) {
                projector.prepareDrawing();
            }
            int k = 0;
            for (Coord3d c : coordinates) {
                GLES2CompatUtils.glDisable(GL.GL_CULL_FACE);
                GLES2CompatUtils.glPolygonMode(GL.GL_FRONT_AND_BACK, GL_FILL);
                if (colors != null) {
                    GLES2CompatUtils.glColor4f(colors[k].r, colors[k].g, colors[k].b, colors[k].a);
                    k++;
                }
                for (Projector projector : ACTIVE_PROJECTORS) {
                    projector.drawGLES2(c);
                }
            }
        }
    }

    @Override
    public void drawGL2(GL gl) {
        if (coordinates != null && !ACTIVE_PROJECTORS.isEmpty()) {
            if (colors == null)
                gl.getGL2().glColor4f(rgb.r, rgb.g, rgb.b, rgb.a);
            for (Projector projector : ACTIVE_PROJECTORS) {
                projector.prepareDrawing();
            }
            int k = 0;
            for (Coord3d c : coordinates) {
                gl.getGL2().glDisable(GL.GL_CULL_FACE);
                gl.getGL2().glPolygonMode(GL.GL_FRONT_AND_BACK, GL_FILL);
                if (colors != null) {
                    gl.getGL2().glColor4f(colors[k].r, colors[k].g, colors[k].b, colors[k].a);
                    k++;
                }
                for (Projector projector : ACTIVE_PROJECTORS) {
                    projector.drawGL2(c, gl);
                }
            }
        }
    }

    void scale(float scaleFactor) {
        this.scaleFactor *= scaleFactor;
    }

    void setEdgeCoords(float xEdgeCoord, float yEdgeCoord, float zEdgeCoord) {
        xProjector.setEdgeCoord(xEdgeCoord);
        yProjector.setEdgeCoord(yEdgeCoord);
        zProjector.setEdgeCoord(zEdgeCoord);
    }

    void projectToX(boolean projectToX) {
        if (projectToX && !ACTIVE_PROJECTORS.contains(xProjector)) {
            ACTIVE_PROJECTORS.add(xProjector);
        } else if (!projectToX) {
            ACTIVE_PROJECTORS.remove(xProjector);
        }
    }

    void projectToY(boolean projectToY) {
        if (projectToY && !ACTIVE_PROJECTORS.contains(yProjector)) {
            ACTIVE_PROJECTORS.add(yProjector);
        } else if (!projectToY) {
            ACTIVE_PROJECTORS.remove(yProjector);
        }
    }

    void projectToZ(boolean projectToZ) {
        if (projectToZ && !ACTIVE_PROJECTORS.contains(zProjector)) {
            ACTIVE_PROJECTORS.add(zProjector);
        } else if (!projectToZ) {
            ACTIVE_PROJECTORS.remove(zProjector);
        }
    }

    private abstract class Projector {

        float edgeCoord;

        Projector() {
            edgeCoord = Float.POSITIVE_INFINITY;
        }

        void setEdgeCoord(float edgeCoord) {
            this.edgeCoord = edgeCoord;
        }

        abstract void prepareDrawing();

        abstract void drawGLES2(Coord3d c);

        abstract void drawGL2(Coord3d c, GL gl);

    }

    private class XProjector extends Projector {

        private float[][] circleEdges;

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
