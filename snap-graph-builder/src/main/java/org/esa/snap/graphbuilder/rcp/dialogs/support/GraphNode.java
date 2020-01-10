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

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.apache.commons.math3.util.FastMath;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.gpf.graph.NodeSource;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Represents a node of the graph for the GraphBuilder
 * Stores, saves and loads the display position for the node
 * User: lveci
 * Date: Jan 17, 2008
 */
public class GraphNode {
    public enum Status {
        UNKNWON,
        ERROR,
        VALIDATED,
    }

    private static final Color errorColor = new Color(255, 80, 80, 128);
    private static final Color validateColor =  new Color(0, 177, 255, 128);
    private static final Color unknownColor =  new Color(177, 177, 177, 128);
    private static final Color connectionColor = new Color(66, 66, 66, 255);

    private final Node node;
    private OperatorDescriptor descriptor = null;
    private final Map<String, Object> parameterMap = new HashMap<>(10);
    private OperatorUI operatorUI = null;

    private Status status = Status.UNKNWON;

    private Boolean selected = false;
    private int nodeWidth = 60;
    private int nodeHeight = 30;
    private int halfNodeHeight = nodeHeight / 2;
    private static final int hotSpotSize = 10;
    private static final int halfHotSpotSize = hotSpotSize / 2;
    private int hotSpotOffset = 10;

    private Point displayPosition = new Point(0, 0);

    private XppDom displayParameters;

    private Color currentFill = unknownColor;
    private Color currentActive = computeActive(unknownColor);
    private Color currentDraw = unknownColor.darker();

    private int minNInputs = 0;
    private int maxNInputs = 0;
    private boolean hasTarget = true;

    public GraphNode(final Node n) throws IllegalArgumentException {
        node = n;
        displayParameters = new XppDom("node");
        displayParameters.setAttribute("id", node.getId());
        initParameters();
    }

    public void setOperatorUI(final OperatorUI ui) {
        operatorUI = ui;
    }

    public OperatorUI getOperatorUI() {
        return operatorUI;
    }

    private void initParameters() throws IllegalArgumentException {

        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(node.getOperatorName());
        if (operatorSpi == null) return;

        descriptor = operatorSpi.getOperatorDescriptor();
        if (descriptor != null) {
            minNInputs = descriptor.getSourceProductDescriptors().length;
            maxNInputs = minNInputs;
            if (descriptor.getSourceProductsDescriptor() != null) {
                maxNInputs = -1;
            }
            hasTarget = descriptor.getTargetProductDescriptor() != null;
        }

        final ParameterDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory();
        final PropertyContainer valueContainer = PropertyContainer.createMapBacked(parameterMap,
                operatorSpi.getOperatorClass(), parameterDescriptorFactory);

        final DomElement config = node.getConfiguration();
        final int count = config.getChildCount();
        for (int i = 0; i < count; ++i) {
            final DomElement child = config.getChild(i);
            final String name = child.getName();
            final String value = child.getValue();
            if (name == null || value == null || value.startsWith("$")) {
                continue;
            }

            try {
                if (child.getChildCount() == 0) {
                    final Converter converter = getConverter(valueContainer, name);
                    if (converter == null) {
                        final String msg = "Graph parameter " + name + " not found for Operator " + operatorSpi.getOperatorAlias();
                        //throw new IllegalArgumentException(msg);
                        SystemUtils.LOG.warning(msg);
                    } else {
                        parameterMap.put(name, converter.parse(value));
                    }
                } else {
                    final DomConverter domConverter = getDomConverter(valueContainer, name);
                    if(domConverter != null) {
                        try {
                            final Object obj = domConverter.convertDomToValue(child, null);
                            parameterMap.put(name, obj);
                        } catch (Exception e) {
                            SystemUtils.LOG.warning(e.getMessage());
                        }
                    } else {
                        final Converter converter = getConverter(valueContainer, name);
                        final Object[] objArray = new Object[child.getChildCount()];
                        int c = 0;
                        for (DomElement ch : child.getChildren()) {
                            final String v = ch.getValue();

                            if (converter != null) {
                                objArray[c++] = converter.parse(v);
                            } else {
                                objArray[c++] = v;
                            }
                        }
                        parameterMap.put(name, objArray);
                    }
                }

            } catch (ConversionException e) {
                throw new IllegalArgumentException(name);
            }
        }
    }

    private static Converter getConverter(final PropertyContainer valueContainer, final String name) {
        final Property[] properties = valueContainer.getProperties();

        for (Property p : properties) {

            final PropertyDescriptor descriptor = p.getDescriptor();
            if (descriptor != null && (descriptor.getName().equals(name) ||
                    (descriptor.getAlias() != null && descriptor.getAlias().equals(name)))) {
                return descriptor.getConverter();
            }
        }
        return null;
    }

    private static DomConverter getDomConverter(final PropertyContainer valueContainer, final String name) {
        final Property[] properties = valueContainer.getProperties();

        for (Property p : properties) {

            final PropertyDescriptor descriptor = p.getDescriptor();
            if (descriptor != null && (descriptor.getName().equals(name) ||
                    (descriptor.getAlias() != null && descriptor.getAlias().equals(name)))) {
                return descriptor.getDomConverter();
            }
        }
        return null;
    }

    void setDisplayParameters(final XppDom presentationXML) {
        for (XppDom params : presentationXML.getChildren()) {
            final String id = params.getAttribute("id");
            if (id != null && id.equals(node.getId())) {
                displayParameters = params;
                final XppDom dpElem = displayParameters.getChild("displayPosition");
                if (dpElem != null) {
                    displayPosition.x = (int) Float.parseFloat(dpElem.getAttribute("x"));
                    displayPosition.y = (int) Float.parseFloat(dpElem.getAttribute("y"));
                }
                return;
            }
        }
    }

    public void updateParameters() throws GraphException {
        if (operatorUI != null) {
            final XppDomElement config = new XppDomElement("parameters");
            updateParameterMap(config);
            node.setConfiguration(config);
        }
    }

    void assignParameters(final XppDom presentationXML) throws GraphException {

        updateParameters();
        assignDisplayParameters(presentationXML);
    }

    private void assignDisplayParameters(final XppDom presentationXML) {
        XppDom nodeElem = null;
        for (XppDom elem : presentationXML.getChildren()) {
            final String id = elem.getAttribute("id");
            if (id != null && id.equals(node.getId())) {
                nodeElem = elem;
                break;
            }
        }
        if (nodeElem == null) {
            presentationXML.addChild(displayParameters);
        }

        XppDom dpElem = displayParameters.getChild("displayPosition");
        if (dpElem == null) {
            dpElem = new XppDom("displayPosition");
            displayParameters.addChild(dpElem);
        }

        dpElem.setAttribute("y", String.valueOf(displayPosition.getY()));
        dpElem.setAttribute("x", String.valueOf(displayPosition.getX()));
    }

    /**
     * Gets the display position of a node
     *
     * @return Point The position of the node
     */
    public Point getPos() {
        return displayPosition;
    }

    /**
     * Sets the display position of a node and writes it to the xml
     *
     * @param p The position of the node
     */
    public void setPos(Point p) {
        displayPosition = p;
    }

    public Node getNode() {
        return node;
    }

    public int getWidth() {
        return nodeWidth;
    }

    public int getHeight() {
        return nodeHeight;
    }

    static int getHotSpotSize() {
        return hotSpotSize;
    }

    int getHalfNodeHeight() {
        return halfNodeHeight;
    }

    private void setSize(final int width, final int height) {
        nodeWidth = width;
        nodeHeight = height;
        halfNodeHeight = nodeHeight / 2;
        hotSpotOffset = 10;
    }

    int getHotSpotOffset() {
        return hotSpotOffset;
    }

    /**
     * Gets the unique node identifier.
     *
     * @return the identifier
     */
    public String getID() {
        return node.getId();
    }

    /**
     * Gets the name of the operator.
     *
     * @return the name of the operator.
     */
    public String getOperatorName() {
        return node.getOperatorName();
    }

    public Map<String, Object> getParameterMap() {
        return parameterMap;
    }

    private boolean canConnect() {
        return maxNInputs < 0 || connectionNumber() < maxNInputs;
    }

    String getSourceName(final String sourceID) {

        for (int i = 0; i < node.getSources().length; ++i) {
            final NodeSource ns = node.getSource(i);
            if (ns.getSourceNodeId().equals(sourceID)) {
                return ns.getName();
            }
        }
        return null;
    }

    void setSourceName(final String sourceName, final String sourceID) {
        for (int i = 0; i < node.getSources().length; ++i) {
            final NodeSource ns = node.getSource(i);
            if (ns.getSourceNodeId().equals(sourceID)) {
                ns.setName(sourceName);
            }
        }
    }

    public void connectOperatorSource(final String id) {
        if(!canConnect()) {
            return;
        }

        // check if already a source for this node
        disconnectOperatorSources(id);

        //check if connected sources exists
        String cntStr = "";
        if(node.getSources().length > 0) {
            cntStr = "."+node.getSources().length;
        }

        final NodeSource ns = new NodeSource("sourceProduct"+cntStr, id);
        node.addSource(ns);
    }

    public void disconnectOperatorSources(final String id) {

        for (NodeSource ns : node.getSources()) {
            if (ns.getSourceNodeId().equals(id)) {
                node.removeSource(ns);
            }
        }
    }

    public void disconnectAllSources() {
        final NodeSource[] sources = node.getSources();
        for (NodeSource source : sources) {
            node.removeSource(source);
        }
    }

    boolean isNodeSource(final GraphNode source) {

        final NodeSource[] sources = node.getSources();
        for (NodeSource ns : sources) {
            if (ns.getSourceNodeId().equals(source.getID())) {
                return true;
            }
        }
        return false;
    }

    boolean hasSources() {
        return node.getSources().length > 0;
    }

    public UIValidation validateParameterMap() {
        if (operatorUI != null)
            return operatorUI.validateParameters();
        return new UIValidation(UIValidation.State.OK, "");
    }

    void setSourceProducts(final Product[] products) {
        if (operatorUI != null) {
            operatorUI.setSourceProducts(products);
        }
    }

    private void updateParameterMap(final XppDomElement parentElement) throws GraphException {
        //if(operatorUI.hasSourceProducts())
        operatorUI.updateParameters();
        operatorUI.convertToDOM(parentElement);
    }

    /**
     * Draw a GraphNode as a rectangle with a name
     *
     * @param g   The Java2D Graphics
     * @param col The color to draw
     */
    void drawNode(final Graphics2D g) {
        Color fill = currentFill;
        Color draw = currentDraw;

        if (selected) {
            fill = fill.brighter();
            draw = draw.brighter();
        }

        Stroke oldStroke = g.getStroke();
        Stroke bigStroke = new BasicStroke(2); 

        final int x = displayPosition.x;
        final int y = displayPosition.y;

        g.setFont(g.getFont().deriveFont(Font.BOLD, 11));
        final FontMetrics metrics = g.getFontMetrics();
        final String name = node.getId();
        final Rectangle2D rect = metrics.getStringBounds(name, g);
        final int stringWidth = (int) rect.getWidth();
        int width = FastMath.round((FastMath.max(stringWidth + 15, 50) + 10) / 15) * 15 ;
        int n = maxNInputs;
        if (n < 0) {
            n = minNInputs > connectionNumber() ? minNInputs + 1 : connectionNumber() + 1;
        } else if (n == 0) {
            n = 1;
        }
        setSize(width, 15 + n * 15);

        g.setColor(fill);
        g.fillRoundRect(x, y, nodeWidth, nodeHeight, 8, 8);
        g.setColor(draw);
        
        
        g.setStroke(bigStroke);
        g.drawRoundRect(x, y, nodeWidth, nodeHeight, 8, 8);
        g.setStroke(oldStroke);

        g.setColor(Color.darkGray);
        g.drawString(name, x + (nodeWidth - stringWidth) / 2, y + 15);

        g.setStroke(bigStroke);
        this.drawHeadHotspot(g, draw);
        this.drawTailHotspot(g, draw);
        
    }

    /**
     * Draws the hotspot where the user can join the node to a source node
     *
     * @param g   The Java2D Graphics
     * @param col The color to draw
     */
    void drawHeadHotspot(final Graphics g, final Color col) {
        if (!this.hasInput()) return;
        final Point p = displayPosition;
        int cnnNum = connectionNumber();
        int n = maxNInputs;
        if (n < 0) {
            n = minNInputs > cnnNum ? minNInputs + 1 : cnnNum + 1;
        }

        for (int i = 0; i < n; i++) {
            g.setColor(Color.white);
            int x = p.x - halfHotSpotSize;
            int y = p.y + hotSpotOffset + i * 15;

            g.fillOval(x, y, hotSpotSize, hotSpotSize);
            if (i > minNInputs && i == cnnNum) {
                g.setColor(Color.gray);
                g.fillOval(x + 3, y + 3, hotSpotSize - 6 , hotSpotSize - 6);
                g.drawOval(x + 3, y + 3, hotSpotSize - 6 , hotSpotSize - 6);
            }
            g.setColor(col);
            g.drawOval(x, y, hotSpotSize, hotSpotSize);          
        }
    }

    /**
     * Draws the hotspot where the user can join the node to a source node
     *
     * @param g   The Java2D Graphics
     * @param col The color to draw
     */
    void drawTailHotspot(final Graphics g, final Color col) {
        if (!this.hasOutput()) return;
        final Point p = displayPosition;

        final int x = p.x + nodeWidth;
        final int y = p.y + halfNodeHeight;
        g.setColor(Color.white);
        g.fillRect(x - halfHotSpotSize, y - halfHotSpotSize, 2 * halfHotSpotSize, 2 * halfHotSpotSize);
        g.setColor(col);
        g.drawRect(x - halfHotSpotSize, y - halfHotSpotSize, 2 * halfHotSpotSize, 2 * halfHotSpotSize);
    }

    /**
     * Draw a line between source and target nodes
     *
     * @param g   The Java2D Graphics
     * @param src the source GraphNode
     */
    void drawConnectionLine(final Graphics2D g, final GraphNode src, int index) {
        final Point nodePos = displayPosition;
        final Point srcPos = src.displayPosition;

        final int srcEndX = srcPos.x + src.getWidth();
        final int srcMidY = srcPos.y + src.getHalfNodeHeight();
        g.setColor(connectionColor);
        drawArrow(g, nodePos.x, nodePos.y + hotSpotOffset + index * 15 + halfHotSpotSize, srcEndX, srcMidY);        
    }

    /**
     * Draws an arrow head at the correct angle
     *
     * @param g     The Java2D Graphics
     * @param tailX position X on target node
     * @param tailY position Y on target node
     * @param headX position X on source node
     * @param headY position Y on source node
     */
    public static void drawArrow(final Graphics2D g, final int tailX, final int tailY, final int headX, final int headY) {
        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(2));
        g.drawLine(tailX, tailY, headX, headY);
        g.fillOval(tailX - halfHotSpotSize + 2, tailY - halfHotSpotSize + 2, hotSpotSize - 3, hotSpotSize - 3);
        g.fillRect(headX - halfHotSpotSize + 2, headY - halfHotSpotSize + 2, hotSpotSize - 3, hotSpotSize - 3);
        g.setStroke(oldStroke); 
    }

    public void select() {
        this.selected = true;
    }

    public void deselect() {
        this.selected = false;
    }
    
    public Boolean isSelected() {
        return this.selected;
    }

    public Status getStatus() {
        return this.status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
    /**
     * Normalize the position of the widget to the current grid
     * 
     * @param gridSpacing size of the grid
     */
    public void normalizePosition(int gridSpacing) {
        double x = FastMath.round(displayPosition.getX() / gridSpacing) * gridSpacing;
        double y = FastMath.round(displayPosition.getY() / gridSpacing) * gridSpacing;

        displayPosition.setLocation(x, y);
    }

    public int connectionNumber() {
        return this.node.getSources().length;
    }

    public boolean hasInput() {
        int n =  this.maxInputs();
        return n < 0 || n > 0;
    }

    public boolean hasOutput() {
       return hasTarget;
    }

    public int getAvailableInputYOffset() {
        if (maxNInputs < 0 || connectionNumber() < maxNInputs) {
            return hotSpotOffset + connectionNumber() * 15 + halfHotSpotSize;
        } 
        return 0;
    }

    public Boolean isMouseOver(Point p) {
        int x = p.x - getPos().x;
        int y = p.y - getPos().y;
        if (x >= 0 && x <= getWidth() && y >= 0 && y <= getHeight()) {
            return true;
        }
        
        if (hasOutput() && FastMath.abs(x - getWidth()) <= halfHotSpotSize + 1 && FastMath.abs(y - halfNodeHeight) <= halfHotSpotSize + 1) return true;  
        if (hasInput() && FastMath.abs(x) <= halfHotSpotSize + 1) {
            for (int i = 0; i < connectionNumber() + 1; i++){
                if (FastMath.abs(y - (i*15 + hotSpotOffset)) <= halfHotSpotSize + 1) return true;
            }
        }
        return false; 
    }
    
    public Boolean isMouseOverTail(Point p) {
        if (!hasOutput()) return false;
        int x = p.x - (getPos().x + getWidth());
        int y = p.y - (getPos().y + halfNodeHeight);
        return FastMath.abs(x) <= halfHotSpotSize + 1 && FastMath.abs(y) <= halfHotSpotSize + 1;
    }

    public Boolean isMouseOverHead(Point p) {
        if (!hasInput()) return false;
        int x = p.x - getPos().x;
        int y = p.y - getPos().y - (connectionNumber() * 15 + hotSpotOffset + halfHotSpotSize) ;
        if (FastMath.abs(x) <= halfHotSpotSize + 1 && FastMath.abs(y) <= halfHotSpotSize + 1) return true;
        return  false;
    }

    public  Boolean isMouseOverConnectedHead(Point p) {
        if (!hasInput()) return false;
        int x = p.x - getPos().x;
        int y = p.y - getPos().y;
        if (FastMath.abs(x) <= halfHotSpotSize + 1) {
            for (int i = 0; i < connectionNumber(); i++){
                if (FastMath.abs(y - (i * 15 + hotSpotOffset + halfHotSpotSize)) <= halfHotSpotSize + 1) return true;
            }
        }
        return false;
    }

    public String getConnectedHeadAt(Point p){
        int x = p.x - getPos().x;
        int y = p.y - getPos().y;
        if (FastMath.abs(x) <= halfHotSpotSize + 1) {
            for (int i = 0; i < connectionNumber(); i++){
                if (FastMath.abs(y - (i * 15 + hotSpotOffset + halfHotSpotSize)) <= halfHotSpotSize + 1) {
                    NodeSource source = this.node.getSource(i);
                    return source.getSourceNodeId();
                }
            }
        }
        return null;
    }

    public void disconnect(String id) {
        for (int i = 0; i < connectionNumber(); i++) {
            NodeSource source = this.node.getSource(i);
            if (source.getSourceNodeId().equals(id)) {
                this.node.removeSource(source);
                return;
            }
        }
    }

    public void valid() {
        this.status = Status.VALIDATED;
        updateColors();
    }

    public void error() {
        this.status = Status.ERROR;
        updateColors();
    }

    public void unknown() {
        this.status = Status.UNKNWON;
        updateColors();
    }
    private Color computeActive(Color fill){
        float k = 0.94f;
        int r = Math.round(fill.getRed() * k);
        int g = Math.round(fill.getGreen() * k);
        int b = Math.round(fill.getBlue() * k);
        int a = 128;

        int max = Math.max(Math.max(r, g), b);

        r = (max - r) * (r / max) + r;
        g = (max - g) * (g / max) + g;
        b = (max - b) * (b / max) + b;

        return new Color(Math.min(r, 255), Math.min(g, 255), Math.min(b, 255), a);
    }

    private void updateColors() {
        switch (status) {
            case ERROR:
                currentFill = errorColor;
                break; 
            case VALIDATED:
                currentFill = validateColor;
                break;
            case UNKNWON:
            default:
                currentFill = unknownColor;
        }
        currentDraw = currentFill.darker().darker();
        currentActive = computeActive(currentFill);
    }



    public Color activeColor() {
        return currentActive;
    }
    
    int minInputs() {
        return minNInputs;
    }

    int maxInputs() {
        return maxNInputs;
    }

   
}

