package org.esa.snap.grapheditor.ui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.esa.snap.grapheditor.ui.components.MainPanel;
import org.esa.snap.grapheditor.ui.components.StatusPanel;
import org.esa.snap.grapheditor.ui.components.graph.NodeGui;
import org.esa.snap.grapheditor.ui.components.utils.GraphListener;
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
public class GraphBuilder extends JPanel implements GraphListener {
    /**
     * GraphBuilder serail ID
     */
    private static final long serialVersionUID = 50849209475264574L;
    private final JButton saveAsButton;
    private final JButton saveButton;
    private final JButton runButton;

    private JToolBar toolBar;
    private StatusPanel statusBar;
    private MainPanel mainPanel;

    private Window parentWindow;

    private boolean hasChanged = false;

    private File openFile = null;

    public GraphBuilder(Window frame, AppContext context) {
        super();
        parentWindow = frame;

        // Initialise empty graph
        GraphManager.getInstance().createEmptyGraph();

        // Init GUI
        this.setLayout(new BorderLayout(0, 0));

        toolBar = new JToolBar();
        this.add(toolBar, BorderLayout.PAGE_START);

        JButton settingsButton = new JButton();
        ImageIcon settingIcon = TangoIcons.categories_preferences_system(TangoIcons.Res.R22);
        settingsButton.setIcon(settingIcon);
        settingsButton.addActionListener(e -> {
            SettingManager.getInstance().showSettingsDialog(parentWindow);
        });

        runButton = new JButton();
        ImageIcon runIcon = TangoIcons.actions_media_playback_start(TangoIcons.R22);
        runButton.setIcon(runIcon);
        runButton.addActionListener(e -> {
            GraphManager.getInstance().evaluate();
        });

        saveButton = new JButton();
        ImageIcon saveIcon = TangoIcons.actions_document_save(TangoIcons.R22);
        saveButton.setIcon(saveIcon);
        saveButton.addActionListener(e -> {
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
        });

        saveAsButton = new JButton();
        ImageIcon saveAsIcon = TangoIcons.actions_document_save_as(TangoIcons.R22);
        saveAsButton.setIcon(saveAsIcon);
        saveAsButton.addActionListener(e-> {
            if (hasChanged) {
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

        statusBar = new StatusPanel();
        this.add(statusBar, BorderLayout.PAGE_END);

        mainPanel = new MainPanel(context);
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

    @Override
    public void selected(NodeGui source) {
        somethingChanged();
    }

    @Override
    public void deselected(NodeGui source) {
        somethingChanged();
    }

    @Override
    public void updated(NodeGui source) {
        somethingChanged();
    }

    @Override
    public void created(NodeGui source) {
        somethingChanged();
    }

    @Override
    public void deleted(NodeGui source) {
        somethingChanged();
    }

    public boolean confirmClean() {
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
     * @param args
     */
    public static void main(String[] args) {

        AppContext context = new DefaultAppContext("Standalone Graph Editor");

        JFrame mainFrame = new JFrame();
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