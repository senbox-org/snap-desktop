package org.esa.snap.rcp.status;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.LayerCanvas;
import eu.esa.snap.netbeans.docwin.DocumentWindowManager;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.math.SphericalDistance;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.PixelPositionListener;
import org.esa.snap.ui.product.ProductSceneView;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.datum.Ellipsoid;
import org.openide.awt.StatusLineElementProvider;
import org.openide.util.lookup.ServiceProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import static org.esa.snap.rcp.pixelinfo.PixelInfoView.*;

/**
 * Displays current pixel position in the status bar.
 *
 * @author Norman Fomferra
 */
@ServiceProvider(service = StatusLineElementProvider.class, position = 10)
public class PixelPosStatusLineElementProvider
        implements StatusLineElementProvider,
        DocumentWindowManager.Listener<Object, ProductSceneView>,
        PixelPositionListener,
        PreferenceChangeListener {

    private static final String GEO_POS_FORMAT = "Lat %8s  Lon %8s";
    private static final String PIXEL_POS_FORMAT = "X %6s  Y %6s";
    private static final String ZOOM_LEVEL_FORMAT = "Zoom %s  Level %s";
    private static final String PIXEL_SIZE_FORMAT = "Pixel Spacing: %s m %s m";

    private final JLabel zoomLevelLabel;
    private final JLabel geoPosLabel;
    private final JLabel pixelPosLabel;
    private final JLabel pixelSpacingLabel;
    private final JLabel scaleLabel;
    private final JPanel panel;

    private boolean showPixelOffsetDecimals;
    private boolean showGeoPosOffsetDecimals;
    private final DecimalFormatSymbols formatSymbols;
    private final DecimalFormat decimalFormat;
    private double longitudeResolutionInMeter;
    private double latitudeResolutionInMeter;

    public PixelPosStatusLineElementProvider() {
        DocumentWindowManager.getDefault().addListener(DocumentWindowManager.Predicate.view(ProductSceneView.class), this);
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

        pixelSpacingLabel = new JLabel();
        pixelSpacingLabel.setPreferredSize(new Dimension(230, 20));
        pixelSpacingLabel.setHorizontalAlignment(SwingConstants.CENTER);

        scaleLabel = new JLabel();
        scaleLabel.setPreferredSize(new Dimension(180, 20));
        scaleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(Box.createHorizontalGlue());

        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(pixelPosLabel);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(geoPosLabel);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(zoomLevelLabel);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(pixelSpacingLabel);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(scaleLabel);
        formatSymbols = new DecimalFormatSymbols();
        formatSymbols.setDecimalSeparator('.');
        decimalFormat = new DecimalFormat("#.##", formatSymbols);
        longitudeResolutionInMeter = Double.NaN;
        latitudeResolutionInMeter = Double.NaN;
    }

    private void computeResolution() {
        longitudeResolutionInMeter = Double.NaN;
        latitudeResolutionInMeter = Double.NaN;
        ProductSceneView productSceneView = SnapApp.getDefault().getSelectedProductSceneView();
        if (productSceneView == null) {
            return;
        }

        RasterDataNode rasterDataNode = productSceneView.getRaster();
        if (rasterDataNode == null) {
            return;
        }
        GeoCoding geoCoding = rasterDataNode.getGeoCoding();
        if (geoCoding == null) {
            return;
        }

        if (geoCoding instanceof CrsGeoCoding) {
            longitudeResolutionInMeter = rasterDataNode.getImageToModelTransform().getScaleX();
            latitudeResolutionInMeter = Math.abs(rasterDataNode.getImageToModelTransform().getScaleY());
        } else {
            int width = rasterDataNode.getRasterWidth();
            int height = rasterDataNode.getRasterHeight();

            int minWidth = 12;//depends on checking area of the computeGeocodingAccordingDuplicatedValue
            if (width > minWidth && height > 2) {
                final DefaultGeographicCRS wgs84 = DefaultGeographicCRS.WGS84;
                final Ellipsoid ellipsoid = wgs84.getDatum().getEllipsoid();
                final double meanEarthRadiusM = (ellipsoid.getSemiMajorAxis() + ellipsoid.getSemiMinorAxis()) * 0.5;

                int x1 = (int) (width * 0.5);
                int y1 = (int) (height * 0.5);
                GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(x1, y1), null);
                double resLon = geoPos.getLon();
                double resLat = geoPos.getLat();
                final SphericalDistance spherDist = new SphericalDistance(resLon, resLat);

                //compute latitude
                GeoPos geoPosY = geoCoding.getGeoPos(new PixelPos(x1, y1 + 1), null);
                double resLonY = geoPosY.getLon();
                double resLatY = geoPosY.getLat();
                double latitudeDistance = spherDist.distance(resLonY, resLatY);
                latitudeResolutionInMeter = latitudeDistance * meanEarthRadiusM;

                // compute longitude with checking of duplicated geocoding
                longitudeResolutionInMeter = computeGeocodingAccordingDuplicatedValue(geoCoding, width, x1, y1, resLon, spherDist, meanEarthRadiusM);
            }
        }
    }

    private double computeGeocodingAccordingDuplicatedValue(GeoCoding geoCoding, int width, int xRef, int yRef, double resLon,
                                                            SphericalDistance spherDist, double meanEarthRadiusM) {

        int step = 5;
        int distanceMax = 20;
        int diffPix = step;
        boolean haveAResolution = false;
        while (!haveAResolution && diffPix < distanceMax && xRef + diffPix < width - 1) {
            GeoPos geoPosX = geoCoding.getGeoPos(new PixelPos(xRef + diffPix, yRef), null);
            double resLonX = geoPosX.getLon();
            if (resLon != resLonX) {
                haveAResolution = true;
            } else {
                diffPix += step;
            }
        }
        GeoPos geoPosX = geoCoding.getGeoPos(new PixelPos((xRef + diffPix), yRef), null);
        double resLonX = geoPosX.getLon();
        double resLatX = geoPosX.getLat();
        double longitudeDistance = spherDist.distance(resLonX, resLatX);
        return longitudeDistance * meanEarthRadiusM / (double) diffPix;
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
            } else {
                GeoPos geoPos = geoCoding.getGeoPos(pixelPos, null);
                if (showGeoPosOffsetDecimals) {
                    geoPosLabel.setText(String.format("Lat %.5f  Lon %.5f", geoPos.getLat(), geoPos.getLon()));
                } else {
                    geoPosLabel.setText(String.format(GEO_POS_FORMAT, geoPos.getLatString(), geoPos.getLonString()));
                }
            }

            if (showPixelOffsetDecimals) {
                pixelPosLabel.setText(String.format(PIXEL_POS_FORMAT, imageP.getX(), imageP.getY()));
            } else {
                pixelPosLabel.setText(String.format(PIXEL_POS_FORMAT, (int) Math.floor(imageP.getX()),
                        (int) Math.floor(imageP.getY())));
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
            if (longitudeResolutionInMeter != Double.NaN && latitudeResolutionInMeter != Double.NaN)
                pixelSpacingLabel.setText(String.format(PIXEL_SIZE_FORMAT,
                        decimalFormat.format(latitudeResolutionInMeter),
                        decimalFormat.format(longitudeResolutionInMeter)));

        } else {
            setDefault();

        }

    }

    private void setDefault() {
        geoPosLabel.setText(String.format(GEO_POS_FORMAT, "--", "--"));
        pixelPosLabel.setText(String.format(PIXEL_POS_FORMAT, "--", "--"));
        zoomLevelLabel.setText(String.format(ZOOM_LEVEL_FORMAT, "--", "--"));
        pixelSpacingLabel.setText(String.format(PIXEL_SIZE_FORMAT, "--", "--"));
    }

    @Override
    public void pixelPosNotAvailable() {
        setDefault();
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        // Called if SNAP preferences change, adjust any status bar setting here.
        final String propertyName = evt.getKey();
        if (PREFERENCE_KEY_SHOW_PIXEL_POS_DECIMALS.equals(propertyName)
                || PREFERENCE_KEY_SHOW_GEO_POS_DECIMALS.equals(propertyName)) {
            updateSettings();
        }
    }

    @Override
    public void windowSelected(DocumentWindowManager.Event<Object, ProductSceneView> e) {
        ProductSceneView view = e.getWindow().getView();
        view.addPixelPositionListener(this);
        computeResolution();
    }

    @Override
    public void windowDeselected(DocumentWindowManager.Event<Object, ProductSceneView> e) {
        ProductSceneView view = e.getWindow().getView();
        view.removePixelPositionListener(this);
        longitudeResolutionInMeter = Double.NaN;
        latitudeResolutionInMeter = Double.NaN;
    }

    private void updateSettings() {
        final Preferences preferences = SnapApp.getDefault().getPreferences();
        showPixelOffsetDecimals = preferences.getBoolean(
                PREFERENCE_KEY_SHOW_PIXEL_POS_DECIMALS,
                PREFERENCE_DEFAULT_SHOW_PIXEL_POS_DECIMALS);

        showGeoPosOffsetDecimals = preferences.getBoolean(
                PREFERENCE_KEY_SHOW_GEO_POS_DECIMALS,
                PREFERENCE_DEFAULT_SHOW_GEO_POS_DECIMALS);
    }
}