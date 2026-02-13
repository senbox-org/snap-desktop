package org.esa.snap.rcp.spectrallibrary.ui;

import org.esa.snap.rcp.actions.help.HelpAction;
import org.esa.snap.rcp.spectrallibrary.model.SpectralProfileTableModel;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;

import javax.swing.*;
import java.awt.*;


public class SpectralLibraryPanel extends JPanel {


    private ToolTopComponent ownerTopComponent = null;

    // library actions
    private final JComboBox<Object> libraryCombo = new JComboBox<>();
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton createFromProductButton = new JButton("New Library from Product...");
    private final JButton deleteLibraryButton = new JButton("Delete Active Library");
    private final JButton renameLibraryButton = new JButton("Rename Active Library");
    private final JButton importButton = new JButton("Import...");
    private final JButton exportButton = new JButton("Export...");

    // profile table actions
    private final SpectralProfileTableModel libraryTableModel = new SpectralProfileTableModel();
    private final JTable libraryTable = new JTable(libraryTableModel);
    private final JButton removeSelectedProfilesButton = new JButton("Remove Profiles");
    private final JButton previewSelectedProfilesButton = new JButton("Preview Selected Profiles");
    private final JButton addAttributeButton = new JButton("Add Attribute...");

    // preview actions
    private final PreviewPanel previewPanel = new PreviewPanel();
    private final JButton addSelectedPreviewButton = new JButton("Add Selected Preview");
    private final JButton addAllPreviewButton = new JButton("Add All Preview");
    private final JButton clearPreviewButton = new JButton("Clear Preview");
    private final JToggleButton extractAtCursorToggle = new JToggleButton("Extract at Cursor");
    private final JButton extractSelectedPinsButton = new JButton("Extract Selected Pins");
    private final JButton extractAllPinsButton = new JButton("Extract All Pins");
    private AbstractButton filterButton;

    // status actions
    private final JLabel statusLabel = new JLabel(" ");
    private AbstractButton helpButton;


    public SpectralLibraryPanel(ToolTopComponent ownerTopComponent) {
        super(new BorderLayout(6, 6));
        this.ownerTopComponent = ownerTopComponent;

        add(buildCenter(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }


    private JComponent buildCenter() {
        previewPanel.setHeader(buildPreviewHeader());

        JPanel right = new JPanel(new BorderLayout(4, 4));
        right.setBorder(BorderFactory.createTitledBorder("Profile Extraction"));
        right.add(previewPanel, BorderLayout.CENTER);

        JPanel left = buildLeftSide();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.5);
        split.setDividerLocation(0.5);
        split.setContinuousLayout(true);

        return split;
    }


    private JPanel buildLeftSide() {
        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.setBorder(BorderFactory.createTitledBorder("Library"));

        left.add(buildToolbar(), BorderLayout.NORTH);
        left.add(buildLibraryTablePanel(), BorderLayout.CENTER);

        return left;
    }


    private JComponent buildToolbar() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row1.add(importButton);
        row1.add(exportButton);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row2.add(createFromProductButton);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row3.add(new JLabel("Active Library: "));
        libraryCombo.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        row3.add(libraryCombo);

        JPanel row4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row4.add(refreshButton);
        row4.add(renameLibraryButton);
        row4.add(deleteLibraryButton);

        p.add(Box.createVerticalStrut(5));
        p.add(row1);
        p.add(Box.createVerticalStrut(5));
        p.add(row2);
        p.add(Box.createVerticalStrut(5));
        p.add(row3);
        p.add(Box.createVerticalStrut(5));
        p.add(row4);
        p.add(Box.createVerticalStrut(15));

        return p;
    }

    private JComponent buildLibraryTablePanel() {
        libraryTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        libraryTable.setAutoCreateRowSorter(true);

        JPanel tablePanel = new JPanel(new BorderLayout(4, 4));
        tablePanel.setBorder(BorderFactory.createTitledBorder("Library Profiles"));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        header.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        header.add(previewSelectedProfilesButton);
        header.add(removeSelectedProfilesButton);
        header.add(addAttributeButton);

        tablePanel.add(header, BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(libraryTable), BorderLayout.CENTER);

        return tablePanel;
    }


    private JComponent buildPreviewHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        filterButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Filter24.gif"), false);
        filterButton.setName("filterButton");
        filterButton.setToolTipText("Filter spectra by bands or band groups");
        filterButton.setEnabled(false);

        JPanel row0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row0.add(filterButton);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row1.add(extractAtCursorToggle);
        row1.add(extractSelectedPinsButton);
        row1.add(extractAllPinsButton);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row2.add(addSelectedPreviewButton);
        row2.add(addAllPreviewButton);
        row2.add(clearPreviewButton);

        header.add(Box.createVerticalStrut(5));
        header.add(row0);
        header.add(Box.createVerticalStrut(5));
        header.add(row1);
        header.add(Box.createVerticalStrut(5));
        header.add(row2);
        header.add(Box.createVerticalStrut(5));

        return header;
    }

    private JComponent buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        helpButton = ToolButtonFactory.createButton(new HelpAction(ownerTopComponent), false);
        helpButton.setName("helpButton");
        helpButton.setToolTipText("Help.");


        p.add(statusLabel, BorderLayout.CENTER);
        p.add(helpButton, BorderLayout.EAST);
        return p;
    }


    // library actions
    public JComboBox<Object> getLibraryCombo() {
        return libraryCombo;
    }
    public JButton getRefreshButton() {
        return refreshButton;
    }
    public JButton getCreateFromProductButton() {
        return createFromProductButton;
    }
    public JButton getDeleteLibraryButton() {
        return deleteLibraryButton;
    }
    public JButton getRenameLibraryButton() {
        return renameLibraryButton;
    }
    public JButton getImportButton() {
        return importButton;
    }
    public JButton getExportButton() {
        return exportButton;
    }

    // profile table actions
    public JTable getLibraryTable() {
        return libraryTable;
    }
    public SpectralProfileTableModel getLibraryTableModel() {
        return libraryTableModel;
    }
    public JButton getRemoveSelectedProfilesButton() {
        return removeSelectedProfilesButton;
    }
    public JButton getPreviewSelectedProfilesButton() {
        return previewSelectedProfilesButton;
    }
    public JButton getAddAttributeButton() {
        return addAttributeButton;
    }

    // preview actions
    public PreviewPanel getPreviewPanel() {
        return previewPanel;
    }
    public JButton getAddSelectedPreviewButton() {
        return addSelectedPreviewButton;
    }
    public JButton getAddAllPreviewButton() {
        return addAllPreviewButton;
    }
    public JButton getClearPreviewButton() {
        return clearPreviewButton;
    }
    public JToggleButton getExtractAtCursorToggle() {
        return extractAtCursorToggle;
    }
    public JButton getExtractSelectedPinsButton() {
        return extractSelectedPinsButton;
    }
    public JButton getExtractAllPinsButton() {
        return extractAllPinsButton;
    }
    public AbstractButton getFilterButton() {
        return filterButton;
    }

    // status actions
    public JLabel getStatusLabel() {
        return statusLabel;
    }
}
