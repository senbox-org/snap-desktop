package org.esa.snap.rcp.status;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.snap.framework.datamodel.GeoCoding;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.RasterDataNode;
import org.esa.snap.framework.ui.PixelPositionListener;
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * @author muhammad.bc on 7/8/2015.
 * To add the level of band on the status bar.
 */
@ServiceProvider(service = StatusLineElementProvider.class, position = 60)
public class StatusGeoPos implements StatusLineElementProvider, DocumentWindowManager.Listener, PixelPositionListener {

    private final Dimension dimension;
    private final JSeparator separator;
    private final JLabel label;
    private final JPanel panel;

    public StatusGeoPos() {
        DocumentWindowManager.getDefault().addListener(this);
        separator = new JSeparator(SwingConstants.VERTICAL);
        label = new JLabel();
        dimension = new Dimension();
        dimension.setSize(150, 20);
        label.setPreferredSize(dimension);
        label.setHorizontalAlignment(SwingConstants.CENTER);

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(Box.createHorizontalGlue());
        panel.add(separator);
        dimension.setSize(250, 20);
        panel.setPreferredSize(dimension);
        panel.add(label);
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
            RasterDataNode rasterDataNode = SnapApp.getDefault().getSelectedProductSceneView().getRaster();
            if (rasterDataNode == null) {
                return;
            }
            GeoCoding geoCoding = rasterDataNode.getGeoCoding();
            GeoPos geoPos = geoCoding.getGeoPos(pixelPos, null);


            label.setForeground(Color.black);
            label.setText(String.format("Lat %6s : Lon %6s", geoPos.getLatString(), geoPos.getLonString()));
        } else {
            label.setText(String.format("Lat %6s : Lon %6s", "--","--"));
        }

    }

    @Override
    public void pixelPosNotAvailable() {
        label.setText(String.format("Lat %6s : Lon %6s", "--", "--"));
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

    @Override
    public Component getStatusLineElement() {
        return panel;
    }
}
