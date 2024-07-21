package org.esa.snap.ui.product.spectrum;

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

public class SpectrumChooser extends ModalDialog implements LoadSaveRasterDataNodesConfigurationsComponent {

    private static final int band_selected_index = 0;
    private static final int band_name_index = 1;
    private static final int band_description_index = 2;
    private static final int band_wavelength_index = 3;
    private static final int band_bandwidth_ndex = 4;
    private static final int band_unit_index = 5;
    private final String[] band_columns =
            new String[]{"", "Band name", "Band description", "Spectral wavelength (nm)", "Spectral bandwidth (nm)", "Unit"};
    private final static ImageIcon[] icons = new ImageIcon[]{
            UIUtils.loadImageIcon("icons/PanelDown12.png"),
            UIUtils.loadImageIcon("icons/PanelUp12.png"),
    };
    private final static ImageIcon[] roll_over_icons = new ImageIcon[]{
            ToolButtonFactory.createRolloverIcon(icons[0]),
            ToolButtonFactory.createRolloverIcon(icons[1]),
    };
    private final DisplayableSpectrum[] originalSpectra;

    private DisplayableSpectrum[] spectra;
    private static SpectrumSelectionAdmin selectionAdmin;
    private static boolean selectionChangeLock;

    private JPanel spectraPanel;
    private final JPanel[] bandTablePanels;
    private final JTable[] bandTables;
    private final TristateCheckBox[] tristateCheckBoxes;
    private boolean[] collapsed;
    private TableLayout spectraPanelLayout;

    public SpectrumChooser(Window parent, DisplayableSpectrum[] originalSpectra) {
        super(parent, "Available Spectra", ModalDialog.ID_OK_CANCEL_HELP, "spectrumChooser");
        if (originalSpectra != null) {
            this.originalSpectra = originalSpectra;
            List<DisplayableSpectrum> spectraWithBands = new ArrayList<>();
            for (DisplayableSpectrum spectrum : originalSpectra) {
                if (spectrum.hasBands()) {
                    spectraWithBands.add(spectrum);
                }
            }
            this.spectra = spectraWithBands.toArray(new DisplayableSpectrum[spectraWithBands.size()]);
        } else {
            this.originalSpectra = new DisplayableSpectrum[0];
            this.spectra = new DisplayableSpectrum[0];
        }
        selectionAdmin = new SpectrumSelectionAdmin();
        selectionChangeLock = false;
        bandTables = new JTable[spectra.length];
        collapsed = new boolean[spectra.length];
        Arrays.fill(collapsed, true);
        bandTablePanels = new JPanel[spectra.length];
        tristateCheckBoxes = new TristateCheckBox[spectra.length];
        initUI();
    }

    private void initUI() {
        final JPanel content = new JPanel(new BorderLayout());
        initSpectraPanel();
        final JScrollPane spectraScrollPane = new JScrollPane(spectraPanel);
        spectraScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        spectraScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        spectraScrollPane.setPreferredSize(new Dimension(spectraScrollPane.getPreferredSize().width, 180));
        content.add(spectraScrollPane, BorderLayout.CENTER);

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

    private void initSpectraPanel() {
        spectraPanelLayout = new TableLayout(7);
        spectraPanelLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        spectraPanelLayout.setTableWeightY(0.0);
        spectraPanelLayout.setTableWeightX(1.0);
        spectraPanelLayout.setColumnWeightX(0, 0.0);
        spectraPanelLayout.setColumnWeightX(1, 0.0);
        spectraPanelLayout.setTablePadding(3, 3);

        spectraPanel = new JPanel(spectraPanelLayout);
        spectraPanel.add(new JLabel(""));
        spectraPanel.add(new JLabel(""));
        spectraPanel.add(new JLabel("Spectrum Name"));
        spectraPanel.add(new JLabel("Unit"));
        spectraPanel.add(new JLabel("Line Style"));
        spectraPanel.add(new JLabel("Symbol"));
        spectraPanel.add(new JLabel("Symbol Size"));

        for (int i = 0; i < spectra.length; i++) {
            selectionAdmin.evaluateSpectrumSelections(spectra[i]);
            addSpectrumComponentsToSpectraPanel(i);
            spectraPanelLayout.setCellColspan((i * 2) + 2, 1, 6);
            spectraPanel.add(new JLabel());
            bandTablePanels[i] = new JPanel(new BorderLayout());
            bandTablePanels[i].setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            spectraPanel.add(bandTablePanels[i]);
            bandTables[i] = createBandsTable(i);
        }
        spectraPanel.add(spectraPanelLayout.createVerticalSpacer());
        spectraPanel.updateUI();
    }

    private void addSpectrumComponentsToSpectraPanel(int index) {
        DisplayableSpectrum spectrum = spectra[index];
        ImageIcon strokeIcon;
        if (spectrum.isDefaultOrRemainingBandsSpectrum()) {
            strokeIcon = new ImageIcon();
        } else {
            strokeIcon = SpectrumStrokeProvider.getStrokeIcon(spectrum.getLineStyle());
        }
        AbstractButton collapseButton = ToolButtonFactory.createButton(icons[0], false);
        collapseButton.addActionListener(new CollapseListener(index));
        final ImageIcon shapeIcon = SpectrumShapeProvider.getShapeIcon(spectrum.getSymbolIndex());
        spectraPanel.add(collapseButton);
        final TristateCheckBox tristateCheckBox = new TristateCheckBox();
        tristateCheckBox.setState(isSelected(spectrum));
        tristateCheckBox.addActionListener(new TristateCheckboxListener(index));
        tristateCheckBoxes[index] = tristateCheckBox;
        spectraPanel.add(tristateCheckBox);
        final JLabel spectrumNameLabel = new JLabel(spectrum.getName());
        Font font = spectrumNameLabel.getFont();
        font = new Font(font.getName(), Font.BOLD, font.getSize());
        spectrumNameLabel.setFont(font);
        spectraPanel.add(spectrumNameLabel);
        spectraPanel.add(new JLabel(spectrum.getUnit()));
        JComboBox<ImageIcon> strokeComboBox;
        if (spectrum.isDefaultOrRemainingBandsSpectrum()) {
            strokeComboBox = new JComboBox<>(new ImageIcon[]{strokeIcon});
            strokeComboBox.setEnabled(false);
        } else {
            strokeComboBox = new JComboBox<>(SpectrumStrokeProvider.getStrokeIcons());
            strokeComboBox.addActionListener(
                    e -> spectrum.setLineStyle(
                            SpectrumStrokeProvider.getStroke((ImageIcon) strokeComboBox.getSelectedItem())));
        }
        strokeComboBox.setPreferredSize(new Dimension(100, 20));
        strokeComboBox.setSelectedItem(strokeIcon);
        spectraPanel.add(strokeComboBox);
        JComboBox<ImageIcon> shapeComboBox = new JComboBox<>(SpectrumShapeProvider.getShapeIcons());
        JComboBox<Integer> shapeSizeComboBox = new JComboBox<>(SpectrumShapeProvider.getScaleGrades());
        shapeComboBox.setPreferredSize(new Dimension(100, 20));
        shapeSizeComboBox.setPreferredSize(new Dimension(100, 20));
        shapeComboBox.setSelectedItem(shapeIcon);
        shapeComboBox.addActionListener(e -> {
            final int shapeIndex = SpectrumShapeProvider.getShape((ImageIcon) shapeComboBox.getSelectedItem());
            spectrum.setSymbolIndex(shapeIndex);
            if (shapeIndex == SpectrumShapeProvider.EMPTY_SHAPE_INDEX) {
                shapeSizeComboBox.setSelectedItem("");
            } else {
                shapeSizeComboBox.setSelectedItem(spectrum.getSymbolSize());
            }
        });
        spectraPanel.add(shapeComboBox);
        shapeSizeComboBox.setSelectedItem(spectrum.getSymbolSize());
        shapeSizeComboBox.addActionListener(e -> {
            final String selectedItem = shapeSizeComboBox.getSelectedItem().toString();
            if (!selectedItem.equals("")) {
                spectrum.setSymbolSize(Integer.parseInt(selectedItem));
            }
        });
        spectraPanel.add(shapeSizeComboBox);
    }

    private static int isSelected(DisplayableSpectrum spectrum) {
        if (spectrum.isSelected()) {
            return TristateCheckBox.STATE_SELECTED;
        }
        return TristateCheckBox.STATE_UNSELECTED;
    }

    private void toggleCollapsed(int index) {
        final boolean isCollapsed = !collapsed[index];
        collapsed[index] = isCollapsed;
        int rowIndex = (index * 2) + 2;
        if (isCollapsed) {
            spectraPanelLayout.setRowFill(rowIndex, TableLayout.Fill.HORIZONTAL);
            spectraPanelLayout.setRowWeightY(rowIndex, 0.0);
            bandTablePanels[index].removeAll();
        } else {
            spectraPanelLayout.setRowFill(rowIndex, TableLayout.Fill.BOTH);
            spectraPanelLayout.setRowWeightY(rowIndex, 1.0);
            bandTablePanels[index].add(bandTables[index].getTableHeader(), BorderLayout.NORTH);
            bandTablePanels[index].add(bandTables[index], BorderLayout.CENTER);
        }
        bandTablePanels[index].updateUI();
        spectraPanel.updateUI();
    }

    private JTable createBandsTable(int index) {
        DisplayableSpectrum spectrum = spectra[index];
        final Band[] spectralBands = spectrum.getSpectralBands();
        Object[][] spectrumData = new Object[spectralBands.length][band_columns.length];
        for (int i = 0; i < spectralBands.length; i++) {
            Band spectralBand = spectralBands[i];
            final boolean selected = spectrum.isBandSelected(i) && spectrum.isSelected();
            spectrumData[i][band_selected_index] = selected;
            spectrumData[i][band_name_index] = spectralBand.getName();
            spectrumData[i][band_description_index] = spectralBand.getDescription();
            spectrumData[i][band_wavelength_index] = spectralBand.getSpectralWavelength();
            spectrumData[i][band_bandwidth_ndex] = spectralBand.getSpectralBandwidth();
            spectrumData[i][band_unit_index] = spectralBand.getUnit();
        }
        final BandTableModel bandTableModel = new BandTableModel(spectrumData, band_columns);
        bandTableModel.addTableModelListener(e -> {
            e.getSource();
            if (e.getColumn() == band_selected_index) {
                final DisplayableSpectrum spectrum1 = spectra[index];
                final int bandRow = e.getFirstRow();
                final Boolean selected = (Boolean) bandTableModel.getValueAt(bandRow, e.getColumn());
                spectrum1.setBandSelected(bandRow, selected);
                if (!selectionChangeLock) {
                    selectionChangeLock = true;
                    selectionAdmin.setBandSelected(index, bandRow, selected);
                    selectionAdmin.updateSpectrumSelectionState(index, selectionAdmin.getState(index));
                    tristateCheckBoxes[index].setState(selectionAdmin.getState(index));
                    spectrum1.setSelected(selectionAdmin.isSpectrumSelected(index));
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

        final TableColumn bandwidthColumn = bandsTable.getColumnModel().getColumn(band_bandwidth_ndex);
        bandwidthColumn.setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("###0.0##")));

        return bandsTable;
    }

    private void updateBandsTable(int index) {
        final TableModel tableModel = bandTables[index].getModel();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(selectionAdmin.isBandSelected(index, i), i, band_selected_index);
        }
    }

    public DisplayableSpectrum[] getSpectra() {
        return originalSpectra;
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

        private BandTableModel(Object[][] spectrumData, String[] bandColumns) {
            super(spectrumData, bandColumns);
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
                selectionAdmin.updateSpectrumSelectionState(index, tristateCheckBoxes[index].getState());
                tristateCheckBoxes[index].setState(selectionAdmin.getState(index));
                updateBandsTable(index);
                spectra[index].setSelected(selectionAdmin.isSpectrumSelected(index));
                selectionChangeLock = false;
            }
        }
    }

}
