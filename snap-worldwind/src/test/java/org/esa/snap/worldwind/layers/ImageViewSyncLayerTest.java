package org.esa.snap.worldwind.layers;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.grender.Viewport;
import gov.nasa.worldwind.render.PointPlacemark;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GcpDescriptor;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PinDescriptor;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.ui.PixelPositionListener;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.worldwind.model.ImageViewGeoUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.swing.SwingUtilities;
import java.awt.geom.AffineTransform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ImageViewSyncLayerTest {

    /**
     * 100 x 50 pixels of 0.01 degrees from lon 10, lat 50 (upper-left corner).
     */
    private static Product createProduct(final String name) throws Exception {
        final Product product = new Product(name, "TEST", 100, 50);
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 100, 50, 10.0, 50.0,
                                                   0.01, 0.01, 0.0, 0.0));
        product.addBand("b", ProductData.TYPE_FLOAT32);
        return product;
    }

    private static Placemark createPlacemark(final Product product, final boolean gcp, final String name,
                                             final double x, final double y) {
        return Placemark.createPointPlacemark(gcp ? GcpDescriptor.getInstance() : PinDescriptor.getInstance(),
                                              name, name, "", new PixelPos(x, y), null, product.getSceneGeoCoding());
    }

    private static void onEDT(final Runnable runnable) throws Exception {
        SwingUtilities.invokeAndWait(runnable);
    }

    private static void assertAt(final PointPlacemark ppm, final double lat, final double lon) {
        assertNotNull(ppm);
        assertEquals(lat, ppm.getPosition().getLatitude().degrees, 1e-6);
        assertEquals(lon, ppm.getPosition().getLongitude().degrees, 1e-6);
    }

    @Test
    public void testPinsAndGcpsAreShownAndFollowChanges() throws Exception {
        final ImageViewSyncLayer layer = new ImageViewSyncLayer();
        final Product product = createProduct("p");
        final Placemark pin = createPlacemark(product, false, "pin1", 10.5, 20.5);
        final Placemark gcp = createPlacemark(product, true, "gcp1", 50.5, 10.5);
        product.getPinGroup().add(pin);
        product.getGcpGroup().add(gcp);

        onEDT(() -> layer.addProduct(product, null));

        assertEquals(2, layer.getNumPlacemarks());
        assertAt(layer.getGlobePlacemark(pin), 50 - 0.205, 10 + 0.105);
        assertEquals(ImageViewSyncLayer.PIN_IMAGE, layer.getGlobePlacemark(pin).getAttributes().getImageAddress());
        assertEquals(ImageViewSyncLayer.GCP_IMAGE, layer.getGlobePlacemark(gcp).getAttributes().getImageAddress());
        assertEquals("pin1", layer.getGlobePlacemark(pin).getLabelText());
        // picking a globe placemark leads back to the SNAP placemark
        assertSame(pin, ImageViewSyncLayer.getPlacemark(layer.getGlobePlacemark(pin)));

        // added, moved, renamed and removed in SNAP
        final Placemark pin2 = createPlacemark(product, false, "pin2", 90.5, 40.5);
        onEDT(() -> product.getPinGroup().add(pin2));
        assertEquals(3, layer.getNumPlacemarks());

        onEDT(() -> {
            pin.setPixelPos(new PixelPos(30.5, 20.5));
            pin.setLabel("moved");
        });
        assertAt(layer.getGlobePlacemark(pin), 50 - 0.205, 10 + 0.305);
        assertEquals("moved", layer.getGlobePlacemark(pin).getLabelText());

        onEDT(() -> product.getGcpGroup().remove(gcp));
        assertEquals(2, layer.getNumPlacemarks());
        assertNull(layer.getGlobePlacemark(gcp));
    }

    @Test
    public void testRemovingTheProductRemovesItsPlacemarksAndListener() throws Exception {
        final ImageViewSyncLayer layer = new ImageViewSyncLayer();
        final Product product = createProduct("p");
        final Product other = createProduct("other");
        product.getPinGroup().add(createPlacemark(product, false, "pin1", 10.5, 20.5));
        other.getPinGroup().add(createPlacemark(other, false, "pin1", 10.5, 20.5));
        final int listenersBefore = product.getProductNodeListeners().length;

        onEDT(() -> {
            layer.addProduct(product, null);
            layer.addProduct(other, null);
        });
        assertEquals(2, layer.getNumPlacemarks());
        assertEquals(listenersBefore + 1, product.getProductNodeListeners().length);

        onEDT(() -> layer.removeProduct(product));

        assertEquals(1, layer.getNumPlacemarks());
        assertEquals(listenersBefore, product.getProductNodeListeners().length);
        // later pins of the removed product are ignored
        onEDT(() -> product.getPinGroup().add(createPlacemark(product, false, "pin2", 1.5, 1.5)));
        assertEquals(1, layer.getNumPlacemarks());
    }

    @Test
    public void testImageViewCursorIsShownOnTheGlobe() throws Exception {
        final ImageViewSyncLayer layer = new ImageViewSyncLayer();
        final Product product = createProduct("p");
        final ProductSceneView view = mockView(product.getBand("b"));
        onEDT(() -> {
            layer.addProduct(product, null);
            layer.setSelectedSceneView(view);
        });
        final ArgumentCaptor<PixelPositionListener> captor = ArgumentCaptor.forClass(PixelPositionListener.class);
        verify(view).addPixelPositionListener(captor.capture());
        final PixelPositionListener listener = captor.getValue();

        // level 1 pixel (10, 5) is raster pixel (20, 10) .. (22, 12), centre (21, 11)
        final ImageLayer imageLayer = mock(ImageLayer.class);
        when(imageLayer.getImageToModelTransform(1)).thenReturn(AffineTransform.getScaleInstance(2, 2));
        when(imageLayer.getModelToImageTransform()).thenReturn(new AffineTransform());
        onEDT(() -> listener.pixelPosChanged(imageLayer, 10, 5, 1, true, null));

        assertTrue(layer.getCursor().isVisible());
        assertAt(layer.getCursor(), 50 - 0.11, 10 + 0.21);

        onEDT(listener::pixelPosNotAvailable);
        assertFalse(layer.getCursor().isVisible());

        // another view takes the cursor over
        final ProductSceneView view2 = mockView(product.getBand("b"));
        onEDT(() -> layer.setSelectedSceneView(view2));
        verify(view).removePixelPositionListener(listener);
        assertSame(view2, layer.getCursorView());

        onEDT(() -> layer.removeProduct(product));
        assertNull(layer.getCursorView());
    }

    @Test
    public void testViewsOfUnknownProductsAreNotFollowed() throws Exception {
        final ImageViewSyncLayer layer = new ImageViewSyncLayer();
        final Product product = createProduct("p");
        final ProductSceneView view = mockView(product.getBand("b"));

        onEDT(() -> layer.setSelectedSceneView(view));

        verify(view, never()).addPixelPositionListener(any());
        assertNull(layer.getCursorView());
    }

    @Test
    public void testPlacePin() throws Exception {
        final Product product = createProduct("p");

        final Placemark pin = ImageViewSyncLayer.placePin(product, new GeoPos(49.795, 10.205));

        assertNotNull(pin);
        assertEquals(1, product.getPinGroup().getNodeCount());
        assertEquals(20.5, pin.getPixelPos().x, 1e-6);
        assertEquals(20.5, pin.getPixelPos().y, 1e-6);
        // outside the product
        assertNull(ImageViewSyncLayer.placePin(product, new GeoPos(10, 10)));
        assertEquals(1, product.getPinGroup().getNodeCount());
        // unique names
        assertFalse(pin.getName().equals(ImageViewSyncLayer.placePin(product, new GeoPos(49.9, 10.5)).getName()));
    }

    @Test
    public void testSelectInView() throws Exception {
        final Product product = createProduct("p");
        final Product other = createProduct("other");
        final Placemark pin = createPlacemark(product, false, "pin1", 10.5, 20.5);
        final Placemark gcp = createPlacemark(product, true, "gcp1", 10.5, 20.5);
        product.getPinGroup().add(pin);
        product.getGcpGroup().add(gcp);
        final ProductSceneView view = mockView(product.getBand("b"));
        final ProductSceneView otherView = mockView(other.getBand("b"));

        assertTrue(ImageViewSyncLayer.selectInView(pin, view));
        verify(view).selectPins(new Placemark[]{pin});
        assertTrue(ImageViewSyncLayer.selectInView(gcp, view));
        verify(view).selectGcps(new Placemark[]{gcp});
        assertFalse(ImageViewSyncLayer.selectInView(pin, otherView));
        assertFalse(ImageViewSyncLayer.selectInView(pin, null));
    }

    @Test
    public void testCentreView() throws Exception {
        final Product product = createProduct("p");
        final ProductSceneView view = mockView(product.getBand("b"));
        final ImageLayer imageLayer = mock(ImageLayer.class);
        when(imageLayer.getImageToModelTransform()).thenReturn(AffineTransform.getScaleInstance(3, 3));
        when(view.getBaseImageLayer()).thenReturn(imageLayer);
        final LayerCanvas canvas = mock(LayerCanvas.class);
        final Viewport viewport = mock(Viewport.class);
        when(view.getLayerCanvas()).thenReturn(canvas);
        when(canvas.getViewport()).thenReturn(viewport);
        when(viewport.getZoomFactor()).thenReturn(2.5);

        assertTrue(ImageViewSyncLayer.centreView(view, new GeoPos(49.795, 10.205)));
        // raster pixel (20.5, 20.5) in model coordinates, zoom kept
        verify(viewport).setZoomFactor(2.5, 61.5, 61.5);

        assertFalse(ImageViewSyncLayer.centreView(view, new GeoPos(0, 0)));
        verify(viewport).setZoomFactor(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    public void testGeoConversions() throws Exception {
        final Product product = createProduct("p");
        final RasterDataNode raster = product.getBand("b");

        final PixelPos pixel = ImageViewGeoUtils.levelToRasterPixel(AffineTransform.getScaleInstance(4, 4),
                                                                    new AffineTransform(), 2, 3);
        assertEquals(10.0, pixel.x, 1e-9);
        assertEquals(14.0, pixel.y, 1e-9);

        assertNotNull(ImageViewGeoUtils.toRasterPixel(raster, new GeoPos(49.9, 10.5)));
        assertNull(ImageViewGeoUtils.toRasterPixel(raster, new GeoPos(49.9, 11.5)));
        assertNull(ImageViewGeoUtils.toRasterPixel(raster, new GeoPos(Double.NaN, 10.5)));
        assertNull(ImageViewGeoUtils.toGeoPos(null, new PixelPos(1, 1)));

        final Placemark pin = createPlacemark(product, false, "pin1", 10.5, 20.5);
        final GeoPos geoPos = ImageViewGeoUtils.placemarkGeoPos(pin, product.getSceneGeoCoding());
        assertEquals(50 - 0.205, geoPos.lat, 1e-6);
    }

    private static ProductSceneView mockView(final RasterDataNode raster) {
        final ProductSceneView view = mock(ProductSceneView.class);
        when(view.getProduct()).thenReturn(raster.getProduct());
        when(view.getRaster()).thenReturn(raster);
        when(view.getRasters()).thenReturn(new RasterDataNode[]{raster});
        return view;
    }
}
