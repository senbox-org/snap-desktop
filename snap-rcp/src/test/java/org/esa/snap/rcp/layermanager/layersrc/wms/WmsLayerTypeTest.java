package org.esa.snap.rcp.layermanager.layersrc.wms;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.geotools.ows.wms.CRSEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.Assert.assertEquals;

public class WmsLayerTypeTest {

    private static VirtualBand band;

    @BeforeClass
    public static void beforeClass() {
        Product product = new Product("P", "T", 10, 10);
        product.setFileLocation(new File(String.format("out/%s.dim", product.getName())));

        band = new VirtualBand("V", ProductData.TYPE_INT32, 10, 10, "42");
        product.addBand(band);

    }

    @Test
    @Ignore
    public void testLayerCreation() throws IOException {
        // @todo 1 tb/** implement correct certificate handling, Java 9 changed the behaviour and is less graceful tb 2023-03-24
        URL wmsUrl = new URL("http://geoservice.dlr.de/basemap/wms");
        URLConnection urlConnection = wmsUrl.openConnection();
        urlConnection.setConnectTimeout(1000);
        boolean connected = false;
        try {
            urlConnection.connect();
            connected = true;
        } catch (Throwable ignore) {
        }
        Assume.assumeTrue(connected);

        final CollectionLayer rootLayer = new CollectionLayer();
        WmsLayerType wmsLayerType = new WmsLayerType();
        TestDummyLayerContext ctx = new TestDummyLayerContext(rootLayer);
        PropertySet layerConfig = wmsLayerType.createLayerConfig(ctx);


        layerConfig.setValue(WmsLayerType.PROPERTY_NAME_URL, wmsUrl);

        layerConfig.setValue(WmsLayerType.PROPERTY_NAME_IMAGE_SIZE, new Dimension(100, 100));
        layerConfig.setValue(WmsLayerType.PROPERTY_NAME_CRS_ENVELOPE, new CRSEnvelope("EPSG:4324", -10, 20, 15, 50));
        layerConfig.setValue(WmsLayerType.PROPERTY_NAME_RASTER, band);
        layerConfig.setValue(WmsLayerType.PROPERTY_NAME_LAYER_INDEX, 12);
        layerConfig.setValue(WmsLayerType.PROPERTY_NAME_STYLE_NAME, "default-style-osm_landusage");

        Layer worldMapLayer = wmsLayerType.createLayer(ctx, layerConfig);
        assertEquals("osm_landusage", worldMapLayer.getName());

    }

    private static class TestDummyLayerContext implements LayerContext {

        private final Layer rootLayer;

        private TestDummyLayerContext(Layer rootLayer) {
            this.rootLayer = rootLayer;
        }

        @Override
        public Object getCoordinateReferenceSystem() {
            return DefaultGeographicCRS.WGS84;
        }

        @Override
        public Layer getRootLayer() {
            return rootLayer;
        }
    }

}
