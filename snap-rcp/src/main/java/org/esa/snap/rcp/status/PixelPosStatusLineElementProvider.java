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

    public final static String PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X = "";
    public final static String PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y = "";
    public final static String PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS = "";



    private final JLabel zoomLevel;
    private final JLabel geoPost;
    private final JLabel pixel;
    private final JPanel panel;

    public PixelPosStatusLineElementProvider() {
        DocumentWindowManager.getDefault().addListener(this);
        SnapApp.getDefault().getPreferences().addPreferenceChangeListener(this);

        pixel = new JLabel();
        pixel.setPreferredSize(new Dimension(200, 20));
        pixel.setHorizontalAlignment(SwingConstants.CENTER);

        geoPost = new JLabel();
        geoPost.setPreferredSize(new Dimension(200, 20));
        geoPost.setHorizontalAlignment(SwingConstants.CENTER);

        zoomLevel = new JLabel();
        zoomLevel.setPreferredSize(new Dimension(250, 20));
        zoomLevel.setHorizontalAlignment(SwingConstants.CENTER);


        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(Box.createHorizontalGlue());


        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(zoomLevel);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(pixel);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(geoPost);

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


            PixelPos pixelPos = new PixelPos(imageP.getX(), imageP.getY());
            ProductSceneView productSceneView = SnapApp.getDefault().getSelectedProductSceneView();
            if (productSceneView == null) {
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


            geoPost.setText(String.format("Lat %6s : Lon %6s", geoPos.getLatString(), geoPos.getLonString()));
            pixel.setText(String.format("X %6d : Y %6d", (int) Math.floor(imageP.getX()), (int) Math.floor(imageP.getY())));
            zoomLevel.setText(String.format("Zoom %s : Level %s", scaleStr, currentLevel));

        } else {
            setDefault();

        }

    }

    private void setDefault() {
        geoPost.setText(String.format("Lat %6s : Lon %6s", "--", "--"));
        pixel.setText(String.format("X %6s : Y %6s", "--", "--"));
        zoomLevel.setText(String.format("Zoom %s . Level %s", "--", "--"));
    }


    @Override
    public void pixelPosNotAvailable() {
        geoPost.setText(String.format("Lat %6s : Lon %6s", "--", "--"));
        pixel.setText(String.format("X %6s : Y %6s", "--", "--"));
        zoomLevel.setText(String.format("Zoom %s : Level %s", "--", "--"));
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        final String propertyName = evt.getKey();
        if (PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X.equals(propertyName)
                || PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y.equals(propertyName)
                || PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS.equals(propertyName)) {

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

}

