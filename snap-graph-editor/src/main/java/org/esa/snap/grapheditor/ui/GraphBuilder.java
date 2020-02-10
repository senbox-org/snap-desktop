package org.esa.snap.grapheditor.ui;

import java.awt.*;

import javax.swing.*;

import org.esa.snap.grapheditor.ui.components.MainPanel;
import org.esa.snap.grapheditor.ui.components.StatusPanel;
import org.esa.snap.grapheditor.ui.components.utils.GraphManager;
import org.esa.snap.grapheditor.ui.components.utils.SettingManager;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.DefaultAppContext;

/**
 * Main View for the new Graph Builder
 * 
 * @author Martino Ferrari (CS Group)
 */
public class GraphBuilder extends JPanel {
    /**
     * GraphBuilder serail ID
     */
    private static final long serialVersionUID = 50849209475264574L;

    private JToolBar toolBar;
    private StatusPanel statusBar;
    private MainPanel mainPanel;

    private Window parentWindow;

    public GraphBuilder(Window frame, AppContext context) {
        super();
        parentWindow = frame;
        this.setLayout(new BorderLayout(0, 0));

        toolBar = new JToolBar();
        this.add(toolBar, BorderLayout.PAGE_START);

        JButton settingsButton = new JButton();
        ImageIcon settingIcon = TangoIcons.categories_preferences_system(TangoIcons.Res.R22);
        settingsButton.setIcon(settingIcon);
        settingsButton.addActionListener(e -> {
            SettingManager.getInstance().showSettingsDialog(parentWindow);
        });

        JButton runButton = new JButton();
        ImageIcon runIcon = TangoIcons.actions_media_playback_start(TangoIcons.R22);
        runButton.setIcon(runIcon);
        runButton.addActionListener(e -> {
            GraphManager.getInstance().evaluate();
        });

        toolBar.setFloatable(false);
        toolBar.add(settingsButton);
        toolBar.add(runButton);

        statusBar = new StatusPanel();
        this.add(statusBar, BorderLayout.PAGE_END);

        mainPanel = new MainPanel(context);
        this.add(mainPanel, BorderLayout.CENTER);
    }

    private JFrame getFrame() {
        Container comp = this.getParent();
        while (comp != null && !(comp instanceof JFrame)) {
            comp = comp.getParent();
        }

        if (comp == null){
            return null;
        }
        return (JFrame) comp;
    }

    public static void main(String[] args) {

        AppContext context = new DefaultAppContext("Standalone Graph Editor");

        JFrame mainFrame = new JFrame();
        GraphBuilder builder = new GraphBuilder(mainFrame, context);

        
        mainFrame.setLayout(new BorderLayout());
        mainFrame.setSize(1024, 800);
        mainFrame.add(builder, BorderLayout.CENTER);

        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setVisible(true);
    }
}