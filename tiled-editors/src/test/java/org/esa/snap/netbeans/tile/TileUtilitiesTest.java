package org.esa.snap.netbeans.tile;

import org.junit.Test;

import java.awt.*;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class TileUtilitiesTest {
    @Test
    public void testComputeMatrixSizeForEqualAreaTiling() throws Exception {
        assertEquals(new Dimension(-1, -1), TileUtilities.computeMatrixSizeForEqualAreaTiling(0));
        assertEquals(new Dimension(1, 1), TileUtilities.computeMatrixSizeForEqualAreaTiling(1));
        assertEquals(new Dimension(2, 1), TileUtilities.computeMatrixSizeForEqualAreaTiling(2));
        assertEquals(new Dimension(2, 2), TileUtilities.computeMatrixSizeForEqualAreaTiling(3));
        assertEquals(new Dimension(2, 2), TileUtilities.computeMatrixSizeForEqualAreaTiling(4));
        assertEquals(new Dimension(3, 2), TileUtilities.computeMatrixSizeForEqualAreaTiling(5));
        assertEquals(new Dimension(3, 2), TileUtilities.computeMatrixSizeForEqualAreaTiling(6));
        assertEquals(new Dimension(3, 3), TileUtilities.computeMatrixSizeForEqualAreaTiling(7));
        assertEquals(new Dimension(3, 3), TileUtilities.computeMatrixSizeForEqualAreaTiling(8));
        assertEquals(new Dimension(3, 3), TileUtilities.computeMatrixSizeForEqualAreaTiling(9));
        assertEquals(new Dimension(4, 3), TileUtilities.computeMatrixSizeForEqualAreaTiling(10));
        assertEquals(new Dimension(4, 3), TileUtilities.computeMatrixSizeForEqualAreaTiling(11));
        assertEquals(new Dimension(4, 3), TileUtilities.computeMatrixSizeForEqualAreaTiling(12));
        assertEquals(new Dimension(4, 4), TileUtilities.computeMatrixSizeForEqualAreaTiling(13));
        assertEquals(new Dimension(4, 4), TileUtilities.computeMatrixSizeForEqualAreaTiling(14));
        assertEquals(new Dimension(4, 4), TileUtilities.computeMatrixSizeForEqualAreaTiling(15));
        assertEquals(new Dimension(4, 4), TileUtilities.computeMatrixSizeForEqualAreaTiling(16));
    }

    @Test
    public void testStreaming() throws Exception {
        Object[] input = new Object[]{1, new Object[]{2, 3, 4,}, 5};
        Object[] output = Arrays.stream(input).flatMap(new Function<Object, Stream<?>>() {
            @Override
            public Stream<?> apply(Object o) {
                if (o instanceof Object[]) {
                    Object[] objects = (Object[]) o;
                    return Arrays.stream(objects);
                } else {
                    return Stream.of(o);
                }
            }
        }).toArray();
        assertEquals(5, output.length);
        assertEquals(1, output[0]);
        assertEquals(2, output[1]);
        assertEquals(3, output[2]);
        assertEquals(4, output[3]);
        assertEquals(5, output[4]);
    }
}
