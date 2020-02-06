package org.esa.snap.grapheditor.ui.components.graph;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.descriptor.SourceProductDescriptor;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.gpf.internal.OperatorContext;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.grapheditor.gpf.ui.OperatorUI;
import org.esa.snap.grapheditor.gpf.ui.UIValidation;
import org.esa.snap.grapheditor.ui.components.utils.GraphicUtils;
import org.esa.snap.grapheditor.ui.components.utils.NodeDragAction;
import org.esa.snap.grapheditor.ui.components.utils.NodeListener;
import org.esa.snap.grapheditor.ui.components.utils.OperatorManager;
import org.esa.snap.grapheditor.ui.components.utils.OperatorManager.SimplifiedMetadata;
import org.esa.snap.ui.AppContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


public class NodeGui implements NodeListener {
    public enum ValidationStatus {
        UNCHECKED,
        VALIDATED,
        ERROR,
        WARNING,
    }


    public static final int STATUS_MASK_OVER = 1 << 1;
    public static final int STATUS_MASK_SELECTED = 1 << 2;

    private static final int MAX_LINE_LENGTH = 45;

    private static final Color errorColor = new Color(255, 80, 80, 128);
    private static final Color validateColor =  new Color(0, 177, 255, 128);
    private static final Color unknownColor =  new Color(233, 229, 225, 230); //Color
    // private static final Color connectionColor = new Color(66, 66, 66, 255);
    private static final Color activeColor = new Color(254, 223, 176, 180);

    private static final Color tooltipBackground = new Color(0, 0, 0, 180);
    private static final Color tooltipBorder = Color.white;
    private static final Color tooltipColor = Color.lightGray;

    static final private BasicStroke borderStroke = new BasicStroke(3);
    static final private BasicStroke tooltipStroke = new BasicStroke(1.5f);
    static final private BasicStroke textStroke = new BasicStroke(1);
    static final private BasicStroke activeStroke = new BasicStroke(6);

    static final private int connectionSize = 10;
    static final private int connectionHalfSize = connectionSize / 2;
    static final private int connectionOffset = 15;

    static final private Font textFont = new Font("Ariel", Font.BOLD, 11);

    static final private int minWidth = 60;

    static final public int CONNECTION_NONE = -202;
    static final public int CONNECTION_OUTPUT = -1;

    private int x;
    private int y;
    private int width = 90;
    private int height = 30;

    private int textW = -1;
    private int textH = -1;

    private String name;

    private int status = 0;
    private ValidationStatus validationStatus = ValidationStatus.UNCHECKED;

    private final SimplifiedMetadata metadata;
    private final OperatorUI operatorUI;
    private final Operator operator;
    private final OperatorContext context;

    private final Node node;
    private Map<String, Object> configuration;
    private int numInputs;

    private JComponent preferencePanel = null;
    private String[] tooltipText_ = null;
    private boolean tooltipVisible_ = false;
    private int tooltipIndex_ = CONNECTION_NONE;

    private ArrayList<NodeListener> nodeListeners = new ArrayList<>();
    private ArrayList<Connection> incomingConnections = new ArrayList<>();
    private ArrayList<Connection> outgoingConnections = new ArrayList<>();

    private boolean hasChanged = false;
    private Product output = null;
    private boolean recomputeOutputNeeded = true;

    public NodeGui (Node node, Map<String, Object> configuration, @NotNull SimplifiedMetadata metadata, OperatorUI operatorUI){
        this.x = 0;
        this.y = 0;
        this.metadata = metadata;
        this.operatorUI = operatorUI;

        this.node = node;
        this.name = this.node.getId();
        this.configuration = configuration;
        numInputs = metadata.getMinNumberOfInputs();
        height = Math.max(height, connectionOffset * (numInputs + 1));
        operator = OperatorManager.getInstance().getOperator(metadata);
        context = new OperatorContext(operator);

    }



    public void paintNode(@NotNull Graphics2D g) {
        g.setFont(textFont);

        if (textW <= 0) {
            FontMetrics fontMetrics = g.getFontMetrics();

            textH = fontMetrics.getHeight();
            textW = fontMetrics.stringWidth(name);

            width = Math.max(GraphicUtils.floor(textW + 30), minWidth);
        }

        if ((this.status & STATUS_MASK_SELECTED) > 0) {
            Graphics2D gactive = (Graphics2D)g.create();
            gactive.setColor(activeColor);
            gactive.setStroke(activeStroke);
            gactive.drawRoundRect(x-2, y-2, width + 4,  height + 4, 8, 8);
            gactive.dispose();
        }

        g.setColor(this.color());
        g.fillRoundRect(x, y, width, height, 8, 8);
        g.setStroke(borderStroke);
        g.setColor(this.borderColor());
        g.drawRoundRect(x, y, width, height, 8, 8);

        g.setStroke(textStroke);
        g.setColor(Color.darkGray);

        g.drawString(name, x + (width - textW) / 2 , y + (5 + textH));

        paintInputs(g);
        paintOutput(g);
    }

    public void paintConnections(Graphics2D g) {
        for (Connection c: incomingConnections) {
            if (c != null)
                c.draw(g);
        }
    }

    @NotNull
    static private ArrayList<String> split_line(@NotNull String line) {
        ArrayList<String> result = new ArrayList<>();

        if (line.length() <= MAX_LINE_LENGTH) {
            result.add(line);
        } else {
            int start = 0;
            int N = (int)Math.ceil((double)line.length() / MAX_LINE_LENGTH);
            for (int i = 0; i < N; i++) {
                int end = Math.min(start + MAX_LINE_LENGTH, line.length());
                String subline = line.substring(start, end);
                if (end < line.length()
                        && Character.isLetter(line.charAt(end-1))
                        && Character.isLetter(line.charAt(end))) {
                    subline += "-";
                }
                result.add(subline);
                start = end;
            }
        }
        return  result;
    }

    @Contract("null -> null")
    static private String[] split_text(String input) {
        if (input == null) return null;
        ArrayList<String> result = new ArrayList<>();
        for (String line: input.split("\n")) {
            result.addAll(split_line(line));
        }
        return result.toArray(new String[0]);
    }

    public void tooltip(Graphics2D g) {
        if (tooltipVisible_ && tooltipText_ != null) {
            FontMetrics fontMetrics = g.getFontMetrics();

            int textH = fontMetrics.getHeight();
            int tooltipH = textH + (textH+4) * (tooltipText_.length - 1) + 8;

            int tooltipW = fontMetrics.stringWidth(tooltipText_[0]) + 8;
            for (int i = 1; i < tooltipText_.length; i++) {
                tooltipW = Math.max(tooltipW, fontMetrics.stringWidth(tooltipText_[i]) + 8);
            }

            int tx;
            int ty;
            if (tooltipIndex_ == CONNECTION_OUTPUT) {
                tx = x + width + connectionSize;
                ty = y + connectionOffset - (tooltipH / 2);
            } else {
                tx = x - tooltipW - connectionSize;
                ty = y + (tooltipIndex_ + 1) * connectionOffset - (tooltipH / 2);
            }
            g.setColor(tooltipBackground);
            g.fillRoundRect(tx, ty, tooltipW, tooltipH, 8, 8);
            g.setStroke(tooltipStroke);
            g.setColor(tooltipBorder);
            g.drawRoundRect(tx, ty, tooltipW, tooltipH, 8, 8);
            g.setStroke(textStroke);
            g.setColor(tooltipColor);
            int stringY = ty + 8 + (textH / 2);
            for (String line: tooltipText_){
                g.drawString(line, tx + 4, stringY);
                stringY += textH + 4;
            }
        }
    }

    private void paintInputs(Graphics2D g) {
        if (metadata.hasInputs()) {
            int xc = x - connectionHalfSize;
            int yc = y + connectionOffset - connectionHalfSize;
            for (int i = 0; i < numInputs(); i++) {
                g.setColor(Color.white);
                g.fillOval(xc, yc, connectionSize, connectionSize);
                g.setStroke(borderStroke);
                g.setColor(borderColor());
                g.drawOval(xc, yc, connectionSize, connectionSize);
                if (i >= metadata.getMinNumberOfInputs()) {
                    g.setColor(Color.lightGray);
                    g.setStroke(textStroke);
                    g.fillOval(xc + 3, yc + 3, connectionSize - 6, connectionSize - 6);
                    g.drawOval(xc + 3, yc + 3, connectionSize - 6, connectionSize - 6);
                }
                yc += connectionOffset;
            }
        }
    }

    @Contract(pure = true)
    private int numInputs() {
        return numInputs;
    }

    private void paintOutput(Graphics2D g) {
        if (metadata.hasOutput()) {
            int xc = x + width - connectionHalfSize;
            int yc = y + connectionOffset - connectionHalfSize;
            g.setColor(Color.white);
            g.fillRect(xc, yc, connectionSize, connectionSize);
            g.setStroke(borderStroke);
            g.setColor(borderColor());
            g.drawRect(xc, yc, connectionSize, connectionSize);
        }
    }

    private Color color() {

        Color c;
        switch (validationStatus) {
            case ERROR:
                c = errorColor;
                break;
            case VALIDATED:
                c = validateColor;
                break;
            case WARNING:
            default:
                c = unknownColor;
                break;
        }
        if ((this.status & STATUS_MASK_OVER) > 0) {
            return c.brighter();
        }
        return c;
    }

    private Color borderColor() {
        return color().darker().darker();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setPosition(int x, int y){
        this.x = x;
        this.y = y;
    }

    public void setPosition(@NotNull Point p) {
        this.x = p.x;
        this.y = p.y;
    }

    public Point getPostion() {
        return new Point(x, y);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getName() {
        return name;
    }

    private int getInputIndex(@NotNull Point p) {
        int dx = p.x - x;
        int dy = p.y - y;
        if (Math.abs(dx) <= connectionHalfSize && dy > 0) {
            int iy = Math.round((float) dy / connectionOffset);
            if (iy - 1 < numInputs()) {
                int cy = iy * connectionOffset;
                if (Math.abs(dy - cy) <= connectionHalfSize) {
                    return iy - 1;
                }
            }
        }
        return -1;
    }

    private boolean isOverOutput(@NotNull Point p) {
        int dx = p.x - x;
        int dy = p.y - y;
        return (metadata.hasOutput()
                && Math.abs(dx - width) <= connectionHalfSize
                && Math.abs(dy - connectionOffset) <= connectionHalfSize);
    }

    public boolean contains(@NotNull Point p) {
        int dx = p.x - x;
        int dy = p.y - y;
        boolean inside = (dx >= 0 && dy >= 0 && dx <= width && dy <= height);
        if (inside)
            return true;
        // check if is over a connection input
        if (getInputIndex(p) >= 0) return true;
        // check if is over a connection output
        return isOverOutput(p);
    }

    public boolean over(Point p) {
        boolean changed = false;
        if ((status & STATUS_MASK_OVER) == 0) {
            status += STATUS_MASK_OVER;
            changed = true;
        }
        int iy = getConnectionAt(p);
        if (iy != CONNECTION_NONE ) {
            show_tooltip(iy);
            return iy != tooltipIndex_;
        }
        hide_tooltip();
        return changed;
    }

    public boolean none() {
        hide_tooltip();
        if ((status & STATUS_MASK_OVER) > 0) {
            status -= STATUS_MASK_OVER;
            return true;
        }
        return false;
    }

    public void select() {
        if ((status & STATUS_MASK_SELECTED) == 0)
            status += STATUS_MASK_SELECTED;
        System.out.println("\033[1;32m>> SELECTED\033[0m");
        if (hasChanged) {
            // TODO UPDATE inputs...
            Product products[] = new Product[incomingConnections.size()];
            for (int i = 0; i < products.length; i++) {
                products[i] = incomingConnections.get(i).getSourceProduct();
            }
            operatorUI.setSourceProducts(products);
            operatorUI.updateParameters();
            hasChanged = false;
            recomputeOutputNeeded = true;
        }
    }
    private boolean incomplete() {
        output = null;
        validationStatus = ValidationStatus.WARNING;
        return true;
    }

    private boolean recomputeOutput() {
        recomputeOutputNeeded = false;
        UIValidation.State state = operatorUI.validateParameters().getState();

        if (state == UIValidation.State.OK) {
            SourceProductDescriptor descriptors[] = metadata.getDescriptor().getSourceProductDescriptors();
            if (incomingConnections.size() < descriptors.length) {
                return incomplete();
            }
            for (int i = 0; i < descriptors.length; i++ ){
                SourceProductDescriptor descr = metadata.getDescriptor().getSourceProductDescriptors()[i];
                Product p = incomingConnections.get(i).getSourceProduct();
                if (p == null)
                    return incomplete();
                System.out.println(descr.getName());
                context.setSourceProduct(descr.getName(), p);
            }
            if (incomingConnections.size() > descriptors.length && metadata.getMaxNumberOfInputs() < 0) {
                Product products[] = new Product[incomingConnections.size() - descriptors.length];
                for (int i = descriptors.length; i < incomingConnections.size(); i++) {
                    Product p = incomingConnections.get(i).getSourceProduct();
                    if (p == null)
                        return incomplete();
                    products[i - descriptors.length] = p;
                }
                context.setSourceProducts(products);
            }

            System.out.println(context.getSourceProducts());

            for (String param: configuration.keySet()) {
                System.out.println(param);
                context.setParameter(param, configuration.get(param));
            }
            try {
                output = context.getTargetProduct();
                validationStatus = ValidationStatus.VALIDATED;
            } catch (Exception e) {
                e.printStackTrace();
                output = null;
                validationStatus = ValidationStatus.ERROR;
            }
        } else {
            output = null;
            if (state == UIValidation.State.ERROR) {
                validationStatus = ValidationStatus.ERROR;
            } else if (state == UIValidation.State.WARNING) {
                validationStatus = ValidationStatus.WARNING;
            }
        }
        return true;
    }

    public Product getProduct() {
        return output;
    }

    public void deselect() {
        if ((status & STATUS_MASK_SELECTED) > 0)
            status -= STATUS_MASK_SELECTED;
        if (recomputeOutputNeeded || check_changes()) {
            System.out.println("\033[1;32m>> SOMETHING CHANGED\033[0m");
            operatorUI.updateParameters();
            if (recomputeOutput()) {
                for (NodeListener l : nodeListeners) {
                    l.outputChanged(this);
                }
            }
        }
    }

    private boolean equals(Map<String, Object> a,Map<String, Object> b){
        Set<String> aset = a.keySet();
        Set<String> bset = b.keySet();
        if (aset.size() == bset.size() && bset.containsAll(aset)) {
            for (String key: aset) {
                System.out.println(key);
                Object aobj = a.get(key);
                Object bobj = b.get(key);
                if ((aobj == null && bobj != null) || (aobj != null && !aobj.equals(bobj))){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean check_changes() {
        operatorUI.updateParameters();
        Map<String, Object> update = operatorUI.getParameters();
        boolean res = equals(update, this.configuration);
        this.configuration = new HashMap<>(update);
        return !res;
    }

    public void disconnect(int index) {
        for (int i = index + 1; i < this.incomingConnections.size(); i++) {
            this.incomingConnections.get(i).setTargetIndex(i - 1);
        }

        numInputs = Math.max(metadata.getMinNumberOfInputs(), numInputs - 1);
        height = (numInputs + 1) * connectionOffset;
        Connection c = this.incomingConnections.get(index);
        c.getSource().removeNodeListener(c);
        this.incomingConnections.remove(c);
        hasChanged = true;
    }

    public NodeDragAction drag(Point p) {
        int iy = getInputIndex(p);
        if (iy >= 0) {
            if (this.incomingConnections.size() > iy) {
                Connection c = this.incomingConnections.get(iy);
                if (c != null) {
                    disconnect(iy);
                    c.showSourceTooltip();
                    return new NodeDragAction(new Connection(c.getSource(), p));
                }
            }
            tooltipVisible_ = false;
            return new NodeDragAction(new Connection(this, iy, p));
        }
        if (isOverOutput(p)) {
            tooltipVisible_ = false;
            return new NodeDragAction(new Connection(this, p));
        }
        tooltipVisible_ = false;
        return new NodeDragAction(this,  p);
    }

    private void hide_tooltip() {
        tooltipVisible_ = false;
        tooltipText_ = null;
        tooltipIndex_ = CONNECTION_NONE;
    }

    private void show_tooltip(int connectionIndex) {
        if (connectionIndex == CONNECTION_OUTPUT && metadata.hasOutput()) {
            // OUTPUT
            tooltipVisible_ = true;
            tooltipText_ = split_text(metadata.getOutputDescription());
            tooltipIndex_ = connectionIndex;
        } else if (connectionIndex >= 0 && metadata.hasInputs()) {
            // INPUT
            tooltipVisible_ = true;
            tooltipText_ = split_text(metadata.getInputDescription(connectionIndex));
            tooltipIndex_ = connectionIndex;
        } else {
            hide_tooltip();
        }
    }

    public JComponent getPreferencePanel(AppContext context){
        if (preferencePanel == null) {
            try {
                preferencePanel = operatorUI.CreateOpTab(this.metadata.getName(), configuration ,context);
            } catch (Exception e) {
                SystemUtils.LOG.info(e.getMessage());
                preferencePanel = null;
                return null;
            }
        }
        return preferencePanel;
    }

    public Point getInputPosition(int index) {
        return new Point(x, y + connectionOffset * (index + 1));
    }

    public Point getOutputPosition() {
        return new Point(x + width, y + connectionOffset);
    }

    public boolean hasTooltip() {
        return tooltipVisible_;
    }

    public int getConnectionAt(Point p) {
        int iy = getInputIndex(p);
        if (iy >= 0) {
            return iy;
        }
        if (isOverOutput(p)) {
            return CONNECTION_OUTPUT;
        }
        return CONNECTION_NONE;
    }

    public boolean isConnectionAvailable(int index, NodeGui other) {
        if (index == CONNECTION_OUTPUT)
            return true;
        if (index == CONNECTION_NONE)
            return false;
        for (Connection c: incomingConnections) {
            if (c != null && other == c.getSource()) {
                return false;
            }
        }
        return (incomingConnections.size() == index && (metadata.getMaxNumberOfInputs() <0 || index < metadata.getMaxNumberOfInputs()));
    }

    private void connect(Connection c){
        incomingConnections.add(c);
        c.getSource().addNodeListener(c);
        hasChanged = true;
    }

    public void addConnection(Connection connection, int index) {
        if (index == incomingConnections.size())  {
            connect(connection);
        } else {
            return;
        }

        if (metadata.getMaxNumberOfInputs() == -1) {
            if (incomingConnections.size() < metadata.getMinNumberOfInputs()) {
                numInputs = metadata.getMinNumberOfInputs();
            } else if (metadata.getMaxNumberOfInputs() > 0 ) {
                numInputs = Math.min(metadata.getMaxNumberOfInputs(), numInputs + 1);
            }else {
                numInputs = incomingConnections.size() + 1;
            }

            height = (numInputs + 1) * connectionOffset;
        }
    }

    @Override
    public void outputChanged(NodeGui source) {
        hasChanged = true;
    }

    public void addNodeListener(Connection c) {
        nodeListeners.add(c.getTarget());
        outgoingConnections.add(c);
    }

    public void removeNodeListener(Connection c) {
        nodeListeners.remove(c.getTarget());
        outgoingConnections.remove(c);
    }

    public Rectangle getBoundingBox(){
        Rectangle r;
        if (tooltipVisible_) {
            int tx = tooltipIndex_ == CONNECTION_OUTPUT ? x - 8 : x - 8 - 80;
            int ty = y - 8;
            int w = width + 16 + 80;
            int h = height + 16;
            r = new Rectangle(x, y, w, h);
        } else {
            r = new Rectangle(x - 8, y - 8, width + 16, height + 16);
        }
        for (Connection c: incomingConnections) {
            r = GraphicUtils.union(r, c.getBoundingBox());
        }
        for (Connection c: outgoingConnections) {
            r = GraphicUtils.union(r, c.getBoundingBox());
        }
        return r;
    }
}