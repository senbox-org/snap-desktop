package org.esa.snap.graphbuilder.ui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.esa.snap.graphbuilder.ui.components.MainPanel;
import org.esa.snap.graphbuilder.ui.components.StatusPanel;
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

    public GraphBuilder(AppContext context) {
        super();

        this.setLayout(new BorderLayout(0, 0));

        toolBar = new JToolBar();
        this.add(toolBar, BorderLayout.PAGE_START);

        statusBar = new StatusPanel();
        this.add(statusBar, BorderLayout.PAGE_END);

        mainPanel = new MainPanel(context);
        this.add(mainPanel, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        AppContext context = new DefaultAppContext("Standalone Graph Builder");
        GraphBuilder builder = new GraphBuilder(context);
        JFrame mainFrame = new JFrame();
        
        mainFrame.setLayout(new BorderLayout());
        mainFrame.setSize(1024, 800);
        mainFrame.add(builder, BorderLayout.CENTER);

        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setVisible(true);
    }

}