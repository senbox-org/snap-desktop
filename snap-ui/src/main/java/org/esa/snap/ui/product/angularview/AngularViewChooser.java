package org.esa.snap.ui.product.angularview;

import com.bc.ceres.swing.TableLayout;
import com.jidesoft.swing.TristateCheckBox;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.util.ArrayUtils;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.ui.DecimalTableCellRenderer;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.color.ColorComboBox;
import org.esa.snap.ui.product.LoadSaveRasterDataNodesConfigurationsComponent;
import org.esa.snap.ui.product.LoadSaveRasterDataNodesConfigurationsProvider;
import org.esa.snap.ui.product.spectrum.DisplayableSpectrum;
import org.esa.snap.ui.product.spectrum.SpectrumChooser;
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



    private int lineIndex = 1;
    private int symbolIndex = 1;
    private int symbolSize = 3;
    private Color plotColor = Color.BLACK;
    
    

    private double group1Wavelength = -1;
    private double group1Tolerance = 1;
    private int group1LineIndex = 1;
    private int group1SymbolIndex = 1;
    private int group1SymbolSize = 3;
    private Color group1Color = Color.BLACK;

    private double group2Wavelength = -1;
    private double group2Tolerance = 1;
    private int group2LineIndex = 1;
    private int group2SymbolIndex = 1;
    private int group2SymbolSize = 3;
    private Color group2Color = Color.BLACK;


    private double group3Wavelength = -1;
    private double group3Tolerance = 1;
    private int group3LineIndex = 1;
    private int group3SymbolIndex = 1;
    private int group3SymbolSize = 3;
    private Color group3Color = Color.BLACK;


    private double group4Wavelength = -1;
    private double group4Tolerance = 1;
    private int group4LineIndex = 1;
    private int group4SymbolIndex = 1;
    private int group4SymbolSize = 3;
    private Color group4Color = Color.BLACK;


    private double group5Wavelength = -1;
    private double group5Tolerance = 1;
    private int group5LineIndex = 1;
    private int group5SymbolIndex = 1;
    private int group5SymbolSize = 3;
    private Color group5Color = Color.BLACK;
    
    
    
    public AngularViewChooser(Window parent, DisplayableAngularview[] originalangularViews,
                              int lineIndex, int symbolIndex, int symbolSize, Color plotColor,
                              double group1Wavelength, double group1Tolerance, int group1LineIndex, int group1SymbolIndex, int group1SymbolSize, Color group1Color,
                              double group2Wavelength, double group2Tolerance, int group2LineIndex, int group2SymbolIndex, int group2SymbolSize, Color group2Color,
                              double group3Wavelength, double group3Tolerance, int group3LineIndex, int group3SymbolIndex, int group3SymbolSize, Color group3Color,
                              double group4Wavelength, double group4Tolerance, int group4LineIndex, int group4SymbolIndex, int group4SymbolSize, Color group4Color,
                              double group5Wavelength, double group5Tolerance, int group5LineIndex, int group5SymbolIndex, int group5SymbolSize, Color group5Color
    ) {


                
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


        this.lineIndex = lineIndex;
        this.symbolIndex = symbolIndex;
        this.symbolSize = symbolSize;
        this.plotColor = plotColor;
        
        
        this.group1Wavelength = group1Wavelength;
        this.group1Tolerance = group1Tolerance;
        this.group1LineIndex = group1LineIndex;
        this.group1SymbolIndex = group1SymbolIndex;
        this.group1SymbolSize = group1SymbolSize;
        this.group1Color = group1Color;


        this.group2Wavelength = group2Wavelength;
        this.group2Tolerance = group2Tolerance;
        this.group2LineIndex = group2LineIndex;
        this.group2SymbolIndex = group2SymbolIndex;
        this.group2SymbolSize = group2SymbolSize;
        this.group2Color = group2Color;


        this.group3Wavelength = group3Wavelength;
        this.group3Tolerance = group3Tolerance;
        this.group3LineIndex = group3LineIndex;
        this.group3SymbolIndex = group3SymbolIndex;
        this.group3SymbolSize = group3SymbolSize;
        this.group3Color = group3Color;


        this.group4Wavelength = group4Wavelength;
        this.group4Tolerance = group4Tolerance;
        this.group4LineIndex = group4LineIndex;
        this.group4SymbolIndex = group4SymbolIndex;
        this.group4SymbolSize = group4SymbolSize;
        this.group4Color = group4Color;



        this.group5Wavelength = group5Wavelength;
        this.group5Tolerance = group5Tolerance;
        this.group5LineIndex = group5LineIndex;
        this.group5SymbolIndex = group5SymbolIndex;
        this.group5SymbolSize = group5SymbolSize;
        this.group5Color = group5Color;
        
        
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
        angularViewsPanelLayout = new TableLayout(8);
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
        angularViewsPanel.add(new JLabel("Color"));

        for (int i = 0; i < angularViews.length; i++) {
            selectionAdmin.evaluateAngularViewSelections(angularViews[i]);
            addAngularViewComponentsToAngularViewsPanel(i);
            angularViewsPanelLayout.setCellColspan((i * 2) + 2, 1, 7);
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
        tristateCheckBox.setState(isSelected(angularView));
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
            strokeComboBox.setSelectedIndex(1);
            strokeComboBox.setEnabled(false);
        } else {
            strokeComboBox = new JComboBox<>(AngularViewStrokeProvider.getStrokeIcons());
            strokeComboBox.setSelectedIndex(1);
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


        // set group color
        DisplayableAngularview displayableAngularview = angularViews[index];
        final Band[] angularBands = displayableAngularview.getAngularBands();



        //  set default graphic settingw
        
        int mainLineIndex = lineIndex;
        int mainSymbolIndex = symbolIndex;
        int mainSymbolSize = symbolSize;
        Color mainPlotColor = plotColor;
        
 
        



//        if (angularView.getColor() == null) {
            // initialize color
            Color COLOR_DEFAULT = Color.BLACK;
            Color colorThisBand = COLOR_DEFAULT;

            // todo  working on differently coloring each group
            float angularBandSpectralWavelength = angularBands[0].getSpectralWavelength();
//            System.out.println("angularBandSpectralWavelength=" + angularBandSpectralWavelength);

            boolean isGroupedByWavelength = true;
            for (Band band : angularBands) {
                if (band.getSpectralWavelength() != angularBandSpectralWavelength) {
                    isGroupedByWavelength = false;
                    break;
                }
            }
            if (isGroupedByWavelength) {
//
//                // Make sure all selections within bounds
//                group1LineIndex = confineIntToBounds(group1LineIndex, 0, (strokeComboBox.getRegisteredKeyStrokes().length - 1));
//                group1SymbolIndex = confineIntToBounds(group1SymbolIndex, 0, (shapeComboBox.getRegisteredKeyStrokes().length - 1));
//                group1SymbolSize = confineIntToBounds(group1SymbolSize, 1, 9);
//                int group1SymbolSizeIndex = group1SymbolSize -1;




//                float TOLERANCE = (float) 2.0;
                if (angularBandSpectralWavelength >= (group1Wavelength - group1Tolerance) && angularBandSpectralWavelength <= (group1Wavelength + group1Tolerance)) {
                    mainLineIndex = group1LineIndex;
                    mainSymbolIndex = group1SymbolIndex;
                    mainSymbolSize = group1SymbolSize;
                    mainPlotColor = group1Color;

                } else if (angularBandSpectralWavelength >= (group2Wavelength - group2Tolerance) && angularBandSpectralWavelength <= (group2Wavelength + group2Tolerance)) {
                    mainLineIndex = group2LineIndex;
                    mainSymbolIndex = group2SymbolIndex;
                    mainSymbolSize = group2SymbolSize;
                    mainPlotColor = group2Color;

                } else if (angularBandSpectralWavelength >= (group3Wavelength - group3Tolerance) && angularBandSpectralWavelength <= (group3Wavelength + group3Tolerance)) {
                    mainLineIndex = group3LineIndex;
                    mainSymbolIndex = group3SymbolIndex;
                    mainSymbolSize = group3SymbolSize;
                    mainPlotColor = group3Color;


                } else if (angularBandSpectralWavelength >= (group4Wavelength - group4Tolerance) && angularBandSpectralWavelength <= (group4Wavelength + group4Tolerance)) {
                    mainLineIndex = group4LineIndex;
                    mainSymbolIndex = group4SymbolIndex;
                    mainSymbolSize = group4SymbolSize;
                    mainPlotColor = group4Color;


                } else if (angularBandSpectralWavelength >= (group5Wavelength - group5Tolerance) && angularBandSpectralWavelength <= (group5Wavelength + group5Tolerance)) {
                    mainLineIndex = group5LineIndex;
                    mainSymbolIndex = group5SymbolIndex;
                    mainSymbolSize = group5SymbolSize;
                    mainPlotColor = group5Color;

                }
            }

//            angularView.setColor(colorThisBand);
//        }


        
        // Make sure all selections within bounds
        mainLineIndex = confineIntToBounds(mainLineIndex, 0, (strokeComboBox.getRegisteredKeyStrokes().length - 1));
        mainSymbolIndex = confineIntToBounds(mainSymbolIndex, 0, (shapeComboBox.getRegisteredKeyStrokes().length - 1));
        mainSymbolSize = confineIntToBounds(mainSymbolSize, 1, 9);
        int mainSymbolSizeIndex = mainSymbolSize - 1;


        strokeComboBox.setSelectedIndex(mainLineIndex);
        shapeComboBox.setSelectedIndex(mainSymbolIndex);
        shapeSizeComboBox.setSelectedIndex(mainSymbolSizeIndex);
        angularView.setColor(mainPlotColor);




        ColorComboBox colorComboBox = new ColorComboBox(angularView.getColor());
        colorComboBox.setPreferredSize(new Dimension(100, 20));
        colorComboBox.setSelectedColor(angularView.getColor());
        colorComboBox.addPropertyChangeListener(e -> {
            final Color selectedColor = colorComboBox.getSelectedColor();
            angularView.setColor(selectedColor);
        });
        angularViewsPanel.add(colorComboBox);

    }



    private static int confineIntToBounds(int intValue, int minValue, int maxValue) {
        if (intValue < minValue) {
            intValue = minValue;
        } else if (intValue > maxValue) {
            intValue = maxValue;
        }

        return intValue;
    }

    private static int isSelected(DisplayableAngularview angularview) {
        if (angularview.isSelected()) {
            return TristateCheckBox.STATE_SELECTED;
        }
        return TristateCheckBox.STATE_UNSELECTED;
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
