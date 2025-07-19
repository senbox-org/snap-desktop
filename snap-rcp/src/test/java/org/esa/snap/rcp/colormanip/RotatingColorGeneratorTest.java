package org.esa.snap.rcp.colormanip;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import org.junit.Test;

public class RotatingColorGeneratorTest {

  @Test
  public void testGetNextDistinctColor_returnsColorsInCorrectOrder() {
    RotatingColorGenerator generator = new RotatingColorGenerator();
    assertEquals(new Color(0, 0, 255), generator.getNextDistinctColor());
    assertEquals(new Color(255, 0, 0), generator.getNextDistinctColor());
    assertEquals(new Color(0, 255, 0), generator.getNextDistinctColor());
    assertEquals(new Color(255, 255, 0), generator.getNextDistinctColor());
    assertEquals(new Color(255, 0, 255), generator.getNextDistinctColor());
    assertEquals(new Color(255, 128, 0), generator.getNextDistinctColor());
    assertEquals(new Color(0, 255, 255), generator.getNextDistinctColor());
    assertEquals(new Color(128, 0, 255), generator.getNextDistinctColor());
  }

  @Test
  public void testGetNextDistinctColor_repeatsAfterAllColors() {
    RotatingColorGenerator generator = new RotatingColorGenerator();
    for (int i = 0; i < generator.getColorCount(); i++) {
      generator.getNextDistinctColor();
    }
    assertEquals(new Color(0, 0, 255), generator.getNextDistinctColor());
  }

  @Test
  public void testGetNextDistinctColor_afterResetStartsFromFirstColor() {
    RotatingColorGenerator generator = new RotatingColorGenerator();
    generator.getNextDistinctColor();
    generator.getNextDistinctColor();
    generator.resetColorIndex();
    assertEquals(new Color(0, 0, 255), generator.getNextDistinctColor());
  }

  @Test
  public void testGetColorCount_returnsCorrectNumberOfDistinctColors() {
    RotatingColorGenerator generator = new RotatingColorGenerator();
    assertEquals(8, generator.getColorCount());
  }

  @Test
  public void testGetColorByIndex_returnsCorrectColorForValidIndex() {
    RotatingColorGenerator generator = new RotatingColorGenerator();
    assertEquals(new Color(0, 0, 255), generator.getColorByIndex(0));
    assertEquals(new Color(255, 0, 0), generator.getColorByIndex(1));
    assertEquals(new Color(0, 255, 0), generator.getColorByIndex(2));
    assertEquals(new Color(255, 255, 0), generator.getColorByIndex(3));
    assertEquals(new Color(255, 0, 255), generator.getColorByIndex(4));
    assertEquals(new Color(255, 128, 0), generator.getColorByIndex(5));
    assertEquals(new Color(0, 255, 255), generator.getColorByIndex(6));
    assertEquals(new Color(128, 0, 255), generator.getColorByIndex(7));
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testGetColorByIndex_throwsExceptionForNegativeIndex() {
    RotatingColorGenerator generator = new RotatingColorGenerator();
    generator.getColorByIndex(-1);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testGetColorByIndex_throwsExceptionForIndexOutOfRange() {
    RotatingColorGenerator generator = new RotatingColorGenerator();
    generator.getColorByIndex(8);
  }

  @Test
  public void testConstructorWithCustomColors_usesProvidedColors() {
    Color[] customColors = {
        new Color(255, 255, 255), // White
        new Color(0, 0, 0),       // Black
        new Color(128, 128, 128)  // Gray
    };
    RotatingColorGenerator generator = new RotatingColorGenerator(customColors);

    assertEquals(new Color(255, 255, 255), generator.getNextDistinctColor());
    assertEquals(new Color(0, 0, 0), generator.getNextDistinctColor());
    assertEquals(new Color(128, 128, 128), generator.getNextDistinctColor());
  }

  @Test
  public void testConstructorWithCustomColors_correctColorCount() {
    Color[] customColors = {
        new Color(255, 255, 255),
        new Color(0, 0, 0)
    };
    RotatingColorGenerator generator = new RotatingColorGenerator(customColors);

    assertEquals(2, generator.getColorCount());
  }


}