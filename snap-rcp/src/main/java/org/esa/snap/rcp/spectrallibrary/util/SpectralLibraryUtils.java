package org.esa.snap.rcp.spectrallibrary.util;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;
import org.esa.snap.ui.product.ProductSceneView;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.util.AffineTransformation;

import java.awt.geom.AffineTransform;
import java.time.Instant;
import java.util.*;


public class SpectralLibraryUtils {


    private static final String PROFILE_PREFIX = "Spectrum_";
    private static final String ATTR_WKT = "wkt";
    private static final String ATTR_PRODUCT_NAME = "product_name";
    private static final String ATTR_DATE_TIME = "time";

    private static final int PREVIEW_DISPLAY_LIMIT = 10_000;


    public static SpectralAxis axisFromBands(List<Band> bands) {
        Objects.requireNonNull(bands, "bands must not be null");

        List<Band> spectralBands = new ArrayList<>();
        for (Band b : bands) {
            if (b == null) {
                continue;
            }
            if (b.isFlagBand()) {
                continue;
            }
            if (b.getSpectralWavelength() <= 0.0f) {
                continue;
            }
            spectralBands.add(b);
        }

        spectralBands.sort(Comparator.comparingDouble(Band::getSpectralWavelength));

        if (spectralBands.isEmpty()) {
            throw new IllegalArgumentException("No spectral bands (wavelength > 0) found");
        }

        double[] wl = new double[spectralBands.size()];
        for (int i = 0; i < spectralBands.size(); i++) {
            wl[i] = spectralBands.get(i).getSpectralWavelength();
        }

        return new SpectralAxis(wl, "nm");
    }


    public static String defaultYUnitFromBands(List<Band> bands) {
        Objects.requireNonNull(bands, "bands must not be null");

        String yUnit = null;
        for (Band b : bands) {
            if (b == null || b.isFlagBand() || b.getSpectralWavelength() <= 0.0f) continue;
            String u = b.getUnit();
            if (u != null && !u.isBlank()) {
                yUnit = u;
                break;
            }
        }
        return yUnit;
    }

    public static SpectralAxis axisFromReferenceSpectralGroup(List<Band> bands) {
        List<Band> spectral = new ArrayList<>();
        for (Band b : bands) {
            if (b == null || b.isFlagBand() || b.getSpectralWavelength() <= 0.0f) {
                continue;
            }
            spectral.add(b);
        }
        spectral.sort(Comparator.comparingDouble(Band::getSpectralWavelength));
        if (spectral.isEmpty()) {
            throw new IllegalArgumentException("No spectral bands (wavelength > 0) found");
        }

        List<Double> uniqueWl = new ArrayList<>();
        double last = Double.NaN;
        for (Band b : spectral) {
            double wl = b.getSpectralWavelength();
            if (uniqueWl.isEmpty() || Double.compare(wl, last) != 0) {
                uniqueWl.add(wl);
                last = wl;
            }
        }

        double[] wl = new double[uniqueWl.size()];
        for (int i = 0; i < uniqueWl.size(); i++) {
            wl[i] = uniqueWl.get(i);
        }
        return new SpectralAxis(wl, "nm");
    }

    public static List<Band> sortSpectralBandsByWavelength(List<Band> bands) {
        List<Band> spectral = new ArrayList<>();
        for (Band b : bands) {
            if (b != null && !b.isFlagBand() && b.getSpectralWavelength() > 0.0f) {
                spectral.add(b);
            }
        }

        spectral.sort(Comparator.comparingDouble(Band::getSpectralWavelength));
        if (spectral.isEmpty()) {
            throw new IllegalArgumentException("No spectral bands (wavelength > 0) found");
        }
        return spectral;
    }


    public record AxisBandSelection(List<Band> bandsOrdered, Set<String> bandNames) {}


    public static AxisBandSelection selectAxisBandsUniqueByWavelength(List<Band> bands) {
        Objects.requireNonNull(bands, "bands must not be null");
        LinkedHashMap<Long, Band> bestByWl = new LinkedHashMap<>();

        for (Band b : bands) {
            if (b == null || b.isFlagBand() || b.getSpectralWavelength() <= 0.0f) {
                continue;
            }

            double wl = b.getSpectralWavelength();
            long key = Math.round(wl * 1_000_000d);

            Band cur = bestByWl.get(key);
            if (cur == null || isBetterAxisBand(b, cur)) {
                bestByWl.put(key, b);
            }
        }

        if (bestByWl.isEmpty()) {
            throw new IllegalArgumentException("No spectral bands (wavelength > 0) found");
        }

        List<Band> ordered = new ArrayList<>(bestByWl.values());
        ordered.sort(Comparator.comparingDouble(Band::getSpectralWavelength)
                .thenComparing(Band::getName, Comparator.nullsLast(String::compareTo)));

        Set<String> names = new LinkedHashSet<>();
        for (Band b : ordered) {
            if (b != null && b.getName() != null) {
                names.add(b.getName());
            }
        }

        return new AxisBandSelection(List.copyOf(ordered), Collections.unmodifiableSet(names));
    }

    public static SpectralAxis axisFromOrderedBands(List<Band> orderedSpectralBands) {
        double[] wl = new double[orderedSpectralBands.size()];
        for (int i = 0; i < orderedSpectralBands.size(); i++) {
            wl[i] = orderedSpectralBands.get(i).getSpectralWavelength();
        }
        return new SpectralAxis(wl, "nm");
    }


    private static boolean isBetterAxisBand(Band candidate, Band current) {
        int rc = Integer.compare(axisBandRank(candidate), axisBandRank(current));
        if (rc != 0) {
            return rc > 0;
        }

        rc = Boolean.compare(candidate.isScalingApplied(), current.isScalingApplied());
        if (rc != 0) {
            return rc > 0;
        }

        String cn = candidate.getName();
        String on = current.getName();
        if (cn == null && on == null) {
            return false;
        }
        if (cn == null) {
            return false;
        }
        if (on == null) {
            return true;
        }
        return cn.compareTo(on) < 0;
    }

    private static int axisBandRank(Band b) {
        if (b == null) {
            return Integer.MIN_VALUE;
        }

        return switch (b.getDataType()) {
            case ProductData.TYPE_FLOAT64 -> 4;
            case ProductData.TYPE_FLOAT32 -> 3;
            case ProductData.TYPE_INT32, ProductData.TYPE_UINT32 -> 2;
            case ProductData.TYPE_INT16, ProductData.TYPE_UINT16 -> 1;
            case ProductData.TYPE_INT8, ProductData.TYPE_UINT8 -> 0;
            default -> -1;
        };
    }

    public static List<PixelPos> pixelsFromGeometry(ProductSceneView view, Geometry modelGeom) {
        if (modelGeom == null || modelGeom.isEmpty()) {
            return new ArrayList<>();
        }

        AffineTransform m2i = view.getBaseImageLayer().getModelToImageTransform();
        AffineTransformation jtsTx = new AffineTransformation(
                m2i.getScaleX(), m2i.getShearX(), m2i.getTranslateX(),
                m2i.getShearY(), m2i.getScaleY(), m2i.getTranslateY()
        );

        Geometry pixGeom = jtsTx.transform(modelGeom);

        Envelope env = pixGeom.getEnvelopeInternal();
        int minX = (int) Math.floor(env.getMinX());
        int maxX = (int) Math.ceil (env.getMaxX());
        int minY = (int) Math.floor(env.getMinY());
        int maxY = (int) Math.ceil (env.getMaxY());

        int w = view.getBaseImageLayer().getImage().getWidth();
        int h = view.getBaseImageLayer().getImage().getHeight();
        minX = Math.max(0, minX); minY = Math.max(0, minY);
        maxX = Math.min(w - 1, maxX); maxY = Math.min(h - 1, maxY);

        GeometryFactory gf = pixGeom.getFactory();
        ArrayList<PixelPos> pixels = new ArrayList<>();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                Point p = gf.createPoint(new Coordinate(x, y));
                if (pixGeom.covers(p)) {
                    pixels.add(new PixelPos(x, y));
                }
            }
        }
        return pixels;
    }

    public static List<Band> collectBandsInAutoGroupingOrder(Product product) {
        List<Band> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        var auto = product.getAutoGrouping();
        if (auto != null) {
            Iterator<String[]> it = auto.iterator();
            while (it.hasNext()) {
                String[] group = it.next();
                if (group == null) {
                    continue;
                }
                for (String bn : group) {
                    if (bn == null) {
                        continue;
                    }
                    Band b = product.getBand(bn);
                    if (b != null && seen.add(bn)) {
                        out.add(b);
                    }
                }
            }
        }

        for (Band b : product.getBands()) {
            if (b != null && b.getName() != null && seen.add(b.getName())) {
                out.add(b);
            }
        }
        return out;
    }

    public static Set<String> copySet(Set<String> in) {
        if (in == null || in.isEmpty()) {
            return null;
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(in));
    }

    public static Map<String, Set<String>> copyMapOfSets(Map<String, Set<String>> in) {
        if (in == null || in.isEmpty()) {
            return null;
        }

        LinkedHashMap<String, Set<String>> out = new LinkedHashMap<>();
        for (var e : in.entrySet()) {
            if (e.getKey() == null || e.getKey().isBlank() || e.getValue() == null || e.getValue().isEmpty()) {
                continue;
            }
            out.put(e.getKey(), Collections.unmodifiableSet(new LinkedHashSet<>(e.getValue())));
        }

        return out.isEmpty() ? null : Collections.unmodifiableMap(out);
    }


    public static SpectralProfile applyAttributes(SpectralProfile p, Map<String, AttributeValue> attrs) {
        if (p == null || attrs == null || attrs.isEmpty()) {
            return p;
        }

        SpectralProfile out = p;
        for (var e : attrs.entrySet()) {
            if (e.getKey() == null || e.getKey().isBlank() || e.getValue() == null) {
                continue;
            }
            out = out.withAttribute(e.getKey().trim(), e.getValue());
        }
        return out;
    }

    public static String normalizeUnit(String yUnit) {
        String unit = (yUnit == null) ? "" : yUnit.trim();
        return unit.isEmpty() ? "value" : unit;
    }

    public static List<SpectralProfile> safeCopyWithoutNulls(List<SpectralProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return List.of();
        }
        List<SpectralProfile> out = new ArrayList<>(profiles.size());
        for (SpectralProfile p : profiles) {
            if (p != null) {
                out.add(p);
            }
        }
        return List.copyOf(out);
    }

    public static SpectralProfile maskUnselectedToNaN(SpectralProfile p, List<Band> bands, Set<String> selected) {
        if (p == null || selected == null || selected.isEmpty()) {
            return p;
        }

        var sig = p.getSignature();

        double[] y = sig.getValues();
        String yUnit = sig.getYUnitOrNull();

        for (int i = 0; i < bands.size() && i < y.length; i++) {
            Band b = bands.get(i);
            if (b != null && !selected.contains(b.getName())) {
                y[i] = Double.NaN;
            }
        }

        SpectralSignature maskedSig = SpectralSignature.of(y, yUnit);

        return new SpectralProfile(p.getId(), p.getName(), maskedSig, p.getAttributes(), p.getSourceRef().orElse(null));
    }

    public static String normalizePrefix(String raw) {
        String p = raw == null ? "" : raw.trim();
        if (p.isEmpty()) {
            return PROFILE_PREFIX;
        }

        p = p.replaceAll("\\s+", "_").replaceAll("[\\\\/:*?\"<>|]", "_");
        return p.endsWith("_") ? p : (p + "_");
    }


    public static String nameOf(SpectralProfile p) {
        if (p == null || p.getName() == null) {
            return null;
        }
        String n = p.getName().trim();
        return n.isEmpty() ? null : n;
    }

    public static int extractAutoIndex(String prefix, String name) {
        if (prefix == null || name == null) {
            return 0;
        }
        String n = name.trim();
        if (!n.startsWith(prefix)) {
            return 0;
        }

        int i = prefix.length();
        int j = i;
        while (j < n.length() && Character.isDigit(n.charAt(j))) {
            j++;
        }
        if (j == i) {
            return 0;
        }

        try {
            return Integer.parseInt(n.substring(i, j));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static SpectralProfile withDefaultAttributesIfPossible(Product product, int x, int y, SpectralProfile profile) {
        profile = withWktIfPossible(product, x, y, profile);
        profile = withProductNameIfPossible(product, profile);
        profile = withDateTimeIfPossible(product, profile);
        return profile;
    }

    private static SpectralProfile withWktIfPossible(Product product, int x, int y, SpectralProfile profile) {
        String wkt = wktPointForPixel(product, x, y);
        if (wkt != null && !wkt.isBlank()) {
            profile = profile.withAttribute(ATTR_WKT, AttributeValue.ofString(wkt));
        }
        return profile;
    }


    private static String wktPointForPixel(Product product, int x, int y) {
        var gc = product.getSceneGeoCoding();
        if (gc == null) {
            return null;
        }

        PixelPos px = new PixelPos(x, y);
        GeoPos gp = gc.getGeoPos(px, null);

        if (gp == null || !gp.isValid()) {
            return null;
        }
        return "POINT (" + gp.lon + " " + gp.lat + ")";
    }

    private static SpectralProfile withProductNameIfPossible(Product product, SpectralProfile profile) {
        String name = product.getName();
        if (name != null) {
            profile = profile.withAttribute(ATTR_PRODUCT_NAME, AttributeValue.ofString(name));
        }
        return profile;
    }

    private static SpectralProfile withDateTimeIfPossible(Product product, SpectralProfile profile) {
        ProductData.UTC startTime = product.getStartTime();
        if (startTime != null) {
            Instant instant = startTime.getAsCalendar().toInstant();
            profile = profile.withAttribute(ATTR_DATE_TIME, AttributeValue.ofInstant(instant));
        }
        return profile;
    }


    public static List<SpectralProfile> limitForDisplay(List<SpectralProfile> all) {
        if (all == null || all.isEmpty()) {
            return List.of();
        }

        List<SpectralProfile> clean = safeCopyWithoutNulls(all);
        int n = clean.size();
        if (n <= PREVIEW_DISPLAY_LIMIT) {
            return clean;
        }

        if (PREVIEW_DISPLAY_LIMIT == 1) {
            return List.of(clean.get(n - 1));
        }

        List<SpectralProfile> out = new ArrayList<>(PREVIEW_DISPLAY_LIMIT);
        for (int i = 0; i < PREVIEW_DISPLAY_LIMIT; i++) {
            int idx = (int) (((long) i * (n - 1)) / (PREVIEW_DISPLAY_LIMIT - 1L));
            out.add(clean.get(idx));
        }
        return List.copyOf(out);
    }

    public static boolean containsId(List<SpectralProfile> list, UUID id) {
        if (id == null || list == null) {
            return false;
        }
        for (SpectralProfile p : list) {
            if (p != null && id.equals(p.getId())) {
                return true;
            }
        }
        return false;
    }
}
