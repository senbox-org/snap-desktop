package org.esa.snap.rcp.spectrallibrary.ui;

import javax.swing.*;
import java.awt.*;


public class SpectralLibraryPanel extends JPanel {


    private final JComboBox<Object> libraryCombo = new JComboBox<>();
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton createFromProductButton = new JButton("New from Product…");
    private final JButton deleteLibraryButton = new JButton("Delete");
    private final JButton removeSelectedProfileButton = new JButton("Remove Profile");

    private final JButton addSelectedPreviewButton = new JButton("Add Selected Preview");
    private final JButton addAllPreviewButton = new JButton("Add All Preview");
    private final JToggleButton extractAtCursorToggle = new JToggleButton("Extract at Cursor");
    private final JButton extractSelectedPinsButton = new JButton("Extract Selected Pins");
    private final JButton extractAllPinsButton = new JButton("Extract All Pins");
    private final JButton clearPreviewButton = new JButton("Clear Preview");

    private final JLabel statusLabel = new JLabel(" ");

    private final SpectralProfileTableModel libraryTableModel = new SpectralProfileTableModel();
    private final JTable libraryTable = new JTable(libraryTableModel);

    private final PreviewPanel previewPanel = new PreviewPanel();


    public SpectralLibraryPanel() {
        super(new BorderLayout(6, 6));

        libraryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        libraryTable.setAutoCreateRowSorter(true);

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }


    private JComponent buildToolbar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);

        tb.add(new JLabel("Library: "));
        libraryCombo.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        tb.add(libraryCombo);

        tb.addSeparator();
        tb.add(refreshButton);
        tb.add(createFromProductButton);
        tb.add(deleteLibraryButton);

        tb.addSeparator();
        tb.add(removeSelectedProfileButton);

        tb.addSeparator();
        tb.add(addSelectedPreviewButton);
        tb.add(addAllPreviewButton);
        tb.add(clearPreviewButton);

        tb.addSeparator();
        tb.add(extractAtCursorToggle);
        tb.add(extractSelectedPinsButton);
        tb.add(extractAllPinsButton);

        return tb;
    }

    private JComponent buildCenter() {
        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.setBorder(BorderFactory.createTitledBorder("Library Profiles"));
        left.add(new JScrollPane(libraryTable), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(4, 4));
        right.setBorder(BorderFactory.createTitledBorder("Preview (not saved)"));
        right.add(previewPanel, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.55);
        return split;
    }

    private JComponent buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        p.add(statusLabel, BorderLayout.CENTER);
        return p;
    }

    public JComboBox<Object> getLibraryCombo() {
        return libraryCombo; }

    public JButton getRefreshButton() {
        return refreshButton;
    }

    public JButton getCreateFromProductButton() {
        return createFromProductButton;
    }

    public JButton getDeleteLibraryButton() {
        return deleteLibraryButton;
    }

    public JButton getRemoveSelectedProfileButton() {
        return removeSelectedProfileButton;
    }

    public JButton getAddSelectedPreviewButton() {
        return addSelectedPreviewButton;
    }

    public JButton getAddAllPreviewButton() {
        return addAllPreviewButton;
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

    public JButton getClearPreviewButton() {
        return clearPreviewButton;
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public JTable getLibraryTable() {
        return libraryTable;
    }

    public SpectralProfileTableModel getLibraryTableModel() {
        return libraryTableModel;
    }

    public PreviewPanel getPreviewPanel() {
        return previewPanel;
    }
}
