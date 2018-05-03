package org.esa.snap.rcp.statistics;

import com.jogamp.opengl.GL;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.rendering.compat.GLES2CompatUtils;

class ScatterPlot3DScatter extends Scatter {

    private boolean displayOnlyDataInAxisBounds;
    private BoundingBox3d axisBounds;

    ScatterPlot3DScatter() {
        displayOnlyDataInAxisBounds = false;
        axisBounds = new BoundingBox3d();
    }

    void displayOnlyDataInAxisBounds(boolean displayOnlyDataInAxisBounds) {
        this.displayOnlyDataInAxisBounds = displayOnlyDataInAxisBounds;
    }

    void setAxisBounds(BoundingBox3d axisBounds) {
        this.axisBounds = axisBounds;
    }

    public void drawGLES2() {
        GLES2CompatUtils.glPointSize(width);

        GLES2CompatUtils.glBegin(GL.GL_POINTS);
        if (colors == null)
            GLES2CompatUtils.glColor4f(rgb.r, rgb.g, rgb.b, rgb.a);
        if (coordinates != null) {
            int k = 0;
            for (Coord3d c : coordinates) {
                if (colors != null && k < colors.length) {
                    GLES2CompatUtils.glColor4f(colors[k].r, colors[k].g, colors[k].b, colors[k].a);
                    k++;
                }
                if (!displayOnlyDataInAxisBounds || axisBounds.contains(c)) {
                    GLES2CompatUtils.glVertex3f(c.x, c.y, c.z);
                }
            }
        }
        GLES2CompatUtils.glEnd();
    }

    public void drawGL2(GL gl) {
        gl.getGL2().glPointSize(width);

        gl.getGL2().glBegin(GL.GL_POINTS);
        if (colors == null)
            gl.getGL2().glColor4f(rgb.r, rgb.g, rgb.b, rgb.a);
        if (coordinates != null) {
            int k = 0;
            for (Coord3d c : coordinates) {
                if (colors != null && k < colors.length) {
                    gl.getGL2().glColor4f(colors[k].r, colors[k].g, colors[k].b, colors[k].a);
                    k++;
                }
                if (!displayOnlyDataInAxisBounds || axisBounds.contains(c)) {
                    gl.getGL2().glVertex3f(c.x, c.y, c.z);
                }
            }
        }
        gl.getGL2().glEnd();
    }

}
