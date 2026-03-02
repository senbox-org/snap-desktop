package org.esa.snap.rcp.spectrallibrary.util;

import org.esa.snap.core.datamodel.Placemark;

import java.awt.*;
import java.util.Locale;
import java.util.regex.Pattern;


public class PreviewUtils {


    public static Color colorFromPlacemark(Placemark pm) {
        if (pm == null) {
            return null;
        }
        String css = pm.getStyleCss();
        String v = cssProp(css, "fill");
        if (v == null) {
            v = cssProp(css, "stroke");
        }
        return parseCssColor(v);
    }

    private static String cssProp(String css, String prop) {
        if (css == null) {
            return null;
        }
        var m = Pattern.compile("(?i)\\b" + Pattern.quote(prop) + "\\s*:\\s*([^;]+)").matcher(css);
        return m.find() ? m.group(1).trim() : null;
    }

    private static Color parseCssColor(String v) {
        if (v == null) {
            return null;
        }
        String s = v.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty() || "none".equals(s) || "transparent".equals(s)) {
            return null;
        }

        try {
            if (s.charAt(0) == '#') {
                String hex = s.substring(1).trim();
                if (hex.length() == 3) {
                    int r = Integer.parseInt(hex.substring(0, 1) + hex.substring(0, 1), 16);
                    int g = Integer.parseInt(hex.substring(1, 2) + hex.substring(1, 2), 16);
                    int b = Integer.parseInt(hex.substring(2, 3) + hex.substring(2, 3), 16);
                    return new Color(r, g, b);
                }
                if (hex.length() == 6) {
                    int r = Integer.parseInt(hex.substring(0, 2), 16);
                    int g = Integer.parseInt(hex.substring(2, 4), 16);
                    int b = Integer.parseInt(hex.substring(4, 6), 16);
                    return new Color(r, g, b);
                }
                return null;
            }

            if (s.startsWith("rgb(") || s.startsWith("rgba(")) {
                int open = s.indexOf('(');
                int close = s.lastIndexOf(')');
                if (open < 0 || close < 0 || close <= open) {
                    return null;
                }

                String[] parts = s.substring(open + 1, close).split(",");
                if (parts.length < 3) {
                    return null;
                }

                int r = parseCssInt(parts[0]);
                int g = parseCssInt(parts[1]);
                int b = parseCssInt(parts[2]);

                int a = 255;
                if (parts.length >= 4) {
                    a = parseCssAlpha(parts[3]);
                }
                return new Color(r, g, b, a);
            }
        } catch (RuntimeException ignore) {}

        return null;
    }

    private static int parseCssInt(String part) {
        String p = part.trim();
        if (p.endsWith("%")) {
            double pct = Double.parseDouble(p.substring(0, p.length() - 1).trim());
            return clamp255((int) Math.round(255.0 * (pct / 100.0)));
        }
        return clamp255((int) Math.round(Double.parseDouble(p)));
    }

    private static int parseCssAlpha(String part) {
        String p = part.trim();
        if (p.endsWith("%")) {
            double pct = Double.parseDouble(p.substring(0, p.length() - 1).trim());
            return clamp255((int) Math.round(255.0 * (pct / 100.0)));
        }
        double d = Double.parseDouble(p);

        if (d <= 1.0) {
            return clamp255((int) Math.round(255.0 * d));
        }
        return clamp255((int) Math.round(d));
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
