package org.esa.snap.grapheditor.ui.components;

import java.awt.*;
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

import javax.swing.*;

import org.esa.snap.grapheditor.ui.components.utils.AddNodeListener;
import org.esa.snap.grapheditor.ui.components.utils.NodeDragAction;
import org.esa.snap.grapheditor.ui.components.graph.NodeGui;
import org.esa.snap.grapheditor.ui.components.utils.GraphKeyEventDispatcher;
import org.esa.snap.grapheditor.ui.components.utils.GraphListener;
import org.esa.snap.grapheditor.ui.components.utils.GraphicUtils;
import org.esa.snap.grapheditor.ui.components.utils.GraphManager;
import org.esa.snap.grapheditor.ui.components.utils.NodeListener;
import org.esa.snap.grapheditor.ui.components.utils.RefreshListener;
import org.esa.snap.grapheditor.ui.components.utils.SettingManager;

public class GraphPanel extends JPanel
        implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener, ActionListener, RefreshListener, NodeListener, AddNodeListener {

    /**
     * Generated UID
     */
    private static final long serialVersionUID = -8787328074424783352L;

    private BufferedImage gridPattern = null;

    private Point lastMousePosition = new Point(0, 0);

    private NodeGui selectedNode = null;
    private NodeDragAction dragAction = null;

    private GraphManager graphManager = GraphManager.getInstance();

    private ArrayList<GraphListener> graphListeners = new ArrayList<>();

    private JPopupMenu addMenu;

    public GraphPanel() {
        super();
        this.setFocusable(true);

        // set event listener for refreshing UI when needed
        GraphManager.getInstance().addEventListener(this);

        this.setBackground(Color.lightGray);
        this.addMenu = new JPopupMenu();
        addMenu.add(graphManager.createOperatorMenu(this));

        this.addMouseMotionListener(this);
        this.addMouseListener(this);
        this.addMouseWheelListener(this);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new GraphKeyEventDispatcher(this));

        for (NodeGui n: GraphManager.getInstance().getNodes()){
            n.addNodeListener(this);
        }
    }

    private void addNode(NodeGui node) {
        if (node != null) {
            node.setPosition(GraphicUtils.normalize(lastMousePosition));
            node.addNodeListener(this);
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
        drawConnections(g2);
        drawDrag(g2);
        drawTooltip(g2);
       // this.addNodeWidget.paint(g2);
    }

    private void drawGrid(Graphics2D g) {
        if (SettingManager.getInstance().isBgGridVisible()) {
            int width = getWidth();
            int height = getHeight();
            if (gridPattern == null || gridPattern.getWidth() != width || gridPattern.getHeight() != height) {
                // initalize gridPattern image buffer
                gridPattern = GraphicUtils.gridPattern(width, height);
            }
            // render gridPattern buffer image
            g.drawImage(gridPattern, 0, 0, null);
        }
    }

    private void drawNodes(Graphics2D g) {
        Graphics2D gNode = (Graphics2D) g.create();
        for (NodeGui node : graphManager.getNodes()) {
            node.paintNode(gNode);
        }
        gNode.dispose();
    }

    private void drawConnections(Graphics2D g) {
        Graphics2D gNode = (Graphics2D) g.create();
        for (NodeGui node : graphManager.getNodes()) {
            node.paintConnections(gNode);
        }
        gNode.dispose();
    }

    private void drawDrag(Graphics2D g) {
        if (dragAction != null) {
            Graphics2D gdrag = (Graphics2D)g.create();
            dragAction.draw(gdrag);
            gdrag.dispose();
        }
    }

    private void drawTooltip(Graphics2D g) {
        if (SettingManager.getInstance().isShowToolipEnabled()) {
            Graphics2D gNode = (Graphics2D) g.create();
            for (NodeGui node : graphManager.getNodes()) {
                node.tooltip(gNode);
            }
            gNode.dispose();
        }
    }

    @Override
    public void keyPressed(KeyEvent event) {
    }

    @Override
    public void keyReleased(KeyEvent event) {
        int key = event.getKeyCode();

        if (key == SettingManager.getInstance().getCommandPanelKey()) {
            AddNodeDialog addNodeDialog = new AddNodeDialog(this);
            addNodeDialog.addListener(this);

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

//        if (this.addNodeWidget.isVisible()) {
//            switch (key) {
//            case (10):
//                // return
//                this.addNode(this.addNodeWidget.enter());
//                break;
//            case (27):
//                // escape
//                this.addNodeWidget.hide();
//                break;
//            case (KeyEvent.VK_UP):
//                break;
//            case (KeyEvent.VK_DOWN):
//                break;
//            case (KeyEvent.VK_BACK_SPACE):
//                break;
//            default:
//                this.addNodeWidget.type(event.getKeyChar());
//                break;
//            }
//            this.repaint();
//        }
    }

    private void removeSelectedNode() {
        if (selectedNode != null) {
            selectedNode.delete();
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
            Rectangle r = dragAction.getBoundingBox();
            dragAction.move(e.getPoint());
            Rectangle u = dragAction.getBoundingBox();
            boolean repainted = false;
            if (dragAction.getType() == NodeDragAction.Type.DRAG) {
                for (GraphListener listener : graphListeners) {
                    listener.updated(dragAction.getSource());
                }
            }
            for (NodeGui node : graphManager.getNodes()) {
                if (node.contains(e.getPoint())) {
                    if (node.over(e.getPoint())) {
                        repainted = true;
                    }
                } else if (node.none())
                        repainted = true;
            }
            if (repainted)
                repaint();
            else
                repaint(GraphicUtils.union(r, u));
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        lastMousePosition = e.getPoint();
//        if (addNodeWidget.isVisible()) {
//            if (addNodeWidget.mouseMoved(lastMousePosition)) {
//                repaint();
//                return;
//            }
//        }
        for (NodeGui node : graphManager.getNodes()) {
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
        this.requestFocus();
        if (e.getButton() != MouseEvent.BUTTON1) {
            // addNodeWidget.hide();
            if (e.getButton() == MouseEvent.BUTTON3) {
                addMenu.show(this, e.getX(), e.getY());
            }
            return;
        }
        boolean somethingSelected = false;
        for (NodeGui node : graphManager.getNodes()) {
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
        this.requestFocus();
        if (e.getButton() == MouseEvent.BUTTON1) {
//                NodeGui node = addNodeWidget.click(e.getPoint());
//                if (node != null) {
//                    addNode(node);
//                    moveNode(node, e.getX() - 10, e.getY() - 10);
//
//                    dragAction = node.drag(e.getPoint());
//                    repaint();
//                    return;
//                }
//            }
            Point p = e.getPoint();
            for (NodeGui node : graphManager.getNodes()) {
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
            dragAction.drop();
            if (dragAction.getType() == NodeDragAction.Type.DRAG) {
                Point p = GraphicUtils.normalize(dragAction.getSource().getPostion());
                moveNode(dragAction.getSource(), p.x, p.y);
            } else {
                for (NodeGui node: graphManager.getNodes()) {
                    if (node != dragAction.getSource() && node.hasTooltip()) {
                        // means is over a connection point
                        dragAction.getConnection().connect(node);
                    }
                }
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
//        if (addNodeWidget.isVisible()) {
//            int rotation = e.getWheelRotation();
//            boolean up = rotation < 0;
//            for (int i = 0; i < Math.abs(rotation); i++) {
//                if (up) {
//                    addNodeWidget.up();
//                } else {
//                    addNodeWidget.down();
//                }
//            }
//            repaint();
//        }
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
        this.addNode(graphManager.newNode(opName));
    }

    @Override
    public void refresh() {
        this.repaint();
    }

    @Override
    public void outputChanged(NodeGui source) {
        // nothing to do, not really a change.
    }

    @Override
    public void sourceDeleted(NodeGui source) {
        // nothing to do, already notified.
    }

    @Override
    public void connectionAdded(NodeGui source) {
        for (GraphListener l: graphListeners) {
            l.updated(source);
        }
    }

    @Override
    public void newNodeAdded(NodeGui node) {
        node.setPosition(lastMousePosition);
        addNode(node);
        repaint();
    }

    @Override
    public void newNodeAddedAtCurrentPosition(NodeGui node) {
        addNode(node);
        repaint();
    }
}