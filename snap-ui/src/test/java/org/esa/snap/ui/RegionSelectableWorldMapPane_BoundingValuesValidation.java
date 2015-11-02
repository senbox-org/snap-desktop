package org.esa.snap.ui;

import org.junit.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import static org.junit.Assert.*;

public class RegionSelectableWorldMapPane_BoundingValuesValidation {

    private final double validNorthBound = 75.0;
    private final double validEastBound = 30.0;
    private final double validSouthBound = 20.0;
    private final double validWestBound = 10.0;

    @Test
    public void testValidBounds() {
        assertTrue(RegionSelectableWorldMapPane.geoBoundsAreValid(validNorthBound, validEastBound, validSouthBound, validWestBound));
    }

    @Test
    public void testThatReturnValueIsFalseIfAllBoundingValuesAreNull() {
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(null, null, null, null));
    }

    @Test
    public void testThatEachValueMustBeNotNull() {
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(null, validEastBound, validSouthBound, validWestBound));
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(validNorthBound, null, validSouthBound, validWestBound));
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(validNorthBound, validEastBound, null, validWestBound));
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(validNorthBound, validEastBound, validSouthBound, null));
    }

    @Test
    public void testThatNorthValueMustBeBiggerThanSouthValue() {
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(10.0, validEastBound, 10.0, validWestBound));
    }

    @Test
    public void testThatEastValueMustBeBiggerThanWestValue() {
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(validNorthBound, 10.0, validSouthBound, 10.0));
    }

    @Test
    public void testThatValuesAreInsideValidBounds() {
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(91.0, validEastBound, validSouthBound, validWestBound));
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(validNorthBound, 181.0, validSouthBound, validWestBound));
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(validNorthBound, validEastBound, -91.0, validWestBound));
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(validNorthBound, validEastBound, validSouthBound, -181.0));
    }


    @Test
    public void testCorrectBoundsIfNecessary_noCorrection() {
        final Rectangle2D.Double toCorrect = new Rectangle2D.Double(-175.0, -85.0, 140.0, 110.0);

        RegionSelectableWorldMapPane.correctBoundsIfNecessary(toCorrect);

        assertEquals(-175.0, toCorrect.getMinX(), 1e-8);
        assertEquals(-85.0, toCorrect.getMinY(), 1e-8);

        assertEquals(-175.0 + 140.0, toCorrect.getMaxX(), 1e-8);
        assertEquals(-85.0 + 110.0, toCorrect.getMaxY(), 1e-8);
    }

    @Test
    public void testCorrectBoundsIfNecessary_correction_positive_x() {
        final Rectangle2D.Double toCorrect = new Rectangle2D.Double(-175.499991, -90.0, 360.0, 180.0);

        RegionSelectableWorldMapPane.correctBoundsIfNecessary(toCorrect);

        assertEquals(-175.499991, toCorrect.getMinX(), 1e-8);
        assertEquals(180.0, toCorrect.getMaxX(), 1e-8);
        assertEquals(-90.0, toCorrect.getMinY(), 1e-8);
        assertEquals(90.0, toCorrect.getMaxY(), 1e-8);

        assertTrue(toCorrect.getMinX() + toCorrect.getWidth() <= 180.0);
        assertTrue(toCorrect.getMinY() + toCorrect.getHeight() <= 90.0);
    }

    @Test
    public void testCorrectBoundsIfNecessary_correction_negative_x() {
        final Rectangle2D.Double toCorrect = new Rectangle2D.Double(-185.499991, -90.0, 360.0, 180.0);

        RegionSelectableWorldMapPane.correctBoundsIfNecessary(toCorrect);

        assertEquals(-180.0, toCorrect.getMinX(), 1e-8);
        assertEquals(174.50000899999895, toCorrect.getMaxX(), 1e-8);
        assertEquals(-90.0, toCorrect.getMinY(), 1e-8);
        assertEquals(90.0, toCorrect.getMaxY(), 1e-8);

        assertTrue(toCorrect.getMinX() + toCorrect.getWidth() <= 180.0);
        assertTrue(toCorrect.getMinY() + toCorrect.getHeight() <= 90.0);
    }
}
