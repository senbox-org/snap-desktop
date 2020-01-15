package org.esa.snap.graphbuilder.ui.components;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import org.esa.snap.graphbuilder.ui.components.graph.NodeGui;
import org.esa.snap.graphbuilder.ui.components.utils.AddNodeWidget;
import org.esa.snap.graphbuilder.ui.components.utils.GraphKeyEventDispatcher;
import org.esa.snap.graphbuilder.ui.components.utils.GridUtils;
import org.esa.snap.graphbuilder.ui.components.utils.OperatorManager;

public class GraphPanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

    /**
     * Genrated UID
     */
    private static final long serialVersionUID = -8787328074424783352L;

    private BufferedImage gridPattern = null;

    private AddNodeWidget addNodeWidget;

    private ArrayList<NodeGui> nodes = new ArrayList<>();
    private Point lastMousePosition = new Point(0, 0);
    private OperatorManager operatorManager = new OperatorManager();

    public GraphPanel() {
        super();
        this.setBackground(Color.darkGray);
        this.addNodeWidget = new AddNodeWidget(operatorManager);

        this.addMouseMotionListener(this);
        this.addMouseListener(this);
        this.addMouseWheelListener(this);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new GraphKeyEventDispatcher(this));
    }

    private void addNode(NodeGui node) {
        if (node != null) {
            node.setPosition(GridUtils.normalize(node.getPostion()));
            this.nodes.add(node);
        }
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
        drawNodes(g2);
        this.addNodeWidget.paint(getWidth(), getHeight(), g2);
    }

    private void drawGrid(Graphics2D g) {
        int width = getWidth();
        int height = getHeight();
        if (gridPattern == null || gridPattern.getWidth() != width || gridPattern.getHeight() != height) {
            // initalize gridPattern image buffer
            gridPattern = GridUtils.gridPattern(width, height);
        }
        // render gridPattern buffer image
        g.drawImage(gridPattern, 0, 0, null);
    }

    private void drawNodes(Graphics2D g) {
        Graphics2D gNode = (Graphics2D) g.create();
        for (NodeGui node : nodes) {
            node.paintNode(gNode);
        }
        gNode.dispose();
    }

    @Override
    public void keyPressed(KeyEvent event) {
        int key = event.getKeyCode();

        if (this.addNodeWidget.isVisible()) {
            switch (key) {
            case (KeyEvent.VK_UP):
                this.addNodeWidget.up();
                this.repaint();
                break;
            case (KeyEvent.VK_DOWN):
                this.addNodeWidget.down();
                this.repaint();
                break;
            case (KeyEvent.VK_BACK_SPACE):
                // backspace
                this.addNodeWidget.backspace();
                this.repaint(); // this.addNodeWidget.getBoundingRect(getWidth(), getHeight()));
                break;
            }

        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        int key = event.getKeyCode();

        if (key == KeyEvent.VK_TAB) {// 65) {
            this.addNodeWidget.changeStatus(lastMousePosition);
            this.repaint();// this.addNodeWidget.getBoundingRect(getWidth(), getHeight())); //
                           // this.addNodeWidget.getBoundingRect(getWidth(),
                           // getHeight()));
            return;
        }

        if (this.addNodeWidget.isVisible()) {
            switch (key) {
            case (10):
                // return
                this.addNode(this.addNodeWidget.enter());
                break;
            case (27):
                // escape
                this.addNodeWidget.hide();
                break;
            case (KeyEvent.VK_UP):
                break;
            case (KeyEvent.VK_DOWN):
                break;
            case (KeyEvent.VK_BACK_SPACE):
                break;
            default:
                this.addNodeWidget.type(event.getKeyChar());
                break;
            }
            this.repaint();
        }
    }

    @Override
    public void keyTyped(KeyEvent event) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        lastMousePosition = e.getPoint();
        if (addNodeWidget.isVisible()) {
            if (addNodeWidget.mouseMoved(lastMousePosition))
                repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            addNodeWidget.hide();
        } else {
            if (addNodeWidget.isVisible()) {
                NodeGui node = addNodeWidget.click(e.getPoint());
                if (node != null) {
                    addNode(node);
                    repaint();
                    return;
                }
                repaint();
            }
        }

        // TODO Auto-generated method stub

    }

    @Override
    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (addNodeWidget.isVisible()){
            int rotation = e.getWheelRotation();
            boolean up = rotation < 0;
            for (int i = 0; i < Math.abs(rotation); i ++ ) {
                if (up) {
                    addNodeWidget.up();
                } else {
                    addNodeWidget.down();
                }
            }
            repaint();
        }
    }

  
}