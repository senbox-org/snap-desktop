package org.esa.snap.graphbuilder.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import org.esa.snap.graphbuilder.ui.components.helpers.AddNodeWidget;
import org.esa.snap.graphbuilder.ui.components.helpers.GraphKeyEventDispatcher;

public class GraphPanel extends JPanel implements KeyListener {

    /**
     * Genrated UID
     */
    private static final long serialVersionUID = -8787328074424783352L;

    private static final int gridSize = 15;
    private static final int gridMajor = 5;
    private static final Color gridMajorColor = new Color(255, 255, 255, 80);
    private static final Color gridMinorColor = new Color(255, 255, 255, 40);
    private BufferedImage gridPattern = null;

    private AddNodeWidget addNodeWidget;

    public GraphPanel() {
        super();
        this.setBackground(Color.darkGray);
        this.addNodeWidget = new AddNodeWidget();

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new GraphKeyEventDispatcher(this));
    }

    /**
     * Paints the panel component
     *
     * @param g The Graphics
     */
    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        drawGrid(g2);
        this.addNodeWidget.paint(getWidth(), getHeight(), g2);
    }

    private void drawGrid(Graphics2D g) {
        int width = getWidth();
        int height = getHeight();
        if (gridPattern == null || gridPattern.getWidth() != width || gridPattern.getHeight() != height) {
            // initalize gridPattern image buffer
            gridPattern = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            Graphics2D gbuff = (Graphics2D) gridPattern.getGraphics();

            int nCol = (int) Math.ceil(width / (float) gridSize);
            int nRow = (int) Math.ceil(height / (float) gridSize);
            int nMax = Math.max(nCol, nRow);

            Stroke majorStroke = new BasicStroke(2);
            Stroke minorStroke = new BasicStroke(1);

            for (int i = 0; i <= nMax; i++) {
                int pos = i * gridSize;

                if (i % gridMajor == 0) {
                    // set style for major lines
                    gbuff.setStroke(majorStroke);
                    gbuff.setColor(gridMajorColor);
                } else {
                    // set style for minor lines
                    gbuff.setStroke(minorStroke);
                    gbuff.setColor(gridMinorColor);
                }

                if (i <= nRow) {
                    // draw row
                    gbuff.drawLine(0, pos, width, pos);
                }
                if (i <= nCol) {
                    // draw col
                    gbuff.drawLine(pos, 0, pos, height);
                }
            }

        }
        // render gridPattern buffer image
        g.drawImage(gridPattern, 0, 0, null);
    }

    @Override
    public void keyPressed(KeyEvent event) {
        int key = event.getKeyCode();

        if (key == 8 && this.addNodeWidget.isVisible()) {
            // backspace
            this.addNodeWidget.backspace();
            this.repaint(this.addNodeWidget.getBoundingRect(getWidth(), getHeight()));
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        int key = event.getKeyCode();
        
        if (event.isControlDown() && key == KeyEvent.VK_TAB) {//65) {
            this.addNodeWidget.changeStatus();
            this.repaint(); //this.addNodeWidget.getBoundingRect(getWidth(), getHeight()));
            return;
        }

        if (this.addNodeWidget.isVisible()) {
            if (key == 10) {
                // return
                this.addNodeWidget.hide();
            } else if (key == 27) {
                // escape
                this.addNodeWidget.hide();
            } else {
                this.addNodeWidget.type(event.getKeyChar());
            } 
            this.repaint();
        }
    }

    @Override
    public void keyTyped(KeyEvent event) {}
}