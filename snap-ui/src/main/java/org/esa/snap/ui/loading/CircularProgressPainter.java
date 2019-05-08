package org.esa.snap.ui.loading;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Created by jcoravu on 28/12/2018.
 */
public class CircularProgressPainter {

    private final Color baseColor;
    private final Color highlightColor;
    private final int trailLength;
    private final int points;
    private final float barWidth;
    private final float barLength;
    private final float centerDistance;

    private int frame;

    public CircularProgressPainter(Color baseColor, Color highlightColor) {
        this.baseColor = baseColor;
        this.highlightColor = highlightColor;

        this.frame = -1;
        this.points = 8;
        this.barWidth = 4;
        this.barLength = 10;
        this.centerDistance = 7;
        this.trailLength = 3;
    }

    protected void doPaint(Graphics2D graphics, int width, int height) {
        RoundRectangle2D rect = new RoundRectangle2D.Float(this.centerDistance, -this.barWidth / 2, this.barLength, this.barWidth, this.barWidth, this.barWidth);
        graphics.setColor(Color.GRAY);

        graphics.translate(width / 2, height / 2);
        for (int i = 0; i < this.points; i++) {
            graphics.setColor(computeFrameColor(i));
            graphics.fill(rect);
            graphics.rotate(Math.PI * 2.0 / (double) this.points); // rotate clockwise direction
        }
    }

    public int getPoints() {
        return points;
    }

    public Dimension getPreferredSize() {
        int size = (int) (2 * (this.centerDistance + this.barLength)) + 5;
        return new Dimension(size, size);
    }

    public void setFrame(int frame) {
        this.frame = frame;
    }

    private Color computeFrameColor(int index) {
        if (this.frame == -1) {
            return this.baseColor;
        }
        for (int i = 0; i < this.trailLength; i++) {
            if (index == (this.frame - i + this.points) % this.points) {
                float terp = 1.0f - ((float) (this.trailLength - i)) / (float) this.trailLength;
                return interpolate(this.baseColor, this.highlightColor, terp);
            }
        }
        return this.baseColor;
    }

    private static Color interpolate(Color color1, Color color2, float factor) {
        float[] componentsClolor1 = color1.getRGBComponents(null);
        float[] componentsClolor2 = color2.getRGBComponents(null);
        float[] componentsNewColor = new float[4];
        for (int i = 0; i < 4; i++) {
            componentsNewColor[i] = componentsClolor2[i] + (componentsClolor1[i] - componentsClolor2[i]) * factor;
        }
        return new Color(componentsNewColor[0], componentsNewColor[1], componentsNewColor[2], componentsNewColor[3]);
    }
}