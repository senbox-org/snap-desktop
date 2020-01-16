package org.esa.snap.graphbuilder.ui.components;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JPopupMenu;

import org.esa.snap.graphbuilder.ui.components.graph.NodeGui;
import org.esa.snap.graphbuilder.ui.components.utils.AddNodeWidget;
import org.esa.snap.graphbuilder.ui.components.utils.GraphKeyEventDispatcher;
import org.esa.snap.graphbuilder.ui.components.utils.GraphListener;
import org.esa.snap.graphbuilder.ui.components.utils.GridUtils;
import org.esa.snap.graphbuilder.ui.components.utils.OperatorManager;

public class GraphPanel extends JPanel
        implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener, ActionListener {

    /**
     * Genrated UID
     */
    private static final long serialVersionUID = -8787328074424783352L;

    private BufferedImage gridPattern = null;

    private AddNodeWidget addNodeWidget;

    private Point lastMousePosition = new Point(0, 0);

    private ArrayList<NodeGui> nodes = new ArrayList<>();
    private NodeGui selectedNode = null;
    private NodeGui activeNode = null;
    private Point activeNodeRelPosition = new Point(0, 0);

    private OperatorManager operatorManager = new OperatorManager();

    private ArrayList<GraphListener> graphListeners = new ArrayList<>();

    private JPopupMenu addMenu;

    public GraphPanel() {
        super();
        this.setBackground(Color.darkGray);
        this.addNodeWidget = new AddNodeWidget(operatorManager);
        this.addMenu = new JPopupMenu();
        addMenu.add(operatorManager.createOperatorMenu(this));

        this.addMouseMotionListener(this);
        this.addMouseListener(this);
        this.addMouseWheelListener(this);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new GraphKeyEventDispatcher(this));
    }

    private void addNode(NodeGui node) {
        if (node != null) {
            node.setPosition(GridUtils.normalize(lastMousePosition));
            this.nodes.add(node);
            for (GraphListener listener : graphListeners) {
                listener.created(node);
            }
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

        if (key == 127) {
            // DELETE KEY
            removeSelectedNode();
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

    private void removeSelectedNode() {
        if (selectedNode != null) {
            this.nodes.remove(selectedNode);
            for (GraphListener listener : graphListeners) {
                listener.deleted(selectedNode);
            }
            selectedNode = null;
            repaint();
        }
    }

    @Override
    public void keyTyped(KeyEvent event) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (activeNode != null) {
            int x = e.getX() - activeNodeRelPosition.x;
            int y = e.getY() - activeNodeRelPosition.y;
            moveNode(activeNode, x, y);
            repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        lastMousePosition = e.getPoint();
        if (addNodeWidget.isVisible()) {
            if (addNodeWidget.mouseMoved(lastMousePosition)) {
                repaint();
                return;
            }
        }
        for (NodeGui node : nodes) {
            if (node.contains(lastMousePosition)) {
                node.over();
            } else {
                node.none();
            }
        }
        this.repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            addNodeWidget.hide();
            if (e.getButton() == MouseEvent.BUTTON3) {
                addMenu.show(this, e.getX(), e.getY());
            }
        } else {
            if (addNodeWidget.isVisible()) {
                NodeGui node = addNodeWidget.click(e.getPoint());
                if (node != null) {
                    addNode(node);
                    repaint();
                    return;
                }
            }
        }

        for (NodeGui node : nodes) {
            if (node.contains(lastMousePosition)) {
                selectNode(node);
            } else {
                node.none();
            }
        }
        repaint();

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (addNodeWidget.isVisible()) {
                NodeGui node = addNodeWidget.click(e.getPoint());
                if (node != null) {
                    addNode(node);
                    activeNode = node;
                    activeNodeRelPosition = new Point(10, 10);
                    moveNode(node, e.getX() - 10, e.getY() - 10);
                    repaint();
                    return;
                }
            }
            Point p = e.getPoint();
            for (NodeGui node : nodes) {
                if (node.contains(p)) {
                    activeNode = node;
                    activeNodeRelPosition = new Point(p.x - node.getX(), p.y - node.getY());
                    return;
                }
            }
        } else {
            activeNode = null;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (activeNode != null) {
            Point p = GridUtils.normalize(activeNode.getPostion());
            moveNode(activeNode, p.x, p.y);

            activeNode = null;
            repaint();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Nothing to do...
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (activeNode != null) {
            activeNode.setPosition(GridUtils.normalize(activeNode.getPostion()));
            activeNode.none();
            activeNode = null;
            repaint();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (addNodeWidget.isVisible()) {
            int rotation = e.getWheelRotation();
            boolean up = rotation < 0;
            for (int i = 0; i < Math.abs(rotation); i++) {
                if (up) {
                    addNodeWidget.up();
                } else {
                    addNodeWidget.down();
                }
            }
            repaint();
        }
    }

    private void selectNode(NodeGui node) {
        if (selectedNode != null && selectedNode != node) {
            deselectNode(selectedNode);
        }
        node.select();
        selectedNode = node;
        for (GraphListener listener : graphListeners) {
            listener.selected(node);
        }
    }

    private void deselectNode(NodeGui node) {
        node.deselect();
        for (GraphListener listener : graphListeners) {
            listener.deselected(node);
        }
    }

    private void moveNode(NodeGui node, int x, int y) {
        node.setPosition(x, y);
        for (GraphListener listener : graphListeners) {
            listener.updated(node);
        }
    }

    public void addGraphListener(GraphListener listener) {
        graphListeners.add(listener);
    }

    public void removeGraphListener(GraphListener listener) {
        graphListeners.remove(listener);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String opName = e.getActionCommand();
        this.addNode(operatorManager.newNode(opName));
    }
}