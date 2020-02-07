package org.esa.snap.rcp.actions.file;

import com.vividsolutions.jts.geom.Geometry;
import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.core.dataio.ProductReaderExposedParams;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.param.ParamChangeEvent;
import org.esa.snap.core.param.ParamChangeListener;
import org.esa.snap.core.param.ParamGroup;
import org.esa.snap.core.param.Parameter;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * @author Denisa Stefanescu
 */
public class ProductAdvancedDialog extends ModalDialog implements ParamChangeListener {

    private static final int MIN_SCENE_VALUE = 0;
    private static final String FORMAT_PATTERN = "#0.00#";

    private final JList bandList = new JList();
    private final JList maskList = new JList();

    private final JTextField bandListTextField = new JTextField("");
    private final JTextField copyMasksTextField = new JTextField("");

    private JCheckBox copyMetadata = new JCheckBox("Copy Metadata", true);
    private final JCheckBox copyMasks = new JCheckBox("Copy Masks", true);

    private final JRadioButton pixelCoordRadio = new JRadioButton("Pixel Coordinates");
    private final JRadioButton geoCoordRadio = new JRadioButton("Geographic Coordinates");

    private final JPanel pixelPanel = new JPanel(new GridBagLayout());
    private final JPanel geoPanel = new JPanel(new GridBagLayout());

    private ProductReaderExposedParams readerExposedParams;

    private MetadataInspector.Metadata readerInspectorExposeParameters;

    private ProductSubsetDef productSubsetDef = null;

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

    protected Logger logger = Logger.getLogger(getClass().getName());

    public ProductAdvancedDialog(Window window, String title, ProductReaderExposedParams readerExposedParams, MetadataInspector.Metadata readerInspectorExposeParameters){
        super(window, title, ID_OK | ID_CANCEL | ID_HELP, "advancedDialog");
        this.readerExposedParams = readerExposedParams;
        this.readerInspectorExposeParameters = readerInspectorExposeParameters;
        updatingUI = new AtomicBoolean(false);
        this.productSubsetDef = new ProductSubsetDef();
        createUI();
    }

    private void setPixelCoord(){
        if (this.readerInspectorExposeParameters == null) {
            paramX1 = new Parameter("source_x1", MIN_SCENE_VALUE);
            paramX1.getProperties().setDescription("Start X co-ordinate given in pixels");
            paramX1.getProperties().setMinValue(MIN_SCENE_VALUE);
            paramX1.getProperties().setMaxValue(Integer.MAX_VALUE);
            paramWidth = new Parameter("sorce_width", Integer.MAX_VALUE);
            paramWidth.getProperties().setMinValue(MIN_SCENE_VALUE);
            paramWidth.getProperties().setDescription("Product width");
            paramWidth.getProperties().setMaxValue(Integer.MAX_VALUE);
            paramY1 = new Parameter("source_y1", MIN_SCENE_VALUE);
            paramY1.getProperties().setDescription("Start Y co-ordinate given in pixels");
            paramY1.getProperties().setMinValue(MIN_SCENE_VALUE);
            paramY1.getProperties().setMaxValue(Integer.MAX_VALUE);
            paramHeight = new Parameter("source_height", Integer.MAX_VALUE);
            paramHeight.getProperties().setMinValue(MIN_SCENE_VALUE);
            paramHeight.getProperties().setDescription("Product height");
            paramHeight.getProperties().setMaxValue(Integer.MAX_VALUE);
        }else{
            // set scene width and scene X for Pixel Coordinates
            paramX1 = new Parameter("source_x1", MIN_SCENE_VALUE);
            paramX1.getProperties().setDescription("Start X co-ordinate given in pixels");
            paramX1.getProperties().setMinValue(MIN_SCENE_VALUE);
            paramX1.getProperties().setMaxValue(this.readerInspectorExposeParameters.getProductWidth() - 1 > 0 ? this.readerInspectorExposeParameters.getProductWidth() - 1 : 0);
            paramWidth = new Parameter("source_width", this.readerInspectorExposeParameters.getProductWidth());
            paramWidth.getProperties().setMinValue((Integer) paramX1.getValue());
            paramWidth.getProperties().setDescription("Product width");
            paramWidth.getProperties().setMaxValue(this.readerInspectorExposeParameters.getProductWidth());


            // set scene height and scene Y for Pixel Coordinates
            paramY1 = new Parameter("source_y1", MIN_SCENE_VALUE);
            paramY1.getProperties().setDescription("Start Y co-ordinate given in pixels");
            paramY1.getProperties().setMinValue(MIN_SCENE_VALUE);
            paramY1.getProperties().setMaxValue(this.readerInspectorExposeParameters.getProductHeight() - 1 > 0 ? this.readerInspectorExposeParameters.getProductHeight() - 1 : 0);
            paramHeight = new Parameter("source_height", this.readerInspectorExposeParameters.getProductHeight());
            paramHeight.getProperties().setMinValue((Integer) paramY1.getValue());
            paramHeight.getProperties().setDescription("Product height");
            paramHeight.getProperties().setMaxValue(this.readerInspectorExposeParameters.getProductHeight());
        }
    }

    private void setGeoCoord(){
        if (this.readerInspectorExposeParameters != null && this.readerInspectorExposeParameters.isHasGeoCoding()) {
            ParamGroup pg = new ParamGroup();
            // set GeoCoding coordinates
            GeoPos geoPos1 = readerInspectorExposeParameters.getGeoCoding().getGeoPos(new PixelPos(0, 0), null);
            GeoPos geoPos2 = readerInspectorExposeParameters.getGeoCoding().getGeoPos(new PixelPos(this.readerInspectorExposeParameters.getProductWidth(), this.readerInspectorExposeParameters.getProductHeight()), null);
            paramNorthLat1 = new Parameter("geo_lat1", MathUtils.crop(geoPos1.getLat(), -90.0, 90.0));
            paramWestLon1 = new Parameter("geo_lon1", MathUtils.crop(geoPos1.getLon(), -180.0, 180.0));
            paramSouthLat2 = new Parameter("geo_lat2", MathUtils.crop(geoPos2.getLat(), -90.0, 90.0));
            paramEastLon2 = new Parameter("geo_lon2", MathUtils.crop(geoPos2.getLon(), -180.0, 180.0));
            paramWestLon1.getProperties().setDescription("West bound longitude");
            paramNorthLat1.getProperties().setDescription("North bound latitude");
            paramSouthLat2.getProperties().setDescription("South bound latitude");
            paramEastLon2.getProperties().setDescription("East bound longitude");
            pg.addParameter(paramNorthLat1);
            pg.addParameter(paramWestLon1);
            pg.addParameter(paramSouthLat2);
            pg.addParameter(paramEastLon2);
        }
    }

    public void createUI() {
        if (this.readerInspectorExposeParameters == null) {
            if (this.readerExposedParams != null && this.readerExposedParams.getBandNames() != null && this.readerExposedParams.getBandNames().isEmpty()) {
                // set the possible selectable values
                this.bandList.setListData(this.readerExposedParams.getBandNames().toArray());
            }
            if (this.readerExposedParams != null && this.readerExposedParams.getMaskNames() != null && this.readerExposedParams.getMaskNames().isEmpty()) {
                // set the possible selectable values
                this.maskList.setListData(this.readerExposedParams.getMaskNames().toArray());
            }
            if (this.readerExposedParams != null && !this.readerExposedParams.isHasMasks()) {
                copyMasks.setSelected(false);
            }
        } else {
            if (this.readerInspectorExposeParameters.getBandList() != null && !this.readerInspectorExposeParameters.getBandList().isEmpty()) {
                // set the possible selectable values
                this.bandList.setListData(this.readerInspectorExposeParameters.getBandList().toArray());
            }
            if (this.readerInspectorExposeParameters.isHasMasks() && this.readerInspectorExposeParameters.getMaskList() != null && !this.readerInspectorExposeParameters.getMaskList().isEmpty()) {
                // set the possible selectable values
                this.maskList.setListData(this.readerInspectorExposeParameters.getMaskList().toArray());
            }
            if (!this.readerInspectorExposeParameters.isHasMasks()) {
                copyMasks.setSelected(false);
            }
        }
        setPixelCoord();
        setGeoCoord();
        productHeight = (int) paramHeight.getValue();
        productWidth = (int) paramWidth.getValue();

        setContent(createPanel());
    }

    public void createSubsetDef (){
        if (pixelPanel.isVisible()) {
            pixelPanelChanged();
            updateSubsetDefNodeNameList(false);
        }
        if (geoPanel.isVisible() && geoCoordRadio.isEnabled()) {
            geoCodingChange();
            updateSubsetDefNodeNameList(true);
        }
    }

    @Override
    protected void onCancel() {
        super.onCancel();
    }

    /**
     * @param geoRegion if <code>true</code>, the geoCoding parameters will be send
     */
    private void updateSubsetDefNodeNameList(boolean geoRegion) {
        //if the user specify the bands that want to be added in the product add only them, else mark the fact that the product must have all the bands
        if (!bandList.isSelectionEmpty()) {
            productSubsetDef.addNodeNames((String[]) bandList.getSelectedValuesList().stream().toArray(String[]::new));
        } else if (!bandListTextField.getText().replaceAll(" ", "").equals("")) {
            if (bandListTextField.getText().contains(",")) {
                //remove all blank spaces
                bandListTextField.setText(bandListTextField.getText().replaceAll(" ", ""));
                //if there are blank values remove them
                bandListTextField.setText(bandListTextField.getText().replaceAll(",,", ","));
                //split the content by comma
                String[] bandAddedValues = bandListTextField.getText().split(",");
                //add all values into productSubsetDef
                productSubsetDef.addNodeNames(bandAddedValues);
            }
        } else {
            if(this.readerInspectorExposeParameters != null && this.readerInspectorExposeParameters.getBandList() != null){
                productSubsetDef.addNodeNames(this.readerInspectorExposeParameters.getBandList().stream().toArray(String[]::new));
            }else if(this.readerExposedParams != null && this.readerExposedParams.getBandNames() != null){
                productSubsetDef.addNodeNames(this.readerExposedParams.getBandNames().stream().toArray(String[]::new));
            }
        }

        //if the user specify the masks that want to be added in the product add only them, else mark the fact that the product must have all the masks
        if (!maskList.isSelectionEmpty()) {
            productSubsetDef.addNodeNames((String[]) maskList.getSelectedValuesList().stream().toArray(String[]::new));
        } else if (!copyMasksTextField.getText().equals("")) {
            if (copyMasksTextField.getText().contains(",")) {
                //remove all blank spaces
                copyMasksTextField.setText(copyMasksTextField.getText().replaceAll(" ", ""));
                //if there are blank values remove them
                copyMasksTextField.setText(copyMasksTextField.getText().replaceAll(",,", ","));
                //split the content by comma
                String[] maskAddedValues = copyMasksTextField.getText().split(",");
                //add all values into productSubsetDef
                productSubsetDef.addNodeNames(maskAddedValues);
            }
        } else if (copyMasks.isSelected()) {
            if(this.readerInspectorExposeParameters != null && this.readerInspectorExposeParameters.getMaskList() != null){
                productSubsetDef.addNodeNames(this.readerInspectorExposeParameters.getMaskList().stream().toArray(String[]::new));
            }else if(this.readerExposedParams != null && this.readerExposedParams.getMaskNames() != null){
                productSubsetDef.addNodeNames(this.readerExposedParams.getMaskNames().stream().toArray(String[]::new));
            }
        }
        if (!copyMetadata.isSelected()) {
            productSubsetDef.setIgnoreMetadata(true);
        }
        if(geoRegion){
            setGeometry();
        }else{
            if (paramX1 != null && paramY1 != null && paramWidth != null && paramHeight != null) {
                productSubsetDef.setRegion(new Rectangle(Integer.parseInt(paramX1.getValueAsText()),
                                                         Integer.parseInt(paramY1.getValueAsText()),
                                                         Integer.parseInt(paramWidth.getValueAsText()),
                                                         Integer.parseInt(paramHeight.getValueAsText())));
            }
        }
    }

    private void setGeometry(){
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
            Rectangle bounds = new Rectangle((int)finalRegion.getMinX(), (int)finalRegion.getMinY(), (int) finalRegion.getMaxX() - (int) finalRegion.getMinX(), (int)finalRegion.getMaxY() - (int)finalRegion.getMinY());
            Geometry geometry = ProductUtils.computeGeoRegion(geoCoding, productWidth, productHeight, bounds);
            productSubsetDef.setGeoRegion(geometry);
        }
    }

    private JComponent createPanel() {
        ParamGroup pg = new ParamGroup();
        JPanel contentPane = new JPanel(new GridBagLayout());
        JScrollPane scrollPaneMask = new JScrollPane(maskList);
        final GridBagConstraints gbc = createGridBagConstraints();

        contentPane.add(new JLabel("Source Bands:"), gbc);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        if (bandList.getModel().getSize() > 0) {
            contentPane.add(new JScrollPane(bandList), gbc);
        } else {
            contentPane.add(bandListTextField, gbc);
        }


        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(copyMetadata, gbc);

        if ((this.readerExposedParams != null && this.readerExposedParams.isHasMasks())
                || (this.readerInspectorExposeParameters != null
                && this.readerInspectorExposeParameters.isHasMasks())) {
            copyMasks.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (copyMasks.isSelected()) {
                        if (!scrollPaneMask.isVisible()) {
                            scrollPaneMask.setVisible(true);
                        }
                        if (!copyMasksTextField.isVisible()) {
                            copyMasksTextField.setVisible(true);
                        }
                    } else {
                        if (scrollPaneMask.isVisible()) {
                            maskList.clearSelection();
                            scrollPaneMask.setVisible(false);
                        }
                        if (copyMasksTextField.isVisible()) {
                            copyMasksTextField.setText(null);
                            copyMasksTextField.setVisible(false);
                        }
                    }
                }
            });
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridy++;
            contentPane.add(copyMasks, gbc);
            gbc.gridx++;
            if (maskList.getModel().getSize() > 0) {
                contentPane.add(scrollPaneMask, gbc);
            } else {
                contentPane.add(copyMasksTextField, gbc);
            }
        }
        if (this.readerExposedParams != null ||
                (this.readerInspectorExposeParameters != null && !this.readerInspectorExposeParameters.isHasGeoCoding()) ||
                        this.readerInspectorExposeParameters == null) {
            geoCoordRadio.setEnabled(false);
        }
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(pixelCoordRadio, gbc);
        gbc.gridx = 1;
        contentPane.add(geoCoordRadio, gbc);

        pixelCoordRadio.setSelected(true);
        pixelCoordRadio.setActionCommand("pixelCoordRadio");
        geoCoordRadio.setActionCommand("geoCoordRadio");
        ButtonGroup group = new ButtonGroup();
        group.add(pixelCoordRadio);
        group.add(geoCoordRadio);
        RadioListener myListener = new RadioListener();
        pixelCoordRadio.addActionListener(myListener);
        geoCoordRadio.addActionListener(myListener);

        final GridBagConstraints pixgbc = createGridBagConstraints();
        pixgbc.gridwidth = 1;
        pixgbc.fill = GridBagConstraints.BOTH;
        addComponent(pixelPanel, pixgbc, "Scene X:", UIUtils.createSpinner(paramX1, 25, "#0"), 0);

        pixgbc.gridy++;
        addComponent(pixelPanel, pixgbc, "SceneY:", UIUtils.createSpinner(paramY1, 25, "#0"), 0);
        pixgbc.gridy++;
        addComponent(pixelPanel, pixgbc, "Scene width:", UIUtils.createSpinner(paramWidth, 25, "#0"), 0);
        pixgbc.gridy++;
        addComponent(pixelPanel, pixgbc, "Scene height:", UIUtils.createSpinner(paramHeight, 25, "#0"), 0);
        pixelPanel.add(new JPanel(), pixgbc);

        final GridBagConstraints geobc = createGridBagConstraints();
        geobc.gridwidth = 1;
        geobc.fill = GridBagConstraints.BOTH;
        if(this.readerInspectorExposeParameters != null && this.readerInspectorExposeParameters.isHasGeoCoding()) {
            addComponent(geoPanel, geobc, "North latitude bound:", UIUtils.createSpinner(paramNorthLat1, 1.0, FORMAT_PATTERN), 0);
            geobc.gridy++;
            addComponent(geoPanel, geobc, "West longitude bound:", UIUtils.createSpinner(paramWestLon1, 1.0, FORMAT_PATTERN), 0);
            geobc.gridy++;
            addComponent(geoPanel, geobc, "South latitude bound:", UIUtils.createSpinner(paramSouthLat2, 1.0, FORMAT_PATTERN), 0);
            geobc.gridy++;
            addComponent(geoPanel, geobc, "East longitude bound:", UIUtils.createSpinner(paramEastLon2, 1.0, FORMAT_PATTERN), 0);
            geoPanel.add(new JPanel(), geobc);
        }

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridy++;
        contentPane.add(pixelPanel, gbc);
        geoPanel.setVisible(false);
        contentPane.add(geoPanel, gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        pg.addParameter(paramX1);
        pg.addParameter(paramY1);
        pg.addParameter(paramWidth);
        pg.addParameter(paramHeight);
        pg.addParameter(paramEastLon2);
        pg.addParameter(paramNorthLat1);
        pg.addParameter(paramSouthLat2);
        pg.addParameter(paramWestLon1);
        pg.addParamChangeListener(this);

        contentPane.add(new JPanel(), gbc);
        return contentPane;
    }

    private GridBagConstraints createGridBagConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 3, 0, 3);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets.top = 1;
        gbc.insets.bottom = 1;
        gbc.insets.right = 1;
        gbc.insets.left = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        return gbc;
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

    /**
     * Called if the value of a parameter changed.
     *
     * @param event the parameter change event
     */
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
                int x1 = ((Number) paramX1.getValue()).intValue();
                int y1 = ((Number) paramY1.getValue()).intValue();
                int x2 = ((Number) paramWidth.getValue()).intValue();
                int y2 = ((Number) paramHeight.getValue()).intValue();

                productSubsetDef.setRegion(x1, y1, x2, y2);
            } finally {
                updatingUI.set(false);
            }
        }
    }

    private class RadioListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().contains("pixelCoordRadio")) {
                pixelPanel.setVisible(true);
                geoPanel.setVisible(false);
            } else {
                pixelPanel.setVisible(false);
                geoPanel.setVisible(true);
            }
        }
    }

    public ProductSubsetDef getProductSubsetDef() {
        return productSubsetDef;
    }

    public static Path convertInputToPath(Object input) {
        if (input == null) {
            throw new NullPointerException();
        } else if (input instanceof File) {
            return ((File) input).toPath();
        } else if (input instanceof Path) {
            return (Path) input;
        } else if (input instanceof String) {
            return Paths.get((String) input);
        } else {
            throw new IllegalArgumentException("Unknown input '" + input + "'.");
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

            paramX1.setValue((int) finalRegion.getMinX(), null);
            paramY1.setValue((int) finalRegion.getMinY(), null);
            int width = (int) finalRegion.getMaxX() - (int) finalRegion.getMinX();
            int height = (int) finalRegion.getMaxY() - (int) finalRegion.getMinY();
            paramWidth.setValue(width, null);
            paramHeight.setValue(height, null);
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
                paramNorthLat1.setValue(lat, null);
                double lon = geoPos1.getLon();
                lon = MathUtils.crop(lon, -180.0, 180.0);
                paramWestLon1.setValue(lon, null);
            }
            if (geoPos2.isValid()) {
                double lat = geoPos2.getLat();
                lat = MathUtils.crop(lat, -90.0, 90.0);
                paramSouthLat2.setValue(lat, null);
                double lon = geoPos2.getLon();
                lon = MathUtils.crop(lon, -180.0, 180.0);
                paramEastLon2.setValue(lon, null);
            }
        }
    }


}
