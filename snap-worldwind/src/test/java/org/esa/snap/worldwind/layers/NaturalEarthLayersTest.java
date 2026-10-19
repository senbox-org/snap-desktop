package org.esa.snap.worldwind.layers;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.GeographicText;
import org.esa.snap.worldwind.LayerConfiguration;
import org.esa.snap.worldwind.model.PlaceNames;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NaturalEarthLayersTest {

    @Test
    public void testBordersLayer() throws Exception {
        final Layer borders = NaturalEarthLayers.createBordersLayer();
        assertNotNull(borders);
        assertEquals(NaturalEarthLayers.BORDERS_NAME, borders.getName());
        assertFalse(borders.isPickEnabled());

        // the shapes are loaded in the background
        final RenderableLayer renderableLayer = (RenderableLayer) borders;
        final long end = System.currentTimeMillis() + 20000;
        while (renderableLayer.getNumRenderables() == 0 && System.currentTimeMillis() < end) {
            Thread.sleep(50);
        }
        assertTrue(renderableLayer.getNumRenderables() > 0);
    }

    @Test
    public void testFeatureClassStyle() {
        assertTrue(NaturalEarthLayers.isInternationalBoundary("International boundary (verify)"));
        assertTrue(NaturalEarthLayers.isInternationalBoundary(null));
        assertFalse(NaturalEarthLayers.isInternationalBoundary("Disputed (please verify)"));
        assertFalse(NaturalEarthLayers.isInternationalBoundary("Line of control (please verify)"));
    }

    @Test
    public void testAddedAsOverlays() {
        final LayerList layers = new LayerList();
        NaturalEarthLayers.addTo(layers);
        assertEquals(2, layers.size());
        for (Layer layer : layers) {
            assertTrue(layer.getName(), LayerConfiguration.isOverlay(layer));
        }
        assertNotNull(layers.getLayerByName(NaturalEarthLayers.BORDERS_NAME));
        assertNotNull(layers.getLayerByName(NaturalEarthLayers.PLACE_NAMES_NAME));
    }

    @Test
    public void testPlaceNamesFollowAltitude() throws Exception {
        final NaturalEarthPlaceNameLayer layer = new NaturalEarthPlaceNameLayer(PlaceNames.getBundledPlaces());
        final List<GeographicText> global = layer.getTexts(2.0e7, Sector.FULL_SPHERE);
        final List<GeographicText> regional = layer.getTexts(1.0e6, Sector.FULL_SPHERE);
        assertFalse(global.isEmpty());
        assertTrue(global.size() < regional.size());
        assertTrue(regional.size() <= PlaceNames.getBundledPlaces().size());

        // only places in the visible sector
        final Sector europe = Sector.fromDegrees(35, 60, -10, 30);
        for (GeographicText text : layer.getTexts(1.0e6, europe)) {
            assertTrue(text.getText().toString(), europe.contains(text.getPosition()));
        }
    }
}
