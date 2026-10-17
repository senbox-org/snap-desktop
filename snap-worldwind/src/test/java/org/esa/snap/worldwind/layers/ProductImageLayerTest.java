package org.esa.snap.worldwind.layers;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.ui.product.ProductSceneView;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.swing.SwingUtilities;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductImageLayerTest {

    private static Product createProduct(final String name, final boolean geoCoded) throws Exception {
        final Product product = new Product(name, "TEST", 100, 50);
        if (geoCoded) {
            product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 100, 50, 10.0, 50.0,
                                                       0.01, 0.01, 0.0, 0.0));
        }
        for (String bandName : new String[]{"a", "b"}) {
            final Band band = new VirtualBand(bandName, ProductData.TYPE_FLOAT32, 100, 50, "X");
            band.setImageInfo(new ImageInfo(new ColorPaletteDef(0, 100)));
            product.addBand(band);
        }
        return product;
    }

    private static ProductSceneView mockView(final RasterDataNode raster) {
        final ProductSceneView view = mock(ProductSceneView.class);
        when(view.getProduct()).thenReturn(raster.getProduct());
        when(view.getRaster()).thenReturn(raster);
        when(view.getRasters()).thenReturn(new RasterDataNode[]{raster});
        when(view.getImageInfo()).thenReturn(raster.getImageInfo());
        return view;
    }

    private static void onEDT(final Runnable runnable) throws Exception {
        SwingUtilities.invokeAndWait(runnable);
    }

    private static PropertyChangeListener imageInfoListener(final ProductSceneView view) {
        final ArgumentCaptor<PropertyChangeListener> captor = ArgumentCaptor.forClass(PropertyChangeListener.class);
        verify(view).addPropertyChangeListener(eq(ProductSceneView.PROPERTY_NAME_IMAGE_INFO), captor.capture());
        return captor.getValue();
    }

    @Test
    public void testSelectedViewIsShownAndReplacesTheQuicklook() throws Exception {
        final ProductImageLayer layer = new ProductImageLayer();
        final List<String> shown = new ArrayList<>();
        layer.setImageShownListener((product, isShown) -> shown.add(product.getName() + "=" + isShown));
        final Product product = createProduct("p", true);
        final ProductSceneView view = mockView(product.getBand("a"));

        onEDT(() -> {
            layer.addProduct(product, null);
            layer.setSelectedSceneView(view);
        });
        assertTrue(layer.awaitPendingImages(10000));

        assertTrue(layer.hasImage(product));
        assertEquals("[p=true]", shown.toString());
        assertSame(product.getBand("a"), layer.getImage(product).rasters[0]);

        onEDT(() -> layer.removeProduct(product));
        assertFalse(layer.hasImage(product));
        assertEquals("[p=true, p=false]", shown.toString());
        verify(view).removePropertyChangeListener(eq(ProductSceneView.PROPERTY_NAME_IMAGE_INFO), any());
    }

    @Test
    public void testAnotherViewOfTheProductReplacesTheImage() throws Exception {
        final ProductImageLayer layer = new ProductImageLayer();
        final List<String> shown = new ArrayList<>();
        layer.setImageShownListener((product, isShown) -> shown.add(String.valueOf(isShown)));
        final Product product = createProduct("p", true);
        final ProductSceneView viewA = mockView(product.getBand("a"));
        final ProductSceneView viewB = mockView(product.getBand("b"));

        onEDT(() -> {
            layer.addProduct(product, null);
            layer.setSelectedSceneView(viewA);
        });
        assertTrue(layer.awaitPendingImages(10000));
        final ProductImageTiledLayer first = layer.getImage(product).tiledLayer;

        onEDT(() -> layer.setSelectedSceneView(viewB));
        assertTrue(layer.awaitPendingImages(10000));

        assertSame(product.getBand("b"), layer.getImage(product).rasters[0]);
        assertTrue(first.isDisposed());
        // still one image for the product: shown once
        assertEquals("[true]", shown.toString());

        // selecting the shown view again keeps the image
        final ProductImageTiledLayer second = layer.getImage(product).tiledLayer;
        onEDT(() -> layer.setSelectedSceneView(viewB));
        assertTrue(layer.awaitPendingImages(10000));
        assertSame(second, layer.getImage(product).tiledLayer);
    }

    @Test
    public void testColourChangeRefreshesTheImage() throws Exception {
        final ProductImageLayer layer = new ProductImageLayer();
        final Product product = createProduct("p", true);
        final ProductSceneView view = mockView(product.getBand("a"));
        onEDT(() -> {
            layer.addProduct(product, null);
            layer.setSelectedSceneView(view);
        });
        assertTrue(layer.awaitPendingImages(10000));
        final ProductImageTiledLayer before = layer.getImage(product).tiledLayer;
        final PropertyChangeListener listener = imageInfoListener(view);

        // several changes while a slider is dragged give one refresh
        final ImageInfo newColours = new ImageInfo(new ColorPaletteDef(0, 50));
        when(view.getImageInfo()).thenReturn(newColours);
        onEDT(() -> {
            for (int i = 0; i < 5; i++) {
                listener.propertyChange(new PropertyChangeEvent(view, ProductSceneView.PROPERTY_NAME_IMAGE_INFO, null, newColours));
            }
        });
        Thread.sleep(ProductImageLayer.IMAGE_INFO_DELAY_MS * 3);
        assertTrue(layer.awaitPendingImages(10000));

        final ProductImageLayer.ProductImage after = layer.getImage(product);
        assertNotSame(before, after.tiledLayer);
        assertTrue(before.isDisposed());
        assertEquals(50, after.imageInfo.getColorPaletteDef().getMaxDisplaySample(), 0.0);
        // a copy: later changes to the view's colours do not change the shown image behind its back
        assertNotSame(newColours, after.imageInfo);
    }

    @Test
    public void testProductRemovedWhileTheImageIsBuilt() throws Exception {
        final ProductImageLayer layer = new ProductImageLayer();
        final Product product = createProduct("p", true);
        final ProductSceneView view = mockView(product.getBand("a"));

        onEDT(() -> {
            layer.addProduct(product, null);
            layer.setSelectedSceneView(view);
            layer.removeProduct(product);
        });
        assertTrue(layer.awaitPendingImages(10000));

        assertFalse(layer.hasImage(product));
        assertTrue(layer.getProductsWithImages().isEmpty());
    }

    @Test
    public void testRemovingTheRasterRemovesTheImage() throws Exception {
        final ProductImageLayer layer = new ProductImageLayer();
        final Product product = createProduct("p", true);
        final Band band = product.getBand("a");
        final ProductSceneView view = mockView(band);
        onEDT(() -> {
            layer.addProduct(product, null);
            layer.setSelectedSceneView(view);
        });
        assertTrue(layer.awaitPendingImages(10000));
        assertTrue(layer.hasImage(product));

        onEDT(() -> product.removeBand(band));

        assertFalse(layer.hasImage(product));
    }

    @Test
    public void testProductsWithoutGeoCodingOrUnknownProductsShowNothing() throws Exception {
        final ProductImageLayer layer = new ProductImageLayer();
        final Product plain = createProduct("plain", false);
        final Product unknown = createProduct("unknown", true);

        onEDT(() -> {
            layer.addProduct(plain, null);
            layer.setSelectedSceneView(mockView(plain.getBand("a")));
            // not added to the layer
            layer.setSelectedSceneView(mockView(unknown.getBand("a")));
            layer.setSelectedSceneView(null);
        });
        assertTrue(layer.awaitPendingImages(10000));

        assertTrue(layer.getProductsWithImages().isEmpty());
    }

    @Test
    public void testLevelSetFollowsTheProduct() throws Exception {
        final Product product = createProduct("p", true);
        final ProductImageTiledLayer tiledLayer = ProductImageLayer.createTiledLayer(
                new RasterDataNode[]{product.getBand("a")}, product.getBand("a").getImageInfo());

        assertNotNull(tiledLayer);
        assertEquals(49.5, tiledLayer.getLevels().getSector().getMinLatitude().degrees, 0.011);
        assertEquals(11.0, tiledLayer.getLevels().getSector().getMaxLongitude().degrees, 0.011);
        assertEquals(512, tiledLayer.getLevels().getFirstLevel().getTileWidth());
        // 0.01 degree pixels at 50 N need more levels than a 1 degree product
        assertTrue(tiledLayer.getLevels().getNumLevels() >= 5);
        // every image has its own tile cache
        assertFalse(tiledLayer.getLevels().getFirstLevel().getCacheName().equals(
                ProductImageLayer.createTiledLayer(new RasterDataNode[]{product.getBand("a")}, null)
                        .getLevels().getFirstLevel().getCacheName()));
    }

    @Test
    public void testQuicklookVisibility() throws Exception {
        final DefaultProductLayer productLayer = new DefaultProductLayer();
        final Product product = createProduct("p", true);

        assertTrue(productLayer.isQuicklookVisible(product));
        productLayer.setQuicklookVisible(product, false);
        assertFalse(productLayer.isQuicklookVisible(product));
        productLayer.setQuicklookVisible(product, true);
        assertTrue(productLayer.isQuicklookVisible(product));

        productLayer.setQuicklookVisible(product, false);
        onEDT(() -> productLayer.removeProduct(product));
        // forgotten with the product
        assertTrue(productLayer.isQuicklookVisible(product));
    }
}
