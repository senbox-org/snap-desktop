package org.esa.snap.worldwind.layers;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.TextureTile;
import org.esa.snap.worldwind.model.GeoBounds;
import org.esa.snap.worldwind.model.ProductTileRenderer;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The tile loading of the full-resolution image layer, without OpenGL: the texture step is replaced.
 */
public class ProductImageTiledLayerTest {

    private static class TestLayer extends ProductImageTiledLayer {
        final List<TextureTile> installed = new ArrayList<>();

        TestLayer(final ProductTileRenderer renderer) {
            super(renderer, new GeoBounds(45, 46, 10, 11), 0.01);
        }

        @Override
        void installTexture(final TextureTile tile, final BufferedImage image) {
            installed.add(tile);
        }
    }

    private static TextureTile tile(final ProductImageTiledLayer layer, final int row, final int column) {
        // level zero tiles are 36 degrees from -90, -180
        final double delta = ProductImageTiledLayer.LEVEL_ZERO_DELTA_DEG;
        return new TextureTile(Sector.fromDegrees(-90 + row * delta, -90 + (row + 1) * delta,
                                                  -180 + column * delta, -180 + (column + 1) * delta),
                               layer.getLevels().getFirstLevel(), row, column);
    }

    private static ProductTileRenderer renderer(final BufferedImage result) {
        final ProductTileRenderer renderer = mock(ProductTileRenderer.class);
        when(renderer.render(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt())).thenReturn(result);
        return renderer;
    }

    @Test
    public void testRenderedTileIsInstalledAndRedrawn() {
        final TestLayer layer = new TestLayer(renderer(new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB)));
        final AtomicInteger redraws = new AtomicInteger();
        layer.addPropertyChangeListener(evt -> redraws.incrementAndGet());
        final TextureTile tile = tile(layer, 3, 5);

        layer.createRenderTask(tile).run();

        assertEquals(1, layer.installed.size());
        assertEquals(1, redraws.get());
        assertFalse(layer.getLevels().isResourceAbsent(tile));
    }

    @Test
    public void testTileWithoutDataIsMarkedAbsent() {
        final TestLayer layer = new TestLayer(renderer(null));
        final TextureTile tile = tile(layer, 3, 5);

        layer.createRenderTask(tile).run();

        assertTrue(layer.installed.isEmpty());
        assertTrue(layer.getLevels().isResourceAbsent(tile));
    }

    @Test
    public void testRenderFailureIsMarkedAbsent() {
        final ProductTileRenderer renderer = mock(ProductTileRenderer.class);
        when(renderer.render(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("broken colour palette"));
        final TestLayer layer = new TestLayer(renderer);
        final TextureTile tile = tile(layer, 3, 5);

        layer.createRenderTask(tile).run();

        assertTrue(layer.installed.isEmpty());
        assertTrue(layer.getLevels().isResourceAbsent(tile));
        // the second failure is quieter, but handled the same way
        final TextureTile other = tile(layer, 3, 6);
        layer.createRenderTask(other).run();
        assertTrue(layer.getLevels().isResourceAbsent(other));
    }

    @Test
    public void testDisposedLayerRendersNothing() {
        final ProductTileRenderer renderer = renderer(new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB));
        final TestLayer layer = new TestLayer(renderer);
        layer.dispose();

        layer.createRenderTask(tile(layer, 3, 5)).run();

        verify(renderer, never()).render(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt());
        assertTrue(layer.installed.isEmpty());
    }

    @Test
    public void testFailureAfterDisposalIsNotAnError() {
        final ProductTileRenderer renderer = mock(ProductTileRenderer.class);
        final TestLayer[] layer = new TestLayer[1];
        when(renderer.render(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt())).thenAnswer(i -> {
            // the product is closed while the tile is rendered
            layer[0].dispose();
            throw new IllegalStateException("raster disposed");
        });
        layer[0] = new TestLayer(renderer);
        final TextureTile tile = tile(layer[0], 3, 5);

        layer[0].createRenderTask(tile).run();

        assertFalse(layer[0].getLevels().isResourceAbsent(tile));
    }

    @Test
    public void testATileRequestedAgainIsRenderedOnce() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger renders = new AtomicInteger();
        final ProductTileRenderer renderer = mock(ProductTileRenderer.class);
        when(renderer.render(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt())).thenAnswer(i -> {
            renders.incrementAndGet();
            release.await(10, TimeUnit.SECONDS);
            return null;
        });
        final TestLayer layer = new TestLayer(renderer);
        final TextureTile tile = tile(layer, 3, 5);

        // the same tile asked for in two frames while it is still being rendered
        layer.submit(layer.createRenderTask(tile));
        Thread.sleep(200);
        layer.submit(layer.createRenderTask(tile));
        release.countDown();
        final long end = System.currentTimeMillis() + 10000;
        while (!layer.getLevels().isResourceAbsent(tile) && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
        Thread.sleep(200);
        assertEquals(1, renders.get());

        // once done, it can be requested again
        layer.getLevels().unmarkResourceAbsent(tile);
        layer.submit(layer.createRenderTask(tile));
        final long end2 = System.currentTimeMillis() + 10000;
        while (renders.get() < 2 && System.currentTimeMillis() < end2) {
            Thread.sleep(20);
        }
        assertEquals(2, renders.get());
    }
}
