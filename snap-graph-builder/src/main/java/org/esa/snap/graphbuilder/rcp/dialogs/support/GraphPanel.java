/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.graphbuilder.rcp.dialogs.support;

import org.esa.snap.core.gpf.graph.NodeSource;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIRegistry;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Set;

/**
 * Draws and Edits the graph graphically
 * User: lveci
 * Date: Jan 15, 2008
 */
public class GraphPanel extends JPanel implements ActionListener, PopupMenuListener, MouseListener, MouseMotionListener {

    /**
     *
     */
    private static final long serialVersionUID = -8846674332592525519L;
    private final GraphExecuter graphEx;
    
    private static final int gridSpacing = 15;
    private BufferedImage gridBuffer = null;

    private JMenu addMenu;
    private Point lastMousePos = null;
    private final AddMenuListener addListener = new AddMenuListener(this);
    private final ConnectMenuListener connectListener = new ConnectMenuListener(this);
    private final RemoveSourceMenuListener removeSourceListener = new RemoveSourceMenuListener(this);

    private static final ImageIcon opIcon = new ImageIcon(GraphPanel.class.getClassLoader().
            getResource("org/esa/snap/graphbuilder/icons/operator.png"));
    private static final ImageIcon folderIcon = new ImageIcon(GraphPanel.class.getClassLoader().
            getResource("org/esa/snap/graphbuilder/icons/folder.png"));

    private static final Font font = new Font("Ariel", Font.BOLD, 10);
    private static final char[] folderDelim = new char[]{'/'};//'\\'};

    private GraphNode selectedNode = null;

    private GraphNode activeNode = null;
    private final static int activeNodePadding = 4;

    private boolean showHeadHotSpot = false;
    private boolean showTailHotSpot = false;
    private boolean connectingSourceFromHead = false;
    private boolean connectingSourceFromTail = false;
    private Point connectingSourcePos = null;
    private GraphNode connectSourceTargetNode = null;
    private GraphNode disconnectTargetNode = null;
    private boolean showRightClickHelp = false;

    public GraphPanel(GraphExecuter graphExec) {

        graphEx = graphExec;

        createAddOpMenu();

        addMouseListener(this);
        addMouseMotionListener(this);
    }

    /**
     * Creates a menu containing the list of operators to the addMenu
     */
    private void createAddOpMenu() {
        addMenu = new JMenu("Add");
        final SwingWorker<Boolean, Object> menuThread = new MenuThread(addMenu, addListener, graphEx);
        menuThread.execute();
    }

    private static class MenuThread extends SwingWorker<Boolean, Object> {
        private final JMenu addMenu;
        private final AddMenuListener addListener;
        private final GraphExecuter graphEx;

        MenuThread(final JMenu addMenu, final AddMenuListener addListener, final GraphExecuter graphEx) {
            this.addMenu = addMenu;
            this.addListener = addListener;
            this.graphEx = graphEx;
        }

        @Override
        protected Boolean doInBackground() throws Exception {
            // get operator list from graph executor
            final Set<String> gpfOperatorSet = graphEx.GetOperatorList();
            final String[] gpfOperatorList = new String[gpfOperatorSet.size()];
            gpfOperatorSet.toArray(gpfOperatorList);
            Arrays.sort(gpfOperatorList);

            // add operators
            for (String anAlias : gpfOperatorList) {
                if (!graphEx.isOperatorInternal(anAlias) && OperatorUIRegistry.showInGraphBuilder(anAlias)) {
                    final String category = graphEx.getOperatorCategory(anAlias);
                    JMenu menu = addMenu;
                    if (!category.isEmpty()) {
                        final String[] categoryPath = StringUtils.split(category, folderDelim, true);
                        for (String folder : categoryPath) {
                            menu = getMenuFolder(folder, menu);
                        }
                    }

                    final JMenuItem item = new JMenuItem(anAlias, opIcon);
                    item.setHorizontalTextPosition(JMenuItem.RIGHT);
                    item.addActionListener(addListener);
                    menu.add(item);
                }
            }
            return true;
        }
    }

    private static JMenu getMenuFolder(final String folderName, final JMenu currentMenu) {
        int insertPnt = 0;
        for (int i = 0; i < currentMenu.getItemCount(); ++i) {
            JMenuItem item = currentMenu.getItem(i);
            if (item instanceof JMenu) {
                int comp = item.getText().compareToIgnoreCase(folderName);
                if (comp == 0) {
                    return (JMenu) item;
                } else if (comp < 0) {
                    insertPnt++;
                }
            }
        }

        final JMenu newMenu = new JMenu(folderName);
        newMenu.setIcon(folderIcon);
        currentMenu.insert(newMenu, insertPnt);
        return newMenu;
    }

    private void addOperatorAction(String name) {
        final GraphNode newGraphNode = graphEx.addOperator(name);
        newGraphNode.setPos(lastMousePos);
        newGraphNode.normalizePosition(gridSpacing);
        if (this.activeNode == null) {
            this.activeNode = newGraphNode;
        }
        repaint();
    }

    private void removeSourceAction(String id) {
        if (selectedNode != null) {
            final GraphNode source = graphEx.getGraphNodeList().findGraphNode(id);
            selectedNode.disconnectOperatorSources(source.getID());
            if (this.selectedNode == this.activeNode) {
                this.activeNode = null;
            }
            repaint();
        }
    }

    private void autoConnectGraph() {
        if (!graphEx.getGraphNodeList().isGraphComplete()) {
            graphEx.autoConnectGraph();
            repaint();
        }
    }

    /**
     * Handles menu item pressed events
     *
     * @param event the action event
     */
    public void actionPerformed(ActionEvent event) {

        final String name = event.getActionCommand();
        if (name.equals("Delete")) {
            graphEx.removeOperator(selectedNode);
            if (selectedNode == activeNode) {
                activeNode = null;
            }
            repaint();
        }
    }

    public void clearGraph(){
        activeNode = null;
        selectedNode = null;
    }

    private void checkPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {

            final JPopupMenu popup = new JPopupMenu();
            popup.add(addMenu);

            if (selectedNode != null) {
                final JMenuItem item = new JMenuItem("Delete");
                popup.add(item);
                item.setHorizontalTextPosition(JMenuItem.RIGHT);
                item.addActionListener(this);

                final NodeSource[] sources = selectedNode.getNode().getSources();
                if (sources.length > 0) {
                    final JMenu removeSourcedMenu = new JMenu("Remove Source");
                    for (NodeSource ns : sources) {
                        final JMenuItem nsItem = new JMenuItem(ns.getSourceNodeId());
                        removeSourcedMenu.add(nsItem);
                        nsItem.setHorizontalTextPosition(JMenuItem.RIGHT);
                        nsItem.addActionListener(removeSourceListener);
                    }
                    popup.add(removeSourcedMenu);
                }
            }

            if (!graphEx.getGraphNodeList().isGraphComplete()) {
                final JMenuItem connectItem = new JMenuItem("Connect Graph", null);
                connectItem.setHorizontalTextPosition(JMenuItem.RIGHT);
                connectItem.addActionListener(connectListener);
                popup.add(connectItem);
            }

            popup.setLabel("Justification");
            popup.setBorder(new BevelBorder(BevelBorder.RAISED));
            popup.addPopupMenuListener(this);
            popup.show(this, e.getX(), e.getY());
            showRightClickHelp = false;
        }
    }

    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
    }

    public void popupMenuCanceled(PopupMenuEvent e) {
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
        drawActiveNode(g2);
        drawGraph(g2, graphEx.getGraphNodes());
    }

    /**
     * Draw the background grid of the Graph
     * 
     * @param g the Graphics object
     */
    private void drawGrid(Graphics2D g) {
        // g.setColor(Color.darkGray);
        int width = getWidth();
        int height = getHeight();
        if (gridBuffer == null || gridBuffer.getWidth() != width || gridBuffer.getHeight() != height) {
            gridBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics g_buff = gridBuffer.getGraphics();
            // g.fillRect(0, 0, width, height);
            int n_cols = Math.round(width / gridSpacing);
            int n_rows = Math.round(height / gridSpacing);
            Color c1 = new Color(220, 220, 220);
            Color c2 = new Color(230, 230, 230);
            g_buff.setColor(Color.lightGray);
            for (int i = 0; i < n_cols; i++) {
                if (i % 5 == 0) {
                    g_buff.setColor(c1);
                } else {
                    g_buff.setColor(c2);
                }
                int x = i * gridSpacing;
                g_buff.drawLine(x, 0, x, height);
                if (i < n_rows) {
                    g_buff.drawLine(0, x, width, x);
                }
            }
            for (int i = n_cols; i < n_rows; i++) {
                int y = i * gridSpacing;
                if (i % 5 == 0) {
                    g_buff.setColor(c1);
                } else {
                    g_buff.setColor(c2);
                }
                g_buff.drawLine(0, y, width, y);
            }
        }
        g.drawImage(gridBuffer, 0, 0, null);
    }   

    private void drawActiveNode(Graphics2D g){
        if (this.activeNode != null) {
            g.setColor(this.activeNode.activeColor());
            Point p = this.activeNode.getPos();
            int x = p.x - activeNodePadding;
            int y = p.y - activeNodePadding;
            int width = this.activeNode.getWidth() + activeNodePadding * 2 + 1;
            int height = this.activeNode.getHeight() + activeNodePadding * 2 + 1;
            g.fillRoundRect(x, y, width, height, 10, 10);
        }
    }

    /**
     * Draw the graphical representation of the Graph
     *
     * @param g        the Graphics
     * @param nodeList the list of graphNodes
     */
    private void drawGraph(Graphics2D g, GraphNode[] nodeList) {

        if(nodeList.length == 0)
            return;

        g.setFont(font);
        if (showRightClickHelp) {
            drawHelp(g);
        }

        for (GraphNode n : nodeList) {
            n.drawNode(g);
        }

        // first pass sets the Size in drawNode according to string length
        for (GraphNode n : nodeList) {
            // connect source nodes
            final NodeSource[] nSources = n.getNode().getSources();
            for (int i = 0; i < nSources.length; i++){
                NodeSource nSource = nSources[i];
                final GraphNode srcNode = graphEx.getGraphNodeList().findGraphNode(nSource.getSourceNodeId());
                if (srcNode != null)
                    n.drawConnectionLine(g, srcNode, i);
            }
        }

        if (connectingSourceFromHead && connectSourceTargetNode != null) {
            final Point p1 = connectSourceTargetNode.getPos();
            final Point p2 = connectingSourcePos;
            if (p1 != null && p2 != null) {
                g.setColor(Color.gray);
                GraphNode.drawArrow(g, p1.x, p1.y + connectSourceTargetNode.getAvailableInputYOffset(), p2.x, p2.y);
            }
        } else if (connectingSourceFromTail && connectSourceTargetNode != null) {
            final Point p1 = connectSourceTargetNode.getPos();
            final Point p2 = connectingSourcePos;
            if (p1 != null && p2 != null) {
                g.setColor(Color.gray);
                GraphNode.drawArrow(g, p2.x, p2.y,  
                    p1.x + connectSourceTargetNode.getWidth(),
                    p1.y + connectSourceTargetNode.getHalfNodeHeight());
            }
        }
    }

    public void showRightClickHelp(boolean flag) {
        showRightClickHelp = flag;
    }

    private static void drawHelp(final Graphics g) {
        final int x = (int) (g.getClipBounds().getWidth() / 2);
        final int y = (int) (g.getClipBounds().getHeight() / 2);

        final FontMetrics metrics = g.getFontMetrics();
        final String name = "Right click here to add an operator";
        final Rectangle2D rect = metrics.getStringBounds(name, g);
        final int stringWidth = (int) rect.getWidth();

        g.setColor(Color.black);
        g.drawString(name, x - stringWidth / 2, y);
    }

    /**
     * Handle mouse pressed event
     *
     * @param e the mouse event
     */
    public void mousePressed(MouseEvent e) {
        checkPopup(e);

        if (showHeadHotSpot) {
            connectingSourceFromHead = true;
        } else if (showTailHotSpot) {
            connectingSourceFromTail = true;
        }

        lastMousePos = e.getPoint();
    }

    /**
     * Handle mouse clicked event
     *
     * @param e the mouse event
     */
    public void mouseClicked(MouseEvent e) {
        checkPopup(e);
        showRightClickHelp = false;

        if (e.getButton() == 1 && selectedNode != null) {
            this.activeNode = selectedNode;
            graphEx.setSelectedNode(this.activeNode);
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    /**
     * Handle mouse released event
     *
     * @param e the mouse event
     */
    public void mouseReleased(MouseEvent e) {
        checkPopup(e);

        if (connectingSourceFromHead) {
            final GraphNode n = findNode(e.getPoint());
            if (n != null && selectedNode != n) {
                connectSourceTargetNode.connectOperatorSource(n.getID());
            }
        } else if (connectingSourceFromTail) {
            final GraphNode n = findNode(e.getPoint());
            if (n != null && selectedNode != n && connectSourceTargetNode != null) {
                n.connectOperatorSource(connectSourceTargetNode.getID());
            }
        } else if (selectedNode != null) {
            selectedNode.normalizePosition(gridSpacing);
        }
        connectingSourceFromHead = false;
        connectingSourceFromTail = false;
        connectSourceTargetNode = null;

        if (graphEx.getGraphNodeList().isGraphComplete()) {
            graphEx.notifyConnection();
        }

        repaint();
    }

    /**
     * Handle mouse dragged event
     *
     * @param e the mouse event
     */
    public void mouseDragged(MouseEvent e) {

        if (selectedNode != null && !connectingSourceFromHead && !connectingSourceFromTail) {
            final Point p = new Point(e.getX() - (lastMousePos.x - selectedNode.getPos().x),
                    e.getY() - (lastMousePos.y - selectedNode.getPos().y));
            selectedNode.setPos(p);
            lastMousePos = e.getPoint();
            repaint();
        }
        if (connectingSourceFromHead || connectingSourceFromTail) {
            if (disconnectTargetNode != null) {
                connectSourceTargetNode = disconnectTargetNode;
                selectedNode.disconnect(disconnectTargetNode.getID());
                disconnectTargetNode = null;
                selectedNode.deselect();
                selectedNode = connectSourceTargetNode;
                selectedNode.select();
            }
            connectingSourcePos = e.getPoint();
            repaint();
        }
    }

    /**
     * Handle mouse moved event
     *
     * @param e the mouse event
     */
    public void mouseMoved(MouseEvent e) {

        final GraphNode n = findNode(e.getPoint());
        if (selectedNode != n) {
            showHeadHotSpot = false;
            showTailHotSpot = false;
            if (selectedNode != null) {
                selectedNode.deselect();
            }
            selectedNode = n;
            if (n != null) {
                n.select();
            }
            repaint();
        }
        if (selectedNode != null) {
            if (selectedNode.isMouseOverHead(e.getPoint())) {
                showHeadHotSpot = true;
                connectSourceTargetNode = selectedNode;
                repaint();
            } else if (selectedNode.isMouseOverTail(e.getPoint())) {
                showTailHotSpot = true;
                connectSourceTargetNode = selectedNode;
                repaint();
            } else if (selectedNode.isMouseOverConnectedHead(e.getPoint())) {
                showTailHotSpot = true;
                disconnectTargetNode = findNode(selectedNode.getConnectedHeadAt(e.getPoint()));
                repaint();
            } else if (showHeadHotSpot || showTailHotSpot) {
                showHeadHotSpot = false;
                showTailHotSpot = false;
                disconnectTargetNode = null;
                repaint();
            }
        }
    }

    private GraphNode findNode(Point p) {

        for (GraphNode n : graphEx.getGraphNodes()) {
            if (n.isMouseOver(p))
                return n;
        }
        return null;
    }

    public GraphNode getSelectedNode(){
        return this.selectedNode;
    }

    private GraphNode findNode(String s) {
        if (s == null) return null;
        for (GraphNode n : graphEx.getGraphNodes()) {
            if (n.getID().equals(s))
                return n;
        }
        return null;
    }

    public GraphNode getActiveNode() {
        return this.activeNode;
    }
    
    public void setActiveNode(GraphNode node) {
        this.activeNode = node;
    }

    static class AddMenuListener implements ActionListener {

        final GraphPanel graphPanel;

        AddMenuListener(GraphPanel panel) {
            graphPanel = panel;
        }

        public void actionPerformed(java.awt.event.ActionEvent event) {
            graphPanel.addOperatorAction(event.getActionCommand());
        }
    }

    static class ConnectMenuListener implements ActionListener {

        final GraphPanel graphPanel;

        ConnectMenuListener(GraphPanel panel) {
            graphPanel = panel;
        }

        public void actionPerformed(java.awt.event.ActionEvent event) {
            graphPanel.autoConnectGraph();
        }
    }

    static class RemoveSourceMenuListener implements ActionListener {

        final GraphPanel graphPanel;

        RemoveSourceMenuListener(GraphPanel panel) {
            graphPanel = panel;
        }

        public void actionPerformed(java.awt.event.ActionEvent event) {
            graphPanel.removeSourceAction(event.getActionCommand());
        }
    }
}
