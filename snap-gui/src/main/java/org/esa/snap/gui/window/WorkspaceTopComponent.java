/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.window;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.netbeans.swing.tabcontrol.DefaultTabDataModel;
import org.netbeans.swing.tabcontrol.TabData;
import org.netbeans.swing.tabcontrol.TabDisplayer;
import org.netbeans.swing.tabcontrol.TabbedContainer;
import org.netbeans.swing.tabcontrol.WinsysInfoForTabbedContainer;
import org.netbeans.swing.tabcontrol.event.TabActionEvent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static WorkspaceTopComponent instance;

    private TabbedContainer tabbedContainer;
    private JDesktopPane desktopPane;

    private final Map<JInternalFrame, Editor> frameToEditorMap;
    private final Map<TabData, JInternalFrame> tabToFrameMap;
    private final Map<JInternalFrame, TabData> frameToTabMap;
    private final ActionListener tabActionListener;
    private final InternalFrameListener internalFrameListener;
    private int tabCount;

    public WorkspaceTopComponent() {
        instance = this;
        frameToEditorMap = new HashMap<>();
        frameToTabMap = new HashMap<>();
        tabToFrameMap = new HashMap<>();
        tabActionListener = new TabActionListener();
        internalFrameListener = new InternalFrameListenerImpl();
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

    public static WorkspaceTopComponent getInstance() {
        return instance;
    }

    public String getUniqueEditorTitle(String titleBase) {
        return UIUtils.getUniqueFrameTitle(desktopPane.getAllFrames(), titleBase);
    }

    public List<Editor<ProductSceneView>> findImageEditors(RasterDataNode band) {
        ArrayList<Editor<ProductSceneView>> list = new ArrayList<>();
        Collection<Editor> editors = frameToEditorMap.values();
        for (Editor editor : editors) {
            Container component = editor.getComponent();
            if (component instanceof ProductSceneView) {
                ProductSceneView view = (ProductSceneView) component;
                if (view.getRaster() == band) {
                    //noinspection unchecked
                    list.add(editor);
                }
            }
        }
        return list;
    }

    public <T extends Container> Editor<T> addEditorComponent(String title, T component) {
        int index = tabCount++;
        JInternalFrame internalFrame = new JInternalFrame(title, true, true, true, true);

        // Note: The following dummyComponent with preferred size (-1, 2) allows for using the tabbedContainer as
        // a *thin*, empty tabbed bar on top of the desktopPane.
        JComponent dummyComponent = new JPanel();
        dummyComponent.setPreferredSize(new Dimension(-1, 2));
        TabData tabData = new TabData(dummyComponent, null, title, "Tab + " + index);

        Editor editor = new Editor(internalFrame);
        frameToEditorMap.put(internalFrame, editor);
        frameToTabMap.put(internalFrame, tabData);
        tabToFrameMap.put(tabData, internalFrame);

        internalFrame.setContentPane(component);
        internalFrame.setBounds(new Rectangle(tabCount * 24, tabCount * 24, 400, 400));

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

        //noinspection unchecked
        return editor;
    }


    // CHECKME: How does NB Platform use this method? What is its use?
    @Override
    public SubComponent[] getSubComponents() {
        ActionListener activator = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println(">> subComponent activated: e = " + e);
            }
        };
        SubComponent[] subComponents = new SubComponent[tabbedContainer.getTabCount()];
        for (int i = 0; i < subComponents.length; i++) {
            TabData tab = tabbedContainer.getModel().getTab(i);
            JInternalFrame internalFrame = tabToFrameMap.get(tab);
            subComponents[i] = new SubComponent(internalFrame.getTitle(),
                                                internalFrame.getToolTipText(),
                                                activator,
                                                internalFrame.isSelected());
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
     * Gets extra actions for moving a given document window to a workspace.
     *
     * @param topComponent The document window.
     * @return The extra actions.
     */
    public Action[] getExtraWorkspaceActions(TopComponent topComponent) {
        return new Action[]{
                new FloatIntoWorkspaceAction(topComponent),
                new FloatGroupIntoWorkspaceAction(topComponent)
        };
    }

    private Container closeInternalFrame(JInternalFrame internalFrame) {
        return closeInternalFrame(internalFrame, true);
    }

    private Container closeInternalFrame(JInternalFrame internalFrame, boolean removeTab) {
        internalFrame.removeInternalFrameListener(internalFrameListener);
        Container contentPane = internalFrame.getContentPane();
        internalFrame.setContentPane(new JPanel());
        frameToEditorMap.remove(internalFrame);
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

        return contentPane;
    }

    private TopComponent moveInternalFrameToEditorMode(JInternalFrame internalFrame) {
        Container container = closeInternalFrame(internalFrame, true);
        TopComponent topComponent = null;
        if (container instanceof TopComponent) {
            topComponent = (TopComponent) container;
        } else if (container instanceof ProductSceneView) {
            ProductSceneView psv = (ProductSceneView) container;
            topComponent = new ProductNodeTopComponent(psv.getVisibleProductNode(), container);
        }

        if (topComponent != null) {
            Mode mode = WindowManager.getDefault().findMode("editor");
            mode.dockInto(topComponent);
            if (!topComponent.isOpened()) {
                topComponent.open();
            }
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
                    if (tabIndex >= 0) {
                        popupMenu.addSeparator();
                        popupMenu.add(new CloneWindowAction(tabIndex));
                    }
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

    /**
     * todo: make the API compatible or adaptible to the
     * <a href="http://bits.netbeans.org/8.0/javadoc/org-netbeans-core-multiview/index.html?overview-summary.html">org.netbeans.core.spi.multiview.MultiViewElement</a>.
     *
     * @param <T> The editor component type.
     */
    @SuppressWarnings("unchecked")
    public class Editor<T extends Container> {
        private final JInternalFrame internalFrame;

        Editor(JInternalFrame internalFrame) {
            this.internalFrame = internalFrame;
        }

        // Add more internalFrame property accessors here

        public void setTitle(String title) {
            internalFrame.setTitle(title);
        }

        public T getComponent() {
            return (T) this.internalFrame.getContentPane();
        }

        public void addListener(EditorListener editorListener) {
            final Editor<T> editor = this;
            internalFrame.addInternalFrameListener(new InternalFrameAdapter() {
                @Override
                public void internalFrameActivated(InternalFrameEvent e) {
                    editorListener.editorActivated(editor);
                }

                @Override
                public void internalFrameDeactivated(InternalFrameEvent e) {
                    editorListener.editorDeactivated(editor);
                }

                @Override
                public void internalFrameClosing(InternalFrameEvent e) {
                    editorListener.editorClosing(editor);
                }

                @Override
                public void internalFrameClosed(InternalFrameEvent e) {
                    editorListener.editorClosed(editor);
                }
            });
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Editor && internalFrame == ((Editor) o).internalFrame;
        }

        @Override
        public int hashCode() {
            return internalFrame.hashCode();
        }

    }

    public interface EditorListener<T extends Container> {
        void editorActivated(Editor<T> editor);

        void editorDeactivated(Editor<T> editor);

        boolean editorClosing(Editor<T> editor);

        void editorClosed(Editor<T> editor);
    }

    public static class EditorAdapter<T extends Container> implements EditorListener<T> {
        @Override
        public void editorActivated(Editor<T> editor) {
        }

        @Override
        public void editorDeactivated(Editor<T> editor) {
        }

        @Override
        public boolean editorClosing(Editor<T> editor) {
            return true;
        }

        @Override
        public void editorClosed(Editor<T> editor) {
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
            // todo - implement me!
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "Not implemented yet.");
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
            // todo - implement me!
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "Not implemented yet. (tabIndex=" + tabIndex + ")");
        }
    }

    private class CloneWindowAction extends AbstractAction {
        private final int tabIndex;

        public CloneWindowAction(int tabIndex) {
            super("Close Others");
            this.tabIndex = tabIndex;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // todo - implement me!
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "Not implemented yet. (tabIndex=" + tabIndex + ")");
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
            Set<JInternalFrame> internalFrameSet = frameToEditorMap.keySet();
            JInternalFrame[] internalFrames = internalFrameSet.toArray(new JInternalFrame[internalFrameSet.size()]);
            TopComponent topComponent = null;
            for (JInternalFrame internalFrame : internalFrames) {
                topComponent = moveInternalFrameToEditorMode(internalFrame);
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
            TopComponent topComponent = moveInternalFrameToEditorMode(internalFrame);
            if (topComponent != null) {
                topComponent.requestActive();
            }
        }
    }

    private class FloatIntoWorkspaceAction extends AbstractAction {
        private TopComponent window;

        public FloatIntoWorkspaceAction(TopComponent window) {
            super("Float into Workspace");
            this.window = window;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            window.close();
            addEditorComponent(window.getDisplayName(), window);
        }
    }

    private class FloatGroupIntoWorkspaceAction extends AbstractAction {
        private TopComponent window;

        public FloatGroupIntoWorkspaceAction(TopComponent window) {
            super("Float Group into Workspace");
            this.window = window;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Mode mode = WindowManager.getDefault().findMode(window);
            if (mode != null) {
                TopComponent[] topComponents = WindowManager.getDefault().getOpenedTopComponents(mode);
                for (TopComponent topComponent : topComponents) {
                    if (!(topComponent instanceof WorkspaceTopComponent)) {
                        topComponent.close();
                        addEditorComponent(topComponent.getDisplayName(), topComponent);
                    }
                }
            }
        }
    }

    private class InternalFrameListenerImpl implements InternalFrameListener {
        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            tabbedContainer.updateUI();
        }

        @Override
        public void internalFrameClosing(InternalFrameEvent e) {
            // do nothing
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            JInternalFrame internalFrame = e.getInternalFrame();
            if (frameToTabMap.containsKey(internalFrame)) {
                closeInternalFrame(internalFrame);
            }
            tabbedContainer.updateUI();
        }

        @Override
        public void internalFrameIconified(InternalFrameEvent e) {
            tabbedContainer.updateUI();
        }

        @Override
        public void internalFrameDeiconified(InternalFrameEvent e) {
            tabbedContainer.updateUI();
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {

            JInternalFrame internalFrame = e.getInternalFrame();
            TabData selectedTab = frameToTabMap.get(internalFrame);

            List<TabData> tabs = tabbedContainer.getModel().getTabs();
            for (int i = 0; i < tabs.size(); i++) {
                TabData tab = tabs.get(i);
                if (tab == selectedTab && tabbedContainer.getSelectionModel().getSelectedIndex() != i) {
                    tabbedContainer.getSelectionModel().setSelectedIndex(i);
                    break;
                }
            }
            tabbedContainer.updateUI();
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            tabbedContainer.updateUI();
        }
    }
}
