package org.esa.snap.worldwind;

import com.bc.ceres.glayer.swing.LayerCanvas;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.ui.product.ProductSceneView;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GlobeCursorSyncTest {

    private static ProductSceneView mockView(final double westLon) throws Exception {
        final Product product = new Product("p", "TEST", 100, 50);
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 100, 50, westLon, 50.0,
                                                   0.01, 0.01, 0.0, 0.0));
        final RasterDataNode raster = product.addBand("b", ProductData.TYPE_FLOAT32);
        final ProductSceneView view = mock(ProductSceneView.class);
        when(view.getRaster()).thenReturn(raster);
        when(view.getProduct()).thenReturn(product);
        when(view.getLayerCanvas()).thenReturn(mock(LayerCanvas.class));
        return view;
    }

    @Test
    public void testCursorIsShownInTheViewsCoveringThePosition() throws Exception {
        final ProductSceneView west = mockView(10);
        final ProductSceneView east = mockView(20);
        final List<ProductSceneView> openViews = new ArrayList<>(Arrays.asList(west, east));
        final GlobeCursorSync sync = new GlobeCursorSync(() -> openViews);

        sync.setGlobePosition(new GeoPos(49.795, 10.205));

        final PixelPos pixel = sync.getCursorPixel(west);
        assertNotNull(pixel);
        assertEquals(20.5, pixel.x, 1e-6);
        assertEquals(20.5, pixel.y, 1e-6);
        assertNull(sync.getCursorPixel(east));
        verify(west.getLayerCanvas()).addOverlay(any());
        verify(east.getLayerCanvas(), never()).addOverlay(any());

        // moving within the view reuses the overlay
        sync.setGlobePosition(new GeoPos(49.8, 10.3));
        verify(west.getLayerCanvas(), times(1)).addOverlay(any());

        // moving to the other view moves the cursor
        sync.setGlobePosition(new GeoPos(49.8, 20.3));
        assertNull(sync.getCursorPixel(west));
        assertNotNull(sync.getCursorPixel(east));
        verify(west.getLayerCanvas()).removeOverlay(any());

        // leaving the globe
        sync.setGlobePosition(null);
        assertNull(sync.getCursorPixel(east));
        verify(east.getLayerCanvas()).removeOverlay(any());
    }

    @Test
    public void testClosedViewsLoseTheirOverlay() throws Exception {
        final ProductSceneView view = mockView(10);
        final List<ProductSceneView> openViews = new ArrayList<>(Arrays.asList(view));
        final GlobeCursorSync sync = new GlobeCursorSync(() -> openViews);
        sync.setGlobePosition(new GeoPos(49.8, 10.3));

        openViews.clear();
        sync.setGlobePosition(new GeoPos(49.8, 10.3));

        assertNull(sync.getCursorPixel(view));
        verify(view.getLayerCanvas()).removeOverlay(any());
    }

    @Test
    public void testDisabled() throws Exception {
        final ProductSceneView view = mockView(10);
        final GlobeCursorSync sync = new GlobeCursorSync(() -> Arrays.asList(view));
        sync.setGlobePosition(new GeoPos(49.8, 10.3));

        sync.setEnabled(false);
        assertNull(sync.getCursorPixel(view));
        sync.setGlobePosition(new GeoPos(49.8, 10.3));
        assertNull(sync.getCursorPixel(view));

        sync.setEnabled(true);
        sync.setGlobePosition(new GeoPos(49.8, 10.3));
        assertNotNull(sync.getCursorPixel(view));
    }
}
