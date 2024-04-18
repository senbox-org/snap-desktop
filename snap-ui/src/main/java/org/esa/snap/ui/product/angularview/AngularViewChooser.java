package org.esa.snap.ui.product.angularview;

import com.bc.ceres.swing.TableLayout;
import com.jidesoft.swing.TristateCheckBox;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.util.ArrayUtils;
import org.esa.snap.ui.DecimalTableCellRenderer;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.product.LoadSaveRasterDataNodesConfigurationsComponent;
import org.esa.snap.ui.product.LoadSaveRasterDataNodesConfigurationsProvider;
import org.esa.snap.ui.tool.ToolButtonFactory;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AngularViewChooser extends ModalDialog implements LoadSaveRasterDataNodesConfigurationsComponent {

    private static final int band_selected_index = 0;
    private static final int band_name_index = 1;
    private static final int band_description_index = 2;
    private static final int band_wavelength_index = 3;
    private static final int band_bandwidth_index = 4;
    private static final int band_unit_index = 5;
    private final String[] band_columns =
            new String[]{"", "Band name", "Band description", "View Angle", "View Angle", "Unit"};
    private final static ImageIcon[] icons = new ImageIcon[]{
            UIUtils.loadImageIcon("icons/PanelDown12.png"),
            UIUtils.loadImageIcon("icons/PanelUp12.png"),
    };
    private final static ImageIcon[] roll_over_icons = new ImageIcon[]{
            ToolButtonFactory.createRolloverIcon(icons[0]),
            ToolButtonFactory.createRolloverIcon(icons[1]),
    };
    private final DisplayableAngularview[] originalangularViews;

    private DisplayableAngularview[] angularViews;
    private static AngularViewSelectionAdmin selectionAdmin;
    private static boolean selectionChangeLock;

    private JPanel angularViewsPanel;
    private final JPanel[] bandTablePanels;
    private final JTable[] bandTables;
    private final TristateCheckBox[] tristateCheckBoxes;
    private boolean[] collapsed;
    private TableLayout angularViewsPanelLayout;

    public AngularViewChooser(Window parent, DisplayableAngularview[] originalangularViews) {
        super(parent, "Available Spectra", ModalDialog.ID_OK_CANCEL_HELP, "AngularViewChooser");
        if (originalangularViews != null) {
            this.originalangularViews = originalangularViews;
            List<DisplayableAngularview> angularViewsWithBands = new ArrayList<>();
            for (DisplayableAngularview angularView : originalangularViews) {
                if (angularView.hasBands()) {
                    angularViewsWithBands.add(angularView);
                }
            }
            this.angularViews = angularViewsWithBands.toArray(new DisplayableAngularview[angularViewsWithBands.size()]);
        } else {
            this.originalangularViews = new DisplayableAngularview[0];
            this.angularViews = new DisplayableAngularview[0];
        }
        selectionAdmin = new AngularViewSelectionAdmin();
        selectionChangeLock = false;
        bandTables = new JTable[angularViews.length];
        collapsed = new boolean[angularViews.length];
        Arrays.fill(collapsed, true);
        bandTablePanels = new JPanel[angularViews.length];
        tristateCheckBoxes = new TristateCheckBox[angularViews.length];
        initUI();
    }

    private void initUI() {
        final JPanel content = new JPanel(new BorderLayout());
        initangularViewsPanel();
        final JScrollPane angularViewsScrollPane = new JScrollPane(angularViewsPanel);
        angularViewsScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        angularViewsScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        angularViewsScrollPane.setPreferredSize(new Dimension(angularViewsScrollPane.getPreferredSize().width, 180));
        content.add(angularViewsScrollPane, BorderLayout.CENTER);

        LoadSaveRasterDataNodesConfigurationsProvider provider = new LoadSaveRasterDataNodesConfigurationsProvider(this);
        AbstractButton loadButton = provider.getLoadButton();
        AbstractButton saveButton = provider.getSaveButton();
        TableLayout layout = new TableLayout(1);
        layout.setTablePadding(4, 4);
        JPanel buttonPanel = new JPanel(layout);
        buttonPanel.add(loadButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(layout.createVerticalSpacer());
        content.add(buttonPanel, BorderLayout.EAST);

        setContent(content);
    }

    private void initangularViewsPanel() {
        angularViewsPanelLayout = new TableLayout(7);
        angularViewsPanelLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        angularViewsPanelLayout.setTableWeightY(0.0);
        angularViewsPanelLayout.setTableWeightX(1.0);
        angularViewsPanelLayout.setColumnWeightX(0, 0.0);
        angularViewsPanelLayout.setColumnWeightX(1, 0.0);
        angularViewsPanelLayout.setTablePadding(3, 3);

        angularViewsPanel = new JPanel(angularViewsPanelLayout);
        angularViewsPanel.add(new JLabel(""));
        angularViewsPanel.add(new JLabel(""));
        angularViewsPanel.add(new JLabel("Spectrum Name"));
        angularViewsPanel.add(new JLabel("Unit"));
        angularViewsPanel.add(new JLabel("Line Style"));
        angularViewsPanel.add(new JLabel("Symbol"));
        angularViewsPanel.add(new JLabel("Symbol Size"));

        for (int i = 0; i < angularViews.length; i++) {
            selectionAdmin.evaluateAngularViewSelections(angularViews[i]);
            addAngularViewComponentsToAngularViewsPanel(i);
            angularViewsPanelLayout.setCellColspan((i * 2) + 2, 1, 6);
            angularViewsPanel.add(new JLabel());
            bandTablePanels[i] = new JPanel(new BorderLayout());
            bandTablePanels[i].setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            angularViewsPanel.add(bandTablePanels[i]);
            bandTables[i] = createBandsTable(i);
        }
        angularViewsPanel.add(angularViewsPanelLayout.createVerticalSpacer());
        angularViewsPanel.updateUI();
    }

    private void addAngularViewComponentsToAngularViewsPanel(int index) {
        DisplayableAngularview angularView = angularViews[index];
        ImageIcon strokeIcon;
        if (angularView.isDefaultOrRemainingBandsAngularView()) {
            strokeIcon = new ImageIcon();
        } else {
            strokeIcon = AngularViewStrokeProvider.getStrokeIcon(angularView.getLineStyle());
        }
        AbstractButton collapseButton = ToolButtonFactory.createButton(icons[0], false);
        collapseButton.addActionListener(new CollapseListener(index));
        final ImageIcon shapeIcon = AngularViewShapeProvider.getShapeIcon(angularView.getSymbolIndex());
        angularViewsPanel.add(collapseButton);
        final TristateCheckBox tristateCheckBox = new TristateCheckBox();
        tristateCheckBox.setState(selectionAdmin.getState(index));
        tristateCheckBox.addActionListener(new TristateCheckboxListener(index));
        tristateCheckBoxes[index] = tristateCheckBox;
        angularViewsPanel.add(tristateCheckBox);
        final JLabel angularViewNameLabel = new JLabel(angularView.getName());
        Font font = angularViewNameLabel.getFont();
        font = new Font(font.getName(), Font.BOLD, font.getSize());
        angularViewNameLabel.setFont(font);
        angularViewsPanel.add(angularViewNameLabel);
        angularViewsPanel.add(new JLabel(angularView.getUnit()));
        JComboBox<ImageIcon> strokeComboBox;
        if (angularView.isDefaultOrRemainingBandsAngularView()) {
            strokeComboBox = new JComboBox<>(new ImageIcon[]{strokeIcon});
            strokeComboBox.setEnabled(false);
        } else {
            strokeComboBox = new JComboBox<>(AngularViewStrokeProvider.getStrokeIcons());
            strokeComboBox.addActionListener(
                    e -> angularView.setLineStyle(
                            AngularViewStrokeProvider.getStroke((ImageIcon) strokeComboBox.getSelectedItem())));
        }
        strokeComboBox.setPreferredSize(new Dimension(100, 20));
        strokeComboBox.setSelectedItem(strokeIcon);
        angularViewsPanel.add(strokeComboBox);
        JComboBox<ImageIcon> shapeComboBox = new JComboBox<>(AngularViewShapeProvider.getShapeIcons());
        JComboBox<Integer> shapeSizeComboBox = new JComboBox<>(AngularViewShapeProvider.getScaleGrades());
        shapeComboBox.setPreferredSize(new Dimension(100, 20));
        shapeSizeComboBox.setPreferredSize(new Dimension(100, 20));
        shapeComboBox.setSelectedItem(shapeIcon);
        shapeComboBox.addActionListener(e -> {
            final int shapeIndex = AngularViewShapeProvider.getShape((ImageIcon) shapeComboBox.getSelectedItem());
            angularView.setSymbolIndex(shapeIndex);
            if (shapeIndex == AngularViewShapeProvider.EMPTY_SHAPE_INDEX) {
                shapeSizeComboBox.setSelectedItem("");
            } else {
                shapeSizeComboBox.setSelectedItem(angularView.getSymbolSize());
            }
        });
        angularViewsPanel.add(shapeComboBox);
        shapeSizeComboBox.setSelectedItem(angularView.getSymbolSize());
        shapeSizeComboBox.addActionListener(e -> {
            final String selectedItem = shapeSizeComboBox.getSelectedItem().toString();
            if (!selectedItem.equals("")) {
                angularView.setSymbolSize(Integer.parseInt(selectedItem));
            }
        });
        angularViewsPanel.add(shapeSizeComboBox);
    }

    private void toggleCollapsed(int index) {
        final boolean isCollapsed = !collapsed[index];
        collapsed[index] = isCollapsed;
        int rowIndex = (index * 2) + 2;
        if (isCollapsed) {
            angularViewsPanelLayout.setRowFill(rowIndex, TableLayout.Fill.HORIZONTAL);
            angularViewsPanelLayout.setRowWeightY(rowIndex, 0.0);
            bandTablePanels[index].removeAll();
        } else {
            angularViewsPanelLayout.setRowFill(rowIndex, TableLayout.Fill.BOTH);
            angularViewsPanelLayout.setRowWeightY(rowIndex, 1.0);
            bandTablePanels[index].add(bandTables[index].getTableHeader(), BorderLayout.NORTH);
            bandTablePanels[index].add(bandTables[index], BorderLayout.CENTER);
        }
        bandTablePanels[index].updateUI();
        angularViewsPanel.updateUI();
    }

    private JTable createBandsTable(int index) {
        DisplayableAngularview angularView = angularViews[index];
        final Band[] angularBands = angularView.getAngularBands();
        Object[][] angularViewData = new Object[angularBands.length][band_columns.length];
        for (int i = 0; i < angularBands.length; i++) {
            Band angularBand = angularBands[i];
            final boolean selected = angularView.isBandSelected(i) && angularView.isSelected();
            angularViewData[i][band_selected_index] = selected;
            angularViewData[i][band_name_index] = angularBand.getName();
            angularViewData[i][band_description_index] = angularBand.getDescription();
            angularViewData[i][band_wavelength_index] = angularBand.getAngularValue();
            angularViewData[i][band_bandwidth_index] = angularBand.getSpectralBandwidth();
            angularViewData[i][band_unit_index] = angularBand.getUnit();
        }
        final BandTableModel bandTableModel = new BandTableModel(angularViewData, band_columns);
        bandTableModel.addTableModelListener(e -> {
            e.getSource();
            if (e.getColumn() == band_selected_index) {
                final DisplayableAngularview angularView1 = angularViews[index];
                final int bandRow = e.getFirstRow();
                final Boolean selected = (Boolean) bandTableModel.getValueAt(bandRow, e.getColumn());
                angularView1.setBandSelected(bandRow, selected);
                if (!selectionChangeLock) {
                    selectionChangeLock = true;
                    selectionAdmin.setBandSelected(index, bandRow, selected);
                    selectionAdmin.updateAngularViewSelectionState(index, selectionAdmin.getState(index));
                    tristateCheckBoxes[index].setState(selectionAdmin.getState(index));
                    angularView1.setSelected(selectionAdmin.isAngularViewSelected(index));
                    selectionChangeLock = false;
                }
            }
        });
        JTable bandsTable = new JTable(bandTableModel);
        bandsTable.setRowSorter(new TableRowSorter<>(bandTableModel));

        final TableColumn selectionColumn = bandsTable.getColumnModel().getColumn(band_selected_index);
        final JCheckBox selectionCheckBox = new JCheckBox();
        selectionColumn.setCellEditor(new DefaultCellEditor(selectionCheckBox));
        selectionColumn.setMinWidth(20);
        selectionColumn.setMaxWidth(20);
        BooleanRenderer booleanRenderer = new BooleanRenderer();
        selectionColumn.setCellRenderer(booleanRenderer);

        final TableColumn wavelengthColumn = bandsTable.getColumnModel().getColumn(band_wavelength_index);
        wavelengthColumn.setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("###0.0##")));

        final TableColumn bandwidthColumn = bandsTable.getColumnModel().getColumn(band_bandwidth_index);
        bandwidthColumn.setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("###0.0##")));

        return bandsTable;
    }

    private void updateBandsTable(int index) {
        final TableModel tableModel = bandTables[index].getModel();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(selectionAdmin.isBandSelected(index, i), i, band_selected_index);
        }
    }

    public DisplayableAngularview[] getAngularViews() {
        return originalangularViews;
    }

    @Override
    public void setReadRasterDataNodeNames(String[] readRasterDataNodeNames) {
        for (JTable bandTable : bandTables) {
            BandTableModel bandTableModel = (BandTableModel) bandTable.getModel();
            for (int j = 0; j < bandTableModel.getRowCount(); j++) {
                String bandName = bandTableModel.getValueAt(j, band_name_index).toString();
                boolean selected = ArrayUtils.isMemberOf(bandName, readRasterDataNodeNames);
                bandTableModel.setValueAt(selected, j, band_selected_index);
            }
        }

    }

    @Override
    public String[] getRasterDataNodeNamesToWrite() {
        List<String> bandNames = new ArrayList<>();
        for (JTable bandTable : bandTables) {
            BandTableModel bandTableModel = (BandTableModel) bandTable.getModel();
            for (int j = 0; j < bandTableModel.getRowCount(); j++) {
                if ((boolean) bandTableModel.getValueAt(j, band_selected_index)) {
                    bandNames.add(bandTableModel.getValueAt(j, band_name_index).toString());
                }
            }
        }
        return bandNames.toArray(new String[bandNames.size()]);
    }

    private static class BandTableModel extends DefaultTableModel {

        private BandTableModel(Object[][] angularViewData, String[] bandColumns) {
            super(angularViewData, bandColumns);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == band_selected_index;
        }

    }

    private class BooleanRenderer extends JCheckBox implements TableCellRenderer {

        private BooleanRenderer() {
            this.setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            boolean selected = (Boolean) value;
            setSelected(selected);
            return this;
        }
    }

    private class CollapseListener implements ActionListener {

        private final int index;

        CollapseListener(int index) {
            this.index = index;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final Object source = e.getSource();
            if (source instanceof AbstractButton) {
                AbstractButton collapseButton = (AbstractButton) source;
                toggleCollapsed(index);
                if (collapsed[index]) {
                    collapseButton.setIcon(icons[0]);
                    collapseButton.setRolloverIcon(roll_over_icons[0]);
                } else {
                    collapseButton.setIcon(icons[1]);
                    collapseButton.setRolloverIcon(roll_over_icons[1]);
                }
            }
        }

    }

    private class TristateCheckboxListener implements ActionListener {

        private final int index;

        TristateCheckboxListener(int index) {
            this.index = index;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!selectionChangeLock) {
                selectionChangeLock = true;
                selectionAdmin.updateAngularViewSelectionState(index, tristateCheckBoxes[index].getState());
                tristateCheckBoxes[index].setState(selectionAdmin.getState(index));
                updateBandsTable(index);
                angularViews[index].setSelected(selectionAdmin.isAngularViewSelected(index));
                selectionChangeLock = false;
            }
        }
    }

}
