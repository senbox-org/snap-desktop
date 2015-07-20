package org.esa.snap.rcp.status;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.LayerCanvas;
import org.esa.snap.framework.datamodel.GeoCoding;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.RasterDataNode;
import org.esa.snap.framework.ui.PixelPositionListener;
import org.esa.snap.framework.ui.product.ProductSceneView;
import org.esa.snap.netbeans.docwin.DocumentWindowManager;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.openide.awt.StatusLineElementProvider;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 * Displays current pixel position in the status bar.
 *
 * @author Norman Fomferra
 */
@ServiceProvider(service = StatusLineElementProvider.class, position = 10)
public class PixelPosStatusLineElementProvider
        implements StatusLineElementProvider,
        DocumentWindowManager.Listener,
        PixelPositionListener,
        PreferenceChangeListener {

    public final static String PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X = "pixel.offset.display.x";
    public final static String PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y = "pixel.offset.display.y";
    public final static String PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS = "pixel.offset.display.show.decimals";
    public final static String PROPERTY_KEY_GEOLOCATION_OFFSET_FOR_DISPLAY_SHOW_DECIMALS = "geolocation.display.decimal";

    public final static double PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_X = 0;
    public final static double PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_Y = 0;
    public final static boolean PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS = false;
    public final static boolean PROPERTY_DEFAULT_DISPLAY_GEOLOCATION_AS_DECIMAL = false;

    private static final String GEO_POS_FORMAT = "Lat %8s  Lon %8s";
    private static final String PIXEL_POS_FORMAT = "X %6s  Y %6s";
    private static final String ZOOM_LEVEL_FORMAT = "Zoom %s  Level %s";


    private final JLabel zoomLevelLabel;
    private final JLabel geoPosLabel;
    private final JLabel pixelPosLabel;
    private final JPanel panel;

    private boolean showPixelOffsetDecimals;
    private boolean showGeoPosOffsetDecimals;

    public PixelPosStatusLineElementProvider() {
        DocumentWindowManager.getDefault().addListener(this);
        SnapApp.getDefault().getPreferences().addPreferenceChangeListener(this);
        updateSettings();

        pixelPosLabel = new JLabel();
        pixelPosLabel.setPreferredSize(new Dimension(120, 20));
        pixelPosLabel.setHorizontalAlignment(SwingConstants.CENTER);

        geoPosLabel = new JLabel();
        geoPosLabel.setPreferredSize(new Dimension(200, 20));
        geoPosLabel.setHorizontalAlignment(SwingConstants.CENTER);

        zoomLevelLabel = new JLabel();
        zoomLevelLabel.setPreferredSize(new Dimension(150, 20));
        zoomLevelLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(Box.createHorizontalGlue());

        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(pixelPosLabel);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(geoPosLabel);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(zoomLevelLabel);

    }

    @Override
    public Component getStatusLineElement() {
        return panel;
    }

    @Override
    public void pixelPosChanged(ImageLayer imageLayer,
                                int pixelX,
                                int pixelY,
                                int currentLevel,
                                boolean pixelPosValid,
                                MouseEvent e) {
        if (pixelPosValid) {
            AffineTransform i2mTransform = imageLayer.getImageToModelTransform(currentLevel);
            Point2D modelP = i2mTransform.transform(new Point2D.Double(pixelX + 0.5, pixelY + 0.5), null);
            AffineTransform m2iTransform = imageLayer.getModelToImageTransform();
            Point2D imageP = m2iTransform.transform(modelP, null);

            PixelPos pixelPos = new PixelPos(imageP.getX(), imageP.getY());
            ProductSceneView productSceneView = SnapApp.getDefault().getSelectedProductSceneView();
            if (productSceneView == null) {
                setDefault();
                return;
            }

            RasterDataNode rasterDataNode = productSceneView.getRaster();
            if (rasterDataNode == null) {
                setDefault();
                return;
            }
            GeoCoding geoCoding = rasterDataNode.getGeoCoding();
            if (geoCoding == null) {
                setDefault();
                return;
            }
            GeoPos geoPos = geoCoding.getGeoPos(pixelPos, null);
            if (showGeoPosOffsetDecimals) {
                geoPosLabel.setText(String.format("Lat %.5f  Lon %.5f", geoPos.getLat(), geoPos.getLon()));
            } else {
                geoPosLabel.setText(String.format(GEO_POS_FORMAT, geoPos.getLatString(), geoPos.getLonString()));
            }

            if (showPixelOffsetDecimals) {
                pixelPosLabel.setText(String.format(PIXEL_POS_FORMAT, imageP.getX(), imageP.getY()));
            } else {
                pixelPosLabel.setText(String.format(PIXEL_POS_FORMAT, (int) Math.floor(imageP.getX()), (int) Math.floor(imageP.getY())));
            }

            LayerCanvas layerCanvas = (LayerCanvas) e.getSource();
            double zoomFactor = layerCanvas.getViewport().getZoomFactor();
            String scaleStr;
            if (zoomFactor > 1.0) {
                double v = Math.round(10.0 * zoomFactor) / 10.0;
                scaleStr = ((int) v == v ? (int) v : v) + ":1";
            } else {
                double v = Math.round(10.0 / zoomFactor) / 10.0;
                scaleStr = "1:" + ((int) v == v ? (int) v : v);
            }
            zoomLevelLabel.setText(String.format(ZOOM_LEVEL_FORMAT, scaleStr, currentLevel));

        } else {
            setDefault();

        }

    }

    private void setDefault() {
        geoPosLabel.setText(String.format(GEO_POS_FORMAT, "--", "--"));
        pixelPosLabel.setText(String.format(PIXEL_POS_FORMAT, "--", "--"));
        zoomLevelLabel.setText(String.format(ZOOM_LEVEL_FORMAT, "--", "--"));
    }


    @Override
    public void pixelPosNotAvailable() {
        setDefault();
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        // Called if SNAP preferences change, adjust any status bar setting here.
        final String propertyName = evt.getKey();
        if (PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X.equals(propertyName)
                || PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y.equals(propertyName)
                || PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS.equals(propertyName)
                || PROPERTY_KEY_GEOLOCATION_OFFSET_FOR_DISPLAY_SHOW_DECIMALS.equals(propertyName)) {
            updateSettings();
        }
    }


    @Override
    public void windowOpened(DocumentWindowManager.Event e) {
        TopComponent topComponent = e.getDocumentWindow().getTopComponent();
        if (topComponent instanceof ProductSceneViewTopComponent) {
            ProductSceneViewTopComponent component = (ProductSceneViewTopComponent) topComponent;
            component.getView().addPixelPositionListener(this);
        }
    }

    @Override
    public void windowClosed(DocumentWindowManager.Event e) {
        TopComponent topComponent = e.getDocumentWindow().getTopComponent();
        if (topComponent instanceof ProductSceneViewTopComponent) {
            ProductSceneViewTopComponent component = (ProductSceneViewTopComponent) topComponent;
            component.getView().removePixelPositionListener(this);
        }
    }

    @Override
    public void windowSelected(DocumentWindowManager.Event e) {
    }

    @Override
    public void windowDeselected(DocumentWindowManager.Event e) {
    }

    private void updateSettings() {
        final Preferences preferences = SnapApp.getDefault().getPreferences();
        //Todo @Muhammad bc
        // Will be implement for calculate the pixels in the setting.
        double pixelOffsetY = preferences.getDouble(PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y,
                                                    PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_X);

        double pixelOffsetX = preferences.getDouble(PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X,
                                                    PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_Y);
        showPixelOffsetDecimals = preferences.getBoolean(
                PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS,
                PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS);

        showGeoPosOffsetDecimals = preferences.getBoolean(
                PROPERTY_KEY_GEOLOCATION_OFFSET_FOR_DISPLAY_SHOW_DECIMALS,
                PROPERTY_DEFAULT_DISPLAY_GEOLOCATION_AS_DECIMAL);
    }
}

