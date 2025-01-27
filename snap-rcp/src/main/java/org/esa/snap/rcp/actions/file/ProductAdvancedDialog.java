package org.esa.snap.rcp.actions.file;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.core.subset.AbstractSubsetRegion;
import org.esa.snap.core.subset.GeometrySubsetRegion;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.core.util.GeoUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.ui.loading.AbstractModalDialog;
import org.esa.snap.ui.loading.LoadingIndicator;
import org.esa.snap.ui.loading.SwingUtils;
import org.esa.snap.ui.product.ProductSubsetByPolygonUiComponents;
import org.locationtech.jts.geom.Geometry;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by jcoravu on 17/2/2020.
 * Updated by Denisa Stefanescu on 18/02/2020
 * Updated by Oana H. on 18/03/2020 in order to replace the deprecated Parameter API
 */
public class ProductAdvancedDialog extends AbstractModalDialog {

    private static final int MIN_SCENE_VALUE = 0;

    private final JList bandList;
    private final JList maskList;

    private JCheckBox copyMetadata;
    private final JCheckBox copyMasks;

    private final JRadioButton pixelCoordRadio;
    private final JRadioButton geoCoordRadio;
    private final JRadioButton vectorFileRadio;

    private final JPanel pixelPanel;
    private final JPanel geoPanel;
    private final JPanel vectorFilePanel;

    private JScrollPane scrollPaneMask;

    private MetadataInspector.Metadata readerInspectorExposeParameters;

    private AtomicBoolean updatingUI;

    private int productWidth;
    private int productHeight;

    private JSpinner pixelCoordXSpinner;
    private JSpinner pixelCoordYSpinner;
    private JSpinner pixelCoordWidthSpinner;
    private JSpinner pixelCoordHeightSpinner;
    private JSpinner geoCoordWestLongSpinner;
    private JSpinner geoCoordEastLongSpinner;
    private JSpinner geoCoordNorthLatSpinner;
    private JSpinner geoCoordSouthLatSpinner;

    private MetadataInspector metadataInspector;
    private File file;
    private ProductSubsetDef productSubsetDef;
    private final ProductSubsetByPolygonUiComponents productSubsetByPolygonUiComponents = new ProductSubsetByPolygonUiComponents(this.getJDialog());

    public ProductAdvancedDialog(Window parent, String title, MetadataInspector metadataInspector, File file) {
        super(parent, title, true, null);

        updatingUI = new AtomicBoolean(false);
        this.metadataInspector = metadataInspector;
        this.file = file;

        bandList = new JList();
        maskList = new JList();

        copyMetadata = new JCheckBox("Copy Metadata", true);
        copyMasks = new JCheckBox("Copy Masks", true);

        pixelCoordRadio = new JRadioButton("Pixel Coordinates");
        geoCoordRadio = new JRadioButton("Geographic Coordinates");
        vectorFileRadio = new JRadioButton("Polygon");

        pixelPanel = new JPanel(new GridBagLayout());
        geoPanel = new JPanel(new GridBagLayout());
        vectorFilePanel = productSubsetByPolygonUiComponents.getImportVectorFilePanel();
    }

    @Override
    protected void onAboutToShow() {
        readProductMetadataAsync();
    }

    @Override
    protected JPanel buildButtonsPanel(ActionListener cancelActionListener) {
        ActionListener okActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                okButtonPressed();
            }
        };
        return buildButtonsPanel("Ok", okActionListener, "Cancel", cancelActionListener);
    }

    @Override
    protected JPanel buildContentPanel(int gapBetweenColumns, int gapBetweenRows) {
        initPixelCoordUIComponents();
        initGeoCoordUIComponents();
        copyMasks.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (copyMasks.isSelected()) {
                    if (!scrollPaneMask.isVisible()) {
                        scrollPaneMask.setVisible(true);
                    }
                } else {
                    if (scrollPaneMask.isVisible()) {
                        maskList.clearSelection();
                        scrollPaneMask.setVisible(false);
                    }
                }
            }
        });

        scrollPaneMask = new JScrollPane(maskList);

        createPixelPanel(gapBetweenColumns, gapBetweenRows);
        createGeoCodingPanel(gapBetweenColumns, gapBetweenRows);

        pixelCoordRadio.setSelected(true);
        pixelCoordRadio.setActionCommand("pixelCoordRadio");
        geoCoordRadio.setActionCommand("geoCoordRadio");
        vectorFileRadio.setActionCommand("vectorFileRadio");
        ButtonGroup group = new ButtonGroup();
        group.add(pixelCoordRadio);
        group.add(geoCoordRadio);
        group.add(vectorFileRadio);
        pixelCoordRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pixelPanel.setVisible(true);
                geoPanel.setVisible(false);
                vectorFilePanel.setVisible(false);
            }
        });
        geoCoordRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pixelPanel.setVisible(false);
                geoPanel.setVisible(true);
                vectorFilePanel.setVisible(false);
            }
        });
        vectorFileRadio.addActionListener(e -> {
            pixelPanel.setVisible(false);
            geoPanel.setVisible(false);
            vectorFilePanel.setVisible(true);
        });

        JPanel contentPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST, 1, 1, 0, 0);
        contentPanel.add(new JLabel("Source Bands:"), gbc);
        gbc = SwingUtils.buildConstraints(1, 0, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST, 1, 1, 0, gapBetweenColumns);
        contentPanel.add(new JScrollPane(bandList), gbc);

        gbc = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST, 1, 1, gapBetweenRows, 0);
        contentPanel.add(copyMetadata, gbc);

        gbc = SwingUtils.buildConstraints(0, 2, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST, 1, 1, gapBetweenRows, 0);
        contentPanel.add(copyMasks, gbc);
        gbc = SwingUtils.buildConstraints(1, 2, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        contentPanel.add(scrollPaneMask, gbc);

        JPanel regionTypePanel = new JPanel(new GridLayout(1, 3));
        regionTypePanel.add(pixelCoordRadio);
        regionTypePanel.add(geoCoordRadio);
        regionTypePanel.add(vectorFileRadio);

        gbc = SwingUtils.buildConstraints(0, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST, 2, 1, gapBetweenRows, 0);
        contentPanel.add(regionTypePanel, gbc);

        gbc = SwingUtils.buildConstraints(0, 4, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST, 2, 1, gapBetweenRows, 0);
        contentPanel.add(pixelPanel, gbc);

        gbc = SwingUtils.buildConstraints(0, 5, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST, 2, 1, gapBetweenRows, 0);
        contentPanel.add(geoPanel, gbc);
        geoPanel.setVisible(false);

        gbc = SwingUtils.buildConstraints(0, 6, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST, 2, 1, gapBetweenRows, 0);
        contentPanel.add(vectorFilePanel, gbc);
        vectorFilePanel.setVisible(false);
        return contentPanel;
    }

    private void createPixelPanel(int gapBetweenColumns, int gapBetweenRows) {
        GridBagConstraints pixgbc = SwingUtils.buildConstraints(0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER, 1, 1, 0, 0);
        pixelPanel.add(new JLabel("SceneX:"), pixgbc);
        pixgbc = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 0, gapBetweenColumns);
        pixelPanel.add(pixelCoordXSpinner, pixgbc);

        pixgbc = SwingUtils.buildConstraints(0, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        pixelPanel.add(new JLabel("SceneY:"), pixgbc);
        pixgbc = SwingUtils.buildConstraints(1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        pixelPanel.add(pixelCoordYSpinner, pixgbc);

        pixgbc = SwingUtils.buildConstraints(0, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        pixelPanel.add(new JLabel("Scene width:"), pixgbc);
        pixgbc = SwingUtils.buildConstraints(1, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        pixelPanel.add(pixelCoordWidthSpinner, pixgbc);

        pixgbc = SwingUtils.buildConstraints(0, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        pixelPanel.add(new JLabel("Scene height:"), pixgbc);
        pixgbc = SwingUtils.buildConstraints(1, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        pixelPanel.add(pixelCoordHeightSpinner, pixgbc);
    }

    private void createGeoCodingPanel(int gapBetweenColumns, int gapBetweenRows) {
        GridBagConstraints geobc = SwingUtils.buildConstraints(0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER, 1, 1, 0, 0);
        geoPanel.add(new JLabel("North latitude bound:"), geobc);
        geobc = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 0, gapBetweenColumns);
        geoPanel.add(geoCoordNorthLatSpinner, geobc);

        geobc = SwingUtils.buildConstraints(0, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        geoPanel.add(new JLabel("West longitude bound:"), geobc);
        geobc = SwingUtils.buildConstraints(1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        geoPanel.add(geoCoordWestLongSpinner, geobc);

        geobc = SwingUtils.buildConstraints(0, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        geoPanel.add(new JLabel("South latitude bound:"), geobc);
        geobc = SwingUtils.buildConstraints(1, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        geoPanel.add(geoCoordSouthLatSpinner, geobc);

        geobc = SwingUtils.buildConstraints(0, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        geoPanel.add(new JLabel("East longitude bound:"), geobc);
        geobc = SwingUtils.buildConstraints(1, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        geoPanel.add(geoCoordEastLongSpinner, geobc);
    }

    /**
     * Creates the UI components for the Pixel Coordinates panel
     */
    private void initPixelCoordUIComponents(){
        pixelCoordXSpinner = new JSpinner(new SpinnerNumberModel(0,0, Integer.MAX_VALUE, 25));
        pixelCoordXSpinner.getModel().setValue(MIN_SCENE_VALUE);
        pixelCoordXSpinner.setToolTipText("Start X co-ordinate given in pixels");
        pixelCoordXSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                updateUIStatePixelCoordsChanged(event);
            }
        });

        pixelCoordYSpinner = new JSpinner(new SpinnerNumberModel(0,0, Integer.MAX_VALUE, 25));
        pixelCoordYSpinner.getModel().setValue(MIN_SCENE_VALUE);
        pixelCoordYSpinner.setToolTipText("Start Y co-ordinate given in pixels");
        pixelCoordYSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                updateUIStatePixelCoordsChanged(event);
            }
        });

        pixelCoordWidthSpinner = new JSpinner(new SpinnerNumberModel(0,0, Integer.MAX_VALUE, 25));
        pixelCoordWidthSpinner.getModel().setValue(Integer.MAX_VALUE);
        pixelCoordWidthSpinner.setToolTipText("Product width");
        pixelCoordWidthSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                updateUIStatePixelCoordsChanged(event);
            }
        });

        pixelCoordHeightSpinner = new JSpinner(new SpinnerNumberModel(0,0, Integer.MAX_VALUE, 25));
        pixelCoordHeightSpinner.getModel().setValue(Integer.MAX_VALUE);
        pixelCoordHeightSpinner.setToolTipText("Product height");
        pixelCoordHeightSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                updateUIStatePixelCoordsChanged(event);
            }
        });
    }

    /**
     * Creates the UI components for the Geographical Coordinates panel
     */
    private void initGeoCoordUIComponents(){
        geoCoordNorthLatSpinner = new JSpinner(new SpinnerNumberModel(0.0,-90.0, 90.0, 1.0));
        geoCoordNorthLatSpinner.getModel().setValue(90.0);
        geoCoordNorthLatSpinner.setToolTipText("North bound latitude (°)");
        geoCoordNorthLatSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                updateUIStateGeoCoordsChanged(event);
            }
        });

        geoCoordWestLongSpinner = new JSpinner(new SpinnerNumberModel(0.0,-180.0, 180.0, 1.0));
        geoCoordWestLongSpinner.getModel().setValue(-180.0);
        geoCoordWestLongSpinner.setToolTipText("West bound longitude (°)");
        geoCoordWestLongSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                updateUIStateGeoCoordsChanged(event);
            }
        });

        geoCoordSouthLatSpinner = new JSpinner(new SpinnerNumberModel(0.0,-90.0, 90.0, 1.0));
        geoCoordSouthLatSpinner.getModel().setValue(-90.0);
        geoCoordSouthLatSpinner.setToolTipText("South bound latitude (°)");
        geoCoordSouthLatSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                updateUIStateGeoCoordsChanged(event);
            }
        });

        geoCoordEastLongSpinner = new JSpinner(new SpinnerNumberModel(0.0,-180.0, 180.0, 1.0));
        geoCoordEastLongSpinner.getModel().setValue(180.0);
        geoCoordEastLongSpinner.setToolTipText("East bound longitude (°)");
        geoCoordEastLongSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                updateUIStateGeoCoordsChanged(event);
            }
        });
    }

    private void okButtonPressed() {
        createSubsetDef();
        getJDialog().dispose();
    }

    private void readProductMetadataAsync() {
        LoadingIndicator loadingIndicator = getLoadingIndicator();
        int threadId = getNewCurrentThreadId();
        ReadProductInspectorTimerRunnable runnable = new ReadProductInspectorTimerRunnable(loadingIndicator, threadId, metadataInspector, file.toPath()) {
            @Override
            protected void onSuccessfullyFinish(MetadataInspector.Metadata result) {
                onSuccessfullyLoadingProductMetadata(result);
            }

            @Override
            protected void onFailed(Exception exception) {
                onFailedLoadingProductMetadata(exception);
            }
        };
        runnable.executeAsync();
    }

    private void onSuccessfullyLoadingProductMetadata(MetadataInspector.Metadata result){
        this.readerInspectorExposeParameters = result;
        this.productSubsetByPolygonUiComponents.setTargetProductMetadata(result);
        productWidth = result.getProductWidth();
        productHeight = result.getProductHeight();

        ((SpinnerNumberModel)pixelCoordXSpinner.getModel()).setMaximum(result.getProductWidth() - 1 > 0 ? result.getProductWidth() - 1 : 0);

        ((SpinnerNumberModel)pixelCoordYSpinner.getModel()).setMaximum(result.getProductHeight() - 1 > 0 ? result.getProductHeight() - 1 : 0);

        ((SpinnerNumberModel)pixelCoordWidthSpinner.getModel()).setMinimum((Integer) pixelCoordXSpinner.getValue());
        ((SpinnerNumberModel)pixelCoordWidthSpinner.getModel()).setMaximum(result.getProductWidth());
        pixelCoordWidthSpinner.setValue(result.getProductWidth());

        ((SpinnerNumberModel)pixelCoordHeightSpinner.getModel()).setMinimum((Integer) pixelCoordYSpinner.getValue());
        ((SpinnerNumberModel)pixelCoordHeightSpinner.getModel()).setMaximum(result.getProductHeight());
        pixelCoordHeightSpinner.setValue(result.getProductHeight());

        if(this.readerInspectorExposeParameters != null  && this.readerInspectorExposeParameters.isHasGeoCoding()) {
            syncLatLonWithXYParams();
        }else{
            geoCoordRadio.setEnabled(false);
            geoPanel.setEnabled(false);
        }

        this.bandList.setListData(result.getBandList().toArray());
        this.maskList.setListData(result.getMaskList().toArray());
        if (!result.isHasMasks()) {
            copyMasks.setSelected(false);
            copyMasks.setEnabled(false);
            scrollPaneMask.setEnabled(false);
        }
    }

    private void onFailedLoadingProductMetadata(Exception exception) {
        showErrorDialog("Failed to load the product metadata", "Loading metadata");
        getJDialog().dispose();
    }

    private void updateUIStatePixelCoordsChanged(ChangeEvent event) {
        if (updatingUI.compareAndSet(false, true)) {
            try {
                if (event != null && pixelCoordRadio.isEnabled()) {
                    pixelPanelChanged();
                    syncLatLonWithXYParams();
                }
            } finally {
                updatingUI.set(false);
            }
        }
    }

    private void updateUIStateGeoCoordsChanged(ChangeEvent event) {
        if (updatingUI.compareAndSet(false, true)) {
            try {
                if (event != null && geoCoordRadio.isEnabled()) {
                    geoCodingChange();
                }
            } finally {
                updatingUI.set(false);
            }
        }
    }

    public void pixelPanelChanged() {
        int x1 = ((Number) pixelCoordXSpinner.getValue()).intValue();
        int y1 = ((Number) pixelCoordYSpinner.getValue()).intValue();
        int w = ((Number) pixelCoordWidthSpinner.getValue()).intValue();
        int h = ((Number) pixelCoordHeightSpinner.getValue()).intValue();

        if (x1 < 0) {
            x1 = 0;
        }
        if (x1 > productWidth - 2) {
            x1 = productWidth - 2;
        }
        if (y1 < 0) {
            y1 = 0;
        }
        if (y1 > productHeight - 2) {
            y1 = productHeight - 2;
        }

        if (this.readerInspectorExposeParameters != null) {
            if (w > productWidth) {
                w = productWidth;
            }
            if (x1 + w > productWidth) {
                if( (w - x1) >= 2) {
                    w = w - x1;
                }
                else {
                    w = productWidth - x1;
                }
            }
        }

        if (this.readerInspectorExposeParameters != null) {
            if (h > productHeight) {
                h = productHeight;
            }
            if (y1 + h > productHeight) {
                if (h - y1 >= 2) {
                    h = h - y1;
                }
                else {
                    h = productHeight - y1;
                }

            }
        }

        //reset fields values when the user writes wrong values
        pixelCoordXSpinner.setValue(0);
        pixelCoordYSpinner.setValue(0);
        pixelCoordWidthSpinner.setValue(w);
        pixelCoordHeightSpinner.setValue(h);

        pixelCoordXSpinner.setValue(x1);
        pixelCoordYSpinner.setValue(y1);
        pixelCoordWidthSpinner.setValue(w);
        pixelCoordHeightSpinner.setValue(h);
    }

    private void geoCodingChange() {
        final GeoPos geoPos1 = new GeoPos((Double) geoCoordNorthLatSpinner.getValue(),
                (Double) geoCoordWestLongSpinner.getValue());
        final GeoPos geoPos2 = new GeoPos((Double) geoCoordSouthLatSpinner.getValue(),
                (Double) geoCoordEastLongSpinner.getValue());

        updateXYParams(geoPos1, geoPos2);
    }

    private void updateXYParams(GeoPos geoPos1, GeoPos geoPos2) {
        GeoCoding geoCoding;
        if (this.readerInspectorExposeParameters != null && this.readerInspectorExposeParameters.getGeoCoding() != null) {
            geoCoding = this.readerInspectorExposeParameters.getGeoCoding();
            final PixelPos pixelPos1 = geoCoding.getPixelPos(geoPos1, null);
            if (!pixelPos1.isValid()) {
                pixelPos1.setLocation(0, 0);
            }
            final PixelPos pixelPos2 = geoCoding.getPixelPos(geoPos2, null);
            if (!pixelPos2.isValid()) {
                pixelPos2.setLocation(this.readerInspectorExposeParameters.getProductWidth(),
                        this.readerInspectorExposeParameters.getProductHeight());
            }

            final Rectangle.Float region = new Rectangle.Float();
            region.setFrameFromDiagonal(pixelPos1.x, pixelPos1.y, pixelPos2.x, pixelPos2.y);
            final Rectangle.Float productBounds;

            productBounds = new Rectangle.Float(0, 0,
                    this.readerInspectorExposeParameters.getProductWidth(),
                    this.readerInspectorExposeParameters.getProductHeight());

            Rectangle2D finalRegion = productBounds.createIntersection(region);

            if(isValueInNumericSpinnerRange(pixelCoordXSpinner, (int) finalRegion.getMinX())){
                pixelCoordXSpinner.setValue((int) finalRegion.getMinX());
            }
            if(isValueInNumericSpinnerRange(pixelCoordYSpinner, (int) finalRegion.getMinY())){
                pixelCoordYSpinner.setValue((int) finalRegion.getMinY());
            }
            int width = (int)(finalRegion.getMaxX() - finalRegion.getMinX()) + 1;
            int height = (int)(finalRegion.getMaxY() - finalRegion.getMinY()) + 1;
            if(isValueInNumericSpinnerRange(pixelCoordWidthSpinner, width)){
                pixelCoordWidthSpinner.setValue(width);
            }
            if(isValueInNumericSpinnerRange(pixelCoordHeightSpinner, height)){
                pixelCoordHeightSpinner.setValue(height);
            }
        }
    }

    private void syncLatLonWithXYParams() {
        if (this.readerInspectorExposeParameters != null && this.readerInspectorExposeParameters.getGeoCoding() != null) {
            final PixelPos pixelPos1 = new PixelPos((Integer) pixelCoordXSpinner.getValue(), (Integer) pixelCoordYSpinner.getValue());
            int paramX2 = (Integer)pixelCoordWidthSpinner.getValue() + (Integer)pixelCoordXSpinner.getValue() - 1;
            int paramY2 = (Integer)pixelCoordHeightSpinner.getValue() + (Integer)pixelCoordYSpinner.getValue() - 1;
            final PixelPos pixelPos2 = new PixelPos(paramX2, paramY2);
            GeoCoding geoCoding = this.readerInspectorExposeParameters.getGeoCoding();

            final GeoPos geoPos1 = geoCoding.getGeoPos(pixelPos1, null);
            final GeoPos geoPos2 = geoCoding.getGeoPos(pixelPos2, null);
            if (geoPos1.isValid()) {
                double lat = geoPos1.getLat();
                lat = MathUtils.crop(lat, -90.0, 90.0);
                geoCoordNorthLatSpinner.setValue(lat);
                double lon = geoPos1.getLon();
                lon = MathUtils.crop(lon, -180.0, 180.0);
                geoCoordWestLongSpinner.setValue(lon);
            }
            if (geoPos2.isValid()) {
                double lat = geoPos2.getLat();
                lat = MathUtils.crop(lat, -90.0, 90.0);
                geoCoordSouthLatSpinner.setValue(lat);
                double lon = geoPos2.getLon();
                lon = MathUtils.crop(lon, -180.0, 180.0);
                geoCoordEastLongSpinner.setValue(lon);
            }
        }
    }

    private boolean isValueInNumericSpinnerRange(JSpinner spinner, Integer value){
        final Integer min = (Integer)((SpinnerNumberModel)spinner.getModel()).getMinimum();
        final Integer max = (Integer)((SpinnerNumberModel)spinner.getModel()).getMaximum();
        if (value >= min && value <= max){
            return true;
        }

        return false;
    }

    public static JLabel addComponent(JPanel contentPane, GridBagConstraints gbc, String text, JComponent component, int pos) {
        gbc.gridx = pos;
        gbc.weightx = 0.5;
        final JLabel label = new JLabel(text);
        contentPane.add(label, gbc);
        gbc.gridx = pos + 1;
        gbc.weightx = 2.0;
        contentPane.add(component, gbc);
        gbc.gridx = pos;
        gbc.weightx = 1.0;
        return label;
    }

    private void createSubsetDef() {
        if (pixelPanel.isVisible() || vectorFilePanel.isVisible()) {
            updateSubsetDefNodeNameList(false);
        } else if (geoPanel.isVisible() && geoCoordRadio.isEnabled()) {
            updateSubsetDefNodeNameList(true);
        }
    }

    /**
     * @param geoRegion if <code>true</code>, the geoCoding parameters will be send
     */
    private void updateSubsetDefNodeNameList(boolean geoRegion) {
        productSubsetDef = new ProductSubsetDef();
        //if the user specify the bands that want to be added in the product add only them, else mark the fact that the product must have all the bands
        if (!bandList.isSelectionEmpty()) {
            productSubsetDef.addNodeNames((String[]) bandList.getSelectedValuesList().stream().toArray(String[]::new));
        } else {
            if(this.readerInspectorExposeParameters != null && this.readerInspectorExposeParameters.getBandList() != null){
                productSubsetDef.addNodeNames(this.readerInspectorExposeParameters.getBandList().stream().toArray(String[]::new));
            }
        }

        //if the user specify the masks that want to be added in the product add only them, else mark the fact that the product must have all the masks
        if (!maskList.isSelectionEmpty()) {
            productSubsetDef.addNodeNames((String[]) maskList.getSelectedValuesList().stream().toArray(String[]::new));
        } else if (copyMasks.isSelected()) {
            if(this.readerInspectorExposeParameters != null && this.readerInspectorExposeParameters.getMaskList() != null){
                productSubsetDef.addNodeNames(this.readerInspectorExposeParameters.getMaskList().stream().toArray(String[]::new));
            }
        }
        if (!copyMetadata.isSelected()) {
            productSubsetDef.setIgnoreMetadata(true);
        }
        AbstractSubsetRegion subsetRegion = null;
        if(geoRegion){
            subsetRegion = setGeometry();
        }else{
            if (pixelCoordXSpinner.getValue() != null && pixelCoordYSpinner.getValue() != null &&
                    pixelCoordWidthSpinner.getValue() != null && pixelCoordHeightSpinner.getValue() != null) {
                subsetRegion = new PixelSubsetRegion(((Integer)pixelCoordXSpinner.getValue()),
                        ((Integer)pixelCoordYSpinner.getValue()),
                        ((Integer)pixelCoordWidthSpinner.getValue()),
                        ((Integer)pixelCoordHeightSpinner.getValue()), 0);
            }
        }
        if (vectorFileRadio.isSelected() && productSubsetByPolygonUiComponents.getProductSubsetByPolygon().isLoaded()) {
            productSubsetDef.setSubsetPolygon(productSubsetByPolygonUiComponents.getProductSubsetByPolygon().getSubsetPolygon());
            final Rectangle subsetFromVectorFileExtent = productSubsetByPolygonUiComponents.getProductSubsetByPolygon().getExtentOfPolygon();
            if (subsetFromVectorFileExtent != null) {
                subsetRegion = new PixelSubsetRegion(subsetFromVectorFileExtent.x, subsetFromVectorFileExtent.y, subsetFromVectorFileExtent.width, subsetFromVectorFileExtent.height, 0);
            }
        }
        productSubsetDef.setSubsetRegion(subsetRegion);
    }

    private AbstractSubsetRegion setGeometry(){
        if(this.readerInspectorExposeParameters != null  && this.readerInspectorExposeParameters.isHasGeoCoding()) {
            final GeoPos geoPos1 = new GeoPos((Double) geoCoordNorthLatSpinner.getValue(),
                    (Double) geoCoordWestLongSpinner.getValue());
            final GeoPos geoPos2 = new GeoPos((Double) geoCoordSouthLatSpinner.getValue(),
                    (Double) geoCoordEastLongSpinner.getValue());
            GeoCoding geoCoding = this.readerInspectorExposeParameters.getGeoCoding();
            final PixelPos pixelPos1 = geoCoding.getPixelPos(geoPos1, null);
            final PixelPos pixelPos2 = geoCoding.getPixelPos(geoPos2, null);

            final Rectangle.Float region = new Rectangle.Float();
            region.setFrameFromDiagonal(pixelPos1.x, pixelPos1.y, pixelPos2.x, pixelPos2.y);
            final Rectangle.Float productBounds = new Rectangle.Float(0, 0, productWidth, productHeight);
            Rectangle2D finalRegion = productBounds.createIntersection(region);
            Rectangle bounds = new Rectangle((int)finalRegion.getMinX(), (int)finalRegion.getMinY(), (int)(finalRegion.getMaxX() - finalRegion.getMinX()) + 1, (int)(finalRegion.getMaxY() - finalRegion.getMinY()) + 1);
            Geometry geometry = GeoUtils.computeGeometryUsingPixelRegion(geoCoding, bounds);
            return new GeometrySubsetRegion(geometry, 0);
        }
        return null;
    }

    public ProductSubsetDef getProductSubsetDef() {
        return productSubsetDef;
    }
}
