package org.esa.snap.grapheditor.ui.components;

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

import org.esa.snap.grapheditor.ui.components.graph.NodeDragAction;
import org.esa.snap.grapheditor.ui.components.graph.NodeGui;
import org.esa.snap.grapheditor.ui.components.utils.AddNodeWidget;
import org.esa.snap.grapheditor.ui.components.utils.GraphKeyEventDispatcher;
import org.esa.snap.grapheditor.ui.components.utils.GraphListener;
import org.esa.snap.grapheditor.ui.components.utils.GridUtils;
import org.esa.snap.grapheditor.ui.components.utils.OperatorManager;

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
    private NodeDragAction dragAction = null;

    private OperatorManager operatorManager = new OperatorManager();

    private ArrayList<GraphListener> graphListeners = new ArrayList<>();

    private JPopupMenu addMenu;

    public GraphPanel() {
        super();
        this.setBackground(Color.lightGray);
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
        drawTooltip(g2);
        drawDrag(g2);
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

    private void drawDrag(Graphics2D g) {
        if (dragAction != null) {
            dragAction.draw(g);
        }
    }

    private void drawTooltip(Graphics2D g) {
        Graphics2D gNode = (Graphics2D) g.create();
        for (NodeGui node : nodes) {
            node.tooltip(gNode);
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
        if (dragAction != null) {
            dragAction.move(e.getPoint());
            if (dragAction.getType() == NodeDragAction.Type.DRAG) {
                for (GraphListener listener : graphListeners) {
                    listener.updated(dragAction.getSource());
                }
            }
            for (NodeGui node : nodes) {
                if (node != dragAction.getSource() && node.contains(e.getPoint())) {
                    node.over(e.getPoint());
                } else if (node != dragAction.getSource()) {
                    node.none();
                }
            }
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
                node.over(lastMousePosition);
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
        boolean somethingSelected = false;
        for (NodeGui node : nodes) {
            if (node.contains(lastMousePosition)) {
                selectNode(node);
                somethingSelected = true;
            } else {
                node.none();
            }
        }
        if (!somethingSelected) {
            deselectNode(selectedNode);
            selectedNode = null;
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
                    moveNode(node, e.getX() - 10, e.getY() - 10);

                    dragAction = node.drag(e.getPoint());
                    repaint();
                    return;
                }
            }
            Point p = e.getPoint();
            for (NodeGui node : nodes) {
                if (node.contains(p)) {
                    dragAction = node.drag(e.getPoint());
                    return;
                }
            }
        } else {
            dragAction = null;
        }
    }

    private void endDrag() {
        if (dragAction != null) {
            if (dragAction.getType() == NodeDragAction.Type.DRAG) {
                Point p = GridUtils.normalize(dragAction.getSource().getPostion());
                moveNode(dragAction.getSource(), p.x, p.y);
            }
            dragAction = null;
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        endDrag();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Nothing to do...
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Nothing to do...
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
        if (node == null)
            return;
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