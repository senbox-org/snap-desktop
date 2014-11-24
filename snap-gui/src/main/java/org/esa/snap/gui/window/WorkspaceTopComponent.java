/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.window;

import org.netbeans.swing.tabcontrol.DefaultTabDataModel;
import org.netbeans.swing.tabcontrol.TabData;
import org.netbeans.swing.tabcontrol.TabDisplayer;
import org.netbeans.swing.tabcontrol.TabbedContainer;
import org.netbeans.swing.tabcontrol.WinsysInfoForTabbedContainer;
import org.netbeans.swing.tabcontrol.event.TabActionEvent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * @author Norman
 */
@TopComponent.Description(
        preferredID = "WorkspaceTopComponent",
        persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(
        mode = "editor",
        openAtStartup = true)
@ActionID(category = "Window", id = "org.esa.snap.gui.window.WorkspaceTopComponent")
@ActionReference(path = "Menu/View/Tool Windows", position = 0)
@TopComponent.OpenActionRegistration(
        displayName = "Workspace Window",
        preferredID = "WorkspaceTopComponent"
)
public class WorkspaceTopComponent extends TopComponent {

    public final static String ID = WorkspaceTopComponent.class.getSimpleName();

    private final InstanceContent content = new InstanceContent();
    private final Map<TabData, JInternalFrame> tabToFrameMap;
    private final Map<JInternalFrame, TabData> frameToTabMap;
    private final Map<Object, Rectangle> idToBoundsMap;
    private final ActionListener tabActionListener;
    private final InternalFrameListener internalFrameListener;
    private final LookupListener internalFrameLookupListener;

    private TabbedContainer tabbedContainer;
    private JDesktopPane desktopPane;

    private int tabCount;

    public WorkspaceTopComponent() {
        associateLookup(new AbstractLookup(content));
        frameToTabMap = new HashMap<>();
        tabToFrameMap = new HashMap<>();
        idToBoundsMap = new HashMap<>();
        tabActionListener = new TabActionListener();
        internalFrameListener = new InternalFrameListenerImpl();
        internalFrameLookupListener = new InternalFrameLookupListener();
        initComponents();
        setName("Workspace");
        setToolTipText("Provides an internal desktop for document windows");
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.TRUE);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        DefaultTabDataModel tabDataModel = new DefaultTabDataModel();
        tabbedContainer = new TabbedContainer(tabDataModel,
                                              TabbedContainer.TYPE_EDITOR,
                                              WinsysInfoForTabbedContainer.getDefault(new MyWinsysInfoForTabbedContainer()));

        tabbedContainer.setVisible(false);

        desktopPane = new JDesktopPane();
        desktopPane.setBackground(new Color(16, 12, 123));

        add(tabbedContainer, BorderLayout.NORTH);
        add(desktopPane, BorderLayout.CENTER);
    }

    public static WorkspaceTopComponent getDefault() {
        TopComponent activatedTopComponent = WindowManager.getDefault().getRegistry().getActivated();
        if (activatedTopComponent instanceof WorkspaceTopComponent) {
            return (WorkspaceTopComponent) activatedTopComponent;
        }
        Set<TopComponent> opened = WindowManager.getDefault().getRegistry().getOpened();
        for (TopComponent topComponent : opened) {
            if (topComponent instanceof WorkspaceTopComponent) {
                return (WorkspaceTopComponent) topComponent;
            }
        }
        TopComponent topComponent = WindowManager.getDefault().findTopComponent(ID);
        if (topComponent instanceof WorkspaceTopComponent) {
            WorkspaceTopComponent workspaceTopComponent = (WorkspaceTopComponent) topComponent;
            workspaceTopComponent.open();
            return workspaceTopComponent;
        }
        WorkspaceTopComponent workspaceTopComponent = new WorkspaceTopComponent();
        workspaceTopComponent.open();
        return workspaceTopComponent;
    }

    public List<TopComponent> getTopComponents() {
        List<TabData> tabs = tabbedContainer.getModel().getTabs();
        List<TopComponent> topComponents = new ArrayList<>();
        for (TabData tab : tabs) {
            JInternalFrame internalFrame = tabToFrameMap.get(tab);
            topComponents.add(getTopComponent(internalFrame));
        }
        return topComponents;
    }

    public void addTopComponent(TopComponent topComponent) {

        if (topComponent.isOpened()) {
            topComponent.close();
        }

        int index = tabCount++;
        JInternalFrame internalFrame = new JInternalFrame(topComponent.getDisplayName(), true, true, true, true);

        // Note: The following dummyComponent with preferred size (-1, 2) allows for using the tabbedContainer as
        // a *thin*, empty tabbed bar on top of the desktopPane.
        JComponent dummyComponent = new JPanel();
        dummyComponent.setPreferredSize(new Dimension(-1, 2));
        TabData tabData = new TabData(dummyComponent, null, topComponent.getDisplayName(), "Tab + " + index);

        frameToTabMap.put(internalFrame, tabData);
        tabToFrameMap.put(tabData, internalFrame);

        internalFrame.setContentPane(topComponent);

        Object internalFrameID = getInternalFrameID(topComponent);
        Rectangle bounds = idToBoundsMap.get(internalFrameID);
        if (bounds == null) {
            bounds = new Rectangle(tabCount * 24, tabCount * 24, 400, 400);
        }
        internalFrame.setBounds(bounds);

        tabbedContainer.getModel().addTab(tabbedContainer.getModel().size(), tabData);
        tabbedContainer.setVisible(true);
        desktopPane.add(internalFrame);

        internalFrame.addInternalFrameListener(internalFrameListener);

        internalFrame.setVisible(true);
        try {
            internalFrame.setSelected(true);
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    private Object getInternalFrameID(TopComponent topComponent) {
        Object internalFrameID = topComponent.getClientProperty("internalFrameID");
        if (internalFrameID == null) {
            internalFrameID = "IF" + Long.toHexString(new Random().nextLong());
            topComponent.putClientProperty("internalFrameID", internalFrameID);
        }
        return internalFrameID;
    }

    // CHECKME: How does NB Platform use this method? What is its use?
    // See https://netbeans.org/bugzilla/show_bug.cgi?id=209051
    @Override
    public SubComponent[] getSubComponents() {
        Map<Object, JInternalFrame> internalFrameMap = new HashMap<>();
        SubComponent[] subComponents = new SubComponent[tabbedContainer.getTabCount()];
        ActionListener activator = actionEvent -> {
            JInternalFrame internalFrame = internalFrameMap.get(actionEvent.getSource());
            try {
                internalFrame.setSelected(true);
                internalFrame.requestFocus();
            } catch (PropertyVetoException e1) {
                // ok
            }
        };
        for (int i = 0; i < subComponents.length; i++) {
            TabData tab = tabbedContainer.getModel().getTab(i);
            JInternalFrame internalFrame = tabToFrameMap.get(tab);
            SubComponent subComponent = new SubComponent(internalFrame.getTitle(),
                                                         internalFrame.getToolTipText(),
                                                         activator,
                                                         internalFrame.isSelected(),
                                                         getTopComponent(internalFrame).getLookup(),
                                                         internalFrame.isShowing());
            internalFrameMap.put(subComponent, internalFrame);
            subComponents[i] = subComponent;
        }
        return subComponents;
    }

    @Override
    public void componentOpened() {
        tabbedContainer.addActionListener(tabActionListener);
    }

    @Override
    public void componentClosed() {
        tabbedContainer.removeActionListener(tabActionListener);
    }

    @Override
    protected void componentActivated() {
        JInternalFrame internalFrame;
        // Make sure that activation states of tabbedContainer and desktopPane are synchronized
        int tabIndex = tabbedContainer.getSelectionModel().getSelectedIndex();
        if (tabIndex >= 0) {
            TabData tab = tabbedContainer.getModel().getTab(tabIndex);
            internalFrame = tabToFrameMap.get(tab);
            if (!internalFrame.isSelected()) {
                try {
                    internalFrame.setSelected(true);
                } catch (PropertyVetoException e) {
                    // ok
                }
            }
        } else {
            internalFrame = desktopPane.getSelectedFrame();
            if (internalFrame != null) {
                TabData tab = frameToTabMap.get(internalFrame);
                tabIndex = tabbedContainer.getModel().indexOf(tab);
                if (tabIndex >= 0) {
                    tabbedContainer.getSelectionModel().setSelectedIndex(tabIndex);
                }
            }
        }
        if (internalFrame != null) {
            internalFrame.requestFocusInWindow();
        }
    }

    public interface WindowVisitor<T> {
        T visit(TopComponent topComponent);
    }

    public static <T> List<T> visitOpenWindows(WindowVisitor<T> visitor) {
        List<T> result = new ArrayList<>();
        T element;
        Set<TopComponent> topComponents = TopComponent.getRegistry().getOpened();
        for (TopComponent topComponent : topComponents) {
            element = visitor.visit(topComponent);
            if (element != null) {
                result.add(element);
            }
            if (topComponent instanceof WorkspaceTopComponent) {
                WorkspaceTopComponent workspaceTopComponent = (WorkspaceTopComponent) topComponent;
                List<TopComponent> containedWindows = workspaceTopComponent.getTopComponents();
                for (TopComponent containedWindow : containedWindows) {
                    element = visitor.visit(containedWindow);
                    if (element != null) {
                        result.add(element);
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("UnusedDeclaration")
    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        //  store your settings
    }

    @SuppressWarnings("UnusedDeclaration")
    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // read your settings according to their version
    }

    /**
     * Gets extra actions, e.g. for floating a docked window or group into a workspace.
     *
     * @param topComponent The document window.
     * @return The extra actions.
     */
    public static Action[] getExtraActions(TopComponent topComponent) {
        return new Action[]{
                new FloatIntoWorkspaceAction(topComponent),
                new FloatGroupIntoWorkspaceAction(topComponent)
        };
    }

    private TopComponent closeInternalFrame(JInternalFrame internalFrame) {
        return closeInternalFrame(internalFrame, true);
    }

    private TopComponent closeInternalFrame(JInternalFrame internalFrame, boolean removeTab) {
        internalFrame.removeInternalFrameListener(internalFrameListener);
        TopComponent topComponent = getTopComponent(internalFrame);

        Object internalFrameID = getInternalFrameID(topComponent);
        idToBoundsMap.put(internalFrameID, new Rectangle(internalFrame.getBounds()));

        TabData tabData = frameToTabMap.get(internalFrame);
        if (tabData != null) {
            if (removeTab) {
                int tabIndex = tabbedContainer.getModel().indexOf(tabData);
                if (tabIndex >= 0) {
                    tabbedContainer.getModel().removeTab(tabIndex);
                }
            }
            tabToFrameMap.remove(tabData);
        }
        frameToTabMap.remove(internalFrame);

        internalFrame.dispose();
        desktopPane.remove(internalFrame);
        if (desktopPane.getComponentCount() == 0) {
            tabbedContainer.setVisible(false);
        }

        // make sure the topComponent's parent is not the internalFrame which we just closed
        internalFrame.setContentPane(new TopComponent());

        return topComponent;
    }

    private TopComponent getTopComponent(JInternalFrame internalFrame) {
        return (TopComponent) internalFrame.getContentPane();
    }

    private TopComponent dockInternalFrame(JInternalFrame internalFrame) {
        TopComponent topComponent = closeInternalFrame(internalFrame, true);

        Mode mode = WindowManager.getDefault().findMode("editor");
        mode.dockInto(topComponent);
        if (!topComponent.isOpened()) {
            topComponent.open();
        }

        return topComponent;
    }

    private JInternalFrame getInternalFrame(int tabIndex) {
        return tabToFrameMap.get(tabbedContainer.getModel().getTab(tabIndex));
    }

    /**
     * Used to listen to actions invoked on the tabbedContainer
     */
    private class TabActionListener implements ActionListener {
        private final Map<String, Action> tabActions;

        private TabActionListener() {
            tabActions = new HashMap<>();
            initTabActions();
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            Action action = tabActions.get(actionEvent.getActionCommand());
            if (action != null) {
                action.actionPerformed(actionEvent);
            }
        }

        private void initTabActions() {
            addTabAction(new TabAction("close") {
                @Override
                public void tabActionPerformed(TabActionEvent actionEvent) {
                    // Note: the tab UI is already removed, but the tabData is still in the model. NetBeans will remove it later.
                    int tabIndex = actionEvent.getTabIndex();
                    JInternalFrame internalFrame = getInternalFrame(tabIndex);
                    if (internalFrame != null) {
                        closeInternalFrame(internalFrame, false);
                    }
                }
            });
            addTabAction(new TabAction("select") {
                @Override
                public void tabActionPerformed(TabActionEvent actionEvent) {
                    int tabIndex = actionEvent.getTabIndex();
                    JInternalFrame internalFrame = getInternalFrame(tabIndex);
                    if (internalFrame != null) {
                        try {
                            if (internalFrame.isIcon()) {
                                internalFrame.setIcon(false);
                            }
                            internalFrame.setSelected(true);
                        } catch (PropertyVetoException e) {
                            // ok
                        }
                    }
                }
            });
            addTabAction(new TabAction("maximize") {
                @Override
                public void tabActionPerformed(TabActionEvent actionEvent) {
                    int tabIndex = actionEvent.getTabIndex();
                    new MaximizeWindowAction(tabIndex).actionPerformed(actionEvent);
                }
            });
            addTabAction(new TabAction("popup") {
                @Override
                public void tabActionPerformed(TabActionEvent actionEvent) {

                    int tabCount = tabbedContainer.getTabCount();
                    if (tabCount == 0) {
                        return;
                    }

                    int tabIndex = actionEvent.getTabIndex();
                    //System.out.println("tabIndex = " + tabIndex);

                    JPopupMenu popupMenu = new JPopupMenu();
                    if (tabIndex >= 0) {
                        popupMenu.add(new CloseWindowAction(tabIndex));
                    }
                    if (tabCount > 1) {
                        popupMenu.add(new CloseAllWindowsAction());
                    }
                    if (tabIndex >= 0 && tabCount > 1) {
                        popupMenu.add(new CloseOtherWindowsAction(tabIndex));
                    }
                    if (tabIndex >= 0 || tabCount > 1) {
                        popupMenu.addSeparator();
                        if (tabIndex >= 0) {
                            popupMenu.add(new MaximizeWindowAction(tabIndex));
                            popupMenu.add(new DockInWorkspaceAction(tabIndex));
                        }
                        if (tabCount > 1) {
                            popupMenu.add(new DockAllInWorkspaceAction());
                        }
                    }
                    //if (tabIndex >= 0) {
                    //    popupMenu.addSeparator();
                    //    popupMenu.add(new CloneWindowAction(tabIndex));
                    //}
                    if (tabCount > 1) {
                        popupMenu.addSeparator();
                        popupMenu.add(new TileEvenlyAction());
                        popupMenu.add(new TileHorizontallyAction());
                        popupMenu.add(new TileVerticallyAction());
                    }
                    popupMenu.show(tabbedContainer, actionEvent.getMouseEvent().getX(), actionEvent.getMouseEvent().getY());
                }
            });
        }

        private void addTabAction(Action action) {
            tabActions.put((String) action.getValue(Action.ACTION_COMMAND_KEY), action);
        }

    }

    private abstract class TabAction extends AbstractAction {
        private TabAction(String name) {
            super(name);
            putValue(Action.ACTION_COMMAND_KEY, name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e instanceof TabActionEvent) {
                tabActionPerformed((TabActionEvent) e);
            }
        }

        public abstract void tabActionPerformed(TabActionEvent e);
    }

    /**
     * Allows telling the tabbedContainer if a tab component is maximized.
     */
    private class MyWinsysInfoForTabbedContainer extends WinsysInfoForTabbedContainer {
        @Override
        public Object getOrientation(Component comp) {
            return TabDisplayer.ORIENTATION_CENTER;
        }

        @Override
        public boolean inMaximizedMode(Component comp) {
            JInternalFrame internalFrame = desktopPane.getSelectedFrame();
            return internalFrame != null && internalFrame.isMaximum();
        }
    }

    private class CloseWindowAction extends AbstractAction {
        private final int tabIndex;

        public CloseWindowAction(int tabIndex) {
            super("Close");
            this.tabIndex = tabIndex;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TabData tab = tabbedContainer.getModel().getTab(tabIndex);
            JInternalFrame internalFrame = tabToFrameMap.get(tab);
            closeInternalFrame(internalFrame);
        }
    }

    private class CloseAllWindowsAction extends AbstractAction {

        public CloseAllWindowsAction() {
            super("Close All");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new CloseOtherWindowsAction(-1).actionPerformed(e);
        }
    }

    private class CloseOtherWindowsAction extends AbstractAction {
        private final int tabIndex;

        public CloseOtherWindowsAction(int tabIndex) {
            super("Close Others");
            this.tabIndex = tabIndex;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TabData tab = tabbedContainer.getModel().getTab(tabIndex);
            JInternalFrame selectedInternalFrame = tabToFrameMap.get(tab);
            Set<JInternalFrame> internalFrameSet = frameToTabMap.keySet();
            JInternalFrame[] internalFrames = internalFrameSet.toArray(new JInternalFrame[internalFrameSet.size()]);
            for (JInternalFrame internalFrame : internalFrames) {
                if (internalFrame != selectedInternalFrame) {
                    closeInternalFrame(internalFrame);
                }
            }
        }
    }

    private class MaximizeWindowAction extends AbstractAction {
        private final int tabIndex;

        public MaximizeWindowAction(int tabIndex) {
            super("Maximise");
            this.tabIndex = tabIndex;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TabData tab = tabbedContainer.getModel().getTab(tabIndex);
            JInternalFrame internalFrame = tabToFrameMap.get(tab);
            try {
                internalFrame.setMaximum(true);
            } catch (PropertyVetoException e1) {
                // ok
            }
        }
    }

    private class TileEvenlyAction extends AbstractAction {
        public TileEvenlyAction() {
            super("Tile Evenly");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int desktopWidth = desktopPane.getWidth();
            int desktopHeight = desktopPane.getHeight();
            int windowCount = frameToTabMap.size();

            double bestDeltaValue = Double.POSITIVE_INFINITY;
            int bestHorCount = -1;
            int bestVerCount = -1;
            for (int verCount = 1; verCount <= windowCount; verCount++) {
                for (int horCount = 1; horCount <= windowCount; horCount++) {
                    if (horCount * verCount >= windowCount && horCount * verCount <= 2 * windowCount) {
                        double deltaRatio = Math.abs(1.0 - verCount / (double) horCount);
                        double deltaCount = Math.abs(1.0 - (horCount * verCount) / ((double) windowCount));
                        double deltaValue = deltaRatio + deltaCount;
                        if (deltaValue < bestDeltaValue) {
                            bestDeltaValue = deltaValue;
                            bestHorCount = horCount;
                            bestVerCount = verCount;
                        }
                    }
                }
            }

            int windowWidth = desktopWidth / bestHorCount;
            int windowHeight = desktopHeight / bestVerCount;

            List<TabData> tabs = tabbedContainer.getModel().getTabs();
            int windowIndex = 0;
            for (int j = 0; j < bestVerCount; j++) {
                for (int i = 0; i < bestHorCount; i++) {
                    if (windowIndex < windowCount) {
                        TabData tabData = tabs.get(windowIndex);
                        JInternalFrame internalFrame = tabToFrameMap.get(tabData);
                        internalFrame.setBounds(i * windowWidth, j * windowHeight, windowWidth, windowHeight);
                    }
                    windowIndex++;
                }
            }
        }
    }

    private class TileHorizontallyAction extends AbstractAction {
        public TileHorizontallyAction() {
            super("Tile Horizontally");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int desktopWidth = desktopPane.getWidth();
            int desktopHeight = desktopPane.getHeight();
            int windowCount = frameToTabMap.size();
            int windowWidth = desktopWidth / windowCount;
            List<TabData> tabs = tabbedContainer.getModel().getTabs();
            for (int windowIndex = 0; windowIndex < windowCount; windowIndex++) {
                TabData tabData = tabs.get(windowIndex);
                JInternalFrame internalFrame = tabToFrameMap.get(tabData);
                internalFrame.setBounds(windowIndex * windowWidth, 0, windowWidth, desktopHeight);
            }
        }
    }

    private class TileVerticallyAction extends AbstractAction {
        public TileVerticallyAction() {
            super("Tile Vertically");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int desktopWidth = desktopPane.getWidth();
            int desktopHeight = desktopPane.getHeight();
            int windowCount = frameToTabMap.size();
            int windowHeight = desktopHeight / windowCount;
            List<TabData> tabs = tabbedContainer.getModel().getTabs();
            for (int windowIndex = 0; windowIndex < windowCount; windowIndex++) {
                TabData tabData = tabs.get(windowIndex);
                JInternalFrame internalFrame = tabToFrameMap.get(tabData);
                internalFrame.setBounds(0, windowIndex * windowHeight, desktopWidth, windowHeight);
            }
        }
    }

    private class DockAllInWorkspaceAction extends AbstractAction {
        public DockAllInWorkspaceAction() {
            super("Dock All");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Set<JInternalFrame> internalFrameSet = frameToTabMap.keySet();
            JInternalFrame[] internalFrames = internalFrameSet.toArray(new JInternalFrame[internalFrameSet.size()]);
            TopComponent topComponent = null;
            for (JInternalFrame internalFrame : internalFrames) {
                topComponent = dockInternalFrame(internalFrame);
            }
            if (topComponent != null) {
                topComponent.requestActive();
            }
        }
    }

    private class DockInWorkspaceAction extends AbstractAction {
        private int tabIndex;

        public DockInWorkspaceAction(int tabIndex) {
            super("Dock");
            this.tabIndex = tabIndex;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TabData tabData = tabbedContainer.getModel().getTab(tabIndex);
            JInternalFrame internalFrame = tabToFrameMap.get(tabData);
            TopComponent topComponent = dockInternalFrame(internalFrame);
            if (topComponent != null) {
                topComponent.requestActive();
            }
        }
    }

    public static class FloatIntoWorkspaceAction extends AbstractAction {
        private TopComponent window;

        public FloatIntoWorkspaceAction(TopComponent window) {
            super("Float into Workspace");
            this.window = window;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            WorkspaceTopComponent workspaceTopComponent = WorkspaceTopComponent.getDefault();
            workspaceTopComponent.addTopComponent(window);
        }
    }

    public static class FloatGroupIntoWorkspaceAction extends AbstractAction {
        private TopComponent window;

        public FloatGroupIntoWorkspaceAction(TopComponent window) {
            super("Float Group into Workspace");
            this.window = window;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            WorkspaceTopComponent workspaceTopComponent = WorkspaceTopComponent.getDefault();
            Mode mode = WindowManager.getDefault().findMode(window);
            if (mode != null) {
                TopComponent[] topComponents = WindowManager.getDefault().getOpenedTopComponents(mode);
                for (TopComponent topComponent : topComponents) {
                    if (!(topComponent instanceof WorkspaceTopComponent)) {
                        workspaceTopComponent.addTopComponent(topComponent);
                    }
                }
            }
        }
    }

    private class InternalFrameListenerImpl implements InternalFrameListener {
        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            System.out.println("internalFrameOpened: e = " + e);

            tabbedContainer.updateUI();

            DocumentWindow dw = getDocumentWindow(e);
            if (dw != null) {
                dw.componentOpened();
            }
        }

        @Override
        public void internalFrameClosing(InternalFrameEvent e) {
            // do nothing
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            System.out.println("internalFrameClosed: e = " + e);
            JInternalFrame internalFrame = e.getInternalFrame();
            if (frameToTabMap.containsKey(internalFrame)) {
                closeInternalFrame(internalFrame);
            }
            tabbedContainer.updateUI();

            DocumentWindow dw = getDocumentWindow(e);
            if (dw != null) {
                dw.componentClosed();
            }
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            System.out.println("internalFrameActivated: e = " + e);

            // Synchronise tab selection state, if not already done
            JInternalFrame internalFrame = e.getInternalFrame();
            TabData selectedTab = frameToTabMap.get(internalFrame);
            int selectedTabIndex = tabbedContainer.getSelectionModel().getSelectedIndex();
            List<TabData> tabs = tabbedContainer.getModel().getTabs();
            for (int i = 0; i < tabs.size(); i++) {
                TabData tab = tabs.get(i);
                if (tab == selectedTab && selectedTabIndex != i) {
                    tabbedContainer.getSelectionModel().setSelectedIndex(i);
                    break;
                }
            }
            tabbedContainer.updateUI();

            DocumentWindow dw = getDocumentWindow(e);
            if (dw != null) {
                dw.componentActivated();
            }

            // Publish lookup contents of selected frame to parent window
            TopComponent topComponent = getTopComponent(internalFrame);
            Lookup.Result<Object> objectResult = topComponent.getLookup().lookupResult(Object.class);
            WorkspaceTopComponent.this.content.set(objectResult.allInstances(), null);

            objectResult.addLookupListener(WeakListeners.create(LookupListener.class, internalFrameLookupListener, objectResult));
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            System.out.println("internalFrameDeactivated: e = " + e);
            tabbedContainer.updateUI();

            DocumentWindow dw = getDocumentWindow(e);
            if (dw != null) {
                dw.componentDeactivated();
            }
        }

        @Override
        public void internalFrameIconified(InternalFrameEvent e) {
            tabbedContainer.updateUI();

            DocumentWindow dw = getDocumentWindow(e);
            if (dw != null) {
                dw.componentHidden();
            }
        }

        @Override
        public void internalFrameDeiconified(InternalFrameEvent e) {
            tabbedContainer.updateUI();

            DocumentWindow dw = getDocumentWindow(e);
            if (dw != null) {
                dw.componentShowing();
            }
        }

        private DocumentWindow getDocumentWindow(InternalFrameEvent e) {
            TopComponent topComponent = getTopComponent(e.getInternalFrame());
            if (topComponent instanceof DocumentWindow) {
                return (DocumentWindow) topComponent;
            }
            return null;
        }

    }

    private class InternalFrameLookupListener implements LookupListener {
        @Override
        public void resultChanged(LookupEvent ev) {

        }
    }

    private class Frame implements LookupListener {
        final TabData tab;
        final JInternalFrame frame;
        final TopComponent topComponent;
        Lookup.Result<Object> lookupResult;

        private Frame(TabData tab, JInternalFrame frame, TopComponent topComponent) {
            this.tab = tab;
            this.frame = frame;
            this.topComponent = topComponent;
        }

        void observeLookup() {
            lookupResult = topComponent.getLookup().lookupResult(Object.class);
            lookupResult.addLookupListener(WeakListeners.create(LookupListener.class, this, lookupResult));
        }

        void releaseLookup() {
            lookupResult.removeLookupListener(this);
            lookupResult = null;
        }

        @Override
        public void resultChanged(LookupEvent ev) {

        }
    }
}