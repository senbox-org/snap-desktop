package org.esa.snap.rcp.status;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.LayerCanvas;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.ListenerSupport;
import org.openide.awt.StatusLineElementProvider;
import org.openide.util.lookup.ServiceProvider;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Component;
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
public class PixelPosStatusLineElementProvider extends ListenerSupport.SceneViewListener
        implements StatusLineElementProvider, PixelPositionListener, PreferenceChangeListener {

    public final static String PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X = "";
    public final static String PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y = "";
    public final static String PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS = "";

    public final static double PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_X = 0;
    public final static double PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_Y = 0;
    public final static boolean PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS = false;

    private double pixelOffsetX;
    private double pixelOffsetY;
    private boolean showPixelOffsetDecimals;
    private JLabel label;

    public PixelPosStatusLineElementProvider() {
        ListenerSupport.installSceneViewListener(this);
        SnapApp.getDefault().getPreferences().addPreferenceChangeListener(this);
        updateSettings();
        label = new JLabel();
    }

    @Override
    public Component getStatusLineElement() {
        return label;
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
            label.setForeground(Color.BLACK);
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
            label.setText(String.format("x=%d y=%d zoom=%s level=%d",
                                        (int) Math.floor(imageP.getX()),
                                        (int) Math.floor(imageP.getY()),
                                        scaleStr, currentLevel));
        } else {
            label.setForeground(Color.BLUE);
        }

    }

    @Override
    public void pixelPosNotAvailable() {
        label.setForeground(Color.RED);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        final String propertyName = evt.getKey();
        if (PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X.equals(propertyName)
            || PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y.equals(propertyName)
            || PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS.equals(propertyName)) {
            updateSettings();
        }
    }


    @Override
    public void closed(ProductSceneView view) {
        view.removePixelPositionListener(this);
    }

    @Override
    public void opened(ProductSceneView view) {
        view.addPixelPositionListener(this);
    }

    private void updateSettings() {
        final Preferences preferences = SnapApp.getDefault().getPreferences();
        pixelOffsetY = preferences.getDouble(PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y,
                                             PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_X);
        pixelOffsetX = preferences.getDouble(PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X,
                                             PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_Y);
        showPixelOffsetDecimals = preferences.getBoolean(
                PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS,
                PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS);
    }
}

