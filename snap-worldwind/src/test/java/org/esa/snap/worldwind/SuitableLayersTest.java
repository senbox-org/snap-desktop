package org.esa.snap.worldwind;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.worldwind.layers.BaseLayer;
import org.esa.snap.worldwind.layers.WWLayer;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SuitableLayersTest {

    private static WWLayer layer(final BaseLayer.Suitability suitability, final boolean complementary) {
        final WWLayer layer = mock(WWLayer.class);
        when(layer.getSuitability(any())).thenReturn(suitability);
        when(layer.isComplementary()).thenReturn(complementary);
        return layer;
    }

    @Test
    public void testIntendedLayerReplacesOnlyTheNonComplementaryLayers() {
        final WWLayer outlines = layer(BaseLayer.Suitability.SUITABLE, false);
        final WWLayer pins = layer(BaseLayer.Suitability.SUITABLE, true);
        final WWLayer ocn = layer(BaseLayer.Suitability.INTENDED, false);
        final WWLayer unrelated = layer(BaseLayer.Suitability.UNSUITABLE, true);
        final Product product = new Product("ocn", "OCN", 10, 10);

        // an OCN product: its own layer plus pins, vectors and images, not the default outlines
        final List<WWLayer> layers = WWBaseToolView.getSuitableLayers(Arrays.asList(outlines, pins, ocn, unrelated), product);
        assertEquals(Arrays.asList(ocn, pins), layers);
    }

    @Test
    public void testWithoutIntendedLayerAllSuitableLayers() {
        final WWLayer outlines = layer(BaseLayer.Suitability.SUITABLE, false);
        final WWLayer pins = layer(BaseLayer.Suitability.SUITABLE, true);
        final List<WWLayer> layers = WWBaseToolView.getSuitableLayers(Arrays.asList(outlines, pins),
                                                                      new Product("p", "T", 10, 10));
        assertEquals(Arrays.asList(outlines, pins), layers);
    }
}
