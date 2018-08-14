package org.esa.snap.rcp.status;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.LayerCanvas;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.netbeans.docwin.DocumentWindowManager;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.PixelPositionListener;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.StatusLineElementProvider;
import org.openide.util.lookup.ServiceProvider;

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


    private final JLabel zoomLevelLabel;
    private final JLabel geoPosLabel;
    private final JLabel pixelPosLabel;
    private final JPanel panel;

    private boolean showPixelOffsetDecimals;
    private boolean showGeoPosOffsetDecimals;

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
                geoPosLabel.setText(String.format(GEO_POS_FORMAT, "--", "--"));
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
        if (PREFERENCE_KEY_SHOW_PIXEL_POS_DECIMALS.equals(propertyName)
            || PREFERENCE_KEY_SHOW_GEO_POS_DECIMALS.equals(propertyName)) {
            updateSettings();
        }
    }

    @Override
    public void windowSelected(DocumentWindowManager.Event<Object, ProductSceneView> e) {
        ProductSceneView view = e.getWindow().getView();
        view.addPixelPositionListener(this);
    }

    @Override
    public void windowDeselected(DocumentWindowManager.Event<Object, ProductSceneView> e) {
        ProductSceneView view = e.getWindow().getView();
        view.removePixelPositionListener(this);
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

