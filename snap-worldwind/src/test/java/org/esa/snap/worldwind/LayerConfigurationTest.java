package org.esa.snap.worldwind;

import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.ScalebarLayer;
import gov.nasa.worldwind.layers.TiledImageLayer;
import gov.nasa.worldwind.layers.WorldMapLayer;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LayerConfigurationTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 18);

    private static String readConfig() throws Exception {
        try (InputStream in = LayerConfiguration.class.getResourceAsStream(LayerConfiguration.RESOURCE)) {
            assertNotNull(in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> names(final LayerList layers) {
        final List<String> names = new ArrayList<>();
        for (Layer layer : layers) {
            names.add(layer.getName());
        }
        return names;
    }

    @Test
    public void testLayersCreated() {
        final LayerList layers = LayerConfiguration.createLayers(DATE);
        final List<String> names = names(layers);

        assertTrue(names.toString(), names.contains("Bing Imagery"));
        assertTrue(names.contains("Blue Marble May 2004"));
        assertTrue(names.contains(AppPanel.LANDSAT_LAYER_NAME));
        assertTrue(layers.getLayerByName("Blue Marble May 2004").isEnabled());
        assertFalse("onRequest layers start disabled", layers.getLayerByName("Bing Imagery").isEnabled());
        assertTrue(layers.getLayerByName("Bing Imagery") instanceof TiledImageLayer);

        // the dead NASA place name and boundary layers are not configured
        assertFalse(names.contains("Political Boundaries"));
        assertFalse(names.contains("Place Names"));
    }

    @Test
    public void testOverlaysLast() {
        final LayerList layers = LayerConfiguration.createLayers(DATE);
        boolean overlaySeen = false;
        for (Layer layer : layers) {
            final boolean overlay = LayerConfiguration.isOverlay(layer);
            if (layer instanceof WorldMapLayer || layer instanceof ScalebarLayer || layer instanceof CompassLayer) {
                assertTrue(layer.getName(), overlay);
            }
            if (overlaySeen) {
                assertTrue("base layer " + layer.getName() + " after an overlay", overlay);
            }
            overlaySeen |= overlay;
        }
        assertTrue(overlaySeen);
    }

    @Test
    public void testOnlyCheckedEndpoints() throws Exception {
        final String config = readConfig();
        // hosts that no longer answer (checked 2026-10-19)
        for (String deadHost : new String[]{"worldwind22.arc.nasa.gov", "worldwind20.arc.nasa.gov", "nasa.network.com",
                "services.nationalmap.gov"}) {
            assertFalse(deadHost, config.contains(deadHost));
        }
        // the level sets are configured, no capabilities download
        assertFalse(config.contains("<RetrievePropertiesFromService>true"));
    }

    @Test
    public void testInsertBelowOverlaysKeepsOrder() {
        final LayerList layers = LayerConfiguration.createLayers(DATE);
        final int firstOverlay = layers.indexOf(findOverlay(layers));
        final RenderableLayer a = new RenderableLayer();
        a.setName("a");
        final RenderableLayer b = new RenderableLayer();
        b.setName("b");

        LayerConfiguration.insertBelowOverlays(layers, a);
        LayerConfiguration.insertBelowOverlays(layers, b);

        assertEquals(firstOverlay, layers.indexOf(a));
        assertEquals(firstOverlay + 1, layers.indexOf(b));
        assertTrue(LayerConfiguration.isOverlay(layers.get(firstOverlay + 2)));
    }

    @Test
    public void testInsertWithoutOverlaysAppends() {
        final LayerList layers = new LayerList();
        final RenderableLayer base = new RenderableLayer();
        layers.add(base);
        final RenderableLayer a = new RenderableLayer();
        LayerConfiguration.insertBelowOverlays(layers, a);
        assertEquals(1, layers.indexOf(a));
    }

    private static Layer findOverlay(final LayerList layers) {
        for (Layer layer : layers) {
            if (LayerConfiguration.isOverlay(layer)) {
                return layer;
            }
        }
        throw new AssertionError("no overlay");
    }
}
