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
    private AbstractButton refreshButton;
    private final JButton createFromProductButton = new JButton("New Library from Product...");
    private final JButton deleteLibraryButton = new JButton("Delete Active Library");
    private final JButton renameLibraryButton = new JButton("Rename Active Library");
    private AbstractButton importButton;
    private AbstractButton exportButton;

    // profile table actions
    private final SpectralProfileTableModel libraryTableModel = new SpectralProfileTableModel();
    private final JTable libraryTable = new JTable(libraryTableModel);
    private final JButton removeSelectedProfilesButton = new JButton("Remove Selected Profiles");
    private final JButton previewSelectedProfilesButton = new JButton("Preview Selected Profiles");
    private final JButton addAttributeButton = new JButton("Add Attribute...");

    // preview actions
    private final PreviewPanel previewPanel = new PreviewPanel();
    private final JButton addSelectedPreviewButton = new JButton("Add Selected Preview");
    private final JButton addAllPreviewButton = new JButton("Add All Preview");
    private final JButton clearPreviewButton = new JButton("Clear Preview");
    private AbstractButton extractAtCursorToggle;
    private AbstractButton extractSelectedPinsButton;
    private AbstractButton extractAllPinsButton;
    private AbstractButton extractSelectedGeometryButton;
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
        right.setBorder(BorderFactory.createTitledBorder("Spectral Profile Extraction"));
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

        importButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Import24.gif"), false);
        importButton.setName("importButton");
        importButton.setToolTipText("Import Spectral Library from file.");
        importButton.setEnabled(true);

        exportButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Export24.gif"), false);
        exportButton.setName("exportButton");
        exportButton.setToolTipText("Export Spectral Library to file.");
        exportButton.setEnabled(true);

        refreshButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Update24.gif"), false);
        refreshButton.setName("refreshButton");
        refreshButton.setToolTipText("Refresh Profiles Table of Active Library.");
        refreshButton.setEnabled(true);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row1.add(importButton);
        row1.add(exportButton);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row2.add(createFromProductButton);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row3.add(new JLabel("Active Library: "));
        libraryCombo.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        row3.add(libraryCombo);
        row3.add(Box.createHorizontalStrut(5));
        row3.add(refreshButton);

        JPanel row4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
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
        filterButton.setToolTipText("Filter Spectra by Bands/Band Groups");
        filterButton.setEnabled(true);

        extractAtCursorToggle = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/CursorSpectrum24.gif"), true);
        extractAtCursorToggle.setName("extractAtCursorToggle");
        extractAtCursorToggle.setToolTipText("Extract Spectrum at Cursor Position (by Click in view).");
        extractAtCursorToggle.setEnabled(true);
        extractAtCursorToggle.setSelected(false);

        extractSelectedPinsButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/SelectedPinSpectra24.gif"), false);
        extractSelectedPinsButton.setName("extractSelectedPinsButton");
        extractSelectedPinsButton.setToolTipText("Extract Spectrum for Selected Pins.");
        extractSelectedPinsButton.setEnabled(true);

        extractAllPinsButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/PinSpectra24.gif"), false);
        extractAllPinsButton.setName("extractAllPinsButton");
        extractAllPinsButton.setToolTipText("Extract Spectrum for All Pins.");
        extractAllPinsButton.setEnabled(true);

        extractSelectedGeometryButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/DrawRectangleTool24.gif"), false);
        extractSelectedGeometryButton.setName("extractSelectedGeometryButton");
        extractSelectedGeometryButton.setToolTipText("Extract Spectrum for All Pixels within Selected Geometry.");
        extractSelectedGeometryButton.setEnabled(true);

        JPanel row0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row0.add(new JLabel("Filter Spectra by bands/band groups: "));
        row0.add(filterButton);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row1.add(new JLabel("Extraction Operations: "));
        row1.add(extractAtCursorToggle);
        row1.add(extractSelectedPinsButton);
        row1.add(extractAllPinsButton);
        row1.add(extractSelectedGeometryButton);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row2.add(new JLabel("Add to Active Library: "));
        row2.add(addSelectedPreviewButton);
        row2.add(addAllPreviewButton);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row3.add(clearPreviewButton);

        header.add(Box.createVerticalStrut(5));
        header.add(row0);
        header.add(Box.createVerticalStrut(5));
        header.add(row1);
        header.add(Box.createVerticalStrut(5));
        header.add(row2);
        header.add(Box.createVerticalStrut(5));
        header.add(row3);
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
    public AbstractButton getRefreshButton() {
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
    public AbstractButton getImportButton() {
        return importButton;
    }
    public AbstractButton getExportButton() {
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
    public AbstractButton getExtractAtCursorToggle() {
        return extractAtCursorToggle;
    }
    public AbstractButton getExtractSelectedPinsButton() {
        return extractSelectedPinsButton;
    }
    public AbstractButton getExtractAllPinsButton() {
        return extractAllPinsButton;
    }

    public AbstractButton getExtractSelectedGeometryButton() {
        return extractSelectedGeometryButton;
    }

    public AbstractButton getFilterButton() {
        return filterButton;
    }

    // status actions
    public JLabel getStatusLabel() {
        return statusLabel;
    }
}
