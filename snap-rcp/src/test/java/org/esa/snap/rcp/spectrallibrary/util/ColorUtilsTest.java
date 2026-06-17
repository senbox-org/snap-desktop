package org.esa.snap.rcp.spectrallibrary.util;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import java.awt.Color;
import java.util.UUID;

import static org.junit.Assert.*;


public class ColorUtilsTest {


    @Test
    @STTM("SNAP-4206")
    public void toHex_returnsNullForNull() {
        assertNull(ColorUtils.toHex(null));
    }

    @Test
    @STTM("SNAP-4206")
    public void toHex_formatsBlack() {
        assertEquals("#000000", ColorUtils.toHex(Color.BLACK));
    }

    @Test
    @STTM("SNAP-4206")
    public void toHex_formatsWhite() {
        assertEquals("#FFFFFF", ColorUtils.toHex(Color.WHITE));
    }

    @Test
    @STTM("SNAP-4206")
    public void toHex_formatsPrimaryColors() {
        assertEquals("#FF0000", ColorUtils.toHex(Color.RED));
        assertEquals("#00FF00", ColorUtils.toHex(Color.GREEN));
        assertEquals("#0000FF", ColorUtils.toHex(Color.BLUE));
    }

    @Test
    @STTM("SNAP-4206")
    public void toHex_formatsDefaultPaletteColors() {
        assertEquals("#1F77B4", ColorUtils.toHex(ColorUtils.DEFAULT_PALETTE[0]));
        assertEquals("#FF7F0E", ColorUtils.toHex(ColorUtils.DEFAULT_PALETTE[1]));
        assertEquals("#2CA02C", ColorUtils.toHex(ColorUtils.DEFAULT_PALETTE[2]));
    }

    @Test
    @STTM("SNAP-4206")
    public void toHex_ignoresAlphaChannel() {
        Color semiTransparent = new Color(100, 150, 200, 128);
        assertEquals("#6496C8", ColorUtils.toHex(semiTransparent));
    }


    @Test
    @STTM("SNAP-4206")
    public void parseCssColor_returnsNullForNull() {
        assertNull(ColorUtils.parseCssColor(null));
    }

    @Test
    @STTM("SNAP-4206")
    public void parseCssColor_returnsNullForEmpty() {
        assertNull(ColorUtils.parseCssColor(""));
        assertNull(ColorUtils.parseCssColor("   "));
    }

    @Test
    @STTM("SNAP-4206")
    public void parseCssColor_returnsNullForNone() {
        assertNull(ColorUtils.parseCssColor("none"));
        assertNull(ColorUtils.parseCssColor("NONE"));
    }

    @Test
    @STTM("SNAP-4206")
    public void parseCssColor_returnsNullForTransparent() {
        assertNull(ColorUtils.parseCssColor("transparent"));
        assertNull(ColorUtils.parseCssColor("Transparent"));
    }


    @Test
    @STTM("SNAP-4206")
    public void parseCssColor_parsesSixDigitHex() {
        Color c = ColorUtils.parseCssColor("#1F77B4");
        assertNotNull(c);
        assertEquals(31, c.getRed());
        assertEquals(119, c.getGreen());
        assertEquals(180, c.getBlue());
    }

    @Test
    @STTM("SNAP-4206")
    public void parseCssColor_parsesSixDigitHex_lowerCase() {
        Color c = ColorUtils.parseCssColor("#ff7f0e");
        assertNotNull(c);
        assertEquals(255, c.getRed());
        assertEquals(127, c.getGreen());
        assertEquals(14, c.getBlue());
    }

    @Test
    @STTM("SNAP-4206")
    public void parseCssColor_parsesSixDigitHex_blackAndWhite() {
        assertEquals(Color.BLACK, ColorUtils.parseCssColor("#000000"));
        assertEquals(Color.WHITE, ColorUtils.parseCssColor("#FFFFFF"));
    }


    @Test
    @STTM("SNAP-4206")
    public void parseCssColor_parsesThreeDigitHex() {
        Color c = ColorUtils.parseCssColor("#f00");
        assertNotNull(c);
        assertEquals(255, c.getRed());
        assertEquals(0, c.getGreen());
        assertEquals(0, c.getBlue());
    }

    @Test
    @STTM("SNAP-4206")
    public void parseCssColor_parsesThreeDigitHex_abc() {
        Color c = ColorUtils.parseCssColor("#abc");
        assertNotNull(c);
        assertEquals(0xAA, c.getRed());
        assertEquals(0xBB, c.getGreen());
        assertEquals(0xCC, c.getBlue());
    }


    @Test
    @STTM("SNAP-4206")
    public void parseCssColor_parsesRgb() {
        Color c = ColorUtils.parseCssColor("rgb(255, 127, 14)");
        assertNotNull(c);
        assertEquals(255, c.getRed());
        assertEquals(127, c.getGreen());
        assertEquals(14, c.getBlue());
        assertEquals(255, c.getAlpha());
    }

    @Test
    @STTM("SNAP-4206")
    public void parseCssColor_parsesRgb_noSpaces() {
        Color c = ColorUtils.parseCssColor("rgb(44,160,44)");
        assertNotNull(c);
        assertEquals(44, c.getRed());
        assertEquals(160, c.getGreen());
        assertEquals(44, c.getBlue());
    }


    @Test
    @STTM("SNAP-4206")
    public void parseCssColor_parsesRgba_alphaOne() {
        Color c = ColorUtils.parseCssColor("rgba(44, 160, 44, 1.0)");
        assertNotNull(c);
        assertEquals(44, c.getRed());
        assertEquals(160, c.getGreen());
        assertEquals(44, c.getBlue());
        assertEquals(255, c.getAlpha());
    }

    @Test
    @STTM("SNAP-4206")
    public void parseCssColor_parsesRgba_alphaHalf() {
        Color c = ColorUtils.parseCssColor("rgba(100, 100, 100, 0.5)");
        assertNotNull(c);
        assertEquals(100, c.getRed());
        assertEquals(100, c.getGreen());
        assertEquals(100, c.getBlue());
        assertEquals(128, c.getAlpha());
    }

    @Test
    @STTM("SNAP-4206")
    public void parseCssColor_parsesRgba_alphaZero() {
        Color c = ColorUtils.parseCssColor("rgba(200, 200, 200, 0)");
        assertNotNull(c);
        assertEquals(0, c.getAlpha());
    }


    @Test
    @STTM("SNAP-4206")
    public void parseCssColor_parsesPercentValues() {
        Color c = ColorUtils.parseCssColor("rgb(100%, 50%, 0%)");
        assertNotNull(c);
        assertEquals(255, c.getRed());
        assertEquals(128, c.getGreen());
        assertEquals(0, c.getBlue());
    }


    @Test
    @STTM("SNAP-4206")
    public void parseCssColor_clampsOverflow() {
        Color c = ColorUtils.parseCssColor("rgb(300, -10, 256)");
        assertNotNull(c);
        assertEquals(255, c.getRed());
        assertEquals(0, c.getGreen());
        assertEquals(255, c.getBlue());
    }


    @Test
    @STTM("SNAP-4206")
    public void parseCssColor_rejectsInvalidStrings() {
        assertNull(ColorUtils.parseCssColor("foo"));
        assertNull(ColorUtils.parseCssColor("red"));
        assertNull(ColorUtils.parseCssColor("#XYZ"));
        assertNull(ColorUtils.parseCssColor("#12345"));
        assertNull(ColorUtils.parseCssColor("rgb()"));
        assertNull(ColorUtils.parseCssColor("rgb(1, 2)"));
    }


    @Test
    @STTM("SNAP-4206")
    public void roundtrip_allDefaultPaletteColors() {
        for (int i = 0; i < ColorUtils.DEFAULT_PALETTE.length; i++) {
            Color original = ColorUtils.DEFAULT_PALETTE[i];
            String hex = ColorUtils.toHex(original);
            Color parsed = ColorUtils.parseCssColor(hex);
            assertNotNull("roundtrip failed for palette[" + i + "] hex=" + hex, parsed);
            assertEquals("red mismatch for palette[" + i + "]", original.getRed(), parsed.getRed());
            assertEquals("green mismatch for palette[" + i + "]", original.getGreen(), parsed.getGreen());
            assertEquals("blue mismatch for palette[" + i + "]", original.getBlue(), parsed.getBlue());
        }
    }

    @Test
    @STTM("SNAP-4206")
    public void roundtrip_extremeValues() {
        Color[] extremes = {
                Color.BLACK, Color.WHITE, Color.RED, Color.GREEN, Color.BLUE,
                new Color(1, 1, 1), new Color(254, 254, 254), new Color(128, 64, 32)
        };
        for (Color original : extremes) {
            String hex = ColorUtils.toHex(original);
            Color parsed = ColorUtils.parseCssColor(hex);
            assertNotNull("roundtrip failed for " + original, parsed);
            assertEquals(original.getRed(), parsed.getRed());
            assertEquals(original.getGreen(), parsed.getGreen());
            assertEquals(original.getBlue(), parsed.getBlue());
        }
    }


    @Test
    @STTM("SNAP-4206")
    public void defaultColor_deterministicForSameUuid() {
        UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Color c1 = ColorUtils.defaultColor(id);
        Color c2 = ColorUtils.defaultColor(id);
        assertNotNull(c1);
        assertEquals(c1, c2);
    }

    @Test
    @STTM("SNAP-4206")
    public void defaultColor_neverReturnsNullForNonNullId() {
        for (int i = 0; i < 100; i++) {
            Color c = ColorUtils.defaultColor(UUID.randomUUID());
            assertNotNull(c);
        }
    }

    @Test
    @STTM("SNAP-4206")
    public void defaultColor_alwaysFromPalette() {
        for (int i = 0; i < 100; i++) {
            Color c = ColorUtils.defaultColor(UUID.randomUUID());
            boolean found = false;
            for (Color paletteColor : ColorUtils.DEFAULT_PALETTE) {
                if (paletteColor.equals(c)) {
                    found = true;
                    break;
                }
            }
            assertTrue("defaultColor returned a color not in DEFAULT_PALETTE: " + c, found);
        }
    }
}
