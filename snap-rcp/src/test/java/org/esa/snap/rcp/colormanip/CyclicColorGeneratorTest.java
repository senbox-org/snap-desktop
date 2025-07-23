package org.esa.snap.rcp.colormanip;

import static org.junit.Assert.assertEquals;

import java.awt.Color;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

public class CyclicColorGeneratorTest {

  @Test
  @STTM("SNAP-4048")
  public void testGetNextDistinctColor_returnsColorsInCorrectOrder() {
    CyclicColorGenerator generator = new CyclicColorGenerator();
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
  @STTM("SNAP-4048")
  public void testGetNextDistinctColor_repeatsAfterAllColors() {
    CyclicColorGenerator generator = new CyclicColorGenerator();
    for (int i = 0; i < generator.getColorCount(); i++) {
      generator.getNextDistinctColor();
    }
    assertEquals(new Color(0, 0, 255), generator.getNextDistinctColor());
  }

  @Test
  @STTM("SNAP-4048")
  public void testGetNextDistinctColor_afterResetStartsFromFirstColor() {
    CyclicColorGenerator generator = new CyclicColorGenerator();
    generator.getNextDistinctColor();
    generator.getNextDistinctColor();
    generator.resetColorIndex();
    assertEquals(new Color(0, 0, 255), generator.getNextDistinctColor());
  }

  @Test
  public void testGetColorCount_returnsCorrectNumberOfDistinctColors() {
    CyclicColorGenerator generator = new CyclicColorGenerator();
    assertEquals(8, generator.getColorCount());
  }

  @Test
  @STTM("SNAP-4048")
  public void testGetColorByIndex_returnsCorrectColorForValidIndex() {
    CyclicColorGenerator generator = new CyclicColorGenerator();
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
  @STTM("SNAP-4048")
  public void testGetColorByIndex_throwsExceptionForNegativeIndex() {
    CyclicColorGenerator generator = new CyclicColorGenerator();
    generator.getColorByIndex(-1);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  @STTM("SNAP-4048")
  public void testGetColorByIndex_throwsExceptionForIndexOutOfRange() {
    CyclicColorGenerator generator = new CyclicColorGenerator();
    generator.getColorByIndex(8);
  }

  @Test
  @STTM("SNAP-4048")
  public void testConstructorWithCustomColors_usesProvidedColors() {
    Color[] customColors = {
        new Color(255, 255, 255), // White
        new Color(0, 0, 0),       // Black
        new Color(128, 128, 128)  // Gray
    };
    CyclicColorGenerator generator = new CyclicColorGenerator(customColors);

    assertEquals(new Color(255, 255, 255), generator.getNextDistinctColor());
    assertEquals(new Color(0, 0, 0), generator.getNextDistinctColor());
    assertEquals(new Color(128, 128, 128), generator.getNextDistinctColor());
  }

  @Test
  @STTM("SNAP-4048")
  public void testConstructorWithCustomColors_correctColorCount() {
    Color[] customColors = {
        new Color(255, 255, 255),
        new Color(0, 0, 0)
    };
    CyclicColorGenerator generator = new CyclicColorGenerator(customColors);

    assertEquals(2, generator.getColorCount());
  }


}