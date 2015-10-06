/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.rcp.pixelinfo;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.CollapsibleItemsPanel;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.product.ProductSceneView;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 * The pixel info view component is used to display the geophysical values for the pixel at a given pixel position
 * (x,y). The pixel info view can simultaneously display band, tie point grid and flag values.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version 1.2
 */
public class PixelInfoView extends JPanel {

    public static final String HELP_ID = "pixelInfoView";

    /**
     * Preferences key for showing all band pixel values in pixel info view
     */
    public static final String PREFERENCE_KEY_SHOW_ONLY_DISPLAYED_BAND_PIXEL_VALUES = "pixelview.showOnlyDisplayedBands";

    /**
     * Preferences key for display style of geo-locations
     */
    public static final String PREFERENCE_KEY_SHOW_GEO_POS_DECIMALS = "pixelview.showGeoPosDecimals";

    /**
     * Preferences key for showing floating-point image coordinates
     */
    public static final String PREFERENCE_KEY_SHOW_PIXEL_POS_DECIMALS = "pixelview.showPixelPosDecimals";

    /**
     * Preferences key for coordinate system starting at (1,1)
     */
    public static final String PREFERENCE_KEY_SHOW_PIXEL_POS_OFFSET_ONE = "pixelview.showPixelPosOffsetOne";

    public static final boolean PREFERENCE_DEFAULT_SHOW_DISPLAYED_BAND_PIXEL_VALUES = true;
    public static final boolean PREFERENCE_DEFAULT_SHOW_GEO_POS_DECIMALS = false;
    public static final boolean PREFERENCE_DEFAULT_SHOW_PIXEL_POS_DECIMALS = false;
    public static final boolean PREFERENCE_DEFAULT_SHOW_PIXEL_POS_OFFSET_1 = false;

    private static final int NAME_COLUMN = 0;
    private static final int VALUE_COLUMN = 1;
    private static final int UNIT_COLUMN = 2;
    private boolean showGeoPosDecimals;

    public static final int POSITION_INDEX = 0;
    public static final int TIME_INDEX = 1;
    public static final int BANDS_INDEX = 2;
    public static final int TIE_POINT_GRIDS_INDEX = 3;
    public static final int FLAGS_INDEX = 4;

    private final PropertyChangeListener displayFilterListener;
    private final ProductNodeListener productNodeListener;

    private boolean showPixelPosDecimals;
    private boolean showPixelPosOffset1;

    private DisplayFilter displayFilter;

    private final PixelInfoViewTableModel positionTableModel;
    private final PixelInfoViewTableModel timeTableModel;
    private final PixelInfoViewTableModel bandsTableModel;
    private final PixelInfoViewTableModel tiePointGridsTableModel;
    private final PixelInfoViewTableModel flagsTableModel;

    private final PixelInfoViewModelUpdater modelUpdater;
    private final PixelInfoUpdateService updateService;
    private CollapsibleItemsPanel collapsibleItemsPanel;

    /**
     * Constructs a new pixel info view.
     */
    public PixelInfoView() {
        super(new BorderLayout());
        displayFilterListener = createDisplayFilterListener();
        productNodeListener = createProductNodeListener();
        positionTableModel = new PixelInfoViewTableModel(new String[]{"Position", "Value", "Unit"});
        timeTableModel = new PixelInfoViewTableModel(new String[]{"Time", "Value", "Unit"});
        bandsTableModel = new PixelInfoViewTableModel(new String[]{"Band", "Value", "Unit"});
        tiePointGridsTableModel = new PixelInfoViewTableModel(new String[]{"Tie-Point Grid", "Value", "Unit"});
        flagsTableModel = new PixelInfoViewTableModel(new String[]{"Flag", "Value",});
        modelUpdater = new PixelInfoViewModelUpdater(this, positionTableModel,
                                                     timeTableModel,
                                                     bandsTableModel,
                                                     tiePointGridsTableModel,
                                                     flagsTableModel
        );
        updateService = new PixelInfoUpdateService(modelUpdater);
        setDisplayFilter(new DisplayFilter());
        final Preferences preferences = SnapApp.getDefault().getPreferences();
        preferences.addPreferenceChangeListener(new PreferenceChangeListener() {
            @Override
            public void preferenceChange(PreferenceChangeEvent evt) {
                final String propertyName = evt.getKey();
                if (PREFERENCE_KEY_SHOW_ONLY_DISPLAYED_BAND_PIXEL_VALUES.equals(propertyName)) {
                    setShowOnlyLoadedBands(preferences);
                } else if (PREFERENCE_KEY_SHOW_PIXEL_POS_DECIMALS.equals(propertyName)) {
                    setShowPixelPosDecimals(preferences);
                } else if (PREFERENCE_KEY_SHOW_GEO_POS_DECIMALS.equals(propertyName)) {
                    setShowGeoPosDecimals(preferences);
                } else if (PREFERENCE_KEY_SHOW_PIXEL_POS_OFFSET_ONE.equals(propertyName)) {
                    setShowPixelPosOffset1(preferences);
                }
            }
        });
        setShowOnlyLoadedBands(preferences);
        setShowPixelPosDecimals(preferences);
        setShowGeoPosDecimals(preferences);
        setShowPixelPosOffset1(preferences);
        createUI();
    }

    ProductNodeListener getProductNodeListener() {
        return productNodeListener;
    }

    private ProductNodeListener createProductNodeListener() {
        return new ProductNodeListenerAdapter() {
            @Override
            public void nodeChanged(ProductNodeEvent event) {
                updateService.requestUpdate();
            }

            @Override
            public void nodeAdded(ProductNodeEvent event) {
                updateService.requestUpdate();
            }

            @Override
            public void nodeRemoved(ProductNodeEvent event) {
                updateService.requestUpdate();
            }
        };
    }

    private PropertyChangeListener createDisplayFilterListener() {
        return evt -> {
            if (getCurrentProduct() != null) {
                updateService.requestUpdate();
                clearSelectionInRasterTables();
            }
        };
    }

    /**
     * Returns the current product
     *
     * @return the current Product
     */
    public Product getCurrentProduct() {
        return modelUpdater.getCurrentProduct();
    }

    /**
     * Sets the filter to be used to filter the displayed bands. <p>
     *
     * @param displayFilter the filter, can be null
     */
    public void setDisplayFilter(DisplayFilter displayFilter) {
        if (this.displayFilter != displayFilter) {
            if (this.displayFilter != null) {
                this.displayFilter.removePropertyChangeListener(displayFilterListener);
            }
            this.displayFilter = displayFilter;
            this.displayFilter.addPropertyChangeListener(displayFilterListener);
        }
    }

    /**
     * Returns the display filter
     *
     * @return the display filter, can be null
     */
    public DisplayFilter getDisplayFilter() {
        return displayFilter;
    }

    private void setShowPixelPosDecimals(boolean showPixelPosDecimals) {
        if (this.showPixelPosDecimals != showPixelPosDecimals) {
            this.showPixelPosDecimals = showPixelPosDecimals;
            updateService.requestUpdate();
        }
    }

    boolean getShowPixelPosDecimal() {
        return showPixelPosDecimals;
    }

    private void setShowGeoPosDecimals(boolean showGeoPosDecimals) {
        if (this.showGeoPosDecimals != showGeoPosDecimals) {
            this.showGeoPosDecimals = showGeoPosDecimals;
            updateService.requestUpdate();
        }
    }

    boolean getShowGeoPosDecimals() {
        return showGeoPosDecimals;
    }

    private void setShowPixelPosOffset1(boolean showPixelPosOffset1) {
        if (this.showPixelPosOffset1 != showPixelPosOffset1) {
            this.showPixelPosOffset1 = showPixelPosOffset1;
            updateService.requestUpdate();
        }
    }

    public boolean getShowPixelPosOffset1() {
        return showPixelPosOffset1;
    }

    public void updatePixelValues(ProductSceneView view, int pixelX, int pixelY, int level, boolean pixelPosValid) {
        updateService.updateState(view, pixelX, pixelY, level, pixelPosValid);
    }

    private void createUI() {
        DefaultTableCellRenderer pixelValueRenderer = new ValueCellRenderer();
        FlagCellRenderer flagCellRenderer = new FlagCellRenderer();

        setLayout(new BorderLayout());

        CollapsibleItemsPanel.Item<JTable> positionItem = CollapsibleItemsPanel.createTableItem("Position", 6, 3);
        positionItem.getComponent().setModel(positionTableModel);
        positionItem.getComponent().getColumnModel().getColumn(1).setCellRenderer(pixelValueRenderer);

        CollapsibleItemsPanel.Item<JTable> timeItem = CollapsibleItemsPanel.createTableItem("Time", 2, 3);
        timeItem.getComponent().setModel(timeTableModel);
        timeItem.getComponent().getColumnModel().getColumn(1).setCellRenderer(pixelValueRenderer);

        CollapsibleItemsPanel.Item<JTable> tiePointGridsItem = CollapsibleItemsPanel.createTableItem("Tie-Point Grids", 0, 3);
        tiePointGridsItem.getComponent().setModel(tiePointGridsTableModel);
        tiePointGridsItem.getComponent().getColumnModel().getColumn(1).setCellRenderer(pixelValueRenderer);

        CollapsibleItemsPanel.Item<JTable> bandsItem = CollapsibleItemsPanel.createTableItem("Bands", 18, 3);
        bandsItem.getComponent().setModel(bandsTableModel);
        bandsItem.getComponent().getColumnModel().getColumn(1).setCellRenderer(pixelValueRenderer);

        CollapsibleItemsPanel.Item<JTable> flagsItem = CollapsibleItemsPanel.createTableItem("Flags", 0, 2);
        flagsItem.getComponent().setModel(flagsTableModel);
        flagsItem.getComponent().getColumnModel().getColumn(1).setCellRenderer(flagCellRenderer);

        collapsibleItemsPanel = new CollapsibleItemsPanel(
                positionItem, // POSITION_INDEX = 0
                timeItem,  // TIME_INDEX = 1
                bandsItem, // BANDS_INDEX = 2
                tiePointGridsItem, // TIE_POINT_GRIDS_INDEX = 3
                flagsItem // FLAGS_INDEX =  4
        );
        collapsibleItemsPanel.setCollapsed(POSITION_INDEX, false);
        collapsibleItemsPanel.setCollapsed(TIME_INDEX, true);
        collapsibleItemsPanel.setCollapsed(BANDS_INDEX, false);
        collapsibleItemsPanel.setCollapsed(TIE_POINT_GRIDS_INDEX, true);
        collapsibleItemsPanel.setCollapsed(FLAGS_INDEX, true);

        JScrollPane scrollPane = new JScrollPane(collapsibleItemsPanel,
                                                 ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }

    void clearSelectionInRasterTables() {
        final JTable bandsTable = (JTable) collapsibleItemsPanel.getItem(BANDS_INDEX).getComponent();
        final JTable tiePointGridsTable = (JTable) collapsibleItemsPanel.getItem(TIE_POINT_GRIDS_INDEX).getComponent();
        bandsTable.clearSelection();
        tiePointGridsTable.clearSelection();
        final RasterDataNode raster = modelUpdater.getCurrentRaster();
        if (raster != null) {
            final String rasterName = raster.getName();
            if (!selectCurrentRaster(rasterName, bandsTable)) {
                selectCurrentRaster(rasterName, tiePointGridsTable);
            }
        }
    }

    public void clearProductNodeRefs() {
        modelUpdater.clearProductNodeRefs();
        updateService.clearState();
    }

    boolean isCollapsiblePaneVisible(int index) {
        return !collapsibleItemsPanel.isCollapsed(index);
    }

    private boolean selectCurrentRaster(String rasterName, JTable table) {
        final TableModel model = table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            final String s = model.getValueAt(i, NAME_COLUMN).toString();
            if (rasterName.equals(s)) {
                table.changeSelection(i, NAME_COLUMN, false, false);
                return true;
            }
        }
        return false;
    }

    private void setShowOnlyLoadedBands(final Preferences preferences) {
        final boolean showOnlyLoadedOrDisplayedBands = preferences.getBoolean(
                PixelInfoView.PREFERENCE_KEY_SHOW_ONLY_DISPLAYED_BAND_PIXEL_VALUES,
                PixelInfoView.PREFERENCE_DEFAULT_SHOW_DISPLAYED_BAND_PIXEL_VALUES);
        displayFilter.setShowOnlyLoadedOrDisplayedBands(showOnlyLoadedOrDisplayedBands);
    }

    private void setShowPixelPosOffset1(final Preferences preferences) {
        setShowPixelPosOffset1(preferences.getBoolean(
                PREFERENCE_KEY_SHOW_PIXEL_POS_OFFSET_ONE,
                PREFERENCE_DEFAULT_SHOW_PIXEL_POS_OFFSET_1));
    }

    private void setShowPixelPosDecimals(final Preferences preferences) {
        setShowPixelPosDecimals(preferences.getBoolean(
                PREFERENCE_KEY_SHOW_PIXEL_POS_DECIMALS,
                PREFERENCE_DEFAULT_SHOW_PIXEL_POS_DECIMALS));
    }

    private void setShowGeoPosDecimals(final Preferences preferences) {
        setShowGeoPosDecimals(preferences.getBoolean(
                PREFERENCE_KEY_SHOW_GEO_POS_DECIMALS,
                PREFERENCE_DEFAULT_SHOW_GEO_POS_DECIMALS));
    }

    private static class ValueCellRenderer extends DefaultTableCellRenderer {
        Font valueFont;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (valueFont == null) {
                Font font = getFont();
                valueFont = new Font(Font.MONOSPACED, Font.PLAIN, font != null ? font.getSize() : 12);
            }
            setFont(valueFont);
            setHorizontalAlignment(RIGHT);
            return this;
        }
    }

    private static class FlagCellRenderer extends ValueCellRenderer {

        static final Color VERY_LIGHT_BLUE = new Color(230, 230, 255);
        static final Color VERY_LIGHT_RED = new Color(255, 230, 230);

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setForeground(Color.black);
            setBackground(Color.white);
            if (column == VALUE_COLUMN && value != null) {
                if (value.equals("true")) {
                    setForeground(UIUtils.COLOR_DARK_RED);
                    setBackground(VERY_LIGHT_BLUE);
                } else if (value.equals("false")) {
                    setForeground(UIUtils.COLOR_DARK_BLUE);
                    setBackground(VERY_LIGHT_RED);
                }
            }
            return this;
        }
    }

    public static class DisplayFilter {

        private final Vector<PropertyChangeListener> propertyChangeListeners = new Vector<>();
        private boolean showOnlyLoadedOrDisplayedBands;

        public void addPropertyChangeListener(PropertyChangeListener displayFilterListener) {
            if (displayFilterListener != null && !propertyChangeListeners.contains(displayFilterListener)) {
                propertyChangeListeners.add(displayFilterListener);
            }
        }

        public void removePropertyChangeListener(PropertyChangeListener displayFilterListener) {
            if (displayFilterListener != null && propertyChangeListeners.contains(displayFilterListener)) {
                propertyChangeListeners.remove(displayFilterListener);
            }
        }

        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
            final PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
            for (int i = 0; i < propertyChangeListeners.size(); i++) {
                (propertyChangeListeners.elementAt(i)).propertyChange(event);
            }
        }

        public void setShowOnlyLoadedOrDisplayedBands(boolean v) {
            if (showOnlyLoadedOrDisplayedBands != v) {
                final boolean oldValue = showOnlyLoadedOrDisplayedBands;
                showOnlyLoadedOrDisplayedBands = v;
                firePropertyChange("showOnlyLoadedOrDisplayedBands", oldValue, v);
            }
        }

        public boolean accept(ProductNode node) {
            if (node instanceof RasterDataNode) {
                final RasterDataNode rasterDataNode = (RasterDataNode) node;
                if (showOnlyLoadedOrDisplayedBands) {
                    return rasterDataNode.hasRasterData() ||
                            WindowUtilities.getOpened(ProductSceneViewTopComponent.class).anyMatch(
                                    topComponent -> rasterDataNode == topComponent.getView().getRaster());
                }
            }
            return true;
        }
    }

}
