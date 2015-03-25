package org.esa.beam.framework.ui.product.spectrum;

import com.bc.ceres.swing.TableLayout;
import com.jidesoft.swing.TristateCheckBox;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.ui.DecimalTableCellRenderer;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

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
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//public class SpectrumChooser extends ModalDialog implements LoadSaveRasterDataNodesConfigurationsComponent {
public class SpectrumChooser extends ModalDialog {

    private static final int bandSelectedIndex = 0;
    private static final int bandNameIndex = 1;
    private static final int bandDescriptionIndex = 2;
    private static final int bandWavelengthIndex = 3;
    private static final int bandBandwidthIndex = 4;
    private static final int bandUnitIndex = 5;

    private final String[] bandColumns =
            new String[]{"", "Band name", "Band description", "Spectral wavelength (nm)", "Spectral bandwidth (nm)", "Unit"};


    private final DisplayableSpectrum[] originalSpectra;

    private DisplayableSpectrum[] spectra;
    private static SpectrumSelectionAdmin selectionAdmin;
    private static boolean selectionChangeLock;

    private static ImageIcon[] icons;
    private static ImageIcon[] rolloverIcons;

    private boolean[] collapsed;

    private final Map<Integer, JTable> rowToBandsTable;
    private JPanel spectraPanel;
    private final JPanel[] bandTablePanels;
    private final TristateCheckBox[] tristateCheckBoxes;
    private TableLayout spectraPanelLayout;
    private JComboBox shapeSizeComboBox;

    public SpectrumChooser(Window parent, DisplayableSpectrum[] originalSpectra) {
        super(parent, "Available Spectra", ModalDialog.ID_OK_CANCEL_HELP, "spectrumChooser");
        if (originalSpectra != null) {
            this.originalSpectra = originalSpectra;
            List<DisplayableSpectrum> spectraWithBands = new ArrayList<DisplayableSpectrum>();
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
        rowToBandsTable = new HashMap<Integer, JTable>();
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
        spectraScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        spectraScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        content.add(spectraScrollPane, BorderLayout.CENTER);

//        LoadSaveRasterDataNodesConfigurationsProvider provider = new LoadSaveRasterDataNodesConfigurationsProvider(this);
//        AbstractButton loadButton = provider.getLoadButton();
//        AbstractButton saveButton = provider.getSaveButton();
        TableLayout layout = new TableLayout(1);
        layout.setTablePadding(4, 4);
        JPanel buttonPanel = new JPanel(layout);
//        buttonPanel.add(loadButton);
//        buttonPanel.add(saveButton);
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

        if (icons == null) {
            icons = new ImageIcon[]{
                    UIUtils.loadImageIcon("icons/PanelDown12.png"),
                    UIUtils.loadImageIcon("icons/PanelUp12.png"),
            };
            rolloverIcons = new ImageIcon[]{
                    ToolButtonFactory.createRolloverIcon(icons[0]),
                    ToolButtonFactory.createRolloverIcon(icons[1]),
            };
        }

        for (int i = 0; i < spectra.length; i++) {
            DisplayableSpectrum spectrum = spectra[i];
            selectionAdmin.evaluateSpectrumSelections(spectrum);
            if (spectrum.hasBands()) {
                ImageIcon strokeIcon;
                if (spectrum.isRemainingBandsSpectrum()) {
                    strokeIcon = new ImageIcon();
                } else {
                    strokeIcon = SpectrumStrokeProvider.getStrokeIcon(spectrum.getLineStyle());
                }
                AbstractButton headerButton = ToolButtonFactory.createButton(icons[0], false);
                headerButton.setName("ComponentsPane.headerButton");
                final int finalI = i;
                headerButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        toggleCollapsed(finalI);
                        if (collapsed[finalI]) {
                            headerButton.setIcon(icons[0]);
                            headerButton.setRolloverIcon(rolloverIcons[0]);
                        } else {
                            headerButton.setIcon(icons[1]);
                            headerButton.setRolloverIcon(rolloverIcons[1]);
                        }
                    }
                });
                final ImageIcon shapeIcon = SpectrumShapeProvider.getShapeIcon(spectrum.getSymbolIndex());
                spectraPanel.add(headerButton);
                final TristateCheckBox tristateCheckBox = new TristateCheckBox();
                tristateCheckBox.setState(selectionAdmin.getState(finalI));
                tristateCheckBox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (!selectionChangeLock) {
                            selectionChangeLock = true;
                            selectionAdmin.updateSpectrumSelectionState(finalI, tristateCheckBox.getState());
                            tristateCheckBox.setState(selectionAdmin.getState(finalI));
                            updateBandsTable(finalI);
                            spectrum.setSelected(selectionAdmin.isSpectrumSelected(finalI));
//                            fireTableCellUpdated(row, column);
                            selectionChangeLock = false;
                        }
                    }
                });
                tristateCheckBoxes[i] = tristateCheckBox;
                spectraPanel.add(tristateCheckBox);
                final JLabel spectrumNameLabel = new JLabel(spectrum.getName());
                Font font = spectrumNameLabel.getFont();
                font = new Font(font.getName(), Font.BOLD, font.getSize());
                spectrumNameLabel.setFont(font);
                spectraPanel.add(spectrumNameLabel);
                spectraPanel.add(new JLabel(spectrum.getUnit()));
                JComboBox strokeComboBox = new JComboBox(SpectrumStrokeProvider.getStrokeIcons());
                strokeComboBox.setSelectedItem(strokeIcon);
                strokeComboBox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        spectrum.setLineStyle(SpectrumStrokeProvider.getStroke((ImageIcon) strokeComboBox.getSelectedItem()));
                    }
                });
                spectraPanel.add(strokeComboBox);
                JComboBox shapeComboBox = new JComboBox(SpectrumShapeProvider.getShapeIcons());
                shapeComboBox.setSelectedItem(shapeIcon);
                shapeComboBox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final int shapeIndex = SpectrumShapeProvider.getShape((ImageIcon) shapeComboBox.getSelectedItem());
                        spectrum.setSymbolIndex(shapeIndex);
                        if(shapeIndex == SpectrumShapeProvider.EMPTY_SHAPE_INDEX) {
                            shapeSizeComboBox.setSelectedItem("");
                        } else {
                            shapeSizeComboBox.setSelectedItem(spectrum.getSymbolSize());
                        }

                    }
                });
                spectraPanel.add(shapeComboBox);
                shapeSizeComboBox = new JComboBox(SpectrumShapeProvider.getScaleGrades());
                shapeSizeComboBox.setSelectedItem(spectrum.getSymbolSize());
                shapeSizeComboBox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final String selectedItem = shapeComboBox.getSelectedItem().toString();
                        if(!selectedItem.equals("")) {
                            spectrum.setSymbolSize(Integer.parseInt(selectedItem));
                        }
                    }
                });
                spectraPanel.add(shapeSizeComboBox);
                final int row = (i * 2) + 2;
                spectraPanelLayout.setCellColspan(row, 1, 6);
                spectraPanel.add(new JLabel());
                bandTablePanels[i] = new JPanel(new BorderLayout());
                bandTablePanels[i].setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                spectraPanel.add(bandTablePanels[i]);
            }
        }
        spectraPanel.add(spectraPanelLayout.createVerticalSpacer());
        spectraPanel.updateUI();
    }

    private void toggleCollapsed(int index) {
        final boolean isCollapsed = !collapsed[index];
        collapsed[index] = isCollapsed;
        collapsed[index] = isCollapsed;
        int rowIndex = (index * 2) + 2;
        if(isCollapsed) {
            spectraPanelLayout.setRowFill(rowIndex, TableLayout.Fill.HORIZONTAL);
            spectraPanelLayout.setRowWeightY(rowIndex, 0.0);
            if (rowToBandsTable.containsKey(index)) {
                bandTablePanels[index].removeAll();
            }
        } else {
            spectraPanelLayout.setRowFill(rowIndex, TableLayout.Fill.BOTH);
            spectraPanelLayout.setRowWeightY(rowIndex, 1.0);
            DisplayableSpectrum spectrum = spectra[index];
            if (rowToBandsTable.containsKey(index)) {
                final JTable table = rowToBandsTable.get(index);
                bandTablePanels[index].add(table.getTableHeader(), BorderLayout.NORTH);
                bandTablePanels[index].add(table, BorderLayout.CENTER);
            } else {
                bandTablePanels[index].removeAll();
                final Band[] spectralBands = spectrum.getSpectralBands();
                Object[][] spectrumData = new Object[spectralBands.length][bandColumns.length];
                for (int i = 0; i < spectralBands.length; i++) {
                    Band spectralBand = spectralBands[i];
                    final boolean selected = spectrum.isBandSelected(i) && spectrum.isSelected();
                    spectrumData[i][bandSelectedIndex] = selected;
                    spectrumData[i][bandNameIndex] = spectralBand.getName();
                    spectrumData[i][bandDescriptionIndex] = spectralBand.getDescription();
                    spectrumData[i][bandWavelengthIndex] = spectralBand.getSpectralWavelength();
                    spectrumData[i][bandBandwidthIndex] = spectralBand.getSpectralBandwidth();
                    spectrumData[i][bandUnitIndex] = spectralBand.getUnit();
                }
                final BandTableModel bandTableModel = new BandTableModel(spectrumData, bandColumns);
                bandTableModel.addTableModelListener(new TableModelListener() {
                    @Override
                    public void tableChanged(TableModelEvent e) {
                        e.getSource();
                        if (e.getColumn() == bandSelectedIndex) {
                            final DisplayableSpectrum spectrum = spectra[index];
                            final int bandRow = e.getFirstRow();
                            final Boolean selected = (Boolean) bandTableModel.getValueAt(bandRow, e.getColumn());
                            spectrum.setBandSelected(bandRow, selected);
                            if (!selectionChangeLock) {
                                selectionChangeLock = true;
                                selectionAdmin.setBandSelected(index, bandRow, selected);
                                selectionAdmin.updateSpectrumSelectionState(index, selectionAdmin.getState(index));
                                tristateCheckBoxes[index].setState(selectionAdmin.getState(index));
                                spectrum.setSelected(selectionAdmin.isSpectrumSelected(index));
                                selectionChangeLock = false;
                            }
                        }
                    }
                });
                JTable bandsTable = new JTable(bandTableModel);
                bandsTable.setRowSorter(new TableRowSorter<>(bandTableModel));

                final TableColumn selectionColumn = bandsTable.getColumnModel().getColumn(bandSelectedIndex);
                final JCheckBox selectionCheckBox = new JCheckBox();
                selectionColumn.setCellEditor(new DefaultCellEditor(selectionCheckBox));
                selectionColumn.setMinWidth(20);
                selectionColumn.setMaxWidth(20);
                BooleanRenderer booleanRenderer = new BooleanRenderer();
                selectionColumn.setCellRenderer(booleanRenderer);

                final TableColumn wavelengthColumn = bandsTable.getColumnModel().getColumn(bandWavelengthIndex);
                wavelengthColumn.setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("###0.0##")));

                final TableColumn bandwidthColumn = bandsTable.getColumnModel().getColumn(bandBandwidthIndex);
                bandwidthColumn.setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("###0.0##")));
                rowToBandsTable.put(index, bandsTable);
                bandTablePanels[index].add(bandsTable.getTableHeader(), BorderLayout.NORTH);
                bandTablePanels[index].add(bandsTable, BorderLayout.CENTER);
            }
        }
        bandTablePanels[index].updateUI();
        spectraPanel.updateUI();
    }

    private void updateBandsTable(int index) {
        if (rowToBandsTable.containsKey(index)) {
            final JTable bandsTable = rowToBandsTable.get(index);
            final TableModel tableModel = bandsTable.getModel();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(selectionAdmin.isBandSelected(index, i), i, bandSelectedIndex);
            }
        } else {
            for (int i = 0; i < spectra[index].getSpectralBands().length; i++) {
                spectra[index].setBandSelected(i, selectionAdmin.isBandSelected(index, i));
            }
        }
    }

    public DisplayableSpectrum[] getSpectra() {
        return originalSpectra;
    }

//    @Override
//    public void setReadRasterDataNodeNames(String[] readRasterDataNodeNames) {
//        SpectrumTableModel spectrumTableModel = getSpectrumTableModel();
//        for (int i = 0; i < spectraTable.getRowCount(); i++) {
//            BandTableModel bandTableModel = spectrumTableModel.getBandTableModel(i);
//            for (int j = 0; j < bandTableModel.getRowCount(); j++) {
//                String bandName = bandTableModel.getValueAt(j, bandNameIndex).toString();
//                boolean selected = ArrayUtils.isMemberOf(bandName, readRasterDataNodeNames);
//                bandTableModel.setValueAt(selected, j, bandSelectedIndex);
//            }
//        }
//    }

//    @Override
//    public String[] getRasterDataNodeNamesToWrite() {
//        List<String> bandNames = new ArrayList<>();
//        SpectrumTableModel spectrumTableModel = getSpectrumTableModel();
//        for (int i = 0; i < spectrumTableModel.getRowCount(); i++) {
//            BandTableModel bandTableModel = spectrumTableModel.getBandTableModel(i);
//            for (int j = 0; j < bandTableModel.getRowCount(); j++) {
//                if ((boolean) bandTableModel.getValueAt(j, bandSelectedIndex)) {
//                    bandNames.add(bandTableModel.getValueAt(j, bandNameIndex).toString());
//                }
//            }
//        }
//        return bandNames.toArray(new String[bandNames.size()]);
//    }

    private static class BandTableModel extends DefaultTableModel {

        private BandTableModel(Object[][] spectrumData, String[] bandColumns) {
            super(spectrumData, bandColumns);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == bandSelectedIndex;
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

}