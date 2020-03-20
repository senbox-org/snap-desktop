package org.esa.snap.rcp.actions.file;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.core.param.ParamChangeEvent;
import org.esa.snap.core.param.ParamChangeListener;
import org.esa.snap.core.param.ParamGroup;
import org.esa.snap.core.param.ParamValidateException;
import org.esa.snap.core.param.Parameter;
import org.esa.snap.core.subset.AbstractSubsetRegion;
import org.esa.snap.core.subset.GeometrySubsetRegion;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.core.util.GeoUtils;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.loading.AbstractModalDialog;
import org.esa.snap.ui.loading.LoadingIndicator;
import org.esa.snap.ui.loading.SwingUtils;
import org.locationtech.jts.geom.Geometry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 17/2/2020.
 * Updated by Denisa Stefanescu on 18/02/2020
 */
public class ProductAdvancedDialog extends AbstractModalDialog implements ParamChangeListener {

    private static final Logger logger = Logger.getLogger(ProductAdvancedDialog.class.getName());

    private static final int MIN_SCENE_VALUE = 0;
    private static final String FORMAT_PATTERN = "#0.00#";

    private final JList bandList;
    private final JList maskList;

    private JCheckBox copyMetadata;
    private final JCheckBox copyMasks;

    private final JRadioButton pixelCoordRadio;
    private final JRadioButton geoCoordRadio;

    private final JPanel pixelPanel;
    private final JPanel geoPanel;

    private JScrollPane scrollPaneMask;

    private MetadataInspector.Metadata readerInspectorExposeParameters;

    private AtomicBoolean updatingUI;

    private int productWidth;
    private int productHeight;

    private Parameter paramX1;
    private Parameter paramY1;
    private Parameter paramWidth;
    private Parameter paramHeight;
    private Parameter paramWestLon1;
    private Parameter paramEastLon2;
    private Parameter paramNorthLat1;
    private Parameter paramSouthLat2;

    private MetadataInspector metadataInspector;
    private File file;
    private ProductSubsetDef productSubsetDef;

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

        pixelPanel = new JPanel(new GridBagLayout());
        geoPanel = new JPanel(new GridBagLayout());
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
        ParamGroup pg = new ParamGroup();
        initPixelParameters(pg);
        initGeoCodingParameters(pg);
        pg.addParamChangeListener(this);
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
        ButtonGroup group = new ButtonGroup();
        group.add(pixelCoordRadio);
        group.add(geoCoordRadio);
        pixelCoordRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pixelPanel.setVisible(true);
                geoPanel.setVisible(false);
            }
        });
        geoCoordRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pixelPanel.setVisible(false);
                geoPanel.setVisible(true);
            }
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

        JPanel regionTypePanel = new JPanel(new GridLayout(1, 2));
        regionTypePanel.add(pixelCoordRadio);
        regionTypePanel.add(geoCoordRadio);

        gbc = SwingUtils.buildConstraints(0, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST, 2, 1, gapBetweenRows, 0);
        contentPanel.add(regionTypePanel, gbc);

        gbc = SwingUtils.buildConstraints(0, 4, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST, 2, 1, gapBetweenRows, 0);
        contentPanel.add(pixelPanel, gbc);

        gbc = SwingUtils.buildConstraints(0, 5, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST, 2, 1, gapBetweenRows, 0);
        contentPanel.add(geoPanel, gbc);
        geoPanel.setVisible(false);

        return contentPanel;
    }

    private void createPixelPanel(int gapBetweenColumns, int gapBetweenRows) {
        GridBagConstraints pixgbc = SwingUtils.buildConstraints(0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER, 1, 1, 0, 0);
        pixelPanel.add(new JLabel("SceneX:"), pixgbc);
        pixgbc = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 0, gapBetweenColumns);
        pixelPanel.add(UIUtils.createSpinner(paramX1, 25, "#0"),pixgbc);

        pixgbc = SwingUtils.buildConstraints(0, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        pixelPanel.add(new JLabel("SceneY:"), pixgbc);
        pixgbc = SwingUtils.buildConstraints(1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        pixelPanel.add(UIUtils.createSpinner(paramY1, 25, "#0"),pixgbc);

        pixgbc = SwingUtils.buildConstraints(0, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        pixelPanel.add(new JLabel("Scene width:"), pixgbc);
        pixgbc = SwingUtils.buildConstraints(1, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        pixelPanel.add(UIUtils.createSpinner(paramWidth, 25, "#0"),pixgbc);

        pixgbc = SwingUtils.buildConstraints(0, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        pixelPanel.add(new JLabel("Scene height:"), pixgbc);
        pixgbc = SwingUtils.buildConstraints(1, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        pixelPanel.add(UIUtils.createSpinner(paramHeight, 25, "#0"),pixgbc);
    }

    private void createGeoCodingPanel(int gapBetweenColumns, int gapBetweenRows) {
        GridBagConstraints geobc = SwingUtils.buildConstraints(0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER, 1, 1, 0, 0);
        geoPanel.add(new JLabel("North latitude bound:"), geobc);
        geobc = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 0, gapBetweenColumns);
        geoPanel.add(UIUtils.createSpinner(paramNorthLat1, 1.0, FORMAT_PATTERN),geobc);

        geobc = SwingUtils.buildConstraints(0, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        geoPanel.add(new JLabel("West longitude bound:"), geobc);
        geobc = SwingUtils.buildConstraints(1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        geoPanel.add(UIUtils.createSpinner(paramWestLon1, 1.0, FORMAT_PATTERN),geobc);

        geobc = SwingUtils.buildConstraints(0, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        geoPanel.add(new JLabel("South latitude bound:"), geobc);
        geobc = SwingUtils.buildConstraints(1, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        geoPanel.add(UIUtils.createSpinner(paramSouthLat2, 1.0, FORMAT_PATTERN),geobc);

        geobc = SwingUtils.buildConstraints(0, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        geoPanel.add(new JLabel("East longitude bound:"), geobc);
        geobc = SwingUtils.buildConstraints(1, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        geoPanel.add(UIUtils.createSpinner(paramEastLon2, 1.0, FORMAT_PATTERN),geobc);
    }

    private void initPixelParameters(ParamGroup pg){
        paramX1 = new Parameter("source_x1", MIN_SCENE_VALUE);
        paramX1.getProperties().setDescription("Start X co-ordinate given in pixels");
        paramY1 = new Parameter("source_y1", MIN_SCENE_VALUE);
        paramY1.getProperties().setDescription("Start Y co-ordinate given in pixels");
        paramWidth = new Parameter("source_width", Integer.MAX_VALUE);
        paramWidth.getProperties().setDescription("Product width");
        paramHeight = new Parameter("source_height", Integer.MAX_VALUE);
        paramHeight.getProperties().setDescription("Product height");
        pg.addParameter(paramX1);
        pg.addParameter(paramY1);
        pg.addParameter(paramWidth);
        pg.addParameter(paramHeight);
    }

    private void initGeoCodingParameters(ParamGroup pg){
        paramNorthLat1 = new Parameter("geo_lat1", 90.0);
        paramNorthLat1.getProperties().setDescription("North bound latitude");
        paramNorthLat1.getProperties().setPhysicalUnit("°");
        paramNorthLat1.getProperties().setMinValue(-90.0);
        paramNorthLat1.getProperties().setMaxValue(90.0);
        pg.addParameter(paramNorthLat1);

        paramWestLon1 = new Parameter("geo_lon1", -180.0);
        paramWestLon1.getProperties().setDescription("West bound longitude");
        paramWestLon1.getProperties().setPhysicalUnit("°");
        paramWestLon1.getProperties().setMinValue(-180.0);
        paramWestLon1.getProperties().setMaxValue(180.0);
        pg.addParameter(paramWestLon1);

        paramSouthLat2 = new Parameter("geo_lat2", -90.0);
        paramSouthLat2.getProperties().setDescription("South bound latitude");
        paramSouthLat2.getProperties().setPhysicalUnit("°");
        paramSouthLat2.getProperties().setMinValue(-90.0);
        paramSouthLat2.getProperties().setMaxValue(90.0);
        pg.addParameter(paramSouthLat2);

        paramEastLon2 = new Parameter("geo_lon2", 180.0);
        paramEastLon2.getProperties().setDescription("East bound longitude");
        paramEastLon2.getProperties().setPhysicalUnit("°");
        paramEastLon2.getProperties().setMinValue(-180.0);
        paramEastLon2.getProperties().setMaxValue(180.0);
        pg.addParameter(paramEastLon2);
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
        productWidth = result.getProductWidth();
        productHeight = result.getProductHeight();
        try {
            paramX1.getProperties().setMaxValue(result.getProductWidth() - 1 > 0 ? result.getProductWidth() - 1 : 0);
            paramY1.getProperties().setMaxValue(result.getProductHeight() - 1 > 0 ? result.getProductHeight() - 1 : 0);

            paramWidth.getProperties().setMinValue((Integer) paramX1.getValue());
            paramWidth.getProperties().setMaxValue(result.getProductWidth());
            paramWidth.setValue(result.getProductWidth());

            paramHeight.getProperties().setMinValue((Integer) paramY1.getValue());
            paramHeight.getProperties().setMaxValue(result.getProductHeight());
            paramHeight.setValue(result.getProductHeight());
            if(this.readerInspectorExposeParameters != null  && this.readerInspectorExposeParameters.isHasGeoCoding()) {
                syncLatLonWithXYParams();
            }else{
                geoCoordRadio.setEnabled(false);
                geoPanel.setEnabled(false);
            }
        } catch (ParamValidateException e) {
            logger.log(Level.SEVERE, e.getMessage());
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

    @Override
    public void parameterValueChanged(ParamChangeEvent event) {
        updateUIState(event);
    }

    private void updateUIState(ParamChangeEvent event) {
        if (updatingUI.compareAndSet(false, true)) {
            try {
                if (event != null) {
                    final String paramName = event.getParameter().getName();
                    if (paramName.startsWith("geo_") && geoCoordRadio.isEnabled()) {
                        geoCodingChange();
                    } else if (paramName.startsWith("pixel_") || paramName.startsWith("source_")) {
                        pixelPanelChanged();
                        syncLatLonWithXYParams();
                    }
                }
            } finally {
                updatingUI.set(false);
            }
        }
    }

    public void pixelPanelChanged() {
        int x1 = ((Number) paramX1.getValue()).intValue();
        int y1 = ((Number) paramY1.getValue()).intValue();
        int w = ((Number) paramWidth.getValue()).intValue();
        int h = ((Number) paramHeight.getValue()).intValue();

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
                w = w - x1;
            }
        }

        if (this.readerInspectorExposeParameters != null) {
            if (h > productHeight) {
                h = productHeight;
            }
            if (y1 + h > productHeight) {
                h = h - y1;
            }
        }

        //reset filed values when the user writes wrong values
        paramX1.setValue(0, null);
        paramY1.setValue(0, null);
        paramWidth.setValue(w, null);
        paramHeight.setValue(h, null);

        paramX1.setValue(x1, null);
        paramY1.setValue(y1, null);
        paramWidth.setValue(w, null);
        paramHeight.setValue(h, null);
    }

    private void geoCodingChange() {
        final GeoPos geoPos1 = new GeoPos((Double) paramNorthLat1.getValue(),
                                          (Double) paramWestLon1.getValue());
        final GeoPos geoPos2 = new GeoPos((Double) paramSouthLat2.getValue(),
                                          (Double) paramEastLon2.getValue());

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

            paramX1.setValue((int) finalRegion.getMinX(), ex -> true);
            paramY1.setValue((int) finalRegion.getMinY(), ex -> true);
            int width = (int)(finalRegion.getMaxX() - finalRegion.getMinX()) + 1;
            int height = (int)(finalRegion.getMaxY() - finalRegion.getMinY()) + 1;
            paramWidth.setValue(width, ex -> true);
            paramHeight.setValue(height, ex -> true);
        }
    }

    private void syncLatLonWithXYParams() {
        if (this.readerInspectorExposeParameters != null && this.readerInspectorExposeParameters.getGeoCoding() != null) {
            final PixelPos pixelPos1 = new PixelPos((Integer) paramX1.getValue(), (Integer) paramY1.getValue());
            int paramX2 = (Integer)paramWidth.getValue() + (Integer)paramX1.getValue() - 1;
            int paramY2 = (Integer)paramHeight.getValue() + (Integer)paramY1.getValue() - 1;
            final PixelPos pixelPos2 = new PixelPos(paramX2, paramY2);
            GeoCoding geoCoding = this.readerInspectorExposeParameters.getGeoCoding();

            final GeoPos geoPos1 = geoCoding.getGeoPos(pixelPos1, null);
            final GeoPos geoPos2 = geoCoding.getGeoPos(pixelPos2, null);
            if (geoPos1.isValid()) {
                double lat = geoPos1.getLat();
                lat = MathUtils.crop(lat, -90.0, 90.0);
                paramNorthLat1.setValue(lat, ex -> true);
                double lon = geoPos1.getLon();
                lon = MathUtils.crop(lon, -180.0, 180.0);
                paramWestLon1.setValue(lon, ex -> true);
            }
            if (geoPos2.isValid()) {
                double lat = geoPos2.getLat();
                lat = MathUtils.crop(lat, -90.0, 90.0);
                paramSouthLat2.setValue(lat, ex -> true);
                double lon = geoPos2.getLon();
                lon = MathUtils.crop(lon, -180.0, 180.0);
                paramEastLon2.setValue(lon, ex -> true);
            }
        }
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
        if (pixelPanel.isVisible()) {
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
            if (paramX1 != null && paramY1 != null && paramWidth != null && paramHeight != null) {
                subsetRegion = new PixelSubsetRegion(Integer.parseInt(paramX1.getValueAsText()),
                                                     Integer.parseInt(paramY1.getValueAsText()),
                                                     Integer.parseInt(paramWidth.getValueAsText()),
                                                     Integer.parseInt(paramHeight.getValueAsText()), 0);
            }
        }
        productSubsetDef.setSubsetRegion(subsetRegion);
    }

    private AbstractSubsetRegion setGeometry(){
        if(this.readerInspectorExposeParameters != null  && this.readerInspectorExposeParameters.isHasGeoCoding()) {
            final GeoPos geoPos1 = new GeoPos((Double) paramNorthLat1.getValue(),
                                              (Double) paramWestLon1.getValue());
            final GeoPos geoPos2 = new GeoPos((Double) paramSouthLat2.getValue(),
                                              (Double) paramEastLon2.getValue());
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
