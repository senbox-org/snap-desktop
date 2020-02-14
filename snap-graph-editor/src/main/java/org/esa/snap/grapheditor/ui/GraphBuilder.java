package org.esa.snap.grapheditor.ui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.esa.snap.grapheditor.ui.components.MainPanel;
import org.esa.snap.grapheditor.ui.components.StatusPanel;
import org.esa.snap.grapheditor.ui.components.interfaces.GraphListener;
import org.esa.snap.grapheditor.ui.components.interfaces.NodeInterface;
import org.esa.snap.grapheditor.ui.components.utils.GraphManager;
import org.esa.snap.grapheditor.ui.components.utils.SettingManager;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.DefaultAppContext;

/**
 * Main View for the new Graph Builder.
 * It implement the GraphListener to know when the graph changes to enable/disable save functionality.
 * 
 * @author Martino Ferrari (CS Group)
 */
public class GraphBuilder extends JPanel implements GraphListener {
    /**
     * GraphBuilder serail ID
     */
    private static final long serialVersionUID = 50849209475264574L;
    private final JButton saveAsButton;
    private final JButton saveButton;

    private final Window parentWindow;

    private boolean hasChanged = false;

    private File openFile = null;

    /**
     * Create a new GraphBuilder inside a frame.
     * @param frame parent frame window
     * @param context application context
     */
    GraphBuilder(Window frame, AppContext context) {
        super();
        parentWindow = frame;

        // Initialise empty graph
        GraphManager.getInstance().setAppContext(context);
        GraphManager.getInstance().createEmptyGraph();

        // Init GUI
        this.setLayout(new BorderLayout(0, 0));

        JToolBar toolBar = new JToolBar();
        this.add(toolBar, BorderLayout.PAGE_START);

        JButton settingsButton = new JButton();
        ImageIcon settingIcon = TangoIcons.categories_preferences_system(TangoIcons.Res.R22);
        settingsButton.setIcon(settingIcon);
        settingsButton.addActionListener(e -> SettingManager.getInstance().showSettingsDialog(parentWindow));

        JButton runButton = new JButton();
        ImageIcon runIcon = TangoIcons.actions_media_playback_start(TangoIcons.R22);
        runButton.setIcon(runIcon);
        runButton.addActionListener(e -> GraphManager.getInstance().evaluate());

        saveButton = new JButton();
        ImageIcon saveIcon = TangoIcons.actions_document_save(TangoIcons.R22);
        saveButton.setIcon(saveIcon);
        saveButton.addActionListener(e -> saveGraph());

        saveAsButton = new JButton();
        ImageIcon saveAsIcon = TangoIcons.actions_document_save_as(TangoIcons.R22);
        saveAsButton.setIcon(saveAsIcon);
        saveAsButton.addActionListener(e-> {
            if (hasChanged) {
                openFile = null;
                saveGraph();
            }
        });

        saveAsButton.setEnabled(false);
        saveButton.setEnabled(false);

        JButton openButton = new JButton();
        ImageIcon openIcon = TangoIcons.actions_document_open(TangoIcons.R22);
        openButton.setIcon(openIcon);
        openButton.addActionListener(e -> {

            if (confirmClean()) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new FileNameExtensionFilter("Graph XML file", "xml"));
                fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                int result = fileChooser.showOpenDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    GraphManager.getInstance().openGraph(selectedFile);
                    openFile = selectedFile;
                }
            }
        });

        JButton newButton = new JButton();
        ImageIcon newIcon = TangoIcons.actions_document_new(TangoIcons.R22);
        newButton.setIcon(newIcon);
        newButton.addActionListener(e -> {
            if (confirmClean()) {
                newGraph();
            }
        });

        toolBar.setFloatable(false);
        toolBar.add(newButton);
        toolBar.add(openButton);
        toolBar.add(saveButton);
        toolBar.add(saveAsButton);
        toolBar.addSeparator();
        toolBar.add(runButton);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(settingsButton);

        StatusPanel statusBar = new StatusPanel();
        this.add(statusBar, BorderLayout.PAGE_END);

        MainPanel mainPanel = new MainPanel();
        this.add(mainPanel, BorderLayout.CENTER);

        mainPanel.getGraphPanel().addGraphListener(this);
    }


    private void somethingChanged() {
        if (!hasChanged) {
            hasChanged = true;
            saveAsButton.setEnabled(true);
            saveButton.setEnabled(true);
        }
    }

    private void newGraph() {
        GraphManager.getInstance().createEmptyGraph();
        hasChanged = false;
        saveAsButton.setEnabled(false);
        saveButton.setEnabled(false);
        openFile = null;
        this.repaint();
    }

    private void saveGraph() {
        if (hasChanged) {
            if (openFile == null) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new FileNameExtensionFilter("Graph XML file",
                                                                      "xml"));
                fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                int result = fileChooser.showSaveDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    if (GraphManager.getInstance().saveGraph(selectedFile)) {
                        hasChanged = false;
                        openFile = selectedFile;
                    }
                }
            } else {
                if (GraphManager.getInstance().saveGraph(openFile)) {
                    hasChanged = false;
                }
            }
        }
    }

    @Override
    public void selected(NodeInterface source) { }

    @Override
    public void deselected(NodeInterface source) { }

    @Override
    public void updated(NodeInterface source) {
        somethingChanged();
    }

    @Override
    public void created(NodeInterface source) {
        somethingChanged();
    }

    @Override
    public void deleted(NodeInterface source) {
        somethingChanged();
    }

    private boolean confirmClean() {
        if (hasChanged) {
            int dialogResult = JOptionPane.showConfirmDialog (null,
                                                              "This graph has unsaved changes, are you sure to close it?",
                                                              "Warning", JOptionPane.YES_NO_OPTION);
            return dialogResult == JOptionPane.YES_OPTION;
        }
        return true;
    }

    /**
     * Simple main routine to test the GraphBuilder, in the future it could be and independent executable as well.
     * @param args program arguments
     */
    public static void main(String[] args) {
        AppContext context = new DefaultAppContext("Standalone Graph Editor");

        JFrame mainFrame = new JFrame("Standalone Graph Editor");
        GraphBuilder builder = new GraphBuilder(mainFrame, context);


        mainFrame.setLayout(new BorderLayout());
        mainFrame.setSize(1024, 800);
        mainFrame.add(builder, BorderLayout.CENTER);
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (builder.confirmClean()) {
                    mainFrame.setVisible(false);
                    mainFrame.dispose();
                    System.exit(0);
                }
            }
        });
        mainFrame.setVisible(true);
    }

}