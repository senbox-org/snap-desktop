package org.esa.snap.ui.loading;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by jcoravu on 28/12/2018.
 */
public class CircularProgressIndicatorLabel extends JLabel {

    private final CircularProgressPainter circularProgressPainter;

    private Timer timer;
    private boolean isRunning;

    public CircularProgressIndicatorLabel() {
        this.circularProgressPainter = new CircularProgressPainter(Color.LIGHT_GRAY, getForeground());
        this.isRunning = false;

        setIcon(new PainterIcon(this.circularProgressPainter, this.circularProgressPainter.getPreferredSize()));
    }

    @Override
    public void removeNotify() {
        stopAnimation();

        super.removeNotify();
    }

    @Override
    public void addNotify() {
        super.addNotify();

        startAnimation();
    }

    public void setRunning(boolean isRunning) {
        boolean timerDefined = (this.timer != null);
        if (!timerDefined && isRunning) {
            this.isRunning = true;
            startAnimation();
        } else if (timerDefined && !isRunning) {
            this.isRunning = false;
            stopAnimation();
        }
    }

    private void startAnimation() {
        if (!this.isRunning || getParent() == null) {
            return;
        }
        stopAnimation();

        this.timer = new Timer(100, new ActionListener() {
            private int frame = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                frame = (frame + 1) % circularProgressPainter.getPoints();
                circularProgressPainter.setFrame(frame);
                repaint();
            }
        });
        this.timer.start(); // start the timer
    }

    private void stopAnimation() {
        if (this.timer != null) {
            this.timer.stop();
            this.circularProgressPainter.setFrame(-1);
            repaint();
            this.timer = null;
        }
    }

    private static class PainterIcon implements Icon {

        private final Dimension size;
        private final CircularProgressPainter painter;

        public PainterIcon(CircularProgressPainter painter, Dimension size) {
            this.painter = painter;
            this.size = size;
        }

        @Override
        public int getIconHeight() {
            return this.size.height;
        }

        @Override
        public int getIconWidth() {
            return this.size.width;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            if (this.painter != null && graphics instanceof Graphics2D) {
                graphics = graphics.create();
                graphics.translate(x, y);

                this.painter.doPaint((Graphics2D) graphics, getIconWidth(), getIconHeight());

                graphics.translate(-x, -y);
                graphics.dispose();
            }
        }
    }
}
